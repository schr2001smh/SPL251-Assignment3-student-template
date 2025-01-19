package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class StompMessageEncoderDecoder implements MessageEncoderDecoder<Frame> {

    private StringBuilder messageBuilder = new StringBuilder(); // To accumulate bytes for decoding

    @Override
    public Frame decodeNextByte(byte nextByte) {
        messageBuilder.append((char) nextByte); // Append the byte as a character

        // If we reach the null character (\u0000), parse the accumulated bytes into a Frame
        if (nextByte == '\u0000') {
            String fullMessage = messageBuilder.toString();
            messageBuilder.setLength(0); // Clear the builder for the next message
            return parseFrame(fullMessage) ; // Parse the message
        }

        return null; // Message is not complete yet
    }

    @Override
    public byte[] encode(Frame frame) {
        StringBuilder encodedMessage = new StringBuilder();

        // Append the command
        encodedMessage.append(frame.getCommand()).append("\n");

        // Append headers
        for (Map.Entry<String, String> header : frame.getHeaders().entrySet()) {
            encodedMessage.append(header.getKey()).append(":").append(header.getValue()).append("\n");
        }

        // Add a blank line and append the body, followed by the null character
        encodedMessage.append("\n").append(frame.getBody()).append("\u0000");

        return encodedMessage.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parses a STOMP message into a Frame object.
     *
     * @param message The raw STOMP message as a string.
     * @return A Frame object representing the parsed message.
     */
    private Frame parseFrame(String message) {
        String[] lines = message.split("\n");

        // First line is the command
        String command = lines[0];

        // Parse headers
        Map<String, String> headers = new HashMap<>();
        int i = 1;
        while (i < lines.length && !lines[i].isEmpty()) {
            String[] parts = lines[i].split(":", 2);
            if (parts.length == 2) {
                headers.put(parts[0].trim(), parts[1].trim());
            }
            i++;
        }

        // Skip the blank line separating headers and body
        i++;

        // Remaining lines are the body
        StringBuilder bodyBuilder = new StringBuilder();
        while (i < lines.length && !lines[i].equals("\u0000")) {
            bodyBuilder.append(lines[i]).append("\n");
            i++;
        }

        // Remove the trailing newline from the body
        String body = bodyBuilder.toString().trim();

        return new Frame(command, headers, body);
    }
}
