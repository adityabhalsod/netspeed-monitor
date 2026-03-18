# CMake build configuration for the native C++ network speed calculator library
cmake_minimum_required(VERSION 3.22.1)

# Project name for the native library
project("netspeed")

# Add the shared native library from source files
add_library(
    # Library name: will produce libnetspeed.so
    netspeed
    SHARED
    # Source files for the native library
    netspeed_calculator.cpp
    netspeed_jni.cpp
)

# Link the native library against required system libraries
target_link_libraries(
    netspeed
    # Android logging library for __android_log_print
    log
)

# Set C++17 standard for modern C++ features
target_compile_features(netspeed PRIVATE cxx_std_17)

# Enable compiler optimizations for release builds
target_compile_options(netspeed PRIVATE
    -Wall          # Enable all warnings
    -Wextra        # Enable extra warnings
    -O2            # Optimize for performance
    -fvisibility=hidden  # Hide internal symbols for smaller binary
)
