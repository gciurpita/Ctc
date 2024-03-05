public class Sym  {
    String      name;
    char        type;
    char        cond;
    RuleList    ruleList;
    Sym         next;
    int         lock;
    boolean     dbg = false;

    // -------------------------------------
    public Sym (
        String  name,
        char    type )
    {
        this.name = name;
        this.type = type;
        if ('*' == type)
            this.cond = 'S';
        else if ('L' == type)
            this.cond = 'c';    // center
        else
            this.cond = '_';

        ruleList = null;
        next     = null;
    }

    // --------------------------------
    public void addRule (
                    String  fld [],
                    SymList symList )
    {
        if (dbg)
            System.out.format ("\nsym.addRule: %s\n", fld [1]);

        // identify first rule, allocate and start new rule list
        RuleList ruleList0 = ruleList;
        ruleList           = new RuleList (fld, symList);
        ruleList.next      = ruleList0;
        ruleList.sym       = this;

        if (false)
            for (RuleList rl = ruleList; null != rl; rl = rl.next)
                System.out.format (" sym.addRule: ruleList\n");

        ruleList.disp (name);
   }
}
