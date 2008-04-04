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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Enumeration;

/**
 * This class encapsulates the functionality to construct and send out an
 * RTCP Packet. This class provides a seperate thread to send out RTCP
 * packets. The thread is put to sleep for a specified amount of time (as
 * calculated using various RTCP parameters and reception feedback). When the
 * thread moves out of the blocked (or sleep) state, it determines what kind
 * of a RTCP packets needs to be send out, constructs the appropriate RTCP
 * packets and sends them
 *
 * @author Unknown
 */
// IP: Removed static classes etc.
public class RTCPSenderThread extends Thread
{
    /**
     *   Sender Port for RTCP Packets
     */
    private int  m_SendFromPort;

    /**
     *   Sender Address for RTCP Packets
     */
    private InetAddress m_InetAddress;

    /**
     *   Multicast Socket for sending RTCP
     */
    // IP:
    // MulticastSocket m_RTCPSenderSocket;
    private DatagramSocket m_RTCPSenderSocket;

    /**
     *   Multicast Port for RTCP Packets
     */
    private int m_MulticastRTCPPort;

    /**
     *   Packet for RTCP Packets
     */
    private int packetcount;

    /**
     *   Flag used to determine when to terminate after sending a BYE
     */
    private boolean waitingForByeBackoff = false;

    // IP: Added this to get rid of all static types
    private Session rtpSession;

    private boolean symmetric;

    /**
     * Initialies the class. Takes care of the variables.
     *
     * @param multicastGroupIPAddress The multicast group IP address
     * @param rtcpSendFromPort The local RTCP port to send data from
     * @param rtcpGroupPort The multicast group port (for reception)
     * @param rtpSession The session to use
     */
    public RTCPSenderThread ( InetAddress multicastGroupIPAddress,
			      int rtcpSendFromPort, int rtcpGroupPort,
			      Session rtpSession)
    {
        // TODO: Perform sanity check on group address and port number
        m_InetAddress = multicastGroupIPAddress;
        m_MulticastRTCPPort = rtcpGroupPort;
        m_SendFromPort = rtcpSendFromPort;
        // IP: Added following
        this.rtpSession = rtpSession;
	symmetric=false;
    }

    public RTCPSenderThread ( InetAddress multicastGroupIPAddress,
			      int rtcpSendFromPort, int rtcpGroupPort,
			      Session rtpSession,
			      DatagramSocket socket)
    {
        // TODO: Perform sanity check on group address and port number
        m_InetAddress = multicastGroupIPAddress;
        m_MulticastRTCPPort = rtcpGroupPort;
        m_SendFromPort = rtcpSendFromPort;
        // IP: Added following
        this.rtpSession = rtpSession;
	m_RTCPSenderSocket=socket;
	symmetric=true;
    }

    /**
     * Starts the RTCPSender Thread
     *
     */
    public void run()
    {
        startRTCPSender();
    }


