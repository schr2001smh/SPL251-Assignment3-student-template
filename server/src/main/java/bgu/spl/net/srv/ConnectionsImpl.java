import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connections;
    private AtomicInteger connectionIdCounter;

    public ConnectionsImpl() {
        connections = new ConcurrentHashMap<>();
        connectionIdCounter = new AtomicInteger(0);
    }
    
    public void send(String channel, T msg){
        
        for (ConnectionHandler<T> handler : connections.values()) {
            if(handler.getChannel().equals(channel)){
                handler.send(msg);
            }
        }
    }
    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = connections.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false;
    }

    @Override
    public void broadcast(T msg) {
        for (ConnectionHandler<T> handler : connections.values()) {
            handler.send(msg);
        }
    }

    @Override
    public void disconnect(int connectionId) {
        connections.remove(connectionId);
    }

    public int addConnection(ConnectionHandler<T> handler) {
        int connectionId = connectionIdCounter.incrementAndGet();
        connections.put(connectionId, handler);
        return connectionId;
    }
}