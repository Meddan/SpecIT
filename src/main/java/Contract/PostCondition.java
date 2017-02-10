package Contract;

import com.github.javaparser.ast.expr.Expression;

/**
 * Class representing a postcondition which is essentially an expression.
 */
public class PostCondition {
    private boolean isReturn;
    private Expression expression;

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public boolean isReturn(){
        return isReturn;
    }

    public String toString(){
        if(isReturn){
            return "ensures \result == " + expression.toString();
        } else {
            return "ensures " + expression.toString();
        }
    }

    public PostCondition(Expression e, boolean isReturn){
        this.isReturn = isReturn;
        this.expression = e;
    }

}
