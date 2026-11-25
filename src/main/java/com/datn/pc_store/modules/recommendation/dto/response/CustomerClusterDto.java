package com.datn.pc_store.modules.recommendation.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerClusterDto {
    private Integer clusterId;
    private String clusterLabel;
    private Integer customerCount;
    private Double percentage;
    private List<Double> centroid;
}
