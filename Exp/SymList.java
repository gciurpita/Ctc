public class SymList  {
    Sym     head = null;
    boolean dbg;

    // -------------------------------------
    public Sym add (
        String  name,
        char    type )
    {
        Sym sym = find (name);
        if (null != sym)
            return sym;

        sym      = new Sym (name, type);
        sym.next = head;
        head     = sym;

        if (dbg)
            System.out.format ("add: %s\n", sym.name);

        return sym;
    }

    // -------------------------------------
    public void disp ()
    {
        Sym sym = head;
        while (null != sym) {
            System.out.format (" sym.disp: %c %-4s\n", sym.cond, sym.name);
            sym = sym.next;
        }
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
