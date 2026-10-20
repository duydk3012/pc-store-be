package com.datn.pc_store.modules.category.mapper;

import com.datn.pc_store.modules.category.dto.CategoryCreateRequest;
import com.datn.pc_store.modules.category.dto.CategoryResponse;
import com.datn.pc_store.modules.category.dto.CategoryTreeResponse;
import com.datn.pc_store.modules.category.dto.CategoryUpdateRequest;
import com.datn.pc_store.modules.category.entity.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    Category toEntity(CategoryCreateRequest req);

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryResponse toResponse(Category entity);

    List<CategoryResponse> toResponseList(List<Category> entities);

    @Mapping(target = "parentId", source = "parent.id")
    CategoryTreeResponse toTreeResponse(Category entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromDto(CategoryUpdateRequest req, @MappingTarget Category entity);
}
