package mipsGenerator;

import java.util.ArrayList;
import java.util.HashMap;

public class RunTimeStack {
    private String funcName;
    private int stackSize;
    private HashMap<String, StackItem> itemMap;
    
    public RunTimeStack(String funcName) {
        this.funcName = funcName;
        stackSize = 8;
        itemMap = new HashMap<>();
    }
    
    public void storeVarAndGenCode(String identName, String regName, ArrayList<String> mipsList) {
        // 使用时无需管某个变量曾经有没有被分配过空间
        if (itemMap.containsKey(identName)) {   // 栈中已经保存了这个值的信息，现在只需要更新
            int offset = itemMap.get(identName).getOffset();
            mipsList.add("sw " + regName + ", " + offset + "($fp)");
        } else {    // 栈中没有保存过这个变量，需要重新分配空间
            stackSize += 4;
            StackItem newItem = new StackItem(identName, -1 * stackSize);
            itemMap.put(identName, newItem);
            mipsList.add("sw " + regName + ", " + -1 * stackSize + "($fp)");
        }
    }
    
    public void addNewArrayAndGenCode(String arrayName, int arraySize, ArrayList<String> mipsList) {
        // 用于数组定义的时候，需要分配空间
        // debug用
        if (itemMap.containsKey(arrayName)) {   // 栈中已经保存了这个值的信息，需要报错
            System.out.println("运行栈上定义数组时出现了重名");
        }
        // 实际起作用的部分，首先是分配数组的实际空间，然后是开一个空间存数组的首地址
        stackSize += arraySize * 4;
        mipsList.add("subi $t0, $fp, " + stackSize);   // 计算出数组的首地址，注意栈是从高地址向低地址生长，数组是从低地址向高地址算偏移
        stackSize += 4;
        StackItem newItem = new StackItem(arrayName, -1 * stackSize);
        itemMap.put(arrayName, newItem);
        mipsList.add("sw $t0, " + -1 * stackSize + "($fp)");
    }
    
    
    
    public void addNewVarWithNoInitVal(String identName) {
        if (itemMap.containsKey(identName)) {   // 栈中已经保存了这个值的信息，现在只需要更新
            // debug使用
            System.out.println("在定义无初值 的 非全局变量时，出现了重名");
        } else {    // 栈中没有保存过这个变量，需要重新分配空间。由于没有初值，所以只用加一个记录项就可以了，并不需要对那一片空间进行实际上的操作
            stackSize += 4;
            StackItem newItem = new StackItem(identName, -1 * stackSize);
            itemMap.put(identName, newItem);
        }
    }
    
    public int getItemOffset(String identName) {
        return itemMap.get(identName).getOffset();
    }
    
    public int getStackSize() {
        return stackSize;
    }
}
