package mipsGenerator;

import tetradGenerator.Tetrad;

import java.util.ArrayList;

public class MipsGeneratorTool {
    private ArrayList<String> mipsList;
    
    public MipsGeneratorTool(ArrayList<String> mipsList) {
        this.mipsList = mipsList;
    }
    
    public boolean isStrInt(String str) {
        return str.matches("^[-\\+]?[\\d]*");
    }
    
    public boolean isCalculateOpType(Tetrad.OpType opType) {
        return opType == Tetrad.OpType.unaryOp_add ||
                opType == Tetrad.OpType.unaryOp_sub ||
                opType == Tetrad.OpType.add ||
                opType == Tetrad.OpType.sub ||
                opType == Tetrad.OpType.div ||
                opType == Tetrad.OpType.mod ||
                opType == Tetrad.OpType.mul ||
                opType == Tetrad.OpType.shl ||
                opType == Tetrad.OpType.shr;
    }
    
    public boolean isGlobalVar(String ident) {
        return ident.charAt(0) == 'g' && ident.charAt(1) == '_';
    }
    
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    // 生成相应的mips语句
    
    public void gen_srl(String regDes, String regFrom, int shiftNum) {
        mipsList.add("srl " + regDes + ", " + regFrom + ", " + shiftNum);
    }
    
    public void gen_sll(String regDes, String regFrom, int shiftNum) {
        mipsList.add("sll " + regDes + ", " + regFrom + ", " + shiftNum);
    }
    public void gen_move(String regDes, String regFrom) {
        mipsList.add("move " + regDes + ", " + regFrom);
    }
    
    public void gen_sw(String regToStore, int offset, String regBasePlace) {
        mipsList.add("sw " + regToStore + ", " + offset + "(" + regBasePlace + ")");
    }
    
    public void gen_lw(String regToLoad, int offset, String regBasePlace) {
        mipsList.add("lw " + regToLoad + ", " + offset + "(" + regBasePlace + ")");
    }
    
    public void gen_li(String reg, int num) {
        mipsList.add("li " + reg + ", " + num);
    }
    
    public void gen_la(String reg, String label) {
        mipsList.add("la " + reg + ", " + label);
    }
    
    public void gen_syscall() {
        mipsList.add("syscall");
    }
    
    public void gen_anything(String str) {
        mipsList.add(str);
    }
    
    // 计算相关
    public void gen_add(String desReg, String reg1, String reg2) {
        mipsList.add("add " + desReg + ", " + reg1 + ", " + reg2);
    }
    
    public void gen_subi(String desRegName, String regName, int num) {
        mipsList.add("subi " + desRegName + ", " + regName + ", " + num);
    }
    
    public void gen_sub(String desReg, String reg1, String reg2) {
        mipsList.add("sub " + desReg + ", " + reg1 + ", " + reg2);
    }
    
    public void gen_mult(String reg1, String reg2) {
        mipsList.add("mult " + reg1 + ", " + reg2);
    }
    
    public void gen_mflo(String reg) {
        mipsList.add("mflo " + reg);
    }
    
    public void gen_mfhi(String reg) {
        mipsList.add("mfhi " + reg);
    }
    
    public void gen_div(String reg1, String reg2) {
        mipsList.add("div " + reg1 + ", " + reg2);
    }
    
    // 跳转相关
    public void gen_bnez(String reg1, String label) {
        mipsList.add("bnez " + reg1 + ", " + label);
    }
    
    public void gen_beqz(String reg1, String label) {
        mipsList.add("beqz " + reg1 + ", " + label);
    }
    
    public void gen_bgez(String reg1, String label) {
        mipsList.add("bgez " + reg1 + ", " + label);
    }
    
    public void gen_bgtz(String reg1, String label) {
        mipsList.add("bgtz " + reg1 + ", " + label);
    }
    
    public void gen_blez(String reg1, String label) {
        mipsList.add("blez " + reg1 + ", " + label);
    }
    
    public void gen_bltz(String reg1, String label) {
        mipsList.add("bltz " + reg1 + ", " + label);
    }
    
    public void gen_j(String label) {
        mipsList.add("j " + label);
    }
    
    public void gen_label(String label) {
        mipsList.add(label + ":");
    }

}
