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
package se.omnitor.media.content.text.t140;

import java.awt.Component;
import java.awt.TextArea;

import java.io.UnsupportedEncodingException ;

import javax.media.Buffer;
import javax.media.Control;
import javax.media.Format;
import javax.media.Owned;
import javax.media.Renderer;
import javax.media.ResourceUnavailableException;
import javax.media.control.BufferControl;
import javax.media.protocol.ContentDescriptor;

import se.omnitor.util.FifoBuffer;
import se.omnitor.protocol.rtp.text.TextConstants;
import se.omnitor.media.protocol.text.t140.TextFormat;

/** 
 * Receives text, and puts the text in a FifoBuffer.
 *
 * @author Erik Zetterström, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class TextPlayer implements Renderer, BufferControl, Owned {

    /**
     * The descriptive name of this renderer
     */
    public static final String PLUGIN_NAME = "RTP-Text renderer";
    
    private TextFormat[] supportedInputFormats;
    private TextFormat[] supportedOutputFormats;
    private boolean started;
    private ContentDescriptor cd;
    private Format inputFormat;
    private FifoBuffer fifo;

    // ??
    private String outString = "";
    private TextArea ta = null;
    private Component component = null;

    /**
     * Initializes text formats.
     *
     */
    public TextPlayer(FifoBuffer fifo) {
	supportedInputFormats = new TextFormat[] {  new TextFormat("UTF8") };
	supportedOutputFormats = new TextFormat [] { new TextFormat("UTF8") };
	
	started = false;

	this.fifo = fifo;

	cd = new ContentDescriptor(ContentDescriptor.RAW);
    }
    
    /**
     * Utility function to see if a format 
     * matches any of a number of given formats
     *
     * @param input The format to check for.
     * @param supported The format to check againast.
     *
     * @return True if it matches any of the given formats, false otherwise.
     */
    public static boolean matches(Format input, Format[] supported) {
	for (int i = 0; i < supported.length; i++) {
	    if (input.matches(supported[i])) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Gets the content descriptor of this player.
     *
     * @return The content descriptor.
     */
    public ContentDescriptor getContentDescriptor() {
	return cd;
    }
    
    /**
     * Returns an array of supported controls
     *
     * @return The supported controls.
     */
    public Object[] getControls() {
	// No controls
	Control[] ctrls = { this };
        return (Object[]) ctrls;
    }


    /**
     * Return the control based on a control type for the PlugIn.
     *    
     * @param controlType The string identfying the  desired controller.
     * @return The control or null if not supported by this plug-in.
     */
    public Object getControl(String controlType) {
	try {
	    Class  cls = Class.forName(controlType);
	    Object[] cs = getControls();
	    for (int i = 0; i < cs.length; i++) {
		if (cls.isInstance(cs[i])) {
		    return cs[i];
		}
	    }
	    return null;
	} catch (Exception e) {   // no such controlType or such control
	    return null;
	}
    }


    /**
     * Returns the name of this plug-in.
     *
     * @return The name of this plug-in.
     */
    public String getName() {
	return PLUGIN_NAME;
    }
    

    /**
     * Opens the plugin.
     * 
     * @throws ResourceUnavailableException The exception.
     */
    public void open() throws ResourceUnavailableException {

    }

    /**
     * Resets the state of the plug-in. Typically at end of media or when media
     * is repositioned.
     *
     */
    public void reset() {
	// Nothing to do
    }
    
    /**
     * Closes the plug-in.
     *
     */
    public void close() {
	// Nothing to do
    }

    /**
     * Starts the plug-in.
     *
     */
    public void start() {
	started = true;
    }

    /**
     * Stops the plug-in.
     *
     */
    public void stop() {
	started = false;
    }
    
    /**
     * Lists the possible input formats supported by this plug-in.
     *
     * @return The possible inpur formats.
     */
    public Format [] getSupportedInputFormats() {
	return ((Format[])(supportedInputFormats));
    }
    
    /**
     * Returns the supported output formats.
     *
     * @param f The format to check for. 
     * @return The supported output formats.
     */
    public Format[] getSupportedOutputFormats(Format f) {
	return ((Format[])(supportedOutputFormats));
    }

    /**
     * Set the data input format.
     *
     * @param format The desired input format.
     * @return The desired input format or null if is not supported
     *         
     */
    public Format setInputFormat(Format format) {
	if ( format != null && matches(format,supportedInputFormats)) {
	    
	    inputFormat = format;
	    return format;
	} else {
	    return null;
	}
    }

    /**
     * Processes the data and puts it into a FIFO buffer
     *
     * @param buffer The input buffer
     *
     * @return A result code as described in the JMF documentation
     */
    public synchronized int process(Buffer buffer) {
	
	Format inf = buffer.getFormat();
	if (inf == null) {
	    return BUFFER_PROCESSED_FAILED;
	}
	
	if (inf != inputFormat || !buffer.getFormat().equals(inputFormat)) {
	    if (setInputFormat(inf) != null) {
		return BUFFER_PROCESSED_FAILED;
	    }
	}
	
	Object data = buffer.getData();
	
	if (!(data instanceof byte[])) {
	    return BUFFER_PROCESSED_FAILED;
	}

	fifo.setData((byte[])buffer.getData());
	
	return BUFFER_PROCESSED_OK;
    }

    /**************
     BufferControl
    **************/

    /**
     * The buffer length. 0 always;
     *
     * @return Always 0.
     */
    public long getBufferLength() {
	return 0;
    }

    /**
     * Sets the buffer length. Buffer length is always 0.
     *
     * @param time The buffer length.
     * @return Always 0.
     */
    public long setBufferLength(long time) {
	return 0;
    }

    /**
     * Returns the mininmum threshold.
     * 
     * @return The minimum threshold is always 0.
     */
    public long getMinimumThreshold() {
	return 0;
    }

    /**
     * Sets the minimum threshold. 
     * The minimum threshold is always 0.
     *
     * @param time The minmum threshold is always 0,
     * 
     * @return Always 0.
     */
    public long setMinimumThreshold(long time) {
	return 0;
    }

    /**
     * The minimum  threshold is never used.
     *
     * @return Always false.
     */
    public boolean getEnabledThreshold() {
	return false;
    }
    
    /**
     * The minumum threshold is never used.
     * 
     * @param b Never used.
     */
    public void setEnabledThreshold(boolean b) {

    }

    /**
     * There is no controller component.
     *
     * @return null.
     */
    public java.awt.Component getControlComponent() {
	return null;
    }

    /******
     Owner
    ******/
     
    /**
     * Returns the owner of this objet.
     * 
     * @return this
     */
    public java.lang.Object getOwner() {
	return this;
    }
 
}
