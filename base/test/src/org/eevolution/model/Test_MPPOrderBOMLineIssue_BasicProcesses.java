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

import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Prepare;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.stream.Stream;

import org.adempiere.exceptions.PeriodClosedException;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.ModelValidationEngine;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderBOMLineIssue")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderBOMLineIssue_BasicProcesses extends CommonUnitTestSetup {

	private final MPPOrder ppOrderMock = mock(MPPOrder.class);
	private final CLogger logger = mock(CLogger.class);

	private MPPOrderBOMLineIssue bomLineIssueMock;
	private ModelValidationEngine modelValidationEngine;

	@Captor
	ArgumentCaptor<Integer> intCaptor;

	@Captor
	ArgumentCaptor<String> stringCaptor;

	@Captor
	ArgumentCaptor<Boolean> boolCaptor;

	@BeforeEach
	void localSetup() {

		bomLineIssueMock = mock(MPPOrderBOMLineIssue.class);
		modelValidationEngine = mock(ModelValidationEngine.class);

	}

	@Test
	void beforeSave_noDocType() {

		doCallRealMethod().when(bomLineIssueMock).beforeSave(anyBoolean());
		when(bomLineIssueMock.getC_DocType_ID()).thenReturn(0);
		when(bomLineIssueMock.getDocType_ID()).thenReturn(1);

		assertTrue(bomLineIssueMock.beforeSave(true), "beforeSave should always return true");

		verify(bomLineIssueMock).setC_DocType_ID(intCaptor.capture());
		assertEquals(1, intCaptor.getValue().intValue(),
				"beforeSave did not set the correct doc type id");

	}

	@Test
	void processIt() throws Exception {

		DocumentEngine docEngineMock = mock(DocumentEngine.class);
		when(docEngineMock.processIt(anyString(), anyString())).thenReturn(true);

		doCallRealMethod().when(bomLineIssueMock).processIt(anyString());
		doCallRealMethod().when(bomLineIssueMock).setProcessMsg(anyString());
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getDocumentEngine()).thenReturn(docEngineMock);
		when(bomLineIssueMock.getDocAction()).thenReturn(DocAction.ACTION_Prepare);

		bomLineIssueMock.setProcessMsg("processMsgString");
		assertTrue(bomLineIssueMock.processIt(DocAction.ACTION_Complete), "Should return true");

		verify(docEngineMock).processIt(DocAction.ACTION_Complete, DocAction.ACTION_Prepare);

		assertNull(bomLineIssueMock.getProcessMsg(), "Process msg should be reset to null");

	}

	@Test
	void unlockIt() {

		doCallRealMethod().when(bomLineIssueMock).unlockIt();
		assertTrue(bomLineIssueMock.unlockIt(), "Should return true");
		verify(bomLineIssueMock, times(1)).setProcessing(false);

	}

	@Test
	void invalidateIt() {

		doCallRealMethod().when(bomLineIssueMock).invalidateIt();
		assertTrue(bomLineIssueMock.invalidateIt(), "Should return true");
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_Prepare);

	}

	@Test
	void approveIt() {

		doCallRealMethod().when(bomLineIssueMock).approveIt();
		when(bomLineIssueMock.get_Logger()).thenReturn(logger);

		assertTrue(bomLineIssueMock.approveIt(), "ApproveIt should return true");
		verify(bomLineIssueMock).setIsApproved(true);

	}

	@Test
	void rejectIt() {

		doCallRealMethod().when(bomLineIssueMock).rejectIt();
		when(bomLineIssueMock.get_Logger()).thenReturn(logger);

		assertTrue(bomLineIssueMock.rejectIt(), "RejectIt should return true");
		verify(bomLineIssueMock).setIsApproved(false);

	}

	@Test
	void checkIfClosedResersedOrVoided_closed() {

		String[] docStatusClosed = {
				DOCSTATUS_Closed,
				DOCSTATUS_Reversed,
				DOCSTATUS_Voided
		};

		doCallRealMethod().when(bomLineIssueMock).checkIfClosedReversedOrVoided();

		Stream.of(docStatusClosed)
				.forEach(status ->
				{

					when(bomLineIssueMock.getDocStatus()).thenReturn(status);
					assertEquals("Document Closed: " + status,
							bomLineIssueMock.checkIfClosedReversedOrVoided(),
							"Process message was not as expected");

				});

	}

	@Test
	void checkIfClosedResersedOrVoided_notClosed() {

		String[] docStatusNotClosed = {
				DOCSTATUS_Drafted,
				DOCSTATUS_Invalid,
				DOCSTATUS_InProgress,
				DOCSTATUS_Approved,
				DOCSTATUS_NotApproved,
				DOCSTATUS_Completed,
				DOCSTATUS_Unknown,
				DOCSTATUS_WaitingConfirmation,
				DOCSTATUS_WaitingPayment
		};

		doCallRealMethod().when(bomLineIssueMock).checkIfClosedReversedOrVoided();

		Stream.of(docStatusNotClosed)
				.forEach(status ->
				{

					when(bomLineIssueMock.getDocStatus()).thenReturn(status);
					assertNull(
							bomLineIssueMock.checkIfClosedReversedOrVoided(),
							"If not closed, should return null");

				});

	}

	@Test
	void directVoidIsPossible() {

		String[] docStatusAllowingDirectVoid = {
				DOCSTATUS_Drafted,
				DOCSTATUS_Invalid,
				DOCSTATUS_InProgress,
				DOCSTATUS_Approved,
				DOCSTATUS_NotApproved
		};

		doCallRealMethod().when(bomLineIssueMock).directVoidIsPossible();

		Stream.of(docStatusAllowingDirectVoid)
				.forEach(status ->
				{

					when(bomLineIssueMock.getDocStatus()).thenReturn(status);
					assertTrue(
							bomLineIssueMock.directVoidIsPossible(),
							"Direct void should be possible when status is " + status);

				});

	}

	@Test
	void directVoidIsNotPossible() {

		String[] docStatusNotAllowingDirectVoid = {
				DOCSTATUS_Completed,
				DOCSTATUS_Unknown,
				DOCSTATUS_WaitingConfirmation,
				DOCSTATUS_WaitingPayment,
				DOCSTATUS_Closed,
				DOCSTATUS_Reversed,
				DOCSTATUS_Voided
		};

		doCallRealMethod().when(bomLineIssueMock).directVoidIsPossible();

		Stream.of(docStatusNotAllowingDirectVoid)
				.forEach(status ->
				{

					when(bomLineIssueMock.getDocStatus()).thenReturn(status);
					assertFalse(
							bomLineIssueMock.directVoidIsPossible(),
							"Direct void should not be possible when status is " + status);

				});

	}

	@Test
	void reActivateIt() {

		doCallRealMethod().when(bomLineIssueMock).reActivateIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.get_Logger()).thenReturn(logger);

		assertFalse(bomLineIssueMock.reActivateIt(), "ReactivateIt should return false");
		assertNotNull(bomLineIssueMock.getProcessMsg(), "Process message should be set");

	}

	@Test
	void getDoc_User_ID() {

		doCallRealMethod().when(bomLineIssueMock).getDoc_User_ID();
		when(bomLineIssueMock.getParentOrder()).thenReturn(null, ppOrderMock);

		when(ppOrderMock.getPlanner_ID()).thenReturn(1);

		assertEquals(0, bomLineIssueMock.getDoc_User_ID(), "With no parent order, should return 0");
		assertEquals(
				1, bomLineIssueMock.getDoc_User_ID(),
				"With parent order, should return parent planner id");

	}

	@Test
	void getModelValidationEngine() {

		doCallRealMethod().when(bomLineIssueMock).getModelValidationEngine();
		doCallRealMethod().when(bomLineIssueMock)
				.setModelValidationEngine(any(ModelValidationEngine.class));
		bomLineIssueMock.setModelValidationEngine(modelValidationEngine);

		assertEquals(
				modelValidationEngine, bomLineIssueMock.getModelValidationEngine(),
				"ModelValidationEngine not set as expected");

		// Case of null left of integration tests

	}

	@Test
	void reverseIt_accrual_copyFails() {

		doCallRealMethod().when(bomLineIssueMock).reverseIt(anyBoolean());
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getCtx()).thenReturn(ctx);
		doNothing().when(bomLineIssueMock).testPeriodOpen(any());
		when(bomLineIssueMock.copyFrom(any(Timestamp.class), any(Timestamp.class), anyBoolean()))
				.thenReturn(null);

		assertNull(bomLineIssueMock.reverseIt(true),
				"If copyFrom fails, reverseIt should return null");
		verify(bomLineIssueMock, times(1)).testPeriodOpen(loginDate);

		assertEquals("Could not create Receipt Reversal", bomLineIssueMock.getProcessMsg(),
				"If the reversal copy failed, the process msg should be set");

	}

	@Test
	void reverseIt_notAccrual_copyFails() {

		Timestamp reversalDate = TimeUtil.addDuration(loginDate, TimeUtil.DURATIONUNIT_Day, -1);

		doCallRealMethod().when(bomLineIssueMock).reverseIt(anyBoolean());
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getCtx()).thenReturn(ctx);
		when(bomLineIssueMock.getDateAcct()).thenReturn(reversalDate);
		when(bomLineIssueMock.getMovementDate()).thenReturn(reversalDate);
		doNothing().when(bomLineIssueMock).testPeriodOpen(any());
		when(bomLineIssueMock.copyFrom(any(Timestamp.class), any(Timestamp.class), anyBoolean()))
				.thenReturn(null);

		assertNull(bomLineIssueMock.reverseIt(false),
				"If copyFrom fails, reverseIt should return null");
