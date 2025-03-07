package com.cz.takeout.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cz.takeout.entity.Orders;

public interface OrderService extends IService<Orders> {

     //用户下单
     void submit(Orders orders);

     //查看订单明细
     Page<Orders> pageOrders(int page, int pageSize, String number, String beginTime, String endTime);
}
