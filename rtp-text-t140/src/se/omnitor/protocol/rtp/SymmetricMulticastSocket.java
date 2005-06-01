/* 
 * Copyright (C) 2004  University of Wisconsin-Madison and Omnitor AB
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package se.omnitor.protocol.rtp;

import java.net.DatagramPacket;
import java.net.MulticastSocket;


/**
 * A MulticastSocket implementation that can send and receive on the same port.
 *
 * @author Erik Zetterström, Omnitor AB
 */
public class SymmetricMulticastSocket extends MulticastSocket {

    /**
     * Constructs a new SymmetricMulticastSocket
     *
     * @param localPort The local port used for this socket.
     */
 
    public SymmetricMulticastSocket(int localPort) throws java.io.IOException {
       
	super(localPort);

	try {
	    super.setSoTimeout(100);
	} catch (Exception e) {
	    System.err.println("SymmetricMulticastSocket failed to set SO_TIMEOUT."+e);
	}
    }

    /**
     *  Sends a DatagramPacket. 
     *  Synchronized to only allow one simultaneous send or receive action 
     *  at one time.
     *
     * @param p The datagram packet to send.
     */
    public synchronized void send(DatagramPacket p) {
	try {
	    super.send(p);
	} catch (Exception e) {
	    System.out.println("SymmetricMulticastSocket: Error sending. "+e);
	}
    }

    /**
     * Receives a datagram packet.
     * Synchronized to allow only one simultaneous send or receive operation.
     * 
     * @param p The received packet.
     */
    public synchronized void receive(DatagramPacket p) throws java.net.SocketTimeoutException {
	try {
	    super.receive(p);
	} catch (java.net.SocketTimeoutException ste) {
	    throw ste;
	    //It's ok to timeout
	} catch (Exception e) {
	    System.err.println("RTPSymmetricThread: Error on receive. "+e);
	}
    } 
}



