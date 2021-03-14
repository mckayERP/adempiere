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

import static org.adempiere.util.attributes.AttributeUtilities.*;

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
import org.adempiere.exceptions.ValueChangeListener;
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
		 *  @param attribute attribute
		 */
		VPAttribute_mouseAdapter(VPAttribute attribute)
		{
			this.attribute = attribute;
		}	//	VPAttribute_mouseAdapter

		private VPAttribute attribute;

		/**
		 *	Mouse Listener
		 *  @param e event
		 */
		public void mouseClicked(MouseEvent e)
		{
			//	Double Click
			if (e.getClickCount() > 1)
				this.attribute.actionPerformed(new ActionEvent(e.getSource(), e.getID(), "Mouse"));
			//	popup menu
			if (SwingUtilities.isRightMouseButton(e))
				this.attribute.popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
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
	 *  @param isUpdated isUpdated
	 * 	@param WindowNo WindowNo
	 * 	@param pAttribute Model Product Attribute
	 */
	@Deprecated
	public VPAttribute (boolean mandatory, boolean isReadOnly, boolean isUpdated, 
		int WindowNo, MPAttributeLookup pAttribute, boolean searchOnly)
	{
		this(null, mandatory, isReadOnly, isUpdated, WindowNo, (MLookup) null, searchOnly);
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
	public VPAttribute (GridTab gridTab,
						boolean mandatory,
						boolean isReadOnly,
						boolean isUpdateable,
						int windowNo,
						MPAttributeLookup lookup,
						boolean searchOnly)
	{
		this(gridTab, mandatory, isReadOnly, isUpdateable, windowNo, (MLookup) null, searchOnly);
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
			boolean isUpdateable, int windowNo, MLookup lookup, boolean searchOnly) {
		super();
		super.setName(columnName);
		this.text.setName("VPAttribute Text - " + columnName);
		this.button.setName("VPAttribute Button - " + columnName);
		this.value = 0;
		this.gridTabAttribute = gridTab; // added for processCallout
		this.windowNo = windowNo;
		this.attributeLookup = lookup;
		this.partnerId = Env.getContextAsInt(Env.getCtx(), windowNo, "C_BPartner_ID");
		this.isSearchOnly = searchOnly;
		// The creating function should set the field and name. See VEditorFactory.
		// To initialize the field in cases of forms, set the field to null.
		setField(null);    
		
		LookAndFeel.installBorder(this, "TextField.border");
		this.setLayout(new BorderLayout());
		//  Size
		this.setPreferredSize(this.text.getPreferredSize());
		int height = this.text.getPreferredSize().height;
		
		//	***	Text	***
		this.text.setEditable(true);
		this.text.setFocusable(true);
		this.text.setBorder(null);
		this.text.setHorizontalAlignment(JTextField.LEADING);
		this.text.addActionListener(this);
		this.text.addFocusListener(this);
	//	Background
		setMandatory(mandatory);
		this.add(this.text, BorderLayout.CENTER);

		//	***	Button	***
		this.button.setIcon(Env.getImageIcon("PAttribute10.gif"));
		this.button.setMargin(new Insets(0, 0, 0, 0));
		this.button.setPreferredSize(new Dimension(height, height));
		this.button.addActionListener(this);
		this.button.setFocusable(true);
		this.add(this.button, BorderLayout.EAST);

		//	Prefereed Size
		this.setPreferredSize(this.getPreferredSize());		//	causes r/o to be the same length
		//	ReadWrite
		if (isReadOnly || !isUpdateable)
			setReadWrite(false);
		else
			setReadWrite(true);

		//	Popup
		this.text.addMouseListener(new VPAttribute_mouseAdapter(this));
		menuInfo = new CMenuItem(Msg.getMsg(Env.getCtx(), "Info"), Env.getImageIcon("Zoom16.gif"));
		menuZoom = new CMenuItem(Msg.getMsg(Env.getCtx(), "Zoom"), Env.getImageIcon("Zoom16.gif"));
		menuInfo.addActionListener(this);
		menuZoom.addActionListener(this);
		popupMenu.add(menuZoom);
		popupMenu.add(menuInfo);

		set_oldValue();
	}	//	VPAttribute

	/**	Data Value				*/
	private Object 				value = new Object();
	/** Attribute Where Clause  */
	private String 				attributeWhere = null;
	/** Column Name - fixed		*/
	private String 				columnName = "M_AttributeSetInstance_ID";
	/** The Attribute Instance	*/
//	private MPAttributeLookup 	attributeLookup;

	/** The Text Field          */
	private CTextField 			text = new CTextField();
	/** The Button              */
	private CButton 			button = new CButton();

	JPopupMenu          		popupMenu = new JPopupMenu();
	private CMenuItem 			menuZoom;

	private boolean 			isReadWrite;
	private boolean 			isMandatory;
	private int 				windowNo;
	private int 				partnerId;
	private boolean 			isSearchOnly;
	private boolean 			isProductWindow;
	/** The Grid Tab * */
	private GridTab 			gridTabAttribute; // added for processCallout
	/** The Grid Field * */
	private GridField 			gridFieldAttribute; // added for processCallout
	
	/**	Calling Window Info				*/
	private int 				columnId = 0;
	private int					tableId = 0;
	/** record the value for comparison at a point in the future */
	private Integer 			oldValue = 0;
	private String 				oldText = "";
	private String 				oldWhere = "";
	private boolean haveFocus;
	/** The last display value.  The text displayed can change without the underlying
	 *  value changing so this variable provides a means to test if a change has occurred.
	 */
	private String lastDisplay;
	private int productId = 0;
	private int productBOMId = 0;
	private int attributeSetId;
	private MLookup attributeLookup;

	/**	No Instance Key					*/
	private static Integer		NO_INSTANCE = new Integer(0);
	/**	Logger			*/
	private static CLogger log = CLogger.getCLogger(VPAttribute.class);
		

	/**
	 * 	Dispose resources
	 */
	public void dispose()
	{
		this.text = null;
		this.button = null;
		this.attributeLookup.dispose();
		this.attributeLookup = null;
		this.gridFieldAttribute = null;
		this.gridTabAttribute = null;
	}	//	dispose

	/**
	 * 	Set Mandatory
	 * 	@param mandatory mandatory
	 */
	public void setMandatory (boolean mandatory)
	{
		this.isMandatory = mandatory;
		this.button.setMandatory(mandatory);
		setBackground (false);
	}	//	setMandatory

	/**
	 * 	Get Mandatory
	 *  @return mandatory
	 */
	public boolean isMandatory()
	{
		return this.isMandatory;
	}	//	isMandatory

	/**
	 * 	Set ReadWrite
	 * 	@param rw read write
	 */
	public void setReadWrite (boolean rw)
	{
		this.isReadWrite = rw;
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
		return this.isReadWrite;
	}	//	isReadWrite

	/**
	 * 	Set Foreground
	 * 	@param color color
	 */
	public void setForeground (Color color)
	{
		this.text.setForeground(color);
	}	//	SetForeground

	/**
	 * 	Set Background
	 * 	@param error Error
	 */
	public void setBackground (boolean error)
	{
		if (error)
			setBackground(AdempierePLAF.getFieldBackground_Error());
		else if (!this.isReadWrite)
			setBackground(AdempierePLAF.getFieldBackground_Inactive());
		else if (this.isMandatory)
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
		this.text.setBackground(color);
	}	//	setBackground

	
	/**************************************************************************
	 * 	Set/lookup Value
	 * 	@param value value
	 */
	public void setValue(Object value)
	{
		log.fine(this.columnName + "=" + value);
		if (value == null || NO_INSTANCE.equals(value))
		{
			this.text.setText("");
			this.text.setToolTipText("");
			this.value = value;
			this.attributeWhere = "";
			this.lastDisplay = "";
		}
		
		//	changed
		else if (!value.equals(this.value)) {
			//	new value
			this.value = value;
			this.attributeWhere = "EXISTS (SELECT * FROM M_Storage s "
					+ "WHERE s.M_AttributeSetInstance_ID=" + value
					+ " AND s.M_Product_ID=p.M_Product_ID)";
		}
		// Reset the display whether a change was made or not - in case text was entered and cancelled
		this.text.setText(this.attributeLookup.getDisplay(this.value));	//	loads value
		// The text can be long.  Use the tooltip to help display the info.
		this.text.setToolTipText(this.text.getText());

		this.lastDisplay = this.text.getText();
		
		enableControl();

		return;
	}	//	setValue

	private void enableControl() {

		setM_Product_ID();
		// Enable or disable controls
		MAttributeSet as = null;
		MProduct product = MProduct.get(Env.getCtx(), this.productId);
		if (product !=null && product.getM_AttributeSet_ID() > 0) {
			as = (MAttributeSet) product.getM_AttributeSet();
		}
		
		boolean enabled = true;
		if (as != null) {
			// Enable the control if the control has a non zero value or is not excluded.
			enabled = ((this.value != null && !NO_INSTANCE.equals(this.value)) || !as.excludeEntry(this.tableId, Env.isSOTrx(Env.getCtx(),this.windowNo)));
			this.button.setEnabled(this.isReadWrite && (this.isProductWindow || this.isSearchOnly || enabled));
			this.text.setEnabled(this.isReadWrite && (this.isProductWindow || this.isSearchOnly || enabled));
		}
		else {
			this.button.setEnabled(this.isReadWrite && (this.isProductWindow || this.isSearchOnly));
			this.text.setEnabled(this.isReadWrite && (this.isProductWindow || this.isSearchOnly));
		}
		
		if (this.gridFieldAttribute != null) {  // The column is found
			columnId = this.gridFieldAttribute.getAD_Column_ID();
			if (product != null) {
				// Set column error if the ASI is mandatory 
				Properties ctx = Env.getCtx();
				Boolean isSOTrx = Env.isSOTrx(ctx, this.windowNo);
				Integer attributeSetInstanceId = null;
				if (getValue() != null) {
				    attributeSetInstanceId = (Integer) getValue();
				}	
				gridFieldAttribute.setError(!isValidAttributeSetInstance(ctx, product, isSOTrx, columnId, attributeSetInstanceId, null));
			}
			else { // No product - so no ASI
				gridFieldAttribute.setError(false);
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
		if (gridTabAttribute != null) {
			productId = Env.getContextAsInt (Env.getCtx (), windowNo, gridTabAttribute.getTabNo(), "M_Product_ID");
			productBOMId = Env.getContextAsInt (Env.getCtx (), windowNo, gridTabAttribute.getTabNo(), "M_ProductBOM_ID");
		} else {
			productId = Env.getContextAsInt (Env.getCtx (), windowNo, "M_Product_ID");
			productBOMId = Env.getContextAsInt (Env.getCtx (), windowNo, "M_ProductBOM_ID");
		}
		if (productBOMId != 0)	//	Use BOM Component
			productId = productBOMId;		
	}

	/**
	 * 	Get Value
	 * 	@return value
	 */
	public Object getValue()
	{
		Integer temp = null;
		if (this.value != null || NO_INSTANCE.equals(this.value)) {
			try {
				temp = (Integer) this.value;
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
		return this.attributeWhere;
	}	//	getAttributeWhere()

	/**
	 * 	Get Display Value
	 *	@return info
	 */
	public String getDisplay()
	{
		return this.text.getText();
	}	//	getDisplay

	
	/**************************************************************************
	 * 	Set Field
	 * 	@param mField MField
	 */
	public void setField(GridField mField)
	{
		//	To determine behaviour
		this.gridFieldAttribute = mField;
		
		if (this.gridFieldAttribute != null) {
			columnName = gridFieldAttribute.getColumnName();
			columnId = gridFieldAttribute.getAD_Column_ID();
			tableId = MColumn.getTable_ID(Env.getCtx(), columnId, null);
			RecordInfo.addMenu(this, popupMenu);
		}
		else {
			columnName = "M_AttributeSetInstance_ID";
			columnId = 0;
			tableId = 0;
		}
		//	M_Product.M_AttributeSetInstance_ID = 8418
		isProductWindow = columnId == MColumn.getColumn_ID(MProduct.Table_Name, MProduct.COLUMNNAME_M_AttributeSetInstance_ID);
		
		enableControl();
	}	//	setField
	
	@Override
	public GridField getField() {
		return this.gridFieldAttribute;
	}

	/**
	 *  Action Listener Interface
	 *  @param listener listener
	 */
	public void addActionListener(ActionListener listener)
	{
		this.text.addActionListener(listener);
	}   //  addActionListener

	/**
	 * 	Action Listener - start dialog
	 * 	@param e Event
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals(RecordInfo.CHANGE_LOG_COMMAND))
		{
			RecordInfo.start(this.gridFieldAttribute);
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
				this.oldValue = ((Integer) getValue());
			} 
			catch (ClassCastException e)
			{
				this.oldValue = null;
			}
		}
		else
			this.oldValue = null;
		if (this.text != null)
			this.oldText = this.text.getDisplay();
		else
			this.oldText = "";
		this.oldWhere = this.attributeWhere;
	}
	/**
	 * Get the old value of the field explicitly set in the past
	 * @return
	 */
	public Object get_oldValue() {
		return oldValue;
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

		if(text != null)
			if(oldText != null)
				return !oldText.equals(text.getDisplay());
			else
				return true;
		else  // text is null
			if(oldText != null)
				return true;

		if(attributeWhere != null)
			if(oldWhere != null)
				return !oldWhere.equals(attributeWhere);
			else
				return true;
		else  // attributeWhere is null
			if(oldWhere != null)
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
		if ((e.getSource() != text)
			|| e.isTemporary() || haveFocus)
			return;

		//
		log.fine("Have Focus!");
		haveFocus = true;     //  prevents calling focus gained twice
		text.selectAll();

	}	//	focusGained

	/**
	 *	Reset Selection List
	 *  @param e FocusEvent
	 */
	public void focusLost(FocusEvent e)
	{
		if (e.isTemporary()
			|| !button.isEnabled()){	//	set by actionButton
			haveFocus = false;    //  can gain focus again
			return;
		}

		//	Text Lost focus
		if (e.getSource() == text)
		{
				actionText();	//	re-display
		}
		//
		log.fine("Losing Focus!");
		haveFocus = false;    //  can gain focus again
	}	//	focusLost

	/**
	 *	Check, if data returns unique entry, otherwise involve Info via Button
	 */
	private void actionText()
	{
		String text = this.text.getText();
		// Nothing entered, just pressing enter again => ignore - teo_sarca BF [ 1834399 ]
		if (text != null && text.length() >= 0 && text.equals(lastDisplay))
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
		log.fine(columnName + " - " + "\"" + text + "\"");
		
		setM_Product_ID();
		
		//	Exclude ability to enter ASI
		boolean exclude = false;
		
		MProduct product = null;
		MAttributeSet mas = null;
		
		if (productId != 0)
		{
			product = MProduct.get(Env.getCtx(), productId);
			attributeSetId = product.getM_AttributeSet_ID();
			if (attributeSetId != 0)
			{
				mas = MAttributeSet.get(Env.getCtx(), attributeSetId);
				exclude = mas.excludeEntry(tableId, Env.isSOTrx(Env.getCtx(), windowNo));
			}
		}
		
		// If the VPAttribute component is in a dialog/search don't need to find a specific ASI.
		// Also, if there is no product or attribute set, there can be no ASI
		if (this.isSearchOnly || !isProductWindow && (productId == 0 || attributeSetId == 0 
													|| product == null || exclude))
		{
			log.info("No action: M_Product_ID == 0 || M_AttributeSet_ID == 0 || product == null || exclude");
			this.text.setText(lastDisplay);
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
					if (!isValidAttributeSetInstance(Env.getCtx(), product, Env.isSOTrx(Env.getCtx(), this.windowNo), this.columnId, asiIDToFind, null)){
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
					if (!isValidAttributeSetInstance(Env.getCtx(), product, Env.isSOTrx(Env.getCtx(), this.windowNo), this.columnId, asiIDToFind, null)){
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
					if (!isValidAttributeSetInstance(Env.getCtx(), product, Env.isSOTrx(Env.getCtx(), this.windowNo), this.columnId, asiIDToFind, null)){
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
						if (!isValidAttributeSetInstance(Env.getCtx(), product, Env.isSOTrx(Env.getCtx(), this.windowNo), this.columnId, asiIDToFind, null)){
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
					&& isAttributeSetInstanceMandatory(Env.getCtx(), product, this.tableId, Env.isSOTrx(Env.getCtx(), this.windowNo), null)
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
			ADialog.warn(this.windowNo, this, "Found duplicate matches. Please select the correct value using the dialog");
		}
	
		// Didn't understand the text input, couldn't find a match or found duplicates  
		// Use the search dialog.
		log.fine(Msg.parseTranslation(Env.getCtx(), "\"" + text + "\"" + " @NotFound@"));
		actionButton();
		this.text.requestFocus();
	}	//	actionText

	/**
	 *	Perform the actions of clicking the button in the control
	 */
	private void actionButton()
	{
		if (!this.button.isEnabled ())
			return;
		this.button.setEnabled (false);
		//
		Integer oldValueEvent = 0;
		try
		{
			oldValueEvent = (Integer)getValue ();
		}
		catch(ClassCastException cce)
		{
			// Possible Invalid Cast exception if getValue() return new instance of Object.
			oldValueEvent = 0;
		}
		int oldValueInt = oldValueEvent == null ? 0 : oldValueEvent.intValue ();
		int attributeSetInstanceId = oldValueInt;
		
		setM_Product_ID();
		
		//	Exclude ability to enter ASI
		boolean exclude = false;
		boolean changed = false;
		
		if (productId != 0)
		{
			MProduct product = MProduct.get(Env.getCtx(), productId);
			int attributeSetId = product.getM_AttributeSet_ID();
			if (attributeSetId != 0)
			{
				int tableId = MColumn.getTable_ID(Env.getCtx(), this.columnId, null);
				MAttributeSet mas = MAttributeSet.get(Env.getCtx(), attributeSetId);
				exclude = mas.excludeEntry(tableId, Env.isSOTrx(Env.getCtx(), this.windowNo));
			}
		}
		
		// If the VPAttribute component is in a dialog, use the search
		if (this.isSearchOnly)
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
			this.attributeWhere = ia.getWhereClause();
			this.text.setText(ia.getDisplay());
			// The text can be long.  Use the tooltip to help display the info.
			this.text.setToolTipText(this.text.getText());
			this.lastDisplay = this.text.getText();

			ActionEvent ae = new ActionEvent(this.text, 1001, "updated");
			//  TODO not the generally correct way to fire an event
			((InfoProduct) me).actionPerformed(ae);
			
			// For search, don't need to set or bind value and trigger callouts.  Just return.
			this.button.setEnabled(true);
			return;
		}
		else if (!isProductWindow && (productId == 0 || attributeSetId == 0))
		{
			log.info("No action: M_Product_ID == 0 || M_AttributeSet_ID == 0");
			attributeSetInstanceId = 0;
			changed = attributeSetInstanceId != oldValueInt;
		}
		else if (!isProductWindow && (attributeSetInstanceId ==0 && exclude))
		{
			log.info("AttributeSetInstance is excluded in this window.");
			changed = attributeSetInstanceId != oldValueInt;
		}
		else
		{
			VPAttributeDialog vad = new VPAttributeDialog (Env.getFrame (this), 
					attributeSetInstanceId, productId, this.partnerId,
					isProductWindow,this.columnId, this.windowNo, isReadWrite());
			if (vad.isChanged() || vad.getM_AttributeSetInstance_ID() != oldValueInt)
			{
					this.text.setText(vad.getM_AttributeSetInstanceName());
//				// The text can be long.  Use the tooltip to help display the info.
					this.text.setToolTipText(vad.getM_AttributeSetInstanceName());
					attributeSetInstanceId = vad.getM_AttributeSetInstance_ID();
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
			log.finest("Changed M_AttributeSetInstance_ID=" + attributeSetInstanceId);
			setAndBindValue(attributeSetInstanceId);
		}	//	change
		
		setValue(getValue()); // Reset the display in case text was entered.
	
		if (attributeSetInstanceId == oldValueInt && gridTabAttribute != null && gridFieldAttribute != null)
		{
			//  force Change - user does not realize that embedded object is already saved.
			//  This will fire the callouts on the field if any.
			gridTabAttribute.processFieldChange(gridFieldAttribute); 
		}
		button.setEnabled(true);
	}

	private void setAndBindValue(int attributeSetInstanceId) {

		Integer currentValue = 0;
		try
		{
			currentValue = (Integer)getValue ();			
		}
		catch(ClassCastException cce)
		{
			// Possible Invalid Cast exception if getValue() returns new instance of Object.
			currentValue = 0;
		}

		Object newValue;		
		if (attributeSetInstanceId == 0)
			newValue = null;
		else
			newValue = Integer.valueOf(attributeSetInstanceId);
		
		//
		try
		{
	 	 	fireVetoableChange(columnName, currentValue, newValue);
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
		Object zoomTargetValue = getValue();
		if (zoomTargetValue == null)
			zoomTargetValue = Integer.valueOf(0);
		String keyTableName = MAttributeSetInstance.Table_Name;
		String keyColumnName = MAttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID;

		zoomQuery.addRestriction(keyColumnName, MQuery.EQUAL, zoomTargetValue);
		zoomQuery.setZoomColumnName(keyColumnName);
		zoomQuery.setZoomTableName(keyTableName);
		zoomQuery.setZoomValue(zoomTargetValue);
		zoomQuery.setRecordCount(1);	//	guess

		int	windowId = attributeLookup.getZoom(zoomQuery);
		//
		log.info(columnName + " - AD_Window_ID=" + windowId
			+ " - Query=" + zoomQuery + " - Value=" + zoomTargetValue);
		//
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//
		AWindow frame = new AWindow();
		if (!frame.initWindow(windowId, zoomQuery))
		{
			setCursor(Cursor.getDefaultCursor());
			ValueNamePair pp = CLogger.retrieveError();
			String msg = pp==null ? "AccessTableNoView" : pp.getValue();
			ADialog.error(attributeLookup.getWindowNo(), this, msg, pp==null ? "" : pp.getName());
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
		//
		setCursor(Cursor.getDefaultCursor());
	}	//	actionZoom

	/**
	 * 	Request Focus
	 */
	public void requestFocus ()
	{
		text.requestFocus ();
	}	//	requestFocus

	@Override
	public void addValueChangeListener(ValueChangeListener listener) {
		// TODO Auto-generated method stub
		
	}

}	//	VPAttribute
