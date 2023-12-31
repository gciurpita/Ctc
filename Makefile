
# LINT_FLAG  = -Xlint:deprecation

    Classes = $(patsubst %.java, %.class, $(Srcs))
    Srcs    = $(shell echo *java)
    Targ    = CtcPanel

    Jar     = $(Targ).jar
    Opt     = -p0
    Opt     = -p3 -fc0.cmd

    Sopt    = cyc1:50,psi1,TE,mph,lbs1:10
    Topt    = Tsec=1

# --------------------------------------------------------------------
%.class : %.java
		javac $(LINT_FLAG) $<

% : %.class
		java $@ $(Args)

# --------------------------------------------------------------------
run : $(Classes)
		java $(Targ)  Resources/ctcNumbered

all : $(Classes)

cmd : $(Classes)
		java $(Targ) $(Opt) | tee $(Targ).out

jar : all
		jar cmvf          MANIFEST.MF $(Targ).jar *.class
#		jar cmvf META-INF/MANIFEST.MF $(Targ).jar *.class

runJar : jar
		java -jar $(Targ).jar

view : $(Jar)
		appletviewer driver.html

xgr :
		plot.k $(Topt) S=$(Sopt) $(Targ).out | tee $(Targ).xgr

xgr2 :
		plot.k  S=TE,mph,lbs1:10 $(Targ).out | tee $(Targ).xgr

# --------------------------------------------------------------------
neat :
		rm -f *~ *.class *.out

clean : neat
		rm -f *.jar
		cd ScktC; make clean
