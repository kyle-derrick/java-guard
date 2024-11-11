cmake_minimum_required(VERSION 3.14)

project(jg-jvmti-wrapper)

if (WIN32)
    include_directories(${PROJECT_SOURCE_DIR}/jdk_include/windows)
elseif (APPLE)
    include_directories(${PROJECT_SOURCE_DIR}/jdk_include/darwin)
else ()
    include_directories(${PROJECT_SOURCE_DIR}/jdk_include/linux)
endif ()
add_library(jg-jvmti-wrapper SHARED c_src/lib.c)