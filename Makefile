
# LINT_FLAG  = -Xlint:deprecation

    Classes = $(patsubst %.java, %.class, $(Srcs))
    Srcs    = $(shell echo *java)
    Targ    = Interlock

    Cmd     = -c0.cmd
#   Pnl     = 0.rules
    Pnl     = Pnls/md2.pnl
    Pnl     = Pnls/b2b.pnl
    Pnl     = Pnls/b2b_a.pnl
    Pnl     = Pnls/4.pnl

%.class : %.java
		javac $(LINT_FLAG) $<

%.pnl :
		java $(Targ) $@

# --------------------------------------------------------------------
all : $(Classes)
		java $(Targ) $(Pnl)

bld : $(Classes)

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
