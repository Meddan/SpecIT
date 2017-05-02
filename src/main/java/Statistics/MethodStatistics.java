package Statistics;

import java.util.ArrayList;

/**
 * Created by dover on 2017-03-28.
 */
public class MethodStatistics {

    private ArrayList<Integer> amountOfpostCons;
    private ArrayList<Integer> amountOfpreCons;
    private ArrayList<Integer> amountOfNullChecks;
    private int behaviors;

    private boolean interesting;
    private String pathToMethod;

    public MethodStatistics(String methodPath){
        amountOfpostCons = new ArrayList<>();
        amountOfpreCons = new ArrayList<>();
        amountOfNullChecks = new ArrayList<>();
        behaviors = -1;
        interesting = false;
        pathToMethod = methodPath;
    }

    public String getPathToMethod(){
        return pathToMethod;
    }

    public void setInteresting(int n){
        if(n > 100) {
            interesting = true;
            System.out.println("INTERESTING");
        }
    }

    public boolean isInteresting(){
        return interesting;
    }

    public void setAmountOfPreCons(int n){
        setInteresting(n);
        amountOfpreCons.add(behaviors, new Integer(n));
    }
    public void setAmountOfNullChecks(int n){
        setInteresting(n);
        amountOfNullChecks.add(behaviors, new Integer(n));
    }

    public ArrayList<Integer> getAmountOfpostCons(){
        return amountOfpostCons;
    }

    public void setAmountOfPostCons(int n){
        setInteresting(n);
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
}
