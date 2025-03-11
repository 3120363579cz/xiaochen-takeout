package com.cz.takeout.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cz.takeout.common.BaseContext;
import com.cz.takeout.common.CustomException;
import com.cz.takeout.common.R;
import com.cz.takeout.entity.ShoppingCart;
import com.cz.takeout.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

//购物车
@Slf4j
@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    //添加购物车
    @PostMapping("/add")
    public R<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        //设置用户id，指定当前是哪个用户的购物车数据
        Long currentId = BaseContext.getCurrentId();
        shoppingCart.setUserId(currentId);

        Long dishId = shoppingCart.getDishId();

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,currentId);

        if(dishId != null){
            //添加到购物车的是菜品
            queryWrapper.eq(ShoppingCart::getDishId,dishId);

        }else{
            //添加到购物车的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }

        //查询当前菜品或者套餐是否在购物车中
        //SQL:select * from shopping_cart where user_id = ? and dish_id/setmeal_id = ?
        ShoppingCart cartServiceOne = shoppingCartService.getOne(queryWrapper);

        if(cartServiceOne != null){
            //如果已经存在，就在原来数量基础上加一
            Integer number = cartServiceOne.getNumber();
            cartServiceOne.setNumber(number + 1);
            shoppingCartService.updateById(cartServiceOne);
        }else{
            //如果不存在，则添加到购物车，数量默认就是一
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            cartServiceOne = shoppingCart;
        }

        return R.success(cartServiceOne);
    }

    //数值删减
    @PostMapping("/sub")
    public R<ShoppingCart> reduceCartItem(@RequestBody ShoppingCart cart) {
        Long userId = BaseContext.getCurrentId();

        // 构建动态查询条件
        LambdaQueryWrapper<ShoppingCart> queryWrapper = Wrappers.lambdaQuery(ShoppingCart.class)
                .eq(ShoppingCart::getUserId, userId)
                .eq(cart.getDishId() != null, ShoppingCart::getDishId, cart.getDishId())
                .eq(cart.getSetmealId() != null, ShoppingCart::getSetmealId, cart.getSetmealId());

        ShoppingCart item = shoppingCartService.getOne(queryWrapper);
        if (item == null) {
            throw new CustomException("购物车条目不存在");
        }

        int currentQuantity = Optional.ofNullable(item.getNumber()).orElse(0);
        if (currentQuantity <= 0) {
            throw new CustomException("商品数量不可为负值");
        }

        if (currentQuantity == 1) {
            shoppingCartService.removeById(item.getId());
            item.setNumber(0); // 保持响应数据一致性
        } else {
            item.setNumber(currentQuantity - 1);
            shoppingCartService.updateById(item);
        }

        return R.success(item);
    }

    //查看购物车
    @GetMapping("/list")
    public R<List<ShoppingCart>> list(){
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());
        queryWrapper.orderByAsc(ShoppingCart::getCreateTime);

        List<ShoppingCart> list = shoppingCartService.list(queryWrapper);

        return R.success(list);
    }

    // 清空购物车
    @DeleteMapping("/clean")
    public R<String> clean(){
        //SQL:delete from shopping_cart where user_id = ?

        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId,BaseContext.getCurrentId());

        shoppingCartService.remove(queryWrapper);

        return R.success("清空购物车成功");
    }
}