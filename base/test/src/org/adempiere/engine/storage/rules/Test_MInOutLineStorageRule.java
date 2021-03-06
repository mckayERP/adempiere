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

import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_CustomerReturns;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_VendorReturns;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_CustomerShipment;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_VendorReceipts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MLocator;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRMALine;
import org.compiere.model.MRefList;
import org.compiere.model.MTransaction;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Storage")
@Tag("StorageRule")
@Tag("MInOutLineStorageRule")
@ExtendWith(MockitoExtension.class)
class Test_MInOutLineStorageRule extends CommonUnitTestSetup  {

	private static final Timestamp now = TimeUtil.getDay(System.currentTimeMillis());
	
	@Captor
	ArgumentCaptor<BigDecimal> bdCaptor;

	@Captor
	ArgumentCaptor<Integer> idCaptor;

	@Captor
	ArgumentCaptor<String> movementTypeCaptor;

	@Captor
	ArgumentCaptor<MInOutLineMA> lineMACaptor;

	private MInOutLine inOutLineMock;

	@BeforeEach
	void beforeEachSetup () {
		
		inOutLineMock = mock(MInOutLine.class);
		
	}
	
	@Test
	void matches() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		MInOutLineStorageRule rule = new MInOutLineStorageRule();
		assertTrue(rule.matches(inOutLineMock));
		assertFalse(rule.matches(orderLineMock));
		assertFalse(rule.matches(null));
		
	}

	@Test
	void noMatch() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		MInOutLineStorageRule rule = new MInOutLineStorageRule();
		assertTrue(rule.matches(inOutLineMock));
		assertFalse(rule.matches(orderLineMock));
		assertFalse(rule.matches(null));
		
	}

	void createTransactionInfo_nullArguments() {
		
		MInOutLineStorageRule ruleUnderTest = new MInOutLineStorageRule();
		assertThrows(NullPointerException.class, () -> {
			ruleUnderTest.createTransactionInfo(null);
		});
		

	}
	
	@Test
	void createTransactionInfo_noOrderLine() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		MInOut inOutMock = mock(MInOut.class);
		MLocator locatorMock = mock(MLocator.class);
		MRefList refListMock = mock(MRefList.class);
		StorageTransactionInfoBuilder builderSpy 
			= spy(StorageTransactionInfoBuilder.class);
		
		when(inOutLineMock.get_ID()).thenReturn(100000);		
		when(inOutLineMock.getC_OrderLine_ID()).thenReturn(0);
		when(inOutLineMock.getMovementQty()).thenReturn(Env.ONE);
		when(inOutLineMock.getM_Product_ID()).thenReturn(456);
		when(inOutLineMock.getM_AttributeSetInstance_ID()).thenReturn(123);
		when(inOutLineMock.getM_Warehouse_ID()).thenReturn(234);
		when(inOutLineMock.getM_Locator_ID()).thenReturn(345);
		when(inOutLineMock.getParent()).thenReturn(inOutMock);
		
		when(inOutMock.getMovementType())
			.thenReturn(MTransaction.MOVEMENTTYPE_VendorReceipts);
		when(inOutMock.getMovementDate()).thenReturn(now);
		when(inOutMock.isSOTrx()).thenReturn(false);
		when(inOutMock.isReversal()).thenReturn(false);

		when(locatorMock.getM_Warehouse_ID()).thenReturn(234);
		
		doReturn(builderSpy).when(ruleUnderTest).getASetOfStorageTransactionInfo();
		doReturn(locatorMock).when(builderSpy).getMLocator();
		doReturn(refListMock).when(builderSpy).getRefList();
		doNothing().when(builderSpy).setFifoPolicyType();

		StorageTransactionInfo info 
			= ruleUnderTest.createTransactionInfo(inOutLineMock);
		
		assertEquals(inOutLineMock, info.getDocumentLine(),
				"DocumentLine not set as expected");
		assertEquals(MTransaction.MOVEMENTTYPE_VendorReceipts, info.getMovementType());
		assertEquals(now, info.getMovementDate());
		assertEquals(Env.ONE, info.getMovementQty());
		assertEquals(456, info.getM_Product_ID());
		assertEquals(123, info.getTransactionAttributeSetInstance_id());
		assertEquals(234, info.getTransactionWarehouse_id());
		assertEquals(345, info.getTransactionLocator_id());
		assertEquals(0, info.getOrderWarehouse_id());
		assertEquals(0, info.getOrderAttributeSetInstance_id());
		assertEquals(0, info.getOrderMPolicyTicket_id());
		assertEquals(false, info.isSOTrx());
		assertEquals(true, info.isCreateMaterialAllocations());
		assertEquals(true, info.isProcessMA());
		
	}

	@Test
	void createTransactionInfo_reverasal() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		MInOut inOutMock = mock(MInOut.class);
		MLocator locatorMock = mock(MLocator.class);
		MRefList refListMock = mock(MRefList.class);
		StorageTransactionInfoBuilder builderSpy 
			= spy(StorageTransactionInfoBuilder.class);
		
		when(inOutLineMock.get_ID()).thenReturn(100000);		
		when(inOutLineMock.getC_OrderLine_ID()).thenReturn(0);
		when(inOutLineMock.getMovementQty()).thenReturn(Env.ONE);
		when(inOutLineMock.getM_Product_ID()).thenReturn(456);
		when(inOutLineMock.getM_AttributeSetInstance_ID()).thenReturn(123);
		when(inOutLineMock.getM_Warehouse_ID()).thenReturn(234);
		when(inOutLineMock.getM_Locator_ID()).thenReturn(345);
		when(inOutLineMock.getParent()).thenReturn(inOutMock);
		
		when(inOutMock.getMovementType())
			.thenReturn(MTransaction.MOVEMENTTYPE_VendorReceipts);
		when(inOutMock.getMovementDate()).thenReturn(now);
		when(inOutMock.isSOTrx()).thenReturn(false);
		when(inOutMock.isReversal()).thenReturn(true);

		when(locatorMock.getM_Warehouse_ID()).thenReturn(234);
		
		doReturn(builderSpy).when(ruleUnderTest).getASetOfStorageTransactionInfo();
		doReturn(locatorMock).when(builderSpy).getMLocator();
		doReturn(refListMock).when(builderSpy).getRefList();
		doNothing().when(builderSpy).setFifoPolicyType();

		StorageTransactionInfo info 
			= ruleUnderTest.createTransactionInfo(inOutLineMock);
		
		assertEquals(inOutLineMock, info.getDocumentLine(),
				"DocumentLine not set as expected");
		assertEquals(MTransaction.MOVEMENTTYPE_VendorReceipts, info.getMovementType());
		assertEquals(now, info.getMovementDate());
		assertEquals(Env.ONE, info.getMovementQty());
		assertEquals(456, info.getM_Product_ID());
		assertEquals(123, info.getTransactionAttributeSetInstance_id());
		assertEquals(234, info.getTransactionWarehouse_id());
		assertEquals(345, info.getTransactionLocator_id());
		assertEquals(0, info.getOrderWarehouse_id());
		assertEquals(0, info.getOrderAttributeSetInstance_id());
		assertEquals(0, info.getOrderMPolicyTicket_id());
		assertEquals(false, info.isSOTrx());
		assertEquals(false, info.isCreateMaterialAllocations());
		assertEquals(true, info.isProcessMA());
		
	}
	@Test
	void createTransactionInfo_withOrderLine() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		MInOut inOutMock = mock(MInOut.class);
		
		MLocator locatorMock = mock(MLocator.class);
		MRefList refListMock = mock(MRefList.class);
		MOrderLine orderLineMock = mock(MOrderLine.class);
		
		StorageTransactionInfoBuilder builderSpy = 
				spy(StorageTransactionInfoBuilder.class);
		
		when(inOutLineMock.get_ID()).thenReturn(100000);
		when(inOutLineMock.getC_OrderLine_ID()).thenReturn(99);
		when(inOutLineMock.getC_OrderLine()).thenReturn(orderLineMock);
		when(inOutLineMock.getMovementQty()).thenReturn(Env.ONE);
		when(inOutLineMock.getM_Product_ID()).thenReturn(456);
		when(inOutLineMock.getM_AttributeSetInstance_ID()).thenReturn(123);
		when(inOutLineMock.getM_Warehouse_ID()).thenReturn(234);
		when(inOutLineMock.getM_Locator_ID()).thenReturn(345);
		when(inOutLineMock.getParent()).thenReturn(inOutMock);
		
		when(inOutMock.getMovementType())
			.thenReturn(MTransaction.MOVEMENTTYPE_VendorReceipts);
		when(inOutMock.getMovementDate()).thenReturn(now);
		when(inOutMock.isSOTrx()).thenReturn(false);
		when(inOutMock.isReversal()).thenReturn(false);
		
		when(locatorMock.getM_Warehouse_ID()).thenReturn(234);
		
		when(orderLineMock.getM_Warehouse_ID()).thenReturn(1);
		when(orderLineMock.getM_AttributeSetInstance_ID()).thenReturn(2);
		when(orderLineMock.getM_MPolicyTicket_ID()).thenReturn(3);
		
		doReturn(builderSpy).when(ruleUnderTest)
		.getASetOfStorageTransactionInfo();
		
		doReturn(locatorMock).when(builderSpy).getMLocator();
		doReturn(refListMock).when(builderSpy).getRefList();
		doNothing().when(builderSpy).setFifoPolicyType();

		ruleUnderTest.setStorageTransactionInfoBuilder(builderSpy);
		StorageTransactionInfo info = 
				ruleUnderTest.createTransactionInfo(inOutLineMock);
		
		assertEquals(inOutLineMock, info.getDocumentLine(),
				"DocumentLine not set as expected");
		assertEquals(MTransaction.MOVEMENTTYPE_VendorReceipts, 
				info.getMovementType());
		assertEquals(now, info.getMovementDate());
		assertEquals(Env.ONE, info.getMovementQty());
		assertEquals(456, info.getM_Product_ID());
		assertEquals(123, info.getTransactionAttributeSetInstance_id());
		assertEquals(234, info.getTransactionWarehouse_id());
		assertEquals(345, info.getTransactionLocator_id());
		assertEquals(1, info.getOrderWarehouse_id());
		assertEquals(2, info.getOrderAttributeSetInstance_id());
		assertEquals(3, info.getOrderMPolicyTicket_id());
		assertEquals(false, info.isSOTrx());
		assertEquals(true, info.isCreateMaterialAllocations());
		assertEquals(true, info.isProcessMA());
		
	}

	
	@Test
	void allocateMaterialAndAssignTickets_NoCreate() {

		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.isCreateMaterialAllocations()).thenReturn(false);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		doCallRealMethod().when(ruleUnderTest).allocateMaterialAndAssignTickets();
		when(ruleUnderTest.getStorageTransactionInfo()).thenReturn(infoMock);
		
		ruleUnderTest.allocateMaterialAndAssignTickets();

		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verify(ruleUnderTest, times(1)).getStorageTransactionInfo();
		verifyNoMoreInteractions(ruleUnderTest);
		
		verify(infoMock, times(1)).isCreateMaterialAllocations();
		verifyNoMoreInteractions(infoMock);
		
	}

	@Test
	void allocateMaterialAndAssignTickets_RMA() {

		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.isCreateMaterialAllocations()).thenReturn(true);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		doCallRealMethod().when(ruleUnderTest).allocateMaterialAndAssignTickets();
		doReturn(true).when(ruleUnderTest).applyRMAMaterialPolicyRules();
		doNothing().when(ruleUnderTest).deleteExistingMaterialAllocations();
		when(ruleUnderTest.getStorageTransactionInfo()).thenReturn(infoMock);
				
		ruleUnderTest.allocateMaterialAndAssignTickets();

		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verify(ruleUnderTest).getStorageTransactionInfo();
		verify(ruleUnderTest).deleteExistingMaterialAllocations();
		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verify(ruleUnderTest, never()).allocateMaterial();
		verifyNoMoreInteractions(ruleUnderTest);
		
		verify(infoMock).isCreateMaterialAllocations();
		verifyNoMoreInteractions(infoMock);

	}

	@Test
	void allocateMaterialAndAssignTickets_NoRMA() {

		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.isCreateMaterialAllocations()).thenReturn(true);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		doCallRealMethod().when(ruleUnderTest).allocateMaterialAndAssignTickets();
		doReturn(false).when(ruleUnderTest).applyRMAMaterialPolicyRules();
		doNothing().when(ruleUnderTest).deleteExistingMaterialAllocations();
		when(ruleUnderTest.getStorageTransactionInfo()).thenReturn(infoMock);
				
		ruleUnderTest.allocateMaterialAndAssignTickets();

		verify(ruleUnderTest).allocateMaterialAndAssignTickets();
		verify(ruleUnderTest).getStorageTransactionInfo();
		verify(ruleUnderTest).deleteExistingMaterialAllocations();
		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verify(ruleUnderTest).allocateMaterial();
		verifyNoMoreInteractions(ruleUnderTest);
		
		verify(infoMock).isCreateMaterialAllocations();
		verifyNoMoreInteractions(infoMock);

	}

	@Test
	void allocateMaterialAndAssignTickets_RMATilt() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLine originalIOLineMock = mock(MInOutLine.class);

		MInOutLineMA originalLineMA = mock(MInOutLineMA.class);
		
		List<MInOutLineMA> mas = new ArrayList<>();
		mas.add(originalLineMA);
		
		MRMALine rmaLineMock = mock(MRMALine.class);
		when(rmaLineMock.getM_InOutLine()).thenReturn(originalIOLineMock);

		when(inOutLineMock.getM_RMALine()).thenReturn(rmaLineMock);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = Env.ONE;
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_CustomerReturns;
		when(ruleUnderTest.isVendorReturn(anyString())).thenReturn(false);
		when(ruleUnderTest.isCustomerReturn(anyString())).thenReturn(false);
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertThrows(AdempiereException.class, () -> {
				ruleUnderTest.applyRMAMaterialPolicyRules();
		});
		
	}

	@Test
	void applyRMAMaterialPolicyRules_noRMA0() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(0);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();
				
		assertFalse(ruleUnderTest.applyRMAMaterialPolicyRules(),
				"With no RMA Line, should return false");

		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verifyNoMoreInteractions(ruleUnderTest);
		
	}

	@Test
	void applyRMAMaterialPolicyRules_noRMA_1() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(-1);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();
				
		assertFalse(ruleUnderTest.applyRMAMaterialPolicyRules(),
				"With no RMA Line, should return false");

		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verifyNoMoreInteractions(ruleUnderTest);
				
	}

	@Test
	void applyRMAMaterialPolicyRules_VendorReturn() {

		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLine originalIOLineMock = mock(MInOutLine.class);
		when(originalIOLineMock.getM_MPolicyTicket_ID()).thenReturn(2);

		MInOutLineMA returnLineMAMock = mock(MInOutLineMA.class);
		
		MRMALine rmaLineMock = mock(MRMALine.class);
		when(rmaLineMock.getM_InOutLine()).thenReturn(originalIOLineMock);

		when(inOutLineMock.getM_RMALine()).thenReturn(rmaLineMock);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = Env.ONE;
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_VendorReturns;
		when(ruleUnderTest.getMInOutLineMA(anyInt(), any(BigDecimal.class)))
		.thenReturn(returnLineMAMock);
		when(ruleUnderTest.isVendorReturn(anyString())).thenReturn(true);
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertTrue(ruleUnderTest.applyRMAMaterialPolicyRules(),
				"Should return true");
		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verify(ruleUnderTest).getMInOutLineMA(idCaptor.capture(), bdCaptor.capture());
		verify(ruleUnderTest).isVendorReturn(movementTypeCaptor.capture());
		assertEquals(2, idCaptor.getValue().intValue(),
				"Wrong material policy ticket id");
		assertEquals(Env.ONE, bdCaptor.getValue(),
				"Wrong material qty");
		assertEquals(MTransaction.MOVEMENTTYPE_VendorReturns, 
				movementTypeCaptor.getValue(),
				"Wrong movement type");

		verify(returnLineMAMock).setMovementType(movementTypeCaptor.capture());
		assertEquals(MTransaction.MOVEMENTTYPE_VendorReturns, 
				movementTypeCaptor.getValue(), "Wrong movement type");
		verify(returnLineMAMock).saveEx();
		
		verify(inOutLineMock).getM_RMALine();
		verify(rmaLineMock).getM_InOutLine();
		verify(originalIOLineMock).getM_MPolicyTicket_ID();
		
		verifyNoMoreInteractions(ruleUnderTest);
		
	}

	@Test
	void applyRMAMaterialPolicyRules_CustomerReturn() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLine originalIOLineMock = mock(MInOutLine.class);

		MInOutLineMA originalLineMA = mock(MInOutLineMA.class);
		when(originalLineMA.getMovementQty()).thenReturn(Env.ONE);
		
		List<MInOutLineMA> mas = new ArrayList<>();
		mas.add(originalLineMA);
		
		MRMALine rmaLineMock = mock(MRMALine.class);
		when(rmaLineMock.getM_InOutLine()).thenReturn(originalIOLineMock);

		when(inOutLineMock.getM_RMALine()).thenReturn(rmaLineMock);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = Env.ONE;
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_CustomerReturns;
		when(ruleUnderTest.isVendorReturn(anyString())).thenReturn(false);
		when(ruleUnderTest.isCustomerReturn(anyString())).thenReturn(true);
		when(ruleUnderTest.getOriginalMaterialAllocations(any(MInOutLine.class)))
				.thenReturn(mas);
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertTrue(ruleUnderTest.applyRMAMaterialPolicyRules(),
				"Should return true");
		
		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verify(ruleUnderTest).isVendorReturn(movementTypeCaptor.capture());
		assertEquals(MTransaction.MOVEMENTTYPE_CustomerReturns, 
				movementTypeCaptor.getValue(),
				"Wrong movement type");

		verify(inOutLineMock).getM_RMALine();
		verify(rmaLineMock).getM_InOutLine();
		verify(ruleUnderTest).createNewMaterialAllocationAndAttachToRMA(lineMACaptor.capture(), 
									bdCaptor.capture());
		verifyNoMoreInteractions(ruleUnderTest);
		
		assertEquals(originalLineMA, lineMACaptor.getValue(), "Incorrect lineMA passed");
		assertEquals(Env.ONE, bdCaptor.getValue());
		
	}

	@Test
	void applyRMAMaterialPolicyRules_CustomerReturnWithZeroQty() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = Env.ZERO;
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_CustomerReturns;
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertFalse(ruleUnderTest.applyRMAMaterialPolicyRules(),
				"Should return true");
		
		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verifyNoMoreInteractions(ruleUnderTest);
		
	}
	@Test
	void applyRMAMaterialPolicyRules_CustomerReturnWithMultipleMALines() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLine originalIOLineMock = mock(MInOutLine.class);

		MInOutLineMA originalLineMA1 = mock(MInOutLineMA.class);
		when(originalLineMA1.getMovementQty()).thenReturn(Env.ONE);
		
		MInOutLineMA originalLineMA2 = mock(MInOutLineMA.class);
		when(originalLineMA2.getMovementQty()).thenReturn(BigDecimal.valueOf(2));
		
		List<MInOutLineMA> mas = new ArrayList<>();
		mas.add(originalLineMA1);
		mas.add(originalLineMA2);
		
		MRMALine rmaLineMock = mock(MRMALine.class);
		when(rmaLineMock.getM_InOutLine()).thenReturn(originalIOLineMock);

		when(inOutLineMock.getM_RMALine()).thenReturn(rmaLineMock);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = BigDecimal.valueOf(3);
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_CustomerReturns;
		when(ruleUnderTest.isVendorReturn(anyString())).thenReturn(false);
		when(ruleUnderTest.isCustomerReturn(anyString())).thenReturn(true);
		when(ruleUnderTest.getOriginalMaterialAllocations(any(MInOutLine.class)))
				.thenReturn(mas);
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertTrue(ruleUnderTest.applyRMAMaterialPolicyRules(),
				"Should return true");
		
		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verify(ruleUnderTest).isVendorReturn(movementTypeCaptor.capture());
		assertEquals(MTransaction.MOVEMENTTYPE_CustomerReturns, 
				movementTypeCaptor.getValue(),
				"Wrong movement type");

		verify(inOutLineMock).getM_RMALine();
		verify(rmaLineMock).getM_InOutLine();
		verify(ruleUnderTest, times(2))
			.createNewMaterialAllocationAndAttachToRMA(lineMACaptor.capture(), 
					bdCaptor.capture());
		verifyNoMoreInteractions(ruleUnderTest);
		
		List<MInOutLineMA> usedList = lineMACaptor.getAllValues();
		assertEquals(originalLineMA1, usedList.get(0), "Incorrect lineMA passed");
		assertEquals(originalLineMA2, usedList.get(1), "Incorrect lineMA passed");
		
		List<BigDecimal> qtyList = bdCaptor.getAllValues();
		assertEquals(Env.ONE, qtyList.get(0));
		assertEquals(BigDecimal.valueOf(2), qtyList.get(1));
		
	}

	@Test
	void applyRMAMaterialPolicyRules_CustomerReturningLessThanAFullLine() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLine originalIOLineMock = mock(MInOutLine.class);

		MInOutLineMA originalLineMA = mock(MInOutLineMA.class);
		when(originalLineMA.getMovementQty()).thenReturn(BigDecimal.valueOf(3));
		
		List<MInOutLineMA> mas = new ArrayList<>();
		mas.add(originalLineMA);
		
		MRMALine rmaLineMock = mock(MRMALine.class);
		when(rmaLineMock.getM_InOutLine()).thenReturn(originalIOLineMock);

		when(inOutLineMock.getM_RMALine()).thenReturn(rmaLineMock);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = Env.ONE;
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_CustomerReturns;
		when(ruleUnderTest.isVendorReturn(anyString())).thenReturn(false);
		when(ruleUnderTest.isCustomerReturn(anyString())).thenReturn(true);
		when(ruleUnderTest.getOriginalMaterialAllocations(any(MInOutLine.class)))
				.thenReturn(mas);
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertTrue(ruleUnderTest.applyRMAMaterialPolicyRules(),
				"Should return true");
		
		verify(ruleUnderTest).applyRMAMaterialPolicyRules();
		verify(ruleUnderTest).isVendorReturn(movementTypeCaptor.capture());
		assertEquals(MTransaction.MOVEMENTTYPE_CustomerReturns, 
				movementTypeCaptor.getValue(),
				"Wrong movement type");

		verify(inOutLineMock).getM_RMALine();
		verify(rmaLineMock).getM_InOutLine();
		verify(ruleUnderTest)
			.createNewMaterialAllocationAndAttachToRMA(lineMACaptor.capture(), 
					bdCaptor.capture());
		verifyNoMoreInteractions(ruleUnderTest);
		
		assertEquals(originalLineMA, lineMACaptor.getValue(), "Incorrect lineMA passed");
		assertEquals(Env.ONE, bdCaptor.getValue());
		
	}

	@Test
	void applyRMAMaterialPolicyRules_CustomerReturningMismatchOfQuantities2() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLine originalIOLineMock = mock(MInOutLine.class);

		MInOutLineMA originalLineMA = mock(MInOutLineMA.class);
		when(originalLineMA.getMovementQty()).thenReturn(Env.ZERO);
		
		List<MInOutLineMA> mas = new ArrayList<>();
		mas.add(originalLineMA);
		
		MRMALine rmaLineMock = mock(MRMALine.class);
		when(rmaLineMock.getM_InOutLine()).thenReturn(originalIOLineMock);

		when(inOutLineMock.getM_RMALine()).thenReturn(rmaLineMock);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = BigDecimal.valueOf(2);
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_CustomerReturns;
		when(ruleUnderTest.isVendorReturn(anyString())).thenReturn(false);
		when(ruleUnderTest.isCustomerReturn(anyString())).thenReturn(true);
		when(ruleUnderTest.getOriginalMaterialAllocations(any(MInOutLine.class)))
				.thenReturn(mas);
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.applyRMAMaterialPolicyRules();
		});
				
	}

	@Test
	void applyRMAMaterialPolicyRules_CustomerReturningMismatchOfQuantities() {

		
		when(inOutLineMock.getM_RMALine_ID()).thenReturn(1);
		
		MInOutLine originalIOLineMock = mock(MInOutLine.class);

		MInOutLineMA originalLineMA = mock(MInOutLineMA.class);
		when(originalLineMA.getMovementQty()).thenReturn(Env.ONE);
		
		List<MInOutLineMA> mas = new ArrayList<>();
		mas.add(originalLineMA);
		
		MRMALine rmaLineMock = mock(MRMALine.class);
		when(rmaLineMock.getM_InOutLine()).thenReturn(originalIOLineMock);

		when(inOutLineMock.getM_RMALine()).thenReturn(rmaLineMock);
		
		MInOutLineStorageRule ruleUnderTest = mock(MInOutLineStorageRule.class);
		ruleUnderTest.inOutLine = inOutLineMock;
		ruleUnderTest.movementQty = BigDecimal.valueOf(2);
		ruleUnderTest.movementType = MTransaction.MOVEMENTTYPE_CustomerReturns;
		when(ruleUnderTest.isVendorReturn(anyString())).thenReturn(false);
		when(ruleUnderTest.isCustomerReturn(anyString())).thenReturn(true);
		when(ruleUnderTest.getOriginalMaterialAllocations(any(MInOutLine.class)))
				.thenReturn(mas);
		
		doCallRealMethod().when(ruleUnderTest).applyRMAMaterialPolicyRules();

		assertThrows(AdempiereException.class, () -> {
			ruleUnderTest.applyRMAMaterialPolicyRules();
		});
				
	}

	@Test
	void isCustomerReturn() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		assertTrue(ruleUnderTest
						.isCustomerReturn(MTransaction.MOVEMENTTYPE_CustomerReturns),
				"Should return true");
		assertFalse(ruleUnderTest
						.isCustomerReturn(MTransaction.MOVEMENTTYPE_VendorReturns),
				"Should return false");

	}
	
	@Test
	void isVendorReturn() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		assertFalse(ruleUnderTest
						.isVendorReturn(MTransaction.MOVEMENTTYPE_CustomerReturns),
				"Should return false");
		assertTrue(ruleUnderTest
						.isVendorReturn(MTransaction.MOVEMENTTYPE_VendorReturns),
				"Should return true");

	}
	
	@Test
	void setStorageRelatedFieldsInModel_orderLinenull() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);		
		ruleUnderTest.orderLine = null;
		
		ruleUnderTest.setStorageRelatedFieldsInModel();
		
		verify(ruleUnderTest).setStorageRelatedFieldsInModel();
		verifyNoMoreInteractions(ruleUnderTest);
		
	}

	@Test
	void setStorageRelatedFieldsInModel_NotisSOTrx() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.orderLine = orderLineMock;
		ruleUnderTest.isSOTrx = false;

		ruleUnderTest.setStorageRelatedFieldsInModel();
		
		verify(ruleUnderTest).setStorageRelatedFieldsInModel();
		verifyNoMoreInteractions(ruleUnderTest);
		verifyNoInteractions(orderLineMock);
		
	}

	@Test
	void setStorageRelatedFieldsInModel_isSOTrx() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		when(orderLineMock.getQtyDelivered()).thenReturn(Env.ZERO);
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.orderLine = orderLineMock;
		ruleUnderTest.isSOTrx = true;
		ruleUnderTest.movementQty = Env.ONE.negate();
		
		ruleUnderTest.setStorageRelatedFieldsInModel();
		
		verify(ruleUnderTest).setStorageRelatedFieldsInModel();
		verifyNoMoreInteractions(ruleUnderTest);
		verify(orderLineMock).getQtyDelivered();
		verify(orderLineMock).setQtyDelivered(bdCaptor.capture());
		verify(orderLineMock).saveEx();		
		assertEquals(Env.ONE, bdCaptor.getValue(),
				"Qty delivered not set correctly");
		
	}

	@Test
	void clearStorageRelatedFieldsInModel() {

		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		MOrderLine orderLineMock = mock(MOrderLine.class);
		ruleUnderTest.orderLine = orderLineMock;

		ruleUnderTest.clearStorageRelatedFieldsInModel();

		verify(orderLineMock).setQtyReserved(bdCaptor.capture());
		assertEquals(Env.ZERO, bdCaptor.getValue(),
				"Should have set to zero");
		
	}

	@Test
	void clearStorageRelatedFieldsInModel_noOrder() {

		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.orderLine = null;

		ruleUnderTest.clearStorageRelatedFieldsInModel();

		verify(ruleUnderTest).clearStorageRelatedFieldsInModel();
		verifyNoMoreInteractions(ruleUnderTest);
		
	}

	@Test
	void getChangeInQtyOnHand() {

		
		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.getMovementType()).thenReturn(MOVEMENTTYPE_VendorReceipts);
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.info = infoMock;		
		ruleUnderTest.movementQty = Env.ONE;
		
		assertEquals(Env.ONE, ruleUnderTest.getChangeInQtyOnHand(),
				"Should return the inOutLine movement qty");
		
	}

	@Test
	void getChangeInQtyOnHand_vendorRMA() {

		
		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.getMovementType()).thenReturn(MOVEMENTTYPE_VendorReturns);
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.info = infoMock;		
		ruleUnderTest.movementQty = Env.ONE;
		
		assertEquals(Env.ONE.negate(), ruleUnderTest.getChangeInQtyOnHand(),
				"For vendor return, should return the negative of inOutLine movement qty");
		
	}

	@Test
	void getChangeInQtyOrdered_isSOTrx() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.isSOTrx = true;
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOrdered(),
				"Sales transactions should have no change in qty ordered");
		
	}

	@Test
	void getChangeInQtyOrdered_notIsSOTrxOrderLineNull() {
		
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.isSOTrx = false;
		ruleUnderTest.movementQty = Env.ONE.negate();
		ruleUnderTest.orderLine = null;
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOrdered(),
				"If orderLine is null, there is no order to change the qty "
				+ "ordered against.");
		
	}

	@Test
	void getChangeInQtyOrdered_notIsSOTrxOrderLineNotNullNotRMA() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.getMovementType()).thenReturn(MOVEMENTTYPE_VendorReceipts);
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.info = infoMock;		
		ruleUnderTest.isSOTrx = false;
		ruleUnderTest.movementQty = Env.ONE.negate();
		ruleUnderTest.orderLine = orderLineMock;
		
		assertEquals(Env.ONE, ruleUnderTest.getChangeInQtyOrdered(),
				"Non sales transactions should return negative of movment qty");
		
	}

	@Test
	void getChangeInQtyOrdered_notIsSOTrxOrderLineNotNullRMA() {
		
		MOrderLine orderLineMock = mock(MOrderLine.class);
		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.getMovementType()).thenReturn(MOVEMENTTYPE_VendorReturns);
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.info = infoMock;		
		ruleUnderTest.isSOTrx = false;
		ruleUnderTest.movementQty = Env.ONE;
		ruleUnderTest.orderLine = orderLineMock;
		
		assertEquals(Env.ONE, ruleUnderTest.getChangeInQtyOrdered(),
				"Non sales return transactions should return movment qty");
		
	}

	@Test
	void getChangeInQtyReserved_isSOTrxOrderLineNull() {

		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.isSOTrx = true;
		ruleUnderTest.movementQty = Env.ONE.negate();
		ruleUnderTest.orderLine = null;
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyOrdered(),
				"If orderLine is null, there is no order to change the qty ordered against.");
		
	}

	@Test
	void getChangeInQtyReserved_isSOTrxOrderLineNotNullNotRMA() {

		MOrderLine orderLineMock = mock(MOrderLine.class);
		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.getMovementType()).thenReturn(MOVEMENTTYPE_CustomerShipment);
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.info = infoMock;		
		ruleUnderTest.isSOTrx = true;
		ruleUnderTest.movementQty = Env.ONE.negate();
		ruleUnderTest.orderLine = orderLineMock;
		
		assertEquals(Env.ONE, ruleUnderTest.getChangeInQtyReserved(),
				"Sales transactions should return negative of movment qty");
	
	}

	@Test
	void getChangeInQtyReserved_isSOTrxOrderLineNotNullRMA() {

		MOrderLine orderLineMock = mock(MOrderLine.class);
		StorageTransactionInfo infoMock = mock(StorageTransactionInfo.class);
		when(infoMock.getMovementType()).thenReturn(MOVEMENTTYPE_CustomerReturns);
		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.info = infoMock;
		
		ruleUnderTest.isSOTrx = true;
		ruleUnderTest.movementQty = Env.ONE;
		ruleUnderTest.orderLine = orderLineMock;
		
		assertEquals(Env.ONE, ruleUnderTest.getChangeInQtyReserved(),
				"Sales RMA transactions should return movment qty");
	
	}

	@Test
	void getChangeInQtyReserved_notIsSOTrxOrderLineNull() {

		MInOutLineStorageRule ruleUnderTest = spy(MInOutLineStorageRule.class);
		ruleUnderTest.isSOTrx = false;
		ruleUnderTest.orderLine = null;
		ruleUnderTest.movementQty = Env.ONE.negate();
		
		assertEquals(Env.ZERO, ruleUnderTest.getChangeInQtyReserved(),
				"Non sales transactions should return zero");
	}

}
