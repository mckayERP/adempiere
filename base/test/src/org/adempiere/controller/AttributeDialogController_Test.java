package org.adempiere.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.adempiere.controller.ed.AttributeEditor;
import org.adempiere.controller.form.AttributeDialog;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.controller.form.AttributeDialogInfoImpl;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.GridTab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;



class AttributeDialogController_Test extends CommonUnitTestSetup {

    AttributeEditor editorMock;
    GridTab tabMock;
    AttributeDialog dialogMock;
    AttributeDialogInfo info;
    
    @BeforeEach
    void localSetup() {
         
        editorMock = mock(AttributeEditor.class);
        tabMock = mock(GridTab.class);
        dialogMock = mock (AttributeDialog.class);
        info = new AttributeDialogInfoImpl();
         
    }
    
    @Test
    final void test_constructor() {

        AttributeDialogController controller = new AttributeDialogControllerImpl(dialogMock, info);
        assertNotNull(controller);
    }

    @Test
    final void test_getWindowNo() {

        info.setWindowNo(2);
        AttributeDialogController controller = new AttributeDialogControllerImpl(dialogMock, info);
        assertEquals(2, controller.getWindowNo());
    }

    @Test
    final void whenInProductWindow_thenTitleIncludesProduct() {

        info.setWindowNo(2);
        AttributeDialogController controller = new AttributeDialogControllerImpl(dialogMock, info);
        assertEquals(2, controller.getWindowNo());
    }

}
