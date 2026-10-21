package com.datn.pc_store.modules.product.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductTypeConverter implements AttributeConverter<ProductType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ProductType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ProductType convertToEntityAttribute(Integer value) {
        return value == null ? null : ProductType.fromValue(value);
    }
}
