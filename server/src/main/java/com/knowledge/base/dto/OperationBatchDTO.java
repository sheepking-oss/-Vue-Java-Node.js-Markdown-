package com.knowledge.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationBatchDTO {
    
    private Long documentId;
    private Integer fromVersion;
    private List<OperationDTO> operations;
    private String userId;
    private String timestamp;
    private String source;
}
