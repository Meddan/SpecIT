package Contract;

import java.util.LinkedList;

/**
 * Created by dover on 2017-02-08.
 */

/*
    A class for modeling the various behaviours that can be seen in a contract, i.e.
    normal and exceptional behavior.
 */
public class Behavior {

    // Flag for showing whether behavior is exceptional or not
    boolean isExceptional;

    // List for representing preconditions. The pre-condition "requires !n"
    // should be represented in this list as "!n"
    LinkedList<String> preCons = new LinkedList<String>();

    // List for represeting postconditions. See above for format.
    LinkedList<String> postCons = new LinkedList<String>();

    // List for representing any assignable variable
    LinkedList<String> assignables = new LinkedList<String>();

    /**
     * Constructs a new instance of a behavior.
     *
     * @param isExceptional Flag indicating whether behavior is exceptional or not
     */
    public Behavior (boolean isExceptional){
        this.isExceptional = isExceptional;
    }

    public void addPreCon(String preCon){
        preCons.add(preCon);
    }

    public void addPostCon(String postCon){
        postCons.add(postCon);
    }

    public void addAssignable(String assignable){
        assignables.add(assignable);
    }

}
