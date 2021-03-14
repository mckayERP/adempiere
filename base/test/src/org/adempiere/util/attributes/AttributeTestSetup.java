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
package org.adempiere.util.attributes;

import static org.adempiere.test.TestUtilities.randomString;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_StringMax40;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_AlwaysMandatory;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_NotMandatory;

import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.test.CommonGWSetup;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeUse;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.model.X_M_AttributeSetExclude;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

public abstract class AttributeTestSetup extends CommonGWSetup {

	protected static MProduct product = null;
	protected static String movementType;

	protected static final int M_Warehouse_ID = 103; // GW HQ Warehouse
	protected static final boolean SALES_TRX = true;
	protected static final boolean PURCHASE_TRX = false;

	protected static void addAttributeToAttributeSet(MAttribute attribute,
			MAttributeSet attributeSet)
	{

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

	}

	protected static void addNoInstanceAttrirbuteSetToProduct(MProduct product)
	{

		MAttributeSet notInstanceAS = createAttributeSet();
		product.setM_AttributeSet_ID(notInstanceAS.getM_AttributeSet_ID());

	}

	protected static void createASITemplateAndAddToProduct(MProduct product)
	{

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(product.getM_AttributeSet_ID());
		asi.saveEx();

		product.setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
		product.saveEx();

	}

	protected static MAttributeSet createAttributeSet()
	{

		return createAttributeSet(null);

	}

	protected static MAttributeSet createAttributeSet(String mandatoryType)
	{

		mandatoryType = Optional.ofNullable(mandatoryType).orElse(MANDATORYTYPE_NotMandatory);
		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setMandatoryType(mandatoryType);
		as.setName("TestAttributeSet_" + randomString(4));
		as.saveEx();
		return as;

	}

	protected static MAttribute createAttribute(boolean isMandatory, boolean isInstanceAttribute,
			String type)
	{

		MAttribute attribute = new MAttribute(ctx, 0, trxName);
		attribute.setName("TestAttribute_" + randomString(4));
		attribute.setAttributeValueType(type);
		attribute.setIsMandatory(isMandatory);
		attribute.setIsInstanceAttribute(isInstanceAttribute);
		attribute.saveEx();
		return attribute;

	}

	protected static MAttributeSet createAttributeSetAndAddToProduct(MProduct product)
	{

		return createAttributeSetAndAddToProduct(product, null);

	}

	protected static MAttributeSet createAttributeSetAndAddToProduct(MProduct product,
			String mandatoryType)
	{

		mandatoryType = Optional.ofNullable(mandatoryType).orElse(MANDATORYTYPE_NotMandatory);

		MAttributeSet as = createAttributeSet(mandatoryType);
		product.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		if (MANDATORYTYPE_AlwaysMandatory.equals(mandatoryType))
			createASITemplateAndAddToProduct(product);
		product.saveEx();
		product.load(trxName);
		return as;

	}

	protected static IDocumentLine[] createMaterialReceiptLines(MProduct product,
			MAttributeSetInstance asi)
	{

		int asi_id = 0;
		if (asi != null)
			asi_id = asi.getM_AttributeSetInstance_ID();

		int m_locator_id = MWarehouse.get(ctx, M_Warehouse_ID).getDefaultLocator()
				.getM_Locator_ID();
		Timestamp now = new Timestamp(System.currentTimeMillis());
		MInOut mr = new MInOut(ctx, 0, trxName);
		mr.setIsSOTrx(false);
		mr.setC_DocType_ID();
		mr.setMovementType(MInOut.MOVEMENTTYPE_VendorReceipts);
		mr.setC_BPartner_ID(SEEDFARM_ID); // GW Seed Farm
		mr.setC_BPartner_Location_ID(SEEDFARM_LOCATION_ID);
		mr.setM_Warehouse_ID(M_Warehouse_ID);
		mr.setMovementDate(now);
		mr.setDateAcct(now);
		mr.saveEx();

		MInOutLine mrLine = new MInOutLine(mr);
		mrLine.setProduct(product);
		mrLine.setM_AttributeSetInstance_ID(asi_id);
		mrLine.setM_Locator_ID(m_locator_id);
		mrLine.setQty(Env.ONE);
		mrLine.saveEx();

		IDocumentLine[] lines = mr.getLines(true);
		return lines;

	}

