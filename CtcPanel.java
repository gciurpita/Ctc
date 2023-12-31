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
class Img   {
    Image   img;
}

// -----------------------------------------------------------------------------
// display railroad schematic and USS switch & signal levers

public class CtcPanel extends JPanel
        implements MouseListener, KeyListener
{
    JFrame frame = new JFrame ();

    private static final String Title           = "Pacific Southern Railway";

    final int       CANVAS_WIDTH    = 800;
    final int       CANVAS_HEIGHT   = 400;

    private int     dbg             = 0;

    String          cfgFile;

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
    int             sigPos  []       = new int [ColMax];
    int             swPos   []       = new int [ColMax];

    Sckt            sckt;
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
        String configuration,
        String ip )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        byte[]  buf     = new byte [10];

        addKeyListener   (this);
        addMouseListener (this);


        cfgFile  = configuration;
        loadCfg (configuration);

        colWid  = imgTo [0].img.getWidth (null);
        nCol    = CANVAS_WIDTH / colWid;
        rowHt1  = RowOff + imgTo  [0].img.getHeight (null);
        rowHt2  = rowHt1 + imgSig [0].img.getHeight (null);
        codeDia = imgCode [0].img.getWidth (null);

        System.out.format (" CtcPanel: sig wid %3d, to wid %3d\n",
            imgSig [1].img.getWidth (null), imgTo [1].img.getWidth (null) );

        this.setPreferredSize (new Dimension (CANVAS_WIDTH, CANVAS_HEIGHT));

        frame.setContentPane (this);
        frame.pack ();
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.setVisible (true);
        frame.setTitle   ("CTC Panel");

        // position app near top center of screen
        Rectangle r = frame.getBounds();        // window size
        frame.setBounds (900, 0, r.width, r.height);

        // initialize CTC
    //  plateWid = bmp [ImgIdxSig].img.getWidth(null);
        for (int col = 0; col < ColMax; col++)
            sigPos [col] = LvrCenter;

        tileWid = imgTile [0].img.getWidth (null);
        System.out.format ("CtcPanel: tile width %d x %d\n",
                tileWid, imgTile [0].img.getHeight (null));

        if (null != ip)  {
            sckt   = new Sckt ("127.0.0.1");
            buf[0] = 0;
            sckt.sendPkt (Sckt.PKT_START, buf, 1);
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
    //Just copy-paste this method
    public static Image makeColorTransparent (
        Image       im,
        final Color color)
    {
        ImageFilter filter = new RGBImageFilter() {

            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                }
                else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

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

        if (y < RowOff)
            return;

        if (y < (rowHt1))  {
            if ((dX < colWid / 2))
                swPos [col] = LvrLeft;
            else
                swPos [col] = LvrRight;

            pktType = Sckt.PKT_LVR_TO;
            pos     = swPos [col];
        }

        else if (y < (rowHt2))  {
            if (dX < (colWid / 3))
                sigPos [col] = LvrLeft;
            else if (dX < (colWid * 2 / 3))
                sigPos [col] = LvrCenter;
            else
                sigPos [col] = LvrRight;

            pktType = Sckt.PKT_LVR_SIG;
            pos     = sigPos [col];
        }

        else if (CodeXoff <= dX && dX < (CodeXoff + codeDia))
            codeBut [col] = ! codeBut [col];
        else
            return;

        if (0 != dbg)
            System.out.format (
            " leverAdjust: (%3d, %3d), col %d, dX %2d, | %d, sw %d, sig %d\n",
                x, y, col, dX, colWid/2, swPos [col], sigPos [col]);

        repaint ();

        buf [0] = (byte) col;
        buf [1] = (byte) pos;

        System.out.format ("leverAdjust: type %d %02x %02x\n",
                                        pktType, buf [0], buf [1]);
        sckt.sendPkt ((byte) pktType, buf, 2);
    }

    // ------------------------------------------------------------------------
    // draw CTC plate

    private void paintPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        boolean     signal,
        int         col,
        int         lvrIdx )
    {
        if (0 != dbg)
            System.out.format ("  paintPlate: %3d %3d, %d\n", x0, y0, lvrIdx);

        if ( ! signal) {
            g2d.drawImage (imgTo  [col].img, x0, y0, this);
            g2d.drawImage (imgLvr [lvrIdx].img, x0 + 6, y0 + 44, this);

            if (0 == lvrIdx)  {
                g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 3, this);
                g2d.drawImage (imgLamp [0].img,  x0 + 34, y0 + 4, this);
            }
            else  {
                g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 3, this);
                g2d.drawImage (imgLamp [1].img,  x0 + 34, y0 + 4, this);
            }
        }
        else {
            g2d.drawImage (imgSig [col].img, x0, y0, this);
            g2d.drawImage (imgLvr [lvrIdx].img, x0 + 5, y0 + 57, this);

            if (0 == lvrIdx)  {
                g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 17, this);
                g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, this);
                g2d.drawImage (imgLamp [5].img,  x0 + 34, y0 + 18, this);
            }
            else if (1 == lvrIdx)  { // right
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
    }

    // --------------------------------
    // draw CTC plate

    private void paintPlates (
        Graphics2D  g2d,
        int         screenWid,
        int         screenHt )
    {
        System.out.format ("paintPlates:\n");

        int y0 = 100;
        for (int i = 0; i < 3; i++)  {
            int x0  = (i+1) * 100;
         // paintPlate (g2d, x0, y0, 4, i);
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

        g2d.setColor (Color.black);
        g2d.fillRect (0, 0, r.width, r.height);

     // g2d.setColor (new Color(49, 107, 53));   // CTC  green
        g2d.setColor (new Color(115, 104, 50));  // #736832
     // y1 += imgSig [0].img.getHeight (null);
        g2d.fillRect (0, y2, r.width, 50);

        for (int col = 0; col < nCol; col++)  {
            int x0 = col * colWid;

            paintPlate (g2d, x0, y0, false, col, swPos  [col]);
            paintPlate (g2d, x0, y1, true,  col, sigPos [col]);

            // code button
            Image img = imgCode [0].img;
            if (codeBut [col])
                img  = imgLamp [1].img;
            g2d.drawImage (img, x0 + CodeXoff, y2 + CodeYoff, this);
        }
    }

    // ------------------------------------------------------------------------
    private void paintTiles (
        Graphics2D  g2d,
        int         screenWid,
        int         screenHt )
    {
     // if (0 != dbg)
            System.out.format ("paintTiles:\n");

        int  nCol   = screenWid / tileWid;
        int  y0     = tileWid;
        int  idx;

        int TrkH     = 2;
        int TrkDL    = 8;
        int TrkDR    = 9;
        int TrkUL    = 10;
        int TrkUR    = 11;
        int TrkDiagU = 6;
        int TrkDiagD = 7;

        int SigGL = 47;     // left green
        int SigGR = 46;     // left red
        int SigRL = 17;     // right red
        int SigRR = 16;     // left  red

        for (int row = 1; row <= 6; row++)  {
            y0 = row * tileWid;
            for (int col = 0; col < nCol; col++)  {
                int x0 = col * tileWid;

                idx = 0;
                if (1 == row)  {
                    if (8 == col)
                        idx = SigRL;
                }
                else if (2 == row)  {
                    if (7 == col)
                        idx = TrkDL;
                    else if (7 < col)
                        idx = TrkH;
                }

                else if (3 == row)  {
                    if (6 == col)
                        idx = TrkDiagU;
                    if (8 == col)
                        idx = SigRL;
                }

                else if (4 == row)  {
                    idx = TrkH;
                    if (5 == col)
                        ;  // idx = TrkUR;
                }
                else if (5 == row)  {
                    if (4 == col)
                        idx = SigRR;
                }

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

        if (false)
            paintPlates    (g2d, r.width, r.height);
        else
            paintCtcPlates (g2d, transform);

        paintTiles   (g2d, r.width, r.height);
    }
}
