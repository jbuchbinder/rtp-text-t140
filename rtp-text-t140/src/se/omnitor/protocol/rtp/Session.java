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

import se.omnitor.protocol.rtp.packets.RTCP_actionListener;
import se.omnitor.protocol.rtp.packets.RTCPBYEPacket;
import se.omnitor.protocol.rtp.packets.RTCPReceiverReportPacket;
import se.omnitor.protocol.rtp.packets.RTCPSDESPacket;
import se.omnitor.protocol.rtp.packets.RTCPSenderReportPacket;
import se.omnitor.protocol.rtp.packets.RTP_actionListener;
import se.omnitor.protocol.rtp.packets.RTPPacket;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;


/**
 * This class maintains session related information and provides startup
 * functions.
 *
 * @author Unknown
 */

// IP: Removed all static classes, methods and variables since it prevents
//     multiple sessions to be created
// IP: TODO remove all public variables
// IP: TODO Check that functions are thread safe

public class Session extends java.lang.Object
{
    /**
     *   Random Offset -32 bit
     */
    protected static final short RANDOM_OFFSET =
	(short) Math.abs ( (new Random()).nextInt() & 0x000000FF) ;

    /**
     * Canonical end-point identifier SDES Item.
     *
     */
    private String cname;

    /**
     *   Electronic mail Address SDES Item.
     *
     */
    private String email;

    /**
     *   Bandwidth Available to the session.
     *
     */
    private double bandwidth;

    /**
     *   Payload type for this session.
     *
     */
    // IP: Removed see below
    // private byte         PayloadType;
    // IP: Added sendPayloadType and receivePayloadType because of the nature
    // of T.140
    private byte sendPayloadType;
    private byte receivePayloadType;

    /**
     *   Synchronization Source identifier for this source.
     *
     */
    protected long ssrc;

    /**
     * Total Number of RTP data packets sent out by this source since
     * starting transmission.
     *
     */
    protected long packetCount;

    /**
     *   Total Number of payload octets (i.e not including header or padding)
     *   sent out by this source since starting transmission.
     *
     */
    protected long octetCount;

    /**
     *   Multicast Address.
     *
     */
    protected InetAddress m_InetAddress;

    /**
     *   Reference to the RTP Sender and Receiver Thread.
     *
     */
    protected RTPThreadHandler m_RTPHandler = null;


    /**
     *   Reference to the RTCP Sender and Receiver Thread handler.
     *
     */
    protected RTCPThreadHandler m_RTCPHandler = null;

    /**
     *   The startup time for the application.
     *
     */
    protected long appStartupTime;

    /**
     *   Datasource which will be used to as a late-arrival repository. Server
     *   server will send objects out to late-coming clients and late coming
     *   clients will save the received data in this vector.
     */
    protected Vector initializationData = new Vector();

    /**
     *   This variable determines whether the RTP loopback packets will
     *   generate events. The user can choose to disable loopback.  Default
     *   is true.
     */
    protected boolean enableLoopBack = true;

    /**
     * This variable determines whether Debug information will be printed
     *  or not. Default is false.
     *
     */
    protected boolean debugOutput = false;

    /**
     *   Initialize the Random Number Generator.
     *
     */
    private Random rnd = new Random();

    /**
     *   Reference to the RTP event action listener.
     *
     */
    private RTP_actionListener m_RTP_actionListener = null;

    /**
     *   Reference to the RTCP event action listener.
     *
     */
    private RTCP_actionListener m_RTCP_actionListener = null;


    /**
     *   The last time an RTCP packet was transmitted.
     */
    protected double timeOfLastRTCPSent = 0;

    /**
     *    The current time.
     */
    protected double tc = 0;

    /**
     *    The next scheduled transmission time of an RTCP packet.
     */
    protected double tn = 0;

    /**
     *   The estimated number of session members at time tp.
     */
    protected int pmembers = 0;

    /**
     *   The target RTCP bandwidth, i.e., the total bandwidth that
     *   will be used for RTCP packets by all members of this session, in
     *   octets per second. This should be 5 parameter supplied to the
     *   application at startup.
     */
    protected double rtcp_bw = 0;

