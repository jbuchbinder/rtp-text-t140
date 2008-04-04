/*
 * RTP text/t140 Library
 *
 * Copyright (C) 2004-2008 Board of Regents of the University of Wisconsin System
 * (Univ. of Wisconsin-Madison, Trace R&D Center)
 * Copyright (C) 2004-2008 Omnitor AB
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
package se.omnitor.media.protocol.text.t140;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.logging.Logger;
import se.omnitor.protocol.rtp.text.SyncBuffer;

/**
 * Reads from stdin and puts data to a SyncBuffer object. <br>
 * <br>
 * Note that it is not possible to read character by character from stdin
 * with Java, therefore this reader always returns a complete line.
 *
 * @author Andreas Piirimets, Omnitor AB
 */
public class StdInReader implements Runnable {

    private SyncBuffer sb;
    private boolean running;
    private Thread thread;
    private Logger logger;

    public StdInReader(SyncBuffer sb) {
	logger = Logger.getLogger("se.omnitor.media.protocol.text.t140");

	this.sb = sb;
	running = false;
	thread = null;
    }

    public void start() {
	running = true;

	thread = new Thread(this, "StdIn reader");
	thread.start();
    }

    public void stop() {
	running = false;
	thread.interrupt();
    }

    public void run() {

	BufferedReader reader =
	    new BufferedReader(new InputStreamReader(System.in));
	int c;

	while (running) {

	    try {
		c = reader.read();
		if (c != -1) {
		    sb.setData(new byte[] { (byte)c });
		}
	    }
	    catch (IOException ioe) {
		logger.throwing(this.getClass().getName(), "run", ioe);
	    }

	}

    }


}



