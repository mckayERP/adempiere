/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2020 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.adempiere.engine.storage.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MClient;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MMovementLineMA;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Storage")
@Tag("StorageRule")
@Tag("MMovementLineStorageRule")
@ExtendWith(MockitoExtension.class)
class Test_MMovementLineStorageRule extends CommonUnitTestSetup {

    private static final Timestamp now =
            TimeUtil.getDay(System.currentTimeMillis());

    @Captor
    ArgumentCaptor<BigDecimal> bdCaptor;

    @Captor
    ArgumentCaptor<Integer> idCaptor;

    @Captor
    ArgumentCaptor<String> movementTypeCaptor;

    @Captor
    ArgumentCaptor<MMovementLineMA> lineMACaptor;

    private MMovement moveMock;
    private MMovementLine moveLineMock;
    private MMovementLineStorageRule rule;
    private StorageTransactionInfoBuilder infoBuilderSpy;
    private MRefList refListMock;
    private MProduct productMock;
    private MLocator locatorMock;
    private MLocator locatorToMock;

    @BeforeEach
    void beforeEachSetup() {

        moveMock = mock(MMovement.class);
        moveLineMock = mock(MMovementLine.class);
        rule = spy(MMovementLineStorageRule.class);
        infoBuilderSpy = spy(StorageTransactionInfoBuilder.class);
        refListMock = mock(MRefList.class);
        productMock = mock(MProduct.class);
        locatorMock = mock(MLocator.class);
        locatorToMock = mock(MLocator.class);

    }

    @Test
    void matches() {

        MMovementLineStorageRule rule = new MMovementLineStorageRule();
        assertTrue(rule.matches(moveLineMock));

    }

    @Test
    void noMatch() {

        MOrderLine orderLineMock = mock(MOrderLine.class);
        assertFalse(rule.matches(orderLineMock));
        assertFalse(rule.matches(null));

    }

    @Test
    void createTransactionInfo_nullArguments() {

        assertThrows(NullPointerException.class, () -> {
            rule.createTransactionInfo(null);
        });

    }

    @Test
    void verifyNoInteractionsForUnusedMethods() {

        rule.clearStorageRelatedFieldsInModel();
        rule.setStorageRelatedFieldsInModel();

        verify(rule).clearStorageRelatedFieldsInModel();
        verify(rule).setStorageRelatedFieldsInModel();
        verifyNoMoreInteractions(rule);

    }

    @Test
    void changeInQtyShouldBeZero() {

        assertEquals(Env.ZERO, rule.getChangeInQtyOnHand());
        assertEquals(Env.ZERO, rule.getChangeInQtyOrdered());
        assertEquals(Env.ZERO, rule.getChangeInQtyReserved());

    }

    @Nested
    @DisplayName("Given a movement with a from and to locator")
    class GivenFromAndToLocator {

        void assertCorrectInfoCreated(StorageTransactionInfo info, int asi,
                int locatorId, int warehouseId, boolean processMA) {

            assertEquals(moveLineMock, info.getDocumentLine(),
                    "DocumentLine not set as expected");
            assertEquals(2, info.getM_Product_ID());

            assertEquals(asi, info.getTransactionAttributeSetInstance_id());
            assertEquals(locatorId, info.getTransactionLocator_id());
            assertEquals(warehouseId, info.getTransactionWarehouse_id());
            assertEquals(0, info.getOrderWarehouse_id());
            assertEquals(0, info.getOrderAttributeSetInstance_id());
            assertEquals(0, info.getOrderMPolicyTicket_id());
            assertTrue(info.isCreateMaterialAllocations());
            assertEquals(true, info.isFifoPolicy());
            assertEquals(processMA, info.isProcessMA());

        }

