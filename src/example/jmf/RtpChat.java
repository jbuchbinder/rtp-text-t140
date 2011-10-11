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
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.PackageManager;
import javax.media.PlugInManager;
import se.omnitor.media.content.text.t140.TextPlayer;
import se.omnitor.media.protocol.text.t140.TextDePacketizer;
import se.omnitor.media.protocol.text.t140.TextPacketizer;
import se.omnitor.util.FifoBuffer;

/**
 * An example of how to use the RTP text/t140 library together with JMF. <br>
 * <br>
 * This is a RTP chat, where two users can connect to each other and chat
 * through RFC 4103. <br>
 *
 * @author Andreas Piirimets, Omnitor AB
 */
public class RtpChat implements Runnable {

    private Logger logger;
    private TextTransmit tx;
    private TextReceive rx;
    private FifoBuffer rcvBuffer;

    /**
     * Main
     *
     * @param argv Arguments from command line
     */
    public static void main(String argv[]) {

	if (argv.length < 3) {
	    printUsage();
	    System.exit(1);
	}

	int localPort = Integer.parseInt(argv[1]);
	int remotePort = Integer.parseInt(argv[2]);
	boolean useRed;
	if (argv.length > 3) {
	    useRed = argv[3].toUpperCase().equals("RED");
	}
	else {
	    useRed = false;
	}

	RtpChat rtpChat = new RtpChat(argv[0], localPort, remotePort, useRed);
	rtpChat.start();
    }

    /**
     * Prints the usage information.
     *
     */
    private static void printUsage() {
	System.out.println("RtpChat - Copyright (C) 2004 University of " +
			   "Wisconsin-Madison and Omnitor AB");
	System.out.println("");
	System.out.println("Syntax: java RtpChat <remote host> <local port> " +
			   "<remote port> [\"red\"]");
	System.out.println("");
	System.out.println("  <ip address>  The IP address or DNS name to " +
			   "remote host");
	System.out.println("  <local port>  The local port number to " +
			   "receive RTP data to");
	System.out.println("  <remote port> The remote host's port number " +
			   "to send RTP data to");
	System.out.println("  [\"red\"]       Write \"red\" (without " +
			   "quotes) to enable redundancy");
	System.out.println("");
    }

    /**
     * Initializes.
     *
     * @param ipAddress The IP address or DNS name to remote host
     * @param localPort The local port number to receive RTP data to
     * @param remotePort The remote host's port number to send RTP data to
     * @param useRed Indicates whether redundancy should be used
     */
    public RtpChat(String ipAddress, int localPort, int remotePort, 
		   boolean useRed) {

	//
	// Configure logger
	//
	logger = Logger.getLogger("");
	configureLogger();

	//
	// Add content and package prefix
	//
	String myPackagePrefix = new String("se.omnitor");     

	Vector packagePrefix = PackageManager.getProtocolPrefixList();
	if (packagePrefix.indexOf(myPackagePrefix) == -1) {
	    packagePrefix.addElement(myPackagePrefix);
	    PackageManager.setProtocolPrefixList(packagePrefix);
	    PackageManager.commitProtocolPrefixList();
	}
    
	Vector contentPrefix = PackageManager.getContentPrefixList();    
	if (contentPrefix.indexOf(myPackagePrefix) == -1) {
	    contentPrefix.addElement(myPackagePrefix);
	    PackageManager.setContentPrefixList(contentPrefix);
	    PackageManager.commitContentPrefixList();
	}

	//
	// Register text packetizer in JMF
	//
	TextPacketizer tp = new TextPacketizer(0, 0, 0);
	String tpName = tp.getClass().getName();

	PlugInManager.removePlugIn(tpName, PlugInManager.CODEC);
	PlugInManager.addPlugIn(tpName, 
				tp.getSupportedInputFormats(),
				tp.getSupportedOutputFormats(null),
				PlugInManager.CODEC);
	try {
	    PlugInManager.commit();
	}
	catch (IOException ioe) {
	    System.err.println("Could not register text packetizer!");
	    ioe.printStackTrace();
	    System.exit(-1);
	}

	//  
	// Register text depacketizer in JMF
	//
	TextDePacketizer tdp = new TextDePacketizer(0, 0, false);
	String tdpName = tdp.getClass().getName();
	
	PlugInManager.removePlugIn(tdpName, PlugInManager.CODEC);
	PlugInManager.addPlugIn(tdpName,
				tdp.getSupportedInputFormats(),
				tdp.getSupportedOutputFormats(null),
				PlugInManager.CODEC);
	try {
	    PlugInManager.commit();
	}
	catch (IOException ioe) {
	    System.err.println("Could not register text depacketizer!");
	    ioe.printStackTrace();
	    System.exit(-1);
	}

	//
	// Register player in JMF
	//
	TextPlayer tPlayer = new TextPlayer(null);
	String tPlayerName = tPlayer.getClass().getName();

	PlugInManager.removePlugIn(tPlayerName, PlugInManager.RENDERER);
	PlugInManager.addPlugIn(tPlayerName,
				tPlayer.getSupportedInputFormats(),
				tPlayer.getSupportedOutputFormats(null),
				PlugInManager.RENDERER);
	try {
	    PlugInManager.commit();
	}
	catch (IOException ioe) {
	    System.err.println("Could not register text player!");
	    ioe.printStackTrace();
	    System.exit(-1);
	}

	//
	// Initialize transmitter
	//
	tx = new TextTransmit("text.t140:stdin", 
			      ipAddress,
			      localPort+2,
			      remotePort,
			      useRed ? 2 : 0,
			      96, 
			      98);

	//
	// Initialize receiver
	//
	rcvBuffer = new FifoBuffer();
	rx = new TextReceive(ipAddress,
			     localPort,
			     96,
			     useRed,
			     98,
			     rcvBuffer);

    }
      
    /**
     * Configures the logger to write to console.
     *
     */
    private void configureLogger() {

        // Set log level to include all.
        logger.setLevel(Level.INFO);

	// Disable parent handlers
	logger.setUseParentHandlers(false);

	// Activate logging to console
	ConsoleHandler ch = new ConsoleHandler();
	ch.setLevel(Level.INFO);
	logger.addHandler(ch);

    }

    /**
     * Fetching incoming text.
     *
     */
    public void run() {
	
	while (true) {
	    
	    try {
		System.out.print(new String(rcvBuffer.getData()));
	    }
	    catch (InterruptedException ie) {
	    }

	}

    }

    /**
     * Starts the chat session.
     *
     */
    public void start() {

	Thread t = new Thread(this);
	t.start();

	rx.start();
	tx.start();

	System.out.println("Transmission has started, feel free to write!");

    }

}
