package com.datn.pc_store.modules.category.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CategoryStatusConverter implements AttributeConverter<CategoryStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(CategoryStatus attribute) {
        return attribute == null ? CategoryStatus.ACTIVE.getValue() : attribute.getValue();
    }

    @Override
    public CategoryStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? CategoryStatus.ACTIVE : CategoryStatus.fromValue(dbData);
    }
}
