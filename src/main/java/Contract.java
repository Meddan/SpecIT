import com.github.javaparser.ast.stmt.Statement;

/**
 * Created by meddan on 31/01/17.
 */
public class Contract {
    private Statement startStmnt;
    public Contract(Statement start){
        startStmnt = start;
    }
    public Statement getStartStmnt(){
        return startStmnt;
    }
}
