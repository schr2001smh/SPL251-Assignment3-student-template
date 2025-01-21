package bgu.spl.net.srv;

import java.io.IOException;

public interface Connections<T> {

   boolean send(String channel, T msg, int connectionId) ;


   boolean disconnect(int connectionId);//note

    
}
