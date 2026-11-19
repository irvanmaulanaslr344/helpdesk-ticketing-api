package com.company.helpdesk.controller;

import com.company.helpdesk.dto.CategoryDtos;
import com.company.helpdesk.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Tag(name = "2. Categories", description = "Daftar kategori tiket (Software & Hardware)")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Mengambil daftar kategori tiket (Redis + HTTP Cache)")
    @GetMapping
    public ResponseEntity<CategoryDtos.CategoryListResponse> getCategories() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(categoryService.findAll());
    }
}