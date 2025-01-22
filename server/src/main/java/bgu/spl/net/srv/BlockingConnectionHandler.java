package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {
    private int connectionId ;
    private final StompMessagingProtocolImpl protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private Connections<Frame> connections;

    public BlockingConnectionHandler(int connectionId ,Socket sock,
     MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = new StompMessagingProtocolImpl(connectionId);
    this.connectionId = connectionId;
    }

    @Override
    public void run() {
    try (Socket sock = this.sock) { //just for automatic closing
        int read;
        System.out.println("client connected and the handler trying to read");
        in = new BufferedInputStream(sock.getInputStream());
        out = new BufferedOutputStream(sock.getOutputStream());
        while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
        T nextMessage = encdec.decodeNextByte((byte) read);
        if (nextMessage != null) {
            Frame response = protocol.process((Frame) nextMessage);
            if (response != null) {
            System.out.println("printing response line########### \n"+response);
            out.write(encdec.encode(response));
            System.out.println("handler trying to send!!!!!!!!!! \n" +
                encdec.decodeString(encdec.encode(response)));
            out.flush();
            }
        }
        }

    } catch (IOException ex) {
        ex.printStackTrace();
    }
    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }


    @Override
    public void send(T msg, int id, String channel) {
        try {
            // Generate a unique message ID (e.g., using a counter or timestamp)
            String messageId = String.valueOf(System.currentTimeMillis());
            // Construct the headers for the MESSAGE frame
            Map<String, String> headers = new HashMap<>();
            headers.put("sender", String.valueOf(id)); // Subscription ID
            headers.put("message-id", messageId);      // Unique message ID
            headers.put("channel", channel);           // Destination/channel name
    
            // Create a Frame object for the MESSAGE frame
            Frame messageFrame = new Frame("MESSAGE", headers, msg.toString());
            System.out.println("SERVER ENCODING MESSAGE FRAME******** \n" + messageFrame);
    
            // Encode the Frame into bytes
            byte[] encodedFrame = encdec.encode(messageFrame);
            System.out.println("SERVER ENCODED MESSAGE FRAME**********\n" + encdec.decodeString(encodedFrame));
    
            // Write the encoded frame to the output stream
            synchronized (this) { // Synchronize to ensure thread safety when writing
                out.write(encodedFrame);
                out.flush(); // Flush to ensure the data is sent immediately
            }
        } catch (IOException e) {
            e.printStackTrace(); // Handle any I/O exceptions during writing
        }
    }

}
