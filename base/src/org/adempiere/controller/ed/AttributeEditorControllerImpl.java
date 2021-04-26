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
package org.adempiere.controller.ed;

import static org.adempiere.util.attributes.AttributeUtilities.isAttributeSetInstanceMandatory;
import static org.adempiere.util.attributes.AttributeUtilities.isValidAttributeSetInstance;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.form.AttributeDialogInfoImpl;
import org.adempiere.exceptions.DBException;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_Product;
import org.compiere.model.Lookup;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MColumn;
import org.compiere.model.MProduct;
import org.compiere.model.MQuery;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;

public class AttributeEditorControllerImpl extends AbstractEditorController
        implements AttributeEditorController {

    static final String FOUND_DUPLICATES_MESSAGE =
            "AttributeEditorController_FoundDuplicates";
    // "Found duplicate matches. Please select the correct value using the
    // dialog"
    
    private static final String M_ATTRIBUTE_SET_INSTANCE_ID =
            "M_AttributeSetInstance_ID";


    private static final long serialVersionUID = -1612874875805220156L;

    public static final Integer NO_INSTANCE = Integer.valueOf(0);
    private static final Integer NOT_FOUND = null;
    private static final Integer MULTIPLE_RESULTS = -2;

    private String text;
    private String oldText;
    private String toolTipText;
    private String attributeWhere;
    private String oldWhere;
    private int tableId;
    private boolean isReadWrite;
    private boolean isProductWindow;
    private boolean isSearchOnly = false;
    private int productId;

    private CLogger log = CLogger.getCLogger(AttributeEditorControllerImpl.class);
    private int attributeSetId;

    private Boolean isEnabled = null;

    public AttributeEditorControllerImpl() {

        super();

    }

    public AttributeEditorControllerImpl(AttributeEditor editor, GridTab gridTab,
            boolean mandatory, boolean isReadOnly, boolean isUpdateable,
            int windowNo, Lookup lookup) {

        super(editor, gridTab, mandatory, isReadOnly, isUpdateable, windowNo,
                lookup);
        isReadWrite = (!readOnly && isUpdateable);

    }

    @Override
    public Object getValue() {

        Integer temp = null;
        Object value = super.getValue();
        if (!isValueNullOrNoInstance()) {
            try {
                temp = (Integer) value;
            } catch (ClassCastException cce) {
                temp = null;
            }
        }
        return temp;

    }

    public void setValue(Object newValue) {

        super.setValue(newValue);

        if (isValueNullOrNoInstance()) {
            text = "";
            toolTipText = "";
            attributeWhere = "";
        } else {
            text = getLookup().getDisplay(getValue());
            toolTipText = getLookup().getDisplay(getValue());
            attributeWhere = "EXISTS (SELECT * FROM M_Storage s "
                    + "WHERE s.M_AttributeSetInstance_ID="
                    + ((Integer) getValue()).intValue()
                    + " AND s.M_Product_ID=p.M_Product_ID)";
        }

        setValueInEditor();
        setEnabled();
        enableControls();

    }

    private void setValueInEditor() {

        ((AttributeEditor) editor).setText(text);
        this.lastDisplay = text;

    }

    public String getText() {

        return text;

    }

    public String getToolTipText() {

        return toolTipText;

    }

    public String getAttributeWhere() {

        return attributeWhere;

    }

    @Override
    public void actionText() {

        String editorText = editor.getDisplay();

        if (hasNotChanged(editorText)) {
            ((AttributeEditor) editor).setText(lastDisplay);
            return;            
        }

        if (isBlankOrWildCard(editorText)) {
            actionButton();
            return;
        }

        if (ignoreChanges()) {
            log.info(
                    "No action: M_Product_ID == 0 || M_AttributeSet_ID == 0 || exclude");
            ((AttributeEditor) editor).setText(lastDisplay);
            return;
        }

        getLogger().fine(columnName + " - " + "\"" + editorText + "\"");

        Integer asiIDToFind = findMatchingOrCreateNewSerialNo(editorText);

        if (asiIDToFind > 0) {
            save(asiIDToFind);
            return;
        }

        if (MULTIPLE_RESULTS.equals(asiIDToFind)) {
            ((AttributeEditor) editor).warn(
                    FOUND_DUPLICATES_MESSAGE);
        }

        // Didn't understand the text input, couldn't find a match or found
        // duplicates. Launch the search dialog.
        log.fine(Msg.parseTranslation(Env.getCtx(),
                "\"" + editorText + "\"" + " @NotFound@"));

        actionButton();
        ((AttributeEditor) editor).requestFocus();

    }

    private void save(Object value) {

        boolean changed = hasChanged();
        ((AttributeEditor) editor).setAndBindValue(value);

        if (changed && this.gridTab != null && this.gridField != null) {
            // force Change - user does not realize that embedded object is
            // already saved.
            this.gridTab.processFieldChange(super.gridField);
        }

    }

    private Integer findMatchingOrCreateNewSerialNo(String editorText) {
        //@formatter:off
        
        return findValidAttributeSetInstance(editorText)
                .orElse(findMatchingLotCode(editorText)
                .orElse(findMatchingSerialNumber(editorText)
                .orElse(findMatchingGuaranteeDate(editorText)
                .orElse(createNewSerialNumberFromText(editorText)))));
        
        //@formatter:on
    }

    private Optional<Integer> findValidAttributeSetInstance(String editorText) {

        Integer id = convertTextToNumber(editorText);
        Integer idFound = findASIFromId(id);
        return validateASI(idFound);

    }

    private Integer convertTextToNumber(String editorText) {

        Integer asiIDToFind = -1;
        try {
            asiIDToFind = Integer.parseInt(editorText);
        } catch (NumberFormatException e) {
            asiIDToFind = -1;
        }
        return asiIDToFind;

    }

    private Integer findASIFromId(Integer asiIDToFind) {

        try {
            String where =
                    I_M_AttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID
                            + "= ?";
            asiIDToFind = new Query(Env.getCtx(),
                    I_M_AttributeSetInstance.Table_Name, where, null)
                            .setClient_ID()
                            .setOnlyActiveRecords(true)
                            .setParameters(asiIDToFind)
                            .firstIdOnly(); // returns ID or -1 if not found.
        } catch (DBException e) {
            asiIDToFind = MULTIPLE_RESULTS;
        }
        return asiIDToFind;

    }

    private Optional<Integer> validateASI(Integer attributeSetInstanceId) {

        if (attributeSetInstanceId > 0
                && !isValidAttributeSetInstance(
                        Env.getCtx(), getProduct(),
                        Env.isSOTrx(Env.getCtx(), this.windowNo),
                        columnId, attributeSetInstanceId, null)) {
            attributeSetInstanceId = NOT_FOUND;
        }
        return Optional.ofNullable(attributeSetInstanceId);

    }

    private Optional<Integer> findMatchingGuaranteeDate(String editorText) {

        // Try to match the Guarantee Date - Date has to be entered in the
        // system date format pattern
        Integer idFound = null;
        Timestamp ts = null;
        SimpleDateFormat dateFormat = DisplayType.getDateFormat();
        try {
            java.util.Date date = dateFormat.parse(editorText);
            ts = new Timestamp(date.getTime());
        } catch (ParseException pe) {
            log.fine("Entered text not in date format "
                    + dateFormat.getDateFormatSymbols().toString());
            log.fine(pe.getMessage());
            ts = null;
        }

        if (ts != null) {
            try {
                String where = I_M_AttributeSetInstance.COLUMNNAME_GuaranteeDate
                        + "= ?";
                idFound = new Query(Env.getCtx(),
                        I_M_AttributeSetInstance.Table_Name, where, null)
                                .setClient_ID()
                                .setOnlyActiveRecords(true)
                                .setParameters(ts)
                                .firstIdOnly(); // returns ID or -1 if not
                                                // found.
                // Check integrity of the result
                if (idFound > 0) {
                    if (!isValidAttributeSetInstance(Env.getCtx(), getProduct(),
                            Env.isSOTrx(Env.getCtx(), this.windowNo),
                            this.columnId, idFound, null)) {
                        idFound = NOT_FOUND;
                    } else
                        log.fine(
                                "Valid Gurantee Date found.  M_AttributeSetInstance_ID = "
                                        + idFound);
                }
            } catch (DBException e) {
                idFound = MULTIPLE_RESULTS;
            }
        }
        return Optional.ofNullable(idFound);

    }

    private Integer createNewSerialNumberFromText(String editorText) {

        Integer newId = NOT_FOUND;

        MProduct product = getProduct();
        if (isSerialNumberRequired(product)) {

            MAttributeSetInstance instanceASI =
                    createNewInstanceASI(product, editorText);
            copyProductTemplateIntoInstance(product, instanceASI);
            instanceASI.setDescription();
            instanceASI.saveEx();
            newId = instanceASI.getM_AttributeSetInstance_ID();
        }
        return newId;

    }

    private MAttributeSetInstance createNewInstanceASI(MProduct product,
            String serialNumberText) {

        MAttributeSetInstance instanceASI = new MAttributeSetInstance(
                Env.getCtx(), 0, product.getM_AttributeSet_ID(), null);

        instanceASI.setSerNo(serialNumberText);
        instanceASI.saveEx();
        return instanceASI;

    }

    private void copyProductTemplateIntoInstance(MProduct product,
            MAttributeSetInstance target) {

        MAttributeInstance.copy(Env.getCtx(),
                target.getM_AttributeSetInstance_ID(),
                product.getM_AttributeSetInstance_ID(), null);

    }

    private boolean isSerialNumberRequired(MProduct product) {

        MAttributeSet mas = getAttributeSet();
        return mas.getM_AttributeSet_ID() != 0
                && mas.isInstanceAttribute()
                && mas.isSerNo()
                && mas.isSerNoMandatory()
                && isAttributeSetInstanceMandatory(Env.getCtx(), product,
                        tableId, Env.isSOTrx(Env.getCtx(), this.windowNo),
                        null)
                && !isProductWindow;

    }

    private MAttributeSet getAttributeSet() {

        return MAttributeSet.get(Env.getCtx(), attributeSetId);

    }

    private Optional<Integer> findMatchingSerialNumber(String editorText) {

        Integer asiIDToFind = NOT_FOUND;
        try {
            String where = I_M_AttributeSetInstance.COLUMNNAME_SerNo + "= ?";
            asiIDToFind = new Query(Env.getCtx(),
                    I_M_AttributeSetInstance.Table_Name, where, null)
                            .setClient_ID()
                            .setOnlyActiveRecords(true)
                            .setParameters(editorText)
                            .firstIdOnly(); // returns ID or -1 if not found.
            // Check integrity of the result
            if (asiIDToFind > 0) {
                if (!isValidAttributeSetInstance(Env.getCtx(), getProduct(),
                        Env.isSOTrx(Env.getCtx(), this.windowNo), this.columnId,
                        asiIDToFind, null)) {
                    asiIDToFind = NOT_FOUND;
                } else
                    log.fine(
                            "Valid serial number found.  M_AttributeSetInstance_ID = "
                                    + asiIDToFind);
            }
        } catch (DBException e) {
            asiIDToFind = MULTIPLE_RESULTS; // multiple results
        }
        return Optional.ofNullable(asiIDToFind);

    }

    private Optional<Integer> findMatchingLotCode(String editorText) {

        Integer asiIDToFind = NOT_FOUND;
        try {
            String where = I_M_AttributeSetInstance.COLUMNNAME_Lot + "= ?";
            asiIDToFind = new Query(Env.getCtx(),
                    I_M_AttributeSetInstance.Table_Name, where, null)
                            .setClient_ID()
                            .setOnlyActiveRecords(true)
                            .setParameters(editorText)
                            .firstIdOnly(); // returns ID or -1 if not found.
            // Check integrity of the result
            if (asiIDToFind > 0) {
                if (!isValidAttributeSetInstance(Env.getCtx(), getProduct(),
                        Env.isSOTrx(Env.getCtx(), this.windowNo), this.columnId,
                        asiIDToFind, null)) {
                    asiIDToFind = -1;
                } else
                    log.fine(
                            "Valid lot number found.  M_AttributeSetInstance_ID = "
                                    + asiIDToFind);
            }
        } catch (DBException e) {
            asiIDToFind = MULTIPLE_RESULTS;
        }
        return Optional.ofNullable(asiIDToFind);

    }

    private boolean ignoreChanges() {

        boolean isExcluded = isProductAttributeSetExcludedForThisColumn();
        return isSearchOnly || !isProductWindow &&
                (productId == 0 || attributeSetId == 0 || isExcluded);

    }

    private boolean isBlankOrWildCard(String editorText) {

        getLogger().fine(
                "Text null or uses wild cards." + "\"" + editorText + "\"");
        return editorText == null || editorText.length() == 0
                || editorText.equals("%");

    }

    private boolean hasNotChanged(String editorText) {

        // Nothing entered, just pressing enter again => ignore - teo_sarca BF [
        // 1834399 ]
        getLogger().fine("Nothing new entered [SKIP]");
        return editorText != null && editorText.length() >= 0
                && editorText.equalsIgnoreCase(lastDisplay);

    }

    private CLogger getLogger() {

        return log;

    }

    @Override
    public void actionButton() {

        if (!isEnabled())
            return;

        ((AttributeEditor) editor).enableButton(false);

        int currentValueInt = getValueAsInt();
        int attributeSetInstanceId = currentValueInt;

        setM_Product_ID();

        // Exclude ability to enter ASI
        boolean changed = false;
        boolean exclude = isProductAttributeSetExcludedForThisColumn();

        if (isSearchOnly) {
            search();
            return;
        } else if (!isProductWindow
                && (productId == 0 || attributeSetId == 0)) {
            log.info("No action: M_Product_ID == 0 || M_AttributeSet_ID == 0");
            attributeSetInstanceId = 0;
            changed = attributeSetInstanceId != currentValueInt;
        } else if (!isProductWindow
                && (attributeSetInstanceId == 0 && exclude)) {
            log.info("AttributeSetInstance is excluded in this window.");
        } else {
            ((AttributeEditor) editor)
                    .openAttributeDialog(getAttributeDialogInfo());
            boolean isChanged =
                    ((AttributeEditor) editor).isAttributeDialogChanged();
            int asiFromDialog = ((AttributeEditor) editor)
                    .getAttributeSetInstanceFromDialog();
            if (isChanged || asiFromDialog != currentValueInt) {
                attributeSetInstanceId = asiFromDialog;
                changed = true;
            }
        }

        // Set Value
        if (changed) {
            log.finest("Changed M_AttributeSetInstance_ID="
                    + attributeSetInstanceId);
            ((AttributeEditor) editor).setAndBindValue(attributeSetInstanceId);
        }

        setValue(getValue()); // Reset the display in case text was entered.

        if (attributeSetInstanceId == currentValueInt && gridTab != null
                && gridField != null) {
            // force Change - user does not realize that embedded object is
            // already saved.
            // This will fire the callouts on the field if any.
            gridTab.processFieldChange(gridField);
        }
        ((AttributeEditor) editor).enableButton(true);

    }

    void search() {

        ((AttributeEditor) editor).actionSearch();
        ((AttributeEditor) editor).enableButton(true);

    }

    public boolean isProductAttributeSetExcludedForThisColumn() {

        setM_Product_ID();
        boolean exclude = false;
        if (productId != 0 && attributeSetId != 0) {
            MAttributeSet mas = MAttributeSet.get(Env.getCtx(),
                    attributeSetId);
            exclude = mas.excludeEntry(tableId,
                    Env.isSOTrx(Env.getCtx(), this.windowNo));
        }
        return exclude;

    }

    public int getValueAsInt() {

        Integer currentValue = (Integer) getValue();
        return currentValue == null ? 0 : currentValue.intValue();

    }

    @Override
    public void enableControls() {

        if (editor == null)
            return;

        boolean enabled = isEnabled();
        ((AttributeEditor) editor).enableButton(enabled);
        ((AttributeEditor) editor).enableTextField(enabled);

    }

    @Override
    public void setError() {

        if (this.gridField != null) {
            gridField.setError(isFieldInError());
        }

    }

    boolean isFieldInError() {

        boolean error = false;
        if (this.gridField != null) {
            columnId = this.gridField.getAD_Column_ID();
            MProduct product = getProduct();
            if (product != null) {
                // Set column error if the ASI is mandatory
                Properties ctx = Env.getCtx();
                Boolean isSOTrx = Env.isSOTrx(ctx, this.windowNo);
                Integer attributeSetInstanceId = null;
                if (getValue() != null) {
                    attributeSetInstanceId = (Integer) getValue();
                }
                error = !isValidAttributeSetInstance(ctx, product, isSOTrx,
                        columnId, attributeSetInstanceId, null);
            }
        }
        return error;

    }

    private boolean setEnabled() {

        setM_Product_ID();
        boolean hasAttributeSet = attributeSetId > 0;
        boolean isExcluded = isProductAttributeSetExcludedForThisColumn();
        boolean canBeEnabled = !(isValueNullOrNoInstance() && isExcluded);
        isEnabled = Boolean.valueOf(
                hasAttributeSet && isReadWrite &&
                        (isProductWindow || isSearchOnly || canBeEnabled));
        return isEnabled;

    }

    public boolean isEnabled() {

        return Optional.ofNullable(isEnabled)
                .orElse(setEnabled());

    }

    private boolean isValueNullOrNoInstance() {

        return !(super.getValue() != null
                && !NO_INSTANCE.equals(super.getValue()));

    }

    public MProduct getProduct() {

        return MProduct.get(Env.getCtx(), productId);

    }

    /**
     * Set the M_Product_ID value from the context. If there is a
     * M_ProductBOM_ID defined, that ID will be used.
     */
    private void setM_Product_ID() {

        productId = 0;
        int productBOMId;
        if (gridTab != null) {
            productId = Env.getContextAsInt(Env.getCtx(), windowNo,
                    gridTab.getTabNo(), "M_Product_ID");
            productBOMId = Env.getContextAsInt(Env.getCtx(), windowNo,
                    gridTab.getTabNo(), "M_ProductBOM_ID");
        } else {
            productId =
                    Env.getContextAsInt(Env.getCtx(), windowNo, "M_Product_ID");
            productBOMId = Env.getContextAsInt(Env.getCtx(), windowNo,
                    "M_ProductBOM_ID");
        }
        if (productBOMId != 0) // Use BOM Component
            productId = productBOMId;

        attributeSetId = Optional.ofNullable(getProduct())
                .map(I_M_Product::getM_AttributeSet_ID)
                .orElse(0);

    }

    public void setGridField(GridField field) {

        super.setGridField(field);

        if (gridField != null) {
            tableId = MColumn.getTable_ID(Env.getCtx(), columnId, null);
            setProductWindow(columnId == getASIColumnIdInProductWindow());
        } else {
            columnName = "M_AttributeSetInstance_ID";
            columnId = 0;
            tableId = 0;
            setProductWindow(false);
        }

        enableControls();

    }

    public int getASIColumnIdInProductWindow() {

        // M_Product.M_AttributeSetInstance_ID = 8418
        return MColumn.getColumn_ID(I_M_Product.Table_Name,
                I_M_Product.COLUMNNAME_M_AttributeSetInstance_ID);

    }

    @Override
    public boolean isProductWindow() {

        return isProductWindow;

    }

    public void setProductWindow(boolean isProductWindow) {

        this.isProductWindow = isProductWindow;

    }

    public void setMandatory(boolean mandatory) {

        super.setMandatory(mandatory);
        ((AttributeEditor) editor).setBackground(false);

    }

    public void setReadOnly(boolean ro) {

        super.setReadOnly(ro);
        isReadWrite = !ro;
        enableControls();

    }

    @Override
    public void actionZoom() {

        MQuery zoomQuery = new MQuery();
        Object zoomTargetValue = getValue();
        if (zoomTargetValue == null)
            zoomTargetValue = Integer.valueOf(0);
        String keyTableName = I_M_AttributeSetInstance.Table_Name;
        String keyColumnName =
                I_M_AttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID;

        zoomQuery.addRestriction(keyColumnName, MQuery.EQUAL, zoomTargetValue);
        zoomQuery.setZoomColumnName(keyColumnName);
        zoomQuery.setZoomTableName(keyTableName);
        zoomQuery.setZoomValue(zoomTargetValue);
        zoomQuery.setRecordCount(1); // guess

        int windowId = lookup.getZoom(zoomQuery);
        int windowNo = lookup.getWindowNo();

        log.info(columnName + " - AD_Window_ID=" + windowId
                + " - Query=" + zoomQuery + " - Value=" + zoomTargetValue);

        ((AttributeEditor) editor).zoomToWindow(zoomQuery, windowId, windowNo);

    }

    @Override
    public Object get_oldValue() {

        return oldValue;

    }

    @Override
    public void set_oldValue() {

        if (getValue() != null) {
            try {
                this.oldValue = getValue();
            } catch (ClassCastException e) {
                this.oldValue = null;
            }
        } else
            this.oldValue = null;

        oldText = text;
        oldWhere = attributeWhere;

    }

    @Override
    public boolean hasChanged() {

        // A test of Value is not needed as this method should
        // only be called by the search window and the value
        // is not set by the search window

        if (text != null)
            return !text.equals(oldText);
        else if (oldText != null)
            return true;

        if (attributeWhere != null)
            return !attributeWhere.equals(oldWhere);
        else if (oldWhere != null)
            return true;

        return false;

    }

    @Override
    public void setAttributeWhere(String whereClause) {

        this.attributeWhere = whereClause;

    }

    @Override
    public void setText(String displayText) {

        text = displayText;

    }

    @Override
    public void setSearchOnly(boolean searchOnly) {

        isSearchOnly = searchOnly;

    }

    @Override
    public AttributeDialogInfo getAttributeDialogInfo() {

        return new AttributeDialogInfoImpl()
                .withWindowNo(windowNo)
                .withAttributeSetInstanceId(getValueAsInt())
                .withProductId(productId)
                .withProductWindow(isProductWindow)
                .withColumnId(columnId)
                .withReadWrite(isReadWrite)
                .withTitle(getDialogTitle());

    }

    public String getDialogTitle() {

        String title = "";

        if (isProductWindow) {
            title = Msg.translate(Env.getCtx(), "M_Product_ID") + " "
                    + Msg.translate(Env.getCtx(), M_ATTRIBUTE_SET_INSTANCE_ID)
                    + ": #" + getValueAsInt();
        } else {
            title = Msg.translate(Env.getCtx(), M_ATTRIBUTE_SET_INSTANCE_ID)
                    + ": #" + getValueAsInt();
        }

        return title;

    }
}
