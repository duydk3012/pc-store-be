package com.datn.pc_store.modules.productprice.mapper;

import com.datn.pc_store.modules.productprice.dto.ProductPriceResponse;
import com.datn.pc_store.modules.productprice.entity.ProductPrice;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProductPriceMapper {
    ProductPriceResponse toResponse(ProductPrice price);
}
