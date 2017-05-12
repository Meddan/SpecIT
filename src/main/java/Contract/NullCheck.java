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

    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(getClass() != o.getClass()){
            return false;
        }

        NullCheck nc = ((NullCheck) o);

        return expression.equals(nc.expression);

    }

}
