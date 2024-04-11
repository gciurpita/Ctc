
# LINT_FLAG  = -Xlint:deprecation

    Classes = $(patsubst %.java, %.class, $(Srcs))
    Srcs    = $(shell echo *java)
    Targ    = Interlock

    Cmd     = -c0.cmd
#   Pnl     = 0.rules
    Pnl     = md2.pnl
    Pnl     = b2b.pnl

%.class : %.java
		javac $(LINT_FLAG) $<

%.pnl :
		java $(Targ) $@

% :
		java $(Targ) $@.pnl

# --------------------------------------------------------------------
all : $(Classes)

go  : $(Classes)
		java $(Targ) $(Pnl)

cmd : $(Classes)
		java $(Targ) $(Cmd) $(Pnl)

check :
		@ echo  $(Srcs)
		@ echo  $(Classes)

# --------------------------------------------------------------------
neat :
		rm -f *~ *.class *.out

clean : neat
		rm -f *.jar *.exe
