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
package org.adempiere.webui.window;

import java.util.logging.Level;

import javax.swing.ImageIcon;

import org.adempiere.controller.AttributeDialogController;
import org.adempiere.controller.AttributeDialogControllerImpl;
import org.adempiere.controller.form.AttributeDialog;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.form.AttributeInstanceSearchDialog;
import org.adempiere.webui.apps.AEnv;
import org.adempiere.webui.component.Button;
import org.adempiere.webui.component.Checkbox;
import org.adempiere.webui.component.ConfirmPanel;
import org.adempiere.webui.component.Grid;
import org.adempiere.webui.component.Label;
import org.adempiere.webui.component.ListItem;
import org.adempiere.webui.component.Listbox;
import org.adempiere.webui.component.Panel;
import org.adempiere.webui.component.Row;
import org.adempiere.webui.component.Rows;
import org.adempiere.webui.component.StringBox;
import org.adempiere.webui.component.Textbox;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.editor.WButtonEditor;
import org.adempiere.webui.editor.WDateEditor;
import org.adempiere.webui.editor.WNumberEditor;
import org.adempiere.webui.editor.WStringEditor;
import org.adempiere.webui.editor.WTimeEditor;
import org.adempiere.webui.editor.WYesNoEditor;
import org.adempiere.webui.panel.InfoPAttributeInstancePanel;
import org.adempiere.webui.session.SessionManager;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MQuery;
import org.compiere.model.MWindow;
import org.compiere.swing.IButton;
import org.compiere.swing.CButton;
import org.compiere.swing.ICheckBox;
import org.compiere.swing.IComboBox;
import org.compiere.swing.IDate;
import org.compiere.swing.CEditor;
import org.compiere.swing.ILabel;
import org.compiere.swing.ITextField;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.South;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;

/**
 * Product Attribute Set Product/Instance Dialog Editor. Called from
 * VPAttribute.actionPerformed
 *
 * @author Jorg Janke
 * 
 *         ZK Port
 * @author Low Heng Sin
 */
public class WPAttributeDialog extends Window
        implements EventListener, AttributeDialog {

    /**
     * 
     */
    private static final long serialVersionUID = -7810825026970615029L;
    private CLogger log = CLogger.getCLogger(getClass());

    /**
     * Product Attribute Instance Dialog
     * 
     * @param attributeSetInstanceId Product Attribute Set Instance id
     * @param productId              Product id
     * @param bpartnerId             b partner
     * @param productWindow          this is the product window (define Product
     *                               Instance)
     * @param columnId               column
     * @param windowNo               window
     */
    public WPAttributeDialog(AttributeDialogInfo info) {

        super();
        this.setTitle(info.getTitle());
        this.setAttribute("modal", Boolean.TRUE);
        this.setBorder("normal");
        this.setWidth("500px");
        this.setHeight("600px");
        this.setSizable(true);

        log.config(info.toString());
        myWindowNo = SessionManager.getAppDesktop().registerWindow(this);
        info.setDialogWindowNo(myWindowNo);
        controller = new AttributeDialogControllerImpl(this, info);

        try {
            init();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "VPAttributeDialog" + ex);
        }

        if (!controller.init()) {
            dispose();
            return;
        }
        AEnv.showCenterScreen(this);

    }

    private static final int INSTANCE_VALUE_LENGTH = 40;

    private AttributeDialogController controller;
    private int myWindowNo;
    private int rowCounter = 0;
    private Checkbox cbNewEdit;
    private Button bSelect;
    private Textbox fieldLotString;
    private Listbox fieldLot;
    private Button bLot;
    Menupopup popupMenu = new Menupopup();
    private Menuitem mZoom;
    private Textbox fieldSerNo;
    private Button bSerNo;
    private WDateEditor fieldGuaranteeDate;
    private Textbox fieldDescription;

    private Borderlayout mainLayout = new Borderlayout();
    private Panel centerPanel = new Panel();
    private Grid centerLayout = new Grid();
    private ConfirmPanel confirmPanel = new ConfirmPanel(true);
    private Rows rows;
    private InfoPAttributeInstancePanel pai;
    private Row row;

    private void init() throws Exception {

        mainLayout.setParent(this);
        mainLayout.setWidth("100%");
        mainLayout.setHeight("100%");
        Center center = new Center();
        center.setParent(mainLayout);
        center.setFlex(true);
        center.appendChild(centerPanel);
        South south = new South();
        south.setParent(mainLayout);
        south.appendChild(confirmPanel);
        centerPanel.appendChild(centerLayout);
        centerLayout.setOddRowSclass("even");
        confirmPanel.addActionListener(Events.ON_CLICK, this);

        rows = new Rows();
        rows.setParent(centerLayout);

    }

