package com.datn.pc_store.modules.payment.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMethodTypeConverter implements AttributeConverter<PaymentMethodType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(PaymentMethodType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PaymentMethodType convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : PaymentMethodType.fromValue(dbData);
    }
}