    /**
     *   Flag that is true if the application has sent data since the
     *   2nd previous RTCP report was transmitted.
     */
    protected boolean we_sent = false;

    /**
     *   The average RTCP packet size sent by this user.
     */
    protected double avg_rtcp_size = 0;

    /**
     *   Flag that is true if the application has not yet sent an
     *   RTCP packet.
     */
    protected boolean initial = true;

    /**
     *   Average size of the packet constructed by the application
     */
    protected double avg_pkt_sz = 0;

    /**
     *   True if session instantiator requested a close.
     */
    protected boolean isByeRequested = false;

    /**
     *   Deterministic time interval for next RTCP transmission.
     */
    protected double td = 0;

    /**
     *   Randomized time interval for next RTCP transmission.
     */
    protected double t = 0;

    /**
     *   Time this source last sent an RTP Packet
     */
    protected double timeOfLastRTPSent = 0;

    /**
     * A hastable that stores all the sources subscribed to this multicast
     * group
     */
    protected Hashtable sourceMap;

    // IP: Moved these two methods from RTPThreadHandler since it is also used
    //     by RTCPSenderThread

    /**
     *   Initialize Random Number Generator
     */
    //protected Random randomNumGenerator = new Random();



    /**
     * Updates all the sources to see whether the sources are still
     * active members or senders. Iterate through all the sources
     * and adjust their status according to the following criteria <br>
     * <br>
     *   1.  If Time of last RTPSent < tc - T, then drop them from the list
     *        of active senders <br>
     *   2.  If time of last RTCPSent < tc - 5Td then remove them from the
     *        members list except if its your own SSRC <br>
     *
     * @return number of sources updated.
     */
    public int updateSources()
    {
	// iterate through the sources updating their flags
	Enumeration SourceCollection  = getSources();

	int n = 0;

	while ( SourceCollection.hasMoreElements() )
	    {
		Source s = (Source) SourceCollection.nextElement();


		// If Time of last RTPSent < tc - t, then drop them from the
		// list of active senders

		if (( s.timeOfLastRTPArrival < this.currentTime() - t*1000) &&
		   s.activeSender ==true)
                    {
                        s.activeSender =false;
			this.outprintln("Degrading from a sender to member");
			this.outprintln("SSRC = 0x" +
					Integer.toHexString((int)s.ssrc));
                        n++;
                    }


		// If time of last RTCPSent < tc - 5Td then remove them from
		// the members list except if its your own SSRC -NOte Td is in
		// seconds and rest of the times are in milliseconds
		if ( (s.timeOfLastRTCPArrival <
		      (this.currentTime() - 5*td*1000) )  &&  s.ssrc != ssrc )
                    {

			//this.outprintln();
			//this.outprintln("Td" + Td);
			//this.outprintln("Removing Source : " + "SSRC = 0x" +
			//Integer.toHexString((int)s.SSRC));
			this.outprintln("Removing Source : " +
					"SSRC = 0x" +
					Integer.toHexString((int)s.ssrc));
			//this.outprintln("Current Time "  +
			//this.CurrentTime());
			//this.outprintln ("s.TimeOfLastRTCPArrival" +
			//s.TimeOfLastRTCPArrival);
                        removeSource (s.ssrc);
			this.outprintln("No. of members" +
					getNumberOfMembers ());
			this.outprintln("No. of senders" +
					getNumberOfActiveSenders ());
			this.outprintln();
			this.outprintln();
			this.outprintln();
			this.outprintln();

                        n++;
                    }
	    }

	// return the number of sources updated
	return (n);
    }


    /**
     * The only constructor. Requires CNAME and session bandwidth.
     * Initializes the SSRC to a randomly generated number.
     *
     * @param  multicastGroupIPAddress Dotted decimal representation of the
     * Multicast group IP address.
     * @param  bandwidth           Bandwidth available to the session.
     */
    public Session (String multicastGroupIPAddress, double bandwidth, int localPort)

