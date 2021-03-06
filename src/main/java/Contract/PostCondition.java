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
        if(expression == null){
            return "";
        }
        if(isReturn){
            return "ensures \\result == (" + expression.toString() + ");\n";
        } else {
            return "ensures " + expression.toString() + ";\n";
        }
    }

    public PostCondition(Expression e, boolean isReturn){
        this.isReturn = isReturn;
        this.expression = e;
    }

    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(getClass() != o.getClass()){
            return false;
        }

        PostCondition pc = ((PostCondition) o);
        if(expression == null){
            return false;
        }
        return expression.equals(pc.getExpression()) && isReturn == pc.isReturn();
    }

}
