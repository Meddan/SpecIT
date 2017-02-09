package Examples;

public class SyntacticPure {
    int k = 0;
    int[] a;
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
    public int arrays(){
        int[] b = new int[4];
        b[1] = 3;
        return 0;
    }
    public int arrays2(){
        a[4] = 0;
        return 0;
    }
    public int arrays3(){
        int[] b = new int[4];
        b[k++] = 2;
        return 0;
    }
    public void cast(){
        int a = 3;
        double d = (double)a;
    }
    public void cast2(){
        double d = (double)k;
    }
    public int conditional(){
        int a = k == 0 ? 1 : 2;
        return a;
    }
    public int conditional2(){
        int a = k++ == 1 ? 2 : 3;
        return a;
    }
    public int inherit(){
        return 0;
    }
    public int inherit2(){
        //super.toString();
        this.toString();
        return k++;
    }
    public SyntacticPure(){
        super();
    }
}
