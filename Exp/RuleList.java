public class RuleList  {
    RuleList    next;
    Rule        head = null;
    Sym         sym;

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

        for (int i = 2; i < fld.length; i++)  {
            id (fld [i]);

            rule      = new Rule (symList.add (name, type), cond);
            rule.next = rule0;
            rule0     = rule;
        }
        head = rule;
    }

    // ------------------------------------------------------------------------
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
    public void check ()
    {
        System.out.format (" ruleList.check:\n");
        for (RuleList rl = this; null != rl; rl = rl.next)  {
            boolean match = true;

            System.out.format (
                "    %c %-4s", sym.cond, sym.name);

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

            if (match)  {
                sym.cond = 'c';
                System.out.println (" -- match");
            }
            System.out.println ();
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
    public void id (
        String fld )
    {
     // System.out.format ("  rule.id:\n");

        char   c0   = fld.charAt (0);
        char   c1   = fld.charAt (1);

        // signals start with a digit
        if (Character.isDigit (c0))  {
            if (0 != (atoi (fld) % 2)) {
                System.out.format (
                    "Error - ruleNew invalid signal ID - %s\n",
                        fld);
                System.exit (1);
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

        // switches
        else if (Character.isDigit (c1))  {
            if (1 != (atoi (fld.substring (1)) % 2)) {
                System.out.format (
                    "Error - ruleNew invalid switch ID - %s\n",
                        fld);
                System.exit (1);
            }

            if ('N' != c0 && 'D' != c0)  {
                System.out.format (
                    "Error - ruleNew invalid switch cond - %c\n", c0);
                System.exit (1);
            }

            type = 'x';
            cond = c0;
            name = fld.substring (1);
        }

        // levers 2nd char is 'L'
        else if ('L' == c1)  {
            if (0 != (atoi (fld.substring (1)) % 2)) {
                System.out.format (
                    "Error - ruleNew invalid signal lever - %s\n",
                        fld);
                System.exit (1);
            }

            if ('l' != c0 && 'c' != c0 && 'r' != c0)  {
                System.out.format (
                    "Error - ruleNew invalid lever cond - %c\n", c0);
                System.exit (1);
            }

            type = 'L';
            cond = c0;
            name = fld.substring (1);
        }

        else {
            System.out.format (
                "Error - ruleNew unknown rule - %s\n", fld);
            System.exit (1);
        }

        if (dbg)
            System.out.format (" %13s %c %c %s\n", fld, type, cond, name);
    }
}
