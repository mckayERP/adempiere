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
package org.compiere.model;

import static org.adempiere.util.attributes.AttributeUtilities.findMatchingAttributeSetInstance;
import static org.adempiere.util.attributes.AttributeUtilities.hasValues;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_List;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_Number;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * Manages the creation of new Attribute Set Instances with the goal of limiting
 * duplication and reusing the same AttributeSetInstance id when the values are
 * all the same.
 * 
 * @author Michael McKay, mckayErp
 *
 */
public class MAttributeSetInstanceManager implements Serializable {

    private static final long serialVersionUID = -1429499635525627786L;

    /**
     * An exception thrown when mandatory elements of the AttributeSetInstance
     * are not set.
     * 
     * @author Michael McKay, mckayERP.com
     *
     */
    public class AttributeSetInstanceMandatoryValueException
            extends AdempiereException implements Serializable {

        private static final long serialVersionUID = -7608788552767776037L;

        public AttributeSetInstanceMandatoryValueException(String message) {

            super(message);

        }

    }

    protected CLogger log =
            CLogger.getCLogger(MAttributeSetInstanceManager.class);
    protected MAttribute[] attributes;
    protected transient Object[] values;
    protected MAttributeSet attributeSet;
    protected MAttributeSetInstance instanceASI;
    protected MAttributeSetInstance productASI;
    private int attributeSetInstanceId;
    private boolean isProductWindow = false;
    private int columnId = 0;
    private boolean isSOTrx = true;
    private int productId = 0;

    /**
     * Finds a matching AttributeSetInstance (ASI) in the system, or saves the
     * current data as a new AttributeSetInstance.
     * 
     * @param instanceASI   a holder for the instanceASI values
     * @param productASI    the associated product ASI or template
     * @param values        the set of attribute values to match
     * @param expectedASIId the current ASI expected by the calling UI dialog or
     *                      method
     * @return changed true if the found or saved ASI id is different then the
     *         expected.
     */
    public MAttributeSetInstance findOrSave(
            MAttributeSetInstance instance,
            MAttributeSetInstance product,
            Object[] values,
            int expectedASIId) {

        log.fine("");

        instanceASI = instance;
        productASI = product;
        this.attributeSetInstanceId = expectedASIId;
        this.values = values;

        attributeSet = instanceASI.getMAttributeSet();
        attributes = attributeSet.getMAttributes();

        preventSaveIfMandatoryValuesAreNotFilled(checkMandatoryValues());
        int matchingASI_ID = findMatchingAttributeSetInstanceId();
        boolean changed = keepExistingOrUseMatchingId(matchingASI_ID);
        changed = saveIfNewOrChanged(changed);
        changed = saveAttributes(changed);
        return saveModel(changed);

    }

    MAttributeSetInstance saveModel(boolean changed) {

        // Save Model
        if (changed) {
            // Reset the description which could change based on the attribute
            // values
            instanceASI.setDescription();
            instanceASI.saveEx();
        }
        return instanceASI;

    }

    boolean saveAttributes(boolean changed) {

        // Save attributes
        // m_readWrite is true
        if (attributeSetInstanceId > 0 && !hasValues(instanceASI, values)) {
            // Save all Attribute value instances
            for (int i = 0; i < attributes.length; i++) {
                if (ATTRIBUTEVALUETYPE_List
                        .equals(attributes[i].getAttributeValueType())) {
                    MAttributeValue value = (MAttributeValue) values[i];
                    attributes[i].setMAttributeInstance(attributeSetInstanceId,
                            value);
                } else if (ATTRIBUTEVALUETYPE_Number
                        .equals(attributes[i].getAttributeValueType())) {
                    BigDecimal value = (BigDecimal) values[i];
                    attributes[i].setMAttributeInstance(attributeSetInstanceId,
                            value);
                } else {
                    String value = (String) values[i];
                    attributes[i].setMAttributeInstance(attributeSetInstanceId,
                            value);
                }
            }
            changed = true; // may affect description
        }
        return changed;

    }

