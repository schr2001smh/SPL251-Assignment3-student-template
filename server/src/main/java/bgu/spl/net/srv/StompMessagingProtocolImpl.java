package bgu.spl.net.srv;

import bgu.spl.net.api.StompMessagingProtocol;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<Frame> {

    private int connectionId;
    private Connections<Frame> connections;
    private boolean shouldTerminate = false;
    private Set<String> subscribedTopics;

    public StompMessagingProtocolImpl() {
        System.out.println("StompMessagingProtocolImpl created");
        subscribedTopics = new HashSet<>();
    }

    @Override
    public void start(int connectionId, Connections<Frame> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public Frame process(Frame frame) {
        String command = frame.getCommand();
        System.out.println("Processing frame with command: " + command);
        // Process based on the STOMP command
        switch (command) {
            case "CONNECT":
                return handleConnect(frame);
            case "SUBSCRIBE":
                return handleSubscribe(frame);
            case "UNSUBSCRIBE":
                return handleUnsubscribe(frame);
            case "SEND":
                return handleSend(frame);
            case "DISCONNECT":
                return handleDisconnect(frame);
            default:
                return buildErrorFrame("Unknown command: " + command);
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private Frame handleConnect(Frame frame) {
        System.out.println("this is the frame with null version?? \n" + frame.toString());
        String version = frame.getHeaders().get("accept - version");
        if (version == null || !version.equals("1.2")) {
            return buildErrorFrame("Unsupported STOMP version");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("version", "1.2");
        return new Frame("CONNECTED", headers, "");
    }

    private Frame handleSubscribe(Frame frame) {
        String destination = frame.getHeaders().get("destination");
        if (destination == null) {
            return buildErrorFrame("Missing destination header for subscription");
        }

        subscribedTopics.add(destination);

        String receiptId = frame.getHeaders().get("receipt");
        if (receiptId != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("receipt-id", receiptId);
            return new Frame("RECEIPT", headers, "");
        }
        return null; // No receipt requested
    }

    private Frame handleUnsubscribe(Frame frame) {
        String destination = frame.getHeaders().get("destination");
        if (destination == null || !subscribedTopics.contains(destination)) {
            return buildErrorFrame("Cannot unsubscribe from a non-subscribed topic");
        }

        subscribedTopics.remove(destination);

        String receiptId = frame.getHeaders().get("receipt");
        if (receiptId != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("receipt-id", receiptId);
            return new Frame("RECEIPT", headers, "");
        }
        return null; // No receipt requested
    }

    private Frame handleSend(Frame frame) {
        String destination = frame.getHeaders().get("destination");
        if (destination == null) {
            return buildErrorFrame("Missing destination header");
        }

        // Broadcast the message to all subscribers
        connections.send(destination, frame);
        return null; // No response to the sender
    }

    private Frame handleDisconnect(Frame frame) {
        shouldTerminate = true;
        connections.disconnect(connectionId);

        String receiptId = frame.getHeaders().get("receipt");
        if (receiptId != null) {
            Map<String, String> headers = new HashMap<>();
            headers.put("receipt-id", receiptId);
            return new Frame("RECEIPT", headers, "");
        }
        return null; // No receipt requested
    }

    private Frame buildErrorFrame(String message) {
        Map<String, String> headers = new HashMap<>();
        headers.put("message", message);
        System.out.println("PRINITNG ERROR FROM PROTOCOL   "+message);
        return new Frame("ERROR", headers, "");
    }
}
