package Contract;

import ContractGeneration.Resources;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.type.Type;

import java.util.*;

/**
 * Created by dover on 2017-02-08.
 */

/*
    A class for modeling the various behaviours that can be seen in a contract, i.e.
    normal and exceptional behavior.
 */
public class Behavior {

    // The level this node has in the behavior tree
    private int level;

    // Flag for showing whether behavior is exceptional or not
    private boolean isExceptional;

    // List for representing preconditions. The pre-condition "requires !n"
    // should be represented in this list as "!n"
    private LinkedList<PreCondition> preCons = new LinkedList<PreCondition>();

    // List for represeting postconditions. See above for format.
    private LinkedList<PostCondition> postCons = new LinkedList<PostCondition>();

    // List for representing exceptions thrown when behavior is exceptional
    private LinkedList<ExceptionCondition> exceptions = new LinkedList<ExceptionCondition>();

    private LinkedList<AssertStmt> asserts = new LinkedList<AssertStmt>();

    private Behavior parent;
    private boolean closed = false;
    private boolean diverges = false;

    public boolean isPure() {
        return pure;
    }

    public void setPure(boolean pure) {
        this.pure = this.pure && pure;
    }

    private boolean pure = true;

    private boolean impureMethods = false;

    private LinkedList<Behavior> children = new LinkedList<Behavior>();

    private HashMap<Variable, VariableValue> assignedValues = new HashMap<>();

    public CallableDeclaration getCallableDeclaration() {
        return callableDeclaration;
    }

    public void setCallableDeclaration(CallableDeclaration callableDeclaration) {
        this.callableDeclaration = callableDeclaration;
    }

