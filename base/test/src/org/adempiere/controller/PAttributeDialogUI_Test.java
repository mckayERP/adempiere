package org.adempiere.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import javax.swing.ImageIcon;

import org.adempiere.controller.AttributeDialogControllerImpl;
import org.adempiere.controller.form.AttributeDialog;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.form.AttributeDialogInfoImpl;
import org.adempiere.controller.form.AttributeInstanceSearchDialog;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MProduct;
import org.compiere.swing.IButton;
import org.compiere.swing.CButton;
import org.compiere.swing.CCheckBox;
import org.compiere.swing.IComboBox;
import org.compiere.swing.IDate;
import org.compiere.swing.CEditor;
import org.compiere.swing.ILabel;
import org.compiere.swing.ITextField;
import org.compiere.util.KeyNamePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class PAttributeDialogUI_Test extends CommonUnitTestSetup
    implements AttributeDialog {

    private String result;
    private AttributeDialogControllerImpl controller;
    
    private int windowNo = 2;
    private int attributeSetInstanceId = 3;
    private int productId = 4;
    private int columnId = 5;
    private boolean isReadWrite = true;
    private boolean isProductWindow = true;
    
    private AttributeDialogInfo info;
    
    @BeforeEach
    void resetResult() {
        result = "";
    }
    
    @BeforeEach
    void setupControllerWithNoProduct() {
        
    }
    
    private AttributeDialogInfo getAttributeDialogInfo() {

        return new AttributeDialogInfoImpl()
                .withWindowNo(2)
                .withAttributeSetInstanceId(attributeSetInstanceId)
                .withProductId(productId)
                .withProductWindow(isProductWindow)
                .withColumnId(columnId)
                .withReadWrite(isReadWrite);

    }
    
    @Test
    final void givenNoProductWhenInitializedThenReturnsFalse() {

        productId = 0;
        isProductWindow = false;        
        controller = spy(new AttributeDialogControllerImpl(this, 
                getAttributeDialogInfo()));
        assertFalse(controller.init());

    }

    @Test
    final void givenProductAndInstanceUseDifferentASThenReturnsFalse() {

        int productASIId = 1;
        int instanceASIId = 2;
        int productASId = 3;
        int instanceASId = 4;
        
        MAttributeSetInstance instanceASIMock = 
                mock(MAttributeSetInstance.class);
        doReturn(instanceASId).when(instanceASIMock).getM_AttributeSet_ID();
        
        MAttributeSetInstance productASIMock = 
                mock(MAttributeSetInstance.class);
        doReturn(productASId).when(productASIMock).getM_AttributeSet_ID();
        
        MProduct productMock = mock(MProduct.class);
        doReturn(productASIId).when(productMock).getM_AttributeSetInstance_ID();
        doReturn(productASIMock).when(productMock).getM_AttributeSetInstance();
        
        productId = 1;
        isProductWindow = false;
        attributeSetInstanceId = instanceASIId;
        controller = spy(new AttributeDialogControllerImpl(this, 
                getAttributeDialogInfo()));
        doReturn(productMock).when(controller).getProduct(productId);
        doReturn(instanceASIMock).when(controller).getInstanceASI();
        
        assertFalse(controller.init());

    }

    @Override
    public void openAttributeSetInstanceSearchAndSelectDialog(String title,
            int warehouseId, int locatorId, int bpartnerId, int productId) {

        // TODO Auto-generated method stub
        

    }

    @Override
    public AttributeInstanceSearchDialog getAttributeInstanceSearchDialog() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public void clearPanel() {

        // TODO Auto-generated method stub
        

    }

    @Override
    public CCheckBox createNewEditCheckBox() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public CButton createButtonSerialNumber(String name) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public CButton createButtonLot(String name) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public ILabel createAttributeGroupLable(String label) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public void enableCancel(boolean enable) {

        // TODO Auto-generated method stub
        

    }

    @Override
    public ITextField getFieldLotString() {

        // TODO Auto-generated method stub
        return null;

    }


    @Override
    public ITextField getFieldSerNo() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public CEditor getFieldGuaranteeDate() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public ILabel createLabel(String name) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public ILabel createGroupLabel(String name) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public ITextField getFieldDescription() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public ITextField createFieldDescription() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public void resize() {

        // TODO Auto-generated method stub
        

    }

    @Override
    public IComboBox createCComboBox(MAttributeValue[] values) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public CEditor createNumberEditor(String name, boolean mandatory,
            boolean readOnly, boolean updateable, int number, String title) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public ITextField createStringEditor(String name, boolean mandatory,
            boolean readOnly, boolean updateable, int displayLength,
            int fieldLength, String vFormat, String obscureType) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public ITextField createFieldLotString() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public IComboBox createFieldLot(KeyNamePair[] lotKeyNamePairs) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public void createNewLotButton() {

        // TODO Auto-generated method stub
        

    }

    @Override
    public ITextField createFieldSerNo(String serNo) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public void createNewSerNoButton() {

        // TODO Auto-generated method stub
        

    }


    @Override
    public void showError(String string, String mandatoryMsg) {

        // TODO Auto-generated method stub
        

    }

    @Override
    public int getM_AttributeSetInstance_ID() {

        // TODO Auto-generated method stub
        return 0;

    }

    @Override
    public String getM_AttributeSetInstanceName() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public IComboBox getFieldLot() {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public IDate createFieldGuaranteeDate(String name) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public void reset() {

        // TODO Auto-generated method stub
        

    }

    @Override
    public void dispose() {

        // TODO Auto-generated method stub
        

    }

    @Override
    public IButton createButtonSelect(String imagePath) {

        // TODO Auto-generated method stub
        return null;

    }

}
