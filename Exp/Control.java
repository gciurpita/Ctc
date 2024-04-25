// Layout Interface

public class Control
{
    private class Cmd   {
        char    type;   // 'T' - turnout, 'S' - signal, '_' - unused
        String  id;     // interface, mqtt topic;
        char    state;  // T - 'N'/'R', S - 'S'/'C'

        Cmd     next;
        int     delay;

        // -------------------------------------
        public Cmd (
        char    type,
        String  id,
        char    state )
        {
            this.type  = type;
            this.id = id;
            this.state = state;
            next       = null;
            delay      = 5;
        }
    }

    // ---------------------------------------------------------
    Cmd  cmd   = null;
    Sckt sckt  = null;
    Mqtt mqtt  = null;

    int  pingCnt;

    // ---------------------------------------------------------
    public Cmd getCmd ()
    {
        if (null == cmd)
            return null;

        if (0 < cmd.delay--)
            return null;

        Cmd  cmd = this.cmd;
        this.cmd = this.cmd.next;

        return cmd;
    }

    // ---------------------------------------------------------
    public void heartbeat ()
    {
        if (null != mqtt)  {
            if (200 <= ++pingCnt)  {
                pingCnt = 0;
                mqtt.ping ();
            }
        }
    }

    // ---------------------------------------------------------
    public void processCmd (
        Track  track,
        Cmd    cmd )
    {
        System.out.format (
            " Control.process: %c %-5s %c\n", cmd.type, cmd.id, cmd.state);

        track.update (cmd.type, cmd.state, cmd.id);
    }

    // ---------------------------------------------------------
    public int receiveMqtt (
        Track  track,
        Panel  panel )
    {
        final int BufSize = 90;
        byte      [] buf  = new byte [BufSize];
        int       nByte;

        nByte = mqtt.receive (buf, BufSize, false);
        if (0 == nByte)
            return 0;

        byte msgType = (byte)(buf [0] & 0xF0);
        if (mqtt.Publish != msgType)
            return msgType;

        // separate and decode topic
        char [] charBuf = new char [40];
        for (int i = 0; i < buf [3]; i++)
            charBuf [i] = (char) buf [i+4];
        charBuf [buf [3]] = 0;

        String topic = new String(charBuf);
        String[] fld = topic.split("/");

        cmd = new Cmd (fld [1].charAt (0), fld [2], (char)buf [buf [1]+1]);

        System.out.format ("Control.receive:");
        System.out.format ("  type %c",  cmd.type);
        System.out.format (", id %s",    cmd.id);
        System.out.format (", state %c", cmd.state);
        System.out.println ();

        processCmd (track, cmd);

        return 0;
    }

    // ---------------------------------------------------------
    public int receive (
        Track  track,
        Panel  panel )
    {
        // -------------------------------------
        Cmd       cmd;
        if (null != (cmd = getCmd()))
            processCmd (track, cmd);

        // -------------------------------------
        if (null != mqtt)  {
            return receiveMqtt (track, panel);
        }

        return 0;
    }

    // ---------------------------------------------------------
    public void receive (
        Track  track,
        Panel  panel,
        int    msgType )
    {
        while (msgType != receive (track, panel))
            ;
    }

    // ---------------------------------------------------------
    public void send (
        char    type,
        String  id,
        char    state )
    {
        Cmd cmd  = new Cmd (type, id, state);
        cmd.next = this.cmd;
        this.cmd = cmd;

        System.out.format ("send: %c %s %c\n", cmd.type, cmd.id, cmd.state);

        if (null != mqtt)  {
            String topic = String.format ("%c/%s/", type, id);
            mqtt.publish (topic, String.valueOf(state));
        }
    }

    // ---------------------------------------------------------
 // public void send (
 //     String  topicName,
 //     char    state )
 // {
 //     Cmd cmd  = new Cmd (' ', topicName, state);
 //     cmd.next = this.cmd;
 //     this.cmd = cmd;

 //     System.out.format ("send: %s %c\n", topicName, cmd.state);

 //     if (null != mqtt)
 //         mqtt.publish (topicName, String.valueOf(state));
 // }

    // ---------------------------------------------------------
    public void set (
        Mqtt  mqtt )
    {
        this.mqtt = mqtt;
    }
}
