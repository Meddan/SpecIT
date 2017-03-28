package Statistics;

import java.util.ArrayList;

/**
 * Created by dover on 2017-03-28.
 */
public class MethodStatistics {

    private ArrayList<Integer> amountOfpostCons;
    private ArrayList<Integer> amountOfpreCons;

    private int behaviors;

    public MethodStatistics(){
        amountOfpostCons = new ArrayList<>();
        amountOfpreCons = new ArrayList<>();
        behaviors = -1;
    }

    public void setAmountOfPreCons(int n){
        amountOfpreCons.add(behaviors, new Integer(n));
    }

    public void setAmountOfPostCons(int n){
        amountOfpostCons.add(behaviors, new Integer(n));
    }

    public void addBehavior(){
        behaviors++;
    }
}
