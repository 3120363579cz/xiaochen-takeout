package com.cz.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    //后台查询订单明细
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, Long number, String beginTime, String endTime) {
        //分页构造器对象
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        //构造条件查询对象
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //链式编程写查询条件
        queryWrapper.like(number!=null,Orders::getNumber,number)
                //前面加上判定条件是十分必要的，用户没有填写该数据，查询条件上就不添加它
                .gt(StringUtils.isNotBlank(beginTime),Orders::getOrderTime,beginTime)//大于起始时间
                .lt(StringUtils.isNotBlank(endTime),Orders::getOrderTime,endTime);//小于结束时间
        orderService.page(pageInfo,queryWrapper);
        return R.success(pageInfo);
    }

    //修改订单状态
    @PutMapping
    public R<String> orderStatusChange(@RequestBody Map<String,String> map){
        Long orderId = Long.parseLong(map.get("id"));//将接收到的id转为Long型
        Integer status = Integer.parseInt(map.get("status"));//转为Integer型

        if(orderId == null || status==null){
            return R.error("传入信息非法");
        }
        Orders orders = orderService.getById(orderId);//根据订单id查询订单数据
        orders.setStatus(status);//修改订单对象里的数据
        orderService.updateById(orders);

        return R.success("订单状态修改成功");

    }
}