package Contract;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.AssertStmt;
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
    private LinkedList<Behavior> behaviors = new LinkedList<Behavior>();

    /**
     * A list of active behaviors. The last element of this list should
     * be the same as currentBehavior
     */

    private MethodDeclaration methodDeclaration;

    private Behavior currentBehavior;
    private final Behavior initialBehavior;

    private boolean pure;

    public Contract(MethodDeclaration md){
        pure = true;
        this.methodDeclaration = md;
        currentBehavior = new Behavior(null);
        initialBehavior = currentBehavior;
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
        for(Behavior b : getLeafs(currentBehavior)){
            b.addPostCon(postCon, isReturn);
        }
    }
    public void addPostAssert(AssertStmt as){
        for (Behavior b : getLeafs(currentBehavior)){
            b.addPostAssert(as);
        }
    }
    public void clearPostAssert(){
        for (Behavior b : getLeafs(currentBehavior)){
            b.getAsserts().clear();
        }
    }

    public void addToAllActive(Expression postCon, boolean isReturn){
        for(Behavior b : behaviors){
            if(!b.isClosed()){
                b.addPostCon(postCon, isReturn);
            }
        }
    }
    public void closeAllActive(){
        for(Behavior b : behaviors){
            b.setClosed(true);
        }
    }

    public void addAssignable(SimpleName assignable){
        for(Behavior b : getLeafs(currentBehavior)){
            b.addAssignable(assignable);
        }
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
    public void setCurrentBehavior(Behavior b){
        this.currentBehavior = b;
    }

    public LinkedList<Behavior> getBehaviors(){
        return behaviors;
    }
    public void addBehavior(Behavior b){
        behaviors.add(b);
    }


    /**
     * Add properties of closed behaviors to current behavior when there is an else.
     *
     * Throws away current behavior
     */
    public void makeMethodNamesGreatAgain(){

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
        splitB.addPreCon(condition); // TODO : This condition should be negated



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
    public LinkedList<Behavior> getLeafs(){
        return getLeafs(initialBehavior);
    }
    private LinkedList<Behavior> getLeafs(Behavior b){
        LinkedList<Behavior> list = new LinkedList<Behavior>();
        if(b.getChildren().isEmpty()){
            list.add(b);
        } else {
            for (Behavior c : b.getChildren()) {
                list.addAll(getLeafs(c));
            }
        }
        return list;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        if(behaviors.isEmpty() == false) {
            sb.append("/*@\n");

            for (Behavior b : getLeafs()) {
                if(!b.isEmpty()) {
                    sb.append(b.toString());
                    if (!behaviors.getLast().equals(b)) {
                        sb.append("also\n");
                    }
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