    {
	this.bandwidth = bandwidth;

	System.out.println("Creating RTP session");
	cname = "";
	email = "";

	sourceMap = new Hashtable();

	m_InetAddress = getInetAddress ( multicastGroupIPAddress );

	// Create a new RTP Handler thread (but do not start it yet)
	m_RTPHandler = new RTPThreadHandler ( m_InetAddress, localPort, this, true);

	// Create a new RTCP Handler thread (but do not start it yet)
	//  Set the sendto and recvfrom ports
	m_RTCPHandler = new RTCPThreadHandler ( m_InetAddress, localPort+1, this, true);

	// Initilize session level variables
	initialize();

	// IP: Removed following line
	// this.outprintln ( "SSRC: 0x" + Long.toHexString(SSRC) +
	//" CName: " + CNAME );
    }

    // IP: Added method
    // Andreas Piirimets: added support for different ports (local
    // and remote)
    /**
     * Opens an RTP transmit socket. Data is sent from a local port that has
     * the same port number as the remote port.
     *
     * @param remotePort The remote RTP port to send data to
     */
    public void openRTPTransmitSocket(int remotePort) {
	openRTPTransmitSocket(remotePort, remotePort);
    }

    /**
     * Opens an RTP transmit socket
     *
     * @param localPort The local RTP port to send data from
     * @param remotePort The remote RTP port to send data to
     */
    public void openRTPTransmitSocket(int localPort, int remotePort)
    {
	m_RTPHandler.openTransmitSocket(localPort, remotePort);
    }

    // IP: Added method
    /**
     * Opens an RTP receive socket.
     *
     * @param localPort The local RTP port to receive data on
     */
    public void openRTPReceiveSocket(int localPort)
    {
	m_RTPHandler.openReceiveSocket(localPort);
    }

    /**
     * Sets the RTP payload type for reception.
     *
     * @param payloadType THe RTP payload type to receive
     */
    // IP: Removed and replaced with two new methods
    //public synchronized void setPayloadType ( int payloadType ) {
    //    PayloadType = (byte) payloadType;
    //}
    public synchronized void setReceivePayloadType ( int payloadType ) {
	receivePayloadType = (byte) payloadType;
    }

    /**
     * Sets the RTP payload type for transmission.
     *
     * @param payloadType The RTP payload type number to transmit
     */
    public synchronized void setSendPayloadType ( int payloadType ) {
	sendPayloadType = (byte) payloadType;
    }

    /**
     * Gets the RTP payload type for reception.
     *
     * @return The RTP paylaod type that is received
     */
    // IP: Removed and replaced with two new methods
    //public synchronized byte getPayloadType ( ) {
    //    return PayloadType;
    //}
    public synchronized byte getReceivePayloadType ( ) {
	return receivePayloadType;
    }

    /**
     * Gets the RTP payload type for transmission
     *
     * @return The RTP payload type that is sent
     */
    public synchronized byte getSendPayloadType ( ) {
	return sendPayloadType;
    }

    /**
     * Set the CNAME.
     *
     * @param cname The CNAME to set
     */
    public synchronized void setCName ( String cname ) {
	this.cname = cname;
    }

    /**
     * Gets the CNAME.
     *
     * @return The CNAME
     */
    public synchronized String getCName () {
	return cname;
    }

    /**
     * Sets the email address.
     *
     * @param email The email address.
     */
    public synchronized void setEMail ( String email ) {
	this.email = email;
    }

    /**
     * Gets the email address.
     *
     * @return The email address.
     */
    public synchronized String getEMail ( ) {
	return email;
    }

    /**
     * Starts the RTP thread.
     *
     */
    public synchronized void startRTPThread( )
    {
	m_RTPHandler.start();
    }

    /**
     * Stops the RTP thread.
     *
     */
    public synchronized void stopRTPThread( )
    {
	m_RTPHandler.stop();
    }

    /**
     * Starts the RTCP Receiver thread.
     *
     * @param localPort The local port to use when sending RTCP data
     */
    public synchronized void createAndStartRTCPReceiverThread(int localPort)
    {
	m_RTCPHandler.createAndStartRTCPReceiverThread(localPort);
    }

