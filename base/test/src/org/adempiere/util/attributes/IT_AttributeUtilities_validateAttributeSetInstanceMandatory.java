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

import static org.adempiere.util.attributes.AttributeUtilities.ATTRIBUTE_UTILITIES_MISSING_ATTRIBUTE_INSTANCE_VALUES;
import static org.adempiere.util.attributes.AttributeUtilities.INSTANCE_ATTRIBUTE;
import static org.adempiere.util.attributes.AttributeUtilities.MANDATORY;
import static org.adempiere.util.attributes.AttributeUtilities.NOT_INSTANCE_ATTRIBUTE;
import static org.adempiere.util.attributes.AttributeUtilities.NOT_MANDATORY;
import static org.adempiere.util.attributes.AttributeUtilities.validateAttributeSetInstanceMandatory;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_StringMax40;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_AlwaysMandatory;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_NotMandatory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;

@Tag("Utilities")
@Tag("AttributeUtilities")
class IT_AttributeUtilities_validateAttributeSetInstanceMandatory extends AttributeTestSetup {

	private static MProduct product = null;

	private static final boolean SALES_TRX = true;
	private static final boolean PURCHASE_TRX = false;

	@BeforeEach
	void setup()
	{

		product = createProductWithNoAttributeSet();

	}

	static Stream<Arguments> tableAndTrxTypeProvider()
	{

		return Stream.of(
				arguments(MOrderLine.Table_ID, SALES_TRX),
				arguments(MOrderLine.Table_ID, PURCHASE_TRX),
				arguments(MInOutLine.Table_ID, SALES_TRX),
				arguments(MInOutLine.Table_ID, PURCHASE_TRX));

	}

	@Test
	void whenPassedNullShouldReturNull()
	{

		assertNull(validateAttributeSetInstanceMandatory(ctx, null, 0, SALES_TRX, 0, trxName),
				"validateAttributeSetInstanceMandatory should return null if passed a null product");

	}

	@Test
	void ifProductHasNoAttributeSetShouldReturNull()
	{

		MProduct product = createProductWithNoAttributeSet();

		assertNull(validateAttributeSetInstanceMandatory(ctx, product,
				MOrderLine.Table_ID, true, 0, trxName),
				"Should return null if product has no attribute set");

	}

	@Test
	void ifProductAttributeSetIsNotAnInstanceShouldReturNull()
	{

		MAttribute a_notInstanceNotMandatory = createAttribute(
				NOT_MANDATORY,
				NOT_INSTANCE_ATTRIBUTE,
				ATTRIBUTEVALUETYPE_StringMax40);

		MAttributeSet as = createAttributeSetAndAddToProduct(product, MANDATORYTYPE_NotMandatory);
		excludeAttributeSetFromTable(as, MOrderLine.Table_ID, SALES_TRX);
		addAttributeToAttributeSet(a_notInstanceNotMandatory, as);
		MAttributeSet.clearCache();

		assertNull(validateAttributeSetInstanceMandatory(ctx, product,
				MOrderLine.Table_ID, true, 0, trxName),
				"Should return null if product attribute set is not mandatory");

	}

	@Test
	void shouldReturTrueIfProductAttributeSetIsMandatory()
	{

		MAttribute a_instanceMandatory = createAttribute(
				MANDATORY,
				INSTANCE_ATTRIBUTE,
				ATTRIBUTEVALUETYPE_StringMax40);

		MAttributeSet as = createAttributeSetAndAddToProduct(product,
				MANDATORYTYPE_AlwaysMandatory);
		excludeAttributeSetFromTable(as, MOrderLine.Table_ID, SALES_TRX);
		addAttributeToAttributeSet(a_instanceMandatory, as);
		MAttributeSet.clearCache();

		String result = validateAttributeSetInstanceMandatory(ctx, product,
				MOrderLine.Table_ID, PURCHASE_TRX, 0, trxName);
		assertTrue(result != null && !result.isEmpty(),
				"Should return a string if attribute set is a mandatory instance "
						+ "and attribute set instance id is 0");
		assertEquals(ATTRIBUTE_UTILITIES_MISSING_ATTRIBUTE_INSTANCE_VALUES
				+ product.getName(), result,
				"String should match the expected value");

	}

	@Test
	void shouldReturNullIfAttributeSetHasNoAttributes()
	{

		MAttributeSet as = createAttributeSetAndAddToProduct(product,
				MANDATORYTYPE_AlwaysMandatory);
		as.setIsLot(true);
		as.saveEx();
		MAttributeSet.clearCache();

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		asi.saveEx();

		String result = validateAttributeSetInstanceMandatory(ctx, product,
				MOrderLine.Table_ID, PURCHASE_TRX,
				asi.getM_AttributeSetInstance_ID(), trxName);
		assertNull(result,
				"Should return null if the attribute set has no attributes even if the "
						+ "attribute set has mandatory but empty lot.");

	}

	@Test
	void shouldReturnResultIfAttributeSetHasMandatoryAttributesWithNoValue()
	{

		MAttribute a_instanceMandatory = createAttribute(
				MANDATORY,
				INSTANCE_ATTRIBUTE,
				ATTRIBUTEVALUETYPE_StringMax40);

		MAttributeSet as = createAttributeSetAndAddToProduct(product,
				MANDATORYTYPE_AlwaysMandatory);
		excludeAttributeSetFromTable(as, MOrderLine.Table_ID, SALES_TRX);
		addAttributeToAttributeSet(a_instanceMandatory, as);
		MAttributeSet.clearCache();

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		asi.saveEx();

		String result = validateAttributeSetInstanceMandatory(ctx, product,
				MOrderLine.Table_ID, false, asi.getM_AttributeSetInstance_ID(), trxName);
		assertNotNull(result,
				"Should return a string if the attribute set "
						+ "has a mandatory attribute with no value");

	}

	@Test
	void shouldReturnNullIfAttributeSetHasAllMandatoryValues()
	{

		MAttribute a_instanceMandatory = createAttribute(
				MANDATORY,
				INSTANCE_ATTRIBUTE,
				ATTRIBUTEVALUETYPE_StringMax40);

		MAttributeSet as = createAttributeSetAndAddToProduct(product,
				MANDATORYTYPE_AlwaysMandatory);
		excludeAttributeSetFromTable(as, MOrderLine.Table_ID, SALES_TRX);
		addAttributeToAttributeSet(a_instanceMandatory, as);
		MAttributeSet.clearCache();

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		asi.saveEx();

		MAttributeInstance ai = new MAttributeInstance(ctx, 0, trxName);
		ai.setM_Attribute_ID(a_instanceMandatory.getM_Attribute_ID());
		ai.setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
		ai.setValue("test");
		ai.saveEx();

		String result = validateAttributeSetInstanceMandatory(ctx, product,
				MOrderLine.Table_ID, false, asi.getM_AttributeSetInstance_ID(), trxName);
		assertNull(result, "Should return null if all of the mandatory "
				+ "attributes are assigned values");

	}
}
