package org.adempiere.webui.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.adempiere.controller.ed.AttributeEditorControllerImpl;
import org.adempiere.controller.ed.EditorController;
import org.adempiere.controller.ed.NullAttributeEditorControllerImpl;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.GridField;
import org.compiere.model.MPAttributeLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;


public class WPAttributeEditor_Test extends CommonUnitTestSetup {

    GridField gridfieldMock;
    AttributeEditorControllerImpl attributeEditorController;
    WPAttributeEditor attributeEditor;
    MPAttributeLookup attributeLookupMock;

    @BeforeEach
    void localSetup() {

        attributeLookupMock = mock(MPAttributeLookup.class);
        gridfieldMock = mock(GridField.class);
        doReturn(attributeLookupMock).when(gridfieldMock).getLookup();
        attributeEditor = mock(WPAttributeEditor.class);
        attributeEditorController = spy(new AttributeEditorControllerImpl());
        attributeEditorController.setEditor(attributeEditor);
        doReturn(8418).when(attributeEditorController).getASIColumnIdInProductWindow();
        doReturn(attributeLookupMock).when(attributeEditorController).getLookup();
        doReturn("TestDisplay").when(attributeLookupMock).getDisplay(any());

    }

    @Nested
    class GivenNoController {

        @Test
        final void controllerDefaults() {

            doCallRealMethod().when(attributeEditor).getController();
            assertTrue(attributeEditor.getController() instanceof NullAttributeEditorControllerImpl);
            
        }

    }
    
    @Nested
    class GivenAControllerIsSet {
        
        @BeforeEach
        void setController() {

            doCallRealMethod().when(attributeEditor).setController(any());
            doCallRealMethod().when(attributeEditor).getController();
            attributeEditor.setController(attributeEditorController);

        }
        
        @Test
        final void controllerCanBeSet() {
            
            assertEquals(attributeEditorController, attributeEditor.getController());
            
        }
    
        @Test
        final void setValuePassesTheValueToTheController() {
            
            doCallRealMethod().when(attributeEditor).setValue(any());
            doReturn(attributeEditorController).when(attributeEditor).getController();
            attributeEditor.setValue(Integer.valueOf(1));
            assertEquals(Integer.valueOf(1), attributeEditorController.getValue());
            
        }
    }
    
}
