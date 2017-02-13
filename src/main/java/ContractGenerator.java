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

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import Contract.*;

public class ContractGenerator {

    //Should be initialized with a class
    private ClassOrInterfaceDeclaration target;
    private ArrayList<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
    private HashMap<MethodDeclaration, Contract> contracts = new HashMap<>();
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
            } else {
                System.out.println("Bodydeclaration " + b + " of " + b.getClass() + " is not covered!");
            }
        }
        for(MethodDeclaration md : contracts.keySet()){
            System.out.println("-----------------");
            System.out.println();
            System.out.println(md.getName() + "\n" + contracts.get(md));
            System.out.println();
            //System.out.println("Purity status: " + syntacticlyPure(md));
            System.out.println();
            System.out.println("-----------------");
        }


    }
    public Contract createContract(MethodDeclaration md){
        //Get row for md
        //Get top-level assertions
        NodeList<Statement> stmtList =  md.getBody().get().getStmts();
        Contract c = new Contract();
        ArrayList<SimpleName> params = new ArrayList<>();
        for(Parameter p : md.getParameters()){
            params.add(p.getName());
        }
        return createContract(stmtList, params, new Contract());

    }
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
    public Contract createContract(NodeList<Statement> stmtList, ArrayList<SimpleName> localVar, Contract c){
        boolean pure = true;
        for(Statement s : stmtList){
           createContract(s, localVar, c);
        }
        return c;
    }
    public Contract createContract(Statement s, ArrayList<SimpleName> localVar, Contract c){
        if(s instanceof ExpressionStmt) {
           c.setPure(pureExpression(((ExpressionStmt) s).getExpression(), localVar));
           return c;
        } else if (s instanceof IfStmt){
            IfStmt sif = (IfStmt) s;
            boolean pure = true;
            c.setPure(pureExpression(sif.getCondition(), localVar));

            //TODO: Evaluate body of if
            return c;
        } else if (s instanceof ReturnStmt){
            ReturnStmt rs = (ReturnStmt) s;
            if(rs.getExpr().isPresent()){
                c.setPure(pureExpression(rs.getExpr().get(), localVar));
            }
            return c;
        } else if(s instanceof BlockStmt){
            BlockStmt bs = (BlockStmt) s;
            return createContract(((BlockStmt) s).getStmts(), (ArrayList<SimpleName>) localVar.clone(), c);
        } else if (s instanceof ThrowStmt){
            //TODO: Add throw behavior
            c.setPure(false);
            return c;
        } else if (s instanceof AssertStmt){
            AssertStmt as = (AssertStmt) s;
            //TODO: Assert
            c.setPure(pureExpression(as.getCheck(), localVar));
            return c;
        } else if (s instanceof BreakStmt){
            return c;
        } else if(s instanceof ContinueStmt){
            return c;
        } else if (s instanceof DoStmt){
            DoStmt ds = (DoStmt) s;
            c.setPure(pureExpression(ds.getCondition(), localVar));
            createContract(ds.getBody(), (ArrayList<SimpleName>) localVar.clone(), c);
            return c;
        } else if(s instanceof EmptyStmt){
            return c;
        } else {
            System.out.println("Statement " + s + " of class " + s.getClass() + " is not covered");
            return c;
        }
    }

    private boolean pureExpression(Expression e, ArrayList<SimpleName> localVar){
        if(e instanceof MethodCallExpr){
            SymbolReference sr = JavaParserFacade.get(combinedTypeSolver).solve((MethodCallExpr) e);
            if(sr.getCorrespondingDeclaration() instanceof JavaParserMethodDeclaration){
                MethodDeclaration md = ((JavaParserMethodDeclaration) sr.getCorrespondingDeclaration()).getWrappedNode();
                return createContract(md).isPure();
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
        } else if (e instanceof DoubleLiteralExpr){
            return true;
        } else if (e instanceof LongLiteralExpr){
            return true;
        } else if (e instanceof StringLiteralExpr){
            return true;
        } else if (e instanceof BooleanLiteralExpr) {
            return true;
        } else if (e instanceof CharLiteralExpr){
            return true;
        } else if (e instanceof NullLiteralExpr){
            return true;
        } else if (e instanceof ClassExpr){
            return true;
        } else if (e instanceof AnnotationExpr){
            return true;
        } else if (e instanceof TypeExpr){
            return true;
        } else if (e instanceof EnclosedExpr){
            if(((EnclosedExpr) e).getInner().isPresent()) {
                return pureExpression(((EnclosedExpr) e).getInner().get(), localVar);
            } else {
                //This should really not happen...
                return true;
            }
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
        } else if (e instanceof ConditionalExpr){
            ConditionalExpr ce = (ConditionalExpr) e;
            return pureExpression(ce.getCondition(),localVar)
                    && pureExpression(ce.getThenExpr(),localVar)
                    && pureExpression(ce.getElseExpr(),localVar);
        } else if(e instanceof InstanceOfExpr){
            return pureExpression(((InstanceOfExpr) e).getExpr(), localVar);
        } else if (e instanceof LambdaExpr){
            LambdaExpr le = (LambdaExpr) e;
            return createContract(le.getBody(), localVar, new Contract()).isPure();
        } else if (e instanceof SuperExpr){
            //TODO: Need to have entire package in scope
            //TODO: Also implement this
            System.out.println("SUPER EXPRESSION");
            return false;
        } else {
            System.out.println("Expression " + e + " of " + e.getClass() + " is not covered");

            return false;
        }
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
                //System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }
}
