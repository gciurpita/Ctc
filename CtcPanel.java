// CTC Panel

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.*;

import java.awt.image.BufferedImage;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.awt.image.FilteredImageSource;

import java.awt.event.*;
import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;

import java.util.Timer;
import java.util.TimerTask;


@SuppressWarnings ("serial")

// -----------------------------------------------------------------------------
// -----------------------------------------------------------------------------
// display railroad schematic and USS switch & signal levers

public class CtcPanel extends JPanel
        implements MouseListener, KeyListener
{
    // ---------------------------------------------------------
    class CtcCol {
        int     to;
        int     sig;
        int     pos;

        PnlSym  symLvr;
        PnlSym  symTo;
        PnlSym  symSigL;
        PnlSym  symSigR;

        // -------------------------------------
        CtcCol () {
            to = sig = -1;
        }
    };

    // ---------------------------------------------------------
    class Img   {
        Image   img;
    }

    // ---------------------------------------------------------
    class PnlSym  {
        int     ctcCol;
        int     row;
        int     col;

        String  lbl;
        int     xLbl;
        int     yLbl;

        int     x;
        int     y;
        int     tile;

        PnlSym  nxtSym;

        Rule    rule []    = new Rule [10];
        int     ruleSize   = 0;

        char    cond;
        int     imgIdx;

        // -------------------------------------
        public PnlSym (
            String  prefix,
            int     plateNum )
        {
            ctcCol  = plateNum;
            cond    = ' ';
            lbl     = prefix + plateNum;

         // System.out.format (" PnlSym: %2d %c  %s\n", plateNum, cond, lbl);
        }

        // -------------------------------------
        public PnlSym (
            int     ctcCol_,
            int     row_,
            int     col_,
            String  lbl_,
            char    cond_,
            int     imgIdx_ )
        {
            ctcCol  = ctcCol_;
            row     = row_;
            col     = col_;
            lbl     = lbl_;
            imgIdx  = imgIdx_;

            x       = tileWid * col;
            y       = tileWid * row;
            cond    = cond_;


         // System.out.format (" PnlSym: %3dx%3d  %s\n", x, y, lbl);
        }
    }

    // ---------------------------------------------------------
    class Rule   {
        PnlSym  sym;
        char    cond;
        Rule    nxt;
    }

    // ---------------------------------------------------------
    JFrame frame = new JFrame ();

    private static final String Title           = "CTC";

    int             CANVAS_WIDTH    = 800;
    int             CANVAS_HEIGHT   = 400;

    private int     dbg             = 0;

    String[]        pnlRow          = new String[10];
    int             nPnlRow         = 0;
    int             maxRowLen       = 0;

    final int       CtcColMax       = 20;
    CtcCol          ctcCol []       = new CtcCol [CtcColMax];

    boolean         ctcSw  []       = new boolean [20];
    boolean         ctcSig []       = new boolean [20];

    final int       MaxPnlSym       = 20;
    PnlSym          symTo  []       = new PnlSym  [MaxPnlSym];
    int             symToSize       = 0;

    PnlSym          symSig []       = new PnlSym  [MaxPnlSym];
    int             symSigSize      = 0;

    PnlSym          symLvr []       = new PnlSym  [MaxPnlSym];
    int             symLvrSize      = 0;

    PnlSym          symCode []      = new PnlSym  [MaxPnlSym];
    int             symCodeSize     = 0;

    Rule            rules []        = new Rule [50];
    int             rulesSize       = 0;

    final int       MaxImg          = 20;
    Img             imgCode []      = new Img     [MaxImg];
    int             imgCodeSize     = 0;

    Img             imgLamp []      = new Img     [MaxImg];
    int             imgLampSize     = 0;

    Img             imgLvr []       = new Img     [MaxImg];
    int             imgLvrSize      = 0;

    Img             imgSig []       = new Img     [MaxImg];
    int             imgSigSize      = 0;

    Img             imgTo []        = new Img     [MaxImg];
    int             imgToSize       = 0;

    Img             imgTile []      = new Img     [100];
    int             imgTileSize     = 0;

    final int       ImgIdxSig       = 3;
    final int       ImgIdxSw        = 4;

    final int       LvrLeft         = 0;
    final int       LvrRight        = 1;
    final int       LvrCenter       = 2;

    final int       ColMax          = 20;
    int             nCol;
    int             colWid;

    final int       RowOff          = 150;
    int             rowHt1;
    int             rowHt2;

    int             tileWid;

    final int       CodeXoff        = 15;
    final int       CodeYoff        =  5;
    int             codeDia;

    Sckt            sckt;
    boolean         scktEn           = false;
    byte[]          buf              = new byte [100];

    // ---------------------------------------------------------
    public enum ImgType { Code, Lamp, Lever, PlateSig, PlateTo, Tile, None };

    public ImgType stringToType (String s)  {
        if (s.equals("Code"))
            return ImgType.Code;
        else if (s.equals("Lamp"))
            return ImgType.Lamp;
        else if (s.equals("Lever"))
            return ImgType.Lever;
        else if (s.equals("Signal"))
            return ImgType.PlateSig;
        else if (s.equals("Turnout"))
            return ImgType.PlateTo;
        else if (s.equals("Tile"))
            return ImgType.Tile;

        return ImgType.None;
    }

    public String typeToString (ImgType type)  {
        if (ImgType.Code == type)
            return "Code";
        else if (ImgType.Lamp == type)
            return "Lamp";
        else if (ImgType.Lever == type)
            return "Lever";
        else if (ImgType.PlateSig == type)
            return "Signal";
        else if (ImgType.PlateTo == type)
            return "Turnout";
        else if (ImgType.Tile == type)
            return "Tile";

        return "Unknown";
    }

    // ------------------------------------------------------------------------
    // constructor loads the configuration files and
    //   determines the initial display geometry

    public CtcPanel (
        String pnlFile,
        String ip )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        byte[]  buf     = new byte [10];

        addKeyListener   (this);
        addMouseListener (this);

        loadCfg ("Resources/ctcNumbered");
        tileWid = imgTile [0].img.getWidth (null);
        loadPnl (pnlFile);

    //  inventory();

        CANVAS_WIDTH  = tileWid * (2 + maxRowLen);

        // set up screen graphics
        colWid  = imgTo [0].img.getWidth (null);
        nCol    = CANVAS_WIDTH / colWid;
        rowHt1  = RowOff + imgTo  [0].img.getHeight (null);
        rowHt2  = rowHt1 + imgSig [0].img.getHeight (null);
        codeDia = imgCode [0].img.getWidth (null);

        this.setPreferredSize (new Dimension (CANVAS_WIDTH, CANVAS_HEIGHT));

        frame.setContentPane (this);
        frame.pack ();
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.setVisible (true);
        frame.setTitle   ("CTC Panel");

        // position app near top center of screen
        Rectangle r = frame.getBounds();        // window size
        frame.setBounds (900, 0, r.width, r.height);

        // open socket
        if (null != ip)  {
            sckt   = new Sckt ("127.0.0.1");
            buf[0] = 0;
            sckt.sendPkt (Sckt.PKT_START, buf, 1);
            scktEn = true;
        }

        // create timer
        TimerTask task = new TimerTask() {
            public void run() {
                update ();
            }
        };

        Timer timer = new Timer("Timer");
        timer.scheduleAtFixedRate (task, 0, 3000);  // 3 sec
    }

    // ------------------------------------------------------------------------
    private void update ()
    {
     // System.out.format ("update\n");

        // process code button
        for (int col = 1; col <= nCol; col++)  {
            if (null != symCode [col] && 'p' == symCode [col].cond)  {
                symCode [col].cond = ' ';

                int     num = 2 * col - 1;
                System.out.format ("update: col %d, num %d\n", col, num);

                // turnout
                CtcCol  ctc = ctcCol [num];
                if (null != ctc)  {
                    if (0 == ctc.pos)  {
                        if ('l' != ctc.symLvr.cond) {
                            ctc.symLvr.cond = 'l';
                            System.out.format (
                                "update: pos %d, cond %c %s\n",
                                    ctc.pos, ctc.symLvr.cond, ctc.symLvr.lbl);

                            PnlSym sym = ctc.symTo;
                            if (null == ctc.symTo)
                                System.out.format ( "Error update: sym null\n");


                            for ( ; null != sym; sym = sym.nxtSym)  {
                                sym.cond  = 'N';
                                System.out.format (
                                    "update: cond %c %s\n", sym.cond, sym.lbl);
                            }
                        }
                    }
                    else if ('r' != ctc.symLvr.cond) {
                        ctc.symLvr.cond = 'r';
                            System.out.format (
                                "update: pos %d, cond %c %s\n",
                                    ctc.pos, ctc.symLvr.cond, ctc.symLvr.lbl);

                        PnlSym sym = ctc.symTo;
                        for ( ; null != sym; sym = sym.nxtSym)
                            sym.cond  = 'R';
                    }
                }

                // signal 
                ctc = ctcCol [num+1];
                if (null != ctc)  {
                    if (0 == ctc.pos)
                        ctc.symLvr.cond = 'l';
                    else if (1 == ctc.pos)
                        ctc.symLvr.cond = 'r';
                    else
                        ctc.symLvr.cond = 'c';

                    ctc.sig = ctc.pos;
                    System.out.format ("update: col %d, pos %d, cond %c %s\n",
                                col, ctc.pos, ctc.symLvr.cond, ctc.symLvr.lbl);
                }
            }
        }

        ruleCheck ();
        repaint ();
    }

    // ------------------------------------------------------------------------
    // main processes command line arguments settign the debug level and

    public static void main (String[] args)
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        String ip = null;
        int    i;

        for (i = 0; i < args.length; i++) {
            System.out.format ("main: %d %s\n", i, args[i]);

            if (args[i].startsWith("-ip"))  {
                ip = args[i].substring(3);
                System.out.format ("main: ip %s\n", ip);
            }
            else if (args[i].startsWith("-"))  {
                throw new IllegalArgumentException (
                            "unknown option - " + args[i]);
            }
            else
                break;
        }

        CtcPanel disp = new CtcPanel (args [i], ip);
    }

    // ------------------------------------------------------------------------
    // process mouse press, search for element closest to mouse location

    public void mousePressed  (MouseEvent ev)
    {
        leverAdjust (ev.getX(), ev.getY());
    }

    // ------------------------------------------------------------------------
    // ignore these mouse events
    public void mouseClicked  (MouseEvent e) { }
    public void mouseEntered  (MouseEvent e) { }
    public void mouseExited   (MouseEvent e) { }
    public void mouseReleased (MouseEvent e) { }

    // --------------------------------
    public void keyReleased (KeyEvent e) { }
    public void keyPressed  (KeyEvent e)
    {
        int     code    = e.getKeyCode();
        switch (code)  {
            case KeyEvent.VK_LEFT:
                break;

            case KeyEvent.VK_RIGHT:
                break;

            default:
                break;      // ignore char keypresses belwo
        }
    }

    // --------------------------------
    public void keyTyped    (KeyEvent e)
    {
        byte[]  buf     = new byte [10];

        char    c = e.getKeyChar();
        switch (c)  {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
                break;

            case 'r':
                repaint();
                break;

            case '?':
                System.out.format ("key commands:\n");
                System.out.format ("    [0-9] show tower\n");
                System.out.format ("    r - repaint\n");
                System.out.format ("   -> - + tower (right arrow)\n");
                System.out.format ("   <- - - tower (left arrow)\n");
                System.out.format ("    ? - this help display\n");
                break;

            default:
                System.out.format ("keyTyped: %c unexpected\n", c);
                break;
        }
    }

    // ------------------------------------------------------------------------
    // open and loads a bitmap file

    private void loadImage (String imgFileName, ImgType type, int id)
        throws IllegalArgumentException
    {
        if (0 != dbg)
            System.out.format ("      loadImage:  %s %8s %2d\n",
                    imgFileName, typeToString (type), id);

        try {
            File   inFile  = new File (imgFileName);
            switch (type) {
            case Code:
                imgCode [imgCodeSize]     = new Img     ();
                imgCode [imgCodeSize].img = ImageIO.read (inFile);
                imgCodeSize++;
                break;

            case Lamp:
                imgLamp [imgLampSize]     = new Img     ();
                imgLamp [imgLampSize].img = ImageIO.read (inFile);
                imgLampSize++;
                break;

            case Lever:
                imgLvr [imgLvrSize]       = new Img    ();
                imgLvr [imgLvrSize].img   = ImageIO.read (inFile);
                imgLvrSize++;
                break;

            case PlateSig:
                imgSig [imgSigSize]       = new Img    ();
                imgSig [imgSigSize].img   = ImageIO.read (inFile);
                imgSigSize++;
                break;

            case PlateTo:
                imgTo [imgToSize]        = new Img   ();
                imgTo [imgToSize].img    = ImageIO.read (inFile);

                if (0 != dbg)
                    System.out.format ("     loadImage: wid %4d\n",
                            imgTo [imgToSize].img.getWidth (null));

                imgToSize++;
                break;

            case Tile:
                imgTile [imgTileSize]        = new Img   ();
                imgTile [imgTileSize].img    = ImageIO.read (inFile);

                if (0 != dbg)
                    System.out.format ("     loadImage: wid %4d\n",
                            imgTile [imgTileSize].img.getWidth (null));

                imgTileSize++;
                break;

            default:
                System.out.format ("loadImage: Error unknown type\n");
                System.exit (1);
                break;
            }
        } catch  (IOException ex) {
            ex.printStackTrace ();
        }
    }

    // ------------------------------------------------------------------------
    // reads a configuration file, processing different record types
    //   recursively read additonal files, create new Bmp and Elem data

    private void loadCfg (String configuration)
            throws FileNotFoundException, IOException
    {
        System.out.format (" loadCfg: %s\n", configuration);

        BufferedReader br = new BufferedReader(new FileReader(configuration + ".cfg"));
        String         line;

        while ((line = br.readLine()) != null)  {
            String[]    fields = line.split("  *");
            if (0 != dbg)
                System.out.format ("   loadCfg: %s - %s\n", line, fields [0]);

            // -----------------------------------------------------------------
            // load icon

            if (fields[0].equals("icon") || fields[0].equals("bg"))  {
                if (fields.length < 4)  {
                    throw new IllegalArgumentException (
                        "Error - loadCfg: icon type id  <filename>\n");
                }

                int     id   = Integer.parseInt(fields[2]);
                ImgType type = stringToType (fields[1]);

                loadImage (fields[3], type, id);
            }
            // -----------------------------------------------------------------
            // recursively process additional files

            else if (fields[0].equals("include"))  {
                if (fields.length < 2)  {
                    throw new IllegalArgumentException (
                            "Error - loadCfg: include <filename>\n");
                }

                loadCfg (fields[1]);
            }
        }
    }

    // ------------------------------------------------------------------------
    //   load panel decription

    private void loadPnl (String pnlFile)
            throws FileNotFoundException, IOException
    {
        System.out.format (" loadPnl: %s\n", pnlFile);

        BufferedReader br = new BufferedReader(new FileReader(pnlFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            String[]    fld = line.split("  *");
            if (0 != dbg)
                System.out.format ("   loadPnl: %s - %s\n", line, fld [0]);

            // -----------------------------------
            if (fld[0].equals("row"))  {
                pnlRow [nPnlRow++] = line.substring (4);

                int len = line.substring (4).length();
                if (maxRowLen < len)
                    maxRowLen = len;
            }

            // -----------------------------------
            else if (fld[0].equals("ctc"))  {
                String[]    sField = fld[1].split(",");

                System.out.format (" loadPnl: %s\n", line);

                for (int i = 0; i < sField.length; i++)  {
                    int num = Integer.parseInt(sField[i]);
                    ctcCol [num]         = new CtcCol ();
                    ctcCol [num].symLvr  = symLvr [symLvrSize++]
                                         = new PnlSym ("L", num);

                    System.out.format ("  loadPnl: num %d\n", num);

                    if (1 == (num % 2))  {
                        ctcCol [num].pos         = ctcCol [num].to  = 0;
                        ctcCol [num].symLvr.cond = 'l';

                    }
                    else  {
                        ctcCol [num].pos         = ctcCol [num].sig = 2;
                        ctcCol [num].symLvr.cond = 'c';
                    }

                    if (true)
                        System.out.format ("   loadPnl: ctc num %2d %s\n",
                                        num, ctcCol [num].symLvr.lbl);

                    int col = 1 + (num-1) / 2;
                    if (null == symCode [col])  {
                        System.out.format (" loadPnl: code %d\n", col);
                        symCode [col] = new PnlSym ("C", col);
                    }
                }
            }

            // -----------------------------------
            else if (fld[0].equals("signal"))  {
                int ctcCol = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);
                int imgIdx = (int) pnlRow [row].charAt(col) - '0';

                symSig [symSigSize++]     = new PnlSym (
                        ctcCol, row, col, fld [4], 's', imgIdx);
            }

            // -----------------------------------
            else if (fld[0].equals("turnout"))  {
                int ctcCol = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);
                int imgIdx = (int) pnlRow [row].charAt(col) - '0';

                symTo [symToSize++]     = new PnlSym (
                        ctcCol, row, col, fld [4], 's', imgIdx);
            }

            // -----------------------------------
            else if (fld[0].equals("rule"))  {
             // System.out.format ("  loadPnl: %s\n", line);
                ruleNew (fld);
            }

            // -----------------------------------
            else if (fld[0].equals("#"))
                ;           // ignore

            // -----------------------------------
            else if (0 < line.length()) {
                System.out.format ("loadPnl: ERROR - %s\n", line);
            }
        }

        linkLevers ();
    }

    // ------------------------------------------------------------------------
    private void linkLevers ()
    {
        System.out.format ("linkLevers:\n");

        for (int num = 0; num < CtcColMax; num++)  {
            if (ctcCol [num] == null)
                continue;

            if (true)
                System.out.format (" linkLevers: plate %2d\n", num);

            if (1 == (num % 2)) {       // TO plate
                // turnouts
                for (int i = 0; i < symToSize; i++)     // ?????????????
                    if (symTo [i].ctcCol == num)  {
                        if (ctcCol [num].symTo == null)
                            ctcCol [num].symTo = symTo [i];
                        else
                            ctcCol [num].symTo.nxtSym = symTo [i];
    
                        if (true)
                            System.out.format (
                                "  linkLevers: TO %2d, num %d, row %d\n",
                                    i, symTo [i].col, symTo [i].row);
    
                        PnlSym sym = symTo [i];
                        sym.tile = (int) pnlRow [sym.row].charAt(sym.col) - '0';
    
                        symTo [i].cond = 'N';
    
                        switch (sym.tile)  {
                        case 8:     // DL
                            sym.xLbl =  tileWid * 2/4;
                            sym.yLbl =  tileWid * 7/4;
                            break;
                        case 9:     // DR
                            sym.xLbl =  tileWid * 6/4;
                            sym.yLbl =  tileWid * 5/4;
                            break;
                        case 10:    // UL
                            sym.xLbl = -tileWid * 7/4;
                            sym.yLbl =  tileWid * 1/4;
                            break;
                        case 11:    // UR
                            sym.xLbl =  tileWid * 6/4;
                            sym.yLbl =  tileWid * 1/4;
                            break;
                        default:    // flat
                            sym.xLbl =  tileWid * 8/4;
                            sym.yLbl =  0;
                            break;
                        };
    
                        if (false)
                            System.out.format (
                            "   linkLevers: TO %d, tile %2d, %3d x %3d\n",
                                i, sym.tile, sym.xLbl, sym.yLbl);
                    }
            }

            else
            // signals
            for (int i = 0; i < symSigSize; i++)  {
                PnlSym sym = symSig [i];
                if (sym.ctcCol == num)  {
                    // left
                    if (sym.lbl.contains ("L"))  {
                        if (ctcCol [num].symSigL == null)
                            ctcCol [num].symSigL = sym;
                        else  {
                            sym.nxtSym          = ctcCol [num].symSigL;
                            ctcCol [num].symSigL = sym;
                        }
                        if (false)
                            System.out.format (
                                "  linkLevers: num %d, sig %d  %s\n",
                                                    num, i, sym.lbl);
                    }
                    // right
                    else if (sym.lbl.contains ("R"))  {
                        if (ctcCol [num].symSigR == null)
                            ctcCol [num].symSigR = sym;
                        else  {
                            sym.nxtSym          = ctcCol [num].symSigR;
                            ctcCol [num].symSigR = sym;
                        }
                        if (false)
                            System.out.format (
                                "  linkLevers: num %d, sig %d  %s\n",
                                                    num, i, sym.lbl);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // change position of lever based on mouse press
    private void leverAdjust (
        int  x,
        int  y )
    {
     // System.out.format ("leverAdjust: %3d x %3d\n", x, y);

        int col = x / colWid;
        int dX  = x % colWid;
        int num = 0;

        int pktType = 0;
        int pos     = 0;

        if (y < RowOff)
            return;

        if (y < (rowHt1))  {        // to lever
            num = 2 * col + 1;
            System.out.format ("leverAdjust: x %d, col %d, num %d\n",
                                                x, col, num);
            if (null == ctcCol [num])
                return;

            if ((dX < colWid / 2))
                ctcCol [num].pos = LvrLeft;
            else
                ctcCol [num].pos = LvrRight;
        }

        else if (y < (rowHt2))  {   // signal lever
            num = (2 * col) + 2;
            if (null == ctcCol [num])
                return;

            if (dX < (colWid / 3))
                ctcCol [num].pos = LvrLeft;
            else if (dX < (colWid * 2 / 3))
                ctcCol [num].pos = LvrCenter;
            else
                ctcCol [num].pos = LvrRight;
        }

        // code button
        else if (CodeXoff <= dX && dX < (CodeXoff + codeDia))  {
         // System.out.format (" leverAdjust: code col %2d\n", col);

            if (null != symCode [col+1])  {
                symCode [col+1].cond = 'p';
                if (false)
                    System.out.format (" leverAdjust: col %2d, lbl %s, %c\n",
                        col, symLvr [col+1].lbl, symCode [col+1].cond);
            }
        }

        else
            return;

        if (false)
            System.out.format (
            " leverAdjust: (%3d, %3d), col %d, dX %2d, | %d\n",
                x, y, col, dX, colWid/2);

        repaint ();
    }

    // ------------------------------------------------------------------------
    private void dispSymList (
        int    ctcCol,
        String type,
        PnlSym sym )
    {
        for ( ; null != sym; sym = sym.nxtSym)  {
            System.out.format ("  dispSymList: %2d %-4s %4s %c\n",
                                        ctcCol, type, sym.lbl, sym.cond);
            for (int i = 0; i < sym.ruleSize; i++)  {
                System.out.format ("    dispSymList: rules %d -", i);
                Rule rule = sym.rule [i];
                for ( ; null != rule; rule = rule.nxt)
                    System.out.format (", %c %-5s", rule.cond, rule.sym.lbl);
                System.out.format ("\n");
            }
        }
    }

    // --------------------------------
    private void inventory ()
    {
        System.out.format ("inventory:\n");

        for (int i = 0; i < CtcColMax; i++) {
            CtcCol ctc = ctcCol [i];
            if (null == ctc)
                continue;

            dispSymList (i, "to",   ctc.symTo);
            dispSymList (i, "sigL", ctc.symSigL);
            dispSymList (i, "sigR", ctc.symSigR);
        }
    }

    // ------------------------------------------------------------------------
    PnlSym symFind (
        String lbl )
    {
        for (int i = 0; i < symLvrSize; i++)
            if (symLvr [i].lbl.equals(lbl))
                return symLvr [i];

        for (int i = 0; i < symSigSize; i++)
            if (symSig [i].lbl.equals(lbl))
                return symSig [i];

        for (int i = 0; i < symToSize; i++)
            if (symTo [i].lbl.equals(lbl))
                return symTo [i];

        System.out.format ("ERROR symFind: %s not found\n", lbl);
        System.exit (4);

        return null;
    }

    // ------------------------------------------------------------------------
    private void ruleChainCheck (
        PnlSym[]  sym,
        int       symSize )
    {
        boolean dbg   = false;
        for (int i = 0; i < symSize; i++)  {
            if (0 == sym [i].ruleSize)
                continue;

            if (dbg)
                System.out.format ("ruleChainCheck: %s\n", sym [i].lbl);

            for (int j = 0 ; j < sym [i].ruleSize; j++)  {
                boolean match = true;
                Rule rule = sym [i].rule [j];

                if (dbg)
                    System.out.format ("  ruleChainCheck: %4s %2d",
                                                    sym [i].lbl, j);

                for ( ; null != rule; rule = rule.nxt)  {
                    char c = '!';
                    if (rule.sym.cond == rule.cond)
                        c = '_';
                    if ('!' == c)
                        match = false;

                    if (dbg)  {
                        System.out.format ("   %c %c %-4s",
                            c, rule.cond, rule.sym.lbl);
                        if (match)
                            System.out.format ("m");
                        else
                            System.out.format ("n");
                    }
                }

                if (dbg)
                    System.out.println ();

                sym [i].cond = ' ';
                if (match)  {
                    sym [i].cond = 'C';
                    System.out.format ("ruleChainCheck: %c %s match\n",
                                                  sym [i].cond, sym [i].lbl);
                    break;      // check no additional rules
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    private void ruleCheck ()
    {
        System.out.println ("\nrulesCheck:");
        ruleChainCheck (symSig, symSigSize);
        ruleChainCheck (symTo,  symToSize);
    }

    // ------------------------------------------------------------------------
    private void ruleNew (
        String fld [] )
    {
        PnlSym sym      = symFind (fld [1]);
        System.out.format ("   ruleNew: sym %s\n", sym.lbl);

        Rule rule0   = null;

        for (int i = 2; i < fld.length; i++)  {
            Rule rule   = new Rule ();
            rule.sym    = symFind (fld [i].substring(1));
            rule.cond   = fld [i].charAt(0);
            rule.nxt    = rule0;
            rule0       = rule;

            System.out.format ("    ruleNew: %c %s\n", rule.cond, rule.sym.lbl);
        }

        sym.rule [sym.ruleSize++] = rule0;


        System.out.format ("     ruleNew: %s - ", sym.lbl);
        for  ( ; null != rule0; rule0 = rule0.nxt)
            System.out.format ("   %c %s", rule0.cond, rule0.sym.lbl);
        System.out.format ("\n");
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    private void paintToPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        int         col,
        int         lvrIdx )
    {
        if (0 != dbg)
            System.out.format ("  paintToPlate: %3d %3d, %d\n", x0, y0, lvrIdx);

        final int  TrackH  = 2;

        g2d.setColor (Color.white);

        CtcCol ctc  = ctcCol [col];
        PnlSym lvr  = ctcCol [col].symLvr;
        PnlSym to   = ctcCol [col].symTo;

        // label turnouts
        for ( ; to != null; to = to.nxtSym)
            g2d.drawString (to.lbl, to.x + to.xLbl, to.y + to.yLbl);

        // plate & lever
        g2d.drawImage (imgTo  [col/2].img,   x0, y0, this);
        g2d.drawImage (imgLvr [ctc.pos].img, x0 + 6, y0 + 44, this);

        to   = ctcCol [col].symTo;

        // lamps
     // if (LvrLeft == lvrIdx)  {
        if ('l' == lvr.cond)  {
            g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 3, this);
            g2d.drawImage (imgLamp [0].img,  x0 + 34, y0 + 4, this);

          if (false)
            while (to != null)  {
             // System.out.format (" pntTo: %s\n", to.lbl);
                g2d.drawImage (imgTile [TrackH].img,  to.x, to.y, this);
                to = to.nxtSym;
            }
        }
        else  {
            g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 3, this);
            g2d.drawImage (imgLamp [1].img,  x0 + 34, y0 + 4, this);
        }
    }

    // ------------------------------------------------------------------------
    private void paintSigPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        int         col,
        int         lvrIdx )
    {
        if (0 != dbg)
            System.out.format ("  paintSigPlate: %3d %3d, %d\n", x0, y0, lvrIdx);

        final int  SigRred = 16;
        final int  SigLred = 17;
        final int  SigRgr  = 46;
        final int  SigLgr  = 47;

        g2d.setColor (Color.white);

        CtcCol ctc  = ctcCol [col];
        PnlSym lvr  = ctcCol [col].symLvr;
        PnlSym symL = ctcCol [col].symSigL;
        PnlSym symR = ctcCol [col].symSigR;

        int    xOff = tileWid * 5/4;
        int    yOff = tileWid * 3/4;

        // set all signals to stop
      if (true)  {
        while (symL != null)  {
 //         g2d.drawImage  (imgTile [SigLred].img,  symL.x, symL.y, this);
            g2d.drawString (symL.lbl, symL.x + xOff, symL.y + yOff);
            symL = symL.nxtSym;
        }
        symL = ctcCol [col].symSigL;

        while (symR != null)  {
 //         g2d.drawImage (imgTile [SigRred].img,  symR.x, symR.y, this);

            int xOff2 = 5 + g2d.getFontMetrics().stringWidth (symR.lbl);
            g2d.drawString (symR.lbl, symR.x - xOff2, symR.y + yOff);
            symR = symR.nxtSym;
        }
        symR = ctcCol [col].symSigR;
      }

        // plate & lever
 //     System.out.format (" paintSigPlate: plate %d\n", col/2);

        g2d.drawImage (imgSig [col/2].img,   x0, y0, this);
        g2d.drawImage (imgLvr [ctc.pos].img, x0 + 5, y0 + 57, this);

        // lamps
        if (0 == lvrIdx && symL != null)  {
 //         g2d.drawImage (imgTile [SigLgr].img, symL.x, symL.y, this);

            g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 17, this);
            g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, this);
            g2d.drawImage (imgLamp [5].img,  x0 + 34, y0 + 18, this);
        }
        else if (1 == lvrIdx && symR != null)  { // right
 //         g2d.drawImage (imgTile [SigRgr].img, symR.x, symR.y, this);

            g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 17, this);
            g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, this);
            g2d.drawImage (imgLamp [6].img,  x0 + 34, y0 + 18, this);
        }
        else  { // center/vertical
            g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 17, this);
            g2d.drawImage (imgLamp [10].img, x0 + 18, y0 +  6, this);
            g2d.drawImage (imgLamp [5].img,  x0 + 34, y0 + 18, this);
        }
    }

    // ------------------------------------------------------------------------
    private void paintCtcPlates (
        Graphics2D  g2d,
        AffineTransform transform )
    {
        if (0 != dbg)
            System.out.format ("paintGrid:\n");

        Rectangle   r      = frame.getBounds();
        int         y0     = RowOff;
        int         y1     = y0 + imgTo  [0].img.getHeight (null);
        int         y2     = y1 + imgSig [0].img.getHeight (null);

        g2d.setColor (new Color(115, 104, 50));  // #736832
        g2d.fillRect (0, y0, r.width, 50 + y2 - y0);

        for (int num = 1; num < ColMax; num++)  {
            int col = 1 + (num-1) / 2;
            int x0  = colWid * ((num-1) / 2);

            // code button
            if (null != symCode [col])  {
                Image img = imgCode [0].img;
                if (' ' != symCode [col].cond)
                    img  = imgCode [1].img;
                g2d.drawImage (img, x0 + CodeXoff, y2 + CodeYoff, this);
            }

            // to or signal
            if (ctcCol [num] == null)
                continue;

 //         System.out.format (" paintCtcPlate: num %d, num/2 %d, x0 %d\n",
 //                                                     num, num/2, x0);
            if (1 == (num % 2))
                paintToPlate  (g2d, x0, y0, num, ctcCol [num].to);
            else
                paintSigPlate (g2d, x0, y1, num, ctcCol [num].sig);
        }
    }

    // ------------------------------------------------------------------------
    private void paintTrack (
        Graphics2D  g2d,
        int         screenWid,
        int         screenHt )
    {
        if (0 != dbg)
            System.out.format ("paintTrack:\n");

        for (int row = 0; row < 8; row++)  {
         // System.out.format ("  paintTrack: %d %s\n", row, pnlRow [row]);

            for (int i = 0; i < pnlRow [row].length(); i++)  {
                int  x0   = tileWid * i;
                int  y0   = tileWid * row;
                char c   = pnlRow [row].charAt(i);
                int  idx = 0;
                if (' ' != c)
                    idx    = (int) c - '0';
           //   System.out.format ("  %2d, %c  %d\n", i, c, idx);

                if (76 < idx)
                    System.out.format ("paintTrack: ERROR idx %d > 76\n", idx);
                else
                    g2d.drawImage (imgTile [idx].img, x0, y0, this);
            }
        }

        // set turnouts -- are by default reversed
        for (int i = 0; i < symToSize; i++)  {
            final int  TrackH  = 2;
            PnlSym to = symTo [i];
         // System.out.format (" paintTrack: %c %s\n", to.cond, to.lbl);
            if ('N' == to.cond)
                g2d.drawImage (imgTile [TrackH].img, to.x, to.y, this);
        }

        // set signals -- are by default stop
        for (int i = 0; i < symSigSize; i++)  {
            final int  TrackH  = 2;
            PnlSym sym    = symSig [i];
            int    imgIdx = sym.imgIdx;
         // System.out.format (" paintTrack: %c %s\n", to.cond, to.lbl);
            if ('C' == sym.cond)  {
                imgIdx += 30;
 //             System.out.format ("  paintTrack: %d x %d, %c %s\n",
 //                     sym.x, sym.y, sym.cond, sym.lbl);
            }
            g2d.drawImage (imgTile [imgIdx].img, sym.x, sym.y, this);
        }
    }

    // ------------------------------------------------------------------------
    // redraw the screen -- recalculate the scale factors, redraw the
    //   background bitmaps and call paintElems() to redraw features

    @Override
    public void paintComponent (Graphics g)
    {
        Graphics2D  g2d = (Graphics2D) g;
        Rectangle   r   = frame.getBounds();

        // set background to black
     // g2d.setColor (Color.black);
     // g2d.setColor (new Color.parseColor("#316b35"));

        // -----------------------------------------------
        AffineTransform transform = new AffineTransform ();

        int         x0  = 0;
        int         y0  = 0;
        int         x;
        int         y;

        g2d.setColor (Color.black);
        g2d.fillRect (0, 0, r.width, r.height);

        paintTrack     (g2d, r.width, r.height);
        paintCtcPlates (g2d, transform);
    }
}
