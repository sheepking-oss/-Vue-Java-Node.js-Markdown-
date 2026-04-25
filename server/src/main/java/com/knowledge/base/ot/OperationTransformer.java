package com.knowledge.base.ot;

import java.util.ArrayList;
import java.util.List;

public class OperationTransformer {
    
    public static class OpPair {
        public final List<Operation> first;
        public final List<Operation> second;
        
        public OpPair(List<Operation> first, List<Operation> second) {
            this.first = first;
            this.second = second;
        }
    }
    
    public static OpPair transform(List<Operation> opsA, List<Operation> opsB) {
        List<Operation> resultA = new ArrayList<>();
        List<Operation> resultB = new ArrayList<>();
        
        int i = 0, j = 0;
        
        while (i < opsA.size() && j < opsB.size()) {
            Operation a = opsA.get(i);
            Operation b = opsB.get(j);
            
            if (a.getType() == Operation.Type.INSERT && b.getType() == Operation.Type.INSERT) {
                if (i <= j) {
                    resultA.add(a);
                    resultB.add(Operation.retain(a.length()));
                    i++;
                } else {
                    resultA.add(Operation.retain(b.length()));
                    resultB.add(b);
                    j++;
                }
            }
            else if (a.getType() == Operation.Type.INSERT) {
                resultA.add(a);
                resultB.add(Operation.retain(a.length()));
                i++;
            }
            else if (b.getType() == Operation.Type.INSERT) {
                resultA.add(Operation.retain(b.length()));
                resultB.add(b);
                j++;
            }
            else {
                int lenA = a.getBaseLength();
                int lenB = b.getBaseLength();
                
                if (lenA < lenB) {
                    Operation newB = cloneOperation(b, lenA);
                    resultA.add(a);
                    resultB.add(newB);
                    
                    if (b.getType() == Operation.Type.DELETE) {
                        opsB.set(j, Operation.delete(b.getDeleteText().substring(lenA)));
                    } else if (b.getType() == Operation.Type.RETAIN) {
                        opsB.set(j, Operation.retain(b.getRetainCount() - lenA));
                    }
                    i++;
                }
                else if (lenA > lenB) {
                    Operation newA = cloneOperation(a, lenB);
                    resultA.add(newA);
                    resultB.add(b);
                    
                    if (a.getType() == Operation.Type.DELETE) {
                        opsA.set(i, Operation.delete(a.getDeleteText().substring(lenB)));
                    } else if (a.getType() == Operation.Type.RETAIN) {
                        opsA.set(i, Operation.retain(a.getRetainCount() - lenB));
                    }
                    j++;
                }
                else {
                    if (a.getType() == Operation.Type.RETAIN && b.getType() == Operation.Type.RETAIN) {
                        resultA.add(Operation.retain(lenA));
                        resultB.add(Operation.retain(lenB));
                    }
                    else if (a.getType() == Operation.Type.DELETE && b.getType() == Operation.Type.RETAIN) {
                        resultA.add(a);
                    }
                    else if (a.getType() == Operation.Type.RETAIN && b.getType() == Operation.Type.DELETE) {
                        resultB.add(b);
                    }
                    else if (a.getType() == Operation.Type.DELETE && b.getType() == Operation.Type.DELETE) {
                        if (!a.getDeleteText().equals(b.getDeleteText())) {
                            throw new IllegalArgumentException("Concurrent delete operations with different text");
                        }
                    }
                    i++;
                    j++;
                }
            }
        }
        
        while (i < opsA.size()) {
            Operation a = opsA.get(i);
            if (a.getType() == Operation.Type.INSERT) {
                resultA.add(a);
                resultB.add(Operation.retain(a.length()));
            } else {
                resultA.add(a);
            }
            i++;
        }
        
        while (j < opsB.size()) {
            Operation b = opsB.get(j);
            if (b.getType() == Operation.Type.INSERT) {
                resultA.add(Operation.retain(b.length()));
                resultB.add(b);
            } else {
                resultB.add(b);
            }
            j++;
        }
        
        return new OpPair(resultA, resultB);
    }
    
    private static Operation cloneOperation(Operation op, int length) {
        switch (op.getType()) {
            case RETAIN:
                return Operation.retain(length);
            case INSERT:
                return Operation.insert(op.getInsertText().substring(0, length));
            case DELETE:
                return Operation.delete(op.getDeleteText().substring(0, length));
            default:
                return null;
        }
    }
    
    public static String apply(List<Operation> operations, String document) {
        StringBuilder result = new StringBuilder();
        int docIndex = 0;
        
        for (Operation op : operations) {
            switch (op.getType()) {
                case RETAIN:
                    if (docIndex + op.getRetainCount() > document.length()) {
                        throw new IllegalArgumentException("Retain operation exceeds document length");
                    }
                    result.append(document, docIndex, docIndex + op.getRetainCount());
                    docIndex += op.getRetainCount();
                    break;
                    
                case INSERT:
                    result.append(op.getInsertText());
                    break;
                    
                case DELETE:
                    String toDelete = document.substring(docIndex, docIndex + op.getDeleteText().length());
                    if (!toDelete.equals(op.getDeleteText())) {
                        throw new IllegalArgumentException("Delete operation text mismatch");
                    }
                    docIndex += op.getDeleteText().length();
                    break;
            }
        }
        
        if (docIndex < document.length()) {
            result.append(document.substring(docIndex));
        }
        
        return result.toString();
    }
    
