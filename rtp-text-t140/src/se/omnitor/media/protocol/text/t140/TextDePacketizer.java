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
package se.omnitor.media.protocol.text.t140;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.format.AudioFormat;

import se.omnitor.protocol.rtp.text.RtpTextDePacketizer;
import se.omnitor.protocol.rtp.text.RtpTextBuffer;

/**
 * Extracts data from incoming RTP-Text packets. <br>
 * Also handles missing packets. <br>
 * <br>
 * According to "RFC2793 - RTP Payload for text conversation" 
 * transmitted text must be in UTF-8 form. <br>
 *
 * @author Erik Zetterström, Omnitor AB
 * @author Andreas Piirimets, Omnitor AB
 */
public class TextDePacketizer implements Codec {

    //Plug-in info

    /**
     * The name of this plug-in.
     */
    public static final String PLUGIN_NAME = "DePacketizer for RTP text/t140";


    //Formats
    private Format inputFormat  = null;
    private Format outputFormat = null;
    private TextFormat[] supportedInputFormats  = null;
    private TextFormat[] supportedOutputFormats = null;

    private RtpTextDePacketizer depacketizer;


    /**
     * Utility function to se if a format matches any of a number of given
     * formats.
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
     * Initializes the depacketizer and all formats.
     *
     * @param t140PayloadType Payload type to use.
     * @param redFlagIncoming Indicates if redundancy is on or off.
     */
    public TextDePacketizer(int t140PayloadType, int redPt, boolean redFlagIncoming) {

	depacketizer = 
	    new RtpTextDePacketizer(t140PayloadType, redPt, redFlagIncoming);


        //Add all supported input formats
        supportedInputFormats = new TextFormat[] { 
	    new TextFormat("UTF8/RTP"),
	    new TextFormat("RED/RTP")
		};

        //Add all supported output formats
        supportedOutputFormats = new TextFormat[] { new TextFormat("UTF8") };
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
     * Returns the input formats supported by this plug-in.
     *
     * @return The supported input formats.
     */
    public Format [] getSupportedInputFormats() {
        return ((Format[])supportedInputFormats);
    }

    /**
     * Returns the output formats supported by this plug-in.
     *
     * @param input The requested format.
     *
     * @return The supported output formats.
     */
    public Format [] getSupportedOutputFormats(Format input) {
        if (input == null || matches(input, supportedInputFormats)) {
            return ((Format[])supportedOutputFormats);
        }
        return new Format[0];
    }

    /**
     * Sets the input format of this plug-in.
     *
     * @param format The desired input format.
     *
     * @return The desired input format. Null if not supported.
     */
    public Format setInputFormat(Format format) {
        if (!matches(format, supportedInputFormats)) {
            return null;
        }
        inputFormat = format;
        return format;
    }

    /**
     * Sets the output format of this plug-in.
     *
     * @param format The desired output format.
     *
     * @return The desired output format. Null if not supported.
     */
    public Format setOutputFormat(Format format) {
        if (!matches(format, supportedOutputFormats)) {
            return null;
        }
        outputFormat = format;
        return format;
    }

    /**
     * Returns the input format.
     *
     * @return The input format.
     */
    protected Format getInputFormat() {
        return inputFormat;
    }

    /**
     * Returns the output format.
     *
     * @return The output format.
     */
    protected Format getOutputFormat() {
        return outputFormat;
    }

    /**
     * Required by JMF. Does nothing.
     */
    public void open() {
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
     * @return Returns the controls of this object.
     */
    public Object[] getControls() {
        return new Object[0];
    }


    /**
     * No controls implemented for this plugin.
     * 
     * @param type Not used.
     * 
     * @return Always null.
     */
    public Object getControl(String type) {
        return null;
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
     * Extracts data from received packets. Handles missing packets.
     *
     * @param inputBuffer  The received packet
     * @param outputBuffer The extracted data
     *
     * @return 1 if success
     * @return 0 if parse failure
     * @return -1 packet received out of order.
     */
    public synchronized int process(Buffer inputBuffer, Buffer outputBuffer) {

	RtpTextBuffer tempInBuffer = new RtpTextBuffer();
	RtpTextBuffer tempOutBuffer = new RtpTextBuffer();

	bufferCopy(inputBuffer, tempInBuffer);
	bufferCopy(outputBuffer, tempOutBuffer);

	depacketizer.decode(tempInBuffer, tempOutBuffer);

	bufferCopy(tempInBuffer, inputBuffer);
	bufferCopy(tempOutBuffer, outputBuffer);

        return BUFFER_PROCESSED_OK;
    }

}

















