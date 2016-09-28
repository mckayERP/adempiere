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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.apps.ADialog;
import org.compiere.apps.AEnv;
import org.compiere.apps.ALayout;
import org.compiere.apps.ALayoutConstraint;
import org.compiere.apps.AWindow;
import org.compiere.apps.ConfirmPanel;
import org.compiere.apps.search.PAttributeInstance;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MDocType;
import org.compiere.model.MLot;
import org.compiere.model.MLotCtl;
import org.compiere.model.MProduct;
import org.compiere.model.MQuery;
import org.compiere.model.MRole;
import org.compiere.model.MSerNoCtl;
import org.compiere.model.MWindow;
import org.compiere.model.Query;
import org.compiere.model.X_M_MovementLine;
import org.compiere.swing.CButton;
import org.compiere.swing.CCheckBox;
import org.compiere.swing.CComboBox;
import org.compiere.swing.CDialog;
import org.compiere.swing.CEditor;
import org.compiere.swing.CLabel;
import org.compiere.swing.CMenuItem;
import org.compiere.swing.CPanel;
import org.compiere.swing.CScrollPane;
import org.compiere.swing.CTextField;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 *  Product Attribute Set Product/Instance Dialog Editor.
 * 	Called from VPAttribute.actionPerformed
 *
 *  @author Jorg Janke
 *  @version $Id: VPAttributeDialog.java,v 1.4 2006/07/30 00:51:27 jjanke Exp $
 *  
 *  @author Michael McKay (mjmckay)
 *  		<li>BF3468823 - Attribute Set Instance editor does not display
 * 			<li>ADEMPIERE-72 VLookup and Info Window improvements
 * 					https://adempiere.atlassian.net/browse/ADEMPIERE-72
 * 			<li>#281 Improve tests of validity of ASI values
 * 			<li>#258 Reduce duplication of ASI values	
 */