//    /**
//     * Dyanmic Init.
//     * 
//     * @return true if initialized
//     */
//    private boolean initAttributes() {
//
//        if (m_M_Product_ID == 0 && !m_productWindow)
//            return false;
//
//        MAttributeSet as = null;
//
//        if (m_M_Product_ID != 0) {
//            // Get Model
//            m_product = MProduct.get(Env.getCtx(), m_M_Product_ID);
//            if (m_product.getM_AttributeSetInstance_ID() > 0) {
//                m_productASI = true;
//                // The product has an instance associated with it.
//                if (m_M_AttributeSetInstance_ID
//                        != m_product.getM_AttributeSetInstance_ID()) {
//                    log.fine(
//                            "Different ASI than what is specified on Product!");
//                }
//            } else {
//                // Only show product attributes when in the product window.
//                m_productASI = m_productWindow;
//            }
//            m_masi = MAttributeSetInstance.get(Env.getCtx(),
//                    m_M_AttributeSetInstance_ID, m_M_Product_ID);
//            if (m_masi == null) {
//                log.severe("No Model for M_AttributeSetInstance_ID="
//                        + m_M_AttributeSetInstance_ID + ", M_Product_ID="
//                        + m_M_Product_ID);
//                return false;
//            }
//            Env.setContext(Env.getCtx(), myWindowNo, "M_AttributeSet_ID",
//                    m_masi.getM_AttributeSet_ID());
//
//            // Get Attribute Set
//            as = m_masi.getMAttributeSet();
//        } else {
//            int M_AttributeSet_ID = Env.getContextAsInt(Env.getCtx(),
//                    m_WindowNoParent, "M_AttributeSet_ID");
//            m_masi = new MAttributeSetInstance(Env.getCtx(), 0,
//                    M_AttributeSet_ID, null);
//            as = m_masi.getMAttributeSet();
//        }
//
//        // Product has no Attribute Set
//        if (as == null) {
//            FDialog.error(myWindowNo, this, "PAttributeNoAttributeSet");
//            return false;
//        }
//
//        // BF3468823 Show Product Attributes
//        // Product attributes can be shown in any window but are read/write only
//        // in the product
//        // window. Instance attributes are shown in any window but the product
//        // window and are
//        // always read/write. The two are exclusive and can't co-exists.
//        if (!m_productWindow || !m_productASI) // Set Instance Attributes and
//                                               // dialog controls
//        {
//            if (!m_productASI) // Instance attributes possible. Set up controls.
//            {
//                Row row = new Row();
//
//                // New/Edit - Selection
//                if (m_M_AttributeSetInstance_ID == 0) // new
//                    cbNewEdit.setLabel(Util
//                            .cleanAmp(Msg.getMsg(Env.getCtx(), "NewRecord")));
//                else
//                    cbNewEdit.setLabel(Util
//                            .cleanAmp(Msg.getMsg(Env.getCtx(), "EditRecord")));
//                cbNewEdit.addEventListener(Events.ON_CHECK, this);
//                row.appendChild(cbNewEdit);
//                bSelect.setLabel(Util
//                        .cleanAmp(Msg.getMsg(Env.getCtx(), "SelectExisting")));
//                bSelect.setImage("images/PAttribute16.png");
//                bSelect.addEventListener(Events.ON_CLICK, this);
//                row.appendChild(bSelect);
//                rows.appendChild(row);
//            }
//            // Add the Instance Attributes if any. If its a product attribute
//            // set
//            // this will do nothing.
//            MAttribute[] attributes = as.getMAttributes(true); // True =
//                                                               // Instances
//            log.fine("Instance Attributes=" + attributes.length);
//            for (int i = 0; i < attributes.length; i++)
//                addAttributeLine(rows, attributes[i], false, false);
//        }
//        // Product attributes can be shown in any window but are read/write in
//        // the Product window only.
//        // This will do nothing if it is an instance attribute set.
//        MAttribute[] attributes = as.getMAttributes(false); // False = products
//        log.fine("Product Attributes=" + attributes.length);
//        for (int i = 0; i < attributes.length; i++)
//            addAttributeLine(rows, attributes[i], true, !m_productWindow);
//
//        // Lot
//        if ((!m_productWindow || !m_productASI) && as.isLot()) {
//            Row row = new Row();
//            row.setParent(rows);
//            rowCounter++;
//            Label label = new Label(
//                    Util.cleanAmp(Msg.translate(Env.getCtx(), "Lot")));
//            row.appendChild(label);
//            row.appendChild(fieldLotString);
//            fieldLotString.setText(m_masi.getInstanceLot());
//            // M_Lot_ID
//            // int AD_Column_ID = 9771; // M_AttributeSetInstance.M_Lot_ID
//            // fieldLot = new VLookup ("M_Lot_ID", false,false, true,
//            // MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, AD_Column_ID,
//            // DisplayType.TableDir));
//            String sql = "SELECT M_Lot_ID, Name "
//                    + "FROM M_Lot l "
//                    + "WHERE EXISTS (SELECT M_Product_ID FROM M_Product p "
//                    + "WHERE p.M_AttributeSet_ID="
//                    + m_masi.getM_AttributeSet_ID()
//                    + " AND p.M_Product_ID=l.M_Product_ID)";
//            fieldLot = new Listbox();
//            fieldLot.setMold("select");
//            KeyNamePair[] keyNamePairs = DB.getKeyNamePairs(sql, true);
//            for (KeyNamePair pair : keyNamePairs) {
//                fieldLot.appendItem(pair.getName(), pair.getKey());
//            }
//
//            label = new Label(
//                    Util.cleanAmp(Msg.translate(Env.getCtx(), "M_Lot_ID")));
//            row = new Row();
//            row.setParent(rows);
//            rowCounter++;
//            row.appendChild(label);
//            row.appendChild(fieldLot);
//            if (m_masi.getM_Lot_ID() != 0) {
//                for (int i = 1; i < fieldLot.getItemCount(); i++) {
//                    ListItem pp = fieldLot.getItemAtIndex(i);
//                    if ((Integer) pp.getValue() == m_masi.getM_Lot_ID()) {
//                        fieldLot.setSelectedIndex(i);
//                        fieldLotString.setReadonly(true);
//                        break;
//                    }
//                }
//            }
//            fieldLot.addEventListener(Events.ON_SELECT, this);
//            // New Lot Button
//            if (m_masi.getMAttributeSet().getM_LotCtl_ID() != 0) {
//                if (MRole.getDefault().isTableAccess(MLot.Table_ID, false)
//                        && MRole.getDefault().isTableAccess(MLotCtl.Table_ID,
//                                false)
//                        && !m_masi.isExcludeLot(m_AD_Column_ID,
//                                Env.isSOTrx(Env.getCtx(), m_WindowNoParent))) {
//                    row = new Row();
//                    row.setParent(rows);
//                    rowCounter++;
//                    row.appendChild(bLot);
//                    bLot.addEventListener(Events.ON_CLICK, this);
//                }
//            }
//            // Popup
////			fieldLot.addMouseListener(new VPAttributeDialog_mouseAdapter(this));    //  popup
//            mZoom = new Menuitem(Msg.getMsg(Env.getCtx(), "Zoom"),
//                    "images/Zoom16.png");
//            mZoom.addEventListener(Events.ON_CLICK, this);
//            popupMenu.appendChild(mZoom);
//            this.appendChild(popupMenu);
//        } // Lot
//
//        // SerNo
//        if ((!m_productWindow || !m_productASI) && as.isSerNo()) {
//            Row row = new Row();
//            row.setParent(rows);
//            rowCounter++;
//            Label label = new Label(
//                    Util.cleanAmp(Msg.translate(Env.getCtx(), "SerNo")));
//            row.appendChild(label);
//            row.appendChild(fieldSerNo);
//            fieldSerNo.setText(m_masi.getSerNo());
//
//            // New SerNo Button
//            if (m_masi.getMAttributeSet().getM_SerNoCtl_ID() != 0) {
//                if (MRole.getDefault().isTableAccess(MSerNoCtl.Table_ID, false)
//                        && !m_masi.isExcludeSerNo(m_AD_Column_ID,
//                                Env.isSOTrx(Env.getCtx(), m_WindowNoParent))) {
//                    row = new Row();
//                    row.setParent(rows);
//                    rowCounter++;
//                    row.appendChild(bSerNo);
//                    bSerNo.addEventListener(Events.ON_CLICK, this);
//                }
//            }
//        } // SerNo
//
//        // GuaranteeDate
//        if ((!m_productWindow || !m_productASI) && as.isGuaranteeDate()) {
//            Row row = new Row();
//            row.setParent(rows);
//            rowCounter++;
//            Label label = new Label(Util
//                    .cleanAmp(Msg.translate(Env.getCtx(), "GuaranteeDate")));
//            if (m_M_AttributeSetInstance_ID == 0)
//                fieldGuaranteeDate.setValue(m_masi.getGuaranteeDate(true));
//            else
//                fieldGuaranteeDate.setValue(m_masi.getGuaranteeDate());
//            row.appendChild(label);
//            row.appendChild(fieldGuaranteeDate);
//        } // GuaranteeDate
//
//        if (rowCounter == 0) {
//            FDialog.error(myWindowNo, this, "PAttributeNoInfo");
//            return false;
//        }
//
//        // New/Edit Window
//        if (!m_productWindow) {
//            cbNewEdit.setChecked(m_M_AttributeSetInstance_ID == 0);
//            cmd_newEdit();
//        }
//
//        // Attrribute Set Instance Description
//        Label label = new Label(
//                Util.cleanAmp(Msg.translate(Env.getCtx(), "Description")));
////		label.setLabelFor(fieldDescription);
//        fieldDescription.setText(m_masi.getDescription());
//        fieldDescription.setReadonly(true);
//        Row row = new Row();
//        row.setParent(rows);
//        row.appendChild(label);
//        row.appendChild(fieldDescription);
//
//        return true;
//
//    } // initAttribute

