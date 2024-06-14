<!--
<meta http-equiv="refresh" content="5">
 -->

<p align=right> May 2024, Greg Ciurpita

<h3>CTC Panel without using JMRI</h3>

<img src=Img/4pnlBlk.PNG align=right height=300>

JMRI is many things and can provide a CTC panel for control of a layout
but describing both the trackage and panel using JMRI is tedious.
An alternative approach is to use a human readable file
to describe both the track diagram and panel
along with interlocking rules.

<p>
The CTC Java app uses icons from JMRI for CTC levers, lamps and buttons, and
non-JMRI icons for the track diagram.
The screenshot shows the ends of two sidings and
the <i>switch-lock</i> for a manually controlled spur.
Signal 10L indicates clear and OS block 3 is occupied.

<p>
It currently supports using MQTT messages for control,
other interfaces are possible
provided that the hardware interfaces are available.

<p>
The CTC panel is arranged in columns
where the top row of odd numbered levers is for turnouts and
the lower row with even numbered levers is for signals.
Levers are checked when the <i>code-button</i>
below the signal lever is pressed.
If the request is accepted,
the lamp above the lever position will be turned on and
the corresponding turnout or signal icon on the track diagram modified.
Turnouts will reflect the new alignment
and signals will be either red or green.

A switch-lock lamp will be blue
when the switch-lock is active allowing manual control and
when properly used in a rule,
preventing a signal clearing that route.

<!-- -----------------------------------------------------  ---------------- -->
<p align=right> June 2024, Greg Ciurpita
<h4>Features</h4>

<ul>
 <li> at a minimum, an interface to turnouts and signals
 <li> <i>rules</i> identify panel element positions and conditions, and
        block occupancy status, to request a signal be cleared
 <li> a locking mechanism that prevents panel elements being changed
        once signal depending on those elements has be <i>set</i>
 <li> a signal is set to Stop (i.e. <i>knocked down</i>)
        when a block becomes the signal depends on (is in its rule)
 <li> a mecanism to clear the lock status of elements in a <i>rule</i>
        when a block becomes un-occupied.
</ul>



<!-- -----------------------------------------------------  ---------------- -->
<h4>Track Description</h4>

<!--
<img src=Img/ctc2.PNG align=right height=300>
 -->

<table align=right border=1 cellspacing=0>
  <tr><th>Track Symbols
  <tr><td>
 <table border=0 cellspacing=4>
 <tr><td><pre>
#    2     2  2  trackH.bmp
#    4     4  4  blockHR.bmp
#    5     5  5  blockHL.bmp
#    6     6  6  diagonalDU.bmp
#    7     7  7  diagonalUD.bmp
#    8     8  8  angleDL.bmp
#    9     9  9  angleDR.bmp
#   10    10  :  angleUL.bmp
#   11    11  ;  angleUR.bmp
#   16    16  @  hsignalR.bmp
#   17    17  A  hsignalL.bmp
#   46    46  ^  hsignalRG.bmp
#   47    47  _  hsignalLG.bmp
#   76    66  |  hsignalRC.bmp
#   77    67  }  hsignalLC.bmp</pre>
 </table>
</table>

A track description is identified by the <i>row</i> keyword
followed by ASCII characters to specify various track elements.
These elements are listed in the Track Symbols table.
Different icons are used by the program to indicate
that switches are thrown or closed or
that a signal is clear or STOP.

<blockquote><pre>
row                    A
row 22229             84222222
row    @ 7 A  82     6 A
row 222522:422222225;224222222
row    @           @
#   01234567890123456789012345678901234567890123456789
</blockquote></pre>

The last line is a comment used to identify columns.

<!-- <table width=100%><tr><td>&nbsp;<table> -->
<!-- ---------------------------------------- -->

<p>
Similarly, <i>ctc, lock, signal</i> and <i>turnout</i> keywords
identify these elements:

<p>
<table>
<tr><td width=30> &nbsp;
<td width=250> <pre>
ctc    3 4          9 10
lock       5
#        id   row  col  lbl
block     3     3    7    _
block     5     3    8    _
block     9     3   15    _
#       ctc   row  col  lbl
signal   10     0   19  LB
signal   10     2   19  L
signal   10     4   15  R

signal    4     2    3  RB
signal    4     4    3  R
signal    4     2    7  L

turnout   3     3    6  _
turnout   9     3   16  _
<td valign=top>

