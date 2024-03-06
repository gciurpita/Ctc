public class RuleList  {
    RuleList    next;
    Rule        head = null;
    Sym         sym;
    boolean     locked;

    private boolean dbg = false;

    // globals from id()
    private String name = "?";
    private char   type = '?';
    private char   cond = '?';

    // --------------------------------
    public RuleList (
        String  fld [],
        SymList symList )
    {
        if (dbg)
            System.out.format (" RuleList: %s\n", fld [1]);

        Rule rule   = null;
        Rule rule0  = head;

        int  err    = 0;

        for (int i = 2; i < fld.length; i++)  {
            if (! id (fld [i]))  {
                System.out.format ("Error: RuleList: bad id, %s\n", fld [i]);
                err++;
                continue;
            }

            Sym sym   = symList.find (name);
            if (null == sym)  {
                System.out.format (
                    " RuleList: unknown sym %s\n", name);
                err++;
                continue;
            }

            rule      = new Rule (sym, cond);
            rule.next = rule0;
            rule0     = rule;
        }

        if (0 < err)  {
            System.out.format ("RuleList: %d errors\n", err);
        }

        head = rule;
    }

    // ------------------------------------------------------------------------
    private void uncheck ()
    {
        System.out.format ("  ruleList.uncheck: %s\n", sym.name);
        for (RuleList rl = this; null != rl; rl = rl.next)  {
            if ( ! rl.locked)
                continue;

            if ( ! check (rl))  {
                rl.locked = false;
                sym.cond  = 'S';
                rl.unlock ();
            }
        }
        System.out.println ();
    }

    // --------------------------------
    public boolean check (
        RuleList  rl )
    {
        System.out.format (
            "    %c %-4s", rl.sym.cond, rl.sym.name);

        boolean match = true;
        for (Rule rule = rl.head; null != rule; rule = rule.next) {
            char d = rule.sym.cond;
            char c = rule.cond;

            if (d != c)  {
                match = false;
                System.out.format ("  %c %c %-4s", d, c, rule.sym.name);
            }
            else
                System.out.format ("  . %c %-4s", c, rule.sym.name);
        }
        if (match)
            System.out.println (" -- match");
        System.out.println ();

        return match;
    }

    // --------------------------------
    public void checks ()
    {
        System.out.format (" ruleList.checks:\n");

        if ('c' == sym.cond) {
            uncheck ();
            return;
        }

        for (RuleList rl = this; null != rl; rl = rl.next)  {
            if (check (rl) && 'S' == sym.cond)  {
                rl.locked = true;
                sym.cond  = 'c';
                rl.lock ();
                break;
            }
        }
    }

    // --------------------------------
    private void lock ()
    {
        for (Rule rule = head; null != rule; rule = rule.next)
            if ('B' != rule.sym.type)
                rule.sym.lock++;
    }

    // --------------------------------
    private void unlock ()
    {
        System.out.format ("   ruleList.unlock()\n");
        for (Rule rule = head; null != rule; rule = rule.next)  {
            if ('B' != rule.sym.type && 0 == rule.sym.lock)  {
                System.out.format (
                    "Error - ruleLost.unlock - %s rule element not locked\n",
                        sym.name);
                System.exit (1);
            }
            rule.sym.lock--;
        }
    }

    // --------------------------------
    public void disp (
        String name )
    {
        System.out.format ("  ruleList.disp:\n");
        for (RuleList rl = this; null != rl; rl = rl.next)  {
         // System.out.format ("  ruleList.disp: %-4s", rl.head.sym.name);
            System.out.format ("    %-4s", name);

            for (Rule rule = rl.head; null != rule; rule = rule.next) {
                char c = rule.cond;
                System.out.format ("  %c %-4s", c, rule.sym.name);
            }
            System.out.println ();
        }
    }

    // --------------------------------
    private int atoi (
        String s )
    {
        int  val = 0;

        for (int i = 0; i < s.length(); i++)  {
            char c = s.charAt (i);
            if (! Character.isDigit (c))
                break;
            val = 10*val + c - '0';
        }

        return val;
    }

    // --------------------------------
    public boolean id (
        String fld )
    {
     // System.out.format ("  rule.id:\n");

        char   c0   = fld.charAt (0);
        char   c1   = fld.charAt (1);

        // signals start with a digit
        if (Character.isDigit (c0))  {
            if (0 != (atoi (fld) % 2)) {
                System.out.format (
                    "Error: id - invalid signal ID - %s\n", fld);
                return false;
            }

            type = '*';
            cond = 'S';
            name = fld;
        }

        // block prefixed with 'B'
        else if ('B' == c0)  {
            type = 'B';
            cond = 'u';
            name = fld.substring (0);
        }

        // switche or signal condition
        else if (Character.isDigit (c1))  {
            int id = atoi (fld.substring (1));

            if (1 == id % 2) {
                if ('N' != c0 && 'R' != c0)  {
                    System.out.format (
                        "Error: id - invalid switch cond - %c  %s, %d\n",
                            c0, fld, id);
                    return false;
                }

                type = 'x';
                cond = c0;
                name = fld.substring (1);

                return true;
            }
            else {
                if ('S' != c0 && 'c' != c0)  {
                    System.out.format (
                        "Error: id invalid signal cond - %s, %d\n", fld, id);
                    return false;
                }

                type = '*';
                cond = c0;
                name = fld.substring (1);

                return true;
            }
        }

        // levers 2nd char is 'L'
        else if ('L' == c1)  {
            if (0 != (atoi (fld.substring (1)) % 2)) {
                System.out.format (
                    "Error - ruleNew invalid signal lever - %s\n",
                        fld);
                return false;
            }

            if ('l' != c0 && 'c' != c0 && 'r' != c0)  {
                System.out.format (
                    "Error - ruleNew invalid lever cond - %c\n", c0);
                return false;
            }

            type = 'L';
            cond = c0;
            name = fld.substring (1);
        }

        else {
            System.out.format (
                "Error - ruleNew unknown rule - %s\n", fld);
            return false;
        }

        if (dbg)
            System.out.format (" %13s %c %c %s\n", fld, type, cond, name);

        return true;
    }
}
