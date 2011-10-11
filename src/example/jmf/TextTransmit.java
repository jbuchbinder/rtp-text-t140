/*
 * T.140 Presentation Library
 * 
 * Copyright (C) 2004 Board of Regents of the University of Wisconsin System
 * (Univ. of Wisconsin-Madison, Trace R&D Center)
 * Copyright (C) 2004 Omnitor AB
 *
 * This software was developed with support from the National Institute on
 * Disability and Rehabilitation Research, US Dept of Education under Grant
 * # H133E990006 and H133E040014  
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * Please send a copy of any improved versions of the library to: 
 * Gunnar Hellstrom, Omnitor AB, Renathvagen 2, SE 121 37 Johanneshov, SWEDEN
 * Gregg Vanderheiden, Trace Center, U of Wisconsin, Madison, Wi 53706
 *
 */
import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.net.UnknownHostException;
import javax.media.Codec;
import javax.media.Format;
import javax.media.Processor;
import javax.media.MediaLocator;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.NoProcessorException;
import javax.media.UnsupportedPlugInException;
import javax.media.control.TrackControl;
import javax.media.format.UnsupportedFormatException;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.InvalidSessionAddressException;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.rtcp.SourceDescription;
import se.omnitor.media.protocol.text.t140.TextPacketizer;
import se.omnitor.media.content.text.t140.TextPlayer;

