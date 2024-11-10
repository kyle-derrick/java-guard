cmake_minimum_required(VERSION 3.14)

project(jvmti-jni-wrapper)

include_directories(${PROJECT_SOURCE_DIR}/jdk_include)
add_library(jvmti-jni-wrapper SHARED src/lib.c)