package com.datn.pc_store.modules.product.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductStatusConverter implements AttributeConverter<ProductStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ProductStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ProductStatus convertToEntityAttribute(Integer value) {
        return value == null ? null : ProductStatus.fromValue(value);
    }
}
