package com.company.helpdesk.service;

import com.company.helpdesk.config.RedisCacheConfig;
import com.company.helpdesk.dto.CategoryDtos;
import com.company.helpdesk.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = RedisCacheConfig.CACHE_CATEGORIES, key = "'all'")
    @Transactional(readOnly = true)
    public CategoryDtos.CategoryListResponse findAll() {
        List<CategoryDtos.CategoryItem> items = categoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(c -> new CategoryDtos.CategoryItem(c.getId(), c.getNamaKategori()))
                .toList();
        return new CategoryDtos.CategoryListResponse(items);
    }
}