    /**
     * Stops the RTCP Sender thread
     *
     */
    public synchronized void stopRTCPReceiverThread()
    {
	m_RTCPHandler.stopRTCPReceiverThread();
    }

    /**
     * Starts the RTCP Sender thread
     *
     * @param localPort The local port to send RTCP data from
     * @param remotePort The remote port to sent RTCP data to
     */
    public synchronized void createAndStartRTCPSenderThread(int localPort,
							    int remotePort)
    {
	m_RTCPHandler.createAndStartRTCPSenderThread(localPort, remotePort);
    }

    /**
     *   Stops the RTCP Sender thread
     *
     */
    public synchronized void stopRTCPSenderThread()
    {
	m_RTCPHandler.stopRTCPSenderThread();
    }

    /**
     *   Retrieves a source object from the map using the given
     *   SSRC as a key.  If the source does not exist, it is added to the
     *   map and newly created source object is returned.
     *
     *   @param  keySSRC The SSRC to look for in the map, if it doesn't exist
     *                   a new source is created and returned.
     *   @return    The source corresponding the given SSRC, this source may be
     *               extracted from the map or newly created.
     */
    public synchronized Source getSource ( long keySSRC )
    {
	Source s;

	if ( sourceMap.containsKey ( new Long( keySSRC) ) )
	    s = (Source) sourceMap.get ( new Long( keySSRC) );
	else    // source doesn't exist in the map, add it
	    {
		s = new Source ( keySSRC );
		addSource ( keySSRC, s);

	    }

	return s;
    }

    /**
     * Removes a source from the map.
     *
     * @param  sourceSSRC The source with this SSRC has to be removed.
     *
     * @return Always returns zero.
     */
    public synchronized int removeSource ( long sourceSSRC )
    {
	if (sourceMap.get ( new Long(sourceSSRC)) != null )
	    {
		sourceMap.remove ( new Long(sourceSSRC) );
		this.outprintln("Removing Source : " + "SSRC = 0x" +
				Integer.toHexString((int)sourceSSRC));
		this.outprintln("No. of members" + getNumberOfMembers ());
		this.outprintln("No. of senders" + getNumberOfActiveSenders());
	    }
	else
	    {
		System.err.println("Trying to remove SSRC which doesnt exist:"+
				   sourceSSRC );
	    }

	return 0;
    }

    /**
     * Creates and return a InetAddress object.
     *
     * @param multicastAddress    Dotted decimal IP address from which a
     * <b> InetAddress </b> object will be created and returned.
     * @return Desired InetAddress object.
     */
    public synchronized InetAddress getInetAddress ( String multicastAddress )
    {
	InetAddress ia = null;
	try
	    {
		try
            	    {
			ia = InetAddress.getByName ( multicastAddress );
		    }
		catch (UnknownHostException e)
		    {
			// It's ok if the host is unknown.
		    }
	    }
	catch (Exception e)
	    {
		System.err.println (e);
		System.exit (1);
	    }

	return (ia);
    }

    /**
     *   Returns the number of members.
     *
     *   @return Total number of members.
     */
    public int getNumberOfMembers ()
    {
	// Go through the map and return the total number of sources.
	return sourceMap.size();
    }

    /**
     *   Returns the number of active senders.
     *
     *   @return Number of senders.
     */
    public int getNumberOfActiveSenders()
    {
	// Hasttable
	Enumeration sourceCollection  = getSources();
	int i=0;
	while ( sourceCollection.hasMoreElements() )
	    {
		Source s = (Source) sourceCollection.nextElement();

		if ( s.activeSender == true )
		    {
			i++;
		    }
	    }
	return (i);
    }

