package com.cz.takeout.dto;

import com.cz.takeout.entity.OrderDetail;
import com.cz.takeout.entity.Orders;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderDto extends Orders {
    private List<OrderDetail> orderDetails;
}

