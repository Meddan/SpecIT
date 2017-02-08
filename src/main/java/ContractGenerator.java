import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.model.methods.MethodUsage;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.base.Strings;

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.StampedLock;

public class ContractGenerator {

    //Should be initialized with a class
    private ClassOrInterfaceDeclaration target;
    private ArrayList<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
    private HashMap<MethodDeclaration, String> contracts = new HashMap<>();
    private CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

    public ContractGenerator(ClassOrInterfaceDeclaration coid){
        this.target = coid;
        if(target.isInterface()){
            return;
        }
        //Create the combinedTypeSolver
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java/Examples")));
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
            System.out.println("Purity status: " + syntacticlyPure(md));
            System.out.println();
            System.out.println("-----------------");
        }


    }
    public String createContract(MethodDeclaration md){
        //Get row for md
        //Get top-level assertions
        NodeList<Statement> stmtList =  md.getBody().get().getStmts();
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
                newBehavior = newBehavior.concat(checkIfBody(((IfStmt) temp).getThenStmt(), preCons));

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
                        sb.append(checkIfBody(((IfStmt) temp).getElseStmt().get(), preCons));

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
    private String genElsePreCondition (LinkedList<String> preCons) {

        StringBuilder sb = new StringBuilder();

        sb.append("requires ");
        sb.append(concatPreCons(preCons));
        sb.append("\n");

        return sb.toString();
    }

    private String concatPreCons (LinkedList<String> preCons){

        StringBuilder sb = new StringBuilder();

        for(String s : preCons){

            sb.append("!(" + s + ")");

            if(!preCons.getLast().equals(s)){
                sb.append(" && ");
            }
        }

        return sb.toString();

    }

    /* Will most likely return some post-condition */
    private String checkIfBody (Statement body, LinkedList<String> preCons){
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
                        + ((ReturnStmt) bodyStmts.get(0)).getExpr().get() + "\n");
            } else if (bodyStmts.get(0) instanceof ThrowStmt) {
                // Is the first statement a throw statement? Big dollahs.

                // Extract expression to be thrown
                Expression toThrow = ((ThrowStmt) bodyStmts.get(0)).getExpr();

                if(toThrow instanceof ObjectCreationExpr){
                    // A new expression is created and thrown on the spot

                    // Get type of thrown expression
                    String typeOfExpr = ((ObjectCreationExpr) toThrow).getType().toString();

                    // Create contract for what exception to be thrown
                    sb.append("signals_only " + typeOfExpr + "\n");

                    // Extract the condition that should hold once that contract is thrown
                    sb.append("signal " + typeOfExpr + " (" + concatPreCons(preCons) + ")\n");
                } else {
                    // Thrown expression is some already defined variable
                    // TODO : Find type of variable
                    System.out.println("NOT YET IMPLEMENTED\n");
                }



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
                    + ((ReturnStmt) body).getExpr().get() + ";\n");
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
    public boolean syntacticlyPure(MethodDeclaration md){
        ArrayList<SimpleName> params = new ArrayList<>();
        for(Parameter p : md.getParameters()){
            params.add(p.getName());
        }
        return syntacticlyPure(md.getBody().get().getStmts(), params);
    }
    public boolean syntacticlyPure(NodeList<Statement> stmtList, ArrayList<SimpleName> localVar){
        for(Statement s : stmtList){
            if(s instanceof ExpressionStmt) {
                if (!pureExpression(((ExpressionStmt) s).getExpression(), localVar)) {
                    return false;
                }
            } else if (s instanceof IfStmt){
                IfStmt sif = (IfStmt) s;
                if(!pureExpression(sif.getCondition(), localVar)){
                    return false;
                }
                if(sif.getThenStmt() instanceof BlockStmt){
                    if(!syntacticlyPure(((BlockStmt) sif.getThenStmt()).getStmts(), (ArrayList<SimpleName>) localVar.clone())){
                        return false;
                    }
                } else {
                    NodeList<Statement> nl = new NodeList<>();
                    nl.add(sif.getThenStmt());
                    if(!syntacticlyPure(nl, localVar)){
                        return false;
                    }
                }
                if(sif.getElseStmt().isPresent()){
                    Statement elseStmt = sif.getElseStmt().get();
                    if(elseStmt instanceof BlockStmt) {
                        if (!syntacticlyPure(((BlockStmt) elseStmt).getStmts(), (ArrayList<SimpleName>) localVar.clone())) {
                            return false;
                        }
                    } else {
                        NodeList<Statement> nl = new NodeList<>();
                        nl.add(sif.getThenStmt());
                        if(!syntacticlyPure(nl, localVar)){
                            return false;
                        }
                    }
                }


            }

                /*
                Commented out due to restructuring
                Expression exp =((ExpressionStmt) s).getExpression();
                if(exp instanceof MethodCallExpr){

                     // Until we get the SymbolSolver to work we cannot evaluate the purity of method calls
                     // so we treat all method calls as unpure

                    return false;
                } else if(exp instanceof VariableDeclarationExpr){
                    //might have to check if assigning the value of a method call
                    VariableDeclarationExpr vde = (VariableDeclarationExpr) exp;
                    for(VariableDeclarator vd : vde.getVariables()){
                       localVar.add(vd.getId().getName());
                       if(vd.getInit().isPresent()){
                           //TODO: 1. check if initializer contians method call
                           //TODO: 2. check if method call is pure
                       }
                    }
                } else if (exp instanceof AssignExpr){
                    //might have to check if assigning the value of a method call
                    AssignExpr ae = (AssignExpr) exp;
                    if(!localVar.contains(((NameExpr)ae.getTarget()).getName())){
                        return false;
                    }
                    //TODO: 1. check if ae.getValue() contians method call
                    //TODO: 2. check if method call is pure
                }
                */
        }
        return true;
    }
    private boolean pureExpression(Expression e, ArrayList<SimpleName> localVar){
        if(e instanceof MethodCallExpr){
            SymbolReference sr = JavaParserFacade.get(combinedTypeSolver).solve((MethodCallExpr) e);
            if(sr.getCorrespondingDeclaration() instanceof JavaParserMethodDeclaration){
                MethodDeclaration md = ((JavaParserMethodDeclaration) sr.getCorrespondingDeclaration()).getWrappedNode();
                return syntacticlyPure(md);
            } else {
                //TODO: Handle other method calls
                return false;
            }
        } else if (e instanceof VariableDeclarationExpr){
            //might have to check if assigning the value of a method call
            VariableDeclarationExpr vde = (VariableDeclarationExpr) e;
            boolean pure = true;
            for(VariableDeclarator vd : vde.getVariables()){
                localVar.add(vd.getId().getName());
                if(vd.getInit().isPresent()){
                    pure = pure && pureExpression(vd.getInit().get(), localVar);
                }
            }
            return pure;
        } else if (e instanceof AssignExpr){
            AssignExpr ae = (AssignExpr) e;
            if(!localVar.contains(((NameExpr)ae.getTarget()).getName())){
                return false;
            } else {
                return pureExpression(ae.getValue(), localVar);
            }
        } else if (e instanceof BinaryExpr){
            BinaryExpr be = (BinaryExpr) e;
            return pureExpression(be.getLeft(), localVar) && pureExpression(be.getRight(), localVar);

        } else if(e instanceof UnaryExpr){
            UnaryExpr ue = (UnaryExpr) e;
            if(ue.getOperator() == UnaryExpr.Operator.postDecrement
                    || ue.getOperator() == UnaryExpr.Operator.postIncrement
                    || ue.getOperator() == UnaryExpr.Operator.preDecrement
                    || ue.getOperator() == UnaryExpr.Operator.preIncrement){
                return (localVar.contains(((NameExpr)ue.getExpr()).getName()));

            }
            return pureExpression(ue.getExpr(), localVar);
        } else if (e instanceof NameExpr) {
            return true;

        } else if (e instanceof IntegerLiteralExpr){
            return true;
        } else if (e instanceof ObjectCreationExpr){
            //TODO: Check if constructor is pure? What will this actually do? Maybe always false?
            ObjectCreationExpr oce = (ObjectCreationExpr) e;
            boolean pure = true;
            for(Expression exp : oce.getArgs()){
                pure = pure && pureExpression(exp, localVar);
            }
            if(pure) {
                if (oce.getAnonymousClassBody().isPresent()) {
                    //TODO: Evaluate body
                    return false;
                }
            }
            return true;
        } else {
            System.out.println("Expression " + e + " of class " + e.getClass() + " is not covered");

            return false;
        }
        //TODO: collections map and for all child nodes

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
