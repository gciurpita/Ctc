
import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;

import javax.imageio.ImageIO;

// -----------------------------------------------------------------------------
public class Panel {
    private class Lever {
        int     id;
        Sym     sym;
        Sym     symCtl;     // turnout/signal

        // --------------------------------
        public Lever (
            int  id,
            Sym  sym )
        {
            this.sym     = sym;
            this.sym.pos = 0 == (id % 2) ? 'c' : 'n';
        }
    }

    // --------------------------------
    final int Nlvr       = 72;   // 1-30
    Lever     lvr  []    = new Lever [Nlvr];
    int       codeBut [] = new int [Nlvr /2];
    Sym       symLock [] = new Sym [Nlvr];

    Control   ctl;
    SymList   symList;

    String    cfgFile  = "./Resources/ctcNumbered.cfg";
    class Icon   {
        Image   img;
    }
    Icon      code    [] = new Icon [2];
    Icon      lamp    [] = new Icon [15];
    Icon      lever   [] = new Icon [3];
    Icon      lock    [] = new Icon [2];
    Icon      signal  [] = new Icon [15];
    Icon      turnout [] = new Icon [15];

    int       iconToHt;
    int       iconToWid;
    int       iconSigHt;
    int       iconSigWid;

    int       y0Panel;
    int       colWid     = 64;
    int       colHt;

    boolean   dbg        = false;

    // --------------------------------
    public Panel (
        Control  ctl,
        SymList  symList )
            throws FileNotFoundException, IOException
    {
        this.ctl     = ctl;
        this.symList = symList;
        loadIcons ();
    }

