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
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Storage")
@Tag("StorageRule")
@Tag("MOrderLineStorageRule")
@ExtendWith(MockitoExtension.class)
public class Test_MOrderLineStorageRule extends CommonUnitTestSetup {

	private static final Timestamp now = TimeUtil.getDay(System.currentTimeMillis());
	
	@Captor ArgumentCaptor<Integer> idCaptor;
	@Captor ArgumentCaptor<BigDecimal> qtyReservedCaptor;

	@Test
	void testMOrderLineStorageRule_allocateMaterialAndAssignTickets_orderNotBinding() {
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.onlyIfOrderIsBinding = false;
		
		ruleUnderTest.allocateMaterialAndAssignTickets();
		
		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verifyNoMoreInteractions(ruleUnderTest);
		
	}

	@Test
	void testMOrderLineStorageRule_allocateMaterialAndAssignTickets_orderIsBinding_hasMPT() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		when(orderLineMock.getM_MPolicyTicket_ID()).thenReturn(1);
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.onlyIfOrderIsBinding = true;
		ruleUnderTest.orderMPolicyTicket_id = 0;
		ruleUnderTest.docLine = orderLineMock;
		
		ruleUnderTest.allocateMaterialAndAssignTickets();
		
		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(orderLineMock).getM_MPolicyTicket_ID();
		verifyNoMoreInteractions(orderLineMock);
		
	}

	@Test
	void testMOrderLineStorageRule_allocateMaterialAndAssignTickets_orderIsBinding_needsMPT() {

		MOrderLine orderLineMock = mock(MOrderLine.class);
		when(orderLineMock.getM_MPolicyTicket_ID())
				.thenReturn(0)
				.thenReturn(123);
		
		CLogger log = CLogger.getCLogger(MOrderLineStorageRule.class);
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.onlyIfOrderIsBinding = true;
		ruleUnderTest.orderMPolicyTicket_id = 0;
		ruleUnderTest.docLine = orderLineMock;
		doReturn(123).when(ruleUnderTest).getNewMaterialPolicyTicketId();
		doReturn(log).when(ruleUnderTest).getLogger();

		ruleUnderTest.allocateMaterialAndAssignTickets();
		assertEquals(123, ruleUnderTest.orderMPolicyTicket_id,
				"Order MPolicyTicket id should be set");


		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verify(ruleUnderTest).getNewMaterialPolicyTicketId();
		verify(ruleUnderTest).getLogger();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(orderLineMock, times(2)).getM_MPolicyTicket_ID();
		verify(orderLineMock).setM_MPolicyTicket_ID(idCaptor.capture());
		verifyNoMoreInteractions(orderLineMock);

		assertEquals(123, idCaptor.getValue().intValue(),
				"New Material Policy Ticket ID does not match expected");
		
	}
	
	@Test
	void testMOrderLineStorageRule_createTransactionInfo_nullArguments() {
		
		MOrderLineStorageRule ruleUnderTest = new MOrderLineStorageRule();

		assertThrows(NullPointerException.class, () -> {
			ruleUnderTest.createTransactionInfo(null);
		});
		
	}

	@Test
	void testMOrderLineStorageRule_createTransactionInfo_NotBinding() {
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		MOrder orderMock = mock(MOrder.class);
		MOrderLine orderLineMock = mock(MOrderLine.class);
		StorageTransactionInfoBuilder builderSpy = spy(StorageTransactionInfoBuilder.class);
		
		when(orderLineMock.get_ID()).thenReturn(100000);		
		when(orderLineMock.getM_Product_ID()).thenReturn(456);
		when(orderLineMock.getM_AttributeSetInstance_ID()).thenReturn(123);
		when(orderLineMock.getM_Warehouse_ID()).thenReturn(234);
		when(orderLineMock.getM_MPolicyTicket_ID()).thenReturn(345);
		when(orderLineMock.getParent()).thenReturn(orderMock);
		
		when(orderMock.getDateOrdered()).thenReturn(now);
		when(orderMock.isSOTrx()).thenReturn(false);
		when(orderMock.isOrderBinding()).thenReturn(false);
		
		ruleUnderTest.setStorageTransactionInfoBuilder(builderSpy);
		StorageTransactionInfo info = ruleUnderTest.createTransactionInfo(orderLineMock);
		
		assertEquals(orderLineMock, info.getDocumentLine(),
				"DocumentLine not set as expected");
		assertEquals("", info.getMovementType());
		assertEquals(now, info.getMovementDate());
		assertEquals(Env.ZERO, info.getMovementQty());
		assertEquals(456, info.getM_Product_ID());
		assertEquals(-1, info.getTransactionAttributeSetInstance_id());
		assertEquals(-1, info.getTransactionWarehouse_id());
		assertEquals(-1, info.getTransactionLocator_id());
		assertEquals(234, info.getOrderWarehouse_id());
		assertEquals(123, info.getOrderAttributeSetInstance_id());
		assertEquals(345, info.getOrderMPolicyTicket_id());
		assertEquals(false, info.isSOTrx());
		assertEquals(false, info.isCreateMaterialAllocations());
		assertEquals(false, info.isUpdateStorage());
		assertEquals(false, info.isProcessMA());

	}

	@Test
	void testMOrderLineStorageRule_setStorageRelatedFieldsInModel() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.orderLine = orderLineMock;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyOrdered();
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyReserved();		
		
		ruleUnderTest.setStorageRelatedFieldsInModel();
		
		verify(ruleUnderTest).setStorageRelatedFieldsInModel();
		verify(ruleUnderTest).getStorageQtyReserved();
		verify(ruleUnderTest).getStorageQtyOrdered();
		verifyNoMoreInteractions(ruleUnderTest);

		verify(orderLineMock).setQtyReserved(qtyReservedCaptor.capture());
		assertEquals(BigDecimal.valueOf(2), qtyReservedCaptor.getValue(),
				"Order line qty reserved not set correctly");

	}

	@Test
	void testMOrderLineStorageRule_clearStorageRelatedFieldsInModel() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.orderLine = orderLineMock;
		
		ruleUnderTest.clearStorageRelatedFieldsInModel();

		verify(orderLineMock).setQtyReserved(qtyReservedCaptor.capture());
		assertEquals(Env.ZERO, qtyReservedCaptor.getValue(),
				"Set value should be zero");
		
	}

	@Test
	void testMOrderLineStorageRule_getChangeInQtyOnHand() {
		
		MOrderLineStorageRule ruleUnderTest = new MOrderLineStorageRule();
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOnHand(),
				"Change in qtyOnHand should be zero for orders");
		
	}

	@Test
	void testMOrderLineStorageRule_getChangeInQtyOrdered_isOTrx() {
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.isSOTrx = true;
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOrdered(),
				"For sales transaction, change in qty ordered should be zero");
	}

	@Test
	void testMOrderLineStorageRule_getChangeInQtyOrdered_isNotSOTrx() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		when(orderLineMock.getQtyOrdered()).thenReturn(Env.ONE);
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.isSOTrx = false;
		ruleUnderTest.orderLine = orderLineMock;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyOrdered();
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOrdered(),
				"For Sales transaction, change in qty Reserved should equal"
				+ "order qtyOrdered - storage qtyOrdered");
		
	}

	@Test
	void testMOrderLineStorageRule_getChangeInQtyReserved_isNotSOTrx() {
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.isSOTrx = false;
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyReserved(),
				"For non-Sales transaction, change in qty Reserved should be zero");
	}

	@Test
	void testMOrderLineStorageRule_getChangeInQtyReserved_isSOTrx() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		when(orderLineMock.getQtyOrdered()).thenReturn(Env.ONE);
		
		MOrderLineStorageRule ruleUnderTest = spy(MOrderLineStorageRule.class);
		ruleUnderTest.isSOTrx = true;
		ruleUnderTest.orderLine = orderLineMock;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyReserved();
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyReserved(),
				"For Sales transaction, change in qty Reserved should equal"
				+ "order qtyOrdered - storage qtyReserved");
		
	}


	@Test
	void testMOrderLineStorageRule_Matches() {
		
		MOrder orderMock = mock(MOrder.class);
		MOrderLine orderLineMock = mock(MOrderLine.class);
		when(orderLineMock.getParent())
			.thenReturn(orderMock);
			
		MInOutLine inOutLineMock = mock(MInOutLine.class);
		
		MOrderLineStorageRule rule = new MOrderLineStorageRule();
		assertTrue(rule.matches(orderLineMock));
		assertFalse(rule.matches(inOutLineMock));
		assertFalse(rule.matches(null));
		
	}
	
}
