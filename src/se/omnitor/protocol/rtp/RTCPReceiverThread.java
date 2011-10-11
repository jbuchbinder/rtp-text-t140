/*
 * Copyright (C) 2004-2008  University of Wisconsin-Madison and Omnitor AB
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

import se.omnitor.protocol.rtp.packets.ReportBlock;
import se.omnitor.protocol.rtp.packets.RTCPBYEPacket;
import se.omnitor.protocol.rtp.packets.RTCPSDESPacket;
import se.omnitor.protocol.rtp.packets.RTCPReceiverReportPacket;
import se.omnitor.protocol.rtp.packets.RTCPSenderReportPacket;
import se.omnitor.protocol.rtp.packets.SDESItem;
import se.omnitor.protocol.rtp.packets.SenderInfo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;


/**
 * This class encapsulates the functionality to receive and parse out RTCP
 * packets. This class provides a seperate thread to receive these RTCP
 * packets. Depending on the kind of RTCP packet received (e.g. SR, RR, SDES,
 * BYE) the receiver parses them out correctly and updates several session and
 * source level statistics. Each packet having been parsed posts an event to
 * the application corresponding to the packet that the receiver received.
 *
 * @author Unknown
 */
// IP: Removed all static objects and added private object rtpSession
public class RTCPReceiverThread implements Runnable {
    /**
     *   Receiver Port for RTCP Packets
     */
    private int  m_port;

    // IP: Added to get rid of all static types
    private Session rtpSession;

    /**
     *   Multicast Address for RTCP Packets
     */
    private InetAddress m_InetAddress;

    private StateThread thisThread;

    private DatagramSocket socket;

    private boolean symmetric;

    /**
     * Constructor for the class. Takes in a TCP/IP Address and a port number
     *
     * @param multicastGroupIPAddress Dotted representation of the Multicast
     * address.
     * @param rtcpGroupPort Port for Multicast group (for receiving RTP
     * Packets).
     * @param rtpSession The session to use
     */
    public RTCPReceiverThread(InetAddress multicastGroupIPAddress,
			      int rtcpGroupPort, Session rtpSession) {

	m_InetAddress = multicastGroupIPAddress;
	m_port = rtcpGroupPort;
	this.rtpSession = rtpSession;
	symmetric=false;
	thisThread = new StateThread(this, "RTCP Received Thread");
    }

    public RTCPReceiverThread(InetAddress multicastGroupIPAddress,
			      int rtcpGroupPort, Session rtpSession,
			      DatagramSocket socket) {
	m_InetAddress = multicastGroupIPAddress;
	m_port = rtcpGroupPort;
	this.rtpSession = rtpSession;
	this.socket=socket;
	symmetric=true;
	thisThread = new StateThread(this, "RTCP Receiver Thread");
    }

    /**
     * Starts the receiver thread
     *
     */
    public void start() {
	thisThread.start();
    }

    /**
     * Stops the receiver thread, closes the socket
     *
     */
    public void stop() {
	if (thisThread != null) {
	    thisThread.setState(StateThread.STOP);
	    thisThread.interrupt();
	}

	if (socket != null) {
	    try {
		Thread.sleep(2000);
	    }
	    catch (InterruptedException e) {
		// Ignore any interruption
	    }
		socket.close();
	}
    }

    /**
     *   Starts the RTCPReceiver Thread
     *
     */
    public void run() {
	startRTCPReceiver();
    }

