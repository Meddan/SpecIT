package Contract;

import com.github.javaparser.ast.expr.Expression;

public class VariableValue {
    public Expression getValue() {
        return value;
    }

    public enum Status{
        known, unknown, old;
    }
    private Expression value;

    public Status getStatus() {
        return status;
    }

    private Status status;
    public VariableValue(Expression e){
        this.value = e;
        this.status = Status.known;
    }
    public VariableValue(Status s){
        if(s == Status.known){
            throw new IllegalArgumentException();
        } else {
            this.status = s;
        }
    }
    public String toString(){
        return "" + this.status + " " + value;
    }
}
