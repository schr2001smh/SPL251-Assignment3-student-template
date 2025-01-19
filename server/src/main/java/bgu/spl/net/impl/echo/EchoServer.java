package bgu.spl.net.impl.echo;

import bgu.spl.net.srv.Server;
import bgu.spl.net.impl.echo.EchoProtocol;

public class EchoServer {

    public static void main(String[] args) {

        // you can use any server... 
        Server.threadPerClient(
                7777, //port
                () -> new bgu.spl.net.srv.StompMessagingProtocolImpl(), //protocol factory
                () -> new bgu.spl.net.srv.StompMessageEncoderDecoder() //message encoder decoder factory
        ).serve();

        // Server.reactor(
        //         Runtime.getRuntime().availableProcessors(),
        //         7777, //port
        //         () -> new EchoProtocol<>(), //protocol factory
        //         LineMessageEncoderDecoder::new //message encoder decoder factory
        // ).serve();
    }
}
