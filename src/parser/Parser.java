package parser;

import constDecl.NodeConst;
import constDecl.SymbolConst;
import errorHandler.ErrorHandler;
import lexical.TokenDate;
import symbolTable.Symbol;
import symbolTable.SymbolTable;

import java.io.PrintStream;
import java.util.ArrayList;

public class Parser {
    private int tokenTotalNum;
    private final ArrayList<TokenDate> tokenDateList;
    private int indexOfToken;
    private TokenDate sym;
    private TreeNode treeRoot;
    
    // 错误处理部分
    private final ParserTools parserTools;
    private final ErrorHandler errorHandler;
    private SymbolTable nowSymbolTable;
    private SymbolTable baseSymbolTable;
    private int newBlockType;
    private int lValType;       // 主要是为了看是不是在对const进行修改
    private int lValLineNum;
    private int loopLevel;
    private int newFuncType;
    private ArrayList<Symbol> calledFuncSymbolList; // 如果函数调用时的某个参数是 调用函数的返回值，此时需要保存当前的calledFuncSymbol
    private Symbol nowCalledFuncSymbol;     // 用于函数调用，需要借助符号表中的信息才可以判断出当前的函数调用的 参数 是否合法
    
    private ArrayList<ArrayList<Integer>> funcRealParaTopStack; // 顶层栈，是栈的栈，用于分析函数调用时的情况
    private ArrayList<Integer> nowFuncRealParaStack; // 实参栈，用于分析某一个函数调用时的参数情况，是funcRealParaTopStack中的栈顶元素，记录实参维数
    private ArrayList<Symbol> symbolFuncParamsNeedAddToTable;   // 等待加入进符号表的函数形参symbol
    
    // 输出流
    private PrintStream outputTxtStream = null;
    private final PrintStream sysOutStream;
    private PrintStream errorStream;
    
    // 随时可删，用于个人debug
    private boolean needPrintForDebug = false;
    
    public Parser(ArrayList<TokenDate> tokenDateList) {
        this.tokenDateList = tokenDateList;
        errorHandler = new ErrorHandler();
        parserTools = new ParserTools(tokenDateList);
        
        // 修改输出
        sysOutStream = System.out;
        try {
            outputTxtStream = new PrintStream("output.txt");
            errorStream = new PrintStream("error.txt");
        } catch (Exception e) {
            System.out.println("修改输出失败");
            System.exit(1);
        }
    }
    
    public void startToParse() {
        // 初始化工作
        indexOfToken = -1;
        tokenTotalNum = tokenDateList.size();
        treeRoot = new TreeNode("root", null);  // 这样写完全是因为函数接口原因
        newBlockType = SymbolConst.NORMAL_BLOCK;
        baseSymbolTable = new SymbolTable(null, newBlockType, errorHandler, 0); // 初始表的默认深度为0
        nowSymbolTable = baseSymbolTable;
        lValType = SymbolConst.VAR_SYMBOL;
        lValLineNum = 0;
        loopLevel = 0;
        symbolFuncParamsNeedAddToTable = new ArrayList<>();
        funcRealParaTopStack = new ArrayList<>();
        calledFuncSymbolList = new ArrayList<>();
        
        // 为了debug
        if (needPrintForDebug) {
            parserTools.printAllToken();
        }
        
        // 开始语法分析
        getSym();
        parseCompUnit(treeRoot);
        treeRoot = treeRoot.getChildList().get(0);
        
        // 输出 语法分析 和 错误处理 的结果
        parserTools.postOrderPrintTheTree(treeRoot, outputTxtStream);
        errorHandler.printErrorMessage(errorStream);
        
    }
    
    ///////////////////////////////////////////////// 以下为功能实现的主要部分
    
    public TreeNode getTreeRoot() {
        return treeRoot;
    }
    
    public SymbolTable getBaseSymbolTable() {
        return baseSymbolTable;
    }
    
    private void getSym() {
        indexOfToken++; // 想清楚边界要怎么处理？ 对于正确的程序，无需进行处理
        if (indexOfToken >= tokenTotalNum) {
            sym = new TokenDate("end", "end", 0);
            return;
        }
        sym = tokenDateList.get(indexOfToken);
        if (needPrintForDebug) {
            sysOutStream.println("   getSym: index = " + indexOfToken + ", sym.name = " + sym.getName() + ", lineNum = " + sym.getLineNum());
        }
    }
    
    private boolean lookAheadIs(int aheadNum, String tokenName) {
        // 使用了TokenDate.isNameEqual() 比较的是其中的name，因此不可用于Ident, IntConst, FormatString
        if (indexOfToken + aheadNum >= tokenTotalNum) {
            return false;
        }
        return tokenDateList.get(indexOfToken + aheadNum).isNameEqual(tokenName);
    }
    
    private boolean lookAheadIsNot(int aheadNum, String tokenName) {
        if (indexOfToken + aheadNum >= tokenTotalNum) {
            return true;
        }
        return !tokenDateList.get(indexOfToken + aheadNum).isNameEqual(tokenName);
    }
    
