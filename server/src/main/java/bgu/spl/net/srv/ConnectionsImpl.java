package bgu.spl.net.srv;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    // Singleton instance
    private static ConnectionsImpl<?> instance;

    // Maps
    // connection id associated with handler  -> handler
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> handlers; 

    // channel -> (subscribe id -> handler)
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConnectionHandler<T>>> channels;

    // connection id associated with handler  -> (channel -> uniqueId) his subscribe id
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> connectionChannelIds; 

    // username -> password
    private final ConcurrentHashMap<String, String> userCredentials;
    
    // online connectionIds
    private final List<Integer> online;

    // Private constructor for Singleton
    private ConnectionsImpl() {
        handlers = new ConcurrentHashMap<>();
        channels = new ConcurrentHashMap<>();
        connectionChannelIds = new ConcurrentHashMap<>();
        userCredentials = new ConcurrentHashMap<>();
        online = new ArrayList<>();
        System.out.println("ConnectionsImpl created");
    }

    // Singleton getInstance method
    @SuppressWarnings("unchecked")
    public static <T> ConnectionsImpl<T> getInstance() {
        if (instance == null) {
            synchronized (ConnectionsImpl.class) {
                if (instance == null) {
                    instance = new ConnectionsImpl<>();
                }
            }
        }
        return (ConnectionsImpl<T>) instance;
    }

    // Add a new channel
    private boolean addChannel(String channel) {
        return channels.putIfAbsent(channel, new ConcurrentHashMap<>()) == null;
    }


    // Subscribe to an existing channel with a unique ID
    public boolean subscribe(String channel, int connectionId, int subscribeid) {
        if  ( !handlers.containsKey(connectionId)) {//check if the client exist
             return false; //client does not exist
         }
        if (!channels.containsKey(channel))// check if the channel already exist
        {
        addChannel(channel) ;// if not adds it
        }
        // add to channels map the unique id and the its associated handler
        if ( connectionChannelIds.containsKey(connectionId) && connectionChannelIds.get(connectionId).containsKey(channel) )
        {
            return false ; // the client is already subscribed to this channel
            
        }
        // Add the connection ID to the channel
        connectionChannelIds.putIfAbsent(connectionId, new ConcurrentHashMap<>());
        // add to the connectionChannelIds map the channel and the unique id
        connectionChannelIds.get(connectionId).put(channel, subscribeid);
        channels.get(channel).put(subscribeid, handlers.get(connectionId));
        // Map the channel and unique ID to the connection ID
        //connectionChannelIds.computeIfAbsent(connectionId, k -> new ConcurrentHashMap<>()).put(channel, subscribeid);
        return true;
    }

    // Unsubscribe from a channel using the unique ID
    public boolean unsubscribe(String channel, int connectionId,int subscriptionid) {
        boolean test1=channels.containsKey(channel);
        boolean test2=connectionChannelIds.containsKey(connectionId);
        
        if (!channels.containsKey(channel) || !connectionChannelIds.get(connectionId).get(channel).equals(subscriptionid)) {
            return false; // Channel or connection does not exist
        }
        // Retrieve the unique ID for the channel
        Integer uniqueId = connectionChannelIds.get(connectionId).remove(channel);
        if (uniqueId != null) {
            channels.get(channel).remove(uniqueId);
            if (channels.get(channel).isEmpty()) {
                channels.remove(channel);
            }
        }

    

        return true;
    }



    @Override
    public boolean send(String channel, T msg , int connectionId) {
        int uniqueid =  connectionChannelIds.get(connectionId).get(channel) ;
        ConcurrentHashMap<Integer, ConnectionHandler<T>> subscribers = channels.get(channel);
        if (subscribers != null) {

            for (ConnectionHandler<T> handler : subscribers.values()) {
                handler.send(msg,uniqueid , channel);
            }
            return true ;
        }
        return false ;
    }



    @Override
    public boolean disconnect(int connectionId) {
        if (online.contains(connectionId)) {
            online.remove(connectionId);
        }
        // Remove the connectionId from all subscribed channels
        if (connectionChannelIds.containsKey(connectionId)) {
            connectionChannelIds.get(connectionId).forEach((channel, uniqueId) -> {
                ConcurrentHashMap<Integer, ConnectionHandler<T>> subscribers = channels.get(channel);
                if (subscribers != null) {
                    subscribers.remove(uniqueId);
                    if (subscribers.isEmpty()) {
                        channels.remove(channel);
                    }
                }
            });

        }
        handlers.remove(connectionId);
        connectionChannelIds.remove(connectionId);
        
        return true ;
    }

   public void addHandler(ConnectionHandler<T> handler,int connectionId )
   {
    if(handlers.get(connectionId)== null )
    {
    handlers.put(connectionId, handler);
    }
   }
    // Add or validate a connection
    public boolean addConnection(int connectionId, String username, String password) {
        // Ensure the connectionId is not already in use
        if (online.contains(connectionId)) {
            return false; // Connection ID already in use
        }
        online.add(connectionId);

        // Check if the user already exists
        if (userCredentials.containsKey(username)) {
            // Validate the password
            String storedPassword = userCredentials.get(username);
            if (!storedPassword.equals(password)) {
                return false; // Incorrect password
            }
        } else {
            // Add a new user
            userCredentials.put(username, password);
        }
        return true;
    }
} 