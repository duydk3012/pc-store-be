package com.datn.pc_store.modules.suppliers.mapper;

import com.datn.pc_store.modules.suppliers.dto.SupplierCreateRequest;
import com.datn.pc_store.modules.suppliers.dto.SupplierResponse;
import com.datn.pc_store.modules.suppliers.dto.SupplierUpdateRequest;
import com.datn.pc_store.modules.suppliers.entity.Supplier;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SupplierMapper {

    @Mapping(target = "id", ignore = true)
    Supplier toEntity(SupplierCreateRequest req);

    SupplierResponse toResponse(Supplier entity);

    List<SupplierResponse> toResponseList(List<Supplier> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(SupplierUpdateRequest req, @MappingTarget Supplier entity);
}