    /**
     * Initializes the thread by creating a multicast socket on a specified
     * address and a port. It manages the thread initialization and blocking
     * of the thread (i.e putting it to sleep for a specified amount of
     * time). This function also implements the BYE backoff algorithm with
     * Option B. The BYE Backoff Algorithm is used in order to avoid a flood
     * of BYE packets when many users leave the system. <br>
     * <br>
     * Note: if a client has never sent an RTP or RTCP Packet, it will not
     * send a BYE Packet when it leaves the group. <br>
     *
     */
    public void startRTCPSender()
    {

        rtpSession.outprintln ("RTCP Sender Thread started ");

        rtpSession.outprintln ("RTCP Group: " + m_InetAddress.toString() +
			       ":" + m_MulticastRTCPPort);

        rtpSession.outprintln ("RTCP Local port for sending: " +
			       m_SendFromPort );

        // Create a new socket and join group
	if(!symmetric) {
	    try
		{
		    // IP: Changed
		    // m_RTCPSenderSocket =
		    // new MulticastSocket ( m_SendFromPort );
		    m_RTCPSenderSocket = new DatagramSocket ( m_SendFromPort );

		}
	    /*catch ( UnknownHostException e )
	      {
	      rtpSession.outprintln("Unknown Host Exception");
	      }*/
	    catch ( java.io.IOException e )
		{
		    rtpSession.outprintln ("RTCPSenderThread: IOException");
		}
	}


        // flag terminates the endless while loop
        boolean terminate = false;

        while ( !terminate )
	    {
		// Update T and Td (Session level variables)
		rtpSession.calculateInterval();

		// If inturrepted during this sleep time, continue with
		// execution
		int sleepResult = sleepTillInterrupted( rtpSession.t);

		if ( sleepResult == 0 )
		    {
			// Sleep was interrupted, this only occurs if thread
			// was terminated to indicate a request to send a BYE
			// packet
			waitingForByeBackoff = true;
			rtpSession.isByeRequested = true;
		    }

		// See if it is the right time to send a RTCP packet or
		// reschedule {{A True}}
		if ( (rtpSession.timeOfLastRTCPSent + rtpSession.t) <=
		     rtpSession.currentTime() )
		    {
			// We know that it is time to send a RTCP packet, is
			// it a BYE packet {{B True}}
			if ( ( rtpSession.isByeRequested &&
			       waitingForByeBackoff ) )
			    {
				// If it is bye then did we ever sent anything
				// {{C True}}
				if ( rtpSession.timeOfLastRTCPSent > 0 &&
				     rtpSession.timeOfLastRTPSent > 0 )
				    {
					// ** BYE Backoff Algorithm **
					// Yes, we did send something, so we
					// need to send this RTCP BYE
					// but first remove all sources from
					// the table
					rtpSession.removeAllSources();

					// We are not active senders anymore
					rtpSession.getMySource().activeSender
					    = false;

					rtpSession.timeOfLastRTCPSent =
					    rtpSession.currentTime();
				    }
				else
				    // We never sent anything and we have to
				    // quit :( do not send BYE {{C False}}
				    {
					terminate = true;
				    }
			    }
			else // {{B False}}
			    {
				byte[] compoundRTCPPacket =
				    assembleRTCPPacket();

				sendPacket ( compoundRTCPPacket );

				// If the packet just sent was a BYE packet,
				// then its time to terminate.
				if ( rtpSession.isByeRequested &&
				     ! waitingForByeBackoff ) // {{D True}}
				    {
					// We have sent a BYE packet, so it's
					// time to terminate
					terminate = true;
				    }
				else // {{D False}}
				    {
					rtpSession.timeOfLastRTCPSent =
					    rtpSession.currentTime();
				    }

			    }
		    }
		else
		    // This is not the right time to send a RTCP packet,
		    // just reschedule
		    // {{A False}}
		    {;}

		waitingForByeBackoff = false;
		rtpSession.tn = rtpSession.currentTime() + rtpSession.t;
		rtpSession.pmembers = rtpSession.getNumberOfMembers();

	    }

	// Added by Andreas Piirimets 2004-02-22
	if (m_RTCPSenderSocket != null) {
	    m_RTCPSenderSocket.close();
	}

    }



    /**
     *    Provides a wrapper around java sleep to handle exceptions
     *    in case when session wants to quit.
     *    Returns 0 is sleep was interrupted and 1 if all
     *    the sleep time was consumed.
     *
     *   @param     seconds   No. of seconds to sleep
     *   @return    0 if interrupted, 1 if the sleep progressed normally
     */
    int sleepTillInterrupted ( double seconds )
    {
        try
	    {
		sleep ( (long) seconds * 1000 );
		rtpSession.outprintln ( "In sleep function after sleep." );
	    }
        catch ( InterruptedException e )
	    {
		rtpSession.outprintln ( "Interrupted" );
		return (0);
	    }

        rtpSession.outprintln ("Just woke up after try");
        return (1);
    }


