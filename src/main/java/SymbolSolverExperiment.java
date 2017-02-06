import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
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
                            System.out.println(node);
                            dostuff(node);
                            return false;
                        } else {
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
        System.out.println("stuff");
        JavaParserTypeSolver jpts = new JavaParserTypeSolver(new File("src/main/java/Examples/SymbolSolver"));
        JavaParserTypeSolver jpts2 = new JavaParserTypeSolver(new File("target/classes/Examples/SymbolSolver"));
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());
        combinedTypeSolver.add(new JavaParserTypeSolver(new File("src/main/java/Examples/SymbolSolver")));
        combinedTypeSolver.add(jpts2);


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
