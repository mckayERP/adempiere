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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;

import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
class Test_MPPOrderReceipt_BeforeSave extends CommonUnitTestSetup {

	private final MPPOrderReceipt ppOrderReceiptMock = mock(MPPOrderReceipt.class);
	private final Timestamp today = TimeUtil.getDay(System.currentTimeMillis());

	@Captor private ArgumentCaptor<Timestamp> dateCaptor;

	@Nested
	@DisplayName("MPPOrderReceipt when beforeSave is called")
	class testMPPOrderReceipt_beforeSave {

		@Test
		@DisplayName("If the document type ID is zero, it should be set")
		void testMPPOrderReceipt_beforeSave_DocTypeSetWhenIDZero() {
	
			when(ppOrderReceiptMock.beforeSave(anyBoolean())).thenCallRealMethod();
			when(ppOrderReceiptMock.getC_DocType_ID()).thenReturn(0);
			when(ppOrderReceiptMock.getDefaultDocType()).thenReturn(123);
			when(ppOrderReceiptMock.getDateDoc())
				.thenReturn(today);
			
			assertTrue(ppOrderReceiptMock.beforeSave(true), 
					"MPPOrderReceipt.beforeSave() should return true");
			verify(ppOrderReceiptMock, times(1)).setC_DocType_ID(123);
			verify(ppOrderReceiptMock, never()).setDateDoc(any());
	
		}
	
		@Test
		@DisplayName("If the document type id is not zero, it should not be set")
		void testMPPOrderReceipt_beforeSave_DocTypeNotSetWhenIDNotZero() {
	
			when(ppOrderReceiptMock.beforeSave(anyBoolean())).thenCallRealMethod();
			when(ppOrderReceiptMock.getC_DocType_ID()).thenReturn(123);
			when(ppOrderReceiptMock.getDateDoc())
				.thenReturn(today);
			
			assertTrue(ppOrderReceiptMock.beforeSave(true), 
					"MPPOrderReceipt.beforeSave() should return true");
			verify(ppOrderReceiptMock, never()).setC_DocType_ID(anyInt());
	
		}
	
		@Test
		@DisplayName("If the document date is null, it should be set from the context")
		void testMPPOrderReceipt_beforeSave_setDateDocFromEnv() {
	
			when(ppOrderReceiptMock.beforeSave(anyBoolean())).thenCallRealMethod();
			when(((X_PP_OrderReceipt) ppOrderReceiptMock).getC_DocType_ID()).thenReturn(123);
			when(ppOrderReceiptMock.getCtx()).thenReturn(ctx);
			when(ppOrderReceiptMock.getDateDoc())
				.thenReturn(null)
				.thenReturn(today);
			
			ppOrderReceiptMock.beforeSave(true);
			verify(ppOrderReceiptMock, times(1)).setDateDoc(dateCaptor.capture());
			assertEquals(loginDate, dateCaptor.getValue(), "The date was not set as expected");
			
		}
	
	}

}
