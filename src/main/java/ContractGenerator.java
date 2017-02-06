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
            System.out.println(md.getName() + "\n" + contracts.get(md));
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

            Statement temp = s;
            LinkedList preCons = new LinkedList<String>();
            while(temp instanceof IfStmt){
                String contract = ""; // New contract

                // Begin building behavior for first case
                String newBehavior = "requires " + ((IfStmt) temp).getCondition().toString() + ";\n";

                // Add expressions of pre-conditions for safekeeping
                preCons.add(((IfStmt) temp).getCondition().toString());

                // Check if-body for contract info
                newBehavior = newBehavior.concat(checkIfBody(((IfStmt) temp).getThenStmt()));

                // Add behavior to contract
                sb.append(newBehavior);

                // If the next statement is also an if-statement, redo the loop
                if(((IfStmt) temp).getElseStmt().isPresent()){ // There is some statement

                    //Prepare new behavior
                    sb.append("also\n");

                    if(((IfStmt) temp).getElseStmt().get() instanceof IfStmt){
                        // That statement is an if-statement

                        // Set temp to new if-statement, so next iteration of
                        // loop will handle contract generation
                        temp = ((IfStmt) temp).getElseStmt().get();

                    } else {
                        // This is the else-statement. It should be written as a new behavior
                        // with a pre-condition that is the negation of all previous requirements.
                        sb.append(genElsePreCondition(preCons));

                        // It is not an if-statement (but it is the body of an else)
                        // Therefore, it is a block-statement or some single-line statement
                        // Check body to extract contract (post-condition)
                        sb.append(checkIfBody(((IfStmt) temp).getElseStmt().get()));

                        break; // Break out of while-loop as there are no more if-cases
                    }

                } else { // There is no else-statement
                    // What do we do?! Panic.
                    break; // No more if-cases and no else-statement, break out of loop
                }

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

    /* Takes a list of Strings
     * Generates the pre-condition that is the negation of those strings */
    private String genElsePreCondition (LinkedList<String> array) {

        StringBuilder sb = new StringBuilder();

        sb.append("requires ");

        for(String s : array){

            sb.append("!(" + s + ")");

            if(array.getLast().equals(s)){
                sb.append(" && ");
            }
        }

        sb.append("\n");

        return sb.toString();
    }

    /* Will most likely return some post-condition */
    private String checkIfBody (Statement body){
        StringBuilder sb = new StringBuilder();
        if(body instanceof BlockStmt){
            // It's a block statement, likely to be many statements
            // But could still be only one

            // Get all highest level statements in block
            List<Node> bodyStmts = body.getChildNodes();

            if(bodyStmts.get(0) instanceof ReturnStmt){
                // Is the first statement a return statement? Easy money.
                // TODO : What if empty return? Should be handled.
                sb.append("ensures \\result == "
                        + ((ReturnStmt) bodyStmts.get(0)).getExpression().get() + "\n");
            } else if (bodyStmts.get(0) instanceof ThrowStmt) {
                // Is the first statement a throw statement? Big dollahs.

            } else {
                // The first statement was not a return statement. Time to think.
                // TODO : Implement logic
                sb.append("NOT YET IMPLEMENTED\n");
            }


            // Check body for return

            // TODO : Fill with logic
        } else if (body instanceof ReturnStmt){
            // Body is only a return, not enclosed by { }
            // TODO : What if empty return? Should be handled.
            sb.append("ensures \\result == "
                    + ((ReturnStmt) body).getExpression().get() + ";\n");
        } else {
            sb.append("NOT YET IMPLEMENTED\n");
            // It's not a return but some other single line expression/statement
            // TODO : Fill with logic
        }

        return sb.toString();
    }

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
