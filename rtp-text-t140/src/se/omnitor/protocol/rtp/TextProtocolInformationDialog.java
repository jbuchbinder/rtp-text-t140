/*
 * Copyright (C) 2004-2008  University of Wisconsin-Madison and Omnitor AB
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package se.omnitor.protocol.rtp;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * This class is a dialog that shows information about text packets.
 * 
 */
@SuppressWarnings("serial")
public class TextProtocolInformationDialog extends JDialog {

	private JPanel pnlPacketsToThrow;
	private JPanel pnlPacketList;
	private JPanel pnlPacketDetails;
	private JPanel pnlHexContents;
	private JPanel pnlSDP;
	private JPanel pnlLocalSDP;
	private JPanel pnlRemoteSDP;
	private JLabel lblPacketsToThrowKey;
	private JLabel lblPacketsToThrowValue;
	private JTextArea taLocalSDP;
	private JTextArea taRemoteSDP;
	private JTextArea taHexDump;
	private JTextArea taDetail;
	private JLabel lblSDP;
	private JLabel lblIncomingPackets;
	private JTable tblPacketList;
	private JTable tblPacketDetail;
	private JScrollPane spPacketList;
	private JScrollPane spPacketDetail;
	private JScrollPane spHexDump;
	private JScrollPane spLocalSDP;
	private JScrollPane spRemoteSDP;
	private AbstractPacketListModel packetListTableModel;
	private PacketDetailsTableModel packetDetailsTableModel;

	private static final int DIALOG_SIZE_X = 550; 
	private static final int DIALOG_SIZE_Y = 650;
	private static final String HEX_FONT = "Courier New"; // This font must be
	// monospaced!!
	private static final String SDP_FONT = "Courier New";

	/**
	 * Initiates the dialog, but does not show it.
	 * 
	 * Default: hide both "SDP" and "Packets to throw" panels. packets to throw
	 * is zero packet list contains headings but no rows packet details and hex
	 * contents only contains the text "Select a packet in the list"
	 * 
	 * @param owner
	 *            The parent frame
	 * @param dialogName
	 *            The name that should be shown in the frame of the dialog.
	 */
	public TextProtocolInformationDialog(Frame owner, String dialogName) {
		super(owner);
		packetListTableModel = new PacketListTableModel();
		packetDetailsTableModel = new PacketDetailsTableModel();
		setTitle(dialogName);
		this.setSize(DIALOG_SIZE_X, DIALOG_SIZE_Y);
		initComponents();
		taHexDump.setText("Select a packet in the list");
		taDetail.setText("Select a packet in the list");
		lblPacketsToThrowValue.setText("0");
		setTopPanelsVisible(false);
		setDetailShowData(false);
	}

	/**
	 * Hide and show SDP and Packet to throw panels
	 */
	private void setTopPanelsVisible(boolean show) {
		pnlPacketsToThrow.setVisible(show);
		pnlSDP.setVisible(show);
	}

