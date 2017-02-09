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

import Contract.*;

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
        Contract c = new Contract();
        for(Statement s : stmtList){
            if(s instanceof AssertStmt){

                c.addBehavior();
                //Checks if assertions are at the start of function and can be added as preconditions
                if(startAssert(stmtList, s)){
                    //sb.append( "requires " + ((AssertStmt) s).getCheck().toString() + "\n");
                    c.addPreCon(((AssertStmt) s).getCheck().toString());
                }
                //Checks if assertions are at the end of function and can be added as postconditions
                if(endAssrt(stmtList, s)){
                    //sb.append( "ensures " + ((AssertStmt) s).getCheck().toString() + "\n");
                    c.addPostCon(((AssertStmt) s).getCheck().toString());
                }
                //Checks if we can ignore/integrate statements before assertions.


            }

            Statement temp = s;
            LinkedList preCons = new LinkedList<String>();
            while(temp instanceof IfStmt){
                c.addBehavior();

                // Begin building behavior for first case
                c.addPreCon(((IfStmt) temp).getCondition().toString());

                // Add expressions of pre-conditions for safekeeping
                preCons.add(((IfStmt) temp).getCondition().toString());

                // Check if-body for contract info
                checkIfBody(((IfStmt) temp).getThenStmt(), c);

                // If the next statement is also an if-statement, redo the loop
                if(((IfStmt) temp).getElseStmt().isPresent()){ // There is some statement

                    if(((IfStmt) temp).getElseStmt().get() instanceof IfStmt){
                        // That statement is an if-statement

                        // Set temp to new if-statement, so next iteration of
                        // loop will handle contract generation
                        temp = ((IfStmt) temp).getElseStmt().get();

                    } else {
                        // This is the else-statement. It should be written as a new behavior
                        // with a pre-condition that is the negation of all previous requirements.

                        // Add new behavior
                        c.addBehavior();

                        // Add preconditions for new behavior
                        c.addPreCon(genElsePreCondition(preCons));

                        // It is not an if-statement (but it is the body of an else)
                        // Therefore, it is a block-statement or some single-line statement
                        // Check body to extract contract (post-condition)
                        checkIfBody(((IfStmt) temp).getElseStmt().get(), c);

                        break; // Break out of while-loop as there are no more if-cases
                    }

                } else { // There is no else-statement
                    // What do we do?! Panic.
                    break; // No more if-cases and no else-statement, break out of loop
                }

            }
        }

        //System.out.println(c.toString());
        return c.toString();
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

        sb.append(concatPreCons(preCons));

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

    private void createReturntStmtPostCon(ReturnStmt rs, Contract c){
        if(rs.getExpr().isPresent()){
            c.addPostCon("\\result == " + rs.getExpr().get());
        } else {
            // TODO : Is this correct?
            c.addPostCon(c.getCurrentBehavior().getPreCons());
        }
    }

    /* Will most likely return some post-condition */
    private void checkIfBody (Statement body, Contract c){

        if(body instanceof BlockStmt){
            // It's a block statement, likely to be many statements
            // But could still be only one

            // Get all highest level statements in block
            List<Node> bodyStmts = body.getChildNodes();

            if(bodyStmts.get(0) instanceof ReturnStmt){
                // Is the first statement a return statement? Easy money.
                createReturntStmtPostCon((ReturnStmt) bodyStmts.get(0), c);
            } else if (bodyStmts.get(0) instanceof ThrowStmt) {
                // Is the first statement a throw statement? Big dollahs.

                c.setExceptional(true);

                // Extract expression to be thrown
                Expression toThrow = ((ThrowStmt) bodyStmts.get(0)).getExpr();

                if(toThrow instanceof ObjectCreationExpr){
                    // A new expression is created and thrown on the spot

                    // Get type of thrown expression
                    String typeOfExpr = ((ObjectCreationExpr) toThrow).getType().toString();

                    c.addException(typeOfExpr);
                } else {
                    // Thrown expression is some already defined variable
                    // TODO : Find type of variable
                    c.addPostCon("NOT YET IMPLEMENTED");
                }

            } else {
                // The first statement was not a return statement. Time to think.
                // TODO : Implement logic
                c.addPostCon("NOT YET IMPLEMENTED");
            }


            // Check body for return

            // TODO : Fill with logic
        } else if (body instanceof ReturnStmt){
            // Body is only a return, not enclosed by { }
            createReturntStmtPostCon((ReturnStmt) body, c);
        } else {
            c.addPostCon("NOT YET IMPLEMENTED");
            // It's not a return but some other single line expression/statement
            // TODO : Fill with logic
        }
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
    public boolean syntacticlyPure(Statement s, ArrayList<SimpleName> localVar){
        if(s instanceof ExpressionStmt) {
           return pureExpression(((ExpressionStmt) s).getExpression(), localVar);
        } else if (s instanceof IfStmt){
            IfStmt sif = (IfStmt) s;
            boolean pure = true;
            if(!pureExpression(sif.getCondition(), localVar)){
                return false;
            }
            if(!syntacticlyPure(sif.getThenStmt(), localVar)){
                return false;
            }
            if(sif.getElseStmt().isPresent()){
                if (!syntacticlyPure(sif.getElseStmt().get(), (ArrayList<SimpleName>) localVar.clone())) {
                    return false;
                }
            }
            return true;
        } else if (s instanceof ReturnStmt){
            ReturnStmt rs = (ReturnStmt) s;
            if(rs.getExpr().isPresent()){
                return pureExpression(rs.getExpr().get(), localVar);
            }
            return true;
        } else if(s instanceof BlockStmt){
            BlockStmt bs = (BlockStmt) s;
            return syntacticlyPure(((BlockStmt) s).getStmts(), (ArrayList<SimpleName>) localVar.clone());
        } else if (s instanceof ThrowStmt){
            return false;
        } else if (s instanceof AssertStmt){
            AssertStmt as = (AssertStmt) s;
            return pureExpression(as.getCheck(), localVar);
        } else {
            System.out.println("Statement " + s + " of class " + s.getClass() + " is not covered");
            return false;
        }
    }
    public boolean syntacticlyPure(NodeList<Statement> stmtList, ArrayList<SimpleName> localVar){
        boolean pure = true;
        for(Statement s : stmtList){
            pure = pure && syntacticlyPure(s, localVar);
        }
        return pure;
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
            if(ae.getTarget() instanceof FieldAccessExpr){
                return false;
            } else if (ae.getTarget() instanceof NameExpr){
                if(!localVar.contains(((NameExpr) ae.getTarget()).getName())){
                    return false;
                }
            } else if(ae.getTarget() instanceof ArrayAccessExpr){
                ArrayAccessExpr aae = (ArrayAccessExpr) ae.getTarget();
                if(!(localVar.contains(aae.getName()) && pureExpression(aae.getIndex(),localVar))){
                    return false;
                }
            } else {
                System.out.println("Assignment target " +  ae.getTarget() + " of " + ae.getTarget().getClass() + " not covered!");
                return false;
            }
            return pureExpression(ae.getValue(), localVar);
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
        } else if (e instanceof FieldAccessExpr){
            System.out.println("FAE");
            return true;
        } else if (e instanceof NameExpr) {
            return true;
        } else if (e instanceof IntegerLiteralExpr){
            return true;
        } else if (e instanceof StringLiteralExpr){
            return true;
        } else if (e instanceof BooleanLiteralExpr) {
            return true;
        } else if (e instanceof CharLiteralExpr){
            return true;
        } else if (e instanceof ClassExpr){
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
        } else if(e instanceof ArrayCreationExpr){
            ArrayCreationExpr ace = (ArrayCreationExpr) e;
            if(ace.getInitializer().isPresent()){
                return pureExpression(ace.getInitializer().get(), localVar);
            } else {
                return true;
            }
        } else if(e instanceof  ArrayInitializerExpr){
            ArrayInitializerExpr aie = (ArrayInitializerExpr) e;
            boolean pure = true;
            for(Expression exp : aie.getValues()){
                pure = pure && pureExpression(exp,localVar);
            }
            return pure;
        } else if(e instanceof ArrayAccessExpr){
            ArrayAccessExpr aae = (ArrayAccessExpr) e;
            return pureExpression(aae.getIndex(), localVar);
        } else if(e instanceof CastExpr){
            CastExpr ce = (CastExpr) e;
            return pureExpression(ce.getExpr(), localVar);
        } else {
            System.out.println("Expression " + e + " of " + e.getClass() + " is not covered");

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
