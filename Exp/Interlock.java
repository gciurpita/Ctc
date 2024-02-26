// CTC interlock tester

import java.io.*;
import java.lang.*;
import java.util.*;

public class Interlock
{
    SymList  symList  = new SymList ();
    RuleList ruleList = new RuleList ();

    public Interlock (
        String pnlFile )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        loadPnl (pnlFile);
        symList.disp ();
        ruleList.disp ();
    }

    // ------------------------------------------------------------------------
    // main processes command line arguments settign the debug level and

    public static void main (String[] args)
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        int    i;

        for (i = 0; i < args.length; i++) {
            System.out.format ("main: %d %s\n", i, args[i]);

            if (args[i].startsWith("-"))  {
                System.out.format ("main:  %s\n", args [i]);
            }
            else
                break;
        }

        System.out.println (args [i]);

        Interlock intrLck = new Interlock (args [i]);
    }

    // ------------------------------------------------------------------------
    //   load panel decription

    private void loadPnl (String pnlFile)
            throws FileNotFoundException, IOException
    {
        boolean dbg = false;
        System.out.format (" loadPnl: %s\n", pnlFile);

        BufferedReader br = new BufferedReader(new FileReader(pnlFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            String[]    fld = line.split("  *");
            if (dbg)
                System.out.format ("   loadPnl: %s - %s\n", line, fld [0]);

            // -----------------------------------
            if (fld[0].equals("#"))
                ;           // ignore

            // -----------------------------------
            else if (fld[0].equals("rule"))  {
             // System.out.format ("  loadPnl: %s\n", line);
                ruleList.add (fld, symList);
            }

            // -----------------------------------
            else if (0 < line.length()) {
                ; // System.out.format ("loadPnl: ERROR - %s\n", line);
            }
        }
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

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    private class Rule  {
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

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    private class RuleList  {
        Rule    head = null;

        private boolean dbg = true;

        // globals from id()
        private String name = "?";
        private char   type = '?';
        private char   cond = '?';

        // --------------------------------
        public void add (
            String  fld [],
            SymList symList )
        {
            System.out.format ("  rule.add:\n");

            id (fld [1]);

            Rule rule0 = new Rule (symList.add (name, type), cond);
            rule0.listNext = head;
            head           = rule0;
    
            for (int i = 2; i < fld.length; i++)  {
                id (fld [i]);

                Rule rule = new Rule (symList.add (name, type), cond);
                rule.next  = rule0.next;
                rule0.next = rule;
            }
        }

        // --------------------------------
        public void disp ()
        {
            System.out.format ("rule.disp:\n");
            for (Rule rule0 = head; null != rule0; rule0 = rule0.listNext)  {
                System.out.format (" rule.disp: %-4s", rule0.sym.name);

                for (Rule rule = rule0.next; null != rule; rule = rule.next)
                    System.out.format (" %4s", rule.sym.name);

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

            if (Character.isDigit (c0))  {
                if (0 != (atoi (fld) % 2)) {
                    System.out.format (
                        "Error - ruleNew invalid signal ID - %s\n",
                            fld);
                    System.exit (1);
                }

                type = '*';
                cond = ' ';
                name = fld;
            }

            else if (Character.isDigit (c1))  {
                if (1 != (atoi (fld.substring (1)) % 2)) {
                    System.out.format (
                        "Error - ruleNew invalid switch ID - %s\n",
                            fld);
                    System.exit (1);
                }

                type = 'x';
                cond = c0;
                name = fld.substring (1);
            }

            else if ('L' == c1)  {
                if (0 != (atoi (fld.substring (1)) % 2)) {
                    System.out.format (
                        "Error - ruleNew invalid signal lever - %s\n",
                            fld);
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

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    private class Sym  {
        String  name;
        char    type;
        char    cond;
        Rule    rule;
        Sym     next;

        // -------------------------------------
        public Sym (
            String  name,
            char    type )
        {
            this.name = name;
            this.type = type;
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
    private class SymList  {
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
                System.out.format (" sym.disp: %-4s %c\n", sym.name, sym.type);
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

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 
}
