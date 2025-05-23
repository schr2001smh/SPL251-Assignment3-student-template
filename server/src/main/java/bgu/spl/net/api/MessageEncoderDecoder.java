package bgu.spl.net.api;

import bgu.spl.net.srv.Frame;

public interface MessageEncoderDecoder<T> {

    /**
     * add the next byte to the decoding process
     *
     * @param nextByte the next byte to consider for the currently decoded
     * message
     * @return a message if this byte completes one or null if it doesnt.
     */
    T decodeNextByte(byte nextByte);

    /**
     * encodes the given message to bytes array
     *
     * @param message the message to encode
     * @return the encoded bytes
     */
    byte[] encode(Frame  message);


     String decodeString(byte[] bytes);
}
