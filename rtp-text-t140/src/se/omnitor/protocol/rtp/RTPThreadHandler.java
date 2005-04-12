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

import se.omnitor.protocol.rtp.packets.RTPPacket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Logger;
import java.util.Random;
import java.io.IOException;

/**
 * This class encapsulates the functionality to construct and send out RTP
 * packets and also to receive RTP Packets. It provides a seperate thread to
 * receive and send out RTP packets.
 *
 * @author Unknown
 */
// IP: Removed all static classes, methods and variables
// IP: Made it possible to create this class and open send and receive
//     independently
// IP: Implement runnable instead of extending Thread
public class RTPThreadHandler implements Runnable
{
    /**
     *   RTP Header Length = 12
     */
    protected static final int RTP_PACKET_HEADER_LENGTH = 12;
    
    /**
     *   Multicast Port for RTP Packets
     */
    private int 	m_mcastPort;
    
    /**
     *   Sender Address for RTP Packets
     */
    private InetAddress m_InetAddress;
    
    /**
     *   Sender Port for RTP Packets
     */
    private int m_sendPort;
    
    /**
     *   Multicast Socket for sending RTP
     */
    //MulticastSocket m_sockSend;
    private DatagramSocket m_sockSend;
    
    // IP: Added StateThread instead of extending Thread
    private StateThread thisThread;
    
    // IP: Moved to here
    //MulticastSocket s;
    private DatagramSocket m_sockReceive;
    
    /**
     *   Initialize Random Number Generator
     */
    // IP: Moved to class Session
    //    public Random RandomNumGenerator = new Random();
    
    /**
     *   Random Offset -32 bit
     */
    // IP: Moved to class Session
    //    public final short RandomOffset = 
    // (short) Math.abs ( RandomNumGenerator.nextInt() & 0x000000FF) ;
    
    private Session rtpSession;
    
    /***********************************************************************
     * RTP Header related fields
     *
     ***********************************************************************
     
     /**
      *   First Byte of header
      */
    // +-+-+-+-+-+-+-+-+
    private byte[] vpxcc = {(byte) 0x80};      // |V=2|P|X|   CC  |
    // +-+-+-+-+-+-+-+-+
    //  1 0 0 0 0 0 0 0 = 0x80
    
    
    /**
     *   Second Byte of header
     */
    private byte[] m_PT = new byte [1];
    // +-+-+-+-+-+-+-+-+
    // |M|     PT      |
    // +-+-+-+-+-+-+-+-+
    //  0 1 0 1 1 0 0 0
    
    /**
     *   Sequence Number
     */
    private long    sequence_number;            // 16 bits
    
    /**
     *   TimeStamp
     */
    private long    timestamp;                  // 32 bits


    private Logger logger = Logger.getLogger("se.omnitor.rtp");

    private boolean packetLossEnabled = false; //EZ: Enables packet loss
    private int lossRatio = 50;                 //EZ: The packet loss ratio
    /**
     * Constructor for the class. Takes in a TCP/IP Address and a port 
     * number. It initializes a new multicast socket according to the
     * multicast address and the port number given
     *
     * @param multicastAddress Dotted representation of the Multicast address.
     * @param rtpSession The Session to use
     */
    public RTPThreadHandler(InetAddress multicastAddress, Session rtpSession) {
	m_InetAddress = multicastAddress;
	// IP: Init
	m_mcastPort = 0;
	m_sendPort = 0;
        
	Random rnd = new Random();  // Use time as default seed
        
	// IP: added
	this.rtpSession = rtpSession;
        
	// Start with a random sequence number
	sequence_number = (long) ( Math.abs(rnd.nextInt()) & 0x000000FF);
	timestamp = rtpSession.currentTime() + rtpSession.RANDOM_OFFSET;
        
        
        rtpSession.outprintln("RTP Session SSRC: " + 
			      Long.toHexString(rtpSession.ssrc) );
        rtpSession.outprintln(" Starting Seq: " + sequence_number );
        
        // IP: Added following line
        thisThread = new StateThread(this);
    }
    
