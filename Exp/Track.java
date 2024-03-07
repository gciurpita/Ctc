
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

    String    cfgFile  = "../Resources/blackScreenTiles.cfg";

    // -------------------------------------
    class Tile   {
        Image   img;
    }
    Tile      tile []  = new Tile [90];

    // -------------------------------------
    class ToSig {
        int     col;
        int     row;
        String  name;

        ToSig   next;

        // -------------------------------------
        public ToSig (
            int     col,
            int     row,
            String  name,
            ToSig   toSig )
        {
            this.col  = col;
            this.row  = row;
            this.name = name;
            next      = toSig;
        }
    };
    ToSig     toSigHd = null;

    // -------------------------------------
    int       panelHt;
    int       panelWid;
    int       tileHt;
    int       tileWid;

    int       dbg      = 0;

    // ------------------------------------------------------------------------
    public Track ()
            throws FileNotFoundException, IOException
    {
        loadTiles ();
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

             // System.out.format ("  loadTiles: %s\n", fields [3]);
            }
        }

        tileHt  = tile [0].img.getHeight (null);
        tileWid = tile [0].img.getWidth  (null);
    }

    // ------------------------------------------------------------------------
    final int BlockHl  = 5;

    final int AngleDl  = 8;
    final int AngleDR  = 9;
    final int AngleUL  = 10;
    final int AngleUR  = 11;

    final int HsignalR = 16;
    final int HsignalL = 17;

    public boolean check (
        int     col,
        int     row,
        char    type,
        String  name )
    {
        byte tile = trk [col][row];

        if (false)
            System.out.format (
                "  Track.check: %d %2d x %2d, %c\n", tile, col, row, type);
        
        if ('*' == type)  {
            if (tile < HsignalR && HsignalL < tile)
                return false;
        }
        else if ('B' == type)  {
            if (BlockHl != tile)
                return false;
        }
        else if ('x' == type)  {
            if (tile < AngleDl && AngleUR < tile)
                return false;
        }

        toSigHd = new ToSig (col, row, name, toSigHd);

        return true;
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

    // --------------------------------
    public void mousePressed (
        int  x,
        int  y )
    {
        int  col  = x / tileWid;
        int  row  = y / tileHt;
        byte tile = trk [col][row];

        System.out.format (
            "Track.mousePressed: %d, %d, <%d, %d> %d\n", x, y, row, col, tile);
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

        panelHt  = tileHt  * nRow;
        panelWid = tileWid * maxCol;
    }

    // ------------------------------------------------------------------------
    public void paint (
        Graphics2D  g2d )
    {
        g2d.setColor (Color.black);
        g2d.fillRect (0, 0, panelWid, panelHt);

        if (0 != dbg)
            System.out.format ("paintTrack:\n");

        for (int row = 0; row < nRow; row++)  {
         // System.out.format ("  paintTrack: %d %s\n", row, pnlRow [row]);

            for (int col = 0; col < maxCol; col++)  {
                int  x0   = tileWid * col;
                int  y0   = tileWid * row;
                int  idx  = trk [col][row];
           //   System.out.format ("  %2d, %c  %d\n", i, c, idx);

                if (76 < idx)
                    System.out.format ("paintTrack: ERROR idx %d > 76\n", idx);
                else
                    g2d.drawImage (tile [idx].img, x0, y0, null);
            }
        }

        // set turnouts -- are by default reversed
 //     if (false)  {
 //     for (int i = 0; i < symToSize; i++)  {
 //         final int  TrackH  = 2;
 //         PnlSym to = symTo [i];
 //      // System.out.format (" paintTrack: %c %s\n", to.cond, to.lbl);
 //         if ('N' == to.cond)
 //             g2d.drawImage (imgTile [TrackH].img, to.x, to.y, null);
 //     }
 //     }
    }
}
