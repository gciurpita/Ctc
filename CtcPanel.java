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
    class Blk {
        int     col;
        int     row;
        String  id;
        char    state;      // 'O'ccupied

        int     x;
        int     y;

        // -------------------------------------
        private Blk (
            String  id_,
            int     col_,
            int     row_ )
        {
            id    = id_;
            col   = col_;
            row   = row_;
            state = ' ';

            x     = tileWid * col;
            y     = tileWid * row;
        }

        // -------------------------------------
        public void tgl ()
        {
            if ('O' == state)
                state = ' ';
            else if ('C' == state)
                state = 'O';
            else
                state = 'C';
        }
    }

    // ---------------------------------------------------------
    class CtcCol {
        int     to;
        int     sig;
        int     pos;

        int     x0;
        int     y0;

        char    lamp;

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
        int     ctcNum;
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
        char    type;
     // char    state;
        int     lock;

        // -------------------------------------
        public PnlSym (
            String  prefix,
            int     plateNum )
        {
            ctcNum  = plateNum;
            cond    = ' ';
            lbl     = prefix + plateNum;

         // System.out.format (" PnlSym: %2d %c  %s\n", plateNum, cond, lbl);
        }

        // -------------------------------------
        public PnlSym (
            int     ctcNum_,
            int     row_,
            int     col_,
            String  lbl_,
            char    cond_,
            int     imgIdx_ )
        {
            ctcNum  = ctcNum_;
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
        boolean lock;
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

    final int       MaxBlk          = 20;
    Blk             blk []          = new Blk     [MaxBlk];
    int             blkSize         = 0;

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

    final int       TrackH  = 2;
    int             tileWid;

    final int       CodeXoff        = 15;
    final int       CodeYoff        =  5;
    int             codeDia;

    boolean         ruleFlag        = true;

    Sckt            sckt;
    boolean         scktEn           = false;
    byte[]          buf              = new byte [100];

    LayoutIf        layIf            = new LayoutIf ();

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

        inventory();

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
        timer.scheduleAtFixedRate (task, 0, 3000);  // 1 sec
    }

    // ------------------------------------------------------------------------
    private void setTrackTile (
        PnlSym  sym,
        int     tileIdx)
    {
        char c = (char)tileIdx;
        c += '0';
        pnlRow [sym.row] =  pnlRow [sym.row].substring (0, sym.col)  + c
                          + pnlRow [sym.row].substring (sym.col + 1);
        System.out.format ("setTrackTile: %d, '%c', \"%s\"\n",
            tileIdx, c, pnlRow [sym.row]);
    }

    // ------------------------------------------------------------------------
    private void update ()
    {
     // System.out.format ("update\n");

        // check response
        if (true) {
            Msg msg;
            while (null != (msg = layIf.receive ()))  {
                System.out.format ("update: receive %c %2d %c\n",
                    msg.type, msg.id, msg.state);

                int num =  msg.id;
                CtcCol ctc = ctcCol [num];
                PnlSym sym = ctcCol [num].symTo;

                if (null == sym)  {
                    System.out.format ( "Error update: receive - sym null\n");
                    System.exit (2);
                }

                if ('T' == msg.type)  {
                    if ('N' == msg.state)
                        ctc.symLvr.cond = 'l';
                    else
                        ctc.symLvr.cond = 'r';

                    // handle multiple turnouts (e.g. crossover)
                    for ( ; null != sym; sym = sym.nxtSym)  {
                        sym.cond  = msg.state;
                        if ('N' == msg.state)
                            setTrackTile (sym, TrackH);
                        else
                            setTrackTile (sym, sym.imgIdx);

                        System.out.format ( "update: receive - cond %c %s\n",
                                                            sym.cond, sym.lbl);
                    }
                }
            }
        }

        // process code button
        for (int col = 1; col <= nCol; col++)  {
            if (null != symCode [col] && 'p' == symCode [col].cond)  {
                symCode [col].cond = ' ';

                System.out.format ("\nupdate: col %d\n", col);

                // turnout
                int     num = 2 * col - 1;
                CtcCol  ctc = ctcCol [num];
                if (null != ctc)  {
                    System.out.format (
                        " update: num %d, pos %d, cond %c, lck %d  %s\n",
                            num, ctc.pos, ctc.symLvr.cond, ctc.symTo.lock,
                            ctc.symLvr.lbl);

                    if (0 != ctc.symTo.lock)  {
                        System.out.format (
                            "   update: turnout %s locked !!\n", ctc.symTo.lbl);
                    }

                    // --------------------------------------
                    else if (LvrLeft == ctc.pos)  {
                        if ('l' != ctc.symLvr.cond)
                            layIf.send ('T', num, 'N');
                    }

                    // --------------------------------------
                    else if ('r' != ctc.symLvr.cond) {
                        layIf.send ('T', num, 'R');
                    }
                }

                // --------------------------------------
                // signal
                num = num + 1;
                ctc = ctcCol [num];
                if (null != ctc)  {
                    System.out.format (
                        " update: num %d, pos %d, cond %c, lck %d  %s\n",
                            num, ctc.pos, ctc.symLvr.cond, ctc.symLvr.lock,
                            ctc.symLvr.lbl);

                    if (LvrCenter == ctc.pos)  {
                        ctc.symLvr.cond = 'c';
                 //  // ruleUnlock (ctc.symSigL);   // ???
                 //  // ruleUnlock (ctc.symSigR);   // ???
                    }
                    else if (0 != ctc.symLvr.lock) {
                 // if (0 != ctc.symLvr.lock) {
                        System.out.format (
                            "   update: sig lever %s locked\n",
                                ctc.symSigL.lbl);
                    }

                    // are these 2 checks needed ???
                    else if (null != ctc.symSigL && 0 != ctc.symSigL.lock) {
                        System.out.format ( "    update: signal %s locked!\n",
                                ctc.symSigL.lbl);
                    }
                    else if (null != ctc.symSigR && 0 != ctc.symSigR.lock) {
                        System.out.format ( "    update: signal %s locked!\n",
                                ctc.symSigR.lbl);
                    }

                    else if (LvrLeft == ctc.pos)  {
                        ctc.symLvr.cond = 'l';
                        System.out.format ( "    update: signal set cond %c\n",
                            ctc.symLvr.cond);
                    }
                    else if (LvrRight == ctc.pos)
                        ctc.symLvr.cond = 'r';

                    ctc.sig = ctc.pos;
                    System.out.format (
                        "   update: num %d, pos %d, cond %c %s\n",
                                num, ctc.pos, ctc.symLvr.cond, ctc.symLvr.lbl);
                }
            }
        }

        if (ruleFlag)
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
        mousePress (ev.getX(), ev.getY());
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

        repaint ();
    }

    // --------------------------------
    public void keyTyped    (KeyEvent e)
    {
        byte[]  buf     = new byte [10];

        char    c = e.getKeyChar();
        System.out.format ("keyTyeped: %c\n", c);

        switch (c)  {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
                break;

            case 'r':
                ruleFlag = ! ruleFlag;
                System.out.format ("keyTyeped: r %d\n", ruleFlag);
                break;

            case '?':
                System.out.format ("key commands:\n");
                System.out.format ("    [0-9] show tower\n");
                System.out.format ("    r - pause ruleCheck\n");
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
        boolean dbg = false;
        System.out.format (" loadPnl: %s\n", pnlFile);

        BufferedReader br = new BufferedReader(new FileReader(pnlFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            String[]    fld = line.split("  *");
            if (dbg)
                System.out.format ("   loadPnl: %s - %s\n", line, fld [0]);

            // -----------------------------------
            if (fld[0].equals("row"))  {
                pnlRow [nPnlRow++] = line.substring (4);

                int len = line.substring (4).length();
                if (maxRowLen < len)
                    maxRowLen = len;
            }

            // -----------------------------------
            else if (fld[0].equals("block"))  {
                if (MaxBlk == blkSize)  {
                    System.out.format ("Error - loadPnl - MaxBlk exceeded\n");
                    System.exit (1);
                }

             // int id     = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);
                blk [blkSize++]   = new Blk (fld [1], col, row);
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

                if (nPnlRow <= row) {
                    System.out.format ("loadPnl: row out of range, %d\n", row);
                    System.exit(10);
                }

                if (pnlRow [row].length() <= col) {
                    System.out.format ("loadPnl: col out of range, %d\n", col);
                    System.exit(10);
                }

                int imgIdx = (int) pnlRow [row].charAt(col) - '0';

                symSig [symSigSize++]     = new PnlSym (
                        ctcCol, row, col, fld [4], '_', imgIdx);
            }

            // -----------------------------------
            else if (fld[0].equals("turnout"))  {
                int ctcCol = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);

                if (nPnlRow <= row) {
                    System.out.format ("loadPnl: row out of range, %d\n", row);
                    System.exit(10);
                }

                if (pnlRow [row].length() <= col) {
                    System.out.format ("loadPnl: col out of range, %d\n", col);
                    System.exit(10);
                }

                int imgIdx = (int) pnlRow [row].charAt(col) - '0';

                symTo [symToSize++]     = new PnlSym (
                        ctcCol, row, col, fld [4], 's', imgIdx);

                setTrackTile (symTo [symToSize-1], TrackH);
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
        boolean dbg = false;

        System.out.format ("\nlinkLevers:\n");

        for (int num = 0; num < CtcColMax; num++)  {
            if (ctcCol [num] == null)
                continue;

            if (dbg)
                System.out.format (" linkLevers: plate %2d\n", num);

            ctcCol [num].x0 = ((num-1) / 2) * imgTo [0].img.getWidth (null);

            if (1 == (num % 2)) {       // TO plate
                // turnouts
                for (int i = 0; i < symToSize; i++)     // ?????????????
                    if (symTo [i].ctcNum == num)  {
                        symTo [i].type  = 'T';
                        ctcCol [num].y0 = RowOff;

                        if (ctcCol [num].symTo == null)
                            ctcCol [num].symTo = symTo [i];
                        else
                            ctcCol [num].symTo.nxtSym = symTo [i];

                        if (dbg)
                            System.out.format (
                                "  linkLevers: TO %2d, num %d, row %d\n",
                                    i, symTo [i].col, symTo [i].row);

                        PnlSym sym = symTo [i];
                        sym.tile = (int) pnlRow [sym.row].charAt(sym.col) - '0';

                        symTo [i].cond = 'N';

                        switch (sym.imgIdx)  {
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

                        if (dbg)
                            System.out.format (
                            "   linkLevers: TO %d, tile %2d, %3d x %3d\n",
                                i, sym.tile, sym.xLbl, sym.yLbl);
                    }
            }

            else
            // signals
            for (int i = 0; i < symSigSize; i++)  {
                PnlSym sym = symSig [i];

                if (dbg)
                    System.out.format (
                        "    linkLevers: num %d, [%d] %d %s\n",
                            num, i, sym.ctcNum, sym.lbl);

                if (sym.ctcNum == num)  {
                    ctcCol [num].y0 = RowOff + imgTo  [0].img.getHeight (null);

                    // left
                    if (sym.lbl.contains ("L"))  {
                        sym.type = 'L';
                        if (ctcCol [num].symSigL == null)
                            ctcCol [num].symSigL = sym;
                        else  {
                            sym.nxtSym          = ctcCol [num].symSigL;
                            ctcCol [num].symSigL = sym;
                        }
                        if (dbg)
                            System.out.format (
                                "      linkLevers: num %d, sigL %d  %s\n",
                                                    num, i, sym.lbl);
                    }
                    // right
                    else if (sym.lbl.contains ("R"))  {
                        sym.type = 'R';
                        if (ctcCol [num].symSigR == null)
                            ctcCol [num].symSigR = sym;
                        else  {
                            sym.nxtSym          = ctcCol [num].symSigR;
                            ctcCol [num].symSigR = sym;
                        }
                        if (dbg)
                            System.out.format (
                                "      linkLevers: num %d, sigR %d  %s\n",
                                                    num, i, sym.lbl);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    int blkFind (
        int  col,
        int  row )
    {
        for (int n = 0; n < blkSize; n++)
            if (blk [n].col == col && blk [n].row == row)  {
                System.out.format ("blkfind: id %s\n", blk [n].id);
                return n;
            }

        System.out.format ("blkfind: not found\n");
        return -1;
    }

    // ------------------------------------------------------------------------
    // change position of lever based on mouse press
    private void mousePress (
        int  x,
        int  y )
    {
     // System.out.format ("mousePress: %3d x %3d\n", x, y);

        int col = x / colWid;
        int dX  = x % colWid;
        int num = 0;

        int pktType = 0;
        int pos     = 0;

        // ---------------------------------------------------------
        if (y < RowOff)  {
                col = x / tileWid;
            int row = y / tileWid;
            int idx = blkFind (col, row);

            System.out.format ("mousePress: (%d, %d)", x, y);
            System.out.format (" col %d, row %d", col, row);
            System.out.format (", idx %d", idx);
            if (0 <= idx)  {
                System.out.format (", id %s", blk [idx].id);
                System.out.format (" %c",     blk [idx].state);

                blk [idx].tgl ();
            }
            System.out.format ("\n");
            return;
        }

        // ---------------------------------------------------------
        if (y < (rowHt1))  {        // to lever
            num = 2 * col + 1;
            System.out.format ("mousePress: x %d, col %d, num %d\n",
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
         // System.out.format (" mousePress: code col %2d\n", col);

            if (null != symCode [col+1])  {
                symCode [col+1].cond = 'p';
                if (false)
                    System.out.format (" mousePress: col %2d, lbl %s, %c\n",
                        col, symLvr [col+1].lbl, symCode [col+1].cond);
            }
        }

        else
            return;

        if (false)
            System.out.format (
            " mousePress: (%3d, %3d), col %d, dX %2d, | %d\n",
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
            System.out.format ("    dispSymList: %2d %-4s %4s %c\n",
                                        ctcCol, type, sym.lbl, sym.cond);
            for (int i = 0; i < sym.ruleSize; i++)  {
                System.out.format ("      dispSymList: rules %d -", i);
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
        System.out.format ("\ninventory:\n");

        for (int i = 0; i < CtcColMax; i++) {
            CtcCol ctc = ctcCol [i];
            if (null == ctc)
                continue;

            System.out.format ("  inventory: num %d\n", i);
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
    private void ruleLock (
        PnlSym  sym,
        int     ruleIdx )
    {
        Rule    rule = sym.rule [ruleIdx];

        System.out.format ("      ruleLock %s: ", sym.lbl);
        rule.lock = true;
        for ( ; null != rule; rule = rule.nxt)  {
            rule.sym.lock++;
            System.out.format (" %s(%d)", rule.sym.lbl, rule.sym.lock);
        }
        System.out.println ();
    }

    // --------------------------------
    private void ruleUnlock (
        PnlSym  sym,
        int     ruleIdx )
    {
        System.out.format ("      ruleUnLock: %s", sym.lbl);

        Rule    rule = sym.rule [ruleIdx];
        rule.lock = false;
        for ( ; null != rule; rule = rule.nxt)  {
            if (0 < rule.sym.lock)  {
                System.out.format (" (%d)--", rule.sym.lock);
                rule.sym.lock--;
            }
            System.out.format (" %s(%d)", rule.sym.lbl, rule.sym.lock);
        }
        System.out.println ();
    }

    // ------------------------------------------------------------------------
    private void ruleChainCheck (
        PnlSym[]  sym,
        int       symSize )
    {
        boolean dbg   = false;

        if (ruleFlag)
            System.out.format (" ruleChainCheck: %d\n", symSize);

        // for each sym
        for (int i = 0; i < symSize; i++)  {
            if (0 == sym [i].ruleSize)
                continue;

            if (0 != sym [i].lock)  {
                System.out.format (
                    "  ruleChainCheck: %4s locked\n", sym [i].lbl);
 //             continue;
            }

            if (dbg)
                System.out.format ("ruleChainCheck: %s\n", sym [i].lbl);

            // for each sym rule
            for (int j = 0 ; j < sym [i].ruleSize; j++)  {
                boolean match = true;
                Rule rule = sym [i].rule [j];

                if (dbg)
                    System.out.format ("  ruleChainCheck: %4s %2d",
                                                    sym [i].lbl, j);

                for ( ; null != rule; rule = rule.nxt)  {
                    char c = '_';
                    if (rule.sym.cond != rule.cond)  {
                        match = false;
                        c = '!';
                    }

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

                if (match)  {
                    sym [i].cond = 'C';
                    if (! sym [i].rule [j].lock)
                        ruleLock (sym [i], j);
                    if (ruleFlag)
                        System.out.format ("    ruleChainCheck: %s match\n",
                                              sym [i].lbl);
                    break;      // check no additional rules
                }

                else if (sym [i].rule [j].lock)  {
                    if (ruleFlag)
                        System.out.format (
                        "    ruleChainCheck: %s can be unlocked\n",
                            sym [i].lbl);
                    ruleUnlock (sym [i], j);
                }
                else if (' ' != sym [i].cond)  {
                    sym [i].cond = ' ';
                    if (ruleFlag)
                        System.out.format (
                        "    ruleChainCheck: %s reset\n", sym [i].lbl);
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    private void ruleCheck ()
    {
        if (ruleFlag)
            System.out.println ("\nrulesCheck:");
        ruleChainCheck (symSig, symSigSize);
        ruleChainCheck (symTo,  symToSize);

        if (ruleFlag)  {
            System.out.format ("  ruleCheck:\n");

            for (int i = 0; i < symSigSize; i++)  {
                if (0 == symSig [i].ruleSize)
                    continue;

                if (0 < symSig [i].lock)
                    System.out.format ("   %-4s  cond %c, lock %d\n",
                        symSig [i].lbl, symSig [i].cond, symSig [i].lock);
                else
                    System.out.format ("   %-4s  cond %c, lock\n",
                        symSig [i].lbl, symSig [i].cond);

                for (int j = 0 ; j < symSig [i].ruleSize; j++)  {
                    Rule rule = symSig [i].rule [j];

                    if (false) {
                        if (symSig [i].rule [j].lock)
                            System.out.format (
                                "   ruleCheck: cond %c locked %s\n",
                                    rule.cond, symSig [i].lbl);
                        else
                            System.out.format (
                                "   ruleCheck: cond %c        %s\n",
                                    rule.cond, symSig [i].lbl);
                    }
                    else  {
                        char lock = '_';
                        if (rule.lock)
                            lock = 'T';
                        System.out.format (
                            "      %d: cond %c, lock %c\n",
                                    j, rule.cond, lock);
                    }
                }
            }
        }
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

        // signal labels
        for (symL = ctcCol [col].symSigL; symL != null; symL = symL.nxtSym)
            g2d.drawString (symL.lbl, symL.x + xOff, symL.y + yOff);

        for (symR = ctcCol [col].symSigR; symR != null; symR = symR.nxtSym)  {
            int xOff2 = 5 + g2d.getFontMetrics().stringWidth (symR.lbl);
            g2d.drawString (symR.lbl, symR.x - xOff2, symR.y + yOff);
        }

        // plate & lever
        g2d.drawImage (imgSig [col/2].img,   x0, y0, this);
        g2d.drawImage (imgLvr [ctc.pos].img, x0 + 5, y0 + 57, this);

        if (false)
            System.out.format (
                "paintSigPlate: Num %d, lamp %c\n", col, ctcCol [col].lamp);

        // lamps
        if ('L' == ctcCol [col].lamp)  {                    // left
            g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 17, this);
            g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, this);
            g2d.drawImage (imgLamp [5].img,  x0 + 34, y0 + 18, this);
        }
        else if ('R' == ctcCol [col].lamp)  {               // left
            g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 17, this);
            g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, this);
            g2d.drawImage (imgLamp [6].img,  x0 + 34, y0 + 18, this);
        }
        else  {                                             // center
            g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 17, this);
            g2d.drawImage (imgLamp [10].img, x0 + 18, y0 +  6, this);
            g2d.drawImage (imgLamp [5].img,  x0 + 34, y0 + 18, this);
        }
    }

    // ------------------------------------------------------------------------
    private void paintSigLamps (
        Graphics2D  g2d )
    {
        // all stop
        for (int i = 0; i < symSigSize; i++)  {
            PnlSym sym               = symSig [i];
            int    imgIdx            = sym.imgIdx;
            ctcCol [sym.ctcNum].lamp = ' ';
            g2d.drawImage (imgTile [imgIdx].img, sym.x, sym.y, this);
        }

        // clear one
        for (int i = 0; i < symSigSize; i++)  {
            PnlSym sym               = symSig [i];
            int    imgIdx            = sym.imgIdx;

            if ('C' == sym.cond)  {
                ctcCol [sym.ctcNum].lamp = sym.type;

                if (true)
                    System.out.format ("paintSigLamps: num %d, %c, %s\n",
                        sym.ctcNum, ctcCol [sym.ctcNum].lamp, sym.lbl);

                imgIdx += 30;
                g2d.drawImage (imgTile [imgIdx].img, sym.x, sym.y, this);
            }
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

        for (int row = 0; row < nPnlRow; row++)  {
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
        if (false)  {
        for (int i = 0; i < symToSize; i++)  {
            final int  TrackH  = 2;
            PnlSym to = symTo [i];
         // System.out.format (" paintTrack: %c %s\n", to.cond, to.lbl);
            if ('N' == to.cond)
                g2d.drawImage (imgTile [TrackH].img, to.x, to.y, this);
        }
        }

        // block occupancy
        for (int n = 0; n < blkSize; n++)  {
            if (false)
                System.out.format ("paintTrack: %d, col %d, row %d, id %d\n",
                                    n, blk [n].col, blk [n].row, blk [n].id);

            if (' ' != blk [n].state)
                trace (g2d, blk [n]);
        }
    }

    // ------------------------------------------------------------------------
    public void trace (
        Graphics2D  g2d,
        Blk  blk )
    {
        final int BlockHR = 4;
        final int BlockHL = 5;

        int       col     = blk.col;
        int       row     = blk.row;
        int       offset  = 0;

        System.out.format ("trace:  col %2d, row %d\n", col, row);

        if ('O' == blk.state)           // occupied, red offset
            offset = 30;
        else if ('C' == blk.state)      // cleared, green offset
            offset = 50;

        int tile = pnlRow [row].charAt(col) - '0';
        do  {
            int idx  = offset + tile;
            g2d.drawImage (
                imgTile [idx].img, col * tileWid, row * tileWid, this);

            switch (tile) {
            case 7:         // diagonalUD \
            case 9:         // angleDR -\
                row++;
                col++;
                break;

            case 6:         // diagonalDU /
            case 11:        // angleUR -/
                row--;
                col++;
                break;

            case 8:         // angleDL /-
            case 10:        // angleUL \-
            default:
                col++;;
                break;
            }

            System.out.format (" trace: col %2d, row %d\n", col, row);

            tile = pnlRow [row].charAt(col) - '0';
            System.out.format (" trace: tile %2d\n", tile);
        } while (0 <= tile && BlockHR != tile && BlockHL != tile);

        if (BlockHR == tile)  {
            int idx  = offset + tile;
            g2d.drawImage (
                imgTile [idx].img, col * tileWid, row * tileWid, this);
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

        // -----------------------------------------------
        AffineTransform transform = new AffineTransform ();

        int         x0  = 0;
        int         y0  = 0;
        int         x;
        int         y;

        g2d.setColor (Color.black);
        g2d.fillRect (0, 0, r.width, r.height);

        paintTrack     (g2d, r.width, r.height);
        paintSigLamps  (g2d);
        paintCtcPlates (g2d, transform);
    }
}
