package ContractGeneration;

public class UncoveredStatementException extends Exception{
    private String message;
    public UncoveredStatementException(String msg){
        message = msg;
    }
    public String toString(){
        return "UncoveredStatementException " + message;
    }
}
