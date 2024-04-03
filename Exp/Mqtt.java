
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

    byte Connect     = (byte) 0x10;  // Client request to connect to Server
    byte ConnAck     = (byte) 0x20;  // Connect ACK
    byte Publish     = (byte) 0x30;  // Publish message
    byte PubAck      = (byte) 0x40;  // Publish ACK
    byte PubRec      = (byte) 0x50;  // Publish Received (assured delivery)
    byte PubRel      = (byte) 0x60;  // Publish Release  (assured delivery)
    byte PubComp     = (byte) 0x70;  // Publish Complete (assured delivery)
    byte Subscribe   = (byte) 0x80;  // Client Subscribe request
    byte SubAck      = (byte) 0x90;  // Subscribe ACK
    byte UnSubscribe = (byte) 0xA0;  // Client Unsubscribe request
    byte UnSubAck    = (byte) 0xB0;  // Unsubscribe ACK
    byte PingReq     = (byte) 0xC0;  // PING Request
    byte PingResp    = (byte) 0xD0;  // PING Response
    byte Disconnect  = (byte) 0xE0;  // Client is Disconnecting
    byte Reserved    = (byte) 0xF0;  // Reserved

    byte QOS0        = 0x00;
    byte QOS1        = 0x02;
    byte QOS2        = 0x04;

    String MsgName [] = {
        "Zeroeth",
        "Connect",
        "ConnAck",
        "Publish",
        "PubAck",
        "PubRec",
        "PubRel",
        "PubComp",
        "Subscribe",
        "SubAck",
        "UnSubscribe",
        "UnSubAck",
        "PingReq",
        "PingResp",
        "Disconnect",
        "Reserved",
    };

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
        if (true)
            return;

        System.out.format ("  dump: %d %s", nByte, label);

        for (int n = 0; n < nByte; n++)  {
            if (0 == (n % 16))
                System.out.format ("\n    %02x:", n);
            else if (0 == (n % 4))
                System.out.format (" ");
            System.out.format (" %02x", buf [n]);
        }
        System.out.println ();
    }

    // ---------------------------------------------------------
    public void ping ()
    {
        byte [] buf = { PingReq, 0 };

        sckt.write (buf, 2);
    }

    // ---------------------------------------------------------
    public void publish (
        String  topic,
        String  value )
    {
        System.out.format ("publish: %s %s\n", topic, value);

        byte [] buf = new byte [90];
        int  idx = 2;

        nextMsgId++;

        // topic name
        buf [idx++] = 0;
        buf [idx++] = (byte) topic.length ();
        for (int i = 0; i < topic.length(); i++)
            buf [idx++] = (byte) topic.charAt (i);

        // value
        for (int i = 0; i < value.length(); i++)
            buf [idx++] = (byte) value.charAt (i);

        // header
        buf [0] = Publish;
        buf [1] = (byte) (idx -2);

        dump (buf, idx, "publish");

        sckt.write (buf, idx);
    }

    // -------------------------------------
    int receive (
        byte buf [],
        int  nByte )
    {
        nByte = sckt.readPckt (buf, nByte);

        dump (buf, nByte, "mqtt.receive");
        byte id = (byte) ((buf [0] >> 4) & 0x0F);
        System.out.format (
            "mqtt.receive: 0x%02x %s\n", id, MsgName [id]);
        return nByte;
    }

    // -------------------------------------
    void subscribe (
        String  topic )
    {
        System.out.format ("subscribe: %s\n", topic);

        byte [] buf = new byte [90];
        int  idx = 2;

        nextMsgId++;

        // nextMsgId
        buf [idx++] = (byte) (nextMsgId >> 8);
        buf [idx++] = (byte) (nextMsgId & 0x0F);

        // topic name
        for (int i = 0; i < topic.length(); i++)
            buf [idx++] = (byte) topic.charAt (i);

        // header
        buf [0] = (byte) (Subscribe | QOS1);
        buf [1] = (byte) (idx -2);

        sckt.write (buf, idx);
    }
}
