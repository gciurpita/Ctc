// 

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <getopt.h>
#include <unistd.h>

#include "sckt.h"

Sckt sckt;

char        *progname;
unsigned int debug = 0;
unsigned int flag  = 0;

int col;
int pos;

// --------------------------------------------------------------------
enum {
    PktString = 2,
    PktStart,
    PktLvrTo,
    PktLvrSig,
    PktHelp,
};

const char *pktTypeStr [] = {
    "PktId0",
    "PktId1",
    "PktString",
    "PktStart",
    "PktLvrTo",
    "PktLvrSig",
    "PktHelp",
};

const char *lvrStr [] = {
    "Left",
    "Right",
    "Center",
};

// ----------------------------
void
pktDecode (
    uint8_t *buf,
    int      nByte )
{
    uint8_t  type = buf [0];
    uint8_t  len  = buf [1];

    switch (type) {
    case PktLvrSig:
    case PktLvrTo:
        if (len < 4)
            printf (" %s: incomplete %s pkt - %d bytes\n",
                                        pktTypeStr [type], len);
        else  {
            col = buf [2] + 1;
            pos = buf [3];
            printf (" %s: %s col %d, pos %s\n", __func__,
                           pktTypeStr [type], col, lvrStr [pos]);
        }
        break;

    case PktString:
        printf (" %s: sring - %s\n", __func__, & buf [2]);
        break;

    case PktStart:
        printf (" %s: start\n", __func__);
        break;

    default:
        printf (" %s: invalid pkt - %d\n", __func__, type);
        break;

    }
};

// --------------------------------------------------------------------
void
pktDump (
    uint8_t *buf,
    int      nByte )
{
    printf ("%s:", __func__);
    printf (" %d %9s", buf [0], pktTypeStr [buf [0]]);
    printf (", len %d", buf [1]);
    for (int n = 2; n < nByte; n++)
        printf (" %02x", buf [n]);
    printf ("\n");
}

// --------------------------------------------------------------------
void
shell (void)
{
    sckt.scktOpen (PORT, 1);

    uint8_t  buf [BUFSIZ];
    int      nByte;

    while (true)  {
        nByte = sckt.receive (buf, BUFSIZ, 0);
        if (nByte)
#if 0
            printf ("%s: nByte %d\n", __func__, nByte);
#elif 0
            pktDump (buf, nByte);
#else
            pktDecode (buf, nByte);
#endif
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