	private void initComponents() {

		// controls instantiation
		pnlPacketsToThrow = new JPanel();
		pnlSDP = new JPanel();
		pnlLocalSDP = new JPanel();
		pnlRemoteSDP = new JPanel();
		pnlPacketList = new JPanel();
		pnlPacketDetails = new JPanel();
		pnlHexContents = new JPanel();

		lblPacketsToThrowKey = new JLabel();
		lblPacketsToThrowValue = new JLabel();
		lblSDP = new JLabel();
		lblIncomingPackets = new JLabel();

		taLocalSDP = new JTextArea();
		taRemoteSDP = new JTextArea();
		taHexDump = new JTextArea();
		taDetail = new JTextArea();

		tblPacketDetail = new JTable(packetDetailsTableModel);
		tblPacketList = new JTable(packetListTableModel);

		spHexDump = new JScrollPane(taHexDump);
		spPacketList = new JScrollPane(tblPacketList);
		spPacketDetail = new JScrollPane(tblPacketDetail);
		spLocalSDP = new JScrollPane(taLocalSDP);
		spRemoteSDP = new JScrollPane(taRemoteSDP);

		//this hack needed for maintain equal size of INVITE and 200 OK panels
		spRemoteSDP.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent arg0) {
				Dimension size = spRemoteSDP.getPreferredSize();
				size.width = spRemoteSDP.getViewport().getWidth() - 
					spRemoteSDP.getVerticalScrollBar().getWidth();
				spRemoteSDP.setPreferredSize(size);
				spLocalSDP.setPreferredSize(size);
			}
			
		});

		setHexPanelHeightAsHalfPacketListPanel();
		
		//this hack needed for maintain hex panel height as 0.5 packet list panel height 
		spPacketList.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent arg0) {
				setHexPanelHeightAsHalfPacketListPanel();
			}
			
		});
		
		tblPacketList.setColumnModel(packetListTableModel.getColumnModel());
		tblPacketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tblPacketList.getTableHeader().setReorderingAllowed(false);
		tblPacketDetail.setEnabled(false);
		
		tblPacketDetail.getTableHeader().setReorderingAllowed(false);

		// setting controls properties
		taLocalSDP.setBackground(this.getBackground());
		taLocalSDP.setEditable(false);
		taLocalSDP.setFont(new Font(SDP_FONT, 0, 11));

		taRemoteSDP.setBackground(this.getBackground());
		taRemoteSDP.setEditable(false);
		taRemoteSDP.setFont(new Font(SDP_FONT, 0, 11));
		
		taHexDump.setBackground(this.getBackground());
		taHexDump.setEditable(false);
		taHexDump.setFont(new Font(HEX_FONT, 0, 11));

		taDetail.setBackground(this.getBackground());
		taDetail.setEditable(false);
		taDetail.setFont(new Font("Courier New", 0, 11));

		pnlLocalSDP.setBorder(BorderFactory.createTitledBorder("INVITE"));
		pnlRemoteSDP.setBorder(BorderFactory.createTitledBorder("200 OK"));
		pnlPacketList
				.setBorder(BorderFactory.createTitledBorder("Packet list"));
		pnlPacketDetails.setBorder(BorderFactory
				.createTitledBorder("Packet details"));
		pnlHexContents.setBorder(BorderFactory
				.createTitledBorder("Hex contents"));

		lblSDP.setText("SDP");
		lblIncomingPackets.setText("Packets");
		lblPacketsToThrowKey.setText("Packets to throw:");

		// setting GridBagLayout for all panels
		getContentPane().setLayout(new GridBagLayout());
		pnlPacketsToThrow.setLayout(new GridBagLayout());
		pnlPacketList.setLayout(new GridBagLayout());
		pnlSDP.setLayout(new GridBagLayout());
		pnlHexContents.setLayout(new GridBagLayout());
		pnlLocalSDP.setLayout(new GridBagLayout());
		pnlRemoteSDP.setLayout(new GridBagLayout());

		// adding Packets To Throw key to top panel
		GridBagConstraints gbcPacketsToThrowKey = new GridBagConstraints();
		gbcPacketsToThrowKey.gridheight = 1;
		gbcPacketsToThrowKey.gridwidth = 1;
		gbcPacketsToThrowKey.weightx = 0.0;
		gbcPacketsToThrowKey.weighty = 0.0;
		gbcPacketsToThrowKey.gridx = 0;
		gbcPacketsToThrowKey.gridy = 0;
		gbcPacketsToThrowKey.fill = GridBagConstraints.HORIZONTAL;
		gbcPacketsToThrowKey.insets = new Insets(0, 0, 0, 15);
		pnlPacketsToThrow.add(lblPacketsToThrowKey, gbcPacketsToThrowKey);

		// adding Packets To Throw value to top panel
		GridBagConstraints gbcPacketsToThrowValue = new GridBagConstraints();
		gbcPacketsToThrowValue.gridheight = 1;
		gbcPacketsToThrowValue.gridwidth = 1;
		gbcPacketsToThrowValue.weightx = 1.0;
		gbcPacketsToThrowValue.weighty = 0.0;
		gbcPacketsToThrowValue.gridx = 1;
		gbcPacketsToThrowValue.gridy = 0;
		gbcPacketsToThrowValue.fill = GridBagConstraints.HORIZONTAL;
		pnlPacketsToThrow.add(lblPacketsToThrowValue, gbcPacketsToThrowValue);

		// adding top panel to content pane
		GridBagConstraints gbcPacketsToThrow = new GridBagConstraints();
		gbcPacketsToThrow.gridheight = 1;
		gbcPacketsToThrow.gridwidth = 1;
		gbcPacketsToThrowValue.weightx = 1.0;
		gbcPacketsToThrowValue.weighty = 1.0;
		gbcPacketsToThrow.gridx = 0;
		gbcPacketsToThrow.gridy = 0;
		gbcPacketsToThrow.fill = GridBagConstraints.HORIZONTAL;
		gbcPacketsToThrow.insets = new Insets(5, 10, 15, 5);
		getContentPane().add(pnlPacketsToThrow, gbcPacketsToThrow);

		// adding SDP label to SDP panel
		GridBagConstraints gbcSDP = new GridBagConstraints();
		gbcSDP.gridheight = 1;
		gbcSDP.gridwidth = 2;
		gbcSDP.weightx = 1.0;
		gbcSDP.weighty = 0.0;
		gbcSDP.gridx = 0;
		gbcSDP.gridy = 0;
		gbcSDP.insets = new Insets(0, 0, 0, 10);
		gbcSDP.fill = GridBagConstraints.HORIZONTAL;
		pnlSDP.add(lblSDP, gbcSDP);

		// adding Local panel to SDP panel
		GridBagConstraints gbcLocalSDP = new GridBagConstraints();
		gbcLocalSDP.gridheight = 1;
		gbcLocalSDP.gridwidth = 1;
		gbcLocalSDP.weightx = 0.5;
		gbcLocalSDP.weighty = 1.0;
		gbcLocalSDP.gridx = 0;
		gbcLocalSDP.gridy = 1;
		gbcLocalSDP.fill = GridBagConstraints.BOTH;
		pnlSDP.add(pnlLocalSDP, gbcLocalSDP);

		// adding Remote panel to SDP panel
		GridBagConstraints gbcRemoteSDP = new GridBagConstraints();
		gbcRemoteSDP.gridheight = 1;
		gbcRemoteSDP.gridwidth = 1;
		gbcRemoteSDP.weightx = 0.5;
		gbcRemoteSDP.weighty = 1.0;
		gbcRemoteSDP.gridx = 1;
		gbcRemoteSDP.gridy = 1;
		gbcRemoteSDP.fill = GridBagConstraints.BOTH;
		pnlSDP.add(pnlRemoteSDP, gbcRemoteSDP);

		// adding scrolling text areas to SDP panels
		GridBagConstraints gbcSPLocalSDP = new GridBagConstraints();
		gbcSPLocalSDP.gridheight = 1;
		gbcSPLocalSDP.gridwidth = 1;
		gbcSPLocalSDP.weightx = 1.0;
		gbcSPLocalSDP.weighty = 1.0;
		gbcSPLocalSDP.gridx = 0;
		gbcSPLocalSDP.gridy = 0;
		gbcSPLocalSDP.fill = GridBagConstraints.BOTH;
		gbcSPLocalSDP.insets = new Insets(5, 5, 5, 5);
		
		pnlLocalSDP.add(spLocalSDP, gbcSPLocalSDP);
		pnlRemoteSDP.add(spRemoteSDP, gbcSPLocalSDP);

		// adding SDP panel to content pane
		GridBagConstraints gbcPanelSDP = new GridBagConstraints();
		gbcPanelSDP.gridheight = 1;
		gbcPanelSDP.gridwidth = 1;
		gbcPanelSDP.weightx = 1.0;
		gbcPanelSDP.weighty = 1.0;
		gbcPanelSDP.gridx = 0;
		gbcPanelSDP.gridy = 1;
		gbcPanelSDP.insets = new Insets(5, 10, 15, 5);
		gbcPanelSDP.fill = GridBagConstraints.BOTH;
		getContentPane().add(pnlSDP, gbcPanelSDP);

		// adding Incoming Packets label to content pane
		GridBagConstraints gbcIncomingPackets = new GridBagConstraints();
		gbcIncomingPackets.gridheight = 1;
		gbcIncomingPackets.gridwidth = 1;
		gbcIncomingPackets.weightx = 1.0;
		gbcIncomingPackets.weighty = 0.0;
		gbcIncomingPackets.gridx = 0;
		gbcIncomingPackets.gridy = 2;
		gbcIncomingPackets.insets = new Insets(5, 10, 5, 5);
		gbcIncomingPackets.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(lblIncomingPackets, gbcIncomingPackets);

		// adding packet list scroll pane to Packet list panel
		GridBagConstraints gbcTablePacketList = new GridBagConstraints();
		gbcTablePacketList.gridheight = 1;
		gbcTablePacketList.gridwidth = 1;
		gbcTablePacketList.weightx = 1.0;
		gbcTablePacketList.weighty = 1.0;
		gbcTablePacketList.gridx = 0;
		gbcTablePacketList.gridy = 0;
		gbcTablePacketList.fill = GridBagConstraints.BOTH;
		gbcTablePacketList.insets = new Insets(5, 5, 5, 5);
		pnlPacketList.add(spPacketList, gbcTablePacketList);

		// adding Packet list panel to content pane
		GridBagConstraints gbcPacketList = new GridBagConstraints();
		gbcPacketList.gridheight = 1;
		gbcPacketList.gridwidth = 1;
		gbcPacketList.weightx = 1.0;
		gbcPacketList.weighty = 1.0;
		gbcPacketList.gridx = 0;
		gbcPacketList.gridy = 3;
		gbcPacketList.insets = new Insets(5, 10, 5, 5);
		gbcPacketList.fill = GridBagConstraints.BOTH;
		getContentPane().add(pnlPacketList, gbcPacketList);

		// adding Packet Details scroll pane to Packet Detail Panel
		pnlPacketDetails.setLayout(new GridBagLayout());
		GridBagConstraints gbcTablePacketDetails = new GridBagConstraints();
		gbcTablePacketDetails.gridheight = 1;
		gbcTablePacketDetails.gridwidth = 1;
		gbcTablePacketDetails.weightx = 1.0;
		gbcTablePacketDetails.weighty = 1.0;
		gbcTablePacketDetails.gridx = 0;
		gbcTablePacketDetails.gridy = 0;
		gbcTablePacketDetails.fill = GridBagConstraints.BOTH;
		gbcTablePacketDetails.insets = new Insets(5, 5, 5, 5);
		pnlPacketDetails.add(spPacketDetail, gbcTablePacketDetails);

		gbcTablePacketDetails.gridy = 1;
		pnlPacketDetails.add(taDetail, gbcTablePacketDetails);

		// adding Packet Details panel to content pane
		GridBagConstraints gbcPacketDetails = new GridBagConstraints();
		gbcPacketDetails.gridheight = 1;
		gbcPacketDetails.gridwidth = 1;
		gbcPacketDetails.weightx = 1.0;
		gbcPacketDetails.weighty = 1.0;
		gbcPacketDetails.gridx = 0;
		gbcPacketDetails.gridy = 4;
		gbcPacketDetails.fill = GridBagConstraints.BOTH;
		gbcPacketDetails.insets = new Insets(5, 10, 5, 5);
		getContentPane().add(pnlPacketDetails, gbcPacketDetails);

		// adding Hex Dump scroll pane to Hex Dump panel
		GridBagConstraints gbcHexScroll = new GridBagConstraints();
		gbcHexScroll.gridheight = 1;
		gbcHexScroll.gridwidth = 1;
		gbcHexScroll.weightx = 1.0;
		gbcHexScroll.weighty = 1.0;
		gbcHexScroll.gridx = 0;
		gbcHexScroll.gridy = 0;
		gbcHexScroll.fill = GridBagConstraints.BOTH;
		gbcHexScroll.insets = new Insets(5, 5, 5, 5);
		pnlHexContents.add(spHexDump, gbcHexScroll);

		// adding Hex Dump panel to content pane
		GridBagConstraints gbcHexContents = new GridBagConstraints();
		gbcHexContents.gridheight = 1;
		gbcHexContents.gridwidth = 1;
		gbcHexContents.weightx = 1.0;
		gbcHexContents.weighty = 1.0;
		gbcHexContents.gridx = 0;
		gbcHexContents.gridy = 5;
		gbcHexContents.fill = GridBagConstraints.BOTH;
		gbcHexContents.insets = new Insets(5, 10, 5, 5);
		getContentPane().add(pnlHexContents, gbcHexContents);

		// adding event processing for table click
		tblPacketList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				Point point = evt.getPoint();
				int rowIndex = tblPacketList
						.convertRowIndexToModel(tblPacketList.rowAtPoint(point));
				tblPacketListRowNavigate(rowIndex);
			}
		});

		// adding event processing for table keyborad navigation
		tblPacketList.addKeyListener(new KeyAdapter() {

			@Override
			public void keyReleased(KeyEvent evt) {
				super.keyReleased(evt);
				if (evt.getKeyCode() == KeyEvent.VK_UP
						|| evt.getKeyCode() == KeyEvent.VK_DOWN) {
					int rowIndex = tblPacketList
							.convertRowIndexToModel(tblPacketList
									.getSelectedRow());
					tblPacketListRowNavigate(rowIndex);
				}
			}

		});
	}
	
	private void setHexPanelHeightAsHalfPacketListPanel() {
		Dimension size = spPacketList.getPreferredSize();
		size.height = spPacketList.getViewport().getHeight() - 
		spPacketList.getHorizontalScrollBar().getHeight();
		spPacketList.setPreferredSize(size);
		Dimension sizeHex = new Dimension(size);
		sizeHex.height = size.height / 2;
		spHexDump.setPreferredSize(sizeHex);
		spPacketDetail.setPreferredSize(sizeHex);
		taDetail.setPreferredSize(sizeHex);
	}

	private void tblPacketListRowNavigate(int rowIndex) {

		TextPacketInfo packet = packetListTableModel.getPacket(rowIndex);
		if (packet != null) {
			setDetailShowData(true);
		}
		//packetDetailsTableModel.clearModel();
		packetDetailsTableModel.setRedundantGenerations(packet.getRedundantGenerations());
		packetDetailsTableModel.fireTableDataChanged();
		tblPacketDetail.setModel(packetDetailsTableModel);
		tblPacketDetail.setColumnModel(packetDetailsTableModel.getColumnModel());
		taHexDump.setText(HexDump.dump(packet.getRawData()));
	}

	/**
	 * Show/hide detail table or "Select a packet..." text on detail panel
	 */
	private void setDetailShowData(boolean show) {
		spPacketDetail.setVisible(show);
		taDetail.setVisible(!show);
	}

	/**
	 * Sets SDP strings. The strings include newline characters (\n) that splits
	 * the lines. These strings shall immediately be shown in the SDP panels.
	 * 
	 * @param localSdp
	 *            The string that should be shown in the "Local" panel
	 * @param remoteSdp
	 *            The string that should be shown in the "Remote" panel
	 */
	public void setLocalSdp(String localSdp) {
		taLocalSDP.setText(localSdp);
	}
	public void setRemoteSdp(String remoteSdp) {
		taRemoteSDP.setText(remoteSdp);
	}

	/**
	 * This function either shows or hides the SDP panels (SDP heading, local
	 * and remote). When hidden, the dialog should be colapsed. I.e. there
	 * should be no empty space between panels above and under SDP panels.
	 * 
	 * @param show
	 *            True means that SDP panels should be shown, false means hide.
	 */
	public void setShowSdp(boolean show) {
		pnlSDP.setVisible(show);
	}

	/**
	 * This function either shows or hides the "Show packets to throw" label and
	 * the following number label. When hidden, the dialog should be colapsed.
	 * I.e. there should be no empty space between dialog top border and panels
	 * below.
	 * 
	 * @param show
	 *            True means that the two labels should be shown, false means
	 *            hide.
	 */
	public void setShowPacketsToThrow(boolean show) {
		pnlPacketsToThrow.setVisible(show);
	}

	/**
	 * Sets the number in the label to the right of "Packets to throw".
	 * 
	 * @param nbr
	 *            The number that should be printed in the label.
	 */
	public void setPacketsToThrow(int nbr) {
		lblPacketsToThrowValue.setText(Integer.toString(nbr));
	}
	
	public void setRedundancy(boolean isRedundancy) {
		AbstractPacketListModel newModel;
		int selectedRow = tblPacketList.getSelectedRow();
		pnlPacketDetails.setVisible(isRedundancy);
		if(!isRedundancy) {
			newModel = new PacketListTableModelRD();
		} else {
			newModel = new PacketListTableModel();
		}
		newModel.setPacketList(packetListTableModel.getPacketList());
		packetListTableModel = newModel;
		tblPacketList.setModel(packetListTableModel);
		tblPacketList.setColumnModel(packetListTableModel.getColumnModel());
		tblPacketList.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
		
	}

	/**
	 * Clears all panels and sets the dialog to its default state, see Javadoc
	 * for constructor to get information about default values and state. One
	 * exception: don't hide SDP and/or "Packets to throw" panels/labels, let
	 * their state remain.
	 * 
	 */
	public void clearAll() {
		lblPacketsToThrowValue.setText("0");
		setDetailShowData(false);
		if (packetDetailsTableModel != null) {
			packetDetailsTableModel.clearModel();
		}
		if (packetListTableModel != null) {
			packetListTableModel.clearModel();
		}
		taHexDump.setText("Select a packet in the list");
	}

	/**
	 * Adds a packet to the packet list.
	 * 
	 * @param packet
	 *            The packet.
	 */
	public void addPacket(final TextPacketInfo packet) {
		packetListTableModel.addTextPacketInfo(packet);
	}

}

