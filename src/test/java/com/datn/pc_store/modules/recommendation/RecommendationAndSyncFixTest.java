package com.datn.pc_store.modules.recommendation;

import com.datn.pc_store.common.exception.BadRequestException;
import com.datn.pc_store.common.exception.ResourceNotFoundException;
import com.datn.pc_store.modules.order.repository.OrderItemRepository;
import com.datn.pc_store.modules.product.entity.Product;
import com.datn.pc_store.modules.product.enums.ProductStatus;
import com.datn.pc_store.modules.product.repository.ProductRepository;
import com.datn.pc_store.modules.recommendation.dto.request.SyncMiningPayloadRequest;
import com.datn.pc_store.modules.recommendation.dto.response.MiningModelRunDto;
import com.datn.pc_store.modules.recommendation.dto.response.PersonalizedRecommendationResponse;
import com.datn.pc_store.modules.recommendation.dto.response.ProductRecommendationDto;
import com.datn.pc_store.modules.recommendation.entity.AssociationRule;
import com.datn.pc_store.modules.recommendation.entity.MiningModelRun;
import com.datn.pc_store.modules.recommendation.enums.ModelType;
import com.datn.pc_store.modules.recommendation.enums.RunStatus;
import com.datn.pc_store.modules.recommendation.repository.*;
import com.datn.pc_store.modules.recommendation.service.impl.DataMiningSyncServiceImpl;
import com.datn.pc_store.modules.recommendation.service.impl.RecommendationServiceImpl;
import com.datn.pc_store.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationAndSyncFixTest {

    @Mock
    private AssociationRuleItemRepository ruleItemRepo;
    @Mock
    private ProductRecommendationRepository prodRecRepo;
    @Mock
    private ClusterRecommendationRepository clusterRecRepo;
    @Mock
    private CustomerClusterRepository customerClusterRepo;
    @Mock
    private ProductRepository productRepo;
    @Mock
    private OrderItemRepository orderItemRepo;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Mock
    private MiningModelRunRepository modelRunRepository;
    @Mock
    private AssociationRuleRepository associationRuleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private DataMiningSyncServiceImpl dataMiningSyncService;

    @Test
    @DisplayName("RecommendationService ném 404 ResourceNotFoundException khi sản phẩm INACTIVE")
    void throws404WhenProductInactive() {
        Product inactive = Product.builder().id(10L).status(ProductStatus.INACTIVE).build();
        when(productRepo.findById(10L)).thenReturn(Optional.of(inactive));

        assertThrows(ResourceNotFoundException.class,
                () -> recommendationService.getFrequentlyBoughtTogether(10L, 4));
        assertThrows(ResourceNotFoundException.class,
                () -> recommendationService.getSimilarProducts(10L, 4));
    }

    @Test
    @DisplayName("RecommendationService không gọi IN () query khi danh sách ứng viên rỗng")
    void avoidsEmptyInClauseQueryWhenNoCandidates() {
        ReflectionTestUtils.setField(recommendationService, "defaultMinLift", 1.5);
        Product active = Product.builder().id(1L).status(ProductStatus.ACTIVE).price(10_000_000L).brandId(2L).build();
        when(productRepo.findById(1L)).thenReturn(Optional.of(active));
        when(ruleItemRepo.findConsequentProductsForSingleProduct(eq(1L), any(), any(PageRequest.class))).thenReturn(List.of());
        when(productRepo.findCompatibleAccessoryIds(eq(1L), any(PageRequest.class))).thenReturn(List.of());
        when(productRepo.findTopSellingAccessoryIds(any(PageRequest.class))).thenReturn(List.of());

        List<ProductRecommendationDto> fbt = recommendationService.getFrequentlyBoughtTogether(1L, 4);
        assertTrue(fbt.isEmpty());

        when(productRepo.findGlobalTopSellingIds(any(PageRequest.class))).thenReturn(List.of());
        PersonalizedRecommendationResponse personalized = recommendationService.getPersonalizedRecommendations(null, 5);
        assertTrue(personalized.getProducts().isEmpty());

        verify(productRepo, never()).findAvailableInStockProductsByIds(any());
    }

    @Test
    @DisplayName("DataMiningSyncService lưu lại bản ghi FAILED sau khi rollback transaction đồng bộ lỗi")
    void syncModelDataPersistsFailedRunOnRollback() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(modelRunRepository.save(any(MiningModelRun.class))).thenAnswer(inv -> {
            MiningModelRun run = inv.getArgument(0);
            if (run.getId() == null) run.setId(100L);
            return run;
        });

        SyncMiningPayloadRequest badPayload = SyncMiningPayloadRequest.builder()
                .modelType(ModelType.ASSOCIATION_RULES)
                .algorithmName("FP-Growth")
                .rules(List.of(SyncMiningPayloadRequest.RulePayload.builder()
                        .ruleCode("R-BAD")
                        .antecedentProductIds(List.of(1L))
                        .consequentProductIds(List.of(2L))
                        .support(-0.5)
                        .confidence(0.8)
                        .lift(2.0)
                        .build()))
                .build();

        assertThrows(BadRequestException.class, () -> dataMiningSyncService.syncModelData(badPayload));

        verify(transactionManager).rollback(any(TransactionStatus.class));
        ArgumentCaptor<MiningModelRun> saveCaptor = ArgumentCaptor.forClass(MiningModelRun.class);
        verify(modelRunRepository, times(2)).save(saveCaptor.capture());
        List<MiningModelRun> savedRuns = saveCaptor.getAllValues();
        assertEquals(RunStatus.FAILED, savedRuns.get(1).getRunStatus());
        assertEquals(0, savedRuns.get(1).getIsActive());
    }

    @Test
    @DisplayName("DataMiningSyncService khử trùng productIds trong AssociationRuleItem và commit SUCCESS")
    void syncAssociationRulesDeduplicatesProductIds() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(modelRunRepository.save(any(MiningModelRun.class))).thenAnswer(inv -> {
            MiningModelRun run = inv.getArgument(0);
            if (run.getId() == null) run.setId(200L);
            return run;
        });
        when(productRepo.findAllById(any())).thenReturn(List.of(
                Product.builder().id(1L).build(),
                Product.builder().id(2L).build()
        ));

        SyncMiningPayloadRequest payload = SyncMiningPayloadRequest.builder()
                .modelType(ModelType.ASSOCIATION_RULES)
                .algorithmName("FP-Growth")
                .rules(List.of(SyncMiningPayloadRequest.RulePayload.builder()
                        .ruleCode("R-DUP")
                        .antecedentProductIds(List.of(1L, 1L))
                        .consequentProductIds(List.of(2L, 2L))
                        .support(0.1)
                        .confidence(0.8)
                        .lift(1.5)
                        .build()))
                .build();

        MiningModelRunDto dto = dataMiningSyncService.syncModelData(payload);

        assertEquals(RunStatus.SUCCESS, dto.getRunStatus());
        ArgumentCaptor<List<AssociationRule>> rulesCaptor = ArgumentCaptor.forClass(List.class);
        verify(associationRuleRepository).saveAll(rulesCaptor.capture());
        assertEquals(2, rulesCaptor.getValue().get(0).getItems().size());
        verify(transactionManager).commit(any(TransactionStatus.class));
    }

    @Test
    @DisplayName("DataMiningSyncService ném 404 ResourceNotFoundException khi không tìm thấy model version và 400 khi so sánh khác ModelType")
    void modelDetailsAndCompareExceptions() {
        when(modelRunRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> dataMiningSyncService.getModelDetails(999L));

        when(modelRunRepository.findById(1L)).thenReturn(Optional.of(MiningModelRun.builder().id(1L).modelType(ModelType.ASSOCIATION_RULES).build()));
        when(modelRunRepository.findById(2L)).thenReturn(Optional.of(MiningModelRun.builder().id(2L).modelType(ModelType.CONTENT_BASED).build()));
        assertThrows(BadRequestException.class, () -> dataMiningSyncService.compareModelVersions(1L, 2L));
    }
}
