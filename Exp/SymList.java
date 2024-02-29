public class SymList  {
    Sym     head = null;
    boolean dbg;

    // -------------------------------------
    public Sym add (
        String  name,
        char    type )
    {
        boolean dbg = false;
        Sym sym = find (name);
        if (null != sym)  {
            if (dbg)
                System.out.format ("symlist.add: %s exists\n", name);
            return sym;
        }

        sym      = new Sym (name, type);
        sym.next = head;
        head     = sym;

        if (dbg)
            System.out.format ("add: %s\n", sym.name);

        return sym;
    }

    // -------------------------------------
    public void checkRules ()
    {
        System.out.println ("symList.checkRule:");
        for (Sym sym = head; null != sym; sym = sym.next) {
            if ('*' != sym.type || null == sym.ruleList)
                continue;
            sym.ruleList.checks ();
        }
    }

    // -------------------------------------
    public void disp ()
    {
        System.out.println ("symList.disp:");
        for (Sym sym = head; null != sym; sym = sym.next)  {
            if (0 < sym.lock)
                System.out.format (
                    "    %c %d %-4s\n", sym.cond, sym.lock, sym.name);
            else
                System.out.format ("    %c   %-4s\n", sym.cond, sym.name);
        }
        System.out.println ();
    }

    // -------------------------------------
    public void dispRules ()
    {
        System.out.println ("symList.dispRule:");
        for (Sym sym = head; null != sym; sym = sym.next) {
            if ('*' != sym.type)
                continue;

            System.out.format (" sym.disp: %c %-4s", sym.cond, sym.name);
            if (null == sym.ruleList)  {
                System.out.println ();
                continue;
            }

            System.out.print (" - has rules\n");
            sym.ruleList.disp (sym.name);
        }
        System.out.println ();
    }

    // -------------------------------------
    public Sym find (
        String  name )
    {
        if (dbg)
            System.out.format ("find: %s\n", name);

        Sym sym = head;
        while (null != sym) {
            if (dbg)
                System.out.format ("  find: %s %s\n", name, sym.name);

            if (sym.name.equals (name))  {
                if (dbg)
                    System.out.format ("    find: found\n");
                return sym;
            }
            sym = sym.next;
        }

        return null;
    }
}
