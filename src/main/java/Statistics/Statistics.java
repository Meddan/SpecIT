package Statistics;

import Contract.*;
import ContractGeneration.SymbolSolverException;
import ContractGeneration.UncoveredStatementException;
import ContractGeneration.UnresolvedParameterException;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import sun.security.krb5.internal.crypto.Des;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Created by dover on 2017-03-28.
 */
public class Statistics {

    public static String projectName;

    private static ArrayList<MethodStatistics> methodStats = new ArrayList<>();
    private static int successBehaviors = 0;
    private static int failingBehaviors = 0;
    private static int methodFailure = 0;
    private static int otherFailure = 0;
    private static int uncoveredStatement = 0;
    private static int unresolvedParameter = 0;

    private static int totalNullChecks = 0;

    private static DescriptiveStatistics prePerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics postPerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics prePerBehavior = new DescriptiveStatistics();
    private static DescriptiveStatistics postPerBehavior = new DescriptiveStatistics();
    private static DescriptiveStatistics behPerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics nullPerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics nullPerBehavior = new DescriptiveStatistics();


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
    private static void unresolvedParameterFail(){
        unresolvedParameter++;
    }

    /**
     * Given a contract, will extract all available information and
     * statistics from it.
     * @param c The contract from which to extract statistics
     */
    public static void gatherStatistics(Contract c){
        LinkedList<Behavior> leafs =  c.getLeafs();

        MethodStatistics ms = new MethodStatistics(c.getPathToMethod());

        for(Behavior b : leafs){
            totalNullChecks += b.getNullChecks().size();
            if(!b.isFailing()) {
                successfulBehavior();
                ms.addBehavior();
                setPostCons(ms, b);
                setPreCons(ms, b);
                setNullChecks(ms, b);
            } else {
                failingBehavior();
                if(b.getFailing().get() instanceof SymbolSolverException){
                    if(((SymbolSolverException) b.getFailing().get()).message.equals("Method call")){
                        methodFail();
                    }
                } else if (b.getFailing().get() instanceof UncoveredStatementException){
                    uncoveredStatementFail();
                } else if (b.getFailing().get() instanceof UnresolvedParameterException){
                    unresolvedParameterFail();
                } else {
                    System.out.println(b.getFailing().get());
                    otherFail();
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
        ArrayList<String> interestingMethods = new ArrayList<>();

        int totalPostCons = 0;
        int totalPreCons = 0;
        int totalNullCons = 0;
        int totalBehaviors = 0;

        for(MethodStatistics ms : methodStats){

            totalBehaviors += ms.getAmountOfBehaviors();
            behPerMethod.addValue(ms.getAmountOfBehaviors());

            ArrayList<Integer> postCons = ms.getAmountOfpostCons();
            ArrayList<Integer> preCons = ms.getAmountOfpreCons();
            ArrayList<Integer> nullChecks = ms.getAmountOfNullChecks();

            int methPostCons = 0;
            int methPreCons = 0;
            int methNullChecks = 0;
            for(int i = 0; i < ms.getAmountOfBehaviors(); i++){
                Integer post = postCons.get(i);
                Integer pre = preCons.get(i);
                Integer nul = nullChecks.get(i);

                if(post != null){
                    methPostCons += post;
                    postPerBehavior.addValue(post.doubleValue());
                }

                if(pre != null){
                    methPreCons += pre;
                    prePerBehavior.addValue(pre.doubleValue());
                }
                if(nul != null){
                    methNullChecks += nul;
                    prePerBehavior.addValue(nul.doubleValue());
                    nullPerBehavior.addValue(nul.doubleValue());
                }
            }

            ms.setInteresting(methPostCons);
            System.out.println(methPostCons);
            ms.setInteresting(methPreCons + methNullChecks);
            ms.setInteresting(methNullChecks);

            totalPostCons += methPostCons;
            totalPreCons += methPreCons;
            totalPreCons += methNullChecks;
            totalNullCons += methNullChecks;

            prePerMethod.addValue(methPreCons);
            postPerMethod.addValue(methPostCons);
            prePerMethod.addValue(methNullChecks);
            nullPerMethod.addValue(methNullChecks);

            if(ms.isInteresting()){
                interestingMethods.add(ms.getPathToMethod());
            }
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
        sb.append("\tUnresolved Parameter Failures: " + unresolvedParameter + "\n");
        sb.append("Total postconditions: " + totalPostCons + "\n");
        sb.append("Total preconditions: " + totalPreCons + "\n");
        sb.append("\t Null checks: " + totalNullCons + "\n");
        sb.append("Total amount of Null checks: " + totalNullChecks + "\n");
        sb.append(String.format("Preconditions per method: \n%s \n", formatStats(prePerMethod)));
        sb.append(String.format("Postconditions per method: \n%s \n", formatStats(postPerMethod)));
        sb.append(String.format("Null checks per method \n%s \n", formatStats(nullPerMethod)));
        sb.append(String.format("Behaviors per method: \n%s \n", formatStats(behPerMethod)));
        sb.append(String.format("Preconditions per behavior: \n%s \n", formatStats(prePerBehavior)));
        sb.append(String.format("Postconditions per behavior: \n%s \n", formatStats(postPerBehavior)));
        sb.append(String.format("Null checks per behavior: \n%s \n", formatStats(nullPerBehavior)));
        sb.append("==================================\n");

        StringBuilder interestingMethodNames = new StringBuilder();

        interestingMethodNames.append("\n\n===========INTERESTING METHODS===========\n");
        interestingMethodNames.append("======== " + interestingMethods.size() + " found ========\n\n");
        for(int i = 0; i < interestingMethods.size(); i++){
            interestingMethodNames.append(interestingMethods.get(i) + "\n");
        }

        writeStatsToFile(sb.toString(), interestingMethodNames.toString());

        return sb.toString();
    }

    private static String formatStats(DescriptiveStatistics ds){
        return String.format("Mean: %.3f Min: %.3f Median: %.3f Max: %.3f Standard Deviation: %.3f",
                ds.getMean(), ds.getMin(), ds.getPercentile(50), ds.getMax(), ds.getStandardDeviation());
    }

    private static void writeStatsToFile(String stats, String interestingMethods){
        Path p = Paths.get("Statistics/" + projectName);

        for(int i = 1; i < p.getNameCount(); i++){
            Path currentPath = p.subpath(0,i);

            if(!Files.exists(currentPath)){
                try{
                    Files.createDirectories(currentPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            if(Files.exists(p)){
                Files.write(p, Arrays.asList("\n\n ####### NEW RUN ####### \n\n" + stats  + interestingMethods), Charset.forName("UTF-8"), StandardOpenOption.APPEND);
            } else {
                Files.write(p, Arrays.asList(stats + interestingMethods), Charset.forName("UTF-8"));
            }
        } catch (IOException e){
            e.printStackTrace();
        }
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
    private static void setNullChecks(MethodStatistics ms, Behavior b){
        ms.setAmountOfNullChecks(b.getNullChecks().size());
    }

    public static void setProjectName(String s){
        projectName = s;
    }
}
