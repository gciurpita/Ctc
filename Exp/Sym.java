public class Sym  {
    String      name;
    char        type;
    int         num;        // ctc num
    String      mqtt;       // interface, mqtt topic

    char        pos;        // needed for signal lvr
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
        String  mqtt )
    {
        this.name = name;
        this.num  = num;
        this.type = type;
        this.mqtt = mqtt;

     // System.out.format ("   Sym: %c %-6s %4d\n", type, name, num);

        switch (type)  {
        case 'B':
            this.cond = 'U';        // Un/Occupied
            break;

        case 'L':
            if (0 == (num % 2))
                this.cond = 'C';    // center signal
            else
                this.cond = 'L';    // set turnout left
            break;

        case 'S':
            this.cond = 'S';        // Stop/Clear
            break;

        case 'T':
            this.cond = 'N';        // Normal/Reverse
            break;

        }

        this.pos = this.cond;

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

        if (true) {
            for (RuleList rl = ruleList; null != rl; rl = rl.next)
                System.out.format (" sym.addRule: ruleList\n");
            ruleList.disp (fld [0]);
        }

     }

    // --------------------------------
    public void disp ()
    {
        System.out.format (
            " Sym %-6s '%c' %4d, %c %c %d %6s",
                name, type, num, pos, cond, lock, mqtt);

        if (null != ruleList)
            System.out.format (" rules");
        System.out.println ();
     }

    // --------------------------------
    public String getSubTopic ()
    {
        if (null == mqtt)
            return type + "/" + name + "/";
        return mqtt;
    }
}
