package Examples;

/**
 * Created by dover on 2017-02-03.
 */
public class IfStatements {

    private String x;

    public boolean simplestCase(int n){
        if(n == 1)
            return true;
        else
            return false;
    }

    public boolean simpleCase(boolean a){
        if(a){
            return true;
        } else {
            return false;
        }
    }

    public String severalCases(int n){
        if(n > 0){
            return "Positive!";
        } else if (n < 0) {
            return "Negative!";
        }
        return "Zero.";
    }

    public void assigningToClassField(int n){
        if(n == 42){
            x = "The answer";
        } else {
            x = "Not the answer";
        }
    }

    public String usingHelperMethod(int n){
        if(someHelperMethod(n)){
            return "Got helped.";
        }

        return "No help.";
    }

    // wow such strong method cool story
    public String severalCasesAndException(int n){
        if(n < 25){
            return "foo";
        } else if (n >= 25){
            return "bar";
        } else {
            throw new IllegalArgumentException();
        }
    }

    private boolean someHelperMethod(int n){
        return n == 10;
    }

}
