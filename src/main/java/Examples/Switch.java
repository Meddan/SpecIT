package Examples;

/**
 * Created by dover on 2017-03-06.
 */
public class Switch {

    public int doubleUpWithReturn(int n){
        switch(n){
            case 1:
                return n + 1;
            case 2:
                return n + 2;
            case 3:
                return n + 3;
            default:
                return -1;
        }
    }

    public int doubleUpWithBreak(int n){
        switch(n){
            case 1:
                n = n + 1;
                break;
            case 2:
                n = n + 2;
                break;
            case 3:
                n = n + 3;
                break;
            default:
                return -1;
        }
        return n;
    }

}