public class VPAttributeDialog extends CDialog
	implements ActionListener
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1062346984681892620L;

	/*****************************************************************************
	 *	Mouse Listener for Popup Menu
	 */
	final class VPAttributeDialog_mouseAdapter extends java.awt.event.MouseAdapter
	{
		/**
		 *	Constructor
		 *  @param adaptee adaptee
		 */
		VPAttributeDialog_mouseAdapter(VPAttributeDialog adaptee)
		{
			m_adaptee = adaptee;
		}	//	VPAttributeDialog_mouseAdapter

		private VPAttributeDialog m_adaptee;

		/**
		 *	Mouse Listener
		 *  @param e MouseEvent
		 */
		public void mouseClicked(MouseEvent e)
		{
		//	System.out.println("mouseClicked " + e.getID() + " " + e.getSource().getClass().toString());
			//	popup menu
			if (SwingUtilities.isRightMouseButton(e))
				m_adaptee.popupMenu.show((Component)e.getSource(), e.getX(), e.getY());
		}	//	mouse Clicked

	}	//	VPAttributeDialog_mouseAdapter	

	private boolean m_readWrite;
	private int old_m_attributeSetInstance_id;
	private boolean m_isSOTrx;
	
	/**
	 *	Product Attribute Instance Dialog
	 *	@param frame parent frame
	 *	@param M_AttributeSetInstance_ID Product Attribute Set Instance id
	 * 	@param M_Product_ID Product id
	 * 	@param C_BPartner_ID b partner
	 * 	@param productWindow this is the product window (define Product Instance)
	 * 	@param AD_Column_ID column
	 * 	@param WindowNo window
	 */
	public VPAttributeDialog (Frame frame, int M_AttributeSetInstance_ID, 
		int M_Product_ID, int C_BPartner_ID, 
		boolean productWindow, int AD_Column_ID, int WindowNo, boolean readWrite)
	{
		super (frame, Msg.translate(Env.getCtx(), "M_AttributeSetInstance_ID") , true);
		log.config("M_AttributeSetInstance_ID=" + M_AttributeSetInstance_ID 
			+ ", M_Product_ID=" + M_Product_ID
			+ ", C_BPartner_ID=" + C_BPartner_ID
			+ ", ProductW=" + productWindow + ", Column=" + AD_Column_ID);
		m_WindowNo = Env.createWindowNo (this);
		m_attributeSetInstance_id = M_AttributeSetInstance_ID;
		old_m_attributeSetInstance_id = M_AttributeSetInstance_ID;
		m_product_id = M_Product_ID;
		m_C_BPartner_ID = C_BPartner_ID;
		isProductWindow = productWindow;
		m_AD_Column_ID = AD_Column_ID;
		m_WindowNoParent = WindowNo;
		m_readWrite = readWrite;

		m_isSOTrx = Env.isSOTrx(Env.getCtx(), m_WindowNoParent);
		
		//get columnName from ad_column
 	 	m_columnName = DB.getSQLValueString(null, "SELECT ColumnName FROM AD_Column WHERE AD_Column_ID = ?", m_AD_Column_ID);
 	 	if (m_columnName == null || m_columnName.trim().length() == 0)
 	 	{
 	 		//fallback
 	 		m_columnName = "M_AttributeSetInstance_ID";
 	 	}
 	 	
 	 	if (isProductWindow) {
 	 		this.setTitle(Msg.translate(Env.getCtx(), "M_Product_ID") + " " + Msg.translate(Env.getCtx(), "M_AttributeSetInstance_ID") + " " + m_attributeSetInstance_id);
 	 	}
 	 	else {
 	 		this.setTitle(this.getTitle() + ": #" + m_attributeSetInstance_id);
 	 	}
 	 	
		try
		{
			jbInit();
		}
		catch(Exception ex)
		{
			log.log(Level.SEVERE, "VPAttributeDialog" + ex);
		}
		//	Dynamic Init
		if (!initAttributes ())
		{
			m_changed = false;
			dispose();
			return;
		}
		AEnv.showCenterWindow(frame, this);
	}	//	VPAttributeDialog

	private int						m_WindowNo;
	private MAttributeSetInstance	instanceASI;
	private int 					m_attributeSetInstance_id;
	private int 					m_M_Locator_ID;
	private String					m_M_AttributeSetInstanceName;
	private int 					m_product_id;
	private int						m_C_BPartner_ID;
	private int						m_AD_Column_ID;
	private int						m_WindowNoParent;
	/**	Enter Product Attributes		*/
	private boolean					isProductWindow = false;
	/**	Change							*/
	private boolean					m_changed = false;
	
	private CLogger					log = CLogger.getCLogger(getClass());
	/** Row Counter					*/
	private int						m_row = 0;
	
	/** A set for the editor and whether it is a productAttribute or not. */
	private class MEditor {
		
		public MEditor (CEditor ceditor, boolean product) {
			this.editor = ceditor;
			this.productAttribute = product;
		}
		
		public CEditor editor = null;
		public boolean productAttribute = false;
	};
	
	/** List of Editors				*/
	private ArrayList<MEditor>		m_editors = new ArrayList<MEditor>();
	/** Length of Instance value (40)	*/
	private static final int		INSTANCE_VALUE_LENGTH = 40;

	private CCheckBox	cbNewEdit = new CCheckBox();
	private CButton		bSelect = new CButton(Env.getImageIcon("PAttribute16.gif")); 
	//	Lot
	private VString fieldLotString = new VString ("Lot", false, false, true, 20, 20, null, null);
	private CComboBox fieldLot = null;
	private CButton bLot = new CButton(Util.cleanAmp(Msg.getMsg (Env.getCtx(), "New")));
	//	Lot Popup
	JPopupMenu 					popupMenu = new JPopupMenu();
	private CMenuItem 			mZoom;
	//	Ser No
	private VString fieldSerNo = new VString ("SerNo", false, false, true, 20, 20, null, null);
	private CButton bSerNo = new CButton(Util.cleanAmp(Msg.getMsg (Env.getCtx(), "New")));
	//	Date
	private VDate fieldGuaranteeDate = new VDate ("GuaranteeDate", false, false, true, DisplayType.Date, Msg.translate(Env.getCtx(), "GuaranteeDate"));
	//
	private CTextField fieldDescription = new CTextField (20);
	//
	private BorderLayout mainLayout = new BorderLayout();
	private CPanel centerPanel = new CPanel();
	private ALayout centerLayout = new ALayout(5,5, true);
	private ConfirmPanel confirmPanel = new ConfirmPanel (true);
	private CScrollPane centerScroll = new CScrollPane();

	private String m_columnName = null;
	private MProduct m_product;
	private boolean hasProductASI = false;
	private int productASI_id = 0;
	private MAttributeSetInstance productASI = null;

	/**
	 *	Layout
	 * 	@throws Exception
	 */
	private void jbInit () throws Exception
	{
		this.getContentPane().setLayout(mainLayout);
		centerScroll.getViewport().add(centerPanel);
		this.add(centerScroll, BorderLayout.CENTER);
		this.getContentPane().add(confirmPanel, BorderLayout.SOUTH);

		centerPanel.setLayout(centerLayout);
		//
		confirmPanel.addActionListener(this);
	}	//	jbInit

	/**
	 *	Dyanmic Init.
	 *  @return true if initialized
	 */
	private boolean initAttributes ()
	{
		// Don't open a dialog if the product is not defined and we are not in the Product Window.
		// In the Product window, the attribute set can be defined before the product record is saved and
		// the M_Product_ID value is created.  Outside the Product window, the Product ID value is 
		// required to create the ASI as the product provides the attribute set and the "master" asi attribute values,
		// if the a product ASI is defined.
		if (m_product_id == 0 && !isProductWindow)
			return false;
		
		MAttributeSet as = null;
		
//		if (m_attributeSetInstance_id > 0)
			instanceASI = MAttributeSetInstance.get(Env.getCtx(), m_attributeSetInstance_id, m_product_id);
//		else
//			instanceASI = MAttributeSetInstance.get(Env.getCtx(), m_attributeSetInstance_id, 0);
		
		if (m_product_id != 0)
		{
			//	Get product model
			m_product = MProduct.get(Env.getCtx(), m_product_id);
			if (m_product.getM_AttributeSetInstance_ID() > 0)
			{
				//  The product has an instance associated with it - the master ASI
				hasProductASI = true;
				productASI_id = m_product.getM_AttributeSetInstance_ID();
				productASI  = (MAttributeSetInstance) m_product.getM_AttributeSetInstance();
			}
			else
			{
				// The product does not have a product ASI defined
				hasProductASI = false;				
			}

			// Check for conflicts in product window (only one ASI allowed)
			if (hasProductASI && m_attributeSetInstance_id != 0 && m_attributeSetInstance_id != productASI_id && isProductWindow) {
				// Major problem that we can't deal with.  There can't be two ASI values for a product
				// in any record and certainly not in the Product window.
				throw new AdempiereException("@Error@ @Invalid@ @M_AttributeSetInstance_ID@");
			}
			// Check for conflicts in attribute sets between the product and this instance.
			// They have to use the same attribute set.
			if (hasProductASI && m_attributeSetInstance_id != 0 && instanceASI != null && productASI != null 
					&& m_attributeSetInstance_id != productASI_id && !isProductWindow ) {
				
				if (productASI.getM_AttributeSet_ID() != instanceASI.getM_AttributeSet_ID()) {
					log.severe("Incompatible attribute sets between the provided ASI and the product ASI.");
					return false;
				}
			}
			
			//	Get Attribute Set
			if (hasProductASI && (instanceASI == null || instanceASI.getM_AttributeSetInstance_ID() ==0) && productASI != null) {
				as = productASI.getMAttributeSet();
				if (as.isInstanceAttribute() && !isProductWindow)
					instanceASI = new MAttributeSetInstance (Env.getCtx(), 0, as.getM_AttributeSet_ID(), null);
				else {
					instanceASI = productASI;
				}
			}
			else if (instanceASI != null) {
				as = instanceASI.getMAttributeSet();
			}
		}
		else 
		{
			int M_AttributeSet_ID = Env.getContextAsInt(Env.getCtx(), m_WindowNoParent, "M_AttributeSet_ID");
			instanceASI = new MAttributeSetInstance (Env.getCtx(), 0, M_AttributeSet_ID, null);
			as = instanceASI.getMAttributeSet();
		}
		
		if (as != null)		
		{
			Env.setContext(Env.getCtx(), m_WindowNo, "M_AttributeSet_ID", as.getM_AttributeSet_ID());
		}

		//	BF3468823 Show Product Attributes
		//  Product attributes can be shown in any window but are read/write only in the product
		//  window.  Instance attributes are shown in any window but the product window and are
		//  always read/write.  
		Boolean productAttributesReadWrite = isProductWindow && m_readWrite;
		Boolean showInstanceAttributes = !isProductWindow;
		Boolean instanceAttributesReadWrite = m_readWrite && showInstanceAttributes && as.isInstanceAttribute();
		
    	//  After select existing, the attributes are reset with the selected values.  
		//  Clear the current ones, if any.
    	centerPanel.removeAll();
    	m_row = 0;

    	cbNewEdit = new CCheckBox();
    	bSelect = new CButton(Env.getImageIcon("PAttribute16.gif")); 
    	bSerNo = new CButton(Util.cleanAmp(Msg.getMsg (Env.getCtx(), "New")));
    	bLot = new CButton(Util.cleanAmp(Msg.getMsg (Env.getCtx(), "New")));

		Boolean isNew = false;  // Create a new record.  False implies edit the current record.
		
        if (as != null) {
        	
        	// Only add buttons and controls if readWrite
			if (productAttributesReadWrite || instanceAttributesReadWrite) {
				//	New/Edit - Selection
				if (m_attributeSetInstance_id >= 0) {	
					//	Don't edit an existing ASI.  Always create a new record.  If the values
					//  match an existing ASI, that ASI will be used.
					cbNewEdit.setText(Msg.getMsg(Env.getCtx(), "NewRecord"));
					isNew = true;
				}
				else
					cbNewEdit.setText(Msg.getMsg(Env.getCtx(), "EditRecord"));
				cbNewEdit.addActionListener(this);
				centerPanel.add(cbNewEdit, new ALayoutConstraint(m_row++,0));
	
				if (!isProductWindow) {
					bSelect.setText(Msg.getMsg(Env.getCtx(), "SelectExisting"));
					bSelect.addActionListener(this);
					centerPanel.add(bSelect, null);
				}
			}

        	if (showInstanceAttributes)	//	Set Instance Attributes and dialog controls
    		{
        		if (as.isInstanceAttribute()) {
    				//CLabel group = new CLabel(Msg.translate(Env.getCtx(), "IsProductAttribute"));
    				CLabel group = new CLabel(Msg.translate(Env.getCtx(), "IsInstanceAttribute"));
    				group.setFontBold(true);
    				group.setHorizontalAlignment(SwingConstants.CENTER);
    				centerPanel.add(group, new ALayoutConstraint(m_row++,0));
        		}
				//	Add the Instance Attributes if any.
				MAttribute[] attributes = as.getMAttributes (true);
				log.fine ("Instance Attributes=" + attributes.length);
				for (int i = 0; i < attributes.length; i++)
					addAttributeLine (attributes[i], false, !m_readWrite && isNew); // Instance only, read/write

				//	Lot
				if (as.isLot())
				{
					CLabel label = new CLabel (Msg.translate(Env.getCtx(), "Lot"));
					label.setLabelFor (fieldLotString);
					centerPanel.add(label, new ALayoutConstraint(m_row++,0));
					centerPanel.add(fieldLotString, null);
					fieldLotString.setReadWrite(m_readWrite && !isNew);
					fieldLotString.setMandatory(as.isLotMandatory() && m_readWrite);
					fieldLotString.setText (instanceASI.getLot());
					//	M_Lot_ID
				//	int AD_Column_ID = 9771;	//	M_AttributeSetInstance.M_Lot_ID
				//	fieldLot = new VLookup ("M_Lot_ID", false,false, true, 
				//		MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, AD_Column_ID, DisplayType.TableDir));
					String sql = "SELECT M_Lot_ID, Name "
						+ "FROM M_Lot l "
						+ "WHERE EXISTS (SELECT M_Product_ID FROM M_Product p "
							+ "WHERE p.M_AttributeSet_ID=" + instanceASI.getM_AttributeSet_ID()
							+ " AND p.M_Product_ID=l.M_Product_ID)";
					fieldLot = new CComboBox(DB.getKeyNamePairs(sql, true));
					label = new CLabel (Msg.translate(Env.getCtx(), "M_Lot_ID"));
					label.setLabelFor (fieldLot);
					centerPanel.add(label, new ALayoutConstraint(m_row++,0));
					centerPanel.add(fieldLot, null);
					if (instanceASI.getM_Lot_ID() != 0)
					{
						for (int i = 1; i < fieldLot.getItemCount(); i++)
						{
							KeyNamePair pp = (KeyNamePair)fieldLot.getItemAt(i);
							if (pp.getKey() == instanceASI.getM_Lot_ID())
							{
								fieldLot.setSelectedIndex(i);
								fieldLotString.setEditable(false);
								break;
							} 
						}
					}
					fieldLot.setReadWrite(m_readWrite && !isNew);
					fieldLot.addActionListener(this);
					fieldLotString.addActionListener(this);
					//	New Lot Button
					if (instanceASI.getMAttributeSet().getM_LotCtl_ID() != 0 && m_readWrite && !isNew)
					{
						if (MRole.getDefault().isTableAccess(MLot.Table_ID, false)
							&& MRole.getDefault().isTableAccess(MLotCtl.Table_ID, false)
							&& !instanceASI.isExcludeLot(m_AD_Column_ID, m_isSOTrx))
						{
							centerPanel.add(bLot, null);
							bLot.addActionListener(this);
						}
					}
					// Background
					if (fieldLotString.isMandatory()) {
						fieldLotString.setBackground(fieldLotString.getText() == null || fieldLotString.getText().isEmpty());
						fieldLot.setBackground(fieldLot.getDisplay() == null || fieldLot.getDisplay().isEmpty());
					}
					//	Popup 
					fieldLot.addMouseListener(new VPAttributeDialog_mouseAdapter(this));    //  popup
					mZoom = new CMenuItem(Msg.getMsg(Env.getCtx(), "Zoom"), Env.getImageIcon("Zoom16.gif"));
					mZoom.addActionListener(this);
					popupMenu.add(mZoom);
				}	//	Lot
		
				//	SerNo
				if (as.isSerNo())
				{
					CLabel label = new CLabel (Msg.translate(Env.getCtx(), "SerNo"));
					label.setLabelFor(fieldSerNo);
					fieldSerNo.setText(instanceASI.getSerNo());
					fieldSerNo.setReadWrite(m_readWrite && !isNew);
					fieldSerNo.setMandatory(as.isSerNoMandatory() && m_readWrite);
					fieldSerNo.addActionListener(this);
					centerPanel.add(label, new ALayoutConstraint(m_row++,0));
					centerPanel.add(fieldSerNo, null);
					//	New SerNo Button
					if (instanceASI.getMAttributeSet().getM_SerNoCtl_ID() != 0 && m_readWrite && isNew)
					{
						if (MRole.getDefault().isTableAccess(MSerNoCtl.Table_ID, false)
							&& !instanceASI.isExcludeSerNo(m_AD_Column_ID, m_isSOTrx))
						{
							centerPanel.add(bSerNo, null);
							bSerNo.addActionListener(this);
						}
					}
					if (fieldSerNo.isMandatory()) {
						fieldSerNo.setBackground(fieldSerNo.getText() == null || fieldSerNo.getText().isEmpty());
					}
				}	//	SerNo
		
				//	GuaranteeDate
				if (as.isGuaranteeDate())
				{
					CLabel label = new CLabel (Msg.translate(Env.getCtx(), "GuaranteeDate"));
					label.setLabelFor(fieldGuaranteeDate);
					fieldGuaranteeDate.setReadWrite(m_readWrite && !isNew);
					fieldGuaranteeDate.setMandatory(as.isGuaranteeDateMandatory() && m_readWrite);
					if (m_attributeSetInstance_id == 0)
						fieldGuaranteeDate.setValue(instanceASI.getGuaranteeDate(true));
					else
						fieldGuaranteeDate.setValue(instanceASI.getGuaranteeDate());
					fieldGuaranteeDate.addActionListener(this);
					centerPanel.add(label, new ALayoutConstraint(m_row++,0));
					centerPanel.add(fieldGuaranteeDate, null);
					if (fieldGuaranteeDate.isMandatory()) {
						fieldGuaranteeDate.setBackground(fieldGuaranteeDate.getValue() == null);
					}
				}	//	GuaranteeDate
    		}

    		//  Product attributes can be shown in any window but are read/write in the Product window only.
        	MAttribute[] attributes = as.getMAttributes (false);  // False = product attribute instances
            log.fine ("Product Attributes=" + attributes.length);
            if (attributes.length > 0) {
				CLabel group = new CLabel(Msg.translate(Env.getCtx(), "IsProductAttribute"));
				group.setFontBold(true);
				group.setHorizontalAlignment(SwingConstants.CENTER);
				centerPanel.add(group, new ALayoutConstraint(m_row++,0));
            }
            for (int i = 0; i < attributes.length; i++)
            	addAttributeLine (attributes[i], true, !productAttributesReadWrite);

        }

		if (m_row == 0)
		{
			ADialog.error(m_WindowNo, this, "PAttributeNoInfo");
			//return false;
		}

		//	New/Edit Window
		if (m_AD_Column_ID != 0 && (productAttributesReadWrite || instanceAttributesReadWrite)) {
			cbNewEdit.setSelected(m_attributeSetInstance_id == 0);
			confirmPanel.getCancelButton().setEnabled(cbNewEdit.isSelected());
			cmd_newEdit();
		}
		else {
			confirmPanel.getCancelButton().setEnabled(false);			
		}

		//	Attribute Set Instance Description
		CLabel label = new CLabel (Msg.translate(Env.getCtx(), "Description"));
		label.setLabelFor(fieldDescription);
		fieldDescription.setText(instanceASI.getDescription());
		fieldDescription.setEditable(false);
		centerPanel.add(label, new ALayoutConstraint(m_row++,0));
		centerPanel.add(fieldDescription, null);

		//	Window usually to wide (??)
		Dimension dd = centerPanel.getPreferredSize();
		dd.width = Math.min(500, dd.width);
		centerPanel.setPreferredSize(dd);
		return true;
	}	//	initAttribute

	/**
	 * 	Add Attribute Line
	 *	@param attribute attribute
	 * 	@param product product level attribute
	 * 	@param readOnly value is read only
	 */
	private void addAttributeLine (MAttribute attribute, boolean product, boolean readOnly)
	{
		log.fine(attribute + ", Product=" + product + ", R/O=" + readOnly);
		CLabel label = new CLabel (attribute.getName());
		if (product)
			label.setFont(new Font(label.getFont().getFontName(), Font.BOLD, label.getFont().getSize()));
		if (attribute.getDescription() != null)
			label.setToolTipText(attribute.getDescription());
		centerPanel.add(label, new ALayoutConstraint(m_row++,0));
		//

		// Set the values according to the instance, if it exists, or the product ASI, if one exists.
		MAttributeInstance instance = null;
		if (m_attributeSetInstance_id != 0) 
			instance = attribute.getMAttributeInstance (m_attributeSetInstance_id);
		else if (hasProductASI && productASI_id != 0)
			instance = attribute.getMAttributeInstance (productASI_id);

		if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
		{
			MAttributeValue[] values = attribute.getMAttributeValues();	//	optional = null
			CComboBox editor = new CComboBox(values);
			editor.setMandatory(attribute.isMandatory());
			boolean found = false;
			if (instance != null)
			{
				for (int i = 0; i < values.length; i++)
				{
					if (values[i] != null && values[i].getM_AttributeValue_ID () == instance.getM_AttributeValue_ID ())
					{
						editor.setSelectedIndex (i);
						found = true;
						break;
					}
				}
				if (found)
					log.fine("Attribute=" + attribute.getName() + " #" + values.length + " - found: " + instance);
				else
					log.warning("Attribute=" + attribute.getName() + " #" + values.length + " - NOT found: " + instance);
			}	//	setComboBox
			else
				log.fine("Attribute=" + attribute.getName() + " #" + values.length + " no instance");
			label.setLabelFor(editor);
			centerPanel.add(editor, null);
 			if (readOnly) {
 				editor.setReadWrite(false);
				editor.setEditable(false);
 			}
			
			m_editors.add (new MEditor(editor, product));
		}
		else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attribute.getAttributeValueType()))
		{
			VNumber editor = new VNumber(attribute.getName(), attribute.isMandatory(), 
				readOnly, !readOnly, DisplayType.Number, attribute.getName());
			editor.setMandatory(attribute.isMandatory());
			if (instance != null)
				editor.setValue(instance.getValueNumber());
			else
				editor.setValue(Env.ZERO);
			label.setLabelFor(editor);
			centerPanel.add(editor, null);
			if (readOnly) {
 				editor.setReadWrite(false);
			}
			m_editors.add (new MEditor(editor, product));
		}
		else	//	Text Field
		{
			VString editor = new VString (attribute.getName(), attribute.isMandatory(), 
				false, true, 20, INSTANCE_VALUE_LENGTH, null, null);
			editor.setMandatory(attribute.isMandatory());
			if (instance != null)
				editor.setValue(instance.getValue());
			label.setLabelFor(editor);
			centerPanel.add(editor, null);
			if (readOnly) {
 				editor.setReadWrite(false);
				editor.setEditable(false);
			}
			m_editors.add (new MEditor(editor,product));
		}
	}	//	addAttributeLine

	/**
	 *	dispose
	 */
	public void dispose()
	{
		removeAll();
		Env.clearWinContext(m_WindowNo);
		//
		Env.setContext(Env.getCtx(), m_WindowNo, Env.TAB_INFO, m_columnName, 
			String.valueOf(m_attributeSetInstance_id));
		Env.setContext(Env.getCtx(), m_WindowNo, Env.TAB_INFO, "M_Locator_ID", 
			String.valueOf(m_M_Locator_ID));
		//
		super.dispose();
	}	//	dispose

	/**
	 *	ActionListener
	 *  @param e event
	 */
	public void actionPerformed(ActionEvent e)
	{
		//	Select Instance
		if (e.getSource() == bSelect)
		{
			if (cmd_select()) {
				initAttributes();
				centerPanel.validate();
			}
		}
		//	New/Edit
		else if (e.getSource() == cbNewEdit)
		{
			cmd_newEdit();
		}
		//	Select Lot from existing
		else if (e.getSource() == fieldLot)
		{
			KeyNamePair pp = (KeyNamePair)fieldLot.getSelectedItem();
			if (pp != null && pp.getKey() != -1)
			{
				fieldLotString.setText(pp.getName());
				fieldLotString.setEditable(false);
				instanceASI.setM_Lot_ID(pp.getKey());
			}
			else
			{
				fieldLotString.setEditable(true);
				instanceASI.setM_Lot_ID(0);
			}
			if (fieldLotString.isMandatory()) {
				fieldLotString.setBackground(fieldLotString.getText() == null || fieldLotString.getText().isEmpty());
			}
		}
		// Manually enter the lot
		else if (e.getSource() == fieldLotString)
		{
			if (fieldLotString.isMandatory()) {
				fieldLotString.setBackground(fieldLotString.getText() == null || fieldLotString.getText().isEmpty());
			}			
		}
		//	Create New Lot
		else if (e.getSource() == bLot)
		{
			KeyNamePair pp = instanceASI.createLot(m_product_id);
			if (pp != null)
			{
				fieldLot.addItem(pp);
				fieldLot.setSelectedItem(pp);				
				fieldLotString.setText (instanceASI.getLot());
				fieldLotString.setEditable(false);
			}
		}
		//  Manually enter SerNo
		else if (e.getSource() == fieldSerNo) {
			if (fieldSerNo.isMandatory()) {
				fieldSerNo.setBackground(fieldSerNo.getText() == null || fieldSerNo.getText().isEmpty());
			}
		}
		//	Create New SerNo
		else if (e.getSource() == bSerNo)
		{
			fieldSerNo.setText(instanceASI.getSerNo(true));
			if (fieldSerNo.isMandatory()) {
				fieldSerNo.setBackground(fieldSerNo.getText() == null || fieldSerNo.getText().isEmpty());
			}
		}
		// Manually enter guarantee date
		else if (e.getSource() instanceof CTextField && ((CTextField) e.getSource()).getParent() == fieldGuaranteeDate) {
			if (fieldGuaranteeDate.isMandatory()) {
				fieldGuaranteeDate.setBackground(fieldGuaranteeDate.getValue() == null);
			}
		}
		
		//	OK
		else if (e.getActionCommand().equals(ConfirmPanel.A_OK))
		{
			if (saveSelection())
				dispose();
		}
		//	Cancel
		else if (e.getActionCommand().equals(ConfirmPanel.A_CANCEL))
		{
//			if (isProductWindow || !hasProductASI)
//			{
				m_changed = m_attributeSetInstance_id != old_m_attributeSetInstance_id;;
				m_attributeSetInstance_id = 0;
				m_M_Locator_ID = 0;
//			}
			dispose();
		}
		//	Zoom M_Lot
		else if (e.getSource() == mZoom)
		{
			cmd_zoom();
		}
		else
			log.log(Level.SEVERE, "not found - " + e);
	}	//	actionPerformed

	/**
	 * 	Instance Selection Button
	 * 	@return true if selected
	 */
	private boolean cmd_select()
	{
		log.config("");
		
		int M_Warehouse_ID = Env.getContextAsInt(Env.getCtx(), m_WindowNoParent, "M_Warehouse_ID");
		
		int C_DocType_ID = Env.getContextAsInt(Env.getCtx(), m_WindowNoParent, "C_DocType_ID");
		if (C_DocType_ID > 0) {
			MDocType doctype = new MDocType (Env.getCtx(), C_DocType_ID, null);
			String docbase = doctype.getDocBaseType();
			// consider also old lot numbers at inventory
			if (docbase.equals(MDocType.DOCBASETYPE_MaterialReceipt)
				||  docbase.equals(MDocType.DOCBASETYPE_MaterialPhysicalInventory))
				M_Warehouse_ID = 0;
		}
		
		// teo_sarca [ 1564520 ] Inventory Move: can't select existing attributes
		// Trifon - Always read Locator from Context. There are too many windows to read explicitly one by one.
		int M_Locator_ID = 0;
		M_Locator_ID = Env.getContextAsInt(Env.getCtx(), m_WindowNoParent, X_M_MovementLine.COLUMNNAME_M_Locator_ID, true); // only window
		
		String title = "";
		//	Get Text
		String sql = "SELECT p.Name, w.Name, w.M_Warehouse_ID FROM M_Product p, M_Warehouse w "
			+ "WHERE p.M_Product_ID=? AND w.M_Warehouse_ID"
				+ (M_Locator_ID <= 0 ? "=?" : " IN (SELECT M_Warehouse_ID FROM M_Locator where M_Locator_ID=?)"); // teo_sarca [ 1564520 ]
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, m_product_id);
			pstmt.setInt(2, M_Locator_ID <= 0 ? M_Warehouse_ID : M_Locator_ID);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				title = ": " + rs.getString(1) + " - " + rs.getString(2);
				M_Warehouse_ID = rs.getInt(3); // fetch the actual warehouse - teo_sarca [ 1564520 ]
			}
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		//		
		PAttributeInstance pai = new PAttributeInstance(this, title, 
			M_Warehouse_ID, M_Locator_ID, m_product_id, m_C_BPartner_ID);
		//
		if (m_attributeSetInstance_id != pai.getM_AttributeSetInstance_ID() ||
				!(m_attributeSetInstance_id == 0 && pai.getM_AttributeSetInstance_ID() == -1))
		{
			m_changed = true;
			//
			if (pai.getM_AttributeSetInstance_ID() != -1)
			{
				m_attributeSetInstance_id = pai.getM_AttributeSetInstance_ID();
				m_M_AttributeSetInstanceName = pai.getM_AttributeSetInstanceName();
				m_M_Locator_ID = pai.getM_Locator_ID();
			}
			else
			{
				// No change
				//m_attributeSetInstance_id = 0;
				//m_M_AttributeSetInstanceName = "";
				// Leave the locator alone
			}
		}
		return m_changed;
	}	//	cmd_select

	/**
	 * 	Instance New/Edit
	 */
	private void cmd_newEdit()
	{
		boolean rw = cbNewEdit.isSelected();
		log.config("R/W=" + rw + " " + instanceASI);
		//
		fieldLotString.setEditable(rw && (instanceASI == null || instanceASI.getM_Lot_ID()==0));
		if (fieldLot != null)
			fieldLot.setReadWrite(rw);
		bLot.setReadWrite(rw);
		fieldSerNo.setReadWrite(rw);
		bSerNo.setReadWrite(rw);
		fieldGuaranteeDate.setReadWrite(rw);
		//
		for (int i = 0; i < m_editors.size(); i++)
		{
			CEditor editor = m_editors.get(i).editor;
			boolean productAttribute = m_editors.get(i).productAttribute;
			if ((!productAttribute && !isProductWindow) || (productAttribute && isProductWindow))
				editor.setReadWrite(rw);
			else
				editor.setReadWrite(false);
		}
		//
		if (rw) {
			confirmPanel.getCancelButton().setEnabled(rw);
		}
		
		// Don't edit the current ASI. Force creation of a new one.
		// This prevents changes to existing serial numbers.  If the values match
		// an existing ASI, that ASI ID will be used.
		instanceASI.setM_AttributeSetInstance_ID(0);
			
	}	//	cmd_newEdit

	/**
	 * 	Zoom M_Lot
	 */
	private void cmd_zoom()
	{
		int M_Lot_ID = 0;
		KeyNamePair pp = (KeyNamePair)fieldLot.getSelectedItem();
		if (pp != null)
			M_Lot_ID = pp.getKey();
		MQuery zoomQuery = new MQuery("M_Lot");
		zoomQuery.addRestriction("M_Lot_ID", MQuery.EQUAL, M_Lot_ID);
		log.info(zoomQuery.toString());
		//
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//
		int AD_Window_ID = MWindow.getWindow_ID("Lot");		//	Lot
		AWindow frame = new AWindow();
		if (frame.initWindow(AD_Window_ID, zoomQuery))
		{
			this.setVisible(false);
			this.setModal (false);	//	otherwise blocked
			this.setVisible(true);
			AEnv.addToWindowManager(frame);
			AEnv.showScreen(frame, SwingConstants.EAST);
		}
		//  async window - not able to get feedback
		frame = null;
		//
		setCursor(Cursor.getDefaultCursor());
	}	//	cmd_zoom

	/**
	 *	Save Selection
	 *	@return true if saved
	 */
	private boolean saveSelection()
	{
		if(!m_readWrite)
			return true;
		
		log.fine("");
		MAttributeSet as = instanceASI.getMAttributeSet();
		if (as == null)
			return true;
				
		m_changed = false;
		String mandatory = "";
		
		if (!isProductWindow && !as.excludeEntry(m_AD_Column_ID, m_isSOTrx)) {
//			if (hasProductASI && !as.isInstanceAttribute()){
//				// use the current product ASI - no changes
//				return true;
//			}
			if (as.isLot() && !as.isExcludeLot(m_AD_Column_ID, m_isSOTrx))
			{
				log.fine("Lot=" + fieldLotString.getText ());
				String text = fieldLotString.getText();
				instanceASI.setLot (text);
				if (as.isLotMandatory() && (text == null || text.length() == 0))
					mandatory += " - " + Msg.translate(Env.getCtx(), "Lot");
			}	//	Lot
			if (as.isSerNo() && !as.isExcludeSerNo(m_AD_Column_ID, m_isSOTrx))
			{
				log.fine("SerNo=" + fieldSerNo.getText());
				String text = fieldSerNo.getText();
				instanceASI.setSerNo(text);
				if (as.isSerNoMandatory() && (text == null || text.length() == 0))
					mandatory += " - " + Msg.translate(Env.getCtx(), "SerNo");
			}	//	SerNo
			if (as.isGuaranteeDate())
			{
				log.fine("GuaranteeDate=" + fieldGuaranteeDate.getValue());
				Timestamp ts = (Timestamp)fieldGuaranteeDate.getValue();
				instanceASI.setGuaranteeDate(ts);
				if (as.isGuaranteeDateMandatory() && ts == null)
					mandatory += " - " + Msg.translate(Env.getCtx(), "GuaranteeDate");
			}	//	GuaranteeDate
		}
		else if (isProductWindow) {
			// To correct errors, remove all instance values
			instanceASI.setLot(null);
			instanceASI.setSerNo(null);
			instanceASI.setGuaranteeDate(null);
		}
		
		// Get the set of attribute values from the editors and check for missing
		// mandatory fields.  The order of the attributes is set by the order that
		// editors are created. 
		MAttribute[] attributes = as.getMAttributes();
		Object[] values = new Object[attributes.length];
		for (int i = 0; i < attributes.length; i++)
		{
			
			if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attributes[i].getAttributeValueType()))
			{
				CComboBox editor = (CComboBox)m_editors.get(i).editor;
				values[i] = (MAttributeValue)editor.getSelectedItem();
			}
			else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attributes[i].getAttributeValueType()))
			{
				VNumber editor = (VNumber)m_editors.get(i).editor;
				values[i] = (BigDecimal)editor.getValue();
			}
			else
			{
				VString editor = (VString)m_editors.get(i).editor;
				values[i] = editor.getText();
			}
			log.fine(attributes[i].getName() + "=" + values[i]);
			if (attributes[i].isMandatory() && values[i] == null)
				mandatory += " - " + attributes[i].getName();

		}
		//  Prevent save if the mandatory fields are not filled.
		if (mandatory.length() > 0)
		{
			ADialog.error(m_WindowNo, this, "FillMandatory", mandatory);
			return false;
		}

		// Limit the number of redundant attribute Set Instances
		// Need to do this here BEFORE the attribute instance values are saved
		// See if this set of instance values already exists.
		int matchingASI_ID = 0;
		// Where there is a product ASI and no instance attributes, ignore matches and just use the product ASI.
		if (productASI != null 
				&& !as.isInstanceAttribute()
				&& productASI.getM_AttributeSetInstance_ID() == instanceASI.getM_AttributeSetInstance_ID()) {
			matchingASI_ID = 0;
		}
		else {
			if (!isProductWindow && productASI != null 
					&& as.isInstanceAttribute()
					&& productASI.getM_AttributeSetInstance_ID() == instanceASI.getM_AttributeSetInstance_ID()) {
				//  Don't use the product asi in this case. A true instance is needed.
				instanceASI.setM_AttributeSetInstance_ID(0); 
			}
			matchingASI_ID = instanceASI.findMatchingASI(values);  // Will return 0 if nothing found.
		}
		
		//	Existing or New Instance with no match
		if (instanceASI.getM_AttributeSetInstance_ID() >= 0 && matchingASI_ID == 0) {
			// No Match to an existing ASI so this set of values is new/unique.  Only 
			// need to save the instance.
			if (m_attributeSetInstance_id != instanceASI.getM_AttributeSetInstance_ID())
				m_changed = true;
		}
		else if (instanceASI.getM_AttributeSetInstance_ID() == 0 && matchingASI_ID > 0) {
			// The proposed instance is new and is a match for an existing instance.  Use the existing
			// ID
			// TODO - use the lowest ID number to avoid switching back and forth
			instanceASI = MAttributeSetInstance.get(Env.getCtx(), matchingASI_ID, 0);
			m_changed = true;
		}
		else if (instanceASI.getM_AttributeSetInstance_ID() > 0 && matchingASI_ID > 0 && m_attributeSetInstance_id != matchingASI_ID ) {
			// There is a duplicate
			log.severe("Duplicate set of values for instances: " + matchingASI_ID + " " + m_attributeSetInstance_id);
			// TODO deal with it - merge and delete?
			// TODO translate
			if (!ADialog.ask(m_WindowNo, this, "This instance is a duplicate of an existing instance. Confirm to continue. Cancel to return to the selection dialog."))
				return false;
		}
		else {
			// instanceASI.getM_AttributeSetInstance_ID() > 0 && matchingASI_ID > 0 && m_attributeSetInstance_id == matchingASI_ID
			// This should never happen as findMatchingASI(values) filters out such a match.
			log.severe("M_AttributeSetInstance_ID matches itself!"); 
		}

		// If new or if anything changed, save the instanceASI
		if (instanceASI.is_new() || instanceASI.is_Changed()) {
			m_changed = true;
			instanceASI.save();
		}
		// Note the ID
		m_attributeSetInstance_id = instanceASI.getM_AttributeSetInstance_ID();

		//  Save attributes
		//  m_readWrite is true
		if (m_attributeSetInstance_id > 0 && !instanceASI.hasValues(values)) {
			//	Save all Attribute value instances
			for (int i = 0; i < attributes.length; i++)
			{
				if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attributes[i].getAttributeValueType()))
				{
					MAttributeValue value = (MAttributeValue)values[i];
					attributes[i].setMAttributeInstance(m_attributeSetInstance_id, value);
				}
				else if (MAttribute.ATTRIBUTEVALUETYPE_Number.equals(attributes[i].getAttributeValueType()))
				{
					BigDecimal value = (BigDecimal)values[i];
					attributes[i].setMAttributeInstance(m_attributeSetInstance_id, value);
				}
				else
				{
					String value = (String) values[i];
					attributes[i].setMAttributeInstance(m_attributeSetInstance_id, value);
				}
			}
			m_changed = true;			
		}	//	for all attributes
		
		//	Save Model
		if (m_changed)
		{
			// Reset the description which could change based on the attribute values
			instanceASI.setDescription ();
			instanceASI.save ();
		}
		m_M_AttributeSetInstanceName = instanceASI.getDescription();
		//
		return true;
	}	//	saveSelection

	
	/**************************************************************************
	 * 	Get Instance ID
	 * 	@return Instance ID
	 */
	public int getM_AttributeSetInstance_ID()
	{
		return m_attributeSetInstance_id;
	}	//	getM_AttributeSetInstance_ID

	/**
	 * 	Get Instance Name
	 * 	@return Instance Name
	 */
	public String getM_AttributeSetInstanceName()
	{
		return m_M_AttributeSetInstanceName;
	}	//	getM_AttributeSetInstanceName
	
	/**
	 * Get Locator ID
	 * @return M_Locator_ID
	 */
	public int getM_Locator_ID()
	{
		return m_M_Locator_ID; 
	}

	/**
	 * 	Value Changed
	 *	@return true if changed
	 */
	public boolean isChanged()
	{
		return m_changed;
	}	//	isChanged
	
	/**
	 * Finds an existing Attribute Set Instance that is identical to the current
	 * selection of values.  The match looks at the M_AttributeSetInstance and
	 * the M_AttributeInstance tables for duplicates across in the main fields. 
	 * 
	 * @return The M_AttributeSetInstance_ID of the first matching instance is returned.
	 */
	public int findMatchingASI(Object[] values) {
		// Test main ASI fields
		
		List<Object> parameters = new ArrayList<Object>();
		
		String where =  MAttributeSetInstance.COLUMNNAME_AD_Org_ID + "=?";
		parameters.add(instanceASI.getAD_Org_ID());
		//
		where += " AND " + MAttributeSetInstance.COLUMNNAME_M_AttributeSet_ID + "=?";
		parameters.add(instanceASI.getM_AttributeSet_ID());
		//
		if (instanceASI.getGuaranteeDate() != null) {
			where += " AND " + MAttributeSetInstance.COLUMNNAME_GuaranteeDate + "=?" ;
			parameters.add(instanceASI.getGuaranteeDate());
		}
		else
			where += " AND " + MAttributeSetInstance.COLUMNNAME_GuaranteeDate + " is null";
		//
		if (instanceASI.getLot() != null) {
			where += " AND " + MAttributeSetInstance.COLUMNNAME_Lot + "=?" ;
			parameters.add(instanceASI.getLot());
		}
		else
			where += " AND " + MAttributeSetInstance.COLUMNNAME_Lot + " is null";
		//
		if (instanceASI.getSerNo() != null) {
			where += " AND " + MAttributeSetInstance.COLUMNNAME_SerNo + "=?" ;
			parameters.add(instanceASI.getSerNo());
		}
		else
			where += " AND " + MAttributeSetInstance.COLUMNNAME_SerNo + " is null";
		//
		where += " AND " + MAttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID + "!=?";			
		parameters.add(instanceASI.getM_AttributeSetInstance_ID());
		
		List<MAttributeSetInstance> matchingASIs = new Query(Env.getCtx(), MAttributeSetInstance.Table_Name, where, null)
						.setClient_ID()
						.setParameters(parameters)
						.list();
		
		if (matchingASIs.size() == 0) {
			return 0;
		}
		
		//  Test for same attribute values
		for (MAttributeSetInstance match : matchingASIs) {
			if (match.hasValues(values))
				return match.getM_AttributeSetInstance_ID();
		}
		return 0;
	}


} //	VPAttributeDialog
