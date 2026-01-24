// cpu.cpp
#include <chrono>
#include <iostream>
#include <thread>
#include <cstdlib>   // atoi
#include <cstring>   // strcmp

int main(int argc, char* argv[]) {
    int seconds = 10;

    // 解析参数：--seconds N
    if (argc == 3 && std::strcmp(argv[1], "--seconds") == 0) {
        int v = std::atoi(argv[2]);
        if (v > 0) {
            seconds = v;
        }
    }

    std::cout << "CPU burn started for " << seconds << " seconds..." << std::endl;

    auto endTime = std::chrono::steady_clock::now()
                 + std::chrono::seconds(seconds);

    // 忙循环，占用 CPU
    volatile unsigned long long counter = 0;
    while (std::chrono::steady_clock::now() < endTime) {
        counter++;
    }

    std::cout << "CPU burn finished. Counter=" << counter << std::endl;
    return 0;
}