    /**
     * Top Level Function to assemble a compound RTCP Packet. This function
     * determines what kind of RTCP packet needs to be created and sent out.
     * If this source  is a sender (ie. generating RTP packets), then a
     * Sender Report (SR) is sent out otherwise a Receiver Report (RR) is
     * sent out. An SDES Packet is appended to the SR or RR Packet. If a BYE
     * was requested by the application, a BYE PAcket is sent out.
     *
     * @return  The Compound RTCP Packet
     */


    public byte[] assembleRTCPPacket ()
    {
        byte[] packet = new byte [0];

        // Determine if the packet is SR or RR
        Source sMe = rtpSession.getSource ( rtpSession.ssrc );

        //
        // Generate an SR packet if I am an active sender and did send an
        // RTP packet since last time I sent an RTCP packet.
        //
        if ( ( sMe.activeSender ) &&
	     ( rtpSession.timeOfLastRTCPSent < rtpSession.timeOfLastRTPSent ) )

            packet =
		PacketUtils.append(packet, assembleRTCPSenderReportPacket());
        else
            packet =
		PacketUtils.append ( packet,
				     assembleRTCPReceiverReportPacket() );


        // Append an SDES packet
        packet =
	    PacketUtils.append(packet,
			       assembleRTCPSourceDescriptionPacket());

        // Append a BYE packet if necessary
        if ( rtpSession.isByeRequested )
            packet =
		PacketUtils.append(packet,
				   assembleRTCPByePacket("Quitting"));

        return packet;
    }

    /***********************************************************************
     *
     *    Functions to assemble RTCP packet components.
     *
     ***********************************************************************/

    /**
     *   Creates a Sender Report RTCP Packet.
     *
     *
     *   @return  The Sender Report Packet.
     */
    private byte[] assembleRTCPSenderReportPacket ()
    {
	/*
	  0                   1                   2                   3
	  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |V=2|P|    RC   |   PT=SR=200   |             length            | header
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                         SSRC of sender                        |
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |              NTP timestamp, most significant word             | sender
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ info
	  |             NTP timestamp, least significant word             |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                         RTP timestamp                         |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                     sender's packet count                     |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                      sender's octet count                     |
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |                 SSRC_1 (SSRC of first source)                 | report
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
	  | fraction lost |       cumulative number of packets lost       |   1
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |           extended highest sequence number received           |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                      interarrival jitter                      |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                         last SR (LSR)                         |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                   delay since last SR (DLSR)                  |
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |                 SSRC_2 (SSRC of second source)                | report
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
	  :                               ...                             :   2
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |                  profile-specific extensions                  |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

	*/

        final int FIXED_HEADER_SIZE = 4; // 4 bytes

        // construct the first byte containing V, P and RC
        byte v_p_rc;
        v_p_rc =    (byte) ( ( RTCPConstants.VERSION << 6 ) |
                             ( RTCPConstants.PADDING << 5 ) |
                             ( 0x00 )
			     // take only the right most 5 bytes i.e.
			     // 00011111 = 0x3F
			     );

        // SSRC of sender
        byte[] ss = PacketUtils.longToBytes ( rtpSession.ssrc, 4);

        // Payload Type = SR
        byte[] pt = PacketUtils.longToBytes ( (long)RTCPConstants.RTCP_SR, 1);

        // Get NTP Time and put in 8 bytes
	byte[] ntp_TimeStamp =
	    PacketUtils.longToBytes (rtpSession.currentTime(), 8 );

        //byte NTP_TimeStamp[] = new byte [8];
        byte[] rtp_TimeStamp =
	    PacketUtils.longToBytes ( (long) rtpSession.tc +
				      Session.RANDOM_OFFSET , 4 );

        byte[] senderPacketCount =
	    PacketUtils.longToBytes ( rtpSession.packetCount, 4);
        byte[] senderOctetCount =
	    PacketUtils.longToBytes ( rtpSession.octetCount, 4);

        // Create an initial report block, this will dynamically grow
        // as each report is appended to it.
	//  byte ReportBlock [] = new byte [0];

        // Append all the sender report blocks
        byte[] receptionReportBlocks = new byte [0];

        receptionReportBlocks =
	    PacketUtils.append(receptionReportBlocks,
			       assembleRTCPReceptionReport() );

        // Each reception report is 24 bytes, so calculate the number of
	// sources in the reception report block and update the reception
	// block count in the header
        byte receptionReports = (byte) (receptionReportBlocks.length / 24 );

        // Reset the RC to reflect the number of reception report blocks

        v_p_rc = (byte) ( v_p_rc | (byte) ( receptionReports & 0x1F ) ) ;
        rtpSession.outprintln("RC: " + receptionReports);

        // Length is 32 bit words contained in the packet -1
        byte[] length =
	    PacketUtils.longToBytes ( (FIXED_HEADER_SIZE + ss.length +
						   ntp_TimeStamp.length +
						   rtp_TimeStamp.length +
						   senderPacketCount.length +
						   senderOctetCount.length +
						   receptionReportBlocks.length
						   ) /4 -1 ,
						  2);
        // SDESPacket.length
        // Append all the above components and construct a Sender Report
	// packet
        byte[] srPacket = new byte [1];
        srPacket[0] = v_p_rc;
        srPacket = PacketUtils.append (srPacket, pt );
        srPacket = PacketUtils.append (srPacket, length );
        srPacket = PacketUtils.append (srPacket, ss );
        srPacket = PacketUtils.append (srPacket, ntp_TimeStamp );
        srPacket = PacketUtils.append (srPacket, rtp_TimeStamp );
        srPacket = PacketUtils.append (srPacket, senderPacketCount );
        srPacket = PacketUtils.append (srPacket, senderOctetCount );
        srPacket = PacketUtils.append (srPacket, receptionReportBlocks );


        return srPacket;
    }

