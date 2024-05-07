
awk '
BEGIN {
    nRow = 0
    nCol = 0
    ctc  = 1
}

/row/ {
    s   = substr($0, 6)
    len = length(s)

  # printf " %3d %s\n", len, s

    for (c = 1; c <= len; c++)
        buf [c, nRow] = substr(s, c, 1)

    if (nCol < len)
        nCol = len
    nRow++
}

# ------------------------------------------------
function dispRow (row, x, c)  {
    printf " %3d ", row
    for (x = 1; x <= nCol; x++)
        printf "%s", buf [x, row]
    printf "\n"
}

function markCol (col, x, c)  {
    printf " %3d ", col
    for (x = 0; x <= col-1; x++)
        printf "%d", x % 10
    printf "^"
    printf "\n"
}


# ------------------------------------------------
function signal (row, col) {
    if (0) {
        markCol(col-x0)
        printf " %2d %s\n", col, buf [col, row]
    }
    printf "signal  %4d %4d %4d %4d %4d\n", ctc, row, col, ctc, 100+ctc
    ctc += 2
}

# ------------------------------------------------
function prSignals () {
    printf "#        ctc  row  col  lbl   id\n"
    for (row = 0; row < nRow; row++)  {
        if (0)
            dispRow(row)

        ctc = 2
        for (x = 1; x <= nCol; x++)  {
            c = buf [x, row]
            switch (c) {
            case "A":
            case "@":
                signal( row, x)
                break
            }
        }
    }
    printf "\n"
}

# ------------------------------------------------
function turnout (row, col) {
    if (0) {
        markCol(col-x0)
        printf " %2d %s\n", col, buf [col, row]
    }
    printf "turnout %4d %4d %4d %4d %4d\n", ctc, row, col, ctc, 100+ctc
    ctc += 2
}

# ------------------------------------------------
function prTurnouts () {
    printf "#        ctc  row  col  lbl   id\n"
    for (row = 0; row < nRow; row++)  {
        if (0)
            dispRow(row)
        x0 = 0
        ctc = 1
        for (x = 1; x <= nCol; x++)  {
            c = buf [x, row]
            switch (c) {
            case ":":
                if (" " != buf [x-1, row] && buf [x-1, row])
                    turnout(row, x)
                break
            case ";":
                if (" " != buf [x+1, row] && buf [x+1, row])
                    turnout(row, x)
                break
            case "9":
                if (" " != buf [x+1, row] && buf [x+1, row])
                    turnout(row, x)
                break
            case "8":
                if (" " != buf [x-1, row] && buf [x-1, row])
                    turnout(row, x)
                break
            }
        }
    }
    printf "\n"
}

# ------------------------------------------------
END {
    prSignals()
    prTurnouts()

    printf "ctc 1"
    for (n = 3; n < ctc; n += 2)
        printf " %d", n
    printf "\n"

}' $*
