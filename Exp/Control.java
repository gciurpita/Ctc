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

    // ---------------------------------------------------------
    public void set (
        Mqtt  mqtt )
    {
        this.mqtt = mqtt;
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

        if (null != mqtt)
            mqtt.publish (id, "/" + state);
    }

    // ---------------------------------------------------------
    public void receive (
        Track  track,
        Panel  panel )
    {
        if (null == cmd)
            return;

        if (0 < cmd.delay--)
            return;

        System.out.format (
            " receive: %c %5s %c\n", cmd.type, cmd.id, cmd.state);

        if ('T' == cmd.type || 'S' == cmd.type)
            track.update (cmd.state, cmd.id);
        cmd = cmd.next;
    }
}
