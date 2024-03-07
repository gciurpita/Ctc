// Layout Interface

public class Control
{
    private class Cmd   {
        char    type;   // 'T' - turnout, 'S' - signal, '_' - unused
        int     id;
        char    state;  // T - 'N'/'R', S - 'S'/'C'

        Cmd     next;
        int     delay;

        // -------------------------------------
        public Cmd (
        char    type,
        int     id,
        char    state )
        {
            this.type  = type;
            this.id    = id;
            this.state = state;
            next       = null;
            delay      = 5;
        }
    }

    // ---------------------------------------------------------
    Cmd  cmd = null;

    // ---------------------------------------------------------
    public void send (
        char    type,
        int     id,
        char    state )
    {
        Cmd cmd  = new Cmd (type, id, state);
        cmd.next = this.cmd;
        this.cmd = cmd;

        System.out.format ("send: %c %2d %c\n", cmd.type, cmd.id, cmd.state);
    }

    // ---------------------------------------------------------
    public void receive (
        Panel  panel )
    {
        if (null == cmd)
            return;

        if (0 < cmd.delay--)
            return;

        System.out.format (
            " receive: %c %2d %c\n", cmd.type, cmd.id, cmd.state);

        panel.response (cmd.type, cmd.id, cmd.state);
        cmd = cmd.next;
    }
}
