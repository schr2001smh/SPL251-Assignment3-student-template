#include <stdlib.h>
#include <thread>
#include <algorithm>
#include <cctype>
#include <locale>
#include "../include/ConnectionHandler.h"
#include "../include/event.h"
#include "../include/Client.h"
bool status = false;
Client myClient;

void listenToServer(ConnectionHandler& connectionHandler) {
    names_and_events parser;
    parser = parseEventsFile("/workspaces/SPL251-Assignment3-student-template/client/data/events1.json");
    while (true) {
    std::cout << "====== \n" << parser.channel_name << "\n" << std::endl;
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
    std::string command, login, passcode, something;
    stream >> command >> something >> login >> passcode;
    std::string frame = "CONNECT\n";
    frame += "accept-version:1.2\n";
    frame += "host:stomp.cs.bgu.ac.il\n";
    frame += "login:" + login + "\n";
    frame += "passcode:" + passcode + "\n";
    frame += "\n" "\0"; // Add three \n before the null character
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
    frame += "\n" + body + "\n" "\0"; // Add three \n before the null character
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
    int id = myClient.getcounter();
    std::istringstream stream(str);
    std::string command, destination;
    stream >> command >> destination;
    // Trim the input values
    trim(command);
    trim(destination);
    myClient.addSubscription(destination, id);
    std::string frame = "SUBSCRIBE\n";
    frame += "destination:" + destination + "\n";
    frame += "id:" + std::to_string(id) + "\n";
    frame += "\n" "\0"; // Add three \n before the null character
    
    return frame;
}

std::string handleUnsubscribe(const std::string& str) {
    std::istringstream stream(str);
    std::string command, channel;
    stream >> command >> channel;
    int id = myClient.getchannelid(channel);
    std::string frame = "UNSUBSCRIBE\n";
    frame += "id:" + std::to_string(id) + "\n";
    frame += "\n" "\0"; // Add three \n before the null character
    return frame;
}

std::string handleDisconnect(const std::string& str) {
    std::istringstream stream(str);
    std::string command, receipt;
    stream >> command >> receipt;
    std::string frame = "DISCONNECT\n";
    frame += "receipt:" + receipt + "\n";
    frame += "\n" "\0"; // Add three \n before the null character
    return frame;
}

std::string handleError(const std::string& str) {
    return "ERROR\nmessage:Invalid command\n" + str + "\n" + std::string(1, '\0'); // Add three \n before the null character
}

std::string stringToFrame(const std::string& str) {
    status = myClient.connectionstatus();
    std::cout << "THE STATUS IS  ====== \n" << status << "\n" << std::endl;
    std::string firstWord;
    std::istringstream stream(str);
    stream >> firstWord;
    if (firstWord == "login") {
        if (!status) {
            return handleConnect(str);
        } else {
            std::cout << "client already connected\n" << std::endl;
            return "ERROR";
        }
    } else if (firstWord == "report") {
        return handleSend(str);
    } else if (firstWord == "join") {
        return handleSubscribe(str);
    } else if (firstWord == "exit") {
        return handleUnsubscribe(str);
    } else if (firstWord == "logout" && status) {
        myClient.disconnected();
        return handleDisconnect(str);
    } else {
        return "ERROR";
    }
}

int main(int argc, char *argv[]) {
    
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
    Client myClient(connectionHandler);

    // Start a new thread to listen to the server
    std::thread listenerThread(listenToServer, std::ref(connectionHandler));
    // Main thread will handle reading input from the terminal and sending it to the server
    while (true) {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
        std::string line(buf);
        std::string myFrame = stringToFrame(line);
        std::cout << "THE LINE YOU WROTE IS  ====== \n" + line + "\n" << std::endl;
        if (myFrame == "ERROR") {
            std::cout << "Invalid command, no frame was sent. Please try again." << std::endl;
            continue;
        }
        std::cout << "THE COMMAND YOU WROTE IS  ====== \n" + myFrame + "\n" << std::endl;
        if (!connectionHandler.sendLine(myFrame)) { // Send the frame instead of the raw line
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
    }

    // Wait for the listener thread to finish
    listenerThread.join();
    return 0;
}