    /**
     * Calculates the next interval, sets the T and Td class level static
     * variables. <br>
     * <br>
     * Method to calculate the RTCP transmission interval T.
     * from Section A7 Computing the RTCP Transmission Interval
     * ( with minor modifications )
     *
     * @return The Calculated Interval
     */
    public synchronized double calculateInterval ()
    {
	long td = 0;
	// Update T and Td ( same as rtcp_interval() function in rfc.

	int members = getNumberOfMembers();
	int senders = getNumberOfActiveSenders();

	/*
	 * Minimum average time between RTCP packets from this site (in
	 * seconds). This time prevents the reports from `clumping' when
	 * sessions are small and the law of large numbers isn't helping
	 * to smooth out the traffic. It also keeps the report interval
	 * from becoming ridiculously small during transient outages like
	 * a network partition.
	 */
	final long RTCP_MIN_TIME = (long) 5.;

	/*
	 * Fraction of the RTCP bandwidth to be shared among active
	 * senders. (This fraction was chosen so that in a typical
	 * session with one or two active senders, the computed report
	 * time would be roughly equal to the minimum report time so that
	 * we don't unnecessarily slow down receiver reports.) The
	 * receiver fraction must be 1 ­ the sender fraction.
	 */
	final double RTCP_SENDER_BW_FRACTION = (double) 0.25;
	final double RTCP_RCVR_BW_FRACTION = (1-RTCP_SENDER_BW_FRACTION);
	double t; /* interval */
	double rtcp_min_time = RTCP_MIN_TIME;
	double n; /* no. of members for computation */
	/*
	 * Very first call at application start­up uses half the min
	 * delay for quicker notification while still allowing some time
	 * before reporting for randomization and to learn about other
	 * sources so the report interval will converge to the correct
	 * interval more quickly. */
	if (initial)
	    {
		rtcp_min_time /= 2;
	    }
	/*
	 * If there were active senders, give them at least a minimum
	 * share of the RTCP bandwidth. Otherwise all participants share
	 * the RTCP bandwidth equally.
	 */

	n = members;

	if (senders > 0 && senders < members * RTCP_SENDER_BW_FRACTION)
	    {
		if ( getMySource().activeSender )
                    {
                        rtcp_bw *= RTCP_SENDER_BW_FRACTION;
                        n = senders;
                    }
		else
                    {
                        rtcp_bw *= RTCP_RCVR_BW_FRACTION;
                        n -= senders;
                    }
	    }

	/*
	 * The effective number of sites times the average packet size is
	 * the total number of octets sent when each site sends a report.
	 * Dividing this by the effective bandwidth gives the time
	 * interval over which those packets must be sent in order to
	 * meet the bandwidth target, with a minimum enforced. In that
	 * time interval we send one report so this time is also our
	 * average time between reports.
	 */
	t = (double) avg_rtcp_size  *n / rtcp_bw;
	if (t < rtcp_min_time) t = rtcp_min_time;
	/*
	 * To avoid traffic bursts from unintended synchronization with
	 * other sites, we then pick our actual next report interval as a
	 * random number uniformly distributed between 0.5*t and 1.5*t.
	 */
	double noise = (rnd.nextDouble() + 0.5);

	this.td = t;
	this.t = t * (double) noise;
	return t;
    }

    /**
     *   Initialize the Session level variables.
     *
     */
    private int initialize()
    {
	appStartupTime = currentTime();
	timeOfLastRTCPSent =  appStartupTime ;
	tc = currentTime();
	pmembers = 1;
	we_sent = true;
	rtcp_bw = (double) 0.05 * (double) this.bandwidth;
	initial = true;
	avg_pkt_sz = 0;
	// TODO: Set avg_pkt_sz to the size of the first packet generated by
	//       app

	ssrc = (long) Math.abs( rnd.nextInt() ) ;

	packetCount = 0;
	octetCount = 0;


	// Set the next transmission time to the interval
	tn = t;

	// Add self as a source object into the SSRC table maintained by the
	// session
	this.addSource ( this.ssrc, new Source( this.ssrc) );

	return (0);
    }

    /**
     *   Returns a self source object.
     *
     *   @return My source object.
     */
    public synchronized Source getMySource()
    {
	Source s = (Source) sourceMap.get ( new Long( ssrc ) );
	return s;

    }

