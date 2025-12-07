package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto createCategory(NewCategoryDto newCategoryDto) {
        log.info("Creating category with name: {}", newCategoryDto.getName());

        if (categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с именем=" + newCategoryDto.getName() + " уже существует");
        }

        Category category = categoryMapper.toEntity(newCategoryDto);
        Category savedCategory = categoryRepository.save(category);

        log.info("Category created with id: {}", savedCategory.getId());
        return categoryMapper.toDto(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto) {
        log.info("Updating category with id: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория", categoryId));

        if (!category.getName().equals(newCategoryDto.getName()) &&
                categoryRepository.existsByName(newCategoryDto.getName())) {
            throw new ConflictException("Категория с именем=" + newCategoryDto.getName() + " уже существует");
        }

        category.setName(newCategoryDto.getName());
        Category updatedCategory = categoryRepository.save(category);

        log.info("Category with id: {} updated", categoryId);
        return categoryMapper.toDto(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("Deleting category with id: {}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundException("Категория", categoryId);
        }

        long eventsCount = eventRepository.countByCategoryId(categoryId);
        if (eventsCount > 0) {
            throw new ConflictException("Невозможно удалить категорию с привязанными событиями");
        }

        categoryRepository.deleteById(categoryId);
        log.info("Category with id: {} deleted", categoryId);
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        log.info("Getting categories from: {}, size: {}", from, size);

        Pageable pageable = PageRequest.of(from / size, size);

        return categoryRepository.findAll(pageable).getContent()
                .stream()
                .map(categoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryDto getCategoryById(Long categoryId) {
        log.info("Getting category by id: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория", categoryId));

        return categoryMapper.toDto(category);
    }
}