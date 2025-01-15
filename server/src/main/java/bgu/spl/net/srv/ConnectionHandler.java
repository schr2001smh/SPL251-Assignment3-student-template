/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package bgu.spl.net.srv;

import java.io.Closeable;

/**
 * The ConnectionHandler interface for Message of type T
 */
public interface ConnectionHandler<T> extends Closeable {
    String channel = "not implemented";

    public String getChannel();

    /**
     * Comment the following lines (both send methods) for the existing implentations to work.
     *
     */

    void send(T msg);

}
