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
import java.awt.Component;
import java.awt.TextArea;

import java.io.IOException;
import java.util.logging.Logger;

import javax.media.Codec;
import javax.media.Control;
import javax.media.Controller;
import javax.media.ControllerClosedEvent;
import javax.media.ControllerErrorEvent;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoProcessorException;
import javax.media.Owned;
import javax.media.Player;
import javax.media.Processor;
import javax.media.RealizeCompleteEvent;
import javax.media.Renderer;
import javax.media.format.AudioFormat;
import javax.media.format.FormatChangeEvent;
import javax.media.format.VideoFormat;
import javax.media.control.BufferControl;
import javax.media.control.TrackControl;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.Participant;
import javax.media.rtp.ReceiveStream;
import javax.media.rtp.ReceiveStreamListener;
import javax.media.rtp.RTPConnector; //Skip later?
import javax.media.rtp.RTPControl;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.SessionListener;
import javax.media.rtp.event.ByeEvent;
import javax.media.rtp.event.NewParticipantEvent;
import javax.media.rtp.event.NewReceiveStreamEvent;
import javax.media.rtp.event.ReceiveStreamEvent;
import javax.media.rtp.event.RemotePayloadChangeEvent;
import javax.media.rtp.event.SessionEvent;
import javax.media.rtp.event.StreamMappedEvent;

import java.net.InetAddress;

import java.util.Vector;

import se.omnitor.media.protocol.text.t140.TextPacketizer;
import se.omnitor.media.protocol.text.t140.TextDePacketizer;
import se.omnitor.media.protocol.text.t140.TextFormat;
import se.omnitor.media.content.text.t140.TextPlayer;
import se.omnitor.util.FifoBuffer;

