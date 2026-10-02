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

# llama.cpp is compiled into a single, separately hidden JNI library. Its ggml
# objects never link to voice_whisper, so the two pinned ggml revisions cannot
# resolve each other's symbols at runtime.
set(LLAMA_ROOT third_party/llamacpp)
file(GLOB LLAMA_CORE CONFIGURE_DEPENDS ${LLAMA_ROOT}/src/*.cpp)
file(GLOB LLAMA_MODELS CONFIGURE_DEPENDS ${LLAMA_ROOT}/src/models/*.cpp)
set(QWEN_GGML
    ${LLAMA_ROOT}/ggml/src/ggml.c
    ${LLAMA_ROOT}/ggml/src/ggml.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-alloc.c
    ${LLAMA_ROOT}/ggml/src/ggml-backend.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-backend-dl.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-backend-meta.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-backend-reg.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-opt.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-threading.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-quants.c
    ${LLAMA_ROOT}/ggml/src/gguf.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/ggml-cpu.c
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/ggml-cpu.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/repack.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/iqp.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/hbm.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/quants.c
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/traits.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/binary-ops.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/unary-ops.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/vec.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/ops.cpp
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/arch/arm/quants.c
    ${LLAMA_ROOT}/ggml/src/ggml-cpu/arch/arm/repack.cpp
)
add_library(qwen_rewrite SHARED qwen_jni.cpp ${LLAMA_CORE} ${LLAMA_MODELS} ${QWEN_GGML})
target_include_directories(qwen_rewrite PRIVATE
    ${LLAMA_ROOT}/include ${LLAMA_ROOT}/src
    ${LLAMA_ROOT}/ggml/include ${LLAMA_ROOT}/ggml/src ${LLAMA_ROOT}/ggml/src/ggml-cpu
)
target_compile_features(qwen_rewrite PRIVATE cxx_std_17)
target_compile_definitions(qwen_rewrite PRIVATE
    GGML_BACKEND_BUILD _GNU_SOURCE
)
target_compile_options(qwen_rewrite PRIVATE -O3 -fvisibility=hidden -fvisibility-inlines-hidden)
target_link_libraries(qwen_rewrite PRIVATE ${log-lib} android)
target_link_options(qwen_rewrite PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
