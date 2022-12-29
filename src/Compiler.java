import lexical.LexicalAnalyser;
import mipsGenerator.MipsGenerator;
import optimizer.MidCodePrinter;
import optimizer.Optimizer;
import parser.Parser;
import tetradGenerator.TetradGenerator;

public class Compiler {
    
    public static void main(String[] args) {
        // 词法分析器
        LexicalAnalyser lexicalAnalyser = new LexicalAnalyser();
        lexicalAnalyser.startLexicalAnalyse();
        
        // 语法分析
        Parser parser = new Parser(lexicalAnalyser.getTokenDateList());
        parser.startToParse();
        
        // 语义分析与中间代码生成
        TetradGenerator tetradGenerator = new TetradGenerator(
                parser.getTreeRoot(), parser.getBaseSymbolTable()
        );
        tetradGenerator.startToGen();
        
        // 输出优化前的代码
        MidCodePrinter midCodePrinter1 = new MidCodePrinter(
                tetradGenerator.getTetradList(),
                tetradGenerator.getStrToPrintList(),
                "testfilei_20373921_丁盛为_优化前中间代码.txt"
        );
        midCodePrinter1.start();
    
        // 代码优化
        Optimizer optimizer = new Optimizer(
                tetradGenerator.getTetradList(),
                tetradGenerator.getStrToPrintList());
        optimizer.startToOptimize();
        
        // 输出优化后的代码
        MidCodePrinter midCodePrinter2 = new MidCodePrinter(
                optimizer.getFinalTetradList(),
                optimizer.getFinalStrToPrintList(),
                "testfilei_20373921_丁盛为_优化后目标代码.txt"
        );
        midCodePrinter2.start();
    
        // 目标代码生成
        // MipsGenerator mipsGenerator = new MipsGenerator(optimizer.getFinalTetradList(), optimizer.getFinalStrToPrintList());
        MipsGenerator mipsGenerator = new MipsGenerator(tetradGenerator.getTetradList(), tetradGenerator.getStrToPrintList());
        mipsGenerator.startToGenMIPS();
    }
    
}
