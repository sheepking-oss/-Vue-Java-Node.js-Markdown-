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
public class OperationDTO {
    
    private String type;
    private Integer retainCount;
    private String insertText;
    private String deleteText;
    
    public static OperationDTO from(com.knowledge.base.ot.Operation op) {
        return OperationDTO.builder()
            .type(op.getType().name().toLowerCase())
            .retainCount(op.getRetainCount())
            .insertText(op.getInsertText())
            .deleteText(op.getDeleteText())
            .build();
    }
    
    public com.knowledge.base.ot.Operation toOperation() {
        com.knowledge.base.ot.Operation.Type opType = 
            com.knowledge.base.ot.Operation.Type.valueOf(type.toUpperCase());
        
        switch (opType) {
            case RETAIN:
                return com.knowledge.base.ot.Operation.retain(retainCount);
            case INSERT:
                return com.knowledge.base.ot.Operation.insert(insertText);
            case DELETE:
                return com.knowledge.base.ot.Operation.delete(deleteText);
            default:
                throw new IllegalArgumentException("Unknown operation type: " + type);
        }
    }
}