    /**
     * Adds an SSRC into the table, if SSRC Exists, error code -1
     * is returned.
     *
     * @param  newSSRC SSRC of the source being added.
     * @param  src     Source object of the source being added.
     * @return -1 if the source with this SSRC already exists, 1 otherwise
     * (source added).
     */
    public int addSource ( long newSSRC , Source src)
    {

	if ( sourceMap.containsKey ( new Long ( newSSRC) ) )
	    {
		return -1;
	    }
	else
	    {
		sourceMap.put ( new Long ( newSSRC), src );
		this.outprintln("Adding Source : " + "SSRC = 0x" +
				Integer.toHexString((int)newSSRC));
		this.outprintln("No. of members" + getNumberOfMembers ());
		this.outprintln("No. of senders" + getNumberOfActiveSenders());

	    }
	return 1;
    }

    /**
     *   Returns all active senders as an iterable enumeration.
     *
     *   @return Enumeration of all active senders.
     */
    public synchronized Enumeration getActiveSenders ()
    {
	Enumeration enumAllMembers = sourceMap.elements();

	Vector vectActiveSenders = new Vector();

	Source s;

	// Go through this enumeration and for each source
	// if it is and active sender, add into a temp vector
	while ( enumAllMembers.hasMoreElements() )
	    {
		s = (Source) enumAllMembers.nextElement();
		if ( s.activeSender )
		    vectActiveSenders.addElement ( s );
	    }

	// Return the enumeration of the temp vector.
	return ( vectActiveSenders.elements() );
    }

    /**
     *   Return an iterable enumeration of all sources
     *   contained in the Map.
     *
     *   @return Enumeration of all the sources (members).
     */
    public synchronized Enumeration getSources ()
    {
	return sourceMap.elements();
    }

    /**
     *   Returns current time from the Date().getTime() function.
     *
     *   @return The current time.
     */
    public long currentTime()
    {
	tc = (new Date()).getTime();
	return (long)tc;
    }

    /**
     *   Function removes all sources from the members table (except self).
     *   Returns number of sources removed.
     *
     *   @return Number of sources successfully removed.
     */
    public synchronized int removeAllSources()
    {
	Enumeration sourceCollection  = getSources();

	int n = 0;

	while ( sourceCollection.hasMoreElements() )
	    {
		Source s = (Source) sourceCollection.nextElement();

		if ( s.ssrc != ssrc )
                    {
                        removeSource ( s.ssrc );
                        n++;
                    }
	    }

	pmembers = 1;
	calculateInterval();

	return (n);
    }

    /**
     *   Register RTCP action listener.
     *   The instantiators of the session who are interested
     *   in getting RTCP information, may wish to implement
     *   the RTCP_actionListener interface and then call
     *   this function to register themselves so that the
     *   incoming RTCP information can be handed over to them.
     *
     *   @param listener who implements the RTCP_actionListener interface and
     *           will be the one to which all RTCP actions will be posted.
     */
    public synchronized void addRTCP_actionListener
	(RTCP_actionListener listener)
    {
	m_RTCP_actionListener = listener;
    }

    /**
     *   Register RTP action listener.
     *   The instantiators of the session must implement
     *   RTP_actionListener and must register with this function
     *   only once.
     *
     *   @param listener who implements the RTP_actionListener interface and
     *           will be the one to which all RTP actions will be posted.
     */
    public synchronized void addRTP_actionListener
	( RTP_actionListener listener )
    {
	m_RTP_actionListener = listener;
    }

    /**
     *   Post the RTCP RR packet to the actionListener. (if any
     *   is registered)
     *
     *   @param    rrpkt The Receiver Report packet received.
     */
    public synchronized void postAction ( RTCPReceiverReportPacket rrpkt )
    {
	if ( m_RTCP_actionListener != null )
	    m_RTCP_actionListener.handleRTCPEvent ( rrpkt );
    }

    /**
     *   Function posts the RTCP SR packet to the actionListener. (if any
     *   is registered)
     *
     *   @param srpkt The sender report packet received.
     */
    public synchronized void postAction ( RTCPSenderReportPacket srpkt )
    {
	if ( m_RTCP_actionListener != null )
	    m_RTCP_actionListener.handleRTCPEvent ( srpkt );

    }

