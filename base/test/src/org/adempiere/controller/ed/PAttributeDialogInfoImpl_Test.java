package org.adempiere.controller.ed;

import static org.junit.jupiter.api.Assertions.*;

import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.form.AttributeDialogInfoImpl;
import org.adempiere.test.CommonUnitTestSetup;
import org.junit.jupiter.api.Test;


class PAttributeDialogInfoImpl_Test extends CommonUnitTestSetup {

    @Test
    final void test() {

        AttributeDialogInfo info = new AttributeDialogInfoImpl();
        assertNotNull(info);
    }

    @Test
    final void testToString() {

        String expected = "M_AttributeSetInstance_ID=" + 0
                + ", M_Product_ID=" + 0
                + ", C_BPartner_ID=" + 0
                + ", Column=" + 0
                + ", windowNo=" + 0
                + ", title=Title not set";
        
        AttributeDialogInfo info = new AttributeDialogInfoImpl();
        assertEquals(expected, info.toString());
    }

    @Test
    final void test_windowNo() {

        int windowNo = 1;
        
        AttributeDialogInfo info = new AttributeDialogInfoImpl()
                .withWindowNo(windowNo);
        
        assertEquals(windowNo, info.getWindowNo());
    }

    @Test
    final void test_productId() {

        int productId = 1;
        
        AttributeDialogInfo info = new AttributeDialogInfoImpl()
                .withProductId(productId);
        
        assertEquals(productId, info.getProductId());
    }

    @Test
    final void test_isProductWindow() {

        boolean isProductWindow = true;
        
        AttributeDialogInfo info = new AttributeDialogInfoImpl()
                .withProductWindow(isProductWindow);
        
        assertEquals(isProductWindow, info.isProductWindow());
    }

    @Test
    final void test_attributeSetInstanceId() {

        int attributeSetInstanceId = 1;
        
        AttributeDialogInfo info = new AttributeDialogInfoImpl()
                .withAttributeSetInstanceId(attributeSetInstanceId);
        
        assertEquals(attributeSetInstanceId, info.getAttributeSetInstanceId());
    }

    @Test
    final void test_columnId() {

        int columnId = 1;
        
        AttributeDialogInfo info = new AttributeDialogInfoImpl()
                .withColumnId(columnId);
        
        assertEquals(columnId, info.getColumnId());
    }

}