    // ------------------------------------------------------------------------
    public void loadIcons ()
            throws FileNotFoundException, IOException
    {
        System.out.format ("loadIcons: %s\n", cfgFile);

        BufferedReader br = new BufferedReader(new FileReader(cfgFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            String[]    fields = line.split("  *");
            if (dbg)
                System.out.format (" loadIcon: %s - %s\n", line, fields [0]);

            if (! fields[0].equals("icon"))
                continue;

            if (fields.length < 4)  {
                System.out.format (
                    "Error - loadIcons: 4 arg - icon type id <filename>\n");
                System.exit (2);
            }

            int    id      = Integer.parseInt (fields [2]);
            File   inFile  = new File (fields [3]);

            if (fields[1].equals("Lever"))  {
                lever [id]     = new Icon ();
                lever [id].img = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Lamp"))  {
                lamp [id]      = new Icon ();
                lamp [id].img  = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Signal"))  {
                signal [id]      = new Icon ();
                signal [id].img  = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Turnout"))  {
                turnout [id]      = new Icon ();
                turnout [id].img  = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Code"))  {
                code [id]      = new Icon ();
                code [id].img  = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Lock"))  {
                lock [id]      = new Icon ();
                lock [id].img  = ImageIO.read (inFile);
            }

         // System.out.format ("  loadIcons: %s\n", fields [3]);
        }

        iconToHt  = turnout [0].img.getHeight (null);
        iconToWid = turnout [0].img.getWidth  (null);
        System.out.format ("loadIcons: to wid %d, ht %d", iconToWid, iconToHt);

        iconSigHt  = signal [0].img.getHeight (null);
        iconSigWid = signal [0].img.getWidth  (null);
        System.out.format ( ", sig wid %d, ht %d", iconSigWid, iconSigHt);

        int iconCodeHt  = code [0].img.getHeight (null);
        int iconCodeWid = code [0].img.getWidth  (null);
        System.out.format ( ", cod wid %d, ht %d", iconCodeWid, iconCodeHt);

        int iconLockHt  = code [0].img.getHeight (null);
        int iconLockWid = code [0].img.getWidth  (null);
        System.out.format ( ", cod wid %d, ht %d", iconLockWid, iconLockHt);

        System.out.println ();

        colHt = iconToHt + iconSigHt + 2 * iconCodeHt + 2 * iconLockHt;
    }

    // --------------------------------
    public void addLever (
        int  ctcId,
        Sym  sym )
    {
        if (lvr.length <= ctcId)  {
            System.err.format ("Error Panel.addLever range %d\n", ctcId);
            System.exit (3);
        }

        lvr [ctcId] = new Lever (ctcId, sym);
    }

    // --------------------------------
    public boolean associate (
        int     num,
        Sym     sym )
    {
        if ('K' == sym.type)  {
            symLock [num] = sym;
            System.out.format (
                "  Panel associate: %-4s %d\n", symLock [num].name, num);
            return true;
        }

        if (null == lvr [num])
            return false;

        lvr [num].symCtl = sym;
        return true;
    }

    // --------------------------------
    public void response (
        int     num,
        char    state )
    {
        if (null == lvr [num])  // not a lever lamp update
            return;

        Sym sym = lvr [num].sym;

        if (0 == num % 2)  {    // signal
            if ('c' == state)   // clear
                sym.cond = lvr [num].sym.pos;
            else
                sym.cond = 'c';   // centered
        }
        else                    // turnout
            sym.cond = state;

        if (false)
            System.out.format (
                "  Panel.response: num %2d, state %c, pos %c, cond %c\n",
                        num, state, sym.pos,  sym.cond);
    }

    // --------------------------------
    public boolean mousePressed (
        int  x,
        int  y )
    {
        int col   = x / colWid;
        int dX    = x % colWid;
        int num   = 1 + (2 * col);

        if (dbg)
            System.out.format (
                "Panel.mousePressed: %d %d, col %d, num %d\n",
                    x, y, col, num);

        // lock
        if (y - y0Panel > (iconToHt + iconSigHt + 50))  {
            if (null != symLock [num])  {
                if (0 == symLock [num].lock)  {
                    if ('u' == symLock [num].cond)
                        symLock [num].cond = 'l';
                    else
                        symLock [num].cond = 'u';
                }
            }
        }

        // skip if no levers
        if (null == lvr [num])
            return false;

        // turnout
        if (y - y0Panel < iconToHt)  {
            Sym sym = lvr [num].sym;

            if ((dX < colWid / 2))
                sym.pos = 'n';
            else
                sym.pos = 'r';

            if (dbg)
                System.out.format (
                    " Panel.mousePressed: num %d, %s - %c %c\n",
                        num, sym.name, sym.pos, sym.cond);
        }

        // signal
        else if (y - y0Panel < (iconToHt + iconSigHt))  {
            num += 1;
            Sym sym = lvr [num].sym;

            if ((dX < colWid * 0.4))
                sym.pos = 'l';
            else if ((colWid * 0.6) < dX)
                sym.pos = 'r';
            else
                sym.pos = 'c';

            sym.pos = sym.pos;

            if (dbg)
                System.out.format (
                    " Panel.mousePressed: sig %d - %c\n", num, sym.pos);
        }

        // code button
        else if (y - y0Panel < (iconToHt + iconSigHt + 50))  {
            boolean dbg = false;

            if (dbg)
                System.out.format ( "Panel.mousePressed: code num %d\n", num);
            codeBut [num] = 3;

            // send turnout request if not locked
            Sym symLvr = lvr [num].sym;
            Sym symTo  = lvr [num].symCtl;

            if (symLvr.pos != symLvr.cond)  {
                if (dbg)
                    System.out.format (
                    "Panel.mousePressed: lvr %s mis-match - %c, %c\n",
                        symLvr.name, symLvr.pos, symLvr.cond);

         // System.out.format (
         //   "Panel.mousePressed: lvr %s, lPos %c, sym %s cond %c, lock %d\n",
         //         symLvr.name, lvr [num].pos,
         //             symTo.name, symTo.cond, symTo.lock);

                if (0 == symTo.lock)  {
                    if (lvr[num].sym.pos == 'n' && 'r' == symTo.cond)
                        ctl.send ('T', symTo.name, 'n');
                    else if (lvr[num].sym.pos == 'r' && 'n' == symTo.cond)
                        ctl.send ('T', symTo.name, 'r');
                }
            }

            // send signal request based on rules
            symLvr = lvr [num+1].sym;
            if (symLvr.pos != symLvr.cond)  {
                if (dbg)
                    System.out.format (
                    "Panel.mousePressed: lvr %s mis-match\n", symLvr.name);

                symList.checkRules (num+1, ctl);
            }

            return true;
        }

        return false;
    }

    // --------------------------------
    public void timer ()
    {
        for (int num = 1; num < lvr.length; num += 2)  {
            if (null == lvr [num])
                continue;

            if (0 < codeBut [num])
                codeBut [num]--;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    private void paintToPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        int         col,
        int         lvrId )
    {
        if (dbg)
            System.out.format ("  paintToPlate: %3d %3d, %d\n", x0, y0, lvrId);

        g2d.setColor (Color.white);

        // plate & lvr
        int pos = ('r' == lvr [lvrId].sym.pos) ? 1 : 0;
        g2d.drawImage (turnout [lvrId/2].img,   x0, y0, null);
        g2d.drawImage (lever   [pos].img, x0 + 6, y0 + 44, null);

        // lamps
        int lampIdxLeft  = 6;
        int lampIdxRight = 0;

        if ('r' == lvr [lvrId].sym.cond)  {
            lampIdxLeft  = 5;
            lampIdxRight = 1;
        }
        g2d.drawImage (lamp [lampIdxLeft].img,  x0 +  5, y0 + 3, null);
        g2d.drawImage (lamp [lampIdxRight].img, x0 + 34, y0 + 4, null);
    }

    // ------------------------------------------------------------------------
    private void paintSigPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        int         col,
        int         lvrId )
    {
        // plate & lvr
        char posCh = lvr [lvrId].sym.pos;
        int  pos   = 'r' == posCh ? 1 : 'c' == posCh ? 2 : 0;
        g2d.drawImage (signal [lvrId/2].img,   x0, y0, null);
        g2d.drawImage (lever  [pos].img,       x0 + 5, y0 + 57, null);

        // lamps
        int idxLeft   = 5;
        int idxCenter = 9;
        int idxRight  = 5;

        if ('L' == lvr [lvrId].sym.cond)
            idxLeft   = 6;
        else if ('r' == lvr [lvrId].sym.cond)
            idxRight  = 6;
        else        // 'c'
            idxCenter = 10;

        g2d.drawImage (lamp [idxLeft].img,   x0 +  5, y0 + 17, null);
        g2d.drawImage (lamp [idxCenter].img, x0 + 18, y0 +  6, null);
        g2d.drawImage (lamp [idxRight].img,  x0 + 34, y0 + 18, null);
    }

    // ------------------------------------------------------------------------
    public void paint (
        Graphics2D  g2d,
        int         y0,
        int         wid,
        int         ht )
    {
     // System.out.format ("Panel paint:\n");

        y0Panel  = y0;

        int   y1 = y0 + iconToHt;
        int   y2 = y1 + iconSigHt;

        g2d.setColor (new Color(115, 104, 50));  // #736832
        g2d.fillRect (0, y0, wid, y0+ht);

        if (false)  {
            System.out.format ("Panel.paint: lvr length %d\n", lvr.length);
            return;
        }

        // lamp colors: amber 1, blue 3, dark 4, green 6, purple 8
        //              red 10, white 12 ... off -1

        for (int num = 1; num < lvr.length-1; num += 2)  {
            int col = 1 + (num-1) / 2;
            int x0  = colWid * ((num-1) / 2);
            int idx;

            // lock button
            if (null != symLock [num])  {
                g2d.drawImage (lock [0].img, x0 + 12, y2 + 50, null);
                idx = 'l' == symLock [num].cond ? 3 : 4;
                g2d.drawImage (lamp [idx].img, x0 + 17, y2 + 76, null);
            }

            if (null == lvr [num] && null == lvr [num+1])
                continue;

            if (null != lvr [num])
                paintToPlate  (g2d, x0, y0, col, num);

            if (null != lvr [num+1])
                paintSigPlate (g2d, x0, y1, col, num+1);

            // code button
            idx = 0 < codeBut [num] ? 1 : 0;
            g2d.drawImage (code [idx].img, x0 + 15, y2 + 10, null);
        }
    }
};
