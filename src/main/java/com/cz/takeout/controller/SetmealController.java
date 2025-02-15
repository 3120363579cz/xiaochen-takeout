package com.cz.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cz.takeout.common.R;
import com.cz.takeout.dto.SetmealDto;
import com.cz.takeout.entity.Category;
import com.cz.takeout.entity.Setmeal;
import com.cz.takeout.service.CategoryService;
import com.cz.takeout.service.SetmealDishService;
import com.cz.takeout.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//套餐管理
@RestController
@RequestMapping("/setmeal")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    //新增套餐
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        log.info("套餐信息：{}",setmealDto);

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }

    //套餐分页查询
    @GetMapping("/page")
    public R<Page<SetmealDto>> page(
            @RequestParam int page,
            @RequestParam int pageSize,
            @RequestParam(required = false) String name
    ) {
            // 构造分页构造器对象
            Page<Setmeal> pageInfo = new Page<>(page, pageSize);
            Page<SetmealDto> dtoPage = new Page<>();

            // 条件构造器
            LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
            // 添加过滤条件（name 不为空时）
            queryWrapper.like(StringUtils.isNotEmpty(name), Setmeal::getName, name);
            // 添加排序条件
            queryWrapper.orderByDesc(Setmeal::getUpdateTime);

            // 执行分页查询
            setmealService.page(pageInfo, queryWrapper);

            // 对象拷贝（排除 records）
            BeanUtils.copyProperties(pageInfo, dtoPage, "records");

            // 获取套餐记录
            List<Setmeal> records = pageInfo.getRecords();

            // 批量查询分类信息
            Set<Long> categoryIds = records.stream()
                    .map(Setmeal::getCategoryId)
                    .collect(Collectors.toSet());

            Map<Long, String> categoryMap = categoryService.listByIds(categoryIds)
                    .stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));

            // 转换为 SetmealDto 列表
            List<SetmealDto> list = records.stream()
                    .map(item -> {
                        SetmealDto setmealDto = new SetmealDto();
                        // 对象拷贝
                        BeanUtils.copyProperties(item, setmealDto);
                        // 设置分类名称
                        setmealDto.setCategoryName(categoryMap.get(item.getCategoryId()));
                        return setmealDto;
                    })
                    .collect(Collectors.toList());

            // 设置分页结果
            dtoPage.setRecords(list);

            return R.success(dtoPage);
    }

    // 删除套餐
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("ids:{}",ids);

        setmealService.removeWithDish(ids);

        return R.success("套餐数据删除成功");
    }

    //根据条件查询套餐数据
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }
}
