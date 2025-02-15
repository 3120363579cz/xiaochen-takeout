package com.cz.takeout.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cz.takeout.entity.Category;

public interface CategoryService extends IService<Category> {

    public void remove(Long id);

}
