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
package org.compiere.grid.ed;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;

import org.adempiere.exceptions.DBException;
import org.adempiere.plaf.AdempierePLAF;
import org.compiere.apps.ADialog;
import org.compiere.apps.AEnv;
import org.compiere.apps.AWindow;
import org.compiere.apps.RecordInfo;
import org.compiere.apps.search.InfoPAttribute;
import org.compiere.apps.search.InfoProduct;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MColumn;
import org.compiere.model.MLookup;
import org.compiere.model.MPAttributeLookup;
import org.compiere.model.MProduct;
import org.compiere.model.MQuery;
import org.compiere.model.Query;
import org.compiere.swing.CButton;
import org.compiere.swing.CDialog;
import org.compiere.swing.CMenuItem;
import org.compiere.swing.CTextField;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Msg;
import org.compiere.util.ValueNamePair;

/**
 *  Product Attribute Set Instance Editor
 *
 *  @author Jorg Janke
 *  @version $Id: VPAttribute.java,v 1.2 2006/07/30 00:51:27 jjanke Exp $
 *  
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1895041 ] NPE when move product with attribute set
 * 			<li>BF [ 1770177 ] Inventory Move Locator Error - integrated MGrigioni bug fix
 * 			<li>BF [ 2011222 ] ASI Dialog is reseting locator
 * 
 * @author Michael McKay, mckayERP www.mckayERP.com 
 * 				<li>ADEMPIERE-72 VLookup and Info Window improvements
 * 					https://adempiere.atlassian.net/browse/ADEMPIERE-72
 * 				<li>#278 Add Lookup to the popup menu
 * 				<li>#280 ASI field should accept text input
 * 
 * @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *		<li> FR [ 146 ] Remove unnecessary class, add support for info to specific column
 *		@see https://github.com/adempiere/adempiere/issues/146
 *
 */
