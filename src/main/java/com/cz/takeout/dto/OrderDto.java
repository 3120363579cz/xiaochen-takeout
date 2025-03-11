package com.cz.takeout.dto;

import com.cz.takeout.entity.OrderDetail;
import com.cz.takeout.entity.Orders;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto extends Orders {
    private List<OrderDetail> orderDetails;
}

