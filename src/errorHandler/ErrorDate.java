package errorHandler;

public class ErrorDate {
    private String kind;
    private int lineNum;
    private String description;
    
    public ErrorDate(String kind, int lineNum, String description) {
        this.kind = kind;
        this.lineNum = lineNum;
        this.description = description;
    }
    
    public ErrorDate(String kind, int lineNum) {
        this.kind = kind;
        this.lineNum = lineNum;
    }
    
    public String getKind() {
        return kind;
    }
    
    public int getLineNum() {
        return lineNum;
    }
}
