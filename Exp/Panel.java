
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
public class Panel {
    private class Lever {
        int     id;
        char    cond;
        char    pos;

        // --------------------------------
        public Lever (
            int  id )
        {
            this.id   = id;
            this.pos  = 0 == (id % 2) ? 'C' : 'L';
            this.cond = this.pos;
        }
    }

    // --------------------------------
    final int Nlvr       = 41;   // 1-40
    Lever     lvr  []    = new Lever [41];
    int       codeBut [] = new int [Nlvr /2];

    Control   ctl;

    String    cfgFile  = "../Resources/ctcNumbered.cfg";
    class Icon   {
        Image   img;
    }
    Icon      code    [] = new Icon [2];
    Icon      lamp    [] = new Icon [15];
    Icon      lever   [] = new Icon [3];
    Icon      signal  [] = new Icon [15];
    Icon      turnout [] = new Icon [15];

    int       iconToHt;
    int       iconToWid;
    int       iconSigHt;
    int       iconSigWid;

    int       y0Panel;
    int       colWid     = 64;
    int       colHt;

    int       dbg        = 0;

    // --------------------------------
    public Panel (
        Control  ctl )
            throws FileNotFoundException, IOException
    {
        this.ctl = ctl;
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
                System.out.format (" loadIcon: %s - %s\n", line, fields [0]);

            if (! fields[0].equals("icon"))
                continue;

            if (fields.length < 4)  {
                System.out.format (
                    "Error - loadIcons: 4 arg - icon type id <filename>\n");
                System.exit (2);
            }

            int    id      = Integer.parseInt (fields [2]);
            File   inFile  = new File ("../" + fields [3]);

            if (fields[1].equals("Lever"))  {
                lever [id]     = new Icon ();
                lever [id].img = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Lamp"))  {
                lamp [id]      = new Icon ();
                lamp [id].img  = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Signal"))  {
                signal [id]      = new Icon ();
                signal [id].img  = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Turnout"))  {
                turnout [id]      = new Icon ();
                turnout [id].img  = ImageIO.read (inFile);
            }

            else if (fields[1].equals("Code"))  {
                code [id]      = new Icon ();
                code [id].img  = ImageIO.read (inFile);
            }

         // System.out.format ("  loadIcons: %s\n", fields [3]);
        }

        iconToHt  = turnout [0].img.getHeight (null);
        iconToWid = turnout [0].img.getWidth  (null);
        System.out.format ("loadIcons: to wid %d, ht %d", iconToWid, iconToHt);

        iconSigHt  = signal [0].img.getHeight (null);
        iconSigWid = signal [0].img.getWidth  (null);
        System.out.format ( ", sig wid %d, ht %d", iconSigWid, iconSigHt);

        int iconCodeHt  = code [0].img.getHeight (null);
        int iconCodeWid = code [0].img.getWidth  (null);
        System.out.format ( ", cod wid %d, ht %d", iconCodeWid, iconCodeHt);

        System.out.println ();

