/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/

/**
 * 2007, Modified by Posterita Ltd.
 */

package org.adempiere.webui.window;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.compiere.model.MLocator;
import org.compiere.model.MLocatorLookup;
import org.compiere.model.MRole;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Vbox;

/**
 * Location Dialog : Based on VLocationDialog
 * 
 * @author  Niraj Sohun
 * @date    Jul 24, 2007
 */

public class WLocatorDialog extends Window implements EventListener 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1013647722305985723L;

	private Vbox mainBox = new Vbox();
	
	private Listbox lstLocator = new Listbox();
	private Listbox lstWarehouse = new Listbox();
	private Listbox lstWMArea = new Listbox();
	
	private Checkbox chkCreateNew = new Checkbox();

	private Textbox txtWarehouse = new Textbox();
	private Textbox txtWMArea = new Textbox();
	private Textbox txtAisleX = new Textbox();
	private Textbox txtBinY = new Textbox();
	private Textbox txtLevelZ = new Textbox();
	private Textbox txtKey = new Textbox();
	
	private Label lblLocator = new Label();
	private Label lblWarehouse = new Label();
	private Label lblWMArea = new Label();
	private Label lblAisleX = new Label();
	private Label lblBinY = new Label();
	private Label lblLevelZ = new Label();
	private Label lblKey = new Label();
	
	private Button btnCancel = new Button();
	private Button btnOk = new Button();

	private MLocatorLookup m_mLocatorLookup;
	
	private int m_WindowNo;
	private int m_M_Locator_ID;
	private int m_AD_Client_ID;
	private int m_AD_Org_ID;
	private int m_only_Warehouse_ID;
	private int m_M_Warehouse_ID;
	private int m_WM_Area_ID;
	
	private boolean m_mandatory;
	
	private String m_M_WarehouseName;
	private String m_M_WarehouseValue;
	private String m_WM_AreaValue;
	private String m_Separator;

	private boolean m_change;

	private String title;

	private static CLogger log = CLogger.getCLogger(WLocatorDialog.class);
	
	/**
	 *	Constructor
	 *  @param title title
	 *  @param mLocator locator
	 *  @param M_Locator_ID locator id
	 * 	@param mandatory mandatory
	 * 	@param only_Warehouse_ID of not 0 restrict warehouse
	 *  @param windowNo
	 */
	
	public WLocatorDialog (	String title, MLocatorLookup mLocator,
							int M_Locator_ID, boolean mandatory, int only_Warehouse_ID, 
							int windowNo)
	{
		super();
		
		m_WindowNo = windowNo;
		this.title = title;
		initComponents();
		
		MLocator loc = MLocator.get(Env.getCtx(), M_Locator_ID);

		m_mLocatorLookup = mLocator;
		m_M_Locator_ID = M_Locator_ID;
		m_WM_Area_ID = loc.getWM_Area_ID();
		m_mandatory = mandatory;
		m_only_Warehouse_ID = only_Warehouse_ID;
		

		initLocator();
	} // WLocatorDialog
	
	private void initComponents()
	{
		lblLocator.setValue(Util.cleanAmp(Msg.translate(Env.getCtx(), "M_Locator_ID")));
		lblWarehouse.setValue(Util.cleanAmp(Msg.translate(Env.getCtx(), "M_Warehouse_ID")));
		lblWMArea.setValue(Util.cleanAmp(Msg.translate(Env.getCtx(), "WM_Area_ID")));
		lblAisleX.setValue(Util.cleanAmp(Msg.getElement(Env.getCtx(), "X")));
		lblBinY.setValue(Util.cleanAmp(Msg.getElement(Env.getCtx(), "Y")));
		lblLevelZ.setValue(Util.cleanAmp(Msg.getElement(Env.getCtx(), "Z")));
		lblKey.setValue(Util.cleanAmp(Msg.translate(Env.getCtx(), "Value")));
		
		Hbox boxLocator = new Hbox();
		boxLocator.setWidth("100%");
		boxLocator.setWidths("30%, 70%");
		
		lstLocator.setWidth("150px"); // Elaine 2009/02/02 - fixed the locator width
		lstLocator.setMold("select");
		lstLocator.setRows(0);
		
		boxLocator.appendChild(lblLocator);
		boxLocator.appendChild(lstLocator);
		
		Hbox boxCheckbox = new Hbox();
		boxCheckbox.setWidth("100%");
		boxCheckbox.setWidths("30%, 70%");
		boxCheckbox.setStyle("text-align:left");
		
		chkCreateNew.setLabel(Msg.getMsg(Env.getCtx(), "CreateNew"));
		
		boxCheckbox.appendChild(new Label());
		boxCheckbox.appendChild(chkCreateNew);
		
		Hbox boxWarehouse = new Hbox();
		boxWarehouse.setWidth("100%");
		boxWarehouse.setWidths("30%, 70%");
		
		lstWarehouse.setWidth("150px");
		lstWarehouse.setMold("select");
		lstWarehouse.setRows(0);
		
		boxWarehouse.appendChild(lblWarehouse);
		boxWarehouse.appendChild(lstWarehouse);
		boxWarehouse.appendChild(txtWarehouse);
		
		Hbox boxArea = new Hbox();
		boxArea.setWidth("100%");
		boxArea.setWidths("30%, 70%");
		
		lstWMArea.setWidth("150px");
		lstWMArea.setMold("select");
		lstWMArea.setRows(0);
		
		boxArea.appendChild(lblWMArea);
		boxArea.appendChild(lstWMArea);
		boxArea.appendChild(txtWMArea);

		Hbox boxAisle = new Hbox();
		boxAisle.setWidth("100%");
		boxAisle.setWidths("30%, 70%");
		
		boxAisle.appendChild(lblAisleX);
		boxAisle.appendChild(txtAisleX);
		
		Hbox boxBin = new Hbox();
		boxBin.setWidth("100%");
		boxBin.setWidths("30%, 70%");

		boxBin.appendChild(lblBinY);
		boxBin.appendChild(txtBinY);
		
		Hbox boxLevel = new Hbox();
		boxLevel.setWidth("100%");
		boxLevel.setWidths("30%, 70%");

		boxLevel.appendChild(lblLevelZ);
		boxLevel.appendChild(txtLevelZ);
		
		Hbox boxKey = new Hbox();
		boxKey.setWidth("100%");
		boxKey.setWidths("30%, 70%");
		
		boxKey.appendChild(lblKey);
		boxKey.appendChild(txtKey);
		
		Hbox boxButtons = new Hbox();
		boxButtons.setWidth("100%");
		boxButtons.setWidths("80%, 10%, 10%");
		boxButtons.setStyle("text-align:right");
		
		btnCancel.setImage("/images/Cancel16.png");
		btnCancel.addEventListener(Events.ON_CLICK, this);
		
		btnOk.setImage("/images/Ok16.png");
		btnOk.addEventListener(Events.ON_CLICK, this);
		
		boxButtons.appendChild(new Label());
		boxButtons.appendChild(btnCancel);
		boxButtons.appendChild(btnOk);
		
		mainBox.setWidth("250px");
		mainBox.setStyle("text-align:right");
		mainBox.appendChild(boxLocator);
		mainBox.appendChild(new Separator());
		mainBox.appendChild(boxCheckbox);
		mainBox.appendChild(new Separator());
		mainBox.appendChild(boxWarehouse);
		mainBox.appendChild(boxArea);
		mainBox.appendChild(boxAisle);
		mainBox.appendChild(boxBin);
		mainBox.appendChild(boxLevel);
		mainBox.appendChild(boxKey);
		mainBox.appendChild(new Separator());
		mainBox.appendChild(boxButtons);
		
		this.appendChild(mainBox);
		this.setTitle(title);
		this.setClosable(true);
		this.setBorder("normal");
		this.setWidth("260Px");
		this.setAttribute("mode","modal");
		this.setSizable(true); // Elaine 2009/02/02 - window set to resizable
	}
	
	private void initLocator()
	{
		log.fine("");

		//	Load Warehouse
		
		String sql = "SELECT M_Warehouse_ID, Name FROM M_Warehouse";
		
		if (m_only_Warehouse_ID != 0)
			sql += " WHERE M_Warehouse_ID=" + m_only_Warehouse_ID;
		
		String SQL = MRole.getDefault().addAccessSQL(
			sql, "M_Warehouse", MRole.SQL_NOTQUALIFIED, MRole.SQL_RO)
			+ " ORDER BY 2";
		
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(SQL, null);
			ResultSet rs = pstmt.executeQuery();
			
			while (rs.next())
			{
				KeyNamePair key = new KeyNamePair(rs.getInt(1), rs.getString(2));
				lstWarehouse.appendItem(key.getName(), key);
			}
			
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, SQL, e);
		}

		log.fine("Warehouses=" + lstWarehouse.getItemCount());

		loadWHArea(m_only_Warehouse_ID, m_WM_Area_ID);
		log.fine("Areas=" + lstWMArea.getItemCount());

		//	Load existing Locators

		m_mLocatorLookup.fillComboBox(m_mandatory, true, true, false);
		
		log.fine(m_mLocatorLookup.toString());
		
		int selectedIndex = 0;
		for (int i = 0; i < m_mLocatorLookup.getSize(); i++)
		{
			MLocator mLoc = (MLocator) m_mLocatorLookup.getElementAt(i);
			if (mLoc.getM_Locator_ID() == m_M_Locator_ID) {
				selectedIndex = i;
			}
			lstLocator.appendItem(mLoc.toString(), (Object) mLoc);
		}
		
		//lstLocator.setModel(m_mLocator);
		//lstLocator.setValue(m_M_Locator_ID);
		lstLocator.setSelectedIndex(selectedIndex);
		lstLocator.addEventListener(Events.ON_SELECT, this);
		
		displayLocator();

		chkCreateNew.setChecked(false);
		chkCreateNew.addEventListener(Events.ON_CHECK, this);
		
		enableNew();

		lstWarehouse.addEventListener(Events.ON_SELECT, this);
		lstWMArea.addEventListener(Events.ON_SELECT, this);
		
		txtAisleX.addEventListener(Events.ON_CHANGE, this);
		txtBinY.addEventListener(Events.ON_CHANGE, this);
		txtLevelZ.addEventListener(Events.ON_CHANGE, this);
		
		//	Update UI
		
		//pack();
		
	} // initLocator

	private void displayLocator()
	{
		MLocator l = null;
		ListItem listitem = lstLocator.getSelectedItem();
		
		if (listitem != null)
			l = (MLocator) listitem.getValue();
		
		if (l == null)
			return;

		m_M_Locator_ID = l.getM_Locator_ID();
		
		txtWarehouse.setText(l.getWarehouseName());
		txtAisleX.setText(l.getX());
		txtBinY.setText(l.getY());
		txtLevelZ.setText(l.getZ());
		txtKey.setText(l.getValue());
		
		getWarehouseInfo(l.getM_Warehouse_ID(), l.getWM_Area_ID());
		
		//	Set Warehouse
		
		int size = lstWarehouse.getItemCount();
		
		for (int i = 0; i < size; i++)
		{
			ListItem listItem = lstWarehouse.getItemAtIndex(i);
			KeyNamePair pp = (KeyNamePair)listItem.getValue();
			
			if (pp.getKey() == l.getM_Warehouse_ID())
			{
				lstWarehouse.setSelectedIndex(i);
				continue;
			}
		}
		loadWHArea(l.getM_Warehouse_ID(), l.getWM_Area_ID());
	} // displayLocator
	
	/**
	 *	Enable/disable New data entry
	 */
	
	private void enableNew()
	{
		boolean sel = chkCreateNew.isChecked();
		//lblWarehouse.setVisible(sel);
		lstWarehouse.setVisible(sel);
		lstWMArea.setVisible(sel);
		//lWarehouseInfo.setVisible(!sel);
		txtWarehouse.setVisible(!sel);
		txtWMArea.setVisible(!sel);
		
		txtWarehouse.setReadonly(true);
		txtWMArea.setReadonly(true);
		txtAisleX.setReadonly(!sel);
		txtBinY.setReadonly(!sel);
		txtLevelZ.setReadonly(!sel);
		txtKey.setReadonly(!sel);
		
		//pack();
	} // enableNew

	/**
	 *	Get Warehouse Info
	 *  @param M_Warehouse_ID warehouse
	 */
	private void getWarehouseInfo (int M_Warehouse_ID, int WM_Area_ID)
	{
		if (M_Warehouse_ID == m_M_Warehouse_ID && WM_Area_ID == m_WM_Area_ID)
			return;

		//	Defaults
		
		m_M_Warehouse_ID = 0;
		m_M_WarehouseName = "";
		m_M_WarehouseValue = "";
		m_WM_Area_ID = 0;
		m_WM_AreaValue = "";
		m_Separator = ".";
		m_AD_Client_ID = 0;
		m_AD_Org_ID = 0;

		String SQL = "SELECT M_Warehouse_ID, Value, Name, Separator, AD_Client_ID, AD_Org_ID "
			+ "FROM M_Warehouse WHERE M_Warehouse_ID=?";
		
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(SQL, null);
			pstmt.setInt(1, M_Warehouse_ID);
			ResultSet rs = pstmt.executeQuery();
		
			if (rs.next())
			{
				m_M_Warehouse_ID = rs.getInt(1);
				m_M_WarehouseValue = rs.getString(2);
				m_M_WarehouseName = rs.getString(3);
				m_Separator = rs.getString(4);
				m_AD_Client_ID = rs.getInt(5);
				m_AD_Org_ID = rs.getInt(6);
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, SQL, e);
		}

		//  WM_Area
		SQL = "SELECT WM_Area_ID, Value "
			+ "FROM WM_Area WHERE M_Warehouse_ID=? AND WM_Area_ID=?";
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(SQL, null);
			pstmt.setInt(1, M_Warehouse_ID);
			pstmt.setInt(2, WM_Area_ID);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
			{
				m_WM_Area_ID = rs.getInt(1);
				m_WM_AreaValue = rs.getString(2);
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, SQL, e);
		}
	} // getWarehouseInfo

	/**
	 *	Create Locator-Value
	 */
	
	private void createValue()
	{
		// Get Warehouse Info
		int wmArea = 0;
		
		ListItem listitem = lstWarehouse.getSelectedItem();
		KeyNamePair pp = (KeyNamePair)listitem.getValue();
		
		if (pp == null)
			return;

		listitem = lstWMArea.getSelectedItem();
		KeyNamePair pa = (KeyNamePair)listitem.getValue();
		
		if (pa != null)
			wmArea = pa.getKey();

		getWarehouseInfo(pp.getKey(), wmArea);
		
		String value = MLocator.createValueString(m_M_WarehouseValue, m_WM_AreaValue,
				txtAisleX.getText(), txtBinY.getText(), txtLevelZ.getText(), m_Separator);
		
		txtKey.setText(value);
	} // createValue

	/**
	 * 	OK - check for changes (save them) & Exit
	 */
	
	private void actionOK()
	{
		if (chkCreateNew.isChecked())
		{
			//	Get Warehouse Info
			int wmArea = 0;
			
			ListItem listitem = lstWarehouse.getSelectedItem();
			KeyNamePair pp = (KeyNamePair)listitem.getValue();
			
			listitem = lstWMArea.getSelectedItem();
			KeyNamePair pa = (KeyNamePair)listitem.getValue();
			
			if (pa != null)
				wmArea = pa.getKey();

			getWarehouseInfo(pp.getKey(), wmArea);

			//	Check mandatory values
			
			String mandatoryFields = "";
			
			if (m_M_Warehouse_ID == 0)
				mandatoryFields += lblWarehouse.getValue() + " - ";
			
			if (txtKey.getText().length()==0)
				mandatoryFields += lblKey.getValue() + " - ";
			
			if (txtAisleX.getText().length()==0)
				mandatoryFields += lblAisleX.getValue() + " - ";
			
			if (txtBinY.getText().length()==0)
				mandatoryFields += lblBinY.getValue() + " - ";
			
			if (txtLevelZ.getText().length()==0)
				mandatoryFields += lblLevelZ.getValue() + " - ";
			
			if (mandatoryFields.length() != 0)
			{
				FDialog.error(m_WindowNo, this, "FillMandatory", mandatoryFields.substring(0, mandatoryFields.length()-3));
				return;
			}

			MLocator loc = MLocator.get(Env.getCtx(), m_M_Warehouse_ID, m_WM_Area_ID, txtKey.getText(),
				txtAisleX.getText(), txtBinY.getText(), txtLevelZ.getText());
			
			m_M_Locator_ID = loc.getM_Locator_ID();
			
			listitem = new ListItem();
			listitem.setValue(loc);
			
			lstLocator.appendItem(loc.get_TableName(), loc);
			lstLocator.setSelectedIndex(lstLocator.getItemCount() - 1);
		} // createNew

		log.config("M_Locator_ID=" + m_M_Locator_ID);
	} // actionOK

	/**
	 *	Get Selected value
	 *  @return value as Integer
	 */
	
	public Integer getValue()
	{
		ListItem listitem = lstLocator.getSelectedItem();
		MLocator l = (MLocator) listitem.getValue();
		
		if (l != null && l.getM_Locator_ID() != 0)
			return new Integer (l.getM_Locator_ID());
		
		return null;
	} // getValue

	/**
	 *	Get result
	 *  @return true if changed
	 */
	
	public boolean isChanged()
	{
		if (m_change)
		{
			ListItem listitem = lstLocator.getSelectedItem();
			MLocator l = (MLocator)listitem.getValue();
			
			if (l != null)
				return l.getM_Locator_ID() == m_M_Locator_ID;
		}
		return m_change;
	} // getChange

	
	public void onEvent(Event event) throws Exception 
	{
		if (event == null)
			return;
		
		if (event.getTarget() == btnCancel)
		{
			m_change = false;
			this.detach();
		}
		else if (event.getTarget() == btnOk)
		{
			actionOK();
			m_change = true;
			this.detach();
		}
		else if (event.getTarget() == lstLocator)
			displayLocator();
		else if (event.getTarget() == chkCreateNew)
			enableNew();
		//	Entered/Changed data for Value
		else if (chkCreateNew.isChecked() && (
				event.getTarget() == lstWarehouse
				|| event.getTarget() == lstWMArea
				|| event.getTarget() == txtAisleX
				|| event.getTarget() == txtBinY
				|| event.getTarget() == txtLevelZ)) {
			createValue();
		}
	}
	
	public void loadWHArea(int M_Warehouse_ID, int WM_Area_ID) {
		//	Load Warehouse Areas
		// Clear the combo box
		lstWMArea.removeAllItems();
		// Add a zero value = no assignment
		lstWMArea.appendItem("", new KeyNamePair(0,""));
		String sql = "SELECT WM_Area_ID, Name FROM WM_Area";
		if (M_Warehouse_ID != 0) {
			sql += " WHERE M_Warehouse_ID=" + M_Warehouse_ID;
		}
		else { // Use the value selected in the warehouse field
			KeyNamePair pp = (KeyNamePair) lstWarehouse.getSelectedItem().getValue();
			if (pp != null) {
				sql += " WHERE M_Warehouse_ID=" + pp.getKey();
			}
		}
		int selectedIndex = 0;
		String SQL = MRole.getDefault().addAccessSQL(
			sql, "WM_Area", MRole.SQL_NOTQUALIFIED, MRole.SQL_RO)
			+ " ORDER BY 2";
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(SQL, null);
			ResultSet rs = pstmt.executeQuery();
			int i = 0;  // Take into account the null entry ...
			while (rs.next()) {
				i++; // ...here.
				KeyNamePair key = new KeyNamePair(rs.getInt(1), rs.getString(2));
				lstWMArea.appendItem(key.getName(), key);
				if (rs.getInt(1) == WM_Area_ID) {
					lstWMArea.setSelectedIndex(i);
					txtWMArea.setValue(rs.getString(2));					
				}
					selectedIndex = i; 
			}
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			log.log(Level.SEVERE, SQL, e);
		}
	}
}
