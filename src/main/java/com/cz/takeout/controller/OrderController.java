package com.cz.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cz.takeout.common.BaseContext;
import com.cz.takeout.common.R;
import com.cz.takeout.dto.OrderDto;
import com.cz.takeout.entity.OrderDetail;
import com.cz.takeout.entity.Orders;
import com.cz.takeout.entity.ShoppingCart;
import com.cz.takeout.service.OrderDetailService;
import com.cz.takeout.service.OrderService;
import com.cz.takeout.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//订单
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private ShoppingCartService shoppingCartService;

    //用户下单
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        orderService.submit(orders);
        return R.success("下单成功");
    }

    //客户端点击再来一单
    @PostMapping("/again")
    public R<String> againSubmit(@RequestBody Map<String, String> map) {
        // 获得ID
        String ids = map.get("id");

        long id = Long.parseLong(ids);

        // 制作判断条件
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, id);

        //获取该订单对应的所有的订单明细表
        List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper);

        //通过用户id把原来的购物车给清空
        shoppingCartService.clean();

        //获取用户id
        Long userId = BaseContext.getCurrentId();

        // 整体赋值
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map((item) -> {

            // 以下均为赋值操作

            ShoppingCart shoppingCart = new ShoppingCart();
            shoppingCart.setUserId(userId);
            shoppingCart.setImage(item.getImage());

            Long dishId = item.getDishId();
            Long setmealId = item.getSetmealId();

            if (dishId != null) {
                // 如果是菜品那就添加菜品的查询条件
                shoppingCart.setDishId(dishId);
            } else {
                // 添加到购物车的是套餐
                shoppingCart.setSetmealId(setmealId);
            }
            shoppingCart.setName(item.getName());
            shoppingCart.setDishFlavor(item.getDishFlavor());
            shoppingCart.setNumber(item.getNumber());
            shoppingCart.setAmount(item.getAmount());
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());

        // 将携带数据的购物车批量插入购物车表
        shoppingCartService.saveBatch(shoppingCartList);

        return R.success("操作成功");
    }


    //用户支付后查看订单
    @GetMapping("/userPage")
    public R<Page> page(int page, int pageSize) {
        //分页构造器对象
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        Page<OrderDto> pageDto = new Page<>(page, pageSize);
        //构造条件查询对象
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        //这里是直接把当前用户分页的全部结果查询出来，要添加用户id作为查询条件，否则会出现用户可以查询到其他用户的订单情况
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //这里是把所有的订单分页查询出来
        orderService.page(pageInfo, queryWrapper);

        //对OrderDto进行属性赋值
        List<Orders> records = pageInfo.getRecords();
        List<OrderDto> orderDtoList = records.stream().map((item) -> {//item其实就是分页查询出来的每个订单对象
            OrderDto orderDto = new OrderDto();
            //此时的orderDto对象里面orderDetails属性还是空 下面准备为它赋值
            Long orderId = item.getId();//获取订单id
            //调用根据订单id条件查询订单明细数据的方法，把查询出来订单明细数据存入orderDetailList
            List<OrderDetail> orderDetailList = orderService.getOrderDetailListByOrderId(orderId);

            BeanUtils.copyProperties(item, orderDto);//把订单对象的数据复制到orderDto中
            //对orderDto进行OrderDetails属性的赋值
            orderDto.setOrderDetails(orderDetailList);
            return orderDto;
        }).collect(Collectors.toList());

        //将订单分页查询的订单数据以外的内容复制到pageDto中，不清楚可以对着图看
        BeanUtils.copyProperties(pageInfo, pageDto, "records");
        pageDto.setRecords(orderDtoList);
        return R.success(pageDto);
    }


    //后台查询订单明细
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {
        return R.success(orderService.pageOrders(page, pageSize, number, beginTime, endTime));
    }

    //修改订单状态
    @PutMapping
    public R<String> orderStatusChange(@RequestBody Map<String, String> map) {
        Long orderId = Long.parseLong(map.get("id"));//将接收到的id转为Long型
        Integer status = Integer.parseInt(map.get("status"));//转为Integer型

        if (orderId == null || status == null) {
            return R.error("传入信息非法");
        }
        Orders orders = orderService.getById(orderId);//根据订单id查询订单数据
        orders.setStatus(status);//修改订单对象里的数据
        orderService.updateById(orders);

        return R.success("订单状态修改成功");

    }
    //查看手机版历史纪录

}