    /**
     *   Creates a Receiver Report RTCP Packet.
     *
     *   @return  byte[] The Receiver Report Packet
     */
    private byte[] assembleRTCPReceiverReportPacket ()
    {
	/*
          0                   1                   2                   3
	  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |V=2|P|    RC   |   PT=RR=201   |             length            | header
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                         SSRC of sender                        |
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |                 SSRC_1 (SSRC of first source)                 | report
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
	  | fraction lost |       cumulative number of packets lost       |   1
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |           extended highest sequence number received           |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                      interarrival jitter                      |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                         last SR (LSR)                         |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                   delay since last SR (DLSR)                  |
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |                 SSRC_2 (SSRC of second source)                | report
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
	  :                               ...                             :   2
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |                  profile-specific extensions                  |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


	*/
	final int FIXED_HEADER_SIZE = 4; // 4 bytes

        // construct the first byte containing V, P and RC
        byte v_p_rc;
        v_p_rc = (byte) ( ( RTCPConstants.VERSION << 6 ) |
			  ( RTCPConstants.PADDING << 5 ) |
			  ( 0x00 )
			  // take only the right most 5 bytes i.e.
			  // 00011111 = 0x1F
			  );

        // SSRC of sender
        byte[] ss = PacketUtils.longToBytes ( rtpSession.ssrc, 4);

        // Payload Type = RR
        byte[] pt =
	    PacketUtils.longToBytes ( (long)RTCPConstants.RTCP_RR, 1);

        byte[] receptionReportBlocks =
	    new byte [0];

        receptionReportBlocks =
	    PacketUtils.append(receptionReportBlocks,
			       assembleRTCPReceptionReport());

        // Each reception report is 24 bytes, so calculate the number of
	// sources in the reception report block and update the reception
	// block count in the header
        byte receptionReports = (byte) (receptionReportBlocks.length / 24 );

        // Reset the RC to reflect the number of reception report blocks
        v_p_rc = (byte) ( v_p_rc | (byte) ( receptionReports & 0x1F ) ) ;

        byte[] length =
	    PacketUtils.longToBytes((FIXED_HEADER_SIZE + ss.length +
				     receptionReportBlocks.length
				     ) /4 -1,
				    2);

        byte[] rrPacket = new byte [1];
        rrPacket[0] = v_p_rc;
        rrPacket = PacketUtils.append(rrPacket, pt );
        rrPacket = PacketUtils.append(rrPacket, length );
        rrPacket = PacketUtils.append(rrPacket, ss );
        rrPacket = PacketUtils.append(rrPacket, receptionReportBlocks );

        rtpSession.outprintln("RRPacket" + rrPacket[1]);
        return rrPacket;
    }