    private void judgeAndAddChild(TreeNode nowNode, String str) { // 如果遇到需要特殊处理的情况，那就自己写这一段就好了
        if (!sym.isNameEqual(str)) {
            error();
            if (str.equals(")")) {
                // 报错行号为正确情况下 )前面那一个符号的行号
                errorHandler.raise_J_lackSmallRightBracket(tokenDateList.get(indexOfToken - 1).getLineNum());
                indexOfToken--; // 为了让下次getSym时还是当前的结果
                nowNode.addChild(new TreeNode(")", sym));
                return;
            } else if (str.equals("]")) {
                errorHandler.raise_K_lackMiddleRightBracket(tokenDateList.get(indexOfToken - 1).getLineNum());
                indexOfToken--;
                nowNode.addChild(new TreeNode("]", sym));
                return;
            } else if (str.equals(";")) {
                errorHandler.raise_I_lackSemicolon(tokenDateList.get(indexOfToken - 1).getLineNum());
                indexOfToken--;
                nowNode.addChild(new TreeNode(";", sym));
                return;
            }
        }
        // 使用这个函数就是为了非终结符的判断，有了非终结符之后，获取信息都从sym中获取，Node的name就不重要了
        nowNode.addChild(new TreeNode(str, sym));
    }
    
    private void isIdentAndAddChild(TreeNode nowNode) {
        //if (!sym.isIdent()) { error(); }
        nowNode.addChild(new TreeNode(NodeConst.Ident, sym));
    }
    
    private void error() {
    }
    
    private void nowParaStackPush(int newTopData) {
        if (nowFuncRealParaStack == null) { // 为空说明当前没有在对函数进行分析，就是一般的exp
            return;
        }
        nowFuncRealParaStack.add(newTopData);
    }
    
    private void nowParaStackUpdate(int xx) {
        if (nowFuncRealParaStack == null) {
            return;
        }
        nowFuncRealParaStack.set(nowFuncRealParaStack.size() - 1, xx);
    }
    
    ////////////////////////////////////////// 以下为具体某个非终结符的parse
    private void parseCompUnit(TreeNode faNode) {
        TreeNode nowNode = new TreeNode(NodeConst.CompUnit, null);
        while (sym.isNameEqual("const") ||
                (sym.isNameEqual("int") && lookAheadIsNot(2, "("))) {   // 主要是为了跟FuncDef区分开，都可以以int开头
            parseDecl(nowNode);
        }
        while (sym.isNameEqual("void") ||
                (sym.isNameEqual("int") && lookAheadIs(2, "(") && lookAheadIsNot(1, "main"))) {
            parseFuncDef(nowNode);
        }
        parseMainFuncDef(nowNode);
        faNode.addChild(nowNode);
    }
    
