
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

// import java.util.Timer;
// import java.util.TimerTask;

// -----------------------------------------------------------------------------
public class Track {
    final int Col      = 100;
    final int Row      = 10;
    byte      trk [][] = new byte [Col][Row];
    int       nRow     = 0;
    int       maxCol   = 0;

    class Tile   {
        Image   img;
    }

    final int TileSize = 90;
    Tile      tile []  = new Tile [TileSize];

    String    cfgFile  = "../Resources/blackScreenTiles.cfg";

    int       dbg      = 0;

    // ------------------------------------------------------------------------
    public Track ()
            throws FileNotFoundException, IOException
    {
        loadTiles ();
    }

    // ------------------------------------------------------------------------
    public int height ()
    {
        return tile [0].img.getHeight (null) * nRow;
    }

    public int tileWidth ()
    {
        return tile [0].img.getWidth (null);
    }

    public int width ()
    {
        return tile [0].img.getWidth (null) * maxCol;
    }

    // ------------------------------------------------------------------------
    public void loadTiles ()
            throws FileNotFoundException, IOException
    {
        System.out.format ("loadTiles: %s\n", cfgFile);

        BufferedReader br = new BufferedReader(new FileReader(cfgFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            String[]    fields = line.split("  *");
            if (0 != dbg)
                System.out.format (" loadTiles: %s - %s\n", line, fields [0]);

            if (fields[0].equals("icon") && fields[1].equals("Tile"))  {
                if (fields.length < 4)  {
                    throw new IllegalArgumentException (
                        "Error - loadTiles: icon type id <filename>\n");
                }

                int    id      = Integer.parseInt (fields [2]);
                File   inFile  = new File ("../" + fields [3]);
                tile [id]      = new Tile ();
                tile [id].img  = ImageIO.read (inFile);

                System.out.format ("  loadTiles: %s\n", fields [3]);
            }
        }
    }

    // ------------------------------------------------------------------------
    public boolean check (
        int     col,
        int     row,
        char    type )
    {
        byte tile = trk [col][row];

     // System.out.format ("  Track.check: 0x%02x %2d x %2d\n", tile, col, row);
        
        if ('*' == type)
            return (16 <= tile && tile <= 17);
        else if ('B' == type)
            return (5 == tile);
        else if ('x' == type)
            return (8 <= tile && tile <= 11);

        System.out.format ("Error Track.check unknown type '%c'\n", type);
        System.exit (2);

        return false;
    }

    // ------------------------------------------------------------------------
    public void disp ()
    {
        System.out.format (" Track.disp: %2d x %2d\n", maxCol, nRow);

        for (int row = 0; row < nRow; row++)  {
            System.out.format ("    ");
            for (int col = 0; col < maxCol; col++)
                System.out.format ("%c", trk [col][row] + '0');
            System.out.println ();
        }
    }

    // ------------------------------------------------------------------------
    public void newRow (
        String  s )
    {
        char c;
     // System.out.format ("  Track.newRow: %s\n", s);

        if (Row <= nRow)  {
            System.out.format ("Error Track.newRow: row lime %d %d\n", nRow);
            System.exit (3);
        }

        for (int col = 0; col < s.length (); col++)  {
            c = s.charAt (col);
            if (' ' == c)
                c = 0;
            else
                c -= '0';
            trk [col][nRow] = (byte)c;
        }
        nRow++;

        if (maxCol < s.length ())
            maxCol = s.length ();
    }
}
