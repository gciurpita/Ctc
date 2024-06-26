
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.*;

// -----------------------------------------------------------------------------
// socket interface

class Sckt {
    public static final byte   PKT_STRING      = 2;
    public static final byte   PKT_START       = 3;
    public static final byte   PKT_LVR_TO      = 4;
    public static final byte   PKT_LVR_SIG     = 5;
    public static final byte   PKT_HELP        = 6;

    Socket          socket;
    int             port        = 4303;     // simple RR cmd protocol
    InetAddress     address;

    DataInputStream in;
    PrintStream     out;

    // ---------------------------------------------------------
    Sckt (
        String hostName,
        int    port )      throws IOException
    {
        System.out.format ("Sckt: host %s, port %s  STREAM\n", hostName, port);

        // repeatedly try to connect to socket
        boolean conn;
        do {
            conn = true;
            try  {
                socket   = new Socket (hostName, port);
                address  = InetAddress.getByName(hostName);
            }
            catch (Exception e)
            {
                conn = false;
                System.out.format(" Sckt: open fail, %d\n", socket);
             // System.exit (2);
            }
        } while (! conn);

        out      = new PrintStream (socket.getOutputStream ());
        in       = new DataInputStream (
                                    new BufferedInputStream (
                                        socket.getInputStream ()));
    }

    // ---------------------------------------------------------
    public void sendPkt (byte pktId, byte[] buf, int bufSize)
    {
        byte[]  pktBuf   = new byte [256];
        int     i;

        System.out.format (" sendPkt: pktId %d, bufSize %d\n", pktId, bufSize);

        for (i = 0; i < bufSize; i++)  {
            pktBuf [i + 2] = buf [i];
        }

        pktBuf [0] = pktId;
        pktBuf [1] = (byte) (bufSize + 2);

        out.write (pktBuf, 0, pktBuf [1]);
    }

    // ---------------------------------------------------------
    public void send (String str)
    {
        System.out.format ("send: %s\n", str);

        byte[]  bufTx   = str.getBytes();

        sendPkt (PKT_STRING, bufTx, bufTx.length);
    }

    // ---------------------------------------------------------
    public void write (byte[] buf, int bufSize)
    {
        out.write (buf, 0, bufSize);
    }

    // -------------------------------------------------------------------
    // Read data from the server
    public  int readPckt (
        byte buf [],
        int  size )
    {
        int nRd = 0;
        int len = 0;

        try {
            if (0 == in.available ())
                return 0;
        }
        catch (Exception e) {
            e.printStackTrace ();
        }

        try {
            if (1 > (nRd = in.read (buf, 0, size)))  {
                System.out.format(" readPckt: nRd %d\n", nRd);
                if (0 > nRd)
                    System.exit (1);
                return 0;
            }
        }
        catch (Exception e) {
            e.printStackTrace ();
        }

        return nRd;
    }
}
