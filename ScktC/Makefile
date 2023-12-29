
# CFLAGS += -Wall -Werror
# CFLAGS += -I ../include
# CFLAGS += -isystem IncSys

  Obj   = Obj
  Objs  = $(patsubst %.cpp, $(Obj)/%.o, $(Srcs))
  Srcs  = $(shell echo *cpp)
  Targ  = targ

$(Obj)/%.o : %.cpp
	$(CXX) $(CFLAGS) -c -o $@ $^


# --------------------------------------------------------------------
all : $(Targ)
	$(Targ)

$(Targ) : $(Obj) $(Objs)
	$(CXX) -o $(Targ) $(Objs) $(LIBS)

$(Obj) :
	mkdir $@

# --------------------------------------------------------------------
check :
	@ echo Src: $(Srcs)
	@ echo Obj: $(Objs)

neat :
	rm -f *~ *stackdump

clean : neat
	rm -rf $(Obj) *.exe