    /**
     *   Creates an Source Description SDES RTCP Packet.
     *
     *   @return  The SDES Packet.
     */
    private byte[] assembleRTCPSourceDescriptionPacket ()
    {
        /* following figure from draft-ietf-avt-rtp-new-00.txt
	   0                   1                   2                   3
	   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	   |V=2|P|    SC   |  PT=SDES=202  |             length            |
	   +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	   |                          SSRC/CSRC_1                          |
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	   |                           SDES items                          |
	   |                              ...                              |
	   +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	   |                          SSRC/CSRC_2                          |
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	   |                           SDES items                          |
	   |                              ...                              |
	   +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
        */

        final int FIXED_HEADER_SIZE = 4; // 4 bytes
        // construct the first byte containing V, P and SC
        byte v_p_sc;
        v_p_sc =    (byte) ( ( RTCPConstants.VERSION << 6 ) |
                             ( RTCPConstants.PADDING << 5 ) |
                             ( 0x01 )
			     );

        byte[] pt =
	    PacketUtils.longToBytes ( (long)RTCPConstants.RTCP_SDES, 1);

        /////////////////////// Chunk 1 ///////////////////////////////
        byte[] ss =
	    PacketUtils.longToBytes ( (long) rtpSession.ssrc, 4 );


        ////////////////////////////////////////////////
        // SDES Item #1 :CNAME
        /* 0                   1                   2                   3
           0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	   |    CNAME=1    |     length    | user and domain name         ...
	   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	*/

        byte item = RTCPConstants.RTCP_SDES_CNAME;
        byte[] user_and_domain = new byte [ rtpSession.getCName().length()];
        user_and_domain = rtpSession.getCName().getBytes();


        // Copy the CName item related fields
        byte[] cnameHeader = { item, (byte) user_and_domain.length };

        // Append the header and CName Information in the SDES Item Array
        byte[] sdesItem = new byte[0] ;
        sdesItem = PacketUtils.append ( sdesItem, cnameHeader );
        sdesItem = PacketUtils.append ( sdesItem, user_and_domain );

        //Variable to keep track of whether to send e-mail info out or not.
        //E-mail info is sent out once every 7 packets


        if ( ( Math.IEEEremainder ( packetcount , (double) 7 ) ) == 0)
            {
                // Construct  the e-mail information

                byte item2 = RTCPConstants.RTCP_SDES_EMAIL;
                byte[] email = new byte [rtpSession.getEMail().length()];
                email = rtpSession.getEMail().getBytes();
                byte[] emailHeader = {item2 , (byte) email.length};

                sdesItem = PacketUtils.append(sdesItem, emailHeader);
                sdesItem = PacketUtils.append(sdesItem, email);

            }
	packetcount++;

        int remain = (int) Math.IEEEremainder(sdesItem.length,
					      (double) 4 );


        int padLen = 0;
        // e.g. remainder -1, then we need to pad 1 extra
        // byte to the end to make it to the 32 bit boundary
        if ( remain < 0 )
            padLen = Math.abs ( remain );
        // e.g. remainder is +1 then we need to pad 3 extra bytes
        else if ( remain > 0 )
            padLen = 4-remain;


        // Assemble all the info into a packet
        // byte SDES[] = new byte [ FIXED_HEADER_SIZE + ss.length +
	// user_and_domain.length + PadLen];
        byte[] sdes = new byte[2];

        // Determine the length of the packet (section 6.4.1 "The length of
	// the RTCP packet in 32 bit words minus one, including the heade and
	// any padding")
        byte[] sdesLength = PacketUtils.longToBytes ( ( FIXED_HEADER_SIZE +
                                                        ss.length +
                                                        sdesItem.length +
                                                        padLen)/4-1,
						      2 );

        sdes[0] = v_p_sc;
        sdes[1] = pt[0];
        sdes = PacketUtils.append(sdes, sdesLength );
        sdes = PacketUtils.append(sdes, ss );
        sdes = PacketUtils.append(sdes, sdesItem);



        // Append necessary padding fields
        byte[] padBytes = new byte [ padLen ];
        sdes = PacketUtils.append ( sdes, padBytes );

        return sdes;
    }


