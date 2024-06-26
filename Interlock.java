// CTC interlock tester

import java.awt.*;
import java.io.*;
import java.lang.*;
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

// -----------------------------------------------------------------------------
public class Interlock extends JPanel
        implements MouseListener, KeyListener
{
    JFrame   frame    = new JFrame ();

    SymList  symList  = new SymList ();
    Control  ctl      = new Control (symList);
    Panel    panel    = new Panel   (ctl,   symList);
    Track    trk      = new Track   (panel, symList);

    Mqtt     mqtt;

    int      xPos     = 500;
    int      yPos     = 0;
    int      canvasHt;
    int      canvasWid;
    int      colWid;
    int      nCol;
    int      tileWid;
    int      trkHt;
    int      lvrHt;

    String   title;

    // --------------------------------
    public Interlock (
        String pnlFile )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        addKeyListener   (this);
        addMouseListener (this);

        loadPnl (pnlFile);

        // set up screen graphics
        tileWid   = trk.tileWid;
        canvasWid = trk.panelWid;
        colWid    = 4 * tileWid;

        nCol      = canvasWid / colWid;
        trkHt     = trk.panelHt;
        lvrHt     = panel.colHt;

        canvasHt  = trkHt + lvrHt;

        this.setPreferredSize (new Dimension (canvasWid, canvasHt));

        if (false) {
            System.out.format ("Interlock: tile wid %d", tileWid);
            System.out.format (", CTC col wid %d", colWid);
            System.out.format (", canvas wid %d", canvasWid);
            System.out.format (", canvas ht %d", canvasHt);
            System.out.println ();
        }

        frame.setContentPane (this);
        frame.pack ();
        frame.setDefaultCloseOperation (JFrame.EXIT_ON_CLOSE);
        frame.setVisible (true);
        frame.setTitle   (title);

        // position app near top center of screen
        Rectangle r = frame.getBounds();        // window size
        frame.setBounds (xPos, yPos, r.width, r.height);

        // create timer
        TimerTask task = new TimerTask() {
            public void run() {
                timerTask ();
            }
        };

        Timer timer = new Timer("Timer");
        timer.scheduleAtFixedRate (task, 0, 1000);
    }

    // --------------------------------
    //   timer task
    private void timerTask ()
    {
        ctl.heartbeat ();
        int msgType = ctl.receive (trk, panel);
        panel.timer ();

        repaint ();
    }

    // --------------------------------
    public Interlock (
        String pnlFile,
        String cmdFile )
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        loadPnl (pnlFile);

        // ???????????????????

        cmdProcess (cmdFile);
    }

    // ------------------------------------------------------------------------
    // main processes command line arguments settign the debug level and

    public static void main (String[] args)
            throws FileNotFoundException, IOException, IllegalArgumentException
    {
        String cmdFname = null;
        int    i;

        for (i = 0; i < args.length; i++) {
            System.out.format ("main: %d %s\n", i, args[i]);

            if (args [i].startsWith("-"))  {
                if (args [i].startsWith ("-c"))
                    cmdFname = args [i].substring (2);
            }
            else
                break;
        }

        Interlock intrLck;
        if (null != cmdFname)
            intrLck = new Interlock (args [i], cmdFname);
        else
            intrLck = new Interlock (args [i]);
    }

    // --------------------------------
    public void keyReleased (KeyEvent e) { }
    public void keyPressed  (KeyEvent e) { }

    public void keyTyped    (KeyEvent e)
    {
     // System.out.println ("keyTyped:");
        char    c = e.getKeyChar();
        switch (c)  {
            case 'b':
                System.out.println ("--------------------");
                break;

            case 'd':
                symList.disp ();
                break;

            case 'l':
                symList.locked ();
                break;

            case 'u':
                symList.unlock ();
                break;

            case '?':
                System.out.format ("key commands:\n");
                System.out.format ("    d - list syms\n");
                System.out.format ("    u - unlock all syms\n");
                break;

            default:
                System.out.format ("keyTyped: %c unexpected\n", c);
                break;
        }
    }

    // ------------------------------------------------------------------------
    // process mouse press, search for element closest to mouse location

    public void mousePressed  (MouseEvent e)
    {
     // System.out.format ("mousePressed: %d %d\n", e.getX(), e.getY());
        if (e.getY() < trkHt)
            trk.mousePressed (e.getX(), e.getY());
        else  {
            panel.mousePressed (e.getX(), e.getY());
        }

        requestFocusInWindow ();
        repaint ();
    }

    // ignore these mouse events
    public void mouseClicked  (MouseEvent e) { }
    public void mouseEntered  (MouseEvent e) { }
    public void mouseExited   (MouseEvent e) { }
    public void mouseReleased (MouseEvent e) { }

    // ------------------------------------------------------------------------
    //   load panel decription

    private void loadPnlErr (
        String line,
        String msg )
    {
        System.out.format ("Error: loadPnl - %s\n", msg);
        System.out.format ("    %s\n", line);
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

        int            err = 0;

        while ((line = br.readLine()) != null)  {
            String[]    fld = line.split("  *");
            if (dbg)
                System.out.format ("   loadPnl: %s - %s\n", line, fld [0]);

            // -----------------------------------
            if (fld[0].equals("#"))
                ;           // ignore

            // -----------------------------------
            else if (fld[0].equals("block"))  {
                int id      = Integer.parseInt (fld [1]);
                int row     = Integer.parseInt (fld [2]);
                int col     = Integer.parseInt (fld [3]);
                String sfx  = fld [4];

                String mqtt = null;
                if (5 < fld.length)
                    mqtt = fld [5];

                String name = "B" + Integer.toString (id);
                if (! sfx.equals("_"))
                    name += sfx;

                Sym sym = symList.add (name, 'B', id, mqtt);

                if (! trk.check (col, row, 'B', id, sym, fld [1]))  {
                    loadPnlErr (line, "invalid block tile");
                    err++;
                    continue;
                }

                System.out.format (" loadPnl block:   %s\n", name);
            }

            // -----------------------------------
            else if (fld[0].equals("ctc"))  {
                for (int n = 1; n < fld.length; n++)  {
                    int num = Integer.valueOf (fld [n]);
                    Sym sym = symList.add ("L" + fld [n], 'L', num, null);
                    panel.addLever (Integer.parseInt (fld [n]), sym);
                }
            }

            // -----------------------------------
            // lock for manual turnout
            else if (fld[0].equals("lock"))  {
                for (int n = 1; n < fld.length; n++)  {
                    int num = Integer.valueOf (fld [n]);
                    if (0 == num % 2)  {
                        loadPnlErr (line, "invalid lock, must be odd");
                        err++;
                        continue;
                    }

                    if (false)  {
                    Sym symCtc = symList.findName ("L" + fld [n]);
                    if (null == symCtc)  {
                        loadPnlErr (line, "invalid lock id");
                        err++;
                        continue;
                    }
                    }

                    Sym sym = symList.add ("K" + fld [n], 'K', num, null);
                    panel.associate (num, sym);

                    System.out.format (
                        " loadPnl lock:    %-4s %2d\n", sym.name, num);
                }

            }

            // -----------------------------------
            else if (fld[0].equals("mqtt"))  {
                if (5 > fld.length)  {
                    loadPnlErr (line, "mqtt ip port name topic");
                    err++;
                    continue;
                }

                if (4 > fld.length)  {
                    loadPnlErr (line, "mqtt ipAddr port node-name");
                    err++;
                    continue;
                }

                symList.disp ();

                String ip    = fld [1];
                String port  = fld [2];
                String name  = fld [3];
                String topic = fld [4];

                mqtt = new Mqtt (ip, port, name, topic);
                ctl.set (mqtt);

                // generate subscriptions
                Sym sym = symList.getNext (null);

                for ( ; null != sym; sym = symList.getNext (sym))  {
                    switch (sym.type)  {
                    case 'B':
                    case 'T':
                        String subTopic = sym.getSubTopic ();
                        if (false)
                            System.out.format (
                                "\nInterlock mqtt: subscribe %s\n", subTopic);
                        mqtt.subscribe (subTopic);
                        ctl.receive (trk, panel, mqtt.SubAck);
                        break;
                    }
                }
            }

            // -----------------------------------
            else if (fld[0].equals("pos"))  {
                xPos = Integer.valueOf (fld [1]);
                yPos = Integer.valueOf (fld [2]);
            }

            // -----------------------------------
            else if (fld[0].equals("row"))  {
                trk.newRow (line.substring (4));
            }

            // -----------------------------------
            else if (fld[0].equals("rule"))  {
                Sym sym = symList.find (fld [1], 'S');
                if (null == sym)  {
                    loadPnlErr (line, "unknown sym");
                    err++;
                    continue;
                }
                System.out.format (" loadPnl addrule: %s\n", fld [1]);
                sym.addRule (fld, symList);
            }

            // -----------------------------------
            else if (fld[0].equals("signal"))  {
                if (5 > fld.length)  {
                    loadPnlErr (line, "signal requires 5 parameters");
                    err++;
                    continue;
                }

                int ctcNum = Integer.parseInt (fld [1]);
                int row    = Integer.parseInt (fld [2]);
                int col    = Integer.parseInt (fld [3]);
                String sfx = fld [4];

                String mqtt = null;
                if (5 < fld.length)
                    mqtt = fld [5];

                String name = Integer.toString (ctcNum);
                if (! sfx.equals("_"))
                    name += sfx;

                Sym sym = symList.add (name, 'S', ctcNum, mqtt);

                if (! panel.associate (ctcNum, sym)) {
                    loadPnlErr (line, "invalid ctc ID");
                    err++;
                    continue;
                }

                if (! trk.check (col, row, 'S', ctcNum, sym, name))  {
                    loadPnlErr (line, "invalid track tile");
                    err++;
                    continue;
                }

                System.out.format (" loadPnl signal:  %s\n", name);
            }

            // -----------------------------------
            else if (fld[0].equals("text"))  {
             // int col     = Integer.parseInt (fld [1]);
             // int row     = Integer.parseInt (fld [2]);
                float col   = Float.parseFloat (fld [1]);
                float row   = Float.parseFloat (fld [2]);

             // trk.addText (col, row, fld [3]);
                trk.addText (col, row, line.substring (line.indexOf (fld [3])));
            }

            // -----------------------------------
            else if (fld[0].equals("title"))  {
                title = line.substring (6);
            }

            // -----------------------------------
            else if (fld[0].equals("turnout"))  {
                if (4 > fld.length)  {
                    loadPnlErr (line, "turnout requires 4 parameters");
                    err++;
                    continue;
                }

                int ctcNum  = Integer.parseInt (fld [1]);
                int row     = Integer.parseInt (fld [2]);
                int col     = Integer.parseInt (fld [3]);
                String sfx  = fld [4];

                String mqtt = null;
                if (5 < fld.length)
                    mqtt = fld [5];

                String name = Integer.toString (ctcNum);
                if (! sfx.equals("_"))
                    name += sfx;

                Sym sym = symList.add (name, 'T', ctcNum, mqtt);

                if (! panel.associate (ctcNum, sym)) {
                    loadPnlErr (line, "invalid ctc ID");
                    err++;
                    continue;
                }

                if (! trk.check (col, row, 'T', ctcNum, sym, name))  {
                    loadPnlErr (line, "invalid track tile");
                    err++;
                    continue;
                }

                System.out.format (" loadPnl turnout: %s\n", name);
            }

            // -----------------------------------
            else if (0 < line.length()) {
                loadPnlErr (line, "unknown keyword");
                err++;
                continue;
            }
        }

        symList.disp ();

        if (0 < err)  {
            System.out.format ("loadPnl: %d errors\n", err);
            System.exit (1);
        }
    }

    // ------------------------------------------------------------------------
    //   cmd process
    private void cmdProcess (String cmdFile)
            throws FileNotFoundException, IOException
    {
        boolean dbg = false;
        System.out.format ("\ncmdProcess: %s\n", cmdFile);

        BufferedReader br = new BufferedReader(new FileReader (cmdFile));
        String         line;

        while ((line = br.readLine()) != null)  {
            if (0 == line.length ())
                continue;

            System.out.format (" cmdProcess: %s\n", line);
            if ('#' == line.charAt (0))
                continue;

            String[]    fld = line.split("  *");

            // ---------------------------
            if (fld [1].equals ("check"))
                ; // symList.checkRules ();

            // ---------------------------
            else if (fld [1].equals ("dispRules"))
                symList.dispRules ();

            // ---------------------------
            else if (fld [1].equals ("list"))
                symList.disp ();

            // ---------------------------
            else if (fld [1].equals ("quit"))
                System.exit (0);

            // ---------------------------
            else if (fld [1].equals ("set"))  {
                if (fld .length < 3)  {
                    System.out.format ("Error - cmdProcess - set sym cond\n");
                    System.exit (2);
                }

                Sym sym = symList.find (fld [2], '_');
                if (null == sym)  {
                    System.out.format (
                        "Error - cmdProcess - set not found - %s\n", fld [2]);
                    System.exit (2);
                }

                if (0 != sym.lock)
                    System.out.format (
                        "  cmdProcess: set %s locked\n", sym.name);
                else
                    sym.cond = fld [3].charAt (0);
            }

            // ---------------------------
            else {
                System.out.format (
                        "Error - cmdProcess - unknown - %s\n", fld [1]);
                System.exit (2);
            }
        }
    }

    // ------------------------------------------------------------------------
    // redraw the screen

    @Override
    public void paintComponent (Graphics g)
    {
        Graphics2D  g2d = (Graphics2D) g;
        Rectangle   r   = frame.getBounds();

        g2d.setColor (Color.gray);
        g2d.fillRect (0, 0, r.width, r.height);

        trk.paint (g2d);
        panel.paint (g2d, trkHt, canvasWid, lvrHt);
    }
}
