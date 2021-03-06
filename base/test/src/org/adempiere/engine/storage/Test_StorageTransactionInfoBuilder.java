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
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.adempiere.engine.storage;

import static org.adempiere.engine.storage.StorageTransactionInfoBuilder.aSetOfStorageTransactionInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MClient;
import org.compiere.model.MInOutLine;
import org.compiere.model.MLocator;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.model.X_M_Transaction;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderBOMLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Storage")
@Tag("StorageEngine")
@ExtendWith(MockitoExtension.class)
class Test_StorageTransactionInfoBuilder extends CommonUnitTestSetup {

	private static int m_warehouse_id = 1;
	private static int m_locator_id = 2;

	private Timestamp today = TimeUtil.getDay(System.currentTimeMillis());

	MOrderLine unsavedOrderLine = mock(MOrderLine.class);
	MOrderLine savedOrderLine = mock(MOrderLine.class);
	MInOutLine savedMInOutLine = mock(MInOutLine.class);

	@Captor
	ArgumentCaptor<Boolean> booleanCaptor;

	@BeforeEach
	void localSetUpBefore() throws Exception {

		when(unsavedOrderLine.get_ID()).thenReturn(0);
		when(savedOrderLine.get_ID()).thenReturn(1);
		when(savedMInOutLine.get_ID()).thenReturn(1);

	}

	private StorageTransactionInfoBuilder setupBuilderUnderTestWithMockedLocator(int warehouse_id) {

		MLocator locatorMock = mock(MLocator.class);
		when(locatorMock.getM_Warehouse_ID()).thenReturn(warehouse_id);

		StorageTransactionInfoBuilder builderUnderTest = spy(StorageTransactionInfoBuilder.class);
		doReturn(locatorMock).when(builderUnderTest).getMLocator();

		return builderUnderTest;

	}

