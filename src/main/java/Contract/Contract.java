package Contract;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;
import sun.awt.image.ImageWatched;

import java.util.LinkedList;

/**
 * Created by dover on 2017-02-08.
 */

/*
    A class for modelling contracts. Contains several behaviors, which together will create
    a complete contract.
 */
public class Contract {

    private LinkedList<Behavior> closedBehaviors = new LinkedList<Behavior>();

    private LinkedList<Behavior> behaviors = new LinkedList<Behavior>();

    /**
     * A list of active behaviors. The last element of this list should
     * be the same as currentBehavior
     */
    private LinkedList<Behavior> activeBehaviors = new LinkedList<Behavior>();

    private MethodDeclaration methodDeclaration;

    private Behavior currentBehavior;

    private boolean pure;

    public Contract(MethodDeclaration md){
        pure = true;
        this.methodDeclaration = md;
        currentBehavior = new Behavior(null);
        behaviors.add(currentBehavior);
    }

    public boolean isPure() {
        return pure;
    }

    public void setPure(boolean pure) {
        this.pure = this.pure && pure;
    }
    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public void addPreCon(Expression preCon){
        currentBehavior.addPreCon(preCon);
    }

    public void addPreCon(LinkedList<Expression> preCon){
        currentBehavior.addPreCon(preCon);
    }

    public void addPostCon(Expression postCon, boolean isReturn){
        currentBehavior.addPostCon(postCon, isReturn);
    }

    public void addAssignable(SimpleName assignable){
        currentBehavior.addAssignable(assignable);
    }

    public void addAssignable(LinkedList<SimpleName> assignable){
        currentBehavior.addAssignable(assignable);
    }

    public void addException(Type t, Expression e){
        currentBehavior.addException(t, e);
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

    /**
     * Closes the currently active behavior.
     *
     * The next active behavior will be the that is next in the list of active
     * behaviors.
     * @return the newly active behavior
     */
    public Behavior closeBehavior(){
        if(activeBehaviors.getLast().equals(currentBehavior)){
            activeBehaviors.remove(activeBehaviors.getLast());
            closedBehaviors.add(currentBehavior);
            currentBehavior = activeBehaviors.getLast();
            return currentBehavior;
        } else {
            throw new Error("Mismatch between activeBehaviors and currentBehavior");
        }
    }

    public void splitBehavior(Expression condition){
        // Create the two behavior that will be the split
        Behavior splitA = new Behavior(currentBehavior);
        Behavior splitB = new Behavior(currentBehavior);

        // Set new behaviors as children
        currentBehavior.addChild(splitA);
        currentBehavior.addChild(splitB);

        // Add condition as pre-condition
        splitA.addPreCon(condition);
        splitB.addPreCon(condition);

        // Add to active behaviors
        activeBehaviors.add(splitB);
        activeBehaviors.add(splitA);

        // Add to list of all behaviors
        behaviors.add(splitA);
        behaviors.add(splitB);

        // Extract contract-info from parents
        extractParentalBehavior(splitA);
        extractParentalBehavior(splitB);

        // Set splitA as currentBehavior
        currentBehavior = splitA;
    }

    private void extractParentalBehavior(Behavior b){
        Behavior p = b.getParent();

        while(p != null){
            b.addPreConFromParent(p.getPreCons());
            b.addPostConFromParent(p.getPostCons());
            b.addAssignable(p.getAssignables());
            b.addExceptionsFromParent(p.getExceptions());
            b.setExceptional(p.getIsExceptional());
            p = p.getParent();
        }
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
