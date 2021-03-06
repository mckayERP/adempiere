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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;

import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
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

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderBOMLineIssue")
@ExtendWith(MockitoExtension.class)
class Test_MPPOrderBOMLineIssue_BasicMethods extends CommonUnitTestSetup {

	private MPPOrderBOMLineIssue bomLineIssueMock;
	private MPPOrderBOMLine bomLineMock;
	private Timestamp today = TimeUtil.getDay(System.currentTimeMillis());

	@BeforeEach
	void localSetup() {

		bomLineIssueMock = mock(MPPOrderBOMLineIssue.class);
		bomLineMock = mock(MPPOrderBOMLine.class);

	}

	@Test
	void setParentNull() {

		doCallRealMethod().when(bomLineIssueMock).setParent(any());
		assertThrows(NullPointerException.class, () -> {
			bomLineIssueMock.setParent(null);
		});

	}

	@Test
	void setParentNotNull() {

		when(bomLineMock.getPP_Order_BOMLine_ID()).thenReturn(1);
		when(bomLineMock.getM_Product_ID()).thenReturn(2);

		doCallRealMethod().when(bomLineIssueMock).setParent(any());

		bomLineIssueMock.setParent(bomLineMock);

		verify(bomLineIssueMock, times(1)).setPP_Order_BOMLine_ID(1);
		verify(bomLineIssueMock, times(1)).setM_Product_ID(2);

	}


	@Test
	void addDescription_null() {

		bomLineIssueMock.addDescription((String) null);
		verify(bomLineIssueMock, never()).setDescription(anyString());

	}

	@Test
	void addDescription_empty() {

		doCallRealMethod().when(bomLineIssueMock).addDescription(anyString());

		bomLineIssueMock.addDescription("");
		verify(bomLineIssueMock, never()).setDescription(anyString());

	}

	@Captor
	ArgumentCaptor<String> stringCaptor;

	@Test
	void addDescription_currentNull() {

		doCallRealMethod().when(bomLineIssueMock).addDescription(anyString());
		when(bomLineIssueMock.getDescription()).thenReturn(null);

		bomLineIssueMock.addDescription("NewDescription");
		verify(bomLineIssueMock).setDescription(stringCaptor.capture());

		assertEquals("NewDescription", stringCaptor.getValue(),
				"Description not set as expected");

	}

	@Test
	void addDescription_notNull() {

		doCallRealMethod().when(bomLineIssueMock).addDescription(anyString());
		when(bomLineIssueMock.getDescription()).thenReturn("CurrentDescription");

		bomLineIssueMock.addDescription("NewDescription");
		verify(bomLineIssueMock).setDescription(stringCaptor.capture());

		assertEquals("CurrentDescription | NewDescription", stringCaptor.getValue(),
				"Description not set as expected");

	}

	@Test
	void getM_LocatorTo_ID() {

		doCallRealMethod().when(bomLineIssueMock).getM_LocatorTo_ID();
		assertEquals(0, bomLineIssueMock.getM_LocatorTo_ID(), "No locator 'To' is used");

	}

	@Test
	void getM_MPoligyTicket_ID() {

		doCallRealMethod().when(bomLineIssueMock).getM_MPolicyTicket_ID();
		assertEquals(0, bomLineIssueMock.getM_MPolicyTicket_ID(),
				"MPPOrderBOMLineIssue should not have a material policy ticket");

	}

	@Test
	void getM_AttributeSetInstanceTo_ID() {

		doCallRealMethod().when(bomLineIssueMock).getM_AttributeSetInstanceTo_ID();
		assertEquals(
				0, bomLineIssueMock.getM_AttributeSetInstanceTo_ID(),
				"No Attribute Set Instance 'To' is used");

	}

	@Test
	void getDateAcct() {

		doCallRealMethod().when(bomLineIssueMock).getDateAcct();
		when(bomLineIssueMock.getMovementDate()).thenReturn(today);

		assertEquals(today, bomLineIssueMock.getDateAcct(), "Date should match movementDate");

	}

	@Test
	void isSOTrx() {

		doCallRealMethod().when(bomLineIssueMock).isSOTrx();
		assertTrue(bomLineIssueMock.isSOTrx(), "Manufacturing Order issues are sales transactions");

	}

	@Test
	void getReversalLine_ID() {

		doCallRealMethod().when(bomLineIssueMock).getReversalLine_ID();
		when(bomLineIssueMock.getReversal_ID()).thenReturn(1);

		assertEquals(
				1, bomLineIssueMock.getReversalLine_ID(),
				"Reversal line id should match the reversal id");

	}

