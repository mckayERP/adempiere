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
package org.eevolution.model;

import static org.eevolution.model.I_PP_OrderReceipt.COLUMNNAME_PP_OrderReceipt_ID;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Close;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Complete;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_None;
import static org.eevolution.model.X_PP_OrderReceipt.DOCACTION_Prepare;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Closed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Completed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_InProgress;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Invalid;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Reversed;
import static org.eevolution.model.X_PP_OrderReceipt.DOCSTATUS_Voided;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.adempiere.engine.storage.StorageTestUtilities;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PeriodClosedException;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.MDocType;
import org.compiere.model.MStorage;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderReceipt")
class IT_MPPOrderReceipt extends CommonGWSetup {

	private final static int m_product_id = 145; // Patio Set
	private final static int m_attributeSetInstance_id = 0;
	private final static int S_Resource_ID = 50000;
	private final static int AD_Workflow_ID = 50018;
	private final static int PP_Product_BOM_ID = 145;

	private final static int m_warehouse_id = 50001; // GW Furniture Warehouse
	private static int m_locator_id;

	private MPPOrderReceipt ppOrderReceipt;
	private MPPOrder ppOrder;
	private BigDecimal initialQtyOrdered;
	private BigDecimal initialQtyReserved;
	private BigDecimal initialQtyAvailable;
	private BigDecimal currentQtyOrdered;
	private BigDecimal currentQtyReserved;
	private BigDecimal currentQtyAvailable;

	private BigDecimal qtyOrdered;
	private BigDecimal initialQtyDelivered;
	private StorageTestUtilities storageUtil;

	@BeforeAll
	static void localSetUpBeforeClass() {

		m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator().get_ID();

	}

	@BeforeEach
	void localSetUp() throws Exception {

		ppOrder = new MPPOrder(ctx, 0, trxName);
		ppOrder.setIsSOTrx(false);
		ppOrder.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ManufacturingOrder);
		ppOrder.setM_Warehouse_ID(m_warehouse_id);
		ppOrder.setM_Locator_ID(m_locator_id);
		ppOrder.setM_Product_ID(m_product_id);
		ppOrder.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		ppOrder.setS_Resource_ID(S_Resource_ID);
		ppOrder.setAD_Workflow_ID(AD_Workflow_ID);
		ppOrder.setPP_Product_BOM_ID(PP_Product_BOM_ID);
		ppOrder.setPriorityRule(MPPOrder.PRIORITYRULE_High);
		ppOrder.setDateOrdered(today);
		ppOrder.setDatePromised(today);
		ppOrder.setDateStartSchedule(today);
		ppOrder.setQty(Env.ONE);
		ppOrder.saveEx();

		ppOrderReceipt = new MPPOrderReceipt(ctx, 0, trxName);

		qtyOrdered = ppOrder.getQtyOrdered();

