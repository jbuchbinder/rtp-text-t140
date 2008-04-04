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
package se.omnitor.protocol.rtp.text;

import se.omnitor.util.FifoBuffer;

//import LogClasses and Classes
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

	private boolean sendOnCR = false;

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

		dataSetSemaphore = Integer.valueOf(0);

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

			thread = new Thread(this, "SyncBuffer");
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

		// write methodname
		final String METHOD = "getData()";
		// log when entering a method
		logger.entering(CLASS_NAME, METHOD);

		byte[] temp = null;

		wait();

		try {

			temp = dataToSend;

			if (sendOnCR) {
				int containsCR = -100;
				// check if temp contains CR, and if it does you get the position of the first element
				containsCR = containsCR(temp);

				// if data does not contain CR
				if (containsCR == -1) {
					temp = new byte[0];
				}
				//  data contains CR
				else {
					logger.logp(Level.FINEST, CLASS_NAME, METHOD, "data contains one or more CR");
					byte[] temp2 = temp;
					int lastPositionOfCR = containsCR;

					// while we don't find the last CR
					while((containsCR = getPositionOfNextCR(temp, lastPositionOfCR))>-1) {
						lastPositionOfCR = containsCR;
					}

					temp = new byte[lastPositionOfCR+3];
					System.arraycopy(temp2, 0, temp, 0, temp.length);

					// if the data ended with an CR
					if(lastPositionOfCR+3 == temp2.length) {
						logger.logp(Level.FINEST, CLASS_NAME, METHOD, "the data ended with an CR");
						dataToSend = new byte[0];
					}
					// else we have to save the data after the last CR
					else {
						logger.logp(Level.FINEST, CLASS_NAME, METHOD, "the data ended with data after last CR");
						dataToSend = new byte[temp2.length -(lastPositionOfCR + 3)];
						System.arraycopy(temp2, lastPositionOfCR+3, dataToSend, 0, dataToSend.length);
					}
				}
			}
			// else don't wait for CR
			else {
				dataToSend = new byte[0];
			}
		}
		catch(Throwable t) {
			logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "unexpected throwable caught (swallowed), probably due to a bug", t);
		}

		return temp;
	}

	/* A Java byte has a value of -128 to 127.  With eight bits and no
     sign, the negative numbers could be represented as
     a value of 128 to 255.  This method makes such a conversion, but
     stores the result in an integer since Java does
     not support unsigned bytes. <p>

     @param aByte The byte with a possibly negative value. <p>
     @return An integer with a value of 0 to 255. <p>
	 */
	public static int byteToPositiveInt( byte aByte )
	{
		int i = aByte ;
		if ( i < 0 ) {
			i += 256 ;
		}
		return i ;
	}

	/**
	 * checks if a bytearray contains a CR (carrige return)
	 * @param byte[] aDataArray the byte array to be examined
	 * @return int first position of the CR, returns -1 if aDataArray is null or
	 * does not contain a CR
	 */
	private int containsCR(byte[] aDataArray) {

		// if null or lenght<3, CR takes at least 3 bytes
		if(aDataArray==null || aDataArray.length<3) {
			return -1;
		}
		// else might contain CR, lets check
		else {
			// check for the value of CR in three bytes
			for (int i = 0; i < aDataArray.length-2; i++) {
				if (byteToPositiveInt(aDataArray[i]) == 0xE2 &&
						byteToPositiveInt(aDataArray[i+1]) == 0x80 &&
						byteToPositiveInt(aDataArray[i+2]) == 0xA8) {
					return i;
				}
			}
			// we have checked the bytearray and it did not contain CR
			return -1;
		}
	}

	/**
	 * checks if for the position of next CR (carrige return) (if any)
	 * @param byte[] aDataArray the byte array to be examined
	 * @param int aStartPosition where to start looking for CR
	 * @return int the first position of a CR from aStartPosition+3,
	 * returns -1 if aDataArray is null or if aStartPosition+3 til end does not contain a CR
	 * */
	private int getPositionOfNextCR(byte[] aDataArray, int aStartPosition) {

		byte[] temp = null;
		int position = -1;
		if(aDataArray==null || aStartPosition>(aDataArray.length-1)) {
			return position;
		}
		else {
			temp = new byte[aDataArray.length-(aStartPosition+3)];
			System.arraycopy(aDataArray, aStartPosition+3, temp, 0, temp.length);
			position = containsCR(temp);
			if(position==-1) {
				return position;
			}
			else {
				return aStartPosition+position+3;
			}
		}
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

					if (dataWaiting.length == 0) {
						dataSetSemaphore.wait(55000);
					}
				}
			}
			catch (InterruptedException ie) {
				logger.logp(Level.FINE, CLASS_NAME, METHOD,
						"Thread was interrupted, possibly caused by hangup",
						ie);
			}

			// If nothing is sent in 55 seconds, send a zero width no break
			// space. This will prevent NATs closing the UDP hole.
			if (dataWaiting.length == 0) {
				setData(TextConstants.ZERO_WIDTH_NO_BREAK_SPACE);
			}
			

			logger.logp(Level.FINEST, CLASS_NAME, METHOD, "the buffertime is", Integer.valueOf(bufferTime));
			while (dataWaiting.length > 0 || redGensToSend > 0) {
				try {
					Thread.sleep(bufferTime);
				}
				catch (InterruptedException ie) {
				}

				synchronized (this) {

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
	 * Sets if the SynchBuffer should send on CR or realtime.
	 *
	 * @param boolean aSendOnCR should buffer send on CR
	 */
	public void setSendOnCR(boolean aSendOnCR) {
		this.sendOnCR = aSendOnCR;
	}

	/**
	 * Gets if syncbuffer is set to send on CR.
	 *
	 * @return The boolean.
	 */
	public boolean getSendOnCR() {
		return sendOnCR;
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