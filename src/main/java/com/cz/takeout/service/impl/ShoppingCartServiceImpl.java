package com.cz.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cz.takeout.common.BaseContext;
import com.cz.takeout.common.R;
import com.cz.takeout.entity.ShoppingCart;
import com.cz.takeout.mapper.ShoppingCartMapper;
import com.cz.takeout.service.ShoppingCartService;
import org.springframework.stereotype.Service;

@Service
public class ShoppingCartServiceImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {
    @Override
    public void clean() {
        // 进行用户比对
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());

        // 删除即可
        this.remove(queryWrapper);

        R.success("清空成功");

    }
}
