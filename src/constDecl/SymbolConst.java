package constDecl;

public class SymbolConst {
    // block的种类
    public static final int NORMAL_BLOCK = 1;
    public static final int LOOP_BLOCK = 2;
    public static final int VOID_FUNC_BLOCK = 3;
    public static final int INT_FUNC_BLOCK = 4;
    
    // symbol的种类
    public static final int CONST_SYMBOL = 0;
    public static final int VAR_SYMBOL = 1;
    public static final int PARAM_SYMBOL = 2;
    public static final int VOID_FUNC_SYMBOL = 3;
    public static final int INT_FUNC_SYMBOL = 4;
    
    // 函数实参的类型，维度有0，1，2，还有一个不应出现的void函数类型做函数参数
    // 0维变量对应0，1维对应1，2维对应2，这3个一定不能变，主体代码中利用了这一约定
    public static final int NORMAL_PARA = 0;
    public static final int ONE_DIM_PARA = 1;
    public static final int TWO_DIM_PARA = 2;
    public static final int VOID_FUNC_PARA = 3;
    public static final int FIT_ANYTHING_PARA = 4; // 打的补丁，针对函数未定义的情况，若其也为函数的实参，就不知道类型了。故创造一个万能类型
    
    // 上一条stmt的类型
    public static final int NOT_RETURN_STMT = 0;
    public static final int RETURN_STMT = 1;
    
}
