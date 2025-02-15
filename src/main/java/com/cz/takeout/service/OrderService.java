package com.cz.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cz.takeout.entity.Orders;

public interface OrderService extends IService<Orders> {

     //用户下单
    public void submit(Orders orders);
}
