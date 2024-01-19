<!-- 
<meta http-equiv="refresh" content="5">
 -->
<h3>CTC Panel</h3>

JMRI supports CTC panels
along with numerous interfaces to a layout.
But JMRI requires creation of a trackplan and
then creation of the panel.
Many tedious and repetitions mouse clicks
are required to do so.

Another approach is to use an ASCII text file
to describe the panel, the trackplan and
relationships between trackplan elements,
turnouts and signals, and
CTC plates, levers and lamps.

The following is a partial list of possible trackplan <i>tiles</i>.
The first column is the internal index of the <i>tile</i>,
the second column an ID, the third is the ASCII char
that maps to its index and the fourth column the name of the bitmap file.
While a (blank) black tile is actually index 0,
it is represent by a space character for better readability.
<pre>
#    2     2  2  trackH.bmp
#    4     4  4  blockHR.bmp
#    5     5  5  blockHL.bmp
#    6     6  6  diagonalDU.bmp /
#    7     7  7  diagonalUD.bmp \

#    8     8  8  angleDL.bmp
#    9     9  9  angleDR.bmp
#   10    10  :  angleUL.bmp
#   11    11  ;  angleUR.bmp

#   16    16  @  hsignalR.bmp
#   17    17  A  hsignalL.bmp
#   46    46  ^  hsignalRG.bmp
#   47    47  _  hsignalLG.bmp
#   76    66  |  hsignalRC.bmp
#   77    67  }  hsignalLC.bmp
</pre>

<a name=row></a>
The panel at right is a simple example.
The trackplan is described with the following ASCII text.
'2' represents a horizontal track segment.
'4' on the second line a Right-handed block boundary.
'9', the down and to the right angled track,
'7', the down/right angled track, and
':', the switch itself set to reverse.
The other switch is represented by the ';', '6' and '8'.
The westbound signal by 'A' and eastbound by '@'.
The '5' represents a left-handed block boundary.
The switch tile, ':' or ';', is replaced with a horizontal track tile, '2',
to represent normal.

<img src=ctc2.PNG align=right width=400>
<pre>
row                       A
row   2222249            85222222
row        @ 7 A        6 A
row   22222422:52222224;225222222
row        @          @
</pre>

The <i>ctc</i> entry identifies which CTC levers are needed.
The <i>signal</i> and <i>turnout</i> entries
identify the CTC lever, trackplan location and name.

<pre>
ctc    5,6,11,12

#       ctc   row  col  lbl
signal   12     0   22  12LB
signal   12     2   22  12L
signal   12     4   18  12R

signal    6     2    7  6RB
signal    6     4    7  6R
signal    6     2   11  6L

turnout   5     3   10  5
turnout  11     3   19  11
</pre>

The <i>rule</i> entries describe
one or more set of conditions for a signal.
Signal <i>6L</i> simply requires that
lever <i>L6</i> be set to left, <i>l</i>.
Signal <i>6R</i> requires that lever <i>L6</i> be set to right, <i>r</i>, and
that turnout <i>5</i> be set normal, <i>N</i>,
while signal <i>6RB</i> requires that
turnout <i>5</i> be set reversed, <i>R</i>.



<pre>
rule    6L  lL6
rule    6R  rL6  N5
rule    6RB rL6  R5
rule    6RB rL6  R5 cL12

rule   12LB lL12 R11
rule   12L  lL12 N11
rule   12R  rL12
</pre>

A more elaborate interlocking is described by the following.

<img src=ctc.PNG align=right width=300>
<pre>
row        A
row       8522222
row      6       A
row   24;222229225
row    @       7 A
row   2422292222:5
row    @    7 A
row          :529
</pre>

<pre>
ctc    3,4,5,6,7,8

#       ctc   row  col  lbl
signal    4     0    7  4LB
signal    4     2   13  4L
signal    4     4    3  4R
signal    8     4   13  8L
signal    6     6    3  6R
signal    6     6   10  6LB

turnout   3     3    4  3
turnout   5     5    7  5
turnout   7     3   10  7A
turnout   7     5   12  7B

# rule    3   n3   n7A s4R s4L
# rule    3   r3   s4R s4LB
# rule    3   s4R  n7A s4L

rule    4R  rL4  R3
rule    4R  rL4  N3   N7A
rule    4R  rL4  N3   R7A  cL8

rule    4LB lL4  R3
rule    4L  lL4  N3   N7A

rule    6LB lL6  R5
rule    6R  rL6  R5
rule    6R  rL6  cL8  N5   N7A

rule    8L  lL8  cL4  N3   R7A
rule    8L  lL8  cL4  N7A
</pre>


<p align=right> Jan 2024, Greg Ciurpita
