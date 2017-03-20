package ContractGeneration;

import ContractGeneration.Resources;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserConstructorDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.model.declarations.*;
import com.github.javaparser.symbolsolver.model.typesystem.Type;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.*;
import com.google.common.base.Strings;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import Contract.*;

public class ContractGenerator {
    //Should be initialized with a class
    private ClassOrInterfaceDeclaration target;
    private ArrayList<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();
    private HashMap<CallableDeclaration, Contract> contracts = new HashMap<>();
    private CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    private LinkedList<String> activeReferences = new LinkedList<>();

    public ContractGenerator(ClassOrInterfaceDeclaration coid, String path, File projectDir){
        this.target = coid;
        if(target.isInterface()){
            return;
        }

        // Set all fields as public for specification
        for(FieldDeclaration fd : coid.getFields()){
            setFieldAsPublic(fd);
        }

        //Create the combinedTypeSolver
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("../RCC")));
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("../RCC/RCC/java")));
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java")));
        //combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java/Examples")));
        //combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java/Examples/SingleExample/CryptoLib")));
        //combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main")));
        //combinedTypeSolver.add(new JavaParserTypeSolver(new File(System.getProperty("java.home"))));
        //Save all class variables of the class
        for (BodyDeclaration<?> b : target.getMembers()){
            if(b instanceof FieldDeclaration){
                fields.add((FieldDeclaration) b);
            }
        }
        //Start generating contracts, method by method
        for (BodyDeclaration<?> bd : target.getMembers()){
            if(bd instanceof CallableDeclaration){
                CallableDeclaration cd = (CallableDeclaration) bd;
                try {
                    contracts.put(cd ,createContract((CallableDeclaration) bd));
                } catch (TooManyLeafsException | SymbolSolverException | CallingMethodWithoutContractException | UncoveredStatementException e) {
                    contracts.put(cd, null);
                }
            }
        }
        for(CallableDeclaration cd : contracts.keySet()){
            Contract c = contracts.get(cd);
            if(c != null) {
                if(false) {
                    System.out.println("-----------------");
                    System.out.println();
                    System.out.println(cd.getName() + "\n" + c);
                    System.out.println();
                    System.out.println("Purity status: " + c.isPure());
                    System.out.println();
                    System.out.println("-----------------");
                }
                cd.setComment(new BlockComment(c.extractContract()));

            }
        }

        writeToFile(path, projectDir, coid.toString());

    }

    private void setFieldAsPublic(FieldDeclaration fd){
        fd.setComment(new BlockComment("@ spec_public @"));
    }

    private boolean endAssrt(CallableDeclaration cd, Statement s) {
        NodeList<Statement> stmtList;
        if(cd instanceof MethodDeclaration){
            stmtList = ((MethodDeclaration) cd).getBody().get().getStatements();
        } else if(cd instanceof ConstructorDeclaration){
            stmtList = ((ConstructorDeclaration) cd).getBody().getStatements();
        } else {
            System.out.println("NOT METHOD NOR CONSTRUCTOR DECLARATION");
            stmtList = null;
        }

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

    private BinaryExpr allConditionsComplemented(ArrayList<Expression> allConditions){
        // If it looks stupid but it works, it aint stupid
        UnaryExpr ue = new UnaryExpr(new UnaryExpr(new EnclosedExpr(allConditions.get(0)), UnaryExpr.Operator.LOGICAL_COMPLEMENT), UnaryExpr.Operator.PLUS);
        for(int i = 1; i < allConditions.size(); i++) {
            if(i == (allConditions.size() - 1)){
                // We're at the last conditions, so we finish building our BinaryExpr
                UnaryExpr temp = new UnaryExpr(new EnclosedExpr(allConditions.get(i)), UnaryExpr.Operator.LOGICAL_COMPLEMENT);
                return new BinaryExpr(ue.getExpression(),temp, BinaryExpr.Operator.AND);
            } else {
                // Build the expression that is the negation of all previous case conditions
                UnaryExpr temp = new UnaryExpr(new EnclosedExpr(allConditions.get(i)), UnaryExpr.Operator.LOGICAL_COMPLEMENT);
                ue.setExpression(new BinaryExpr(ue.getExpression(), temp, BinaryExpr.Operator.AND));
            }
        }

        return null;
    }

    private boolean startAssert(CallableDeclaration cd, Statement s){
        NodeList<Statement> stmtList;
        if(cd instanceof MethodDeclaration){
            stmtList = ((MethodDeclaration) cd).getBody().get().getStatements();
        } else if (cd instanceof ConstructorDeclaration) {
            stmtList = ((ConstructorDeclaration) cd).getBody().getStatements();
        } else {
            stmtList = null;
            System.out.println("NOT METHOD NOR CONSTRUCTOR DECLARATION");
        }

        int index = stmtList.indexOf(s);
        for(int i = 0 ; i < index ; i++){
            if(!(stmtList.get(i) instanceof AssertStmt)){
                return false;
            }
        }
        return true;
    }

    public Contract createContract(CallableDeclaration cd) throws TooManyLeafsException, SymbolSolverException, CallingMethodWithoutContractException, UncoveredStatementException {
        //Get row for md
        //Get top-level assertions
        System.out.println("Working on " + cd.getName());
        if(contracts.containsKey(cd)){
            System.out.println("Already done " + cd.getName() + " returning.");
            return contracts.get(cd);
        }
        NodeList<Statement> stmtList;
        if(cd instanceof MethodDeclaration){
            if(!((MethodDeclaration) cd).getBody().isPresent()){
                return null;
            }
            stmtList = ((MethodDeclaration) cd).getBody().get().getStatements();
        } else if (cd instanceof ConstructorDeclaration) {
            stmtList = ((ConstructorDeclaration) cd).getBody().getStatements();
        } else {
            stmtList = null;
            System.out.println("NOT METHOD NOR CONSTRUCTOR DECLARATION");
        }
        Contract c = new Contract();
        Behavior b = c.getCurrentBehavior();
        b.setCallableDeclaration(cd);
        for(Object o : cd.getParameters()){
            Parameter p = (Parameter) o;
            b.addLocalVar(p.getName());
            b.putAssignedValue(p.getName(), new NameExpr(p.getName()));
        }
        for(FieldDeclaration fd : fields){
            for(VariableDeclarator vd : fd.getVariables()){
                SimpleName name = new SimpleName("this." + vd.getName());
                b.putAssignedValue(name, new NameExpr(new SimpleName("\\old(" + name + ")")));
            }
        }
        createContract(stmtList, c.getCurrentBehavior());
        System.out.println("Done working with " + cd.getName());
        return c;
    }

    public void createContract(NodeList<Statement> stmtList, Behavior b) throws TooManyLeafsException, SymbolSolverException, CallingMethodWithoutContractException, UncoveredStatementException {
        boolean pure = true;
        for(Statement s : stmtList){
            createContract(s, b);
        }
    }
    public void createContract(Statement s, Behavior b) throws TooManyLeafsException, SymbolSolverException, CallingMethodWithoutContractException, UncoveredStatementException {
        /* We first identify if the current statement is assert or return in which case we want to make sure our
        assertions (ensures) are handled correctly.
        */
        if (s instanceof AssertStmt){
            AssertStmt as = (AssertStmt) s;
            //An assert-statement can be seen as a precondition if it appears at the start of a function
            //We
            for(Behavior beh : b.getLeafs()) {
                Expression e = createContract(as.getCheck(), beh);
                if(e != null) {
                    if (startAssert(beh.getCallableDeclaration(), as)) {
                        beh.addPreCon(e);
                    }
                    //Add the assertion as a potential postcondition
                    b.addPostCon(e, false);
                }
            }
            //Create contract for the expression in the assertion

        } else if (s instanceof ReturnStmt){
            ReturnStmt rs = (ReturnStmt) s;
            if(rs.getExpression().isPresent()){
                //Create contracts for the return expression and add the return statement to the contract
                for(Behavior beh : b.getLeafs()){
                    Expression exp = createContract(rs.getExpression().get(),  beh);
                    beh.addPostCon(exp, true);
                }
            }
            //Path of execution ends and we close all behaviors on this path
            b.setClosed(true);
        } else if (s instanceof ThrowStmt) {
            ThrowStmt ts = (ThrowStmt) s;
            b.setExceptional(true);

            // These conditions is what will hold after we throw an exception
            // TODO : Make more intelligent
            LinkedList<PostCondition> listOfPostCons = b.getPostCons();
            LinkedList<Expression> listOfExprs = new LinkedList<>();
            for(PostCondition pc : listOfPostCons){
                listOfExprs.add(pc.getExpression());
            }

            if(ts.getExpression() instanceof ObjectCreationExpr){
                // We create a new object when throwing
                b.addException(((ObjectCreationExpr) ts.getExpression()).getType(), listOfExprs);
            } else {
                // We're throwing some already created exception
                Type t = JavaParserFacade.get(combinedTypeSolver).getType(ts.getExpression());
                b.addException(new ClassOrInterfaceType(t.asReferenceType().getQualifiedName()), listOfExprs);

            }
            b.setClosed(true);
        } else {
            //We are doing further modifications to our code thus we cannot guarantee our postconditions will hold.
            b.clearPostAssert();
            if (s instanceof ExpressionStmt) {
                for(Behavior beh : b.getLeafs()){
                    createContract(((ExpressionStmt) s).getExpression(), beh);
                }
            } else if (s instanceof IfStmt) {
                /*
                 * When evaluating a if-statement we split the current behavior in 2, one that enters the then-block and
                 * one that enters the else-block (if present). When evaluating the blocks the new behaviors created are
                 * set as the current behavior for their respective block. Once the if-statement is done the current
                 * behavior is set to the initial one.
                 */
                IfStmt sif = (IfStmt) s;
                if(b.getLeafs().size() > 10){
                    System.out.println("Too many leafs in: " + b.getCallableDeclaration().getName());
                    throw new TooManyLeafsException();
                }
                for(Behavior beh : b.getLeafs()) {
                    Expression ifCond = createContract(sif.getCondition(), beh);
                    Behavior a = new Behavior(beh);
                    a.addPreCon(ifCond);
                    beh.setClosed(true);
                    beh.addChild(a);
                    createContract(sif.getThenStmt(), a);

                    if (sif.getElseStmt().isPresent()) {
                        Behavior d = new Behavior(beh);
                        beh.addChild(d);
                        //TODO: d.addPreCon(); need to fix double negation
                        //d.addPreCon(ifCond);
                        d.addPreCon(new UnaryExpr(new EnclosedExpr(ifCond), UnaryExpr.Operator.LOGICAL_COMPLEMENT));
                        createContract(sif.getElseStmt().get(), d);
                    } else {
                        Behavior e = new Behavior(beh);
                        beh.addChild(e);
                        e.addPreCon(new UnaryExpr(new EnclosedExpr(ifCond), UnaryExpr.Operator.LOGICAL_COMPLEMENT));
                        //TODO: e.addPreCon(); need to fix double negation
                    }
                }
            } else if (s instanceof SwitchStmt) {
                // Loop through all entries, create contract
                NodeList<SwitchEntryStmt> entries = ((SwitchStmt) s).getEntries();

                // Will be used (negated) for the default case
                ArrayList<Expression> allConditions = new ArrayList<>();

                // Go though all active behaviors
                for(Behavior leaf : b.getLeafs()) {
                    Expression selector = ((SwitchStmt) s).getSelector();

                    leaf.setClosed(true);

                    // Go through all entries in switchcase and create behaviors for them
                    for (SwitchEntryStmt ses : entries) {
                        // Build the condition upon which we enter this case
                        Expression cond;
                        if(ses.getLabel().isPresent()) { // Entry has a label
                            cond = new BinaryExpr(selector, ses.getLabel().get(), BinaryExpr.Operator.EQUALS);
                            allConditions.add(cond);
                        } else { // No label, it's the default

                            cond = new BinaryExpr();
                            if(allConditions.size() == 1){
                                cond = new UnaryExpr(allConditions.get(0), UnaryExpr.Operator.LOGICAL_COMPLEMENT);
                            } else {
                                cond = allConditionsComplemented(allConditions);
                            }
                        }
                        // Create contract on condition
                        Expression contractCond = createContract(cond, leaf);

                        // Create new behavior and set as child
                        Behavior newB = new Behavior(leaf);
                        leaf.addChild(newB);
                        // Add precon to behavior
                        newB.addPreCon(contractCond);

                        // Create contract on body of case
                        createContract(ses.getStatements(), newB);

                    }
                }
            } else if (s instanceof BlockStmt) {
                BlockStmt bs = (BlockStmt) s;
                createContract(((BlockStmt) s).getStatements(), b);
            } else if (s instanceof BreakStmt) {
                return;
            } else if (s instanceof ContinueStmt) {
                return;
            } else if (s instanceof EmptyStmt) {
                return;
            } else if (s instanceof WhileStmt){
                WhileStmt ws = (WhileStmt) s;
                Statement body = ws.getBody();
                Behavior temporary = new Behavior(null);
                createContract(ws.getCondition(), temporary);
                createContract(body, temporary);
                for(Behavior leaf : temporary.getLeafs()){
                    for(SimpleName sn : leaf.getAssignables()){
                        b.putAssignedValue(sn, null);
                    }
                }
                b.setPure(false);
            } else if (s instanceof ForStmt) {
                ForStmt fs = (ForStmt) s;
                Behavior temporary = new Behavior(null);
                for(Expression e : fs.getInitialization()){
                    createContract(e, temporary);
                }
                if(fs.getCompare().isPresent()){
                    createContract(fs.getCompare().get(), temporary);
                }
                for(Expression e : fs.getUpdate()){
                    createContract(e, temporary);
                }
                createContract(fs.getBody(), temporary);
                for(Behavior leaf : temporary.getLeafs()){
                    for(SimpleName sn : leaf.getAssignables()){
                        b.putAssignedValue(sn, null);
                    }
                }
                b.setPure(false);
            } else if (s instanceof DoStmt){
                DoStmt ds = (DoStmt) s;
                Statement body = ds.getBody();
                Behavior temporary = new Behavior(null);
                createContract(ds.getCondition(), temporary);
                createContract(body, temporary);
                for(Behavior leaf : temporary.getLeafs()){
                    for(SimpleName sn : leaf.getAssignables()){
                        b.putAssignedValue(sn, null);
                    }
                }
                b.setPure(false);
            } else if (s instanceof SynchronizedStmt) {
                SynchronizedStmt ss = (SynchronizedStmt) s;
                createContract(ss.getExpression(), b);
                createContract(ss.getBody(), b);
            } else if(s instanceof TryStmt){
                throw new UncoveredStatementException();
            } else if (s instanceof LabeledStmt){
                LabeledStmt ls = (LabeledStmt) s;
                createContract(ls.getStatement(), b);
            } else if (s instanceof ExplicitConstructorInvocationStmt) {
                ExplicitConstructorInvocationStmt ecis = (ExplicitConstructorInvocationStmt) s;
                //JavaParserConstructorDeclaration jvpcd;
                /*
                SymbolReference<> sr;
                try {
                    sr = JavaParserFacade.get(combinedTypeSolver).solve(ecis);
                } catch (Exception error) {
                    System.out.println("Could not solver reference of super!");
                    throw new SymbolSolverException();
                }
                if(sr.getCorrespondingDeclaration() instanceof JavaParserConstructorDeclaration){
                    JavaParserConstructorDeclaration jpcd = (JavaParserConstructorDeclaration) sr.getCorrespondingDeclaration();
                    System.out.println("thisname" +  jpcd.getName());
                    createContract(sr.getCorrespondingDeclaration(), new Behavior(null));
                }*/
                throw new UncoveredStatementException();

            } else {
                System.out.println("Statement " + s + " of class " + s.getClass() + " is not covered");
                throw new UncoveredStatementException();
            }
        }
    }

    private Expression createContract(Expression e, Behavior b) throws SymbolSolverException, CallingMethodWithoutContractException {
        if(e == null){
            return null;
        }
        if(Resources.ignorableExpression(e)){
            return e;
        } else if (e instanceof NameExpr){
            NameExpr ne = (NameExpr) e;
            if(b.isLocalVar(ne.getName())){
                if(b.getAssignedValue(ne.getName()) != null){
                    return b.getAssignedValue(ne.getName());
                } else {
                    return ne;
                }
            } else {
                SimpleName sn;
                if(!ne.getName().toString().contains("this.")) {
                    sn = new SimpleName("this." + ne.getName());
                } else {
                    sn = ne.getName();
                }
                if(b.getAssignedValue(sn) != null){
                    return b.getAssignedValue(sn);
                } else {
                    return new NameExpr(sn);
                }
            }
        } else if(e instanceof MethodCallExpr){
            MethodCallExpr mce = (MethodCallExpr) e;
            MethodCallExpr newMCE = mce.clone();
            //System.out.println("MCE "+  mce);
            SymbolReference sr;
            for(Expression exp : mce.getArguments()){
                newMCE.getArguments().replace(exp, createContract(exp, b));
            }
            try{
                //System.out.println("arg " + mce.getArguments().get(0));
                //System.out.println("type of arg: ");
                //System.out.println(JavaParserFacade.get(combinedTypeSolver).getType(mce.getArguments().get(0)));
                sr = JavaParserFacade.get(combinedTypeSolver).solve(mce, false);
            } catch (Exception error){
                System.out.println();
                System.out.println("Cannot solve " + mce);
                throw new SymbolSolverException();
            }

            if(activeReferences.contains(sr.getCorrespondingDeclaration().getName())){
                return e;
            }
            activeReferences.add(sr.getCorrespondingDeclaration().getName());

            if(sr.getCorrespondingDeclaration() instanceof JavaParserMethodDeclaration){
                MethodDeclaration md = ((JavaParserMethodDeclaration) sr.getCorrespondingDeclaration()).getWrappedNode();
                Contract temp;
                if(contracts.containsKey(md)){
                    temp = contracts.get(md);
                } else {
                    try {
                        temp = createContract(md);
                        contracts.put(md, temp);
                    } catch (TooManyLeafsException | CallingMethodWithoutContractException | SymbolSolverException | UncoveredStatementException error){
                        contracts.put(md, null);
                        return null;
                    }
                }
                b.setClosed(true);
                if(temp == null){
                    throw new CallingMethodWithoutContractException();
                }
                for(Behavior beh : temp.getLeafs()){
                    Behavior newChild = new Behavior(b);
                    for (PreCondition pc : beh.getPreCons()) {
                        newChild.addPreCon(pc.getExpression());
                    }
                    for (PostCondition pc : beh.getPostCons()){
                        if(!pc.isReturn()){
                            newChild.addPostCon(createContract(pc.getExpression(), b), false);
                        }
                    }
                    for (SimpleName sn : beh.getAssignedValues().keySet()){
                        RemoveOldVisitor rov = new RemoveOldVisitor();
                        Expression exp = beh.getAssignedValue(sn);
                        rov.visit(exp, null);
                        newChild.putAssignedValue(sn, createContract(exp, b));
                    }
                    newChild.setExceptional(beh.getIsExceptional());
                    for(ExceptionCondition ec : beh.getExceptions()){
                        newChild.addException(ec.getType(), ec.getName());
                    }
                    b.addChild(newChild);
                }
                b.setPure(temp.isPure());
                activeReferences.remove(sr.getCorrespondingDeclaration().getName());
                if(!temp.isPure()){
                    //TODO: Should add comment stating method call might be unpure.
                    b.setImpureMethods();
                }
                return newMCE;
            } else if (sr.getCorrespondingDeclaration() instanceof ReflectionMethodDeclaration){
                ReflectionMethodDeclaration rmd = (ReflectionMethodDeclaration) sr.getCorrespondingDeclaration();
                b.setPure(false);
                if(rmd.getQualifiedName().equals("java.lang.System.exit")){
                    b.setDiverges(true);
                    b.setClosed(true);
                }
                activeReferences.remove(sr.getCorrespondingDeclaration().getName());
                return null;
            } else {
                //TODO: Handle other method calls
                System.out.println("Method call expression with declaration: " + sr.getCorrespondingDeclaration() + " is not covered!");
                b.setPure(false);
                activeReferences.remove(sr.getCorrespondingDeclaration().getName());
                return null;
            }
        } else if (e instanceof VariableDeclarationExpr){
            //might have to check if assigning the value of a method call
            VariableDeclarationExpr vde = (VariableDeclarationExpr) e;
            //System.out.println("TYPE? " + vde.getVariables().get(0).getInitializer().get().toString());
            //System.out.println(JavaParserFacade.get(combinedTypeSolver).getType(vde.getVariables().get(0)));

            for(VariableDeclarator vd : vde.getVariables()){
                b.addLocalVar(vd.getName());
                if(vd.getInitializer().isPresent()){
                    Expression initExp = vd.getInitializer().get();
                    //If we initialize a variable we save the name in the behaviors assigned values with the
                    // value of the expression that we get from evaluating the initializer
                    if(!(initExp instanceof ArrayCreationExpr)) {
                        //b.putAssignedValue(vd.getId().getName(), new NameExpr(vd.getId().getName()));
                        b.putAssignedValue(vd.getName(), createContract(initExp, b));
                    }
                }
            }
            return null;
        } else if (e instanceof AssignExpr){
            AssignExpr ae = (AssignExpr) e;
            SimpleName fieldName;
            if(ae.getTarget() instanceof FieldAccessExpr){
                fieldName = new SimpleName("this." +((FieldAccessExpr) ae.getTarget()).getName());
                b.setPure(false);
            } else if (ae.getTarget() instanceof NameExpr){
                NameExpr ne = (NameExpr) ae.getTarget();
                if(b.isLocalVar(ne.getName())){
                    fieldName = ne.getName();
                } else {
                    fieldName = new SimpleName("this." + ne.getName());
                }
                b.setPure(b.isLocalVar(ne.getName()));
            } else if(ae.getTarget() instanceof ArrayAccessExpr){
                ArrayAccessExpr aae = (ArrayAccessExpr) ae.getTarget();
                String arrayName = createContract(aae.getName(), b).toString();
                if(aae.getName() instanceof NameExpr) {
                    NameExpr ne = (NameExpr) aae.getName();
                    if(arrayName.equals("\\old(this." + ne.toString() + ")") && !b.isLocalVar(ne.getName())){
                        arrayName = "this." + ne.toString();
                    }
                }

                String index = createContract(aae.getIndex(), b).toString();
                fieldName = new SimpleName( arrayName + "[" + index + "]");
                if(aae.getName() instanceof NameExpr){
                    b.setPure(b.isLocalVar(((NameExpr) aae.getName()).getName()));
                }
            } else {
                System.out.println("Assignment target " +  ae.getTarget() + " of " + ae.getTarget().getClass() + " not covered!");
                b.setPure(false);
                return null;
            }
            Expression exp = createContract(ae.getValue(), b);
            b.putAssignedValue(fieldName, exp);
            return null;
        } else if (e instanceof BinaryExpr) {
            BinaryExpr be = (BinaryExpr) e;
            Expression left = createContract(be.getLeft(), b);
            Expression right = createContract(be.getRight(), b);

            if (left != null && right != null) {
                BinaryExpr newBe = new BinaryExpr();
                newBe.setLeft(left);
                newBe.setRight(right);
                newBe.setOperator(be.getOperator());
                return newBe;
            } else {
                return null;
            }
        } else if (e instanceof UnaryExpr) {
            UnaryExpr ue = (UnaryExpr) e;
            if (ue.getExpression() instanceof NameExpr) {
                NameExpr nameExpr = (NameExpr) ue.getExpression();
                SimpleName name = nameExpr.getName();
                if(!b.isLocalVar(name)){
                    name.setIdentifier("this." + name.getId());
                }
                IntegerLiteralExpr ile = new IntegerLiteralExpr();
                ile.setValue("1");
                Expression temp;
                if(b.getAssignedValue(nameExpr.getName()) != null){
                    temp = b.getAssignedValue(nameExpr.getName());
                } else {
                    temp = nameExpr;
                }
                BinaryExpr be = new BinaryExpr();
                if(temp == null){
                    return null;
                }
                be.setLeft(temp);
                be.setRight(ile);
                if (ue.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT) {
                    be.setOperator(BinaryExpr.Operator.MINUS);
                    b.putAssignedValue(name, be);
                    return temp;
                } else if (ue.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT) {
                    be.setOperator(BinaryExpr.Operator.PLUS);
                    b.putAssignedValue(name, be);
                    return temp;
                } else if (ue.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT) {
                    be.setOperator(BinaryExpr.Operator.MINUS);
                    b.putAssignedValue(name, be);
                    return be;
                } else if (ue.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT) {
                    be.setOperator(BinaryExpr.Operator.PLUS);
                    b.putAssignedValue(name, be);
                    return be;
                } else {
                    return e;
                }
            } else {
                createContract(ue.getExpression(), b);
                return ue;
            }
        } else if (e instanceof EnclosedExpr) {
            if (((EnclosedExpr) e).getInner().isPresent()) {
                return ((EnclosedExpr) e).setInner(createContract(((EnclosedExpr) e).getInner().get(), b));
            } else {
                //This should really not happen...
                System.out.println("This is not covered! Enclosed expression is non-existent!");
                return null;
            }
        } else if (e instanceof ObjectCreationExpr) {
            //TODO: Check if constructor is pure? What will this actually do? Maybe always false?
            ObjectCreationExpr oce = (ObjectCreationExpr) e;
            boolean pure = true;
            for (Expression exp : oce.getArguments()) {
                createContract(exp, b);
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
                createContract(ace.getInitializer().get(), b);
            }
            return e;
        } else if (e instanceof ArrayInitializerExpr) {

            ArrayInitializerExpr aie = (ArrayInitializerExpr) e;
            for (Expression exp : aie.getValues()) {
                createContract(exp, b);
            }
            return e;
        } else if (e instanceof ArrayAccessExpr) {
            ArrayAccessExpr aae = (ArrayAccessExpr) e;
            createContract(aae.getIndex(), b);
            return e;
        } else if (e instanceof CastExpr) {
            CastExpr ce = (CastExpr) e;
            createContract(ce.getExpression(), b);
            return e;
        } else if (e instanceof ConditionalExpr) {
            ConditionalExpr ce = (ConditionalExpr) e;
            ConditionalExpr newCe = new ConditionalExpr();
            newCe.setCondition(createContract(ce.getCondition(), b));
            newCe.setThenExpr(createContract(ce.getThenExpr(), b));
            newCe.setElseExpr(createContract(ce.getElseExpr(), b));
            return new EnclosedExpr(newCe);
        } else if (e instanceof InstanceOfExpr) {
            if (((InstanceOfExpr) e).getExpression() instanceof MethodCallExpr) {
                createContract(((InstanceOfExpr) e).getExpression(), b);
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

    private static void clearDirectory() throws IOException{
        Path p = Paths.get("Generated");
        System.out.println(Files.exists(p));
        if(Files.exists(p)){
            Files.walkFileTree(p, new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if(exc != null){
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private void writeToFile(String path, File projectDir, String toPrint){

        // TODO : Generate package signature

        // Extract imports
        String packageAndImports = extractImports(new File(projectDir + path));

        Path p = Paths.get("Generated/" + projectDir.toPath() + path);

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
            if(Files.exists(p)){
                // Files already exists, so we do not write package and import information again
                Files.write(p, Arrays.asList(toPrint), Charset.forName("UTF-8"), StandardOpenOption.APPEND);
            } else {
                Files.write(p, Arrays.asList(packageAndImports + toPrint), Charset.forName("UTF-8"));
            }
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
                        if(n.getPackageDeclaration().isPresent()){
                            sb.append(n.getPackageDeclaration().get().toString());
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
        //File projectDir = new File("../RCC");
        //File projectDir = new File("src/main/java/Examples");
        File projectDir = new File("src/main/java/Examples/SingleExample");
        try {
            clearDirectory();
        } catch (IOException ioe){
            System.out.println("Could not delete directory with generated files.");
            System.out.println("Consider manually deleting and rerunning.");
            ioe.printStackTrace();
            System.exit(1);
        }

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
