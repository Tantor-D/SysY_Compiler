package optimizer;

import tetradGenerator.Tetrad;

import java.io.PrintStream;
import java.util.ArrayList;

public class MidCodePrinter {
    private final ArrayList<Tetrad> tetradList;
    private final ArrayList<String> strToPrintList;
    private final ArrayList<String> midCodeList;
    
    private PrintStream sysOut;
    private PrintStream midCodeOut;
    
    public MidCodePrinter(ArrayList<Tetrad> tetradList, ArrayList<String> strToPrintList, String outFilePath) {
        this.tetradList = tetradList;
        this.strToPrintList = strToPrintList;
        this.midCodeList = new ArrayList<>();
        
        try {
            midCodeOut = new PrintStream(outFilePath);
            sysOut = System.out;
        } catch (Exception e) {
            System.out.println("设置输出流" + outFilePath + "失败");
        }
    }
    
    public void start() {
        // 首先处理要输出的字符串信息
        for (int i = 0; i < strToPrintList.size(); i++) {
            midCodeList.add("const str str" + i + " = \"" + strToPrintList.get(i) + "\"");
        }
        
        // 然后输出四元式转中间代码的信息
        checkTetrad();
        
        // 分析完之后输出结果
        for (String midCode : midCodeList) {
            midCodeOut.println(midCode);
        }
    }
    
    private void checkTetrad() {
        for (Tetrad nowTetrad : tetradList) {
            String label1 = nowTetrad.getLabel1();
            String label2 = nowTetrad.getLabel2();
            String des = nowTetrad.getDes();
            if (nowTetrad.getOpType() == Tetrad.OpType.assign) {
                midCodeList.add(des + " = " + label1);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.add) {
                midCodeList.add(des + " = " + label1 + " + " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.sub) {
                midCodeList.add(des + " = " + label1 + " - " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.mul) {
                midCodeList.add(des + " = " + label1 + " * " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.div) {
                midCodeList.add(des + " = " + label1 + " / " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.mod) {
                midCodeList.add(des + " = " + label1 + " % " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.shl) {
                midCodeList.add(des + " = " + label1 + " << " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.shr) {
                midCodeList.add(des + " = " + label1 + " >> " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.unaryOp_add) {
                // (op, label1, _, des)
                midCodeList.add(des + " = +" + label1);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.unaryOp_sub) {
                midCodeList.add(des + " = -" + label1);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal) {
                // (op, _, _, var)
                midCodeList.add("var int " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defVar_normal_AndAssign) {
                // (op, label, _, var)
                midCodeList.add("var int " + des + " = " + label1);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defConst_normal_And_Assign) {
                midCodeList.add("const int " + des + " = " + label1);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defOneDimArray) {
                // (op, size, _, array)
                midCodeList.add("arr int " + des + "[" + label1 + "]");
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defTwoDimArray) {
                // (op, size1, size2, array)
                int size = Integer.parseInt(label1) * Integer.parseInt(label2);
                midCodeList.add("arr int " + des + "[" + size + "]");
            } else if (nowTetrad.getOpType() == Tetrad.OpType.getArrayVal) {
                // (op, array, offset, des)
                midCodeList.add(des + " = " + label1 + "[" + label2 + "]");
            } else if (nowTetrad.getOpType() == Tetrad.OpType.getArrayAddr) {
                midCodeList.add(des + " = &" + label1 + "[" + label2 + "]");    // 地址仅仅用于函数传参
            } else if (nowTetrad.getOpType() == Tetrad.OpType.assignArrayVal) {
                // (op, offset, val, array)
                midCodeList.add(des + "[" + label1 + "]" + " = " + label2);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFuncPara_oneDim) {
                midCodeList.add("para int " + des + "[]");
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFuncPara_twoDim) {
                // (op, num, _, var) 作为函数参数时，第1维长度缺省
                midCodeList.add("para int " + des + "[][" + label1 + "]");
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFunc) {
                // (op, int/void, _, funcName)
                midCodeList.add("\n\n" + label1 + " " + des + "()");    // \n\n是为了最后输出好看
            } else if (nowTetrad.getOpType() == Tetrad.OpType.defFuncPara_normal) {
                // (op, _, _, var)
                midCodeList.add("para int " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.pushPara) {
                // (op, index, _, var)  index为是第几个参数，从1开始
                midCodeList.add("push " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.callFunc) {
                // (op, 参数个数, _, funcName)
                midCodeList.add("call " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.returnVoid) {
                midCodeList.add("retVoid");    // 这是与实验参考手册中不同的
            } else if (nowTetrad.getOpType() == Tetrad.OpType.returnInt) {
                midCodeList.add("retInt " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.printStr) {
                midCodeList.add("printf " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.printInt) {
                midCodeList.add("printf " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.getint) {
                // (op, _, _, ident)
                midCodeList.add("scanf " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bnez) {
                midCodeList.add("bnez " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.beqz) {
                midCodeList.add("beqz " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bltz) {
                midCodeList.add("bltz " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.blez) {
                midCodeList.add("blez " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bgtz) {
                midCodeList.add("bgtz " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.bgez) {
                midCodeList.add("bgez " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.jump) {
                // (op, _, _, label)
                midCodeList.add("j " + des);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.genLabel) {
                midCodeList.add(des + ":");
            }
        }
    }
    
}
