public class Sym  {
    String  name;
    char    type;
    char    cond;
    Rule    rule;
    Sym     next;

    // -------------------------------------
    public Sym (
        String  name,
        char    type )
    {
        this.name = name;
        this.type = type;
        if ('*' == type)
            this.cond = 'S';
        else
            this.cond = ' ';
    }
}
