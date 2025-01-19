package bgu.spl.net.srv;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.util.HashSet;
import java.util.Set;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<String> {

    private int connectionId;
    private Connections<String> connections;
    private boolean shouldTerminate = false;
    private Set<String> subscribedTopics;

    public StompMessagingProtocolImpl() {
        subscribedTopics = new HashSet<>();
    }

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public String process(String message) {
        // Decode the STOMP frame
        StompMessageEncoderDecoder s = new StompMessageEncoderDecoder();
        Frame frame = s.parseFrame(message);
        String command = frame.getCommand();

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

    private String handleConnect(Frame frame) {
        String version = frame.getHeaders().get("accept-version");
        if (version == null || !version.equals("1.2")) {
            return buildErrorFrame("Unsupported STOMP version");
        }

        return "CONNECTED\nversion:1.2\n\n\u0000";
    }

    private String handleSubscribe(Frame frame) {
        String destination = frame.getHeaders().get("destination");
        if (destination == null) {
            return buildErrorFrame("Missing destination header for subscription");
        }

        subscribedTopics.add(destination);
        return buildReceiptFrame(frame.getHeaders().get("receipt"));
    }

    private String handleUnsubscribe(Frame frame) {
        String destination = frame.getHeaders().get("destination");
        if (destination == null || !subscribedTopics.contains(destination)) {
            return buildErrorFrame("Cannot unsubscribe from a non-subscribed topic");
        }

        subscribedTopics.remove(destination);
        return buildReceiptFrame(frame.getHeaders().get("receipt"));
    }

    private String handleSend(Frame frame) {
        String destination = frame.getHeaders().get("destination");
        if (destination == null) {
            return buildErrorFrame("Missing destination header");
        }

        // Broadcast the message to all subscribers
        connections.send(destination, frame.getBody());
        return null; // No response to the sender
    }

    private String handleDisconnect(Frame frame) {
        shouldTerminate = true;
        connections.disconnect(connectionId);
        return buildReceiptFrame(frame.getHeaders().get("receipt"));
    }

    private String buildErrorFrame(String message) {
        return "ERROR\nmessage:" + message + "\n\n\u0000";
    }

    private String buildReceiptFrame(String receiptId) {
        return receiptId != null
                ? "RECEIPT\nreceipt-id:" + receiptId + "\n\n\u0000"
                : null;
    }
}
