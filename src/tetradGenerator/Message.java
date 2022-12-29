package tetradGenerator;

import symbolTable.SymbolTable;

public class Message {
    // 这个类中存的是进行函数制导的翻译时 被传递的信息
    public enum MesKind {
        ident_normal,       // 变量ident
        ident_and_offset,   // mes里面存ident，offset里面存offset。仅仅用于数组作为真正的左值要刷新值的时候使用
        temp,               // 临时变量t_x
        num,                // 在中间代码生成阶段完成所有num和const的计算。 这样一是后面的优化少了很多事，二是实现了编译阶段的数组维度计算需求
        
        // 已经实现了对funcRet使用temp封装返回，因此无需增加funcRet这一个MesKind了，不会有这种信息的出现
        // funcRet,    // UnaryExp 调用的函数，返回的应是一个RET。 mes = funcRet  实际上现在已经无用了，因为使用temp进行了Ret的封装
        
        noUse
    }
    
    private MesKind mesKind;
    private String mes;     // 始终要维护这一字段，即便是数字
    private String offset_rightMes;  // 本质是一个中转站，仅用于一个地方，其内部存储的是RightMes，因此上层在获取offset的时候不需要执行getRightMes的操作
    private int num;        // 当mesKind = num时使用，表示传递的值。
    // 特别需要注意的是，即使MesKind = num，依旧要维护mes = Integer.toSting(num)，即始终要维护mes的正确性
    
    public Message(MesKind mesKind, String mes) {
        this.mesKind = mesKind;
        this.mes = mes;
        if (mesKind == MesKind.num) {
            num = Integer.parseInt(mes);
        }
    }
    
    public Message(MesKind mesKind, int num) {
        this.mesKind = mesKind;
        this.mes = Integer.toString(num);
        this.num = num;
    }
    
    public Message(MesKind mesKind, String mes, String offset_rightMes) {
        this.mesKind = mesKind;
        this.mes = mes;
        this.offset_rightMes = offset_rightMes;
    }
    
    public String getRightMes(SymbolTable nowSymbolTable) { // 用于代码生成阶段，对于ident需要返回的是fake_name
        if (mesKind == MesKind.ident_normal || mesKind == MesKind.ident_and_offset) {
            return nowSymbolTable.getAliveFakeName(mes);
        } else {
            return mes;
        }
    }
    
    ///////////////////////////////////////////////////
    // gets and sets
    public String getOffset_rightMes() {
        return offset_rightMes;
    }
    
    public MesKind getMesKind() {
        return mesKind;
    }
    
    public void setMesKind(MesKind mesKind) {
        this.mesKind = mesKind;
    }
    
    public String getMes() {
        return mes;
    }
    
    public void setMes(String mes) {
        this.mes = mes;
    }
    
    public int getNum() {
        return num;
    }
    
    public void setNum(int num) {
        this.num = num;
    }
}
