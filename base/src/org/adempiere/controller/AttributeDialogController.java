package org.adempiere.controller;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.swing.ComboBoxModel;

import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.util.KeyNamePair;

public interface AttributeDialogController extends Serializable {

    int getWindowNo();

    boolean initAttributes();

    boolean isInstanceAttributesReadWrite();

    boolean isProductAttributesReadWrite();

    boolean isShowInstanceAttributes();

    boolean init();

    boolean isOKToAddButtonsAndControls();

    boolean isEnableNewRecord();

    boolean isEnableSelectFromExisting();

    boolean isAttributeSetAnInstance();

    MAttribute[] getInstanceAttributes();

    boolean isLot();

    String getInstanceLot();

    boolean isLotMandatory();

    KeyNamePair[] getLotKeyNamePairs();

    int getInstanceLotId();

    boolean isEnableNewLotButton();

    boolean isSerNo();

    String getSerNo();

    boolean isSerNoMandatory();

    boolean isEnableNewSerNoButton();

    boolean isGuaranteeDate();

    boolean isGuaranteeDateMandatory();

    boolean isNewGuaranteeDate();

    Timestamp getNewGuaranteeDate();

    Timestamp getGuaranteeDate();

    MAttribute[] getProductAttributes();

    String getProductAttributeGroupLabel();

    boolean addNewEditButtonForProductAttributes();

    boolean isNewEditSelectedForProductAttributes();

    String getInstanceDescription();

    MAttributeInstance getAttributeInstance(MAttribute attribute);

    void dispose();

    String getDialogTitle();

    void setLotId(int key);

    void createNewLot();

    String getLot();

    String getNewSerialNumber();

    void actionCancel();

    void actionSelect();

    boolean isFieldLotStringEditable();

    void actionNewEdit(boolean selected);

    void actionOK();

    int getAttributeSetInstanceId();
    
    String getM_AttributeSetInstanceName();

    boolean isChanged();

    void action(Object source);

}
