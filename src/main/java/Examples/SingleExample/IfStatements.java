package Examples.SingleExample;

/**
 * Created by dover on 2017-02-03.
 */
public class IfStatements {
    int a;
    public String simplestCase(int n){
        boolean b = true;
        boolean c = false;
        if(!b) {
            if(c){
                a = 1;
            }
            a = 2;
        } else {
            a = 3;
            assert false;
            return "hi";
        }
        assert (true);
        return "hello";
    }
}
