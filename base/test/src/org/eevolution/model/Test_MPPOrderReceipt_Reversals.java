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

import static org.compiere.model.ModelValidator.TIMING_AFTER_REVERSEACCRUAL;
import static org.compiere.model.ModelValidator.TIMING_AFTER_REVERSECORRECT;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_REVERSEACCRUAL;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_REVERSECORRECT;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_None;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Reversed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;

import org.adempiere.test.CommonUnitTestSetup;
import org.adempiere.test.ModelValidationTester;
import org.compiere.model.MDocType;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderReceipt")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderReceipt_Reversals extends CommonUnitTestSetup {

	private final ModelValidationEngine modelValidationEngine = mock(ModelValidationEngine.class);
	private final MPPOrderReceipt ppOrderReceiptMock = mock(MPPOrderReceipt.class);
	private final MPPOrder ppOrderMock = mock(MPPOrder.class);
	private final CLogger log = mock(CLogger.class);
	
	private static final boolean BEFORE_FAILS = true;
	private static final boolean BEFORE_SUCCEEDS = false;
	private static final boolean AFTER_FAILS = true;
	private static final boolean AFTER_SUCCEEDS = false;

	private ModelValidationTester mvTester;
	
	@Captor ArgumentCaptor<PO> poCaptor;
	@Captor ArgumentCaptor<Integer> timingCaptor;

	@BeforeEach
	void setUp() throws Exception {

		when(ppOrderMock.getM_Product_ID()).thenReturn(123);

		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);
		when(ppOrderReceiptMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		
		mvTester = new ModelValidationTester(ppOrderReceiptMock, modelValidationEngine);
		mvTester.setPOCaptor(poCaptor);
		mvTester.setTimingCaptor(timingCaptor);

	}
	

	@Test
	void testMPPOrderReceipt_reverseCorrectIt_ValidationBeforeFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseCorrectIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_REVERSECORRECT, BEFORE_FAILS);
		
		assertFalse(ppOrderReceiptMock.reverseCorrectIt(), 
				"ReverseCorrectIt should return false when validation fails");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).reverseIt(anyBoolean());
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_reverseCorrectIt_reverseItFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseCorrectIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.reverseIt(false)).thenReturn(null); // ReverseIt fails
		
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_REVERSECORRECT, BEFORE_SUCCEEDS);
		
		assertFalse(ppOrderReceiptMock.reverseCorrectIt(),
				"ReverseCorrectIt should return false when reverseIt fails");
		assertNull(ppOrderReceiptMock.getProcessMsg(), "Process message should be null");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).reverseIt(false);
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_reverseCorrectIt_reverseItSucceeds_validationAfterFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseCorrectIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.reverseIt(false)).thenReturn(ppOrderReceiptMock); // ReverseIt succeeds
		
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_REVERSECORRECT, TIMING_AFTER_REVERSECORRECT, AFTER_FAILS);

		assertFalse(ppOrderReceiptMock.reverseCorrectIt(),
				"ReverseCorrectIt should return false when validation fails");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).reverseIt(false);
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_reverseCorrectIt_success() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseCorrectIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.reverseIt(false)).thenReturn(ppOrderReceiptMock); // ReverseIt succeeds
		when(ppOrderReceiptMock.getDocumentNo()).thenReturn("NewNumber");
		
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_REVERSECORRECT, TIMING_AFTER_REVERSECORRECT, AFTER_SUCCEEDS);

		assertTrue(ppOrderReceiptMock.reverseCorrectIt(),
				"ReverseCorrectIt should return true if successful");
		assertEquals("NewNumber", ppOrderReceiptMock.getProcessMsg(), 
				"Process message should be the reversed document number");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).reverseIt(false);
		verify(ppOrderReceiptMock, times(1)).setProcessed(true);
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_reverseAccrualIt_ValidationBeforeFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseAccrualIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_REVERSEACCRUAL, BEFORE_FAILS);
		
		assertFalse(ppOrderReceiptMock.reverseAccrualIt(),
				"ReverseAccrualIt should return false when before-reverseAccrual validation fails");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).reverseIt(anyBoolean());
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_reverseAccrualIt_reverseItFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseAccrualIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.reverseIt(true)).thenReturn(null); // ReverseIt fails
		
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_REVERSEACCRUAL, BEFORE_SUCCEEDS);

		assertFalse(ppOrderReceiptMock.reverseAccrualIt(), 
				"ReverseAccrualIt should return false when reverseIt fails");
		assertNull(ppOrderReceiptMock.getProcessMsg(), "Process message should be null");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).reverseIt(true);
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		verify(ppOrderReceiptMock, times(1)).getModelValidationEngine();
		
	}

	@Test
	void testMPPOrderReceipt_reverseAccrualIt_reverseItSucceeds_validationAfterFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseAccrualIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.reverseIt(true)).thenReturn(ppOrderReceiptMock); 
		
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_REVERSEACCRUAL, TIMING_AFTER_REVERSEACCRUAL, AFTER_FAILS);
		
		assertFalse(ppOrderReceiptMock.reverseAccrualIt(),
				"ReverseCorrectIt should return false when validation fails");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).reverseIt(true);
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_reverseAccrualtIt_success() {
		
		doCallRealMethod().when(ppOrderReceiptMock).reverseAccrualIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.reverseIt(true)).thenReturn(ppOrderReceiptMock); 
		when(ppOrderReceiptMock.getDocumentNo()).thenReturn("NewNumber");
		
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_REVERSEACCRUAL, TIMING_AFTER_REVERSEACCRUAL, AFTER_SUCCEEDS);
		
		assertTrue(ppOrderReceiptMock.reverseAccrualIt(),
				"ReverseCorrectIt should return true if successful");
		assertEquals("NewNumber", ppOrderReceiptMock.getProcessMsg(),
				"Process message should be the reversed document number");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).reverseIt(true);
		verify(ppOrderReceiptMock, times(1)).setProcessed(true);
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Reversed);
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_reverseIt_accrual_copyFails() {
		
		Timestamp loginDate = Env.getContextAsDate(ctx,"#Date");
		
		MDocType docTypeMock = mock(MDocType.class);
				
		doCallRealMethod().when(ppOrderReceiptMock).reverseIt(true);
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getCtx()).thenReturn(ctx);
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		doNothing().when(ppOrderReceiptMock).testPeriodOpen(any());
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		when(ppOrderReceiptMock.copyFrom(any(Timestamp.class), any(Timestamp.class), anyBoolean()))
				.thenReturn(null);
		
		assertNull(ppOrderReceiptMock.reverseIt(true),
				"If copyFrom fails, reverseIt should return null");
		assertEquals("Could not create Receipt Reversal", ppOrderReceiptMock.getProcessMsg(),
				"Process Message not set as expected");
		verify(ppOrderReceiptMock, times(1)).testPeriodOpen(loginDate);
		
	}

	@Test
	void testMPPOrderReceipt_reverseIt_accrual_reversalNotCompleted() {
		
		MPPOrderReceipt reversalMock = mock(MPPOrderReceipt.class);
		when(reversalMock.processIt(anyString())).thenReturn(false);
		when(reversalMock.getProcessMsg()).thenReturn("completion failed");
		
		Timestamp loginDate = Env.getContextAsDate(ctx,"#Date");
		
		MDocType docTypeMock = mock(MDocType.class);
				
		doCallRealMethod().when(ppOrderReceiptMock).reverseIt(true);
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getCtx()).thenReturn(ctx);
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		doNothing().when(ppOrderReceiptMock).testPeriodOpen(any());
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		when(ppOrderReceiptMock.copyFrom(any(Timestamp.class), any(Timestamp.class), 
				anyBoolean())).thenReturn(reversalMock);
		when(ppOrderReceiptMock.getMovementQty()).thenReturn(Env.ONE);
		when(ppOrderReceiptMock.getDocumentNo()).thenReturn("123");
		when(ppOrderReceiptMock.getPP_OrderReceipt_ID()).thenReturn(234);
		
		assertNull(ppOrderReceiptMock.reverseIt(true), 
				"If reversal complete fails, reverseIt should return null");
		assertEquals("Reversal ERROR: completion failed", ppOrderReceiptMock.getProcessMsg(),
				"Process Message not set as expected");
		verify(ppOrderReceiptMock, times(1)).testPeriodOpen(loginDate);
		
	}

	@Test
	void testMPPOrderReceipt_reverseIt_accrual_success() {
		
		MPPOrderReceipt reversalMock = mock(MPPOrderReceipt.class);
		when(reversalMock.processIt(anyString())).thenReturn(true);
		when(reversalMock.getDocStatus()).thenReturn("CO");
		when(reversalMock.getDocumentNo()).thenReturn("456");
		when(reversalMock.get_ID()).thenReturn(1000056);
		
		Timestamp loginDate = Env.getContextAsDate(ctx,"#Date");
		
		MDocType docTypeMock = mock(MDocType.class);
				
		doCallRealMethod().when(ppOrderReceiptMock).reverseIt(true);
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getCtx()).thenReturn(ctx);
		when(ppOrderReceiptMock.get_TrxName()).thenReturn(trxName);
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		doNothing().when(ppOrderReceiptMock).testPeriodOpen(any());
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		when(ppOrderReceiptMock.copyFrom(any(Timestamp.class), any(Timestamp.class), 
				anyBoolean())).thenReturn(reversalMock);
		when(ppOrderReceiptMock.getMovementQty()).thenReturn(Env.ONE);
		when(ppOrderReceiptMock.getDocumentNo()).thenReturn("123");
		when(ppOrderReceiptMock.getPP_OrderReceipt_ID()).thenReturn(234);
		
		assertEquals(reversalMock, ppOrderReceiptMock.reverseIt(true),
				"reverseIt should return the reversed");
		assertNull(ppOrderReceiptMock.getProcessMsg(), "Process Message should be null");
		verify(ppOrderReceiptMock, times(1)).testPeriodOpen(loginDate);
		verify(ppOrderReceiptMock).addDescription("(456<-)");
		verify(ppOrderReceiptMock).setReversal_ID(1000056);
		verify(ppOrderReceiptMock).setIsReversal(true);
		verify(ppOrderReceiptMock).setDocStatus(DOCSTATUS_Reversed);
		
	}

	@Test
	void testMPPOrderReceipt_reverseIt_accrual_reversalDocStatusNotCompleted() {
		
		MPPOrderReceipt reversalMock = mock(MPPOrderReceipt.class);
		when(reversalMock.processIt(anyString())).thenReturn(true);
		when(reversalMock.getDocStatus()).thenReturn("IN");
		when(reversalMock.getProcessMsg()).thenReturn("completion failed");
		
		Timestamp loginDate = Env.getContextAsDate(ctx,"#Date");
		
		MDocType docTypeMock = mock(MDocType.class);
				
		doCallRealMethod().when(ppOrderReceiptMock).reverseIt(true);
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getCtx()).thenReturn(ctx);
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		doNothing().when(ppOrderReceiptMock).testPeriodOpen(any());
		when(ppOrderReceiptMock.getDocType()).thenReturn(docTypeMock);
		when(ppOrderReceiptMock.copyFrom(any(Timestamp.class), any(Timestamp.class), anyBoolean()))
			.thenReturn(reversalMock);
		when(ppOrderReceiptMock.getMovementQty()).thenReturn(Env.ONE);
		when(ppOrderReceiptMock.getDocumentNo()).thenReturn("123");
		when(ppOrderReceiptMock.getPP_OrderReceipt_ID()).thenReturn(234);
		
		assertNull(ppOrderReceiptMock.reverseIt(true), 
				"If reversal complete fails, reverseIt should return null");
		assertEquals("Reversal ERROR: completion failed", ppOrderReceiptMock.getProcessMsg(),
				"Process Message not set as expected");
		verify(ppOrderReceiptMock, times(1)).testPeriodOpen(loginDate);
		
	}
	
}
