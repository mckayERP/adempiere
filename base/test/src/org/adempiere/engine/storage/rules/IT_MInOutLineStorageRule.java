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

import static org.compiere.model.X_C_DocType.DOCBASETYPE_SalesOrder;
import static org.compiere.model.X_C_DocType.DOCSUBTYPESO_ReturnMaterial;
import static org.compiere.model.X_C_DocType.DOCBASETYPE_PurchaseOrder;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_CustomerReturns;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_VendorReturns;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_CustomerShipment;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_VendorReceipts;

import static org.adempiere.test.TestUtilities.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.adempiere.engine.storage.StorageTestUtilities;
import org.adempiere.test.CommonGWData;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeUse;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutConfirm;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MRMA;
import org.compiere.model.MRMALine;
import org.compiere.model.MStorage;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Storage")
@Tag("StorageRule")
@Tag("MInOutLineStorageRule")
class IT_MInOutLineStorageRule extends CommonGWSetup {

	private static final int GW_HQ_WAREHOUSE_ID = CommonGWData.HQ_WAREHOUSE_ID;
	private static final int PRODUCT_CATEGORY_STANDARD_ID 
								= CommonGWData.PRODUCT_CATEGORY_STANDARD_ID; 
	private static final int TAX_CATEGORY_STANDARD_ID 
								= CommonGWData.TAX_CATEGORY_STANDARD_ID;
	private static final int SUPPLIER_PRICELIST_ID 
								= CommonGWData.SUPPLIER_PRICELIST_ID;
	private static final int ADMIN_USER_ID = CommonGWData.AD_USER_ID;
	
	private static int salesRep_id = ADMIN_USER_ID;
	private static int m_warehouse_id = GW_HQ_WAREHOUSE_ID;
	private static int m_attributeSetInstance_id = 0;

	private static BigDecimal qtyOrdered = Env.ONE;
	private static BigDecimal qtyReceived = Env.ONE;
	private static BigDecimal qtySold = Env.ONE;
	private static BigDecimal qtyDelivered = Env.ONE;

	private static int m_locator_id;
	private static Timestamp now = new Timestamp(System.currentTimeMillis());
	private static MProduct product_withASI;
	private static MProduct product_withoutASI;
	private static int m_product_id;
	private static int m_productWithASI_id;
	private static int m_productWithoutASI_id;
	
	private StorageTestUtilities storageUtil;

	private MOrder order;

	private MOrderLine orderLine;

	private MInOutLine mrLine;


