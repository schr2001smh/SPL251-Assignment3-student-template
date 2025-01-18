package bgu.spl.net.srv;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> handlers; // connectionId -> handler
    private AtomicInteger connectionIdCounter;

    public ConnectionsImpl() {
        handlers = new ConcurrentHashMap<>();
        connectionIdCounter = new AtomicInteger(0);
    }
    
    public void send(String channel, T msg){
        
        for (ConnectionHandler<T> handler : handlers.values()) {
                handler.send(msg);
        }
    }
    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = handlers.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false;
    }

    
    public void broadcast(T msg) {
        for (ConnectionHandler<T> handler : handlers.values()) {
            handler.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        handlers.remove(connectionId);
    }

    public int addConnection(ConnectionHandler<T> handler) {
        int connectionId = connectionIdCounter.incrementAndGet();
        handlers.put(connectionId, handler);
        return connectionId;
    }
}