    /**
     * Starts the RTCP Receiver Thread. The thread creates a new multicast
     * socket according to the Multicast address and port provided in the
     * constructor. Then the thread waits in idle state for the reception
     * of a RTCP Packet. As soon as a RTCP Packet is received, the receiver
     * first validates the RTCP Packet accoring to the following rules: <br>
     * <br>
     * RTCP Header Validity Checks <br>
     * <br>
     *	  1) Version should always =2 <br>
     *	  2) The payload type field of the first RTCP Packet should be SR
     *       or RR <br>
     *	  3) The Padding Bit (P) should be zero for the first packet <br>
     *	  4) The length fields of individual RTCP Packets must total to the
     *	     overall length of the compound RTCP Packet <br>
     * <br>
     * After the packet has been validated, it is parsed differently
     * according to the type of packet (e.g. Sender Report, Receive Report,
     * BYE, SDES) etc. For each packet parsed, session and source level
     * statistics are updated and an event is posted for the application
     * corresponding to the type of packet that was parsed.
     *
     */
    public void startRTCPReceiver() {

	if (thisThread == null)
	    return;

	rtpSession.outprintln ("RTCP Receiver Thread started ");
	rtpSession.outprintln ("RTCP Group: " + m_InetAddress + ":" + m_port);

	try {
	    // IP: Changed
	    //MulticastSocket socket = new MulticastSocket ( m_port );
	    //socket.joinGroup ( m_InetAddress );
	    if(!symmetric) {
		socket = new DatagramSocket(m_port);
	    }

	    byte[] packet = new byte[1024];
	    DatagramPacket header =
		new DatagramPacket( packet, packet.length );
	    int i= 0;

	    while (thisThread.checkState() != StateThread.STOP) {
		// Preliminary Information - Time Pkt Received, Length of
		// the Packet, hostname , IP address :: portnumber
		rtpSession.outprint ("\n");

		rtpSession.outprint
		    (Long.toString((new Date()).getTime()) +
		     "   " + "RTCP" + "  ");

		try {
		    socket.receive(header);
		}catch (java.net.SocketTimeoutException ste) {
		    //It's ok to timeout
		}catch (SocketException e) {
                    // It's OK to have socket exception when socket is closed.
		    //System.err.println("RTCPReceiver recieve error:"+e);
                    //e.printStackTrace();
		}catch (Exception e) {
			// Sometimes an interruptedException comes here due to Hangup
		}

		rtpSession.outprint("Len " + header.getLength() +  "  " +
				    "from " + header.getAddress()+ ":" +
				    header.getPort() + "\n");

		/*******************************************************
		 * RTCP Header Validity Checks
		 * 1) Version should always =2
		 * 2) The payload type field of the first RTCP Packet
		 *    should be SR or RR
		 * 3) The Padding Bit (P) should be zero for the first
		 *    packet
		 * 4) The length fields of individual RTCP Packets must
		 *    total to the overall length of the compound RTCP
		 *    Packet
		 *******************************************************/

		// RTCP Header Validity Check 2
		int payloadType_1 = (int) (packet[1] & 0xff);
		if  ((payloadType_1 !=  RTCPConstants.RTCP_SR) &&
		     (payloadType_1 !=   RTCPConstants.RTCP_RR ) ) {

		    rtpSession.outprint("RTCP Header Check Fail : " +
					"First Payload type not a SR " +
					"or RR\n");
		}

		//RTCP Header Validity Check 3

		if ((((packet[0] & 0xff) >> 5) & 0x01) != 0) {
		    rtpSession.outprint("RTCP Header Check Fail : " +
					"First Padding bit not " +
					" zero\n");
		}

		// Since a RTCP Packet may contain many types of
		// RTCP Packets. Keep parsing the packets until the
		// no. of bytes parsed  = total no of bytes read
		int bytesRead = 0;
		int totalBytesRead =0;

		int hLength = header.getLength();

		while (totalBytesRead < hLength) {
		    // RTCP Header Validity Check 1

		    byte version =
			(byte)((packet[bytesRead] & 0xff) >> 6);

		    if ( version != 0x02) {
			rtpSession.outprint("RTCP Header Check " +
					    "Fail : Wrong Version\n");
		    }

		    // Check the length of this particular packet
		    short length =
			(short)((packet[bytesRead+2] << 8) |
				(packet [bytesRead +3] & 0xff));


		    // Check the Payload type of this packet
		    int pt =  ((packet[bytesRead+1] & 0xff) );

		    if (pt == RTCPConstants.RTCP_SR) {
			// Create an RTCP SR Packet and post it for
			// interested listeners
			RTCPSenderReportPacket rtcpSRPacket =
			    new RTCPSenderReportPacket();

			SenderInfo senderInfo = new SenderInfo();

			// Check if there are any reception reports
			byte rc =
			    (byte)((packet[bytesRead] & 0xff) & 0x1f);

			rtpSession.outprint("RC" + rc +"\n");

			long ssrc =
			    (((packet[bytesRead + 4] & 0xff) << 24) |
			     ((packet[bytesRead+5] & 0xff) << 16) |
			     ((packet[bytesRead +6] & 0xff) << 8) |
			     (packet[bytesRead +7] & 0xff));

			//Update the Sender SSRC for the SR Packet
			rtcpSRPacket.setSenderSsrc(ssrc);

			// Increment the bytes
			bytesRead +=8;

			rtpSession.outprint("(SR ssrc=0x" +
					    Long.toHexString(ssrc) +
					    "    count= " + rc +
					    "   len =  " + length +
					    "\n");

			// Get the source from the Session
			// corresponding to this particular SSRC
			Source sender_Source =
			    rtpSession.getSource(ssrc);

			if (sender_Source != null) {
			    // Update all the source parameters and
			    // statistics
			    sender_Source.timeOfLastRTCPArrival =
				rtpSession.currentTime();

			}

			if (length!=1) { // Not an empty packet

			    long ntp_sec =
				(((packet[bytesRead] & 0xff)<<24) |
				 ((packet[bytesRead +1] & 0xff)<<16) |
				 ((packet[bytesRead+2] & 0xff) << 8) |
				 (packet[bytesRead+3] & 0xff) );

			    long ntp_frac =
				(((packet[bytesRead+4] & 0xff)<<24) |
				 ((packet[bytesRead+5] & 0xff)<<16) |
				 ((packet[bytesRead+6] & 0xff) << 8) |
				 (packet[bytesRead+7] & 0xff));

			    long rtp_ts =
				(((packet[bytesRead+8] & 0xff)<<24) |
				 ((packet[bytesRead+9] & 0xff)<<16) |
				 ((packet[bytesRead+10] & 0xff)<<8) |
				 (packet[bytesRead+11] & 0xff));

			    long psent =
				(((packet[bytesRead+12] & 0xff)<<24) |
				 ((packet[bytesRead+13] & 0xff)<<16) |
				 ((packet[bytesRead+14] & 0xff)<<8) |
				 (packet[bytesRead+15] & 0xff));

			    long osent =
				(((packet[bytesRead+16] & 0xff)<<24) |
				 ((packet[bytesRead+17] & 0xff)<<16) |
				 ((packet[bytesRead+18] & 0xff)<<8) |
				 (packet[bytesRead+19] & 0xff));

			    // Set the lst - middle 32 bits out of
			    // NTPTimeStamp
			    sender_Source.lst =
				((packet[bytesRead+6] & 0xff) << 24) |
				((packet[bytesRead+7] & 0xff)<<16)|
				(((packet[bytesRead+8] & 0xff) << 8) |
				 (packet[bytesRead+9] & 0xff) ) ;

			    // Set the SenderInfo part of the SR
			    // Packet to be thrown out
			    senderInfo.setSenderOctetCount(osent);

			    //Set the arrival time of this SR report
			    sender_Source.timeofLastSRRcvd =
				sender_Source.timeOfLastRTCPArrival;
			    senderInfo.setSenderPacketCount(psent);
			    senderInfo.setRtpTimeStamp(rtp_ts);
			    senderInfo.setNtpTimeStampLeastSignificant
				(ntp_frac);
			    senderInfo.setNtpTimeStampMostSignificant
				(ntp_sec);
			    rtcpSRPacket.setSenderInfo(senderInfo);

			    rtpSession.outprint("ntp = " +  ntp_sec +
						" " + ntp_frac +
						"   ts=   " +
						rtp_ts +
						"  psent =  " +
						psent + "  osent   " +
						osent + "\n" + ")" +
						"\n");

			    bytesRead +=20;
			}

			//Parse the reports
			for (int j=0; j<rc;j++) {

			    long rcvr_ssrc =
				(((packet[bytesRead] & 0xff) << 24) |
				 ((packet[bytesRead+1] & 0xff)<<16) |
				 ((packet[bytesRead+2] & 0xff)<<8) |
				 (packet[bytesRead +3 ] & 0xff));

			    double fractionLost =
				(packet[bytesRead+4] & 0xff);

			    long cumPktsLost =
				(((((packet[bytesRead+4] & 0xff)<<24)|
				   ((packet[bytesRead+5] & 0xff)<<16)|
				   ((packet[bytesRead+6] & 0xff)<<8)|
				   (packet[bytesRead+7] & 0xff)) ) &
				 0xffffff);

			    long extHighSqRcvd =
				(((packet[bytesRead + 8] & 0xff)<<24)|
				 ((packet[bytesRead + 9] & 0xff)<<16)|
				 ((packet[bytesRead+10] & 0xff)<<8)|
				 (packet[bytesRead+11] & 0xff));

			    long intJitter =
				(((packet[bytesRead+12] & 0xff)<<24)|
				 ((packet[bytesRead+13] & 0xff)<<16)|
				 ((packet[bytesRead+14] & 0xff)<<8)|
				 (packet[bytesRead+15] & 0xff));

			    long lastSR =
				(((packet[bytesRead+16] & 0xff)<<24)|
				 ((packet[bytesRead+17] & 0xff)<<16)|
				 ((packet[bytesRead+18] & 0xff)<<8) |
				 (packet[bytesRead+19] & 0xff));

			    long delay_LastSR =
				(((packet[bytesRead+20] & 0xff)<<24)|
				 ((packet[bytesRead+21] & 0xff)<<16)|
				 ((packet[bytesRead+22] & 0xff)<<8) |
				 (packet[bytesRead+23] & 0xff));

			    // Update the statistics -  only if the
			    // rcvr_ssrc matches your own ssrc

			    Source reception_Source =
				rtpSession.getMySource();

			    // Check if sender report contains
			    // information about this particular source
			    if (rcvr_ssrc == reception_Source.ssrc) {

				// Create a new report block and set
				// its attributes
				rtcpSRPacket.doesContainReportBlock(true);

				ReportBlock reportblock =
				    new ReportBlock();
				reportblock.setFractionLost(fractionLost);
				reportblock.setCumulativeNumberOfPacketsLost
				    (cumPktsLost);
				reportblock.
				    setExtendedHighestSequenceNumberReceived
				    (extHighSqRcvd);
				reportblock.setInterarrivalJitter
				    (intJitter);
				reportblock.setLastSr(lastSR);
				reportblock.setDelayLastSr(delay_LastSR);

				// Set the Sender Report Packet's
				// Report Block to this Report Block
				rtcpSRPacket.setReportBlock(reportblock);

			    }

			    //Print the statistics
			    rtpSession.outprint
				("(ssrc=0x" +
				 Long.toHexString(rcvr_ssrc) +
				 "    fraction =  " + fractionLost +
				 "     lost =  " + cumPktsLost +
				 "     last_seq =  " + extHighSqRcvd +
				 "   jit  =   " + intJitter +
				 "  lsr =  " + lastSR +
				 "    dlsr = " + delay_LastSR +
				 "\n");

			    bytesRead += 24;

			}

			// Update Average RTCP Packet size
			rtpSession.avg_rtcp_size =
			    1/16*(length*4 + 1 ) +
			    15/16*(rtpSession.avg_rtcp_size);

			if (ssrc != rtpSession.ssrc) {

			    // Post the SR Packet only if its not the
			    // same packet sent out by this source
			    rtpSession.postAction (rtcpSRPacket);
			}

		    }

		    if (pt == RTCPConstants.RTCP_RR) {

			// Create an RTCP RR Packet and post it for
			// interested listeners
			RTCPReceiverReportPacket rtcpRRPacket =
			    new RTCPReceiverReportPacket();

			// Check if there are any reception reports
			byte rc =
			    (byte)((packet[bytesRead] & 0xff) & 0x1f);

			long ssrc =
			    (((packet[bytesRead+4] & 0xff) << 24) |
			     ((packet[bytesRead+5] & 0xff) << 16) |
			     ((packet[bytesRead+6] & 0xff) << 8) |
			     (packet[bytesRead+7] & 0xff));

			rtpSession.outprint
			    ("( RR ssrc=0x" +
			     Long.toHexString(ssrc) + "    count= " +
			     rc +"   len =  " + length  + "\n" + ")" +
			     "\n");

			// Get the source from the Session
			// corresponding to this particular SSRC
			Source sender_Source =
			    rtpSession.getSource(ssrc);

			//Set the Sender SSRC of the RR Packet
			rtcpRRPacket.setSenderSsrc(ssrc);

			if (sender_Source != null) {

			    // Update all the source parameters and
			    // statistics
			    sender_Source.timeOfLastRTCPArrival =
				rtpSession.currentTime();
			}

			// Increment the Bytes read by the length
			// of this packet
			bytesRead +=8;

			// Parse the reports
			for (int j=0; j<rc;j++) {

			    long rcvr_ssrc =
				(((packet[bytesRead] & 0xff) << 24) |
				 ((packet[bytesRead+1] & 0xff)<<16) |
				 ((packet[bytesRead+2] & 0xff) << 8) |
				 (packet[bytesRead +3 ] & 0xff));

			    byte fractionLost =
				(byte)(packet[bytesRead+4] &  0xff);

			    long cumPktsLost =
				(((((packet[bytesRead+4] & 0xff)<<24)|
				   ((packet[bytesRead+5] & 0xff)<<16)|
				   ((packet[bytesRead+6] & 0xff)<<8)|
				   (packet[bytesRead+7] & 0xff))) &
				 0xffffff);

			    long extHighSqRcvd =
				(((packet[bytesRead + 8]&0xff)<<24)|
				 ((packet[bytesRead + 9]&0xff)<<16)|
				 ((packet[bytesRead+10] & 0xff)<<8) |
				 (packet[bytesRead+11] & 0xff));

			    long intJitter =
				(((packet[bytesRead+12] & 0xff)<<24) |
				 ((packet[bytesRead+13] & 0xff)<<16) |
				 ((packet[bytesRead+14] & 0xff)<<8) |
				 (packet[bytesRead+15] & 0xff));

			    long lastSR =
				(((packet[bytesRead+16] & 0xff)<<24) |
				 ((packet[bytesRead+17] & 0xff)<<16) |
				 ((packet[bytesRead+18] & 0xff)<<8) |
				 (packet[bytesRead+19] & 0xff));

			    long delay_LastSR =
				(((packet[bytesRead+20] & 0xff)<<24) |
				 ((packet[bytesRead+21] & 0xff)<<16) |
				 ((packet[bytesRead+22] & 0xff)<<8) |
				 (packet[bytesRead+23] & 0xff));

			    // Print the statistics
			    rtpSession.outprint
				("(ssrc=0x" +
				 Long.toHexString(rcvr_ssrc) +
				 "    fraction =  " + fractionLost +
				 "     lost =  " + cumPktsLost +
				 "     last_seq =  " + extHighSqRcvd +
				     "   jit  =   " + intJitter +
				 "  lsr =  " + lastSR +
				 "    dlsr = " + delay_LastSR + "\n");

			    bytesRead += 24;

			    // Update the statistics - only if the
			    // rcvr_ssrc matches your own ssrc

			    Source reception_Source =
				rtpSession.getMySource();

			    if (rcvr_ssrc == reception_Source.ssrc) {
				// Update all the source parameters
				// and statistics

				// Create a new report block and set
				// its attributes

				rtcpRRPacket.doesContainReportBlock(true);

				ReportBlock reportblock =
				    new ReportBlock();
				reportblock.setFractionLost(fractionLost);
				reportblock.setCumulativeNumberOfPacketsLost
				    (cumPktsLost);
				reportblock.
				    setExtendedHighestSequenceNumberReceived
				    (extHighSqRcvd);
				reportblock.setInterarrivalJitter(intJitter);
				reportblock.setLastSr(lastSR);
				reportblock.setDelayLastSr(delay_LastSR);

				// Set the Receiver Report Packet's
				// Report Block to this Report Block
				rtcpRRPacket.setReportBlock(reportblock);

			    }

			    // Update Average RTCP Packet size
			    rtpSession.avg_rtcp_size =
				1/16*(length*4 + 1 ) +
				15/16*(rtpSession.avg_rtcp_size);

			    if (ssrc != rtpSession.ssrc) {

				// Post the RR Packet only if its not
				// the same packet sent out by this
				// source
				rtpSession.postAction(rtcpRRPacket);
			    }

			}

			// Added by Andreas Piirimets 2004-02-26
			// Also send an empty report if no reports
			// were available in the packet
			if (rc == 0) {
			    if (ssrc != rtpSession.ssrc) {
				// Post the RR Packet only if its not
				// the same packet sent out by this
				// source
				rtpSession.postAction(rtcpRRPacket);
			    }
			}

		    }

		    if (pt ==RTCPConstants.RTCP_SDES) {
			int len = (length+1) * 4 ;
			byte sc =
			    (byte)((packet[bytesRead] & 0xff) & 0x1f);
			bytesRead += 4;
			len -= 4 ;
			// Keep track of no. of bytes read from this
			// package with 'len'

			//Parse the packet for all sources
			for (int j=0; j<sc;j++) {

			    // Read in the SSRC of the source
			    long ssrc =
				(((packet[bytesRead] & 0xff) << 24) |
				 ((packet[bytesRead+1] & 0xff)<<16) |
				 ((packet[bytesRead+2] & 0xff) << 8) |
				 (packet[bytesRead+3] & 0xff));

			    rtpSession.outprint
				("(SDES ssrc=0x" +
				 Long.toHexString(ssrc) +
				 "    count= " + sc +
				 "   len =  " + length + "\n" + ")" +
				 "\n");

			    // Increment the Bytes Read
			    bytesRead += 4;
			    len -= 4;

			    // Note that we don't know how many
			    // items, so have to check if the byte is
			    // null or not
			    //while (((byte)(packet[BytesRead] &
			    //0xff)) != 0x00)

			    while ((((byte)(packet[bytesRead] & 0xff))
				    != 0x00) && (len > 0)) {

				byte name =
				    (byte)(packet[bytesRead] & 0xff);

				String itemType = "";

				if (name ==
				    RTCPConstants.RTCP_SDES_END) {

				    itemType = "BYE";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_CNAME) {

				    itemType = "CNAME";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_NAME) {

				    itemType = "NAME";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_EMAIL) {

				    itemType = "EMAIL";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_PHONE) {

				    itemType = "PHONE";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_LOC) {

				    itemType = "LOC";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_TOOL) {

				    itemType = "TOOL";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_NOTE) {

				    itemType = "NOTE";
				}

				if (name ==
				    RTCPConstants.RTCP_SDES_PRIV) {

				    itemType = "PRIV";
				}

				byte fieldlength =
				    (byte)(packet[bytesRead+1] &0xff);

				bytesRead += 2;
				len-=2;

				String text = "";

				for (j = 0 ; j<fieldlength ;j++) {

				    char character =
					(char)((packet[bytesRead+j])&
					       0xff);

				    text += character;

				}

				bytesRead += fieldlength;
				len -= fieldlength;

				rtpSession.outprint
				    (itemType + "=" + "\"" + text +
				     "\"  ");

				// Create an RTCP SDES Packet and
				// post it for interested listeners

				RTCPSDESPacket rtcpSdesPkt =
				    new RTCPSDESPacket();

				SDESItem sdesItem =
				    new SDESItem();

				sdesItem.setType(name);
				sdesItem.setValue(text);

				rtcpSdesPkt.setSdesItem(sdesItem);

				// Post Action if the packet was not
				// generated by this source
				if (ssrc != rtpSession.ssrc) {

				    rtpSession.postAction(rtcpSdesPkt);
				}
			    }

			    // Check for null bytes and increment the
			    // count - in each chunk the item list is
			    // terminated by null octets to the next
			    // 32 bit word boundary

			    while ((((byte)(packet[bytesRead]&0xff))
				    == 0x00) &&
				   (len>0) ) {

				bytesRead++;
				len--;
			    }
			}

			// Update Average RTCP Packet size
			rtpSession.avg_rtcp_size =
			    1/16*(length*4 + 1 ) +
			    15/16*(rtpSession.avg_rtcp_size);
		    }

		    if (pt== RTCPConstants.RTCP_BYE) {

			byte sc =
			    (byte)((packet[bytesRead] & 0xff) & 0x1f);
			bytesRead += 4;
			rtpSession.outprint
			    ("(BYE" +"    count= " + sc +
			     "   len =  " + length + "\n" + ")" +
			     "\n");

			// Construct a BYE Packet Array
			RTCPBYEPacket[] rtcpBYEPacketArray =
			    new RTCPBYEPacket[sc];

			for (i=0; i<sc; i++) {

			    // For each source get the SSRC
			    long ssrc =
				(((packet[bytesRead ] & 0xff)<<24) |
				 ((packet[bytesRead+1] & 0xff)<<16) |
				 ((packet[bytesRead +2] & 0xff)<<8) |
				 (packet[bytesRead +3] & 0xff));

			    if (rtpSession.isByeRequested == false) {

				// Ask the Session to remove the
				// source object corresponding to that
				// SSRC

				rtpSession.removeSource(ssrc);

			    }
			    else if (rtpSession.isByeRequested==true) {

				// If a BYE has been requested by
				// this particular member and it
				// receives a BYE from some other
				// source , then add that to the list
				// of members - NOTE: This is true for
				// only BYE Packets not any other RTCP
				// or RTP Packets

				rtpSession.getSource(ssrc);

			    }

			    // To make the transmission rate of RTCP
			    // Packets more adaptive to changes in
			    // group membership, the "reverse
			    // reconsideration algorithm is
			    // implemented when a BYE packet is
			    // received.

			    rtpSession.tn =
				rtpSession.tc +
				(rtpSession.getNumberOfMembers() /
				 rtpSession.pmembers) *
				(rtpSession.tn - rtpSession.tc);

			    rtpSession.timeOfLastRTCPSent =
				rtpSession.tc -
				(rtpSession.getNumberOfMembers() /
				 rtpSession.pmembers) *
				(rtpSession.tc -
				 rtpSession.timeOfLastRTCPSent);

			    // Reschedule the next RTCP Packet for
			    // transmission at time tn which is now
			    // earlier

			    rtpSession.pmembers =
				rtpSession.getNumberOfMembers();

			    //Increment the bytes read by the length
			    // of this packet

			    bytesRead += 4;
			    rtpSession.outprint
				("ssrc=0x" + Long.toHexString(ssrc));

			    rtpSession.outprintln
				("In the Bye Packet " + i);

			    rtcpBYEPacketArray[i] =
				new RTCPBYEPacket();

			    rtcpBYEPacketArray[i].setSsrc(ssrc);

			}

			byte fieldlength =
			    (byte)(packet[bytesRead] &0xff);

			bytesRead ++;

			String text = "";

			for (int j=0 ; j < fieldlength ;j++) {

			    char character =
				(char) ((packet[bytesRead+j] ) & 0xff);

			    text += character;

			}

			bytesRead += fieldlength;

			rtpSession.outprint("len = " + fieldlength );

			rtpSession.outprint("Reasons for leaving=" +
					    "\"" + text + "\"  ");

			// Read through the null padding bytes and
			// update counters.
			int hLen = header.getLength();
			while ((((byte)(packet[bytesRead] & 0xff)) ==
				0x00) &&
			       bytesRead < hLen ) {

			    bytesRead++;
			}

			// Update Average RTCP Packet size
			rtpSession.avg_rtcp_size =
			    1/16*(length*4 + 1 ) +
			    15/16*(rtpSession.avg_rtcp_size);

			for (i=0; i< sc; i++) {

			    rtcpBYEPacketArray[i].setReasonForLeaving(text);

			    // Post the action i.e. generate an event
			    // if the packet was not generated from
			    // this source

			    if (rtcpBYEPacketArray[i].getSsrc() !=
				rtpSession.ssrc) {

				rtpSession.postAction
				    (rtcpBYEPacketArray[i]);
			    }
			}

		    }

		    if (pt == RTCPConstants.RTCP_APP) {

			// Increment the Bytes read by the
			// length of this packet
			bytesRead += 4*(length+1);

		    }
		    totalBytesRead = bytesRead;

		}

		// RTCP Header Validity Check 4
		rtpSession.outprintln ("TotalBytesRead: " +
				       totalBytesRead +
				       " Header.getLength" +
				       header.getLength() );

		if ( totalBytesRead != header.getLength()) {
		    rtpSession.outprintln
			("RTCP Header Check Fail : " +
			 "Bytes Read do not Match Total Packet " +
			 "Length\n");
		}

		i++;

		// Every time a RTCP Packet is received , update the
		// other users timeout i.e remove them from the
		// member or the sender lists if they have
		// not been active for a while
		rtpSession.updateSources();

	    }
	    // IP: Removed
	    /* try
	       {
	       socket.leaveGroup( m_InetAddress );
	       }

	       catch ( java.io.IOException e)
	       {
	       System.err.println(e);
	       }
	    */

	    thisThread = null;

	    socket.close();
	}
	//catch ( UnknownHostException e ) {
	//    System.err.println (e);
	//}
	catch ( java.io.IOException e ) {
	    System.err.println (e);
	}
    }
}

