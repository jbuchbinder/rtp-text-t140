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

import java.util.Random;
import se.omnitor.protocol.rtp.Session;
import se.omnitor.protocol.rtp.StateThread;
import se.omnitor.protocol.rtp.packets.RTPPacket;
import se.omnitor.protocol.rtp.text.RtpTextBuffer;
import se.omnitor.protocol.rtp.text.RtpTextPacketizer;
import se.omnitor.protocol.rtp.text.SyncBuffer;
import se.omnitor.protocol.rtp.text.TextConstants;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An RTP text transmitter that reads characters from a buffer and sends them
 * over the network to another host.
 *
 * @author Ingemar Persson, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class RtpTextTransmitter implements Runnable {

	private StateThread thisThread = null;
	private Session rtpSession;
	private RtpTextPacketizer textPacketizer;

	private String ipAddress;
	private int localPort;
	private int remotePort;
	private boolean redFlagOutgoing;

	private boolean isEconf351Client = false;

	private SyncBuffer dataBuffer;

	// EZ: T140 redundancy
	private se.omnitor.protocol.rtp.t140redundancy.RedundancyFilter redFilter;

	private int redundantT140Generations = 0;

	//EZ: SSRC
	private long ssrc = 0;

	// declare package and classname
	public final static String CLASS_NAME = RtpTextTransmitter.class.getName();
	// get an instance of Logger
	private static Logger logger = Logger.getLogger(CLASS_NAME);


	/**
	 * Initializes the transmitter. Calculates buffer time.
	 *
	 * @param startRtpTransmit Whether RTP transmission should start directly
	 * or not
	 * @param ipAddress The IP address to the remote host
	 * @param localPort The local RTP port to send RTP data from
	 * @param remotePort The remote RTP port to send RTP data to
	 * @param t140PayloadType The RTP payload type number for T140 to use
	 * @param redFlagOutgoing Whether redundancy should be used
	 * @param redPayloadType The RTP payload type number to use for RED
	 * @param redundantGenerations The number of redundant generations to use,
	 * if redundancy should be used.
	 * @param redT140FlagOutgoing Whether T.140 redundancy should be used.
	 * @param redundantT140Generations The number of redundant T.140
	 * generations to use.
	 * @param dataBuffer The buffer with incoming data. This has to be started
	 * before transmission begins.
	 */
	public RtpTextTransmitter(Session rtpSession,
			boolean startRtpTransmit,
			String ipAddress,
			int localPort,
			int remotePort,
			int t140PayloadType,
			boolean redFlagOutgoing,
			int redPayloadType,
			int redundantGenerations,
			boolean redT140FlagOutgoing,
			int redundantT140Generations,
			SyncBuffer dataBuffer, boolean econf351Client) {

		// write methodname
		final String METHOD = "RtpTextTransmitter(Session rtpSession, ...)";
		// log when entering a method
		logger.entering(CLASS_NAME, METHOD, new Object[]{ipAddress, "'" + localPort + "'" , "'" + remotePort + "'"});


		this.rtpSession = rtpSession;// new Session(ipAddress, 64000);
		this.ipAddress = ipAddress;
		this.localPort = localPort;
		this.remotePort = remotePort;
		this.redFlagOutgoing = redFlagOutgoing;
		this.redundantT140Generations = redundantT140Generations;
		this.dataBuffer = dataBuffer;
		this.isEconf351Client = econf351Client;

		// using redundancy
		if (redFlagOutgoing) {
			dataBuffer.setRedGen(redundantGenerations);
		}
		else {
			dataBuffer.setRedGen(0);
		}

		// EZ: T140 redundancy init
		if (redundantT140Generations>0) {
			redFilter =
				new se.omnitor.protocol.rtp.t140redundancy.RedundancyFilter
				(redFlagOutgoing, redundantT140Generations);
		}

		// Calculate new buffertime and cpp according to cps
		/*
	if (cps > 0) {
	    cpp = (int)Math.round((double)cps * (double)bufferTime/1000.0);
	    if (cpp == 0) {
		cpp = 1;
	    }
	    this.bufferTime =
		Math.round(1000.0 * (double)cpp / (double)cps);

	    useCpp = true;
	}
	else {
	    this.bufferTime = bufferTime;
	    cpp = 100;
	    useCpp = false;
	}
		 */

		textPacketizer = new RtpTextPacketizer(t140PayloadType,
				redPayloadType,
				redundantGenerations);

		if (redFlagOutgoing) {
			rtpSession.setSendPayloadType(redPayloadType);
		}
		else {
			rtpSession.setSendPayloadType(t140PayloadType);
		}

		// Changed by Andreas Piirimets 2004-02-16
		// The transmission should be able to handle a localPort which is
		// different to the remote port.
		//rtpSession.openRTPTransmitSocket(remotePort);
		//rtpSession.openRTPTransmitSocket(localPort, remotePort);

		//rtpSession.createAndStartRTCPSenderThread(localPort+1, remotePort+1);

		//Construct SSRC
		ssrc = createSSRC();

		if (startRtpTransmit) {
			start();
		}
		logger.logp(Level.FINEST, CLASS_NAME, METHOD, "checking ssrc", Long.valueOf(ssrc));
		logger.exiting(CLASS_NAME, METHOD);
	}

	/**
	 * Creates an SSRC for this session.
	 *
	 * @return The SSRC
	 */
	private long createSSRC() {

		//Creata a seed to ensure the SSRC is as random as possible,
		long time  = java.lang.System.currentTimeMillis();
		long ports = (long)remotePort << 32 | localPort;
		long addr  = 0;
		byte[] rawLocalIPAddr  = null;
		byte[] rawRemoteIPAddr = null;
		long seed = 0;

		try {
			rawLocalIPAddr  = java.net.InetAddress.getLocalHost().getAddress();
			rawRemoteIPAddr = java.net.InetAddress.getByName(ipAddress).getAddress();
		} catch (java.net.UnknownHostException uhe) {

		}

		//IPv6
		if(rawLocalIPAddr.length==6) {
			addr = (long)rawLocalIPAddr[0] << 40 |
			(long)rawLocalIPAddr[1] << 32 |
			(long)rawLocalIPAddr[2] << 24 |
			(long)rawLocalIPAddr[3] << 16 |
			(long)rawLocalIPAddr[4] << 8  |
			(long)rawLocalIPAddr[5];
		}
		//IPv4
		else if(rawLocalIPAddr.length==4) {
			addr = (long)rawLocalIPAddr[0] << 56 |
			(long)rawLocalIPAddr[1] << 48 |
			(long)rawLocalIPAddr[2] << 40 |
			(long)rawLocalIPAddr[3] << 32 |
			(long)rawRemoteIPAddr[0] << 24 |
			(long)rawRemoteIPAddr[1] << 16 |
			(long)rawRemoteIPAddr[2] << 8  |
			(long)rawRemoteIPAddr[3];
		}
		else {
			logger.warning("Unknown IP format in createSSRC");
		}

		seed = (time | ports | addr);

		//Use the seed to get the SSRC.
		Random rand = new Random(seed);
		return rand.nextLong();
	}

	/**
	 * Starts the process.
	 *
	 * This process will try to read from the buffer and send it over the
	 * network. It handles automatic resending of redundant data if no data
	 * has been written to the buffer.
	 *
	 */
	public void run()
	{
		// write methodname
		final String METHOD = "run()";
		// log when entering a method
		logger.entering(CLASS_NAME, METHOD);

		RTPPacket outputPacket;
		byte[] data;

		RtpTextBuffer inBuffer;
		RtpTextBuffer outBuffer;

		long lastSentTime = 0;
		long timeNow = 0;
		int bufferTime;

		logger.logp(Level.FINEST, CLASS_NAME, METHOD, "is using redundacy:" + redFlagOutgoing);

		dataBuffer.start();
		dataBuffer.setData(TextConstants.ZERO_WIDTH_NO_BREAK_SPACE);

		bufferTime = dataBuffer.getBufferTime();

		while (thisThread.checkState() != StateThread.STOP) {

			outputPacket = new RTPPacket();

			// Catch data from buffer
			try {
				data = dataBuffer.getData();

				for (int cnt5=0; cnt5<data.length; cnt5++) {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD, "data fetched from buffer, element " + cnt5 + " was '" + data[cnt5] + "' from buffer");
				}

				if (data.length > 0 || redFlagOutgoing) {

					if (thisThread.checkState() == StateThread.STOP) {
						break;
					}

					//EZ: Add T.140 redundancy
					if (redundantT140Generations > 0) {
						data = redFilter.addRedundancy(data);
					}

					inBuffer = new RtpTextBuffer();
					inBuffer.setData(data);
					if (data == null) {
						inBuffer.setLength(0);
					} else {
						inBuffer.setLength(data.length);
					}

					outBuffer = new RtpTextBuffer();

					textPacketizer.encode(inBuffer, outBuffer);
					timeNow = outBuffer.getTimeStamp();

					//EZ: Mark packets after idle period of bufferTime.
					//    Allow an additional 250 ms for processing.
					//    Also mark first packet.
					//    Ignores time wraparounds.
					if ((timeNow - lastSentTime) > (bufferTime + 250)) {
						outBuffer.setMarker(true);
					} else {
						outBuffer.setMarker(false);
					}
					lastSentTime = timeNow;

					// Temp: adding zero at end. This will be removed.
					if (isEconf351Client) {
						byte[] dataToSend = outBuffer.getData();
						byte[] newData = new byte[dataToSend.length + 1];
						System.arraycopy(dataToSend, 0, newData, 0,
								dataToSend.length);
						newData[dataToSend.length] = 0;
						outBuffer.setData(newData);
					}
					outputPacket.setPayloadData(outBuffer.getData());
					outputPacket.setTimeStamp(outBuffer.getTimeStamp());
					outputPacket.setSequenceNumber(outBuffer.
							getSequenceNumber());
					outputPacket.setMarker(outBuffer.getMarker());
					outputPacket.setSsrc(ssrc);

					rtpSession.sendRTPPacket(outputPacket);
				}

			}
			catch (InterruptedException ie) {
				logger.logp(Level.FINE, CLASS_NAME, METHOD, "Transmit thread interrupted", ie);
			}
		}

		// Release thread
		thisThread = null;

		logger.exiting(CLASS_NAME, METHOD);
	}

	/**
	 * Gets the remote host's RTP port.
	 *
	 * @return The remote host's RTP port.
	 */
	public int getRemotePort()
	{
		return remotePort;
	}

	/**
	 * Starts the transmit thread.
	 *
	 */
	public void start()
	{
		if (thisThread == null)
		{
			//logger.finest("Starting transmit thread.");
			thisThread = new StateThread(this, "RtpTextTransmitter");
			thisThread.start();
		}
	}

	/**
	 * Stops the transmit thread.
	 *
	 */
	public synchronized void stop()
	{
		if (thisThread != null)
		{
			//logger.finest("Stopping transmit thread.");
			dataBuffer.stop();
			thisThread.setState(StateThread.STOP);
			thisThread.interrupt();
		}
		if (rtpSession != null) {
			// logger.finest("Stopping RTP and RTCP sessions.");
			rtpSession.stopRTCPSenderThread();
			rtpSession.stopRTPThread();
			rtpSession = null;
			//logger.finest("RTP session stopped.");
		}
	}

	/**
	 * Sets the CName, which will be used in the RTP session
	 *
	 * @param name The CName
	 */
	public void setCName(String name) {
		rtpSession.setCName(name);
	}

	/**
	 * Sets the email address, which will be used in the RTP stream
	 *
	 * @param email The email address
	 */
	public void setEmail(String email) {
		rtpSession.setEmail(email);
	}

	public int dropOneRtpTextSeqNo() {
		if (textPacketizer != null) {
			return textPacketizer.dropOneRtpTextSeqNo();
		}
		return 0;
	}

	public void sendHexCode(String hexCode) {
		byte b[] = new byte[hexCode.length()/2];
		
		for (int cnt=0; cnt<b.length; cnt++) {
			System.out.print("Converting: " + hexCode.substring(cnt*2, cnt*2+2));
			try {
				b[cnt] = (byte)Integer.parseInt(""+hexCode.substring(cnt*2, cnt*2+2), 16);
			}
			catch (NumberFormatException e) {
				// Ignore!
			}
			System.out.println(" to " + b);			
		}
		dataBuffer.setData(b);
	}
}
