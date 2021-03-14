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
import static org.adempiere.util.attributes.AttributeUtilities.findMatchingAttributeSetInstance;
import static org.adempiere.util.attributes.AttributeUtilities.hasMandatoryValues;
import static org.adempiere.util.attributes.AttributeUtilities.hasValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.compiere.model.MAttribute;
import org.compiere.model.MAttributeInstance;
import org.compiere.model.MAttributeSet;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MAttributeUse;
import org.compiere.model.MAttributeValue;
import org.compiere.model.MTransaction;
import org.compiere.util.Env;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Utilities")
@Tag("AttributeUtilities")
class IT_AttributeUtilities extends AttributeTestSetup {

	private static MAttributeSetInstance asiWithValues = null;
	private static MAttributeSet attributeSet = null;
	private static MAttribute attributeString = null;
	private static MAttribute attributeNumber = null;
	private static MAttribute attributeList = null;
	private static MAttributeValue attributeValue1 = null;
	private static MAttributeValue attributeValue2 = null;
	private static MAttributeSetInstance asiWithNoValues;
	private static MAttributeSetInstance asiWithNoAttributeSet;
	private static MAttribute attributeStringMandatory;
	private static MAttributeSet attributeSetMandatory;

	private MAttributeSetInstance asiWithMandatoryAttributes;
	private MAttributeSetInstance asiWithAllMandatoryValues;

	@BeforeAll
	static void localSetUpBeforeClass()
	{

		attributeString = new MAttribute(ctx, 0, trxName);
		attributeString.setName("TestAttribute1");
		attributeString.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attributeString.setIsMandatory(false);
		attributeString.setIsInstanceAttribute(false);
		attributeString.saveEx();

		attributeStringMandatory = new MAttribute(ctx, 0, trxName);
		attributeStringMandatory.setName("MandatoryTestAttribute1");
		attributeStringMandatory.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attributeStringMandatory.setIsMandatory(true);
		attributeStringMandatory.setIsInstanceAttribute(false);
		attributeStringMandatory.saveEx();

		attributeNumber = new MAttribute(ctx, 0, trxName);
		attributeNumber.setName("TestAttribute2");
		attributeNumber.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_Number);
		attributeNumber.setIsMandatory(false);
		attributeNumber.setIsInstanceAttribute(false);
		attributeNumber.saveEx();

