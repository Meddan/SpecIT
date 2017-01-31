/**
 * Created by meddan on 31/01/17.
 */
public class ContractExpression {
    public enum type{
        implication, and, equal, symbol
    }
    private type type;
    public ContractExpression(type t){
        type = t;
    }
    public type getType(){
        return type;
    }
}