        @BeforeEach
        void setupMovement() {

            lenient().when(locatorMock.get_ID()).thenReturn(123);
            lenient().when(locatorMock.getM_Warehouse_ID()).thenReturn(234);
            lenient().doReturn(locatorMock).when(moveLineMock)
                    .getM_Locator();

            lenient().when(locatorToMock.get_ID()).thenReturn(345);
            lenient().when(locatorToMock.getM_Warehouse_ID())
                    .thenReturn(456);
            lenient().doReturn(locatorToMock).when(moveLineMock)
                    .getM_LocatorTo();

            doReturn(moveMock).when(moveLineMock).getParent();
            when(moveLineMock.get_ID()).thenReturn(1);
            when(moveLineMock.getM_Product_ID()).thenReturn(2);
            lenient().when(moveLineMock.getM_AttributeSetInstance_ID())
                    .thenReturn(3);
            lenient().when(moveLineMock.getM_AttributeSetInstanceTo_ID())
                    .thenReturn(4);

            when(moveLineMock.getMovementDate()).thenReturn(now);
            when(moveLineMock.getMovementQty()).thenReturn(Env.ONE);

            doReturn(infoBuilderSpy).when(rule)
                    .getASetOfStorageTransactionInfo();
            doReturn(refListMock).when(infoBuilderSpy).getRefList();
            doReturn(productMock).when(infoBuilderSpy).getProduct();
            lenient().doReturn(productMock).when(rule).getProduct();
            doReturn(MClient.MMPOLICY_FiFo).when(productMock).getMMPolicy();

        }

        @Nested
        @DisplayName("Given the product is not stocked")
        class GivenProductIsNotStocked {

            @BeforeEach
            void setupNotStockedProduct() {

                lenient().doReturn(false).when(productMock).isStocked();

            }

            @Test
            @DisplayName("When the process is called, no action is taken")
            void whenProcessIsCalled_thenNoActionIsTaken() {

                doReturn(locatorMock, locatorToMock).when(infoBuilderSpy)
                        .getMLocator();

                rule.process(moveLineMock);
                verify(rule, never()).deleteExistingMaterialAllocations();
                verify(rule, never()).allocateMaterial();
                verify(rule, never()).updateStorageRecords();
                verify(rule, never()).save(moveLineMock);

            }

        }

        @Nested
        @DisplayName("Given the product is stocked")
        class GivenProductIsStocked {

            @BeforeEach
            void setupStockedProduct() {

                lenient().doReturn(true).when(productMock).isStocked();

            }

            @Test
            @DisplayName("When creating the transaction info \"from\", then the "
                    + "\"from\" side is used")
            void createTransactionInfo_From() {

                doReturn(locatorMock).when(infoBuilderSpy).getMLocator();

                StorageTransactionInfo info =
                        rule.createTransactionInfo(moveLineMock);

                assertCorrectInfoCreated(info, 3, 123, 234, false);

            }

            @Test
            @DisplayName("When creating the transaction info \"to\", then the "
                    + "\"to\" side is used")
            void createTransactionInfo_To() {

                doReturn(locatorToMock).when(infoBuilderSpy).getMLocator();

                StorageTransactionInfo info =
                        rule.createTransactionInfoTo(moveLineMock);

                assertCorrectInfoCreated(info, 4, 345, 456, true);

            }

            @Test
            @DisplayName("When the movement is a reversal, the CreateMaterialAllocation is "
                    + "set to false on the From side")
            void createTransactionInfo_reverasal() {

                doReturn(locatorMock).when(infoBuilderSpy).getMLocator();
                doReturn(true).when(moveMock).isReversal();

                StorageTransactionInfo info =
                        rule.createTransactionInfo(moveLineMock);

                assertFalse(info.isCreateMaterialAllocations(),
                        "Material allocations should not be created for a reversal");

            }

            @Test
            @DisplayName("When the movement is a reversal, the CreateMaterialAllocation is "
                    + "set to false on the TO side")
            void createTransactionInfoTo_reverasal() {

                doReturn(locatorToMock).when(infoBuilderSpy).getMLocator();
                doReturn(true).when(moveMock).isReversal();

                StorageTransactionInfo info =
                        rule.createTransactionInfoTo(moveLineMock);

                assertFalse(info.isCreateMaterialAllocations(),
                        "Material allocations should not be created for a reversal");

            }

            @Test
            @DisplayName("When process is called, then the line is processed")
            void whenProcessIsCalled_thenTheLineIsProcessed() {

                doReturn(locatorMock, locatorToMock).when(infoBuilderSpy)
                        .getMLocator();
                doNothing().when(rule).allocateMaterial();
                doNothing().when(rule).deleteExistingMaterialAllocations();
                doReturn(true).when(rule).updateStorageRecords();
                doNothing().when(rule).save(any());

                rule.process(moveLineMock);
                verify(rule).deleteExistingMaterialAllocations();
                verify(rule).updateStorageRecords();
                verify(rule, times(2)).allocateMaterial();
                verify(rule).save(moveLineMock);

            }

        }

    }

}
