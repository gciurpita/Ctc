public class Sym  {
    String      name;
    char        type;
    int         num;        // ctc num
    String      id;         // interface, mqtt topic

    char        cond;
    RuleList    ruleList;
    Sym         next;

    int         lock;
    boolean     dbg = false;

    // -------------------------------------
    public Sym (
        String  name,
        char    type,
        int     num,
        String  id )
    {
        this.name = name;
        this.num  = num;
        this.type = type;
        this.id   = id;
        if ('*' == type)
            this.cond = 'S';
        else if ('L' == type)
            if (0 == (num % 2))
                this.cond = 'c';    // center
            else
                this.cond = 'L';
        else
            this.cond = '_';

        ruleList = null;
        next     = null;

     // System.out.format ("   Sym: '%c' %s\n", type, name);
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

     // ruleList.disp (name);
     }

    // --------------------------------
    public void disp ()
    {
        System.out.format (
            " Sym %4s '%c' %4d, %c %d %6s", name, type, num, cond, lock, id);

        if (null != ruleList)
            System.out.format (" rules");
        System.out.println ();
    }
}
