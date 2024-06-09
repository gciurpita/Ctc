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

    SigList     sigList;    // signal syms dropped when blk occupied

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

        case 'K':                   // manual turnout lock
            this.cond = 'u';        // Un/locked
            break;

        case 'L':                   // lever
            if (0 == (num % 2))
                this.cond = 'C';    // center signal
            else
                this.cond = 'N';    // set turnout normal
            break;

        case 'S':                   // signal
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

        if (dbg) {
            for (RuleList rl = ruleList; null != rl; rl = rl.next)
                System.out.format (" sym.addRule: ruleList\n");
            ruleList.disp (fld [0]);
        }

        // add sym to block
 //   if (false) {
 //     for (int i = 2; i < fld.length; i++)  {
 //         if ('B' == fld [i].charAt(1)) {
 //             String blkName = fld [i].substring (1);
 //             Sym    symBlk  = symList.findName (blkName);

 //             if (null == symBlk)  {
 //                 System.err.format ("Error blkSym not found %s\n", blkName);
 //                 System.exit (2);
 //             }

 //             System.out.format (
 //                 "  sym.addRule: add rule %s includes blk %s\n",
 //                     fld [1], symBlk.name);

 //             // append this to blk sigList
 //             symBlk.sigList = new SigList (this, symBlk.sigList);
 //         }
 //     }
 // }
     }

    // --------------------------------
    public void disp ()
    {
        System.out.format (
            "   Sym %-6s '%c' %4d, %c %c %d %6s",
                name, type, num, pos, cond, lock, mqtt);

        if (null != ruleList)  {
            System.out.format (" rules");
 //         System.out.println ();

 //         if ('B' == type)  {
 //             for (RuleList rL = ruleList; null != rL; rL = rL.next)
 //                 rL.disp (name);
 //         }
        }

        if (null != sigList)  {
            System.out.format (" sigList");
            for (SigList sl = sigList; null != sl; sl = sl.next)
                System.out.format (" %s", sl.sym.name);
        }

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
