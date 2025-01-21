package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
import bgu.spl.net.srv.StompMessageEncoderDecoder;
import bgu.spl.net.srv.StompMessagingProtocolImpl;

public class StompServer {
    private Server server;

    public static void main(String[] args) {

       Server server = Server.threadPerClient(
                7777, //port
                () -> new StompMessagingProtocolImpl(1), //protocol factory
                () -> new StompMessageEncoderDecoder() //message encoder decoder factory
        );
        server.serve();
    }
}