    private CallableDeclaration callableDeclaration;
    private Optional<Exception> failing;
    public boolean isLocalVar(SimpleName name){
        String s = name.toString();
        for(Variable v : assignedValues.keySet()){
            if(v.getName().equals(s) && v.getScope()==Variable.Scope.local){
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new behavior. Sets isExcpetional to false by default.
     * Use setExceptional() to change.
     */
    public Behavior(Behavior parent){
        isExceptional = false;
        this.parent = parent;
        if(parent != null){
            this.level = parent.getLevel()+1;
            extractInformation(parent);
        } else {
            this.failing = Optional.empty();
            this.level = 0;
        }
    }

    /**
     * Extract all info from given behavior except info regarding
     * the tree (such as level, children etc)
     * @param original the behavior from which to extract
     */
    private void extractInformation(Behavior original){
        this.preCons = (LinkedList<PreCondition>) original.getPreCons().clone();
        this.postCons = (LinkedList<PostCondition>) original.getPostCons().clone();
        this.exceptions = (LinkedList<ExceptionCondition>) original.getExceptions().clone();
        this.asserts = (LinkedList<AssertStmt>) original.getAsserts().clone();
        this.isExceptional = original.getIsExceptional();
        this.assignedValues = (HashMap<Variable, VariableValue>) original.assignedValues.clone();
        this.callableDeclaration = original.getCallableDeclaration();
        this.pure = original.isPure();
        this.impureMethods = original.impureMethods;
        this.failing = original.failing;
    }

    public void setExceptional(boolean isExceptional){
        if(!this.closed) {
            this.isExceptional = isExceptional;
        }
        for(Behavior b : children){
            b.setExceptional(isExceptional);
        }

    }
    public HashMap<Variable, VariableValue> getAssignedValues() {
        return assignedValues;
    }
    public void addPreCon(Expression preCon){
        PreCondition toAdd = new PreCondition(preCon);

        for(PreCondition pc : preCons){
            // If we have already added this condition
            if(pc.equals(toAdd)){
                return; // End method
            }
        }

        for(Behavior b : children){
            b.addPreCon(preCon);
        }
        if(!this.closed) {
            preCons.add(toAdd);
        }
    }

    public void addPostCon(Expression postCon, boolean isReturn){
        PostCondition toAdd = new PostCondition(postCon,isReturn);

        for(PostCondition pc : postCons){
            // If we have already added this condition
            if(pc.equals(toAdd)){
                return; // End method
            }
        }

        for(Behavior b : children){
            b.addPostCon(postCon, isReturn);
        }
        if(!closed) {
            postCons.add(toAdd);
        }
    }

    public void addException(Type t, Expression e){
        ExceptionCondition toAdd = new ExceptionCondition(t,e);

        for(ExceptionCondition ec : exceptions){
            // If we have already added this exception
            if(ec.equals(toAdd)){
                return; // End method
            }
        }

        for(Behavior b : children){
            b.addException(t, e);
        }
        if(!closed) {
            exceptions.add(toAdd);
        }
    }

    public void addException(Type t, LinkedList<Expression> e){
        ExceptionCondition toAdd = new ExceptionCondition(t,e);

        for(ExceptionCondition ec : exceptions){
            // If we have already added this exception
            if(ec.equals(toAdd)){
                return; // End method
            }
        }

        if(!closed){
            exceptions.add(toAdd);
        }
        for(Behavior b : children){
            b.addException(t, e);
        }
    }

    public LinkedList<PreCondition> getPreCons(){
        return preCons;
    }

    public LinkedList<PostCondition> getPostCons(){
        return postCons;
    }

    public Set<Variable> getAssignables(){
        return assignedValues.keySet();
    }

    public LinkedList<ExceptionCondition> getExceptions(){
        return exceptions;
    }

    public boolean getIsExceptional(){
        return isExceptional;
    }

    public LinkedList<PostCondition> complementPostConAgainstParent(){
        LinkedList<PostCondition> clonedList = (LinkedList<PostCondition>) postCons.clone();
        clonedList.removeAll(parent.getPostCons());
        return clonedList;
    }
    
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(createBehaviorHeader());
        if(impureMethods) {
            sb.append("//might contain impure method calls\n");
        }
        sb.append(createPreCons());
        sb.append(createPostCons());
        sb.append(createSignalsOnly());
        sb.append(createSignal());
        if(diverges){
            sb.append("diverges true;\n");
        }
        sb.append(createAssignables());


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
                && assignedValues.equals(b.assignedValues)
                && exceptions.equals(b.getExceptions());
    }

    public boolean isEmpty(){
        return preCons.isEmpty()
                && postCons.isEmpty()
                && assignedValues.keySet().isEmpty()
                && exceptions.isEmpty()
                && !diverges;
    }

    private String createPreCons(){
        StringBuilder sb = new StringBuilder();
        // Write out preconditions
        for(PreCondition p : preCons){
            sb.append(p.toString());
        }

        return sb.toString();
    }

    private String createAssignables(){
        StringBuilder sb = new StringBuilder();

        if(!isExceptional) {
            for (Variable v : assignedValues.keySet()) {
                VariableValue value = assignedValues.get(v);
                if(value.getStatus() == VariableValue.Status.known){
                    sb.append("ensures " + v.toString() + " == " + value.getValue().toString() + "\n");
                }
            }
        }
        sb.append(createAssignable());
        
        return sb.toString();
    }

    private String createPostCons(){
        StringBuilder sb = new StringBuilder();
        if(!isExceptional) {
            // Write out postconditions
            for (PostCondition p : postCons) {
                sb.append(p.toString());
            }
            for (AssertStmt as : asserts) {
                sb.append("ensures " + as.getCheck().toString() + ";\n");
            }
        }

        return sb.toString();

    }

    private String createAssignable() {
        StringBuilder sb = new StringBuilder();
        sb.append("assignable ");
        if (!assignedValues.keySet().isEmpty()) {
            for (Variable v : assignedValues.keySet()) {
                if (assignedValues.get(v).getStatus() != VariableValue.Status.known) {
                    sb.append(v.toString());
                    sb.append(", ");
                }
            }
            if (sb.lastIndexOf(", ") != -1) {
                return sb.substring(0, sb.lastIndexOf(", ")) + ";\n";
            }
        }
        sb.append("\\nothing;");
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
                sb.append(ec.getType().toString());
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
            sb.append(ec.toString());
        }

        return sb.toString();
    }

    public Behavior getParent() {
        return parent;
    }

    public LinkedList<Behavior> getChildren() {
        return children;
    }

    public void addChild(Behavior child){
        this.children.add(child);
    }

    public int getLevel() {
        return level;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
        for(Behavior b : children){
            b.setClosed(closed);
        }
    }

    public void addPostAssert(AssertStmt as) {
        asserts.add(as);
    }

    public LinkedList<AssertStmt> getAsserts() {
        return asserts;
    }

    public void clearPostAssert() {
        if(!closed){
            this.postCons.clear();
        }
        for (Behavior b : children){
            b.clearPostAssert();
        }
    }
    public Expression getAssignedValue(Variable v){
        VariableValue value = assignedValues.get(v);
        if(value.getStatus() == VariableValue.Status.known){
            return value.getValue();
        } else if (value.getStatus() == VariableValue.Status.old){
            return new NameExpr(v.toString());
        } else {
            return null;
        }
    }
    public void addField(Variable v){
        assignedValues.put(v, new VariableValue(VariableValue.Status.old));
    }
    public void putAssignedValue(Variable v, Expression e){
        if(!closed) {
            if(e == null){
                assignedValues.put(v, new VariableValue(VariableValue.Status.unknown));
            } else {
                assignedValues.put(v, new VariableValue(e));
            }
        }
        for(Behavior b : children){
            b.putAssignedValue(v, e);
        }
    }
    public LinkedList<Behavior> getLeafs(){
        LinkedList<Behavior> list = new LinkedList<>();
        if(this.children.isEmpty()){
            list.add(this);
        } else {
            for(Behavior b : this.children){
                list.addAll(b.getLeafs());
            }
        }
        return list;
    }

    public void setDiverges(boolean diverges) {
        this.diverges = diverges;
        for(Behavior beh : children){
            beh.setDiverges(diverges);
        }
    }

    public boolean isDiverges() {
        return diverges;
    }

    public void setImpureMethods() {
        this.impureMethods = true;
        for(Behavior b : children){
            b.setImpureMethods();
        }
    }

    public Optional<Exception> getFailing() {
        return failing;
    }

    public void setFailing(Optional<Exception> failing) {
        this.failing = failing;
    }
}