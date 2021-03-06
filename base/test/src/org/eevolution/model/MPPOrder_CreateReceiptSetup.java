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

import static org.adempiere.test.TestUtilities.randomString;
import static org.adempiere.test.CommonGWData.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Properties;

import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutConfirm;
import org.compiere.model.MInOutLine;
import org.compiere.model.MPriceListVersion;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPrice;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.eevolution.service.dsl.ProcessBuilder;

public class MPPOrder_CreateReceiptSetup {
	
	private final static int warehouse_id = HQ_WAREHOUSE_ID;
	private final static int attributeSetInstance_id = 0;
	private final static int seedFarm_id = SEEDFARM_ID;
	private final static int seedFarmLocation_id = SEEDFARM_LOCATION_ID;
	private final static int S_Resource_ID= FURNITURE_PLANT_RESOURCE_ID;
	private final static int AD_Workflow_ID=PATIOSET_WORKFLOW_ID;
	private final static BigDecimal orderQty = BigDecimal.valueOf(4);

	private MProduct parentProduct;
	private int parentProduct_id;
	private MPPProductBOM bom;
	private int locator_id;
	private Properties ctx;
	private String trxName;
	private Timestamp today = TimeUtil.getDay(System.currentTimeMillis());

	private MPPOrder ppOrder;

	protected MPPOrder_CreateReceiptSetup() {
		
	}

	protected MPPOrder_CreateReceiptSetup(Properties ctx, String trxName) {
		
		this.ctx = ctx;
		this.trxName = trxName;
		createDraftOrder();

	}

	protected int getWarehouse_id() {
		return warehouse_id;
	}

	protected int getAttributeSetInstance_id() {
		return attributeSetInstance_id;
	}

	protected int getBPartner_id() {
		return seedFarm_id;
	}

	protected int getResource_id() {
		return S_Resource_ID;
	}

	protected int getAdWorkflow_id() {
		return AD_Workflow_ID;
	}

	protected BigDecimal getOrderQty() {
		return orderQty;
	}

	protected Timestamp getDate() {
		return today;
	}
	
	protected MProduct getParentProduct() {
		return parentProduct;
	}

	protected int getParentProduct_id() {
		return parentProduct_id;
	}

	protected MPPProductBOM getBom() {
		return bom;
	}

	protected int getLocator_id() {
		
		if (locator_id == 0)
			locator_id = MWarehouse.get(ctx, warehouse_id).getDefaultLocator().getM_Locator_ID();
			
		return locator_id;
		
	}
	
	protected MPPOrder createDraftOrder() {
		
		createParentProductAndBOM();
		addSubProductsQtysToInventory();
		ppOrder = createProductionOrder();
		
		return ppOrder;

	}
	
	private void createParentProductAndBOM() {
		
		createParentProduct();
		createBOMHeader();
		createSubProductsAndBOMLines();
		verifyBOM();
	
	}

	private void addSubProductsQtysToInventory() {
				
		for (MPPProductBOMLine line : bom.getLines()) {
			receiveAndProcess(line);
		}
		
	}

	private MPPOrder createProductionOrder() {
		
		MPPOrder order = new MPPOrder(ctx, 0, trxName);
		order.setIsSOTrx(false);
		order.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ManufacturingOrder);
		order.setM_Warehouse_ID(warehouse_id);
		order.setM_Locator_ID(getLocator_id());
		order.setM_Product_ID(parentProduct_id);
		order.setC_UOM_ID(parentProduct.getC_UOM_ID());
		order.setM_AttributeSetInstance_ID(attributeSetInstance_id);
		order.setS_Resource_ID(S_Resource_ID );
		order.setAD_Workflow_ID(AD_Workflow_ID);
		order.setPP_Product_BOM_ID(bom.getPP_Product_BOM_ID());
		order.setPriorityRule(MPPOrder.PRIORITYRULE_High);
		order.setDateOrdered(today);
		order.setDatePromised(today);
		order.setDateStartSchedule(today);
		order.setQty(orderQty);
		order.saveEx();
	
