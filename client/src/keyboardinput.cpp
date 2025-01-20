#include <iostream>
#include <string>
#include <limits>
#include <thread>

class KeyboardInput {
public:
    // Function to read a line of input from the keyboard
    std::string readLine() {
        std::string input;
        std::getline(std::cin, input);
        return input;
    }
};

void readInput(KeyboardInput& keyboard) {
    std::cout << "Enter a line of text: ";
    std::string line = keyboard.readLine();
    std::cout << "You entered: " << line << std::endl;
}

int main() {
    KeyboardInput keyboard;
    std::thread inputThread(readInput, std::ref(keyboard));

    // Wait for the thread to finish
    inputThread.join();

    return 0;
}
