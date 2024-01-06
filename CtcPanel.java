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


@SuppressWarnings ("serial")

// -----------------------------------------------------------------------------
// -----------------------------------------------------------------------------
// display railroad schematic and USS switch & signal levers

public class CtcPanel extends JPanel
        implements MouseListener, KeyListener
{
    // ---------------------------------------------------------
    class Img   {
        Image   img;
    }

    // ---------------------------------------------------------
    class Rule   {
        PnlSym  sym;
        char    cond;
        Rule    nxt;
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

        // -------------------------------------
        public PnlSym (
            String  ctcCol_,
            String  row_,
            String  col_,
            String  lbl_ )
        {
            ctcCol  = Integer.parseInt (ctcCol_);
            row     = Integer.parseInt (row_);
            col     = Integer.parseInt (col_);
            lbl     = lbl_;

            x       = tileWid * col;
            y       = tileWid * row;

         // System.out.format (" PnlSym: %3dx%3d  %s\n", x, y, lbl);
        }
    }

    // ---------------------------------------------------------
    class CtcCol {
        int     to;
        int     sig;
        boolean code;

        PnlSym  toSym;
        PnlSym  sigLsym;
        PnlSym  sigRsym;

        // -------------------------------------
        CtcCol () {
            to = sig = -1;
        }
    };

    // ---------------------------------------------------------
    JFrame frame = new JFrame ();

    private static final String Title           = "Pacific Southern Railway";

    final int       CANVAS_WIDTH    = 800;
    final int       CANVAS_HEIGHT   = 400;

    private int     dbg             = 0;

    String[]        pnlRow          = new String[10];
    int             nPnlRow         = 0;

    final int       CtcColMax       = 20;
    CtcCol          ctcCol []       = new CtcCol [CtcColMax];

    boolean         ctcSw  []       = new boolean [20];
    boolean         ctcSig []       = new boolean [20];

    final int       MaxPnlSym       = 20;
    PnlSym          symTo  []       = new PnlSym  [MaxPnlSym];
    int             symToSize       = 0;

    PnlSym          symSig []       = new PnlSym  [MaxPnlSym];
    int             symSigSize      = 0;

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

    boolean         codeBut []       = new boolean [ColMax];

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
        linkLevers ();
        inventory();


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
    }

    // --------------------------------
    public void keyReleased (KeyEvent e) { }
    public void keyPressed (KeyEvent e)
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
    public void keyTyped (KeyEvent e)
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
    private void dispSymList (
        int    ctcCol,
        String type,
        PnlSym sym )
    {
        for ( ; null != sym; sym = sym.nxtSym)  {
            System.out.format ("  dispSymList: %2d %-4s %4s\n",
                                        ctcCol, type, sym.lbl);
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

            dispSymList (i, "to",   ctc.toSym);
            dispSymList (i, "sigL", ctc.sigLsym);
            dispSymList (i, "sigR", ctc.sigRsym);
        }
    }

    // ------------------------------------------------------------------------
    PnlSym symFind (
        String lbl )
    {
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
    private void ruleNew (
        String fld [] )
    {
        PnlSym sym              = symFind (fld [1]);
        System.out.format ("   ruleNew: sym %s\n", sym.lbl);

     // sym.rule [sym.ruleSize] = new Rule ();
     // Rule rule0              = sym.rule [sym.ruleSize++];

        if (null == sym.rule [sym.ruleSize])  {
            System.out.format ("Error ruleNew - symRule null\n");
        }

        for (int i = 2; i < fld.length; i++)  {
         // rules [rulesSize] = new Rule ();
         // Rule rule         = rules [rulesSize++];
         // rule.nxt          = rule0;
         // rule0             = rule;

            Rule rule               = new Rule ();
            rule.sym                = symFind (fld [i].substring(1));
            rule.cond               = fld [i].charAt(0);
            rule.nxt                = sym.rule [sym.ruleSize];
            sym.rule [sym.ruleSize] = rule;

            System.out.format ("    ruleNew: %c %s\n", rule.cond, rule.sym.lbl);
        }

        System.out.format ("    ruleNew: ");
        System.out.format (" sym %s ", sym.lbl);
        if (null != sym.rule [0])  {
            if (null != sym.rule [0].sym)
                System.out.format (", rule [0].sym %s", sym.rule [0].sym.lbl);
            else
                System.out.format (", rule [0].sym null");

            if (null == sym.rule [0].nxt)
                System.out.format (", rule [0].nxt null");
        }
        else
            System.out.format (", rule [0] null");
        System.out.format ("\n");

        sym.ruleSize++;
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
            }

            // -----------------------------------
            else if (fld[0].equals("ctcSig"))  {
                String[]    sField = fld[1].split(",");

                for (int i = 0; i < sField.length; i++)  {
                    int idx = Integer.parseInt(sField[i]);
                    if (ctcCol [idx] == null)
                        ctcCol [idx] = new CtcCol ();
                    ctcCol [idx].sig = 2;
                }
            }

            // -----------------------------------
            else if (fld[0].equals("ctcTo"))  {
                String[]    sField = fld[1].split(",");

                for (int i = 0; i < sField.length; i++)  {
                    int idx = Integer.parseInt(sField[i]);
                    if (ctcCol [idx] == null)
                        ctcCol [idx] = new CtcCol ();
                    ctcCol [idx].to    = 0;
                }
            }

            // -----------------------------------
            else if (fld[0].equals("signal"))  {
                symSig [symSigSize++]     = new PnlSym (
                        fld [1], fld [2], fld [3], fld [4]);
            }

            // -----------------------------------
            else if (fld[0].equals("turnout"))  {
                symTo [symToSize++]     = new PnlSym (
                        fld [1], fld [2], fld [3], fld [4]);
            }

            // -----------------------------------
            else if (fld[0].equals("rule"))  {
                System.out.format ("  loadPnl: %s - %d\n", line, symSigSize);
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
    }

    // ------------------------------------------------------------------------
    private void linkLevers ()
    {
        for (int col = 0; col < CtcColMax; col++)  {
            if (ctcCol [col] == null)
                continue;

            if (false)
                System.out.format (" linkLevers: col %d\n", col);

            // turnouts
            for (int i = 0; i < symToSize; i++)
                if (symTo [i].ctcCol == col)  {
                    if (ctcCol [col].toSym == null)
                        ctcCol [col].toSym = symTo [i];
                    else
                        ctcCol [col].toSym.nxtSym = symTo [i];

                    PnlSym sym = symTo [i];
                    sym.tile = (int) pnlRow [sym.row].charAt(sym.col) - '0';

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

            // signals
            for (int i = 0; i < symSigSize; i++)  {
                PnlSym sym = symSig [i];
                if (sym.ctcCol == col)  {
                    // left
                    if (sym.lbl.contains ("L"))  {
                        if (ctcCol [col].sigLsym == null)
                            ctcCol [col].sigLsym = sym;
                        else  {
                            sym.nxtSym          = ctcCol [col].sigLsym;
                            ctcCol [col].sigLsym = sym;
                        }
                        if (false)
                            System.out.format (
                                "  linkLevers: col %d, sig %d  %s\n",
                                                    col, i, sym.lbl);
                    }
                    // right
                    else if (sym.lbl.contains ("R"))  {
                        if (ctcCol [col].sigRsym == null)
                            ctcCol [col].sigRsym = sym;
                        else  {
                            sym.nxtSym          = ctcCol [col].sigRsym;
                            ctcCol [col].sigRsym = sym;
                        }
                        if (false)
                            System.out.format (
                                "  linkLevers: col %d, sig %d  %s\n",
                                                    col, i, sym.lbl);
                    }
                }
            }
        }
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

    public void mousePressed (MouseEvent ev)
    {
        leverAdjust (ev.getX(), ev.getY());
    }

    // ------------------------------------------------------------------------
    // ignore these mouse events
    public void mouseClicked  (MouseEvent e) { }
    public void mouseEntered  (MouseEvent e) { }
    public void mouseExited   (MouseEvent e) { }
    public void mouseReleased (MouseEvent e) { }

    // ------------------------------------------------------------------------
    private void leverAdjust (
        int  x,
        int  y )
    {
     // System.out.format ("leverAdjust: %3d x %3d\n", x, y);

        int col = x / colWid;
        int dX  = x % colWid;

        int pktType = 0;
        int pos     = 0;

        if (ctcCol [col] == null)  {
            System.out.format (" leverAdjust: col %d null\n", col);
            return;
        }

        if (y < RowOff)
            return;

        if (y < (rowHt1))  {
            if ((dX < colWid / 2))
                ctcCol [col].to = LvrLeft;
            else
                ctcCol [col].to = LvrRight;

            pktType = Sckt.PKT_LVR_TO;
            pos     = ctcCol [col].to;
        }

        else if (y < (rowHt2))  {
            if (dX < (colWid / 3))
                ctcCol [col].sig = LvrLeft;
            else if (dX < (colWid * 2 / 3))
                ctcCol [col].sig = LvrCenter;
            else
                ctcCol [col].sig = LvrRight;

            pktType = Sckt.PKT_LVR_SIG;
            pos     = ctcCol [col].sig;
        }

        else if (CodeXoff <= dX && dX < (CodeXoff + codeDia))
            ctcCol [col].code = ! ctcCol [col].code;
        else
            return;

        if (0 != dbg)
            System.out.format (
            " leverAdjust: (%3d, %3d), col %d, dX %2d, | %d, sw %d, sig %d\n",
                x, y, col, dX, colWid/2, ctcCol [col].to, ctcCol [col].sig);

        repaint ();

        buf [0] = (byte) col;
        buf [1] = (byte) pos;

        System.out.format ("leverAdjust: type %d %02x %02x\n",
                                        pktType, buf [0], buf [1]);
        if (scktEn)
            sckt.sendPkt ((byte) pktType, buf, 2);
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
            System.out.format ("  paintPlate: %3d %3d, %d\n", x0, y0, lvrIdx);

        final int  TrackH  = 2;

        g2d.setColor (Color.white);

        PnlSym to   = ctcCol [col].toSym;

        // label turnouts
        while (to != null)  {
            g2d.drawString (to.lbl, to.x + to.xLbl, to.y + to.yLbl);
            to = to.nxtSym;
        }

        // lever
        g2d.drawImage (imgTo  [col].img, x0, y0, this);
        g2d.drawImage (imgLvr [lvrIdx].img, x0 + 6, y0 + 44, this);

        to   = ctcCol [col].toSym;

        // lamps
        if (LvrLeft == lvrIdx)  {
            g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 3, this);
            g2d.drawImage (imgLamp [0].img,  x0 + 34, y0 + 4, this);

            while (to != null)  {
             // System.out.format (" pntTo: %s\n", to.lbl);
                g2d.drawImage (imgTile [TrackH].img,  to.x, to.y, this);
                to = to.nxtSym;
            }
        }
        else  {
            g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 3, this);
            g2d.drawImage (imgLamp [1].img,  x0 + 34, y0 + 4, this);

            while (to != null)  {
                g2d.drawImage (imgTile [to.tile].img,  to.x, to.y, this);
                to = to.nxtSym;
            }
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
            System.out.format ("  paintPlate: %3d %3d, %d\n", x0, y0, lvrIdx);

        final int  SigRred = 16;
        final int  SigLred = 17;
        final int  SigRgr  = 46;
        final int  SigLgr  = 47;

        g2d.setColor (Color.white);

        PnlSym symL = ctcCol [col].sigLsym;
        PnlSym symR = ctcCol [col].sigRsym;
        int    xOff = tileWid * 5/4;
        int    yOff = tileWid * 3/4;

        // set all signals to stop
        while (symL != null)  {
            g2d.drawImage  (imgTile [SigLred].img,  symL.x, symL.y, this);
            g2d.drawString (symL.lbl, symL.x + xOff, symL.y + yOff);
            symL = symL.nxtSym;
        }
        symL = ctcCol [col].sigLsym;

        while (symR != null)  {
            g2d.drawImage (imgTile [SigRred].img,  symR.x, symR.y, this);

            int xOff2 = 5 + g2d.getFontMetrics().stringWidth (symR.lbl);
            g2d.drawString (symR.lbl, symR.x - xOff2, symR.y + yOff);
            symR = symR.nxtSym;
        }
        symR = ctcCol [col].sigRsym;

        // lever
        g2d.drawImage (imgSig [col].img, x0, y0, this);
        g2d.drawImage (imgLvr [lvrIdx].img, x0 + 5, y0 + 57, this);

        // lamps
        if (0 == lvrIdx && symL != null)  {
            g2d.drawImage (imgTile [SigLgr].img, symL.x, symL.y, this);

            g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 17, this);
            g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, this);
            g2d.drawImage (imgLamp [5].img,  x0 + 34, y0 + 18, this);
        }
        else if (1 == lvrIdx && symR != null)  { // right
            g2d.drawImage (imgTile [SigRgr].img, symR.x, symR.y, this);

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

     // g2d.setColor (new Color(49, 107, 53));   // CTC  green
        g2d.setColor (new Color(115, 104, 50));  // #736832
     // y1 += imgSig [0].img.getHeight (null);
        g2d.fillRect (0, y0, r.width, 50 + y2 - y0);

        for (int col = 0; col < nCol; col++)  {
            if (ctcCol [col] == null)
                continue;

            int x0 = col * colWid;

            if (0 <= ctcCol [col].to)
                paintToPlate  (g2d, x0, y0, col, ctcCol  [col].to);

            if (0 <= ctcCol [col].sig)
             // paintPlate (g2d, x0, y1, true,  col, sigPos [col]);
                paintSigPlate (g2d, x0, y1, col, ctcCol [col].sig);

            // code button
            Image img = imgCode [0].img;
            if (codeBut [col])
                img  = imgLamp [1].img;
            g2d.drawImage (img, x0 + CodeXoff, y2 + CodeYoff, this);
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
