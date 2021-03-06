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
package org.eevolution.model;

import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Approved;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Closed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Completed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Drafted;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_InProgress;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Invalid;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_NotApproved;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Reversed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Unknown;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Voided;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_WaitingConfirmation;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_WaitingPayment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MTransaction;
import org.compiere.model.ModelValidationEngine;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderReceipt")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderReceipt_BasicMethods extends CommonUnitTestSetup {

	private final ModelValidationEngine modelValidationEngine = mock(ModelValidationEngine.class);
	private final MPPOrderReceipt ppOrderReceiptMock = mock(MPPOrderReceipt.class);
	private final MPPOrder ppOrderMock = mock(MPPOrder.class);
	private final Timestamp today = TimeUtil.getDay(System.currentTimeMillis());
	private final CLogger log = mock(CLogger.class);
	
	@BeforeEach
	void setUp() throws Exception {

		when(ppOrderMock.getM_Product_ID()).thenReturn(123);

		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);
		when(ppOrderReceiptMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		
	}
	
	private void assertAddDescriptionWorks(String currentDescription) {
		
		doCallRealMethod().when(ppOrderReceiptMock).addDescription(anyString());
		when(ppOrderReceiptMock.getDescription()).thenReturn(currentDescription);
		
		ppOrderReceiptMock.addDescription("New Description");
		verify(ppOrderReceiptMock).getDescription();
		verify(ppOrderReceiptMock).setDescription(descCaptor.capture());
		verifyNoMoreInteractions(ppOrderMock);
		
		String expectedDescription = 
				Optional.ofNullable(currentDescription).orElse("") 
					+ getSeparator(currentDescription) 
					+ "New Description";
		assertEquals(expectedDescription, descCaptor.getValue(),
				"Description should not have changed");
	}

	private String getSeparator(String currentDescription) {
		
		if (currentDescription == null || currentDescription.isEmpty())
			return "";
		else 
			return " | ";
	}

	@Captor ArgumentCaptor<String> descCaptor;
	
	@Captor 
	ArgumentCaptor<String> processActionCaptor;
	@Captor 
	ArgumentCaptor<String> docActionCaptor;

	@Captor 
	ArgumentCaptor<String> logCaptor;

	@Captor
	ArgumentCaptor<String> stringCaptor;

	@Test
	void addDescription_null() {

		ppOrderReceiptMock.addDescription((String) null);
		verify(ppOrderReceiptMock, never()).setDescription(anyString());

	}

	@Test
	void addDescription_empty() {

		doCallRealMethod().when(ppOrderReceiptMock).addDescription(anyString());

		ppOrderReceiptMock.addDescription("");
		verify(ppOrderReceiptMock, never()).setDescription(anyString());

	}

	@Test
	void addDescription_currentNull() {

		doCallRealMethod().when(ppOrderReceiptMock).addDescription(anyString());
		when(ppOrderReceiptMock.getDescription()).thenReturn(null);

		ppOrderReceiptMock.addDescription("NewDescription");
		verify(ppOrderReceiptMock).setDescription(stringCaptor.capture());

		assertEquals("NewDescription", stringCaptor.getValue(),
				"Description not set as expected");

	}

	@Test
	void addDescription_notNull() {

		doCallRealMethod().when(ppOrderReceiptMock).addDescription(anyString());
		when(ppOrderReceiptMock.getDescription()).thenReturn("CurrentDescription");

		ppOrderReceiptMock.addDescription("NewDescription");
		verify(ppOrderReceiptMock).setDescription(stringCaptor.capture());

		assertEquals("CurrentDescription | NewDescription", stringCaptor.getValue(),
				"Description not set as expected");

	}

	
	@ParameterizedTest
	@NullAndEmptySource
	void testMPPOrderReceipt_addDescription(String currentDescription) {

		assertAddDescriptionWorks(currentDescription);

	}

	@ParameterizedTest 
	@ValueSource(strings = {
				DOCSTATUS_Closed, 
				DOCSTATUS_Reversed,
				DOCSTATUS_Voided
			})
	void testMPPOrderReceipt_checkIfClosedResersedOrVoided_closed(String status) {
	
		doCallRealMethod().when(ppOrderReceiptMock).checkIfClosedReversedOrVoided();
		when(ppOrderReceiptMock.getDocStatus()).thenReturn(status);		
		assertEquals("Document Closed: " + status, 
				ppOrderReceiptMock.checkIfClosedReversedOrVoided(), 
				"Process message was not as expected");
		
	}

	@ParameterizedTest 
	@ValueSource(strings = {
			DOCSTATUS_Drafted, 
			DOCSTATUS_Invalid,
			DOCSTATUS_InProgress,
			DOCSTATUS_Approved,
			DOCSTATUS_NotApproved,
			DOCSTATUS_Completed,
			DOCSTATUS_Unknown,
			DOCSTATUS_WaitingConfirmation,
			DOCSTATUS_WaitingPayment
		})
	void testMPPOrderReceipt_checkIfClosedResersedOrVoided_notClosed(String status) {
		
		doCallRealMethod().when(ppOrderReceiptMock).checkIfClosedReversedOrVoided();
		when(ppOrderReceiptMock.getDocStatus()).thenReturn(status);		
		assertNull(ppOrderReceiptMock.checkIfClosedReversedOrVoided(),
				"If not closed, should return null");
			
	}

	@Test
	void testMPPOrderReceipt_createPDF() {
		
		doCallRealMethod().when(ppOrderReceiptMock).createPDF();
		assertNull(ppOrderReceiptMock.createPDF(), "Expected null: not implemented");
		
	}


	@ParameterizedTest 
	@ValueSource(strings = {
			DOCSTATUS_Drafted, 
			DOCSTATUS_Invalid,
			DOCSTATUS_InProgress,
			DOCSTATUS_Approved,
			DOCSTATUS_NotApproved
		}) 
	void testMPPOrderReceipt_directVoidIsPossible(String status) {
		
		doCallRealMethod().when(ppOrderReceiptMock).directVoidIsPossible();
		when(ppOrderReceiptMock.getDocStatus()).thenReturn(status);		
		assertTrue(ppOrderReceiptMock.directVoidIsPossible(),
				"Direct void should be possible");
	
	}

	@ParameterizedTest 
	@ValueSource(strings = {
			DOCSTATUS_Completed,
			DOCSTATUS_Unknown,
			DOCSTATUS_WaitingConfirmation,
			DOCSTATUS_WaitingPayment,
			DOCSTATUS_Closed, 
			DOCSTATUS_Reversed,
			DOCSTATUS_Voided
		}) 
	void testMPPOrderReceipt_directVoidIsNotPossible(String status) {
		
		doCallRealMethod().when(ppOrderReceiptMock).directVoidIsPossible();
		when(ppOrderReceiptMock.getDocStatus()).thenReturn(status);			
		assertFalse(ppOrderReceiptMock.directVoidIsPossible(),
				"Direct void should not be possible");
		
	}

	@Test
	void testMPPOrderReceipt_getApprovalAmt() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getApprovalAmt();
		assertEquals(Env.ZERO, ppOrderReceiptMock.getApprovalAmt(),
				"Should return zero");
	
	}


	@Test
	void testMPPOrderReceipt_getC_ConversionType_ID() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getC_ConversionType_ID();
		when(ppOrderReceiptMock.getDefaultConverstionType_ID()).thenReturn(1);
		assertEquals(1, ppOrderReceiptMock.getC_ConversionType_ID(), 
				"Converstion type id not set as expected");
		verify(ppOrderReceiptMock).getDefaultConverstionType_ID();
		
	}


	@Test
	void testMPPOrderReceipt_getC_Currency_ID() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getC_Currency_ID();
		MClient clientMock = mock(MClient.class);
		when(clientMock.getC_Currency_ID()).thenReturn(1);
		ppOrderReceiptMock.client = clientMock;
		
		assertEquals(1, ppOrderReceiptMock.getC_Currency_ID(), "Currency should match client");
				
	}


	@Test
	void testMPPOrderReceipt_getDateAcct() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getDateAcct();
		when(ppOrderReceiptMock.getMovementDate()).thenReturn(today);
		
		assertEquals(today, ppOrderReceiptMock.getDateAcct(), "Date should equal movementDate");
		verify(ppOrderReceiptMock, times(1)).getMovementDate();
		
	}


	@Test
	void testMPPOrderReceipt_getDoc_User_ID() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getDoc_User_ID();
		when(ppOrderReceiptMock.getParent()).thenReturn(null,ppOrderMock);
		
		when(ppOrderMock.getPlanner_ID()).thenReturn(1);
		
		assertEquals(0, ppOrderReceiptMock.getDoc_User_ID(),
				"With no parent order, should return 0");
		assertEquals(1, ppOrderReceiptMock.getDoc_User_ID(),
				"With parent order, should return parent planner id");
		
	}


	@Test
	void testMPPOrderReceipt_getDocumentInfo() {
	
		MDocType docTypeMock = mock(MDocType.class);
		when(docTypeMock.getName()).thenReturn("Manufacturing Order Receipt");
	
		doCallRealMethod().when(ppOrderReceiptMock).getDocumentInfo();
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		when(ppOrderReceiptMock.getDocumentNo()).thenReturn("1000000");
		
		assertEquals("Manufacturing Order Receipt 1000000", ppOrderReceiptMock.getDocumentInfo(),
				"Document Info not as expected");
	}

	@Test
	void testMPPOrderReceipt_getM_AttributeSetInstanceTo_ID() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getM_AttributeSetInstanceTo_ID();
		assertEquals(0, ppOrderReceiptMock.getM_AttributeSetInstanceTo_ID(), 
				"No Attribute Set Instance 'To' is used");
		
	}


	@Test
	void testMPPOrderReceipt_getM_LocatorTo_ID() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getM_LocatorTo_ID();
		assertEquals(0, ppOrderReceiptMock.getM_LocatorTo_ID(), "No locator 'To' is used");
		
	}

	@Test
	void testMPPOrderReceipt_getM_Warehouse_IDWithLocator() {
	
		MLocator locatorMock = mock(MLocator.class);
		when(locatorMock.getM_Warehouse_ID()).thenReturn(123);
	
		doCallRealMethod().when(ppOrderReceiptMock).getM_Warehouse_ID();		
		when(ppOrderReceiptMock.getM_Locator()).thenReturn(locatorMock);
		when(ppOrderReceiptMock.getM_Locator_ID()).thenReturn(1);
		
		assertEquals(123, ppOrderReceiptMock.getM_Warehouse_ID(), 
				"Warehouse should match the locator warehouse");
		verify(ppOrderReceiptMock, times(1)).getM_Locator_ID();
		verify(ppOrderReceiptMock, never()).getParent();
	
	}


	@Test
	void testMPPOrderReceipt_getM_Warehouse_IDNoLocatorNoParent() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getM_Warehouse_ID();
	
		when(ppOrderReceiptMock.getM_Locator_ID()).thenReturn(0);
		when(ppOrderReceiptMock.getParent()).thenReturn(null);
	
		assertEquals(0, ppOrderReceiptMock.getM_Warehouse_ID(), 
				"With no locator and no parent, should be zero");
		verify(ppOrderReceiptMock, times(1)).getM_Locator_ID();
		verify(ppOrderReceiptMock, times(1)).getParent();
	
	}


	@Test
	void testMPPOrderReceipt_getM_Warehouse_IDNoLocatorWithParent() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getM_Warehouse_ID();
		when(ppOrderReceiptMock.getM_Locator_ID()).thenReturn(0);
		when(ppOrderReceiptMock.getParent()).thenReturn(ppOrderMock);
		
		when(ppOrderMock.getM_Warehouse_ID()).thenReturn(234);
	
		assertEquals(234, ppOrderReceiptMock.getM_Warehouse_ID(), 
				"With no locator but a parent, warehouse should match the parent");
		verify(ppOrderReceiptMock, times(1)).getM_Locator_ID();
		verify(ppOrderReceiptMock, times(1)).getParent();
		verify(ppOrderMock, times(1)).getM_Warehouse_ID();
	
	}


	@Test
	void testMPPOrderReceipt_getModelValidationEngine() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getModelValidationEngine();
		ppOrderReceiptMock.modelValidationEngine = modelValidationEngine;
		
		assertEquals(modelValidationEngine, ppOrderReceiptMock.getModelValidationEngine(),
				"ModelValidationEngine not set as expected");
	
		// Case of null left of integration tests
		
	}


	@Test
	void testMPPOrderReceipt_getMovementType() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getMovementType();
		assertEquals(MTransaction.MOVEMENTTYPE_ProductionPlus, 
				ppOrderReceiptMock.getMovementType(),
				"MovementType should be fixed");
		
	}


	@Test
	void testMPPOrderReceipt_getParentNoParentNoID() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getParent();
		when(ppOrderReceiptMock.getPP_Order_ID()).thenReturn(0);
		
		assertNull(ppOrderReceiptMock.getParent(), "No ID, parent should be null");
						
	}


	@Test
	void testMPPOrderReceipt_getParentWithParent() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getParent();
		doCallRealMethod().when(ppOrderReceiptMock).setParent(ppOrderMock);
			
		ppOrderReceiptMock.setParent(ppOrderMock);
		assertEquals(ppOrderMock, ppOrderReceiptMock.getParent(),
				"With a parent defined, should return the parent");
						
	}


	@Test
	void testMPPOrderReceipt_getPriceActual() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getPriceActual();
		assertEquals(Env.ZERO, ppOrderReceiptMock.getPriceActual(), "Price Actual should be zero");
		
	}


	@Test
	void testMPPOrderReceipt_getPriceActualCurrency() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getPriceActualCurrency();
		assertEquals(Env.ZERO, ppOrderReceiptMock.getPriceActualCurrency(),
				"Price Actual Currency should be zero");
		
	}


	@Test
	void testMPPOrderReceipt_getProcessMsg() {
	
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		ppOrderReceiptMock.processMsg = "test";
		assertEquals("test", ppOrderReceiptMock.getProcessMsg(), "processMsg not set as expected");
	
		ppOrderReceiptMock.processMsg = null;
		assertNull(ppOrderReceiptMock.getProcessMsg(), "processMsg not set as expected");
	
	}


	@Test
	void testMPPOrderReceipt_getReversalDocumentLine() {
	
		MPPOrderReceipt reversalMock = mock(MPPOrderReceipt.class);
		
		doCallRealMethod().when(ppOrderReceiptMock).getReversalDocumentLine();
		when(ppOrderReceiptMock.getReversal()).thenReturn(reversalMock);
	
		assertEquals(reversalMock, ppOrderReceiptMock.getReversalDocumentLine(),
				"Reversal Document Line should be null");
	
	}


	@Test
	void testMPPOrderReceipt_getReversalLine_ID() {
		
		doCallRealMethod().when(ppOrderReceiptMock).getReversalLine_ID();
		when(ppOrderReceiptMock.getReversal_ID()).thenReturn(1);
		assertEquals(1, ppOrderReceiptMock.getReversalLine_ID(),
				"Reversal Document Line should match reversal");
	
	}


	@Test
	void testMPPOrderReceipt_getSummary() {
	
		MProduct productMock = mock(MProduct.class);
		when(productMock.getValue()).thenReturn("123");
		when(productMock.getName()).thenReturn("Widget");
	
		doCallRealMethod().when(ppOrderReceiptMock).getSummary();		
		when(ppOrderReceiptMock.getM_Product()).thenReturn(productMock);
		when(ppOrderReceiptMock.getMovementQty()).thenReturn(Env.ONE);
		when(ppOrderReceiptMock.getMovementDate()).thenReturn(today);
		
		assertEquals("123_Widget/Qty 1/" + today.toString(), ppOrderReceiptMock.getSummary(),
				"Summary not as expected");
		
	}

	@Test
	void testMPPOrderReceipt_isReversalParent() {
	
		doCallRealMethod().when(ppOrderReceiptMock).isReversalParent();
		when(ppOrderReceiptMock.getPP_OrderReceipt_ID()).thenReturn(2);
		when(ppOrderReceiptMock.getReversal_ID())
			.thenReturn(1)
			.thenReturn(3);
	
		assertFalse(ppOrderReceiptMock.isReversalParent(), 
				"PP_OrderReceipt should not be a reversal parent");
		assertTrue(ppOrderReceiptMock.isReversalParent(), 
				"PP_OrderReceipt should be a reversal parent");
	
	}


	@Test
	void testMPPOrderReceipt_isSOTrx() {

		doCallRealMethod().when(ppOrderReceiptMock).isSOTrx();
		assertFalse(ppOrderReceiptMock.isSOTrx(), 
				"Manufacturing Order Receipts are not sales transactions");
		
	}

	@Test
	void testMPPOrderReceipt_setDocumentEngine() {
		
		doCallRealMethod().when(ppOrderReceiptMock).setDocumentEngine(any(DocumentEngine.class));
		doCallRealMethod().when(ppOrderReceiptMock).getDocumentEngine();
		
		DocumentEngine engineMock = mock(DocumentEngine.class);
		
		ppOrderReceiptMock.setDocumentEngine(engineMock);
		assertEquals(engineMock, ppOrderReceiptMock.getDocumentEngine(),
				"Get should return the set value");
		
	}


	@Test
	void testMPPOrderReceipt_setParentWithNull() {
		
		doCallRealMethod().when(ppOrderReceiptMock).setParent(any());
		
		assertThrows(NullPointerException.class, () -> {
					ppOrderReceiptMock.setParent(null);
				},
				"setParent should require non null parent");
				
	}

	@Test
	void testMPPOrderReceipt_setParentNotNull() {
		
		doCallRealMethod().when(ppOrderReceiptMock).setParent(any());
		
		when(ppOrderMock.getPP_Order_ID()).thenReturn(1);
		when(ppOrderMock.getM_Product_ID()).thenReturn(2);
		
		ppOrderReceiptMock.setParent(ppOrderMock);
		
		verify(ppOrderReceiptMock, times(1)).setPP_Order_ID(1);
		verify(ppOrderReceiptMock, times(1)).setM_Product_ID(2);
				
	}

	@Test
	void testMPPOrderReceipt_setReversal() {
		
		doCallRealMethod().when(ppOrderReceiptMock).setReversal(anyBoolean());
		
		ppOrderReceiptMock.setReversal(true);
		verify(ppOrderReceiptMock).setIsReversal(true);
		
		ppOrderReceiptMock.setReversal(false);
		verify(ppOrderReceiptMock).setIsReversal(false);
		
	}
	
}
