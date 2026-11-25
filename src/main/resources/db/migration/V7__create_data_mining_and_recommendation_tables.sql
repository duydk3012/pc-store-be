-- =============================================================================
-- V7: TẠO CÁC BẢNG QUẢN LÝ KHAI PHÁ DỮ LIỆU & HỆ THỐNG GỢI Ý (RECOMMENDATION)
-- PHIÊN BẢN V2.2: RÀNG BUỘC DUY NHẤT, NGỮ CẢNH PHIÊN BẢN & LUẬT ĐA MẶT HÀNG
-- =============================================================================

-- 1. Bảng lưu trữ phiên bản và nhật ký các lần chạy mô hình (Model Runs)
CREATE TABLE IF NOT EXISTS `mining_model_runs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `model_type` VARCHAR(50) NOT NULL COMMENT 'ASSOCIATION_RULES, CUSTOMER_CLUSTERING, CONTENT_BASED, COLLABORATIVE, HYBRID',
    `algorithm_name` VARCHAR(100) NOT NULL COMMENT 'FP-Growth, K-Means, Cosine Similarity, etc.',
    `run_status` VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING, SUCCESS, FAILED',
    `is_active` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1: Mô hình đang phục vụ chính thức, 0: Mô hình nháp/lịch sử',
    `data_origin` VARCHAR(20) NOT NULL DEFAULT 'REAL' COMMENT 'REAL, SYNTHETIC, BENCHMARK',
    `dataset_version` VARCHAR(100) NULL COMMENT 'Mã hash SHA-256 hoặc tên tệp dữ liệu nguồn',
    `dataset_row_count` INT NOT NULL DEFAULT 0,
    `parameters_json` JSON NULL COMMENT 'Tham số cấu hình: k, min_support, min_confidence, min_lift, metric, etc.',
    `metrics_json` JSON NULL COMMENT 'Kết quả độ đo: silhouette_score, davies_bouldin, calinski_harabasz, rules_count, etc.',
    `notes` VARCHAR(500) NULL,
    `started_at` DATETIME(6) NOT NULL,
    `finished_at` DATETIME(6) NULL,
    `created_by` BIGINT NULL,
    `created_date` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `idx_mmr_type_active` (`model_type`, `is_active`, `run_status`),
    INDEX `idx_mmr_created_date` (`created_date` DESC),
    CONSTRAINT `chk_mmr_status` CHECK (`run_status` IN ('RUNNING', 'SUCCESS', 'FAILED')),
    CONSTRAINT `chk_mmr_active` CHECK (`is_active` IN (0, 1)),
    CONSTRAINT `chk_mmr_origin` CHECK (`data_origin` IN ('REAL', 'SYNTHETIC', 'BENCHMARK'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng cha lưu trữ Luật kết hợp (Association Rules)
CREATE TABLE IF NOT EXISTS `association_rules` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rule_code` VARCHAR(64) NULL COMMENT 'Mã định danh duy nhất của luật',
    `antecedent_count` INT NOT NULL DEFAULT 1 COMMENT 'Số lượng sản phẩm ở vế điều kiện (X)',
    `consequent_count` INT NOT NULL DEFAULT 1 COMMENT 'Số lượng sản phẩm ở vế hệ quả (Y)',
    `antecedents_summary` TEXT NULL COMMENT 'Mô tả tóm tắt vế trái [SKU_A, SKU_B] để hiển thị/debug',
    `consequents_summary` TEXT NULL COMMENT 'Mô tả tóm tắt vế phải [SKU_C, SKU_D] để hiển thị/debug',
    `support` DOUBLE NOT NULL COMMENT 'Độ hỗ trợ P(A U B)',
    `confidence` DOUBLE NOT NULL COMMENT 'Độ tin cậy P(B|A)',
    `lift` DOUBLE NOT NULL COMMENT 'Độ nâng P(B|A) / P(B)',
    `leverage` DOUBLE NULL,
    `conviction` DOUBLE NULL,
    `transaction_count` INT NOT NULL DEFAULT 0,
    `model_run_id` BIGINT NOT NULL,
    `is_active` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1: Sử dụng gợi ý, 0: Tạm ẩn',
    `created_date` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    INDEX `idx_ar_lift_conf` (`lift` DESC, `confidence` DESC),
    INDEX `idx_ar_ant_count` (`antecedent_count`),
    INDEX `idx_ar_model_run` (`model_run_id`),
    CONSTRAINT `fk_ar_model_run` FOREIGN KEY (`model_run_id`) REFERENCES `mining_model_runs` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_ar_metrics` CHECK (`support` > 0 AND `confidence` > 0 AND `lift` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng con lưu trữ chi tiết các mặt hàng trong Luật kết hợp (Hỗ trợ {A, B} -> {C, D})
CREATE TABLE IF NOT EXISTS `association_rule_items` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rule_id` BIGINT NOT NULL COMMENT 'FK liên kết association_rules',
    `product_id` BIGINT NOT NULL COMMENT 'FK liên kết products',
    `item_type` VARCHAR(20) NOT NULL COMMENT 'ANTECEDENT (Vế điều kiện) hoặc CONSEQUENT (Vế hệ quả)',
    `created_date` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_product_type` (`rule_id`, `product_id`, `item_type`),
    INDEX `idx_ari_prod_type` (`product_id`, `item_type`),
    INDEX `idx_ari_rule_type` (`rule_id`, `item_type`),
    CONSTRAINT `fk_ari_rule` FOREIGN KEY (`rule_id`) REFERENCES `association_rules` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ari_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_ari_item_type` CHECK (`item_type` IN ('ANTECEDENT', 'CONSEQUENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng lưu trữ Phân cụm Khách hàng (Customer Clusters)
CREATE TABLE IF NOT EXISTS `customer_clusters` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_id` BIGINT NOT NULL COMMENT 'FK liên kết bảng users',
    `cluster_id` INT NOT NULL COMMENT 'Mã cụm: 0, 1, 2, 3',
    `cluster_label` VARCHAR(100) NOT NULL COMMENT 'Tên diễn giải trung lập theo dữ liệu thực nghiệm',
    `distance_to_centroid` DOUBLE NULL COMMENT 'Khoảng cách Euclidean tới tâm cụm',
    `features_json` JSON NULL COMMENT 'Snapshot vector đặc trưng RFM + cấu hình tại thời điểm gán',
    `model_run_id` BIGINT NOT NULL COMMENT 'FK liên kết mining_model_runs',
    `assigned_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_cluster_active` (`customer_id`, `model_run_id`),
    INDEX `idx_cc_cluster_id` (`cluster_id`),
    CONSTRAINT `fk_cc_customer` FOREIGN KEY (`customer_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cc_model_run` FOREIGN KEY (`model_run_id`) REFERENCES `mining_model_runs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng lưu trữ Danh mục Sản phẩm Gợi ý theo Cụm (Cluster Recommendations - Tối ưu 99% bộ nhớ)
CREATE TABLE IF NOT EXISTS `cluster_recommendations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `cluster_id` INT NOT NULL COMMENT 'Mã cụm: 0, 1, 2, 3',
    `recommended_product_id` BIGINT NOT NULL COMMENT 'Sản phẩm gợi ý đại diện cho cụm',
    `score` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Trọng số phù hợp / mức độ ưu tiên của sản phẩm đối với cụm',
    `rank_no` INT NOT NULL DEFAULT 1 COMMENT 'Thứ hạng hiển thị: 1, 2, 3...',
    `model_run_id` BIGINT NOT NULL,
    `created_date` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cluster_product_run` (`cluster_id`, `recommended_product_id`, `model_run_id`),
    INDEX `idx_cr_cluster_rank` (`cluster_id`, `model_run_id`, `rank_no`),
    CONSTRAINT `fk_cr_product` FOREIGN KEY (`recommended_product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cr_model_run` FOREIGN KEY (`model_run_id`) REFERENCES `mining_model_runs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng lưu trữ Gợi ý Cấu hình Tương đồng (Content-Based Similar Products)
CREATE TABLE IF NOT EXISTS `product_recommendations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `recommendation_type` VARCHAR(50) NOT NULL COMMENT 'SIMILAR_CONTENT, POPULAR_FALLBACK',
    `source_product_id` BIGINT NOT NULL COMMENT 'Sản phẩm nguồn đang xem',
    `recommended_product_id` BIGINT NOT NULL COMMENT 'Sản phẩm đề xuất tương đồng cấu hình',
    `score` DOUBLE NOT NULL DEFAULT 0.0 COMMENT 'Cosine Similarity score (0.0 -> 1.0)',
    `rank_no` INT NOT NULL DEFAULT 1 COMMENT 'Thứ hạng hiển thị: 1, 2, 3...',
    `model_run_id` BIGINT NOT NULL,
    `created_date` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_source_recommended_type_run` (`source_product_id`, `recommended_product_id`, `recommendation_type`, `model_run_id`),
    INDEX `idx_pr_source_type_rank` (`source_product_id`, `recommendation_type`, `model_run_id`, `rank_no`),
    CONSTRAINT `fk_pr_source` FOREIGN KEY (`source_product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pr_recommended` FOREIGN KEY (`recommended_product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pr_model_run` FOREIGN KEY (`model_run_id`) REFERENCES `mining_model_runs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
