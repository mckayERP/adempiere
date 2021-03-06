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
package org.adempiere.engine.storage.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MLocator;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRefList;
import org.compiere.model.MTransaction;
import org.compiere.model.X_M_Transaction;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderReceipt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Storage")
@Tag("StorageRule")
@Tag("MPPOrderRelated")
@Tag("MPPOrderReceiptStorageRule")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderReceiptStorageRule extends CommonUnitTestSetup {
	
	MPPOrder ppOrderMock;
	MPPOrderReceipt ppReceiptMock;
	MPPOrderReceiptStorageRule ruleUnderTest;
	MPPOrderReceiptStorageRule ruleUnderTestSpy;
	
	Timestamp today  = TimeUtil.getDay(System.currentTimeMillis());
	
	@BeforeEach
	public void localBefore() {
		
		ppOrderMock = mock(MPPOrder.class);
		ppReceiptMock = mock(MPPOrderReceipt.class);
		ruleUnderTest = new MPPOrderReceiptStorageRule();
		ruleUnderTestSpy = spy(MPPOrderReceiptStorageRule.class);

	}
	
	
	@Test
	void testMPPOrderReceiptStorageRule_Matches() {
		
		when(ppReceiptMock.getParent()).thenReturn(ppOrderMock);
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		
		assertTrue(ruleUnderTest.matches(ppReceiptMock));
		assertFalse(ruleUnderTest.matches(null));
		assertFalse(ruleUnderTest.matches(orderLineMock));
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_bothNull() {

		assertThrows(NullPointerException.class, () -> {
			ruleUnderTest.createTransactionInfo(null);
		});
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_parentNull() {

		assertThrows(NullPointerException.class, () -> {
			ruleUnderTest.createTransactionInfo(ppReceiptMock);
		});
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_noID() {

		when(ppReceiptMock.getParent()).thenReturn(ppOrderMock);
		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.createTransactionInfo(ppReceiptMock);
		});
		
		assertEquals("Document line has ID <= 0. The document line should be saved and have a valid ID.", thrown.getMessage());
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_noProduct() {

		when(ppReceiptMock.getParent()).thenReturn(ppOrderMock);
		when(ppReceiptMock.get_ID()).thenReturn(1);
		when(ppReceiptMock.getM_Product_ID()).thenReturn(0);
		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.createTransactionInfo(ppReceiptMock);
		});
		
		assertEquals("The Product ID is not set.", thrown.getMessage());
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_noDate() {

		when(ppReceiptMock.get_ID()).thenReturn(1);
		when(ppReceiptMock.getM_Product_ID()).thenReturn(1);
		when(ppReceiptMock.getMovementDate()).thenReturn(null);
		when(ppReceiptMock.getParent()).thenReturn(ppOrderMock);
		
		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.createTransactionInfo(ppReceiptMock);
		});
		
		assertEquals("The Movement Date cannot be null.", thrown.getMessage());
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_noWarehouse() {

		when(ppReceiptMock.get_ID()).thenReturn(1);
		when(ppReceiptMock.getM_Product_ID()).thenReturn(1);
		when(ppReceiptMock.getMovementDate()).thenReturn(today);
		when(ppReceiptMock.getM_Warehouse_ID()).thenReturn(0);
		when(ppReceiptMock.getParent()).thenReturn(ppOrderMock);
		
		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.createTransactionInfo(ppReceiptMock);
		});
		
		assertEquals("No transaction Warehouse ID is set.  A transaction warehouse is required.", thrown.getMessage());
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_noLocator() {

		when(ppReceiptMock.get_ID()).thenReturn(1);
		when(ppReceiptMock.getM_Product_ID()).thenReturn(1);
		when(ppReceiptMock.getMovementDate()).thenReturn(today);
		when(ppReceiptMock.getM_Warehouse_ID()).thenReturn(2);
		when(ppReceiptMock.getM_Locator_ID()).thenReturn(0);
		when(ppReceiptMock.getParent()).thenReturn(ppOrderMock);
		
		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.createTransactionInfo(ppReceiptMock);
		});
		
		assertEquals("No Locator ID is set.  A transaction locator is required.", thrown.getMessage());
		
	}

	@Test
	void testMPPOrderReceiptStorageRule_createTransactionInfo_locatorNotInWarehouse() {

		StorageTransactionInfoBuilder builderSpy = spy(StorageTransactionInfoBuilder.class);
		MLocator locatorMock = mock(MLocator.class);
		MRefList refListMock = mock(MRefList.class);
		
		doReturn(builderSpy).when(ruleUnderTestSpy).getASetOfStorageTransactionInfo();
		doReturn(locatorMock).when(builderSpy).getMLocator();
		doReturn(refListMock).when(builderSpy).getRefList();
		doNothing().when(builderSpy).setFifoPolicyType();
		
		when(locatorMock.getM_Warehouse_ID()).thenReturn(3);
		
		when(ppReceiptMock.get_ID()).thenReturn(1);
		when(ppReceiptMock.getM_Product_ID()).thenReturn(1);
		when(ppReceiptMock.getM_AttributeSetInstance_ID()).thenReturn(2);
		when(ppReceiptMock.getMovementQty()).thenReturn(Env.ONE);
		when(ppReceiptMock.getMovementDate()).thenReturn(today);
		when(ppReceiptMock.getMovementType())
				.thenReturn(X_M_Transaction.MOVEMENTTYPE_ProductionPlus);
		when(ppReceiptMock.getM_Warehouse_ID()).thenReturn(3);
		when(ppReceiptMock.getM_Locator_ID()).thenReturn(4);
		when(ppReceiptMock.isSOTrx()).thenReturn(false);
		when(ppReceiptMock.isReversal()).thenReturn(false);
		when(ppReceiptMock.getParent()).thenReturn(ppOrderMock);

		when(ppOrderMock.getM_Warehouse_ID()).thenReturn(5);
		when(ppOrderMock.getM_AttributeSetInstance_ID()).thenReturn(6);
		when(ppOrderMock.getM_MPolicyTicket_ID()).thenReturn(7);

		StorageTransactionInfo info = ruleUnderTestSpy
				.createTransactionInfo(ppReceiptMock);
		
		assertEquals(ppReceiptMock, info.getDocumentLine(),
				"DocumentLine not set as expected");
		assertEquals(MTransaction.MOVEMENTTYPE_ProductionPlus, info.getMovementType());
		assertEquals(today, info.getMovementDate());
		assertEquals(Env.ONE, info.getMovementQty());
		assertEquals(1, info.getM_Product_ID());
		assertEquals(2, info.getTransactionAttributeSetInstance_id());
		assertEquals(3, info.getTransactionWarehouse_id());
		assertEquals(4, info.getTransactionLocator_id());
		assertEquals(5, info.getOrderWarehouse_id());
		assertEquals(6, info.getOrderAttributeSetInstance_id());
		assertEquals(7, info.getOrderMPolicyTicket_id());
		assertEquals(false, info.isSOTrx());
		assertEquals(true, info.isCreateMaterialAllocations());
		assertEquals(true, info.isProcessMA());
	
	}
	
	@Captor ArgumentCaptor<BigDecimal> qtyDeliveredCaptor;
	@Captor ArgumentCaptor<BigDecimal> qtyReservedCaptor;
	
	@Test
	void testMInOutLineStorageRule_setStorageRelatedFieldsInModel() {
		
		when(ppOrderMock.getQtyDelivered()).thenReturn(Env.ONE);
		
		doReturn(Env.ZERO).when(ruleUnderTestSpy).getStorageQtyOrdered();
		ruleUnderTestSpy.order = ppOrderMock;
		ruleUnderTestSpy.movementQty = Env.ONE;

		ruleUnderTestSpy.setStorageRelatedFieldsInModel();
		verify(ruleUnderTestSpy).setStorageRelatedFieldsInModel();
		verify(ruleUnderTestSpy).getStorageQtyOrdered();
		verifyNoMoreInteractions(ruleUnderTestSpy);
		verify(ppOrderMock).setQtyDelivered(qtyDeliveredCaptor.capture());
		verify(ppOrderMock).setQtyReserved(qtyReservedCaptor.capture());
		verify(ppOrderMock).saveEx();
		
		assertEquals(new BigDecimal(2).intValue(), qtyDeliveredCaptor.getValue().intValue(),
				"Manufacturing Order Qty Delivered not set as expected");
		assertEquals(0, qtyReservedCaptor.getValue().intValue(),
				"Manufacturing Order Qty Reserved not set as expected");
		
	}

	@Test
	void testMInOutLineStorageRule_clearStorageRelatedFieldsInModel() {
		
		ruleUnderTestSpy.order = ppOrderMock;
		ruleUnderTestSpy.movementQty = Env.ONE;

		ruleUnderTestSpy.clearStorageRelatedFieldsInModel();
		verify(ruleUnderTestSpy).clearStorageRelatedFieldsInModel();
		verifyNoMoreInteractions(ruleUnderTestSpy);
		verify(ppOrderMock).setQtyDelivered(qtyDeliveredCaptor.capture());
		verify(ppOrderMock).saveEx();
		
		assertEquals(0, qtyDeliveredCaptor.getValue().intValue(),
				"Manufacturing Order Qty Delivered not set as expected");
		
	}

	@Test
	void testMInOutLineStorageRule_getChangeInQtyOnHand() {
		
		ruleUnderTestSpy.movementQty = Env.ONE;

		BigDecimal change = ruleUnderTestSpy.getChangeInQtyOnHand();

		assertEquals(Env.ONE, change,
				"Change in qty on hand should match the movement qty");
		verify(ruleUnderTestSpy).getChangeInQtyOnHand();
		verifyNoMoreInteractions(ruleUnderTestSpy);
		
	}

	@Test
	void testMInOutLineStorageRule_getChangeInQtyOrdered() {
		
		ruleUnderTestSpy.movementQty = Env.ONE;

		BigDecimal change = ruleUnderTestSpy.getChangeInQtyOrdered();

		assertEquals(Env.ONE.negate(), change,
				"Change in qty ordered should match the negated movement qty");
		verify(ruleUnderTestSpy).getChangeInQtyOrdered();
		verify(ruleUnderTestSpy).getChangeInQtyOnHand();
		verifyNoMoreInteractions(ruleUnderTestSpy);
		
	}

	@Test
	void testMInOutLineStorageRule_getChangeInQtyReserved() {
		
		BigDecimal change = ruleUnderTestSpy.getChangeInQtyReserved();

		assertEquals(Env.ZERO, change,
				"Change in qty reserved should be zero for a receipt");
		verify(ruleUnderTestSpy).getChangeInQtyReserved();
		verifyNoMoreInteractions(ruleUnderTestSpy);
		
	}

}