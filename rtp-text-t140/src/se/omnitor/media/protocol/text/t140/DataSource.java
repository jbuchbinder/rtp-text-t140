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

import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.Time;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import se.omnitor.protocol.rtp.text.SyncBuffer;

/**
 * This data source creates the medialocator text:// with three different
 * input models: <br>
 * <br>
 * <u>text.t140:stdin</u> <br>
 * Text is read from stdin. Everything is handled by JMF. <br>
 * <br>
 * <u>text.t140:buffer</u> <br>
 * This data source expects a SyncBuffer object to be assigned to it with the
 * function setSyncBuffer(). The text in the buffer is supposed to be in
 * T.140 format. <br>
 * <br>
 * "text.t140:" is equal to "text.t140:buffer". <br>
 * <br>
 *
 * @author Erik Zetterstrom, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class DataSource extends PushBufferDataSource {

    private Logger logger;
    private boolean connected;
    private TextStream[] streams;
    private boolean started;
    private SyncBuffer sb;

    private StdInReader stdInReader;


    /**
     * Initializes.
     *
     */
    public DataSource() {

	logger = Logger.getLogger("se.omnitor.media.protocol.text.t140");
	connected = false;
	streams = new TextStream[] { new TextStream() };
	started = false;
	sb = null;
	stdInReader = null;
    }

    /**
     * Gets the content type of this data source.
     *
     * @return The content type.
     */
    public String getContentType() {
	if (!connected) {
	    logger.severe("Cannot get content type if data source is not " +
			  "connected!");
	    return null;
	}

	return ContentDescriptor.RAW;
    }

    public void setSyncBuffer(SyncBuffer sb) {
	this.sb = sb;
    }

    /**
     * Connects this data source to its source.
     *
     * @throws IOException If connect fails.
     */
    public void connect() throws IOException {
	connected = true;
    }

    /**
     * Sets the number of redundant generations to use.
     *
     * @param redGen The number of redundant generations to use, a value of
     * zero disables redundancy.
     */
    public void setRedGen(int redGen) {
	streams[0].setRedGen(redGen);
    }

    /**
     * Disconnects this data source.
     */
    public void disconnect() {
	try {
            if (started) {
                stop();
	    }
        } catch (IOException ioe) {
	    logger.throwing(this.getClass().getName(), "disconnect", ioe);
	}

	connected = false;
    }

    /**
     * Starts this data source. The data source must be connected.
     *
     * @throws IOException If start fails.
     */
    public synchronized void start() throws IOException {

        if (!connected) {
	    logger.severe("DataSource must be connected before it can be " +
			  "started!");
	}
        if (!started) {
	    String type;
	    if (getLocator() != null) {
		type = getLocator().getRemainder();
		if (type.equals("")) {
		    type = "BUFFER";
		}
	    }
	    else {
		type = "BUFFER";
	    }


	    if (type.toUpperCase(Locale.US).equals("STDIN")) {

		sb = new SyncBuffer(0, 300);
		streams[0].start(sb);
		stdInReader = new StdInReader(sb);
		stdInReader.start();
		started = true;

	    }
	    else if (type.toUpperCase().equals("BUFFER")) {

		if (sb == null) {
		    logger.severe("SyncBuffer must be set before starting " +
				  "DataSource!");
		}
		else {
		    streams[0].start(sb);
		    started = true;
		}
	    }
	    else {

		logger.severe("Unknown format: " + getLocator().toString() +
			      " (" + type + ")");

	    }

	}

    }

    /**
     * Stops this data source. The data source must be started and connected.
     *
     * @throws IOEXception failure.
     */
    public void stop() throws IOException {
	if ((!connected) || (!started)) {
	    logger.severe("DataSource must be connected and started before " +
			  "it can be stopped!");
	    return;
	}

	if (stdInReader != null) {
	    stdInReader.stop();
	    stdInReader = null;
	}

	streams[0].stop();

	started = false;
    }

    /**
     * Gets the controls of this data source.
     *
     * @return The controls.
     */
    public Object[] getControls() {
	return new Object[0];
    }

    /**
     * Gets at specified control from this data source.
     *
     * @param controlType A string describing the control.
     *                    Example: "javax.media.control.BufferControl"
     * @return The requested control or null if it was not found.
     */
    public Object getControl(String controlType) {
	return null;
    }

    /**
     * Gets the duration of this data source.
     *
     * @return The duration.
     */
    public Time getDuration() {
	return DURATION_UNKNOWN;
    }

    /**
     * Gets the streams of this data source.
     *
     * @return The streams.
     */
    public PushBufferStream[] getStreams() {
	return streams;
    }



    class TextStream implements PushBufferStream, Runnable {

	private Format format;
	private byte[] data;
	private BufferTransferHandler transferHandler;
	private ContentDescriptor cd;
	private boolean running;
	private Thread thread;
	private SyncBuffer sb;
	private int redGen;

	public TextStream() {
	    format = new TextFormat("UTF8");
	    data = null;
	    cd = new ContentDescriptor(ContentDescriptor.RAW);
	    running = false;
	    thread = null;
	    redGen = 0;
	}

	public Format getFormat() {
	    return format;
	}

	public void read(Buffer buffer) {
	    synchronized (this) {
		buffer.setData(data);
		buffer.setLength(data.length);
		data = null;
	    }
	    buffer.setFormat(format);
	}

	public void setRedGen(int redGen) {
	    this.redGen = redGen;
	    if (sb != null) {
		sb.setRedGen(redGen);
	    }
	}

	public void setTransferHandler(BufferTransferHandler transferHandler) {
	    this.transferHandler = transferHandler;
	}

	public boolean endOfStream() {
	    return false;
	}

	public long getContentLength() {
	    return LENGTH_UNKNOWN;
	}

	public ContentDescriptor getContentDescriptor() {
	    return cd;
	}

	public Object getControl(String controlName) {
	    return null;
	}

	public Object[] getControls() {
	    return new Object[0];
	}

	public void start(SyncBuffer sb) {
	    if (running) {
		stop();
	    }

	    this.sb = sb;
	    sb.setRedGen(redGen);
	    sb.start();

	    running = true;

	    thread = new Thread(this, "JMF text datasource");
	    thread.start();
	}

	public void stop() {
	    running = false;

	    if (thread != null) {
		thread.interrupt();
	    }
	    if (sb != null) {
		sb.stop();
		sb = null;
	    }

	    thread = null;
	}

	public void run() {

	    byte[] tempData;

	    while (running) {
		try {
		    tempData = sb.getData();

		    synchronized (this) {
			if (data == null) {
			    data = tempData;
			}
			else {
			    byte[] temp = data;
			    data = new byte[temp.length + tempData.length];
			    System.arraycopy(temp, 0, data, 0, temp.length);
			    System.arraycopy(tempData, 0, data, temp.length,
					     tempData.length);
			}
		    }

		    transferHandler.transferData(this);
		}
		catch (InterruptedException ie) {
		    // Closing down.
		}

	    }
	}

    }

}