package optimizer;

import tetradGenerator.Tetrad;

import java.math.BigInteger;
import java.util.ArrayList;

public class Optimizer {
    private final ArrayList<Tetrad> preTetradList;
    private final ArrayList<String> preStrToPrintList;
    
    private final ArrayList<Tetrad> finalTetradList;
    private final ArrayList<String> finalStrToPrintList;
    
    private boolean needOptimizeDivWithMul = false;
    
    public Optimizer(ArrayList<Tetrad> preTetradList, ArrayList<String> preStrToPrintList) {
        this.preTetradList = preTetradList;
        this.preStrToPrintList = preStrToPrintList;
        
        this.finalTetradList = new ArrayList<>();
        this.finalStrToPrintList = preStrToPrintList;   // 放弃优化字符串输出优化
    }
    
    public ArrayList<Tetrad> getFinalTetradList() {
        return finalTetradList;
    }
    
    public ArrayList<String> getFinalStrToPrintList() {
        return finalStrToPrintList;
    }
    
    public void startToOptimize() {
        // 仅优化乘除法
        for (int i = 0; i < preTetradList.size(); i++) {
            Tetrad nowTetrad = preTetradList.get(i);
            if (nowTetrad.getOpType() == Tetrad.OpType.mul) {
                optomizeMul(nowTetrad);
            } else if (nowTetrad.getOpType() == Tetrad.OpType.div) {
                optomizeDiv(nowTetrad);
            } else {
                finalTetradList.add(nowTetrad);
            }
        }
    }
    
    private void optomizeMul(Tetrad nowTetrad) {
        if (isStrInt(nowTetrad.getLabel1()) || isStrInt(nowTetrad.getLabel2())) {
            String intStr = isStrInt(nowTetrad.getLabel1()) ? nowTetrad.getLabel1() : nowTetrad.getLabel2();
            String identStr = isStrInt(nowTetrad.getLabel1()) ? nowTetrad.getLabel2() : nowTetrad.getLabel1();
            if (Integer.parseInt(intStr) < 0) {
                // 负数，不做优化
                finalTetradList.add(nowTetrad);
            } else if (Integer.parseInt(intStr) == 0) {
                // 0，直接赋0
                finalTetradList.add(new Tetrad(Tetrad.OpType.assign, "0", null, nowTetrad.getDes()));
            } else {
                // 正数
                ArrayList<Integer> eList = getShiftNum(Integer.parseInt(intStr));
                if (eList.size() != 1) {
                    finalTetradList.add(nowTetrad);
                } else {
                    // 乘法中其中的一个数为2^i
                    finalTetradList.add(new Tetrad(Tetrad.OpType.shl, identStr, Integer.toString(eList.get(0)), nowTetrad.getDes()));
                }
            }
        } else {
            // 没有常数，不做优化
            finalTetradList.add(nowTetrad);
        }
    }
    
    private void optomizeDiv(Tetrad nowTetrad) {
        if (!isStrInt(nowTetrad.getLabel2())) { // 除数不是常数的话，就不用管了
            finalTetradList.add(nowTetrad);
            return;
        }
        String intStr = nowTetrad.getLabel2();
        String identStr = nowTetrad.getLabel1();
        
        if (needOptimizeDivWithMul) {
            // 用乘法来优化除法
            boolean isNeg;  // 标志除数是不是负数
            long long_pos_lb2 = (long) Integer.parseInt(intStr);
            isNeg = long_pos_lb2 < 0;
            long_pos_lb2 = long_pos_lb2 < 0 ? long_pos_lb2 * -1 : long_pos_lb2;
            
            // 对2的整次幂的情况进行特殊处理
            ArrayList<Integer> eList = getShiftNum((int) long_pos_lb2);
            if (eList.size() == 1) {
                // 乘法中其中的一个数为2^i
                finalTetradList.add(new Tetrad(Tetrad.OpType.shr, identStr, Integer.toString(eList.get(0)), nowTetrad.getDes()));
                if (isNeg) {
                    finalTetradList.add(new Tetrad(Tetrad.OpType.mul, nowTetrad.getDes(), "-1", nowTetrad.getDes()));
                }
                return;
            }
            
            // 对一般情况进行处理
            int k = 0;
            long m;
            BigInteger big_c = new BigInteger(Long.toString(long_pos_lb2));
            BigInteger big_2 = new BigInteger("2");
            BigInteger big_m;
            for (int i = 0; i < 31; i++) {
                // i最大到30，除数一定小于2^31
                if ((1 << i) < long_pos_lb2 && long_pos_lb2 < (1L << (i + 1))) {
                    k = i;
                    break;
                }
            }
            big_m = (big_2.pow(k + 32)).divide(big_c);
            if (big_2.pow(k + 32).mod(big_c).compareTo(BigInteger.ZERO) == 0) {
                // 取余后结果为0，向上取整的话就不变
                m = big_m.longValue();
            } else {
                m = big_m.longValue() + 1;
            }
            finalTetradList.add(
                    new Tetrad(Tetrad.OpType.mul, identStr, Long.toString(m), nowTetrad.getDes()));
            finalTetradList.add(
                    new Tetrad(Tetrad.OpType.shr, nowTetrad.getDes(), Integer.toString(k), nowTetrad.getDes()));
            // 除的是负数的话，还需要再乘-1
            if (isNeg) {
                finalTetradList.add(
                        new Tetrad(Tetrad.OpType.mul, nowTetrad.getDes(), "-1", nowTetrad.getDes()));
            }
            
        } else {
            // 此处是不用乘法来优化除法的
            // 仅仅优化除数为整数且为2的整次幂的情况
            if (Integer.parseInt(nowTetrad.getLabel2()) > 0) {
                // 不会出现被除数为0
                // 被除数为负的话暂时不管，只除正的
                ArrayList<Integer> eList = getShiftNum(Integer.parseInt(intStr));
                if (eList.size() != 1) {
                    finalTetradList.add(nowTetrad);
                } else {
                    // 乘法中其中的一个数为2^i
                    finalTetradList.add(new Tetrad(Tetrad.OpType.shr, identStr, Integer.toString(eList.get(0)), nowTetrad.getDes()));
                }
            } else {
                finalTetradList.add(nowTetrad);
            }
        }
        
    }
    
    private ArrayList<Integer> getShiftNum(int positiveNum) {
        ArrayList<Integer> retList = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if ((positiveNum & (1 << i)) != 0) {
                retList.add(i);
            }
        }
        return retList;
    }
    
    private boolean isStrInt(String str) {
        return str.matches("^[-\\+]?[\\d]*");
    }
    
    private boolean isCalculateOpType(Tetrad.OpType opType) {
        return opType == Tetrad.OpType.unaryOp_add ||
                opType == Tetrad.OpType.unaryOp_sub ||
                opType == Tetrad.OpType.add ||
                opType == Tetrad.OpType.sub ||
                opType == Tetrad.OpType.div ||
                opType == Tetrad.OpType.mod ||
                opType == Tetrad.OpType.mul;
    }
    
}
