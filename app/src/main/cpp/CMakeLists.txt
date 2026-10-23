cmake_minimum_required(VERSION 3.22.1)
project(didge_swipe C)
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(SPTK ${CMAKE_CURRENT_SOURCE_DIR}/vendor/sptk)
add_library(swipe_vendor OBJECT
    ${SPTK}/bin/pitch/swipe/swipe.c
    ${SPTK}/bin/pitch/swipe/vector.c
    ${SPTK}/bin/fft/_fft.c
    ${SPTK}/bin/fftr/_fftr.c
    ${SPTK}/lib/getmem.c)
set_target_properties(swipe_vendor PROPERTIES POSITION_INDEPENDENT_CODE ON)
target_include_directories(swipe_vendor PRIVATE ${SPTK}/include)
target_compile_options(swipe_vendor PRIVATE -include ${CMAKE_CURRENT_SOURCE_DIR}/tracked_alloc.h
    -fno-fast-math -ffp-contract=off)
target_compile_definitions(swipe_vendor PRIVATE round=round isnan=isnan)
add_library(swipe SHARED swipe_jni.c $<TARGET_OBJECTS:swipe_vendor>)
target_compile_options(swipe PRIVATE -Wall -Wextra -fno-fast-math -ffp-contract=off)
target_link_libraries(swipe m)
target_link_options(swipe PRIVATE -Wl,-z,max-page-size=16384)
