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

        int totalPostCons = 0;
        int totalPreCons = 0;
        int totalBehaviors = 0;

        for(MethodStatistics ms : methodStats){
            totalBehaviors += ms.getAmountOfBehaviors();

            ArrayList<Integer> postCons = ms.getAmountOfpostCons();
            ArrayList<Integer> preCons = ms.getAmountOfpreCons();

            for(int i = 0; i < ms.getAmountOfBehaviors(); i++){
                Integer post = postCons.get(i);
                Integer pre = preCons.get(i);

                if(post != null){
                    totalPostCons += post.intValue();
                }

                if(pre != null){
                    totalPreCons += pre.intValue();
                }
            }
        }

        sb.append("========= STATS GATHERED =========\n");
        sb.append("Exceptions thrown: " + exceptionsThrown + "\n");
        sb.append("Methods processed: " + methodStats.size() + "\n");
        sb.append("Total behaviors: " + totalBehaviors + "\n");
        sb.append("Total postconditions: " + totalPostCons + "\n");
        sb.append("Total preconditions: " + totalPreCons + "\n");
        sb.append("Average preconditions per method: " + (double) totalPreCons/methodStats.size() + "\n");
        sb.append("Average postconditions per method: " + (double) totalPostCons/methodStats.size() + "\n");
        sb.append("Average behaviors per method: " + (double) totalBehaviors/methodStats.size() + "\n");
        sb.append("Average preconditions per behavior: " + (double) totalPostCons/totalBehaviors + "\n");
        sb.append("Average postconditions per behavior: " + (double) totalPreCons/totalBehaviors + "\n");
        sb.append("==================================\n");

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
