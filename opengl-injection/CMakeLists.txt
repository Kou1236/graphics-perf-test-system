cmake_minimum_required(VERSION 3.20)
project(opengl_injection LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 生成 DLL
add_library(tp_opengl_hook SHARED
    src/DllMain.cpp
    src/SwapHook.cpp
    src/RenderEventWriter.cpp
    minhook/src/buffer.c
    minhook/src/hook.c
    minhook/src/trampoline.c
    minhook/src/hde/hde64.c
)

target_include_directories(tp_opengl_hook
    PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_CURRENT_SOURCE_DIR}/minhook/include
)

target_compile_definitions(tp_opengl_hook PRIVATE
    _CRT_SECURE_NO_WARNINGS
    WIN32_LEAN_AND_MEAN
)

# 生成注入器
add_executable(injector
    injector/Injector.cpp
)

target_compile_definitions(injector PRIVATE
    _CRT_SECURE_NO_WARNINGS
    WIN32_LEAN_AND_MEAN
)