/**
 * Receives RTP streams that contains text according to RFC2793 using JMF.
 * 
 * @author Erik Zetterstrom, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
class TextReceive implements ReceiveStreamListener, 
			     SessionListener,
			     ControllerListener,
			     Runnable {

    private int t140Pt;
    private int redPt;
    private boolean useRed;
    private int port;
    private Thread receiveThread;
    private RTPManager[] mgrs = null;
    private Logger logger;
    private Object dataSync = new Object();
    private boolean dataReceived = false;
    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    // ??
    private String[] sessions;
    private Vector playerStreamContainers = null;
    private TextPlayer textPlayer = null;
    private int captureBufSize=0;
    private Processor processor = null;
    private int renderBufSize=0;

    /*
 

    
    private TextFormat textFormat;

    private String addr = ""; //Address to accept connection from

    private int red =0;


    private DataSource dsOutput = null;
    private PushBufferDataSource pbdsOutput = null;
    private TextBufferDataSource tbdsOutput = null;
    
    private RTPConnector connector = null;


    */


    /**
     * Initializes.
     *
     * @param ipAdress The ip to receive from.
     * @param port The port to receive on.
     * @param t140Pt The payload type to use for t140
     * @param useRed If true redundancy is enabled, false otherwise.
     * @param redPt The payload type to use for redundancy.
     */       
    public TextReceive(String ipAddress,
                       int port,
                       int t140Pt,
                       boolean useRed,
                       int redPt,
		       FifoBuffer fifo) {

	logger = Logger.getLogger("");

        this.port = port;
        this.t140Pt = t140Pt;
        this.useRed = useRed;
        this.redPt = redPt;

        sessions = new String[] { ipAddress + "/" + port + "/50" };
    
        playerStreamContainers = new Vector(0,1);

	textPlayer = new TextPlayer(fifo);

    }

    /**
     * Starts the receiver.
     *
     */
    public void start() {
        receiveThread = new Thread(this);
        receiveThread.start();
    }

    /**
     * Does all the work.
     *
     */
    public void run() {

        Object o = Manager.getHint(Manager.PLUGIN_PLAYER);

        try {
            InetAddress ipAddr;
            SessionAddress localAddr = new SessionAddress();
            SessionAddress destAddr;

            mgrs = new RTPManager[sessions.length];

            SessionLabel session;

            // Open the RTP sessions.
            for (int i = 0; i < sessions.length; i++) {

                // Parse the session addresses.
                try {
                    session = new SessionLabel(sessions[i]);
                } catch (IllegalArgumentException e) {
		    logger.severe("Failed to parse the session address"+
				  " given: " + sessions[i]);
                    return;
                }

		logger.fine("Open RTP session for: addr: " + 
			    session.getAddr() + 
			    " port: " + session.getPort() + 
			    " ttl: " + session.getTTL());


                mgrs[i] = (RTPManager) RTPManager.newInstance();
                BufferControl bc=(BufferControl)mgrs[i].
                    getControl("javax.media.control.BufferControl");
                if (bc != null) {
                    bc.setBufferLength(0);
                    bc.setMinimumThreshold(0);
                    bc.setEnabledThreshold(true);
                }
                TextFormat tf = null;
                if (useRed) {
                    tf = new TextFormat("RED/RTP");
		    mgrs[i].addFormat(tf, redPt);
                }
                else {
                    tf = new TextFormat("UTF8/RTP");
		    mgrs[i].addFormat(tf, t140Pt);
                }
                mgrs[i].addSessionListener(this);
                mgrs[i].addReceiveStreamListener(this);

                ipAddr = InetAddress.getByName(session.getAddr());

                if (ipAddr.isMulticastAddress()) {
                    // local and remote address pairs are identical:
                    localAddr = new SessionAddress( ipAddr,
						    session.getPort(),
						    session.getTTL());
                    destAddr = new SessionAddress( ipAddr,
                                                   session.getPort(),
                                                   session.getTTL());
                } else {

                    localAddr = new SessionAddress( InetAddress.getLocalHost(),
						    session.getPort());
                    destAddr = new SessionAddress( ipAddr, session.getPort());
		}
         
                mgrs[i].initialize(localAddr);
  
                mgrs[i].addTarget(destAddr);
            }

        } catch (Exception e){
	    logger.throwing(this.getClass().getName(), "run", e);
            return;
        }

        // Wait for data to arrive before moving on.

        long then = System.currentTimeMillis();
        long waitingPeriod = 60000;  // wait for a maximum of 60 secs.

        try{
            synchronized (dataSync) {
                while (!dataReceived && true) {
                    if (!dataReceived) {
                        dataSync.wait(1000);
                    }
                }
            }
        } catch (Exception e) {
            //Ignore interruptions
        }

        if (!dataReceived) {
            logger.info("No RTP data was received.");
            close();
            return;
        }

        return;
    }
    
    /**
     * Close the players and the session managers.
     */
    protected void close() {

        // close the RTP session.
        for (int i = 0; i < mgrs.length; i++) {
            if (mgrs[i] != null) {
                mgrs[i].removeTargets( "Closing session from AVCustomRecv");
                mgrs[i].dispose();
                mgrs[i] = null;
            }
        }
        mgrs = null;
    }

    /**
     * SessionListener.
     *
     * @param evt The event.
     */
    public synchronized void update(SessionEvent evt) {
        if (evt instanceof NewParticipantEvent) {
            Participant p = ((NewParticipantEvent)evt).getParticipant();
            logger.fine("A new participant had just joined: " + p.getCNAME());
        }
    }
    
    /**
     * ReceiveStreamListener
     *
     * @param evt The event
     */
    public synchronized void update( ReceiveStreamEvent evt) {

        RTPManager mgr = (RTPManager)evt.getSource();
        Participant participant = evt.getParticipant();    // could be null.
        ReceiveStream stream = evt.getReceiveStream();  // could be null.

        if (evt instanceof RemotePayloadChangeEvent) {
     
            logger.severe("Received an RTP PayloadChangeEvent, cannot " +
			  "handle that!");
            System.exit(0);

        }
    
        else if (evt instanceof NewReceiveStreamEvent) {
            try {
                stream = ((NewReceiveStreamEvent)evt).getReceiveStream();
                PushBufferDataSource ds = (PushBufferDataSource)
                    stream.getDataSource();

                Control cont = (Control)ds.
                    getControl("javax.media.control.BufferControl");
        
                if (cont != null) {
                    ((BufferControl)cont).setBufferLength(captureBufSize);
                    ((BufferControl)cont).setMinimumThreshold(0);
                    ((BufferControl)cont).setEnabledThreshold(true);
                }
                PushBufferStream[] pbs = (PushBufferStream[])ds.getStreams();

                RTPControl ctl = (RTPControl)ds.
                    getControl("javax.media.rtp.RTPControl");

                // Check to see if there's a buffer control
                // on the data source.
                // It could be that we are using a capture data source.
                Control c = (Control)ds.
                    getControl("javax.media.control.BufferControl");
                if (c != null) {
                    ((BufferControl)c).setBufferLength(captureBufSize);
                    ((BufferControl)cont).setMinimumThreshold(0);
                    ((BufferControl)cont).setEnabledThreshold(true);
                }


                if (ctl != null){
                    logger.fine("Recevied new RTP stream: " + ctl.getFormat());
                } else {
                    logger.fine("Recevied new RTP stream");
                }
                if (participant == null) {
                    logger.fine("The sender of this stream is yet " +
				"to be identified.");
                }
                else {
                    logger.fine("The stream comes from: " + 
				participant.getCNAME()); 
                }

                try {
                    processor = javax.media.Manager.createProcessor(ds);
                } catch (NoProcessorException npe) {
		    logger.throwing(this.getClass().getName(), "update", npe);
                    return;
                } catch (IOException ioe) {
		    logger.throwing(this.getClass().getName(), "update", ioe);
                    return;
                } 

                boolean result = waitForState(processor, Processor.Configured);
    
                if (result == false) {
                    logger.severe("Couldn't configure processor");
                    return;
                }
                processor.setContentDescriptor(null);
            
                // Get the tracks from the processor
                TrackControl [] tracks = processor.getTrackControls();
        
                // Do we have atleast one track?
                if (tracks == null || tracks.length < 1) {
                    logger.severe("Couldn't find tracks in processor");
                    return; 
                }
        
                Format[] supported;
                Format chosen;
                boolean atLeastOneTrack = false;
        
                // Program the tracks.
                Codec[] chain = 
		    new Codec[] { new TextDePacketizer(t140Pt, redPt, useRed)};

                for (int i = 0; i < tracks.length; i++) {
                    Format format = tracks[i].getFormat();
                    try {
                        tracks[i].setCodecChain(chain);    
                        tracks[i].setRenderer(textPlayer);
                    } catch(Exception e) {
			logger.throwing(this.getClass().getName(), "update",
					e);
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
                                logger.fine("The input track cannot be "+
					    "received as the custom payload:");
                            }
                            tracks[i].setFormat(chosen);
                            logger.fine("The track is received as: " + chosen);
                            atLeastOneTrack = true;
                        } else {
                            logger.fine("The input track cannot be received.");
                            tracks[i].setEnabled(false);
                        }
                    } else {
                        tracks[i].setEnabled(false);
                    }
                }
        
                if (!atLeastOneTrack) {
                    logger.severe("Couldn't set any of the tracks "+
				  "to a valid RTP format.");
                    return;
                }
        
                result = waitForState(processor, Controller.Realized);
                if (result == false) {
                    logger.severe("Couldn't realize processor");
                    return;
                }

        
                // After the processor has been realized, we can now set the
                // renderer's buffer size.  We need to do this before the
                // processor is prefetched.
                // We need to loop the array of controls to make sure that we 
                // are setting the size of the correct buffer control since
                // the DataSource's controls are also included in the list.
                Control[] cs = processor.getControls();
                Object owner = null;
        

                for (int i = 0; i < cs.length; i++) {
                    if (cs[i] instanceof Owned && 
                        cs[i] instanceof BufferControl) {
                        owner = ((Owned)cs[i]).getOwner();
                        if (owner instanceof Renderer) {
                            ((BufferControl)cs[i]).
                                setBufferLength(renderBufSize);
                            ((BufferControl)cs[i]).setMinimumThreshold(0);
                            ((BufferControl)cs[i]).setEnabledThreshold(true);
                        }
                    }
                }
                dataReceived=true;
                textPlayer.start();
                processor.start();
        
        
            } catch (Exception e) {
		logger.throwing(this.getClass().getName(), "update", e);
                return;
            }
        
            return;
        
        }
    
        else if (evt instanceof StreamMappedEvent) {
    
            if (stream != null && stream.getDataSource() != null) {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl)ds.
                    getControl("javax.media.rtp.RTPControl");
                logger.fine("The previously unidentified stream has now " +
			    "been identified as sent by: " + 
			    participant.getCNAME());
            }
        }

        else if (evt instanceof ByeEvent) {

            logger.fine("Got bye from: " + participant.getCNAME());
        
        }

    }

    /**
     * Handles state changes of the player.
     *
     * @param ce The event
     */

    public synchronized void controllerUpdate(ControllerEvent ce) {

        if (ce instanceof ControllerErrorEvent) {
            logger.severe("TextReceive internal error: " + ce);
        }


    }

    /****************************************************************
     * Convenience methods to handle processor's state changes.
     ****************************************************************/
    
    Integer getStateLock() {
        return stateLock;
    }

    void setFailed() {
        failed = true;
    }
    
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

    /**
     * Checks if the reception of text has started.
     *
     * @return true if reception of text has started, false otherwise.
     */
    public boolean isStarted() {
        return dataReceived;
    }

    /****************************************************************
     * Inner Classes
     ****************************************************************/

    class StateListener implements ControllerListener {
    

        /**
         * Handles state changes.
         *
         * @param ce The event that specifies the state change.
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
    
    
    /**
     * A utility class to parse the session addresses.
     *
     */
    class SessionLabel {

        private String addr = null;
        private int port;
        private int ttl = 1;

        SessionLabel(String session) throws IllegalArgumentException {

            int off;
            String portStr = null;
            String ttlStr = null;
            
            if (session != null && session.length() > 0) {
                int sessionLength = session.length();
                char sessionChar  = session.charAt(0);
                while (sessionLength > 1 && sessionChar == '/') {
                    session = session.substring(1);
                    sessionLength=session.length();
                    sessionChar  = session.charAt(0);
                }
            
                // Now see if there's a addr specified.
                off = session.indexOf('/');
                if (off == -1) {
                    if (!session.equals("")) {
                        addr = session;
                    }
                } else {
                    addr = session.substring(0, off);
                    session = session.substring(off + 1);
                    // Now see if there's a port specified
                    off = session.indexOf('/');
                    if (off == -1) {
                        if (!session.equals("")) {
                            portStr = session;
                        }
                    } else {
                        portStr = session.substring(0, off);
                        session = session.substring(off + 1);
                        // Now see if there's a ttl specified
                        off = session.indexOf('/');
                        if (off == -1) {
                            if (!session.equals("")) {
                                ttlStr = session;
                            }
                        } else {
                            ttlStr = session.substring(0, off);
                        }
                    }
                }
            }
        
            if (addr == null) {
                throw new IllegalArgumentException();
            }
        
            if (portStr != null) {
                try {
                    Integer integer = Integer.valueOf(portStr);
                    if (integer != null) {
                        port = integer.intValue();
                    }
                } catch (Throwable t) {
                    throw new IllegalArgumentException();
                }
            } else {
                throw new IllegalArgumentException();
            }

            if (ttlStr != null) {
                try {
                    Integer integer = Integer.valueOf(ttlStr);
                    if (integer != null) {
                        ttl = integer.intValue();
                    }
                } catch (Throwable t) {
                    throw new IllegalArgumentException();
                }
            }
        }
        
        /**
         * Gets the address of this session.
         * 
         * @return The address of this session.
         */
        public String getAddr() {
            return addr;
        }

        /**
         * Gets the port of this session.
         *
         * @return The port of this session.
         */
        public int getPort() {
            return port;
        }

        /**
         * Gets the Time To Live for this session.
         *
         * @return The TTL for this session.
         */
        public int getTTL() {
            return ttl;
        }

    }    

}
