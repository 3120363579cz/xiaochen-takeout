package com.cz.takeout.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cz.takeout.entity.OrderDetail;
import com.cz.takeout.entity.Orders;

import java.util.List;

public interface OrderService extends IService<Orders> {

    //用户下单
    void submit(Orders orders);

    //查看订单明细
    Page<Orders> pageOrders(int page, int pageSize, String number, String beginTime, String endTime);

    //根据订单id来查询订单明细的数据
    List<OrderDetail> getOrderDetailListByOrderId(Long orderId);

}