	protected static MProduct createProductWithNoAttributeSet()
	{

		MProduct product = new MProduct(ctx, 0, trxName);
		product.setName("ProductUnderTest_" + randomString(4));
		product.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		product.setM_Product_Category_ID(105); // GW Standard
		product.setC_TaxCategory_ID(107); // GW Standard
		product.setIsPurchased(true);
		product.setIsSold(true);
		product.setIsStocked(true);
		product.saveEx();
		product.load(trxName);
		return product;

	}

	protected static MAttributeSetInstance createSerialNoProductAndAddToInventory()
	{

		boolean isSerNo = true;
		boolean isSerNoMandatory = true;

		MAttributeSet attributeSetUnderTest = setupProductWithAttributeSetAndAttribute(product,
				MANDATORYTYPE_NotMandatory, null, isSerNo, isSerNoMandatory);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0,
				attributeSetUnderTest.getM_AttributeSet_ID(), trxName);
		asi.saveEx();

		String serialNumber = randomString(6);
		asi.setSerNo(serialNumber);
		asi.saveEx();

		int M_Locator_ID = MWarehouse.get(ctx, M_Warehouse_ID).getDefaultLocator()
				.getM_Locator_ID();

		MMPolicyTicket ticket = new MMPolicyTicket(ctx, 0, trxName);
		ticket.setMovementDate(TimeUtil.getDay(System.currentTimeMillis()));
		ticket.saveEx();
		MStorage.add(ctx, M_Warehouse_ID, 0, M_Locator_ID, product.getM_Product_ID(),
				asi.getM_AttributeSetInstance_ID(), 0, ticket.getM_MPolicyTicket_ID(), 0,
				Env.ONE, Env.ZERO, Env.ZERO, trxName);
		return asi;

	}

	protected static void excludeAttributeSetFromTable(MAttributeSet as, int tableId,
			boolean isSOTrx)
	{

		X_M_AttributeSetExclude ase = new X_M_AttributeSetExclude(ctx, 0, trxName);
		ase.setM_AttributeSet_ID(as.get_ID());
		ase.setAD_Table_ID(tableId);
		ase.setIsSOTrx(isSOTrx);
		ase.saveEx();

	}

	protected static MAttributeSet setupProductWithAttributeSetAndAttribute(MProduct product,
			String mandatoryType, boolean isAttributeMandatory, boolean isInstanceAttribute,
			boolean setSerNo, boolean isSerNoMandatory)
	{

		MAttribute attribute = createAttribute(
				isAttributeMandatory,
				isInstanceAttribute,
				ATTRIBUTEVALUETYPE_StringMax40);

		return setupProductWithAttributeSetAndAttribute(product, mandatoryType, attribute,
				setSerNo, isSerNoMandatory);

	}

	protected static MAttributeSet setupProductWithAttributeSetAndAttribute(MProduct product,
			String setMandatoryType, MAttribute attribute,
			boolean setSerNo, boolean isSerNoMandatory)
	{

		MAttributeSet as = createAttributeSetAndAddToProduct(product, setMandatoryType);
		excludeAttributeSetFromTable(as, MOrderLine.Table_ID, SALES_TRX);
		if (attribute != null)
			addAttributeToAttributeSet(attribute, as);

		if (setSerNo)
		{

			as.setIsSerNo(setSerNo);
			as.setIsSerNoMandatory(isSerNoMandatory);
			as.saveEx();

		}
		MAttributeSet.clearCache();

		return as;

	}

}