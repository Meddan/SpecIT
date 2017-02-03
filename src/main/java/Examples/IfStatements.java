package Examples;

/**
 * Created by dover on 2017-02-03.
 */
public class IfStatements {

    public boolean method1(boolean a){
        if(a){
            return true;
        } else {
            return false;
        }
    }

    public String method2(int n){
        if(n > 0){
            return "Positive!";
        } else if (n < 0) {
            return "Negative!";
        }
        return "Zero.";
    }

    // wow such strong method cool story
    public String methodXX(int n){
        if(n < 25){
            return "foo";
        } else if (n >= 25){
            return "bar";
        } else {
            throw new IllegalArgumentException();
        }
    }


}