public class VPAttribute extends JComponent
	implements VEditor, ActionListener, FocusListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1823370077523962901L;

	/**
	 *	Mouse Listener
	 */
	final class VPAttribute_mouseAdapter extends MouseAdapter
	{
		/**
		 *	Constructor
		 *  @param adaptee adaptee
		 */
		VPAttribute_mouseAdapter(VPAttribute adaptee)
		{
			m_adaptee = adaptee;
		}	//	VPAttribute_mouseAdapter

		private VPAttribute m_adaptee;

		/**
		 *	Mouse Listener
		 *  @param e event
		 */
		public void mouseClicked(MouseEvent e)
		{
			//	Double Click
			if (e.getClickCount() > 1)
				m_adaptee.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "Mouse"));
			//	popup menu
			if (SwingUtilities.isRightMouseButton(e))
				m_adaptee.popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
		}	//	mouse Clicked

	}	//	VPAttribute_mouseAdapter

	private CMenuItem menuInfo;
	
	/**
	 *	IDE Constructor
	 */
	public VPAttribute()
	{
		this (null, false, false, true, 0, (MLookup) null, false);
	}

	/**
	 *	Create Product Attribute Set Instance Editor.
	 *  @param mandatory mandatory
	 *  @param isReadOnly read only
	 *  @param isUpdateable updateable
	 * 	@param WindowNo WindowNo
	 * 	@param pAttribute Model Product Attribute
	 */
	@Deprecated
	public VPAttribute (boolean mandatory, boolean isReadOnly, boolean isUpdateable, 
		int WindowNo, MPAttributeLookup pAttribute, boolean searchOnly)
	{
		this(null, mandatory, isReadOnly, isUpdateable, WindowNo, (MLookup) null, searchOnly);
	}

	/**
	 *	Create Product Attribute Set Instance Editor.
	 *  @param mandatory mandatory
	 *  @param isReadOnly read only
	 *  @param isUpdateable updateable
	 * 	@param WindowNo WindowNo
	 * 	@param lookup the colum lookup model (MLookup)
	 */
	public VPAttribute (boolean mandatory, boolean isReadOnly, boolean isUpdateable, 
		int WindowNo, MLookup lookup, boolean searchOnly)
	{
		this(null, mandatory, isReadOnly, isUpdateable, WindowNo, lookup, searchOnly);
	}

	/**
	 *	Create Product Attribute Set Instance Editor.
	 *  @param gridTab
	 *  @param mandatory mandatory
	 *  @param isReadOnly read only
	 *  @param isUpdateable updateable
	 * 	@param WindowNo WindowNo
	 * 	@param pAttribute Model Product Attribute
	 *  @param searchOnly True if only used to search instances
	 */
	@Deprecated
	public VPAttribute (GridTab gridTab, boolean mandatory, boolean isReadOnly, boolean isUpdateable, 
		int WindowNo, MPAttributeLookup pAttribute, boolean searchOnly)
	{
		this(gridTab, mandatory, isReadOnly, isUpdateable, WindowNo, (MLookup) null, searchOnly);
	}
	
	/**
	 * Create Product Attribute Set Instance Editor.
	 * @param gridTab
	 * @param mandatory Set true if the field is mandatory
	 * @param isReadOnly Set true if the field is read only
	 * @param isUpdateable Set true if the field can be updated
	 * @param WindowNo The parent window number
	 * @param lookup The MLookup to use
	 * @param searchOnly Set true if the field is to be used to 
	 * search only and should not hold a value.
	 */
	public VPAttribute(GridTab gridTab, boolean mandatory, boolean isReadOnly,
			boolean isUpdateable, int WindowNo, MLookup lookup, boolean searchOnly) {
		super();
		super.setName(m_columnName);
		m_value = 0;
		m_GridTab = gridTab; // added for processCallout
		m_WindowNo = WindowNo;
//		m_mPAttribute = new MPAttributeLookup(Env.getCtx(), WindowNo);
		m_lookup = lookup;
		m_C_BPartner_ID = Env.getContextAsInt(Env.getCtx(), WindowNo, "C_BPartner_ID");
		m_searchOnly = searchOnly;		
		m_text.setName("VPAttribute Text - " + m_columnName);
		m_button.setName("VPAttribute Button - " + m_columnName);

		// The creating function should set the field and name. See VEditorFactory.
		// To initialize the field in cases of forms, set the field to null.
		setField(null);    
		
		LookAndFeel.installBorder(this, "TextField.border");
		this.setLayout(new BorderLayout());
		//  Size
		this.setPreferredSize(m_text.getPreferredSize());
		int height = m_text.getPreferredSize().height;
		
		//	***	Text	***
		m_text.setEditable(true);
		m_text.setFocusable(true);
		m_text.setBorder(null);
		m_text.setHorizontalAlignment(JTextField.LEADING);
		m_text.addActionListener(this);
		m_text.addFocusListener(this);
	//	Background
		setMandatory(mandatory);
		this.add(m_text, BorderLayout.CENTER);

		//	***	Button	***
		m_button.setIcon(Env.getImageIcon("PAttribute10.gif"));
		m_button.setMargin(new Insets(0, 0, 0, 0));
		m_button.setPreferredSize(new Dimension(height, height));
		m_button.addActionListener(this);
		m_button.setFocusable(true);
		this.add(m_button, BorderLayout.EAST);

		//	Prefereed Size
		this.setPreferredSize(this.getPreferredSize());		//	causes r/o to be the same length
		//	ReadWrite
		if (isReadOnly || !isUpdateable)
			setReadWrite(false);
		else
			setReadWrite(true);

		//	Popup
		m_text.addMouseListener(new VPAttribute_mouseAdapter(this));
        menuInfo = new CMenuItem(Msg.getMsg(Env.getCtx(), "Info"), Env.getImageIcon("Zoom16.gif"));
		menuZoom = new CMenuItem(Msg.getMsg(Env.getCtx(), "Zoom"), Env.getImageIcon("Zoom16.gif"));
		menuInfo.addActionListener(this);
		menuZoom.addActionListener(this);
		popupMenu.add(menuZoom);
		popupMenu.add(menuInfo);
		
		set_oldValue();
	}	//	VPAttribute

	/**	Data Value				*/
	private Object				m_value = new Object();
	/** Attribute Where Clause  */
	private String m_pAttributeWhere = null;
	/** Column Name - fixed		*/
	private String				m_columnName = "M_AttributeSetInstance_ID";
	/** The Attribute Instance	*/