    /**
     * Opens a socket for reception of RTP data
     *
     * @param multicastPort The port number for multicast reception
     */
    public void openReceiveSocket(int multicastPort) {
        if (isReceiveSocketOpened())
            return ;
        
        m_mcastPort = multicastPort;
        
        // If send and receive uses the same port do NOT open a new socket
        if (isTransmitSocketOpened() && (m_sendPort == m_mcastPort)) {
            m_sockReceive = m_sockSend;
        }
	else {
            try {
                m_sockReceive = new MulticastSocket( m_mcastPort );
                //s.setTimeToLive(128);
            } catch (IOException e) {
		logger.severe("Cannot open receive socket! " +
			      "Exception raised:\n" +
			      e.getMessage() + "\n" + 
			      e.getStackTrace().toString());
            }
        }
    }
    
    /**
     * Indicates whether the reception socket is opened.
     *
     * @return True if the reception socket is opened
     */
    public boolean isReceiveSocketOpened() {
        if (m_sockReceive != null) {
            if (m_sockReceive.isClosed())
                return false;
            else
                return true;
        }
        else
            return false;
    }


    /*
     * Change by Andreas Piirimets 2004-02-16
     *
     * Also added opportunity to specify a separate local port
     *     
     */
    /**
     * Opens a socket for transmission. The local port is set to the same value
     * as the remote port.
     *
     * @param remotePort The remote RTP port
     */
    public void openTransmitSocket(int remotePort) {
	openTransmitSocket(remotePort, remotePort);
    }

    /**
     * Opens a socket for transmission.
     *
     * @param localPort The local RTP port to send data from
     * @param remotePort The remote RTP port to send data to
     */
    public void openTransmitSocket(int localPort, int remotePort)
    {
        if (isTransmitSocketOpened())
            return;
        
        m_sendPort = remotePort;
        
        // If send and receive uses the same port do NOT open a new socket
        if (isReceiveSocketOpened() && (m_sendPort == m_mcastPort)) {
            m_sockSend = m_sockReceive;
        }
	else {
            //Initialize a Multicast Sender Port to send RTP Packets
            rtpSession.outprintln ( "Openning local port " + 
				    localPort + " for sending RTP..");

            try
            {
                // m_sockSend = new MulticastSocket ( m_sendPort );
                m_sockSend = new DatagramSocket( localPort );
            }
	    catch ( SocketException e) {
		System.err.println ( "RTPThreadHandler: " + e );
	    }
	    catch ( java.io.IOException e ) {
		System.err.println (e);
	    }
            rtpSession.outprintln ( "Successfully openned local port " + 
				    localPort );

        }
    }
    
    /**
     * Indicates whether the transmission socket is opened
     *
     * @return True if the transmission socket is opened
     */
    public boolean isTransmitSocketOpened() {
        if (m_sockSend != null) {
            if (m_sockSend.isClosed())
                return false;
            else
                return true;
        }
        else
            return false;
    }
    
