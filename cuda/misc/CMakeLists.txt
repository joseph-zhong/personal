cmake_minimum_required(VERSION 3.6)
project(misc)

set(CMAKE_CXX_STANDARD 11)

set(SOURCE_FILES
        cube.cu
        square.cu)

add_executable(misc ${SOURCE_FILES})