/**
 * Abstract table model class for different packet list views
 *  
 */
abstract class AbstractPacketListModel extends AbstractTableModel {
	protected TableColumnModel columnModel;
	protected List<TextPacketInfo> packetList;
	private final static int INITIAL_PACKET_LIST_CAPACITY = 50;
	
	public AbstractPacketListModel() {
		packetList = new ArrayList<TextPacketInfo>(INITIAL_PACKET_LIST_CAPACITY);
		columnModel = new DefaultTableColumnModel();
		
		TableColumn colSeq = new TableColumn(0);
		colSeq.setPreferredWidth(50);
		colSeq.setMaxWidth(50);
		colSeq.setHeaderValue("Seq");

		TableColumn colPT = new TableColumn(1);
		colPT.setPreferredWidth(30);
		colPT.setMaxWidth(30);
		colPT.setHeaderValue("PT");
		
		columnModel.addColumn(colSeq);
		columnModel.addColumn(colPT);
		
		initializeTableModel();
	}

	/**
	 * This method must be overrided
	 */
	protected abstract void initializeTableModel();
	
	public TableColumnModel getColumnModel() {
		return columnModel;
	}

	@Override
	public int getColumnCount() {
		return columnModel.getColumnCount();
	}

	@Override
	public int getRowCount() {
		return packetList.size();
	}

