package com.cz.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
//import com.cz.takeout.dto.SetmealDto;
import com.cz.takeout.dto.SetmealDto;
import com.cz.takeout.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    //新增套餐，同时需要保存套餐和菜品的关联关系
    void saveWithDish(SetmealDto setmealDto);

    //删除套餐，同时需要删除套餐和菜品的关联数据
    void removeWithDish(List<Long> ids);

    //回显套餐数据：根据套餐id查询套餐
    SetmealDto getData(Long id);

    //修改操作
    void updateWithDish(SetmealDto setmealDto);
}
