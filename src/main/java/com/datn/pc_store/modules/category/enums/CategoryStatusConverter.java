package com.datn.pc_store.modules.category.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CategoryStatusConverter implements AttributeConverter<CategoryStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(CategoryStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public CategoryStatus convertToEntityAttribute(Integer dbData) {
        return dbData != null ? CategoryStatus.fromValue(dbData) : null;
    }
}