	@Override
	public String getColumnName(int column) {
		return (String) columnModel.getColumn(column).getHeaderValue();
	}
	
	public void setPacketList(List<TextPacketInfo> packetList) {
		this.packetList = packetList;
		fireTableDataChanged();
	}
	
	public List<TextPacketInfo> getPacketList() {
		return packetList;
	}

	public TextPacketInfo getPacket(int index) {
		return packetList.get(index);
	}

	public void clearModel() {
		if (packetList != null) {
			packetList.clear();
		}
	}
	public void addTextPacketInfo(TextPacketInfo textPacketInfo) {
		packetList.add(textPacketInfo);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fireTableRowsInserted(getRowCount(), getRowCount());
			}

		});
	}		
}

/**
 * Table Model for packet list table (redundancy disabled)
 *
 */
@SuppressWarnings("serial")
class PacketListTableModelRD extends AbstractPacketListModel {
	private static final String DATA_FONT = "Courier New";
	
	public PacketListTableModelRD() {
		super();
	}
	
	@Override
	protected void initializeTableModel() {
		
		final Font dataFont = new Font(DATA_FONT, 0, 11);
		
		// setting packet list columns and it's preferences
		TableCellRenderer cellRenderer = new DefaultTableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);

				c.setFont(dataFont);
				return c;
			}
		};
		
		TableColumn colTime = new TableColumn(2);
		colTime.setPreferredWidth(100);
		colTime.setHeaderValue("Time");
		colTime.setCellRenderer(cellRenderer);
		
		TableColumn colLength = new TableColumn(3);
		colLength.setPreferredWidth(50);
		colLength.setHeaderValue("Length");
		colLength.setCellRenderer(cellRenderer);
		
		TableColumn colData = new TableColumn(4);
		colData.setPreferredWidth(100);
		colData.setHeaderValue("Data");
		colData.setCellRenderer(cellRenderer);
		
		columnModel.addColumn(colTime);
		columnModel.addColumn(colLength);
		columnModel.addColumn(colData);
		
	}

	@Override
	public Object getValueAt(int row, int column) {
		TextPacketInfo textPacketInfo;
		textPacketInfo = packetList.get(row);
		if(textPacketInfo.getPrimaryRedGen() == null) return null;
		switch (column) {
		case 0:
			return textPacketInfo.getSequenceNumber();
		case 1:
			return textPacketInfo.getPayloadType();
		case 2:
			return textPacketInfo.getPrimaryRedGen().getTime();
		case 3:
			return textPacketInfo.getPrimaryRedGen().getLength();
		case 4:
			return textPacketInfo.getPrimaryRedGen().getData();

		}
		return null;
	}

	
}

