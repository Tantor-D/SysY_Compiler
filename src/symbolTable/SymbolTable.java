package symbolTable;

import constDecl.SymbolConst;
import errorHandler.ErrorHandler;
import lexical.TokenDate;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;

public class SymbolTable {
    private final HashMap<String, Symbol> symbolMap;
    private final int level;      // 当前符号表所处的层的深度
    private final SymbolTable faTable;    // 上一层的符号表
    private final int blockType;  // 分为NORMAL_BLOCK、LOOP_BLOCK、VOID_FUNC_BLOCK、INT_FUNC_BLOCK 四种
    private final ErrorHandler errorHandler;
    private int lastStmtType; // 当前block中（不包括子层），上一句的stmt类型，有normal和return两种，用于int型函数最后一句的判定
    private boolean hasFinishSetSymbolFakeName; // 需要这个是因此在defFunc时需要提前换nowSymbolTable，但每个表是要setFakeName一次
    
    public void setSymbolFakeName(boolean isBaseTable, HashSet<String> fakeNameSet) {
        // 在这里实现四元式中标记的生成
        // CONST, VAR, PARAM, VOIDFUNC, INTFUNC
        // 对于FUNC，其只会存在于baseRunTable中，为其加前缀 f_
        // 对于CONST、VAR，如果是全局变量，为其加前缀 g_，MIPS生成时放在.data段中，定义全局变量时不需要关心重不重名
        // CONST、VAR、PARAM，考虑到某个ident名字为g_a、f_a这种以g_、f_ 为前缀的可能，为了方便统一为其加前缀^
        // 根据以上设计，可以保证第0层的符号表(全局变量与函数)互不重名，第1层的符号表(CONST、VAR、PARAM)必然不与第0层的重名(前缀)
        // 整个分析过程中，还有的label就是要输出的字符串，那个的前缀为str_x，且无需进入符号表中，放在.data段，不用管
        // 还一个就是temp变量，那个的前缀为t，其与以上的所有变量之间也一定是互斥的
        if (hasFinishSetSymbolFakeName) {
            return; //已经set过了，因为函数的形参，需要提前设一次符号表的fakeName
        }
        int symbolType;
        if (isBaseTable) {
            for (Symbol nowSymbol : symbolMap.values()) {
                symbolType = nowSymbol.getType();
                if (symbolType == SymbolConst.INT_FUNC_SYMBOL || symbolType == SymbolConst.VOID_FUNC_SYMBOL) {
                    nowSymbol.setFakeName("f_" + nowSymbol.getName());
                } else {
                    nowSymbol.setFakeName("g_" + nowSymbol.getName());
                }
            }
        } else {
            for (Symbol nowSymbol : symbolMap.values()) {
                symbolType = nowSymbol.getType();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("^");
                stringBuilder.append(nowSymbol.getName());
                while (true) {
                    String newName = stringBuilder.toString();
                    if (fakeNameSet.contains(newName)) {    // 重名了，在后面加^
                        stringBuilder.append("^");
                    } else {    // 没有重名，设置其为fakeName
                        nowSymbol.setFakeName(newName);
                        fakeNameSet.add(newName);
                        break;
                    }
                }
            }
        }
        hasFinishSetSymbolFakeName = true;
        
    }
    
    // 代码生成阶段使用，变量一定得要是alive的
    public String getAliveFakeName(String identName) {
        Symbol symbol = findAliveSymbol(identName);
        if (symbol == null) {
            System.out.println("inGetFakeName, symbol=null, identName=" + identName);
        }
        return symbol.getFakeName();
    }
    
    public String getFakeName_NoCareAlive(String identName) {
        Symbol symbol = findSymbol(identName);
        if (symbol == null) {
            System.out.println("inGetFakeName, symbol=null, identName=" + identName);
        }
        return symbol.getFakeName();
    }
    
    // 这个函数在数组定义之始就要调用，当时的变量没有被激活
    public void setArrayUpBound_noCareAlive(String identName, int index, int size) {
        Symbol symbol = findSymbol(identName);  // 数组定义一开始的时候就要调用，当时还没有setAlive
        symbol.setArrayUpBound(index, size);
    }
    
    public Symbol findAliveSymbol(String identName) {
        if (symbolMap.containsKey(identName) && symbolMap.get(identName).isAlive()) {
            return symbolMap.get(identName);
        }
        if (faTable == null) {  // 返回null说明没有找到
            return null;
        }
        return faTable.findAliveSymbol(identName);
    }
    
