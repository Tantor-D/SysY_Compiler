package parser;

import lexical.TokenDate;
import symbolTable.SymbolTable;

import java.util.ArrayList;

public class TreeNode {
    private String name;
    // name可能为 Ident、IntConst、FormatString 或 代码中的原语句。
    // 注意前三者是代号，源代码中的值要进入token中查看
    private ArrayList<TreeNode> childList;
    private int childNum;
    
    private TreeNode fatherNode;
    private TokenDate tokenDate;
    private boolean isLeaf;
    
    private SymbolTable newTable;
    
    public TreeNode(String name, TokenDate tokenDate) {
        this.name = name;
        this.tokenDate = tokenDate;
        childNum = 0;
        childList = new ArrayList<>();
        if (tokenDate != null) {
            isLeaf = true;
        } else {
            isLeaf = false;
        }
        newTable = null;
    }
    
    public void addChild(TreeNode child) {
        childList.add(child);
        childNum++;
        child.setFatherNode(this);
    }
    
    public boolean isThisKind(String str) {
        return str.equals(name);
    }
    
    ////////////////////////////////////////////////////////////////
    // gets and sets
    public SymbolTable getNewTable() {
        return newTable;
    }
    
    public void setNewTable(SymbolTable newTable) {
        this.newTable = newTable;
    }
    
    private void setFatherNode(TreeNode fatherNode) {
        this.fatherNode = fatherNode;
    }
    
    public String getName() {
        return name;
    }
    
    public ArrayList<TreeNode> getChildList() {
        return childList;
    }
    
    public int getChildNum() {
        return childNum;
    }
    
    public TreeNode getFatherNode() {
        return fatherNode;
    }
    
    public TokenDate getTokenDate() {
        return tokenDate;
    }
    
    public boolean isLeaf() {
        return isLeaf;
    }
}
