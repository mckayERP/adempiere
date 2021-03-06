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

import static org.compiere.model.ModelValidator.TIMING_AFTER_CLOSE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_COMPLETE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_PREPARE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_VOID;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_CLOSE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_COMPLETE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_PREPARE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_VOID;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Close;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Complete;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_None;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Prepare;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Closed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Completed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Drafted;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_InProgress;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Invalid;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Voided;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.adempiere.test.CommonUnitTestSetup;
import org.adempiere.test.ModelValidationTester;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.PO;
import org.compiere.process.DocumentEngine;
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
class Test_MPPOrderReceipt extends CommonUnitTestSetup {

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
	@Captor ArgumentCaptor<String> descCaptor;	
	@Captor ArgumentCaptor<String> processActionCaptor;
	@Captor ArgumentCaptor<String> docActionCaptor;

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
	void testMPPOrderReceipt_approveIt() {
		
		doCallRealMethod().when(ppOrderReceiptMock).approveIt();
				
		assertTrue(ppOrderReceiptMock.approveIt(), "ApproveIt should return true");
		verify(ppOrderReceiptMock).setIsApproved(true);
		
	}

	@Test
	void testMPPOrderReceipt_closeIt_AlreadyClosed() {
		
		doCallRealMethod().when(ppOrderReceiptMock).closeIt();
		when(ppOrderReceiptMock.getDocStatus()).thenReturn(DOCSTATUS_Closed);
		
		assertTrue(ppOrderReceiptMock.closeIt(), "A closed order receipt should return true");
		
		verify(ppOrderReceiptMock, times(1)).closeIt();
		verify(ppOrderReceiptMock, times(1)).get_Logger();
		verify(ppOrderReceiptMock, times(1)).getDocStatus();
		verifyNoMoreInteractions(ppOrderReceiptMock);
	
	}

