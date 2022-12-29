package lexical;

import constDecl.LexicalConst;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;

public class LexicalAnalyser {
    private final Scanner scanner;
    private final PrintStream sysOutStream;
    
    // 词法分析的结果保留在这里
    private final ArrayList<TokenDate> tokenDateList;
    
    // 为了对每行字符串进行处理
    private String readStr;
    private int lenOfReadStr;
    private char nowChar;   // nowChar对应的是目前正在处理的字符
    private int nowIndex;   // 对应了nowChar对应的index，因此每次读的时候index要++
    private final StringBuffer token;
    private boolean isEnd;
    private int lineNum;
    private boolean flagChangeLine;
    
    public LexicalAnalyser() {
        Scanner scanner = null;
        
        // 修改输入
        try {
            File file = new File("./testfile.txt");     // 相对路径是相对的程序入口，也有是src目录下
            scanner = new Scanner(Files.newInputStream(file.toPath()), "UTF-8");  // 这样就过了
        } catch (Exception e) {
            System.out.println("修改读入失败");
        }
        
        this.scanner = scanner;
        sysOutStream = System.out;
        
        tokenDateList = new ArrayList<>();
        isEnd = false;
        lineNum = 0;
        token = new StringBuffer();
    }
    
    public void startLexicalAnalyse() {
        nowChar = '\n';
        getChar(); // 因为函数间的约定，getSym默认一开始就已经读入了一个char
        
        while (!isEnd) {
            getSym();
        }
    }
    
    public ArrayList<TokenDate> getTokenDateList() {
        return tokenDateList;
    }
    
