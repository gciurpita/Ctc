// CTC interlock tester

import java.io.*;
import java.lang.*;
import java.util.*;

public class Interlock
{
    SymList  symList  = new SymList ();
    RuleList ruleList = new RuleList ();

    // --------------------------------
    public Interlock (
        String pnlFile )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        loadPnl (pnlFile);
     // symList.disp ();
        ruleList.disp ();
    }

    // --------------------------------
    public Interlock (
        String pnlFile,
        String cmdFile )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        loadPnl (pnlFile);
     // symList.disp ();
        ruleList.disp ();
        cmdProcess (cmdFile);
    }

    // ------------------------------------------------------------------------
    // main processes command line arguments settign the debug level and

    public static void main (String[] args)
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        String cmdFname = null;
        int    i;

        for (i = 0; i < args.length; i++) {
            System.out.format ("main: %d %s\n", i, args[i]);

            if (args [i].startsWith("-"))  {
                if (args [i].startsWith ("-c"))
                    cmdFname = args [i].substring (2);
            }
            else
                break;
        }

        Interlock intrLck;
        if (null != cmdFname)
            intrLck = new Interlock (args [i], cmdFname);
        else
            intrLck = new Interlock (args [i]);
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
    //   cmd process
    private void cmdProcess (String cmdFile)
            throws FileNotFoundException, IOException
    {
        boolean dbg = false;
        System.out.format ("cmdProcess: %s\n", cmdFile);

        BufferedReader br = new BufferedReader(new FileReader (cmdFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            System.out.format (" cmdProcess: %s\n", line);
            if (0 == line.length () || '#' == line.charAt (0))
                continue;

            String[]    fld = line.split("  *");

            // ---------------------------
            if (fld [1].equals ("check"))
                ruleList.check ();

            // ---------------------------
            else if (fld [1].equals ("list"))
                symList.disp ();

            // ---------------------------
            else if (fld [1].equals ("set"))  {
                if (fld .length < 3)  {
                    System.out.format ("Error - cmdProcess - set sym cond\n");
                    System.exit (2);
                }

                Sym sym = symList.find (fld [2]);
                if (null == sym)  {
                    System.out.format (
                        "Error - cmdProcess - set not found - %s\n", fld [2]);
                    System.exit (2);
                }

                sym.cond = fld [3].charAt (0);
            }
        }
    }
}