    boolean saveIfNewOrChanged(boolean changed) {

        instanceASI = changeInstanceOrgToMatchProduct(instanceASI, productId);

        // If new or if anything changed, save the instanceASI
        if (instanceASI.is_new() || instanceASI.is_Changed()) {
            changed = true;
            instanceASI.save();
        }
        attributeSetInstanceId = instanceASI.getM_AttributeSetInstance_ID();
        return changed;

    }

    boolean keepExistingOrUseMatchingId(int matchingASI_ID) {

        boolean isChanged = false;
        if (isExistingOrNewWithNoMatch(matchingASI_ID)) {
            isChanged = true;
        } else if (isNewWithMatch(matchingASI_ID)) {
            isChanged = useExisting(matchingASI_ID);
        } else if (isDuplicated(matchingASI_ID)) {
            isChanged = useOriginal(matchingASI_ID);
        } else {
            // instanceASI.getM_AttributeSetInstance_ID() > 0 && matchingASI_ID
            // > 0 && m_attributeSetInstance_id == matchingASI_ID
            // This should never happen as findMatchingAttributeSetInstance(asi,
            // values) filters out such a match.
            log.severe("M_AttributeSetInstance_ID matches itself!");
            isChanged = false;
        }
        return isChanged;

    }

    boolean useOriginal(int matchingASI_ID) {

        log.warning("Duplicate set of values for instances: "
                + matchingASI_ID + " " + attributeSetInstanceId);

        boolean isChanged = false;
        if (matchingASI_ID > instanceASI.getM_AttributeSetInstance_ID()) {
            instanceASI =
                    MAttributeSetInstance.get(Env.getCtx(), matchingASI_ID, 0);
            isChanged = true;
        }
        return isChanged;

    }

    boolean isDuplicated(int matchingASI_ID) {

        return instanceASI.getM_AttributeSetInstance_ID() > 0
                && matchingASI_ID > 0
                && instanceASI.getM_AttributeSetInstance_ID()
                        != matchingASI_ID;

    }

    boolean useExisting(int matchingASI_ID) {

        // The proposed instance is new and is a match for an existing
        // instance. Use the existing ID
        instanceASI =
                MAttributeSetInstance.get(Env.getCtx(), matchingASI_ID, 0);
        return true;

    }

    boolean isNewWithMatch(int matchingASI_ID) {

        return instanceASI.getM_AttributeSetInstance_ID() == 0
                && matchingASI_ID > 0;

    }

    boolean isChanged() {

        return attributeSetInstanceId
                != instanceASI.getM_AttributeSetInstance_ID();

    }

    boolean isExistingOrNewWithNoMatch(int matchingASI_ID) {

        return instanceASI.getM_AttributeSetInstance_ID() >= 0
                && matchingASI_ID == 0;

    }

    int findMatchingAttributeSetInstanceId() {

        // Limit the number of redundant attribute Set Instances
        // Need to do this here BEFORE the attribute instance values are saved
        // See if this set of instance values already exists.
        int matchingASI_ID = 0;
        // Where there is a product ASI and no instance attributes, ignore
        // matches and just use the product ASI.
        if (!isProductWindow && productASI != null
                && !attributeSet.isInstanceAttribute()) {
            matchingASI_ID = productASI.getM_AttributeSetInstance_ID();
            attributeSetInstanceId = 0;
            instanceASI.setM_AttributeSetInstance_ID(0);
        } else {
            instanceASI.setM_AttributeSetInstance_ID(0);
            matchingASI_ID = findMatchingAttributeSetInstance(Env.getCtx(),
                    instanceASI, values, null); // Will return 0 if nothing
                                                // found.
        }
        return matchingASI_ID;

    }

    void preventSaveIfMandatoryValuesAreNotFilled(String mandatory) {

        if (mandatory.length() > 0) {
            throw new AttributeSetInstanceMandatoryValueException(mandatory);
        }

    }

