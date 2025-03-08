package com.cz.takeout.controller;

import com.cz.takeout.common.R;
import com.cz.takeout.entity.Orders;
import com.cz.takeout.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//订单
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    //用户下单
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        orderService.submit(orders);
        return R.success("下单成功");
    }

    //修改订单状态
    @PutMapping
    public R<String> orderStatusChange(@RequestBody Map<String, String> map) {
        Long  orderId = Long.parseLong(map.get("id"));
        Integer status = Integer.parseInt(map.get("status"));
        if (orderId == null || status == null) {
            return R.error("非法信息传入");
        }

        Orders orders = orderService.getById(orderId);
        orders.setStatus(status);
        orderService.updateById(orders);

        return R.success("修改成功");
    }
}