<ul>
 <li> <i>ctc</i> identifies which levers are needed
 <li> <i>lock</i> identifies a manual switch-lock and
                turnout number regardless if a lever exists
 <li> <i>block</i> identifies its number and
                corresponding <i>common</i> track symbol,
                starting from either the left or right side
                since a block may diverge through a switch (see above).

 <li> <i>signal</i> identifies its lever number, suffix and
                position on the track diagram

 <li> <i>turnout</i> identifies its lever number, suffix and
                position on the track diagram.
                An A/B suffix would be used to identify two
                commonly operated turnouts in a cross-over.
</ul>

<p>
An optional column can identify MQTT topic names
if not generically generated from block, signal or turnout descriptions
(e.g. <i>layout/T/9/</i>);

<p>
Comments are prefixed with '#' and blank lines ignored.
Errors are generated if
the specified signal or turnout don't match the symbol in the track diagram
or the specified CTC lever number.
</table>


<!-- -----------------------------------------------------  ---------------- -->
<h4> Interlocking Rules </h4>

<table align=left>
<tr><td width=30> &nbsp; </td>
<td width=350>
 <table cellspacing=0> <tr><td> <pre>
rule    4RB rL4  r3  uB3 uB5 uB9 uK5 r9 s10LB
rule    4RB rL4  r3  uB3 uB5 uB9 uK5 n9 s10L

rule    4R  rL4  n3  uB3 uB5 uB9 uK5 r9 s10LB
rule    4R  rL4  n3  uB3 uB5 uB9 uK5 n9 s10L

rule    4L  lL4  uB3

rule   10LB lL10 r9  uB9 uB5 uB3 uK5 r3 s4RB
rule   10LB lL10 r9  uB9 uB5 uB3 uK5 n3 s4R

rule   10L  lL10 n9  uB9 uB5 uB3 uK5 r3 s4RB
rule   10L  lL10 n9  uB9 uB5 uB3 uK5 n3 s4R

rule   10R  rL10 uB9
 </table>
</td></tr>
</table>

Interlockings check the following before
clearing a signal or changing turnout:

<p>
 <li> route locking - turnout
 <li> indication locking - signal
 <li> traffic locking - block
 <li> manual switch locking - switch-lock

<p>
Each signal requires one or more rules.
The signal is allowed to be clear
if all the conditions are met on any one rule.
When the signal is not force to STOP,
the CTC machine relies on other control element to display
the appropriate aspect (CLEAR, APPROACH, MEDIUM-APPROACH).

<blockquote><pre>
rule    4RB rL4  r3  uB3 uB5 uB9 uK5 r9 s10LB
</blockquote></pre>

<p>
Following the <i>rule</i> keyword is the signal, <i>4RB</i>,
that rule applies to.
Each rule identifies the signal
then one or more condition/panel-elements.

<p>
Each rule will require that the corresponding CTC signal lever, <i>RL4</i>,
is set either right, 'R', or left, 'L'.
The lever number is prefixed with an 'L'.

<p>
The remaining rules similarly identify the condition/panel-elements:
<ul>
 <li> <i>R3</i> turnout 3 needs to be reversed
 <li> <i>B3</i> block 3 needs to be unoccupied
 <li> <i>B5</i> block 5 needs to be unoccupied
 <li> <i>uK5</i> switch-lock 5 needs to be un-locked
 <li> <i>R9 S10LB</i> if turnout 9 is reversed then signal 10LB needs to be STOP
</ul>

<p>
Multiple rules are necessary to check for opposite signals and
are typically paired with a turnout position (e.g. <i>N9 S10L</i>).

<hr>
<!-- -----------------------------------------------------  ---------------- -->
<h4> 2nd Example </h4>

<img src=Img/md0Pnl.PNG width=100%>

<pre>
row                                                                   8229
row        A                                                        8;2222:9
row   82229229                                                    8;22222222:9
row  <     7  :22222222222222222222222222222222222222222222222222;222222222222:222229
row  >      7                                                                        =
row   :222222:222222222222228222222222222222222222222282222229222222222222222229     ?
row           7            6         @           8228;        :22222222222222222:222;
row            7       822;       82229        8;22;
row        A    7     6         8;22222:9 A   6    A
row   82222222222:922;222222222;222222222:282;222222229222222222222222222222222229
row  <             7                      6            :22222222222222222222222222:29
row  >              7                    6                                           =
row   :22222222222222:222222222222222222;22222222222229                              ?
row                                     @          @   7              82222222282222;
row                                                     7           8;22222228;
row                                                      7        8;22222228;
row                                                       :222222;22222222;
row
</pre>

<hr>
<!-- -----------------------------------------------------  ---------------- -->
<h4> 3rd Example </h4>

<img src=Img/ctc.PNG align=right width=300>
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
