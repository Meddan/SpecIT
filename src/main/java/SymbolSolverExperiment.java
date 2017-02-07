import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.methods.MethodUsage;
import com.github.javaparser.symbolsolver.model.typesystem.Type;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.google.common.base.Strings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by meddan on 06/02/17.
 */
public class SymbolSolverExperiment {
    public static void findreturn(File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new NodeIterator(new NodeIterator.NodeHandler() {
                    @Override
                    public boolean handle(Node node) {
                        if (node instanceof ExpressionStmt) {
                            //System.out.println(node);
                            System.out.println("expstmt");
                            Expression e = ((ExpressionStmt) node).getExpression();
                            //if(e instanceof ReturnStmt){
                                //dostuff(e);
                            //}

                            if (e instanceof MethodCallExpr){
                                System.out.println();
                                System.out.println("METHOD CALL:");
                                System.out.println(((MethodCallExpr) e));

                                JavaParserTypeSolver jpts2 = new JavaParserTypeSolver(new File("target/classes/Examples/SymbolSolver"));
                                CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
                                combinedTypeSolver.add(new ReflectionTypeSolver());
                                combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java/Examples/SymbolSolver")));
                                //combinedTypeSolver.add(jpts2);
                                MethodUsage mu = JavaParserFacade.get(combinedTypeSolver).solveMethodAsUsage((MethodCallExpr) e);
                                System.out.println("MU");
                                System.out.println(mu);
                                System.out.println("DECLARATION");
                                System.out.println(mu.getDeclaration());
                                System.out.println();
                            }
                            /*
                            if(((ExpressionStmt)node).getExpression() instanceof AssignExpr){
                                AssignExpr
                            }*/
                            return false;
                        }  else {
                            return true;
                        }
                    }
                }).explore(JavaParser.parse(file));
                System.out.println(); // empty line
            } catch (IOException e) {
                new RuntimeException(e);
            }
        }).explore(projectDir);
    }
    private static void dostuff(Node node){
        JavaParserTypeSolver jpts = new JavaParserTypeSolver(new File("src/main/java/Examples/SymbolSolver"));
        JavaParserTypeSolver jpts2 = new JavaParserTypeSolver(new File("target/classes/Examples/SymbolSolver"));
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java/Examples/SymbolSolver")));
        combinedTypeSolver.add(jpts2);

        System.out.println("stuff");
        System.out.println(node);
        Type typeOfTheNode = JavaParserFacade.get(combinedTypeSolver).getType(node);

        System.out.println(typeOfTheNode);
        System.out.println("done");

    }
    private static void methodfind(Node node){
        System.out.println("methodfind");
        System.out.println(node);


    }
    public static void main(String[] args) {
        File projectDir = new File("src/main/java/Examples/SymbolSolver");
        findreturn(projectDir);
        /*try {
            CompilationUnit cu = JavaParser.parse(projectDir);
            for(Node n : cu.getChildNodes()){

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
    }
}
