#include <stdlib.h>
#include <thread>
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
        std::cout << "Reply: " << answer << " " << len << " bytes " << std::endl << std::endl;
        if (answer == "bye") {
            std::cout << "Exiting...\n" << std::endl;
            break;
        }
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
        
        std::cout << "THE LINE YOU WROTE IS  ======"+ lineStr << std::endl;
        
        if (!connectionHandler.sendLine(line)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        // connectionHandler.sendLine(line) appends '\n' to the message. Therefore we send len+1 bytes.
        std::cout << "Sent " << len+1 << " bytes to server" << std::endl;
    }

    // Wait for the listener thread to finish
    listenerThread.join();
    return 0;

}

std::string stringToFrame(const std::string& str) {
    std::string frame = "";
    int i = 0;
    while (str[i] != '\n') {
        frame += str[i];
        i++;
    }
    return frame;
}