package com.knowledge.base.ot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Operation {
    
    public enum Type {
        RETAIN,
        INSERT,
        DELETE
    }

    private Type type;
    
    private Integer retainCount;
    
    private String insertText;
    
    private String deleteText;
    
    public static Operation retain(int count) {
        Operation op = new Operation();
        op.setType(Type.RETAIN);
        op.setRetainCount(count);
        return op;
    }
    
    public static Operation insert(String text) {
        Operation op = new Operation();
        op.setType(Type.INSERT);
        op.setInsertText(text);
        return op;
    }
    
    public static Operation delete(String text) {
        Operation op = new Operation();
        op.setType(Type.DELETE);
        op.setDeleteText(text);
        return op;
    }
    
    public int length() {
        switch (type) {
            case RETAIN:
                return retainCount;
            case INSERT:
                return insertText.length();
            case DELETE:
                return deleteText.length();
            default:
                return 0;
        }
    }
    
    public int getBaseLength() {
        switch (type) {
            case RETAIN:
                return retainCount;
            case INSERT:
                return 0;
            case DELETE:
                return deleteText.length();
            default:
                return 0;
        }
    }
    
    public int getTargetLength() {
        switch (type) {
            case RETAIN:
                return retainCount;
            case INSERT:
                return insertText.length();
            case DELETE:
                return 0;
            default:
                return 0;
        }
    }
    
    @Override
    public String toString() {
        switch (type) {
            case RETAIN:
                return "retain(" + retainCount + ")";
            case INSERT:
                return "insert(" + insertText + ")";
            case DELETE:
                return "delete(" + deleteText + ")";
            default:
                return "unknown";
        }
    }
    
    public Operation invert() {
        switch (type) {
            case RETAIN:
                return Operation.retain(retainCount);
            case INSERT:
                return Operation.delete(insertText);
            case DELETE:
                return Operation.insert(deleteText);
            default:
                return null;
        }
    }
}
