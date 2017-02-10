package Contract;

import com.github.javaparser.ast.expr.Expression;

/**
 * Created by meddan on 10/02/17.
 */
public class PreCondtion {
    private Expression expression;

    public PreCondtion(Expression e){
        expression = e;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }
    public String toString(){
        return "requires " + expression.toString() + ";";
    }
}