//    /**
//     * Add Attribute Line
//     * 
//     * @param attribute attribute
//     * @param product   product level attribute
//     * @param readOnly  value is read only
//     */
//    private void addAttributeLine(Rows rows, MAttribute attribute,
//            boolean product, boolean readOnly) {
//
//        log.fine(attribute + ", Product=" + product + ", R/O=" + readOnly);
//
//        rowCounter++;
//        Label label = new Label(attribute.getName());
//        if (product)
//            label.setStyle("font-weight: bold");
//
//        if (attribute.getDescription() != null)
//            label.setTooltiptext(attribute.getDescription());
//
//        Row row = rows.newRow();
//        row.appendChild(label.rightAlign());
//        //
//        MAttributeInstance instance =
//                attribute.getMAttributeInstance(m_M_AttributeSetInstance_ID);
//        if (MAttribute.ATTRIBUTEVALUETYPE_List
//                .equals(attribute.getAttributeValueType())) {
//            MAttributeValue[] values = attribute.getMAttributeValues(); // optional
//                                                                        // =
//                                                                        // null
//            Listbox editor = new Listbox();
//            editor.setMold("select");
//            for (MAttributeValue value : values) {
//                ListItem item = new ListItem(
//                        value != null ? value.getName() : "", value);
//                editor.appendChild(item);
//            }
//            boolean found = false;
//            if (instance != null) {
//                for (int i = 0; i < values.length; i++) {
//                    if (values[i] != null && values[i].getM_AttributeValue_ID()
//                            == instance.getM_AttributeValue_ID()) {
//                        editor.setSelectedIndex(i);
//                        found = true;
//                        break;
//                    }
//                }
//                if (found)
//                    log.fine("Attribute=" + attribute.getName() + " #"
//                            + values.length + " - found: " + instance);
//                else
//                    log.warning("Attribute=" + attribute.getName() + " #"
//                            + values.length + " - NOT found: " + instance);
//            } // setComboBox
//            else
//                log.fine("Attribute=" + attribute.getName() + " #"
//                        + values.length + " no instance");
//            row.appendChild(editor);
//            if (readOnly)
//                editor.setEnabled(false);
//            else
//                m_editors.add(editor);
//        } else if (MAttribute.ATTRIBUTEVALUETYPE_Number
//                .equals(attribute.getAttributeValueType())) {
//            NumberBox editor = new NumberBox(false);
//            if (instance != null)
//                editor.setValue(instance.getValueNumber());
//            else
//                editor.setValue(Env.ZERO);
//            row.appendChild(editor);
//            if (readOnly)
//                editor.setEnabled(false);
//            else
//                m_editors.add(editor);
//        } else // Text Field
//        {
//            Textbox editor = new Textbox();
//            if (instance != null)
//                editor.setText(instance.getValue());
//            row.appendChild(editor);
//            if (readOnly)
//                editor.setEnabled(false);
//            else
//                m_editors.add(editor);
//        }
//
//    } // addAttributeLine

    @Override
    public void dispose() {

        this.detach();

    }

    public void onEvent(Event e) throws Exception {

        if (e.getTarget().getId().equals("Ok"))
            controller.actionOK();
        else if (e.getTarget().getId().equals("Cancel"))
            controller.actionCancel();
        else if (e.getTarget() == mZoom)
            cmd_zoom();
        else
            controller.action(e.getTarget());

    }

    /**
     * Zoom M_Lot
     */
    private void cmd_zoom() {

        int lotId = 0;
        ListItem pp = fieldLot.getSelectedItem();
        if (pp != null)
            lotId = (Integer) pp.getValue();
        MQuery zoomQuery = new MQuery("M_Lot");
        zoomQuery.addRestriction("M_Lot_ID", MQuery.EQUAL, lotId);
        log.info(zoomQuery.toString());
        //
        int windowId = MWindow.getWindow_ID("Lot"); // Lot
        AEnv.zoom(windowId, zoomQuery);

    } // cmd_zoom

    @Override
    public void openAttributeSetInstanceSearchAndSelectDialog(String title,
            int warehouseId, int locatorId, int bpartnerId, int productId) {

        pai = new InfoPAttributeInstancePanel(this,
                title, warehouseId, locatorId, productId, bpartnerId);

    }

    @Override
    public AttributeInstanceSearchDialog getAttributeInstanceSearchDialog() {

        return pai;

    }

    @Override
    public void clearPanel() {

//        for (Object child : centerLayout.getChildren())
//            centerLayout.removeChild((Component) child);
//
//        rows = new Rows();
//        rows.setParent(centerLayout);

    }

    @Override
    public ICheckBox createNewEditCheckBox() {

        cbNewEdit = new Checkbox();
        cbNewEdit.addEventListener(Events.ON_CHECK, this);
        addComponentToNewRow(cbNewEdit);
        return cbNewEdit;

    }

    void addComponentToNewRow(Component comp) {

        row = rows.newRow();
        rowCounter++;
        row.appendChild(comp);

    }

    @Override
    public IButton createButtonSelect(String imagePath) {

        bSelect.setImage("images/" + imagePath);
        bSelect.addEventListener(Events.ON_CLICK, this);
        addComponentToCurrentRow(bSelect);
        return bSelect;

    }

    void addComponentToCurrentRow(Component comp) {

        row.appendChild(comp);

    }

    @Override
    public IButton createButtonSerialNumber(String name) {

        bSerNo = new Button(name);
        return bSerNo;

    }

    @Override
    public IButton createButtonLot(String name) {

        bLot = new Button(name);
        return bLot;

    }

    @Override
    public ILabel createAttributeGroupLable(String name) {

        Label label = new Label(name);
        label.setFontBold(true);
        addToNewRow(label);
        return label;

    }

    @Override
    public void enableCancel(boolean enable) {

        confirmPanel.setEnabled(ConfirmPanel.A_CANCEL, enable);

    }

    @Override
    public ITextField getFieldLotString() {

        return fieldLotString;

    }

    @Override
    public IComboBox getFieldLot() {

        return fieldLot;

    }

    @Override
    public ITextField getFieldSerNo() {

        return fieldSerNo;

    }

    @Override
    public CEditor getFieldGuaranteeDate() {

        return fieldGuaranteeDate;

    }

    @Override
    public ILabel createLabel(String name) {

        Label label = new Label(name);
        addToNewRow(label.rightAlign());
        return label;

    }

    void addToNewRow(Component component) {

        row = rows.newRow();
        row.appendChild(component);
        rowCounter++;

    }

    @Override
    public ILabel createGroupLabel(String name) {

        Label label = new Label(name);
        label.setFontBold(true);
        addToNewRow(label);
        return label;

    }

    @Override
    public ITextField getFieldDescription() {

        return fieldDescription;

    }

    @Override
    public ITextField createFieldDescription() {

        fieldDescription = new Textbox();
        fieldDescription.setReadonly(true);
        addComponentToCurrentRow(fieldDescription);
        return fieldDescription;

    }

    @Override
    public void resize() {

        centerPanel.invalidate();

    }

    @Override
    public IComboBox createCComboBox(MAttributeValue[] values) {

        Listbox combo = new Listbox();
        combo.setMold("select");
        for (MAttributeValue value : values) {
            if (value == null) {
                combo.appendItem("", null);
            }
            else {
                combo.appendItem(value.getKeyNamePair().getName(), value);
            }
        }
        addComponentToCurrentRow(combo);
        return combo;

    }

    @Override
    public CEditor createNumberEditor(String name, boolean mandatory,
            boolean readOnly, boolean updateable, int displayType,
            String title) {

        WNumberEditor ed =
                new WNumberEditor(name, mandatory, readOnly, updateable,
                        displayType, title);
        addComponentToCurrentRow(ed.getComponent());
        return ed;

    }

    @Override
    public ITextField createStringEditor(String name, boolean mandatory,
            boolean readOnly, boolean updateable, int displayLength,
            int fieldLength, String vFormat, String obscureType) {

        Textbox ed = new Textbox();
        ed.setMandatory(mandatory);
        ed.setReadonly(readOnly);
        addComponentToCurrentRow(ed);
        return ed;

    }

    @Override
    public ITextField createFieldLotString() {

        fieldLotString = new Textbox();
        fieldLotString.addEventListener(Events.ON_CHANGE, this);
        addComponentToCurrentRow(fieldLotString);
        return fieldLotString;

    }

    @Override
    public IComboBox createFieldLot(KeyNamePair[] lotKeyNamePairs) {

        fieldLot = new Listbox();
        fieldLot.setMold("select");
        for (KeyNamePair pair : lotKeyNamePairs)
            fieldLot.appendItem(pair.getName(), pair.getKey());

        fieldLot.addEventListener(Events.ON_SELECT, this);
        addComponentToCurrentRow(fieldLot);

        mZoom = new Menuitem(Msg.getMsg(Env.getCtx(), "Zoom"),
                "images/Zoom16.png");
        mZoom.addEventListener(Events.ON_CLICK, this);
        popupMenu.appendChild(mZoom);
        this.appendChild(popupMenu);
        return fieldLot;

    }

    @Override
    public void createNewLotButton() {

        bLot = new Button();
        bLot.addEventListener(Events.ON_CLICK, this);
        addToNewRow(bLot);
        
    }

    @Override
    public ITextField createFieldSerNo(String serNo) {

        fieldSerNo = new Textbox();
        fieldSerNo.addEventListener(Events.ON_CHANGE, this);
        addComponentToCurrentRow(fieldSerNo);
        return fieldSerNo;

    }

    @Override
    public void createNewSerNoButton() {

        bSerNo = new Button();
        bSerNo.addEventListener(Events.ON_CLICK, this);
        addToNewRow(bSerNo);
        
    }

    @Override
    public IDate createFieldGuaranteeDate(String name) {

        fieldGuaranteeDate = new WDateEditor("GuaranteeDate", false, false, true, name);
        fieldGuaranteeDate.getComponent().addEventListener(Events.ON_CHANGE, this);
        addComponentToCurrentRow(fieldGuaranteeDate.getComponent());
        return fieldGuaranteeDate;

    }

    @Override
    public void showError(String string, String errorMsg) {

        FDialog.error(myWindowNo, this, errorMsg);

    }

    @Override
    public void reset() {

        // TODO Auto-generated method stub

    }

    @Override
    public int getM_AttributeSetInstance_ID() {

        return controller.getAttributeSetInstanceId();

    }

    @Override
    public String getM_AttributeSetInstanceName() {

        return controller.getM_AttributeSetInstanceName();

    }

    public boolean isChanged() {

        return controller.isChanged();

    }

}