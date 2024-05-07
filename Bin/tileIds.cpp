// 

#include <stdio.h>
#include <stdlib.h>
#include <getopt.h>
#include <string.h>

char        *progname;
unsigned int debug = 0;
unsigned int flag  = 0;


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

    int idx = 0;
    while (fgets (s, BUFSIZ, fp) != NULL)  {
        char  fname [60];
        int   id;

        sscanf (s, "%*s %*s %d %s\n", &id, fname);
        strtok (s, "/");
        strtok (NULL, "/");
        char *p = strtok (NULL, "/");

        printf ("  %4d", id);
        printf ("  %4d", idx);
        printf ("  %c",  '0' + id);
        printf ("  %s", p);
        idx++;
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

    if (optind == argc)
        help();
    else
        for ( ; optind < argc; optind++)
            application (argv[optind]);

    return 0;
}
