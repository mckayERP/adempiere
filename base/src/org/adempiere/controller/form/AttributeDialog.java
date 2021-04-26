package org.adempiere.controller.form;

import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;

import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.swing.IButton;
import org.compiere.swing.CButton;
import org.compiere.swing.ICheckBox;
import org.compiere.swing.CCheckBox;
import org.compiere.swing.IComboBox;
import org.compiere.swing.IDate;
import org.compiere.swing.CEditor;
import org.compiere.swing.ILabel;
import org.compiere.swing.CLabel;
import org.compiere.swing.ITextField;
import org.compiere.swing.CTextField;
import org.compiere.util.KeyNamePair;

public interface AttributeDialog {

    void openAttributeSetInstanceSearchAndSelectDialog(String title,
            int warehouseId, int locatorId, int bpartnerId, int productId);

    AttributeInstanceSearchDialog getAttributeInstanceSearchDialog();

    void clearPanel();

    ICheckBox createNewEditCheckBox();

    IButton createButtonSelect(String imagePath);

    IButton createButtonSerialNumber(String name);

    IButton createButtonLot(String name);

    ILabel createAttributeGroupLable(String label);

    void enableCancel(boolean enable);

    ITextField getFieldLotString();

    IComboBox getFieldLot();

    ITextField getFieldSerNo();

    CEditor getFieldGuaranteeDate();

    ILabel createLabel(String name);
    ILabel createGroupLabel(String name);

    ITextField getFieldDescription();

    ITextField createFieldDescription();

    void resize();

    IComboBox createCComboBox(MAttributeValue[] values);

    CEditor createNumberEditor(String name, boolean mandatory, boolean readOnly,
            boolean updateable, int number, String title);

    ITextField createStringEditor(String name, boolean mandatory,
            boolean readOnly, boolean updateable, int displayLength,
            int fieldLength, String vFormat, String obscureType);

    ITextField createFieldLotString();

    IComboBox createFieldLot(KeyNamePair[] lotKeyNamePairs);

    void createNewLotButton();

    ITextField createFieldSerNo(String serNo);

    void createNewSerNoButton();

    IDate createFieldGuaranteeDate(String name);

    void showError(String string, String mandatoryMsg);

    int getM_AttributeSetInstance_ID();

    String getM_AttributeSetInstanceName();

    void reset();

    void dispose();

}