/**
 * Table Model for packet list table (redundancy enabled)
 */
@SuppressWarnings("serial")
class PacketListTableModel extends AbstractPacketListModel {

	private static final String DATA_FONT = "Courier New";

	public PacketListTableModel() {
		super();
	}

	@Override
	protected void initializeTableModel() {

		final Font dataFont = new Font(DATA_FONT, 0, 11);
		// setting packet list columns and it's preferences
		TableCellRenderer cellRenderer = new DefaultTableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);

				c.setFont(dataFont);
				return c;
			}

		};

//		TableColumn colSeq = new TableColumn(0);
//		colSeq.setPreferredWidth(50);
//		colSeq.setMaxWidth(50);
//		colSeq.setHeaderValue("Seq");
//
//		TableColumn colPT = new TableColumn(1);
//		colPT.setPreferredWidth(30);
//		colPT.setMaxWidth(30);
//		colPT.setHeaderValue("PT");

		TableColumn colPrimary = new TableColumn(2);
		colPrimary.setPreferredWidth(150);
		colPrimary.setHeaderValue("Prim. data");
		colPrimary.setCellRenderer(cellRenderer);

		TableColumn colData1 = new TableColumn(3);
		colData1.setPreferredWidth(150);
		colData1.setHeaderValue("1: Time and Data");
		colData1.setCellRenderer(cellRenderer);

		TableColumn colData2 = new TableColumn(4);
		colData2.setPreferredWidth(150);
		colData2.setHeaderValue("2: Time and Data");
		colData2.setCellRenderer(cellRenderer);

