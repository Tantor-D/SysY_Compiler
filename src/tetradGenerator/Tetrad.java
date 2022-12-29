package tetradGenerator;

public class Tetrad {
    public enum OpType {    // 表示操作的类型
        assign,         // (op, label1, _ ,des) 这个四元式用于temp封装信息，label1可以是funcRet这样的
        // 计算相关
        add,
        sub,
        mul,
        div,
        mod,
        shl,    // 优化部分，将一些mul转为shl
        shr,    // 优化部分。将一些div转为shr
    
        // unary表达式
        unaryOp_add,    // (op, label1, _, des)
        unaryOp_sub,    // (op, label1, _, des)
        // unaryOp_not  不存在!的四元式，因为其涉及到了if-else，而label相关的信息我是在四元式中就确定了的，因此直接使用if-else的方法处理完了
        
        // 定义一般变量
        defVar_normal,              // (op, _, _, var)
        defVar_normal_AndAssign,     // (op, label, _, var)
        defConst_normal_And_Assign,   // (op, num, _, var)需要与数组进行区分(const的定义一定是会进行赋值的)
        
        // 数组相关的内容
        defOneDimArray,     // (op, size, _, array)
        defTwoDimArray,     // (op, size1, size2, array)
        
        getArrayVal,        // (op, array, offset, des)
        getArrayAddr,      // (op, array, offset, des) 计算数组对应的地址，并存到变量des中
        assignArrayVal,     // (op, offset, val, array)
        
        // 定义函数
        defFunc,    // (op, int/void, _, funcName)  这个int/void 实际上没有用到，是为了输出中间代码加的
        defFuncPara_normal, // (op, _, _, var)
        defFuncPara_oneDim, // (op, _, _, var)  作为函数参数时，长度缺省
        defFuncPara_twoDim, // (op, num, _, var) 作为函数参数时，第1维长度缺省
        
        // 调用函数
        pushPara,   // (op, index, _, var)  index为是第几个参数，从1开始
        callFunc,   // (op, 参数个数, _, funcName)
        
        // return
        returnVoid,     // (op, _, _, _)
        returnInt,      // (op, _, _, ret)
        
        // 输入输出
        printStr,       // (op, _, _, strLabel)
        printInt,       // (op, _, _, ident)
        getint,         // (op, _, _, ident)
        
        // 跳转和label
        bnez,           // (op, var, _, label)
        beqz,           // (op, var, _, label)
        bltz,           // (op, var, _, label)
        blez,           // (op, var, _, label)
        bgtz,           // (op, var, _, label)
        bgez,           // (op, var, _, label)
        jump,           // (op, _, _, label)
        genLabel       // (op, _, _, label)
        
    }
    
    private OpType opType;
    private String label1;
    private String label2;
    private String des;
    
    public Tetrad(OpType opType, String label1, String label2, String des) {
        this.opType = opType;
        this.label1 = label1;
        this.label2 = label2;
        this.des = des;
    }
    
    @Override
    public String toString() {
        return "Tetrad{" +
                "opType=" + opType +
                ", label1='" + label1 + '\'' +
                ", label2='" + label2 + '\'' +
                ", des='" + des + '\'' +
                '}';
    }
    
    public String myToString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(opType.name());
        stringBuffer.append(", ");
        if (label1 == null) {
            stringBuffer.append("_");
        } else {
            stringBuffer.append(label1);
        }
        stringBuffer.append(", ");
        if (label2 == null) {
            stringBuffer.append("_");
        } else {
            stringBuffer.append(label2);
        }
        stringBuffer.append(", ");
        if (des == null) {
            stringBuffer.append("_");
        } else {
            stringBuffer.append(des);
        }
        return stringBuffer.toString();
    }
    
    /////////////////////////////////////////////////////////
    // gets and sets
    
    public OpType getOpType() {
        return opType;
    }
    
    public void setOpType(OpType opType) {
        this.opType = opType;
    }
    
    public String getLabel1() {
        return label1;
    }
    
    public void setLabel1(String label1) {
        this.label1 = label1;
    }
    
    public String getLabel2() {
        return label2;
    }
    
    public void setLabel2(String label2) {
        this.label2 = label2;
    }
    
    public String getDes() {
        return des;
    }
    
    public void setDes(String des) {
        this.des = des;
    }
}
