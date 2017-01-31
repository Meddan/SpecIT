import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ContractGenerator {
    private ArrayList<Contract> contracts = new ArrayList<>();
    public void createContracts(){
        File projectDir = new File("src/main/java");
        for (ReturnStmt rs : findReturn(projectDir)){
            contracts.add(new Contract(rs));
        }

        for (Contract c : contracts){
            buildContract(c);
        }
    }

    private void buildContract(Contract c) {
        c.getStartStmnt();

    }

    public List<ReturnStmt> findReturn(File projectDir) {
        LinkedList<ReturnStmt> stmts = new LinkedList<ReturnStmt>();
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new NodeIterator(new NodeIterator.NodeHandler() {
                    @Override
                    public boolean handle(Node node) {
                        if (node instanceof ReturnStmt) {
                            System.out.println(" [Lines " + node.getBegin() + " - " + node.getEnd() + " ] " + node);
                            stmts.add((ReturnStmt) node);
                            return false;
                        } else {
                            return true;
                        }
                    }
                }).explore(JavaParser.parse(file));
                System.out.println(); // empty line
            } catch ( IOException e) {
                new RuntimeException(e);
            }
        }).explore(projectDir);
        return stmts;
    }
}
