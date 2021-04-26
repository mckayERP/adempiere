/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2021 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.adempiere.controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.adempiere.controller.form.AttributeDialog;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.form.AttributeInstanceSearchDialog;
import org.adempiere.controller.ed.AttributeEditor;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.GridTab;
import org.compiere.model.I_M_AttributeSet;
import org.compiere.model.I_M_Lot;
import org.compiere.model.I_M_LotCtl;
import org.compiere.model.I_M_MovementLine;
import org.compiere.model.I_M_SerNoCtl;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeSetInstanceManager;
import org.compiere.model.MAttributeSetInstanceManager.AttributeSetInstanceMandatoryValueException;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MDocType;
import org.compiere.model.MProduct;
import org.compiere.model.MRole;
import org.compiere.model.X_C_DocType;
import org.compiere.model.X_M_Attribute;
import org.compiere.swing.IButton;
import org.compiere.swing.ICheckBox;
import org.compiere.swing.IComboBox;
import org.compiere.swing.IDate;
import org.compiere.swing.CEditor;
import org.compiere.swing.ILabel;
import org.compiere.swing.ITextField;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.Msg;
import org.compiere.util.Util;

public class AttributeDialogControllerImpl
        implements AttributeDialogController {

    private static final long serialVersionUID = 2274914193424031743L;

    private static CLogger log =
            CLogger.getCLogger(AttributeDialogControllerImpl.class);

    private static final String M_ATTRIBUTE_SET_INSTANCE_ID =
            "M_AttributeSetInstance_ID";

    /** A set for the editor and whether it is a productAttribute or not. */
    private class MEditor {

        public MEditor(CEditor ceditor, boolean isProductAttribute) {

            this.editor = ceditor;
            this.isProductAttribute = isProductAttribute;

        }

        private CEditor editor = null;
        private boolean isProductAttribute = false;

    }

    private transient ArrayList<MEditor> editorList = new ArrayList<>();

    private transient AttributeEditor editor;
    private boolean isSOTrx;
    private int windowNo;
    private GridTab gridTab;
    private transient AttributeDialog dialog;
    private int productId;
    private int attributeSetInstanceId;
    private int bpartnerId;
    private boolean isProductWindow;
    private int columnId;
    private boolean readWrite;
    private AttributeDialogInfo info;
    private MAttributeSetInstance instanceASI;
    private boolean hasProductASI;
    private int productASIId;
    private MAttributeSetInstance productASI;

    private MAttributeSet attributeSet;

    private boolean productAttributesReadWrite;

    private boolean showInstanceAttributes;

    private boolean instanceAttributesReadWrite;

    private int locatorId = 0;

    private String columnName;

    private int initialAttributeSetInstanceId;

    private transient ICheckBox cbNewEdit;

    private transient IButton bSerNo;

    private transient IButton bLot;

    private int dialogWindowNo;

    private static final int INSTANCE_VALUE_LENGTH = 40;

    private ITextField fieldLotString;

    private IComboBox fieldLot;

    private ITextField fieldSerNo;

    private IDate fieldGuaranteeDate;

    private IButton bSelect;

    public AttributeDialogControllerImpl(AttributeDialog dialog,
            AttributeDialogInfo info) {

        this.dialog = dialog;
        this.info = info;

        windowNo = info.getWindowNo();
        dialogWindowNo = info.getDialogWindowNo();
        attributeSetInstanceId = info.getAttributeSetInstanceId();
        initialAttributeSetInstanceId = attributeSetInstanceId;
        isSOTrx = Env.isSOTrx(Env.getCtx(), windowNo);
        columnId = info.getColumnId();
        productId = info.getProductId();
        isProductWindow = info.isProductWindow();
        readWrite = info.isReadWrite();

    }

    public boolean init() {

        return createAttributes();

    }

    public String getDialogTitle() {

        String title = "";

        if (isProductWindow) {
            title = Msg.translate(Env.getCtx(), "M_Product_ID") + " "
                    + Msg.translate(Env.getCtx(), M_ATTRIBUTE_SET_INSTANCE_ID)
                    + ": #" + attributeSetInstanceId;
        } else {
            title = Msg.translate(Env.getCtx(), M_ATTRIBUTE_SET_INSTANCE_ID)
                    + ": #" + attributeSetInstanceId;
        }

        return title;

    }

    public boolean initAttributes() {

        // Don't open a dialog if the product is not defined and we are not in
        // the Product Window. In the Product window, the attribute set
        // instance can be defined before the product record is saved and the
        // M_Product_ID value is created. Outside the Product window, the
        // Product ID value is required to create the ASI as the product
        // provides the attribute set and the "master" asi attribute values,
        // if the a product ASI is defined.
        if (productId == 0 && !isProductWindow)
            return false;

        attributeSet = null;
        instanceASI = getInstanceASI();
        productASI = getProductASI();
        if (!productAndInstanceASIUseSameAttributeSet())
            return false;

        attributeSet = getAttributeSetFromProductOrInstance();

        if (attributeSet == null)
            return false;

        // BF3468823 Show Product Attributes
        // Product attributes can be shown in any window but are read/write only
        // in the product window. Instance attributes are shown in any window
        // but the product window and are always read/write.
        productAttributesReadWrite = isProductWindow && readWrite;
        showInstanceAttributes = !isProductWindow;
        instanceAttributesReadWrite = readWrite && showInstanceAttributes
                && attributeSet.isInstanceAttribute();

        return true;

    }

    void initInstanceAttributes(Boolean isNew) {

        addInstancesAttributes(isNew);
        addLot(isNew);
        addSerialNumber(isNew);
        addGuaranteeDate(isNew);

    }

    void addGuaranteeDate(Boolean isNew) {

        if (isGuaranteeDate()) {
            ILabel label = dialog
                    .createLabel(Msg.translate(Env.getCtx(), "GuaranteeDate"));
            fieldGuaranteeDate = addGuranteeDateField();
            label.setLabelFor(fieldGuaranteeDate);
            fieldGuaranteeDate.setReadWrite(readWrite && !isNew);
            fieldGuaranteeDate.setMandatory(
                    isGuaranteeDateMandatory() && readWrite);
            if (isNewGuaranteeDate())
                fieldGuaranteeDate
                        .setValue(getNewGuaranteeDate());
            else
                fieldGuaranteeDate
                        .setValue(getGuaranteeDate());
            if (fieldGuaranteeDate.isMandatory()) {
                fieldGuaranteeDate.setBackground(
                        fieldGuaranteeDate.getValue() == null);
            }
        }

    }

    IDate addGuranteeDateField() {

        return dialog.createFieldGuaranteeDate(
                Msg.translate(Env.getCtx(), "GuaranteeDate"));

    }

    void addSerialNumber(Boolean isNew) {

        if (isSerNo()) {
            fieldSerNo = addSerialNumberField(isNew);
            if (isEnableNewSerNoButton()) {
                dialog.createNewSerNoButton();
            }
        }

    }

    void addLot(Boolean isNew) {

        if (isLot()) {
            fieldLotString = addLotString(isNew);
            fieldLot = addLotField(isNew, fieldLotString);
            if (isEnableNewLotButton()) {
                dialog.createNewLotButton();
            }
        }

    }

    void addInstancesAttributes(Boolean isNew) {

        if (isAttributeSetAnInstance())
            dialog.createGroupLabel(
                    Msg.translate(Env.getCtx(), "IsInstanceAttribute"));
        MAttribute[] attributes = getInstanceAttributes();
        log.fine("Instance Attributes=" + attributes.length);
        for (int i = 0; i < attributes.length; i++)
            addAttributeLine(attributes[i], false, !readWrite && isNew);

    }

    ITextField addSerialNumberField(Boolean isNew) {

        ILabel label = dialog.createLabel(Msg.translate(Env.getCtx(), "SerNo"));
        ITextField fsn = dialog.createFieldSerNo(getSerNo());
        label.setLabelFor(fsn);
        fsn.setText(getSerNo());
        fsn.setReadWrite(readWrite && !isNew);
        fsn.setMandatory(isSerNoMandatory() && readWrite);
        if (isSerNoMandatory() && readWrite) {
            fsn.setBackground(fsn.getText() == null
                    || fsn.getText().isEmpty());
        }

        return fsn;

    }

    IComboBox addLotField(Boolean isNew, ITextField fieldLotString) {

        ILabel label =
                dialog.createLabel(Msg.translate(Env.getCtx(), "M_Lot_ID"));
        IComboBox fl = dialog.createFieldLot(getLotKeyNamePairs());
        label.setLabelFor(fl);
        int selectedKey = getInstanceLotId();
        if (selectedKey != 0) {
            for (int i = 1; i < fl.getItemCount(); i++) {
                KeyNamePair pp =
                        (KeyNamePair) fl.getItemAt(i);
                if (pp.getKey() == selectedKey) {
                    fl.setSelectedIndex(i);
                    fieldLotString.setEditable(false);
                    break;
                }
            }
        }
        fl.setReadWrite(readWrite && !isNew);
        if (isLotMandatory()) {
            fl.setBackground(fl.getDisplay() == null
                    || fl.getDisplay().isEmpty());
        }

        return fl;

    }

    ITextField addLotString(Boolean isNew) {

        ILabel label = dialog.createLabel(Msg.translate(Env.getCtx(), "Lot"));
        ITextField fls = dialog.createFieldLotString();
        label.setLabelFor(fls);
        fls.setReadWrite(readWrite && !isNew);
        fls.setMandatory(isLotMandatory());
        fls.setText(getInstanceLot());
        if (isLotMandatory()) {
            fls
                    .setBackground(fls.getText() == null
                            || fls.getText().isEmpty());
        }
        return fls;

    }

    void setAttributeSetInContext() {

        if (attributeSet != null) {
            Env.setContext(Env.getCtx(), windowNo,
                    I_M_AttributeSet.COLUMNNAME_M_AttributeSet_ID,
                    attributeSet.getM_AttributeSet_ID());
        }

    }

    void getAttributeSetFromContext() {

        int attributeSetId = Env.getContextAsInt(Env.getCtx(),
                windowNo, "M_AttributeSet_ID");
        instanceASI = new MAttributeSetInstance(Env.getCtx(), 0,
                attributeSetId, null);
        attributeSet = instanceASI.getMAttributeSet();

    }

    MAttributeSet getAttributeSetFromProductOrInstance() {

        MAttributeSet as = null;
        if (hasProductASI) {
            as = productASI.getMAttributeSet();
        } else if (instanceASI.getM_AttributeSet_ID() > 0) {
            as = instanceASI.getMAttributeSet();
        } else {
            int attributeSetId = Env.getContextAsInt(Env.getCtx(),
                    windowNo, "M_AttributeSet_ID");
            as = new MAttributeSet(Env.getCtx(), attributeSetId, null);
        }

        setAttributeSetInContext();

        return as;
    }

    private MAttributeSetInstance getProductASI() {

        MAttributeSetInstance pASI;
        hasProductASI = false;
        pASI = null;
        productASIId = 0;
        if (productId != 0) {
            int id = productId;
            MProduct product = getProduct(id);
            if (product.getM_AttributeSetInstance_ID() > 0) {
                hasProductASI = true;
                productASIId = product.getM_AttributeSetInstance_ID();
                pASI = (MAttributeSetInstance) product
                        .getM_AttributeSetInstance();
            }
        }
        return pASI;
    }

    MProduct getProduct(int id) {

        return MProduct.get(Env.getCtx(), id);

    }

    private boolean productAndInstanceASIUseSameAttributeSet() {

        // Check for conflicts in attribute sets between the product and
        // this instance. They have to use the same attribute set.
        if (hasProductASI
                && attributeSetInstanceId != 0
                && attributeSetInstanceId != productASIId
                && !isProductWindow
                && productASI.getM_AttributeSet_ID()
                        != instanceASI.getM_AttributeSet_ID()) {
            getLogger().severe("Incompatible attribute sets between the "
                    + "provided ASI and the product ASI.");
            return false;
        }
        return true;

    }

    private Logger getLogger() {

        return log;

    }

    void checkForASIConflictInProductWindow() {

        if (hasProductASI && attributeSetInstanceId != 0
                && attributeSetInstanceId != productASIId
                && isProductWindow) {
            // Major problem that we can't deal with. There can't be two ASI
            // values for a product in any record and certainly not in the
            // Product window.
            throw new AdempiereException(
                    "@Error@ @Invalid@ @M_AttributeSetInstance_ID@");
        }

    }

    public MAttributeSetInstance getInstanceASI() {

        return MAttributeSetInstance.get(Env.getCtx(),
                attributeSetInstanceId, productId);

    }

    public AttributeEditor getEditor() {

        return editor;

    }

    public void setEditor(AttributeEditor editor) {

        this.editor = editor;

    }

    public int getWindowNo() {

        return windowNo;

    }

    public void setWindowNo(int windowNo) {

        this.windowNo = windowNo;

    }

    public GridTab getGridTab() {

        return gridTab;

    }

    public void setGridTab(GridTab gridTab) {

        this.gridTab = gridTab;

    }

    @Override
    public boolean isProductAttributesReadWrite() {

        return productAttributesReadWrite;

    }

    @Override
    public boolean isShowInstanceAttributes() {

        return showInstanceAttributes;

    }

    @Override
    public boolean isInstanceAttributesReadWrite() {

        return instanceAttributesReadWrite;

    }

    @Override
    public boolean isOKToAddButtonsAndControls() {

        return productAttributesReadWrite || instanceAttributesReadWrite;

    }

    @Override
    public boolean isEnableNewRecord() {

        // Don't edit an existing ASI. Always create a new record.
        // If the values match an existing ASI, the existing ASI will be
        // used instead.
        return attributeSetInstanceId >= 0;

    }

    @Override
    public boolean isEnableSelectFromExisting() {

        // If not in the product window, ASI values can be selected from the
        // set of existing records
        return !isProductWindow;

    }

    @Override
    public boolean isAttributeSetAnInstance() {

        return attributeSet.isInstanceAttribute();

    }

    @Override
    public MAttribute[] getInstanceAttributes() {

        return attributeSet.getMAttributes(true);

    }

    @Override
    public MAttribute[] getProductAttributes() {

        return attributeSet.getMAttributes(false);

    }

    @Override
    public boolean isLot() {

        return attributeSet.isLot();

    }

    @Override
    public String getInstanceLot() {

        return instanceASI.getLot();

    }

    @Override
    public boolean isLotMandatory() {

        return attributeSet.isLotMandatory() && readWrite;

    }

    @Override
    public KeyNamePair[] getLotKeyNamePairs() {

        String sql = "SELECT M_Lot_ID, Name "
                + "FROM M_Lot l "
                + "WHERE EXISTS (SELECT M_Product_ID FROM M_Product p "
                + "WHERE p.M_AttributeSet_ID="
                + instanceASI.getM_AttributeSet_ID()
                + " AND p.M_Product_ID=l.M_Product_ID)";
        return DB.getKeyNamePairs(sql, true);

    }

    @Override
    public int getInstanceLotId() {

        return instanceASI.getM_Lot_ID();

    }

    @Override
    public boolean isEnableNewLotButton() {

        boolean isNew = isOKToAddButtonsAndControls() && isEnableNewRecord();
        return instanceASI.getMAttributeSet().getM_LotCtl_ID() != 0
                && readWrite && !isNew
                && MRole.getDefault().isTableAccess(I_M_Lot.Table_ID, false)
                && MRole.getDefault().isTableAccess(I_M_LotCtl.Table_ID, false)
                && !instanceASI.isExcludeLot(columnId, isSOTrx);

    }

    @Override
    public boolean isSerNo() {

        return attributeSet.isSerNo();

    }

    @Override
    public String getSerNo() {

        return instanceASI.getSerNo();

    }

    @Override
    public boolean isSerNoMandatory() {

        return attributeSet.isSerNoMandatory();

    }

    @Override
    public boolean isEnableNewSerNoButton() {

        boolean isNew = isOKToAddButtonsAndControls() && isEnableNewRecord();
        return instanceASI.getMAttributeSet().getM_SerNoCtl_ID() != 0
                && readWrite && !isNew
                && MRole.getDefault().isTableAccess(I_M_SerNoCtl.Table_ID,
                        false)
                && !instanceASI.isExcludeSerNo(columnId, isSOTrx);

    }

    @Override
    public boolean isGuaranteeDate() {

        return attributeSet.isGuaranteeDate();

    }

    @Override
    public boolean isGuaranteeDateMandatory() {

        return attributeSet.isGuaranteeDateMandatory();

    }

    @Override
    public boolean isNewGuaranteeDate() {

        return attributeSetInstanceId == 0;

    }

    @Override
    public Timestamp getNewGuaranteeDate() {

        return instanceASI.getGuaranteeDate(true);

    }

    @Override
    public Timestamp getGuaranteeDate() {

        return instanceASI.getGuaranteeDate();

    }

    @Override
    public String getProductAttributeGroupLabel() {

        return Msg.translate(Env.getCtx(), "IsProductAttribute");

    }

    @Override
    public boolean addNewEditButtonForProductAttributes() {

        return columnId != 0 && (isProductAttributesReadWrite()
                || isInstanceAttributesReadWrite());

    }

    @Override
    public boolean isNewEditSelectedForProductAttributes() {

        return attributeSetInstanceId == 0;

    }

    @Override
    public String getInstanceDescription() {

        return instanceASI.getDescription();

    }

    @Override
    public MAttributeInstance getAttributeInstance(MAttribute attribute) {

        MAttributeInstance instance = null;
        if (attributeSetInstanceId != 0)
            instance = attribute.getMAttributeInstance(attributeSetInstanceId);
        else if (hasProductASI && productASIId != 0)
            instance = attribute.getMAttributeInstance(productASIId);
        return instance;

    }

    @Override
    public void dispose() {

        Env.clearWinContext(dialogWindowNo);
        Env.setContext(Env.getCtx(), windowNo, Env.TAB_INFO, columnName,
                String.valueOf(attributeSetInstanceId));
        Env.setContext(Env.getCtx(), windowNo, Env.TAB_INFO, "M_Locator_ID",
                String.valueOf(locatorId));

        dialog.dispose();
    }

    @Override
    public void setLotId(int id) {

        instanceASI.setM_Lot_ID(id);

    }

    @Override
    public void createNewLot() {

        KeyNamePair pp = instanceASI.createLot(productId);
        if (pp != null) {
            dialog.getFieldLot().addItem(pp);
            dialog.getFieldLot().setSelectedItem(pp);
            dialog.getFieldLotString().setText(getLot());
            dialog.getFieldLotString().setEditable(false);
        }

    }

    @Override
    public String getLot() {

        return instanceASI.getLot();

    }

    @Override
    public String getNewSerialNumber() {

        return instanceASI.getSerNo(true);

    }

    @Override
    public void actionCancel() {

        attributeSetInstanceId = 0;
        locatorId = 0;
        dispose();

    }

    @Override
    public void actionSelect() {

        int warehouseId = Env.getContextAsInt(Env.getCtx(), windowNo,
                "M_Warehouse_ID");

        int docTypeId = Env.getContextAsInt(Env.getCtx(), windowNo,
                "C_DocType_ID");
        if (docTypeId > 0) {
            MDocType doctype = new MDocType(Env.getCtx(), docTypeId, null);
            String docbase = doctype.getDocBaseType();
            // consider also old lot numbers at inventory
            if (docbase.equals(X_C_DocType.DOCBASETYPE_MaterialReceipt)
                    || docbase.equals(
                            X_C_DocType.DOCBASETYPE_MaterialPhysicalInventory))
                warehouseId = 0;
        }

        // teo_sarca [ 1564520 ] Inventory Move: can't select existing
        // attributes
        // Trifon - Always read Locator from Context. There are too many windows
        // to read explicitly one by one.
        locatorId = Env.getContextAsInt(Env.getCtx(), windowNo,
                I_M_MovementLine.COLUMNNAME_M_Locator_ID, true); // only window

        String title = "";
        // Get Text
        String sql =
                "SELECT p.Name, w.Name, w.M_Warehouse_ID "
                        + "FROM M_Product p, M_Warehouse w "
                        + "WHERE p.M_Product_ID=? AND w.M_Warehouse_ID"
                        + (locatorId <= 0 ? "=?"
                                : " IN (SELECT M_Warehouse_ID "
                                        + "FROM M_Locator "
                                        + "where M_Locator_ID=?)");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, productId);
            pstmt.setInt(2, locatorId <= 0 ? warehouseId : locatorId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                title = ": " + rs.getString(1) + " - " + rs.getString(2);
                warehouseId = rs.getInt(3); // fetch the actual warehouse -
                                            // teo_sarca [ 1564520 ]
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, sql, e);
        } finally {
            DB.close(rs, pstmt);
            rs = null;
            pstmt = null;
        }

        dialog.openAttributeSetInstanceSearchAndSelectDialog(title,
                productId, warehouseId, locatorId, bpartnerId);

        AttributeInstanceSearchDialog pai =
                dialog.getAttributeInstanceSearchDialog();
        if (!pai.wasCancelled() &&
                (attributeSetInstanceId != pai.getM_AttributeSetInstance_ID()
                        && pai.getM_AttributeSetInstance_ID() != -1)) {
            attributeSetInstanceId = pai.getM_AttributeSetInstance_ID();
            locatorId = pai.getM_Locator_ID();
            createAttributes();
        }

    }

    @Override
    public boolean isFieldLotStringEditable() {

        return instanceASI == null || instanceASI.getM_Lot_ID() == 0;

    }

    /**
     * Dyanmic Init.
     * 
     * @return true if initialized
     */
    public boolean createAttributes() {

        if (!initAttributes())
            return false;

        dialog.clearPanel();

        bSerNo = dialog.createButtonSerialNumber(
                Util.cleanAmp(Msg.getMsg(Env.getCtx(), "New")));
        bLot = dialog.createButtonLot(
                Util.cleanAmp(Msg.getMsg(Env.getCtx(), "New")));

        // Only add buttons and controls if readWrite
        if (isOKToAddButtonsAndControls()) {
            cbNewEdit = dialog.createNewEditCheckBox();
            if (isEnableNewRecord()) {
                cbNewEdit.setText(Msg.getMsg(Env.getCtx(), "NewRecord"));
            } else
                cbNewEdit.setText(Msg.getMsg(Env.getCtx(), "EditRecord"));

            if (isEnableSelectFromExisting()) {
                bSelect = dialog.createButtonSelect("PAttribute16.gif");
                bSelect.setText(Msg.getMsg(Env.getCtx(), "SelectExisting"));
            }
        }

        if (isShowInstanceAttributes()) {
            initInstanceAttributes(isEnableNewRecord());
        }

        // Product attributes can be shown in any window but are read/write
        // in the Product window only.
        MAttribute[] attributes = getProductAttributes();

        if (attributes.length > 0) {
            dialog.createAttributeGroupLable(getProductAttributeGroupLabel());
        }
        for (int i = 0; i < attributes.length; i++)
            addAttributeLine(attributes[i], true,
                    !isProductAttributesReadWrite());

        // New/Edit Window
        if (addNewEditButtonForProductAttributes()) {
            cbNewEdit.setSelected(isNewEditSelectedForProductAttributes());
            dialog.enableCancel(isNewEditSelectedForProductAttributes());
            actionNewEdit(isNewEditSelectedForProductAttributes());
        } else {
            dialog.enableCancel(false);
        }

        // Attribute Set Instance Description
        ILabel label =
                dialog.createLabel(Msg.translate(Env.getCtx(), "Description"));
        ITextField fieldDesc = dialog.createFieldDescription();
        label.setLabelFor(fieldDesc);
        fieldDesc.setText(getInstanceDescription());
        fieldDesc.setEditable(false);

        dialog.resize();

        return true;

    }

    /**
     * Add Attribute Line
     * 
     * @param attribute attribute
     * @param isProduct product level attribute
     * @param readOnly  value is read only
     */
    private void addAttributeLine(MAttribute attribute, boolean isProduct,
            boolean readOnly) {

        log.fine(attribute + ", Product=" + isProduct + ", R/O=" + readOnly);
        ILabel label = dialog.createLabel(attribute.getName());
        if (isProduct)
            label.setFontBold(true);
        if (attribute.getDescription() != null)
            label.setToolTipText(attribute.getDescription());

        // Set the values according to the instance, if it exists, or the
        // product ASI, if one exists.
        MAttributeInstance instance = getAttributeInstance(attribute);

        if (X_M_Attribute.ATTRIBUTEVALUETYPE_List
                .equals(attribute.getAttributeValueType())) {
            addListEditor(attribute, isProduct, readOnly, label, instance);
        } else if (X_M_Attribute.ATTRIBUTEVALUETYPE_Number
                .equals(attribute.getAttributeValueType())) {
            addNumberEditor(attribute, isProduct, readOnly, label, instance);
        } else // Text Field
        {
            addTextEditor(attribute, isProduct, readOnly, label, instance);
        }

    } // addAttributeLine

    void addTextEditor(MAttribute attribute, boolean isProduct,
            boolean readOnly, ILabel label, MAttributeInstance instance) {

        ITextField textEd = dialog.createStringEditor(attribute.getName(),
                attribute.isMandatory(),
                false, true, 20, INSTANCE_VALUE_LENGTH, null, null);
        textEd.setMandatory(attribute.isMandatory());
        if (instance != null)
            textEd.setValue(instance.getValue());
        label.setLabelFor(textEd);
        if (readOnly) {
            textEd.setReadWrite(false);
            textEd.setEditable(false);
        }
        editorList.add(new MEditor(textEd, isProduct));

    }

    void addNumberEditor(MAttribute attribute, boolean isProduct,
            boolean readOnly, ILabel label, MAttributeInstance instance) {

        CEditor numEd = dialog.createNumberEditor(
                attribute.getName(), attribute.isMandatory(),
                readOnly, !readOnly, DisplayType.Number,
                attribute.getName());
        numEd.setMandatory(attribute.isMandatory());
        if (instance != null)
            numEd.setValue(instance.getValueNumber());
        else
            numEd.setValue(Env.ZERO);
        label.setLabelFor(numEd);
        if (readOnly) {
            numEd.setReadWrite(false);
        }
        editorList.add(new MEditor(numEd, isProduct));

    }

    void addListEditor(MAttribute attribute, boolean isProduct,
            boolean readOnly, ILabel label, MAttributeInstance instance) {

        MAttributeValue[] values = attribute.getMAttributeValues();
        IComboBox cbEditor = dialog.createCComboBox(values);
        cbEditor.setMandatory(attribute.isMandatory());
        boolean found = false;
        final String attributeEquals = "Attribute=";
        if (instance != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null && values[i].getM_AttributeValue_ID()
                        == instance.getM_AttributeValue_ID()) {
                    cbEditor.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (found)
                log.fine(attributeEquals + attribute.getName() + " #"
                        + values.length + " - found: " + instance);
            else
                log.warning(attributeEquals + attribute.getName() + " #"
                        + values.length + " - NOT found: " + instance);
        } // setComboBox
        else
            log.fine(attributeEquals + attribute.getName() + " #"
                    + values.length + " no instance");
        label.setLabelFor(cbEditor);
        if (readOnly) {
            cbEditor.setReadWrite(false);
            cbEditor.setEditable(false);
        }

        editorList.add(new MEditor(cbEditor, isProduct));

    }

    public void actionNewEdit(boolean newEditSelected) {

        boolean rw = newEditSelected;
        setEditorsReadWrite(rw);

    }

    void setEditorsReadWrite(boolean rw) {

        if (isLot()) {
            dialog.getFieldLotString().setEditable(
                    rw && isFieldLotStringEditable());
            dialog.getFieldLot().setReadWrite(rw);
            bLot.setReadWrite(rw);
        }
        if (isSerNo()) {
            dialog.getFieldSerNo().setReadWrite(rw);
            bSerNo.setReadWrite(rw);
        }
        if (isGuaranteeDate())
            dialog.getFieldGuaranteeDate().setReadWrite(rw);

        for (int i = 0; i < editorList.size(); i++) {
            CEditor attributeEditor = editorList.get(i).editor;
            boolean productAttribute = editorList.get(i).isProductAttribute;
            if ((!productAttribute && !isProductWindow)
                    || (productAttribute && isProductWindow))
                attributeEditor.setReadWrite(rw);
            else
                attributeEditor.setReadWrite(false);
        }

        if (rw) {
            dialog.enableCancel(rw);
        }

    }

    /**
     * Save Selection
     * 
     * @return true if saved
     */
    public boolean saveSelection() {

        if (!readWrite)
            return true;

        log.fine("");
        MAttributeSet as = instanceASI.getMAttributeSet();
        if (as == null)
            return true;

        // Get the set of attribute values from the editors and check for
        // missing mandatory fields. The order of the attributes is set by
        // the order that editors are created.
        MAttribute[] attributes = as.getMAttributes();
        Object[] values = new Object[attributes.length];
        for (int i = 0; i < attributes.length; i++) {

            if (X_M_Attribute.ATTRIBUTEVALUETYPE_List
                    .equals(attributes[i].getAttributeValueType())) {
                IComboBox attrEditor = (IComboBox) editorList.get(i).editor;
                values[i] = attrEditor.getValue();
            } else if (X_M_Attribute.ATTRIBUTEVALUETYPE_Number
                    .equals(attributes[i].getAttributeValueType())) {
                CEditor numEditor = editorList.get(i).editor;
                values[i] = numEditor.getValue();
            } else {
                ITextField textEditor = (ITextField) editorList.get(i).editor;
                values[i] = textEditor.getText();
            }

        }

        try {
            instanceASI = getASIManager().withColumnId(columnId)
                    .withProductId(productId)
                    .withSOTrx(isSOTrx)
                    .withProductWindow(isProductWindow)
                    .findOrSave(instanceASI, productASI, values,
                            attributeSetInstanceId);
        } catch (AttributeSetInstanceMandatoryValueException e) {
            dialog.showError("FillMandatory", e.getMessage());
            return false;
        }

        attributeSetInstanceId = instanceASI.getM_AttributeSetInstance_ID();
        return true;

    }

    MAttributeSetInstanceManager getASIManager() {

        return new MAttributeSetInstanceManager();

    }

    @Override
    public void actionOK() {

        if (saveSelection())
           dispose();

    }

    @Override
    public int getAttributeSetInstanceId() {

        return attributeSetInstanceId;

    }

    @Override
    public String getM_AttributeSetInstanceName() {

        return instanceASI.getDescription();

    }

    @Override
    public boolean isChanged() {

        return attributeSetInstanceId != initialAttributeSetInstanceId;

    }

    @Override
    public void action(Object source) {

        if (source == bSelect) {
            actionSelect();
            dialog.reset();
        } else if (source == cbNewEdit)
            actionNewEdit(cbNewEdit.isSelected());
        else if (source == fieldLot)
            setLotStringFromLot();
        else if (source == fieldLotString)
            manuallySetLot();
        else if (source == bLot)
            createNewLot();
        else if (source == fieldSerNo)
            manuallyEnterSerialNumber();
        else if (source == bSerNo)
            createNewSerialNumber();
        else if (source == fieldGuaranteeDate)
            manuallySetGuaranteeDate();
        else
            log.log(Level.SEVERE, "not found - " + source);

    }

    void manuallySetGuaranteeDate() {

        if (fieldGuaranteeDate.isMandatory()) {
            fieldGuaranteeDate
                    .setBackground(fieldGuaranteeDate.getValue() == null);
        }

    }

    void createNewSerialNumber() {

        fieldSerNo.setText(getNewSerialNumber());
        manuallyEnterSerialNumber();

    }

    void manuallyEnterSerialNumber() {

        if (fieldSerNo.isMandatory()) {
            fieldSerNo.setBackground(fieldSerNo.getText() == null
                    || fieldSerNo.getText().isEmpty());
        }

    }

    void manuallySetLot() {

        if (fieldLotString.isMandatory()) {
            fieldLotString.setBackground(fieldLotString.getText() == null
                    || fieldLotString.getText().isEmpty());
        }

    }

    void setLotStringFromLot() {

        KeyNamePair pp = (KeyNamePair) fieldLot.getSelectedItem();
        if (pp != null && pp.getKey() != -1) {
            fieldLotString.setText(pp.getName());
            fieldLotString.setEditable(false);
            setLotId(pp.getKey());
        } else {
            fieldLotString.setEditable(true);
            setLotId(0);
        }
        manuallySetLot();

    }

}