    private void getSym() {
        // sysOutStream.println("start new getSym");
        cleanToken();
        while (isSpace() || isNewLine() || isTab()) {
            getChar();
            if (isEnd) {
                return;
            }
        }
        
        // 正式开始词法分析
        if (isIdentNonDigit()) {
            while (isIdentNonDigit() || isDigit()) {
                catToken(); // 第一个字母也放到这来加了
                getChar();
            }
            
            String category = judgeReserver();
            tokenDateList.add(new TokenDate(token.toString(), category, lineNum));
            
        } else if (isDigit()) {
            while (isDigit()) {
                catToken();
                getChar();
            } // 数字这里不能为前导0，之后要查错的
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_INT_CONST, lineNum));
            
        } else if (isDoubleQuota()) {
            catToken();
            while (true) {
                getChar();
                catToken(); // 这里也是要有查错的，字符串中的内容是有限制的
                if (isDoubleQuota()) { // 这个要小心陷入死循环
                    break;
                }
            }
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_FORMAT_STR, lineNum));
            
        } else if (isAnd()) {
            catToken();
            getChar();
            if (isAnd()) {
                catToken();
                getChar();
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_2AND, lineNum));
            } else {
                // error
                return;
            }
            
        } else if (isOr()) { // ||
            catToken();
            getChar();
            if (isOr()) {
                catToken();
                getChar();
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_2OR, lineNum));
            } else {
                // error
                return;
            }
            
        } else if (isPlus()) { // +
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_PLUS, lineNum));
            
        } else if (isMinus()) { // -
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_MINUS, lineNum));
            
        } else if (isStar()) { // *
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_MULTIPLICATION, lineNum));
            
        } else if (isDivision()) { // '/'
            catToken();
            getChar();
            if (isDivision()) {   //   注释//
                while (!flagChangeLine && !isEnd) {
                    getChar();
                }
                return;
            } else if (isStar()) {  // /*
                sysOutStream.println("start of /*");
                char lastChar;
                getChar(); // 无论如何都要先读一个字符
                lastChar = nowChar;
                while (!isEnd) {
                    getChar();
                    if (lastChar == '*' && isDivision()) {
                        getChar();
                        break;
                    }
                    lastChar = nowChar;
                }
                sysOutStream.println("end of */");
                return;
            } else {
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_DIVISION, lineNum));
            }
            
            
        } else if (isMod()) {   // %
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_MOD, lineNum));
            
        } else if (isLess()) {  // <
            catToken();
            getChar();
            if (isEqu()) { // <=
                catToken();
                getChar();
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_LESS_EQUAL, lineNum));
            } else { // <
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_LESS, lineNum));
            }
            
        } else if (isGreater()) {   // >
            catToken();
            getChar();
            if (isEqu()) {  // >=
                catToken();
                getChar();
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_GREATER_EQUAL, lineNum));
            } else {    // >
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_GREATER, lineNum));
            }
            
        } else if (isExclamation()) {   // !
            catToken();
            getChar();
            if (isEqu()) {  // !=
                catToken();
                getChar();
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_NOT_EQUAL, lineNum)); // !=
            } else { // !
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_EXCLAMATION, lineNum)); // !
            }
            
        } else if (isEqu()) {
            catToken();
            getChar();
            if (isEqu()) { // ==
                catToken();
                getChar();
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_2EQUAL, lineNum));
            } else {  // =
                tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_ASSIGN_1EQUAL, lineNum));
            }
            
        } else if (isSemi()) { // ;
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_SEMICOLON, lineNum));
        } else if (isComma()) { // ,
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_COMMA, lineNum));
        } else if (isLSmall()) { // (
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_LEFT_SMALL, lineNum));
        } else if (isRSmall()) { // )
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_RIGHT_SMALL, lineNum));
        } else if (isLMid()) { // [
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_LEFT_MIDDLE, lineNum));
        } else if (isRMid()) { // ]
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_RIGHT_MIDDLE, lineNum));
        } else if (isLBig()) { // {
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_LEFT_BIG, lineNum));
        } else if (isRBig()) { // }
            catToken();
            getChar();
            tokenDateList.add(new TokenDate(token.toString(), LexicalConst.IDENT_RIGHT_BIG, lineNum));
        } else if (isDivision()) {
            getChar();
            
        } else {
            // error
            return;
        }
    }
    
    /*
    getChar()的设计逻辑。 一次读一个字符，记录当前所读的字符串readSt、当前所读的索引
     */
    public void getChar() {
        flagChangeLine = false;
        if (nowChar == '\n') {  // 上次读到的是一个换行符
            if (scanner.hasNextLine()) {
                flagChangeLine = true;
                lineNum++;
                readStr = scanner.nextLine();
                lenOfReadStr = readStr.length();
                nowIndex = -1;
            } else { // 到头了
                isEnd = true;
                return;
            }
        }
        nowIndex++;
        if (nowIndex >= lenOfReadStr) {
            nowChar = '\n';
            if (!scanner.hasNextLine()) {   // 已经是最后一行了
                isEnd = true;
            }
        } else {
            nowChar = readStr.charAt(nowIndex);
        }
        
    }
    
    public String judgeReserver() {
        String nowStr = token.toString();
        // 以下和idenfr的前缀有交集
        if (nowStr.equals("main")) {
            return LexicalConst.IDENT_MAIN;
        } else if (nowStr.equals("const")) {
            return LexicalConst.IDENT_CONST;
        } else if (nowStr.equals("int")) {
            return LexicalConst.IDENT_INT;
        } else if (nowStr.equals("break")) {
            return LexicalConst.IDENT_BREAK;
        } else if (nowStr.equals("continue")) {
            return LexicalConst.IDENT_CONTINUE;
        } else if (nowStr.equals("if")) {
            return LexicalConst.IDENT_IF;
        } else if (nowStr.equals("else")) {
            return LexicalConst.IDENT_ELSE;
        } else if (nowStr.equals("while")) {
            return LexicalConst.IDENT_WHILE;
        } else if (nowStr.equals("getint")) {
            return LexicalConst.IDENT_GETINT;
        } else if (nowStr.equals("printf")) {
            return LexicalConst.IDENT_PRINTF;
        } else if (nowStr.equals("return")) {
            return LexicalConst.IDENT_RETURN;
        } else if (nowStr.equals("void")) {
            return LexicalConst.IDENT_VOID;
        } else {
            return LexicalConst.IDENT_IDENT;
        }
    }
    
    public void catToken() {
        token.append(nowChar);
    }
    
    public void cleanToken() {
        token.delete(0, token.length());    // 不是很确定行不行，因为要toString
    }
    
    ///////////////////////////////////////////////////////////////// 以下为is xxx函数
    public boolean isLetter() {
        return ('a' <= nowChar && nowChar <= 'z') || ('A' <= nowChar && nowChar <= 'Z');
    }
    
    public boolean isIdentNonDigit() {
        return ('a' <= nowChar && nowChar <= 'z') ||
                ('A' <= nowChar && nowChar <= 'Z') ||
                (nowChar == '_');
    }
    
    public boolean isDigit() {
        return ('0' <= nowChar && nowChar <= '9');
    }
    
    public boolean isDoubleQuota() {
        return nowChar == '"';
    }
    
    public boolean isSpace() {
        return nowChar == ' ';
    }
    
    public boolean isNewLine() {
        return nowChar == '\n';
    }
    
    public boolean isTab() {
        return nowChar == '\t';
    }
    
    public boolean isExclamation() {
        return nowChar == '!';
    } // exclamation
    
    public boolean isComma() {
        return nowChar == ',';
    } // comma
    
    public boolean isSemi() {
        return nowChar == ';';
    } // semicolon
    
    public boolean isEqu() {
        return nowChar == '=';
    }
    
    public boolean isPlus() {
        return nowChar == '+';
    }
    
    public boolean isMinus() {
        return nowChar == '-';
    }
    
    public boolean isDivision() {
        return nowChar == '/';
    }
    
    public boolean isStar() {
        return nowChar == '*';
    }
    
    public boolean isLSmall() {
        return nowChar == '(';
    }
    
    public boolean isRSmall() {
        return nowChar == ')';
    }
    
    public boolean isLMid() {
        return nowChar == '[';
    }
    
    public boolean isRMid() {
        return nowChar == ']';
    }
    
    public boolean isLBig() {
        return nowChar == '{';
    }
    
    public boolean isRBig() {
        return nowChar == '}';
    }
    
    public boolean isAnd() {
        return nowChar == '&';
    }
    
    public boolean isOr() {
        return nowChar == '|';
    }
    
    public boolean isMod() {
        return nowChar == '%';
    }
    
    public boolean isLess() {
        return nowChar == '<';
    }
    
    public boolean isGreater() {
        return nowChar == '>';
    }
    
    
}