    private void parseDecl(TreeNode faNode) {
        TreeNode nowNode = new TreeNode(NodeConst.Decl, null);
        if (sym.isNameEqual("const")) {
            parseConstDecl(nowNode);
        } else {
            parseVarDecl(nowNode);
        }
        if (needPrintForDebug) {
            nowSymbolTable.printForDebug(sysOutStream);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseConstDecl(TreeNode faNode) {
        // 常量声明 ConstDecl → 'const' BType ConstDef { ',' ConstDef } ';'
        // 1.花括号内重 复0次 2.花括号内重复多次
        TreeNode nowNode = new TreeNode(NodeConst.ConstDecl, null);
        judgeAndAddChild(nowNode, "const");
        getSym();
        parseBType(nowNode);
        parseConstDef(nowNode);
        while (sym.isNameEqual(",")) {
            nowNode.addChild(new TreeNode(",", sym));
            getSym();
            parseConstDef(nowNode);
        }
        judgeAndAddChild(nowNode, ";");
        getSym();
        faNode.addChild(nowNode);
    }
    
    private void parseBType(TreeNode faNode) {
        TreeNode nowNode = new TreeNode("BType", null);
        // if (!sym.isNameEqual("int")) { error(); }
        judgeAndAddChild(nowNode, "int");
        getSym();
        faNode.addChild(nowNode);
    }
    
    private void parseConstDef(TreeNode faNode) {
        // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
        // 包含普通变量、一维数组、二维数组共三种情况
        int thisDim = 0;
        TreeNode nowNode = new TreeNode(NodeConst.ConstDef, null);
        // if (!sym.isIdent()) { error(); }
        nowNode.addChild(new TreeNode(NodeConst.Ident, sym));
        Symbol nowSymbol = new Symbol(sym.getName(), sym.getLineNum(), SymbolConst.CONST_SYMBOL);
        getSym();
        while (sym.isNameEqual("[")) {
            thisDim++;
            judgeAndAddChild(nowNode, "[");
            getSym();
            parseConstExp(nowNode);
            judgeAndAddChild(nowNode, "]");
            getSym();
        }
        judgeAndAddChild(nowNode, "=");
        getSym();
        parseConstInitVal(nowNode);
        // 填符号表
        nowSymbol.setDim(thisDim);
        nowSymbolTable.addSymbolAndCheckErrorReName(nowSymbol); // 符号表中新增变量
        faNode.addChild(nowNode);
    }
    
    private void parseConstInitVal(TreeNode faNode) {
        TreeNode nowNode = new TreeNode("ConstInitVal", null);
        if (sym.isNameEqual("{")) {
            judgeAndAddChild(nowNode, "{");
            getSym();
            if (sym.isNameEqual("}")) { // 对应大括号中为空的情况
                judgeAndAddChild(nowNode, "}");
                getSym(); // 直接就结束了
            } else { // 大括号中有值
                parseConstInitVal(nowNode);
                while (sym.isNameEqual(",")) {
                    judgeAndAddChild(nowNode, ",");
                    getSym();
                    parseConstInitVal(nowNode);
                }
                judgeAndAddChild(nowNode, "}");
                getSym();
            }
        } else {
            parseConstExp(nowNode);
        }
        
        faNode.addChild(nowNode);
    }
    
    private void parseVarDecl(TreeNode faNode) {
        //VarDecl → BType VarDef { ',' VarDef } ';'
        TreeNode nowNode = new TreeNode("VarDecl", null);
        parseBType(nowNode);
        parseVarDef(nowNode);
        
        while (sym.isNameEqual(",")) {
            judgeAndAddChild(nowNode, ",");
            getSym();
            parseVarDef(nowNode);
        }
        judgeAndAddChild(nowNode, ";");
        getSym();
        faNode.addChild(nowNode);
    }
    
    private void parseVarDef(TreeNode faNode) {
        // VarDef → Ident { '[' ConstExp ']' }              // 包含普通变量、一维数组、二维数组定义
        //        | Ident { '[' ConstExp ']' } '=' InitVal
        TreeNode nowNode = new TreeNode("VarDef", null);
        isIdentAndAddChild(nowNode);
        Symbol nowSymbol = new Symbol(sym.getName(), sym.getLineNum(), SymbolConst.VAR_SYMBOL);
        int nowDim = 0;
        getSym();
        while (sym.isNameEqual("[")) {
            nowDim++;
            judgeAndAddChild(nowNode, "[");
            getSym();
            parseConstExp(nowNode);
            judgeAndAddChild(nowNode, "]");
            getSym();
        }
        
        if (sym.isNameEqual("=")) {
            judgeAndAddChild(nowNode, "=");
            getSym();
            parseInitVal(nowNode);
        }
        
        nowSymbol.setDim(nowDim);
        nowSymbolTable.addSymbolAndCheckErrorReName(nowSymbol);
        faNode.addChild(nowNode);
    }
    
    private void parseInitVal(TreeNode faNode) {
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        // 1.表达式初值 2.一 维数组初值 3.二维数组初值
        TreeNode nowNode = new TreeNode("InitVal", null);
        
        if (sym.isNameEqual("{")) {
            judgeAndAddChild(nowNode, "{");
            getSym();
            if (sym.isNameEqual("}")) { // 大括号内为空
                judgeAndAddChild(nowNode, "}");
                getSym();
            } else {
                parseInitVal(nowNode);
                while (sym.isNameEqual(",")) {
                    judgeAndAddChild(nowNode, ",");
                    getSym();
                    parseInitVal(nowNode);
                }
                judgeAndAddChild(nowNode, "}");
                getSym();
            }
        } else {
            parseExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    /////////////////////////////////////////////////// 进入函数部分
    private void parseFuncDef(TreeNode faNode) {
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        // 1.无形参 2.有形 参
        TreeNode nowNode = new TreeNode(NodeConst.FuncDef, null);
        parseFuncType(nowNode);     // 其会更新全局变量funcType
        // if (!sym.isIdent()) { error(); }
        nowNode.addChild(new TreeNode(NodeConst.Ident, sym));
        Symbol funcSymbol = new Symbol(sym.getName(), sym.getLineNum(), newFuncType, 0);
        nowSymbolTable.addSymbolAndCheckErrorReName(funcSymbol);
        getSym();
        judgeAndAddChild(nowNode, "(");
        getSym();
        
        // FIRST(FuncFParams) = FIRST(FuncFParam) = FIRST(BType) = 'int'，说明有参数
        // 同时，FuncFParams后面两个终结符要求是 ')' 和 '{' ， 右括号可能缺，但是左大括号不会缺，正确代码后面不会是int
        if (sym.isNameEqual("int")) {
            parseFuncFParams(nowNode);
            // 若有参数，需要将参数的维度信息写到函数名的符号项中
            funcSymbol.funcGetParaDim(symbolFuncParamsNeedAddToTable);
            // 有参数，要加到下一层的符号表中
        }
        judgeAndAddChild(nowNode, ")");
        getSym();
        if (needPrintForDebug) {
            nowSymbolTable.printForDebug(sysOutStream);
        }
        parseBlock(nowNode);
        faNode.addChild(nowNode);
    }
    
    private void parseMainFuncDef(TreeNode faNode) {
        TreeNode nowNode = new TreeNode("MainFuncDef", null);
        judgeAndAddChild(nowNode, "int");
        getSym();
        judgeAndAddChild(nowNode, "main");
        getSym();
        judgeAndAddChild(nowNode, "(");
        getSym();
        judgeAndAddChild(nowNode, ")");
        getSym();
        newBlockType = SymbolConst.INT_FUNC_BLOCK;
        parseBlock(nowNode);
        faNode.addChild(nowNode);
    }
    
    
    private void parseFuncType(TreeNode faNode) {
        // 函数类型 FuncType → 'void' | 'int' // 覆盖两种类型的函数
        TreeNode nowNode = new TreeNode("FuncType", null);
        
        if (sym.isNameEqual("void")) {
            judgeAndAddChild(nowNode, "void");
            newFuncType = SymbolConst.VOID_FUNC_SYMBOL;
            newBlockType = SymbolConst.VOID_FUNC_BLOCK;
            getSym();
        } else {
            judgeAndAddChild(nowNode, "int");
            newFuncType = SymbolConst.INT_FUNC_SYMBOL;
            newBlockType = SymbolConst.INT_FUNC_BLOCK;
            getSym();
        }
        
        faNode.addChild(nowNode);
    }
    
    private void parseFuncFParams(TreeNode faNode) {
        // 函数形参表 FuncFParams → FuncFParam { ',' FuncFParam }
        // 1.花括号内重复0次 2.花括号 内重复多次
        TreeNode nowNode = new TreeNode(NodeConst.FuncFParams, null);
        parseFuncFParam(nowNode);
        while (sym.isNameEqual(",")) {
            judgeAndAddChild(nowNode, ",");
            getSym();
            parseFuncFParam(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseFuncFParam(TreeNode faNode) {
        // 函数形参 FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        // 1.普通变量 2.一维数组变量 3.二维数组变量
        TreeNode nowNode = new TreeNode("FuncFParam", null);
        parseBType(nowNode);
        if (!sym.isIdent()) {
            error();
        }
        Symbol newSymbol = new Symbol(sym.getName(), sym.getLineNum(), SymbolConst.PARAM_SYMBOL);
        nowNode.addChild(new TreeNode(NodeConst.Ident, sym));
        getSym();
        int nowDim = 0;
        if (sym.isNameEqual("[")) { // 存在文法中方括号括起来的内容
            nowDim++;
            judgeAndAddChild(nowNode, "[");
            getSym();
            judgeAndAddChild(nowNode, "]");
            getSym();
            while (sym.isNameEqual("[")) {
                nowDim++;
                judgeAndAddChild(nowNode, "[");
                getSym();
                parseConstExp(nowNode);
                judgeAndAddChild(nowNode, "]");
                getSym();
            }
        }
        newSymbol.setDim(nowDim);
        symbolFuncParamsNeedAddToTable.add(newSymbol);
        faNode.addChild(nowNode);
    }
    
    private void parseBlock(TreeNode faNode) {
        // 语句块 Block → '{' { BlockItem } '}'
        // 1.花括号内重复0次 2.花括号内重复多次
        TreeNode nowNode = new TreeNode(NodeConst.Block, null);
        nowSymbolTable = new SymbolTable(nowSymbolTable, newBlockType, errorHandler);
        nowNode.setNewTable(nowSymbolTable);    // 用于中间代码生成阶段，使用其获取分析block新建的的符号表
        if (newBlockType == SymbolConst.LOOP_BLOCK) {
            loopLevel++;
        } else if (newBlockType == SymbolConst.INT_FUNC_BLOCK || newBlockType == SymbolConst.VOID_FUNC_BLOCK) {
            for (Symbol paraSymbol : symbolFuncParamsNeedAddToTable) {
                nowSymbolTable.addSymbolAndCheckErrorReName(paraSymbol);
            }
            symbolFuncParamsNeedAddToTable.clear(); // 加完之后要清空
        }
        // 只有函数定义语句和 while语句 会修改newTableType，生效一次后就变回normal_block
        newBlockType = SymbolConst.NORMAL_BLOCK;
        
        judgeAndAddChild(nowNode, "{");
        getSym();
        while (!sym.isNameEqual("}")) {
            parseBlockItem(nowNode);
        }
        judgeAndAddChild(nowNode, "}");
        int rightBigLineNum = sym.getLineNum();
        getSym();
        
        // 在最后再补一些错误处理的内容
        if (nowSymbolTable.getBlockType() == SymbolConst.LOOP_BLOCK) {
            loopLevel--;    // 用于检查当前所处的循环的深度
        } else if (nowSymbolTable.getBlockType() == SymbolConst.INT_FUNC_BLOCK) {
            if (nowSymbolTable.getLastStmtType() != SymbolConst.RETURN_STMT) {  //  仅需区分有无return就可以了
                errorHandler.raise_G_IntFuncNoReturnAtLast(rightBigLineNum);    // 检查int函数的return情况
            }
        }
        nowSymbolTable = nowSymbolTable.getFaTable();   // 弹出这个符号表，在错误处理中不再使用了
        faNode.addChild(nowNode);
    }
    
    private void parseBlockItem(TreeNode faNode) {
        // 语句块项 BlockItem → Decl | Stmt
        // 覆盖两种语句块项
        TreeNode nowNode = new TreeNode("BlockItem", null);
        if (sym.isNameEqual("const") || sym.isNameEqual("int")) {
            parseDecl(nowNode);
        } else {
            parseStmt(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseStmt(TreeNode faNode) {
        /*语句 Stmt → Block
                 | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
                 | 'while' '(' Cond ')' Stmt
                 | 'break' ';'
                 | 'continue' ';'
                 | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
                 | [Exp] ';' //有无Exp两种情况
                 | 'return' [Exp] ';' // 1.有Exp 2.无Exp
                 | LVal '=' 'getint''('')'';'
                 | LVal '=' Exp ';' // 每种类型的语句都要覆盖
         */
        TreeNode nowNode = new TreeNode("Stmt", null);
        
        boolean isReturnStmt = false;
        // 先把能直接判断出的判断完，接下来就是3选1
        if (sym.isNameEqual("{")) {
            // block
            parseBlock(nowNode);
        } else if (sym.isNameEqual("if")) {
            // 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
            judgeAndAddChild(nowNode, "if");
            getSym();
            judgeAndAddChild(nowNode, "(");
            getSym();
            parseCond(nowNode);
            judgeAndAddChild(nowNode, ")");
            getSym();
            parseStmt(nowNode);
            if (sym.isNameEqual("else")) {
                judgeAndAddChild(nowNode, "else");
                getSym();
                parseStmt(nowNode);
            }
        } else if (sym.isNameEqual("while")) {
            // 'while' '(' Cond ')' Stmt
            judgeAndAddChild(nowNode, "while");
            getSym();
            judgeAndAddChild(nowNode, "(");
            getSym();
            parseCond(nowNode);
            judgeAndAddChild(nowNode, ")");
            getSym();
            if (sym.isNameEqual("{")) { // 这说明接下来的是block，否则后面就只是一个普通的语句。
                newBlockType = SymbolConst.LOOP_BLOCK;
                parseStmt(nowNode);
            } else {
                loopLevel++;    // 这里的++ 和 -- 是为了处理2行后的Stmt
                parseStmt(nowNode);
                loopLevel--;
            }
            
        } else if (sym.isNameEqual("break") || sym.isNameEqual("continue")) {
            // 'break' ';' | 'continue' ';'
            if (sym.isNameEqual("break")) {
                judgeAndAddChild(nowNode, "break");
            } else {
                judgeAndAddChild(nowNode, "continue");
            }
            if (loopLevel == 0) {   // 此时不在循环体中，报错
                errorHandler.raise_M_WrongBreakContinue(sym.getLineNum());
            }
            getSym();
            judgeAndAddChild(nowNode, ";");
            getSym();
        } else if (sym.isNameEqual("printf")) {
            // 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
            judgeAndAddChild(nowNode, "printf");
            int savePrintLineNum = sym.getLineNum(); // 保存print的行号，用于错误分析
            getSym();
            judgeAndAddChild(nowNode, "(");
            getSym();
            if (!sym.isFormatString()) {
                error();
            }
            TokenDate formatStringToken = sym; // 暂存，用于错误分析
            errorHandler.check_A_WrongString_AndUpdateFormatString(formatStringToken);
            nowNode.addChild(new TreeNode("FormatString", sym));
            getSym();
            int printExpNum = 0;
            while (sym.isNameEqual(",")) {
                judgeAndAddChild(nowNode, ",");
                getSym();
                parseExp(nowNode);
                printExpNum++;
            }
            // 增加判断数目是否匹配
            errorHandler.check_L_UnMatchFormatString(savePrintLineNum, formatStringToken, printExpNum);
            
            judgeAndAddChild(nowNode, ")");
            getSym();
            judgeAndAddChild(nowNode, ";");
            getSym();
        } else if (sym.isNameEqual("return")) {
            // 'return' [Exp] ';' // 1.有Exp 2.无Exp
            judgeAndAddChild(nowNode, "return");
            boolean isInVoid = nowSymbolTable.isInVoidFunc();
            int returnLineNum = sym.getLineNum();
            getSym();
            
            if (parserTools.needParseExp(indexOfToken)) {  // 有exp的return语句
                if (isInVoid) { // 错误处理部分，在void函数里且返回了具体的值
                    errorHandler.raise_F_VoidFuncReturnVal(returnLineNum);
                }
                parseExp(nowNode);
            }
            judgeAndAddChild(nowNode, ";");
            // 错误处理，需要记录int func的block的最后一条语句是不是return
            isReturnStmt = true;
            nowSymbolTable.setLastStmtType(SymbolConst.RETURN_STMT);
            getSym();
        } else if (sym.isNameEqual(";")) { // [Exp]; 中Exp为空的情况
            judgeAndAddChild(nowNode, ";");
            getSym();
        } else {
            // 进行预读，判断是[Exp], LVal = Exp, LVal = getint
            boolean hasEqual = false;   // 这个对应的是单个的=，双等号得到的是false
            boolean hasGetInt = false;
            for (int lookAheadNum = 0; lookAheadNum < tokenTotalNum - indexOfToken; lookAheadNum++) {
                // 值得注意的是，lookAheadNum要从0开始，因为存在直接一个分号的情况
                // 把单一个分号的情况往前提，现在仅考虑存在exp的情况
                if (lookAheadIs(lookAheadNum, ";") ||
                        tokenDateList.get(indexOfToken + lookAheadNum).getLineNum() != sym.getLineNum()) {
                    break;
                }
                if (lookAheadIs(lookAheadNum, "=")) {
                    if (lookAheadIsNot(lookAheadNum + 1, "=")) {
                        hasEqual = true; // 不算==，为了和Cond区分开来
                    }
                    
                }
                if (lookAheadIs(lookAheadNum, "getint")) {
                    hasGetInt = true;
                }
            }
            if (hasEqual && !hasGetInt) {   // <Stmt> -> <LVal> ‘=’ <Exp>‘;’
                // LVal '=' Exp ';' // 每种类型的语句都要覆盖
                parseLVal(nowNode);
                errorHandler.check_H_TryChangeConst(lValType, lValLineNum);
                judgeAndAddChild(nowNode, "=");
                getSym();
                parseExp(nowNode);
                judgeAndAddChild(nowNode, ";");
                getSym();
            } else if (hasEqual && hasGetInt) { // <Stmt> -> <LVal> ‘=’ ‘getint’
                // LVal '=' 'getint''('')'';'
                parseLVal(nowNode);
                errorHandler.check_H_TryChangeConst(lValType, lValLineNum);
                judgeAndAddChild(nowNode, "=");
                getSym();
                judgeAndAddChild(nowNode, "getint");
                getSym();
                judgeAndAddChild(nowNode, "(");
                getSym();
                judgeAndAddChild(nowNode, ")");
                getSym();
                judgeAndAddChild(nowNode, ";");
                getSym();
            } else { // [Exp] ';' 且Exp一定存在
                parseExp(nowNode);
                judgeAndAddChild(nowNode, ";");
                getSym();
            }
        }
        if (!isReturnStmt) {
            nowSymbolTable.setLastStmtType(SymbolConst.NOT_RETURN_STMT); // 不是return语句。
        }
        faNode.addChild(nowNode);
    }
    
    private void parseExp(TreeNode faNode) {
        // 表达式 Exp → AddExp 注：SysY 表达式是int 型表达式 // 存在即可
        TreeNode nowNode = new TreeNode(NodeConst.Exp, null);
        parseAddExp(nowNode);
        faNode.addChild(nowNode);
    }
    
    private void parseCond(TreeNode faNode) {
        // 条件表达式 Cond → LOrExp // 存在即可
        TreeNode nowNode = new TreeNode("Cond", null);
        parseLOrExp(nowNode);
        faNode.addChild(nowNode);
    }
    
    private void parseLVal(TreeNode faNode) {
        // 左值表达式 LVal → Ident {'[' Exp ']'} //1.普通变量 2.一维数组 3.二维数组
        TreeNode nowNode = new TreeNode(NodeConst.LVal, null);
        // if (!sym.isIdent()) { error(); }
        // 进行错误处理，看看是不是未定义的变量
        errorHandler.check_C_UndefineIdent(sym, nowSymbolTable);
        
        // 然后记录当前LVal的维度，考虑当前LVal作为函数的参数
        int valDim;
        Symbol identSymbol = nowSymbolTable.findSymbol(sym.getName());
        if (identSymbol == null) {  // 说明未定义
            lValType = SymbolConst.VAR_SYMBOL; // lValType是为了判断左值是不是const。若未定义，就设为一般变量，即可更改，不报H错
            valDim = -1; // 变量未定义，将其维度定为-1，后面将其处理为函数实参的万能类型，是为了不报函数类型错误
        } else { // 变量之前定义过了
            lValType = identSymbol.getType();
            valDim = identSymbol.getDim();
        }
        lValLineNum = sym.getLineNum();
        
        // 正常的梯度下降分析
        nowNode.addChild(new TreeNode(NodeConst.Ident, sym));
        getSym();
        while (sym.isNameEqual("[")) {
            valDim--;
            judgeAndAddChild(nowNode, "[");
            getSym();
            parseExp(nowNode);
            judgeAndAddChild(nowNode, "]");
            getSym();
        }
        
        // 当前LVal作为函数实参时，确定其维度，保存下来
        if (valDim < 0) {   // 当前LVal未定义，由于一行只报一个错，因此视其匹配了形参
            nowParaStackUpdate(SymbolConst.FIT_ANYTHING_PARA);
        } else {
            nowParaStackUpdate(valDim);
        }
        faNode.addChild(nowNode);
    }
    
    private void parsePrimaryExp(TreeNode faNode) {
        // 基本表达式 PrimaryExp → '(' Exp ')' | LVal | Number // 三种情况均需覆盖
        TreeNode nowNode = new TreeNode("PrimaryExp", null);
        if (sym.isNameEqual("(")) {
            judgeAndAddChild(nowNode, "(");
            getSym();
            parseExp(nowNode);
            judgeAndAddChild(nowNode, ")");
            getSym();
        } else if (sym.isIntConst()) {
            parseNumber(nowNode);
        } else {
            parseLVal(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseNumber(TreeNode faNode) {
        // 数值 Number → IntConst // 存在即可
        TreeNode nowNode = new TreeNode(NodeConst.Number, null);
        // if (!sym.isIntConst()) { error(); }
        nowParaStackUpdate(SymbolConst.NORMAL_PARA);    // 错误处理，实参维度的确定
        nowNode.addChild(new TreeNode(NodeConst.IntConst, sym));  // 语法分析，建立语法树
        getSym();
        faNode.addChild(nowNode);
    }
    
    private void parseUnaryExp(TreeNode faNode) {
        // 一元表达式 UnaryExp → PrimaryExp    // 3种情况均需覆盖, 函数调用也需要覆盖FuncRParams的不同情况
        //                   | Ident '(' [FuncRParams] ')'
        //                   | UnaryOp UnaryExp // 存在即可
        TreeNode nowNode = new TreeNode(NodeConst.UnaryExp, null);
        if (sym.isNameEqual("+") || sym.isNameEqual("-") || sym.isNameEqual("!")) {
            parseUnaryOp(nowNode);
            parseUnaryExp(nowNode);
        } else if (sym.isIdent() && lookAheadIs(1, "(")) {
            // 函数调用的情况   UnaryExp → Ident '(' [FuncRParams] ')'
            // 首先看有无函数未定义的情况
            errorHandler.check_C_UndefineIdent(sym, nowSymbolTable);
            nowCalledFuncSymbol = nowSymbolTable.findSymbol(sym.getName()); // 为null说明函数未定义
            calledFuncSymbolList.add(nowCalledFuncSymbol);  // 入栈保存
            int calledErrorLine = sym.getLineNum();
            
            // 此时函数被调用，考虑此函数是作为一个函数的参数进行调用
            // 接着进行函数实参维度的确定。通过返回值的类型来判断
            if (nowCalledFuncSymbol == null) { // 说明函数未定义，由于一行至多一个错，因此函数未定义时不会发生参数类型不匹配的情况
                nowParaStackUpdate(SymbolConst.FIT_ANYTHING_PARA);  // 认为加的一个万能类型
            } else { // 函数可以被找到，将其返回值放入栈中进行作为某个函数的参数
                if (nowCalledFuncSymbol.getType() == SymbolConst.VOID_FUNC_SYMBOL) {
                    nowParaStackUpdate(SymbolConst.VOID_FUNC_PARA); // 若现在没有在进行函数参数分析，这步相当于没做
                } else if (nowCalledFuncSymbol.getType() == SymbolConst.INT_FUNC_SYMBOL) {
                    nowParaStackUpdate(SymbolConst.NORMAL_PARA);
                }
            }
            
            // 如果calledFuncSymbol为null说明没有找到函数，函数未定义
            // 每次调用函数时，开一个新的栈
            nowFuncRealParaStack = new ArrayList<>();
            funcRealParaTopStack.add(nowFuncRealParaStack);
            
            // 进行正常的函数 递归下降分析
            nowNode.addChild(new TreeNode(NodeConst.Ident, sym));
            getSym();
            judgeAndAddChild(nowNode, "(");
            getSym();
            if (parserTools.isExpFirst(sym)) {
                parseFuncRParams(nowNode);
            }
            judgeAndAddChild(nowNode, ")");
            
            // 进行函数 参数个数 和 参数类型的匹配操作，错误至多一个
            // nowCalledFuncSymbol == null说明函数未定义，直接忽略这一块的错误，不做检查
            if (nowCalledFuncSymbol != null) {
                // 检查 参数个数 和 参数类型 是否匹配，解耦写到errorHandler中了
                errorHandler.check_D_E_FuncNumOrFuncType(nowCalledFuncSymbol, nowFuncRealParaStack, calledErrorLine);
            }
            
            // 当前函数调用结束了，要对函数分析栈进行弹栈操作
            funcRealParaTopStack.remove(funcRealParaTopStack.size() - 1);
            nowFuncRealParaStack = null; // 栈非空的话之后会更新值的，空的话就是null了
            if (!funcRealParaTopStack.isEmpty()) {
                nowFuncRealParaStack = funcRealParaTopStack.get(funcRealParaTopStack.size() - 1);
            }
            
            calledFuncSymbolList.remove(calledFuncSymbolList.size() - 1);
            nowCalledFuncSymbol = null;
            if (!calledFuncSymbolList.isEmpty()) {
                nowCalledFuncSymbol = calledFuncSymbolList.get(calledFuncSymbolList.size() - 1);
            }
            getSym();
        } else {
            parsePrimaryExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseUnaryOp(TreeNode faNode) {
        TreeNode nowNode = new TreeNode(NodeConst.UnaryOp, null);
        if (sym.isNameEqual("+")) {
            judgeAndAddChild(nowNode, "+");
            getSym();
        } else if (sym.isNameEqual("-")) {
            judgeAndAddChild(nowNode, "-");
            getSym();
        } else if (sym.isNameEqual("!")) {
            judgeAndAddChild(nowNode, "!");
            getSym();
        } //else { error(); }
        faNode.addChild(nowNode);
    }
    
    private void parseFuncRParams(TreeNode faNode) {
        // 函数实参表 FuncRParams → Exp { ',' Exp }
        // 1.花括号内重复0次 2.花括号内重复多次 3. Exp需要覆盖数组传参和部分数组传参
        TreeNode nowNode = new TreeNode(NodeConst.FuncRParams, null);
        nowParaStackPush(-1); // 用于函数匹配，若不在进行函数分析(now=null)，会自动无效
        parseExp(nowNode);
        while (sym.isNameEqual(",")) {
            judgeAndAddChild(nowNode, ",");
            getSym();
            nowParaStackPush(-1);
            parseExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    //////////////////////////////////////////// 以下代码存在左递归的问题
    private void parseMulExp(TreeNode faNode) {
        // 乘除模表达式 MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp  // 1.UnaryExp 2.* 3./ 4.% 均需覆盖
        TreeNode nowNode = new TreeNode(NodeConst.MulExp, null);
        TreeNode childNode;
        parseUnaryExp(nowNode);
        while (sym.isNameEqual("*") || sym.isNameEqual("/") || sym.isNameEqual("%")) {
            childNode = nowNode;
            nowNode = new TreeNode(NodeConst.MulExp, null);
            nowNode.addChild(childNode);
            if (sym.isNameEqual("*")) {
                judgeAndAddChild(nowNode, "*");
            } else if (sym.isNameEqual("/")) {
                judgeAndAddChild(nowNode, "/");
            } else {
                judgeAndAddChild(nowNode, "%");
            }
            getSym();
            parseUnaryExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseAddExp(TreeNode faNode) {
        // 加减表达式 AddExp → MulExp | AddExp ('+' | '−') MulExp  // 1.MulExp 2.+ 需覆盖 3.- 需覆盖
        TreeNode nowNode = new TreeNode(NodeConst.AddExp, null);
        TreeNode childNode;
        // 取巧，函数参数维度的记录。多个mulExp之间不会出现类型不同的情况，因此不用对不同MulExp之间的运算进行分析，只要得到任意一个mulExp的类型就ok
        parseMulExp(nowNode);
        while (sym.isNameEqual("+") || sym.isNameEqual("-")) {
            childNode = nowNode;
            nowNode = new TreeNode(NodeConst.AddExp, null);
            nowNode.addChild(childNode);
            if (sym.isNameEqual("+")) {
                judgeAndAddChild(nowNode, "+");
            } else {
                judgeAndAddChild(nowNode, "-");
            }
            getSym();
            parseMulExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseRelExp(TreeNode faNode) {
        // 关系表达式 RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp // 1.AddExp 2.< 3.> 4.<= 5.>= 均需覆盖
        TreeNode nowNode = new TreeNode(NodeConst.RelExp, null);
        TreeNode childNode;
        parseAddExp(nowNode);
        while (sym.isNameEqual("<") || sym.isNameEqual(">") || sym.isNameEqual("<=") || sym.isNameEqual(">=")) {
            childNode = nowNode;
            nowNode = new TreeNode(NodeConst.RelExp, null);
            nowNode.addChild(childNode);
            if (sym.isNameEqual("<")) {
                judgeAndAddChild(nowNode, "<");
            } else if (sym.isNameEqual(">")) {
                judgeAndAddChild(nowNode, ">");
            } else if (sym.isNameEqual("<=")) {
                judgeAndAddChild(nowNode, "<=");
            } else {
                judgeAndAddChild(nowNode, ">=");
            }
            getSym();
            parseAddExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseEqExp(TreeNode faNode) {
        // 相等性表达式 EqExp → RelExp | EqExp ('==' | '!=') RelExp   // 1.RelExp 2.== 3.!= 均 需覆盖
        TreeNode nowNode = new TreeNode(NodeConst.EqExp, null);
        TreeNode childNode;
        parseRelExp(nowNode);
        while (sym.isNameEqual("==") || sym.isNameEqual("!=")) {
            childNode = nowNode;
            nowNode = new TreeNode(NodeConst.EqExp, null);
            nowNode.addChild(childNode);
            if (sym.isNameEqual("==")) {
                judgeAndAddChild(nowNode, "==");
            } else {
                judgeAndAddChild(nowNode, "!=");
            }
            getSym();
            parseRelExp(nowNode);
        }
        
        faNode.addChild(nowNode);
    }
    
    private void parseLAndExp(TreeNode faNode) {
        // 逻辑与表达式 LAndExp → EqExp | LAndExp '&&' EqExp // 1.EqExp 2.&& 均需覆盖
        TreeNode nowNode = new TreeNode(NodeConst.LAndExp, null);
        TreeNode childNode;
        parseEqExp(nowNode);
        while (sym.isNameEqual("&&")) {
            childNode = nowNode;
            nowNode = new TreeNode(NodeConst.LAndExp, null);
            nowNode.addChild(childNode);
            judgeAndAddChild(nowNode, "&&");
            getSym();
            parseEqExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseLOrExp(TreeNode faNode) {
        // 逻辑或表达式 LOrExp → LAndExp | LOrExp '||' LAndExp
        // 1.LAndExp 2.|| 均需覆盖
        TreeNode nowNode = new TreeNode(NodeConst.LOrExp, null);
        TreeNode childNode;
        parseLAndExp(nowNode);
        while (sym.isNameEqual("||")) {
            childNode = nowNode;
            nowNode = new TreeNode(NodeConst.LOrExp, null);
            nowNode.addChild(childNode);
            judgeAndAddChild(nowNode, "||");
            getSym();
            parseLAndExp(nowNode);
        }
        faNode.addChild(nowNode);
    }
    
    private void parseConstExp(TreeNode faNode) {
        TreeNode nowNode = new TreeNode(NodeConst.ConstExp, null);
        parseAddExp(nowNode);
        faNode.addChild(nowNode);
    }
}
