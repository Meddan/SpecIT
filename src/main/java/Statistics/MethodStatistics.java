package Statistics;

import java.util.ArrayList;

/**
 * Created by dover on 2017-03-28.
 */
public class MethodStatistics {

    private ArrayList<Integer> amountOfpostCons;
    private ArrayList<Integer> amountOfpreCons;
    private ArrayList<Integer> amountOfNullChecks;
    private ArrayList<Boolean> isFailing;
    private int behaviors;

    private int totalPostCons;
    private int totalPreCons;
    private int totalNullChecks;

    private boolean interesting;
    private String pathToMethod;

    public MethodStatistics(String methodPath){
        amountOfpostCons = new ArrayList<>();
        amountOfpreCons = new ArrayList<>();
        amountOfNullChecks = new ArrayList<>();
        isFailing = new ArrayList<>();
        behaviors = -1;
        interesting = false;
        pathToMethod = methodPath;
        totalPreCons = 0;
        totalPostCons = 0;
        totalNullChecks = 0;
    }

    public String getPathToMethod(){
        return pathToMethod;
    }

    public void setInteresting(int n){
        if(n > 50) {
            interesting = true;
        }
    }

    public boolean isInteresting(){
        return interesting;
    }

    public void setAmountOfPreCons(int n){
        setInteresting(n);
        totalPreCons += n;
        amountOfpreCons.add(behaviors, new Integer(n));
    }
    public void setAmountOfNullChecks(int n){
        setInteresting(n);
        totalNullChecks += n;
        amountOfNullChecks.add(behaviors, new Integer(n));
    }

    public void setIsFailing(boolean b){
        isFailing.add(behaviors, b);        
    }

    public ArrayList<Integer> getAmountOfpostCons(){
        return amountOfpostCons;
    }

    public void setAmountOfPostCons(int n){
        setInteresting(n);
        totalPostCons += n;
        amountOfpostCons.add(behaviors, new Integer(n));
    }

    public ArrayList<Integer> getAmountOfpreCons(){
        return amountOfpreCons;
    }
    public ArrayList<Integer> getAmountOfNullChecks(){
        return amountOfNullChecks;
    }

    public void addBehavior(){
        behaviors++;
    }

    public int getAmountOfBehaviors(){
        return behaviors + 1;
    }

    public int getTotalPostCons() {
        return totalPostCons;
    }

    public int getTotalPreCons() {
        return totalPreCons;
    }

    public int getTotalNullChecks() {
        return totalNullChecks;
    }
}
