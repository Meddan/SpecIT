package Statistics;

import Contract.*;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by dover on 2017-03-28.
 */
public class Statistics {

    private static ArrayList<MethodStatistics> methodStats = new ArrayList<>();
    private static int exceptionsThrown = 0;

    public static void exceptionThrown(){
        exceptionsThrown++;
    }

    /**
     * Given a contract, will extract all available information and
     * statistics from it.
     * @param c The contract from which to extract statistics
     */
    public static void gatherStatistics(Contract c){
        LinkedList<Behavior> leafs =  c.getLeafs();

        MethodStatistics ms = new MethodStatistics();

        for(Behavior b : leafs){
            ms.addBehavior();
            setPostCons(ms, b);
            setPreCons(ms, b);
        }
    }

    private static void setPostCons(MethodStatistics ms, Behavior b){

    }

    private static void setPreCons(MethodStatistics ms, Behavior b){

    }
}
