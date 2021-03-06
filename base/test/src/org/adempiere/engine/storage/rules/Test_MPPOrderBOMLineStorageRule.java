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
import org.compiere.util.CLogger;
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
@Tag("StorageRule")
@Tag("MPPOrderRelated")
@Tag("MPPOrderBOMLineStorageRule")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderBOMLineStorageRule extends CommonUnitTestSetup {

	private static Timestamp today = TimeUtil.getDay(System.currentTimeMillis());
	
	private MPPOrderBOMLineStorageRule ruleUnderTest;
	
	@BeforeEach
	void localSetUp() throws Exception {

		ruleUnderTest = spy(MPPOrderBOMLineStorageRule.class);
		
	}

	@Test
	void testMPPOrderStorageRule_allocateMaterialAndAssignTickets_hasMPT() {
		
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		when(lineMock.getM_MPolicyTicket_ID()).thenReturn(123);
		
		ruleUnderTest.orderMPolicyTicket_id = 0;
		ruleUnderTest.docLine = lineMock;
		
		ruleUnderTest.allocateMaterialAndAssignTickets();
		
		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(lineMock, times(1)).getM_MPolicyTicket_ID();
		verifyNoMoreInteractions(lineMock);
		
	}

	@Captor ArgumentCaptor<Integer> idCaptor;
	
	@Test
	void testMPPOrderStorageRule_allocateMaterialAndAssignTickets_needsMPT() {

		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		when(lineMock.getM_MPolicyTicket_ID())
				.thenReturn(0)
				.thenReturn(123);
		
		CLogger log = CLogger.getCLogger(MPPOrderBOMLineStorageRule.class);

		ruleUnderTest.orderMPolicyTicket_id = 0;
		ruleUnderTest.docLine = lineMock;
		
		doReturn(123).when(ruleUnderTest).getNewMaterialPolicyTicketId();
		doReturn(log).when(ruleUnderTest).getLogger();

		ruleUnderTest.allocateMaterialAndAssignTickets();
		assertEquals(123, ruleUnderTest.orderMPolicyTicket_id,
				"Order MPolicyTicket id should be set");


		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verify(ruleUnderTest).getNewMaterialPolicyTicketId();
		verify(ruleUnderTest).getLogger();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(lineMock, times(2)).getM_MPolicyTicket_ID();
		verify(lineMock).setM_MPolicyTicket_ID(idCaptor.capture());
		verifyNoMoreInteractions(lineMock);

		assertEquals(123, idCaptor.getValue().intValue(),
				"New Material Policy Ticket ID does not match expected");
		
	}


	void createTransactionInfo_null() {

		assertThrows(NullPointerException.class, () -> {
			ruleUnderTest.createTransactionInfo(null);
		});

	}

	@Test
	void createTransactionInfo_notNullCoProduct() {

		MPPOrder orderMock = mock(MPPOrder.class);
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		
		when(lineMock.get_ID()).thenReturn(123);
		when(lineMock.getMovementDate()).thenReturn(today);
		when(lineMock.getM_Product_ID()).thenReturn(1);
		when(lineMock.getM_Warehouse_ID()).thenReturn(2);
		when(lineMock.getM_AttributeSetInstance_ID()).thenReturn(3);
		when(lineMock.getComponentType()).thenReturn(MPPOrderBOMLine.COMPONENTTYPE_Co_Product);
		when(lineMock.getParent()).thenReturn(orderMock);
		
		StorageTransactionInfoBuilder builderSpy = spy(StorageTransactionInfoBuilder.class);
		doReturn(builderSpy).when(ruleUnderTest).getASetOfStorageTransactionInfo();

		StorageTransactionInfo info = ruleUnderTest.createTransactionInfo(lineMock);
		
		assertEquals(orderMock, ruleUnderTest.order, "Order does not match");
		assertEquals(lineMock, ruleUnderTest.bomLine, "Line does not match");
		assertEquals(lineMock, info.getDocumentLine(), "DocumentLine not set as expected");
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

	@Test
	void createTransactionInfo_notNullnotCoProduct() {

		MPPOrder orderMock = mock(MPPOrder.class);
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		
		when(lineMock.get_ID()).thenReturn(123);
		when(lineMock.getMovementDate()).thenReturn(today);
		when(lineMock.getM_Product_ID()).thenReturn(1);
		when(lineMock.getM_Warehouse_ID()).thenReturn(2);
		when(lineMock.getM_AttributeSetInstance_ID()).thenReturn(3);
		when(lineMock.getComponentType()).thenReturn(MPPOrderBOMLine.COMPONENTTYPE_Component);
		when(lineMock.getParent()).thenReturn(orderMock);

		StorageTransactionInfoBuilder builderSpy = spy(StorageTransactionInfoBuilder.class);
		doReturn(builderSpy).when(ruleUnderTest).getASetOfStorageTransactionInfo();

		StorageTransactionInfo info = ruleUnderTest.createTransactionInfo(lineMock);
		
		assertEquals(orderMock, ruleUnderTest.order, "Order does not match");
		assertEquals(lineMock, ruleUnderTest.bomLine, "Line does not match");
		assertEquals(lineMock, info.getDocumentLine(), "DocumentLine not set as expected");
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
		assertEquals(true, info.isSOTrx());
		assertEquals(false, info.isCreateMaterialAllocations());
		assertEquals(true, info.isProcessMA());

	}
	
	@Captor ArgumentCaptor<BigDecimal> bdCaptor;

	@Test
	void setStorageRelatedFieldsInModel() {
		
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		
		ruleUnderTest.bomLine = lineMock;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyOrdered();
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyReserved();
		
		ruleUnderTest.setStorageRelatedFieldsInModel();
		
		verify(lineMock).setQtyReserved(bdCaptor.capture());
		
		assertEquals(BigDecimal.valueOf(2), bdCaptor.getValue(),
				"The BOMLine qty reserved should be the sum of "
				+ "storage qty ordered and qty reserved");
		
	}

	@Test
	void clearStorageRelatedFieldsInModel() {
		
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		
		ruleUnderTest.bomLine = lineMock;
		
		ruleUnderTest.clearStorageRelatedFieldsInModel();
		
		verify(lineMock).setQtyReserved(bdCaptor.capture());
		
		assertEquals(Env.ZERO, bdCaptor.getValue(),
				"The BOMLine qty reserved should be set to zero");
		
	}

	@Test
	void getChangeInQtyOnHand() {
		
		MPPOrderBOMLineStorageRule ruleUnderTest = new MPPOrderBOMLineStorageRule();
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOnHand(),
				"Change in qtyOnHand should be zero");

	}

	@Test
	void getChangeInQtyOrdered_isSoTrx() {
		
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		
		ruleUnderTest.bomLine = lineMock;
		ruleUnderTest.isSOTrx = true;
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOrdered(),
				"Sales Trx should not have qty ordered");
		
	}

	@Test
	void getChangeInQtyOrdered_notIsSoTrx() {
		
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		when(lineMock.getQtyRequired()).thenReturn(Env.ZERO);
		
		ruleUnderTest.bomLine = lineMock;
		ruleUnderTest.isSOTrx = false;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyOrdered();

		assertEquals(Env.ONE.negate(), ruleUnderTest.getChangeInQtyOrdered(),
				"Non Sales Transactions whould return qrtyRequired - Storage qty ordered");
		
	}

	@Test
	void getChangeInQtyReserved_nonIsSoTrx() {

		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		
		ruleUnderTest.bomLine = lineMock;
		ruleUnderTest.isSOTrx = false;
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyReserved(),
				"No sales Trx should not have qty reserved");
		
	}

	@Test
	void getChangeInQtyReserved_IsSoTrx() {
		
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		when(lineMock.getQtyRequired()).thenReturn(Env.ZERO);
		
		ruleUnderTest.bomLine = lineMock;
		ruleUnderTest.isSOTrx = true;
		doReturn(Env.ONE).when(ruleUnderTest).getStorageQtyReserved();

		assertEquals(Env.ONE.negate(), ruleUnderTest.getChangeInQtyReserved(),
				"Sales Transactions whould return qrtyRequired - Storage qty reserveded");
		
	}

	@Test
	void matches() {

		MPPOrder ppOrderMock = mock(MPPOrder.class);
		MPPOrderBOMLine bomLineMock = mock(MPPOrderBOMLine.class);

		MPPOrderBOMLineStorageRule ruleUnderTest = new MPPOrderBOMLineStorageRule();
		assertTrue(ruleUnderTest.matches(bomLineMock));
		assertFalse(ruleUnderTest.matches(ppOrderMock));
		assertFalse(ruleUnderTest.matches(null));
		
	}


}
