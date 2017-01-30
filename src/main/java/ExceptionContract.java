import com.github.javaparser.ast.expr.Expression;

/**
 * Created by meddan on 30/01/17.
 */
public class ExceptionContract {
    private Expression exp;
    public ExceptionContract(Expression e) {
        exp = e;
    }
    public String toString(){
        return "\\signals (Exception) b1" + exp;
    }
}
