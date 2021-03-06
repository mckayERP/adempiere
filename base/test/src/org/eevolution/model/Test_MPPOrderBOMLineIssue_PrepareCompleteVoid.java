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

import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Close;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Complete;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_None;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Completed;
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.adempiere.test.CommonGWSetup;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderBOMLineIssue")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderBOMLineIssue_PrepareCompleteVoid extends CommonGWSetup {

	private final MPPOrder ppOrderMock = mock(MPPOrder.class);
	private final CLogger log = mock(CLogger.class);

	private MPPOrderBOMLineIssue bomLineIssueMock;
	private ModelValidationEngine modelValidationEngine;

	@BeforeEach
	void localSetup() {

		bomLineIssueMock = mock(MPPOrderBOMLineIssue.class);
		modelValidationEngine = mock(ModelValidationEngine.class);

		when(bomLineIssueMock.get_Logger()).thenReturn(log);

	}

	@Test
	void prepareIt_ParentNotInProgress() {

		doCallRealMethod().when(bomLineIssueMock).prepareIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();

		when(bomLineIssueMock.getParentOrder()).thenReturn(ppOrderMock);
		when(ppOrderMock.getDocStatus())
				.thenReturn(MPPOrder.DOCSTATUS_Drafted);

		// Test Parent status drafted
		assertEquals(DOCSTATUS_Invalid, bomLineIssueMock.prepareIt(), "Status should be invalid");
		assertNotNull(bomLineIssueMock.getProcessMsg(), "prepareIt() should set a process message");
		assertEquals(
				MPPOrderBOMLineIssue.PROCESS_MSG_PARENT_NOT_IN_PROGRESS,
				bomLineIssueMock.getProcessMsg(),
				"prepareIt() did not set the correct process message");
		verifyNoInteractions(modelValidationEngine);
		verify(bomLineIssueMock, never()).setDocAction(anyString());

	}

	@Test
	void prepareIt_failsValidationBeforePrepare() {

		doCallRealMethod().when(bomLineIssueMock).prepareIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();

		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.getParentOrder()).thenReturn(ppOrderMock);
		when(ppOrderMock.getDocStatus())
				.thenReturn(MPPOrder.DOCSTATUS_InProgress);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn("validationBeforePrepare-Error"); // Before causes error

		// Test before validation error
		assertEquals(DOCSTATUS_Invalid, bomLineIssueMock.prepareIt(), "Status should be invalid");
		assertNotNull(bomLineIssueMock.getProcessMsg(), "prepareIt() should set a process message");
		assertEquals(
				"validationBeforePrepare-Error", bomLineIssueMock.getProcessMsg(),
				"prepareIt() did not set the correct process message");
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_PREPARE);
		verify(modelValidationEngine, never()).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_PREPARE);
		verifyNoMoreInteractions(modelValidationEngine);
		verify(bomLineIssueMock, never()).setDocAction(anyString());

	}

	@Test
	void prepareIt_failsValidationAfterPrepare() {

		doCallRealMethod().when(bomLineIssueMock).prepareIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();

		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.getParentOrder()).thenReturn(ppOrderMock);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn(null)
				.thenReturn("validationAfterPrepare-Error"); // After causes error

		when(ppOrderMock.getDocStatus())
				.thenReturn(MPPOrder.DOCSTATUS_InProgress);

		// Test after validation error
		assertEquals(DOCSTATUS_Invalid, bomLineIssueMock.prepareIt(), "Status should be invalid");
		assertNotNull(bomLineIssueMock.getProcessMsg(), "prepareIt() should set a process message");
		assertEquals(
				"validationAfterPrepare-Error", bomLineIssueMock.getProcessMsg(),
				"prepareIt() did not set the correct process message");
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_PREPARE);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_PREPARE);
		verifyNoMoreInteractions(modelValidationEngine);
		verify(bomLineIssueMock, never()).setDocAction(anyString());

	}

	@Test
	void prepareIt_allOk() {

		doCallRealMethod().when(bomLineIssueMock).prepareIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();

		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.getParentOrder()).thenReturn(ppOrderMock);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn(null)
				.thenReturn(null);

		when(ppOrderMock.getDocStatus())
				.thenReturn(MPPOrder.DOCSTATUS_InProgress);

		// Test all OK
		assertEquals(DOCSTATUS_InProgress, bomLineIssueMock.prepareIt(),
				"Status should be InProgress");
		assertNull(
				bomLineIssueMock.getProcessMsg(),
				"prepareIt() should not set a process message if successful");
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_PREPARE);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_PREPARE);
		verifyNoMoreInteractions(modelValidationEngine);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_Complete);

	}

	@Test
	void completeIt_prepareItFails() {

		doCallRealMethod().when(bomLineIssueMock).completeIt();
		when(bomLineIssueMock.isJustPrepared()).thenReturn(false);
		when(bomLineIssueMock.prepareIt()).thenReturn(DOCSTATUS_Invalid);
		when(bomLineIssueMock.getProcessMsg()).thenReturn("prepareItFailed");

		// Test not prepared - prepareIt fails
		assertEquals(

				DOCSTATUS_Invalid, bomLineIssueMock.completeIt(),
				"Status should be invalid for draft order");
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(log, times(1))
				.info((String) argThat(arg -> ((String) arg).startsWith("CompleteIt - ")));
		verifyNoInteractions(modelValidationEngine);
		verify(bomLineIssueMock, never()).approveIt();
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocAction(anyString());

	}

	@Test
	void completeIt_validationBeforeCompleteFails() {

		doCallRealMethod().when(bomLineIssueMock).completeIt();
		when(bomLineIssueMock.getModelValidationEngine())
				.thenReturn(modelValidationEngine);
		when(bomLineIssueMock.isJustPrepared()).thenReturn(false);
		when(bomLineIssueMock.prepareIt()).thenReturn(null);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn("BeforeCompleteValidationError");

		// Test not prepared - prepare it succeeds
		// Validation error before complete
		assertEquals(DOCSTATUS_Invalid, bomLineIssueMock.completeIt(), "Status should be invalid");
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_COMPLETE);
		verify(modelValidationEngine, never()).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_COMPLETE);
		verify(bomLineIssueMock, times(1)).prepareIt();
		verify(bomLineIssueMock, never()).approveIt();
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocAction(anyString());

	}

	@Test
	void completeIt_ImplicitApproval() {

		doCallRealMethod().when(bomLineIssueMock).completeIt();
		when(bomLineIssueMock.getModelValidationEngine())
				.thenReturn(modelValidationEngine);
		when(bomLineIssueMock.isJustPrepared()).thenReturn(true);
		when(bomLineIssueMock.isApproved()).thenReturn(false);
		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null, (String) null);

		bomLineIssueMock.completeIt();
		verify(bomLineIssueMock, times(1)).approveIt();

	}

	@Test
	void completeIt_storageRulesFail() {

		doCallRealMethod().when(bomLineIssueMock).completeIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine())
				.thenReturn(modelValidationEngine);
		when(bomLineIssueMock.isJustPrepared()).thenReturn(true);
		when(bomLineIssueMock.isApproved()).thenReturn(true);
		when(bomLineIssueMock.applyStorageRules()).thenReturn("StorageRuleError");
		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn(null);

		assertEquals(DOCSTATUS_Invalid, bomLineIssueMock.completeIt(), "Status should be invalid");
		assertEquals("StorageRuleError", bomLineIssueMock.getProcessMsg(),
				"applyStorageRule did not set the correct process message");
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_COMPLETE);
		verify(modelValidationEngine, never()).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_COMPLETE);
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocAction(anyString());

	}

	@Test
	void completeIt_validationAfterCompleteFails() {

		doCallRealMethod().when(bomLineIssueMock).completeIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine())
				.thenReturn(modelValidationEngine);
		when(bomLineIssueMock.isJustPrepared()).thenReturn(true);
		when(bomLineIssueMock.isApproved()).thenReturn(true);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn(null)
				.thenReturn("AfterCompleteValidationError");

		// Test validator error after complete
		assertEquals(DOCSTATUS_Invalid, bomLineIssueMock.completeIt(), "Status should be invalid");
		assertEquals("AfterCompleteValidationError", bomLineIssueMock.getProcessMsg(),
				"completeIt() did not set the correct process message");
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_COMPLETE);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_COMPLETE);
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocAction(anyString());

	}

	@Test
	void completeIt_success() {

		doCallRealMethod().when(bomLineIssueMock).completeIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine())
				.thenReturn(modelValidationEngine);
		when(bomLineIssueMock.isJustPrepared()).thenReturn(true);
		when(bomLineIssueMock.isApproved()).thenReturn(true);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null, (String) null);

		// Test all OK
		assertEquals(DOCSTATUS_Completed, bomLineIssueMock.completeIt(),
				"Status should be Completed");
		assertNull(bomLineIssueMock.getProcessMsg(),
				"completeIt() should not set a process message if successful");
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_COMPLETE);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_COMPLETE);
		verify(bomLineIssueMock, times(1)).applyStorageRules();
		verify(bomLineIssueMock, times(1)).setProcessed(true);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_Close);

	}

	@Test
	void voidIt_DocClosed() {

		doCallRealMethod().when(bomLineIssueMock).voidIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.checkIfClosedReversedOrVoided()).thenReturn("DocClosed");
		when(bomLineIssueMock.get_Logger()).thenReturn(log);

		assertFalse(bomLineIssueMock.voidIt(),
				"Voiding a closed order receipt should return false");
		assertEquals("DocClosed", bomLineIssueMock.getProcessMsg(),
				"Process message was not as expected");
		verify(bomLineIssueMock, never()).setMovementQty(any());
		verifyNoInteractions(modelValidationEngine);

	}

	@Test
	void voidIt_DocNotProcessed_FailsBeforeVoid() {

		doCallRealMethod().when(bomLineIssueMock).voidIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(bomLineIssueMock.directVoidIsPossible()).thenReturn(true);
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.get_Logger()).thenReturn(log);

		reset(modelValidationEngine);
		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn("error_beforeVoid"); // Trigger an error on beforeVoid model validation

		assertFalse(bomLineIssueMock.voidIt(),
				"Failed model validation before void should return false");
		assertEquals("error_beforeVoid", bomLineIssueMock.getProcessMsg(),
				"Process message was not as expected");
		verify(bomLineIssueMock, never()).setMovementQty(Env.ZERO);
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Voided);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_VOID);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void voidIt_DocNotProcessed_FailsAfterVoid() {

		doCallRealMethod().when(bomLineIssueMock).voidIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(bomLineIssueMock.directVoidIsPossible()).thenReturn(true);
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.get_Logger()).thenReturn(log);

		reset(modelValidationEngine);
		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn(null) // Before_Void - validation successful
				.thenReturn("error_afterVoid"); // After_void fails

		assertFalse(bomLineIssueMock.voidIt(),
				"Failed model validation after void should return false");
		assertEquals("error_afterVoid", bomLineIssueMock.getProcessMsg(),
				"Process message was not as expected");
		verify(bomLineIssueMock, times(1)).setMovementQty(Env.ZERO);
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Voided);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_VOID);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_VOID);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void voidIt_DirectVoidPossible_Success() {

		doCallRealMethod().when(bomLineIssueMock).voidIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(bomLineIssueMock.directVoidIsPossible()).thenReturn(true);
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.get_Logger()).thenReturn(log);

		reset(modelValidationEngine);
		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn(null) // Before_Void - validation successful
				.thenReturn(null); // After_void successful

		assertTrue(bomLineIssueMock.voidIt(), "Successful void should return true");
		assertNull(bomLineIssueMock.getProcessMsg(), "Process message should be null");
		verify(bomLineIssueMock, times(1)).setMovementQty(Env.ZERO);
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Voided);
		verify(bomLineIssueMock, times(1)).setProcessed(true);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_None);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_VOID);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_VOID);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void voidIt_DocProcessedWaiting_NeedsAccrual() {

		reset(modelValidationEngine);
		reset(bomLineIssueMock);
		doCallRealMethod().when(bomLineIssueMock).voidIt();
		when(bomLineIssueMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(bomLineIssueMock.directVoidIsPossible()).thenReturn(false);
		when(bomLineIssueMock.reverseAccrualIt()).thenReturn(true);
		when(bomLineIssueMock.isAccrualNecessary()).thenReturn(true);
		when(bomLineIssueMock.get_Logger()).thenReturn(log);

		assertTrue(bomLineIssueMock.voidIt(), "Should have returned True");
		verifyNoInteractions(modelValidationEngine);
		verify(bomLineIssueMock, times(1)).reverseAccrualIt();
		verify(bomLineIssueMock, never()).reverseCorrectIt();

	}

	@Test
	void voidIt_DocProcessedWaiting_NeedsCorrection() {

		doCallRealMethod().when(bomLineIssueMock).voidIt();
		when(bomLineIssueMock.checkIfClosedReversedOrVoided()).thenReturn(null);
		when(bomLineIssueMock.directVoidIsPossible()).thenReturn(false);
		when(bomLineIssueMock.isAccrualNecessary()).thenReturn(false);
		when(bomLineIssueMock.get_Logger()).thenReturn(log);
		when(bomLineIssueMock.reverseCorrectIt()).thenReturn(true);

		assertTrue(bomLineIssueMock.voidIt(), "Should have returned True");
		verifyNoInteractions(modelValidationEngine);
		verify(bomLineIssueMock, never()).reverseAccrualIt();
		verify(bomLineIssueMock, times(1)).reverseCorrectIt();

	}

}
