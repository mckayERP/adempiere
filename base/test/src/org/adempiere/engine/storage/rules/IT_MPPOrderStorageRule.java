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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.MDocType;
import org.compiere.model.MStorage;
import org.compiere.model.MWarehouse;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Storage")
@Tag("StorageEngine")
@Tag("MPPOrderRelated")
@Tag("MPPOrderStorageRule")
class IT_MPPOrderStorageRule extends CommonGWSetup {

	private final static int m_product_id = 145; // Patio Set
	private final static int m_attributeSetInstance_id = 0;
	private final static int S_Resource_ID = 50000;
	private final static int AD_Workflow_ID = 50018;
	private final static int PP_Product_BOM_ID = 145;

	private final static int m_warehouse_id = 50001; // GW Furniture Warehouse

	private static MPPOrderStorageRule rule = new MPPOrderStorageRule();

	@Test
	void testMPPOrderStorageRule_createTransactionInfo()
	{

		Timestamp now = new Timestamp(System.currentTimeMillis());
		int m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator().get_ID();

		MPPOrder order = new MPPOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ManufacturingOrder);
		order.setM_Warehouse_ID(m_warehouse_id);
		order.setM_Locator_ID(m_locator_id);
		order.setM_Product_ID(m_product_id);
		order.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		order.setS_Resource_ID(S_Resource_ID);
		order.setAD_Workflow_ID(AD_Workflow_ID);
		order.setPP_Product_BOM_ID(PP_Product_BOM_ID);
		order.setPriorityRule(MPPOrder.PRIORITYRULE_High);
		order.setDateOrdered(now);
		order.setDatePromised(now);
		order.setDateStartSchedule(now);
		order.setQty(Env.ONE);
		order.saveEx();

		StorageTransactionInfo transactionInfo = rule.createTransactionInfo(order);

		assertEquals(order, transactionInfo.getDocumentLine());
		assertEquals("", transactionInfo.getMovementType());
		assertEquals(-1, transactionInfo.getTransactionAttributeSetInstance_id());
		assertEquals(m_product_id, transactionInfo.getM_Product_ID());
		assertEquals(m_warehouse_id, transactionInfo.getOrderWarehouse_id());
		assertEquals(-1, transactionInfo.getTransactionMPolicyTicket_id());
		assertEquals(0, transactionInfo.getOrderAttributeSetInstance_id());
		assertEquals(-1, transactionInfo.getOrderMPolicyTicket_id());
		assertEquals(Env.ZERO, transactionInfo.getMovementQty());
		assertEquals(false, transactionInfo.isSOTrx());
		assertEquals(true, transactionInfo.isDeleteExistingMAEntries());
		assertEquals(true, transactionInfo.isProcessMA());
		assertEquals(false, transactionInfo.isUseToFields());
		assertEquals(true, transactionInfo.isUpdateStorage());

	}

	@Test
	void testMPPOrderStorageRule_checkQtyOrderedInStorage()
	{

		BigDecimal qtyOrdered = new BigDecimal(10);
		int m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator().get_ID();

		BigDecimal currentQtyOrdered = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		BigDecimal currentQtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		BigDecimal currentQtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);
		int initialNumberOfStorageLocations = MStorage.getOfProduct(ctx, m_product_id,
				trxName).length;

		Timestamp now = new Timestamp(System.currentTimeMillis());

		MPPOrder order = new MPPOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ManufacturingOrder);
		order.setM_Warehouse_ID(m_warehouse_id);
		order.setM_Locator_ID(m_locator_id);
		order.setM_Product_ID(m_product_id);
		order.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		order.setS_Resource_ID(S_Resource_ID);
		order.setAD_Workflow_ID(AD_Workflow_ID);
		order.setPP_Product_BOM_ID(PP_Product_BOM_ID);
		order.setPriorityRule(MPPOrder.PRIORITYRULE_High);
		order.setQty(qtyOrdered);
		order.setDateOrdered(now);
		order.setDatePromised(now);
		order.setDateStartSchedule(now);
		order.saveEx();

		if (!order.processIt(MPPOrder.DOCACTION_Prepare))
			fail("Order could not be prepared: " + order.getProcessMsg());

		order.saveEx();

		MStorage[] storages = MStorage.getOfProduct(ctx, m_product_id, trxName);
		BigDecimal qtyAvailable = MStorage.getQtyAvailable(null, m_product_id,
				m_attributeSetInstance_id, m_warehouse_id, m_locator_id, trxName);
		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, order.getM_MPolicyTicket_ID(), trxName);
		BigDecimal changeInQtyAvailable = MStorage
				.getQtyAvailable(null, m_product_id, m_attributeSetInstance_id, m_warehouse_id,
						m_locator_id, trxName)
				.subtract(currentQtyAvailable);
		assertTrue(initialNumberOfStorageLocations < storages.length,
				"Storage locations should have increased");

		boolean foundOrder = false;
		for (MStorage storage : storages)
		{

			if (order.getM_MPolicyTicket_ID() == storage.getM_MPolicyTicket_ID())
				foundOrder = true;

		}
		assertTrue(foundOrder, "Couldn't find the order MPolicyTicket in storage");

		assertEquals(Env.ZERO, changeInQtyAvailable, "Quantity available should not have changed.");
		assertEquals(qtyAvailable, currentQtyAvailable, "Quantity available should not changed");

		assertNotEquals(0, order.getM_MPolicyTicket_ID(),
				"Manufacturing order M_MPolicyTicket is zero.");
		assertEquals(qtyOrdered, order.getQtyReserved(),
				"Manufacturing order qty reserved should match the qty ordered.");

		assertEquals(qtyOrdered, orderedStorage.getQtyOrdered(),
				"Ordered qty in storage is incorrect");

		BigDecimal changeInOrderedQty = MStorage.getOrderedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName).subtract(currentQtyOrdered);
		assertEquals(qtyOrdered, changeInOrderedQty, "Quantity ordered in storage didn't change");

		BigDecimal qtyReserved = MStorage.getReservedQty(ctx, m_product_id, m_warehouse_id,
				m_attributeSetInstance_id, trxName);
		assertEquals(Env.ZERO, qtyReserved.subtract(currentQtyReserved),
				"Quantity reserved should not change.");

	}

}
