package Examples.SymbolSolver;

/**
 * Created by meddan on 06/02/17.
 */
public class Basic {
    int f;
    public static int test(){
        int a = 3;
        Basic.method();
        return a;
    }
    public static int method(){
        System.out.println("doing method");
        return 0;
    }

    /**
     * tests that we create new contexts when entering if statements
     *
     */
    public int advanced(){
        if(true){
            int f = 2;
        } else {
            f = 3;
        }
        if(true)
            return 0;
        else
            f = 1+1;
        return f;

    }
}
