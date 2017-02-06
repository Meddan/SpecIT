import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import com.google.common.base.Strings;

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class ContractGenerator {
    //Should be initialized with a class
    private ClassOrInterfaceDeclaration target;
    private ArrayList<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
    private HashMap<MethodDeclaration, String> contracts = new HashMap<>();
    public ContractGenerator(ClassOrInterfaceDeclaration coid){
        this.target = coid;
        if(target.isInterface()){
            return;
        }
        //Save all class variables of the class
        for (BodyDeclaration<?> b : target.getMembers()){
            if(b instanceof FieldDeclaration){
                fields.add((FieldDeclaration) b);
            }
        }
        //Start generating contracts, method by method
        for (BodyDeclaration<?> b : target.getMembers()){
            if(b instanceof MethodDeclaration){
                contracts.put((MethodDeclaration) b, createContract((MethodDeclaration) b));
            }
        }
        for(MethodDeclaration md : contracts.keySet()){
            System.out.println("-----------------");
            System.out.println();
            System.out.println(md.getName() + " " + contracts.get(md));
            System.out.println();
            System.out.println("-----------------");
        }


    }
    public String createContract(MethodDeclaration md){
        //Get row for md
        //Get top-level assertions
        NodeList<Statement> stmtList =  md.getBody().get().getStatements();
        StringBuilder sb = new StringBuilder();
        for(Statement s : stmtList){
            if(s instanceof AssertStmt){

                //Checks if assertions are at the start of function and can be added as preconditions
                if(startAssert(stmtList, s)){
                    sb.append( "requires " + ((AssertStmt) s).getCheck().toString() + "\n");
                }
                //Checks if assertions are at the end of function and can be added as postconditions
                if(endAssrt(stmtList, s)){
                    sb.append( "ensures " + ((AssertStmt) s).getCheck().toString() + "\n");
                }
                //Checks if we can ignore/integrate statements before assertions.


            }

            if(s instanceof IfStmt){
                String contract = ""; // New contract

                //System.out.println((s));

                // Begin building behavior for first case
                String newBehavior = "requires " + ((IfStmt) s).getCondition().toString() + ";\n";

                // Extract if-body
                Statement ifBody = ((IfStmt) s).getThenStmt();
                if(ifBody instanceof BlockStmt){
                    // It's a block statement, likely to be many statements
                    // But could still be only one

                    // Check body for return
                } else if (ifBody instanceof ReturnStmt){
                    // Body is only a return, not enclosed by { }
                    newBehavior = newBehavior.concat("ensures \\result == "
                            + ((ReturnStmt) ifBody).getExpression().get()) + ";\n";
                } else {
                    // It's not a return but some other single line expression/statement
                }

                // Add behavior to contract
                contract = contract.concat(newBehavior);

                                

                System.out.println(contract);
            }
        }

        return sb.toString();
    }
/*
probably not useful
    private boolean ignoreableStmt(NodeList<Statement> stmtList, Statement s) {
        int index = stmtList.indexOf(s);
        for(int i = 0 ; i < index ; i++){
            Statement stmt = stmtList.get(i);
            if(stmt instanceof ExpressionStmt){
                ExpressionStmt es = (ExpressionStmt) stmt;
                if(!(((ExpressionStmt) stmt).getExpression() instanceof AssignExpr)){
                    return false;
                }
            } else if(!(stmt instanceof AssertStmt)){
                return false;
            }
        }
        return true;
    }
*/
    private boolean endAssrt(NodeList<Statement> stmtList, Statement s) {
        int index = stmtList.indexOf(s);
        for(int i = index ; i < stmtList.size() ; i++){
            if(stmtList.get(i) instanceof ReturnStmt){
                return true;
            } else if(!(stmtList.get(i) instanceof AssertStmt)){
                return false;
            }
        }
        return true;
    }

    private boolean startAssert(NodeList<Statement> stmtList, Statement s){
        int index = stmtList.indexOf(s);
        for(int i = 0 ; i < index ; i++){
            if(!(stmtList.get(i) instanceof AssertStmt)){
                return false;
            }
        }
        return true;
    }

    public static void main(String args[]){
        File projectDir = new File("src/main/java/Examples");
        testClasses(projectDir);
    }
    public static void testClasses(File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                        super.visit(n, arg);
                        ContractGenerator cg = new ContractGenerator(n);
                    }
                }.visit(JavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }
}