	@Test
	void getC_Currency_ID() {

		doCallRealMethod().when(bomLineIssueMock).getC_Currency_ID();

		MClient clientMock = mock(MClient.class);
		when(clientMock.getC_Currency_ID()).thenReturn(1);

		bomLineIssueMock.client = clientMock;

		assertEquals(1, bomLineIssueMock.getC_Currency_ID(), "Currency should match client");

	}

	@Test
	void getC_ConversionType_ID() {

		doCallRealMethod().when(bomLineIssueMock).getC_ConversionType_ID();
		when(bomLineIssueMock.getDefaultConverstionType_ID()).thenReturn(1);

		assertEquals(
				1, bomLineIssueMock.getC_ConversionType_ID(),
				"Currency conversion type should match the default for the client");

	}

	@Test
	void getPriceActual() {

		doCallRealMethod().when(bomLineIssueMock).getPriceActual();
		assertNull(bomLineIssueMock.getPriceActual(), "Price Actual should be null");

	}

	@Test
	void getPriceActualCurrency() {

		doCallRealMethod().when(bomLineIssueMock).getPriceActualCurrency();
		assertNull(bomLineIssueMock.getPriceActualCurrency(),
				"Price Actual Currency should be null");

	}

	@Test
	void getReversalDocumentLine() {

		MPPOrderBOMLineIssue reversalMock = mock(MPPOrderBOMLineIssue.class);

		doCallRealMethod().when(bomLineIssueMock).getReversalDocumentLine();
		when(bomLineIssueMock.getReversal()).thenReturn(reversalMock);
		assertEquals(
				reversalMock, bomLineIssueMock.getReversalDocumentLine(),
				"Reversal Document Line hould match the reversal");

	}

	@Test
	void isReversalParent_True() {

		doCallRealMethod().when(bomLineIssueMock).isReversalParent();
		when(bomLineIssueMock.getPP_Order_BOMLineIssue_ID()).thenReturn(1);
		when(bomLineIssueMock.getReversal_ID()).thenReturn(2);

		assertTrue(
				bomLineIssueMock.isReversalParent(),
				"PP_Order_BOMLineIssue should be a reversal parent");

	}

	@Test
	void isReversalParent_false() {

		doCallRealMethod().when(bomLineIssueMock).isReversalParent();
		when(bomLineIssueMock.getPP_Order_BOMLineIssue_ID()).thenReturn(2);
		when(bomLineIssueMock.getReversal_ID()).thenReturn(1);

		assertFalse(
				bomLineIssueMock.isReversalParent(),
				"PP_Order_BOMLineIssue should not be a reversal parent");

	}

	@Test
	void isReversalParent_noReversal() {

		doCallRealMethod().when(bomLineIssueMock).isReversalParent();
		when(bomLineIssueMock.getPP_Order_BOMLineIssue_ID()).thenReturn(2);
		when(bomLineIssueMock.getReversal_ID()).thenReturn(0);

		assertFalse(
				bomLineIssueMock.isReversalParent(),
				"PP_Order_BOMLineIssue should not be a reversal parent");

	}

	@Test
	void setM_MPolicyTicket_ID() {

		doCallRealMethod().when(bomLineIssueMock).setM_MPolicyTicket_ID(anyInt());

		bomLineIssueMock.setM_MPolicyTicket_ID(1);
		verify(bomLineIssueMock).setM_MPolicyTicket_ID(1);
		verifyNoMoreInteractions(bomLineIssueMock);

	}

	@Test
	void getM_Warehouse_ID_WithNoLocatorOrParent() {

		doCallRealMethod().when(bomLineIssueMock).getM_Warehouse_ID();
		when(bomLineIssueMock.getM_Locator_ID()).thenReturn(0);
		when(bomLineIssueMock.getParent()).thenReturn(null);

		assertEquals(
				0, bomLineIssueMock.getM_Warehouse_ID(),
				"With no locator and no parent, should return 0");

	}

	@Test
	void getM_Warehouse_ID_WithLocator() {

		MLocator locatorMock = mock(MLocator.class);
		when(locatorMock.getM_Warehouse_ID()).thenReturn(1);

		doCallRealMethod().when(bomLineIssueMock).getM_Warehouse_ID();
		when(bomLineIssueMock.getM_Locator_ID()).thenReturn(1);
		when(bomLineIssueMock.getM_Locator()).thenReturn(locatorMock);

		assertEquals(
				1, bomLineIssueMock.getM_Warehouse_ID(),
				"With locator and a parent, should not match the parent warehouse");

	}

