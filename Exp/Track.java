
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
    SymList   symList;

    final int Col      = 100;
    final int Row      = 30;
    byte      trk [][] = new byte [Col][Row];
    int       nRow     = 0;
    int       maxCol   = 0;

    Panel     panel;

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
        int     ctcNum;
        String  name;

        ToSig   next;

        byte    tile;
        int     x;
        int     y;
        int     xLbl;
        int     yLbl;

        Sym     sym;

        // -------------------------------------
        public ToSig (
            int     col,
            int     row,
            byte    tile,
            Sym     sym,
            String  name,
            ToSig   toSig )
        {
            this.col  = col;
            this.row  = row;
            this.tile = tile;
            this.sym  = sym;
            this.name = name;
            this.next = toSig;

            this.x    = col * tileWid;
            this.y    = row * tileHt;

            if (null != sym)
                this.sym.cond = 'N';
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
    public Track (
        Panel   panel,
        SymList symList )
            throws FileNotFoundException, IOException
    {
        this.panel   = panel;
        this.symList = symList;
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

    // --------------------------------
    private int atoi (
        String s )
    {
        int  val = 0;

        for (int i = 0; i < s.length(); i++)  {
            char c = s.charAt (i);
            if (! Character.isDigit (c))
                break;
            val = 10*val + c - '0';
        }

        return val;
    }

    // ------------------------------------------------------------------------
    final int TrackH   = 2;
    final int BlockHl  = 5;

    final int AngleDL  = 8;
    final int AngleDR  = 9;
    final int AngleUL  = 10;
    final int AngleUR  = 11;

    final int HsignalR = 16;
    final int HsignalL = 17;

    public boolean check (
        int     col,
        int     row,
        char    type,
        int     ctcNum,
        Sym     sym,
        String  name )
    {
        byte tile = trk [col][row];

        if ('*' == type)  {
            if (tile < HsignalR || HsignalL < tile)
                return false;
        }
        else if ('B' == type)  {
            if (BlockHl != tile)
                return false;
        }
        else if ('x' == type)  {
            if (tile < AngleDL || AngleUR < tile)
                return false;
            trk [col][row] = TrackH;
        }
        else
                return false;

        if (null == name)
            return true;

        toSigHd = new ToSig (col, row, tile, sym, name, toSigHd);

        ToSig ts = toSigHd;
        if (false)
            System.out.format ( "  Track.check: <%2d, %2d> %2d %s\n",
                ts.col, ts.row, ts.tile, ts.name);

        toSigHd.ctcNum = ctcNum;

        // set text offsets
        switch (tile)  {
        case AngleDL:
            toSigHd.xLbl =  tileWid * 2/4;
            toSigHd.yLbl =  tileWid * 7/4;
            break;
        case AngleDR:
            toSigHd.xLbl =  tileWid * 6/4;
            toSigHd.yLbl =  tileWid * 5/4;
            break;
        case AngleUL:
            toSigHd.xLbl = -tileWid * 7/4;
            toSigHd.yLbl =  tileWid * 1/4;
            break;
        case AngleUR:
            toSigHd.xLbl =  tileWid * 6/4;
            toSigHd.yLbl =  tileWid * 1/4;
            break;

        case HsignalL:
            toSigHd.xLbl =  tileWid * 5/4;
            toSigHd.yLbl =  tileWid * 3/4;
            break;

        case HsignalR:
            toSigHd.xLbl =  tileWid * 3/4;
            toSigHd.xLbl =  0;
            toSigHd.yLbl =  tileWid * 3/4;

        case BlockHl:
            break;

        default:    // flat
            System.out.format ( "  Track.check: %d %2d x %2d, '%c' %s\n",
                tile, col, row, type, name);
            return false;
        };

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
            System.out.format (
                "Error Track.newRow: # rows %d > Row %d\n", nRow, Row);
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
    public void update (
        char    pos,
        String  id )
    {
        System.out.format ("  Track.update: '%c' \"%s\"\n", pos, id);

        Sym sym = symList.findId (id);

        panel.response (sym.num, pos);      // notify panel

        for (ToSig ts = toSigHd; null != ts; ts = ts.next)  {
            if (ts.sym.id != id)
                continue;

            // TO case
            if (1 == (ts.ctcNum % 2))  {
                byte tile = TrackH;
                if ('R' == pos)         // right/reversed
                    tile = ts.tile;

                trk [ts.col][ts.row] = tile;
                ts.sym.cond          = pos;

                System.out.format ( "    Track.update: <%2d, %2d> %2d %s\n",
                                    ts.col, ts.row, tile, ts.name);
            }

            // signal case
            else {
                byte tile = ts.tile;
                if ('R' == pos || 'L' == pos)
                    tile += 30;     // hsignalRG or hsignalLG

                trk [ts.col][ts.row] = tile;
                ts.sym.cond          = pos;

                System.out.format ( "    Track.update: <%2d, %2d> %2d %s\n",
                                    ts.col, ts.row, tile, ts.name);
            }
        }
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

        // add labels
        g2d.setColor (Color.white);
        for (ToSig ts = toSigHd; null != ts; ts = ts.next)  {
            if (false)  {
                System.out.format (" Track.paint: %4s", ts.name);
                System.out.format (" %3d, %3d", ts.x, ts.y);
                System.out.format (" -- %3d, %3d", ts.xLbl, ts.yLbl);
                System.out.println ();
            }

            if (0 == ts.xLbl)  {
                ts.xLbl = -(5 + g2d.getFontMetrics().stringWidth (ts.name));
            }
            g2d.drawString (ts.name, ts.x + ts.xLbl, ts.y + ts.yLbl);

        }
    }
}
