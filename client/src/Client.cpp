#include "../include/Client.h"

// Constructor
Client::Client(ConnectionHandler& handler) : connectionNumber(0), isconnected(false),connectionHandler(handler){}
Client::Client() : connectionNumber(0), isconnected(false), connectionHandler(*(new ConnectionHandler)) {}
// Get the connection number
int Client::getconnectionNumber() const {
    return connectionNumber;
}
// Check if the client is connected
bool Client::connectionstatus() {
    return connectionHandler.connectionstatus();
}

// Set the client as connected
void Client::connected() {
}

// Set the client as disconnected
void Client::disconnected() {
    isconnected = false;
    connectionHandler.disconncted();
}
// Get the counter
int Client::getcounter() {
    static int counter = 0;
    return counter++;
}

// Add a subscription
void Client::addSubscription(const std::string& channelName, int subscriptionId) {
    subscriptions[channelName] = subscriptionId;
}

// Add a connected user
void Client::addConnectedUser(const std::string& userName) {
    connectedUsers.push_back(userName);
}

// Get the channel ID
int Client::getchannelid(const std::string& channelName) const {
    auto it = subscriptions.find(channelName);
    if (it != subscriptions.end()) {
        return it->second;
    }
    return -1; // Return -1 if the channel is not found
}

// Print client information
void Client::printInfo() const {
    std::cout << "Connection Number: " << connectionNumber << std::endl;
    std::cout << "Subscriptions:" << std::endl;
    for (const auto& [channel, id] : subscriptions) {
        std::cout << "  Channel: " << channel << ", Subscription ID: " << id << std::endl;
    }
    std::cout << "Connected Users:" << std::endl;
    for (const auto& user : connectedUsers) {
        std::cout << "  User: " << user << std::endl;
    }
}