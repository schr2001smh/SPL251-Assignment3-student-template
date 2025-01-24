#include "../include/Client.h"
#include <fstream>
// Constructor
Client::Client(ConnectionHandler& handler) : connectionNumber(0), isconnected(false), connectionHandler(handler) {
    events = std::map<std::string, std::map<std::string, std::list<Event>>>();
}
Client::Client() : connectionNumber(0), isconnected(false), connectionHandler(*(new ConnectionHandler)) {
    events = std::map<std::string, std::map<std::string, std::list<Event>>>();
}


void Client::summary(std::string channelName, std::string userName, std::string location) {
    std::ofstream outFile(location, std::ios::app); // Open file in append mode
    if (!outFile) {
        std::cerr << "Error opening file: " << location << std::endl;
        return;
    }
    // Collect and sort events
    std::vector<Event> sortedEvents(events[channelName][userName].begin(), events[channelName][userName].end());
    std::sort(sortedEvents.begin(), sortedEvents.end(), [](const Event& a, const Event& b) {
        if (a.get_date_time() == b.get_date_time()) {
            return a.get_name() < b.get_name();
        }
        return a.get_date_time() < b.get_date_time();
    });
    // Calculate stats
    int totalReports = sortedEvents.size();
    int activeReports = std::count_if(sortedEvents.begin(), sortedEvents.end(), [](const Event& e) { return !e.get_general_information().begin()->first.empty(); });
    int forcesArrivalReports = std::count_if(sortedEvents.begin(), sortedEvents.end(), [](const Event& e) { return !e.get_general_information().begin()->second.empty(); });
    // Write stats to file
    outFile << "Channel: " << channelName << std::endl;
    outFile << "Stats:" << std::endl;
    outFile << "Total: " << totalReports << std::endl;
    outFile << "Active: " << activeReports << std::endl;
    outFile << "Forces arrival at scene: " << forcesArrivalReports << std::endl;
    outFile << "Event Reports:" << std::endl;
    // Write event reports to file
    for (const auto& event : sortedEvents) {
        outFile << "Report:" << std::endl;
        outFile << "  City: " << event.get_city() << std::endl;
        outFile << "  Date time: " << event.get_date_time() << std::endl;
        outFile << "  Event name: " << event.get_name() << std::endl;
        std::string description = event.get_description();
        if (description.length() > 27) {
            description = description.substr(0, 27) + "...";
        }
        outFile << "  Summary: " << description << std::endl;
    }

    outFile.close();
}



void Client::addevent(Event event){
    if(events.find(event.get_channel_name()) == events.end()){
        events[event.get_channel_name()] = std::map<std::string,std::list<Event>>();
    }
    if(events[event.get_channel_name()].find(event.getEventOwnerUser()) == events[event.get_channel_name()].end()){
        events[event.get_channel_name()][event.getEventOwnerUser()] = std::list<Event>();
    }
    events[event.get_channel_name()][event.getEventOwnerUser()].push_back(event);
}
// Get the connection number
int Client::getconnectionNumber() const {
    return connectionNumber;
}
// Check if the client is connected
bool Client::connectionstatus() {
    return connectionHandler.connectionstatus();
}
    std::string Client::getname(){
        return name;
    }

    void Client::setname(std::string newname){
        name = newname;
    }
// Set the client as connected
void Client::connected() {
}
void Client::disconnected() {
    isconnected = false;
    connectionHandler.disconncted();
}
// Get the counter
int Client::getcounter() {
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