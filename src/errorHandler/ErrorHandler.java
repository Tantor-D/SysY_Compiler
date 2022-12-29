package errorHandler;

import constDecl.SymbolConst;
import lexical.TokenDate;
import symbolTable.Symbol;
import symbolTable.SymbolTable;

import java.io.PrintStream;
import java.util.ArrayList;

public class ErrorHandler {
    private ArrayList<ErrorDate> errorDateList = new ArrayList<>();
    
    public void printErrorMessage(PrintStream errorStream) {
        for (ErrorDate errorDate : errorDateList) {
            errorStream.println(errorDate.getLineNum() + " " + errorDate.getKind());
        }
    }
    
    public void check_A_WrongString_AndUpdateFormatString(TokenDate formatStringSym) {
        String formatString = formatStringSym.getName();
        int paraNum = 0;
        boolean isFaultA = false;
        for (int i = 1; i < formatString.length() - 1; i++) {   // 不要左右两边的"
            char c = formatString.charAt(i);
            if (c == '%') { // ansi(%) = 37
                if (formatString.charAt(i + 1) == 'd') {    // 不会越界
                    paraNum++;
                } else { // %d出错
                    isFaultA = true;
                }
            } else if (c == 32 || c == 33 || (c >= 40 && c <= 126)) {
                // 另外的合法字符
                if (c == '\\') {
                    if (formatString.charAt(i + 1) != 'n') { // 出现\ 且不为\n
                        isFaultA = true;
                    }
                }
            } else { // 不合法的字符
                isFaultA = true;
            }
        }
        formatStringSym.setParaNumInFormatString(paraNum);
        if (isFaultA) {
            formatStringSym.setFormatStringRight(false); // 存在错误A，则不正确
            errorDateList.add(new ErrorDate("a", formatStringSym.getLineNum()));
        } else {
            formatStringSym.setFormatStringRight(true);
        }
    }
    
    public void raise_B_Rename(int lineNum) {
        errorDateList.add(new ErrorDate("b", lineNum));
    }
    
    public void check_C_UndefineIdent(TokenDate sym, SymbolTable nowSymbolTable) {
        // 仅考虑了是否存在ident，没有针对数组和函数进行分类判断
        if (!nowSymbolTable.isIdentExist(sym)) {  // 符号表中没有找到ident
            errorDateList.add(new ErrorDate("c", sym.getLineNum()));
            // System.out.println("A new C error: lineNum = " + sym.getLineNum() + ", sym.name = " + sym.getName());
        }
    }
    
    public void check_D_E_FuncNumOrFuncType(Symbol calledFuncSymbol, ArrayList<Integer> realParaDimList, int errorLine) {
        
        if (calledFuncSymbol.getParaNum() != realParaDimList.size()) {  // 个数参数不匹配
            errorDateList.add(new ErrorDate("d", errorLine));
        } else {
            for (int i = 0; i < realParaDimList.size(); i++) {
                if (realParaDimList.get(i) == SymbolConst.FIT_ANYTHING_PARA) {  // 万能匹配，直接过
                    continue;
                } else if (realParaDimList.get(i) != calledFuncSymbol.getParaDimList().get(i)) {
                    // 若void函数为实参，则一定会报错
                    errorDateList.add(new ErrorDate("e", errorLine));
                    return; // 只要找到了一个错就ok了
                }
            }
        }
    }
    
    public void raise_F_VoidFuncReturnVal(int returnLineNum) {
        errorDateList.add(new ErrorDate("f", returnLineNum));
    }
    
    public void raise_G_IntFuncNoReturnAtLast(int lineNum) {
        errorDateList.add(new ErrorDate("g", lineNum));
    }
    
    public void check_H_TryChangeConst(int lValType, int lValLineNum) {
        if (lValType == SymbolConst.CONST_SYMBOL) {
            errorDateList.add(new ErrorDate("h", lValLineNum));
        }
    }
    
    public void raise_I_lackSemicolon(int lineNum) {
        errorDateList.add(new ErrorDate("i", lineNum));
    }
    
    public void raise_J_lackSmallRightBracket(int lineNum) {
        errorDateList.add(new ErrorDate("j", lineNum));
    }
    
    public void raise_K_lackMiddleRightBracket(int lineNum) {
        errorDateList.add(new ErrorDate("k", lineNum));
    }
    
    public void check_L_UnMatchFormatString(int printfLineNum, TokenDate formatStringSym, int printExpNum) {
        // error L
        if (formatStringSym.getParaNumInFormatString() != printExpNum) {
            errorDateList.add(new ErrorDate("l", printfLineNum));
        }
    }
    
    public void raise_M_WrongBreakContinue(int lineNum) {
        errorDateList.add(new ErrorDate("m", lineNum));
    }
}
