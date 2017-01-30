import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;

public class ContractGenerator {
    public static ResultContract generateReturn(ReturnStmt stmt){
        return new ResultContract(stmt.getExpression());
    }
    public static ExceptionContract generateException(ThrowStmt stmt){
        return new ExceptionContract(stmt.getExpression());
    }
}
