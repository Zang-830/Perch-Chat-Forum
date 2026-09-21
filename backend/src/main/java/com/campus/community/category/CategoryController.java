package com.campus.community.category;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.community.category.dto.CategoryView;
import com.campus.community.category.entity.Category;
import com.campus.community.category.mapper.CategoryMapper;
import com.campus.community.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    @GetMapping
    public ApiResponse<List<CategoryView>> list() {
        List<CategoryView> categories = categoryMapper.selectList(
                        Wrappers.<Category>lambdaQuery()
                                .eq(Category::getEnabled, true)
                                .orderByAsc(Category::getSortOrder))
                .stream().map(CategoryView::from).toList();
        return ApiResponse.success(categories);
    }
}