	@Test
	void failsWithNoInput() {

		StorageTransactionInfoBuilder builder = aSetOfStorageTransactionInfo();

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});
		assertEquals(StorageTransactionInfoBuilder.NO_DOCUMENT_LINE, thrown.getMessage());

	}

	@Test
	void failsWithUnsavedDocumentLine() {

		StorageTransactionInfoBuilder builder = aSetOfStorageTransactionInfo()
				.withDocumentLine(unsavedOrderLine);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});
		assertEquals(StorageTransactionInfoBuilder.DOCUMENT_LINE_NOT_SAVED, thrown.getMessage());

	}

	@Test
	void failsWithNoProduct() {

		StorageTransactionInfoBuilder builder = aSetOfStorageTransactionInfo()
				.withDocumentLine(savedMInOutLine);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});
		assertEquals(StorageTransactionInfoBuilder.NO_PRODUCT_DEFINED, thrown.getMessage());

	}

	@Test
	void failsWithNullMovementDate() {

		StorageTransactionInfoBuilder builder = aSetOfStorageTransactionInfo()
				.withDocumentLine(savedMInOutLine)
				.withM_Product_ID(1);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});
		assertEquals(StorageTransactionInfoBuilder.NO_MOVEMENT_DATE, thrown.getMessage());

	}

	@Test
	void shouldNotFailForOrderTypes() {

		try {

			aSetOfStorageTransactionInfo()
					.withDocumentLine(savedOrderLine)
					.withM_Product_ID(1)
					.withMovementDate(today)
					.withOrderWarehouse_id(m_warehouse_id)
					.withTransactionLocator_id(m_locator_id)
					.build();

		} catch (Exception e) {

			fail("Builder threw an unexpected error: " + e.getMessage());

		}

	}

	@Test
	void failsWithNullMovementType() {

		StorageTransactionInfoBuilder builderUnderTest = setupBuilderUnderTestWithMockedLocator(
				m_warehouse_id);

		StorageTransactionInfoBuilder builder = builderUnderTest
				.withDocumentLine(savedMInOutLine)
				.withM_Product_ID(1)
				.withMovementDate(today)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionLocator_id(m_locator_id)
				.withMovementType(null);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});

		assertEquals(StorageTransactionInfoBuilder.NO_MOVEMENT_TYPE, thrown.getMessage());

	}

	@Test
	void failsWithEmptyMovementType() {

		StorageTransactionInfoBuilder builderUnderTest = setupBuilderUnderTestWithMockedLocator(
				m_warehouse_id);

		StorageTransactionInfoBuilder builder = builderUnderTest
				.withDocumentLine(savedMInOutLine)
				.withM_Product_ID(1)
				.withMovementDate(today)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionLocator_id(m_locator_id)
				.withMovementType("");

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});

		assertEquals(StorageTransactionInfoBuilder.NO_MOVEMENT_TYPE, thrown.getMessage());

	}

	@Test
	void failsWithWrongMovementType() {

		MLocator locatorMock = mock(MLocator.class);
		when(locatorMock.getM_Warehouse_ID()).thenReturn(m_warehouse_id);

		StorageTransactionInfoBuilder builderUnderTest = spy(StorageTransactionInfoBuilder.class);
		doReturn(locatorMock).when(builderUnderTest).getMLocator();
		doReturn(null).when(builderUnderTest).getRefList();

		StorageTransactionInfoBuilder builder = builderUnderTest
				.withDocumentLine(savedMInOutLine)
				.withM_Product_ID(1)
				.withMovementDate(today)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionLocator_id(m_locator_id)
				.withMovementType("xyz");

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});

		assertEquals(StorageTransactionInfoBuilder.UNKNOWN_MOVEMENT_TYPE + ": xyz",
				thrown.getMessage());

	}

	@Test
	void failsWithNoWarhouse() {

		StorageTransactionInfoBuilder builder = aSetOfStorageTransactionInfo()
				.withDocumentLine(savedMInOutLine)
				.withM_Product_ID(1)
				.withMovementDate(today);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});
		assertEquals(StorageTransactionInfoBuilder.NO_WAREHOUSE_DEFINED, thrown.getMessage());

	}

	@Test
	void failsWithNoLocator() {

		StorageTransactionInfoBuilder builder = aSetOfStorageTransactionInfo()
				.withDocumentLine(savedMInOutLine)
				.withM_Product_ID(1)
				.withMovementDate(today)
				.withTransactionWarehouse_id(m_warehouse_id);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});
		assertEquals(StorageTransactionInfoBuilder.NO_LOCATOR_DEFINED, thrown.getMessage());

	}

	@Test
	void failsWithLocatorNotInWarehouse() {

		StorageTransactionInfoBuilder builderUnderTest = setupBuilderUnderTestWithMockedLocator(3);

		StorageTransactionInfoBuilder builder = builderUnderTest
				.withDocumentLine(savedMInOutLine)
				.withM_Product_ID(1)
				.withMovementDate(today)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionLocator_id(m_locator_id);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});

		assertEquals(StorageTransactionInfoBuilder.LOCATOR_NOT_IN_WAREHOUSE, thrown.getMessage());

	}

	@Test
	void failsWithNoOrderWarehouse() {

		StorageTransactionInfoBuilder builder = aSetOfStorageTransactionInfo()
				.withDocumentLine(savedOrderLine)
				.withM_Product_ID(1)
				.withMovementDate(today);

		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {

			builder.build();

		});
		assertEquals(StorageTransactionInfoBuilder.NO_ORDER_WAREHOUSE_DEFINED, thrown.getMessage());

	}

	@Test
	void verifyDefaultValues() {

		MLocator locatorMock = mock(MLocator.class);
		when(locatorMock.getM_Warehouse_ID()).thenReturn(m_warehouse_id);

		MRefList refListMock = mock(MRefList.class);

		StorageTransactionInfoBuilder builderUnderTest = spy(StorageTransactionInfoBuilder.class);
		doReturn(locatorMock).when(builderUnderTest).getMLocator();
		doReturn(refListMock).when(builderUnderTest).getRefList();

		int m_product_id = 1;
		int transactionAttributeSetInstance_id = 4;
		int transactionMPolicyTicket_id = 5;
		int orderMPolicyTicket_id = 7;
		int orderAttributeSetInstance_id = 8;
		boolean createMA = true;
		boolean isSOTrx = true;
		boolean deleteExistingMAEntries = true;
		boolean processMA = true;
		boolean useToFields = false;
		boolean updateStorage = true;
		boolean isIncomingTransaction = false;
		boolean isFifoPolicy = true;

		StorageTransactionInfo transactionInfo = builderUnderTest
				.withDocumentLine(savedMInOutLine)
				.withMovementType(X_M_Transaction.MOVEMENTTYPE_CustomerShipment)
				.withMovementDate(today)
				.withMovementQty(Env.ONE)
				.withM_Product_ID(m_product_id)
				.withTransactionAttributeSetInstance_id(transactionAttributeSetInstance_id)
				.withTransactionLocator_id(m_locator_id)
				.withTransactionWarehouse_id(m_warehouse_id)
				.withTransactionMPolicyTicket_id(transactionMPolicyTicket_id)
				.withOrderWarehouse_id(m_warehouse_id)
				.withOrderMPolicyTicket_id(orderMPolicyTicket_id)
				.withOrderAttributeSetInstance_id(orderAttributeSetInstance_id)
				.withCreateMaterialAllocation(createMA)
				.withSOTrx(isSOTrx)
				.withDeleteExistingMAEntries(deleteExistingMAEntries)
				.withProcessMA(processMA)
				.withUseToFields(useToFields)
				.withUpdateStrorage(updateStorage)
				.withIsIncomingTransaction(isIncomingTransaction)
				.withIsFifoPolicy(isFifoPolicy)
				.build();

		assertEquals(savedMInOutLine, transactionInfo.getDocumentLine());
		assertEquals(X_M_Transaction.MOVEMENTTYPE_CustomerShipment,
				transactionInfo.getMovementType());
		assertEquals(today, transactionInfo.getMovementDate());
		assertEquals(m_product_id, transactionInfo.getM_Product_ID());
		assertEquals(transactionAttributeSetInstance_id,
				transactionInfo.getTransactionAttributeSetInstance_id());
		assertEquals(m_warehouse_id, transactionInfo.getTransactionWarehouse_id());
		assertEquals(m_locator_id, transactionInfo.getTransactionLocator_id());
		assertEquals(transactionMPolicyTicket_id, transactionInfo.getTransactionMPolicyTicket_id());
		assertEquals(m_warehouse_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(orderAttributeSetInstance_id,
				transactionInfo.getOrderAttributeSetInstance_id());
		assertEquals(orderMPolicyTicket_id, transactionInfo.getOrderMPolicyTicket_id());
		assertEquals(createMA, transactionInfo.isCreateMaterialAllocations());
		assertEquals(isSOTrx, transactionInfo.isSOTrx());
		assertEquals(deleteExistingMAEntries, transactionInfo.isDeleteExistingMAEntries());
		assertEquals(processMA, transactionInfo.isProcessMA());
		assertEquals(useToFields, transactionInfo.isUseToFields());
		assertEquals(updateStorage, transactionInfo.isUpdateStorage());
		assertEquals(isIncomingTransaction, transactionInfo.isIncomingTransaction());
		assertEquals(isFifoPolicy, transactionInfo.isFifoPolicy());

	}

	@Test
	void setFifoPolicyType_AlreadySet() {

		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.isFifoPolicyOptional()).thenReturn(Optional.of(true));

		StorageTransactionInfoBuilder builderUnderTest = spy(StorageTransactionInfoBuilder.class);
		builderUnderTest.setTransactionInfo(infoMock);

		builderUnderTest.setFifoPolicyType();
		verify(builderUnderTest).setTransactionInfo(any(StorageTransactionInfo.class));
		verify(builderUnderTest).setFifoPolicyType();
		verify(infoMock).isFifoPolicyOptional();
		verifyNoMoreInteractions(builderUnderTest);

	}

	@Test
	void setFifoPolicyType_Fifo() {

		MProduct productMock = mock(MProduct.class);
		when(productMock.getMMPolicy())
				.thenReturn(MClient.MMPOLICY_FiFo);

		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.isFifoPolicyOptional()).thenReturn(Optional.empty());

		StorageTransactionInfoBuilder builderUnderTest = spy(StorageTransactionInfoBuilder.class);
		builderUnderTest.setTransactionInfo(infoMock);
		doReturn(productMock).when(builderUnderTest).getProduct();

		builderUnderTest.setFifoPolicyType();

		verify(builderUnderTest).setTransactionInfo(any(StorageTransactionInfo.class));
		verify(builderUnderTest).setFifoPolicyType();
		verify(builderUnderTest).getProduct();
		verify(infoMock).isFifoPolicyOptional();
		verify(infoMock).setIsFifoPolicy(booleanCaptor.capture());
		verify(productMock).getMMPolicy();
		verifyNoMoreInteractions(builderUnderTest);

		assertTrue(booleanCaptor.getValue());

	}

	@Test
	void setFifoPolicyType_Lifo() {

		MProduct productMock = mock(MProduct.class);
		when(productMock.getMMPolicy())
				.thenReturn(MClient.MMPOLICY_LiFo);

		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.isFifoPolicyOptional()).thenReturn(Optional.empty());

		StorageTransactionInfoBuilder builderUnderTest = spy(StorageTransactionInfoBuilder.class);
		builderUnderTest.setTransactionInfo(infoMock);
		doReturn(productMock).when(builderUnderTest).getProduct();

		builderUnderTest.setFifoPolicyType();

		verify(builderUnderTest).setTransactionInfo(any(StorageTransactionInfo.class));
		verify(builderUnderTest).setFifoPolicyType();
		verify(builderUnderTest).getProduct();
		verify(infoMock).isFifoPolicyOptional();
		verify(infoMock).setIsFifoPolicy(booleanCaptor.capture());
		verify(productMock).getMMPolicy();
		verifyNoMoreInteractions(builderUnderTest);

		assertFalse(booleanCaptor.getValue());

	}

	@Test
	void notAnOrderType() {

		MOrderLine orderLineMock = mock(MOrderLine.class);
		MPPOrder ppOrderMock = mock(MPPOrder.class);
		MPPOrderBOMLine ppOrderBOMLineMock = mock(MPPOrderBOMLine.class);

		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.getDocumentLine())
				.thenReturn(orderLineMock)
				.thenReturn(ppOrderMock, ppOrderMock)
				.thenReturn(ppOrderBOMLineMock, ppOrderBOMLineMock, ppOrderBOMLineMock)
				.thenReturn(savedMInOutLine, savedMInOutLine, savedMInOutLine);

		StorageTransactionInfoBuilder builderUnderTest = spy(StorageTransactionInfoBuilder.class);
		builderUnderTest.setTransactionInfo(infoMock);

		assertFalse(builderUnderTest.notAnOrderType());
		assertFalse(builderUnderTest.notAnOrderType());
		assertFalse(builderUnderTest.notAnOrderType());
		assertTrue(builderUnderTest.notAnOrderType());

	}

}
