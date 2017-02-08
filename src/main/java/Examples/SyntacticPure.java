package Examples;

public class SyntacticPure {
    int k = 0;
    public int basic(){
        int i = 3;
        return i;
    }
    public int notBasic(){
        int j = k++;
        return j;
    }
    public int trickyVar(){
        int k = 0;
        this.k = k;
        return this.k;
    }
    public int trickyVar2(){
        int k = this.k;
        return k;
    }
}
