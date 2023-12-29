// 

#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <unistd.h>

#include "sckt.h"

Sckt sckt;

char        *progname;
unsigned int debug = 0;
unsigned int flag  = 0;


// --------------------------------------------------------------------
void
shell (void)
{
    sckt.scktOpen (PORT, 1);

    unsigned char buf [BUFSIZ];
    int           nByte;

    while (true)  {
        nByte = sckt.receive (buf, BUFSIZ, 0);
        printf ("%s: nByte %d\n", __func__, nByte);
        sleep (1);
    }
}

// --------------------------------------------------------------------
void
application (char *filename)  {
    FILE  *fp;
    char   s[BUFSIZ];

    printf ("%s: %s\n", progname, filename);

    if ( (fp = fopen (filename, "rb")) == NULL)  {
        perror ("app - fopen input");
        exit (1);
    }


    while (fgets (s, BUFSIZ, fp) != NULL)  {
        printf ("%s", s);
    }
}

// --------------------------------------------------------------------
void help (void)  {
    printf (" Usage: %s \n", progname);
}

// --------------------------------------------------------------------
int main (int argc, char **argv)  {
    int   c;

    progname = *argv;

    while ((c = getopt(argc, argv, "D:o")) != -1)  {
        switch (c)  {
        case 'D':
            debug = atoi (optarg);
            break;

        case 'o':
            break;

        default:
            printf ("Error: unknown option '%c'\n", optopt);
                        help();
            break;
        };

    }

#if 0
    if (optind == argc)
        help();
    else
        for ( ; optind < argc; optind++)
            application (argv[optind]);

#else
    shell ();
#endif

    return 0;
}
