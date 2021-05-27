public class Server {
    protected String type;
    protected int id;
    protected String state;
    protected int curStartTime ;
    protected int core;
    protected int memory;
    protected int disk;
    protected int wjobs;
    protected int rjobs;

    public Server( String t, int id, String s, int cst, int c, int m, int d,int wj, int rj) {
        this.type = t;
        this.id = id;
        this.state= s ;
        this.curStartTime = cst;
        this.core = c;
        this.memory = m;
        this.disk = d;
        this.wjobs=wj;
        this.rjobs=rj;

    }
}
