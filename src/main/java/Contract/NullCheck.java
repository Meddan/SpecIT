package Contract;

import com.github.javaparser.ast.expr.Expression;

/**
 * Class representing a preconditional nullcheck
 */
public class NullCheck {
    private Expression expression;
    public NullCheck(Expression exp){
        expression = exp;
    }
    public String toString(){
        return "requires " + expression.toString() + " != null;\n";
    }

}
