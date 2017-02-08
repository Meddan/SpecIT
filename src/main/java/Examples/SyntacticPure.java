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
}
