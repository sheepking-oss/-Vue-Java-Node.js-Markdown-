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
public class OperationResultDTO {
    
    private boolean success;
    private String errorMessage;
    private Integer newVersion;
    private String newContent;
    private List<OperationDTO> transformedOperations;
    private List<OperationDTO> concurrentOperations;
}
