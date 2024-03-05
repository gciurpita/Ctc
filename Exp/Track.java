
public class Track {
    final int Col  = 100;
    final int Row  = 10;
    byte      trk [][]  = new byte [Col][Row];
    int       nRow = 0;
    int       maxCol = 0;

    // ------------------------------------------------------------------------
    public boolean check (
        int     col,
        int     row,
        char    type )
    {
        byte tile = trk [col][row];

     // System.out.format ("  Track.check: 0x%02x %2d x %2d\n", tile, col, row);
        
        if ('*' == type)
            return (16 <= tile && tile <= 17);
        else if ('B' == type)
            return (5 == tile);
        else if ('x' == type)
            return (8 <= tile && tile <= 11);

        System.out.format ("Error Track.check unknown type '%c'\n", type);
        System.exit (2);

        return false;
    }

    // ------------------------------------------------------------------------
    public void disp ()
    {
        System.out.format (" Track.disp: %2d x %2d\n", maxCol, nRow);

        for (int row = 0; row < nRow; row++)  {
            System.out.format ("    ");
            for (int col = 0; col < maxCol; col++)
                System.out.format ("%c", trk [col][row] + '0');
            System.out.println ();
        }
    }

    // ------------------------------------------------------------------------
    public void newRow (
        String  s )
    {
        char c;
     // System.out.format ("  Track.newRow: %s\n", s);

        if (Row <= nRow)  {
            System.out.format ("Error Track.newRow: row lime %d %d\n", nRow);
            System.exit (3);
        }

        for (int col = 0; col < s.length (); col++)  {
            c = s.charAt (col);
            if (' ' == c)
                c = 0;
            else
                c -= '0';
            trk [col][nRow] = (byte)c;
        }
        nRow++;

        if (maxCol < s.length ())
            maxCol = s.length ();
    }
}
