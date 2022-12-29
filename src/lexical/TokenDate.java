package lexical;

public class TokenDate {
    private final String name;  // name中存的是源代码中的字符串
    private final String category;  // 是词法分析中name在符号表中所对应的
    private final int lineNum;  // 行号
    private int value;  // 数字对应的值
    private boolean isFormatStringRight;
    private int paraNumInFormatString;
    
    public TokenDate(String name, String category, int lineNum) {
        this.name = name;
        this.category = category;
        this.lineNum = lineNum;
        if (category.equals("INTCON")) {
            value = Integer.parseInt(name);
        }
        // System.out.println(name + "  lineNum: " + lineNum);
    }
    
    public String getSubString(int i) {
        String fullString = getFullStr();
        String[] strArr = fullString.split("%", -1);    // 加-1表示不丢弃末尾的空字符串
        /*System.out.println("inGetSubString, i=" + i);
        for (String str:strArr) {
            System.out.println(str);
        }*/
        
        if (i == 0) {
            if (strArr[0].length() == 0) {
                return null;
            }
            return strArr[0];
        } else {
            String nowStr = strArr[i];
            if (nowStr.length() == 1) { // 除了第一个字串，其它的都有%d 中的d，因此若长度为1，说明为空
                return null;
            } else {
                StringBuilder stringBuilder = new StringBuilder(strArr[i]);
                stringBuilder.deleteCharAt(0);
                return stringBuilder.toString();
            }
        }
    }
    
    public String getFullStr() {    // name是包含两边的双引号" 的，此处会删去
        return name.split("\"", -1)[1];
    }
    
    public void setFormatStringRight(boolean formatStringRight) {
        isFormatStringRight = formatStringRight;
    }
    
    public void setParaNumInFormatString(int paraNumInFormatString) {
        this.paraNumInFormatString = paraNumInFormatString;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public int getValue() {
        return value;
    }
    
    public int getLineNum() {
        return lineNum;
    }
    
    public int getParaNumInFormatString() {
        return paraNumInFormatString;
    }
    
    // 以下是为了语法分析
    public boolean isNameEqual(String str) {
        return str.equals(name);
    }
    
    public boolean isIdent() {
        return "IDENFR".equals(category);
    }
    
    public boolean isIntConst() {
        return "INTCON".equals(category);
    }
    
    public boolean isFormatString() {
        return "STRCON".equals(category);
    }
}