//		verify(bomLineIssueMock, times(1)).testPeriodOpen(reversalDate, "MOR");

		assertEquals("Could not create Receipt Reversal", bomLineIssueMock.getProcessMsg(),
				"If the reversal copy failed, the process msg should be set");

	}

	@Test
	void reverseIt_accrual_reversalCantBeCompleted() {

		MPPOrderBOMLineIssue reversalMock = mock(MPPOrderBOMLineIssue.class);
		when(reversalMock.processIt(DocAction.ACTION_Complete)).thenReturn(false);
		when(reversalMock.getProcessMsg()).thenReturn("ReversalFailed");

		doCallRealMethod().when(bomLineIssueMock).reverseIt(true);
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getCtx()).thenReturn(ctx);
		doNothing().when(bomLineIssueMock).testPeriodOpen(any());
		when(bomLineIssueMock.copyFrom(any(Timestamp.class), any(Timestamp.class), anyBoolean()))
				.thenReturn(reversalMock);
		when(bomLineIssueMock.getMovementQty()).thenReturn(Env.ONE);
		when(bomLineIssueMock.getDocumentNo()).thenReturn("1000024");

		assertNull(bomLineIssueMock.reverseIt(true),
				"If copyFrom fails, reverseIt should return the reversal");

		assertEquals("Reversal ERROR: ReversalFailed", bomLineIssueMock.getProcessMsg(),
				"If the reversal can be completed, the process msg should be set");

	}

	@Test
	void reverseIt_accrual_success() {

		MPPOrderBOMLineIssue reversalMock = mock(MPPOrderBOMLineIssue.class);
		when(reversalMock.getDocumentNo()).thenReturn("1000012");
		when(reversalMock.get_ID()).thenReturn(12345);
		when(reversalMock.processIt(DocAction.ACTION_Complete)).thenReturn(true);
		when(reversalMock.getDocStatus()).thenReturn(DocAction.STATUS_Completed);

		doCallRealMethod().when(bomLineIssueMock).reverseIt(true);
		when(bomLineIssueMock.getCtx()).thenReturn(ctx);
		doNothing().when(bomLineIssueMock).testPeriodOpen(any());
		when(bomLineIssueMock.copyFrom(any(Timestamp.class), any(Timestamp.class), anyBoolean()))
				.thenReturn(reversalMock);
		when(bomLineIssueMock.getMovementQty()).thenReturn(Env.ONE);
		when(bomLineIssueMock.getDocumentNo()).thenReturn("1000024");

		assertEquals(reversalMock, bomLineIssueMock.reverseIt(true),
				"If copyFrom succeeds, reverseIt should return the reversal");
		verify(bomLineIssueMock, times(1)).testPeriodOpen(loginDate);
		verify(bomLineIssueMock).addDescription(stringCaptor.capture());
		assertEquals("(1000012<-)", stringCaptor.getValue(),
				"Line description not set as expected");
		verify(bomLineIssueMock).setReversal_ID(intCaptor.capture());
		assertEquals(12345, intCaptor.getValue(), "Reversal ID not set as expected");
		verify(bomLineIssueMock).setIsReversal(boolCaptor.capture());
		assertTrue(boolCaptor.getValue().booleanValue(),
				"The line should have its isReversal flag set");

	}

	@Test
	void setReversal() {

		doCallRealMethod().when(bomLineIssueMock).setReversal(anyBoolean());

		bomLineIssueMock.setReversal(true);
		verify(bomLineIssueMock).setIsReversal(true);

		bomLineIssueMock.setReversal(false);
		verify(bomLineIssueMock).setIsReversal(false);

	}

	@Test
	void isAccrualNecessary_periodOpen() {

		doCallRealMethod().when(bomLineIssueMock).isAccrualNecessary();
		when(bomLineIssueMock.getDateAcct()).thenReturn(loginDate);
		assertFalse(bomLineIssueMock.isAccrualNecessary(),
				"If period is open, accrual should not be necessary");

	}

	@Test
	void isAccrualNecessary_periodClosed() {

		doCallRealMethod().when(bomLineIssueMock).isAccrualNecessary();
		when(bomLineIssueMock.getDateAcct()).thenReturn(loginDate);
		doThrow(PeriodClosedException.class).when(bomLineIssueMock)
				.testPeriodOpen(any(Timestamp.class));
		assertTrue(bomLineIssueMock.isAccrualNecessary(),
				"If period is closed, accrual should be necessary");

	}

}
