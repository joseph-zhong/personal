# makefile 

NVCC=nvcc 

# CUFLAGS=-Wall -Wpedantic -std=c++11 -g -O0

CU_SRC=$(wildcard *.cu)

HELLO_EXEC=hello
FLOW_EXEC=flow

# OBJS = $(CU_SRC:.cu=.o)

all: $(patsubst %.cu, %.out, $(wildcard *.cu))

%.out: %.cu 
	$(NVCC) $(CUFLAGS) -o $@ $< 

clean:
	rm *.out *~