/**
 * This class transmits text/t140 over RTP using JMF.
 * 
 * @author Erik Zetterstrom, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
class TextTransmit {

    private DataSource inputDs;
    private String ipAddress;
    private int localPort;
    private int remotePort;
    private int redGen;
    private int t140Pt;
    private int redPt;
    private Thread transmitThread;
    private Logger logger;
    private Processor processor;
    private boolean started;
    private RTPManager[] rtpMgrs = null;
    private DataSource outputDs;
    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    /**
     * Initializes.
     *
     * @param mediaLocator For text, use "text:stdin", "text:buffer" or
     * "text:gui"
     * @param ipAddress The ip address to send to.
     * @param localPort The local port to use.
     * @param remotePort The remote port to use.
     * @param redGen The number of redundant generations to use.
     * @param t140Pt The payload type to use.
     * @param redPt  The payload type to use for redundancy.
     */
    public TextTransmit(String mediaLocator,
			String ipAddress,
                        int localPort,
                        int remotePort,
                        int redGen,
                        int t140Pt,
                        int redPt) {

	logger = Logger.getLogger("");

        this.ipAddress = ipAddress;
	this.localPort = localPort;
	this.remotePort = remotePort;
        this.redGen = redGen;
        this.t140Pt = t140Pt;
        this.redPt = redPt;

	started = false;
	processor = null;
	outputDs = null;
	
	try {
	    inputDs = 
		javax.media.Manager.createDataSource
		(new MediaLocator(mediaLocator));

	} catch(Exception e) {
	    logger.throwing(this.getClass().getName(), "<init>", e);
	    return;
	}   

	// Set number of redundant generations to data source
	if (redGen > 0 && 
	    inputDs instanceof 
	    se.omnitor.media.protocol.text.t140.DataSource) {
	    
	    ((se.omnitor.media.protocol.text.t140.DataSource)inputDs).
		setRedGen(redGen);
	}

        try {
            inputDs.connect();
        } catch (java.io.IOException ioe) {
	    logger.throwing(this.getClass().getName(), "Constructor", ioe);
	    System.exit(-1);
        }
    

	// MOVE TO T.140 LIBRARY
	//
        //Transmit the the character ZERO WIDTH NO BREAK SPACE
        //on session start, as defined in T.140
        //synchObject.set(TextConstants.ZERO_WIDTH_NO_BREAK_SPACE);

    }

    /**
     * Starts the transmission.
     *
     */
    public synchronized void start() {

        if (!createProcessor()) {
	    logger.severe("Could not create processor!");
            return;
        }

        // Create an RTP session to transmit the output of the
        // processor to the specified IP address and port no.
        if (!createTransmitter()) {
	    logger.severe("Failed to create transmitter!");
            processor.close();
            processor = null;
            return;
        }    

        started = true;

        // Start the transmission
        processor.start();
        return;
    }

    /**
     * Stops the transmission if already started
     *
     */
    public void stop() {
        synchronized (this) {
            if (processor != null) {
                processor.stop();
                processor.close();
                processor = null;
                for (int i = 0; i < rtpMgrs.length; i++) {
                    rtpMgrs[i].removeTargets("Session ended.");
                    rtpMgrs[i].dispose();
                }
            }
        }
    }

    /**
     * Creates a processor
     *
     * @returns True if processor was created, false if something went wrong
     */
    private boolean createProcessor() {

        DataSource ds;
        DataSource clone;

        // Try to create a processor
	DataSource d = null;
	try {
	    processor = javax.media.Manager.createProcessor(inputDs);
	}
	catch (IOException ioe) {
	    logger.throwing(this.getClass().getName(), "createProcessor", ioe);
	    return false;
	}
	catch (NoProcessorException npe) {
	    logger.throwing(this.getClass().getName(), "createProcessor", npe);
	    return false;
	}

        // Wait for it to configure
        boolean result = waitForState(processor, Processor.Configured);
        if (result == false) {
	    logger.severe("Couldn't configure processor!");
	    return false;
        }

        // Get the tracks from the processor
        TrackControl [] tracks = processor.getTrackControls();

        // Do we have atleast one track?
        if (tracks == null || tracks.length < 1) {
	    logger.severe("Couldn't fins tracks in processor!");
	    return false;
        }

        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW);
        processor.setContentDescriptor(cd);

        Format[] supported;
        Format chosen;
        boolean atLeastOneTrack = false;

        // Program the tracks.
        for (int i = 0; i < tracks.length; i++) {

	    // Add packetizer to codec chain
            Format format = tracks[i].getFormat();
            Codec[] chain = 
		new Codec[] { new TextPacketizer(t140Pt, redPt, redGen) };

            try {
                tracks[i].setCodecChain(chain);    
                tracks[i].setRenderer(new TextPlayer(null));
	    }
	    catch (UnsupportedPlugInException upie) {
		logger.throwing(this.getClass().getName(), "createProcessor",
				upie);
	    }
            if (tracks[i].isEnabled()) {

                supported = tracks[i].getSupportedFormats();

                if (supported.length > 0) {

                    chosen = supported[0];

                    for (int j = 0; j < supported.length; j++) {
                        if ((supported[j]).matches(format)) {
                            chosen = supported[j];
                            break;
                        }
                    }

                    if (!chosen.matches(format)) {
			logger.severe("The input track cannot be " +
				      "transmitted as the custom payload!");
                    }
                    tracks[i].setFormat(chosen);
		    logger.fine("The track is set to transmit as: " + chosen);
                    atLeastOneTrack = true;
                } else {
		    logger.severe("The input track cannot be transmitted!");
                    tracks[i].setEnabled(false);
                }
            } else {
                tracks[i].setEnabled(false);
            }
        }

        if (!atLeastOneTrack) {
	    logger.severe("Couldn't set any of the tracks to a valid RTP " +
			  "format!");
	    return false;
        }

  
        result = waitForState(processor, Controller.Realized);
        if (result == false) {
	    logger.severe("Couldn't realize processor!");
	    return false;
        }

        // Get the output data source of the processor
        outputDs = processor.getDataOutput();

        return true;
    }


    /**
     * Use the RTPManager API to create sessions for each media 
     * track of the processor.
     *
     * @return True if transmitter was created, false if something went wrong.
     */
    private boolean createTransmitter() {

        PushBufferDataSource pbds = (PushBufferDataSource)outputDs;
        PushBufferStream[] pbss = pbds.getStreams();

        rtpMgrs = new RTPManager[pbss.length];
        SessionAddress localAddr;
        SessionAddress destAddr;
        InetAddress ipAddr;
        SendStream sendStream=null;
        int rPort;
        int lPort;
        SourceDescription[] srcDesList;
	
	InetAddress localHost;

	try {
	    ipAddr = InetAddress.getByName(ipAddress);
	    localHost = InetAddress.getLocalHost();
	}
	catch (UnknownHostException uhe) {
	    logger.throwing(this.getClass().getName(), "createTransmitter", 
			    uhe);
	    return false;
	}

	// This variable indicates if there is at least one successfully
	// created sendStream.
	boolean createdOk = false;

        for (int i = 0; i < pbss.length; i++) {
	    rtpMgrs[i] = RTPManager.newInstance();        
	    
	    rPort = remotePort + 2*i;
	    lPort = localPort + 2*i;

	    localAddr = new SessionAddress(localHost, lPort);
	    
	    destAddr = new SessionAddress( ipAddr, rPort);
	    
	    try {
                rtpMgrs[i].initialize(localAddr);
                rtpMgrs[i].addTarget(destAddr);
		logger.fine("Created RTP session to " + ipAddress + 
			    ", ports: " + rPort + "(r), " + lPort + "(l)");
	    }
	    catch (InvalidSessionAddressException isae) {
		logger.throwing(this.getClass().getName(), "createTransmitter",
				isae);
	    }
	    catch (IOException ioe) {
		logger.throwing(this.getClass().getName(), "createTransmitter",
				ioe);
	    }
        
	    PushBufferStream[] str = 
		((PushBufferDataSource)outputDs).getStreams();
        
	    int payload;
	    if (redGen == 0) {
		payload = t140Pt;
	    }
	    else {
		payload = redPt;
	    }
	    
	    rtpMgrs[i].addFormat(str[i].getFormat(), payload);

	    try {
                sendStream = rtpMgrs[i].createSendStream(outputDs, i);
		sendStream.start();
		createdOk = true;
	    }
	    catch (UnsupportedFormatException ufe) {
		logger.throwing(this.getClass().getName(), "createTransmitter",
				ufe);
	    }
	    catch (IOException ioe) {
		logger.throwing(this.getClass().getName(), "createTransmitter",
				ioe);
	    }

        }
        return createdOk;
    }


    /**
     * Determines if the transmission has been started.
     *
     * @return true if transmission has started, false otherwise
     */
    public boolean isStarted() {
        return started;
    }

    /****************************************************************
     * Convenience methods to handle processor's state changes.
     ****************************************************************/

    /**
     * Gets the state lock
     *
     * @return The state lock.
     */
    public Integer getStateLock() {
        return stateLock;
    }

    /**
     * Indicates that the transmission has failed in some way.
     *
     */
    public void setFailed() {
        failed = true;
    }
    
    /**
     * Waits for processor to reach a given state.
     *
     * @param p The processor
     * @param state The state
     */
    private synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(new StateListener());
        failed = false;

        // Call the required method on the processor
        if (state == Processor.Configured) {
            p.configure();
        } else if (state == Processor.Realized) {
            p.realize();
        }
    
        // Wait until we get an event that confirms the
        // success of the method, or a failure event.
        // See StateListener inner class
        int pState=p.getState();
        while (pState < state && !failed) {
            synchronized (getStateLock()) {
                try {
                    getStateLock().wait();
                } catch (InterruptedException ie) {
                    return false;
                }
            }
            pState=p.getState();
        }

        if (failed) {
            return false;
        }
        else {
            return true;
        }
    }

    /****************************************************************
     * Inner Classes
     ****************************************************************/

    class StateListener implements ControllerListener {

        /**
         * Handles state shanges.
         * 
         * @param ce The event
         */
        public void controllerUpdate(ControllerEvent ce) {

            // If there was an error during configure or
            // realize, the processor will be closed
            if (ce instanceof ControllerClosedEvent) {
                setFailed();
            }

            // All controller events, send a notification
            // to the waiting thread in waitForState method.
            if (ce instanceof ControllerEvent) {
                synchronized (getStateLock()) {
                    getStateLock().notifyAll();
                }
            }
        }
    }

}


