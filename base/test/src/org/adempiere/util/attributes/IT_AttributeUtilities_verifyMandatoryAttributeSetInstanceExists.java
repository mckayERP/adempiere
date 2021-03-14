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

import static org.adempiere.util.attributes.AttributeUtilities.INSTANCE_ATTRIBUTE;
import static org.adempiere.util.attributes.AttributeUtilities.MANDATORY;
import static org.adempiere.util.attributes.AttributeUtilities.NOT_INSTANCE_ATTRIBUTE;
import static org.adempiere.util.attributes.AttributeUtilities.NOT_MANDATORY;
import static org.adempiere.util.attributes.AttributeUtilities.verifyMandatoryAttributeSetInstancesExist;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_StringMax40;
import static org.compiere.model.X_M_AttributeSet.MANDATORYTYPE_AlwaysMandatory;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.adempiere.engine.IDocumentLine;
import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Utilities")
@Tag("AttributeUtilities")
class IT_AttributeUtilities_verifyMandatoryAttributeSetInstanceExists extends AttributeTestSetup {

	@BeforeEach
	void setup()
	{

		product = createProductWithNoAttributeSet();

	}

	@Test
	void throwsNPEIfPassedNull()
	{

		assertThrows(NullPointerException.class, () -> {
			verifyMandatoryAttributeSetInstancesExist(null);
		}, "verifyMandatoryAttributeSetInstancesExist should "
				+ "throw an NPE if passed a null set of lines");

	}

	@Test
	void ifPassedEmptyArrayReturnsNull()
	{

		assertNull(verifyMandatoryAttributeSetInstancesExist(new IDocumentLine[0]),
				"Should return null if passed an empty array");

	}

	@Test
	void ifProductHasNoASIReturnsNull()
	{

		IDocumentLine[] lines = createMaterialReceiptLines(product, null);
		assertNull(verifyMandatoryAttributeSetInstancesExist(lines),
				"Should return null if product has no attribute set");

	}

	@Test
	void IfAttributeSetHasNoAttributesReturnsNull()
	{

		MAttributeSet as = createAttributeSetAndAddToProduct(product,
				MANDATORYTYPE_AlwaysMandatory);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		asi.saveEx();

		IDocumentLine[] lines = createMaterialReceiptLines(product, asi);

		String result = verifyMandatoryAttributeSetInstancesExist(lines);
		assertNull(result,
				"Should return null if the attribute set has no attributes.");

	}

	@Test
	void ifAttributeSetHasNoMandatoryAttributesReturnsNull()
	{

		MAttributeSet as = setupProductWithAttributeSetAndAttribute(product,
				MANDATORYTYPE_AlwaysMandatory,
				NOT_MANDATORY, NOT_INSTANCE_ATTRIBUTE, false, false);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		asi.saveEx();

		IDocumentLine[] lines = createMaterialReceiptLines(product, asi);

		String result = verifyMandatoryAttributeSetInstancesExist(lines);
		assertNull(result, "Should return null if the attribute set has no mandatory attributes");

	}

	@Test
	void ifHasMandatoryAttributesWithNoValueReturnString()
	{

		MProduct product = createProductWithNoAttributeSet();
		MAttributeSet as = createAttributeSetAndAddToProduct(product,
				MANDATORYTYPE_AlwaysMandatory);

		MAttribute attributeString = createAttribute(
				MANDATORY,
				INSTANCE_ATTRIBUTE,
				ATTRIBUTEVALUETYPE_StringMax40);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		asi.saveEx();

		IDocumentLine[] lines = createMaterialReceiptLines(product, asi);

		// Do the following after the mrLine.SaveEx() or the ASI will be flagged
		// as invalid during the save. Need to mimic user changes to the attribute
		// after the line was saved.
		addAttributeToAttributeSet(attributeString, as);
		MAttributeSet.clearCache();

		String result = verifyMandatoryAttributeSetInstancesExist(lines);
		assertNotNull(result,
				"Should return a string if the attribute set "
						+ "has a mandatory attribute with no value");

	}

	@Test
	void ifAllMandatoryAttributesAreAssignedReturnNull()
	{

		MProduct product = createProductWithNoAttributeSet();
		MAttributeSet as = createAttributeSetAndAddToProduct(product,
				MANDATORYTYPE_AlwaysMandatory);

		MAttribute attributeString = createAttribute(
				MANDATORY,
				INSTANCE_ATTRIBUTE,
				ATTRIBUTEVALUETYPE_StringMax40);

		addAttributeToAttributeSet(attributeString, as);

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		asi.setM_AttributeSet_ID(as.getM_AttributeSet_ID());
		asi.saveEx();

		MAttributeInstance ai = new MAttributeInstance(ctx, 0, trxName);
		ai.setM_Attribute_ID(attributeString.getM_Attribute_ID());
		ai.setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
		ai.setValue("test");
		ai.saveEx();
		MAttributeSet.clearCache();

		IDocumentLine[] lines = createMaterialReceiptLines(product, asi);

		String result = verifyMandatoryAttributeSetInstancesExist(lines);
		assertNull(result,
				"Should return null if all of the mandatory attributes are assigned");

	}

}
