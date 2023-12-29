
#ifndef _SCKT_H_
# define _SCKT_H_

#include <stdio.h>
#include <string.h>

#include <cygwin/socket.h>
#include <netinet/in.h>

# define PORT      4303

// msg defines
enum {
    NULL_ID   = 0,
    BMP_ID,
    TXT_ID,
    RECT_ID,
    INFO_ID,
    LAST_ID,
};

# define PKT_STRING     2
# define PKT_START      3
# define PKT_MOUSE      4
# define PKT_SCREEN     5
# define PKT_HELP       6

# define BMP_TILE       1
# define BMP_LAMP       2
# define BMP_LVRLG      3
# define BMP_LVRSH      4
# define BMP_LVRBG      5

// ----------------------------
class Sckt
{
    private:
    int                 sock;
    int                 port;
    char                name [10] = "__";

#define MAX_CLI_SOCK    30
    int  sockCli [MAX_CLI_SOCK] = {};
    int  nSockCli = 0;

    public:
    // ----------------------------
    Sckt (const char* tag)   {
        port = sock = 0;
        printf ("%s: %s,\n", __func__, tag);
        strncpy (name, tag, 10);
    };

    Sckt (void)   {}

    ~Sckt (void)  { 
        // printf ("%s: %s port %d, sock %d\n", __func__, name, port, sock);
    };

    // ----------------------------
    void
    scktOpen (
        int port,
        int server);

    // ----------------------------
    int
    transmitTo (
        unsigned char  *buf,
        int             size);

    // ----------------------------
    int
    receive (
        unsigned char  *buf,
        int             size,
        int             msgOpt);

    // ----------------------------
    int
    getSock (void) { return sock; };
};

#endif
