package com.datn.pc_store.modules.brand.mapper;

import com.datn.pc_store.modules.brand.dto.BrandCreateRequest;
import com.datn.pc_store.modules.brand.dto.BrandResponse;
import com.datn.pc_store.modules.brand.dto.BrandUpdateRequest;
import com.datn.pc_store.modules.brand.entity.Brand;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BrandMapper {

    @Mapping(target = "id", ignore = true)
    Brand toEntity(BrandCreateRequest req);

    BrandResponse toResponse(Brand entity);

    List<BrandResponse> toResponseList(List<Brand> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(BrandUpdateRequest req, @MappingTarget Brand entity);
}
