package com.cz.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cz.takeout.common.R;
import com.cz.takeout.dto.DishDto;
import com.cz.takeout.entity.Category;
import com.cz.takeout.entity.Dish;
import com.cz.takeout.entity.DishFlavor;
import com.cz.takeout.service.CategoryService;
import com.cz.takeout.service.DishFlavorService;
import com.cz.takeout.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//菜品管理
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    //新增菜品
    @PostMapping
    @CacheEvict(value = "dishCache", allEntries = true)
    public R<String> save(@RequestBody DishDto dishDto) {

        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }

    //批量删除
    @DeleteMapping
    public R<String> deleteByIds(Long[] ids) {
        dishService.removeByIds(Arrays.asList(ids));
        return R.success("删除成功");
    }

    //根据id查询菜品信息和对应的口味信息
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    //修改菜品
    @PutMapping
    @CacheEvict(value = "dishCache", allEntries = true)
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        return R.success("修改菜品成功");
    }

    //启售，批量启售
    @PostMapping("/status/1")
    public R<String> openStatus(Long[] ids) {
        // 构建UpdateWrapper批量更新
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId, ids)
                .set(Dish::getStatus, 1);
        dishService.update(updateWrapper);

        return R.success("修改成功");
    }

    //停售，批量停售
    @PostMapping("/status/0")
    public R<String> closeStatus(Long[] ids){
        // 构建UpdateWrapper批量更新
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Dish::getId, ids)
                .set(Dish::getStatus, 0);
        dishService.update(updateWrapper);

        return R.success("修改成功");
    }

    //菜品信息分页查询
    @GetMapping("/page")
    public R<Page<DishDto>> page(
            @RequestParam int page,
            @RequestParam int pageSize,
            @RequestParam(required = false) String name
    ) {
        // 构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        // 条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件（name 不为空时）
        queryWrapper.like(StringUtils.isNotEmpty(name), Dish::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        // 执行分页查询
        dishService.page(pageInfo, queryWrapper);

        // 对象拷贝（排除 records）
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        // 获取菜品记录
        List<Dish> records = pageInfo.getRecords();

        // 批量查询分类信息
        Set<Long> categoryIds = records.stream()
                .map(Dish::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, String> categoryMap = categoryService.listByIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // 转换为 DishDto 列表
        List<DishDto> list = records.stream()
                .map(item -> {
                    DishDto dishDto = new DishDto();
                    BeanUtils.copyProperties(item, dishDto);

                    // 设置分类名称
                    dishDto.setCategoryName(categoryMap.get(item.getCategoryId()));
                    return dishDto;
                })
                .collect(Collectors.toList());

        // 设置分页结果
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    // 根据条件查询对应的菜品数据
    @GetMapping("/list")
    @Cacheable(value = "dishCache", key = "#dish.categoryId + '_' + #dish.status")
    public R<List<DishDto>> list(Dish dish) {
        // 构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        // 添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus, 1);

        // 添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        // 批量查询分类信息
        Set<Long> categoryIds = list.stream()
                .map(Dish::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, String> categoryMap = categoryService.listByIds(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        // 批量查询口味信息
        List<Long> dishIds = list.stream()
                .map(Dish::getId)
                .collect(Collectors.toList());

        Map<Long, List<DishFlavor>> dishFlavorMap = dishFlavorService.list(new LambdaQueryWrapper<DishFlavor>()
                        .in(DishFlavor::getDishId, dishIds))
                .stream()
                .collect(Collectors.groupingBy(DishFlavor::getDishId));

        List<DishDto> dishDtoList = list.stream().map(item -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            // 设置分类名称
            dishDto.setCategoryName(categoryMap.get(item.getCategoryId()));

            // 设置口味信息
            dishDto.setFlavors(dishFlavorMap.get(item.getId()));
            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }
}
