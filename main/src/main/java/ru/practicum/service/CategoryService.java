package ru.practicum.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {

    CategoryDto createCategory(NewCategoryDto newCategoryDto);

    CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto);

    void deleteCategory(Long categoryId);

    List<CategoryDto> getCategories(@Min(0) Integer from, @Positive Integer size);

    CategoryDto getCategoryById(Long categoryId);
}