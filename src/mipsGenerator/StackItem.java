package mipsGenerator;

public class StackItem {
    private final String name;
    private final int offset;     // 是个负数
    
    public StackItem(String name, int offset) {
        this.name = name;
        this.offset = offset;
    }
    
    public String getName() {
        return name;
    }
    
    
    public int getOffset() {
        return offset;
    }
    
}
