import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;

public class StatementLines {

    public static void statementsByLine(File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new NodeIterator(new NodeIterator.NodeHandler() {
                    @Override
                    public boolean handle(Node node) {
                        if (node instanceof Statement) {
                            System.out.println(" [Lines " + node.getBegin().get() + " - " + node.getEnd().get() + " ] " + node);
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

    public static void main(String[] args) {
        File projectDir = new File("C:/Users/dover/IdeaProjects/testparser");
        statementsByLine(projectDir);
    }

}