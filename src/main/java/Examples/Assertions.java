package Examples;

/**
 * Created by meddan on 03/02/17.
 */
public class Assertions {
    private int a;

    /*
     * Assert as precondition
     */
    public int method1(){
        assert a != 5;
        return a;
    }
    /*
    Assert as postcondition
     */
    public int method2(){
        a = 2;
        a = a * 2;
        a++;
        assert a != 5;
        return a;
    }
    public static void main(String args[]){
        Assertions ass = new Assertions();
        ass.method1();
    }
}
