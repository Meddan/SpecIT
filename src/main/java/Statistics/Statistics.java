package Statistics;

import Contract.*;
import ContractGeneration.SymbolSolverException;
import ContractGeneration.UncoveredStatementException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by dover on 2017-03-28.
 */
public class Statistics {

    private static ArrayList<MethodStatistics> methodStats = new ArrayList<>();
    private static int successBehaviors = 0;
    private static int failingBehaviors = 0;
    private static int methodFailure = 0;
    private static int otherFailure = 0;
    private static int uncoveredStatement = 0;

    private static DescriptiveStatistics prePerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics postPerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics prePerBehavior = new DescriptiveStatistics();
    private static DescriptiveStatistics postPerBehavior = new DescriptiveStatistics();
    private static DescriptiveStatistics behPerMethod = new DescriptiveStatistics();


    private static void successfulBehavior(){
        successBehaviors++;
    }
    private static void failingBehavior(){
        failingBehaviors++;
    }
    private static void methodFail(){
        methodFailure++;
    }
    private static void otherFail() {
        otherFailure++;
    }
    private static void uncoveredStatementFail() {
        uncoveredStatement++;
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
            if(!b.isFailing()) {
                successfulBehavior();
                ms.addBehavior();
                setPostCons(ms, b);
                setPreCons(ms, b);
            } else {
                failingBehavior();
                if(b.getFailing().get() instanceof SymbolSolverException){
                    if(((SymbolSolverException) b.getFailing().get()).message.equals("Method call")){
                        methodFail();
                    } else {
                        System.out.println(b.getFailing().get());
                        otherFail();
                    }
                } else if (b.getFailing().get() instanceof UncoveredStatementException){
                    uncoveredStatementFail();
                } else {
                    System.out.println(b.getFailing().get());
                }
            }
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
            behPerMethod.addValue(ms.getAmountOfBehaviors());

            ArrayList<Integer> postCons = ms.getAmountOfpostCons();
            ArrayList<Integer> preCons = ms.getAmountOfpreCons();

            int methPostCons = 0;
            int methPreCons = 0;
            for(int i = 0; i < ms.getAmountOfBehaviors(); i++){
                Integer post = postCons.get(i);
                Integer pre = preCons.get(i);

                if(post != null){
                    methPostCons += post;
                    postPerBehavior.addValue(post.doubleValue());
                }

                if(pre != null){
                    methPreCons += pre;
                    prePerBehavior.addValue(pre.doubleValue());
                }
            }

            totalPostCons += methPostCons;
            totalPreCons += methPreCons;
            prePerMethod.addValue(methPreCons);
            postPerMethod.addValue(methPostCons);
        }

        sb.append("========= STATS GATHERED =========\n");
        sb.append("Methods processed: " + methodStats.size() + "\n");
        sb.append("Total behaviors: " + (successBehaviors + failingBehaviors) + "\n");
        sb.append("Successful behaviors: " + successBehaviors + "\n");
        sb.append("Failing behaviors: " + failingBehaviors + "\n");
        sb.append("\tSymbol solver Failures: " + (methodFailure + otherFailure) + "\n");
        sb.append("\t\tMethod Failures: " + methodFailure + "\n");
        sb.append("\t\tOther Failures: " + otherFailure + "\n");
        sb.append("\tUncovered Statement Failures: " + uncoveredStatement + "\n");
        sb.append("Total postconditions: " + totalPostCons + "\n");
        sb.append("Total preconditions: " + totalPreCons + "\n");
        sb.append(String.format("Preconditions per method: \n%s \n", formatStats(prePerMethod)));
        sb.append(String.format("Postconditions per method: \n%s \n", formatStats(postPerMethod)));
        sb.append(String.format("Behaviors per method: \n%s \n", formatStats(behPerMethod)));
        sb.append(String.format("Preconditions per behavior: \n%s \n", formatStats(prePerBehavior)));
        sb.append(String.format("Postconditions per behavior: \n%s \n", formatStats(postPerBehavior)));
        sb.append("==================================\n");

        return sb.toString();
    }

    private static String formatStats(DescriptiveStatistics ds){
        return String.format("Mean: %.3f Min: %.3f Median: %.3f Max: %.3f Standard Deviation: %.3f",
                ds.getMean(), ds.getMin(), ds.getPercentile(50), ds.getMax(), ds.getStandardDeviation());
    }

    private static void setPostCons(MethodStatistics ms, Behavior b){
        int amountOfPostCons = 0;

        amountOfPostCons += b.getPostCons().size();

        HashMap<Variable, VariableValue> hm = b.getAssignedValues();
        if(!b.getIsExceptional()) {
            for (Variable v : hm.keySet()){
                if(b.getAssignedValue(v) != null){
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
