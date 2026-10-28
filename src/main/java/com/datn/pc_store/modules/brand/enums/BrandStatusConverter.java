package com.datn.pc_store.modules.brand.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BrandStatusConverter implements AttributeConverter<BrandStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(BrandStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public BrandStatus convertToEntityAttribute(Integer dbData) {
        return dbData != null ? BrandStatus.fromValue(dbData) : null;
    }
}
