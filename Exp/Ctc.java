public class Ctc {
    int     id;
    Ctc     next;

    // --------------------------------
    public Ctc (
        int   id )
    {
        this.id = id;
        next    = null;
     // System.out.format ("  Ctc: %d\n", this.id);
    }

    // --------------------------------
    public boolean check (
        int  ctcId)
    {
        for (Ctc ctc = this; null != ctc; ctc = ctc.next)  {
         // System.out.format ("  Ctc check: %d %d\n", ctc.id, ctcId);
            if (ctc.id == ctcId)
                return true;
        }

        return false;
    }
};
