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
     */
    public void addBehavior(){
        currentBehavior = new Behavior();
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

    public void setExceptional(boolean isExceptional){
        currentBehavior.setExceptional(isExceptional);
    }

    public Behavior getCurrentBehavior(){
        return currentBehavior;
    }

    public LinkedList<Behavior> getBehaviors(){
        return behaviors;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        if(behaviors.isEmpty() == false) {
            sb.append("/*@\n");

            for (Behavior b : behaviors) {
                sb.append(b.toString());
                if (!behaviors.getLast().equals(b)) {
                    sb.append("also\n");
                }
            }

            sb.append("@*/");
        }

        return sb.toString();
    }

    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(getClass() != o.getClass()){
            return false;
        }

        Contract c = (Contract) o;

        if(behaviors.size() == c.getBehaviors().size()){ // Amount of behaviors must be equal
            for(int i = 0; i < behaviors.size(); i++){
                if(behaviors.get(i) != c.getBehaviors().get(i)){
                    // If any two behaviors do not match, not equal
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;

    }

}