//		columnModel.addColumn(colSeq);
//		columnModel.addColumn(colPT);
		columnModel.addColumn(colPrimary);
		columnModel.addColumn(colData1);
		columnModel.addColumn(colData2);

	}

	@Override
	public Object getValueAt(int row, int column) {
		TextPacketInfo textPacketInfo;
		if (packetList == null) return null;
		textPacketInfo = packetList.get(row);
		switch (column) {
		case 0:
			return textPacketInfo.getSequenceNumber();
		case 1:
			return textPacketInfo.getPayloadType();
		case 2:
			RedGen primary = textPacketInfo.getPrimaryRedGen();
			if (primary != null) {
				return primary.getData();
			}
			return null;
		case 3:
			RedGen redGen1 = textPacketInfo.findRedGen(1);
			if (redGen1 != null) {
				return Long.toString(redGen1.getTime()) + "  "
						+ redGen1.getData();
			}
			return null;
		case 4:
			RedGen redGen2 = textPacketInfo.findRedGen(2);
			if (redGen2 != null) {
				return Long.toString(redGen2.getTime()) + "  "
						+ redGen2.getData();
			}
			return null;

		}
		return null;
	}
}

/**
 * Table Model for packet details table
 * 
 */
@SuppressWarnings("serial")
class PacketDetailsTableModel extends AbstractTableModel {