	@BeforeAll
	static void localSetUpBeforeClass() {

		product_withoutASI = new MProduct(ctx, 0, trxName);
		product_withoutASI.setName("ProductUnderTest_" + randomString(4));
		product_withoutASI.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		product_withoutASI.setM_Product_Category_ID(
				PRODUCT_CATEGORY_STANDARD_ID);  
		product_withoutASI.setC_TaxCategory_ID(TAX_CATEGORY_STANDARD_ID);
		product_withoutASI.setIsPurchased(true);
		product_withoutASI.setIsSold(true);
		product_withoutASI.setIsStocked(true);
		product_withoutASI.saveEx();
		product_withoutASI.load(trxName);
		m_productWithoutASI_id = product_withoutASI.getM_Product_ID();
		
		m_locator_id = MWarehouse.get(ctx, m_warehouse_id)
				.getDefaultLocator().getM_Locator_ID();
				
		MPriceListVersion plv = new MPriceListVersion(ctx, 
				SUPPLIER_PRICELIST_ID, trxName);
		MProductPrice price = new MProductPrice(plv, product_withoutASI.getM_Product_ID(),
								Env.ONE, Env.ONE, Env.ONE);
		price.setIsActive(true);
		price.saveEx();

		MAttributeSet attributeSet = new MAttributeSet(ctx, 0, trxName);
		attributeSet.setName("TestAttributeSet");
		attributeSet.setDescription("Just for testing");
		attributeSet.setIsActive(true);
		attributeSet.setIsSerNo(false);
		attributeSet.setIsLot(false);
		attributeSet.setIsGuaranteeDate(false);
		attributeSet.setMandatoryType(MAttributeSet.MANDATORYTYPE_AlwaysMandatory);
		attributeSet.saveEx();
		
		MAttribute attribute = new MAttribute(ctx, 0, trxName);
		attribute.setName("TestAttribute");
		attribute.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attribute.setIsMandatory(true);
		attribute.setIsInstanceAttribute(false);
		attribute.saveEx();
		attribute.saveEx();
		
		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();
		
		MAttributeSetInstance attributeSetInstance = new MAttributeSetInstance(ctx, 0, trxName);
		attributeSetInstance.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeSetInstance.saveEx();
		
		MAttributeValue attributeValue = new MAttributeValue(ctx, 0, trxName);
		attributeValue.setM_Attribute_ID(attribute.get_ID());
		attributeValue.setName("TestAttribute");
		attributeValue.setValue("A Product Description");
		attributeValue.saveEx();

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, 0, trxName);
		attributeInstance.setM_Attribute_ID(attribute.get_ID());
		attributeInstance.setM_AttributeSetInstance_ID(attributeSetInstance.get_ID());
		attributeInstance.setM_AttributeValue_ID(attributeValue.get_ID());
		attributeInstance.saveEx();

		product_withASI = new MProduct(ctx, 0, trxName);
		product_withASI.setName("ProductUnderTest_" + randomString(4));
		product_withASI.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		product_withASI.setM_Product_Category_ID(PRODUCT_CATEGORY_STANDARD_ID);  // GW Standard
		product_withASI.setC_TaxCategory_ID(TAX_CATEGORY_STANDARD_ID);  // GW Standard
		product_withASI.setIsPurchased(true);
		product_withASI.setIsStocked(true);
		product_withASI.setM_AttributeSet_ID(attributeSet.get_ID());
		product_withASI.setM_AttributeSetInstance_ID(attributeSetInstance.get_ID());
		product_withASI.saveEx();
		product_withASI.load(trxName);
		m_productWithASI_id = product_withASI.getM_Product_ID();

