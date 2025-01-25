#include <stdlib.h>
#include <thread>
#include <algorithm>
#include <cctype>
#include <locale>
#include "../include/ConnectionHandler.h"
#include "../include/event.h"
#include "../include/Client.h"
bool status;
Client myClient;
std::string myname;
std::string destination;
std::mutex lock;

void listenToServer(ConnectionHandler& connectionHandler) {
    std::string  message;
    while (true) {
        std::string line;

        if (!connectionHandler.getLine(line)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
        if(line == "CONNECTED" || line == "CONNECTED\n") {
            status = true;
        }
        message += line;
        if (line.rfind("destination:/", 0) == 0) {
            size_t endPos = line.find('\n', 12); // Find the position of the first newline after "destination:/"
            if (endPos != std::string::npos) {
            destination = line.substr(12, endPos - 12); // Extract the text between "destination:/" and the first newline
            } else {
            destination = line.substr(12); // If no newline is found, extract the rest of the string
            }
            std::cout << "Destination message: " << destination << std::endl;
        }
        
        if (message.size() >= 2 && message.substr(message.size() - 2) == "\n\n") { // Check for end of message
        std::string temp = message.substr(0, 4);
            if (temp == "user") {
            Event event(static_cast<const std::string&>(message));
            event.set_channel(destination);
            myClient.addevent(event);
            }
            message.clear(); // Clear the message buffer for the next message
        }
        if (message!=""||message!="\n")
        {
            std::cout <<"message from the server :\n" + message << std::endl;
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
    myname = login;
    return frame;
}

std::string handleSend(const std::string& str, ConnectionHandler& connectionHandler) {
    std::istringstream stream(str);
    std::string command, destination;
    stream >> command >> destination;
    std::cout << "THE DESTINATION IS  ====== \n" + destination + "\n" << std::endl;
    lock.lock();
    names_and_events parser = parseEventsFile(destination);
    lock.unlock();
    std::string frame;
    for (const auto& event : parser.events) {
        frame = "SEND\n";
        frame += "destination:/" + parser.channel_name + "\n";
        frame += "\n";
        frame += "user:" + myname + "\n";
        frame += "city:" + event.get_city() + "\n";
        frame += "event name:" + event.get_name() + "\n";
        frame += "date time:" + std::to_string(event.get_date_time()) + "\n";
        frame += "general information:\n";
        frame += "        active:" + event.get_general_information().at("active") + "\n";
        frame += "        forces_arrival_at_scene:" + event.get_general_information().at("forces_arrival_at_scene") + "\n";
        frame += "description:\n" + event.get_description()+"\n";
        frame += "\0"; // Add null character at the end
        std::cout << "\n ~~~~~~~~~~ \n" + frame + "\n" << std::endl;
        connectionHandler.sendLine(frame);
    }
    return "handled";

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
    status = false;
    int receipt1 = myClient.getcounter();
    std::istringstream stream(str);
    std::string command, receipt;
    stream >> command >> receipt;
    std::string frame = "DISCONNECT\n";
    frame += "receipt:" + std::to_string(receipt1) + "\n";
    frame += "\n" "\0"; // Add three \n before the null character
    return frame;
}

std::string handleError(const std::string& str) {
    return "ERROR\nmessage:Invalid command\n" + str + "\n" + std::string(1, '\0'); // Add three \n before the null character
}

std::string handlesummary(const std::string& str) {
    std::istringstream stream(str);
    std::string command, channel_name, user, file;
    stream >> command >> channel_name >> user >> file;
    // Call the client's summary function with the provided parameters
    lock.lock();
    myClient.summary("/"+channel_name, user, file);
    lock.unlock();
    return "handled";
}

std::string stringToFrame(const std::string& str, ConnectionHandler& connectionHandler) {
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
    } else if (firstWord == "report"&&status) {
        return handleSend(str,connectionHandler);
    } else if (firstWord == "join"&&status) {
        return handleSubscribe(str);
    } else if (firstWord == "exit"&&status) {
        return handleUnsubscribe(str);
    } else if (firstWord == "logout" && status) {
        
        return handleDisconnect(str);
    }else if(firstWord == "summary" && status){
        return handlesummary(str);
    }
    else {
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
    // Start a new thread to listen to the server
    std::thread listenerThread(listenToServer, std::ref(connectionHandler));
    Client myClient(connectionHandler);
    // Main thread will handle reading input from the terminal and sending it to the server
    while (true) {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
        std::string line(buf);
        std::string myFrame = stringToFrame(line,connectionHandler);
        std::cout << "THE LINE YOU WROTE IS  ====== \n" + line + "\n" << std::endl;
        if (myFrame == "ERROR") {
            std::cout << "Invalid command, no frame was sent. Please try again." << std::endl;
            continue;
        }
        std::cout << "THE COMMAND YOU WROTE IS  ====== \n" + myFrame + "\n" << std::endl;

        if (myFrame != "handled") {
            if (!connectionHandler.sendLine(myFrame)) { // Send the frame instead of the raw line
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
            }
        }
        }

    // Wait for the listener thread to finish
    listenerThread.join();
    return 0;
}
///workspaces/SPL251-Assignment3-student-template/client/data/events1.json