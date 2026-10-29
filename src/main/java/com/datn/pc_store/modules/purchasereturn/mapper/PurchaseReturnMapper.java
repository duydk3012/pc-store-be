package com.datn.pc_store.modules.purchasereturn.mapper;

import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnItemResponse;
import com.datn.pc_store.modules.purchasereturn.dto.PurchaseReturnResponse;
import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturn;
import com.datn.pc_store.modules.purchasereturn.entity.PurchaseReturnItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PurchaseReturnMapper {

    @Mapping(target = "totalItems", expression = "java(pr.getItems() != null ? pr.getItems().size() : 0)")
    PurchaseReturnResponse toResponse(PurchaseReturn pr);

    @Mapping(target = "purchaseReturnId", source = "purchaseReturn.id")
    @Mapping(target = "productSerialId", source = "productSerial.id")
    @Mapping(target = "serialNumber", source = "productSerial.serialNumber")
    @Mapping(target = "productId", source = "productSerial.productId")
    PurchaseReturnItemResponse toItemResponse(PurchaseReturnItem item);

    List<PurchaseReturnItemResponse> toItemResponseList(List<PurchaseReturnItem> items);
}
