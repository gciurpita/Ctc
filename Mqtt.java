
import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.*;

// -----------------------------------------------------------------------------
// MQTT interface

class Mqtt
{
    String  topic;

    Sckt    sckt;
    int     nextMsgId;
    boolean dbg = false;

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

    byte Qos0        = 0x00;
    byte Qos1        = 0x02;
    byte Qos2        = 0x04;

    byte Retain      = 1;

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
        String  nodeName,
        String  topic )
                throws IOException
    {
        System.out.format ("Mqtt: %s %s %s\n", ipAddr, portStr, nodeName);

        int  port = Integer.parseInt (portStr);
        sckt = new Sckt (ipAddr, port);

        connect (nodeName);
        this.topic = topic;
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

        if (false)
            dump (buf, idx, "connect");

        if (null == sckt)  {
            System.out.format ("connect: sckt null\n");
            System.exit (1);
        }
        sckt.write (buf, idx);

        nextMsgId = 2;

        waitFor (ConnAck, false);
    }

    // -------------------------------------
    void dump (
        byte [] buf,
        int     nByte,
        String  label )
    {
        if (! dbg)
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
        String  subTopic,
        String  value )
    {
        System.out.format ("  publish: %s %s\n", subTopic, value);

        byte [] buf = new byte [90];
        int  idx = 2;

        nextMsgId++;

        // topic name
        subTopic = topic + "/" + subTopic;

        buf [idx++] = 0;
        buf [idx++] = (byte) subTopic.length ();
        for (int i = 0; i < subTopic.length(); i++)
            buf [idx++] = (byte) subTopic.charAt (i);

        // value
        for (int i = 0; i < value.length(); i++)
            buf [idx++] = (byte) value.charAt (i);

        // header
        buf [0] = (byte)(Publish | Retain);
        buf [1] = (byte) (idx -2);

        dump (buf, idx, "publish");

        sckt.write (buf, idx);
    }

    // -------------------------------------
    int receive (
        byte    buf [],
        int     nByte,
        boolean dbg )
    {
        nByte = sckt.readPckt (buf, nByte);

        if (0 == nByte)
            return 0;

        dump (buf, nByte, "mqtt.receive");

        byte id = (byte) ((buf [0] >> 4) & 0x0F);

        if (dbg)
            System.out.format (
                "mqtt.receive: 0x%02x %s\n", id, MsgName [id]);

        return nByte;
    }

    // -------------------------------------
    int receive (
        byte    buf [],
        int     nByte )
    {
        return receive (buf, nByte, false);
    }

    // -------------------------------------
    void subscribe (
        String  subTopic )
    {
        boolean dbg = false;

        String fullTopic =  topic + "/" + subTopic;
        System.out.format ("  subscribe: %s\n", fullTopic);

        byte [] buf = new byte [90];
        int  idx = 2;

        nextMsgId++;

        // nextMsgId
        buf [idx++] = (byte) (nextMsgId >> 8);
        buf [idx++] = (byte)  nextMsgId;

        // subscription name
        int len = fullTopic.length ();
        buf [idx++] = (byte) (len >> 8);
        buf [idx++] = (byte)  len;
        for (int i = 0; i < len; i++)
            buf [idx++] = (byte) fullTopic.charAt (i);
        idx++;

        // header
        buf [0] = (byte) (Subscribe | Qos1);
        buf [1] = (byte) (idx -2);

     // dump (buf, idx, "subscribe");

        sckt.write (buf, idx);
    }

    // -------------------------------------
    void waitFor (
        byte    msgHdr,
        boolean dbg )
    {
        if (dbg)
            System.out.format ("Mqtt.waitFor: 0x%02x\n", msgHdr);
        final int BufSize = 90;
        byte      [] buf  = new byte [BufSize];
        do {
            while (0 == receive (buf, BufSize))
                ;
        } while (msgHdr != (byte) (buf [0] & 0xF0));
        if (dbg)
            System.out.format ("  Mqtt.waitFor:\n\n");
    }
}
