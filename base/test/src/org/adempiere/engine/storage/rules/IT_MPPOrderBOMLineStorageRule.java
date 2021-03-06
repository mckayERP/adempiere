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

import static org.adempiere.test.CommonGWData.FURNITURE_PLANT_RESOURCE_ID;
import static org.adempiere.test.CommonGWData.FURNITURE_WAREHOUSE_ID;
import static org.adempiere.test.CommonGWData.PATIOCHAIR_BOM_PRODUCT_ID;
import static org.adempiere.test.CommonGWData.PATIOSET_PP_PRODUCT_BOM_ID;
import static org.adempiere.test.CommonGWData.PATIOSET_PRODUCT_ID;
import static org.adempiere.test.CommonGWData.PATIOSET_WORKFLOW_ID;
import static org.compiere.model.X_C_DocType.DOCBASETYPE_ManufacturingOrder;
import static org.compiere.util.Env.ONE;
import static org.compiere.util.Env.ZERO;
import static org.eevolution.model.X_PP_Order.DOCACTION_Prepare;
import static org.eevolution.model.X_PP_Order.PRIORITYRULE_High;
import static org.eevolution.model.X_PP_Order_BOMLine.COMPONENTTYPE_Co_Product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import org.adempiere.engine.storage.StorageTestUtilities;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.MWarehouse;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderBOMLine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Storage")
@Tag("StorageRule")
@Tag("MPPOrderRelated")
@Tag("MPPOrderBOMLineStorageRule")
class IT_MPPOrderBOMLineStorageRule extends CommonGWSetup {

	private static MPPOrder order;
	private static int product_id = PATIOSET_PRODUCT_ID;
	private static int bom_product_id = PATIOCHAIR_BOM_PRODUCT_ID;
	private static int S_Resource_ID = FURNITURE_PLANT_RESOURCE_ID;
	private static int AD_Workflow_ID = PATIOSET_WORKFLOW_ID;
	private static int PP_Product_BOM_ID = PATIOSET_PP_PRODUCT_BOM_ID;

	private static int warehouse_id = FURNITURE_WAREHOUSE_ID;
	private static int locator_id = 0;

	private static BigDecimal qtyMultiplier = new BigDecimal(4);
	private static int m_attributeSetInstance_id = 0;

	private StorageTestUtilities storageUtil;

	private static MPPOrderBOMLineStorageRule rule = new MPPOrderBOMLineStorageRule();

	@BeforeAll
	static void localSetUpBeforeClass() throws Exception
	{

		locator_id = MWarehouse.get(ctx, warehouse_id).getDefaultLocator().get_ID();

	}

	@BeforeEach
	void localSetupBefore()
	{

		storageUtil = new StorageTestUtilities(ctx, bom_product_id,
				m_attributeSetInstance_id, warehouse_id, locator_id, trxName);
		storageUtil.setbPartner_id(SEEDFARM_ID);
		storageUtil.setbPartnerLocation_id(SEEDFARM_LOCATION_ID);

	}

	@Test
	void testMPPOrderBOMLineStorageRule_storageInfo()
	{

		order = createManufacturingOrder(ONE);

		for (MPPOrderBOMLine line : order.getLines())
		{

			StorageTransactionInfo transactionInfo = rule.createTransactionInfo(line);

			assertEquals(line, transactionInfo.getDocumentLine());
			assertEquals("", transactionInfo.getMovementType());
			assertEquals(-1, transactionInfo.getTransactionAttributeSetInstance_id());
			assertEquals(line.getM_Product_ID(), transactionInfo.getM_Product_ID());
			assertEquals(line.getM_Warehouse_ID(), transactionInfo.getOrderWarehouse_id());
			assertEquals(-1, transactionInfo.getTransactionMPolicyTicket_id());
			assertEquals(0, transactionInfo.getOrderAttributeSetInstance_id());
			assertEquals(-1, transactionInfo.getOrderMPolicyTicket_id());
			assertEquals(ZERO, transactionInfo.getMovementQty());
			assertEquals(!COMPONENTTYPE_Co_Product.equals(line.getComponentType()),
					transactionInfo.isSOTrx());
			assertEquals(true, transactionInfo.isDeleteExistingMAEntries());
			assertEquals(true, transactionInfo.isProcessMA());
			assertEquals(false, transactionInfo.isUseToFields());
			assertEquals(true, transactionInfo.isUpdateStorage());

		}

	}

	private MPPOrder createManufacturingOrder(BigDecimal qtyOrdered)
	{

		MPPOrder order = new MPPOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(DOCBASETYPE_ManufacturingOrder);
		order.setM_Warehouse_ID(warehouse_id);
		order.setM_Locator_ID(locator_id);
		order.setM_Product_ID(product_id);
		order.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		order.setS_Resource_ID(S_Resource_ID);
		order.setAD_Workflow_ID(AD_Workflow_ID);
		order.setPP_Product_BOM_ID(PP_Product_BOM_ID);
		order.setPriorityRule(PRIORITYRULE_High);
		order.setDateOrdered(today);
		order.setDatePromised(today);
		order.setDateStartSchedule(today);
		order.setQty(qtyOrdered);
		order.saveEx();
		
		return order;

	}

	@Test
	void testMPPOrderBOMLineStorageRule_checkQtyOrderedInStorage()
	{

		BigDecimal qtyOrdered = new BigDecimal(10);
		BigDecimal qtyBomLine = qtyOrdered.multiply(qtyMultiplier);

		storageUtil.determineInitialQtyAmounts();

		order = createManufacturingOrder(qtyOrdered);
		
		if (!order.processIt(DOCACTION_Prepare))
			fail("Order could not be prepared: " + order.getProcessMsg());

		order.saveEx();

		MPPOrderBOMLine bomLine = null;
		for (MPPOrderBOMLine line : order.getLines())
		{

			if (line.getM_Product_ID() == bom_product_id)
				bomLine = line;

		}
		if (bomLine == null)
			fail("Couldn't find the bom line for product id = " + bom_product_id);

		storageUtil.determineCurrentQtyAmounts();

		assertTrue(
				storageUtil.getInitialNumberOfStorageLocations() < storageUtil
						.getCurrentNumberOfStorageLocations(),
				"Storage locations should have increased");
		assertNotEquals(0, bomLine.getM_MPolicyTicket_ID(),
				"Manufacturing order M_MPolicyTicket is zero.");
		assertEquals(qtyBomLine, bomLine.getQtyReserved(),
				"Manufacturing bom line qty reserved should match the qty ordered*bom qty.");

		boolean foundOrder = storageUtil
				.isPolicyTicketFoundInReservedStorage(bomLine.getM_MPolicyTicket_ID());
		assertTrue(foundOrder, "Couldn't find the order MPolicyTicket in storage");

		storageUtil.assertCorrectChangeInStorageQuantities(
				qtyBomLine.negate(), "Quantity available should be reduced",
				Env.ZERO, "Qty on hand should not have changed",
				qtyBomLine, "Quantity reserved should have increased",
				Env.ZERO, "Quantity ordered should not change",
				0);

	}

}