        colHt = iconToHt + iconSigHt + 2 * iconCodeHt;
    }

    // --------------------------------
    public void addLever (
        int  ctcId)
    {
        if (lvr.length <= ctcId)  {
            System.err.format ("Error Panel.addLever range %d\n", ctcId);
            System.exit (3);
        }

        lvr [ctcId] = new Lever (ctcId);
    }

    // --------------------------------
    public boolean check (
        int  ctcId)
    {
        return (null != lvr [ctcId]);
    }

    // --------------------------------
    public void response (
        char    type,
        int     id,
        char    state )
    {
        System.out.format (
            "  Panel.response: %c %2d %c\n", type, id, state);

        lvr [id].cond = state;
    }

    // --------------------------------
    public boolean mousePressed (
        int  x,
        int  y )
    {
        int col   = x / colWid;
        int dX    = x % colWid;
        int num   = 1 + (2 * col);

        if (false)
            System.out.format (
                "Panel.mousePressed: %d %d, col %d, num %d\n",
                    x, y, col, num);

        if (null == lvr [num])
            return false;

        // turnout
        if (y - y0Panel < iconToHt)  {
            if ((dX < colWid / 2))
                lvr [num].pos = 'L';
            else
                lvr [num].pos = 'R';

            ctl.send ('T', num, lvr [num].pos);
        }

        // signal
        else if (y - y0Panel < (iconToHt + iconSigHt))  {
            num += 1;
            if ((dX < colWid / 3))
                lvr [num].pos = 'L';
            else if ((colWid * 2 / 3) < dX)
                lvr [num].pos = 'R';
            else
                lvr [num].pos = 'C';

            ctl.send ('*', num, lvr [num].pos);
        }

        // code
        else {
            codeBut [num] = 5;
            return true;
        }

        return false;
    }

    // --------------------------------
    public void timer ()
    {
        for (int num = 1; num < lvr.length; num += 2)  {
            if (null == lvr [num])
                continue;

            if (0 < codeBut [num])
                codeBut [num]--;
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    private void paintToPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        int         col,
        int         lvrId )
    {
        if (0 != dbg)
            System.out.format ("  paintToPlate: %3d %3d, %d\n", x0, y0, lvrId);

        g2d.setColor (Color.white);

        // plate & lvr
        int pos = ('R' == lvr [lvrId].pos) ? 1 : 0;
        g2d.drawImage (turnout [lvrId/2].img,   x0, y0, null);
        g2d.drawImage (lever   [pos].img, x0 + 6, y0 + 44, null);

        // lamps
        int lampIdxLeft  = 5;
        int lampIdxRight = 1;

        if ('L' == lvr [lvrId].cond)  {
            lampIdxLeft  = 6;
            lampIdxRight = 0;
        }
        g2d.drawImage (lamp [lampIdxLeft].img,  x0 +  5, y0 + 3, null);
        g2d.drawImage (lamp [lampIdxRight].img, x0 + 34, y0 + 4, null);
    }

    // ------------------------------------------------------------------------
    private void paintSigPlate (
        Graphics2D  g2d,
        int         x0,
        int         y0,
        int         col,
        int         lvrId )
    {
        // plate & lvr
        char posCh = lvr [lvrId].pos;
        int  pos   = 'R' == posCh ? 1 : 'C' == posCh ? 2 : 0;
        g2d.drawImage (signal [lvrId/2].img,   x0, y0, null);
        g2d.drawImage (lever  [pos].img,       x0 + 5, y0 + 57, null);

        // lamps
        int idxLeft   = 5;
        int idxCenter = 9;
        int idxRight  = 5;

        if ('L' == lvr [lvrId].cond)
            idxLeft   = 6;
        else if ('R' == lvr [lvrId].cond)
            idxRight  = 6;
        else        // 'C'
            idxCenter = 10;

        g2d.drawImage (lamp [idxLeft].img,   x0 +  5, y0 + 17, null);
        g2d.drawImage (lamp [idxCenter].img, x0 + 18, y0 +  6, null);
        g2d.drawImage (lamp [idxRight].img,  x0 + 34, y0 + 18, null);
    }

//  // ------------------------------------------------------------------------
//  private void paintSigLamps (
//      Graphics2D  g2d )
//  {
//      // all stop
//      for (int i = 0; i < symSigSize; i++)  {
//          PnlSym sym               = symSig [i];
//          int    imgIdx            = sym.imgIdx;
//          ctcCol [sym.ctcNum].lamp = ' ';
//          g2d.drawImage (imgTile [imgIdx].img, sym.x, sym.y, this);
//      }

//      // clear one
//      for (int i = 0; i < symSigSize; i++)  {
//          PnlSym sym               = symSig [i];
//          int    imgIdx            = sym.imgIdx;

//          if ('C' == sym.cond)  {
//              ctcCol [sym.ctcNum].lamp = sym.type;

//              if (true)
//                  System.out.format ("paintSigLamps: num %d, %c, %s\n",
//                      sym.ctcNum, ctcCol [sym.ctcNum].lamp, sym.lbl);

//              imgIdx += 30;
//              g2d.drawImage (imgTile [imgIdx].img, sym.x, sym.y, this);
//          }
//      }
//  }

    // ------------------------------------------------------------------------
    public void paint (
        Graphics2D  g2d,
        int         y0,
        int         wid,
        int         ht )
    {
     // System.out.format ("Panel paint:\n");

        y0Panel  = y0;

        int   y1 = y0 + iconToHt;
        int   y2 = y1 + iconSigHt;

        g2d.setColor (new Color(115, 104, 50));  // #736832
        g2d.fillRect (0, y0, wid, y0+ht);
 
        for (int num = 1; num < lvr.length; num += 2)  {
            if (null == lvr [num] && null == lvr [num+1])
                continue;

            int col = 1 + (num-1) / 2;
            int x0  = colWid * ((num-1) / 2);

            if (null != lvr [num])
                paintToPlate  (g2d, x0, y0, col, num);

            if (null != lvr [num+1])
                paintSigPlate (g2d, x0, y1, col, num+1);

            // code button
            int  idx = 0 < codeBut [num] ? 1 : 0;
            g2d.drawImage (code [idx].img, x0 + 15, y2 + 10, null);
        }
    }
};
