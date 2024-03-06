
import java.awt.*;
import java.io.*;
// import java.net.*;
import javax.swing.*;
import java.util.*;

// import java.awt.image.BufferedImage;
// import java.awt.image.ImageFilter;
// import java.awt.image.ImageProducer;
// import java.awt.image.RGBImageFilter;
// import java.awt.image.FilteredImageSource;

// import java.awt.event.*;
// import java.awt.geom.AffineTransform;
import javax.imageio.ImageIO;

// -----------------------------------------------------------------------------
public class Lever {
    int     lever  []  = new int [40];;

    String    cfgFile  = "../Resources/ctcNumbered.cfg";
    class Icon   {
        Image   img;
    }
    Icon      lamp    [] = new Icon [15];
    Icon      signal  [] = new Icon [15];
    Icon      turnout [] = new Icon [15];
    Icon      code    [] = new Icon [2];

    int       dbg        = 0;

    // --------------------------------
    public Lever ()
            throws FileNotFoundException, IOException
    {
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
            if (0 != dbg)
                System.out.format (" loadTiles: %s - %s\n", line, fields [0]);

            if (! fields[0].equals("icon"))
                continue;

            if (fields.length < 4)  {
                System.out.format (
                    "Error - loadIcons: 4 arg - icon type id <filename>\n");
                System.exit (2);
            }

            int    id      = Integer.parseInt (fields [2]);
            File   inFile  = new File ("../" + fields [3]);

            if (fields[1].equals("Lamp"))  {
                lamp [id]      = new Icon ();
                lamp [id].img  = ImageIO.read (inFile);
            }

            if (fields[1].equals("Signal"))  {
                signal [id]      = new Icon ();
                signal [id].img  = ImageIO.read (inFile);
            }

            if (fields[1].equals("Turnout"))  {
                turnout [id]      = new Icon ();
                turnout [id].img  = ImageIO.read (inFile);
            }

            if (fields[1].equals("Code"))  {
                code [id]      = new Icon ();
                code [id].img  = ImageIO.read (inFile);
            }

            System.out.format ("  loadIcons: %s\n", fields [3]);
        }

     // tileHt  = tile [0].img.getHeight (null);
     // tileWid = tile [0].img.getWidth  (null);
    }

    // --------------------------------
    public void addLever (
        int  ctcId)
    {
        if (lever.length <= ctcId)  {
            System.err.format ("Error Lever.addLever range %d\n", ctcId);
            System.exit (3);
        }

        lever [ctcId]++;
    }

    // --------------------------------
    public boolean check (
        int  ctcId)
    {
        return (0 < lever [ctcId]);
    }

    // ------------------------------------------------------------------------
    public void paint (
        Graphics2D  g2d,
        int         wid,
        int         ht )
    {
        g2d.setColor (new Color(115, 104, 50));  // #736832
        g2d.fillRect (0, ht, wid, ht);

    }
};
