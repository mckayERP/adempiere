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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MOrderLine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.eevolution.model.MPPOrder;
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
@Tag("MPPOrderStorageRule")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderStorageRule extends CommonUnitTestSetup {

	private static Timestamp today = TimeUtil.getDay(System.currentTimeMillis());
	
	private MPPOrderStorageRule ruleUnderTest;
	
	@BeforeEach
	public void localSetUp() throws Exception {

		ruleUnderTest = spy(MPPOrderStorageRule.class);
		
	}

	@Test
	void testMPPOrderStorageRule_allocateMaterialAndAssignTickets_hasMPT() {
		
		MPPOrder orderMock = mock(MPPOrder.class);
		when(orderMock.getM_MPolicyTicket_ID()).thenReturn(123);
		
		ruleUnderTest.orderMPolicyTicket_id = 0;
		ruleUnderTest.docLine = orderMock;
		
		ruleUnderTest.allocateMaterialAndAssignTickets();
		
		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(orderMock, times(2)).getM_MPolicyTicket_ID();
		verifyNoMoreInteractions(orderMock);
		
	}

	@Captor ArgumentCaptor<Integer> idCaptor;
	
	@Test
	void testMPPOrderStorageRule_allocateMaterialAndAssignTickets_needsMPT() {

		MPPOrder orderMock = mock(MPPOrder.class);
		when(orderMock.getM_MPolicyTicket_ID())
				.thenReturn(0)
				.thenReturn(123);
		
		CLogger log = CLogger.getCLogger(MPPOrderStorageRule.class);

		ruleUnderTest.orderMPolicyTicket_id = 0;
		ruleUnderTest.docLine = orderMock;
		
		doReturn(123).when(ruleUnderTest).getNewMaterialPolicyTicketId();
		doReturn(log).when(ruleUnderTest).getLogger();

		ruleUnderTest.allocateMaterialAndAssignTickets();
		assertEquals(123, ruleUnderTest.orderMPolicyTicket_id,
				"Order MPolicyTicket id should be set");

		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verify(ruleUnderTest).getNewMaterialPolicyTicketId();
		verify(ruleUnderTest).getLogger();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(orderMock, times(2)).getM_MPolicyTicket_ID();
		verify(orderMock).setM_MPolicyTicket_ID(idCaptor.capture());
		verifyNoMoreInteractions(orderMock);

		assertEquals(123, idCaptor.getValue().intValue(),
				"New Material Policy Ticket ID does not match expected");
		
	}


	@Test
	void testMPPOrderStorageRule_createTransactionInfo_null() {

		assertThrows(NullPointerException.class, () -> {
			ruleUnderTest.createTransactionInfo(null);
		});
				
	}

	@Test
	void testMPPOrderStorageRule_createTransactionInfo_notNullNoID() {

		MPPOrderStorageRule ruleUnderTest = new MPPOrderStorageRule();
		MPPOrder orderMock = mock(MPPOrder.class);
		when(orderMock.get_ID()).thenReturn(0);
		
		AdempiereException thrown = assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.createTransactionInfo(orderMock);
		});
		
		assertEquals("Document line has ID <= 0. The document line should be saved and have a valid ID.", 
				thrown.getMessage(), "Message not as expected");
		
	}


	@Test
	void testMPPOrderStorageRule_createTransactionInfo_notNullhasID() {

		MPPOrder orderMock = mock(MPPOrder.class);
		when(orderMock.get_ID()).thenReturn(123);
		when(orderMock.getDateOrdered()).thenReturn(today);
		when(orderMock.getM_Product_ID()).thenReturn(1);
		when(orderMock.getM_Warehouse_ID()).thenReturn(2);
		when(orderMock.getM_AttributeSetInstance_ID()).thenReturn(3);
		
		StorageTransactionInfoBuilder builderSpy = spy(StorageTransactionInfoBuilder.class);
		doReturn(builderSpy).when(ruleUnderTest).getASetOfStorageTransactionInfo();

		StorageTransactionInfo info = ruleUnderTest.createTransactionInfo(orderMock);
		assertEquals(orderMock, ruleUnderTest.order, "Order does not match");
		assertEquals(orderMock, info.getDocumentLine(), "DocumentLine not set as expected");
		assertEquals("", info.getMovementType());
		assertEquals(today, info.getMovementDate());
		assertEquals(Env.ZERO, info.getMovementQty());
		assertEquals(1, info.getM_Product_ID());
		assertEquals(-1, info.getTransactionAttributeSetInstance_id());
		assertEquals(-1, info.getTransactionWarehouse_id());
		assertEquals(-1, info.getTransactionLocator_id());
		assertEquals(2, info.getOrderWarehouse_id());
		assertEquals(3, info.getOrderAttributeSetInstance_id());
		assertEquals(-1, info.getOrderMPolicyTicket_id());
		assertEquals(false, info.isSOTrx());
		assertEquals(false, info.isCreateMaterialAllocations());
		assertEquals(true, info.isProcessMA());

	}

	@Captor ArgumentCaptor<BigDecimal> bdCaptor;
	
	@Test
	void testMPPOrderStorageRule_setStorageRelatedFieldsInModel() {
		
		MPPOrder ppOrderMock = mock(MPPOrder.class);
		
		ruleUnderTest.order = ppOrderMock;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyOrdered();
		
		ruleUnderTest.setStorageRelatedFieldsInModel();
		
		verify(ruleUnderTest).setStorageRelatedFieldsInModel();
		verify(ruleUnderTest).getStorageQtyOrdered();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(ppOrderMock).setQtyReserved(bdCaptor.capture());
	
		assertEquals(Env.ONE, bdCaptor.getValue(), "QtyReserved not set as expected");
		
	}

	@Test
	void testMPPOrderStorageRule_clearStorageRelatedFieldsInModel() {

		MPPOrder ppOrderMock = mock(MPPOrder.class);
		
		ruleUnderTest.order = ppOrderMock;
		
		ruleUnderTest.clearStorageRelatedFieldsInModel();

		verify(ruleUnderTest).clearStorageRelatedFieldsInModel();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(ppOrderMock).setQtyReserved(bdCaptor.capture());
	
		assertEquals(Env.ZERO, bdCaptor.getValue(), "QtyReserved not cleared as expected");

	}

	@Test
	void testMPPOrderStorageRule_getChangeInQtyOnHand() {
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOnHand(), 
				"Change in qty on hand is always zero");
		
	}

	@Test
	void testMPPOrderStorageRule_getChangeInQtyOrdered() {

		MPPOrder ppOrderMock = mock(MPPOrder.class);
		when(ppOrderMock.getQtyOrdered()).thenReturn(Env.ONE);
		
		ruleUnderTest.order = ppOrderMock;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyOrdered();
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOrdered(),
				"Change should be ordered qtyOrdered - storage qtyOrdered");
		
		verify(ruleUnderTest).getChangeInQtyOrdered();
		verify(ruleUnderTest).getStorageQtyOrdered();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(ppOrderMock).getQtyOrdered();
		
	}

	@Test
	void testMPPOrderStorageRule_getChangeInQtyReserved() {

		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyReserved(),
				"Change in qty reserved is always zero");

	}

	@Test
	void testMPPOrderStorageRule_Matches() {
		
		MPPOrder ppOrderMock = mock(MPPOrder.class);
		MOrderLine orderLineMock = mock(MOrderLine.class);
		
		MPPOrderStorageRule ruleUnderTest = new MPPOrderStorageRule();
		assertTrue(ruleUnderTest.matches(ppOrderMock));
		assertFalse(ruleUnderTest.matches(orderLineMock));
		assertFalse(ruleUnderTest.matches(null));
		
	}


}
