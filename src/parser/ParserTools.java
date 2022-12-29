package parser;

import constDecl.NodeConst;
import lexical.TokenDate;

import java.io.PrintStream;
import java.util.ArrayList;

public class ParserTools {
    
    private final ArrayList<TokenDate> tokenDateList;
    
    public void printAllToken() {
        System.out.println("-------------");
        int debugTokenCount = 0;
        for (TokenDate tokenDate : tokenDateList) {
            System.out.println(debugTokenCount++ + " " + tokenDate.getName());
        }
        System.out.println("-------------");
    }
    
    public void postOrderPrintTheTree(TreeNode nowNode, PrintStream outputTxtStream) {
        if (nowNode.isLeaf()) { // 对于叶子节点直接输出
            outputTxtStream.println(nowNode.getTokenDate().getCategory() + " " + nowNode.getTokenDate().getName());
            return;
        }
        
        for (TreeNode childNode : nowNode.getChildList()) {
            postOrderPrintTheTree(childNode, outputTxtStream);
        }
        if (nowNode.getName().equals(NodeConst.BlockItem) ||
                nowNode.getName().equals(NodeConst.Decl) ||
                nowNode.getName().equals(NodeConst.BType)) {
            return;
        }
        outputTxtStream.println("<" + nowNode.getName() + ">");
    }
    
    public ParserTools(ArrayList<TokenDate> tokenDateList) {
        this.tokenDateList = tokenDateList;
    }
    
    public boolean needParseExp(int indexOfToken) {
        if (!isExpFirst(tokenDateList.get(indexOfToken))) {
            return false;   // 不是First(exp)
        }
        // 不是ident的First(Exp)，可以直接开始parseExp
        if (!tokenDateList.get(indexOfToken).isIdent()) {
            return true;
        }
        
        // 目前已经确定是First(exp)了，就是要区分 Lval开头的Stmt 和 exp
        for (int i = indexOfToken; i < tokenDateList.size(); i++) {
            if (tokenDateList.get(i).getName().equals("=")) {
                return false;
            }
            if (tokenDateList.get(i).getName().equals(";") ||
                    tokenDateList.get(i).getLineNum() != tokenDateList.get(i-1).getLineNum()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isExpFirst(TokenDate sym) {
        return sym.isIdent() ||
                sym.isNameEqual("(") ||
                sym.isIntConst() ||
                sym.isNameEqual("+") ||
                sym.isNameEqual("-") ||
                sym.isNameEqual("!");
    }
    
    public boolean isFuncRealParamsFirst(TokenDate sym) {
        return isExpFirst(sym);
    }
}
