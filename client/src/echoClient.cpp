#include <stdlib.h>
#include <thread>
#include <algorithm>
#include <cctype>
#include <locale>
#include "../include/ConnectionHandler.h"

/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/

void listenToServer(ConnectionHandler& connectionHandler) {

    while (true) {

        std::string answer;

        if (!connectionHandler.getLine(answer)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        
        int len = answer.length();
        if (len > 0) {
            answer.resize(len - 1); // Remove the newline character
        }
        std::cout << answer << " "  << std::endl << std::endl;
        if (answer == "bye") {
            std::cout << "Exiting...\n" << std::endl;
            break;
        }
    }
}

std::string handleConnect(const std::string& str) {
    std::istringstream stream(str);
    std::string command, login, passcode;
    stream >> command >> login >> passcode;
    std::string frame = "CONNECT\n";
    frame += "accept-version:1.2\n";
    frame += "host:stomp.cs.bgu.ac.il\n";
    frame += "login:" + login + "\n";
    frame += "passcode:" + passcode + "\n";
    frame += '\u0000';
    return frame;
}

std::string handleSend(const std::string& str) {
    std::istringstream stream(str);
    std::string command, destination, body, id;
    stream >> command >> destination >> id;
    std::getline(stream, body);
    std::string frame = "SEND\n";
    frame += "destination:" + destination + "\n";
    frame += "id:" + id + "\n";
    frame += "\n" + body + '\u0000';
    return frame;
}

// Helper function to trim whitespace from both ends of a string
static inline void trim(std::string &s) {
    // Trim from the start (left)
    s.erase(s.begin(), std::find_if(s.begin(), s.end(), [](unsigned char ch) {
        return !std::isspace(ch);
    }));
    // Trim from the end (right)
    s.erase(std::find_if(s.rbegin(), s.rend(), [](unsigned char ch) {
        return !std::isspace(ch);
    }).base(), s.end());
}

std::string handleSubscribe(const std::string& str) {
    static int id = 0;
    std::istringstream stream(str);
    std::string command, destination, additionalParam;
    stream >> command >> destination >> additionalParam;

    // Trim the input values
    trim(command);
    trim(destination);
    trim(additionalParam);

    std::string frame = "SUBSCRIBE\n";
    frame += "destination:" + destination + "\n";
    frame += "id:" + additionalParam + "\n";
    frame += '\u0000';
    return frame;
}

std::string handleUnsubscribe(const std::string& str) {
    std::istringstream stream(str);
    std::string command, id;
    stream >> command >> id;
    std::string frame = "UNSUBSCRIBE\n";
    frame += "id:" + id + "\n";
    frame += '\u0000';
    return frame;
}

std::string handleDisconnect(const std::string& str) {
    std::istringstream stream(str);
    std::string command, receipt;
    stream >> command >> receipt;
    std::string frame = "DISCONNECT\n";
    frame += "receipt:" + receipt + "\n";
    frame += '\u0000';
    return frame;
}

std::string handleError(const std::string& str) {
    return "ERROR\nmessage:Invalid command\n" + str + '\u0000';
}

std::string stringToFrame(const std::string& str) {
    std::string firstWord;
    std::istringstream stream(str);
    stream >> firstWord;

    if (firstWord == "connect") {
        return handleConnect(str);
    } else if (firstWord == "send") {
        return handleSend(str);
    } else if (firstWord == "subscribe") {
        return handleSubscribe(str);
    } else if (firstWord == "unsubscribe") {
        return handleUnsubscribe(str);
    } else if (firstWord == "logout") {
        return handleDisconnect(str);
    } else {
        return handleError(str);
    }
}

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);

    ConnectionHandler connectionHandler(host, port);
    
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    std::string connectMessage = "CONNECT\n"
                                 "accept-version:1.2\n"
                                 "host:stomp.cs.bgu.ac.il\n"
                                 "login:meni\n"
                                 "passcode:films" + std::string(1, '\u0000');

    if (!connectionHandler.sendLine(connectMessage)) {
        std::cerr << "Failed to send connect message to " << host << ":" << port << std::endl;
        return 1;
    }

    // Start a new thread to listen to the server
    std::thread listenerThread(listenToServer, std::ref(connectionHandler));

    // Main thread will handle reading input from the terminal and sending it to the server
    while (true) {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
        std::string line(buf);
        int len = line.length();
        
        std::string lineStr(line);
        std::string myFrame = stringToFrame(line);
        
        std::cout << "THE LINE YOU WROTE IS  ====== \n"+ lineStr +"\n" << std::endl;
        std::cout << "THE COMMAND YOU WROTE IS  ====== \n"+ myFrame +"\n" << std::endl;
        
        if (!connectionHandler.sendLine(myFrame)) { // Send the frame instead of the raw line
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        // connectionHandler.sendLine(myFrame) appends '\n' to the message. Therefore we send len+1 bytes.
    }

    // Wait for the listener thread to finish
    listenerThread.join();
    return 0;
}