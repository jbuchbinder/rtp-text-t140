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
package se.omnitor.protocol.rtp;

//import LogClasses and Classes
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Insets;
import java.awt.GridLayout;
import javax.swing.JPanel;

import se.omnitor.protocol.rtp.packets.RTPPacket;

/**
 * This Dialog is used for showing throwed packets. Packets are thrown as a
 * debug option for the RFC 4103 developer/tester. <br>
 *
 * @author Andreas Piirimets, Omnitor AB
 */
public class ThrowPacketDialogHandler {
	
	JLabel packetsToThrowLabel;
	
	private TextProtocolInformationDialog dialog;
	private Frame owner;
	private int nbrOfPacketsToThrow = 0;
	private boolean isRedundancy = true;
	
	public ThrowPacketDialogHandler(Frame owner) {
		this.owner = owner;
		
		createGui();
		
	}
	
	private void createGui() {
		dialog = new TextProtocolInformationDialog(owner, "Thrown packets");
		dialog.setShowPacketsToThrow(true);
		dialog.setPacketsToThrow(0);
		/*
		this.setSize(600, 300);
		
		JPanel p = new JPanel(new GridBagLayout());

		JLabel label;
		GridBagConstraints gbc;
		
		label = new JLabel("Packets to throw:");
		label.setFont(label.getFont().deriveFont(label.getFont().getStyle() ^ Font.BOLD));
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 10, 5);
		p.add(label, gbc);
		
		packetsToThrowLabel = new JLabel("0");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 10, 5);
		p.add(packetsToThrowLabel, gbc);
		
		label = new JLabel("");
		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1.0;
		gbc.weighty = 0;
		p.add(label, gbc);
		
		label = new JLabel("Thrown packets:");
		label.setFont(label.getFont().deriveFont(label.getFont().getStyle() ^ Font.BOLD));
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1.0;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		p.add(label, gbc);

		packetAnalyzerPanel = new PacketAnalyzerPanel(true);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 3;
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.insets = new Insets(0, 5, 5, 5);
		gbc.fill = GridBagConstraints.BOTH;
		p.add(packetAnalyzerPanel, gbc);
		
		getContentPane().setLayout(new GridLayout(1, 1));
		getContentPane().add(p);
		*/
	}
	
	public void updatePacketsToThrow(int nbr) {
		nbrOfPacketsToThrow = nbr;
		dialog.setPacketsToThrow(nbr);
		//packetsToThrowLabel.setText(""+nbr);
	}
	
	public int getNbrOfPacketsToThrow() {
		return nbrOfPacketsToThrow;
	}
	
	public void addPacketsToThrow(int nbr) {
		nbrOfPacketsToThrow += nbr;
		dialog.setPacketsToThrow(nbrOfPacketsToThrow);
	}
	
	public boolean isVisible() {
		return dialog.isVisible();
	}
	
	public void throwedPacket(RTPPacket packet) {
		String seqNo = Long.toString(packet.getSequenceNumber() & 0x0000FFFFL);
		
		byte[] payload = packet.getPayloadData();
		byte[] newPayloadData = new byte[payload.length];
		System.arraycopy(payload, 0, newPayloadData, 0, payload.length);

		TextPacketInfo info = new TextPacketInfo((int)(packet.getSequenceNumber() & 0x0000FFFFL), 99, newPayloadData);

		if (isRedundancy) {
			RedGen[] redGenArray = extractRedGens(packet);
			if (redGenArray != null) {
				for (int cnt=0; cnt<redGenArray.length; cnt++) {
					info.addRedundantGeneration(redGenArray[cnt]);
				}
			}
		}
		else {
			byte[] primdata = packet.getPayloadData();
			info.addRedundantGeneration(new RedGen(0, packet.getTimeStamp(), primdata.length, byteToAsciiString(primdata)));
		}
		
		dialog.addPacket(info);
		
		//packetAnalyzerPanel.addPacket(packet);
	}
	
	public void setVisible(boolean isVisible) {
		if (!dialog.isVisible() && isVisible) {
			dialog = new TextProtocolInformationDialog(owner, "Thrown packets");
			dialog.setShowPacketsToThrow(true);
			dialog.setPacketsToThrow(nbrOfPacketsToThrow);
			dialog.setRedundancy(isRedundancy);
		}
		
		dialog.setVisible(isVisible);
	}

