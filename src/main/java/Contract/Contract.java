package Contract;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.type.Type;
import sun.awt.image.ImageWatched;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by dover on 2017-02-08.
 */

/*
    A class for modelling contracts. Contains several behaviors, which together will create
    a complete contract.
 */
public class Contract {

    /**
     * A list of active behaviors. The last element of this list should
     * be the same as currentBehavior
     */

    private MethodDeclaration methodDeclaration;

    private Behavior currentBehavior;
    private final Behavior initialBehavior;

    public Contract(MethodDeclaration md){
        this.methodDeclaration = md;
        currentBehavior = new Behavior(null);
        initialBehavior = currentBehavior;
    }

    public boolean isPure() {
        boolean pure = true;
        for (Behavior b : getLeafs()){
            pure &= b.isPure();
        }
        return pure;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public Behavior getCurrentBehavior(){
        return currentBehavior;
    }
    public void setCurrentBehavior(Behavior b){
        this.currentBehavior = b;
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("/*@\n");
        for (Behavior b : getLeafs(initialBehavior)) {
            if (!b.isEmpty()) {
                sb.append(b.toString());
                sb.append("\nalso\n\n");
            }
        }
        if(sb.lastIndexOf("also\n") != -1) {
            return sb.substring(0, sb.lastIndexOf("also\n")) + "@*/";
        } else {
            return sb.append("@*/").toString();
        }
    }

    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(getClass() != o.getClass()){
            return false;
        }

        Contract c = (Contract) o;

        return initialBehavior.equals(c.initialBehavior) && currentBehavior.equals(c.getCurrentBehavior());

    }

}
