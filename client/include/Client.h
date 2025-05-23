#pragma once

#include <iostream>
#include <map>
#include <list>
#include <string>
#include "../include/ConnectionHandler.h"
#include "../include/event.h"

class Client {
public:
    std::string getname();
    
    void summary(std::string channelName, std::string userName,std::string location);

    void sethandler(ConnectionHandler& handler);

    void setname(std::string name);
    
    void connected();

    void disconnected();

    bool connectionstatus() ;

    // Constructor
    Client(ConnectionHandler& connectionHandler);
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
    void addevent(Event event);

private:
    std::map<std::string, int> subscriptions;
    int connectionNumber;
    std::list<std::string> connectedUsers;
    bool isconnected;
    ConnectionHandler& connectionHandler;
    std::string name;
    int counter = 0;
    std::map<std::string,std::map<std::string,std::list<Event>>> events;
    
};