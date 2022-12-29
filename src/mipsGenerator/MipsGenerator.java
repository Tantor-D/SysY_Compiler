package mipsGenerator;

import tetradGenerator.Tetrad;

import java.io.PrintStream;
import java.util.ArrayList;

public class MipsGenerator {
    // 构造函数中维护
    private final ArrayList<Tetrad> tetradList;
    private final ArrayList<String> strToPrintList;
    private MipsGeneratorTool tool;
    private ArrayList<String> mipsList;
    
    private PrintStream mipsOut;
    private PrintStream sysOut;
    
    // startGen 中初始化
    private RunTimeStack baseRunTimeStack;
    private RunTimeStack nowRunTimeStack;
    private ArrayList<RunTimeStack> runTimeStackList;
    
    private int indexOfFirstFuncDef;
    
    private void printNowMips() {
        sysOut.println();
        sysOut.println();
        sysOut.println();
        for (String mipsCode : mipsList) {
            sysOut.println(mipsCode);
        }
    }
    
    public MipsGenerator(ArrayList<Tetrad> tetradList, ArrayList<String> strToPrintList) {
        this.tetradList = tetradList;
        this.strToPrintList = strToPrintList;
        mipsList = new ArrayList<>();
        tool = new MipsGeneratorTool(mipsList);
        try {
            mipsOut = new PrintStream("mips.txt");
            sysOut = System.out;
        } catch (Exception e) {
            System.out.println("设置输出流mips.txt失败");
        }
    }
    
    public void startToGenMIPS() {
        // 初始化
        runTimeStackList = new ArrayList<>();
        baseRunTimeStack = new RunTimeStack("base");    // base里面会存的都是计算全局变量信息时引入的 临时变量，暂时放到栈里
        nowRunTimeStack = baseRunTimeStack;
        runTimeStackList.add(baseRunTimeStack);
        indexOfFirstFuncDef = 0;
        
        // 开始生成中间代码
        genDataSeg();
        genTextSeg();
        
        // 输出结果
        for (String mipsCode : mipsList) {
            mipsOut.println(mipsCode);
        }
    }
    
    public void genDataSeg() {
        mipsList.add(".data");
        // 仅仅负责定义全局变量 和 字符串
        // 首先定义全局变量
        // 由于全局变量都使用标号来记录，可以直接通过变量名找到地址，因此不需要全局的表
        for (Tetrad nowTetrad : tetradList) {
            if (nowTetrad.getOpType() == Tetrad.OpType.defFunc) {   // 到函数部分了，退出
                break;
            }
            if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal) {
                dealGlobal_defVar_normal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal_AndAssign) {
                dealGlobal_defVarAndAssign_normal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defConst_normal_And_Assign) {
                dealGlobal_defConstAndAssign_normal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defOneDimArray) {
                dealGlobal_defOneDimArray(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defTwoDimArray) {
                dealGlobal_defTwoDimArray(nowTetrad);
            }
        }
        // 定义字符串
        for (int i = 0; i < strToPrintList.size(); i++) {
            mipsList.add("str" + i + ": .asciiz \"" + strToPrintList.get(i) + "\"");
        }
    }
    
