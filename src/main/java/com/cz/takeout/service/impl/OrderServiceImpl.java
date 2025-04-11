package com.cz.takeout.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cz.takeout.common.BaseContext;
import com.cz.takeout.common.CustomException;
import com.cz.takeout.entity.*;
import com.cz.takeout.mapper.OrderMapper;
import com.cz.takeout.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    @Override
    public List<OrderDetail> getOrderDetailListByOrderId(Long orderId){
        LambdaQueryWrapper<OrderDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderDetail::getOrderId, orderId);
        //根据order表的条件查询出order_detail的数据，因为一个订单可能有多条菜品数据
        return orderDetailService.list(queryWrapper);
    }

    //查看订单明细
    @Override
    public Page<Orders> pageOrders(int page, int pageSize, String number, String beginTime, String endTime) {
        // 构建分页对象
        Page<Orders> pageInfo = new Page<>(page, pageSize);

        // 创建查询条件
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(number != null, Orders::getNumber, number)
                .gt(StringUtils.isNotEmpty(beginTime), Orders::getOrderTime, beginTime)
                .lt(StringUtils.isNotEmpty(endTime), Orders::getOrderTime, endTime);

        // 执行分页查询
        return baseMapper.selectPage(pageInfo, queryWrapper);
    }

    //用户下单
    @Transactional
    public void submit(Orders orders) {
        //获得当前用户id
        Long userId = BaseContext.getCurrentId();

        //查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId,userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        if(shoppingCarts == null || shoppingCarts.isEmpty()){
            throw new CustomException("购物车为空，不能下单");
        }

        //查询用户数据
        User user = userService.getById(userId);

        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if(addressBook == null){
            throw new CustomException("用户地址信息有误，不能下单");
        }

        // 初始化校验容器
        List<Long> dishIds = new ArrayList<>();    // 单品菜品ID集合
        List<Long> setmealIds = new ArrayList<>(); // 套餐ID集合
        Map<Long, List<Long>> setmealDishMap = new HashMap<>(); // 套餐-菜品关系
        Map<Long, Setmeal> setmealMap = new HashMap<>();// 套餐

        // 分离单品与套餐
        shoppingCarts.forEach(cart -> {
            if (cart.getDishId() != null) {
                dishIds.add(cart.getDishId()); // 收集单品菜品ID
            } else if (cart.getSetmealId() != null) {
                setmealIds.add(cart.getSetmealId()); // 收集套餐ID
            }
        });

        // 批量查询菜品状态（绕过缓存）
        Map<Long, Dish> dishMap = new HashMap<>(!dishIds.isEmpty() ?
                dishService.listByIds(dishIds).stream()
                        .collect(Collectors.toMap(Dish::getId, d -> d)) :
                Collections.emptyMap());

        // 批量查询套餐状态及关联菜品
        if (!setmealIds.isEmpty()) {
            List<Setmeal> setmeals = setmealService.listByIds(setmealIds);
            setmealMap = setmeals.stream()
                    .collect(Collectors.toMap(Setmeal::getId, s -> s));

            // 查询所有套餐关联的菜品ID
            List<SetmealDish> setmealDishes = setmealDishService.list(
                    new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getSetmealId, setmealIds)
            );

            // 构建套餐ID -> 菜品ID列表映射
            setmealDishes.forEach(sd ->
                    setmealDishMap.computeIfAbsent(sd.getSetmealId(), k -> new ArrayList<>())
                            .add(sd.getDishId())
            );

            // 收集所有关联菜品ID
            List<Long> relatedDishIds = setmealDishes.stream()
                    .map(SetmealDish::getDishId)
                    .collect(Collectors.toList());
            if (!relatedDishIds.isEmpty()) {
                dishMap.putAll(dishService.listByIds(relatedDishIds).stream()
                        .collect(Collectors.toMap(Dish::getId, d -> d)));
            }
        }

        // 多层级状态校验
        List<String> errorMessages = new ArrayList<>();
        Map<Long, Setmeal> finalSetmealMap = setmealMap;
        shoppingCarts.forEach(cart -> {
            if (cart.getDishId() != null) { // 单品校验
                Dish dish = dishMap.get(cart.getDishId());
                if (dish == null || dish.getStatus() != 1) {
                    errorMessages.add("单品「" + cart.getName() + "」已停售");
                }
            } else if (cart.getSetmealId() != null) { // 套餐校验
                Setmeal setmeal = finalSetmealMap.get(cart.getSetmealId());
                if (setmeal == null || setmeal.getStatus() != 1) {
                    errorMessages.add("套餐「" + cart.getName() + "」已停售");
                } else {
                    // 校验套餐内菜品状态
                    List<Long> relatedDishIds = setmealDishMap.get(setmeal.getId());
                    if (relatedDishIds != null) {
                        relatedDishIds.stream()
                                .filter(dishId -> !dishMap.containsKey(dishId) || dishMap.get(dishId).getStatus() != 1)
                                .findAny()
                                .ifPresent(dishId -> errorMessages.add("套餐「" + setmeal.getName() + "」包含停售菜品"));
                    }
                }
            }
        });

        if (!errorMessages.isEmpty()) {
            throw new CustomException(String.join("；", errorMessages));
        }

        //订单号
        long orderId = IdWorker.getId();

        AtomicInteger amount = new AtomicInteger(0);

        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());


        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        //向订单表插入数据，一条数据
        this.save(orders);

        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        //清空购物车数据
        shoppingCartService.remove(wrapper);
    }
}