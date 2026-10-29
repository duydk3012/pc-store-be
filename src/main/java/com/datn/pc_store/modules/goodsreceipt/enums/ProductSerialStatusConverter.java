package com.datn.pc_store.modules.goodsreceipt.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProductSerialStatusConverter implements AttributeConverter<ProductSerialStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ProductSerialStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ProductSerialStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : ProductSerialStatus.fromValue(dbData);
    }
}
