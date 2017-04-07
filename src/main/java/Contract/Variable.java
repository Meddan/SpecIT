package Contract;

public class Variable {
    public String getName() {
        return name;
    }
    public enum Scope {
        local, field, staticfield;
    }
    public String getClassName() {
        return className;
    }
    private Scope scope;
    private String name;
    private String className;
    private Scope getScope(){
        return this.scope;
    }
    public Variable(Scope s, String name, String clazz){
        this.scope = s;
        this.className = clazz;
        this.name = name;
    }
    public boolean equals(Object o){
        if(o == null){
            return false;
        }
        if(o instanceof Variable) {
            Variable v = (Variable) o;
            return (v.getScope() == this.scope && className.equals(v.getClassName()) && name.equals(v.getName()));
        }
        return false;
    }
    public String toString(){
        if(scope == Scope.field){
            return "this." + name;
        } else if (scope == Scope.staticfield) {
            return className + name;
        } else {
            return name;
        }
    }
}
