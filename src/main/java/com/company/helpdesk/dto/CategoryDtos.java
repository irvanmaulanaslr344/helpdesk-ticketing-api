package com.company.helpdesk.dto;

import java.util.List;

public final class CategoryDtos {

    private CategoryDtos() {}

    public record CategoryItem(
            Long id,
            String namaKategori
    ) {}

    public record CategoryListResponse(
            List<CategoryItem> data
    ) {}
}