package com.datn.pc_store.modules.brand.service;

import com.datn.pc_store.common.dto.PageResponse;
import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.DuplicateResourceException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.common.util.SlugUtils;
import com.datn.pc_store.modules.brand.dto.BrandCreateRequest;
import com.datn.pc_store.modules.brand.dto.BrandResponse;
import com.datn.pc_store.modules.brand.dto.BrandUpdateRequest;
import com.datn.pc_store.modules.brand.entity.Brand;
import com.datn.pc_store.modules.brand.enums.BrandStatus;
import com.datn.pc_store.modules.brand.mapper.BrandMapper;
import com.datn.pc_store.modules.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Transactional(readOnly = true)
    public PageResponse<BrandResponse> getAllBrands(String keyword, BrandStatus status, Pageable pageable) {
        log.info("Lấy danh sách thương hiệu với keyword: '{}', status: '{}', pageable: {}", keyword, status, pageable);
        Page<Brand> page = brandRepository.searchBrands(keyword, status, pageable);
        return PageResponse.of(page.map(brandMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        log.info("Lấy toàn bộ danh sách thương hiệu đang hoạt động");
        return brandMapper.toResponseList(brandRepository.findByStatus(BrandStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long id) {
        log.info("Lấy thông tin thương hiệu theo id: {}", id);
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));
        return brandMapper.toResponse(brand);
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandBySlug(String slug) {
        log.info("Lấy thông tin thương hiệu theo slug: {}", slug);
        Brand brand = brandRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "slug", slug));
        return brandMapper.toResponse(brand);
    }

    @Transactional
    public BrandResponse createBrand(BrandCreateRequest req) {
        log.info("Thêm mới thương hiệu: {}", req.getName());

        String slug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? SlugUtils.toSlug(req.getSlug())
                : SlugUtils.toSlug(req.getName());

        if (slug.isBlank()) {
            throw new BadRequestException("Tên thương hiệu không hợp lệ để tạo slug đường dẫn.");
        }

        if (brandRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Thương hiệu", "slug", slug);
        }

        Brand brand = brandMapper.toEntity(req);
        brand.setSlug(slug);

        if (req.getStatus() == null) {
            brand.setStatus(BrandStatus.ACTIVE);
        }

        Brand saved = brandRepository.save(brand);
        log.info("Thêm mới thương hiệu thành công với id: {}, slug: {}", saved.getId(), saved.getSlug());
        return brandMapper.toResponse(saved);
    }

    @Transactional
    public BrandResponse updateBrand(Long id, BrandUpdateRequest req) {
        log.info("Cập nhật thương hiệu id: {}", id);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));

        String slug = (req.getSlug() != null && !req.getSlug().isBlank())
                ? SlugUtils.toSlug(req.getSlug())
                : SlugUtils.toSlug(req.getName());

        if (slug.isBlank()) {
            throw new BadRequestException("Tên thương hiệu không hợp lệ để tạo slug đường dẫn.");
        }

        if (brandRepository.existsBySlugAndIdNot(slug, id)) {
            throw new DuplicateResourceException("Thương hiệu", "slug", slug);
        }

        brandMapper.updateEntityFromDto(req, brand);
        brand.setSlug(slug);

        if (req.getStatus() != null) {
            brand.setStatus(req.getStatus());
        }

        Brand updated = brandRepository.save(brand);
        log.info("Cập nhật thương hiệu id: {} thành công", updated.getId());
        return brandMapper.toResponse(updated);
    }

    @Transactional
    public BrandResponse updateStatus(Long id, BrandStatus status) {
        log.info("Cập nhật trạng thái thương hiệu id: {} sang: {}", id, status);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));

        brand.setStatus(status);
        Brand updated = brandRepository.save(brand);
        log.info("Cập nhật trạng thái thương hiệu id: {} thành công", updated.getId());
        return brandMapper.toResponse(updated);
    }

    @Transactional
    public void deleteBrand(Long id) {
        log.info("Yêu cầu xóa mềm thương hiệu id: {}", id);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thương hiệu", "id", id));

        // Soft delete bằng cách gọi delete (được kích hoạt thông qua @SQLDelete trên Entity)
        brandRepository.delete(brand);
        log.info("Đã xóa mềm thương hiệu id: {} thành công", id);
    }
}
