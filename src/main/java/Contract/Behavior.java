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
    private boolean isExceptional;

    // List for representing preconditions. The pre-condition "requires !n"
    // should be represented in this list as "!n"
    private LinkedList<String> preCons = new LinkedList<String>();

    // List for represeting postconditions. See above for format.
    private LinkedList<String> postCons = new LinkedList<String>();

    // List for representing any assignable variable
    private LinkedList<String> assignables = new LinkedList<String>();

    // List for representing exceptions thrown when behavior is exceptional
    private LinkedList<String> exceptions = new LinkedList<String>();

    /**
     * Creates a ne behavior. Sets isExcpetional to false by default.
     * Use setExceptional() to change.
     */
    public Behavior(){
        isExceptional = false;
    }

    public void setExceptional(boolean isExceptional){
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

    public void addException(String exception){
        if (isExceptional) {
            exceptions.add(exception);
        } else {
            throw new Error("Behavior is not exceptional");
        }

    }

    public LinkedList<String> getPreCons(){
        return preCons;
    }

    public LinkedList<String> getPostCons(){
        return postCons;
    }

    public LinkedList<String> getAssignables(){
        return assignables;
    }

    public LinkedList<String> getExceptions(){
        return exceptions;
    }

    public boolean getIsExceptional(){
        return isExceptional;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append(createBehaviorHeader());
        sb.append(createPreCons());
        sb.append(createPostCons());
        sb.append(createSignalsOnly());
        sb.append(createSignal());
        sb.append(createAssignable());

        return sb.toString();
    }

    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(getClass() != o.getClass()){
            return false;
        }

        Behavior b = (Behavior) o;

        return isExceptional == b.getIsExceptional()
                && preCons.equals(b.getPreCons())
                && postCons.equals(b.getPostCons())
                && assignables.equals(b.getAssignables())
                && exceptions.equals(b.getExceptions());
    }

    private String createPreCons(){
        StringBuilder sb = new StringBuilder();
        // Write out preconditions
        for(String s : preCons){
            sb.append("requires ");
            sb.append(s);
            sb.append(";\n");
        }

        return sb.toString();
    }

    private String createPostCons(){
        StringBuilder sb = new StringBuilder();

        // Write out postconditions
        for(String s : postCons){
            sb.append("ensures ");
            sb.append(s);
            sb.append(";\n");
        }

        return sb.toString();
    }

    private String createAssignable(){
        StringBuilder sb = new StringBuilder();

        if(assignables.isEmpty() == false) {
            sb.append("assignable ");
            for (String s : assignables) {
                sb.append(s);
                if (assignables.getLast().equals(s)) {
                    // If we're at the last element
                    sb.append(";\n");
                } else {
                    // If we're not at the last element
                    sb.append(", ");
                }
            }

        }

        return sb.toString();
    }

    private String createBehaviorHeader(){
        if(isExceptional){
            return "public exceptional_behavior\n";
        } else {
            return "public normal_behavior\n";
        }
    }

    private String createSignalsOnly(){
        StringBuilder sb = new StringBuilder();

        if(exceptions.isEmpty() == false) {
            sb.append("signals_only ");

            for (String s : exceptions) {
                sb.append(s);
                if (exceptions.getLast().equals(s)) {
                    // If we're at the last element
                    sb.append(";\n");
                } else {
                    // If we're not at the last element
                    sb.append(", ");
                }
            }
        }

        return sb.toString();
    }

    private String createSignal(){
        StringBuilder sb = new StringBuilder();

        for(String s : exceptions){
            sb.append("signal ");
            sb.append(s);
            sb.append(" (");
            sb.append(concatPreCons());
            sb.append(");\n");
        }

        return sb.toString();
    }

    private String concatPreCons(){
        StringBuilder sb = new StringBuilder();

        for(String s : preCons){
            sb.append(s);
            if(!preCons.getLast().equals(s)){
                sb.append(" && ");
            }
        }

        return sb.toString();
    }

}
