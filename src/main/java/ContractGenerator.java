import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
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
                //Check if first / statements can be ignored

                if(stmtList.indexOf(s) == 0 || allAssert(stmtList, s)){
                    sb.append( "requires " +  ((AssertStmt) s).getCheck().toString() + "\n");
                }
                //Check distance from start / end to determine if assertion is pre/post condition
                //Check if statements between allow for ignore
            }
        }

        return sb.toString();
    }
    private boolean allAssert(NodeList<Statement> stmtList, Statement s){
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