    /**
     * Starts the thread
     *
     */    
    public void start() {
        thisThread.start();
    }
    
    
    /**
     * Constructs a datagram, assembles it into an RTP packet and sends it out.
     *
     * @param packet RTP packet to be sent out
     *
     * @return 0 if an error occured, 1 if everything is ok.
     */
    //  IP: Altered method and input parameter
    public int sendPacket( RTPPacket packet ) {
        
        if (m_sockSend == null)
            // IP: Should be 1 here but the return OK code seems to be 1 so
	    //     go with 0 instead
            return 0;
        
        if (m_sockSend.isClosed())
            return 0;
        
        m_PT[0]    = (byte) ((0x0 << 8) | ( rtpSession.getSendPayloadType() ));
        // +-+-+-+-+-+-+-+-+
        // |M|     PT      |
        // +-+-+-+-+-+-+-+-+
        //  0 1 0 1 1 0 0 0
        
        if (packet.getTimeStamp() != 0)
            timestamp = packet.getTimeStamp();
        else
            timestamp = rtpSession.currentTime() + rtpSession.RANDOM_OFFSET;
        
        byte[] ts = new byte[4];         // timestamp is 4 bytes
        //ts = PacketUtils.LongToBytes( timestamp, 4 );
        ts = PacketUtils.longToBytes(0, 4 );
        
        byte[] seq = new byte[2];       // sequence is 2 bytes
        if (packet.getSequenceNumber() != 0)
            seq = PacketUtils.longToBytes( packet.getSequenceNumber(), 2 );
        else
            seq = PacketUtils.longToBytes( sequence_number, 2 );
        
        byte[] ss = new byte[4];
        //ss = PacketUtils.LongToBytes( rtpSession.SSRC, 4 );
        ss = PacketUtils.longToBytes(0, 4 );
        
        ////////////////////////////////////////////////////////
        // Construct the header by appending all the above byte
        // arrays into RTPPacket
        ////////////////////////////////////////////////////////
        byte[] rtpPacket = new byte [0];
        // Append the compound version, Padding, Extension and CSRC Count bits
        rtpPacket  = PacketUtils.append( rtpPacket, vpxcc );
        
        // Append the compound Marker and payload type byte
        rtpPacket  = PacketUtils.append( rtpPacket,  m_PT );
        
        // Append the sequence number
        rtpPacket  = PacketUtils.append( rtpPacket, seq );
        
        // Append the 4 timestamp bytes
        rtpPacket  = PacketUtils.append( rtpPacket, ts );
        
        
        // Append the 4 SSRC bytes
        rtpPacket  = PacketUtils.append( rtpPacket, ss );
        
        // Append the data packet after 12 byte header
        rtpPacket  = PacketUtils.append( rtpPacket, packet.getPayloadData() );
        
        sequence_number++;
        
        // Create a datagram packet from the RTP byte packet, set ttl and send
        // IP: Changed destination from m_mcastPort to m_sendPort for pkt
        DatagramPacket pkt = 
	    new DatagramPacket( rtpPacket, rtpPacket.length, 
				m_InetAddress, m_sendPort );


        try {
	    // IP: Replaced following line
	    // m_sockSend.send( pkt, (byte) 5 ); 
	    // TODO: Change Hardcoded TTL - WA

	    //EZ 041114: Simulates packet loss.
	    /*
	      if(packetLossEnabled) {
		if(Math.random()>=(lossRatio*0.01))
		    m_sockSend.send(pkt);
		else
		    System.out.println("Dropped packet.");
	    }
	    else
	    */

	    m_sockSend.send(pkt);
	                
            //Update own status to Active Sender
            Source s1 = rtpSession.getMySource();
            s1.activeSender = true;
	    rtpSession.tc = rtpSession.currentTime();
            rtpSession.timeOfLastRTPSent = rtpSession.currentTime();
            rtpSession.packetCount++;
            rtpSession.octetCount += packet.getPayloadData().length;
            
        }
	catch ( java.io.IOException e ) {
            System.err.println(e);
            System.exit(1);
        }
        
        return 1;
        
    }
    
    /**
     * Starts receiving
     *
     */
    public void run() {
        startRTPReceiver();
    }
    
