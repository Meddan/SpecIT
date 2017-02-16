package Contract;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;

import java.util.LinkedList;

/**
 * Created by meddan on 10/02/17.
 */
public class ExceptionCondition {
    private Type type;
    /* List of expressions to hold after axception is thrown */
    private LinkedList<Expression> expressions = new LinkedList<Expression>();

    public ExceptionCondition(Type t, Expression e){
        type = t;
        expressions.add(e);
    }

    public ExceptionCondition(Type t, LinkedList<Expression> e){
        type = t;
        expressions = e;
    }

    public Type getType() {
        return type;
    }

    public LinkedList<Expression> getName() {
        return expressions;
    }
}
