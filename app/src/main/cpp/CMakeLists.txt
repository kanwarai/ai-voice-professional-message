cmake_minimum_required(VERSION 3.22.1)
project(voice_whisper LANGUAGES C CXX)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_ALL_WARNINGS OFF CACHE BOOL "" FORCE)

add_subdirectory(third_party/whispercpp/ggml ggml-build)

add_library(voice_whisper SHARED
    whisper_jni.cpp
    third_party/whispercpp/src/whisper.cpp
)

target_include_directories(voice_whisper PRIVATE
    third_party/whispercpp/include
    third_party/whispercpp/src
    third_party/whispercpp/ggml/include
    third_party/whispercpp/ggml/src
)
target_compile_definitions(voice_whisper PRIVATE WHISPER_VERSION="1.9.4")

# Native inference stays optimized in debug APKs so device measurements are meaningful.
target_compile_options(voice_whisper PRIVATE -O3 -fvisibility=hidden -fvisibility-inlines-hidden)
foreach(target ggml ggml-base ggml-cpu)
    if(TARGET ${target})
        target_compile_options(${target} PRIVATE -O3)
    endif()
endforeach()

find_library(log-lib log)
target_link_libraries(voice_whisper PRIVATE ggml ${log-lib} android)
target_link_options(voice_whisper PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
