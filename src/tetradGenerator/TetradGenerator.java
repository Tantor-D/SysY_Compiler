package tetradGenerator;

import constDecl.NodeConst;
import constDecl.SymbolConst;
import lexical.TokenDate;
import parser.TreeNode;
import symbolTable.Symbol;
import symbolTable.SymbolTable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

public class TetradGenerator {
    // 对语法树进行分析，获得中间代码序列
    private ArrayList<Tetrad> tetradList;   // 存储最终的结果
    private ArrayList<String> strToPrintList;      // strList.get(i) -> 最后MIPS中的stri
    private HashSet<String> fakeNameSet;    // 用于生成symbol的fakeName，保证不重名
    
    // 计数器
    private int tempCount = 0;      // 第一个为 temp_1，不会生成temp_0
    private int ifCount = -1;       // 第一个为 if_0, else_0
    private int whileCount = -1;    // 第一个为 loop_0, end_loop_0
    private int lOrCount = -1;      // 用于短路求值来生成跳转
    private int lAndCount = -1;     // 用于短路求值来生成跳转
    private int eqCount = -1;
    private int relCount = -1;
    private int unaryNotCount = -1;
    
    private ArrayList<Integer> loopLabelCountStack;    // 用于break和while，使其可以找到正确的跳转label
    
    private TreeNode rootNode;  // 树的根节点
    private SymbolTable baseSymbolTable;    // 最底层的符号表
    private SymbolTable nowSymbolTable;     // 当前的符号表
    private final PrintStream sysOut = System.out;
    
    
    public TetradGenerator(TreeNode rootNode, SymbolTable baseSymbolTable) {
        this.rootNode = rootNode;
        this.baseSymbolTable = baseSymbolTable;
    }
    
    public void startToGen() {
        // 初始化
        tempCount = 0;  // 第一个为temp_1，不会生成temp_0
        ifCount = -1;
        whileCount = -1;
        tetradList = new ArrayList<>();
        strToPrintList = new ArrayList<>();
        fakeNameSet = new HashSet<>();
        nowSymbolTable = baseSymbolTable;
        nowSymbolTable.setSymbolFakeName(true, fakeNameSet);
        loopLabelCountStack = new ArrayList<>();
        // 开始分析
        checkCompUnit(rootNode);
        
        // 处理完之后，输出得到的信息
        sysOut.println("\n\n---------------------------------------------");
        sysOut.println("teTradList:");
        for (int i = 0; i < tetradList.size(); i++) {
            sysOut.println(i + " " + tetradList.get(i).myToString());
        }
        sysOut.println("---------------------------------------------");
        sysOut.println("strToPrintList:");
        int printCount = 0;
        for (String str : strToPrintList) {
            sysOut.println((printCount++) + " :" + str);
        }
        sysOut.println("---------------------------------------------");
    }
    
    private void genTetrad(Tetrad.OpType opType, String label1, String label2, String des) {
        tetradList.add(new Tetrad(opType, label1, label2, des));
    }
    
    private String genNewTempX() {
        tempCount++;
        return "#t" + Integer.toString(tempCount);  // 加警号，使其与原文法中的ident保持互相异同
    }
    
    private String getNewStrName() {
        return "str" + Integer.toString(strToPrintList.size() - 1);
    }
    
    public ArrayList<Tetrad> getTetradList() {
        return tetradList;
    }
    
    public ArrayList<String> getStrToPrintList() {
        return strToPrintList;
    }
    
    private void assetMesIsNum(Message mes, String str) {
        if (mes.getMesKind() != Message.MesKind.num) {
            sysOut.println("本应传递常数的message没有传递常数!  " + str);
        }
    }
    
    /////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////
    // 以下为具体的check函数
    private void checkCompUnit(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        for (TreeNode childNode : childList) {
            if (childNode.isThisKind(NodeConst.Decl)) {
                checkDecl(childNode);
            } else if (childNode.isThisKind(NodeConst.FuncDef)) {
                checkFuncDef(childNode);
            } else {
                checkMainFuncDef(childNode);
            }
        }
    }
    