	private List<RedGen> generationsList;
	private static final String DATA_FONT = "Courier New";
	private TableColumnModel columnModel;

	public PacketDetailsTableModel() {
		columnModel = new DefaultTableColumnModel();
		initializeTableModel();
	}

	private void initializeTableModel() {

		final Font dataFont = new Font(DATA_FONT, 0, 11);
		// setting packet list columns and it's preferences
		TableCellRenderer cellRenderer = new DefaultTableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				Component c = super.getTableCellRendererComponent(table, value,
						isSelected, hasFocus, row, column);

				c.setFont(dataFont);
				return c;
			}

		};

		TableColumn colRedGen = new TableColumn(0);
		colRedGen.setPreferredWidth(20);
		colRedGen.setWidth(20);
		colRedGen.setHeaderValue("Red Gen");

		TableColumn colTime = new TableColumn(1);
		colTime.setPreferredWidth(20);
		colTime.setHeaderValue("Time");

		TableColumn colLength = new TableColumn(2);
		colLength.setPreferredWidth(20);
		colLength.setHeaderValue("Length");

		TableColumn colData = new TableColumn(3);
		colData.setPreferredWidth(300);
		colData.setHeaderValue("Data");
		colData.setCellRenderer(cellRenderer);

		columnModel.addColumn(colRedGen);
		columnModel.addColumn(colTime);
		columnModel.addColumn(colLength);
		columnModel.addColumn(colData);
	}
	
	public void setRedundantGenerations(List<RedGen> generationsList) {
		this.generationsList = generationsList;
	}

	public TableColumnModel getColumnModel() {
		return columnModel;
	}

	@Override
	public int getColumnCount() {
		return columnModel.getColumnCount();
	}

	@Override
	public int getRowCount() {
		return (generationsList == null)? 0: generationsList.size();
	}

	@Override
	public String getColumnName(int column) {
		return (String) columnModel.getColumn(column).getHeaderValue();
	}

	@Override
	public Object getValueAt(int row, int column) {
		RedGen redGen = generationsList.get(row);
		switch (column) {
		case 0:
			int generation = redGen.getGeneration();
			if (generation == 0)
				return "Primary";
			return generation;
		case 1:
			return redGen.getTime();
		case 2:
			return redGen.getLength();
		case 3:
			return redGen.getData();
		}
		return null;
	}

	public void clearModel() {
		if (generationsList != null) {
			generationsList.clear();
		}
	}
}