		price = new MProductPrice(plv, product_withASI.getM_Product_ID(),
				Env.ONE, Env.ONE, Env.ONE);
		price.setIsActive(true);
		price.saveEx();

	}

	@BeforeEach
	void localSetup() {
		
		m_product_id = m_productWithoutASI_id;
		m_attributeSetInstance_id = 0;
		
		storageUtil = new StorageTestUtilities(ctx, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);
		storageUtil.setbPartner_id(SEEDFARM_ID);
		storageUtil.setbPartnerLocation_id(SEEDFARM_LOCATION_ID);

		qtyOrdered = Env.ONE;
		qtyReceived = Env.ONE;
		qtySold = Env.ONE;
		qtyDelivered = Env.ONE;
	}
	



	private void assertMaterialPolicyTicketsOnPOSOrder(MInOutLine shipLine) {
		
		List<MInOutLineMA> lineMAs = MInOutLineMA.get(ctx, shipLine.getM_InOutLine_ID(), trxName);
		assertEquals(1, lineMAs.size(), "Shipment should have a single Material Allocation");
		
		MInOutLineMA lineMA = lineMAs.get(0);
		int lineMA_mpt_id = lineMA.getM_MPolicyTicket_ID();
		assertNotEquals(0, lineMA_mpt_id, 
				"Shipment Line MA M_MPolicyTicket should not be zero");
		assertEquals(qtySold, lineMA.getMovementQty(), 
				"Shipment Line MA quantity should equal the qty sold");
		MMPolicyTicket ticket = (MMPolicyTicket) lineMA.getM_MPolicyTicket();
	
		assertTrue(storageUtil.isPolicyTicketFoundInEmptyOnHandStorage(lineMA_mpt_id), 
				"Couldn't find the Shipment lineMA MPolicyTicket in storage");
		
		int orderLineMPT_id = orderLine.getM_MPolicyTicket_ID();
		assertTrue(storageUtil.isPolicyTicketFoundInEmptyReservedStorage(orderLineMPT_id),
				"Couldn't find the orderLine MPolicyTicket in storage");
		
		assertNotEquals(shipLine, (MInOutLine) ticket.getM_InOutLine(), 
				"MPolicyTicket reference line should NOT match the shipment line");		
		
	}

	private void assertPOSData() {
		
		MInOutLine shipLine = assertPOSShipmentData();
		assertMaterialPolicyTicketsOnPOSOrder(shipLine);
		assertQuantitiesInStorageForPOSOrder(shipLine);
		
		assertEquals(m_attributeSetInstance_id, shipLine.getM_AttributeSetInstance_ID(),
				"Shipment Line MA ASI should match the product ASI");

	}

	private MInOutLine assertPOSShipmentData() {
		MInOut shipment = (MInOut) new Query(ctx, MInOut.Table_Name, "C_Order_ID=?", trxName)
										.setParameters(order.getC_Order_ID())
										.setClient_ID()
										.firstOnly();
	
		assertNotNull(shipment, "Can't find shipment");
		assertTrue(shipment.getM_InOut_ID()>0, "Shipment ID not set");
		assertEquals(MInOut.DOCSTATUS_Completed, shipment.getDocStatus(), "Shipment not completed");		
		MInOutLine[] shipLines = shipment.getLines();
		assertEquals(1, shipLines.length, "Should only have one shipment line");
		MInOutLine shipLine = shipment.getLines()[0];
		assertEquals(0, shipLine.getM_MPolicyTicket_ID(), "Shipment Line M_MPolicyTicket should be zero");
	
		return shipLine;
	}

	private void assertQuantitiesInStorageForPOSOrder(MInOutLine shipLine) {
	
		assertEquals(1, storageUtil.getChangeInNumberOfStorageLocations(),
				"Should have one new storage locations");
	
		assertEquals(qtyOrdered.negate(), storageUtil.getChangeInQtyAvailable(),
				"Quantity available should have changed for a POS Sale.");
		assertEquals(qtyDelivered, shipLine.getMovementQty(),
				"Material Receipt Line qtyDelivered");
		assertEquals(qtySold, orderLine.getQtyDelivered(),
				"OrderQtyDelivered should show qty sold");
		assertEquals(Env.ZERO, storageUtil.getCurrentQtyOrdered(),
				"Ordered qty in storage should be zero");
		assertEquals(Env.ZERO, storageUtil.getCurrentQtyReserved(),
				"Reserved qty in storage should be zero");
		
		storageUtil.assertCorrectChangeInStorageQuantities(
				qtyDelivered.negate(), 
				qtyDelivered.negate(), 
				Env.ZERO, 
				Env.ZERO);
		
		assertEquals(orderLine.getQtyDelivered(), orderLine.getQtyOrdered(),
				"OrderLine should be marked as delivered");
		assertTrue(order.isDelivered(),
				"Order should be marked as delivered");
		
	}

	private void assertStandardOrderShipmentData(MInOutLine shipLine) {
		
		storageUtil.determineCurrentQtyAmounts();		
		
		List<MInOutLineMA> lineMAs = MInOutLineMA.get(ctx, shipLine.getM_InOutLine_ID(), trxName);
		assertEquals(1, lineMAs.size(),
				"Shipment should have a single Material Allocation");
		
		MInOutLineMA lineMA = lineMAs.get(0);
		assertNotEquals(0, lineMA.getM_MPolicyTicket_ID(),
				"Shipment Line MA M_MPolicyTicket should not be zero");
		assertEquals(qtyDelivered, lineMA.getMovementQty(),
				"Shipment Line MA quantity should equal the qty sold");
	
		assertEquals(0, storageUtil.getChangeInNumberOfStorageLocations(),
				"Should have no new storage locations");
	
		assertTrue(storageUtil.isPolicyTicketFoundInOnHandStorage(lineMA.getM_MPolicyTicket_ID()),
				"Couldn't find the Shipment lineMA MPolicyTicket in storage");
		assertTrue(storageUtil.isPolicyTicketFoundInReservedStorage(orderLine.getM_MPolicyTicket_ID()),
				"Couldn't find the orderLine MPolicyTicket in storage");
		
		storageUtil.assertCorrectChangeInStorageQuantities(
				Env.ZERO,
				qtyDelivered.negate(),
				qtyDelivered.negate(),
				Env.ZERO
				);
	
		assertEquals(qtyOrdered.subtract(qtyDelivered).intValue(), 
				storageUtil.getCurrentQtyReserved().intValue(),
				"Reserved qty in storage:");
	
		assertEquals(qtyDelivered, shipLine.getMovementQty(),
				"Material Receipt Line qtyDelivered");
	
		assertEquals(qtyDelivered, orderLine.getQtyDelivered(),
				"OrderQtyDelivered should show qty shipped");
	
		assertNotEquals(orderLine.getQtyDelivered(), orderLine.getQtyOrdered(),
				"OrderLine should not be marked as delivered");
		assertFalse(order.isDelivered(),
				"Order should not be marked as delivered");
		
	}

	private MInOutLine createAndCompleteShipment() {
		
		storageUtil.determineInitialQtyAmounts();
		
		MInOut shipment = new MInOut(order, order.getC_DocType().getC_DocTypeShipment_ID(), now);
		shipment.saveEx();
		
		MInOutLine shipLine = new MInOutLine(shipment);
		shipLine.setM_Product_ID(m_product_id);
		shipLine.setM_Locator_ID(m_locator_id);
		shipLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
		shipLine.setQty(qtyDelivered);
		shipLine.saveEx();
	
		shipment.processIt(MInOut.DOCACTION_Prepare);
		
		Arrays.asList(shipment.getConfirmations(true)).stream()
			.forEach(confirm -> {
				Arrays.asList(confirm.getLines(true)).stream()
					.forEach(confirmLine -> {
						confirmLine.setConfirmedQty(confirmLine.getTargetQty());
						confirmLine.saveEx();
					});
				confirm.processIt(MInOutConfirm.DOCACTION_Complete);
				confirm.saveEx();
			});
		
		shipment.processIt(MInOut.DOCACTION_Complete);
		
		shipLine.load(trxName);
		orderLine.load(trxName);
		
		assertEquals(MInOut.DOCSTATUS_Completed, shipment.getDocStatus(),
				"Customer Shipment was not completed. DocStatus:");
		
		storageUtil.determineCurrentQtyAmounts();
		
		return shipLine;
	}

	private void createAndCompleteStandardSalesOrder() {
		
		storageUtil.determineInitialQtyAmounts();		
		
		order = storageUtil.createStandardSOHeader();
		orderLine = storageUtil.addProductAndCompleteOrder(order, qtyOrdered);

		storageUtil.determineCurrentQtyAmounts();

}

	private MInOutLine purchaseAndReceiveProduct() {

		storageUtil.determineInitialQtyAmounts();		
		
		order = storageUtil.createPOHeader();
		orderLine = storageUtil.addProductAndCompleteOrder(order, qtyOrdered);
		mrLine = storageUtil
				.receiveProductIntoInventory(order, orderLine, qtyReceived);
		
		order.load(trxName);
		orderLine.load(trxName);
		
		storageUtil.determineCurrentQtyAmounts();

		BigDecimal currentQtyAvailable = storageUtil.getCurrentQtyAvailable();
		assertEquals(qtyReceived.intValue(), currentQtyAvailable.intValue(),
				"Unable to ensure quantity available for sale");

		return mrLine;
		
	}

	private void sellAndShipProduct_POS() {

		storageUtil.determineInitialQtyAmounts();

		order = storageUtil.createPOSHeader();
		orderLine = storageUtil.addProductAndCompleteOrder(order, qtyOrdered);
		order.load(trxName);
		orderLine.load(trxName);
		
		storageUtil.determineCurrentQtyAmounts();

	}

	@Test
	void materialReceiptShouldCreateANewStorageLocation() {

		storageUtil.receiveProductAndCheckQty();
		
		assertEquals(1, storageUtil.getChangeInNumberOfStorageLocations(),
				"Only one storage location should have been created");

	}

	@Test
	void materialReceiptLineShouldHaveNonZeroMPolicyTicketId() {

		MInOutLine mrLine = storageUtil.receiveProductAndCheckQty();

		assertNotEquals(0, mrLine.getM_MPolicyTicket_ID(),
				"Material Receipt Line M_MPolicyTicket is zero");

	}

	@Test
	void materialReceiptLineShouldBeReferencedOnMPolicyTicketId() {

		MInOutLine mrLine = storageUtil.receiveProductAndCheckQty();
		MMPolicyTicket ticket = (MMPolicyTicket) mrLine.getM_MPolicyTicket();

		assertEquals(mrLine, (MInOutLine) ticket.getM_InOutLine(),
				"MPolicyTicket reference line should match the material receipt line");

	}

	@Test
	void materialReceiptShouldAddMPolicyTicketToOnHandStorage() {

		MInOutLine mrLine = storageUtil.receiveProductAndCheckQty();
		
		int mPolicyTicket_id = mrLine.getM_MPolicyTicket_ID();
		boolean foundMR = storageUtil.isPolicyTicketFoundInOnHandStorage(mPolicyTicket_id);
		assertTrue(foundMR, "Couldn't find the MR MPolicyTicket in storage");

	}

	@Test
	void materialReceiptMPolicyTicketShouldNotBeFoundInOrderedStorage() {

		MInOutLine mrLine = storageUtil.receiveProductAndCheckQty();
		
		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, 
				m_product_id, m_warehouse_id, m_attributeSetInstance_id, 
				mrLine.getM_MPolicyTicket_ID(), trxName);

		assertEquals(Env.ZERO, orderedStorage.getQtyOrdered(),
				"Ordered qty in storage should be zero");

	}


	@Test
	void MR_standAlone() {
		
		storageUtil.receiveProductAndCheckQty();
		storageUtil.assertCorrectChangeInStorageQuantities(
				qtyReceived, 
				qtyReceived, 
				Env.ZERO, 
				Env.ZERO);
		
	}


	@Test
	void poThenMR_ShouldAdd2StorageLocations() {

		qtyReceived = qtyOrdered;	
		storageUtil.determineInitialQtyAmounts();
		purchaseAndReceiveProduct();
		storageUtil.determineCurrentQtyAmounts();
		
		assertEquals(2, storageUtil.getChangeInNumberOfStorageLocations(), 
				"Should have 2 new storage locations");
		
	}

	@Test
	void poThenMR_ShouldUpdateTheOrder() {

		qtyReceived = qtyOrdered;	
		purchaseAndReceiveProduct();
		
		assertEquals(orderLine.getQtyDelivered(), orderLine.getQtyOrdered(),
				"OrderLine should be marked as delivered");
		assertTrue(order.isDelivered(),
				"Order should be marked as delivered");
   		
	}

	@Test
	void poThenMR_OrderLineAndMRLineShouldHaveDifferentTickets() {

		qtyReceived = qtyOrdered;	
		purchaseAndReceiveProduct();

		assertNotEquals(0, mrLine.getM_MPolicyTicket_ID(),
				"Material Receipt Line M_MPolicyTicket is zero");

		MMPolicyTicket mrLineTicket = (MMPolicyTicket) mrLine.getM_MPolicyTicket();
		MMPolicyTicket orderLineTicket = (MMPolicyTicket) orderLine.getM_MPolicyTicket();
   		
		assertNotEquals(mrLineTicket.getM_MPolicyTicket_ID(),
				orderLineTicket.getM_MPolicyTicket_ID(), 
				"The MR Line and Order Line should have different material policy tickets");
		
	}

	@Test
	void poThenMR_mrLineTicketShouldReferenceMRLine() {

		qtyReceived = qtyOrdered;
		purchaseAndReceiveProduct();
		
		MMPolicyTicket ticket = (MMPolicyTicket) mrLine.getM_MPolicyTicket();
		
		assertEquals(mrLine, (MInOutLine) ticket.getM_InOutLine(), 
				"MPolicyTicket reference line should match the material receipt line");		
		
	}

	@Test
	void poThenMR_checkStorageQty() {

		qtyReceived = qtyOrdered;
		purchaseAndReceiveProduct();
		
		storageUtil.assertCorrectChangeInStorageQuantities(
				qtyReceived, 
				qtyReceived, 
				Env.ZERO, 
				Env.ZERO);
	}

	@Test
	void shipment__POSOrder() {
		
		qtyOrdered = Env.ONE;
		qtyReceived = qtyOrdered;	
		purchaseAndReceiveProduct();		
		sellAndShipProduct_POS();
		
		assertPOSData();
		
	}

	@Test
	void shipment_from_POSOrder_withProductASI() {
		
		m_attributeSetInstance_id = product_withASI.getM_AttributeSetInstance_ID();
		m_product_id = m_productWithASI_id;
		storageUtil.setM_Product_ID(product_withASI.getM_Product_ID());
		storageUtil.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		
		qtyOrdered = Env.ONE;
		qtyReceived = qtyOrdered;
		
		purchaseAndReceiveProduct();
		
		sellAndShipProduct_POS();
		
		assertPOSData();
		
	}

	@Test
	void shipment_with_StandardOrder() {
		
		BigDecimal qtyTarget = BigDecimal.valueOf(2.0);
		qtyOrdered = qtyTarget;
		qtyReceived = qtyTarget;
		
		purchaseAndReceiveProduct();
		createAndCompleteStandardSalesOrder();

		qtyDelivered = Env.ONE;
		MInOutLine shipLine = createAndCompleteShipment();

		assertStandardOrderShipmentData(shipLine);
		
	}

	@Test
	void shipment__WithRMA() {

		BigDecimal qtyTarget = BigDecimal.valueOf(2.0);
		qtyOrdered = qtyTarget;
		qtyReceived = qtyTarget;
		qtyDelivered = qtyTarget;
		
		purchaseAndReceiveProduct();
		createAndCompleteStandardSalesOrder();
		MInOutLine shipLine = createAndCompleteShipment();
		
		createAndShipOrReceiveRMA(shipLine);
		
		assertCustomerRMAData();
		
	}

	private void assertCustomerRMAData() {
		
		storageUtil.assertCorrectChangeInStorageQuantities(
				Env.ZERO,
				Env.ONE, 
				Env.ONE, 
				Env.ZERO);
	}

	private void assertVendorRMAData() {
		
		storageUtil.assertCorrectChangeInStorageQuantities(
				Env.ONE.negate(),
				Env.ONE.negate(), 
				Env.ZERO, 
				Env.ONE);
	}

	private void createAndShipOrReceiveRMA(MInOutLine line) {
		
		storageUtil.determineInitialQtyAmounts();
		
		boolean isSOTrx = line.isSOTrx();
		String docBaseType = DOCBASETYPE_SalesOrder;
		String docType = DOCSUBTYPESO_ReturnMaterial;
		String movementType = MOVEMENTTYPE_CustomerReturns;		
		if (!isSOTrx)
		{
			docBaseType = DOCBASETYPE_PurchaseOrder;
			movementType = MOVEMENTTYPE_VendorReturns;
		}

		int rma_docType_id = MDocType.getDocTypeBaseOnSubType(AD_ORG_ID, 
				docBaseType, docType);

		MInOut shipment = (MInOut) line.getM_InOut();
		MRMA rmaHeader = new MRMA(ctx, 0, trxName);
		rmaHeader.setName("TestRMA");
		rmaHeader.setM_InOut_ID(line.getM_InOut_ID());
		rmaHeader.setC_Order_ID(shipment.getC_Order_ID());
		rmaHeader.setC_DocType_ID(rma_docType_id);
		rmaHeader.setSalesRep_ID(salesRep_id);
		rmaHeader.setIsSOTrx(shipment.isSOTrx());
		rmaHeader.saveEx();
		
		MRMALine rmaLine = new MRMALine(ctx, 0, trxName);
		rmaLine.setM_RMA_ID(rmaHeader.getM_RMA_ID());
		rmaLine.setM_InOutLine_ID(line.getM_InOutLine_ID());
		rmaLine.setQty(Env.ONE);
		rmaLine.saveEx();

		storageUtil.determineCurrentQtyAmounts();
		storageUtil.assertCorrectChangeInStorageQuantities(
				Env.ZERO,
				Env.ZERO, 
				Env.ZERO, 
				Env.ZERO);
		
		MInOut mr = new MInOut(ctx, 0, trxName);
		mr.setIsSOTrx(isSOTrx);
		mr.setC_DocType_ID();
		mr.setMovementType(movementType);
		mr.setC_BPartner_ID(CommonGWData.SEEDFARM_ID); // GW Seed Farm
		mr.setC_BPartner_Location_ID(CommonGWData.SEEDFARM_LOCATION_ID);
		mr.setM_Warehouse_ID(CommonGWData.HQ_WAREHOUSE_ID);
		mr.setMovementDate(now);
		mr.setDateAcct(now);
		mr.setM_RMA_ID(rmaHeader.getM_RMA_ID());
		mr.saveEx();

		MInOutLine mrLine = new MInOutLine(mr);
		mrLine.setM_Product_ID(m_product_id);
		if (m_attributeSetInstance_id > 0)
			mrLine.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		mrLine.setM_Locator_ID(m_locator_id);
		mrLine.setQty(Env.ONE);
		mrLine.setM_RMALine_ID(rmaLine.getM_RMALine_ID());
		mrLine.saveEx();		
		
		mr.processIt(MInOut.DOCACTION_Prepare);
		
		Arrays.asList(mr.getConfirmations(true)).stream()
			.forEach(confirm -> {
				Arrays.asList(confirm.getLines(true)).stream()
					.forEach(confirmLine -> {
						confirmLine.setConfirmedQty(confirmLine.getTargetQty());
						confirmLine.saveEx();
					});
				confirm.processIt(MInOutConfirm.DOCACTION_Complete);
				confirm.saveEx();
			});
		
		mr.processIt(MInOut.DOCACTION_Complete);
		mrLine.load(trxName);
		order.load(trxName);
		orderLine.load(trxName);

		assertEquals(MInOut.DOCSTATUS_Completed, mr.getDocStatus(), 
				"Material Receipt was not completed. DocStatus:");

		storageUtil.determineCurrentQtyAmounts();
		
	}

	@Test
	void receipt__WithRMA() {

		BigDecimal qtyTarget = BigDecimal.valueOf(2.0);
		qtyOrdered = qtyTarget;
		qtyReceived = qtyTarget;
		qtyDelivered = qtyTarget;
		
		MInOutLine originalInOutLine = purchaseAndReceiveProduct();
		
		createAndShipOrReceiveRMA(originalInOutLine);
		
		assertVendorRMAData();
		
	}

}
