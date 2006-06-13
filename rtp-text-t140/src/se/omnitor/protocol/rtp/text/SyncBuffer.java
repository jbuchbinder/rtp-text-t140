/*
 * RTP text/t140 Library
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
package se.omnitor.protocol.rtp.text;

import se.omnitor.util.FifoBuffer;

// import LogClasses and Classes
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RFC 4103 states that text should be buffered 300 ms before sending. If
 * no text has been written in 300 ms and we have redundant data that should
 * be sent, empty data should be appended. <br>
 * <br>
 * All this is handled by this class. <br>
 * <br>
 * Text should be sent from GUI to this class, and data should be read by
 * the RTP sender. <br>
 * <br>
 * All data added to this class must be in T.140 format. <br>
 *
 * @author Andreas Piirimets, Omnitor AB
 */
public class SyncBuffer extends FifoBuffer implements Runnable {

    private byte[] dataWaiting;
    private byte[] dataToSend;
    private int redGensToSend;
    private int redGen;
    private int bufferTime;
    private boolean running;
    private Thread thread;
    private Integer dataSetSemaphore;

    // declare package and classname
    public final static String CLASS_NAME = SyncBuffer.class.getName();
    // get an instance of Logger
    private static Logger logger = Logger.getLogger(CLASS_NAME);


    /**
     * Initializes.
     *
     * @param redGen Number of redundant generations. A value of zero turns
     * off redundancy.
     * @param bufferTime The number of milliseconds to buffer data. According
     * to RFC 4103, this SHOULD be 300 ms.
     */
    public SyncBuffer(int redGen, int bufferTime) {

        // write methodname
        final String METHOD = "SyncBuffer(int redGen, int bufferTime)";
        // log when entering a method
        logger.entering(CLASS_NAME, METHOD);

        this.redGen = redGen;
	this.bufferTime = bufferTime;

	dataSetSemaphore = new Integer(0);

	dataWaiting = new byte[0];
	dataToSend = new byte[0];
	redGensToSend = 0;

	running = false;
	thread = null;

        logger.exiting(CLASS_NAME, METHOD);
    }

    public void start() {

        // write methodname
        final String METHOD = "start()";
        // log when entering a method
        logger.entering(CLASS_NAME, METHOD);


	if (!running) {
	    running = true;

	    thread = new Thread(this);
	    thread.start();
	}

        logger.exiting(CLASS_NAME, METHOD);
    }

    public void stop() {
	running = false;
	thread.interrupt();
    }

    /**
     * Sets new data, this should be called from GUI. If data exists it is
     * appended to the existing data.
     *
     * @param newData The data to set/append.
     *
     * @todo Backspace handling - If a backspace is in the middle of the
     * buffer, remove characters from buffer instead of sending backspace.
     */
    public synchronized void setData(byte[] newData) {
	synchronized (dataSetSemaphore) {
	    byte[] temp = null;

	    if (dataWaiting.length == 0) {
		dataWaiting = newData;
	    }
	    else {
		temp = dataWaiting;
		dataWaiting = new byte[temp.length + newData.length];
		System.arraycopy(temp, 0, dataWaiting, 0, temp.length);
		System.arraycopy(newData, 0, dataWaiting, temp.length,
				 newData.length);
	    }

	    /*
	      int arrayCnt = temp.length;
	      int cnt;
	      for (cnt=0; cnt<data.length; cnt++) {
	      if (data[cnt] == TextConstants.BACKSPACE &&
	      arrayCnt > 0 &&
	      (int)this.data[arrayCnt-1] != 8) {

	      arrayCnt--;
	      }
	      else {
	      this.data[arrayCnt] = data[cnt];

	      arrayCnt++;
	      }
	      }

	      if (arrayCnt != cnt+temp.length) {
	      temp = this.data;
	      this.data = new byte[arrayCnt];
	      System.arraycopy(temp, 0, this.data, 0, arrayCnt);
	      }
	    */

	    dataSetSemaphore.notify();
	}

    }


    /**
     * Gets the data of this object.
     * Data is consumed it is retrieved.
     * This method blocks until data is available.
     *
     * @throws InterruptedException If the wait was interrupted.
     * @return The data.
     */
    public synchronized byte[] getData() throws InterruptedException {
	byte[] temp;

	wait();

	temp = dataToSend;
	dataToSend = new byte[0];

	return temp;
    }

    /**
     * Empties the buffers.
     *
     */
    public synchronized void empty() {
	dataWaiting = new byte[0];
	dataToSend = new byte[0];
    }

    /**
     * Handles buffer times.
     *
     * @todo CPS handling - According to RFC 4103, we must respect remote's
     * CPS demand.
     */
    public void run() {

        // write methodname
        final String METHOD = "run()";
        // log when entering a method
        logger.entering(CLASS_NAME, METHOD);


	while (running) {

	    try {
		synchronized (dataSetSemaphore) {

		    if (dataToSend.length == 0) {
			dataSetSemaphore.wait();

		    }
		}
	    }
	    catch (InterruptedException ie) {
	    }

	    while (dataWaiting.length > 0 || redGensToSend > 0) {
		try {
		    Thread.sleep(bufferTime);
		}
		catch (InterruptedException ie) {
		}

		synchronized (this) {
		    logger.logp(Level.FINEST, CLASS_NAME, METHOD, "dataWaiting to be sent in future", dataWaiting.toString());
                    logger.logp(Level.FINEST, CLASS_NAME, METHOD, "dataWaiting to be sent now", dataToSend.toString());
                    if (dataWaiting.length > 0) {

			if (dataToSend.length > 0) {
			    byte[] temp = dataToSend;
			    dataToSend =
				new byte[temp.length + dataWaiting.length];
			    System.arraycopy(temp, 0, dataToSend, 0,
					     temp.length);
			    System.arraycopy(dataWaiting, 0, dataToSend,
					     temp.length, dataWaiting.length);
			}
			else {
			    dataToSend = dataWaiting;
			}

			dataWaiting = new byte[0];
			notify();
			redGensToSend = redGen;
		    }
		    else if (redGensToSend > 0) {

			notify();

			if (dataToSend.length == 0) {
			    redGensToSend--;
			}
		    }
		}
	    }

	}

        logger.exiting(CLASS_NAME, METHOD);
    }

    /**
     * Sets the number of redundant generations.
     *
     * @param redGen The number of redundant generations to use, a value of
     * zero disables redundancy.
     */
    public void setRedGen(int redGen) {
	this.redGen = redGen;
    }


    /**
     * Sets the buffer time.
     *
     * @param bufferTime The buffer time
     */
    public void setBufferTime(int bufferTime) {
	this.bufferTime = bufferTime;
    }

    /**
     * Gets the buffer time.
     *
     * @return The buffer time.
     */
    public int getBufferTime() {
	return bufferTime;
    }
}



