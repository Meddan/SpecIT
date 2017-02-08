package Contract;

import java.util.LinkedList;

/**
 * Created by dover on 2017-02-08.
 */

/*
    A class for modelling contracts. Contains several behaviors, which together will create
    a complete contract.
 */
public class Contract {

    LinkedList<Behavior> behaviors = new LinkedList<Behavior>();

    Behavior currentBehavior;

    /**
     * Add a new behavior to this contract and sets the current behavior to
     * be the newly created one.
     *
     * @param isExceptional Flag indicating whether behavior is exceptional or not
     */
    public void addBehavior(boolean isExceptional){
        currentBehavior = new Behavior(isExceptional);
        behaviors.add(currentBehavior);
    }

    public void addPreCon(String preCon){
        currentBehavior.addPreCon(preCon);
    }

    public void addPostCon(String postCon){
        currentBehavior.addPostCon(postCon);
    }

    public void addAssignable(String assignable){
        currentBehavior.addAssignable(assignable);
    }

    public void addException(String exception){
        currentBehavior.addException(exception);
    }

    public Behavior getCurrentBehavior(){
        return currentBehavior;
    }

    public LinkedList<Behavior> getBehaviors(){
        return behaviors;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("/*@");

        for(Behavior b : behaviors){
            sb.append(b.toString());
            if(!behaviors.getLast().equals(b)){
                sb.append("also\n");
            }
        }

        sb.append("@*/");

        return sb.toString();
    }

}
