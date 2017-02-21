package Examples.SingleExample;

/**
 * Created by dover on 2017-02-03.
 */
public class IfStatements {
    int a;
    int b = 3 + a;
    public String simplestCase(int n){
        if(a++ > 4 ){
            return "hi";
        }
        a = b == 3 ? 2 : 1;
        return "hello";
    }
}
