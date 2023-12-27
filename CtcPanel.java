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
// bitmap description - id, label, ...

class Bmp {
    Image   img;
    public Bmp (String filename) {
    }
}

// -----------------------------------------------------------------------------
// display railroad schematic and USS switch & signal levers

public class CtcPanel extends JPanel
        implements MouseListener, KeyListener
{
    JFrame frame = new JFrame ();

    private static final String Title           = "Pacific Southern Railway";

    public static final int     CANVAS_WIDTH    = 800;
    public static final int     CANVAS_HEIGHT   = 400;

    private int                 dbg             = 0;

    private Bmp                 bg;
    private int                 bgSize          = 0;

    private static final int    MAX_BMP         = 200;
    private static final int    MAX_ELEM        = 5000;
    private static final int    MAX_RECT        = 50;
    private static final int    MAX_TEXT        = 1000;

    private String              cfgFile;
    private Bmp                 bmp []          = new Bmp  [MAX_BMP];
    private int                 bmpSize         = 0;

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

        this.setPreferredSize (new Dimension (CANVAS_WIDTH, CANVAS_HEIGHT));

        frame.setContentPane (this);
        frame.pack ();
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.setVisible (true);

        // position app near top center of screen
        Rectangle r = frame.getBounds();        // window size
        frame.setBounds (900, 0, r.width, r.height);
    }

    // --------------------------------
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
    public void keyReleased (KeyEvent e)
    {
        // ignore
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

    private void loadImage (String imgFileName, Bmp bmp)
        throws IllegalArgumentException
    {
        System.out.format ("      loadImage: %s\n", imgFileName);

        URL url = getClass ().getClassLoader ().getResource (imgFileName);

        if  (url == null) {
            throw new IllegalArgumentException ("cannot find " + imgFileName);
        } else {
            try {
                bmp.img  = ImageIO.read (url);
            } catch  (IOException ex) {
                ex.printStackTrace ();
            }
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
            System.out.format ("   loadCfg: %s - %s\n", line, fields [0]);

            // -----------------------------------------------------------------
            // load icon

            if (fields[0].equals("icon") || fields[0].equals("bg"))  {
                if (fields.length < 4)  {
                    throw new IllegalArgumentException (
                        "Error - loadCfg: icon type id  <filename>\n");
                }

                bmp[bmpSize] = new Bmp (fields[3]);
                loadImage (fields[3], bmp[bmpSize++]);
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
     // CtcPanel disp = new CtcPanel ("Resources/ctcTiles");
        System.out.format ("  main: done\n");
    }

    // ------------------------------------------------------------------------
    // process mouse press, search for element closest to mouse location

    public void mousePressed (MouseEvent ev)
    {
        int  p       = ev.getX();
        int  q       = ev.getY();


        requestFocusInWindow ();
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
    // draws bmps

    private void paintBmps (
        Graphics2D  g2d,
        int         screenWid,
        int         screenHt )
    {
        int     b;
        int     x0  = 0;
        int     y0  = 0;
        int     x;
        int     y;

        System.out.format ("paintBmps:\n");

        g2d.setColor (Color.white);
        g2d.drawLine (0, 100, CANVAS_WIDTH, 100);

        int  wid   = bmp [3].img.getWidth  (null);
        int  ht    = bmp [3].img.getHeight (null);

        g2d.setColor (Color.black);
        g2d.drawLine (0, ht, screenWid, ht);

        for (int i = 0; i < 3; i++)  {
            x  = i * wid;
            g2d.drawImage (bmp [3].img, x, 0, this);

            int  lvrWid   = bmp [i].img.getWidth  (null);
            int  lvrHt    = bmp [i].img.getHeight (null);

            g2d.drawImage (bmp [i].img, x, ht-lvrHt, this);
        }
    }

    // ------------------------------------------------------------------------
    // draw grid

    private void paintGrid (
        Graphics2D  g2d,
        AffineTransform transform,
        int         screenWid,
        int         screenHt )
    {
        int       wid   = bmp [2].img.getWidth  (null);
        int       ht    = bmp [2].img.getHeight (null);
        int       cols  = screenWid / wid;

        g2d.setColor (new Color(49, 107, 53));
        g2d.fillRect (0, 0, screenWid, screenHt);

        System.out.format ("paintGrid: wid %d, ht %d\n", wid, ht, cols);

        int  y = screenHt - 2*ht;
        for (int row = 0; row < 2; row++)  {
            for (int col = 0; col <= cols; col++)  {
                int x  = wid * col;
                g2d.drawImage (
                    makeColorTransparent (bmp [2+row].img, Color.white),
                    x, y, this);
            }
            y -= ht;
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

        if (true)
            paintBmps     (g2d, r.width, r.height);
        else
            paintGrid     (g2d, transform, r.width, r.height);
    }
}
