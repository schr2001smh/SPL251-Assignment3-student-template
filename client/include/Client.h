#pragma once

#include <iostream>
#include <map>
#include <list>
#include <string>

class Client {
public:
    // Constructor
    Client();

    int getconnectionNumber() const;

    int getcounter();
    
    // Add a subscription
    void addSubscription(const std::string& channelName, int subscriptionId);

    // Add a connected user
    void addConnectedUser(const std::string& userName);

    // Get the channel ID
    int getchannelid(const std::string& channelName) const;

    // Print client information
    void printInfo() const;

private:
    std::map<std::string, int> subscriptions;
    int connectionNumber;
    std::list<std::string> connectedUsers;
};