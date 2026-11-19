package com.datn.pc_store.modules.recommendation.controller;

import com.datn.pc_store.modules.recommendation.dto.request.SyncMiningPayloadRequest;
import com.datn.pc_store.modules.recommendation.dto.response.*;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import com.datn.pc_store.modules.recommendation.service.DataMiningSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/mining")
@RequiredArgsConstructor
@Tag(name = "Admin - Data Mining & Recommendation Engine", description = "Quản lý các mô hình khai phá dữ liệu, luật kết hợp, phân cụm khách hàng, quản lý phiên bản và đồng bộ kết quả")
@PreAuthorize("hasRole('ADMIN')")
public class RecommendationAdminController {

    private final DataMiningSyncService miningSyncService;

    @GetMapping("/models/latest")
    @Operation(summary = "Xem phiên bản mô hình khai phá dữ liệu mới nhất",
            description = "Lấy mô hình đang Active theo modelType (ASSOCIATION_RULES, CUSTOMER_CLUSTERING, CONTENT_BASED) hoặc danh sách toàn bộ lịch sử chạy nếu không truyền param.")
    public ResponseEntity<?> getLatestModels(@RequestParam(required = false) ModelType modelType) {
        if (modelType != null) {
            MiningModelRunDto activeModel = miningSyncService.getLatestActiveModel(modelType);
            return ResponseEntity.ok(activeModel);
        }
        List<MiningModelRunDto> allRuns = miningSyncService.getAllModelRuns();
        return ResponseEntity.ok(allRuns);
    }

    @GetMapping("/models/history")
    @Operation(summary = "Lịch sử các phiên bản mô hình (Model Registry History)",
            description = "Xem danh sách phân trang các phiên bản đã từng huấn luyện, hỗ trợ lọc theo modelType để theo dõi tiến trình cập nhật mô hình.")
    public ResponseEntity<Page<ModelVersionSummaryDto>> getModelHistory(
            @RequestParam(required = false) ModelType modelType,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(miningSyncService.getModelHistory(modelType, pageable));
    }

    @GetMapping("/models/{id}")
    @Operation(summary = "Xem chi tiết một phiên bản mô hình",
            description = "Xem toàn bộ tham số cấu hình (parameters_json), độ đo đánh giá (metrics_json) và ghi chú của một lần chạy.")
    public ResponseEntity<ModelVersionSummaryDto> getModelDetails(@PathVariable("id") Long id) {
        return ResponseEntity.ok(miningSyncService.getModelDetails(id));
    }

    @PostMapping("/models/{id}/activate")
    @Operation(summary = "Kích hoạt lại phiên bản cũ (Rollback / Model Switcher)",
            description = "Chuyển giao quyền Active (is_active=1) cho phiên bản được chọn và vô hiệu hóa phiên bản hiện tại trong 1 Transaction an toàn. Áp dụng tức thì cho hệ thống Serving người dùng.")
    public ResponseEntity<ModelVersionSummaryDto> activateModelVersion(@PathVariable("id") Long id) {
        return ResponseEntity.ok(miningSyncService.activateModelVersion(id));
    }

    @GetMapping("/models/compare")
    @Operation(summary = "So sánh 2 phiên bản mô hình & Phân tích chuyển dịch bán hàng (Drift Analysis)",
            description = "Đối chiếu 2 phiên bản để xác định: chuyển dịch độ đo toán học, chuyển dịch tỷ trọng phân khúc khách hàng, và các luật kết hợp mới nổi (Emerging Rules) so với luật cũ (Dropped Rules).")
    public ResponseEntity<ModelComparisonResponse> compareModels(
            @RequestParam("sourceId") Long sourceId,
            @RequestParam("targetId") Long targetId) {
        return ResponseEntity.ok(miningSyncService.compareModelVersions(sourceId, targetId));
    }

    @GetMapping("/clusters")
    @Operation(summary = "Thống kê phân cụm khách hàng hiện hành",
            description = "Thống kê số lượng và tỷ lệ % khách hàng phân bổ trong từng Cụm (Cluster) từ mô hình K-Means đang Active.")
    public ResponseEntity<List<CustomerClusterDto>> getClusterStats() {
        return ResponseEntity.ok(miningSyncService.getClusterSummaries());
    }

    @GetMapping("/rules")
    @Operation(summary = "Danh sách luật kết hợp (Association Rules) đang kích hoạt",
            description = "Xem danh sách luật sinh từ thuật toán FP-Growth, hỗ trợ phân trang và lọc theo ngưỡng minLift, minConfidence.")
    public ResponseEntity<Page<AssociationRuleDto>> getActiveRules(
            @RequestParam(required = false) Double minLift,
            @RequestParam(required = false) Double minConfidence,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(miningSyncService.getActiveAssociationRules(minLift, minConfidence, pageable));
    }

    @PostMapping("/sync")
    @Operation(summary = "Đồng bộ kết quả khai phá dữ liệu từ Offline Pipeline (Python)",
            description = "Nạp luật kết hợp, trọng số cụm khách hàng hoặc ma trận tương đồng sản phẩm vào CSDL. Tự động chuyển giao Active model trong 1 transaction an toàn.")
    public ResponseEntity<MiningModelRunDto> syncModelData(@Valid @RequestBody SyncMiningPayloadRequest request) {
        return ResponseEntity.ok(miningSyncService.syncModelData(request));
    }
}
