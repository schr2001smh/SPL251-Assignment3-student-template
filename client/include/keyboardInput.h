#ifndef KEYBOARDINPUT_H
#define KEYBOARDINPUT_H

#include <string>

class KeyboardInput {
public:
    // Constructor
    KeyboardInput();

    // Destructor
    ~KeyboardInput();

    // Method to read input from the terminal
    std::string readInput() const;

private:
    // Add any private members if necessary
};

#endif // KEYBOARDINPUT_H