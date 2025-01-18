package bgu.spl.net.impl.echo;
import bgu.spl.net.srv.*;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

import java.time.LocalDateTime;

public class EchoProtocol<T> implements StompMessagingProtocol<String> {
    private int ownerID;
    private Connections<String> connections;
    private boolean shouldTerminate = false;


    
    @Override
    public String process(String msg) {
        shouldTerminate = "bye".equals(msg);
        System.out.println("[" + LocalDateTime.now() + "]: " + msg);
        return createEcho(msg);
         // should proccess the message and send a response t
         // the clinets by the connections object
    }

    @Override
    public void start(int connectionId, Connections<String> connections) {
        ownerID = connectionId;
        this.connections = connections;
        System.out.println("echo protocol started");
    }
    private String createEcho(String message) {
        String echoPart = message.substring(Math.max(message.length() - 2, 0), message.length());
        return message + " .. " + echoPart + " .. " + echoPart + " ..";
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
}
