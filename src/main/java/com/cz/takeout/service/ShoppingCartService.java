package com.cz.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cz.takeout.entity.ShoppingCart;

public interface ShoppingCartService extends IService<ShoppingCart> {
    //通过用户id把原来的购物车给清空
    void clean();

}
