package bgu.spl.net.srv;

import java.util.HashMap;
import java.util.Map;

public class Frame {
    private final String command;
    private final Map<String, String> headers;
    private final String body;

    /**
     * Constructor for the Frame class.
     *
     * @param command The STOMP command (e.g., CONNECT, SEND, SUBSCRIBE).
     * @param headers The headers as a map of key-value pairs.
     * @param body    The body of the frame as a string.
     */
    public Frame(String command, Map<String, String> headers, String body) {
        this.command = command;
        this.headers = headers;
        this.body = body;
        System.out.println("frame created \n"+this.toString());
    }

    /**
     * @return The command of the frame.
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return The headers of the frame as a map.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * @return The body of the frame as a string.
     */
    public String getBody() {
        return body;
    }
    
    
    @Override
    public String toString() {
        StringBuilder frameBuilder = new StringBuilder();
        frameBuilder.append(command).append("\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            frameBuilder.append(header.getKey()).append(":").append(header.getValue()).append("\n");
        }
        frameBuilder.append("#\n"); // Special character instead of an empty line
        // Debug statement to print the body
        System.out.println("Body: " + body);
        // Null check before appending the body
        if (body != null) {
            frameBuilder.append(body);
        } else {
            frameBuilder.append("No body content");
        }
        return frameBuilder.toString();
    }
}