    public static List<Operation> compose(List<Operation> ops1, List<Operation> ops2) {
        List<Operation> result = new ArrayList<>();
        
        int i = 0, j = 0;
        
        while (i < ops1.size() || j < ops2.size()) {
            Operation op1 = i < ops1.size() ? ops1.get(i) : null;
            Operation op2 = j < ops2.size() ? ops2.get(j) : null;
            
            if (op1 == null) {
                result.add(op2);
                j++;
            }
            else if (op2 == null) {
                result.add(op1);
                i++;
            }
            else if (op1.getType() == Operation.Type.INSERT) {
                result.add(op1);
                i++;
            }
            else if (op2.getType() == Operation.Type.DELETE) {
                result.add(op2);
                j++;
            }
            else {
                int len1 = op1.getType() == Operation.Type.RETAIN ? op1.getRetainCount() : op1.getDeleteText().length();
                int len2 = op2.getType() == Operation.Type.RETAIN ? op2.getRetainCount() : op2.getInsertText().length();
                
                if (len1 < len2) {
                    if (op1.getType() == Operation.Type.DELETE) {
                        result.add(op1);
                    }
                    
                    if (op2.getType() == Operation.Type.INSERT) {
                        result.add(Operation.insert(op2.getInsertText().substring(0, len1)));
                        ops2.set(j, Operation.insert(op2.getInsertText().substring(len1)));
                    }
                    else if (op2.getType() == Operation.Type.RETAIN) {
                        if (op1.getType() != Operation.Type.DELETE) {
                            result.add(Operation.retain(len1));
                        }
                        ops2.set(j, Operation.retain(op2.getRetainCount() - len1));
                    }
                    i++;
                }
                else if (len1 > len2) {
                    if (op1.getType() == Operation.Type.DELETE) {
                        result.add(Operation.delete(op1.getDeleteText().substring(0, len2)));
                        ops1.set(i, Operation.delete(op1.getDeleteText().substring(len2)));
                    }
                    else if (op1.getType() == Operation.Type.RETAIN) {
                        if (op2.getType() == Operation.Type.INSERT) {
                            result.add(op2);
                        }
                        else {
                            result.add(Operation.retain(len2));
                        }
                        ops1.set(i, Operation.retain(op1.getRetainCount() - len2));
                    }
                    j++;
                }
                else {
                    if (op1.getType() == Operation.Type.DELETE) {
                        result.add(op1);
                    }
                    else if (op2.getType() == Operation.Type.INSERT) {
                        result.add(op2);
                    }
                    else if (op1.getType() == Operation.Type.RETAIN && op2.getType() == Operation.Type.RETAIN) {
                        result.add(Operation.retain(len1));
                    }
                    i++;
                    j++;
                }
            }
        }
        
        return result;
    }
    
    public static List<Operation> invert(List<Operation> operations) {
        List<Operation> inverted = new ArrayList<>();
        for (int i = operations.size() - 1; i >= 0; i--) {
            inverted.add(operations.get(i).invert());
        }
        return inverted;
    }
    
    public static List<Operation> diff(String oldStr, String newStr) {
        int m = oldStr.length();
        int n = newStr.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldStr.charAt(i - 1) == newStr.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                }
                else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        List<Operation> operations = new ArrayList<>();
        int i = m, j = n;
        
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldStr.charAt(i - 1) == newStr.charAt(j - 1)) {
                i--;
                j--;
                addRetainOrMerge(operations, 1);
            }
            else if (i > 0 && (j == 0 || dp[i - 1][j] <= dp[i][j - 1])) {
                addDeleteOrMerge(operations, oldStr.charAt(i - 1));
                i--;
            }
            else {
                addInsertOrMerge(operations, newStr.charAt(j - 1));
                j--;
            }
        }
        
        List<Operation> reversed = new ArrayList<>();
        for (int k = operations.size() - 1; k >= 0; k--) {
            reversed.add(operations.get(k));
        }
        
        return reversed;
    }
    
    private static void addRetainOrMerge(List<Operation> ops, int count) {
        if (ops.isEmpty() || ops.get(ops.size() - 1).getType() != Operation.Type.RETAIN) {
            ops.add(Operation.retain(count));
        }
        else {
            Operation last = ops.get(ops.size() - 1);
            last.setRetainCount(last.getRetainCount() + count);
        }
    }
    
    private static void addDeleteOrMerge(List<Operation> ops, char ch) {
        if (ops.isEmpty() || ops.get(ops.size() - 1).getType() != Operation.Type.DELETE) {
            ops.add(Operation.delete(String.valueOf(ch)));
        }
        else {
            Operation last = ops.get(ops.size() - 1);
            last.setDeleteText(last.getDeleteText() + ch);
        }
    }
    
    private static void addInsertOrMerge(List<Operation> ops, char ch) {
        if (ops.isEmpty() || ops.get(ops.size() - 1).getType() != Operation.Type.INSERT) {
            ops.add(Operation.insert(String.valueOf(ch)));
        }
        else {
            Operation last = ops.get(ops.size() - 1);
            last.setInsertText(last.getInsertText() + ch);
        }
    }
}
