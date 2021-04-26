package org.compiere.grid.ed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.util.stream.Stream;

import org.adempiere.controller.ed.AttributeEditorControllerImpl;
import org.adempiere.controller.ed.EditorController;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.Lookup;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MLookup;
import org.compiere.model.MLookupInfo;
import org.compiere.model.MPAttributeLookup;
import org.compiere.model.MProduct;
import org.compiere.util.DisplayType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class VPAttribute_Test extends CommonGWSetup {

    static Integer NO_INSTANCE = new Integer(0);

    GridField gridfieldMock;
    AttributeEditorControllerImpl attributeEditorController;
    VPAttribute attributeEditor;
    MLookup lookup;
    MLookupInfo infoMock;
    MPAttributeLookup attributeLookupMock;

    static Stream<Arguments> argProviderButtonEnabled() {

        final boolean AS_NULL = true;
        final boolean AS_NOTNULL = false;
        final boolean MANDATORY = true;
        final boolean NOT_MANDATORY = false;
        final boolean READ_ONLY = true;
        final boolean READ_WRITE = false;
        final boolean UPDATEABLE = true;
        final boolean NOT_UPDATEABLE = false;
        final boolean SEARCH_ONLY = true;
        final boolean NOT_SEARCH_ONLY = false;
        final boolean EXPECTED_ENABLED = true;
        final boolean EXPECTED_DISABLED = false;
        final boolean PRODUCT_WINDOW = true;
        final boolean NOT_PRODUCT_WINDOW = false;
        final boolean EXCLUDED = true;
        final boolean NOT_EXCLUDED = false;
        final boolean DONT_CARE = false;

        MLookupInfo infoMock = mock(MLookupInfo.class);
        MPAttributeLookup attributeLookupMock = mock(MPAttributeLookup.class);
        infoMock.DisplayType = DisplayType.PAttribute;
        MLookup lookup = new MLookup(infoMock, 0);
        lookup.setMPAttributeLookup(attributeLookupMock);

        Integer value = Integer.valueOf(1);

        return Stream.of(

    //@formatter:off
    //  AttributeSetNull, mandatory, exclude, Value, isReadWrite, isProductWindow, isSearchOnly, expected
    //gridTab, mandatory, isReadOnly, isUpdateable, windowNo, lookup, searchOnly
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     null, EXPECTED_ENABLED),
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     null, EXPECTED_ENABLED),
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, null, EXPECTED_ENABLED),
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     null, EXPECTED_DISABLED),
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     null, EXPECTED_DISABLED),
    arguments(AS_NULL,    DONT_CARE,    NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     null, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     null, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, null, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     null, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, null, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     null, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, null, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, null, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, NO_INSTANCE, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     value, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, value, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     value, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, EXCLUDED,     NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, value, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_WRITE, UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, value, EXPECTED_ENABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, PRODUCT_WINDOW,     value, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, NOT_SEARCH_ONLY, NOT_PRODUCT_WINDOW, value, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     PRODUCT_WINDOW,     value, EXPECTED_DISABLED),
    arguments(AS_NOTNULL, NOT_EXCLUDED, NOT_MANDATORY, READ_ONLY,  UPDATEABLE, 0, lookup, SEARCH_ONLY,     NOT_PRODUCT_WINDOW, value, EXPECTED_DISABLED)
    //@formatter:on 

        );

    }

    @BeforeEach
    void localSetup() {

        attributeLookupMock = mock(MPAttributeLookup.class);
        infoMock = mock(MLookupInfo.class);
        infoMock.DisplayType = DisplayType.PAttribute;
        lookup = new MLookup(infoMock, 0);
        lookup.setMPAttributeLookup(attributeLookupMock);
        gridfieldMock = mock(GridField.class);
        attributeEditor = new VPAttribute(null, false, false,
                true, 0, lookup, false);

    }

    @Nested
    class GivenNoController {

        @Test
        final void controllerDefaults() {

            assertNotNull(attributeEditor.getController());

        }

    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    class GivenAControllerIsSet {

        @BeforeEach
        void setController() {

        }

        @Test
        final void controllerCanBeSet() {

            assertEquals(attributeEditorController,
                    attributeEditor.getController());

        }

        @Test
        final void setValuePassesTheValueToTheController() {

            attributeEditor.setValue(Integer.valueOf(1));
            assertEquals(Integer.valueOf(1),
                    ((AttributeEditorControllerImpl) attributeEditor
                            .getController()).getValue());

        }

        @Test
        final void setValueSetsText() {

            Object value = Integer.valueOf(1);
            doReturn("The Expected Text").when(attributeLookupMock)
                    .getDisplay(value);
            attributeEditor.setValue(value);
            assertEquals("The Expected Text",
                    attributeEditor.getDisplay());

        }

        @Test
        final void setValueSetsToolTipText() {

            Object value = Integer.valueOf(1);
            doReturn("The Expected Text").when(attributeLookupMock)
                    .getDisplay(value);
            attributeEditor.setValue(value);
            assertEquals("The Expected Text",
                    attributeEditor.textField.getToolTipText());

        }

        @ParameterizedTest
        @MethodSource("getButtonEnabledArgs")
        final void testButtonIsEnabled(boolean isASNull, boolean excludeEntry,
                boolean mandatory, boolean isReadOnly, boolean isUpdateable,
                int windowNo, MLookup lookup, boolean searchOnly,
                boolean isProductWindow, Object value,
                boolean expectedSetting) {

            MProduct productMock = mock(MProduct.class);
            MAttributeSet asMock = mock(MAttributeSet.class);

            attributeEditor = spy(new VPAttribute(null, mandatory, isReadOnly,
                    isUpdateable, windowNo, lookup, searchOnly));
            
            attributeEditorController = spy(new AttributeEditorControllerImpl(
                    attributeEditor, null, mandatory, isReadOnly,
                isUpdateable, windowNo, (Lookup) lookup));
            attributeEditorController.setSearchOnly(searchOnly);
            attributeEditorController.setProductWindow(isProductWindow);
            attributeEditor.setController(attributeEditorController);
            if (isASNull) {
                doReturn(null).when(attributeEditorController).getProduct();
            } else {
                doReturn(1).when(productMock).getM_AttributeSet_ID();
                doReturn(asMock).when(productMock).getM_AttributeSet();
                doReturn(excludeEntry).when(asMock).excludeEntry(anyInt(),
                        anyBoolean());
                doReturn(productMock).when(attributeEditorController).getProduct();
            }
            attributeEditor.setValue(value);
            assertEquals(expectedSetting,
                    attributeEditor.getButton().isEnabled());

        }

        Stream<Arguments> getButtonEnabledArgs() {

            return argProviderButtonEnabled();

        }

    }

}
