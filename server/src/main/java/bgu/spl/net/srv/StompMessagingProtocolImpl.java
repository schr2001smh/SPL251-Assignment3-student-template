package bgu.spl.net.srv;

import bgu.spl.net.api.StompMessagingProtocol;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<Frame> {

    private int connectionId;
    private ConnectionsImpl connections;
    private boolean shouldTerminate = false;
    private Set<String> subscribedTopics;
    private ConcurrentHashMap<Integer, String> subscriptionId ; // Maps subscription IDs to channel names

    

    public StompMessagingProtocolImpl(int connectionId) {
        System.out.println("StompMessagingProtocolImpl created");
        subscribedTopics = new HashSet<>();
        this.connectionId = connectionId;
        this.subscriptionId= new ConcurrentHashMap<>();
        this.connections = ConnectionsImpl.getInstance() ;
    }



    @Override
    public Frame process(Frame frame) {
        String command = frame.getCommand();
        System.out.println("Processing frame with command: " + command);

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
                return buildErrorFrame("Unknown command: " + command ,this.connectionId);
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    private Frame handleConnect(Frame frame) {
        System.out.println("recieved connection frame \n" + frame);
        // Parse the "accept-version" header
        String version = frame.getHeaders().get("accept-version");
        if (version == null || !version.equals("1.2")) {
            // Build an ERROR frame if the version is unsupported
            return buildErrorFrame("Unsupported STOMP version: " + (version == null ? "none" : version),this.connectionId);
        }
    
        // Parse additional headers if needed (e.g., host, login, passcode)
        String host = frame.getHeaders().get("host");
        String login = frame.getHeaders().get("login");
        String passcode = frame.getHeaders().get("passcode");
    
        // (Optional) Validate the headers (e.g., check for null or invalid values)
        if (host == null || login == null || passcode == null) {
            return buildErrorFrame("Missing required headers", this.connectionId);
        }
    
    if( !connections.addConnection(this.connectionId , login , passcode) )
    {
        return buildErrorFrame("could not add client", this.connectionId);
    }
    return buildConnectedFrame(connectionId) ;
    }
    

    private Frame handleSubscribe(Frame frame) {
        // Parse the "destination" header
        String destination = frame.getHeaders().get("destination");
        if (destination == null) {
            // Return an ERROR frame if the "destination" header is missing
            return buildErrorFrame("Missing destination header for subscription", this.connectionId);
        }
    
        // Parse the "id" header (unique subscription ID)
        String uniqueIdStr = frame.getHeaders().get("id");
        if (uniqueIdStr == null) {
            // Return an ERROR frame if the "id" header is missing
            return buildErrorFrame("Missing unique subscription ID (id header)", this.connectionId);
        }
    
        // Convert the unique ID to an integer
        int uniqueId;
        try {
            uniqueId = Integer.parseInt(uniqueIdStr);
        } catch (NumberFormatException e) {
            // Return an ERROR frame if the "id" header is not a valid integer
            return buildErrorFrame("Invalid unique subscription ID: " + uniqueIdStr, this.connectionId);
        }

        if (!connections.subscribe(destination, this.connectionId, uniqueId)) {

            return buildErrorFrame("Failed to subscribe to channel: " + destination, this.connectionId);
        }
        subscribedTopics.add(destination) ;
        subscriptionId.put(uniqueId,destination) ;
            return createReceiptFrame(uniqueId);
    }
    

    private Frame handleUnsubscribe(Frame frame) {
        // Parse the "id" header (unique subscription ID)
        String uniqueIdStr = frame.getHeaders().get("id");
        if (uniqueIdStr == null) {
            // Return an ERROR frame if the "id" header is missing
            return buildErrorFrame("Missing unique subscription ID (id header)", this.connectionId);
        }
    
        // Convert the unique ID to an integer
        int uniqueId;
        try {
            uniqueId = Integer.parseInt(uniqueIdStr);
        } catch (NumberFormatException e) {
            // Return an ERROR frame if the "id" header is not a valid integer
            return buildErrorFrame("Invalid unique subscription ID: " + uniqueIdStr, this.connectionId);
        }
    
        // Look up and remove the subscription from the map
        String destination = subscriptionId.get(uniqueId);
        if (destination == null) {
            // Return an ERROR frame if no subscription exists for the given ID
            return buildErrorFrame("No subscription found for ID: " + uniqueId, this.connectionId);
        }
    
        // Notify connections to unsubscribe (if necessary)
       if( !connections.unsubscribe(destination, this.connectionId))
       {
        return buildErrorFrame("did not unsuscribe ", uniqueId) ;
       }
       subscribedTopics.remove(destination) ;
       subscriptionId.remove(uniqueId) ;
       return createReceiptFrame(uniqueId); 

    }
    

    private Frame handleSend(Frame frame) {
        String destination = frame.getHeaders().get("destination");
        if (destination == null) {
            return buildErrorFrame("Missing destination header", this.connectionId);
        }

        if (!connections.send(destination, frame.getBody() , connectionId))
        {
            this.shouldTerminate = true ;
            return buildErrorFrame("could not send message ", connectionId) ;
        }
        return null;
    }

    private Frame handleDisconnect(Frame frame) {

        if(!connections.disconnect(this.connectionId))
        {
            return buildErrorFrame("could not disconnect", connectionId);
        }

return createReceiptFrame(connectionId);
    }
    



    private Frame buildErrorFrame(String message, int connectionId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("message", message);
        headers.put("connection-id", String.valueOf(connectionId)); // Include connection ID in the error frame
        return new Frame("ERROR", headers, "The server cannot process the request: " + message);
    }
    
    private Frame buildConnectedFrame(int connectionId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("version", "1.2"); // STOMP version
        headers.put("connection-id", String.valueOf(connectionId)); // Include connection ID
        return new Frame("CONNECTED", headers, ""); // Frame body remains empty
    }
    private Frame createReceiptFrame(int id) {
        Map<String, String> headers = new HashMap<>();
        headers.put("receipt-id", String.valueOf(id)); // Include the receipt-id in the headers
        return new Frame("RECEIPT", headers, ""); // Frame body is empty
    }
    


}
