package com.datn.pc_store.modules.user.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UserStatusConverter implements AttributeConverter<UserStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserStatus attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public UserStatus convertToEntityAttribute(Integer dbData) {
        return dbData != null ? UserStatus.fromValue(dbData) : null;
    }
}