	private RedGen[] extractRedGens(RTPPacket packet) {
		RedGen[] retArray = new RedGen[0];
		
		
		byte[] data = packet.getPayloadData();
		int walker = 0;
		int nextGeneration = 1;
		
		Vector<RedGen> tempRedGenList = new Vector<RedGen>(0, 1);
		Vector<RedGen> redGenList = new Vector<RedGen>(0, 1);
		
		String error = "";
		
		while (data.length > walker) {
			int generation = -1;
			int time = -1;
			int length = -1;
			byte redGenData[] = new byte[0];
			
			if ((data[walker] & 0x80) > 0) {
				generation = nextGeneration;
				nextGeneration++;
				
				//redLine.pt = data[walker] & 0x7f;
				
				if (data.length <= (walker+3)) {
					// Something is wrong - cannot show red generations.
					return null;
				}
				else {
					walker++;
					time = (data[walker] << 6) + ((data[walker] & 0xfc) >> 2);
					walker++;
					length = ((data[walker] & 0x03) << 8) + data[walker+1];
					walker += 2;
				}
				
				tempRedGenList.add(new RedGen(generation, time, length, ""));

			}
			else {
				int pt = data[walker] & 0x7f;
				byte[] primdata = new byte[0];
				walker++;
				
				for (int lcnt=0; lcnt<tempRedGenList.size(); lcnt++) {
					RedGen redGen = tempRedGenList.elementAt(lcnt);
					if (data.length < walker+redGen.getLength()) {
						error = "Data for redundant generation " + redGen.getGeneration() + " is less than expected blocklength!";
						walker = data.length;
						continue;
					}
					byte[] tempData = new byte[redGen.getLength()];
					for (int bcnt=0; bcnt<redGen.getLength(); bcnt++) {
						tempData[bcnt] = data[walker];
						walker++;
					}
					
					redGenList.add(new RedGen(redGen.getGeneration(), redGen.getTime(), redGen.getLength(), byteToAsciiString(tempData)));
				}
				
				if (data.length > walker) {
					primdata = new byte[data.length - walker];
				}
				int datacnt = 0;
				while (data.length > walker) {
					primdata[datacnt] = data[walker];
					walker++;
					datacnt++;
				}
				
				//addLine("Prim", ""+pt, "ts", ""+primdata.length, new String(primdata).toString(), false);
				
				retArray = new RedGen[redGenList.size() + 1];
				retArray[0] = new RedGen(0, packet.getTimeStamp(), primdata.length, byteToAsciiString(primdata));
				int arraycnt = 1;
				
				for (int cnt=redGenList.size()-1; cnt>=0; cnt--) {
					RedGen redGen = redGenList.elementAt(cnt);
					retArray[arraycnt] = new RedGen(redGenList.size()-redGen.getGeneration()+1, redGen.getTime(), redGen.getLength(), redGen.getData());
/*
					try {
						addLine(""+(lines.size()-rl.generation+1), ""+rl.pt, ""+rl.timestamp, ""+rl.blocklength, new String(rl.data, "UTF-8").toString(), false);
					}
					catch (UnsupportedEncodingException e) {
						addLine(""+(lines.size()-rl.generation+1), ""+rl.pt, ""+rl.timestamp, ""+rl.blocklength, new String(rl.data + " (Warning: Could not decode UTF-8)").toString(), false);
					}
					*/
					arraycnt++;
				}
			}
		}
		
		/*
		if (error.length() > 0) {
			JLabel label = new JLabel(error);
			label.setForeground(Color.RED);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(1, 1, 1, 1);
			gbc.gridx = 1;
			gbc.gridy = nextAvailableLine;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.gridwidth = 7;
			contentPanel.add(label, gbc);
			
			nextAvailableLine++;
		}
		
		endLine();
		*/
		
		return retArray;
		
	}

	
	/**
	 * Check if this dialog is closed by the user.
	 * 
	 * @return
	 */
	public boolean getIsClosed() {
		return false;
	}

	
	public String byteToAsciiString(byte[] bytelist) {
		String retStr = "";
		for (int cnt=0; cnt<bytelist.length; cnt++) {
			if (bytelist[cnt] >= 32 && 
				bytelist[cnt] <= 125 &&
				bytelist[cnt] != '[' &&
				bytelist[cnt] != ']') {
			/*
			if ((bytelist[cnt] >= 'A' && bytelist[cnt] <= 'Z') ||
				(bytelist[cnt] >= 'a' && bytelist[cnt] <= 'z') ||
				(bytelist[cnt] >= '0' && bytelist[cnt] <= '9') ||
				bytelist[cnt] == ' ') {
				*/
				retStr += (char)bytelist[cnt];
			}
			else {
				retStr += "[" + Integer.toHexString((int)bytelist[cnt] & 0x000000ff) + "]";
			}
		}
		
		return retStr;
	}
	
	public void setSdp(String invite, String ok) {
		dialog.setSdp(invite, ok);
	}
	
	public void setRedundancy(boolean isRedundancy) {
		this.isRedundancy = isRedundancy;
		dialog.setRedundancy(isRedundancy);
	}
}
