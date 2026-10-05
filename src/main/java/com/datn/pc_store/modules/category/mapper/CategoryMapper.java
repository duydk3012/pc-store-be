package com.datn.pc_store.category.mapper;

import com.datn.pc_store.category.dto.CategoryRequest;
import com.datn.pc_store.category.dto.CategoryResponse;
import com.datn.pc_store.category.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "slug", expression = "java(request.slug().trim())")
    Category toEntity(CategoryRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "slug", expression = "java(request.slug().trim())")
    void updateEntity(CategoryRequest request, @MappingTarget Category category);

    CategoryResponse toResponse(Category category);
}
