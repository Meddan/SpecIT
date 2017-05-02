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

    private String pathToMethod;

    public MethodStatistics(String methodPath){
        amountOfpostCons = new ArrayList<>();
        amountOfpreCons = new ArrayList<>();
        amountOfNullChecks = new ArrayList<>();
        behaviors = -1;

        pathToMethod = methodPath;
    }

    public void setAmountOfPreCons(int n){
        amountOfpreCons.add(behaviors, new Integer(n));
    }
    public void setAmountOfNullChecks(int n){
        amountOfNullChecks.add(behaviors, new Integer(n));
    }

    public ArrayList<Integer> getAmountOfpostCons(){
        return amountOfpostCons;
    }

    public void setAmountOfPostCons(int n){
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