    /**
     * Creates the Reception reports by determining which source need to be
     * included and makes calls to AssembleRTCPReceptionReportBlock function
     * to generate the individual blocks. The function returns the fixed
     * length RTCP Sender Info ( 5*32 bits or 20 bytes ).
     *
     * @return The RTCP Reception Report Blocks
     */
    private byte[] assembleRTCPReceptionReport()
    {
        byte[] reportBlock = new byte [0];

        // Keeps track of how many sender report blocks are generated. Make
	// sure that no more than 31 blocks are generated.
        int receptionReportBlocks = 0;

        Enumeration activeSenderCollection = rtpSession.getSources();

        // Iterate through all the sources and generate packets for those
        // that are active senders.
        while ( receptionReportBlocks < 31 &&
		activeSenderCollection.hasMoreElements() )
	    {
		Source s = (Source) activeSenderCollection.nextElement();

		// rtpSession.outprintln ( "\ns.TimeoflastRTPArrival : " +
		// s.TimeOfLastRTPArrival + "\t"
		// + "rtpSession.TimeOfLastRTCPSent : " +
		// rtpSession.TimeOfLastRTCPSent + "\n" );

		if ((s.timeOfLastRTPArrival>rtpSession.timeOfLastRTCPSent) &&
		     (s.ssrc != rtpSession.ssrc)  )
		    {
			reportBlock =
			    PacketUtils.append
			    (reportBlock,
			     assembleRTCPReceptionReportBlock ( s ) );
			receptionReportBlocks++;
		    }

		// TODO : Add logic for more than 31 Recption Reports - AN

	    }

        return reportBlock;
    }


    /**
     *
     *    Constructs a fixed length RTCP Reception.
     *    Report block ( 6*32 bits or 24 bytes ) for a particular source.
     *
     *   @param Source  The source for which this Report is being constructed.
     *   @return        The RTCP Reception Report Block
     *
     */
    private byte[] assembleRTCPReceptionReportBlock(Source rtpSource )
    {
	/*
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |                 SSRC_1 (SSRC of first source)                 | report
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
	  | fraction lost |       cumulative number of packets lost       |   1
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |           extended highest sequence number received           |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                      interarrival jitter                      |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                         last SR (LSR)                         |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                   delay since last SR (DLSR)                  |
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+

	*/

        byte[] rrBlock = new byte [ 0 ];

        //Update all the statistics associated with this source
        rtpSource.updateStatistics();

	//SSRC_n - source identifier - 32 bits
        byte[] ssrc = PacketUtils.longToBytes ( (long) rtpSource.ssrc, 4 );

        //fraction lost -   8 bits
        byte[] fraction_lost =
	    PacketUtils.longToBytes ( (long) rtpSource.fraction, 1 );


        // cumulative number of packets lost -  24 bits
        byte[] pkts_lost = PacketUtils.longToBytes((long) rtpSource.lost, 3 );

        // extended highest sequence number received - 32 bits
        byte[] last_seq =
	    PacketUtils.longToBytes ( (long) rtpSource.last_seq, 4);

        // interarrival jitter - 32 bits
        byte[] jitter = PacketUtils.longToBytes((long) rtpSource.jitter, 4);

        // last SR timestamp(LSR) - 32 bits
        byte[] lst = PacketUtils.longToBytes ( (long) rtpSource.lst, 4);

        // delay since last SR (DLSR)   32 bits
        byte[] dlsr =  PacketUtils.longToBytes ( (long) rtpSource.dlsr, 4);

        rrBlock = PacketUtils.append ( rrBlock, ssrc );
        rrBlock = PacketUtils.append ( rrBlock, fraction_lost);
        rrBlock = PacketUtils.append ( rrBlock, pkts_lost );
        rrBlock = PacketUtils.append ( rrBlock, last_seq );
        rrBlock = PacketUtils.append ( rrBlock, jitter );
        rrBlock = PacketUtils.append ( rrBlock, lst );
        rrBlock = PacketUtils.append ( rrBlock, dlsr );
	// rtpSession.outprintln("fraction_lost" + RRBlock[4]);

        return rrBlock;
    }


