package symbolTable;

import constDecl.SymbolConst;

import java.util.ArrayList;

public class Symbol {
    private String name;    // 标识名称
    private int lineNum;    // 声明symbol时的行号
    private int type;   // 标记CONST, VAR, PARAM, VOIDFUNC, INTFUNC
    
    // 为了数组
    private int dim;    // 一般变量和函数就是0，数组则会是1，2
    
    // 为了函数，记录参数的维度，为0，1，2
    private ArrayList<Integer> paraDimList; // 列表的len就代表了参数的个数
    private int paraNum;
    
    // 代码生成阶段
    // 记录值的信息，编译阶段主要是记录const的值
    // 每个变量的唯一标识，值得注意的是，在语法树->四元式的过程中，分析变量时的行为都是对name操作的，只有当生成四元式时用fakeName
    // 在树中，由于其存在结构信息 且 符号表是嵌在树上的，因此有重名也没事，但是四元式中最好保证标识符唯一
    private String fakeName;
    private int val;    // 一般变量的值
    private boolean isAlive;    // 用于代码生成阶段，在同一个block中可能出现先调用外层的变量a，再重新定义变量a的情况，需要借此区分。
    private final ArrayList<Integer> arrayUpBoundList;    // 仅数组有用，记录数组元素某一维的上界，(这个事留到语义分析做)
    private final ArrayList<Integer> arrayValList;    // 用于代码生成阶段，记录const数组中的内容
    
    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", lineNum=" + lineNum +
                ", type=" + type +
                ", dim=" + dim +
                ", paraDimList=" + paraDimList +
                ", paraNum=" + paraNum +
                '}';
    }
    
    public Symbol(String name, int lineNum, int type) {
        this.name = name;
        this.lineNum = lineNum;
        this.type = type;
        arrayUpBoundList = new ArrayList<>();
        arrayValList = new ArrayList<>();
        if (type == SymbolConst.INT_FUNC_SYMBOL || type == SymbolConst.VOID_FUNC_SYMBOL) {
            paraDimList = new ArrayList<>();
        }
        this.isAlive = false;
    }
    
    public Symbol(String name, int lineNum, int type, int dim) {
        this.name = name;
        this.lineNum = lineNum;
        this.type = type;
        this.dim = dim;
        arrayUpBoundList = new ArrayList<>();
        arrayValList = new ArrayList<>();
        if (type == SymbolConst.INT_FUNC_SYMBOL || type == SymbolConst.VOID_FUNC_SYMBOL) {
            paraDimList = new ArrayList<>();
        }
        this.isAlive = false;
    }
    
    public void funcGetParaDim(ArrayList<Symbol> paraSymbolList) {
        for (Symbol paraSymbol : paraSymbolList) {
            paraDimList.add(paraSymbol.getDim());
        }
        paraNum = paraDimList.size();
    }
    
    public boolean isArray() {
        return dim > 0;
    }
    
    public void setArrayUpBound(int index, int size) {
        if (arrayUpBoundList.isEmpty()) {   // 没有初始化，还是空列表
            arrayUpBoundList.add(-1);
            arrayUpBoundList.add(-1);
        }
        arrayUpBoundList.set(index, size);
    }
    
    public void setArrayVal(int offset, int val) {
        if (arrayValList.size() != offset) {
            System.out.println("数组赋初值阶段有错");
        }
        arrayValList.add(val);
    }
    
    public int getArrayVal(int offset) {
        return arrayValList.get(offset);
    }
    
    ////////////////////////////////// gets and sets
    public ArrayList<Integer> getArrayUpBoundList() {
        return arrayUpBoundList;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getLineNum() {
        return lineNum;
    }
    
    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }
    
    public int getDim() {
        return dim;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public ArrayList<Integer> getParaDimList() {
        return paraDimList;
    }
    
    public void setParaDimList(ArrayList<Integer> paraDimList) {
        this.paraDimList = paraDimList;
    }
    
    public int getParaNum() {
        return paraNum;
    }
    
    public void setParaNum(int paraNum) {
        this.paraNum = paraNum;
    }
    
    public void setDim(int dim) {
        this.dim = dim;
    }
    
    public int getVal() {
        return val;
    }
    
    public void setVal(int val) {
        this.val = val;
    }
    
    public String getFakeName() {
        return fakeName;
    }
    
    public void setFakeName(String fakeName) {
        this.fakeName = fakeName;
    }
    
    public boolean isAlive() {
        return isAlive;
    }
    
    public void setAlive(boolean alive) {
        isAlive = alive;
    }
}
