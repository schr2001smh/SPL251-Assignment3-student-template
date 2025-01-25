package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
import bgu.spl.net.srv.StompMessageEncoderDecoder;
import bgu.spl.net.srv.StompMessagingProtocolImpl;

public class StompServer {
    private Server server;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java StompServer <port> <reactor/tpc>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String serverType = args[1];

        if (serverType.equalsIgnoreCase("tpc")) {
            createThreadPerClientServer(port);
        } else if (serverType.equalsIgnoreCase("reactor")) {
            createReactorServer(port);
        } else {
            System.err.println("Invalid server type. Use 'reactor' or 'tpc'.");
        }
    }

    private static void createThreadPerClientServer(int port) {
        Server server = Server.threadPerClient(
                port, // port
                () -> new StompMessagingProtocolImpl(1), // protocol factory
                () -> new StompMessageEncoderDecoder() // message encoder decoder factory
        );
        server.serve();
    }

    private static void createReactorServer(int port) {
        Server server = Server.reactor(
                Runtime.getRuntime().availableProcessors(),
                port, // port
                () -> new StompMessagingProtocolImpl(1), // protocol factory
                () -> new StompMessageEncoderDecoder() // message encoder decoder factory
        );
        server.serve();
    }
}