package com.cz.takeout.dto;

import com.cz.takeout.entity.Setmeal;
import com.cz.takeout.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
