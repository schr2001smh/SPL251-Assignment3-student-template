#include <iostream>
#include <boost/asio.hpp>
#include "../include/ConnectionHandler.h"

using boost::asio::ip::tcp;

ConnectionHandler::ConnectionHandler(std::string host, short port) :
    host_(host), port_(port), io_service_(), socket_(io_service_),isconnected(false) {
    isconnected = false;
    }
    ConnectionHandler::ConnectionHandler() :
    host_(""), port_(0), io_service_(), socket_(io_service_),isconnected(false) {}

ConnectionHandler::~ConnectionHandler() {
    close();
}

bool ConnectionHandler::connect() {
    std::cout << "Starting connect to "
              << host_ << ":" << port_ << std::endl;
    try {
        tcp::resolver resolver(io_service_);
        tcp::resolver::query query(host_, std::to_string(port_));
        tcp::resolver::iterator endpoint_iterator = resolver.resolve(query);
        boost::asio::connect(socket_, endpoint_iterator);
    } catch (std::exception &e) {
        std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::getBytes(char *bytes, unsigned int bytesToRead) {
    size_t tmp = 0;
    try {
        while (bytesToRead > tmp) {
            tmp += socket_.read_some(boost::asio::buffer(bytes + tmp, bytesToRead - tmp));
        }
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendBytes(const char *bytes, int bytesToWrite) {
    int tmp = 0;
    try {
        while (bytesToWrite > tmp) {
            tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp));
        }
    } catch (std::exception &e) {
        std::cerr << "send failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}
bool ConnectionHandler::connectionstatus() {
    return isconnected;
}

void ConnectionHandler::disconncted() {
    isconnected = false;
}

bool ConnectionHandler::getLine(std::string &line) {
    bool result = getFrameAscii(line, '\n');
    return result;
}

bool ConnectionHandler::sendLine(std::string &line) {
    return sendFrameAscii(line, '\0'); // Use '\0' as the delimiter instead of '\n'
}

bool ConnectionHandler::getFrameAscii(std::string &frame, char delimiter) {
    char ch;
    std::string command;
    bool isFirstLine = true;
    // Stop when we encounter the null character.
    // Notice that the null character is not appended to the frame string.
    try {
        do {
            if (!getBytes(&ch, 1)) {
                return false;
            }
            if (ch == '#') {
                frame.append(1, '\n'); // Replace special character with newline
            } else if (ch != '\0') {
                frame.append(1, ch);
                if (isFirstLine && ch == '\n') {
                    command = frame.substr(0, frame.find('\n'));
                    if (command=="CONNECTED"&&isFirstLine)
                    {
                        isconnected = true;
                    }
                    isFirstLine = false;
                }
            }
        } while (delimiter != ch);
    } catch (std::exception &e) {
        std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
        return false;
    }
    return true;
}

bool ConnectionHandler::sendFrameAscii(const std::string &frame, char delimiter) {
    bool result = sendBytes(frame.c_str(), frame.length());
    if (!result) return false;
    return sendBytes(&delimiter, 1); // Send the delimiter without appending a newline
}

void ConnectionHandler::close() {
    try {
        socket_.close();
    } catch (...) {
        std::cerr << "closing failed: connection already closed" << std::endl;
    }
}
