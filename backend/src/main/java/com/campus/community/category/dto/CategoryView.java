package com.campus.community.category.dto;

import com.campus.community.category.entity.Category;

public record CategoryView(Long id, String code, String name, String description, String color) {
    public static CategoryView from(Category category) {
        return new CategoryView(category.getId(), category.getCode(), category.getName(),
                category.getDescription(), category.getColor());
    }
}

