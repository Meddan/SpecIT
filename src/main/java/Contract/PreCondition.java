package Contract;

import com.github.javaparser.ast.expr.Expression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by meddan on 10/02/17.
 */
public class PreCondition {
    private Expression expression;

    public PreCondition(Expression e){
        expression = e;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
    public String toString(){
        if(expression == null){
            return "";
        }
        return "requires " + expression.toString() + ";\n";
    }

    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(getClass() != o.getClass()){
            return false;
        }

        PreCondition pc = ((PreCondition) o);
        if(expression == null){
            return pc.getExpression() == null;
        }
        return expression.equals(pc.getExpression());
    }
}
