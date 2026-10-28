package com.smartstock.service;

import com.smartstock.exception.BusinessRuleException;
import com.smartstock.exception.ResourceNotFoundException;
import com.smartstock.model.Category;
import com.smartstock.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with ID " + id + " not found."));
    }

    @Transactional
    public Long createCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleException("Category name cannot be blank.");
        }
        if (categoryRepository.findByName(name.trim()).isPresent()) {
            throw new BusinessRuleException("Category with name '" + name.trim() + "' already exists.");
        }
        return categoryRepository.save(name.trim(), description);
    }

    @Transactional
    public void updateCategory(Long id, String name, String description) {
        getCategoryById(id);
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessRuleException("Category name cannot be blank.");
        }
        categoryRepository.findByName(name.trim()).ifPresent(existing -> {
            if (!existing.getCategoryId().equals(id)) {
                throw new BusinessRuleException("Another category with name '" + name.trim() + "' already exists.");
            }
        });
        categoryRepository.update(id, name.trim(), description);
    }

    @Transactional
    public void deleteCategory(Long id) {
        getCategoryById(id);
        categoryRepository.delete(id);
    }
}
