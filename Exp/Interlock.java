// CTC interlock tester

import java.io.*;
import java.lang.*;
import java.util.*;

public class Interlock
{
    Ctc      ctc      = null;
    SymList  symList  = new SymList ();
    Track    trk      = new Track ();

    // --------------------------------
    public Interlock (
        String pnlFile )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        loadPnl (pnlFile);
    }

    // --------------------------------
    public Interlock (
        String pnlFile,
        String cmdFile )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        loadPnl (pnlFile);

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

    private void loadPnlErr (
        String line,
        String msg )
    {
        System.out.format ("Error: loadPnl - %s\n", msg);
        System.out.format ("    %s\n", line);
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

        int            err = 0;

        while ((line = br.readLine()) != null)  {
            String[]    fld = line.split("  *");
            if (dbg)
                System.out.format ("   loadPnl: %s - %s\n", line, fld [0]);

            // -----------------------------------
            if (fld[0].equals("#"))
                ;           // ignore

            // -----------------------------------
            else if (fld[0].equals("block"))  {
                int nlkId  = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);

                if (! trk.check (col, row, 'B'))  {
                    loadPnlErr (line, "invalid block tile");
                    err++;
                    continue;
                }

                System.out.format (
                        " loadPnl block: add block ???\n");
            }

            // -----------------------------------
            else if (fld[0].equals("ctc"))  {
                Ctc c = null;
                for (int n = 1; n < fld.length; n++)  {

                    Sym sym = symList.add ("L" + fld [n], 'L');

                    c      = new Ctc (Integer.parseInt (fld [n]));
                    c.next = ctc;
                    ctc    = c;
                }
            }

            // -----------------------------------
            else if (fld[0].equals("row"))  {
                trk.newRow (line.substring (4));
            }

            // -----------------------------------
            else if (fld[0].equals("rule"))  {
                Sym sym = symList.find (fld [1]);
                if (null == sym)  {
                    loadPnlErr (line, "unknown sym");
                    err++;
                    continue;
                }
                System.out.format (" loadPnl addrule: %s\n", fld [1]);
                sym.addRule (fld, symList);
            }

            // -----------------------------------
            else if (fld[0].equals("signal"))  {
                int ctcCol = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);

                if (null == ctc || ! ctc.check (ctcCol)) {
                    loadPnlErr (line, "invalid ctc ID");
                    err++;
                    continue;
                }

                if (! trk.check (col, row, '*'))  {
                    loadPnlErr (line, "invalid track tile");
                    err++;
                    continue;
                }

                System.out.format (" loadPnl signal:  %s\n", fld [4]);
                Sym sym = symList.add (fld [4], '*');
            }

            // -----------------------------------
            else if (fld[0].equals("turnout"))  {
                int ctcCol = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);

                if (null == ctc || ! ctc.check (ctcCol)) {
                    loadPnlErr (line, "invalid invalid ctc ID");
                    err++;
                    continue;
                }

                if (! trk.check (col, row, 'x'))  {
                    loadPnlErr (line, "invalid track tile");
                    err++;
                    continue;
                }

                System.out.format (" loadPnl turnout: %s\n", fld [4]);
                Sym sym = symList.add (fld [4], 'x');
            }

            // -----------------------------------
            else if (0 < line.length()) {
                loadPnlErr (line, "unknown keyword");
                err++;
                continue;
            }
        }

     // symList.disp ();

        if (0 < err)  {
            System.out.format ("loadPnl: %d errors\n", err);
            System.exit (1);
        }
    }

    // ------------------------------------------------------------------------
    //   cmd process
    private void cmdProcess (String cmdFile)
            throws FileNotFoundException, IOException
    {
        boolean dbg = false;
        System.out.format ("\ncmdProcess: %s\n", cmdFile);

        BufferedReader br = new BufferedReader(new FileReader (cmdFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            if (0 == line.length ())
                continue;

            System.out.format (" cmdProcess: %s\n", line);
            if ('#' == line.charAt (0))
                continue;

            String[]    fld = line.split("  *");

            // ---------------------------
            if (fld [1].equals ("check"))
                ; // symList.checkRules ();

            // ---------------------------
            else if (fld [1].equals ("dispRules"))
                symList.dispRules ();

            // ---------------------------
            else if (fld [1].equals ("list"))
                symList.disp ();

            // ---------------------------
            else if (fld [1].equals ("quit"))
                System.exit (0);

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

                if (0 != sym.lock)
                    System.out.format (
                        "  cmdProcess: set %s locked\n", sym.name);
                else
                    sym.cond = fld [3].charAt (0);
            }

            // ---------------------------
            else {
                System.out.format (
                        "Error - cmdProcess - unknown - %s\n", fld [1]);
                System.exit (2);
            }
        }
    }
}
