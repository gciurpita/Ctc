    // -------------------------------------
    public class SigList  {
        Sym     sym;
        SigList next;

        public SigList (
            Sym     sym,
            SigList sigList )
        {
            this.sym  = sym;
            this.next = sigList;
        }
    }

