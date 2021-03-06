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
package org.adempiere.engine.storage.rules;

import static org.adempiere.test.TestUtilities.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;

import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeUse;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MDocType;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MStorage;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Storage")
@Tag("StorageEngine")
@Tag("MOrderLineStorageRule")
class IT_MOrderLineStorageRule extends CommonGWSetup {

	private static MProduct product;
	private static MOrder order;
	private static MOrderLine orderLine;

	private static int m_product_id;
	private static int m_warehouse_id = 103; // GW HQ Warehouse
	private static int m_attributeSetInstance_id = 0;
	private static BigDecimal currentQtyReserved;
	private static BigDecimal currentQtyOrdered;
	private static BigDecimal currentQtyAvailable;

	private static BigDecimal qtyOrdered = Env.ONE;

	private static int m_locator_id;

	private static int org1_Value = 11; // GW HQ
	private static int org2_Value = 50001; // GW Fertilizer
	private static int warehouse1_id = 103; // GW HQ Warehouse
	private static int warehouse2_id = 50002; // GW Fertilizer Warehouse

	private static int purchasePL_id = 102;

	private static MOrderLineStorageRule rule = new MOrderLineStorageRule();
	private static MProduct product_withASI;
	private static int m_productWithASI_id;

	@BeforeAll
	static void localSetUpBeforeClass() throws Exception {

		product = new MProduct(ctx, 0, trxName);
		product.setName("ProductUnderTest_" + randomString(4));
		product.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		product.setM_Product_Category_ID(105); // GW Standard
		product.setC_TaxCategory_ID(107); // GW Standard
		product.setIsPurchased(true);
		product.setIsStocked(true);
		product.saveEx();
		product.load(trxName);
		m_product_id = product.getM_Product_ID();

		m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator().getM_Locator_ID();

		MPriceListVersion plv = new MPriceListVersion(ctx, 103, trxName);
		MProductPrice price = new MProductPrice(plv, product.getM_Product_ID(),
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
		product_withASI.setM_Product_Category_ID(105); // GW Standard
		product_withASI.setC_TaxCategory_ID(107); // GW Standard
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

	@Test
	void StorageInfo_PurchaseOrder_linesWithDifferentOrgAndWarehouse() {

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(MDocType.getDocType(MDocType.DOCBASETYPE_PurchaseOrder)); // GW
																								// Purchase
																								// Order
		order.setM_Warehouse_ID(warehouse1_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(purchasePL_id); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setAD_Org_ID(org2_Value);
		orderLine.setM_Warehouse_ID(warehouse2_id);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		assertEquals(org2_Value, orderLine.getAD_Org_ID());

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		StorageTransactionInfo transactionInfo = rule.createTransactionInfo(orderLine);

		assertEquals(org2_Value, orderLine.getAD_Org_ID());

		assertEquals(orderLine, transactionInfo.getDocumentLine());
		assertEquals("", transactionInfo.getMovementType());
		assertEquals(-1, transactionInfo.getTransactionAttributeSetInstance_id());
		assertEquals(-1, transactionInfo.getTransactionWarehouse_id());
		assertEquals(-1, transactionInfo.getTransactionLocator_id());
		assertEquals(-1, transactionInfo.getTransactionMPolicyTicket_id());
		assertEquals(warehouse2_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(0, transactionInfo.getOrderAttributeSetInstance_id());
		assertNotEquals(0, transactionInfo.getOrderMPolicyTicket_id());
		assertEquals(false, transactionInfo.isSOTrx());
		assertEquals(true, transactionInfo.isDeleteExistingMAEntries());
		assertEquals(true, transactionInfo.isProcessMA());
		assertEquals(false, transactionInfo.isUseToFields());
		assertEquals(true, transactionInfo.isUpdateStorage());

	}

	@Test
	void purchaseOrder_linesWithSameOrgAndWarehouse() {

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(MDocType.getDocType(MDocType.DOCBASETYPE_PurchaseOrder)); // GW
																								// Purchase
																								// Order
		order.setM_Warehouse_ID(warehouse1_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(purchasePL_id); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setAD_Org_ID(org1_Value);
		orderLine.setM_Warehouse_ID(warehouse1_id);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		assertEquals(org1_Value, orderLine.getAD_Org_ID());

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		StorageTransactionInfo transactionInfo = rule.createTransactionInfo(orderLine);

		assertEquals(warehouse1_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(org1_Value, orderLine.getAD_Org_ID());

	}

	@Test
	void standardOrder_linesWithDifferentOrgAndWarehouse() {

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(true);
		order.setC_DocTypeTarget_ID(MDocType.getDocTypeBaseOnSubType(org1_Value,
				MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_StandardOrder));
		order.setM_Warehouse_ID(warehouse1_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(purchasePL_id); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setAD_Org_ID(org2_Value);
		orderLine.setM_Warehouse_ID(warehouse2_id);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		assertEquals(org2_Value, orderLine.getAD_Org_ID());

		order.prepareIt();
		order.completeIt();

		orderLine.load(trxName);

		StorageTransactionInfo transactionInfo = rule.createTransactionInfo(orderLine);

		assertEquals(warehouse2_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(org2_Value, orderLine.getAD_Org_ID());

	}

	@Test
	void StandardOrder_linesWithSameOrgAndWarehouse() {

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(true);
		order.setC_DocTypeTarget_ID(MDocType.getDocTypeBaseOnSubType(org1_Value,
				MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_StandardOrder));
		order.setM_Warehouse_ID(warehouse1_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(purchasePL_id); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setAD_Org_ID(org1_Value);
		orderLine.setM_Warehouse_ID(warehouse1_id);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		assertEquals(org1_Value, orderLine.getAD_Org_ID());

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		StorageTransactionInfo transactionInfo = rule.createTransactionInfo(orderLine);

		assertEquals(warehouse1_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(org1_Value, orderLine.getAD_Org_ID());

	}

	@Test
	void POSOrder_linesWithDifferentOrgAndWarehouse() {

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(true);
		order.setC_DocTypeTarget_ID(MDocType.getDocTypeBaseOnSubType(org1_Value,
				MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_POSOrder));
		order.setM_Warehouse_ID(warehouse1_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(purchasePL_id); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setAD_Org_ID(org2_Value);
		orderLine.setM_Warehouse_ID(warehouse2_id);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		assertEquals(org2_Value, orderLine.getAD_Org_ID());

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		StorageTransactionInfo transactionInfo = rule.createTransactionInfo(orderLine);

		// These should have been changed to match the parent order
		assertEquals(warehouse1_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(org1_Value, orderLine.getAD_Org_ID());

	}

	@Test
	void POSOrder_linesWithSameOrgAndWarehouse() {

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(true);
		order.setC_DocTypeTarget_ID(MDocType.getDocTypeBaseOnSubType(org1_Value,
				MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_POSOrder));
		order.setM_Warehouse_ID(warehouse1_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(purchasePL_id); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setAD_Org_ID(org1_Value);
		orderLine.setM_Warehouse_ID(warehouse1_id);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		assertEquals(org1_Value, orderLine.getAD_Org_ID());

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		StorageTransactionInfo transactionInfo = rule.createTransactionInfo(orderLine);

		assertEquals(warehouse1_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(org1_Value, orderLine.getAD_Org_ID());

	}

	@Test
	void purchaseOrderCompleteThenVoid() {

		currentQtyOrdered = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(126); // GW Purchase Order
		order.setM_Warehouse_ID(m_warehouse_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(102); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		MStorage[] storages = MStorage.getOfProduct(ctx, m_product_id, trxName);
		assertEquals(1, storages.length, "Only one storage location should have been created.");

		BigDecimal qtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);
		assertEquals(qtyAvailable, currentQtyAvailable,
				"Quantity available changed on a standared purchase order. It should remain the same. Current="
						+ currentQtyAvailable
						+ " new=" + qtyAvailable);

		assertNotEquals(0, orderLine.getM_MPolicyTicket_ID(),
				"Purchase order M_MPolicyTicket is zero.");
		assertEquals(qtyOrdered, orderLine.getQtyReserved(),
				"Purchase order qty reserved should match the qty ordered.");

		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, orderLine.getM_MPolicyTicket_ID(), trxName);
		assertEquals(qtyOrdered, orderedStorage.getQtyOrdered(), 
				"Ordered qty in storage is incorrect");

		BigDecimal changeInOrderedQty = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName).subtract(currentQtyOrdered);
		assertEquals(qtyOrdered, changeInOrderedQty,
				"Quantity ordered (" + orderedStorage.getQtyOrdered() + ") should be "
						+ currentQtyOrdered.add(qtyOrdered));

		BigDecimal qtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		assertEquals(currentQtyReserved, qtyReserved, "Quantity reserved should not change.");

		order.voidIt();
		orderLine.load(trxName);

		orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, orderLine.getM_MPolicyTicket_ID(), trxName);
		assertEquals(Env.ZERO, orderedStorage.getQtyOrdered(),
				"Ordered not adjusted to zero after void!");
		assertEquals(Env.ZERO, orderLine.getQtyOrdered(), "OrderLine qtyOrdered should equal zero");
		assertEquals(Env.ZERO, orderLine.getQtyReserved(),
				"OrderLine qtyReserved should equal zero");

	}

	@Test
	void purchaseOrderCompleteThenClose() {

		currentQtyOrdered = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(126); // GW Purchase Order
		order.setM_Warehouse_ID(m_warehouse_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(102); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		MStorage[] storages = MStorage.getOfProduct(ctx, m_product_id, trxName);
		assertEquals(1, storages.length, "Only one storage location should have been created.");

		BigDecimal qtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);
		assertEquals(qtyAvailable, currentQtyAvailable,
				"Quantity available changed on a standared purchase order. It should remain the same. Current="
						+ currentQtyAvailable
						+ " new=" + qtyAvailable);

		assertNotEquals(0, orderLine.getM_MPolicyTicket_ID(),
				"Purchase order M_MPolicyTicket is zero.");
		assertEquals(qtyOrdered, orderLine.getQtyReserved(),
				"Purchase order qty reserved should match the qty ordered.");

		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, orderLine.getM_MPolicyTicket_ID(), trxName);
		assertEquals(orderedStorage.getQtyOrdered(), qtyOrdered,
				"Ordered qty in storage is incorrect");

		BigDecimal changeInOrderedQty = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName).subtract(currentQtyOrdered);
		assertEquals(changeInOrderedQty, qtyOrdered,
				"Quantity ordered (" + orderedStorage.getQtyOrdered() + ") should be "
						+ currentQtyOrdered.add(qtyOrdered));

		BigDecimal qtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		assertEquals(currentQtyReserved, qtyReserved, "Quantity reserved should not change.");

		order.processIt(MOrder.DOCACTION_Close);
		orderLine.load(trxName);

		orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, orderLine.getM_MPolicyTicket_ID(), trxName);
		assertEquals(Env.ZERO, orderedStorage.getQtyOrdered(),
				"Ordered not adjusted to delivered after close!");
		assertEquals(Env.ZERO, orderLine.getQtyOrdered(),
				"OrderLine qtyOrdered should equal close");
		assertEquals(Env.ZERO, orderLine.getQtyReserved(),
				"OrderLine qtyReserved should equal zero");

	}

	@Test
	void purchaseOrder_withProductASI() {

		currentQtyOrdered = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(126); // GW Purchase Order
		order.setM_Warehouse_ID(m_warehouse_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(102); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setM_Product_ID(m_productWithASI_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		assertEquals(product_withASI.getM_AttributeSetInstance_ID(),
				orderLine.getM_AttributeSetInstance_ID(),
				"Order line ASI should have been set to the product ASI");

		MStorage[] storages = MStorage.getOfProduct(ctx, m_productWithASI_id, trxName);
		assertEquals(1, storages.length, "Only one storage location should have been created.");

		assertEquals(product_withASI.getM_AttributeSetInstance_ID(),
				storages[0].getM_AttributeSetInstance_ID(), "Storage should reference the ASI");

		BigDecimal qtyAvailable = MStorage.getQtyAvailable(null, m_productWithASI_id,
				product_withASI.getM_AttributeSetInstance_ID(), m_warehouse_id, m_locator_id,
				trxName);
		assertEquals(qtyAvailable, currentQtyAvailable,
				"Quantity available changed on a standared purchase order. It should remain the same. Current="
						+ currentQtyAvailable
						+ " new=" + qtyAvailable);

		assertNotEquals(0, orderLine.getM_MPolicyTicket_ID(),
				"Purchase order M_MPolicyTicket is zero.");
		MMPolicyTicket ticket = (MMPolicyTicket) orderLine.getM_MPolicyTicket();
		assertEquals(orderLine.get_ID(), ticket.getC_OrderLine_ID(),
				"Material Policy Ticket reference orderLine");

		assertEquals(qtyOrdered, orderLine.getQtyReserved(),
				"Purchase order qty reserved should match the qty ordered.");

		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_productWithASI_id,
				m_warehouse_id, product_withASI.getM_AttributeSetInstance_ID(),
				orderLine.getM_MPolicyTicket_ID(), trxName);
		assertEquals(orderedStorage.getQtyOrdered(), qtyOrdered,
				"Ordered qty in storage is incorrect");

		BigDecimal changeInOrderedQty = MStorage
				.getOrderedQty(ctx, m_productWithASI_id, m_warehouse_id,
						product_withASI.getM_AttributeSetInstance_ID(), trxName)
				.subtract(currentQtyOrdered);
		assertEquals(changeInOrderedQty, qtyOrdered,
				"Quantity ordered (" + orderedStorage.getQtyOrdered() + ") should be "
						+ currentQtyOrdered.add(qtyOrdered));

		BigDecimal qtyReserved = MStorage.getReservedQty(ctx, m_productWithASI_id, m_warehouse_id,
				product_withASI.getM_AttributeSetInstance_ID(), trxName);
		assertEquals(currentQtyReserved, qtyReserved, "Quantity reserved should not change.");

		order.processIt(MOrder.DOCACTION_Close);
		orderLine.load(trxName);

		orderedStorage = MStorage.getReservedOrdered(ctx, m_productWithASI_id, m_warehouse_id,
				product_withASI.getM_AttributeSetInstance_ID(), orderLine.getM_MPolicyTicket_ID(),
				trxName);
		assertEquals(Env.ZERO, orderedStorage.getQtyOrdered(),
				"Ordered not adjusted to delivered after close!");
		assertEquals(Env.ZERO, orderLine.getQtyOrdered(),
				"OrderLine qtyOrdered should equal close");
		assertEquals(Env.ZERO, orderLine.getQtyReserved(),
				"OrderLine qtyReserved should equal zero");

	}

	@Test
	void nonBindingProposal() {

		currentQtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyOrdered = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		currentQtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);
		int currentNumberOfStorageLocations = MStorage.getOfProduct(ctx, m_product_id,
				trxName).length;

		order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(true);
		order.setC_DocTypeTarget_ID(MDocType.getDocTypeBaseOnSubType(Env.getAD_Org_ID(ctx),
				MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_Proposal));
		order.setM_Warehouse_ID(m_warehouse_id); // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(102); // GW Purchase
		order.saveEx();

		orderLine = new MOrderLine(order);
		orderLine.setM_Product_ID(m_product_id);
		orderLine.setQty(qtyOrdered);
		orderLine.saveEx();

		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);

		orderLine.load(trxName);

		MStorage[] storages = MStorage.getOfProduct(ctx, m_product_id, trxName);
		BigDecimal qtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		BigDecimal qtyOrdered = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		BigDecimal qtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);

		assertEquals(currentNumberOfStorageLocations, storages.length,
				"A non-binding proposal should not create a storage record.");

		assertEquals(0, orderLine.getM_MPolicyTicket_ID(),
				"Proposal order M_MPolicyTicket should be zero.");
		assertEquals(Env.ZERO, orderLine.getQtyReserved(),
				"Proposal order qty reserved should be zero.");
		assertEquals(currentQtyOrdered, qtyOrdered, "Quantity ordered should not change.");
		assertEquals(currentQtyAvailable, qtyAvailable, "Quantity available should not change.");
		assertEquals(currentQtyReserved, qtyReserved, "Quantity reserved should not change.");

	}

}
