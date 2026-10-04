package com.datn.pc_store.product.entity;

import com.datn.pc_store.product.enums.ProductStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ProductStatusConverter implements AttributeConverter<ProductStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ProductStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public ProductStatus convertToEntityAttribute(Integer code) {
        return code == null ? null : ProductStatus.fromCode(code);
    }
}
