import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;

public class ReturnStatementFinder {

    public static void findReturn(File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new NodeIterator(new NodeIterator.NodeHandler() {
                    @Override
                    public boolean handle(Node node) {
                        if (node instanceof ReturnStmt) {
                            System.out.println(" [Lines " + node.getBegin() + " - " + node.getEnd() + " ] " + node);
                            System.out.println(((ReturnStmt)node).getExpression().get());
                            ResultContract rc = ContractGenerator.generateReturn((ReturnStmt) node);
                            System.out.println(rc.toString());

                            return false;
                        } else {
                            return true ? true : false;
                        }
                    }
                }).explore(JavaParser.parse(file));
                System.out.println(); // empty line
            } catch ( IOException e) {
                new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    public static void main(String[] args) {
        System.out.println("hello");
        File projectDir = new File("/Users/meddan/Documents/Skola/Exjobb/src/main/java");
        findReturn(projectDir);
        System.out.println("we did not crash");
    }
}