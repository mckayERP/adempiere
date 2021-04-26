package org.adempiere.controller.ed;

import static org.adempiere.controller.ed.AttributeEditorControllerImpl.NO_INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.Lookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeEditorController_Test extends CommonUnitTestSetup {

    AttributeEditorControllerImpl controller;
    AttributeEditor editor;

    @BeforeEach
    void localSetup() {

        Lookup lookupMock = mock(Lookup.class);
        doReturn("theExpectedText").when(lookupMock).getDisplay(any());

        editor = mock(AttributeEditor.class);
        controller = spy(new AttributeEditorControllerImpl());
        controller.setEditor(editor);
        doReturn(8418).when(controller).getASIColumnIdInProductWindow();
        doReturn(lookupMock).when(controller).getLookup();

    }

    @Test
    void whenValueIsNull_displayIsEmptyString() {

        controller.setValue(null);
        assertEquals("", controller.getText());
        assertEquals("", controller.getToolTipText());
        assertEquals("", controller.getAttributeWhere());
        assertEquals("", controller.getLastDisplay());

    }

    @Test
    void whenValueIsNoInstance_displayIsEmptyString() {

        controller.setValue(NO_INSTANCE);
        assertEquals("", controller.getText());
        assertEquals("", controller.getToolTipText());
        assertEquals("", controller.getAttributeWhere());
        assertEquals("", controller.getLastDisplay());

    }

    @Test
    void whenValueIsInteger_attributeWhereIsQuery() {

        controller.setValue(Integer.valueOf(1));

        String sql = controller.getAttributeWhere();
        assertEquals("EXISTS (SELECT * FROM M_Storage s "
                + "WHERE s.M_AttributeSetInstance_ID=1"
                + " AND s.M_Product_ID=p.M_Product_ID)", sql);

    }

    @Test
    void whenValueIsInteger_testIsSetFromLookup() {

        Lookup lookupMock = mock(Lookup.class);
        doReturn("theExpectedText").when(lookupMock)
                .getDisplay(Integer.valueOf(1));

        doReturn(lookupMock).when(controller).getLookup();

        controller.setValue(Integer.valueOf(1));

        String displayText = controller.getText();
        assertEquals("theExpectedText", displayText);

    }

    @Test
    void getAttributeDialogInfoHasValuesSet() {

        controller.setWindowNo(1);

        AttributeDialogInfo info = controller.getAttributeDialogInfo();

        assertEquals(1, info.getWindowNo());

    }

}
