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
    public Contract createContract(Statement s, ArrayList<SimpleName> localVar, Contract c){
        if(s instanceof ExpressionStmt) {
           c.setPure(createContract(((ExpressionStmt) s).getExpression(), localVar, c).isPure());
           return c;
        } else if (s instanceof IfStmt){
            IfStmt sif = (IfStmt) s;

            //Create new behavior for if-statement
            c.addBehavior();

            // Set purity
            c.setPure(createContract(sif.getCondition(), localVar, c).isPure());

            c.addPreCon(sif.getCondition());
            // Create contract from if-body
            createContract(sif.getThenStmt(), localVar, c);

            //TODO: Evaluate body of if
            return c;
        } else if (s instanceof ReturnStmt){
            ReturnStmt rs = (ReturnStmt) s;
            if(rs.getExpr().isPresent()){
                c.addPostCon(rs.getExpr().get(), true);
            }
            return c;
        } else if(s instanceof BlockStmt){
            BlockStmt bs = (BlockStmt) s;
            return createContract(((BlockStmt) s).getStmts(), (ArrayList<SimpleName>) localVar.clone(), c);
        } else if (s instanceof ThrowStmt){
            //TODO: Add throw behavior
            return c;
        } else if (s instanceof AssertStmt){
            AssertStmt as = (AssertStmt) s;
            //TODO: Should not have to add a new behavior, should only change current one
            if(startAssert(c.getMethodDeclaration(), as)){
                c.addPreCon(as.getCheck());
            }
            if(endAssrt(c.getMethodDeclaration(), as)){
                c.addPostCon(as.getCheck(), false);
            }
            c.setPure(createContract(as.getCheck(), localVar, c).isPure());
            return c;
        } else if (s instanceof BreakStmt){
            return c;
        } else if(s instanceof ContinueStmt){
            return c;
        } else if (s instanceof DoStmt){
            DoStmt ds = (DoStmt) s;
            c.setPure(createContract(ds.getCondition(), localVar, c).isPure());
            createContract(ds.getBody(), (ArrayList<SimpleName>) localVar.clone(), c);
            return c;
        } else if(s instanceof EmptyStmt){
            return c;
        } else {
            System.out.println("Statement " + s + " of class " + s.getClass() + " is not covered");
            return c;
        }
    }

    private Contract createContract(Expression e, ArrayList<SimpleName> localVar, Contract c){
        if(e instanceof MethodCallExpr){
            SymbolReference sr = JavaParserFacade.get(combinedTypeSolver).solve((MethodCallExpr) e);
            if(sr.getCorrespondingDeclaration() instanceof JavaParserMethodDeclaration){
                MethodDeclaration md = ((JavaParserMethodDeclaration) sr.getCorrespondingDeclaration()).getWrappedNode();
                return createContract(md);
            } else {
                //TODO: Handle other method calls
                System.out.println("Method call expression with declaration: " + sr.getCorrespondingDeclaration() + " is not covered!");
                return null;
            }
        } else if (e instanceof VariableDeclarationExpr){
            //might have to check if assigning the value of a method call
            VariableDeclarationExpr vde = (VariableDeclarationExpr) e;
            boolean pure = true;
            for(VariableDeclarator vd : vde.getVariables()){
                localVar.add(vd.getId().getName());
                if(vd.getInit().isPresent()){
                    pure = pure && createContract(vd.getInit().get(), localVar, c).isPure();
                }
            }
            c.setPure(pure);
            return c;
        } else if (e instanceof AssignExpr){
            AssignExpr ae = (AssignExpr) e;
            if(ae.getTarget() instanceof FieldAccessExpr){
                c.setPure(false);
                //TODO: Add contract for assignment
                return c;
            } else if (ae.getTarget() instanceof NameExpr){
                c.setPure(localVar.contains(((NameExpr) ae.getTarget()).getName()));
            } else if(ae.getTarget() instanceof ArrayAccessExpr){
                ArrayAccessExpr aae = (ArrayAccessExpr) ae.getTarget();
                c.setPure(localVar.contains(aae.getName()) && createContract(aae.getIndex(),localVar, c).isPure());
            } else {
                System.out.println("Assignment target " +  ae.getTarget() + " of " + ae.getTarget().getClass() + " not covered!");
                c.setPure(false);
                return c;
            }
            return createContract(ae.getValue(), localVar, c);
        } else if (e instanceof BinaryExpr){
            BinaryExpr be = (BinaryExpr) e;
            createContract(be.getLeft(), localVar, c);
            createContract(be.getRight(), localVar, c);
            return c;

        } else if(e instanceof UnaryExpr){
            UnaryExpr ue = (UnaryExpr) e;
            if(ue.getOperator() == UnaryExpr.Operator.postDecrement
                    || ue.getOperator() == UnaryExpr.Operator.postIncrement
                    || ue.getOperator() == UnaryExpr.Operator.preDecrement
                    || ue.getOperator() == UnaryExpr.Operator.preIncrement){
                c.setPure(localVar.contains(((NameExpr)ue.getExpr()).getName()));
                //TODO: add to contract
                return c;

            }
            return createContract(ue.getExpr(), localVar,c );
        } else if (e instanceof FieldAccessExpr){
            System.out.println("FAE");
            return c;
        } else if (e instanceof NameExpr) {
            return c;
        } else if (e instanceof IntegerLiteralExpr){
            return c;
        } else if (e instanceof DoubleLiteralExpr){
            return c;
        } else if (e instanceof LongLiteralExpr){
            return c;
        } else if (e instanceof StringLiteralExpr){
            return c;
        } else if (e instanceof BooleanLiteralExpr) {
            return c;
        } else if (e instanceof CharLiteralExpr){
            return c;
        } else if (e instanceof NullLiteralExpr){
            return c;
        } else if (e instanceof ClassExpr){
            return c;
        } else if (e instanceof AnnotationExpr){
            return c;
        } else if (e instanceof TypeExpr){
            return c;
        } else if (e instanceof EnclosedExpr){
            if(((EnclosedExpr) e).getInner().isPresent()) {
                return createContract(((EnclosedExpr) e).getInner().get(), localVar, c);
            } else {
                //This should really not happen...
                System.out.println("This is not covered! Enclosed expression is non-existent!");
                return c;
            }
        } else if (e instanceof ObjectCreationExpr){
            //TODO: Check if constructor is pure? What will this actually do? Maybe always false?
            ObjectCreationExpr oce = (ObjectCreationExpr) e;
            boolean pure = true;
            for(Expression exp : oce.getArgs()){
                pure = pure && createContract(exp, localVar, c).isPure();
            }
            if(pure) {
                if (oce.getAnonymousClassBody().isPresent()) {
                    //TODO: Evaluate body
                    c.setPure(false);
                    return c;
                }
            }
            c.setPure(false);
            return c;
        } else if(e instanceof ArrayCreationExpr){
            ArrayCreationExpr ace = (ArrayCreationExpr) e;
            if(ace.getInitializer().isPresent()){
                return createContract(ace.getInitializer().get(), localVar, c);
            } else {
                c.setPure(true);
                return c;
            }
        } else if(e instanceof  ArrayInitializerExpr){
            ArrayInitializerExpr aie = (ArrayInitializerExpr) e;
            boolean pure = true;
            for(Expression exp : aie.getValues()){
                pure = pure && createContract(exp,localVar,c).isPure();
            }
            c.setPure(pure);
            return c;
        } else if(e instanceof ArrayAccessExpr){
            ArrayAccessExpr aae = (ArrayAccessExpr) e;
            c.setPure(createContract(aae.getIndex(), localVar, c).isPure());
            return c;
        } else if(e instanceof CastExpr){
            CastExpr ce = (CastExpr) e;
            c.setPure(createContract(ce.getExpr(), localVar, c).isPure());
            return c;
        } else if (e instanceof ConditionalExpr){
            ConditionalExpr ce = (ConditionalExpr) e;
            c.setPure( createContract(ce.getCondition(),localVar, c).isPure()
                    && createContract(ce.getThenExpr(),localVar, c).isPure()
                    && createContract(ce.getElseExpr(),localVar, c).isPure());
            return c;
        } else if(e instanceof InstanceOfExpr){
            c.setPure(createContract(((InstanceOfExpr) e).getExpr(), localVar, c).isPure());
            return c;
        } else if (e instanceof LambdaExpr){
            System.out.println("LAMDA EXPRESSIONS ARE NOT SUPPORTED");
            c.setPure(false);
            return c;
        } else if (e instanceof SuperExpr){
            //TODO: Need to have entire package in scope
            //TODO: Also implement this
            System.out.println("SUPER EXPRESSION");
            c.setPure(false);
            return c;
        } else {
            System.out.println("Expression " + e + " of " + e.getClass() + " is not covered");
            c.setPure(false);
            return c;
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
