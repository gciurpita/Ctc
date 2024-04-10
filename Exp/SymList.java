public class SymList  {
    Sym     head = null;
    boolean dbg  = false;

    // -------------------------------------
    public Sym add (
        String  name,
        char    type,
        int     num,
        String  id )        // interface
    {
     // boolean dbg = false;

        Sym sym = find (name);
        if (null != sym)  {
            if (dbg)
                System.out.format ("symlist.add: %s exists\n", name);
            return sym;
        }

        sym      = new Sym (name, type, num, id);
        sym.next = head;
        head     = sym;

        if (dbg)
            System.out.format ("  symList.add: %s\n", sym.name);

        return sym;
    }

    // -------------------------------------
    public void checkRules (
        Control   ctl )
    {
        System.out.println ("symList.checkRule:");
        for (Sym sym = head; null != sym; sym = sym.next) {
            if ('S' != sym.type || null == sym.ruleList)
                continue;
            sym.ruleList.checks (ctl);
        }
    }

    // -------------------------------------
    public void disp ()
    {
        System.out.println ("symList.disp:");
        for (Sym sym = head; null != sym; sym = sym.next)
            sym.disp ();
    }

    // -------------------------------------
    public void dispRules ()
    {
        System.out.println ("symList.dispRule:");
        for (Sym sym = head; null != sym; sym = sym.next) {
            if ('S' != sym.type)
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
            System.out.format ("  symList.find: %s\n", name);

        for (Sym sym = head; null != sym; sym = sym.next) {
            if (dbg)
                System.out.format ("   symlist.find: %s\n", sym.name);

            if (sym.name.equals (name))  {
                if (dbg)
                    System.out.format ("    symlist.find: found\n");
                return sym;
            }
        }

        return null;
    }
    // -------------------------------------
    public Sym findName (
        String  name )
    {
        if (dbg)
            System.out.format ("  symList.findName: %s\n", name);

        for (Sym sym = head; null != sym; sym = sym.next) {
            if (dbg)
                System.out.format ("   symlist.findName: %s\n", sym.name);

            if (sym.name.equals (name))  {
                if (dbg)
                    System.out.format ("    symlist.findName: found\n");
                return sym;
            }
        }

        return null;
    }
}