		storageUtil = new StorageTestUtilities(ctx, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);

	}

	private void determineInitialQtyAmounts() {

		storageUtil.determineInitialQtyAmounts();
		initialQtyOrdered = storageUtil.getInitialQtyOrdered();
		initialQtyReserved = storageUtil.getInitialQtyReserved();
		initialQtyAvailable = storageUtil.getInitialQtyAvailable();
		initialQtyDelivered = ppOrder.getQtyDelivered();

	}

	private void determineCurrentQtyAmounts() {

		storageUtil.determineCurrentQtyAmounts();
		currentQtyOrdered = storageUtil.getCurrentQtyOrdered();
		currentQtyReserved = storageUtil.getCurrentQtyReserved();
		currentQtyAvailable = storageUtil.getCurrentQtyAvailable();

	}

	private void initializeSingleReceipt() {

		determineInitialQtyAmounts();

		ppOrder.processIt(DOCACTION_Prepare);
		ppOrderReceipt.setParent(ppOrder);
		ppOrderReceipt.setMovementQty(qtyOrdered);
		ppOrderReceipt.setMovementDate(today);
		ppOrderReceipt.setM_Locator_ID(m_locator_id);
		ppOrderReceipt.saveEx();
		ppOrderReceipt.load(trxName);

		MStorage[] storages = MStorage.getOfProduct(ctx, m_product_id, trxName);
		assertEquals(1, storages.length,
				"Only one storage location should have been created.");

		determineCurrentQtyAmounts();

	}

	private void initializeSingleReceiptAndCompleteIt() {

		initializeSingleReceipt();
		ppOrderReceipt.processIt(DOCACTION_Complete);

	}

	@Test
	@Disabled("TODO: Add the translations")
	void checkTranslationsExist() {

		String translation = Msg.parseTranslation(ctx,
				MPPOrderReceipt.PROCESS_MSG_PARENT_ORDER_NOT_IN_PROGRESS);
		assertNotEquals("MPPOrderReceipt_ParentOrderNotInProgress", translation,
				"Translation not added. Check migrations");

		translation = Msg.parseTranslation(ctx,
				MPPOrderReceipt.PROCESS_MSG_MPPORDER_RECEIPTS_CANNOT_BE_REACTIVATED);
		assertNotEquals("MPPOrderReceipt_CannotBeReactivated", translation,
				"Translation not added. Check migrations");

	}

	@Test
	void beforeSave() {

		ppOrderReceipt.setPP_Order_ID(ppOrder.getPP_Order_ID());
		ppOrderReceipt.setM_Product_ID(ppOrder.getM_Product_ID());
		ppOrderReceipt.saveEx();

		assertEquals(MDocType.getDocType("MOR"), ppOrderReceipt.getC_DocType_ID(),
				"Document Type should be set");
		assertEquals(Env.getContextAsDate(ctx, "#Date"), ppOrderReceipt.getDateDoc(),
				"Document date should be set");

		Env.setContext(ctx, "#Date", (Timestamp) null);

		ppOrderReceipt.setPP_Order_ID(ppOrder.get_ID());
		ppOrderReceipt.setM_Product_ID(ppOrder.getM_Product_ID());
		ppOrderReceipt.saveEx();

		// Warning. This may fail if the test is run within a second of midnight
		assertEquals(today, ppOrderReceipt.getDateDoc(),
				"Document date should be set to current date");

		Env.setContext(ctx, "#Date", TimeUtil.getDay(System.currentTimeMillis()).toString());

	}

	@Test
	void consturctor_contextIdTrxName() {

		assertTrue(ppOrderReceipt instanceof MPPOrderReceipt);
		assertEquals(0, ppOrderReceipt.get_ID(),
				"ID of new record should be zero");
		assertEquals(0, ppOrderReceipt.getPP_Order_ID(),
				"PP_Order_ID should be zero");
		// Missing parent order
		assertThrows(AdempiereException.class, ppOrderReceipt::saveEx,
				"PP_Order_ID should be mandatory");

		ppOrderReceipt = new MPPOrderReceipt(ctx, 0, trxName);
		ppOrderReceipt.setPP_Order_ID(ppOrder.get_ID());
		assertThrows(AdempiereException.class, ppOrderReceipt::saveEx,
				"M_Product_ID should be mandatory");

		ppOrderReceipt = new MPPOrderReceipt(ctx, 0, trxName);
		ppOrderReceipt.setPP_Order_ID(ppOrder.get_ID());
		ppOrderReceipt.setM_Product_ID(ppOrder.getM_Product_ID());
		ppOrderReceipt.saveEx();

		int id = ppOrderReceipt.get_ID();
		assertTrue(id > 0, "ID of saved record should be > zero");

		ppOrderReceipt = new MPPOrderReceipt(ctx, id, trxName);

		assertEquals(id, ppOrderReceipt.get_ID(), "ID of saved record should match");

	}

	@Test
	void consturctor_contextResultSetTrxName() {

		boolean tested = false;

		ppOrderReceipt.setPP_Order_ID(ppOrder.get_ID());
		ppOrderReceipt.setM_Product_ID(ppOrder.getM_Product_ID());
		ppOrderReceipt.saveEx();

		int id = ppOrderReceipt.get_ID();

		String sql = "SELECT * FROM PP_OrderReceipt "
				+ "WHERE PP_OrderReceipt_ID=?";
		PreparedStatement pstmt = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				MPPOrderReceipt receipt = new MPPOrderReceipt(ctx, rs, trxName);
				assertNotNull(receipt, "Constructor returns null");
				assertEquals(rs.getInt(COLUMNNAME_PP_OrderReceipt_ID), receipt.get_ID(),
						"MPPOrderReceipt id doesn't match record set.");
				assertEquals(id, receipt.get_ID(),
						"MPPOrderReceipt id doesn't match parameter.");
				tested = true;
				break;
			}
			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try {
			if (pstmt != null)
				pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			fail(e.getMessage());
			pstmt = null;
		}

		assertTrue(tested, "Did not find the saved id");

	}

	@Test
	void getM_Warehouse_ID() {

		MPPOrderReceipt ppOrderReceipt = new MPPOrderReceipt(ctx, 0, trxName);
		assertEquals(0, ppOrderReceipt.getM_Warehouse_ID(),
				"With no locator and no parent, should be zero");

		ppOrderReceipt.setParent(ppOrder);
		assertEquals(ppOrder.getM_Warehouse_ID(), ppOrderReceipt.getM_Warehouse_ID(),
				"With no locator but a parent, should match the parent warehouse");

		int M_Warehouse_ID = 103; // HQ Warehouse
		int hq_default_locator_id = MWarehouse.get(ctx, M_Warehouse_ID, trxName)
				.getDefaultLocator().get_ID();
		ppOrderReceipt.setM_Locator_ID(hq_default_locator_id);
		assertNotEquals(ppOrder.getM_Warehouse_ID(), ppOrderReceipt.getM_Warehouse_ID(),
				"With locator and a parent, should not match the parent warehouse");
		assertEquals(103, ppOrderReceipt.getM_Warehouse_ID(),
				"Warehouse should match the locator warehouse");

	}

	@Test
	void getParent() {

		MPPOrderReceipt ppOrderReceipt = new MPPOrderReceipt(ctx, 0, trxName);
		assertNull(ppOrderReceipt.getParent(), "No ID, parent should be null");

		((X_PP_OrderReceipt) ppOrderReceipt).setPP_Order_ID(ppOrder.getPP_Order_ID());
		assertNotNull(ppOrderReceipt.getParent(),
				"If parent ID is set, parent should be not null");
		assertTrue(ppOrderReceipt.getParent() instanceof PO,
				"Return value should be an instance of PO");
		assertTrue(ppOrderReceipt.getParent() instanceof MPPOrder,
				"Return value should be an instance of MPPOrder");
		assertEquals(ppOrder.getPP_Order_ID(),
				((MPPOrder) ppOrderReceipt.getParent()).getPP_Order_ID(),
				"Parent ID should match the set ID");

	}

	@Test
	void processIt_parentNotInProgress() {

		ppOrderReceipt.setParent(ppOrder);

		try {

			assertFalse(ppOrderReceipt.processIt(DOCACTION_Prepare),
					"Should return false if parent ppOrder is not in progress");
			assertEquals(MPPOrderReceipt.PROCESS_MSG_PARENT_ORDER_NOT_IN_PROGRESS,
					ppOrderReceipt.getProcessMsg(),
					"Process message not as expected");

		} catch (Exception e) {

			e.printStackTrace();
			fail("Unable to test processIt()." + e.getMessage());

		}

	}

	@Test
	void processIt_parentInProgress() {

		ppOrderReceipt.setParent(ppOrder);
		ppOrder.processIt(DOCACTION_Prepare);

		try {

			assertTrue(ppOrderReceipt.processIt(DOCACTION_Prepare),
					"Should return true if parent ppOrder is in progress");
			assertEquals(DocAction.ACTION_Complete, ppOrderReceipt.getDocAction(),
					"DocAction should be set to Complete");
			assertNull(ppOrderReceipt.getProcessMsg(),
					"Process message should be null");

		} catch (Exception e) {

			e.printStackTrace();
			fail("Unable to test processIt()." + e.getMessage());

		}

	}

	@Test
	void prepareIt() {

		ppOrderReceipt.setParent(ppOrder);
		ppOrderReceipt.setDocAction(DOCACTION_Prepare); // To show there is no change.

		// Test Parent status drafted
		assertEquals(DOCSTATUS_Invalid, ppOrderReceipt.prepareIt(),
				"Status should be invalid");
		assertEquals(DOCACTION_Prepare, ppOrderReceipt.getDocAction(),
				"Action should be Prepare");
		assertNotNull(ppOrderReceipt.getProcessMsg(),
				"prepareIt() should set a process message");
		assertEquals(MPPOrderReceipt.PROCESS_MSG_PARENT_ORDER_NOT_IN_PROGRESS,
				ppOrderReceipt.getProcessMsg(),
				"prepareIt() did not set the correct process message");

		// Test all OK
		ppOrder.processIt(MPPOrder.DOCACTION_Prepare);
		assertEquals(DOCSTATUS_InProgress, ppOrderReceipt.prepareIt(),
				"Status should be InProgress");
		assertEquals(DOCACTION_Complete, ppOrderReceipt.getDocAction(),
				"Action should be Complete");
		assertNull(ppOrderReceipt.getProcessMsg(),
				"prepareIt() should not set a process message if successful");

	}

	@Test
	void completeIt() {

		initializeSingleReceipt();

		assertEquals(initialQtyAvailable, currentQtyAvailable,
				"Quantity available changed. It should remain the same");

		assertEquals(initialQtyAvailable.add(qtyOrdered), currentQtyOrdered,
				"Quantity on order should have changed");

		assertNotEquals(0, ppOrder.getM_MPolicyTicket_ID(),
				"Manufacturing order M_MPolicyTicket is zero.");
		assertEquals(qtyOrdered, ppOrder.getQtyReserved(),
				"Manufacturing order qty reserved should match the qty ordered.");
		assertEquals(currentQtyOrdered.subtract(initialQtyOrdered), ppOrder.getQtyReserved(),
				"Manufacturing order qty ordered should match the change in qtyordered in storage");

		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id,
				m_warehouse_id, m_attributeSetInstance_id, ppOrder.getM_MPolicyTicket_ID(),
				trxName);
		assertEquals(orderedStorage.getQtyOrdered(), qtyOrdered,
				"Ordered qty in storage is incorrect");

		assertEquals(initialQtyReserved, currentQtyReserved,
				"Quantity reserved should not change.");

		String docStatus = ppOrderReceipt.completeIt();
		assertNull(ppOrderReceipt.getProcessMsg(), "ProcessMsg should be null");
		assertEquals(DOCSTATUS_Completed, docStatus, "DocStatus not set as expected");

		determineCurrentQtyAmounts();

		assertEquals(initialQtyAvailable.add(qtyOrdered), currentQtyAvailable,
				"Quantity should have increased");

		assertEquals(initialQtyDelivered.add(qtyOrdered), ppOrder.getQtyDelivered(),
				"Manufacturing Order qty delivered should have increased");

		assertEquals(initialQtyAvailable, currentQtyOrdered,
				"Quantity on order should not have changed");

	}

	@Test
	void voidIt_draftReceipt() {

		initializeSingleReceipt();

		assertTrue(ppOrderReceipt.voidIt(),
				"Successful void should return true");
		assertEquals(Env.ZERO, ppOrderReceipt.getMovementQty(),
				"MovementQty should be zero");
		assertEquals(DOCSTATUS_Voided, ppOrderReceipt.getDocStatus(),
				"DocStatus should be voiced");
		assertEquals(DOCACTION_None, ppOrderReceipt.getDocAction(),
				"DocAction should be non");
		assertTrue(ppOrderReceipt.isProcessed(),
				"A voided receipt should be processed");

	}

	@Test
	void voidIt_CompletedReceipt() {

		initializeSingleReceiptAndCompleteIt();

		ppOrder.load(trxName);
		assertEquals(Env.ONE, ppOrder.getQtyDelivered(),
				"Manufacturing order should have qty one delivered");

		assertTrue(ppOrderReceipt.voidIt(), "Successful void should return true");
		assertEquals(Env.ONE, ppOrderReceipt.getMovementQty(),
				"MovementQty should not change");
		assertEquals(DOCSTATUS_Reversed, ppOrderReceipt.getDocStatus(),
				"DocStatus should be Reversed");
		assertEquals(DOCACTION_None, ppOrderReceipt.getDocAction(),
				"DocAction should be none");
		assertTrue(ppOrderReceipt.isProcessed(), "A voided receipt should be processed");

		assertTrue(ppOrderReceipt.getReversal_ID() > 0, "The reversal ID should be set");
		assertTrue(ppOrderReceipt.isReversal(), "This order should be a reversal");
		assertTrue(ppOrderReceipt.isReversalParent(),
				"This order should be the reversal parent");

		MPPOrderReceipt reversal = new MPPOrderReceipt(ctx, ppOrderReceipt.getReversal_ID(),
				trxName);
		assertEquals(Env.ONE.negate(), reversal.getMovementQty(),
				"Reversal amount should be negative");

		ppOrder.load(trxName);
		assertEquals(Env.ZERO, ppOrder.getQtyDelivered(),
				"Manufacturing order should have qty zero delivered");

	}

	@Test
	void closeIt_usingProcessIt() {

		initializeSingleReceipt();

		assertEquals(initialQtyAvailable, currentQtyAvailable,
				"Quantity available changed. It should remain the same");
		assertEquals(initialQtyAvailable.add(qtyOrdered), currentQtyOrdered,
				"Quantity on order should have changed");
		assertNotEquals(0, ppOrder.getM_MPolicyTicket_ID(),
				"Manufacturing order M_MPolicyTicket should be set");
		assertEquals(qtyOrdered, ppOrder.getQtyReserved(),
				"Manufacturing order qty reserved should match the qty ordered");
		assertEquals(currentQtyOrdered.subtract(initialQtyOrdered),
				ppOrder.getQtyReserved(),
				"Manufacturing order qty ordered should match the "
						+ "change in qtyordered in storage");
		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id,
				m_warehouse_id, m_attributeSetInstance_id,
				ppOrder.getM_MPolicyTicket_ID(), trxName);
		assertEquals(orderedStorage.getQtyOrdered(), qtyOrdered,
				"Ordered qty in storage is incorrect");
		assertEquals(initialQtyReserved, currentQtyReserved,
				"Quantity reserved should not change.");

		ppOrderReceipt.processIt(DOCACTION_Complete);
		assertTrue(ppOrderReceipt.processIt(DOCACTION_Close),
				"Closed order should return true");

		assertNull(ppOrderReceipt.getProcessMsg(), "ProcessMsg should be null");
		assertEquals(DOCSTATUS_Closed, ppOrderReceipt.getDocStatus(),
				"DocStatus not set as expected");
		assertEquals(DOCACTION_None, ppOrderReceipt.getDocAction(),
				"DocAction not set as expected");
		determineCurrentQtyAmounts();
		assertEquals(initialQtyAvailable.add(qtyOrdered), currentQtyAvailable,
				"Quantity should have increased");
		assertEquals(initialQtyDelivered.add(qtyOrdered), ppOrder.getQtyDelivered(),
				"Manufacturing Order qty delivered should have increased");
		assertEquals(initialQtyAvailable, currentQtyOrdered,
				"Quantity on order should not have changed");

	}

	@Test
	void closeIt_directCall() {

		initializeSingleReceipt();

		assertEquals(initialQtyAvailable, currentQtyAvailable,
				"Quantity available changed. It should remain the same");
		assertEquals(initialQtyAvailable.add(qtyOrdered), currentQtyOrdered,
				"Quantity on order should have changed");
		assertNotEquals(0, ppOrder.getM_MPolicyTicket_ID(),
				"Manufacturing order M_MPolicyTicket should be set");
		assertEquals(qtyOrdered, ppOrder.getQtyReserved(),
				"Manufacturing order qty reserved should match the qty ordered");
		assertEquals(currentQtyOrdered.subtract(initialQtyOrdered),
				ppOrder.getQtyReserved(),
				"Manufacturing order qty ordered should match the change in "
						+ "qtyordered in storage");
		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id,
				m_warehouse_id, m_attributeSetInstance_id,
				ppOrder.getM_MPolicyTicket_ID(), trxName);
		assertEquals(orderedStorage.getQtyOrdered(), qtyOrdered,
				"Ordered qty in storage is incorrect");
		assertEquals(initialQtyReserved, currentQtyReserved,
				"Quantity reserved should not change");

		assertTrue(ppOrderReceipt.closeIt(),
				"Closed order should return true");

		assertNull(ppOrderReceipt.getProcessMsg(), "ProcessMsg should be null");
		assertEquals(DOCSTATUS_Closed, ppOrderReceipt.getDocStatus(),
				"DocStatus not set as expected");
		assertEquals(DOCACTION_None, ppOrderReceipt.getDocAction(),
				"DocAction not set as expected");
		determineCurrentQtyAmounts();
		assertEquals(initialQtyAvailable.add(qtyOrdered), currentQtyAvailable,
				"Quantity should have increased");
		assertEquals(initialQtyDelivered.add(qtyOrdered), ppOrder.getQtyDelivered(),
				"Manufacturing Order qty delivered should have increased");
		assertEquals(initialQtyAvailable, currentQtyOrdered,
				"Quantity on order should not have changed");

	}

	@Test
	void reverseCorrectIt() {

		initializeSingleReceiptAndCompleteIt();
		ppOrderReceipt.voidIt();

		MPPOrderReceipt reversal = new MPPOrderReceipt(ctx,
				ppOrderReceipt.getReversal_ID(), trxName);

		assertEquals(today, reversal.getMovementDate(), "Should happen on the same day");

	}

	@Test
	void copy() {

		initializeSingleReceiptAndCompleteIt();

		MPPOrderReceipt copy = MPPOrderReceipt.copyFrom(ppOrderReceipt,
				today, today, false, trxName);

		assertFalse(copy.isReversal(), "Straight copy should not be marked as reversal");

	}

	@Test
	void copyWithDocNumber() {

		MPPOrderReceipt copy = createReversalAndCopyDocumentNo();

		String expectedDocumentNo = ppOrderReceipt.getDocumentNo() + Msg.getMsg(ctx, "^");
		assertEquals(expectedDocumentNo, copy.getDocumentNo(), "DocNumber should match");

	}

	private MPPOrderReceipt createReversalAndCopyDocumentNo() {

		initializeSingleReceiptAndCompleteIt();
		MDocType docType = MDocType.get(ctx, ppOrderReceipt.getC_DocType_ID(), trxName);
		docType.setIsCopyDocNoOnReversal(true);
		MPPOrderReceipt copy = MPPOrderReceipt.copyFrom(ppOrderReceipt,
				today, today, true, trxName);
		return copy;

	}

	@Test
	void copyWithOwnDocNumber() {

		MPPOrderReceipt copy = createReversalButDontCopyDocumentNo();

		String expectedDocumentNo = ppOrderReceipt.getDocumentNo() + Msg.getMsg(ctx, "^");
		assertNotEquals("DocNumber should not match", expectedDocumentNo, copy.getDocumentNo());

	}

	private MPPOrderReceipt createReversalButDontCopyDocumentNo() {

		initializeSingleReceiptAndCompleteIt();
		MDocType docType = MDocType.get(ctx, ppOrderReceipt.getC_DocType_ID(), trxName);
		docType.setIsCopyDocNoOnReversal(false);
		MPPOrderReceipt copy = MPPOrderReceipt.copyFrom(ppOrderReceipt, today, today, true,
				trxName);
		return copy;

	}

	@Test
	void reverseAccrualIt() {

		turnOffAutoPeriodControl(ctx, AD_CLIENT_ID);
		openPeriod(ctx, AD_ORG_ID, today, "MOR", trxName);
		initializeSingleReceiptAndCompleteIt();
		closePeriod(ctx, AD_ORG_ID, today, "MOR", trxName);

		Timestamp newLoginDate = TimeUtil.addMonths(today, 1);
		setLoginDate(ctx, newLoginDate);

		openPeriod(ctx, AD_ORG_ID, newLoginDate, "MOR", trxName);

		ppOrderReceipt.voidIt();

		MPPOrderReceipt reversal = new MPPOrderReceipt(ctx, ppOrderReceipt.getReversal_ID(),
				trxName);

		assertEquals(newLoginDate, reversal.getMovementDate(),
				"Should happen on login date");

		openPeriod(ctx, AD_ORG_ID, today, "MOR", trxName);
		resetAutoPeriodControl(ctx, AD_CLIENT_ID);
		resetLoginDate(ctx);

	}

	@Test
	void getModelValidationEngine() {

		assertNull(ppOrderReceipt.modelValidationEngine,
				"ppOrderReceipt.modelValidationEngine should be null if not called");
		assertNotNull(ppOrderReceipt.getModelValidationEngine(),
				"ModelValidationEngine should not be null if called");

	}

	@Test
	void testPeriodClosed() {

		initializeSingleReceipt();
		turnOffAutoPeriodControl(ctx, AD_CLIENT_ID);
		closePeriod(ctx, AD_ORG_ID, today, "MOR", trxName);
		assertThrows(PeriodClosedException.class, () -> {
			ppOrderReceipt.testPeriodOpen(today);
		});
		openPeriod(ctx, AD_ORG_ID, today, "MOR", trxName);
		resetAutoPeriodControl(ctx, AD_CLIENT_ID);

	}

	@Test
	void testPeriodOpen() {

		initializeSingleReceipt();
		turnOffAutoPeriodControl(ctx, AD_CLIENT_ID);
		openPeriod(ctx, AD_ORG_ID, today, "MOR", trxName);
		ppOrderReceipt.testPeriodOpen(today);
		resetAutoPeriodControl(ctx, AD_CLIENT_ID);

	}

}