    public void genTextSeg() {
        mipsList.add("\n\n.text");
        mipsList.add("move $fp, $sp");   // 用$fp, 不用$sp
        
        // 将没做完的全局变量计算完并赋值，仅需考虑 计算 、 定义全局变量 、 全局数组赋值 三种类型的四元式
        genAssignForGlobalVar();
        // 完成调用main函数 和 最终的退出
        genCallFuncAndInitStack(nowRunTimeStack.getStackSize(), "f_main");
        mipsList.add("li $v0, 10");
        mipsList.add("syscall");
        
        // 完成对所有四元式的分析
        // 从函数定义的地方开始，此时已经没有全局变量的定义了
        for (int i = indexOfFirstFuncDef; i < tetradList.size(); i++) {
            Tetrad nowTetrad = tetradList.get(i);
            tool.gen_anything("# " + nowTetrad.myToString());    // 输出四元式，便于debug
            if (tool.isCalculateOpType(nowTetrad.getOpType())) {
                dealCalculation(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.assign) {
                deal_assign(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal) {
                deal_defVar_normal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal_AndAssign) {
                deal_defVarAndAssign_normal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defConst_normal_And_Assign) {
                deal_defConstAndAssign_normal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFunc) {
                deal_defFunc(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFuncPara_normal) {
                deal_defFuncPara_normal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.pushPara) {
                deal_pushPara(nowTetrad, Integer.parseInt(nowTetrad.getLabel1()));
            } else if (nowTetrad.getOpType() == Tetrad.OpType.callFunc) {
                deal_callFunc(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.returnVoid) {
                deal_returnVoid(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.returnInt) {
                deal_returnInt(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.printStr) {
                deal_printStr(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.printInt) {
                deal_printInt(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.getint) {
                deal_getint(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bnez) {
                deal_bnez(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.beqz) {
                deal_beqz(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bgez) {
                deal_bgez(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bgtz) {
                deal_bgtz(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.blez) {
                deal_blez(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bltz) {
                deal_bltz(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.jump) {
                deal_jump(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.genLabel) {
                deal_genLabel(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defOneDimArray) {
                deal_defOneDimArray(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defTwoDimArray) {
                deal_defTwoDimArray(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFuncPara_oneDim) {
                deal_defFuncPara_oneDim(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFuncPara_twoDim) {
                deal_defFuncPara_TwoDim(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.getArrayAddr) {
                deal_getArrayAddr(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.getArrayVal) {
                deal_getArrayVal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.assignArrayVal) {
                deal_assignArrayVal(nowTetrad);
            }
            
        }
    }
    
    private void genAssignForGlobalVar() {
        for (int i = 0; i < tetradList.size(); i++) {
            Tetrad nowTetrad = tetradList.get(i);
            if (nowTetrad.getOpType() == Tetrad.OpType.defFunc) {
                indexOfFirstFuncDef = i;
                break;
            }
            if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal ||
                    nowTetrad.getOpType() == Tetrad.OpType.defConst_normal_And_Assign ||
                    nowTetrad.getOpType() == Tetrad.OpType.defOneDimArray ||
                    nowTetrad.getOpType() == Tetrad.OpType.defTwoDimArray) {
                continue;   // 这4种都已经处理过了，无需再处理
            }
            
            // 此处仅考虑 计算、定义全局变量、数组赋值、数组读取 4种类型的四元式
            tool.gen_anything("# " + nowTetrad.myToString());    // 输出四元式，便于debug
            if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal_AndAssign) {
                // 对一般全局变量的赋值
                if (!tool.isStrInt(nowTetrad.getLabel1())) {
                    // 只有那些是全局变量 且 赋值时不是赋数字的才需要进行处理
                    moveValToReg(nowTetrad.getLabel1(), Reg.t0_s);
                    mipsList.add("sw" + " " + Reg.t0_s + ", " + nowTetrad.getDes());
                }
            } else if (nowTetrad.getOpType() == Tetrad.OpType.assignArrayVal) {
                // 对全局数组的赋值
                deal_assignArrayVal(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.getArrayVal) {
                // 读取数组的值(可能定义一个非const的数组，然后对其值进行读取)
                deal_getArrayVal(nowTetrad);
            } else if (tool.isCalculateOpType(nowTetrad.getOpType())) {
                // 可能出现的运算add、sub、mul、div、mod、unaryAdd、unarySub
                dealCalculation(nowTetrad);
            } else { // 为了debug
                sysOut.println("在函数定义中出现了没有考虑到的计算行为");
            }
        }
    }
    
    private void dealCalculation(Tetrad nowTetrad) {   // 支持7种操作
        if (nowTetrad.getOpType() == Tetrad.OpType.add) {
            deal_add(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.sub) {
            deal_sub(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.mul) {
            deal_mul(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.div) {
            deal_div(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.mod) {
            deal_mod(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.unaryOp_add) {
            deal_unaryAdd(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.unaryOp_sub) {
            deal_unarySub(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.shl) {
            deal_shl(nowTetrad);
        } else if (nowTetrad.getOpType() == Tetrad.OpType.shr) {
            deal_shr(nowTetrad);
        }
    }
    
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    // 提供支持的函数
    private void moveValToReg(String str, String regName) {
        // 用于读取变量 或 将数字、函数返回值加载到寄存器中
        // 不可以用于读取数组的基地址，因为全局变量的数组应该使用la而不是lw加载，此处无法区分
        
        // 约定：调用此函数时，已知是数字、全局变量、局部变量、中间代码临时变量、函数返回值
        // ident为 数字、局部变量^、全局变量g_、临时变量t、函数返回值funcRet。
        // 这几个东西的命名是互斥的
        // 这个过程中仅用到了$fp 还有目标 reg，没有引入多的中间寄存器
        if (tool.isStrInt(str)) {
            tool.gen_li(regName, Integer.parseInt(str));
        } else if (str.equals("funcRet")) {      // 函数返回值
            mipsList.add("move " + regName + ", $v0");
        } else if (tool.isGlobalVar(str)) {    // 全局变量
            mipsList.add("lw " + regName + ", " + str);
        } else {    // 局部变量、中间代码临时变量，都在运行栈上
            int offset = nowRunTimeStack.getItemOffset(str);
            mipsList.add("lw " + regName + ", " + offset + "($fp)");
        }
    }
    
    private void getArrayBaseAddr(String arrayName, String regName) {
        // 这个数组专用于获取数组的基地址，有这个函数是因为moveValToReg不能用于加载数组基地址
        if (tool.isGlobalVar(arrayName)) {
            // 全局变量，使用la加载基地址
            tool.gen_la(regName, arrayName);
        } else {
            // 局部变量在运行栈上
            int offset = nowRunTimeStack.getItemOffset(arrayName);
            mipsList.add("lw " + regName + ", " + offset + "($fp)");
        }
    }
    
    private void saveVal(String identName, String regName) {
        // 该函数用于保存变量。需要保存的变量要么是去全局那里，要么是去栈上
        // 调用此函数时，无需管identName事先有没有分配过空间
        if (tool.isGlobalVar(identName)) {
            mipsList.add("sw " + regName + ", " + identName);
        } else {
            // 对于新变量，会分配新空间。对于已经在栈上的变量，会覆盖旧值
            nowRunTimeStack.storeVarAndGenCode(identName, regName, mipsList);
        }
    }
    
    private void addVarToStackWithNoInitVal(String identName) {
        // 用于定义了一个变量但是没有赋初值的情况
        // 此时仅仅需要在栈中为其分配一个位置，记录下偏移量就ok了。
        // 无需生成相关的mips代码，只要我知道变量在哪就ok了
        nowRunTimeStack.addNewVarWithNoInitVal(identName);
    }
    
    private void genCallFuncAndInitStack(int nowStackSize, String funcName) {
        // 在改栈帧的时候，需要一个个push，push完了之后才动栈帧
        // 这里仅仅只负责生成callFunc的代码，至于运行时的runTimeTable，那是在分析有关函数时使用的，因此在defFunc那里才处理
        mipsList.add("sw $fp, -" + (nowStackSize + 4) + "($fp)");
        mipsList.add("sw $ra, -" + (nowStackSize + 8) + "($fp)");
        mipsList.add("addi $fp, $fp, -" + nowStackSize);
        mipsList.add("jal " + funcName);
    }
    
    private void genReturnCode() {
        tool.gen_anything("move $t0, $ra");
        tool.gen_anything("lw $ra, -8($fp)");
        tool.gen_anything("lw $fp, -4($fp)");
        tool.gen_anything("jr $t0");
    }
    
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////
    // 以下deal_用于分析某一条具体的tetrad
    // 数组相关
    private void deal_defOneDimArray(Tetrad nowTetrad) {
        // 用于非全局数组的定义
        // defOneDimArray,  (op, size, _, array)
        nowRunTimeStack.addNewArrayAndGenCode(nowTetrad.getDes(), Integer.parseInt(nowTetrad.getLabel1()), mipsList);
    }
    
    private void deal_defTwoDimArray(Tetrad nowTetrad) {
        // 用于非全局数组的定义
        // defTwoDimArray,  (op, size1, size2, array)
        int arraySize = Integer.parseInt(nowTetrad.getLabel1()) * Integer.parseInt(nowTetrad.getLabel2());
        nowRunTimeStack.addNewArrayAndGenCode(nowTetrad.getDes(), arraySize, mipsList);
    }
    
    private void deal_defFuncPara_normal(Tetrad nowTetrad) {
        // 由于defFunc之后紧跟着的就是defFParam，因此可以保证参数是按顺序加到运行符号栈中的
        addVarToStackWithNoInitVal(nowTetrad.getDes());
    }
    
    private void deal_defFuncPara_oneDim(Tetrad nowTetrad) {
        // 处理方法跟一般的变量一样，就是在运行栈中记录一下
        addVarToStackWithNoInitVal(nowTetrad.getDes());
    }
    
    private void deal_defFuncPara_TwoDim(Tetrad nowTetrad) {
        addVarToStackWithNoInitVal(nowTetrad.getDes());
    }
    
    private void deal_getArrayAddr(Tetrad nowTetrad) {
        // getArrayAddr,  (op, array, offset, des)
        getArrayBaseAddr(nowTetrad.getLabel1(), Reg.t0_s);  // 数组基地址
        moveValToReg(nowTetrad.getLabel2(), Reg.t1_s);  // 偏移量
        tool.gen_sll(Reg.t1_s, Reg.t1_s, 2);   // 左移两位相当于是*4
        tool.gen_add(Reg.t0_s, Reg.t0_s, Reg.t1_s);     // 此时t0中的是实际的地址
        saveVal(nowTetrad.getDes(), Reg.t0_s);
    }
    
    private void deal_getArrayVal(Tetrad nowTetrad) {
        // getArrayVal,  (op, array, offset, des)
        getArrayBaseAddr(nowTetrad.getLabel1(), Reg.t0_s);  // 数组基地址
        if (tool.isStrInt(nowTetrad.getLabel2())) {
            // 偏移量是一个常数，得到了基地址后可以一个lw解决问题
            int offset = Integer.parseInt(nowTetrad.getLabel2());
            tool.gen_lw(Reg.t0_s, offset * 4, Reg.t0_s);
            saveVal(nowTetrad.getDes(), Reg.t0_s);
        } else {
            // 偏移量不是常数，需要首先计算真正的地址
            moveValToReg(nowTetrad.getLabel2(), Reg.t1_s);  // 偏移量
            tool.gen_sll(Reg.t1_s, Reg.t1_s, 2);   // 左移两位相当于是*4
            tool.gen_add(Reg.t0_s, Reg.t0_s, Reg.t1_s);     // 此时t0中的是实际的地址
            tool.gen_lw(Reg.t0_s, 0, Reg.t0_s);
            saveVal(nowTetrad.getDes(), Reg.t0_s);
        }
    }
    
    private void deal_assignArrayVal(Tetrad nowTetrad) {
        // assignArrayVal,  (op, offset, val, array)
        getArrayBaseAddr(nowTetrad.getDes(), Reg.t0_s);  // 数组基地址
        if (tool.isStrInt(nowTetrad.getLabel1())) {
            // 偏移量是常数
            int offset = Integer.parseInt(nowTetrad.getLabel1());
            moveValToReg(nowTetrad.getLabel2(), Reg.t1_s);  // 要保存的变量
            tool.gen_sw(Reg.t1_s, offset * 4, Reg.t0_s);
        } else {
            // 偏移量不是常数，需要首先计算真正的地址
            moveValToReg(nowTetrad.getLabel1(), Reg.t1_s);  // 偏移量
            tool.gen_sll(Reg.t1_s, Reg.t1_s, 2);   // 偏移字节量
            tool.gen_add(Reg.t0_s, Reg.t0_s, Reg.t1_s);     // 现在t0中存的是真正的要赋值的地址
            moveValToReg(nowTetrad.getLabel2(), Reg.t1_s);  // 现在t1中存的是要保存的变量值
            tool.gen_sw(Reg.t1_s, 0, Reg.t0_s);
        }
    }
    
    // 跳转相关
    private void deal_bnez(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t0_s);
        tool.gen_bnez(Reg.t0_s, des);
    }
    
    private void deal_beqz(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t0_s);
        tool.gen_beqz(Reg.t0_s, des);
    }
    
    private void deal_bgez(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t0_s);
        tool.gen_bgez(Reg.t0_s, des);
    }
    
    private void deal_bgtz(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t0_s);
        tool.gen_bgtz(Reg.t0_s, des);
    }
    
    private void deal_blez(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t0_s);
        tool.gen_blez(Reg.t0_s, des);
    }
    
    private void deal_bltz(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t0_s);
        tool.gen_bltz(Reg.t0_s, des);
    }
    
    private void deal_jump(Tetrad nowTetrad) {
        tool.gen_j(nowTetrad.getDes());
    }
    
    private void deal_genLabel(Tetrad nowTetrad) {
        tool.gen_label(nowTetrad.getDes());
    }
    
    // def相关
    private void deal_defFunc(Tetrad nowTetrad) {
        nowRunTimeStack = new RunTimeStack(nowTetrad.getDes());
        tool.gen_anything("\n\n" + nowTetrad.getDes() + ":");
    }
    
    private void deal_defVar_normal(Tetrad nowTetrad) {
        addVarToStackWithNoInitVal(nowTetrad.getDes());
    }
    
    private void deal_defVarAndAssign_normal(Tetrad nowTetrad) {
        moveValToReg(nowTetrad.getLabel1(), Reg.t1_s);
        saveVal(nowTetrad.getDes(), Reg.t1_s);
    }
    
    private void deal_defConstAndAssign_normal(Tetrad nowTetrad) {
        moveValToReg(nowTetrad.getLabel1(), Reg.t1_s);
        saveVal(nowTetrad.getDes(), Reg.t1_s);
    }
    
    // 函数相关
    private void deal_pushPara(Tetrad nowTetrad, int index) {
        // index 表示是第几个para，从1开始
        int nowStackSize = nowRunTimeStack.getStackSize();
        moveValToReg(nowTetrad.getDes(), Reg.t1_s);
        tool.gen_sw(Reg.t1_s, -4 * index - 8 - nowStackSize, Reg.fp_s);
    }
    
    private void deal_callFunc(Tetrad nowTetrad) {
        genCallFuncAndInitStack(nowRunTimeStack.getStackSize(), nowTetrad.getDes());
    }
    
    private void deal_returnInt(Tetrad nowTetrad) {
        moveValToReg(nowTetrad.getDes(), Reg.v0_s);
        genReturnCode();
    }
    
    private void deal_returnVoid(Tetrad nowTetrad) {
        genReturnCode();
    }
    
    // 计算相关
    private void deal_assign(Tetrad nowTetrad) {
        // sysOut.println(tetradList.indexOf(nowTetrad));
        moveValToReg(nowTetrad.getLabel1(), Reg.t1_s);
        saveVal(nowTetrad.getDes(), Reg.t1_s);
    }
    
    private void deal_shl(Tetrad nowTetrad) {
        // label2一定是一个常数
        String lb1 = nowTetrad.getLabel1();
        String lb2 = nowTetrad.getLabel2();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t1_s);
        tool.gen_sll(Reg.t1_s, Reg.t1_s, Integer.parseInt(lb2));
        saveVal(des, Reg.t1_s);
    }
    
    private void deal_shr(Tetrad nowTetrad) {
        // label2一定是一个常数
        String lb1 = nowTetrad.getLabel1();
        String lb2 = nowTetrad.getLabel2();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t1_s);
        tool.gen_srl(Reg.t1_s, Reg.t1_s, Integer.parseInt(lb2));
        saveVal(des, Reg.t1_s);
    }
    
    private void deal_add(Tetrad nowTetrad) {
        // 可能生成add 和 addi两种
        String lb1 = nowTetrad.getLabel1();
        String lb2 = nowTetrad.getLabel2();
        String des = nowTetrad.getDes();
        if (nowTetrad.getOpType() == Tetrad.OpType.add) {
            if (tool.isStrInt(lb1) || tool.isStrInt(lb2)) { // addi
                String intStr;
                String identStr;
                if (tool.isStrInt(lb1)) {
                    intStr = lb1;
                    identStr = lb2;
                } else {
                    intStr = lb2;
                    identStr = lb1;
                }
                moveValToReg(identStr, Reg.t1_s);
                mipsList.add("addi" + " " + Reg.t2_s + ", " + Reg.t1_s + ", " + intStr);
                saveVal(des, Reg.t2_s);
            } else {    // add
                moveValToReg(lb1, Reg.t1_s);
                moveValToReg(lb2, Reg.t2_s);
                mipsList.add("add" + " " + Reg.t3_s + ", " + Reg.t1_s + ", " + Reg.t2_s);
                saveVal(des, Reg.t3_s);
            }
        } else {
            sysOut.println("一个不是add的四元式使用了genAddOrAddi");
        }
    }
    
    private void deal_sub(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String lb2 = nowTetrad.getLabel2();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t1_s);
        if (tool.isStrInt(lb2)) {   // 第二个是常量，用subi
            tool.gen_subi(Reg.t3_s, Reg.t1_s, Integer.parseInt(lb2));
        } else {    // 两个都是变量或者第一个是常量，用sub
            moveValToReg(lb2, Reg.t2_s);
            tool.gen_sub(Reg.t3_s, Reg.t1_s, Reg.t2_s);
        }
        saveVal(des, Reg.t3_s);
    }
    
    private void deal_mul(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String lb2 = nowTetrad.getLabel2();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t1_s);
        moveValToReg(lb2, Reg.t2_s);
        tool.gen_mult(Reg.t1_s, Reg.t2_s);
        tool.gen_mflo(Reg.t3_s);
        saveVal(des, Reg.t3_s);
    }
    
    private void deal_div(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String lb2 = nowTetrad.getLabel2();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t1_s);
        moveValToReg(lb2, Reg.t2_s);
        tool.gen_div(Reg.t1_s, Reg.t2_s);
        tool.gen_mflo(Reg.t3_s);
        saveVal(des, Reg.t3_s);
    }
    
    private void deal_mod(Tetrad nowTetrad) {
        String lb1 = nowTetrad.getLabel1();
        String lb2 = nowTetrad.getLabel2();
        String des = nowTetrad.getDes();
        moveValToReg(lb1, Reg.t1_s);
        moveValToReg(lb2, Reg.t2_s);
        tool.gen_div(Reg.t1_s, Reg.t2_s);
        tool.gen_mfhi(Reg.t3_s);
        saveVal(des, Reg.t3_s);
    }
    
    private void deal_unaryAdd(Tetrad nowTetrad) {
        moveValToReg(nowTetrad.getLabel1(), Reg.t1_s);
        saveVal(nowTetrad.getDes(), Reg.t1_s);
    }
    
    private void deal_unarySub(Tetrad nowTetrad) {
        // 变为0 - label1
        tool.gen_li(Reg.t1_s, 0);
        moveValToReg(nowTetrad.getLabel1(), Reg.t2_s);
        tool.gen_sub(Reg.t3_s, Reg.t1_s, Reg.t2_s);
        saveVal(nowTetrad.getDes(), Reg.t3_s);
    }
    
    // 输入输出相关
    private void deal_getint(Tetrad nowTetrad) {
        tool.gen_li(Reg.v0_s, 5);
        tool.gen_syscall();
        saveVal(nowTetrad.getDes(), Reg.v0_s);
    }
    
    private void deal_printInt(Tetrad nowTetrad) {
        moveValToReg(nowTetrad.getDes(), Reg.a0_s);
        tool.gen_li(Reg.v0_s, 1);
        tool.gen_syscall();
    }
    
    private void deal_printStr(Tetrad nowTetrad) {
        tool.gen_li(Reg.v0_s, 4);
        tool.gen_la(Reg.a0_s, nowTetrad.getDes());
        tool.gen_syscall();
    }
    
    //////////////////////////////////////////////////////////////////////////
    // global define
    private void dealGlobal_defVar_normal(Tetrad nowTetrad) {
        mipsList.add(nowTetrad.getDes() + ": .word 0");
    }
    
    private void dealGlobal_defVarAndAssign_normal(Tetrad nowTetrad) {
        if (tool.isStrInt(nowTetrad.getLabel1())) {
            mipsList.add(nowTetrad.getDes() + ": .word " + nowTetrad.getLabel1());
        } else {    // 不是常数，因此要放到后面计算，最后可以优化为直接计算出结果
            mipsList.add(nowTetrad.getDes() + ": .word 0");    // 暂时直接定义为0
        }
    }
    
    private void dealGlobal_defConstAndAssign_normal(Tetrad nowTetrad) {
        mipsList.add(nowTetrad.getDes() + ": .word " + nowTetrad.getLabel1());
    }
    
    private void dealGlobal_defOneDimArray(Tetrad nowTetrad) {
        int size = 4 * Integer.parseInt(nowTetrad.getLabel1());
        mipsList.add(nowTetrad.getDes() + ": .space " + size);
    }
    
    private void dealGlobal_defTwoDimArray(Tetrad nowTetrad) {
        int size = 4*Integer.parseInt(nowTetrad.getLabel1()) * Integer.parseInt(nowTetrad.getLabel2());
        mipsList.add(nowTetrad.getDes() + ": .space " + size);
    }
}
