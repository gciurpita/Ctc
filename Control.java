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
         // delay      = 3;
        }
    }

    // ---------------------------------------------------------
    Cmd     cmd   = null;
    Mqtt    mqtt  = null;
    Sckt    sckt  = null;
    SymList symList;

    int  delay;
    int  pingCnt;

    // ---------------------------------------------------------
    Control (
        SymList symList )
    {
        this.symList = symList;
    }

    // ---------------------------------------------------------
    public void cmdDisp (
        String  label,
        Cmd     cmd )
    {
        if (true)
            System.out.format (
                "%s: %c %s %c\n", label, cmd.type, cmd.id, cmd.state);
    }

    // -------------------------------------
    public void cmdDisps ()
    {
        for (Cmd c = cmd; null != c; c = c.next)
            cmdDisp (" cmdDisps", c);
    }

    // ---------------------------------------------------------
    public Cmd getCmd ()
    {
        if (null == cmd)
            return null;

        if (0 < delay--)
            return null;

        Cmd  cmd = this.cmd;
        this.cmd = this.cmd.next;

        cmdDisp ("  getCmd ", cmd);
        return cmd;
    }

    // ---------------------------------------------------------
    public void heartbeat ()
    {
        if (null != mqtt)  {
            if (100 <= ++pingCnt)  {        // seconds
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
        if (true)
            System.out.println ("Control.processCmd");

        cmdDisp ("   processCmd", cmd);
        track.update (cmd.type, cmd.state, cmd.id);

        // check if block "knocks down" signal
        if ('B' == cmd.type)  {
            Sym      sym = symList.findName (cmd.id);
            RuleList rL  = sym.ruleList;

            if (null != sym.ruleList)  {
                if ('O' == cmd.state)  {
                    Sym symSig = rL.sym;

                    System.out.format (
                        "   processCmd: blk %s occupied, sig %s\n",
                                sym.name, symSig.name );

                    track.update (symSig.type, 'S', symSig.name);
                }
                else  {
                    System.out.format (
                        "   processCmd: blk %s unoccupied\n", sym.name);
                    if (0 < sym.lock)
                        rL.uncheck ();
                }
            }
        }
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

        cmdDisp (" Control.receiveMqtt:", cmd);

     // processCmd (track, cmd);

        return 0;
    }

    // ---------------------------------------------------------
    public int receive (
        Track  track,
        Panel  panel )
    {
        if (false)
            System.out.format ("Control.receive:\n");

        // -------------------------------------
        Cmd       cmd;
        while (null != (cmd = getCmd()))
            processCmd (track, cmd);

        // -------------------------------------
        if (null != mqtt)
            return receiveMqtt (track, panel);

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

        delay    = 2;

        cmdDisp ("send", cmd);
        cmdDisps ();

        if (null != mqtt)  {
            String topic = String.format ("%c/%s/", type, id);
            mqtt.publish (topic, String.valueOf(state));
        }
    }

    // ---------------------------------------------------------
    public void set (
        Mqtt  mqtt )
    {
        this.mqtt = mqtt;
    }
}
