import com.github.javaparser.ast.expr.Expression;

import java.util.Optional;

public class ResultContract {
    private Optional<Expression> exp;
    public ResultContract(Optional<Expression> e){
        exp = e;
    }
    public boolean empty(){
        return !exp.isPresent();
    }
    public String toString(){
        if(exp.isPresent()) {
            return "\\result == " + exp.get();
        } else {
            return "";
        }
    }
}
