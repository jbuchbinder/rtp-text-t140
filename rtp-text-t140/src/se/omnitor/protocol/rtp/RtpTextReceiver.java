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

import java.util.logging.Logger;
import se.omnitor.protocol.rtp.packets.RTCP_actionListener;
import se.omnitor.protocol.rtp.packets.RTCPBYEPacket;
import se.omnitor.protocol.rtp.packets.RTCPReceiverReportPacket;
import se.omnitor.protocol.rtp.packets.RTCPSDESPacket;
import se.omnitor.protocol.rtp.packets.RTCPSenderReportPacket;
import se.omnitor.protocol.rtp.packets.RTP_actionListener;
import se.omnitor.protocol.rtp.packets.RTPPacket;
import se.omnitor.protocol.rtp.text.RtpTextDePacketizer;
import se.omnitor.util.FifoBuffer;
import se.omnitor.protocol.rtp.text.RtpTextBuffer;

/**
 * A RTP text receiver who reads incoming RTP text packets, depacketizes them
 * and puts them into a buffer.
 *
 * @author Ingemar Persson, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class RtpTextReceiver implements Runnable,
					RTP_actionListener,
					RTCP_actionListener {

    private StateThread thisThread = null;
    private RtpTextDePacketizer textDePacketizer;
    private Session rtpSession;

    private int localPort;

    private FifoBuffer dataBuffer;

    private boolean remoteReceiverIsReady;
    private boolean localReceiverIsReady;

    // Retrieve logger for this package
    private Logger logger = Logger.getLogger("se.omnitor.protocol.rtp");

    //EZ: T140 redundancy
    private se.omnitor.protocol.rtp.t140redundancy.RedundancyFilter redFilter;

    /**
     * Initializes the RTP text receiver and starts the reception thread.
     *
     * @param ipAddress The IP address to the remote RTP sender
     * @param localPort The local RTP port to receive RTP text data on
     * @param redFlagIncoming Indicates whether redundancy will be used
     * @param t140PayloadType The RTP payload number for T140.
     * @param redPayloadType The RTP payload number for RED, if used.
     * @param dataBuffer The buffer to write incoming data to
     */
    public RtpTextReceiver(Session rtpSession,
			   String ipAddress,
                           int localPort,
			   boolean redFlagIncoming,
			   int t140PayloadType,
			   int redPayloadType,
			   FifoBuffer dataBuffer) {

        logger.finest("ENTRY");

        this.rtpSession = rtpSession;//new Session(ipAddress, 64000);

        this.localPort = localPort;
	this.dataBuffer = dataBuffer;


	textDePacketizer = new RtpTextDePacketizer(t140PayloadType,
						   redPayloadType,
						   redFlagIncoming);

        if (redFlagIncoming)
        {
            rtpSession.setReceivePayloadType(redPayloadType);
        }
	else {
            rtpSession.setReceivePayloadType(t140PayloadType);
        }

        /*System.err.println("redPayloadType = " + redPayloadType + "\nt140PayloadType = " +t140PayloadType);
        System.err.println("RtpTextReceiver, redFlagIncoming = " + (redFlagIncoming + "").toUpperCase());*/

	remoteReceiverIsReady = false;
	localReceiverIsReady = false;


	//EZ: T140 redundancy init
	//if(redT140FlagIncoming) {
	redFilter =
	    new se.omnitor.protocol.rtp.t140redundancy.RedundancyFilter();
	    //}
    }

    /**
     * Writes a log comment.
     *
     * @throws Throwable (This will never be thrown, it is just a requirement
     * from the finalize() funciton that this Throw clause should be here.)
     */
    protected void finalize() throws Throwable
    {
        logger.finest("Finalizing instance of RtpTextReceiver.");
    }

    /**
     * Initializes the reception thread and starts it.
     *
     */
    public void start()
    {
	logger.finest("Starting text receiver");

        thisThread = new StateThread(this, "RTP Text Receiver");
        thisThread.start();
    }

    /**
     * Starts the reception thread. Actually, this thread doesn't do very
     * much. It starts all listeners and just sleeps until the transmission
     * is stopped. Then it destroys the thread.
     *
     */
    public void run()
    {
	logger.finest("ENTRY");

        //rtpSession.openRTPReceiveSocket(localPort);
        //rtpSession.startRTPThread();
        //rtpSession.createAndStartRTCPReceiverThread(localPort+1);

        rtpSession.addRTP_actionListener(this);
        rtpSession.addRTCP_actionListener(this);

        logger.finest("Ready to receive.");

	localReceiverIsReady = true;

        while (thisThread.checkState() != StateThread.STOP)
        {
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
		// Ignore any interruption here.
            }
        }
        // Finally do..
        logger.finest("Receive thread stopped.");

        // Release thread
        thisThread = null;
    }

    /**
     * Stops the reception of RTP text.
     *
     */
    public synchronized void stop()
    {
        logger.finest("Stopping receive thread.");

	if (thisThread != null) {
	    thisThread.setState(StateThread.STOP);
	    thisThread.interrupt();
	}
	if (rtpSession != null) {
	    rtpSession.stopRTPThread();
	    rtpSession.stopRTCPReceiverThread();
	}
    }

    /**
     * Gets the local RTP port.
     *
     * @return The local RTP port.
     */
    public int getLocalPort()
    {
        return localPort;
    }

    /**
     * Handles incoming RTP text packets. The packet is depacketized and put
     * into the buffer, which GUI will read from.
     *
     * @param rtpPacket The incoming packet.
     */
    public void handleRTPEvent(RTPPacket rtpPacket)
    {
        //TODO: PUT THIS IN THE RUN THREAD AND START THE THREAD IN THIS METHOD

	RtpTextBuffer inBuffer = new RtpTextBuffer();
	RtpTextBuffer outBuffer = new RtpTextBuffer();

	inBuffer.setData(rtpPacket.getPayloadData());
	inBuffer.setSequenceNumber(rtpPacket.getSequenceNumber());
	inBuffer.setTimeStamp(rtpPacket.getTimeStamp());
	inBuffer.setLength(rtpPacket.getPayloadData().length);
	inBuffer.setOffset(0);
	inBuffer.setSsrc(rtpPacket.getSsrc());

        textDePacketizer.decode(inBuffer, outBuffer);

	rtpPacket.setPayloadData(null);
	byte[] datap = outBuffer.getData();


	//EZ: T140 redundancy filter
	byte[] data = redFilter.filterInput(datap);


        if (data != null) {
	    logger.finest("Data to buffer: " + new String(data));
	    dataBuffer.setData(data);
            //System.err.println("RECEIVED DATA = " + new String(data));
        }
    }

    /**
     * When this function is invoked (when an RTCP RR packet has arrived), it
     * means that the remote receiver is ready and RTP Text packets may be
     * sent to remote.
     *
     * @param rrpkt The incoming packet
     */
    public void handleRTCPEvent ( RTCPReceiverReportPacket rrpkt)
    {
	remoteReceiverIsReady = true;
	/*
        logger.finest("\n**** ActionListener RTCP RR Packet: *****\n" +
		      "SSRC = " + Long.toHexString(RRpkt.SenderSSRC) +
		      "\n" +
		      "ReportBlock = " + RRpkt.containsReportBlock +
		      "\n" +
		      "FractionLost = " + RRpkt.ReportBlock.FractionLost +
		      "\n"
		      + "CumPktsLost = " +
		      RRpkt.ReportBlock.CumulativeNumberOfPacketsLost + "\n"
		      + "ExtHighSqRcvd = " +
		      RRpkt.ReportBlock.ExtendedHighestSequenceNumberReceived
		      + "\n"
		      + "IntJitter = " +
		      RRpkt.ReportBlock.InterarrivalJitter   + "\n"
		      + "LastSR = " + RRpkt.ReportBlock.LastSR + "\n"
		      + "Delay_LastSR = " +  RRpkt.ReportBlock.Delay_LastSR +
		      "\n" );
	*/
    }

    /**
     * Does nothing. This is just an implementation of the RTCP_actionListener
     *
     * @param srpkt The incoming packet
     */
    public void handleRTCPEvent ( RTCPSenderReportPacket srpkt) {
	/*
	logger.finest ("\n**** ActionListener RTCP SR Packet: *****\n"
		       + "SSRC = " +
		       Long.toHexString(SRpkt.SenderSSRC) + "\n"
		       + "SenderOctetCount:" +
		       SRpkt.SenderInfo.SenderOctetCount + "\n"
		       + " SenderPacketCount:" +
		       SRpkt.SenderInfo.SenderPacketCount + "\n"
		       + "RTPTimeStamp" +
		       SRpkt.SenderInfo.RTPTimeStamp + "\n"
		       + "NTPTimeStampLeastSignificant" +
		       SRpkt.SenderInfo.NTPTimeStampLeastSignificant + "\n"
		       +  "NTPTimeStampMostSignificant" +
		       SRpkt.SenderInfo.NTPTimeStampMostSignificant + "\n"
		       + "ReportBlock = " +
		       SRpkt.containsReportBlock + "\n" );

        if ( SRpkt.containsReportBlock )
            logger.finest ("FractionLost = " +
			   SRpkt.ReportBlock.FractionLost  + "\n"
			   +  "CumPktsLost = " +
			   SRpkt.ReportBlock.CumulativeNumberOfPacketsLost +
			   "\n"
			   +  "ExtHighSqRcvd = " +
			   SRpkt.ReportBlock.
			   ExtendedHighestSequenceNumberReceived  + "\n"
			   +  "IntJitter = " +
			   SRpkt.ReportBlock.InterarrivalJitter   + "\n"
			   +  "LastSR = " +
			   SRpkt.ReportBlock.LastSR + "\n"
			   +  "Delay_LastSR = " +
			   SRpkt.ReportBlock.Delay_LastSR  + "\n"
			   );
	*/
    }

    /**
     * Does nothing. This is just an implementation of the RTCP_actionListener
     *
     * @param sdespkt The incoming packet
     */
    public void handleRTCPEvent ( RTCPSDESPacket sdespkt) {
	/*
      logger.finest (    "\n**** ActionListener RTCP SDES: *****\n"
                                + "SDES Type: " + sdespkt.SDESItem.Type + "\n"
                                + "SDES Value: " + sdespkt.SDESItem.Value +
                                "\n"
                                + "**************************************\n");
	*/
    }

    /**
     * Does nothing. This is just an implementation of the RTCP_actionListener
     *
     * @param byepkt The incoming packet
     */
    public void handleRTCPEvent ( RTCPBYEPacket byepkt) {
	/*
        logger.finest (    "\n**** ActionListener RTCP BYE: *****\n"
                                + "BYE SSRC: " + byepkt.SSRC + "\n"
                                + "REason For Leaving " +
                                byepkt.ReasonForLeaving + "\n"
                                + "**************************************\n");
	*/
    }

    /**
     * Waiting for the receiver at the other end to be ready. The function
     * exits when the receiver is ready or when the maximum waiting time has
     * elapsed.
     *
     * @param maxWaitingTime The maximum time in seconds to wait. To wait
     * forever, set this value to zero.
     */
    public void waitForRemoteReceiver(int maxWaitingTime) {

	if (maxWaitingTime > 0) {
	    while (!remoteReceiverIsReady && maxWaitingTime > 0) {
		try {
		    Thread.sleep(1000);
		}
		catch (InterruptedException ie) {
		    // Ignore any interruption here.
		}

		maxWaitingTime--;
	    }
	}

	else {
	    while (!remoteReceiverIsReady) {
		try {
		    Thread.sleep(1000);
		}
		catch (InterruptedException ie) {
		    // Ignore any interruption here.
		}
	    }
	}

    }

    /**
     * Waiting for the local receiver to be ready. The function exits when the
     * receiver is ready or when the maximum waiting time has elapsed.
     *
     * @param maxWaitingTime The maximum time in seconds to wait. To wait
     * forever, set this value to zero.
     */
    public void waitForLocalReceiver(int maxWaitingTime) {

	if (maxWaitingTime > 0) {
	    while (!localReceiverIsReady && maxWaitingTime > 0) {
		try {
		    Thread.sleep(1000);
		}
		catch (InterruptedException ie) {
		    // Ignore any interruption here.
		}

		maxWaitingTime--;
	    }
	}

	else {
	    while (!localReceiverIsReady) {
		try {
		    Thread.sleep(1000);
		}
		catch (InterruptedException ie) {
		    // Ignore any interruption here.
		}
	    }
	}

    }

    /**
     * Sets the CName, which will be used in the RTP stream
     *
     * @param name The CName
     */
    public void setCName(String name) {
	rtpSession.setCName(name);
    }

    /**
     * Sets the email, which will be used in the RTP stream
     *
     * @param email The email address
     */
    public void setEmail(String email) {
	rtpSession.setEmail(email);
    }

}