    /**
     * Starts the RTP Receiver. This method instantiates a multicast socket
     * on the multicast address and port specified and listens for packets on
     * that multicast group. When it receives a packet, it parses the packet
     * out and updates several session and source level statistics. It also
     * posts an event to the Application about the reception of an RTP Packet
     *
     */
    private void startRTPReceiver() {

        // Do some initial checks
        if (thisThread == null)
            return;
        
        if (m_sockReceive == null)
            return;
        
        if (m_sockReceive.isClosed())
            return;
        
        rtpSession.outprintln("RTP Thread started ");
        rtpSession.outprintln("RTP Group: " + m_InetAddress + "/" + 
			      m_mcastPort);
        
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket( buf, buf.length );
        int receivePayloadType;
        
        receivePayloadType = rtpSession.getReceivePayloadType();
        
        try {
            
            // IP: Moved following line own method instead
            // MulticastSocket m_sockReceive = 
	    // new MulticastSocket ( m_mcastPort );
            
            //m_sockReceive.joinGroup ( m_InetAddress );
            
            while (thisThread.checkState() != StateThread.STOP) {
                m_sockReceive.receive( packet );

                if ( validateRTPPacketHeader( packet.getData() ) ) {
                    long ssrc = 0;
                    int timeStamp = 0;
                    short seqNo = 0;
                    byte pt = 0;
                    
                    pt = (byte) ((buf[1] & 0xff) & 0x7f);
                    seqNo =(short)((buf[2] << 8) | ( buf[3] & 0xff)) ;
                    timeStamp = (((buf[4] & 0xff) << 24) | 
				 ((buf[5] & 0xff) << 16) | 
				 ((buf[6] & 0xff) << 8) | 
				 (buf[7] & 0xff)) ;

                    ssrc = (((buf[8] & 0xff) << 24) | 
			    ((buf[9] & 0xff) << 16) | 
			    ((buf[10] & 0xff) << 8) | 
			    (buf[11] & 0xff));
                    
                    rtpSession.outprintln("RTP (");
                    rtpSession.outprintln("ssrc=0x" + 
					  Long.toHexString(ssrc) + 
					  "\tts=" + timeStamp + 
					  "\tseq=" + seqNo + 
					  "\tpt=" + pt );
                    rtpSession.outprintln(")");
                    
                    // Create a RTPPacket and post it with Session.
                    // If there are any interested actionListeners, they will
		    // get it.
                    RTPPacket rtppkt = new RTPPacket();
                    rtppkt.setCsrcCount(0);
                    rtppkt.setSequenceNumber(seqNo);
                    rtppkt.setTimeStamp(timeStamp);
                    rtppkt.setSsrc(ssrc);
                    
                    // the payload is after the fixed 12 byte header
                    byte[] payload = 
			new byte [ packet.getLength() - 
				 RTP_PACKET_HEADER_LENGTH ];
                    
                    for ( int i=0; i < payload.length; i++ )
                        payload [i] = buf [ i+RTP_PACKET_HEADER_LENGTH ];
                    
                    rtppkt.setPayloadData(payload);
                    if (rtpSession.enableLoopBack) {
                        rtpSession.postAction( rtppkt );
                    }
                    else {
                        if (ssrc != rtpSession.ssrc) {
                            rtpSession.postAction( rtppkt );
                        }
                    }
                    
                    // Get the source corresponding to this SSRC
                    Source rtpSource =  rtpSession.getSource(ssrc);
                    
                    //Set teh Active Sender Property to true
                    rtpSource.activeSender = true;
                    
                    //Set the time of last RTP Arrival
                    rtpSource.timeOfLastRTPArrival = 
			rtpSession.tc = rtpSession.currentTime() ;
                    
                    //Update the sequence number
                    rtpSource.updateSeq(seqNo);
                    
                    // if this is the first RTP Packet Received from this 
		    // source then store the seq no. as its base
                    if (rtpSource.noOfRTPPacketsRcvd == 0) {
                        rtpSource.base_seq = seqNo;
		    }
                    
                    // Increment the total number of RTP Packets Received
                    rtpSource.noOfRTPPacketsRcvd++;
                }
                else {
                    System.err.println
			("RTP Receiver: Bad RTP Packet received");
                    System.err.println
			("From : " + packet.getAddress() + "/" + 
			 packet.getPort() + "\n" + "Length : " + 
			 packet.getLength()
                    );
                }
            }

            // IP: Added line
            thisThread = null;
            
            //m_sockReceive.leaveGroup( m_InetAddress );
            //m_sockReceive.close();
        }
        catch ( SocketException se) {
            System.err.println("RTPThreadHandler: " +  se );
        }
	catch ( java.io.IOException e ) {
            rtpSession.outprintln(" IO exception" );
        }
    }
    
    // IP: Added method
    /**
     * Closes all sockets
     *
     */
    public void close() {
        stop();
        if (m_sockSend != null) {
            if (!m_sockSend.isClosed())
                m_sockSend.close();
            m_sockSend = null;
        }
        if (m_sockReceive != null) {
            if (!m_sockReceive.isClosed())
                m_sockReceive.close();
            m_sockReceive = null;
        }
    }
    
    // IP: Added method
    /**
     * Stops the thread
     *
     */
    private void stop() {
        if (thisThread != null) {
            thisThread.setState(StateThread.STOP);
            thisThread.interrupt();
        }
    }
    
    /**
     * Validates RTP Packet.
     * Returns true or false corresponding to the test results.
     *
     * @param   packet The RTP Packet to be validated.
     *
     * @return  True if validation was successful, False otherwise.
     */
    public boolean validateRTPPacketHeader(byte[] packet) {
        boolean versionValid = false;
        boolean payloadTypeValid = false;
        
        // +-+-+-+-+-+-+-+-+
        // |V=2|P|X|   CC  |
        // +-+-+-+-+-+-+-+-+
        
        // Version MUST be 2
        if ( ( (packet[0] & 0xC0) >> 6 ) == 2 )
            versionValid = true;
        else
            versionValid = false;
        
        // +-+-+-+-+-+-+-+-+
        // |M|     PT      |
        // +-+-+-+-+-+-+-+-+
        //  0 1 0 1 1 0 0 0
        
        // Payload Type must be the same as the session's
        if ( ( packet [1] & 0x7F ) == rtpSession.getReceivePayloadType() )
            payloadTypeValid = true;
        else
            payloadTypeValid = false;
        
        return ( versionValid && payloadTypeValid );
        
    }
    // IP: Removed send test package method
}

