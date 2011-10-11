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

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;

import javax.media.ResourceUnavailableException;

import se.omnitor.protocol.rtp.text.RtpTextBuffer;
import se.omnitor.protocol.rtp.text.RtpTextPacketizer;

/**
 * JMF wrapper for the RTP text/t140 packetizer. <br>
 * <br>
 * Put this into the codec chain in order to get it work!
 *
 * @author Erik Zetterstrom, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class TextPacketizer implements Codec {

    //Constants

    /**
     * The name of this plug-in.
     */
    public static final String PLUGIN_NAME = "RTP packetizer for text/t140";

    private RtpTextPacketizer packetizer;

    private Format inputFormat  = null;
    private Format outputFormat = null;
    private TextFormat[] supportedInputFormats  = null;
    private TextFormat[] supportedOutputFormats = null;

    /**
     * Initializes the packetizer.
     *
     * @param t140Pt The payload type to use.
     * @param redGen The number of redundant generations.
     */
    public TextPacketizer(int t140Pt, int redPt, int redGen) {

	packetizer = new RtpTextPacketizer(t140Pt, redPt, redGen);

	//Add all supported input formats
	supportedInputFormats = new TextFormat[] { new TextFormat("UTF8"),
						   new TextFormat
						       ("ISO-8859-1"),
						   new TextFormat("UTF-16"),
						   new TextFormat("UTF-16BE"),
						   new TextFormat("UTF-16LE"),
	};
	
        //Add all supported output formats                           
	supportedOutputFormats = new TextFormat[] { new TextFormat("UTF8/RTP"),
						    new TextFormat("RED/RTP"),
	};
	
    }
    
    /**
     * Checks if a given format matches any of the other given formats.
     * Utility to check if a format is supported or not.
     * 
     * @param inputFormat The format to be checked.
     * @param supportedFormats The supported formats.
     *
     * @return True if the given format matches any of the given supported
     * formats, false otherwise.
     */
    public static boolean matches(Format inputFormat,
				  Format[] supportedFormats) {

	for (int i = 0; i < supportedFormats.length; i++) {
	    if (inputFormat.matches(supportedFormats[i])) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Copies information in buffers.
     *
     * @param src Source buffer
     * @param dst Destination buffer.
     */
    private void bufferCopy(Buffer src, RtpTextBuffer dst) {
	dst.setData((byte[])src.getData());
	dst.setLength(src.getLength());
	dst.setOffset(src.getOffset());
	dst.setTimeStamp(src.getTimeStamp());
	dst.setSequenceNumber(src.getSequenceNumber());
    }

    /**
     * Copies information in buffers.
     *
     * @param src Source buffer
     * @param dst Destination buffer.
     */
    private void bufferCopy(RtpTextBuffer src, Buffer dst) {
	dst.setData(src.getData());
	dst.setLength(src.getLength());
	dst.setOffset(src.getOffset());
	dst.setTimeStamp(src.getTimeStamp());
	dst.setSequenceNumber(src.getSequenceNumber());
    }

    /**
     * The process called by RTPManager. Contructs the actual packet.
     *
     * @param inBuffer The data to be packetized.
     * @param outBuffer The resulting packet
     * 
     * @return A statuscode
     */
    public synchronized int process(Buffer inBuffer, Buffer outBuffer) {
	
	RtpTextBuffer tmpInBuffer = new RtpTextBuffer();
	RtpTextBuffer tmpOutBuffer = new RtpTextBuffer();

	bufferCopy(inBuffer, tmpInBuffer);
	bufferCopy(outBuffer, tmpOutBuffer);

	packetizer.encode(tmpInBuffer, tmpOutBuffer);

	bufferCopy(tmpOutBuffer, outBuffer);

	return BUFFER_PROCESSED_OK;
    }
    
    /**
     * Sets the type of format received in the input buffer.
     * 
     * @param format The input format.
     * 
     * @return The format if success, null if the format is not supported.
     */
    public Format setInputFormat(Format format) {
	if (!matches(format, supportedInputFormats)) {
	    return null;
	}
	inputFormat = format;
	return format;
    }
    
    /**
     * Sets the type of format to use in the output buffer.
     * 
     * @param format The output format.
     * 
     * @return The format if success, null if the format is not supported.
     */
    public Format setOutputFormat(Format format) {
	if (!matches(format, supportedOutputFormats)) {
	    return null;
	}
	outputFormat = format;
	return format;
    }
    
    /**
     * Gets the type of format used as input format.
     *
     * @return The format used, null if not set.
     */
    protected Format getInputFormat() {
	return inputFormat;
    }
    
    /**
     * Gets the type of format used as output format.
     * 
     * @return The format used, null if not set.
     */
    protected Format getOutputFormat() {
	return outputFormat;
    }

    /**
     * Gets the supported input formats.
     *
     * @return An array containing the supported input formats.
     */
    public Format[] getSupportedInputFormats() {
	return supportedInputFormats;
    }

    /**
     * Gets the supported output formats.
     *
     * @param input The format to check for.
     * @return An array containing the supported output formats or 
     *         if the input format is not supported an empty format array.
     */
    public Format[] getSupportedOutputFormats(Format input) {
	if (input == null || matches(input, supportedInputFormats)) {
	    return supportedOutputFormats;
	}
	return new Format[0];
    }

    /**
     * Required by JMF. Does nothing.
     *
     * @throws ResourceUnavailableException If open fails.
     */
    public void open() throws ResourceUnavailableException {
    }
    
    /**
     * Required by JMF. Does nothing.
     */
    public void close() {
    }

    /**
     * Required by JMF. Does nothing.
     */
    public void reset() {
    }

    /**
     * No controls implemented for this plugin.
     *
     * @return An empty Object array.
     */
    public Object[] getControls() {
        return new Object[0];
    }

    /**
     * No controls implemented for this plugin.
     * 
     * @param type THe desired control.
     *
     * @return null
     */
    public Object getControl(String type) {
	return null;
    }

    /**
     * Returns the name of this plug-in.
     *
     * @return The name of this plug-in.
     */
    public String getName() {
	return PLUGIN_NAME;
    }

}