    public Symbol findFuncSymbol(String identName) {
        if (symbolMap.containsKey(identName) &&
                (symbolMap.get(identName).getType() == SymbolConst.VOID_FUNC_SYMBOL ||
                        symbolMap.get(identName).getType() == SymbolConst.INT_FUNC_SYMBOL)) {
            return symbolMap.get(identName);
        }
        if (faTable == null) {  // 返回null说明没有找到
            return null;
        }
        return faTable.findFuncSymbol(identName);
    }
    
    public void printForDebug(PrintStream printStream) {
        for (Symbol symbol : symbolMap.values()) {
            printStream.println(symbol);
        }
        if (faTable != null) {
            faTable.printForDebug(printStream);
        }
    }
    
    public SymbolTable(SymbolTable faTable, int blockType, ErrorHandler errorHandler, int level) {
        this.level = level;
        this.blockType = blockType;
        this.faTable = faTable;
        this.errorHandler = errorHandler;
        this.symbolMap = new HashMap<>();
        hasFinishSetSymbolFakeName = false;
    }
    
    public SymbolTable(SymbolTable faTable, int blockType, ErrorHandler errorHandler) {
        this.faTable = faTable;
        this.blockType = blockType;
        this.errorHandler = errorHandler;
        this.level = faTable.getLevel() + 1;
        this.symbolMap = new HashMap<>();
        hasFinishSetSymbolFakeName = false;
    }
    
    public int getAliveConstVal(String ident) {
        // 四元式生成阶段使用，找的是alive变量的值
        Symbol symbol = findAliveSymbol(ident);
        return symbol.getVal();
    }
    
    ////////////////////////////////////////////////// 功能函数
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    //////////////////////////////////////////////////
    public void addSymbolAndCheckErrorReName(Symbol newSymbol) {
        if (symbolMap.containsKey(newSymbol.getName())) { // 出现重名
            errorHandler.raise_B_Rename(newSymbol.getLineNum());
        } else {
            symbolMap.put(newSymbol.getName(), newSymbol);
        }
    }
    
    public Symbol findSymbol(String name) {
        if (symbolMap.containsKey(name)) {
            return symbolMap.get(name);
        }
        if (faTable == null) {  // 返回null说明没有找到
            return null;
        }
        return faTable.findSymbol(name);
    }
    
    public void setAliveIdentVal(String identName, int val) {
        Symbol symbol = findAliveSymbol(identName);
        symbol.setVal(val);
    }
    
    public boolean isAliveIdentConst(String ident) {
        // 这个函数用于代码生成阶段，要看的是alive的变量
        if (!isIdentExist(ident)) {
            System.out.println("在查ident是不是const时，有个ident是不存在的");
            return false;
        }
        Symbol symbol = findAliveSymbol(ident);
        return symbol.getType() == SymbolConst.CONST_SYMBOL;
    }

    
    public boolean isIdentExist(TokenDate sym) {
        if (symbolMap.containsKey(sym.getName())) {
            return true;
        }
        if (faTable == null) { // 没法往上找了
            return false;
        }
        return faTable.isIdentExist(sym);   // 往上找
    }
    
    public boolean isIdentExist(String identName) {
        if (symbolMap.containsKey(identName)) {
            return true;
        }
        if (faTable == null) { // 没法往上找了
            return false;
        }
        return faTable.isIdentExist(identName);   // 往上找
    }
    
    public boolean isInVoidFunc() {
        if (this.blockType == SymbolConst.VOID_FUNC_BLOCK) {
            return true;
        }
        if (faTable == null) {
            return false;
        }
        return faTable.isInVoidFunc();
    }
    
    /////////////////////////////////// gets and sets
    public int getLevel() {
        return level;
    }
    
    ///////////////////////////////////////////////// gets and sets
    /////////////////////////////////////////////////
    public HashMap<String, Symbol> getSymbolMap() {
        return symbolMap;
    }
    
    public SymbolTable getFaTable() {
        return faTable;
    }
    
    public int getBlockType() {
        return blockType;
    }
    
    public int getLastStmtType() {
        return lastStmtType;
    }
    
    public void setLastStmtType(int lastStmtType) {
        this.lastStmtType = lastStmtType;
    }
}
