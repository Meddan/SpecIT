package ContractGeneration;

/**
 * Exception thrown when the SymbolSolver fails to solve.
 */
public class SymbolSolverException extends Exception {
    public String message;
    public SymbolSolverException(String msg){
        super();
        message = msg;
    }
    public String toString(){
        return "SymbolSolverException: " + message;
    }
}