    StringBuilder checkMandatoryAttributeValues() {

        // Get the set of attribute values from the editors and check for
        // missing mandatory fields. The order of the attributes is set by
        // the order that editors are created.
        StringBuilder mandatoryMsg = new StringBuilder();
        for (int i = 0; i < attributes.length; i++) {
            log.fine(attributes[i].getName() + "=" + values[i]);
            if (attributes[i].isMandatory() && values[i] == null)
                mandatoryMsg.append(" - " + attributes[i].getName());
        }
        return mandatoryMsg;

    }

    MAttributeSetInstance changeInstanceOrgToMatchProduct(
            MAttributeSetInstance instanceASI,
            int productId) {

        // Change org to match product. The ASI will require the same org access
        // as the product.
        if (productId > 0) {
            MProduct product = MProduct.get(Env.getCtx(), productId);
            instanceASI.setAD_Org_ID(product.getAD_Org_ID());
        } else
            instanceASI.setAD_Org_ID(0);

        return instanceASI;

    }

    String checkMandatoryValues() {

        StringBuilder mandatoryMsg = new StringBuilder();
        if (!isProductWindow && !attributeSet.excludeEntry(
                MColumn.getTable_ID(Env.getCtx(), columnId, null), isSOTrx)) {
            mandatoryMsg.append(checkLotMandatory());
            mandatoryMsg.append(checkSerNoMandatory());
            mandatoryMsg.append(checkGuaranteeDateMandatory());
        } else if (isProductWindow) {
            // To correct errors, remove all instance values
            instanceASI.setLot(null);
            instanceASI.setSerNo(null);
            instanceASI.setGuaranteeDate(null);
        }

        mandatoryMsg.append(checkMandatoryAttributeValues());

        return mandatoryMsg.toString();

    }

    String checkGuaranteeDateMandatory() {

        String mandatoryMsg = "";
        if (attributeSet.isGuaranteeDate()) {
            Timestamp guaranteeDate = instanceASI.getGuaranteeDate();
            log.fine("GuaranteeDate=" + guaranteeDate);
            if (attributeSet.isGuaranteeDateMandatory()
                    && guaranteeDate == null)
                mandatoryMsg = " - "
                        + Msg.translate(Env.getCtx(), "GuaranteeDate");
        }
        return mandatoryMsg;

    }

    String checkSerNoMandatory() {

        String mandatoryMsg = "";
        if (attributeSet.isSerNo()
                && !attributeSet.isExcludeSerNo(columnId, isSOTrx)) {
            String serNoText = instanceASI.getSerNo();
            log.fine("SerNo=" + serNoText);
            if (attributeSet.isSerNoMandatory()
                    && (serNoText == null || serNoText.length() == 0))
                mandatoryMsg = " - " + Msg.translate(Env.getCtx(), "SerNo");
        }
        return mandatoryMsg;

    }

    String checkLotMandatory() {

        String mandatoryMsg = "";
        if (attributeSet.isLot()
                && !attributeSet.isExcludeLot(columnId, isSOTrx)) {
            String lotText = instanceASI.getLot();
            log.fine("Lot=" + lotText);
            if (attributeSet.isLotMandatory() &&
                    (lotText == null || lotText.length() == 0))
                mandatoryMsg = " - " + Msg.translate(Env.getCtx(), "Lot");
        }
        return mandatoryMsg;

    }

    public int getProductId() {

        return productId;

    }

    public MAttributeSetInstanceManager withProductId(int productId) {

        this.productId = productId;
        return this;

    }

    public boolean isProductWindow() {

        return isProductWindow;

    }

    public MAttributeSetInstanceManager
            withProductWindow(boolean isProductWindow) {

        this.isProductWindow = isProductWindow;
        return this;

    }

    public int getColumnId() {

        return columnId;

    }

    public MAttributeSetInstanceManager withColumnId(int columnId) {

        this.columnId = columnId;
        return this;

    }

    public boolean isSOTrx() {

        return isSOTrx;

    }

    public MAttributeSetInstanceManager withSOTrx(boolean isSOTrx) {

        this.isSOTrx = isSOTrx;
        return this;

    }

}
