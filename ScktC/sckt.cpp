//

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>

#include <sys/socket.h>
#include <netdb.h>

// #include "debug.h"
#include "sckt.h"

int dbg = 0;

// --------------------------------------------------------------------
//
void dispSin (
    struct sockaddr_in  *sin )
{
    unsigned char *p = (unsigned char *) & sin->sin_addr.s_addr;

    printf (" %s:  %d",   __func__, p[0]);
    for (int i = 1; i < 4; i++)
        printf (".%d", p[i]);
    printf ("  port %d\n", sin->sin_port);
}

// --------------------------------------------------------------------
// open a socket for the specified port

void Sckt :: scktOpen (
    int port,
    int server )
{
    struct sockaddr_in  sockAddr;
    int                 res;

    if (0 != sock)  {
        printf ("Error: %s: sock %2d already opened\n", __func__, sock);
        exit (2);
    }

    if ((sock = socket (AF_INET, SOCK_STREAM, 0)) < 0)  {
        perror ("scktOpen: socket");
        exit (1);
    }

    bzero((char *) &sockAddr, sizeof(sockAddr));

    sockAddr.sin_family      = AF_INET;
    sockAddr.sin_port        = htons (port);
    sockAddr.sin_addr.s_addr = INADDR_ANY;  /* auto-fill with my IP */

    res = bind (sock, (struct sockaddr *)&sockAddr,
                    sizeof(struct sockaddr));
    if (res < 0)  {
        perror ("scktOpen: bind");
        exit (1);
    }

    listen (sock, 5);

    this->port = port;

    printf ("%s: port %d, sock %d SOCK_STREAM\n", __func__, port, sock);
}

// --------------------------------------------------------------------
// check for input and echo it back
int Sckt :: transmitTo (unsigned char *buf, int size)
{
    int nByte;
    int sd;

    // send to each client socket
    for (int i = 0 ; i < nSockCli ; i++) {
        if (0 == (sd = sockCli [i]))
            continue;

#if 0
        nByte = sendto (sd, buf, size, 0,
                    (struct sockaddr *) &sin[i], sizeof(struct sockaddr));
#else
        nByte = send (sd, buf, size, 0);
#endif

        if (nByte < 0) {
            printf ("%s: Error: %d nByte %d\n", __func__, name, nByte);
            perror ("transmit: send()");
            exit (-1);
        }
    }

    return nByte;
}

// -----------------------------------------------------------------------------
// use select to service socket events
int addrLen = sizeof(struct sockaddr_in);

int Sckt :: receive (
    unsigned char  *buf,
    int             size,
    int             msgOpt )
{
    struct timeval  timeout = {};
    struct sockaddr addr;
    fd_set  readfds;
	int     maxSd;
    int     nRd   = 0;
    int     sd;
    int     i;

    //clear the socket set
    FD_ZERO (&readfds);

    //add master socket to set
    FD_SET (sock, &readfds);
    maxSd = sock;

    //add child sockets to set
    for (i = 0 ; i < nSockCli ; i++) {
        if (0 == (sd = sockCli [i]))
            continue;

        FD_SET (sd, &readfds);

        if (maxSd < sd)
            maxSd = sd;
    }

    // check event
    int activity = select (maxSd +1, & readfds, NULL, NULL, & timeout);

    if (activity && dbg)
        printf ("%s: select event, %d\n", __func__, activity);

    // -----------------------------------------------
    // check for new connections
    if (FD_ISSET (sock, &readfds))
    {
        if (0 > (sd = accept (sock, & addr, & addrLen))) {
            perror ("accept");
            exit (-1);
        }
        printf (" %s: new connection - sock %d, ", __func__, sd);
        dispSin ((struct sockaddr_in *) & addr);

        // attempt to reuse entry from disconnected socket
        for (i = 0 ; i < nSockCli ; i++)  {
            if (0 == sockCli [i])  {
                sockCli [i] = sd;
                break;
            }
        }

        // add new socket to list
        if (nSockCli == i)
            sockCli [nSockCli++] = sd;
    }

    // -----------------------------------------------
    // check for new input
    for (i = 0; i < nSockCli; i++) {
        sd = sockCli [i];
        if (0 == sd)
            continue;

        if (FD_ISSET (sd, &readfds)) {
            nRd = read (sd, buf, size);
            if (dbg)
                printf (" %s: idx %d, sd %d, nRd %d\n", __func__, i, sd, nRd);

            // disconnect
            if (0 >= nRd)  {
                if (0 > nRd && dbg)
                    perror ("  receive: read");

                printf ("  %s: disconnection - sock %d\n", __func__, sd);

                close (sd);
                sockCli [i] = 0;
            }

            else if (dbg) {
                printf ("  %s: nRd %d, ", __func__, nRd);
                for (int i = 0; i < nRd; i++)
                    printf (" %02x", buf [i]);
                printf ("\n");
            }
        }
    }

    return nRd;
}
