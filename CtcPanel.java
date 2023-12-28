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
class ImgLamp {
    Image   img;
}
class ImgLvr {
    Image   img;
}

class ImgSig {
    Image   img;
}

class ImgTo {
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

    final int       ColMax          = 20;
    final   int     ColOff          = 25;
    final   int     ColWid          = 60;
    final   int     Ncol            = CANVAS_WIDTH / ColWid;

    final   int     RowOff          = 150;
    final   int     RowHt           = 100;

    private int     dbg             = 0;

    String          cfgFile;

    final int       MaxImg          = 20;
    ImgLvr          imgLvr []       = new ImgLvr  [MaxImg];
    int             imgLvrSize      = 0;

    ImgLamp         imgLamp []      = new ImgLamp [MaxImg];
    int             imgLampSize     = 0;

    ImgSig          imgSig []       = new ImgSig  [MaxImg];
    int             imgSigSize      = 0;

    ImgTo           imgTo []        = new ImgTo   [MaxImg];
    int             imgToSize       = 0;

    final int       ImgIdxSig       = 3;
    final int       ImgIdxSw        = 4;

    final int       LvrLeft         = 0;
    final int       LvrRight        = 1;
    final int       LvrCenter       = 2;

    int             plateWid;

    int             sigPos []       = new int [ColMax];
    int             swPos  []       = new int [ColMax];

    // ---------------------------------------------------------
    public enum ImgType { Lamp, Lever, PlateSig, PlateTo, None };

    public ImgType stringToType (String s)  {
        if (s.equals("Lamp"))
            return ImgType.Lamp;
        else if (s.equals("Lever"))
            return ImgType.Lever;
        else if (s.equals("Signal"))
            return ImgType.PlateSig;
        else if (s.equals("Turnout"))
            return ImgType.PlateTo;

        return ImgType.None;
    }

    public String typeToString (ImgType type)  {
        if (ImgType.Lamp == type)
            return "Lamp";
        else if (ImgType.Lever == type)
            return "Lever";
        else if (ImgType.PlateSig == type)
            return "Signal";
        else if (ImgType.PlateTo == type)
            return "Turnout";

        return "Unknown";
    }

    // ------------------------------------------------------------------------
    // constructor loads the configuration files and
    //   determines the initial display geometry

    public CtcPanel (String configuration)
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        byte[]  buf     = new byte [10];

        addKeyListener   (this);
        addMouseListener (this);


        cfgFile  = configuration;
        loadCfg (configuration);

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
            case Lamp:
                imgLamp [imgLampSize]     = new ImgLamp ();
                imgLamp [imgLampSize].img = ImageIO.read (inFile);
                imgLampSize++;
                break;

            case Lever:
                imgLvr [imgLvrSize]       = new ImgLvr ();
                imgLvr [imgLvrSize].img   = ImageIO.read (inFile);
                imgLvrSize++;
                break;

            case PlateSig:
                imgSig [imgSigSize]       = new ImgSig ();
                imgSig [imgSigSize].img   = ImageIO.read (inFile);
                imgSigSize++;
                break;

            case PlateTo:
                imgTo [imgToSize]        = new ImgTo ();
                imgTo [imgToSize].img    = ImageIO.read (inFile);

                if (0 != dbg)
                    System.out.format ("     loadImage: wid %4d\n", 
                            imgTo [imgToSize].img.getWidth (null));

                imgToSize++;
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
        }
    }

    // ------------------------------------------------------------------------
    // main processes command line arguments settign the debug level and

    public static void main (String[] args)
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        for (int i = 0; i < args.length; i++)
            System.out.format ("main: %d %s\n", i, args[i]);

        CtcPanel disp = new CtcPanel (args[0]);
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
        int col = (x - ColOff) / ColWid;
        int row = (y - RowOff) / RowHt;

        int dX  = (x - ColOff) % ColWid;

        if (plateWid < dX)
            return;

        if (0 == row) {
            if ((dX < plateWid / 2))
                swPos [col] = LvrLeft;
            else
                swPos [col] = LvrRight;
        }
        else {
            if (dX < (plateWid / 3))
                sigPos [col] = LvrLeft;
            else if (dX < (plateWid * 2 / 3))
                sigPos [col] = LvrCenter;
            else
                sigPos [col] = LvrRight;
        }

        System.out.format (
            " leverAdjust: (%3d, %3d), [%2d, %d], dX %2d, | %d, sw %d, sig %d\n",
                x, y, col, row, dX, ColWid/2, swPos [col], sigPos [col]);

        repaint ();
    }

    // ------------------------------------------------------------------------
    // draw CTC plate

    private void paintPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        int         plateIdx,
        int         lvrIdx )
    {
        if (0 != dbg)
            System.out.format ("  paintPlate: %3d %3d, %d\n", x0, y0, lvrIdx);

        final int[] LvrXoff = {  9,  8, 20};
        final int[] LvrYoff = { 50, 49, 43};

        g2d.drawImage (imgTo [plateIdx].img, x0, y0, this);

        g2d.drawImage ( imgLvr [lvrIdx].img,
                x0 + LvrXoff [lvrIdx],
                y0 + LvrYoff [lvrIdx], this);
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
            paintPlate (g2d, x0, y0, 4, i);
        }
    }

    // ------------------------------------------------------------------------
    // draw grid

    private void paintCtcPlates (
        Graphics2D  g2d,
        AffineTransform transform )
    {
        if (0 != dbg)
            System.out.format ("paintGrid:\n");

        Rectangle   r      = frame.getBounds();
        int         colWid = imgTo [0].img.getWidth (null);
        int         rowHt  = imgTo [0].img.getHeight (null);
        int         y0     = RowOff;
        int         y1     = y0 + imgTo [0].img.getHeight (null);

        g2d.setColor (Color.black);
        g2d.fillRect (0, 0, r.width, r.height);

     // g2d.setColor (new Color(49, 107, 53));   // CTC  green
        g2d.setColor (new Color(115, 104, 50));  // #736832

        for (int col = 0; col <= Ncol; col++)  {
            int x0 = col * colWid;

            paintPlate (g2d, x0, y0, ImgIdxSw,  swPos  [col]);
            paintPlate (g2d, x0, y1, ImgIdxSig, sigPos [col]);
        }

        y0 += rowHt + imgTo [0].img.getHeight (null);   // To or Sig

        g2d.fillRect (0, y0, r.width, 30);
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
    }
}
