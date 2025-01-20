package bgu.spl.net.srv;

import java.io.IOException;

public interface Connections<T> {

    public void send(String channel, T msg , int connectionId) ;

    void message(String channel, T msg);

    void disconnect(int connectionId);//note

    
}
