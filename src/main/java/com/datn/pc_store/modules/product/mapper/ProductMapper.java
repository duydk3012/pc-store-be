package com.datn.pc_store.product.mapper;

import com.datn.pc_store.product.dto.ProductRequest;
import com.datn.pc_store.product.dto.ProductResponse;
import com.datn.pc_store.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "sku", expression = "java(request.sku().trim())")
    @Mapping(target = "unit", expression = "java(request.unit().trim())")
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "sku", expression = "java(request.sku().trim())")
    @Mapping(target = "unit", expression = "java(request.unit().trim())")
    @Mapping(
            target = "status",
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(ProductRequest request, @MappingTarget Product product);

    ProductResponse toResponse(Product product);
}