	@Test
	void getM_Warehouse_ID_WithParent() {

		MPPOrderBOMLine parentMock = mock(MPPOrderBOMLine.class);
		when(parentMock.getM_Warehouse_ID()).thenReturn(1);

		doCallRealMethod().when(bomLineIssueMock).getM_Warehouse_ID();
		when(bomLineIssueMock.getM_Locator_ID()).thenReturn(0);
		when(bomLineIssueMock.getParent()).thenReturn(parentMock);

		assertEquals(
				1, bomLineIssueMock.getM_Warehouse_ID(),
				"With no locator and a parent, should not match the parent warehouse");

	}

	@Test
	void getMovementType() {

		doCallRealMethod().when(bomLineIssueMock).getMovementType();
		assertEquals(
				MTransaction.MOVEMENTTYPE_ProductionPlus, bomLineIssueMock.getMovementType(),
				"MovementType should be fixed");

	}

	@Test
	void getParent() {

		MPPOrderBOMLine parentMock = mock(MPPOrderBOMLine.class);

		doCallRealMethod().when(bomLineIssueMock).getParent();
		doCallRealMethod().when(bomLineIssueMock).setParent(any());
		bomLineIssueMock.setParent(parentMock);

		assertEquals(parentMock, (MPPOrderBOMLine) bomLineIssueMock.getParent(),
				"getParent should return the parent BOM Linet");

	}

	@Test
	void getParentOrder() {

		MPPOrder orderMock = mock(MPPOrder.class);
		MPPOrderBOMLine lineMock = mock(MPPOrderBOMLine.class);
		when(lineMock.getPP_Order()).thenReturn(orderMock);

		doCallRealMethod().when(bomLineIssueMock).getParentOrder();
		when(bomLineIssueMock.getParent()).thenReturn(lineMock);

		assertEquals(orderMock, bomLineIssueMock.getParentOrder(),
				"getParentOrder did not return the correct order");

	}

	@Test
	void getSummary() {

		MProduct productMock = mock(MProduct.class);
		when(productMock.getValue()).thenReturn("ProductValue");
		when(productMock.getName()).thenReturn("ProductName");

		doCallRealMethod().when(bomLineIssueMock).getSummary();
		when(bomLineIssueMock.getM_Product()).thenReturn(productMock);
		when(bomLineIssueMock.getMovementQty()).thenReturn(Env.ONE);
		when(bomLineIssueMock.getMovementDate()).thenReturn(today);

		assertEquals(
				"ProductValue_ProductName/Qty 1/" + today.toString(), bomLineIssueMock.getSummary(),
				"Summary string not as expected");

	}

	@Test
	void getDocumentInfo() {

		MDocType docTypeMock = mock(MDocType.class);
		when(docTypeMock.getName()).thenReturn("Production Issue");

		doCallRealMethod().when(bomLineIssueMock).getDocumentInfo();
		when(bomLineIssueMock.getDocumentNo()).thenReturn("123456");
		when(bomLineIssueMock.getDocType()).thenReturn(docTypeMock);

		assertEquals(
				"Production Issue 123456", bomLineIssueMock.getDocumentInfo(),
				"DocumentInfo string not as expected");

	}

	@Test
	void createPDF() {

		doCallRealMethod().when(bomLineIssueMock).createPDF();
		assertNull(bomLineIssueMock.createPDF(), "CreatePDF should return null");

	}

	@Test
	void getProcessMsg() {

		doCallRealMethod().when(bomLineIssueMock).getProcessMsg();
		doCallRealMethod().when(bomLineIssueMock).setProcessMsg(anyString());

		bomLineIssueMock.setProcessMsg("TestMessage");
		assertEquals("TestMessage",
				bomLineIssueMock.getProcessMsg(), "Process Message not returned as expected");

	}

	@Test
	void getDoc_User_ID_noParent() {

		doCallRealMethod().when(bomLineIssueMock).getDoc_User_ID();
		when(bomLineIssueMock.getParentOrder()).thenReturn(null);

		assertEquals(0, bomLineIssueMock.getDoc_User_ID(), "Doc_User_ID not returned as expected");

	}

	@Test
	void getDoc_User_ID_parent() {

		MPPOrder orderMock = mock(MPPOrder.class);
		when(orderMock.getPlanner_ID()).thenReturn(123);

		when(bomLineIssueMock.getParentOrder()).thenReturn(orderMock);

		doCallRealMethod().when(bomLineIssueMock).getDoc_User_ID();

		assertEquals(123, bomLineIssueMock.getDoc_User_ID(),
				"Doc_User_ID not returned as expected");

	}

	@Test
	void getApprovalAmt() {

		doCallRealMethod().when(bomLineIssueMock).getApprovalAmt();

		assertEquals(Env.ZERO, bomLineIssueMock.getApprovalAmt(), "Approval amount should be zero");

	}

}
