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

import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Complete;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_None;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Closed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Completed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Drafted;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Invalid;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Reversed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderBOMLineIssue")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderBOMLineIssue_CloseReverse extends CommonUnitTestSetup {

	private final CLogger log = mock(CLogger.class);

	private MPPOrderBOMLineIssue bomLineIssueMock;
	private ModelValidationEngine modelValidationEngine;

	@BeforeEach
	void localSetup()
	{

		bomLineIssueMock = mock(MPPOrderBOMLineIssue.class);
		modelValidationEngine = mock(ModelValidationEngine.class);

		when(bomLineIssueMock.get_Logger()).thenReturn(log);

	}

	@Test
	void closeIt_AlreadyClosed()
	{

		doCallRealMethod().when(bomLineIssueMock).closeIt();
		when(bomLineIssueMock.getDocStatus()).thenReturn(DOCSTATUS_Closed);

		assertTrue(bomLineIssueMock.closeIt(), "A closed order receipt should return true");
		verify(bomLineIssueMock, times(1)).closeIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getDocStatus();
		verify(bomLineIssueMock, never()).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verifyNoInteractions(modelValidationEngine);

	}

	@Test
	void closeIt_ImplicitCompleteFails()
	{

		doCallRealMethod().when(bomLineIssueMock).closeIt();
		when(bomLineIssueMock.getDocStatus())
				.thenReturn(DOCSTATUS_Drafted)
				.thenReturn(DOCSTATUS_Drafted);
		when(bomLineIssueMock.completeIt()).thenReturn(DOCSTATUS_Invalid);
		when(bomLineIssueMock.getProcessMsg()).thenReturn("error");

		assertFalse(bomLineIssueMock.closeIt(),
				"CloseIt should return false if implicit complete fails");
		verify(bomLineIssueMock, times(1)).closeIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(2)).getDocStatus();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).completeIt();
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Invalid);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_Complete);
		verify(bomLineIssueMock, never()).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verifyNoInteractions(modelValidationEngine);

	}

	@Test
	void closeIt_ImplicitCompleteSucceeds_ValidationBeforeFails()
	{

		doCallRealMethod().when(bomLineIssueMock).closeIt();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.getDocStatus())
				.thenReturn(DOCSTATUS_Drafted)
				.thenReturn(DOCSTATUS_Drafted);
		when(bomLineIssueMock.completeIt()).thenReturn(DOCSTATUS_Completed);
		when(bomLineIssueMock.getProcessMsg()).thenReturn(null);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn("ValidationErrorBeforeClose"); // Before_Close - validation fails

		assertFalse(bomLineIssueMock.closeIt(),
				"CloseIt should return false if implicit complete succeeds but validation fails");
		verify(bomLineIssueMock, times(1)).closeIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(2)).getDocStatus();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).completeIt();
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Completed);
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Closed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(1)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_CLOSE);
		verify(modelValidationEngine, never()).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_CLOSE);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void closeIt_DocCompleted_ValidationBeforeFails()
	{

		doCallRealMethod().when(bomLineIssueMock).closeIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.getDocStatus())
				.thenReturn(DOCSTATUS_Completed)
				.thenReturn(DOCSTATUS_Completed);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn("ValidationErrorBeforeClose"); // Before_Close - validation fails

		assertFalse(bomLineIssueMock.closeIt(),
				"CloseIt should return false when before-close validation fails");
		assertEquals("ValidationErrorBeforeClose", bomLineIssueMock.getProcessMsg(),
				"Process message should be set");
		verify(bomLineIssueMock, times(1)).closeIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(2)).getDocStatus();
		verify(bomLineIssueMock, never()).completeIt();
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Closed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(1)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_CLOSE);
		verify(modelValidationEngine, never()).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_CLOSE);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void closeIt_DocCompleted_ValidationAfterFails()
	{

		doCallRealMethod().when(bomLineIssueMock).closeIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.getDocStatus())
				.thenReturn(DOCSTATUS_Completed)
				.thenReturn(DOCSTATUS_Completed);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn(null, "ValidationErrorAfterClose"); // Before_Close ok, After_Close
																// validation fails

		assertFalse(bomLineIssueMock.closeIt(),
				"CloseIt should return false when after-close validation fails");
		assertEquals("ValidationErrorAfterClose", bomLineIssueMock.getProcessMsg(),
				"Process message should be set");
		verify(bomLineIssueMock, times(1)).closeIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(2)).getDocStatus();
		verify(bomLineIssueMock, never()).completeIt();
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Closed);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(2)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_BEFORE_CLOSE);
		verify(modelValidationEngine, times(1)).fireDocValidate((PO) bomLineIssueMock,
				ModelValidator.TIMING_AFTER_CLOSE);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void closeIt_success()
	{

		doCallRealMethod().when(bomLineIssueMock).closeIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.getDocStatus())
				.thenReturn(DOCSTATUS_Completed)
				.thenReturn(DOCSTATUS_Completed);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null, (String) null); // Before_Close ok, After_Close ok

		assertTrue(bomLineIssueMock.closeIt(), "CloseIt should return true if successful");
		assertNull(bomLineIssueMock.getProcessMsg(), "Process message should be null");
		verify(bomLineIssueMock, times(1)).closeIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(2)).getDocStatus();
		verify(bomLineIssueMock, never()).completeIt();
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Closed);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(2)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_BEFORE_CLOSE);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_CLOSE);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseCorrectIt_ValidationBeforeFails()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseCorrectIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn("ValidationErrorBeforeReverseCorrect"); // Before_ReverseCorrect -
																	// validation fails

		assertFalse(bomLineIssueMock.reverseCorrectIt(),
				"ReverseCorrectIt should return false when before-reverseCorrect validation fails");
		assertEquals("ValidationErrorBeforeReverseCorrect",
				bomLineIssueMock.getProcessMsg(), "Process message should be set");
		verify(bomLineIssueMock, times(1)).reverseCorrectIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, never()).reverseIt(anyBoolean());
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(1)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		verify(modelValidationEngine, never())
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseCorrectIt_reverseItFails()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseCorrectIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.reverseIt(false)).thenReturn(null); // ReverseIt fails

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null); // Before_ReverseCorrect - validation OK

		assertFalse(bomLineIssueMock.reverseCorrectIt(),
				"ReverseCorrectIt should return false when reverseIt fails");
		assertNull(bomLineIssueMock.getProcessMsg(), "Process message should be null");
		verify(bomLineIssueMock, times(1)).reverseCorrectIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).reverseIt(false);
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(1)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		verify(modelValidationEngine, never())
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseCorrectIt_reverseItSucceeds_validationAfterFails()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseCorrectIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.reverseIt(false)).thenReturn(bomLineIssueMock); // ReverseIt succeeds

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null, "ValidationAfterError"); // Before_ReverseCorrect -
																	// validation OK

		assertFalse(bomLineIssueMock.reverseCorrectIt(),
				"ReverseCorrectIt should return false when validation fails");
		assertEquals("ValidationAfterError", bomLineIssueMock.getProcessMsg(),
				"Process message should be set");
		verify(bomLineIssueMock, times(1)).reverseCorrectIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).reverseIt(false);
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(2)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseCorrectIt_success()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseCorrectIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.reverseIt(false)).thenReturn(bomLineIssueMock); // ReverseIt succeeds
		when(bomLineIssueMock.getDocumentNo()).thenReturn("NewNumber");

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null, (String) null); // Validation OK

		assertTrue(bomLineIssueMock.reverseCorrectIt(),
				"ReverseCorrectIt should return true if successful");
		assertEquals("NewNumber", bomLineIssueMock.getProcessMsg(),
				"Process message should be the reversed document number");
		verify(bomLineIssueMock, times(1)).reverseCorrectIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).reverseIt(false);
		verify(bomLineIssueMock, times(1)).getDocumentNo();
		verify(bomLineIssueMock, times(1)).setProcessed(true);
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(2)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseAccrualIt_ValidationBeforeFails()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseAccrualIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn("ValidationErrorBeforeReverseAccrual"); // Before_ReverseAccrual -
																	// validation fails

		assertFalse(
				bomLineIssueMock.reverseAccrualIt(),
				"ReverseAccrualIt should return false when before-reverseAccrual validation fails");
		assertEquals("ValidationErrorBeforeReverseAccrual", bomLineIssueMock.getProcessMsg(),
				"Process message should be set");
		verify(bomLineIssueMock, times(1)).reverseAccrualIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, never()).reverseIt(anyBoolean());
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(1)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		verify(modelValidationEngine, never())
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseAccrualIt_reverseItFails()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseAccrualIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.reverseIt(true)).thenReturn(null); // ReverseIt fails

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null); // Before_ReverseAccrual - validation OK

		assertFalse(bomLineIssueMock.reverseAccrualIt(),
				"ReverseAccrualIt should return false when reverseIt fails");
		assertNull(bomLineIssueMock.getProcessMsg(), "Process message should be null");
		verify(bomLineIssueMock, times(1)).reverseAccrualIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).reverseIt(true);
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(1)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		verify(modelValidationEngine, never())
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseAccrualIt_reverseItSucceeds_validationAfterFails()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseAccrualIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.reverseIt(true)).thenReturn(bomLineIssueMock); // ReverseIt succeeds

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null, "ValidationAfterError"); // After validation fails

		assertFalse(bomLineIssueMock.reverseAccrualIt(),
				"ReverseCorrectIt should return false when validation fails");
		assertEquals("ValidationAfterError",
				bomLineIssueMock.getProcessMsg(), "Process message should be set");
		verify(bomLineIssueMock, times(1)).reverseAccrualIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).reverseIt(true);
		verify(bomLineIssueMock, never()).setProcessed(anyBoolean());
		verify(bomLineIssueMock, never()).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, never()).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(2)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		verifyNoMoreInteractions(modelValidationEngine);

	}

	@Test
	void reverseAccrualtIt_success()
	{

		doCallRealMethod().when(bomLineIssueMock).reverseAccrualIt();
		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		when(bomLineIssueMock.getModelValidationEngine()).thenReturn(modelValidationEngine);
		when(bomLineIssueMock.reverseIt(true)).thenReturn(bomLineIssueMock); // ReverseIt succeeds
		when(bomLineIssueMock.getDocumentNo()).thenReturn("NewNumber");

		when(modelValidationEngine.fireDocValidate(any(PO.class), anyInt()))
				.thenReturn((String) null, (String) null); // Validation OK

		assertTrue(bomLineIssueMock.reverseAccrualIt(),
				"ReverseCorrectIt should return true if successful");
		assertEquals("NewNumber", bomLineIssueMock.getProcessMsg(),
				"Process message should be the reversed document number");
		verify(bomLineIssueMock, times(1)).reverseAccrualIt();
		verify(bomLineIssueMock, times(1)).get_Logger();
		verify(bomLineIssueMock, times(1)).getProcessMsg();
		verify(bomLineIssueMock, times(1)).reverseIt(true);
		verify(bomLineIssueMock, times(1)).getDocumentNo();
		verify(bomLineIssueMock, times(1)).setProcessed(true);
		verify(bomLineIssueMock, times(1)).setDocStatus(DOCSTATUS_Reversed);
		verify(bomLineIssueMock, times(1)).setDocAction(DOCACTION_None);
		verify(bomLineIssueMock, times(2)).getModelValidationEngine();
		verifyNoMoreInteractions(bomLineIssueMock);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock,
						ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		verify(modelValidationEngine, times(1))
				.fireDocValidate((PO) bomLineIssueMock, ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		verifyNoMoreInteractions(modelValidationEngine);

	}

}