    private void checkDecl(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.get(0).isThisKind(NodeConst.ConstDecl)) {
            checkConstDecl(childList.get(0));
        } else {
            checkVarDecl(childList.get(0));
        }
    }
    
    private void checkConstDecl(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        int childNum = nowNode.getChildNum();
        for (int i = 2; i < childNum; i += 2) {
            if (childList.get(i).isThisKind(NodeConst.ConstDef)) {
                checkConstDef(childList.get(i));
            }
        }
    }
    
    private void checkConstDef(TreeNode nowNode) {
        // 对于数组和一般变量，需要分开处理
        ArrayList<TreeNode> childList = nowNode.getChildList();
        String identName = childList.get(0).getTokenDate().getName();
        if (childList.get(1).isThisKind("=")) { // 一般变量，不是数组
            // ConstDef -> Ident '=' ConstInitVal
            Message initMes = checkConstInitVal(childList.get(2));
            String initMesRightStr = initMes.getRightMes(nowSymbolTable);
            nowSymbolTable.findSymbol(identName).setAlive(true);    // 把当前符号表中的这个变量置为alive，要在checkConstIniuVal之后，在生成四元式之前
            genTetrad(Tetrad.OpType.defConst_normal_And_Assign, initMesRightStr, null, nowSymbolTable.getAliveFakeName(identName));
            assetMesIsNum(initMes, "constDef 一般变量的定义");
            // 对于const定义，除了需要进行代码生成的定义，还需要在符号表中记录const变量的值
            nowSymbolTable.setAliveIdentVal(identName, initMes.getNum());
            
        } else if (childList.size() == 6) {
            // 一维数组
            int arraySize = checkConstExp(childList.get(2)).getNum();
            String identFakeName = nowSymbolTable.getFakeName_NoCareAlive(identName);   // 此时还没有把变量设为alive
            nowSymbolTable.setArrayUpBound_noCareAlive(identName, 0, arraySize);
            genTetrad(Tetrad.OpType.defOneDimArray, Integer.toString(arraySize), null, identFakeName);
            checkConstInitVal_oneDim(childList.get(5), nowSymbolTable.findSymbol(identName), 0, identFakeName, arraySize);
            // 做完赋值任务之后才setAlive，因为赋值阶段可能会调用同名的ident
            nowSymbolTable.findSymbol(identName).setAlive(true);
            
        } else {
            // 二维数组
            int arraySize1 = checkConstExp(childList.get(2)).getNum();
            int arraySize2 = checkConstExp(childList.get(5)).getNum();
            String identFakeName = nowSymbolTable.getFakeName_NoCareAlive(identName);
            nowSymbolTable.setArrayUpBound_noCareAlive(identName, 0, arraySize1);
            nowSymbolTable.setArrayUpBound_noCareAlive(identName, 1, arraySize2);
            genTetrad(Tetrad.OpType.defTwoDimArray, Integer.toString(arraySize1), Integer.toString(arraySize2), identFakeName);
            checkConstInitVal_twoDim(childList.get(8), nowSymbolTable.findSymbol(identName), identFakeName, arraySize1, arraySize2);
            // 做完赋值任务之后才setAlive，因为赋值阶段可能会调用同名的ident
            nowSymbolTable.findSymbol(identName).setAlive(true);
        }
    }
    
    private void checkVarDecl(TreeNode nowNode) {
        // VarDecl -> Btype VarDef { ',' VarDef}
        ArrayList<TreeNode> childList = nowNode.getChildList();
        for (int i = 1; i < childList.size(); i += 2) {
            checkVarDef(childList.get(i));
        }
    }
    
    private void checkVarDef(TreeNode nowNode) {
        // VarDef → Ident { '[' ConstExp ']' }
        //        | Ident { '[' ConstExp ']' } '=' InitVal
        ArrayList<TreeNode> childList = nowNode.getChildList();
        String identName = childList.get(0).getTokenDate().getName();
        String identFakeName = nowSymbolTable.getFakeName_NoCareAlive(identName);
        if (childList.size() == 1 || childList.get(1).isThisKind("=")) {
            // 一般变量
            if (childList.size() > 1) { // 有赋值语句
                // VarDef -> Ident = InitVal
                Message iniMes = checkInitVal(childList.get(2));
                String iniRightMesStr = iniMes.getRightMes(nowSymbolTable);
                nowSymbolTable.findSymbol(identName).setAlive(true); // 要在分析InitVal之后
                genTetrad(Tetrad.OpType.defVar_normal_AndAssign, iniRightMesStr, null, identFakeName);
                //genTetrad(Tetrad.OpType.defVarAndAssign_normal, iniMes.getRightMes(nowSymbolTable), null, nowSymbolTable.getAliveFakeName(identName));
            } else { // 没有赋值语句
                // VarDef -> Ident
                nowSymbolTable.findSymbol(identName).setAlive(true);
                genTetrad(Tetrad.OpType.defVar_normal, null, null, identFakeName);
            }
        } else if (childList.size() == 4 || childList.size() == 6) {
            // 一维数组
            int arraySize = checkConstExp(childList.get(2)).getNum();   // 获取一维数组大小
            nowSymbolTable.setArrayUpBound_noCareAlive(identName, 0, arraySize);    // 将数组大小记入符号表中
            genTetrad(Tetrad.OpType.defOneDimArray, Integer.toString(arraySize), null, identFakeName);  // 生成定义数组的四元式
            if (childList.size() == 6) { // 存在赋值语句
                checkInitVal_oneDim(childList.get(5), 0, identFakeName, arraySize);
            }
            nowSymbolTable.findSymbol(identName).setAlive(true);
        } else {
            // 二维数组
            int arraySize1 = checkConstExp(childList.get(2)).getNum();
            int arraySize2 = checkConstExp(childList.get(5)).getNum();
            nowSymbolTable.setArrayUpBound_noCareAlive(identName, 0, arraySize1);
            nowSymbolTable.setArrayUpBound_noCareAlive(identName, 1, arraySize2);
            genTetrad(Tetrad.OpType.defTwoDimArray, Integer.toString(arraySize1), Integer.toString(arraySize2), identFakeName);
            if (childList.size() > 7) {
                // 存在赋值语句
                checkInitVal_twoDim(childList.get(8), identFakeName, arraySize1, arraySize2);
            }
            nowSymbolTable.findSymbol(identName).setAlive(true);
        }
    }
    
    private void checkFuncDef(TreeNode nowNode) {
        // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
        ArrayList<TreeNode> childList = nowNode.getChildList();
        String funcType = checkFuncType(childList.get(0));
        String funcName = childList.get(1).getTokenDate().getName();
        nowSymbolTable.findSymbol(funcName).setAlive(true);
        genTetrad(Tetrad.OpType.defFunc, funcType, null, nowSymbolTable.getAliveFakeName(funcName));
        if (childList.get(3).isThisKind(NodeConst.FuncFParams)) {
            // 此处需要提前修改nowSymbolTable
            nowSymbolTable = childList.get(5).getNewTable();
            nowSymbolTable.setSymbolFakeName(false, fakeNameSet);
            checkFuncFParams(childList.get(3));
            checkBlock(childList.get(5));
        } else {
            checkBlock(childList.get(4));
        }
    }
    
    private void checkMainFuncDef(TreeNode nowNode) {
        genTetrad(Tetrad.OpType.defFunc, "int", null, "f_main");
        checkBlock(nowNode.getChildList().get(4));
    }
    
    private String checkFuncType(TreeNode nowNode) {
        return nowNode.getName(); // void 或 int
    }
    
    private void checkFuncFParams(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        for (int i = 0; i < childList.size(); i += 2) {
            if (childList.get(i).isThisKind(NodeConst.FuncFParam)) {
                checkFuncFParam(childList.get(i));
            }
        }
    }
    
    private void checkFuncFParam(TreeNode nowNode) {
        // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        ArrayList<TreeNode> childList = nowNode.getChildList();
        String identName = childList.get(1).getTokenDate().getName();
        if (childList.size() == 2) {    // 常规变量
            nowSymbolTable.findSymbol(identName).setAlive(true);
            genTetrad(Tetrad.OpType.defFuncPara_normal, null, null, nowSymbolTable.getAliveFakeName(identName));
        } else if (childList.size() == 4) {
            // 一维数组
            nowSymbolTable.findSymbol(identName).setAlive(true);
            genTetrad(Tetrad.OpType.defFuncPara_oneDim, null, null, nowSymbolTable.getAliveFakeName(identName));
        } else {
            // 二维数组
            Message mes = checkConstExp(childList.get(5));
            if (mes.getMesKind() != Message.MesKind.num) {
                sysOut.println("FuncFParam这里没有计算出正确的constExp");
            }
            assetMesIsNum(mes, "函数参数，二维数组第二维不是num");
            nowSymbolTable.setArrayUpBound_noCareAlive(identName, 1, mes.getNum()); // 这个函数并不要求变量是alive的
            nowSymbolTable.findSymbol(identName).setAlive(true);    // 最后才setAlive，因为二维数组第二维的大小涉及到了计算，要先算完才setAlive
            genTetrad(Tetrad.OpType.defFuncPara_twoDim, mes.getRightMes(nowSymbolTable), null, nowSymbolTable.getAliveFakeName(identName));
        }
    }
    
    private void checkBlock(TreeNode nowNode) {
        // Block → '{' { BlockItem } '}'
        ArrayList<TreeNode> childList = nowNode.getChildList();
        // 首先更新符号表
        nowSymbolTable = nowNode.getNewTable();
        nowSymbolTable.setSymbolFakeName(false, fakeNameSet);
        // 递归下降
        for (TreeNode childNode : childList) {
            if (childNode.isThisKind(NodeConst.BlockItem)) {
                checkBlockItem(childNode);
            }
        }
        // 检查void函数块的最后一句是不是void
        if (nowSymbolTable.getBlockType() == SymbolConst.VOID_FUNC_BLOCK) {
            boolean isNoReturn = false;
            if (childList.size() == 2) {    // 空函数
                isNoReturn = true;
            } else {
                TreeNode node = childList.get(childList.size() - 2); // 最后一个blockItem
                node = node.getChildList().get(0);  // blockItem的子节点，stmt 或 decl
                if (!node.isThisKind("Stmt")) {
                    isNoReturn = true;
                } else {
                    node = node.getChildList().get(0);
                    if (!node.isThisKind("return")) {
                        isNoReturn = true;
                    }
                }
            }
            if (isNoReturn) {
                tetradList.add(new Tetrad(Tetrad.OpType.returnVoid, null, null, null));
            }
        }
        // 抛弃当前的符号表
        nowSymbolTable = nowSymbolTable.getFaTable();
    }
    
    private void checkBlockItem(TreeNode nowNode) {
        if (nowNode.getChildList().get(0).isThisKind(NodeConst.Decl)) {
            checkDecl(nowNode.getChildList().get(0));
        } else {
            checkStmt(nowNode.getChildList().get(0));
        }
    }
    
    private void checkStmt(TreeNode nowNode) {
        // Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
        //  | LVal '=' 'getint''('')'';'
        //  | [Exp] ';' //有无Exp两种情况
        //  | Block
        //  | 'return' [Exp] ';' // 1.有Exp 2.无Exp
        //  | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
        //  | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
        //  | 'while' '(' Cond ')' Stmt
        //  | 'break' ';'
        //  | 'continue' ';'
        
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() == 1) {
            if (childList.get(0).isThisKind(NodeConst.Block)) {
                checkBlock(childList.get(0));
            } else if (childList.get(0).isThisKind(";")) {
                return;
            }
        } else if (childList.get(0).isThisKind(NodeConst.Exp) && childList.get(1).isThisKind(";")) {
            checkExp(childList.get(0));
        } else if (childList.get(0).isThisKind(NodeConst.LVal) && childList.get(2).isThisKind(NodeConst.Exp)) {
            // Stmt → LVal '=' Exp ';'
            Message lValMes = checkLValReal(childList.get(0));
            Message expMes = checkExp(childList.get(2));
            if (lValMes.getMesKind() == Message.MesKind.ident_normal) {
                // 一般变量
                genTetrad(Tetrad.OpType.assign, expMes.getRightMes(nowSymbolTable), null, lValMes.getRightMes(nowSymbolTable));
            } else {
                // 数组 (op, offset, val, array)
                // getOffset()这里，在设置offset时，已经getRightMes()过了，所以直接拿就可以了
                // 这是唯一一个在生成四元式的时候，无需采用getRightMes进行转fakeName的地方，其本质上做了一个中转站
                genTetrad(Tetrad.OpType.assignArrayVal, lValMes.getOffset_rightMes(), expMes.getRightMes(nowSymbolTable), lValMes.getRightMes(nowSymbolTable));
            }
        } else if (childList.get(0).isThisKind(NodeConst.LVal) && childList.get(2).isThisKind("getint")) {
            //  Stmt → LVal '=' 'getint''('')'';'
            Message lValMes = checkLValReal(childList.get(0));
            if (lValMes.getMesKind() == Message.MesKind.ident_normal) {
                // 一般变量
                genTetrad(Tetrad.OpType.getint, null, null, lValMes.getRightMes(nowSymbolTable));
            } else {
                // 数组 (op, offset, val, array) // getOffset()这里，在设置offset时，已经getRightMes()过了，所以直接拿就可以了
                String saveTemp = genNewTempX();
                genTetrad(Tetrad.OpType.getint, null, null, saveTemp);
                genTetrad(Tetrad.OpType.assignArrayVal, lValMes.getOffset_rightMes(), saveTemp, lValMes.getRightMes(nowSymbolTable));
            }
        } else if (childList.get(0).isThisKind("return")) {
            // Stmt → 'return' [Exp] ';'
            if (childList.size() == 2) { // 没有返回值
                genTetrad(Tetrad.OpType.returnVoid, null, null, null);
            } else { // 有返回值，此时size = 3
                Message retMes = checkExp(childList.get(1));
                genTetrad(Tetrad.OpType.returnInt, null, null, retMes.getRightMes(nowSymbolTable));
            }
        } else if (childList.get(0).isThisKind("printf")) {
            TokenDate formatStringToken = childList.get(2).getTokenDate();
            int expNum = formatStringToken.getParaNumInFormatString();
            ArrayList<Tetrad> printTetradStack = new ArrayList<>(); // 如果待输出的int是一个函数的返回值，但是函数内部也有输出存在，就不能见一个输出一个
            for (int i = 0; i <= expNum; i++) {
                String strToPrint = formatStringToken.getSubString(i);
                if (strToPrint != null) {   // ==null说明字符串是空串，不管
                    strToPrintList.add(strToPrint);
                    String nowStrLabel = getNewStrName();
                    printTetradStack.add(new Tetrad(Tetrad.OpType.printStr, null, null, nowStrLabel));
                }
                if (i == expNum) {
                    break;
                }
                int index = 4 + i * 2;  // 分析的是要输出的数，这是其对应的index
                Message expMes = checkExp(childList.get(index));
                if (expMes.getMesKind() == Message.MesKind.num) {
                    printTetradStack.add(new Tetrad(Tetrad.OpType.printInt, null, null, expMes.getRightMes(nowSymbolTable)));
                } else {
                    String saveTemp = genNewTempX();    // ident_normal、funcRet、temp可能被后面的信息所影响，导致值得改变，因此要先存下来
                    genTetrad(Tetrad.OpType.assign, expMes.getRightMes(nowSymbolTable), null, saveTemp);
                    printTetradStack.add(new Tetrad(Tetrad.OpType.printInt, null, null, saveTemp));
                }
            }
            // 栈式处理printf要输出的信息，当处理完了之后统一将本次输出的四元式加到四元式列表中
            for (Tetrad tetrad : printTetradStack) {
                tetradList.add(tetrad);
            }
        } else if (childList.get(0).isThisKind("if")) {
            ifCount++;
            String elseLabel1;
            String elseEndLabel = "end_if_" + ifCount;
            if (childList.size() == 5) { // 没有else
                elseLabel1 = "end_if_" + ifCount;
            } else { // 有else的情况
                elseLabel1 = "else_" + ifCount;
            }
            Message mes = checkCond(childList.get(2));  // 根据约定，返回的一定是一个temp
            // 跳过if后的这一段
            genTetrad(Tetrad.OpType.beqz, mes.getRightMes(nowSymbolTable), null, elseLabel1);
            // if后的这一段
            checkStmt(childList.get(4));
            
            if (childList.size() == 5) { // 没有else
                // 直接生成else结束的标志就ok了
                genTetrad(Tetrad.OpType.genLabel, null, null, elseLabel1);
            } else { // 分析else
                // 如果有else的话，如果之前if条件是成立的，首先就要直接跳到最后
                genTetrad(Tetrad.OpType.jump, null, null, elseEndLabel);
                genTetrad(Tetrad.OpType.genLabel, null, null, elseLabel1);
                checkStmt(childList.get(6));
                genTetrad(Tetrad.OpType.genLabel, null, null, elseEndLabel);
            }
        } else if (childList.get(0).isThisKind("while")) {
            whileCount++;
            String loopStartLabel = "loop_" + whileCount;
            String loopEndLabel = "end_loop_" + whileCount;
            loopLabelCountStack.add(whileCount);    // 这是当前的最外层
            genTetrad(Tetrad.OpType.genLabel, null, null, loopStartLabel);
            Message mes = checkCond(childList.get(2));
            genTetrad(Tetrad.OpType.beqz, mes.getRightMes(nowSymbolTable), null, loopEndLabel);
            checkStmt(childList.get(4));
            genTetrad(Tetrad.OpType.jump, null, null, loopStartLabel);
            genTetrad(Tetrad.OpType.genLabel, null, null, loopEndLabel);
            loopLabelCountStack.remove(loopLabelCountStack.size() - 1);   // 这个循环分析完了，移去最外层的循环
        } else if (childList.get(0).isThisKind("break")) {
            genTetrad(Tetrad.OpType.jump, null, null, "end_loop_" + loopLabelCountStack.get(loopLabelCountStack.size() - 1));
        } else if (childList.get(0).isThisKind("continue")) {
            genTetrad(Tetrad.OpType.jump, null, null, "loop_" + loopLabelCountStack.get(loopLabelCountStack.size() - 1));
        }
        
    }
    
    private Message checkConstInitVal(TreeNode nowNode) {
        // 这个函数是为一般变量准备的，后面有多态的函数专门处理数组，其仅仅处理只有一个ConstExp的情况
        // 本身不会生成四元式！
        // ConstInitVal → ConstExp
        //              | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        ArrayList<TreeNode> childList = nowNode.getChildList();
        Message mes = checkConstExp(childList.get(0));
        assetMesIsNum(mes, "constInitVal");
        return mes;
    }
    
    private void checkConstInitVal_oneDim(TreeNode nowNode,
                                          Symbol symbol,
                                          int alreadyOffset,
                                          String arrayFakeName,
                                          int size1) {
        // 可以用于处理一维数组，以及 二维数组赋值时借用
        // symbol是用于在符号表中设置const信息的
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() != size1 * 2 + 1) {
            sysOut.println("一维数组分析时，可能出现问题了");
        }
        for (int i = 0; i < size1; i++) {
            Message mes = checkConstInitVal(childList.get(i * 2 + 1));
            assetMesIsNum(mes, "ConstInitVal 处理一维数组的部分");
            // assignArrayVal   (op, offset, val, array)
            genTetrad(Tetrad.OpType.assignArrayVal,
                    Integer.toString(alreadyOffset + i),
                    mes.getRightMes(nowSymbolTable),
                    arrayFakeName);
            // 这一句负责给符号表中的const数组赋值
            symbol.setArrayVal(alreadyOffset + i, mes.getNum());
        }
    }
    
    private void checkConstInitVal_twoDim(TreeNode nowNode, Symbol symbol, String arrayFakeName, int size1, int size2) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() != size1 * 2 + 1) {
            sysOut.println("二维数组分析时，可能出现问题了");
        }
        for (int i = 0; i < size1; i++) {
            checkConstInitVal_oneDim(childList.get(i * 2 + 1), symbol, size2 * i, arrayFakeName, size2);
        }
    }
    
    private Message checkInitVal(TreeNode nowNode) {
        // InitVal → Exp
        //         | '{' [ InitVal { ',' InitVal } ] '}'
        ArrayList<TreeNode> childList = nowNode.getChildList();
        // normal变量
        return checkExp(childList.get(0));
    }
    
    private void checkInitVal_oneDim(TreeNode nowNode,
                                     int alreadyOffset,
                                     String arrayFakeName,
                                     int arraySize) {
        // InitVal → Exp
        //         | '{' [ InitVal { ',' InitVal } ] '}'
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() != arraySize * 2 + 1) {
            sysOut.println("一维数组分析时，可能出现问题了");
        }
        for (int i = 0; i < arraySize; i++) {
            Message mes = checkInitVal(childList.get(i * 2 + 1));
            genTetrad(Tetrad.OpType.assignArrayVal,
                    Integer.toString(alreadyOffset + i),
                    mes.getRightMes(nowSymbolTable),
                    arrayFakeName);
        }
    }
    
    private void checkInitVal_twoDim(TreeNode nowNode, String arrayFakeName, int size1, int size2) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() != size1 * 2 + 1) {
            sysOut.println("二维数组分析时，可能出现问题了");
        }
        for (int i = 0; i < size1; i++) {
            checkInitVal_oneDim(childList.get(i * 2 + 1), size2 * i, arrayFakeName, size2);
        }
    }
    
    private Message checkConstExp(TreeNode nowNode) {
        // ConstExp → AddExp
        Message mes = checkAddExp(nowNode.getChildList().get(0));
        if (mes.getMesKind() != Message.MesKind.num) {
            sysOut.println("ConstExp 计算出错，没有返回一个constExp");
        }
        return mes;
    }
    
    private Message checkExp(TreeNode nowNode) {
        return checkAddExp(nowNode.getChildList().get(0));
    }
    
    private Message checkAddExp(TreeNode nowNode) {
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() == 1) { // 就一个mul
            return checkMulExp(childList.get(0));
        } else {
            Message mes1 = checkAddExp(childList.get(0));
            String rightMes1 = mes1.getRightMes(nowSymbolTable);
            Message mes2 = checkMulExp(childList.get(2));
            String nowOp = childList.get(1).getName();
            if (mes1.getMesKind() == Message.MesKind.num && mes2.getMesKind() == Message.MesKind.num) {
                if ("+".equals(nowOp)) {
                    return new Message(Message.MesKind.num, mes1.getNum() + mes2.getNum());
                } else {
                    return new Message(Message.MesKind.num, mes1.getNum() - mes2.getNum());
                }
            } else {    // 不会返回一个常数
                String newTemp = genNewTempX();
                if ("+".equals(nowOp)) {
                    genTetrad(Tetrad.OpType.add, rightMes1, mes2.getRightMes(nowSymbolTable), newTemp);
                } else {
                    genTetrad(Tetrad.OpType.sub, rightMes1, mes2.getRightMes(nowSymbolTable), newTemp);
                }
                return new Message(Message.MesKind.temp, newTemp);
            }
        }
    }
    
    private Message checkMulExp(TreeNode nowNode) {
        // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() == 1) {
            return checkUnaryExp(childList.get(0));
        } else {
            Message mes1 = checkMulExp(childList.get(0));
            String rightMes1 = mes1.getRightMes(nowSymbolTable);
            Message mes2 = checkUnaryExp(childList.get(2));
            String nowOp = childList.get(1).getName();
            if (mes1.getMesKind() == Message.MesKind.num && mes2.getMesKind() == Message.MesKind.num) {
                // 两边都是const， 需要进行计算
                if ("*".equals(nowOp)) {
                    return new Message(Message.MesKind.num, mes1.getNum() * mes2.getNum());
                } else if ("/".equals(nowOp)) {
                    return new Message(Message.MesKind.num, mes1.getNum() / mes2.getNum());
                } else { // %
                    return new Message(Message.MesKind.num, mes1.getNum() % mes2.getNum());
                }
            } else {    // 不是 返回一个常数，需要进行计算
                String newTemp = genNewTempX();
                if ("*".equals(nowOp)) {
                    genTetrad(Tetrad.OpType.mul, rightMes1, mes2.getRightMes(nowSymbolTable), newTemp);
                } else if ("/".equals(nowOp)) {
                    genTetrad(Tetrad.OpType.div, rightMes1, mes2.getRightMes(nowSymbolTable), newTemp);
                } else { // %
                    genTetrad(Tetrad.OpType.mod, rightMes1, mes2.getRightMes(nowSymbolTable), newTemp);
                }
                return new Message(Message.MesKind.temp, newTemp);
            }
        }
    }
    
    private Message checkUnaryExp(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.get(0).isThisKind(NodeConst.UnaryOp)) {   // UnaryExp -> UnaryOP UnaryExp
            String unaryOp = checkUnaryOp(childList.get(0));
            Message mes = this.checkUnaryExp(childList.get(1));
            if (unaryOp.equals("!")) {
                String newTemp = genNewTempX();
                unaryNotCount++;
                String unaryNotNextLabel = "unary_not_next_" + unaryNotCount;
                String unaryNotEndLabel = "end_unary_not_" + unaryNotCount;
                
                genTetrad(Tetrad.OpType.beqz, mes.getRightMes(nowSymbolTable), null, unaryNotNextLabel);
                // 此处是不为0的，要赋值为0
                genTetrad(Tetrad.OpType.assign, "0", null, newTemp);
                genTetrad(Tetrad.OpType.jump, null, null, unaryNotEndLabel);
                
                // 此处是等于0的，要赋值为1
                genTetrad(Tetrad.OpType.genLabel, null, null, unaryNotNextLabel);
                genTetrad(Tetrad.OpType.assign, "1", null, newTemp);
                genTetrad(Tetrad.OpType.genLabel, null, null, unaryNotEndLabel);
                
                return new Message(Message.MesKind.temp, newTemp);
            } else if (unaryOp.equals("+")) {   // '+'
                if (mes.getMesKind() == Message.MesKind.num) {  // 是const的话，就直接计算结果
                    return new Message(Message.MesKind.num, mes.getNum());
                } else {
                    String newTemp = genNewTempX();
                    genTetrad(Tetrad.OpType.unaryOp_add, mes.getRightMes(nowSymbolTable), null, newTemp);
                    return new Message(Message.MesKind.temp, newTemp);
                }
            } else if (unaryOp.equals("-")) {   // '-'
                if (mes.getMesKind() == Message.MesKind.num) {  // 是const的话，就直接计算结果
                    return new Message(Message.MesKind.num, -1 * mes.getNum());
                } else {
                    String newTemp = genNewTempX();
                    genTetrad(Tetrad.OpType.unaryOp_sub, mes.getRightMes(nowSymbolTable), null, newTemp);
                    return new Message(Message.MesKind.temp, newTemp);
                }
            }
        } else if (childList.get(0).isThisKind(NodeConst.Ident)) {  // UnaryExp -> Ident '(' [FuncRParams] ')'
            String identName = childList.get(0).getTokenDate().getName();
            if (childList.get(2).isThisKind(NodeConst.FuncRParams)) {
                checkFuncRParams(childList.get(2));
            }
            genTetrad(Tetrad.OpType.callFunc,
                    Integer.toString(nowSymbolTable.findFuncSymbol(identName).getParaNum()), // 参数个数，由于是函数，
                    null,
                    nowSymbolTable.getAliveFakeName(identName));   // call Func
            // return new Message(Message.MesKind.funcRet, "funcRet");
            // 修改，message不可能为funcRet
            String tempSaveFuncRet = genNewTempX();
            genTetrad(Tetrad.OpType.assign, "funcRet", null, tempSaveFuncRet);
            return new Message(Message.MesKind.temp, tempSaveFuncRet);
        } else {    // UnaryExp -> PrimaryExp
            return checkPrimaryExp(childList.get(0));
        }
        // 这一句话应该是没用的啊
        sysOut.println("到了不应该到的地方");
        return new Message(Message.MesKind.noUse, 0);
    }
    
    private String checkUnaryOp(TreeNode nowNode) {
        return nowNode.getChildList().get(0).getName();
    }
    
    private Message checkPrimaryExp(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.get(0).isThisKind("(")) { // PrimaryExp → '(' Exp ')'
            return checkExp(childList.get(1));
        } else if (childList.get(0).isThisKind(NodeConst.LVal)) { // PrimaryExp → LVal
            return checkLVal(childList.get(0));
        } else {    // PrimaryExp → Number
            return checkNumber(childList.get(0));
        }
    }
    
    private Message checkLVal(TreeNode nowNode) {
        // LVal → Ident {'[' Exp ']'}
        // 在这里，变量不会成为真正的“左值”被赋值，都是对数组的调用。数组如果调用到底（二维调用了两个维度，一维调一个维度），那么返回的是值，否则返回的就是地址
        // 另一个checkLValReal，那个调用的LVal就是真的作为左值使用的，那里返回的就是identName和offset
        // 当返回的是一个数组时。无论返回的是地址还是变量值，都返回一个temp。  -> 为了简化message的设计，同时还不会引入错误
        ArrayList<TreeNode> childList = nowNode.getChildList();
        String identName = childList.get(0).getTokenDate().getName();
        String identFakeName = nowSymbolTable.getAliveFakeName(identName);
        int identDim = nowSymbolTable.findAliveSymbol(identName).getDim();
        boolean isConst = nowSymbolTable.isAliveIdentConst(identName);
        
        if (childList.size() == 1) { // 一般变量 or 数组首地址
            if (identDim == 0) { // 一般变量，需要区分是不是const
                if (isConst) {
                    return new Message(Message.MesKind.num, nowSymbolTable.getAliveConstVal(identName));
                } else {
                    return new Message(Message.MesKind.ident_normal, identName);
                }
            } else { // 数组都是直接返回首地址，需要进行temp的封装
                String temp = genNewTempX();
                genTetrad(Tetrad.OpType.getArrayAddr, identFakeName, "0", temp);
                return new Message(Message.MesKind.temp, temp);
            }
            
        } else if (childList.size() == 4) {
            // 长度为4说明调用了一个维度，但不一定是一维数组。一维数组返回值，二维数组返回地址
            // 错误处理保证了函数参数的维数是一致的，若函数形参是一维的，那么传进去的要么是一维数组基地址要么是二维的一部分
            Message mes = checkExp(childList.get(2));
            if (identDim == 1) {
                // 一维数组返回值
                String temp = genNewTempX();
                if (mes.getMesKind() == Message.MesKind.num && isConst) {
                    // 一维const数组，调用了const偏移，返回一个常数
                    int val = nowSymbolTable.findAliveSymbol(identName).getArrayVal(mes.getNum());
                    return new Message(Message.MesKind.num, val);
                } else {
                    // 只要不是两个都是const，那么就需要进行取值计算
                    genTetrad(Tetrad.OpType.getArrayVal, identFakeName, mes.getRightMes(nowSymbolTable), temp);
                    return new Message(Message.MesKind.temp, temp);
                }
            } else {
                // 二维数组，返回地址的偏移，不用管是不是const
                int size2 = nowSymbolTable.findAliveSymbol(identName).getArrayUpBoundList().get(1); // 得到数组第二维的大小
                // 计算实际的偏移
                if (mes.getMesKind() == Message.MesKind.num) {
                    // 如果是常数，因为第二维的大小已知，那么偏移量是可以直接算出来的
                    String retTemp = genNewTempX();
                    int offset = mes.getNum() * size2;
                    genTetrad(Tetrad.OpType.getArrayAddr, identFakeName, Integer.toString(offset), retTemp);
                    return new Message(Message.MesKind.temp, retTemp);
                } else {
                    // 不是常数，需要让程序自己算地址的偏移
                    String offsetTemp = genNewTempX();
                    String retTemp = genNewTempX();
                    genTetrad(Tetrad.OpType.mul, mes.getRightMes(nowSymbolTable), Integer.toString(size2), offsetTemp);
                    genTetrad(Tetrad.OpType.getArrayAddr, identFakeName, offsetTemp, retTemp);
                    return new Message(Message.MesKind.temp, retTemp);
                }
            }
            
        } else {
            // 调用了两个维度，一定不会是地址
            Message mes1 = checkExp(childList.get(2));
            Message mes2 = checkExp(childList.get(5));
            int size2 = nowSymbolTable.findAliveSymbol(identName).getArrayUpBoundList().get(1); // 第二维的大小
            // 首先计算offset，之后在offset为常数时，才需要考虑数组本身是不是常数
            if (mes1.getMesKind() == Message.MesKind.num) {
                // 第一维是常数
                int offset = mes1.getNum() * size2;
                if (mes2.getMesKind() == Message.MesKind.num) {
                    // 第二维也是常数，因此offset是常数，此时还需要考虑数组本身是不是const
                    offset = offset + mes2.getNum();
                    if (isConst) {
                        // 数组本身也是const，直接返回一个数
                        int val = nowSymbolTable.findAliveSymbol(identName).getArrayVal(offset);
                        return new Message(Message.MesKind.num, val);
                    } else {
                        // 数组不是const，需要运行时实时计算
                        String temp = genNewTempX();
                        genTetrad(Tetrad.OpType.getArrayVal, identFakeName, Integer.toString(offset), temp);
                        return new Message(Message.MesKind.temp, temp);
                    }
                } else {
                    // 数组第二维不是常数
                    String offsetTemp = genNewTempX();
                    genTetrad(Tetrad.OpType.add, Integer.toString(offset), mes2.getRightMes(nowSymbolTable), offsetTemp);
                    String temp = genNewTempX();
                    genTetrad(Tetrad.OpType.getArrayVal, identFakeName, offsetTemp, temp);
                    return new Message(Message.MesKind.temp, temp);
                }
            } else {
                // 第1维不是常数，因此所有的都是要计算的
                String offsetTemp1 = genNewTempX();
                genTetrad(Tetrad.OpType.mul, mes1.getRightMes(nowSymbolTable), Integer.toString(size2), offsetTemp1);
                String offsetTemp2 = genNewTempX();
                genTetrad(Tetrad.OpType.add, offsetTemp1, mes2.getRightMes(nowSymbolTable), offsetTemp2);
                String temp = genNewTempX();
                genTetrad(Tetrad.OpType.getArrayVal, nowSymbolTable.getAliveFakeName(identName), offsetTemp2, temp);
                return new Message(Message.MesKind.temp, temp);
            }
        }
    }
    
    private Message checkLValReal(TreeNode nowNode) {
        // 这个函数专用于Stmt那里的真正的左值，对于数组，有几维就会算几维度，返回ident还有offset
        // Stmt -> LVal '=' Exp ';'
        //       | LVal '=' Exp ';'
        // LVal → Ident {'[' Exp ']'}
        
        ArrayList<TreeNode> childList = nowNode.getChildList();
        String identName = childList.get(0).getTokenDate().getName();
        if (nowSymbolTable.findAliveSymbol(identName).isArray()) {
            // 数组，由于是要赋值或者输入变量，因此一定不是const，计算出相应的offset就ok
            /*sysOut.println("当前行数:" + childList.get(0).getTokenDate().getLineNum());
            sysOut.println("in checkLValReal ,认为 "+ identName + "是一个数组" +
                    ",维数为" + nowSymbolTable.findAliveSymbol(identName).getDim() +
                    "调用行数为：" + nowSymbolTable.findAliveSymbol(identName).getLineNum());*/
            if (nowSymbolTable.findAliveSymbol(identName).getDim() == 1) {
                // 1维数组
                if (childList.size() != 4) {
                    sysOut.println("一维数组分析时长度不对，LValReal");
                }
                Message mes = checkExp(childList.get(2));   // num, ident, temp。只有这3中可能，因为对数组的读操作一定会被封装成一个temp返回
                return new Message(Message.MesKind.ident_and_offset, identName, mes.getRightMes(nowSymbolTable));
            } else {
                // 2维数组
                if (childList.size() != 7) {
                    sysOut.println("二维数组分析时长度不对，LValReal");
                }
                int size2 = nowSymbolTable.findAliveSymbol(identName).getArrayUpBoundList().get(1); // 第二维的大小
                Message mes1 = checkExp(childList.get(2));
                Message mes2 = checkExp(childList.get(5));
                // 开始计算偏移量
                if (mes1.getMesKind() == Message.MesKind.num) {
                    // 第一维是常数
                    int offset1 = mes1.getNum() * size2;
                    if (mes2.getMesKind() == Message.MesKind.num) { // 偏移量都是常数，直接计算出实际偏移量
                        offset1 = offset1 + mes2.getNum();
                        return new Message(Message.MesKind.ident_and_offset, identName, Integer.toString(offset1));
                    } else {    // 第一维是常数，可以直接计算，还需要加上第二维的大小
                        String temp1 = genNewTempX();
                        genTetrad(Tetrad.OpType.add, Integer.toString(offset1), mes2.getRightMes(nowSymbolTable), temp1);
                        return new Message(Message.MesKind.ident_and_offset, identName, temp1);
                    }
                } else {
                    // 第一维不是常数
                    String temp1 = genNewTempX();
                    genTetrad(Tetrad.OpType.mul, Integer.toString(size2), mes1.getRightMes(nowSymbolTable), temp1);
                    String temp2 = genNewTempX();
                    genTetrad(Tetrad.OpType.add, temp1, mes2.getRightMes(nowSymbolTable), temp2);
                    return new Message(Message.MesKind.ident_and_offset, identName, temp2);
                }
            }
        } else {
            // 一般变量，由于是要赋值或者输入变量，因此一定不是const，返回标号就可以了
            return new Message(Message.MesKind.ident_normal, identName);
        }
    }
    
    private Message checkNumber(TreeNode nowNode) {
        // Number → IntConst
        return new Message(Message.MesKind.num, nowNode.getChildList().get(0).getTokenDate().getValue());
    }
    
    private void checkFuncRParams(TreeNode nowNode) {
        // FuncRParams → Exp { ',' Exp }
        ArrayList<TreeNode> childList = nowNode.getChildList();
        ArrayList<String> tempList = new ArrayList<>();
        for (TreeNode childNode : childList) {
            if (childNode.isThisKind(NodeConst.Exp)) {
                Message mes = checkExp(childNode);
                String newTemp = genNewTempX();
                // 由于para可能要很久以后才会被放到栈上去作为参数调用，因此要先存起来
                genTetrad(Tetrad.OpType.assign, mes.getRightMes(nowSymbolTable), null, newTemp);
                tempList.add(newTemp);
            }
        }
        // 将参数全部分析完之后统一pushPara，由于值全部都使用temp预存了，所以肯定没有问题
        for (int i = 0; i < tempList.size(); i++) {
            genTetrad(Tetrad.OpType.pushPara, Integer.toString(i + 1), null, tempList.get(i));
        }
    }
    
    private Message checkCond(TreeNode nowNode) {
        // Cond → LOrExp
        return checkLorExp(nowNode.getChildList().get(0));
    }
    
    private Message checkLorExp(TreeNode nowNode) { // 不会做针对常数的优化，放到之后的优化环节做
        lOrCount++;
        String nowLorNextLabel = "lOr_next_" + lOrCount;
        String nowLorEndLabel = "lOr_End_" + lOrCount;
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() == 1) {
            return checkLAndExp(childList.get(0));
        } else {
            String tempSave = genNewTempX();    // 要首先生成出来，避免在后面分析的时候被子程序抢先了
            Message mes1 = checkLorExp(childList.get(0));
            // 第一步是若为0则跳转到第二个的分析过程中，是1的话直接就短路结束了
            genTetrad(Tetrad.OpType.beqz, mes1.getRightMes(nowSymbolTable), null, nowLorNextLabel);
            // 如果到时候mips到了这里，说明第一个表达式的结果不是0
            genTetrad(Tetrad.OpType.assign, "1", null, tempSave);
            genTetrad(Tetrad.OpType.jump, null, null, nowLorEndLabel);
            // 开始后面的一段的分析
            genTetrad(Tetrad.OpType.genLabel, null, null, nowLorNextLabel);
            Message mes2 = checkLAndExp(childList.get(2));
            genTetrad(Tetrad.OpType.assign, mes2.getRightMes(nowSymbolTable), null, tempSave);  // 把第2个表达式的结果存起来
            genTetrad(Tetrad.OpType.genLabel, null, null, nowLorEndLabel);
            return new Message(Message.MesKind.temp, tempSave);
        }
    }
    
    private Message checkLAndExp(TreeNode nowNode) {
        lAndCount++;
        String nowLAndNextLabel = "lAnd_next_" + lAndCount;
        String nowLAndEndLabel = "lAnd_End_" + lAndCount;
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() == 1) {
            return checkEqExp(childList.get(0));
        } else {
            String tempSave = genNewTempX();    // 要首先生成出来，避免在后面分析的时候被子程序抢先了
            Message mes1 = checkLAndExp(childList.get(0));
            // 若不为0需要对第二个进行分析，是0的话直接就短路结束了
            genTetrad(Tetrad.OpType.bnez, mes1.getRightMes(nowSymbolTable), null, nowLAndNextLabel);
            genTetrad(Tetrad.OpType.assign, "0", null, tempSave);
            genTetrad(Tetrad.OpType.jump, null, null, nowLAndEndLabel);
            // 第二段
            genTetrad(Tetrad.OpType.genLabel, null, null, nowLAndNextLabel);
            Message mes2 = checkEqExp(childList.get(2));
            genTetrad(Tetrad.OpType.assign, mes2.getRightMes(nowSymbolTable), null, tempSave);
            genTetrad(Tetrad.OpType.genLabel, null, null, nowLAndEndLabel);
            return new Message(Message.MesKind.temp, tempSave);
        }
    }
    
    private Message checkEqExp(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        if (childList.size() == 1) {
            return checkRelExp(childList.get(0));
        } else {
            String nowTemp = genNewTempX();
            eqCount++;
            String eqNextLabel = "eq_Next_" + eqCount;
            String eqEndLabel = "eq_End_" + eqCount;
            Message mes1 = checkEqExp(childList.get(0));
            Message mes2 = checkRelExp(childList.get(2));
            genTetrad(Tetrad.OpType.sub, mes1.getRightMes(nowSymbolTable), mes2.getRightMes(nowSymbolTable), nowTemp);
            if (childList.get(1).isThisKind("==")) {    // nowTemp为0则为1
                genTetrad(Tetrad.OpType.beqz, nowTemp, null, eqNextLabel);
                // 到这里相减结果不为0
                genTetrad(Tetrad.OpType.assign, "0", null, nowTemp);
                genTetrad(Tetrad.OpType.jump, null, null, eqEndLabel);
                // 到这里说明相减结果为0
                genTetrad(Tetrad.OpType.genLabel, null, null, eqNextLabel);
                genTetrad(Tetrad.OpType.assign, "1", null, nowTemp);
                genTetrad(Tetrad.OpType.genLabel, null, null, eqEndLabel);
            } else { // nowTemp为0则为0
                genTetrad(Tetrad.OpType.beqz, nowTemp, null, eqNextLabel);
                // 到这里相减结果不为0，最终返回1
                genTetrad(Tetrad.OpType.assign, "1", null, nowTemp);
                genTetrad(Tetrad.OpType.jump, null, null, eqEndLabel);
                // 到这里说明相减结果为0，说明相等，返回0
                genTetrad(Tetrad.OpType.genLabel, null, null, eqNextLabel);
                genTetrad(Tetrad.OpType.assign, "0", null, nowTemp);
                genTetrad(Tetrad.OpType.genLabel, null, null, eqEndLabel);
            }
            return new Message(Message.MesKind.temp, nowTemp);
        }
    }
    
    private Message checkRelExp(TreeNode nowNode) {
        ArrayList<TreeNode> childList = nowNode.getChildList();
        String saveTemp = genNewTempX();
        if (childList.size() == 1) {
            Message mes = checkAddExp(childList.get(0));
            genTetrad(Tetrad.OpType.assign, mes.getRightMes(nowSymbolTable), null, saveTemp);
            return new Message(Message.MesKind.temp, saveTemp);
        } else {
            relCount++;
            String relNextLabel = "rel_next_" + relCount;
            String relEndLabel = "rel_end_" + relCount;
            String subTemp = genNewTempX();
            Message mes1 = checkRelExp(childList.get(0));
            Message mes2 = checkAddExp(childList.get(2));
            genTetrad(Tetrad.OpType.sub, mes1.getRightMes(nowSymbolTable), mes2.getRightMes(nowSymbolTable), subTemp);
            // 四种不同的符号，跳转的条件不一样
            if (childList.get(1).isThisKind("<")) {
                genTetrad(Tetrad.OpType.bltz, subTemp, null, relNextLabel);
            } else if (childList.get(1).isThisKind("<=")) {
                genTetrad(Tetrad.OpType.blez, subTemp, null, relNextLabel);
            } else if (childList.get(1).isThisKind(">")) {
                genTetrad(Tetrad.OpType.bgtz, subTemp, null, relNextLabel);
            } else if (childList.get(1).isThisKind(">=")) {
                genTetrad(Tetrad.OpType.bgez, subTemp, null, relNextLabel);
            }
            // 这是结果为0的情况
            genTetrad(Tetrad.OpType.assign, "0", null, saveTemp);
            genTetrad(Tetrad.OpType.jump, null, null, relEndLabel);
            // 这里是为1的情况
            genTetrad(Tetrad.OpType.genLabel, null, null, relNextLabel);
            genTetrad(Tetrad.OpType.assign, "1", null, saveTemp);
            genTetrad(Tetrad.OpType.genLabel, null, null, relEndLabel);
            return new Message(Message.MesKind.temp, saveTemp);
        }
    }
}
