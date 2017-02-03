package Examples;

/**
 * Created by meddan on 03/02/17.
 */
public class Assertions {
    private int a;

    /*
     * Assert as precondition
     */
    public int basicAssert(){
        assert a != 5;
        return a;
    }
    /*
    Assert as postcondition
     */
    public int assertWithCode(){
        a = 2;
        a = a * 2;
        a++;
        assert a != 5;
        return a;
    }
    public int doubleAssert(){
        assert a != 3;
        assert a != 4;
        return a;
    }

    public static void main(String args[]){
        Assertions ass = new Assertions();
    }
}
