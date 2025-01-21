package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private final Supplier<MessagingProtocol<T>> protocolFactory;
    private final ConnectionsImpl connections;
    private ServerSocket sock;

    public BaseServer(
            int port,
            Supplier<MessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {
        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.connections = ConnectionsImpl.getInstance();
        this.sock = null;
    }

    @Override
    public void serve() {
        int connectionId = 0;
        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("Server started");
            this.sock = serverSock; // Assign the server socket for later closing

            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Waiting for client connection...");
                Socket clientSock = serverSock.accept(); // Blocking call
                System.out.println("Client connected");

                // Create a new connection handler for the client
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        connectionId,
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get()
                );

                // Add the handler to connections for tracking
                connections.addHandler(handler, connectionId);
                connectionId++;

                // Execute the handler (thread-per-client behavior)
                execute(handler);
            }
        } catch (IOException ex) {
            System.err.println("Error occurred while running the server: " + ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("Server shutting down...");
    }

    @Override
    public void close() throws IOException {
        if (sock != null) {
            System.out.println("Closing server socket...");
            sock.close();
        }
    }

    /**
     * This method is implemented by subclasses to determine how the handler is executed.
     * For example, it can start a new thread or use an executor service.
     *
     * @param handler the connection handler to execute
     */
    protected abstract void execute(BlockingConnectionHandler<T> handler);
}