	@Test
	void testMPPOrderReceipt_closeIt_ImplicitCompleteFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).closeIt();
		when(ppOrderReceiptMock.getDocStatus())
			.thenReturn(DOCSTATUS_Drafted)
			.thenReturn(DOCSTATUS_Drafted);
		when(ppOrderReceiptMock.completeIt()).thenReturn(DOCSTATUS_Invalid);
		when(ppOrderReceiptMock.getProcessMsg()).thenReturn("error");
	
		assertFalse(ppOrderReceiptMock.closeIt(),
				"CloseIt should return false if implicit complete fails");
		
		verify(ppOrderReceiptMock, times(1)).completeIt();
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Invalid);
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_Complete);
		
	}

	@Test
	void testMPPOrderReceipt_closeIt_ImplicitCompleteSucceeds_ValidationBeforeFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).closeIt();
		when(ppOrderReceiptMock.getDocStatus())
			.thenReturn(DOCSTATUS_Drafted)
			.thenReturn(DOCSTATUS_Drafted);
		when(ppOrderReceiptMock.completeIt()).thenReturn(DOCSTATUS_Completed);
		when(ppOrderReceiptMock.getProcessMsg())
			.thenReturn(null)
			.thenCallRealMethod();
	
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_CLOSE, BEFORE_FAILS);
	
		assertFalse(ppOrderReceiptMock.closeIt(),
				"CloseIt should return false if implicit complete succeeds but validation fails");
	
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).completeIt();
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Completed);
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Closed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_closeIt_DocCompleted_ValidationBeforeFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).closeIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getDocStatus())
			.thenReturn(DOCSTATUS_Completed)
			.thenReturn(DOCSTATUS_Completed);
		
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_CLOSE, BEFORE_FAILS);
	
		assertFalse(ppOrderReceiptMock.closeIt(),
				"CloseIt should return false when before-close validation fails");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Closed);
		verify(ppOrderReceiptMock, never()).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_closeIt_DocCompleted_ValidationAfterFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).closeIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getDocStatus())
			.thenReturn(DOCSTATUS_Completed)
			.thenReturn(DOCSTATUS_Completed);
		
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_CLOSE, TIMING_AFTER_CLOSE, AFTER_FAILS);
	
		assertFalse(ppOrderReceiptMock.closeIt(), 
				"CloseIt should return false when after-close validation fails");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Closed);
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_closeIt_success() {
		
		doCallRealMethod().when(ppOrderReceiptMock).closeIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getDocStatus())
			.thenReturn(DOCSTATUS_Completed)
			.thenReturn(DOCSTATUS_Completed);
	
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_CLOSE, TIMING_AFTER_CLOSE, AFTER_SUCCEEDS);
		
		assertTrue(ppOrderReceiptMock.closeIt(), "CloseIt should return true if successful");
		assertNull(ppOrderReceiptMock.getProcessMsg(), "Process message should be null");
		
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Closed);
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_None);
		
	}

	@Test
	void testMPPOrderReceipt_completeIt_prepareItFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).completeIt();		
		when(ppOrderReceiptMock.isJustPrepared()).thenReturn(false);
		when(ppOrderReceiptMock.prepareIt()).thenReturn(DOCSTATUS_Invalid);
		when(ppOrderReceiptMock.getProcessMsg()).thenReturn("prepareItFailed");
	
		// Test not prepared - prepareIt fails
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.completeIt(), 
				"Status should be invalid for draft order");
		verify(ppOrderReceiptMock, times(1)).get_Logger();
		verify(log, times(1)).info((String) argThat(arg -> (
													(String)arg).startsWith("CompleteIt - "))
												);
		verifyNoInteractions(modelValidationEngine);
		verify(ppOrderReceiptMock, never()).approveIt();
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());
		
	}

	@Test
	void testMPPOrderReceipt_completeIt_validationBeforeCompleteFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).completeIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.isJustPrepared())
			.thenReturn(true);
	
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_COMPLETE, BEFORE_FAILS); 
		
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.completeIt(), 
				"Status should be invalid");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).prepareIt();
		verify(ppOrderReceiptMock, never()).approveIt();
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());
	
	}

	@Test
	void testMPPOrderReceipt_completeIt_ImplicitApproval() {
		
		doCallRealMethod().when(ppOrderReceiptMock).completeIt();
		when(ppOrderReceiptMock.getModelValidationEngine())
			.thenReturn(modelValidationEngine);
		when(ppOrderReceiptMock.isJustPrepared()).thenReturn(true);
		when(ppOrderReceiptMock.isApproved()).thenReturn(false);
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_COMPLETE, TIMING_AFTER_COMPLETE, AFTER_SUCCEEDS);
		
		ppOrderReceiptMock.completeIt();
		verify(ppOrderReceiptMock, times(1)).approveIt();
		
	}

	@Test
	void testMPPOrderReceipt_completeIt_storageRuleFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).completeIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getModelValidationEngine())
			.thenReturn(modelValidationEngine);
		when(ppOrderReceiptMock.isJustPrepared()).thenReturn(true);
		when(ppOrderReceiptMock.isApproved()).thenReturn(true);
		when(ppOrderReceiptMock.applyStorageRules()).thenReturn("StorageRuleFails");
		
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_COMPLETE, BEFORE_SUCCEEDS); 
		
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.completeIt(), 
				"Status should be invalid");
		
		mvTester.assertModelValidationResult();
		
		assertEquals("StorageRuleFails", ppOrderReceiptMock.getProcessMsg(),
				"completeIt() did not set the correct process message");
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());
	
	}

	@Test
	void testMPPOrderReceipt_completeIt_validationAfterCompleteFails() {
		
		doCallRealMethod().when(ppOrderReceiptMock).completeIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getModelValidationEngine())
			.thenReturn(modelValidationEngine);
		when(ppOrderReceiptMock.isJustPrepared()).thenReturn(true);
		when(ppOrderReceiptMock.isApproved()).thenReturn(true);
		
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_COMPLETE, TIMING_AFTER_COMPLETE, 
				AFTER_FAILS); 
		
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.completeIt(), 
				"Status should be invalid");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).setProcessed(anyBoolean());
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());
	
	}

	@Test
	void testMPPOrderReceipt_completeIt_success() {
		
		doCallRealMethod().when(ppOrderReceiptMock).completeIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getModelValidationEngine())
			.thenReturn(modelValidationEngine);
		when(ppOrderReceiptMock.isJustPrepared()).thenReturn(true);
		when(ppOrderReceiptMock.isApproved()).thenReturn(true);
	
		mvTester.setupBeforeAndAfterModelValidationTest( 
				TIMING_BEFORE_COMPLETE, TIMING_AFTER_COMPLETE, 
				AFTER_SUCCEEDS); 
	
		assertEquals(DOCSTATUS_Completed, ppOrderReceiptMock.completeIt(), 
				"Status should be Completed");
		assertNull(ppOrderReceiptMock.getProcessMsg(),
				"completeIt() should not set a process message if successful");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).applyStorageRules();
		verify(ppOrderReceiptMock, times(1)).setProcessed(true);
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_Close);
		
	}

	@Test
	void testMPPOrderReceipt_invalidateIt() {
	
		doCallRealMethod().when(ppOrderReceiptMock).invalidateIt();
		assertTrue(ppOrderReceiptMock.invalidateIt(), "Should return true");
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_Prepare);
		
	}

	@Test
	void testMPPOrderReceipt_prepareIt_ParentNotInProgress() {
				
		doCallRealMethod().when(ppOrderReceiptMock).prepareIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		
		when(ppOrderReceiptMock.getParent()).thenReturn(ppOrderMock);
		
		when(ppOrderMock.getDocStatus())
			.thenReturn(MPPOrder.DOCSTATUS_Drafted);

		// Test Parent status drafted
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.prepareIt(), 
				"Status should be invalid");
		assertNotNull(ppOrderReceiptMock.getProcessMsg(), 
				"prepareIt() should set a process message");
		assertEquals(MPPOrderReceipt.PROCESS_MSG_PARENT_ORDER_NOT_IN_PROGRESS, 
				ppOrderReceiptMock.getProcessMsg(),
				"prepareIt() did not set the correct process message");
		verifyNoInteractions(modelValidationEngine);
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());
		
	}

	@Test
	void testMPPOrderReceipt_prepareIt_failsValidationBeforePrepare() {
		
		doCallRealMethod().when(ppOrderReceiptMock).prepareIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();		
		when(ppOrderReceiptMock.getParent()).thenReturn(ppOrderMock);
				
		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_PREPARE, BEFORE_FAILS);
		
		when(ppOrderMock.getDocStatus())
			.thenReturn(MPPOrder.DOCSTATUS_InProgress);
				
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.prepareIt(),
				"Status should be invalid");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());
		
		
	}

	@Test
	void testMPPOrderReceipt_prepareIt_failsValidationOfASI() {
		
		doCallRealMethod().when(ppOrderReceiptMock).prepareIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getParent()).thenReturn(ppOrderMock);
		when(ppOrderReceiptMock.validateAttributeSetIntance()).thenReturn("ASIError");
		when(ppOrderMock.getDocStatus())
		.thenReturn(MPPOrder.DOCSTATUS_InProgress);
				
		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
			.thenReturn(null);
		
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.prepareIt(),
				"Status should be invalid");
		
		assertNotNull(ppOrderReceiptMock.getProcessMsg(),
				"prepareIt() should set a process message");
		assertEquals("ASIError", ppOrderReceiptMock.getProcessMsg(),
				"prepareIt() did not set the correct process message");
		
		
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());
		
	}

	@Test
	void testMPPOrderReceipt_prepareIt_failsValidationAfterPrepare() {
		
		doCallRealMethod().when(ppOrderReceiptMock).prepareIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getParent()).thenReturn(ppOrderMock);		
		when(ppOrderMock.getDocStatus())
			.thenReturn(MPPOrder.DOCSTATUS_InProgress);

		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_PREPARE, TIMING_AFTER_PREPARE, AFTER_FAILS);
		
		assertEquals(DOCSTATUS_Invalid, ppOrderReceiptMock.prepareIt(), "Status should be invalid");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).setDocAction(anyString());


	}

	@Test
	void testMPPOrderReceipt_prepareIt_allOk() {
		
		doCallRealMethod().when(ppOrderReceiptMock).prepareIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.getParent()).thenReturn(ppOrderMock);		
		when(ppOrderMock.getDocStatus())
			.thenReturn(MPPOrder.DOCSTATUS_InProgress);
		

		mvTester.setupBeforeAndAfterModelValidationTest(TIMING_BEFORE_PREPARE, TIMING_AFTER_PREPARE, AFTER_SUCCEEDS);
		
		assertEquals(DOCSTATUS_InProgress, ppOrderReceiptMock.prepareIt(),
				"Status should be InProgress");
		assertNull(ppOrderReceiptMock.getProcessMsg(), 
				"prepareIt() should not set a process message if successful");

		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_Complete);

	}

	
	@Test
	void testMPPOrderReceipt_processIt() throws Exception {
		
		DocumentEngine documentEngine = mock(DocumentEngine.class);
		when(documentEngine.processIt(anyString(), anyString()))
			.thenReturn(true);
		
		doCallRealMethod().when(ppOrderReceiptMock).processIt(anyString());
		when(ppOrderReceiptMock.getDocumentEngine()).thenReturn(documentEngine);
		when(ppOrderReceiptMock.getDocAction()).thenReturn(DOCACTION_Prepare);
		
		assertTrue(ppOrderReceiptMock.processIt(DOCACTION_Complete), 
				"Should return true");
									
		verify(ppOrderReceiptMock, times(1)).getDocumentEngine();
		verify(documentEngine, times(1))
			.processIt(processActionCaptor.capture(), docActionCaptor.capture());
		assertEquals(DOCACTION_Complete, processActionCaptor.getValue(),
				"Action not passed correctly");
		assertEquals(DOCACTION_Prepare, docActionCaptor.getValue(),
				"getAction not passed correctly");
		
	}

	@Test
	void testMPPOrderReceipt_reActivateIt() {
	
		doCallRealMethod().when(ppOrderReceiptMock).reActivateIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
	
		assertFalse(ppOrderReceiptMock.reActivateIt(), "ReactivateIt should return false");
		assertNotNull(ppOrderReceiptMock.getProcessMsg(), "Process message should be set");
	
	}

	@Test
	void testMPPOrderReceipt_rejectIt() {

		doCallRealMethod().when(ppOrderReceiptMock).rejectIt();
		
		assertTrue(ppOrderReceiptMock.rejectIt(), "RejectIt should return true");
		verify(ppOrderReceiptMock).setIsApproved(false);
		
	}

	@Test
	void testMPPOrderReceipt_unlockIt() {
		
		doCallRealMethod().when(ppOrderReceiptMock).unlockIt();
		assertTrue(ppOrderReceiptMock.unlockIt(), "Should return true");
		verify(ppOrderReceiptMock, times(1)).setProcessing(false);
		
	}

	@Test
	void testMPPOrderReceipt_voidIt_DocClosed() {
		
		doCallRealMethod().when(ppOrderReceiptMock).voidIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.checkIfClosedReversedOrVoided()).thenReturn("DocClosed");
		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);
	
		assertFalse(ppOrderReceiptMock.voidIt(), 
				"Voiding a closed order receipt should return false");
		assertEquals("DocClosed", ppOrderReceiptMock.getProcessMsg(),
				"Process message was not as expected");
		
		verify(ppOrderReceiptMock, never()).setMovementQty(any());
		verifyNoInteractions(modelValidationEngine);
				
	}

	@Test
	void testMPPOrderReceipt_voidIt_DocNotProcessed_FailsBeforeVoid() {
		
		doCallRealMethod().when(ppOrderReceiptMock).voidIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(ppOrderReceiptMock.directVoidIsPossible()).thenReturn(true);
		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);

		mvTester.setupBeforeModelValidationTest(TIMING_BEFORE_VOID, BEFORE_FAILS);
		
		assertFalse(ppOrderReceiptMock.voidIt(), 
				"Failed model validation before void should return false");
		
		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, never()).setMovementQty(Env.ZERO);
		verify(ppOrderReceiptMock, never()).setDocStatus(DOCSTATUS_Voided);
				
	}

	@Test
	void testMPPOrderReceipt_voidIt_DocNotProcessed_FailsAfterVoid() {
		
		doCallRealMethod().when(ppOrderReceiptMock).voidIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(ppOrderReceiptMock.directVoidIsPossible()).thenReturn(true);
		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);
		
		mvTester.setupBeforeAndAfterModelValidationTest(TIMING_BEFORE_VOID, TIMING_AFTER_VOID, AFTER_FAILS);
		
		assertFalse(ppOrderReceiptMock.voidIt(),
				"Failed model validation after void should return false");

		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).setMovementQty(Env.ZERO);
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Voided);
				
	}

	@Test
	void testMPPOrderReceipt_voidIt_DirectVoidPossible_Success() {
		
		doCallRealMethod().when(ppOrderReceiptMock).voidIt();
		doCallRealMethod().when(ppOrderReceiptMock).getProcessMsg();
		when(ppOrderReceiptMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(ppOrderReceiptMock.directVoidIsPossible()).thenReturn(true);
		when(ppOrderReceiptMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);
		
		mvTester.setupBeforeAndAfterModelValidationTest(
				TIMING_BEFORE_VOID, TIMING_AFTER_VOID, AFTER_SUCCEEDS);
		
		assertTrue(ppOrderReceiptMock.voidIt(), "Successful void should return true");
		assertNull(ppOrderReceiptMock.getProcessMsg(), "Process message should be null");

		mvTester.assertModelValidationResult();
		
		verify(ppOrderReceiptMock, times(1)).setMovementQty(Env.ZERO);
		verify(ppOrderReceiptMock, times(1)).setDocStatus(DOCSTATUS_Voided);
		verify(ppOrderReceiptMock, times(1)).setProcessed(true);
		verify(ppOrderReceiptMock, times(1)).setDocAction(DOCACTION_None);
				
	}

	@Test
	void testMPPOrderReceipt_voidIt_DocProcessedWaiting_NeedsAccrual() {
		
		doCallRealMethod().when(ppOrderReceiptMock).voidIt();
		when(ppOrderReceiptMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(ppOrderReceiptMock.directVoidIsPossible()).thenReturn(false);
		when(ppOrderReceiptMock.reverseAccrualIt()).thenReturn(true);
		when(ppOrderReceiptMock.isAccrualNecessary()).thenReturn(true);
		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);

		assertTrue(ppOrderReceiptMock.voidIt(), "Should have returned True");
		
		verifyNoInteractions(modelValidationEngine);
		verify(ppOrderReceiptMock, times(1)).reverseAccrualIt();
		verify(ppOrderReceiptMock, never()).reverseCorrectIt();

	}

	@Test
	void testMPPOrderReceipt_voidIt_DocProcessedWaiting_NeedsCorrection() {
		
		doCallRealMethod().when(ppOrderReceiptMock).voidIt();
		when(ppOrderReceiptMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(ppOrderReceiptMock.directVoidIsPossible()).thenReturn(false);
		when(ppOrderReceiptMock.isAccrualNecessary()).thenReturn(false);
		when(ppOrderReceiptMock.get_Logger()).thenReturn(log);
		when(ppOrderReceiptMock.reverseCorrectIt()).thenReturn(true);

		assertTrue(ppOrderReceiptMock.voidIt(), "Should have returned True");
		
		verifyNoInteractions(modelValidationEngine);
		verify(ppOrderReceiptMock, never()).reverseAccrualIt();
		verify(ppOrderReceiptMock, times(1)).reverseCorrectIt();

	}


	
}
