package ContractGeneration;

import ContractGeneration.Resources;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.imports.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import Contract.*;

public class ContractGenerator {

    //Should be initialized with a class
    private ClassOrInterfaceDeclaration target;
    private ArrayList<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
    private HashMap<MethodDeclaration, Contract> contracts = new HashMap<>();
    private CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

    public ContractGenerator(ClassOrInterfaceDeclaration coid, String path, File projectDir){
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
            System.out.println("Purity status: " + contracts.get(md).isPure());
            System.out.println();
            System.out.println("-----------------");
            md.setComment(new BlockComment(contracts.get(md).toString()));
        }

        writeToFile(path, projectDir, coid.toString());


    }

    private boolean endAssrt(MethodDeclaration md, Statement s) {
        NodeList<Statement> stmtList = md.getBody().get().getStmts();
        int index = stmtList.indexOf(s);
        if(index < 0){
            System.out.println("Assert statement not in method declaration block");
            return false;
        }
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

    public Contract createContract(MethodDeclaration md){
        //Get row for md
        //Get top-level assertions
        NodeList<Statement> stmtList =  md.getBody().get().getStmts();
        //TODO:
        ArrayList<SimpleName> params = new ArrayList<>();
        Contract c = new Contract(md);
        Behavior b = c.getCurrentBehavior();
        b.setMethodDeclaration(md);
        for(Parameter p : md.getParameters()){
            params.add(p.getName());
            b.putAssignedValue(p.getName(), new NameExpr(new SimpleName("\\old(" + p.getName() + ")")));
        }
        for(FieldDeclaration fd : fields){
            for(VariableDeclarator vd : fd.getVariables()){
                params.add(vd.getId().getName());
                if(vd.getInit().isPresent()){
                    //If we initialize a variable we save the name in the behaviors assigned values with the
                    // value of the expression that we get from evaluating the initializer
                    if(!(vd.getInit().get() instanceof ArrayCreationExpr)){
                        //b.putAssignedValue(vd.getId().getName(), new NameExpr(vd.getId().getName()));
                        b.putAssignedValue(vd.getId().getName(), createContract(vd.getInit().get(), params, b));
                    }
                } else {
                    b.putAssignedValue(vd.getId().getName(), new NameExpr(new SimpleName("\\old(" + vd.getId().getName() + ")")));
                }
            }
        }

        createContract(stmtList, params, c.getCurrentBehavior());
        return c;

    }

    public void createContract(NodeList<Statement> stmtList, ArrayList<SimpleName> localVar, Behavior b){
        boolean pure = true;
        for(Statement s : stmtList){
            createContract(s, localVar, b);
        }
    }
    public void createContract(Statement s, ArrayList<SimpleName> localVar, Behavior b){
        /* We first identify if the current statement is assert or return in which case we want to make sure our
        assertions (ensures) are handled correctly.
        */
        if (s instanceof AssertStmt){
            AssertStmt as = (AssertStmt) s;
            //An assert-statement can be seen as a precondition if it appears at the start of a function
            //We
            Expression e = createContract(as.getCheck(), localVar, b);
            if(startAssert(b.getMethodDeclaration(), as)){
                b.addPreCon(e);
            }
            //Add the assertion as a potential postcondition
            b.addPostCon(e, false);
            //Create contract for the expression in the assertion

        } else if (s instanceof ReturnStmt){
            ReturnStmt rs = (ReturnStmt) s;
            if(rs.getExpr().isPresent()){
                //Create contracts for the return expression and add the return statement to the contract
                b.addPostCon(createContract(rs.getExpr().get(), localVar, b), true);
            }
            //Path of execution ends and we close all behaviors on this path
            b.setClosed(true);
        } else if (s instanceof ThrowStmt) {
            ThrowStmt ts = (ThrowStmt) s;
            b.setExceptional(true);
            if(ts.getExpr() instanceof ObjectCreationExpr){
                // These conditions is what will hold after we throw an exception
                LinkedList<PostCondition> listOfPostCons = b.getPostCons();
                LinkedList<Expression> listOfExprs = new LinkedList<>();

                for(PostCondition pc : listOfPostCons){
                    listOfExprs.add(pc.getExpression());
                }

                b.addException(((ObjectCreationExpr) ts.getExpr()).getType(), listOfExprs);
                // We create a new object when throwing
                // TODO : Make more intelligent
            } else {
                // We're throwing some already created exception
                // TODO : Get type and add to contract
            }
            b.setClosed(true);
        } else {
            //We are doing further modifications to our code thus we cannot guarantee our postconditions will hold.
            b.clearPostAssert();
            if (s instanceof ExpressionStmt) {
                createContract(((ExpressionStmt) s).getExpression(), localVar, b);
            } else if (s instanceof IfStmt) {
                /*
                 * When evaluating a if-statement we split the current behavior in 2, one that enters the then-block and
                 * one that enters the else-block (if present). When evaluating the blocks the new behaviors created are
                 * set as the current behavior for their respective block. Once the if-statement is done the current
                 * behavior is set to the initial one.
                 */
                IfStmt sif = (IfStmt) s;
                Expression ifCond = createContract(sif.getCondition(),localVar, b);
                Behavior a = new Behavior(b);
                a.addPreCon(ifCond);
                b.setClosed(true);
                b.addChild(a);
                createContract(sif.getThenStmt(), (ArrayList<SimpleName>) localVar.clone(), a);

                if (sif.getElseStmt().isPresent()) {
                    Behavior d = new Behavior(b);
                    b.addChild(d);
                    //TODO: d.addPreCon(); need to fix double negation
                    d.addPreCon(new UnaryExpr(ifCond, UnaryExpr.Operator.not));
                    createContract(sif.getElseStmt().get(), (ArrayList<SimpleName>) localVar.clone(), d);
                } else {
                    Behavior e = new Behavior(b);
                    b.addChild(e);
                    e.addPreCon(new UnaryExpr(ifCond, UnaryExpr.Operator.not));
                    //TODO: e.addPreCon(); need to fix double negation
                }

            } else if (s instanceof BlockStmt) {
                BlockStmt bs = (BlockStmt) s;
                createContract(((BlockStmt) s).getStmts(), (ArrayList<SimpleName>) localVar.clone(), b);
            } else if (s instanceof BreakStmt) {
                return;
            } else if (s instanceof ContinueStmt) {
                return;
            } else if (s instanceof EmptyStmt) {
                return;
            } else if (s instanceof WhileStmt){
                System.out.println("WhileStmt");
                WhileStmt ws = (WhileStmt) s;
                Statement body = ws.getBody();
                Behavior temporary = new Behavior(null);
                createContract(ws.getCondition(), localVar, temporary);
                createContract(body, localVar, temporary);
                for(Behavior leaf : temporary.getLeafs()){
                    for(SimpleName sn : leaf.getAssignables()){
                        b.putAssignedValue(sn, null);
                        System.out.println("Removing: " + sn);
                    }
                }


                System.out.println("WHILE DONE");
            } else if (s instanceof ForStmt || s instanceof ForeachStmt || s instanceof DoStmt){
                //TODO: implement
            } else {
                System.out.println("Statement " + s + " of class " + s.getClass() + " is not covered");
            }
        }
    }

    private Expression createContract(Expression e, ArrayList<SimpleName> localVar, Behavior b){
        if(Resources.ignorableExpression(e)){
            return e;
        } else if (e instanceof NameExpr){
            NameExpr ne = (NameExpr) e;
            if(b.getAssignedValues().keySet().contains((ne.getName()))){
                return b.getAssignedValues().get((ne.getName()));
            } else {
                return e;
            }
        } else if(e instanceof MethodCallExpr){
            SymbolReference sr = JavaParserFacade.get(combinedTypeSolver).solve((MethodCallExpr) e);
            if(sr.getCorrespondingDeclaration() instanceof JavaParserMethodDeclaration){
                MethodDeclaration md = ((JavaParserMethodDeclaration) sr.getCorrespondingDeclaration()).getWrappedNode();
                b.setPure(createContract(md).isPure());
            } else {
                //TODO: Handle other method calls
                System.out.println("Method call expression with declaration: " + sr.getCorrespondingDeclaration() + " is not covered!");
            }
            return e;
        } else if (e instanceof VariableDeclarationExpr){
            //might have to check if assigning the value of a method call
            VariableDeclarationExpr vde = (VariableDeclarationExpr) e;
            for(VariableDeclarator vd : vde.getVariables()){
                localVar.add(vd.getId().getName());
                if(vd.getInit().isPresent()){
                    //If we initialize a variable we save the name in the behaviors assigned values with the
                    // value of the expression that we get from evaluating the initializer
                    if(!(vd.getInit().get() instanceof ArrayCreationExpr)){
                        //b.putAssignedValue(vd.getId().getName(), new NameExpr(vd.getId().getName()));
                        b.putAssignedValue(vd.getId().getName(), createContract(vd.getInit().get(), localVar, b));
                    }
                }
            }
            return null;
        } else if (e instanceof AssignExpr){
            AssignExpr ae = (AssignExpr) e;

            if(ae.getTarget() instanceof FieldAccessExpr){
                b.putAssignedValue(((FieldAccessExpr) ae.getTarget()).getField(), createContract(ae.getValue(), localVar, b));
                b.setPure(false);
            } else if (ae.getTarget() instanceof NameExpr){
                NameExpr ne = (NameExpr) ae.getTarget();
                b.putAssignedValue(((NameExpr) ae.getTarget()).getName(), createContract(ae.getValue(), localVar, b));
                b.addPostCon(ae, false);
                b.setPure(localVar.contains(((NameExpr) ae.getTarget()).getName()));
            } else if(ae.getTarget() instanceof ArrayAccessExpr){
                ArrayAccessExpr aae = (ArrayAccessExpr) ae.getTarget();
                String arrayName = createContract(aae.getName(), localVar, b).toString();
                String index = createContract(aae.getIndex(), localVar, b).toString();
                SimpleName name = new SimpleName( arrayName + "[" + index + "]");
                b.putAssignedValue(name, createContract(ae.getValue(), localVar, b));
                if(aae.getName() instanceof NameExpr){
                    b.setPure(localVar.contains(((NameExpr) aae.getName()).getName()));
                }
            } else {
                System.out.println("Assignment target " +  ae.getTarget() + " of " + ae.getTarget().getClass() + " not covered!");
                b.setPure(false);
            }
            return null;
        } else {
            if (e instanceof BinaryExpr) {
                BinaryExpr be = (BinaryExpr) e;
                Expression left = createContract(be.getLeft(), localVar, b);
                Expression right = createContract(be.getRight(), localVar, b);

                if (left != null && right != null) {
                    be.setLeft(left);
                    be.setRight(right);
                    return be;
                } else {
                    return null;
                }
            } else if (e instanceof UnaryExpr) {
                UnaryExpr ue = (UnaryExpr) e;
                if (ue.getExpr() instanceof NameExpr) {
                    NameExpr nameExpr = (NameExpr) ue.getExpr();
                    SimpleName name = nameExpr.getName();
                    IntegerLiteralExpr ile = new IntegerLiteralExpr();
                    ile.setValue("1");
                    Expression temp;
                    if (b.getAssignedValues().containsKey(nameExpr.getName())) {
                        temp = b.getAssignedValues().get(nameExpr.getName());
                    } else {
                        temp = nameExpr;
                    }
                    BinaryExpr be = new BinaryExpr();
                    be.setLeft(temp);
                    be.setRight(ile);
                    if (ue.getOperator() == UnaryExpr.Operator.postDecrement) {
                        be.setOperator(BinaryExpr.Operator.minus);
                        b.putAssignedValue(name, be);
                        return temp;
                    } else if (ue.getOperator() == UnaryExpr.Operator.postIncrement) {
                        be.setOperator(BinaryExpr.Operator.plus);
                        b.putAssignedValue(name, be);
                        return temp;
                    } else if (ue.getOperator() == UnaryExpr.Operator.preDecrement) {
                        be.setOperator(BinaryExpr.Operator.minus);
                        b.putAssignedValue(name, be);
                        return be;
                    } else if (ue.getOperator() == UnaryExpr.Operator.preIncrement) {
                        be.setOperator(BinaryExpr.Operator.plus);
                        b.putAssignedValue(name, be);
                        return be;
                    } else {
                        return e;
                    }
                } else {
                    return ue.setExpr(createContract(ue.getExpr(), localVar, b));
                }
            } else if (e instanceof EnclosedExpr) {
                if (((EnclosedExpr) e).getInner().isPresent()) {
                    return ((EnclosedExpr) e).setInner(createContract(((EnclosedExpr) e).getInner().get(), localVar, b));
                } else {
                    //This should really not happen...
                    System.out.println("This is not covered! Enclosed expression is non-existent!");
                    return null;
                }
            } else if (e instanceof ObjectCreationExpr) {
                //TODO: Check if constructor is pure? What will this actually do? Maybe always false?
                ObjectCreationExpr oce = (ObjectCreationExpr) e;
                boolean pure = true;
                for (Expression exp : oce.getArgs()) {
                    createContract(exp, localVar, b);
                }
                if (oce.getAnonymousClassBody().isPresent()) {
                    //TODO: Evaluate body
                    b.setPure(false);

                }
                b.setPure(false);
                return e;
            } else if (e instanceof ArrayCreationExpr) {
                ArrayCreationExpr ace = (ArrayCreationExpr) e;
                if (ace.getInitializer().isPresent()) {
                    createContract(ace.getInitializer().get(), localVar, b);
                }
                return e;
            } else if (e instanceof ArrayInitializerExpr) {

                ArrayInitializerExpr aie = (ArrayInitializerExpr) e;
                for (Expression exp : aie.getValues()) {
                    createContract(exp, localVar, b);
                }
                return e;
            } else if (e instanceof ArrayAccessExpr) {
                ArrayAccessExpr aae = (ArrayAccessExpr) e;
                createContract(aae.getIndex(), localVar, b);
                return e;
            } else if (e instanceof CastExpr) {
                CastExpr ce = (CastExpr) e;
                createContract(ce.getExpr(), localVar, b);
                return e;
            } else if (e instanceof ConditionalExpr) {
                //ConditionalExpr ce = (ConditionalExpr) e;
                //ce.setCondition(createContract(ce.getCondition(), localVar, b));
                //ce.setThenExpr(createContract(ce.getThenExpr(), localVar, b));
                //ce.setElseExpr(createContract(ce.getElseExpr(), localVar, b));
                //TODO: A lot of work has to be done in order to properly evaluate conditionals.
                return null;
            } else if (e instanceof InstanceOfExpr) {
                if (((InstanceOfExpr) e).getExpr() instanceof MethodCallExpr) {
                    createContract(((InstanceOfExpr) e).getExpr(), localVar, b);
                }
                return e;
            } else if (e instanceof LambdaExpr) {
                System.out.println("LAMDA EXPRESSIONS ARE NOT SUPPORTED");
                b.setPure(false);
                return e;
            } else if (e instanceof SuperExpr) {
                //TODO: Need to have entire package in scope
                //TODO: Also implement this
                System.out.println("Super expression is not covered");
                b.setPure(false);
                return e;
            } else {
                System.out.println("Expression " + e + " of " + e.getClass() + " is not covered");
                b.setPure(false);
                return null;
            }
        }
    }

    private void writeToFile(String path, File projectDir, String toPrint){

        // TODO : Generate package signature

        // Extract imports
        String packageAndImports = extractImports(new File(projectDir + path));

        Path p = Paths.get("Generated" + path);

        // Check that all directories exist
        for(int i = 1; i < p.getNameCount(); i++){
            Path currentPath = p.subpath(0,i);

            // If not, create them
            if(!Files.exists(currentPath)){
                try{
                    Files.createDirectories(currentPath);
                } catch (IOException e){
                    e.printStackTrace();
                }
            }

        }

        // Now we write to file
        try {
            Files.write(p, Arrays.asList(packageAndImports + toPrint), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String extractImports(File fileDir){
        StringBuilder sb = new StringBuilder();

        new DirExplorer((level, path, file) -> true, (level, path, file) -> {
            try {
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(CompilationUnit n, Object arg) {
                        super.visit(n, arg);

                        // Extract package if it exists
                        if(n.getPackage().isPresent()){
                            sb.append(n.getPackage().get().toString());
                        }

                        // Extract all imports
                        NodeList<ImportDeclaration> imports = n.getImports();
                        for(ImportDeclaration id : imports){
                            sb.append(id.toString());
                        }
                    }
                }.visit(JavaParser.parse(file), null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(fileDir);

        return sb.toString();
    }

    public static void main(String args[]){
        File projectDir = new File("src/main/java/Examples/SingleExample");
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
                        ContractGenerator cg = new ContractGenerator(n, path, projectDir);
                    }
                }.visit(JavaParser.parse(file), null);
                //System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }
}
