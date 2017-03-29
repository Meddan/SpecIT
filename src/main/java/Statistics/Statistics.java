package Statistics;

import Contract.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;

import java.util.ArrayList;
import java.util.HashMap;
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

        methodStats.add(ms);
    }

    /**
     * Calculates all interesting and relevant stats and presents them
     * in a nice and easy to read way (lul).
     */
    public static String getStatistics(){
        StringBuilder sb = new StringBuilder();
        sb.append("Exceptions thrown: " + exceptionsThrown + "\n");
        sb.append("Methods processed: " + methodStats.size() + "\n");

        return sb.toString();
    }

    private static void setPostCons(MethodStatistics ms, Behavior b){
        int amountOfPostCons = 0;

        amountOfPostCons += b.getPostCons().size();

        HashMap<SimpleName, Expression> hm = b.getAssignedValues();
        if(!b.getIsExceptional()) {
            for (SimpleName sm : hm.keySet()){
                if(hm.get(sm) != null){
                    amountOfPostCons++;
                }
            }
        }

        ms.setAmountOfPostCons(amountOfPostCons);
    }

    private static void setPreCons(MethodStatistics ms, Behavior b){
        ms.setAmountOfPreCons(b.getPreCons().size());
    }
}
