package com.datn.pc_store.modules.product.mapper;

import com.datn.pc_store.modules.product.dto.*;
import com.datn.pc_store.modules.product.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {
    Product toEntity(ProductCreateRequest req);

    ProductResponse toResponse(Product entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productType", ignore = true)
    void updateEntityFromDto(ProductUpdateRequest req, @MappingTarget Product entity);
}
