
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
    int     lvr  []  = new int [41];    // 1-40

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

    int       colWid     = 64;
    int       colHt;

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

            System.out.format ("  loadIcons: %s\n", fields [3]);
        }

        iconToHt  = turnout [0].img.getHeight (null);
        iconToWid = turnout [0].img.getWidth  (null);
        System.out.format (
                "loadIcons: to wid %d, ht %d", iconToWid, iconToHt);

        iconSigHt  = signal [0].img.getHeight (null);
        iconSigWid = signal [0].img.getWidth  (null);
        System.out.format ( ", sig wid %d, ht %d\n", iconSigWid, iconSigHt);

        int iconCodeHt  = code [0].img.getHeight (null);
        int iconCodeWid = code [0].img.getWidth  (null);
        System.out.format ( ", cod wid %d, ht %d\n", iconCodeWid, iconCodeHt);

        colHt = iconToHt + iconSigHt + 2 * iconCodeHt;
    }

    // --------------------------------
    public void addLever (
        int  ctcId)
    {
        if (lvr.length <= ctcId)  {
            System.err.format ("Error Lever.addLever range %d\n", ctcId);
            System.exit (3);
        }

        lvr [ctcId]++;
    }

    // --------------------------------
    public boolean check (
        int  ctcId)
    {
        return (0 < lvr [ctcId]);
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

 //     CtcCol ctc  = ctcCol [col];
 //     PnlSym lvr  = ctcCol [col].symLvr;
 //     PnlSym to   = ctcCol [col].symTo;

        // label turnouts
 //     for ( ; to != null; to = to.nxtSym)
 //         g2d.drawString (to.lbl, to.x + to.xLbl, to.y + to.yLbl);

        // plate & lvr
        g2d.drawImage (turnout [col/2].img,   x0, y0, null);
     // g2d.drawImage (lever   [ctc.pos].img, x0 + 6, y0 + 44, null);
        g2d.drawImage (lever   [0]      .img, x0 + 6, y0 + 44, null);

     // to   = ctcCol [col].symTo;

        // lamps
        int lampIdxLeft  = 5;
        int lampIdxRight = 1;

     // if ('l' == lvr.cond)  {
        if (true)  {
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
        int         lvrIdx )
    {
//      if (0 != dbg)
//          System.out.format ("  paintSigPlate: %3d %3d, %d\n", x0, y0, lvrIdx);

//      final int  SigRred = 16;
//      final int  SigLred = 17;
//      final int  SigRgr  = 46;
//      final int  SigLgr  = 47;

//      g2d.setColor (Color.white);

//      CtcCol ctc  = ctcCol [col];
//      PnlSym lvr  = ctcCol [col].symLvr;
//      PnlSym symL = ctcCol [col].symSigL;
//      PnlSym symR = ctcCol [col].symSigR;

//      int    xOff = tileWid * 5/4;
//      int    yOff = tileWid * 3/4;

//      // signal labels
//      for (symL = ctcCol [col].symSigL; symL != null; symL = symL.nxtSym)
//          g2d.drawString (symL.lbl, symL.x + xOff, symL.y + yOff);

//      for (symR = ctcCol [col].symSigR; symR != null; symR = symR.nxtSym)  {
//          int xOff2 = 5 + g2d.getFontMetrics().stringWidth (symR.lbl);
//          g2d.drawString (symR.lbl, symR.x - xOff2, symR.y + yOff);
//      }

        // plate & lvr
        g2d.drawImage (signal [col/2].img,   x0, y0, null);
     // g2d.drawImage (lever  [ctc.pos].img, x0 + 5, y0 + 57, null);
        g2d.drawImage (lever  [2].img,       x0 + 5, y0 + 57, null);

//      if (false)
//          System.out.format (
//              "paintSigPlate: Num %d, lamp %c\n", col, ctcCol [col].lamp);

//      // lamps
//      if ('L' == ctcCol [col].lamp)  {                    // left
//          g2d.drawImage (imgLamp [6].img,  x0 +  5, y0 + 17, null);
//          g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, null);
//          g2d.drawImage (imgLamp [5].img,  x0 + 34, y0 + 18, null);
//      }
//      else if ('R' == ctcCol [col].lamp)  {               // left
//          g2d.drawImage (imgLamp [5].img,  x0 +  5, y0 + 17, null);
//          g2d.drawImage (imgLamp [9].img,  x0 + 18, y0 +  6, null);
//          g2d.drawImage (imgLamp [6].img,  x0 + 34, y0 + 18, null);
//      }
//      else  {                                             // center
            int idxLeft   = 5;
            int idxCenter = 10;
            int idxRight  = 5;
//      }
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
        System.out.format ("Lever paint:\n");

 //     Rectangle   r      = frame.getBounds();
        int         y1     = y0 + iconToHt;
        int         y2     = y1 + iconSigHt;

        g2d.setColor (new Color(115, 104, 50));  // #736832
        g2d.fillRect (0, y0, wid, y0+ht);
 
        for (int num = 1; num < lvr.length; num += 2)  {
            if (0 == lvr [num] && 0 == lvr [num+1])
                continue;

            int col = 1 + (num-1) / 2;
            int x0  = colWid * ((num-1) / 2);

            if (0 < lvr [num])
                paintToPlate  (g2d, x0, y0, num, num);

            g2d.drawLine (0, y2, wid, y2);

            if (0 < lvr [num+1])
                paintSigPlate (g2d, x0, y1, num, num);

            // code button
            Image img = code [1].img;
         // if (' ' != symCode [col].cond)
         //     img  = imgCode [0].img;
            g2d.drawImage (img, x0 + 15, y2 + 10, null);
        }
    }
};
