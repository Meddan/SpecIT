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
            System.out.println("Purity status: " + contracts.get(md).isPure());
            System.out.println();
            System.out.println("-----------------");
        }


    }
    public Contract createContract(MethodDeclaration md){
        //Get row for md
        //Get top-level assertions
        NodeList<Statement> stmtList =  md.getBody().get().getStmts();
        ArrayList<SimpleName> params = new ArrayList<>();
        for(Parameter p : md.getParameters()){
            params.add(p.getName());
        }
        Contract c = new Contract(md);
        c.addBehavior();
        createContract(stmtList, params, c);
        return c;

    }

    private boolean endAssrt(MethodDeclaration md, Statement s) {
        NodeList<Statement> stmtList = md.getBody().get().getStmts();
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

    private boolean startAssert(MethodDeclaration md, Statement s){
        NodeList<Statement> stmtList = md.getBody().get().getStmts();
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
    public void createContract(Statement s, ArrayList<SimpleName> localVar, Contract c){
        if(s instanceof ExpressionStmt) {
           createContract(((ExpressionStmt) s).getExpression(), localVar, c);
        } else if (s instanceof IfStmt){
            IfStmt sif = (IfStmt) s;

            //Create new behavior for if-statement
            c.addBehavior();

            // Set purity
            createContract(sif.getCondition(), localVar, c);

            c.addPreCon(sif.getCondition());
            // Create contract from if-body
            createContract(sif.getThenStmt(), localVar, c);

            //TODO: Evaluate body of if
        } else if (s instanceof ReturnStmt){
            ReturnStmt rs = (ReturnStmt) s;
            if(rs.getExpr().isPresent()){
                c.addPostCon(rs.getExpr().get(), true);
                createContract(rs.getExpr().get(), localVar, c);
            }
        } else if(s instanceof BlockStmt){
            BlockStmt bs = (BlockStmt) s;
            createContract(((BlockStmt) s).getStmts(), (ArrayList<SimpleName>) localVar.clone(), c);
        } else if (s instanceof ThrowStmt){
            //TODO: Add throw behavior
        } else if (s instanceof AssertStmt){
            AssertStmt as = (AssertStmt) s;
            //TODO: Should not have to add a new behavior, should only change current one
            if(startAssert(c.getMethodDeclaration(), as)){
                c.addPreCon(as.getCheck());
            }
            if(endAssrt(c.getMethodDeclaration(), as)){
                c.addPostCon(as.getCheck(), false);
            }
            createContract(as.getCheck(), localVar, c);
        } else if (s instanceof BreakStmt){
            return;
        } else if(s instanceof ContinueStmt){
            return;
        } else if (s instanceof DoStmt){
            DoStmt ds = (DoStmt) s;
            createContract(ds.getCondition(), localVar, c);
            createContract(ds.getBody(), (ArrayList<SimpleName>) localVar.clone(), c);
        } else if(s instanceof EmptyStmt){
            return;
        } else {
            System.out.println("Statement " + s + " of class " + s.getClass() + " is not covered");
            return;
        }
    }

    private void createContract(Expression e, ArrayList<SimpleName> localVar, Contract c){
        if(e instanceof MethodCallExpr){
            SymbolReference sr = JavaParserFacade.get(combinedTypeSolver).solve((MethodCallExpr) e);
            if(sr.getCorrespondingDeclaration() instanceof JavaParserMethodDeclaration){
                MethodDeclaration md = ((JavaParserMethodDeclaration) sr.getCorrespondingDeclaration()).getWrappedNode();
                c.setPure(createContract(md).isPure());
            } else {
                //TODO: Handle other method calls
                System.out.println("Method call expression with declaration: " + sr.getCorrespondingDeclaration() + " is not covered!");
            }
        } else if (e instanceof VariableDeclarationExpr){
            //might have to check if assigning the value of a method call
            VariableDeclarationExpr vde = (VariableDeclarationExpr) e;
            for(VariableDeclarator vd : vde.getVariables()){
                localVar.add(vd.getId().getName());
                if(vd.getInit().isPresent()){
                    createContract(vd.getInit().get(), localVar, c);
                }
            }
        } else if (e instanceof AssignExpr){
            AssignExpr ae = (AssignExpr) e;
            if(ae.getTarget() instanceof FieldAccessExpr){
                c.setPure(false);
                //TODO: Add contract for assignment
            } else if (ae.getTarget() instanceof NameExpr){
                c.setPure(localVar.contains(((NameExpr) ae.getTarget()).getName()));
            } else if(ae.getTarget() instanceof ArrayAccessExpr){
                ArrayAccessExpr aae = (ArrayAccessExpr) ae.getTarget();
                createContract(aae.getIndex(), localVar, c);
                c.setPure(localVar.contains(aae.getName()));
            } else {
                System.out.println("Assignment target " +  ae.getTarget() + " of " + ae.getTarget().getClass() + " not covered!");
                c.setPure(false);
            }
            createContract(ae.getValue(), localVar, c);
        } else if (e instanceof BinaryExpr){
            BinaryExpr be = (BinaryExpr) e;
            createContract(be.getLeft(), localVar, c);
            createContract(be.getRight(), localVar, c);

        } else if(e instanceof UnaryExpr){
            UnaryExpr ue = (UnaryExpr) e;
            if(ue.getOperator() == UnaryExpr.Operator.postDecrement
                    || ue.getOperator() == UnaryExpr.Operator.postIncrement
                    || ue.getOperator() == UnaryExpr.Operator.preDecrement
                    || ue.getOperator() == UnaryExpr.Operator.preIncrement){
                c.setPure(localVar.contains(((NameExpr)ue.getExpr()).getName()));
                //TODO: add to contract
            }
            createContract(ue.getExpr(), localVar, c);
        } else if (e instanceof FieldAccessExpr){
            return;
        } else if (e instanceof NameExpr) {
            return;
        } else if (e instanceof IntegerLiteralExpr){
            return;
        } else if (e instanceof DoubleLiteralExpr){
            return;
        } else if (e instanceof LongLiteralExpr){
            return;
        } else if (e instanceof StringLiteralExpr){
            return;
        } else if (e instanceof BooleanLiteralExpr) {
            return;
        } else if (e instanceof CharLiteralExpr){
            return;
        } else if (e instanceof NullLiteralExpr){
            return;
        } else if (e instanceof ClassExpr){
            return;
        } else if (e instanceof AnnotationExpr){
            return;
        } else if (e instanceof TypeExpr){
            return;
        } else if (e instanceof EnclosedExpr){
            if(((EnclosedExpr) e).getInner().isPresent()) {
                createContract(((EnclosedExpr) e).getInner().get(), localVar, c);
            } else {
                //This should really not happen...
                System.out.println("This is not covered! Enclosed expression is non-existent!");
            }
        } else if (e instanceof ObjectCreationExpr){
            //TODO: Check if constructor is pure? What will this actually do? Maybe always false?
            ObjectCreationExpr oce = (ObjectCreationExpr) e;
            boolean pure = true;
            for(Expression exp : oce.getArgs()){
                createContract(exp, localVar, c);
            }
            if (oce.getAnonymousClassBody().isPresent()) {
                //TODO: Evaluate body
                c.setPure(false);

            }
            c.setPure(false);
        } else if(e instanceof ArrayCreationExpr){
            ArrayCreationExpr ace = (ArrayCreationExpr) e;
            if(ace.getInitializer().isPresent()){
                createContract(ace.getInitializer().get(), localVar, c);
            }
        } else if(e instanceof  ArrayInitializerExpr){
            ArrayInitializerExpr aie = (ArrayInitializerExpr) e;
            for(Expression exp : aie.getValues()){
                createContract(exp,localVar,c);
            }
        } else if(e instanceof ArrayAccessExpr){
            ArrayAccessExpr aae = (ArrayAccessExpr) e;
            createContract(aae.getIndex(), localVar, c);
        } else if(e instanceof CastExpr){
            CastExpr ce = (CastExpr) e;
            createContract(ce.getExpr(), localVar, c);
        } else if (e instanceof ConditionalExpr){
            ConditionalExpr ce = (ConditionalExpr) e;
            createContract(ce.getCondition(),localVar, c);
            createContract(ce.getThenExpr(),localVar, c);
            createContract(ce.getElseExpr(),localVar, c);
        } else if(e instanceof InstanceOfExpr){
           createContract(((InstanceOfExpr) e).getExpr(), localVar, c);
        } else if (e instanceof LambdaExpr){
            System.out.println("LAMDA EXPRESSIONS ARE NOT SUPPORTED");
            c.setPure(false);
        } else if (e instanceof SuperExpr){
            //TODO: Need to have entire package in scope
            //TODO: Also implement this
            System.out.println("SUPER EXPRESSION");
            c.setPure(false);
        } else {
            System.out.println("Expression " + e + " of " + e.getClass() + " is not covered");
            c.setPure(false);
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