		return order;
		
	}

	private void createParentProduct() {
		
		parentProduct = new MProduct(ctx, 0, trxName);
		parentProduct.setName("ProductUnderTest_" + randomString(4));
		parentProduct.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		parentProduct.setM_Product_Category_ID(PRODUCT_CATEGORY_STANDARD_ID);
		parentProduct.setC_TaxCategory_ID(TAX_CATEGORY_STANDARD_ID);
		parentProduct.setIsPurchased(true);
		parentProduct.setIsStocked(true);
		parentProduct.saveEx();
		parentProduct.load(trxName);
		parentProduct_id = parentProduct.getM_Product_ID();
		
	}

	private void createBOMHeader() {
		bom = new MPPProductBOM(ctx, 0, trxName);
		bom.setName("Test Product BOM");
		bom.setM_Product_ID(parentProduct_id);
		bom.setM_Product_ID(parentProduct_id);
		bom.setBOMType(MPPProductBOM.BOMUSE_Manufacturing);
		bom.setBOMUse(MPPProductBOM.BOMUSE_Manufacturing);
		bom.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		bom.saveEx();
	}

	private void createSubProductsAndBOMLines() {
		
		addProductToBOM(createSubProduct());
		addProductToBOM(createSubProduct());
				
	}

	private void verifyBOM() {
		
		ProcessBuilder.create(ctx)
				.process(org.compiere.process.BOMVerify.class)
				.withTitle("Verify BOM")
				.withParameter("M_Product_ID", parentProduct_id)
				.withoutTransactionClose()
				.execute(trxName);
		parentProduct.load(trxName);
				
	}

	private void receiveAndProcess(MPPProductBOMLine line) {
		
		MInOut mr = createMaterialReceiptHeader();
		createMaterialReceiptLine(line, mr);
		prepareConfirmAndCompleteMaterialReceipt(mr);
		
	}

	private void prepareConfirmAndCompleteMaterialReceipt(MInOut mr) {
		
		mr.processIt(MInOut.DOCACTION_Prepare);
		comfirmLines(mr);
		mr.processIt(MInOut.DOCACTION_Complete);
		
	}

	private void comfirmLines(MInOut mr) {
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
	}

	private MInOut createMaterialReceiptHeader() {
		MInOut mr = new MInOut(ctx, 0, trxName);
		mr.setIsSOTrx(false);
		mr.setC_DocType_ID();
		mr.setMovementType(MInOut.MOVEMENTTYPE_VendorReceipts);
		mr.setC_BPartner_ID(seedFarm_id); // GW Seed Farm
		mr.setC_BPartner_Location_ID(seedFarmLocation_id);
		mr.setM_Warehouse_ID(warehouse_id);
		mr.setMovementDate(today);
		mr.setDateAcct(today);
		mr.saveEx();
		return mr;
	}

	private void createMaterialReceiptLine(MPPProductBOMLine line, MInOut mr) {
		MInOutLine mrLine = new MInOutLine(mr);
		mrLine.setM_Product_ID(line.getM_Product_ID());
		mrLine.setM_Locator_ID(getLocator_id());
		mrLine.setC_OrderLine_ID(0);
		mrLine.setQty(orderQty.multiply(line.getQty(true)));
		mrLine.saveEx();
	}

	private MProduct createSubProduct() {
		
		int subProduct_id;
		MProduct subProduct = new MProduct(ctx, 0, trxName);
		subProduct.setName("SubProduct_" + randomString(4));
		subProduct.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		subProduct.setM_Product_Category_ID(PRODUCT_CATEGORY_STANDARD_ID);
		subProduct.setC_TaxCategory_ID(TAX_CATEGORY_STANDARD_ID);
		subProduct.setIsPurchased(true);
		subProduct.setIsStocked(true);
		subProduct.saveEx();
		subProduct.load(trxName);
		subProduct_id = subProduct.getM_Product_ID();		
		addToPriceList(subProduct_id);
		
		return subProduct;
		
	}

	private void addToPriceList(int subProduct_id) {
		MPriceListVersion plv = new MPriceListVersion(ctx, SUPPLIER_PRICELIST_ID, trxName);
		MProductPrice price = new MProductPrice(plv, subProduct_id,
								Env.ONE, Env.ONE, Env.ONE);
		price.setIsActive(true);
		price.saveEx();
	}

	private void addProductToBOM(MProduct subProduct) {
		MPPProductBOMLine bomLine = new MPPProductBOMLine(bom);
		bomLine.setM_Product_ID(subProduct.getM_Product_ID());
		bomLine.setC_UOM_ID(subProduct.getC_UOM_ID());
		bomLine.setQtyBOM(Env.ONE);
		bomLine.setComponentType(MPPProductBOMLine.COMPONENTTYPE_Component);
		bomLine.saveEx();
	}

}