//	private MPAttributeLookup	m_mPAttribute;

	/** The Text Field          */
	private CTextField			m_text = new CTextField();
	/** The Button              */
	private CButton				m_button = new CButton();

	JPopupMenu          		popupMenu = new JPopupMenu();
	private CMenuItem 			menuZoom;

	private boolean				m_readWrite;
	private boolean				m_mandatory;
	private int					m_WindowNo;
	private int					m_C_BPartner_ID;
	private boolean 			m_searchOnly;
	private boolean 			isProductWindow;
	/** The Grid Tab * */
	private GridTab m_GridTab; // added for processCallout
	/** The Grid Field * */
	private GridField m_GridField; // added for processCallout
	
	/**	Calling Window Info				*/
	private int					m_AD_Column_ID = 0;
	/** record the value for comparison at a point in the future */
	private Integer m_oldValue = 0;
	private String m_oldText = "";
	private String m_oldWhere = "";
	private boolean m_haveFocus;
	/** The last display value.  The text displayed can change without the underlying
	 *  value changing so this variable provides a means to test if a change has occurred.
	 */
	private String m_lastDisplay;
	private int M_Product_ID = 0;
	private int M_ProductBOM_ID = 0;
	private int M_AttributeSet_ID;
	private MLookup m_lookup;

	/**	No Instance Key					*/
	private static Integer		NO_INSTANCE = new Integer(0);
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(VPAttribute.class);
		

	/**
	 * 	Dispose resources
	 */
	public void dispose()
	{
		m_text = null;
		m_button = null;
		m_lookup.dispose();
		m_lookup = null;
//		m_mPAttribute.dispose();
//		m_mPAttribute = null;
		m_GridField = null;
		m_GridTab = null;
	}	//	dispose

	/**
	 * 	Set Mandatory
	 * 	@param mandatory mandatory
	 */
	public void setMandatory (boolean mandatory)
	{
		m_mandatory = mandatory;
		m_button.setMandatory(mandatory);
		setBackground (false);
	}	//	setMandatory

	/**
	 * 	Get Mandatory
	 *  @return mandatory
	 */
	public boolean isMandatory()
	{
		return m_mandatory;
	}	//	isMandatory

	/**
	 * 	Set ReadWrite
	 * 	@param rw read write
	 */
	public void setReadWrite (boolean rw)
	{
		m_readWrite = rw;
		enableControl();
		//m_button.setReadWrite(rw);
		//setBackground (false);
	}	//	setReadWrite

	/**
	 * 	Is Read Write
	 * 	@return read write
	 */
	public boolean isReadWrite()
	{
		return m_readWrite;
	}	//	isReadWrite

	/**
	 * 	Set Foreground
	 * 	@param color color
	 */
	public void setForeground (Color color)
	{
		m_text.setForeground(color);
	}	//	SetForeground

	/**
	 * 	Set Background
	 * 	@param error Error
	 */
	public void setBackground (boolean error)
	{
		if (error)
			setBackground(AdempierePLAF.getFieldBackground_Error());
		else if (!m_readWrite)
			setBackground(AdempierePLAF.getFieldBackground_Inactive());
		else if (m_mandatory)
			setBackground(AdempierePLAF.getFieldBackground_Mandatory());
		else
			setBackground(AdempierePLAF.getInfoBackground());
	}	//	setBackground

	/**
	 * 	Set Background
	 * 	@param color Color
	 */
	public void setBackground (Color color)
	{
		m_text.setBackground(color);
	}	//	setBackground

	
	/**************************************************************************
	 * 	Set/lookup Value
	 * 	@param value value
	 */
	public void setValue(Object value)
	{
		log.fine(m_columnName + "=" + value);
		if (value == null || NO_INSTANCE.equals(value))
		{
			m_text.setText("");
			m_text.setToolTipText("");
			m_value = value;
			m_lastDisplay = "";
			m_pAttributeWhere = "";
		}
		
		//	changed
		else if (!value.equals(m_value)) {
			//	new value
			m_value = value;
			m_pAttributeWhere = "EXISTS (SELECT * FROM M_Storage s "
					+ "WHERE s.M_AttributeSetInstance_ID=" + value
					+ " AND s.M_Product_ID=p.M_Product_ID)";
		}
		// Reset the display whether a change was made or not - in case text was entered and cancelled
		m_text.setText(m_lookup.getDisplay(value));	//	loads value
		// The text can be long.  Use the tooltip to help display the info.
		m_text.setToolTipText(m_text.getText());

		m_lastDisplay = m_text.getText();
		
		enableControl();

		return;
	}	//	setValue

	private void enableControl() {

		setM_Product_ID();
		// Enable or disable controls
		MAttributeSet as = null;
		MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
		if (product !=null && product.getM_AttributeSet() != null) {
			as = product.getAttributeSet();
		}
		
		boolean enabled = true;
		if (as != null) {
			// Enable the control if the control has a non zero value or is not excluded.
			enabled = ((m_value != null && !NO_INSTANCE.equals(m_value)) || !as.excludeEntry(m_AD_Column_ID, Env.isSOTrx(Env.getCtx(),m_WindowNo)));
			m_button.setEnabled(m_readWrite && (isProductWindow || m_searchOnly || enabled));
			m_text.setEnabled(m_readWrite && (isProductWindow || m_searchOnly || enabled));
		}
		else {
			m_button.setEnabled(m_readWrite && (isProductWindow || m_searchOnly));
			m_text.setEnabled(m_readWrite && (isProductWindow || m_searchOnly));
		}
		
		if (m_GridField != null) {  // The column is found
			int AD_Column_ID = m_GridField.getAD_Column_ID();
			if (product != null) {
				// Set column error if the ASI is mandatory 
				Properties ctx = Env.getCtx();
				Boolean isSOTrx = Env.isSOTrx(ctx, m_WindowNo);
				Integer M_AttributeSetInstance_ID = null;
				if (getValue() != null) {
					M_AttributeSetInstance_ID = (Integer) getValue();
				}	
				m_GridField.setError(!product.isValidAttributeSetInstance(ctx, isSOTrx, AD_Column_ID, M_AttributeSetInstance_ID));
			}
			else { // No product - so no ASI
				m_GridField.setError(false);
				//column.setDisplayed(false);
			}
		}
	}

	/**
	 * Set the M_Product_ID value from the context.  If there is a M_ProductBOM_ID 
	 * defined, that ID will be used.
	 */
	private void setM_Product_ID() {
		// Get the product
		if (m_GridTab != null) {
			M_Product_ID = Env.getContextAsInt (Env.getCtx (), m_WindowNo, m_GridTab.getTabNo(), "M_Product_ID");
			M_ProductBOM_ID = Env.getContextAsInt (Env.getCtx (), m_WindowNo, m_GridTab.getTabNo(), "M_ProductBOM_ID");
		} else {
			M_Product_ID = Env.getContextAsInt (Env.getCtx (), m_WindowNo, "M_Product_ID");
			M_ProductBOM_ID = Env.getContextAsInt (Env.getCtx (), m_WindowNo, "M_ProductBOM_ID");
		}
		if (M_ProductBOM_ID != 0)	//	Use BOM Component
			M_Product_ID = M_ProductBOM_ID;		
	}

	/**
	 * 	Get Value
	 * 	@return value
	 */
	public Object getValue()
	{
		Integer temp = null;
		if (m_value != null || NO_INSTANCE.equals(m_value)) {
			try {
				temp = (Integer) m_value;
			}
			catch (ClassCastException cce)
			{
				temp = null;
			}
		}
		return temp;
	}	//	getValue

	/**
	 * Get Attribute Where clause
	 * @return String
	 */
	public String getAttributeWhere()
	{
		return m_pAttributeWhere;
	}	//	getAttributeWhere()

	/**
	 * 	Get Display Value
	 *	@return info
	 */
	public String getDisplay()
	{
		return m_text.getText();
	}	//	getDisplay

	
	/**************************************************************************
	 * 	Set Field
	 * 	@param mField MField
	 */
	public void setField(GridField mField)
	{
		//	To determine behaviour
		m_GridField = mField;
		
		if (m_GridField != null) {
			m_columnName = m_GridField.getColumnName();
			m_AD_Column_ID = m_GridField.getAD_Column_ID();
			RecordInfo.addMenu(this, popupMenu);
		}
		else {
			m_columnName = "M_AttributeSetInstance_ID";
			m_AD_Column_ID = 0;
		}
		//	M_Product.M_AttributeSetInstance_ID = 8418
		isProductWindow = m_AD_Column_ID == MColumn.getColumn_ID(MProduct.Table_Name, MProduct.COLUMNNAME_M_AttributeSetInstance_ID);
		
		enableControl();
	}	//	setField
	
	@Override
	public GridField getField() {
		return m_GridField;
	}

	/**
	 *  Action Listener Interface
	 *  @param listener listener
	 */
	public void addActionListener(ActionListener listener)
	{
		m_text.addActionListener(listener);
	}   //  addActionListener

	/**
	 * 	Action Listener - start dialog
	 * 	@param e Event
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals(RecordInfo.CHANGE_LOG_COMMAND))
		{
			RecordInfo.start(m_GridField);
			return;
		}
		
		if (e.getSource() instanceof CTextField)
			actionText();
		else if (e.getSource() instanceof CButton || e.getSource() == menuInfo)
			actionButton();
		
		//  Popup Menu
		else if (e.getSource() == menuZoom)
			actionZoom();
		
		requestFocus();
	}	//	actionPerformed

	/**
	 *  Property Change Listener
	 *  @param evt event
	 */
	public void propertyChange (PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(org.compiere.model.GridField.PROPERTY))
			setValue(evt.getNewValue());
	}   //  propertyChange
	/**
	 * Set the old value of the field.  For use in future comparisons.
	 * The old value must be explicitly set though this call.
	 */
	public void set_oldValue() {
		if (getValue() != null) {
			try {
				this.m_oldValue = ((Integer) getValue());
			} 
			catch (ClassCastException e)
			{
				this.m_oldValue = null;
			}
		}
		else
			this.m_oldValue = null;
		if (m_text != null)
			this.m_oldText = m_text.getDisplay();
		else
			m_oldText = "";
		this.m_oldWhere = m_pAttributeWhere;
	}
	/**
	 * Get the old value of the field explicitly set in the past
	 * @return
	 */
	public Object get_oldValue() {
		return m_oldValue;
	}
	/**
	 * Has the field changed over time?
	 * @return true if the old value is different than the current.
	 */
	public boolean hasChanged() {
		// Both or either could be null
		
		// Don't think a test of Value is needed as value is not set by the search window
		//if(getValue() != null)
		//	if(m_oldValue != null)
		//		return !m_oldValue.equals(getValue());
		//	else
		//		return true;
		//else  // getValue() is null
		//	if(m_oldValue != null)
		//		return true;

		if(m_text != null)
			if(m_oldText != null)
				return !m_oldText.equals(m_text.getDisplay());
			else
				return true;
		else  // m_text is null
			if(m_oldText != null)
				return true;

		if(m_pAttributeWhere != null)
			if(m_oldWhere != null)
				return !m_oldWhere.equals(m_pAttributeWhere);
			else
				return true;
		else  // m_pAttributeWhere is null
			if(m_oldWhere != null)
				return true;

		return false;

	
	}

	/**************************************************************************
	 *	Focus Listener for ComboBoxes with missing Validation or invalid entries
	 *	- Requery listener for updated list
	 *  @param e FocusEvent
	 */
	public void focusGained (FocusEvent e)
	{
		if ((e.getSource() != m_text)
			|| e.isTemporary() || m_haveFocus)
			return;

		//
		log.fine("Have Focus!");
		m_haveFocus = true;     //  prevents calling focus gained twice
		m_text.selectAll();

	}	//	focusGained

	/**
	 *	Reset Selection List
	 *  @param e FocusEvent
	 */
	public void focusLost(FocusEvent e)
	{
		if (e.isTemporary()
			|| !m_button.isEnabled()){	//	set by actionButton
			m_haveFocus = false;    //  can gain focus again
			return;
		}

		//	Text Lost focus
		if (e.getSource() == m_text)
		{
				actionText();	//	re-display
		}
		//
		log.fine("Losing Focus!");
		m_haveFocus = false;    //  can gain focus again
	}	//	focusLost

	/**
	 *	Check, if data returns unique entry, otherwise involve Info via Button
	 */
	private void actionText()
	{
		String text = m_text.getText();
		// Nothing entered, just pressing enter again => ignore - teo_sarca BF [ 1834399 ]
		if (text != null && text.length() >= 0 && text.equals(m_lastDisplay))
		{
			log.fine("Nothing entered [SKIP]");
			return;
		}
		log.fine("");
		//	Nothing entered
		if (text == null || text.length() == 0 || text.equals("%"))
		{
			log.fine("Text null or uses wild cards." + "\"" + text + "\"");
			actionButton();
			return;
		}
		text = text.toUpperCase();
		log.fine(m_columnName + " - " + "\"" + text + "\"");
		
		setM_Product_ID();
		
		//	Exclude ability to enter ASI
		boolean exclude = false;
		
		MProduct product = null;
		MAttributeSet mas = null;
		
		if (M_Product_ID != 0)
		{
			product = MProduct.get(Env.getCtx(), M_Product_ID);
			M_AttributeSet_ID = product.getM_AttributeSet_ID();
			if (M_AttributeSet_ID != 0)
			{
				mas = MAttributeSet.get(Env.getCtx(), M_AttributeSet_ID);
				exclude = mas.excludeEntry(m_AD_Column_ID, Env.isSOTrx(Env.getCtx(), m_WindowNo));
			}
		}
		
		// If the VPAttribute component is in a dialog/search don't need to find a specific ASI.
		// Also, if there is no product or attribute set, there can be no ASI
		if (m_searchOnly || !isProductWindow && (M_Product_ID == 0 || M_AttributeSet_ID == 0 
													|| product == null || exclude))
		{
			log.info("No action: M_Product_ID == 0 || M_AttributeSet_ID == 0 || product == null || exclude");
			m_text.setText(m_lastDisplay);
			return;
		}

		// The control will accept text input and will try to match that text
		// against Attribute Set Instances as follows:
		//   1. by the M_AttributeSetInstance_ID number, then if not found
		//   2. by the serial number, lot or guarantee date (exact, unique)
		// The match will include the M_Product_ID and associated Attribute Set
		// as well as the Locator, if this information is available.
		// If a match is not found, the dialog box will be opened.
		
		// Test the text to see if it is in the form of a number.
		Integer asiIDToFind = -1;
		
		String where = "";
		try {
			asiIDToFind = Integer.parseInt(text);
		}
		catch (NumberFormatException e) {
			asiIDToFind = -1;
		}


		if (asiIDToFind > 0) {

			try {
				where = MAttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID + "= ?";
				asiIDToFind = new Query(Env.getCtx(), MAttributeSetInstance.Table_Name, where, null)
								.setClient_ID()
								.setOnlyActiveRecords(true)
								.setParameters(asiIDToFind)
								.firstIdOnly();  // returns ID or -1 if not found.
				// Check integrity of the result
				if (asiIDToFind > 0) {
					if (!product.isValidAttributeSetInstance(Env.getCtx(), Env.isSOTrx(Env.getCtx(), m_WindowNo), m_AD_Column_ID, asiIDToFind)){
						asiIDToFind = -1;
					}
				}
			}
			catch (DBException e) {
				asiIDToFind = -2; // multiple results
			}		
		}

		
		if (asiIDToFind == -1 ) { // Not found, 
			// Try to match the lot code
			try {
				where = MAttributeSetInstance.COLUMNNAME_Lot + "= ?";
				asiIDToFind = new Query(Env.getCtx(), MAttributeSetInstance.Table_Name, where, null)
									.setClient_ID()
									.setOnlyActiveRecords(true)
									.setParameters(text)
									.firstIdOnly();  // returns ID or -1 if not found.
				// Check integrity of the result
				if (asiIDToFind > 0) {
					if (!product.isValidAttributeSetInstance(Env.getCtx(), Env.isSOTrx(Env.getCtx(), m_WindowNo), m_AD_Column_ID, asiIDToFind)){
						asiIDToFind = -1;
					}
					else
						log.fine("Valid lot number found.  M_AttributeSetInstance_ID = " + asiIDToFind);
				}
			}
			catch (DBException e) {
				asiIDToFind = -2; // multiple results
			}
		}

		if (asiIDToFind == -1 ) { // Not found, 
			// Try to match the serial number code
			try {
				where = MAttributeSetInstance.COLUMNNAME_SerNo + "= ?";
				asiIDToFind = new Query(Env.getCtx(), MAttributeSetInstance.Table_Name, where, null)
									.setClient_ID()
									.setOnlyActiveRecords(true)
									.setParameters(text)
									.firstIdOnly();  // returns ID or -1 if not found.
				// Check integrity of the result
				if (asiIDToFind > 0) {
					if (!product.isValidAttributeSetInstance(Env.getCtx(), Env.isSOTrx(Env.getCtx(), m_WindowNo), m_AD_Column_ID, asiIDToFind)){
						asiIDToFind = -1;
					}
					else
						log.fine("Valid serial number found.  M_AttributeSetInstance_ID = " + asiIDToFind);
				}
			}
			catch (DBException e) {
				asiIDToFind = -2; // multiple results
			}
		}
		

		if (asiIDToFind == -1 ) { // Not found, 
			// Try to match the Guarantee Date - Date has to be entered in the 
			// system date format pattern 
			Timestamp ts = null;
			SimpleDateFormat dateFormat = DisplayType.getDateFormat();
			try
			{
				java.util.Date date = dateFormat.parse(text);
				ts = new Timestamp(date.getTime());
			}
			catch (ParseException pe)
			{
				log.fine("Entered text not in date format " + dateFormat.getDateFormatSymbols().toString());
				log.fine(pe.getMessage());
				ts = null;
			}
			
			if (ts != null) {
				try {
					where = MAttributeSetInstance.COLUMNNAME_GuaranteeDate + "= ?";
					asiIDToFind = new Query(Env.getCtx(), MAttributeSetInstance.Table_Name, where, null)
										.setClient_ID()
										.setOnlyActiveRecords(true)
										.setParameters(ts)
										.firstIdOnly();  // returns ID or -1 if not found.
					// Check integrity of the result
					if (asiIDToFind > 0) {
						if (!product.isValidAttributeSetInstance(Env.getCtx(), Env.isSOTrx(Env.getCtx(), m_WindowNo), m_AD_Column_ID, asiIDToFind)){
							asiIDToFind = -1;
						}
						else
							log.fine("Valid Gurantee Date found.  M_AttributeSetInstance_ID = " + asiIDToFind);
					}
				}
				catch (DBException e) {
					asiIDToFind = -2; // multiple results
				}
			}
		}
		
		// If we have found a value, set it
		if (asiIDToFind > 0) {
			setAndBindValue(asiIDToFind.intValue());
			return;
		}
		else if (asiIDToFind == -1) {
			// TODO make this configurable
			// Assume the text is a new serial number if the serial number is mandatory
			// Create a new ASI using the values from the product and the serial number
			if (mas != null 
					&& mas.isInstanceAttribute() 
					&& mas.isSerNo() 
					&& mas.isSerNoMandatory()
					&& product.isASIMandatory(Env.isSOTrx(Env.getCtx(), m_WindowNo), product.getAD_Org_ID())
					&& !isProductWindow) {
				
				MAttributeSetInstance instanceASI = new MAttributeSetInstance (Env.getCtx(), 0, product.getM_AttributeSet_ID(), null);
				
				if (instanceASI != null) {
					instanceASI.setSerNo(text);
					instanceASI.saveEx();
					
					if (instanceASI.getM_AttributeSetInstance_ID() > 0) {
						// Need the instanceASI ID to create the attribute instances
						if (product.getM_AttributeSetInstance_ID() > 0) {
							MAttributeInstance.copy(Env.getCtx(), instanceASI.getM_AttributeSetInstance_ID(), 
									product.getM_AttributeSetInstance_ID(), null);
						}
						
						// Need the attribute instances, if any, before creating the description
						instanceASI.setDescription();
						instanceASI.saveEx();
						//
						setAndBindValue(instanceASI.getM_AttributeSetInstance_ID());
						//
						return;
					}
				}
			}
		}
		else if (asiIDToFind == -2) {
			ADialog.warn(m_WindowNo, this, "Found duplicate matches. Please select the correct value using the dialog");
		}
	
		// Didn't understand the text input, couldn't find a match or found duplicates  
		// Use the search dialog.
		log.fine(Msg.parseTranslation(Env.getCtx(), "\"" + text + "\"" + " @NotFound@"));
		actionButton();
		m_text.requestFocus();
	}	//	actionText

	/**
	 *	Perform the actions of clicking the button in the control
	 */
	private void actionButton()
	{
		if (!m_button.isEnabled ())
			return;
		m_button.setEnabled (false);
		//
		Integer oldValue = 0;
		try
		{
			oldValue = (Integer)getValue ();			
		}
		catch(ClassCastException cce)
		{
			// Possible Invalid Cast exception if getValue() return new instance of Object.
			oldValue = 0;
		}
		int oldValueInt = oldValue == null ? 0 : oldValue.intValue ();
		int M_AttributeSetInstance_ID = oldValueInt;
		
		setM_Product_ID();
		
		//	Exclude ability to enter ASI
		boolean exclude = false;
		boolean changed = false;
		
		if (M_Product_ID != 0)
		{
			MProduct product = MProduct.get(Env.getCtx(), M_Product_ID);
			M_AttributeSet_ID = product.getM_AttributeSet_ID();
			if (M_AttributeSet_ID != 0)
			{
				MAttributeSet mas = MAttributeSet.get(Env.getCtx(), M_AttributeSet_ID);
				exclude = mas.excludeEntry(m_AD_Column_ID, Env.isSOTrx(Env.getCtx(), m_WindowNo));
			}
		}
		
		// If the VPAttribute component is in a dialog, use the search
		if (m_searchOnly)
		{	
			// As in the infoProduct panel so there is no Product or Locator
			// The component is an element in a CPanel, which is part of a JPanel
			// which is in a JLayeredPane which is in ...  the InfoProduct window
			Container me = ((Container) this).getParent();
			while (me != null)
			{
				if (me instanceof InfoProduct)
					break;
				me = me.getParent();
			}
			// The infoPAttribute doesn't select an attribute set instance, it builds the where clause
			// so setting the value is not required here.
			InfoPAttribute ia = new InfoPAttribute((CDialog) me);
			m_pAttributeWhere = ia.getWhereClause();
			m_text.setText(ia.getDisplay());
			// The text can be long.  Use the tooltip to help display the info.
			m_text.setToolTipText(m_text.getText());
			m_lastDisplay = m_text.getText();

			ActionEvent ae = new ActionEvent(m_text, 1001, "updated");
			//  TODO not the generally correct way to fire an event
			((InfoProduct) me).actionPerformed(ae);
			
			// For search, don't need to set or bind value and trigger callouts.  Just return.
			m_button.setEnabled(true);
			return;
		}
		else if (!isProductWindow && (M_Product_ID == 0 || M_AttributeSet_ID == 0))
		{
			log.info("No action: M_Product_ID == 0 || M_AttributeSet_ID == 0");
			M_AttributeSetInstance_ID = 0;
			changed = M_AttributeSetInstance_ID != oldValueInt;
		}
		else if (!isProductWindow && (M_AttributeSetInstance_ID ==0 && exclude))
		{
			log.info("AttributeSetInstance is excluded in this window.");
			changed = M_AttributeSetInstance_ID != oldValueInt;
		}
		else
		{
			VPAttributeDialog vad = new VPAttributeDialog (Env.getFrame (this), 
				M_AttributeSetInstance_ID, M_Product_ID, m_C_BPartner_ID,
				isProductWindow, m_AD_Column_ID, m_WindowNo, isReadWrite());
			if (vad.isChanged() || vad.getM_AttributeSetInstance_ID() != oldValueInt)
			{
//				m_text.setText(vad.getM_AttributeSetInstanceName());
//				// The text can be long.  Use the tooltip to help display the info.
//				m_text.setToolTipText(vad.getM_AttributeSetInstanceName());
				M_AttributeSetInstance_ID = vad.getM_AttributeSetInstance_ID();
				if (!isProductWindow && vad.getM_Locator_ID() > 0)
				{
					vad.getM_Locator_ID();
				}
				changed = true;
			}
		}
		
		//	Set Value
		if (changed)
		{
			log.finest("Changed M_AttributeSetInstance_ID=" + M_AttributeSetInstance_ID);
			setAndBindValue(M_AttributeSetInstance_ID);
		}	//	change
		
		setValue(getValue()); // Reset the display in case text was entered.
	
		if (M_AttributeSetInstance_ID == oldValueInt && m_GridTab != null && m_GridField != null)
		{
			//  force Change - user does not realize that embedded object is already saved.
			//  This will fire the callouts on the field if any.
			m_GridTab.processFieldChange(m_GridField); 
		}
		m_button.setEnabled(true);
	}

	private void setAndBindValue(int M_AttributeSetInstance_ID) {

		Integer oldValue = 0;
		try
		{
			oldValue = (Integer)getValue ();			
		}
		catch(ClassCastException cce)
		{
			// Possible Invalid Cast exception if getValue() returns new instance of Object.
			oldValue = 0;
		}

		Object newValue;		
		if (M_AttributeSetInstance_ID == 0)
			newValue = null;
		else
			newValue = new Integer(M_AttributeSetInstance_ID);
		
		//
		try
		{
	 	 	fireVetoableChange(m_columnName, oldValue, newValue);
	 	 	if (getValue() != newValue) // !This overwrites the veto!
	 	 		setValue(newValue);
		}
		catch (PropertyVetoException pve)
		{
			log.log(Level.SEVERE, "", pve);
		}
	}
	
	/**
	 *	Action - Zoom
	 *	@param selectedItem item
	 */
	private void actionZoom ()
	{
		//
		MQuery zoomQuery = new MQuery();
		Object value = getValue();
		if (value == null)
			value = Integer.valueOf(0);
		String keyTableName = MAttributeSetInstance.Table_Name;
		String keyColumnName = MAttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID;

		zoomQuery.addRestriction(keyColumnName, MQuery.EQUAL, value);
		zoomQuery.setZoomColumnName(keyColumnName);
		zoomQuery.setZoomTableName(keyTableName);
		zoomQuery.setZoomValue(value);
		zoomQuery.setRecordCount(1);	//	guess

		int	AD_Window_ID = m_lookup.getZoom(zoomQuery);
		//
		log.info(m_columnName + " - AD_Window_ID=" + AD_Window_ID
			+ " - Query=" + zoomQuery + " - Value=" + value);
		//
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//
		AWindow frame = new AWindow();
		if (!frame.initWindow(AD_Window_ID, zoomQuery))
		{
			setCursor(Cursor.getDefaultCursor());
			ValueNamePair pp = CLogger.retrieveError();
			String msg = pp==null ? "AccessTableNoView" : pp.getValue();
			ADialog.error(m_lookup.getWindowNo(), this, msg, pp==null ? "" : pp.getName());
		}
		else
		{
			AEnv.addToWindowManager(frame);
			if (Ini.isPropertyBool(Ini.P_OPEN_WINDOW_MAXIMIZED))
			{
				AEnv.showMaximized(frame);
			}
			else
			{
				AEnv.showCenterScreen(frame);
			}
		}
			//  async window - not able to get feedback
		frame = null;
		//
		setCursor(Cursor.getDefaultCursor());
	}	//	actionZoom

	/**
	 * 	Request Focus
	 */
	public void requestFocus ()
	{
		m_text.requestFocus ();
	}	//	requestFocus

}	//	VPAttribute
