// Layout Interface


public class LayoutIf
{
    // ---------------------------------------------------------
    final int   QueueSize   = 10;
    Msg         cmds []  = new Msg [QueueSize];
    int         idxSend  = 0;
    int         idxRec   = 0;

    // ---------------------------------------------------------
    public void send (
        char    type_,
        int     id_,
        char    state_ )
    {
        if (null == cmds [idxSend])  {
            cmds [idxSend] = new Msg ();
        }

        else if ('_' != cmds [idxSend].type)  {
            System.out.format ("send: idx %d in use\n", idxSend);
            System.exit (1);
        }

        cmds [idxSend].type  = type_;
        cmds [idxSend].id    = id_;
        cmds [idxSend].state = state_;

        System.out.format ("  send: %c %2d %c -- send %2d, rec %2d\n",
                cmds [idxSend].type, cmds [idxSend].id, cmds [idxSend].state,
                idxSend, idxRec);

        if (QueueSize <= ++idxSend)
            idxSend = 0;
    }

    // ---------------------------------------------------------
    public Msg receive ()
    {
        if (idxRec == idxSend)
            return null;

        System.out.format ("  receive: %c %2d %c -- send %2d, rec %2d\n",
                cmds [idxRec].type, cmds [idxRec].id, cmds [idxRec].state,
                idxSend, idxRec);

        Msg msg = cmds [idxRec];
        if (QueueSize <= ++idxRec)
            idxRec = 0;

        return msg;
    }
}
