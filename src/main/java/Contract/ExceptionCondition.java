package Contract;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;
/**
 * Created by meddan on 10/02/17.
 */
public class ExceptionCondition {
    private Type type;
    private Expression expression;
    public ExceptionCondition(Type t, Expression e){
        type = t;
        expression = e;
    }

    public Type getType() {
        return type;
    }

    public Expression getName() {
        return expression;
    }
}
