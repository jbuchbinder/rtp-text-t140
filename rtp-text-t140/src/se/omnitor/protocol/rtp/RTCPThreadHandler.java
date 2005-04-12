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

import java.net.InetAddress;


// IP: Made it possible to open send and receive independently

/**
 * This class creates and starts the RTCP sender and receiver threads
 *
 * @author Unknown
 */
public class RTCPThreadHandler extends java.lang.Object
{
    /**
     *   Reference to the RTCP Receiver Thread
     *
     */
    private RTCPReceiverThread rtcpReceiverThread;
    
    /**
     *   Reference to the RTCP Sender Thread
     *
     */
    private RTCPSenderThread rtcpSenderThread;
    
    // IP: Added following chunk
    Session rtpSession;
    InetAddress multicastGroupIPAddress;
    
    /**
     * Constructor creates the sender and receiver
     * threads. (Does not start the threads)
     *
     * @param multicastGroupIPAddress Dotted representation of the Multicast
     * address.
     * @param rtpSession The session to use
     */
    public RTCPThreadHandler (  InetAddress multicastGroupIPAddress,
                                Session rtpSession
				)
    {
        this.multicastGroupIPAddress = multicastGroupIPAddress;
        this.rtpSession = rtpSession;
	
    }
    
    /**
     * Starts the RTCP Sender thread.
     *
     * @param rtcpSendFromPort The RTCP port to send data from
     * @param rtcpGroupPort Port for multicast group (for receiving)
     */
    public void createAndStartRTCPSenderThread(int rtcpSendFromPort, 
					       int rtcpGroupPort)
    {
        // create an rtcpSender thread
        rtcpSenderThread = 
	    new RTCPSenderThread ( multicastGroupIPAddress, rtcpSendFromPort, 
				   rtcpGroupPort, rtpSession );
        // Start thread
        rtcpSenderThread.start();
    }
    
    /**
     * Stops the receiver thread
     *
     */
    public void stopRTCPReceiverThread()
    {
        rtcpReceiverThread.stop();
    }
    
    /**
     * Starts the RTCP Receiver thread.
     *
     * @param rtcpGroupPort Port for multicast group (for receiving)
     */
    public synchronized void createAndStartRTCPReceiverThread
	(int rtcpGroupPort)
    {
        // create an rtcpReceiver thread
        rtcpReceiverThread = 
	    new RTCPReceiverThread ( multicastGroupIPAddress, rtcpGroupPort, 
				     rtpSession );
        // Start thread
        rtcpReceiverThread.start();
    }
    
    /**
     *   Interrupts a running RTCP sender thread.  This will
     *   cause the sender to send BYE packet and finally terminate.
     *
     */
    public synchronized void stopRTCPSenderThread()
    {
        rtcpSenderThread.interrupt();
	rtcpSenderThread = null;
    }
}
