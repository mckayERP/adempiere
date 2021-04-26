package org.compiere.grid.ed;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.awt.Frame;

import javax.swing.JFrame;

import org.adempiere.controller.ed.AttributeEditor;
import org.adempiere.controller.form.AttributeDialogInfo;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.GridTab;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


class VPAttributeDialog_Test extends CommonUnitTestSetup {

    private JFrame testFrame;
    GridTab tabMock;
    AttributeEditor editorMock;
    VPAttributeDialog dialog;
    Frame frameMock;
    private AttributeDialogInfo info;
    
    @AfterEach
    void localTeardown() {
        if (testFrame != null) {
            testFrame.dispose(  );
            testFrame = null;
        }
    }
    @BeforeEach
    void localSetup() {
        
        tabMock = mock(GridTab.class);
        editorMock = mock(AttributeEditor.class);
        frameMock = mock(Frame.class);
        info = mock(AttributeDialogInfo.class);
        
        testFrame = new JFrame("Test");
        
        dialog = new VPAttributeDialog(testFrame, info);
        
    }
    
    @Test
    final void test() {

        assertNotNull(dialog.getController());

    }

}