    /**
     *
     *   Constructs a "BYE" packet (PT=BYE=203)
     *
     *   @param   ReasonsForLeaving.
     *   @return  The BYE Packet .
     *
     */
    private byte[] assembleRTCPByePacket ( String reasonsForLeaving )
    {
        /*
	  7.6 BYE: Goodbye RTCP packet
	  0                   1                   2                   3
	  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |V=2|P|    SC   |   PT=BYE=203  |             length            |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  |                           SSRC/CSRC                           |
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	  :                              ...                              :
	  +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
	  |     length    |               reason for leaving             ... (opt)
	  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	*/

        final int FIXED_HEADER_SIZE = 4; // 4 bytes
        // construct the first byte containing V, P and SC
        byte v_p_sc;
        v_p_sc =    (byte) ( ( RTCPConstants.VERSION << 6 ) |
                             ( RTCPConstants.PADDING << 5 ) |
                             ( 0x01 )
			     );

        // Generate the payload type byte
        byte[] pt = PacketUtils.longToBytes ( (long)RTCPConstants.RTCP_BYE, 1);


        // Generate the SSRC
        byte[] ss = PacketUtils.longToBytes ( (long) rtpSession.ssrc, 4 );

        byte[] textLength =
	    PacketUtils.longToBytes(reasonsForLeaving.length(), 1);

        // Number of octects of data (excluding any padding)
        int dataLen =
	    FIXED_HEADER_SIZE + ss.length + 1 + reasonsForLeaving.length();

        // Calculate the pad octects required
        int padLen = PacketUtils.calculatePadLength( dataLen );
        byte[] padBytes = new byte [ padLen ];

        // Length of the packet is number of 32 byte words - 1
        byte[] packetLength =
	    PacketUtils.longToBytes ( ( dataLen + padLen )/4 -1, 2);

        //////////////////////// Packet Construction /////////////////////////
        byte[] packet = new byte [1];

        packet[0] = v_p_sc;
        packet = PacketUtils.append ( packet, pt );
        packet = PacketUtils.append ( packet, packetLength );
        packet = PacketUtils.append ( packet, ss );
        packet = PacketUtils.append ( packet, textLength );
        packet = PacketUtils.append ( packet, reasonsForLeaving.getBytes() );
        packet = PacketUtils.append ( packet, padBytes );

        return packet;
    }


    /**
     *   Sends the byte array RTCP packet.
     *   Zero return is error condition
     *
     *   @param packet packet to be sent out.
     *   @return 1 for success, 0 for failure.
     */
    private int sendPacket ( byte[] packet)
    {
	DatagramPacket dGram =
	    new DatagramPacket( packet, packet.length, m_InetAddress,
				m_MulticastRTCPPort );

	// IP: Temp to overcome problem with this method being called
	//     before m_RTCPSenderSocket is created
	if (m_RTCPSenderSocket == null)
	    return 0;

	// Set ttl=5 and send
	try
            {
		m_RTCPSenderSocket.send ( dGram/*, (byte) 5 */);
		return (1);
            }
	catch ( java.io.IOException e )
	    {
		//System.err.println ("Error: While sending the RTCP Packet");
                //e.printStackTrace();
		return (0);
	    }
    }

}



