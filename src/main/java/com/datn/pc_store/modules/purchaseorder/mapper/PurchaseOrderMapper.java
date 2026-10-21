package com.datn.pc_store.modules.purchaseorder.mapper;

import com.datn.pc_store.modules.purchaseorder.dto.PurchaseOrderItemResponse;
import com.datn.pc_store.modules.purchaseorder.dto.PurchaseOrderResponse;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrder;
import com.datn.pc_store.modules.purchaseorder.entity.PurchaseOrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PurchaseOrderMapper {
    PurchaseOrderResponse toResponse(PurchaseOrder order);

    PurchaseOrderItemResponse toItemResponse(PurchaseOrderItem item);
}
