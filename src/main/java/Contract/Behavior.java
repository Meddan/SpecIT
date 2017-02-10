package Contract;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import sun.awt.image.ImageWatched;

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
    private LinkedList<PreCondtion> preCons = new LinkedList<PreCondtion>();

    // List for represeting postconditions. See above for format.
    private LinkedList<PostCondition> postCons = new LinkedList<PostCondition>();

    // List for representing any assignable variable
    private LinkedList<SimpleName> assignables = new LinkedList<SimpleName>();

    // List for representing exceptions thrown when behavior is exceptional
    private LinkedList<ExceptionCondition> exceptions = new LinkedList<ExceptionCondition>();

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

    public void addPreCon(Expression preCon){
        preCons.add(new PreCondtion(preCon));
    }

    public void addPreCon(LinkedList<Expression> preCon){
        for(Expression e : preCon){
            addPreCon(e);
        }
    }

    public void addPostCon(Expression postCon, boolean isReturn){
        postCons.add(new PostCondition(postCon, isReturn));
    }

    public void addAssignable(SimpleName assignable){
        assignables.add(assignable);
    }

    public void addAssignable(LinkedList<SimpleName> assignable){
        for(SimpleName s : assignable){
            addAssignable(s);
        }
    }

    public void addException(Type t, Expression e){
        exceptions.add(new ExceptionCondition(t,e));
    }

    public LinkedList<PreCondtion> getPreCons(){
        return preCons;
    }

    public LinkedList<PostCondition> getPostCons(){
        return postCons;
    }

    public LinkedList<SimpleName> getAssignables(){
        return assignables;
    }

    public LinkedList<ExceptionCondition> getExceptions(){
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
        for(PreCondtion p : preCons){
            sb.append(p.toString());
        }

        return sb.toString();
    }

    private String createPostCons(){
        StringBuilder sb = new StringBuilder();

        // Write out postconditions
        for(PostCondition p : postCons){
            sb.append(p.toString());
        }

        return sb.toString();
    }

    private String createAssignable(){
        StringBuilder sb = new StringBuilder();

        if(!assignables.isEmpty()) {
            sb.append("assignable ");
            for (SimpleName s : assignables) {
                sb.append(s.toString());
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

            for (ExceptionCondition ec : exceptions) {
                sb.append(ec.toString());
                if (exceptions.getLast().equals(ec)) {
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

        for(ExceptionCondition ec : exceptions){
            sb.append("signal ");
            sb.append(ec.getName().toString());
            sb.append(" (");
            sb.append(concatPreCons());
            sb.append(");\n");
        }

        return sb.toString();
    }

    private String concatPreCons(){
        StringBuilder sb = new StringBuilder();

        for(PreCondtion p : preCons){
            sb.append(p.toString());
            if(!preCons.getLast().equals(p)){
                sb.append(" && ");
            }
        }

        return sb.toString();
    }

}
