
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.*;

// -----------------------------------------------------------------------------
// MQTT interface

class Mqtt
{
    Sckt    sckt;
    int     nextMsgId;
    
    byte Connect     = 0x1;  // Client request to connect to Server
    byte ConnAck     = 0x2;  // Connect ACK
    byte Publish     = 0x3;  // Publish message
    byte PubAck      = 0x4;  // Publish ACK
    byte PubRec      = 0x5;  // Publish Received (assured delivery part 1)
    byte PubRel      = 0x6;  // Publish Release  (assured delivery part 2)
    byte PubComp     = 0x7;  // Publish Complete (assured delivery part 3)
    byte Subscribe   = 0x8;  // Client Subscribe request
    byte SubAck      = 0x9;  // Subscribe ACK
    byte UnSubscribe = 0xA;  // Client Unsubscribe request
    byte UnSubAck    = 0xB;  // Unsubscribe ACK
    byte PingReq     = 0xC;  // PING Request
    byte PingResp    = 0xD;  // PING Response
    byte Disconnect  = 0xE;  // Client is Disconnecting
    byte Reserved    = 0xF;  // Reserved

    // -------------------------------------
    Mqtt (
 //     Sckt    sckt,
        String  ipAddr,
        String  portStr,
        String  nodeName )     throws IOException
    {
        System.out.format ("Mqtt: %s %s %s\n", ipAddr, portStr, nodeName);
    
        int  port = Integer.parseInt (portStr);
        sckt = new Sckt (ipAddr, port);
    
        connect (nodeName);
    }
    
    // -------------------------------------
    void connect (
        String nodeName )
    {
        byte ver [] = { 0, 4, 'M', 'Q', 'T', 'T', 4 };
        byte [] buf = new byte [90];
        int  idx = 2;
    
        for (int i = 0; i < ver.length; i++)
            buf [idx++] = ver [i];
    
        buf [idx++] = 2;        // connect flag
        
        buf [idx++] = 0;        // keep alive
        buf [idx++] = 120;
    
        // node name
        buf [idx++] = 0;        // type (nul)
        buf [idx++] = (byte) nodeName.length ();
    
        for (int i = 0; i < nodeName.length(); i++)
            buf [idx++] = (byte) nodeName.charAt (i);
        
        // header
        final byte Connect = (byte) 0x10;
        buf [0] = Connect;
        buf [1] = (byte) (idx -2);

        dump (buf, idx, "connect");

        if (null == sckt)  {
            System.out.format ("connect: sckt null\n");
            System.exit (1);
        }
        sckt.write (buf, idx);
    
        nextMsgId = 2;
    }
    
    // -------------------------------------
    void dump (
        byte [] buf,
        int     nByte,
        String  label )
    {
        System.out.format ("dump: %d %s", nByte, label);

        for (int n = 0; n < nByte; n++)  {
            if (0 == (n % 16))
                System.out.format ("\n %02x:", n);
            else if (0 == (n % 4))
                System.out.format (" ");
            System.out.format (" %02x", buf [n]);
        }
        System.out.println ();
    }
    
    // -------------------------------------
    void send (
        byte [] buf,
        int     nByte )
    {
    }
}
