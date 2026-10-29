package com.datn.pc_store.modules.goodsreceipt.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GoodsReceiptStatusConverter implements AttributeConverter<GoodsReceiptStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(GoodsReceiptStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public GoodsReceiptStatus convertToEntityAttribute(Integer dbData) {
        return dbData == null ? null : GoodsReceiptStatus.fromValue(dbData);
    }
}
