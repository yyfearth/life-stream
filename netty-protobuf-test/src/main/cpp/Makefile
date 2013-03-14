COMPILER=G++

C = cpp
OUTPUT_PATH = ./
SOURCE_PATH = ./
EXE = $(OUTPUT_PATH)client

ifeq ($(COMPILER), G++)
  ifeq ($(OS),Windows_NT)
    OBJ = obj
  else
    OBJ = o
  endif
  COPT = -O2
  CCMD = g++
  OBJFLAG = -o
  EXEFLAG = -o
# INCLUDES = -I../.includes
  INCLUDES =
# LIBS = -lgc
  LIBS = -lboost_system-mt -lprotobuf
# LIBPATH = -L../gc/.libs
  LIBPATH =
  CPPFLAGS = $(COPT) -g $(INCLUDES)
  LDFLAGS = $(LIBPATH) -g $(LIBS)
  DEP = dep
endif

OBJS := $(patsubst %.$(C),%.$(OBJ),$(wildcard $(SOURCE_PATH)*.$(C)))

all:
	$(CCMD) $(LIBS) $(EXEFLAG) $(EXE) client.cpp meta.pb.cc

clean:
	rm -rf $(SOURCE_PATH)*.$(OBJ) $(EXE)

rebuild: clean all
#rebuild is not entirely correct

run: 
	@echo Run...
	$(EXE) 127.0.0.1 8888
