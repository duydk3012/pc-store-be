package com.datn.pc_store.modules.goodsreceipt.mapper;

import com.datn.pc_store.modules.goodsreceipt.dto.GoodsReceiptResponse;
import com.datn.pc_store.modules.goodsreceipt.dto.ProductSerialResponse;
import com.datn.pc_store.modules.goodsreceipt.dto.ReceiptItemResponse;
import com.datn.pc_store.modules.goodsreceipt.entity.GoodsReceipt;
import com.datn.pc_store.modules.goodsreceipt.entity.ProductSerial;
import com.datn.pc_store.modules.goodsreceipt.entity.ReceiptItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GoodsReceiptMapper {
    GoodsReceiptResponse toResponse(GoodsReceipt receipt);

    @Mapping(target = "goodsReceiptId", source = "goodsReceipt.id")
    ReceiptItemResponse toItemResponse(ReceiptItem item);

    @Mapping(target = "receiptItemId", source = "receiptItem.id")
    @Mapping(target = "isTerminated", expression = "java(serial.isTerminated())")
    ProductSerialResponse toSerialResponse(ProductSerial serial);
}
