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
        } else if (n > 25){
            return "bar";
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void emptyReturn(String s){
        if(!s.equals("FOO")){
            return;
        } else {
            x = "BAR";
        }
    }

    public void moreExceptions(int n){
        if(n > 0){
            if(n < 10){
                x = "Small error";
            } else {
                x = "Big error";
            }
            throw new IllegalArgumentException();
        } else {
            x = "This is good number" + n;
        }
    }

    public void didSomeoneSayMoreExceptions(int n){
        if(n > 0){
            IllegalArgumentException e;
            if(n < 10){
                e = new IllegalArgumentException("Small error");
            } else {
                e = new IllegalArgumentException("Big error");
            }
            throw e;
        } else {
            x = "This is good number" + n;
        }
    }

    private boolean someHelperMethod(int n){
        return n == 10;
    }

}