    /**
     *   Function posts the SDES packet to the actionListener. (if any
     *   is registered)
     *
     *   @param sdespkt The SDES packet received.
     */
    public synchronized void postAction ( RTCPSDESPacket sdespkt )
    {
	if ( m_RTCP_actionListener != null )
	    m_RTCP_actionListener.handleRTCPEvent ( sdespkt );
    }

    /**
     *   Function posts the RTCP BYE packet to the actionListener. (if any
     *   is registered)
     *
     *   @param byepkt The BYE packet received.
     */
    public synchronized void postAction ( RTCPBYEPacket byepkt )
    {
	if ( m_RTCP_actionListener != null )
	    m_RTCP_actionListener.handleRTCPEvent ( byepkt );
    }

    /**
     *   Function posts the RTP packet to the actionListener. (if any
     *   is registered).
     *
     *   @param rtppkt The RTP Packet received.
     */
    public synchronized void postAction ( RTPPacket rtppkt )
    {
	if ( m_RTP_actionListener != null )
	    m_RTP_actionListener.handleRTPEvent ( rtppkt );
	else
	    System.err.println ("ERROR: No RTP Action Listener registered :(");
    }

    /**
     * Print a newline. Provided to enable the
     * debug console print messages in the source code. By setting the
     * DebugOutput flag,
     * user can control the debug message output.
     *
     * @see Session#debugOutput
     */
    public synchronized void outprintln () {
	if ( debugOutput ) System.out.println ();
    }


    /**
     * Print a string. Provided to enable the
     * debug console print messages in the source code.  By setting the
     * DebugOutput flag, user can control the debug message output.
     *
     * @param s The string to print out
     *
     * @see Session#debugOutput
     */
    public synchronized void outprintln ( String s ) {
	if ( debugOutput ) System.out.println ( s );
    }

    /**
     * Print a string. Provided to enable the
     * debug console print messages in the source code.  By setting the
     * DebugOutput flag, user can control the debug message output.
     *
     * @param s The string to print out
     *
     * @see Session#debugOutput
     */
    public synchronized void outprint ( String s ) {
	if ( debugOutput ) System.out.print ( s );
    }

    /**
     * Sends an RTP Packet.
     *
     * @param packet The RTP packet
     */
    // IP: Changed parameter from byte [] data to RTPPacket packet
    public void sendRTPPacket ( RTPPacket packet )
    {
	if ( m_RTPHandler != null )
	    m_RTPHandler.sendPacket( packet );
	else
	    System.err.println ( "ERROR: Cannot send RTP data if " +
				 " RTPHandler not instantiated.");
    }

    /**
     * Starts the threads. Starts the RTCP sender and receiver threads and the
     * RTP receiver thread.  This function calls the thread startup functions
     * which could also be called manually if needed.  This function is an
     * alternative for calling the following: <br>
     * <br>
     * StartRTCPSenderThread(); <br>
     * StartRTCPReceiverThread(); <br>
     * StartRTPReceiverThread(); <br>
     *
     * @param rtpLocalPort The local RTP port
     * @param rtcpLocalPort The local RTCP port
     * @param rtpRemotePort The remote RTP port
     * @param rtcpRemotePort The remote RTCP port
     *
     * @see Session#startRTPThread
     */
    public void start (MulticastSocket socket,int rtpLocalPort, int rtcpLocalPort, int rtpRemotePort,
		       int rtcpRemotePort)
    {
	createAndStartRTCPSenderThread(rtcpLocalPort, rtcpRemotePort);
	createAndStartRTCPReceiverThread(rtcpLocalPort);
	openRTPReceiveSocket(rtpLocalPort);

	openRTPTransmitSocket(rtpLocalPort,rtpRemotePort);
	startRTPThread();
    }

    /**
     * Stops the session and terminates the RTCP sending loop by forcing a bye
     * packet.<
     *
     * @see Session#stopRTCPSenderThread
     */
    public void stop()
    {
	stopRTCPSenderThread();
	stopRTCPReceiverThread();
	// IP: Added following method call so that send socket is
	// properly closed
	m_RTPHandler.stop();
	stopRTPThread();
    }

}

