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

    private static DescriptiveStatistics failPrePerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics failPostPerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics failPrePerBehavior = new DescriptiveStatistics();
    private static DescriptiveStatistics failPostPerBehavior = new DescriptiveStatistics();
    private static DescriptiveStatistics failBehPerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics failNullPerMethod = new DescriptiveStatistics();
    private static DescriptiveStatistics failNullPerBehavior = new DescriptiveStatistics();


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
            ms.addBehavior();
            ms.setIsFailing(b.isFailing());
            setPostCons(ms, b);
            setPreCons(ms, b);
            setNullChecks(ms, b);
            if(!b.isFailing()) {
                successfulBehavior();
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
        ArrayList<MethodStatistics> interestingMethods = new ArrayList<>();

        int totalPostCons = 0;
        int totalPreCons = 0;
        int totalNullCons = 0;
        int totalBehaviors = 0;

        for(MethodStatistics ms : methodStats){

            boolean isFailing = false;
            boolean isSuccessful = false;

            int successfulBehaviors = 0;
            int failingBehaviors = 0;

            totalBehaviors += ms.getAmountOfBehaviors();

            ArrayList<Integer> postCons = ms.getAmountOfpostCons();
            ArrayList<Integer> preCons = ms.getAmountOfpreCons();
            ArrayList<Integer> nullChecks = ms.getAmountOfNullChecks();
            ArrayList<Boolean> failings = ms.getIsFailing();

            int methPostCons = 0;
            int methPreCons = 0;
            int methNullChecks = 0;
            int failMethPostCons = 0;
            int failMethPreCons = 0;
            int failMethNullChecks = 0;

            for(int i = 0; i < ms.getAmountOfBehaviors(); i++){
                Integer post = postCons.get(i);
                Integer pre = preCons.get(i);
                Integer nul = nullChecks.get(i);

                if(failings.get(i)){
                    isFailing = true;
                    failingBehaviors++;
                } else {
                    isSuccessful = true;
                    successfulBehaviors++;
                }

                if(post != null){
                    if(!failings.get(i)) {
                        methPostCons += post;
                        postPerBehavior.addValue(post.doubleValue());
                    } else {
                        failMethPostCons += post;
                        failPostPerBehavior.addValue(post);
                    }
                }

                if(pre != null){
                    if(!failings.get(i)) {
                        methPreCons += pre;
                        prePerBehavior.addValue(pre.doubleValue());
                    } else {
                        failMethPreCons += pre;
                        failPrePerBehavior.addValue(pre);
                    }
                }
                if(nul != null){
                    if(!failings.get(i)) {
                        methNullChecks += nul;
                        prePerBehavior.addValue(nul.doubleValue());
                        nullPerBehavior.addValue(nul.doubleValue());
                    } else {
                        failMethNullChecks += nul;
                        failPrePerBehavior.addValue(nul);
                        failNullPerBehavior.addValue(nul);
                    }
                }
            }

            ms.setInteresting(methPostCons);
            ms.setInteresting(methPreCons + methNullChecks);
            ms.setInteresting(methNullChecks);

            totalPostCons += methPostCons;
            totalPreCons += methPreCons;
            totalPreCons += methNullChecks;
            totalNullCons += methNullChecks;

            if(isSuccessful) {
                behPerMethod.addValue(successfulBehaviors);
                prePerMethod.addValue(methPreCons + methNullChecks);
                postPerMethod.addValue(methPostCons);
                nullPerMethod.addValue(methNullChecks);
            }

            if(isFailing){
                failBehPerMethod.addValue(failingBehaviors);
                failPrePerMethod.addValue(failMethPreCons + failMethNullChecks);
                failPostPerMethod.addValue(failMethPostCons);
                failNullPerMethod.addValue(failMethNullChecks);
            }

            if(ms.isInteresting()){
                interestingMethods.add(ms);
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

        interestingMethods.sort(Comparator.comparingInt(o -> ( (- o.getTotalNullChecks()) - o.getTotalPreCons() - o.getTotalPostCons())));

        StringBuilder interestingMethodNames = new StringBuilder();

        interestingMethodNames.append("\n\n===========INTERESTING METHODS===========\n");
        interestingMethodNames.append("======== " + interestingMethods.size() + " found ========\n\n");
        for(int i = 0; i < interestingMethods.size(); i++){
            MethodStatistics temp = interestingMethods.get(i);

            interestingMethodNames.append(temp.getPathToMethod() + " " +
                    "\n\t Total PostCons " + temp.getTotalPostCons() +
                    "\n\t Total PreCons " + temp.getTotalPreCons() +
                    "\n\t Total NullChecks " + temp.getTotalNullChecks() + "\n");
        }

        writeStatsToFile(sb.toString(), createTabular(), interestingMethodNames.toString());

        return sb.toString();
    }

    private static String formatStats(DescriptiveStatistics ds){
        return String.format("Mean: %.3f Min: %.3f Median: %.3f Max: %.3f Standard Deviation: %.3f",
                ds.getMean(), ds.getMin(), ds.getPercentile(50), ds.getMax(), ds.getStandardDeviation());
    }

    private static String createTabular(){
        StringBuilder sb = new StringBuilder();

        sb.append("\\begin{tabular}{| l | l | l | l | l | l |} \n");
        sb.append("\\hline \n");
        sb.append("\\multicolumn{6}{|c|}{" + projectName + " [SUCCESSFUL]} \\\\ \n");
        sb.append("\\hline \n");
        sb.append("Statistic measured & Min & Mean & Median & Max & $\\sigma$ \\\\ \\hline \n");
        sb.append("Preconditions per method " + formatTabular(prePerMethod) + "\n");
        sb.append("Postconditions per method " + formatTabular(postPerMethod) + "\n");
        sb.append("Null checks per method " + formatTabular(nullPerMethod) + "\n");
        sb.append("Behaviors per method " + formatTabular(behPerMethod) + "\n");
        sb.append("Preconditions per behavior " + formatTabular(prePerBehavior) + "\n");
        sb.append("Postconditions per behavior " + formatTabular(postPerBehavior) + "\n");
        sb.append("Null checks per method " + formatTabular(nullPerBehavior) + "\n");
        sb.append("\\end{tabular}\n");

        sb.append("\\begin{tabular}{| l | l | l | l | l | l |} \n");
        sb.append("\\hline \n");
        sb.append("\\multicolumn{6}{|c|}{" + projectName + " [FAILING]} \\\\ \n");
        sb.append("\\hline \n");
        sb.append("Statistic measured & Min & Mean & Median & Max & $\\sigma$ \\\\ \\hline \n");
        sb.append("Preconditions per method " + formatTabular(failPrePerMethod) + "\n");
        sb.append("Postconditions per method " + formatTabular(failPostPerMethod) + "\n");
        sb.append("Null checks per method " + formatTabular(failNullPerMethod) + "\n");
        sb.append("Behaviors per method " + formatTabular(failBehPerMethod) + "\n");
        sb.append("Preconditions per behavior " + formatTabular(failPostPerBehavior) + "\n");
        sb.append("Postconditions per behavior " + formatTabular(failPostPerBehavior) + "\n");
        sb.append("Null checks per method " + formatTabular(failNullPerBehavior) + "\n");
        sb.append("\\end{tabular}");

        return sb.toString();

    }

    private static String formatTabular(DescriptiveStatistics ds){
        return String.format("& %.3f & %.3f & %.3f & %.3f & %.3f \\\\ \\hline",
                ds.getMin(), ds.getMean(), ds.getPercentile(50), ds.getMax(), ds.getStandardDeviation());

    }

    private static void writeStatsToFile(String stats, String tabular, String interestingMethods){
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
                Files.write(p, Arrays.asList("\n\n ####### NEW RUN ####### \n\n" + stats + tabular + interestingMethods), Charset.forName("UTF-8"), StandardOpenOption.APPEND);
            } else {
                Files.write(p, Arrays.asList(stats + tabular + interestingMethods), Charset.forName("UTF-8"));
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
                if(b.getAssignedValue(v) != null && b.getAssignedValue(v).getStatus() == VariableValue.Status.known){
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
