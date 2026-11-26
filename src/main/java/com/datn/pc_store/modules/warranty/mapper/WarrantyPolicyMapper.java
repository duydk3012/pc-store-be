package com.datn.pc_store.modules.warranty.mapper;

import com.datn.pc_store.modules.warranty.dto.WarrantyPolicyCreateRequest;
import com.datn.pc_store.modules.warranty.dto.WarrantyPolicyResponse;
import com.datn.pc_store.modules.warranty.dto.WarrantyPolicyUpdateRequest;
import com.datn.pc_store.modules.warranty.entity.WarrantyPolicy;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WarrantyPolicyMapper {

    @Mapping(target = "id", ignore = true)
    WarrantyPolicy toEntity(WarrantyPolicyCreateRequest req);

    WarrantyPolicyResponse toResponse(WarrantyPolicy entity);

    List<WarrantyPolicyResponse> toResponseList(List<WarrantyPolicy> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(WarrantyPolicyUpdateRequest req, @MappingTarget WarrantyPolicy entity);
}