/**
 * Helper class for HEX dumping byte array to String
 * 
 */
class HexDump {

	/**
	 * The line-separator (initializes to "line.separator" system property.
	 */
	public static final String EOL = System.getProperty("line.separator");
	private static final char[] _hexcodes = { '0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	private static final int[] _shifts = { 28, 24, 20, 16, 12, 8, 4, 0 };

	/**
	 * Instances couldn't be constructed in standard programming.
	 */
	private HexDump() {
	}

	/**
	 * Dump an array of bytes to String object.
	 * 
	 * @param data
	 *            the byte array to be dumped
	 */

	public static String dump(byte[] data) {

		long offset = 0;
		int index = 0;

		long display_offset = offset + index;
		StringBuilder buffer = new StringBuilder(74);

		for (int j = index; j < data.length; j += 16) {
			int chars_read = data.length - j;

			if (chars_read > 16) {
				chars_read = 16;
			}
			for (int k = 0; k < 16; k++) {
				if (k < chars_read) {
					dump(buffer, data[k + j]);
				} else {
					buffer.append("  ");
				}
				if (k == 7) {
					buffer.append("  ");
				} else {
					buffer.append(' ');
				}
			}
			buffer.append("   ");
			for (int k = 0; k < chars_read; k++) {
				if ((data[k + j] >= ' ') && (data[k + j] < 127)) {
					buffer.append((char) data[k + j]);
				} else {
					buffer.append('.');
				}
			}
			buffer.append(EOL);
			display_offset += chars_read;
		}
		return buffer.toString();
	}

	private static StringBuilder dump(StringBuilder _cbuffer, byte value) {
		for (int j = 0; j < 2; j++) {
			_cbuffer.append(_hexcodes[(value >> _shifts[j + 6]) & 15]);
		}
		return _cbuffer;
	}
}
