package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    // Singleton instance
    private static ConnectionsImpl<?> instance;

    // Maps
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> handlers; // connectionId -> handler
    private final ConcurrentHashMap<String, ConcurrentHashMap<Integer, ConnectionHandler<T>>> channels; // channel -> (uniqueId -> handler)
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> connectionChannelIds; // connectionId -> (channel -> uniqueId)
    private final ConcurrentHashMap<String, String> userCredentials; // username -> password

    // Private constructor for Singleton
    private ConnectionsImpl() {
        handlers = new ConcurrentHashMap<>();
        channels = new ConcurrentHashMap<>();
        connectionChannelIds = new ConcurrentHashMap<>();
        userCredentials = new ConcurrentHashMap<>();
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
    public boolean subscribe(String channel, int connectionId, int uniqueId) {
        if (!channels.containsKey(channel))
        {
        addChannel(channel) ;
        }
       if  ( !handlers.containsKey(connectionId)) {
            return false; // Channel or client does not exist
        }

        // Add the subscription
        channels.get(channel).put(uniqueId, handlers.get(connectionId));

        // Map the channel and unique ID to the connection ID
        connectionChannelIds.computeIfAbsent(connectionId, k -> new ConcurrentHashMap<>()).put(channel, uniqueId);

        return true;
    }

    // Unsubscribe from a channel using the unique ID
    public boolean unsubscribe(String channel, int connectionId) {
        if (!channels.containsKey(channel) || !connectionChannelIds.containsKey(connectionId)) {
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
            connectionChannelIds.remove(connectionId);
            return true ;
        }
        return false ;
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
        if (handlers.containsKey(connectionId)) {
            return false; // Connection ID already in use
        }

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