		attributeList = new MAttribute(ctx, 0, trxName);
		attributeList.setName("TestAttribute3");
		attributeList.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_List);
		attributeList.setIsMandatory(false);
		attributeList.setIsInstanceAttribute(false);
		attributeList.saveEx();

		attributeValue1 = new MAttributeValue(ctx, 0, trxName);
		attributeValue1.setM_Attribute_ID(attributeList.getM_Attribute_ID());
		attributeValue1.setName("List attribute item 1");
		attributeValue1.setValue("Item 1");
		attributeValue1.saveEx();

		attributeValue2 = new MAttributeValue(ctx, 0, trxName);
		attributeValue2.setM_Attribute_ID(attributeList.getM_Attribute_ID());
		attributeValue2.setName("List attribute item 2");
		attributeValue2.setValue("Item 2");
		attributeValue2.saveEx();

		attributeSet = new MAttributeSet(ctx, 0, trxName);
		attributeSet.setName("Test AttributeSet with attributes");
		attributeSet.saveEx();

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeString.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeNumber.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeList.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		attributeSetMandatory = new MAttributeSet(ctx, 0, trxName);
		attributeSetMandatory.setName("Test AttributeSet with attributes");
		attributeSetMandatory.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeStringMandatory.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSetMandatory.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeNumber.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSetMandatory.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeList.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSetMandatory.get_ID());
		attributeUse.saveEx();

	}

	@BeforeEach
	void setup()
	{

		product = createProductWithNoAttributeSet();
		movementType = MTransaction.MOVEMENTTYPE_InventoryIn;

		asiWithNoValues = new MAttributeSetInstance(ctx, 0, attributeSet.get_ID(), trxName);
		asiWithNoValues.saveEx();

		asiWithValues = new MAttributeSetInstance(ctx, 0, trxName);
		asiWithValues.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		asiWithValues.saveEx();

		asiWithMandatoryAttributes = new MAttributeSetInstance(ctx, 0, trxName);
		asiWithMandatoryAttributes
				.setM_AttributeSet_ID(attributeSetMandatory.getM_AttributeSet_ID());
		asiWithMandatoryAttributes.saveEx();

		asiWithAllMandatoryValues = new MAttributeSetInstance(ctx, 0, trxName);
		asiWithAllMandatoryValues
				.setM_AttributeSet_ID(attributeSetMandatory.getM_AttributeSet_ID());
		asiWithAllMandatoryValues.saveEx();

		asiWithNoAttributeSet = new MAttributeSetInstance(ctx, 0, trxName);

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx,
				attributeString.getM_Attribute_ID(),
				asiWithValues.getM_AttributeSetInstance_ID(),
				"stringAttributeValue",
				trxName);
		attributeInstance.saveEx();

		attributeInstance = new MAttributeInstance(ctx,
				attributeNumber.getM_Attribute_ID(),
				asiWithValues.getM_AttributeSetInstance_ID(),
				Env.ONE,
				trxName);
		attributeInstance.saveEx();

		attributeInstance = new MAttributeInstance(ctx,
				attributeList.getM_Attribute_ID(),
				asiWithValues.getM_AttributeSetInstance_ID(),
				attributeValue1.getM_AttributeValue_ID(),
				attributeValue1.getValue(),
				trxName);
		attributeInstance.saveEx();

		attributeInstance = new MAttributeInstance(ctx,
				attributeStringMandatory.getM_Attribute_ID(),
				asiWithAllMandatoryValues.getM_AttributeSetInstance_ID(),
				"MandatoryStringAttributeValue",
				trxName);
		attributeInstance.saveEx();

		attributeInstance = new MAttributeInstance(ctx,
				attributeNumber.getM_Attribute_ID(),
				asiWithAllMandatoryValues.getM_AttributeSetInstance_ID(),
				Env.ONE,
				trxName);
		attributeInstance.saveEx();

		attributeInstance = new MAttributeInstance(ctx,
				attributeList.getM_Attribute_ID(),
				asiWithAllMandatoryValues.getM_AttributeSetInstance_ID(),
				attributeValue1.getM_AttributeValue_ID(),
				attributeValue1.getValue(),
				trxName);
		attributeInstance.saveEx();

	}

	private MAttributeSetInstance copyAttributeSetInstance(
			MAttributeSetInstance attributeSetInstance)
	{

		MAttributeSetInstance newASI = new MAttributeSetInstance(ctx, 0, trxName);
		newASI.setAD_Org_ID(attributeSetInstance.getAD_Org_ID());
		newASI.setM_AttributeSet_ID(attributeSetInstance.getM_AttributeSet_ID());
		newASI.setGuaranteeDate(attributeSetInstance.getGuaranteeDate());
		newASI.setLot(attributeSetInstance.getLot());
		newASI.setSerNo(attributeSetInstance.getSerNo());
		return newASI;

	}

	private Object[] makeExactCopyAttributeValues(MAttributeSet attributeSet,
			MAttributeSetInstance attributeSetInstance)
	{

		return copyAttributeValues(attributeSet, attributeSetInstance, true, false);

	}

	private Object[] makeInexactCopyAttributeValues(MAttributeSet attributeSet,
			MAttributeSetInstance attributeSetInstance)
	{

		return copyAttributeValues(attributeSet, attributeSetInstance, false, false);

	}

	private Object[] makeDifferentCopyAttributeValues(MAttributeSet attributeSet,
			MAttributeSetInstance attributeSetInstance)
	{

		return copyAttributeValues(attributeSet, attributeSetInstance, false, true);

	}

	private Object[] copyAttributeValues(MAttributeSet attributeSet,
			MAttributeSetInstance attributeSetInstance,
			boolean makeExactCopy, boolean changeAll)
	{

		MAttribute[] attributes = attributeSet.getMAttributes();
		int m_attributeSetInstance_id = attributeSetInstance.getM_AttributeSetInstance_ID();

		Object[] values = new Object[attributes.length];
		for (int i = 0; i < attributes.length; i++)
		{

			MAttribute attribute = attributes[i];
			MAttributeInstance instance = attribute
					.getMAttributeInstance(m_attributeSetInstance_id);

			if (MAttribute.ATTRIBUTEVALUETYPE_List.equals(attribute.getAttributeValueType()))
			{

				values[i] = (MAttributeValue) instance.getM_AttributeValue();
				if (!makeExactCopy && changeAll)
					values[i] = -1;

			} else if (MAttribute.ATTRIBUTEVALUETYPE_Number
					.equals(attribute.getAttributeValueType()))
			{

				values[i] = instance.getValueNumber();
				if (!makeExactCopy && changeAll)
					values[i] = instance.getValueNumber().add(Env.ONE);

			} else // Text Field
			{

				if (makeExactCopy)
					values[i] = instance.getValue();
				else
					values[i] = values[i] + randomString(4);

			}

		}
		return values;

	}

	@Test
	void findMatchingAttributeSetInstance_ifPassedNullThrowsNPE()
	{

		assertThrows(NullPointerException.class, () -> {

			findMatchingAttributeSetInstance(ctx, null, null, trxName);

		});

	}

	@Test
	void findMatchingAttributeSetInstance_ifPassedNullValuesReturns0()
	{

		int matchingID = findMatchingAttributeSetInstance(ctx, asiWithValues, null, trxName);
		assertEquals(0, matchingID, "Found an ID when passed a null Object array");

	}

	@Test
	void findMatchingAttributeSetInstance_ifPassedEmptyValuesReturns0()
	{

		int matchingID = findMatchingAttributeSetInstance(ctx, asiWithValues, new Object[] { null },
				trxName);
		assertEquals(0, matchingID, "Found an ID when passed an empty Object array");

	}

	@Test
	void findMatchingAttributeSetInstance_ifNoMatchReturns0()
	{

		asiWithValues.setMAttributeSet(attributeSet);
		Object[] otherValues = makeInexactCopyAttributeValues(attributeSet, asiWithValues);
		MAttributeSetInstance newASI = copyAttributeSetInstance(asiWithValues);

		int matchingID = findMatchingAttributeSetInstance(ctx, newASI, otherValues, trxName);

		assertEquals(0, matchingID, "Found an ID for other values when none should exist");

	}

	@Test
	void findMatchingAttributeSetInstance_returnsIDIfMatchFound()
	{

		asiWithValues.setMAttributeSet(attributeSet);
		Object[] values = makeExactCopyAttributeValues(attributeSet, asiWithValues);
		MAttributeSetInstance newASI = copyAttributeSetInstance(asiWithValues);

		int matchingID = findMatchingAttributeSetInstance(ctx, newASI, values, trxName);
		assertEquals(asiWithValues.get_ID(), matchingID,
				"Matching Attribute Set Instance ID not found");

	}

	@Test
	void hasValues_ifPassedNullShouldReturnFalse()
	{

		assertFalse(hasValues(asiWithValues, null),
				"hasValues should return false if passed a null set of values");

	}

	@Test
	void hasValues_ifASIHasNoAttSetShouldReturnFalse()
	{

		asiWithValues.setM_AttributeSet_ID(0);
		assertFalse(hasValues(asiWithValues, new Object[] { "1" }),
				"hasValues should return false if the ASI has no attribute set.");

	}

	@Test
	void hasValues_ifPassedSameValuesAsASIShouldReturnTrue()
	{

		Object[] values = makeExactCopyAttributeValues(attributeSet, asiWithValues);
		assertTrue(hasValues(asiWithValues, values),
				"hasValues should return true when passed the same set of values as the ASI");

	}

	@Test
	void hasValues_ifPassedDifferentValuesAsASIShouldReturnTrue()
	{

		Object[] differentValues = makeDifferentCopyAttributeValues(attributeSet, asiWithValues);
		assertFalse(hasValues(asiWithValues, differentValues),
				"hasValues should return false when passed a different set of values than the ASI");

	}

	@Test
	void hasValues_ifASIHasNoValuesAssignedButValuesAreValidShouldReturnFalse()
	{

		Object[] values = makeExactCopyAttributeValues(attributeSet, asiWithValues);

		assertFalse(hasValues(asiWithNoValues, values),
				"hasValues should return false when the ASI has no values "
						+ "assigned but is passed a valid set of values");

	}

	@Test
	void hasValues_ifASIHasNoValuesAssignedAndValuesAreEmptyShouldReturnTrue()
	{

		MAttribute[] attributes = attributeSet.getMAttributes();
		Object[] values = new Object[attributes.length];

		assertTrue(hasValues(asiWithNoValues, values),
				"hasValues should return true when the ASI has no values assigned and is passed an empty array");

	}

	@Test
	void hasValues_ifNumberOfAttributesAndSizeOfValueArrayDontMatchReturnFalse()
	{

		MAttribute[] attributes = attributeSet.getMAttributes();
		Object[] longerSetOfValues = new Object[attributes.length + 1];
		assertFalse(hasValues(asiWithValues, longerSetOfValues),
				"hasValues should return false when the number of attributes and size of the values don't match");

	}

	@Test
	void hasMandatoryValues_ifPassNullThrowNPE()
	{

		assertThrows(NullPointerException.class, () -> {

			hasMandatoryValues(null);

		},
				"hasMandatoryValues should throw NPE when passed a null value");

	}

	@Test
	void hasMandatoryValues_ifASIHasNoAttSetThrowNPE()
	{

		assertThrows(NullPointerException.class, () -> {

			hasMandatoryValues(asiWithNoAttributeSet);

		},
				"hasMandatoryValues should throw NPE when passed an ASI with no attribute set.");

	}

	void hasMandatoryValues_whenPassedAnASIWithAnAttSetWithNoAttReturnTrue()
	{

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("test");
		as.setIsLot(true);
		as.saveEx();

		asi.setMAttributeSet(as);
		asi.saveEx();

		assertTrue(hasMandatoryValues(asi),
				"hasMandatoryValues should return true if passed an ASI with an attriute set that has no attributes.");

	}

	@Test
	void hasMandatoryValues_ifAttSetHasNotAttributesButIncludesLotShouldIgnoreLot()
	{

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("test");
		as.setIsLot(true);
		as.setIsLotMandatory(false);
		as.saveEx();

		asi.setMAttributeSet(as);
		asi.saveEx();

		assertTrue(hasMandatoryValues(asi),
				"hasMandatoryValues should return true if passed an ASI with an attriute set that has no attributes.");

	}

	@Test
	void hasMandatoryValues_ifAttSetHasNotAttributesButIncludesMandtoryLotShouldIgnoreLot()
	{

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("test");
		as.setIsLot(true);
		as.setIsLotMandatory(true);
		as.saveEx();

		asi.setMAttributeSet(as);
		asi.saveEx();

		assertTrue(hasMandatoryValues(asi),
				"hasMandatoryValues should return true if passed an ASI with an attriute set that has no attributes.");

	}

	@Test
	void hasMandatoryValues_ifNoAttributeIsMandatoryReturnTrue()
	{

		assertTrue(hasMandatoryValues(asiWithNoValues),
				"hasMandtoryValues should return true if none of the attributes are mandatory");

	}

	@Test
	void hasMandatoryValues_ifOneAttributeIsMandatoryReturnFalse()
	{

		assertFalse(hasMandatoryValues(asiWithMandatoryAttributes),
				"hasMandtoryValues should return false if one of the attributes is mandatory but not assigned");

	}

	@Test
	void hasMandatoryValues_ifASIHasAllMandatoryValuesReturnTrue()
	{

		assertTrue(hasMandatoryValues(asiWithAllMandatoryValues),
				"hasMandtoryValues should return true if all of the mandatory attributes are assigned");

	}
}
