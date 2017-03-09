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

    /**
     * Creates a string representation of this ExceptionCondition.
     *
     * Use when wanting to create a signal property for a behavior.
     *
     * @return a string representation of this ExceptionCondition
     */
    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("signals (");
        sb.append(type.toString());
        sb.append(") (");

        if(expressions.isEmpty()){
            sb.append("true");
        } else {
            for(Expression e : expressions){
                sb.append(e.toString());
                if(!expressions.getLast().equals(e)){
                    sb.append(" && ");
                }
            }
        }

        sb.append(");\n");

        return sb.toString();

    }

    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(getClass() != o.getClass()){
            return false;
        }

        ExceptionCondition ec = ((ExceptionCondition) o);

        return type.equals(ec.getType()) && expressions.equals(ec.getName());
    }
}
