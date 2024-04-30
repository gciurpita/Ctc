public class Rule  {
    Sym     sym;
    char    cond;
    boolean lock;

    Rule    listNext;
    Rule    next;

    public Rule (
        Sym     sym,
        char    cond )
    {
        this.sym  = sym;
        this.cond = cond;
    }
}
