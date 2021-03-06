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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import org.adempiere.test.CommonGWSetup;
import org.adempiere.test.CommonIntegrationTestUtilities;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MClient;
import org.compiere.model.MCost;
import org.compiere.model.MCostElement;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


@Tag("Model")
@Tag("MPPOrder")
@Tag("MPPOrderRelated")
class IT_MPPOrder_CreateReceipt extends CommonGWSetup {

	private MPPOrder_CreateReceiptSetup setupData;
	private MPPOrder ppOrder;
	private BigDecimal qtyToDeliver;
	private static CommonIntegrationTestUtilities testUtils = new CommonIntegrationTestUtilities();

	@BeforeAll
	static void beforeAllSetup() {
	    
	    testUtils.openPeriod(ctx, AD_ORG_ID, today, "MMR", trxName);
	    
	}
	
	@BeforeEach
	void localSetUp()
	{

		setupData = new MPPOrder_CreateReceiptSetup(ctx, trxName);
		ppOrder = setupData.createDraftOrder();

		ppOrder.processIt(DocAction.ACTION_Complete);

		Timestamp today = setupData.getDate();
		openPeriod(ctx, AD_ORG_ID, today, "MCC", trxName);

		qtyToDeliver = ppOrder.getQtyOrdered().subtract(ppOrder.getQtyDelivered());

	}

	@Test
	final void fullOrder_orderDatesAndQtyAreSet()
	{

		assertEquals(setupData.getOrderQty(), ppOrder.getQtyReserved(), "QtyReserved");

		MPPOrder.createReceipt(ppOrder, today, qtyToDeliver, qtyToDeliver, Env.ZERO, Env.ZERO,
				setupData.getLocator_id(), setupData.getAttributeSetInstance_id());

		ppOrder.load(trxName);

		assertEquals(today, ppOrder.getDateDelivered(), "Delivered date should be set");
		assertEquals(today, ppOrder.getDateStart(), "Start date should be set");
		assertEquals(today, ppOrder.getDateFinish(), "Finish date should be set");
		assertEquals(qtyToDeliver, ppOrder.getQtyDelivered(), "QtyDelivered");
		assertEquals(Env.ZERO, ppOrder.getQtyReserved(), "QtyReserved");

		List<MPPCostCollector> ccList = getCostCollector(ppOrder);

		for (MPPCostCollector cc : ccList)
		{

			assertNotNull(cc);

		}

		BigDecimal materialCost = Env.ZERO;
		MAcctSchema as = MClient.get(ctx).getAcctSchema();
		List<MCost> costList = MCost.getForProduct(as, ppOrder);
		for (MCost cost : costList)
		{

			MCostElement ce = cost.getCostElement();
			if (MCostElement.COSTELEMENTTYPE_Material.equals(ce.getCostElementType()))
			{
				materialCost = cost.getCurrentCostPrice();
			}

		}


	}

	private List<MPPCostCollector> getCostCollector(MPPOrder order)
	{

		String where = MPPCostCollector.COLUMNNAME_PP_Order_ID + "=?";

		return new Query(ctx, MPPCostCollector.Table_Name, where, trxName)
				.setClient_ID()
				.setParameters(order.getPP_Order_ID())
				.list();

	}

	@Test
	final void partialOrder_orderDatesAndQtyAreSet()
	{

		assertEquals(setupData.getOrderQty(), ppOrder.getQtyReserved(), "QtyReserved");

		MPPOrder.createReceipt(ppOrder, today, Env.ONE, qtyToDeliver, Env.ZERO, Env.ZERO,
				setupData.getLocator_id(), setupData.getAttributeSetInstance_id());

		ppOrder.load(trxName);

		assertEquals(today, ppOrder.getDateDelivered(), "Delivered date should be set");
		assertEquals(today, ppOrder.getDateStart(), "Start date should be set");
		assertNull(ppOrder.getDateFinish(), "Finish date should not be set");
		assertEquals(Env.ONE, ppOrder.getQtyDelivered(), "QtyDelivered");
		assertEquals(qtyToDeliver.subtract(Env.ONE), ppOrder.getQtyReserved(), "QtyReserved");

	}

}
