package Contract;

import Statistics.Statistics;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.type.Type;
import sun.awt.image.ImageWatched;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
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

    private Behavior currentBehavior;
    private final Behavior initialBehavior;
    private Comment oldComment;

    public Contract(){
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


    public Behavior getCurrentBehavior(){
        return currentBehavior;
    }
    public void setCurrentBehavior(Behavior b){
        this.currentBehavior = b;
    }
    public void setOldComment(Comment c){
        this.oldComment = c;
    }

    public LinkedList<Behavior> getLeafs(){
        return getLeafs(initialBehavior);
    }

    private LinkedList<Behavior> getLeafs(Behavior b){
        return b.getLeafs();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String modifier = checkModifiers(initialBehavior.getCallableDeclaration().getModifiers());
        sb.append("@\n");
        sb.append(formatOldComment());
        sb.append("//Generated\n");
        for (Behavior b : getLeafs(initialBehavior)) {
            if (!b.isEmpty()) {
                if(b.getFailing().isPresent()){
                    sb.append("// Failing behavior : " + b.getFailing().get().toString() + "\n");
                }
                sb.append(b.toString());
                sb.append("\nalso\n\n");
            }
        }
        if(sb.lastIndexOf("also\n") != -1) {
            return sb.substring(0, sb.lastIndexOf("also\n")) + modifier + "@";
        } else {
            return sb.append(modifier).append("@").toString();
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

    private String formatOldComment(){
        StringBuilder sb = new StringBuilder();

        sb.append("// Old comment can be seen below\n");

        if(oldComment != null){
            String[] commentLines = oldComment.getContent().split("\\r?\\n");

            for(int i = 0; i < commentLines.length; i++){
                sb.append("//" + commentLines[i] + "\n");
            }
            sb.append("\n");
            return sb.toString();

        } else {
            return "";
        }

    }

    private String checkModifiers(EnumSet<Modifier> modifiers){
        Iterator<Modifier> it = modifiers.iterator();
        while(it.hasNext()){
            Modifier m = it.next();
            if(m == Modifier.PRIVATE || m == Modifier.PROTECTED){
                // The method is private or protected
                return "spec_public\n";
            }
        }

        return "";
    }
    public String extractContract(){
        Statistics.gatherStatistics(this);
        return this.toString();
    }

}
