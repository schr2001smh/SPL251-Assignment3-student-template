package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class StompMessageEncoderDecoder implements MessageEncoderDecoder<Frame> {

    private StringBuilder messageBuilder = new StringBuilder(); // To accumulate bytes for decoding

    @Override
    public Frame decodeNextByte(byte nextByte) {
        System.out.println("decoding next byte");
        messageBuilder.append((char) nextByte); // Append the byte as a character
        // If we reach the null character (\u0000), parse the accumulated bytes into a Frame
        if (nextByte == '\u0000') {
            String fullMessage = messageBuilder.toString();
            System.out.println("full message:~~~~~~~~3 \n" + fullMessage);
            if (fullMessage.length() > 0 && fullMessage.charAt(0) == '\n') {
                fullMessage = fullMessage.substring(1);
            }
            messageBuilder.setLength(0); // Clear the builder for the next message
            return parseFrame(fullMessage); // Parse the message
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
    // Trim the input message to remove leading/trailing whitespace characters
    message = message.trim();

    String[] lines = message.split("\n");

    // Check if the first line is a null character or empty string and remove it
    if (lines.length > 0 && (lines[0].isEmpty() || lines[0].charAt(0) == '\u0000')) {
        lines = Arrays.copyOfRange(lines, 1, lines.length);
    }

    // First line is the command
    if (lines.length == 0) {
        return null; // No command found
    }
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

    // Parse body
    StringBuilder body = new StringBuilder();
    while (i < lines.length) {
        body.append(lines[i]).append("\n");
        i++;
    }

    return new Frame(command, headers, body.toString().trim());
}

    public String decodeString(byte[] bytes) {
        return new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
    }
}
