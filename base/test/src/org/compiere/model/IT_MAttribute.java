/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2008 SC ARHIPAC SERVICE SRL. All Rights Reserved.            *
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
 * For the text or an alternative of this  license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.compiere.model;

import static org.adempiere.test.TestUtilities.randomString;
import static org.compiere.model.I_M_Attribute.COLUMNNAME_M_Attribute_ID;
import static org.compiere.model.X_M_Attribute.ATTRIBUTEVALUETYPE_StringMax40;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.stream.Stream;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonGWSetup;
import org.compiere.util.DB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("Model")
@Tag("Attribute")
@Tag("MAttribute")
class IT_MAttribute extends CommonGWSetup {

	private static MAttribute attributeNotInstanceString = null;
	private static MAttribute attributeInstanceString = null;
	private static MAttribute attributeNotInstanceList = null;
	private static MAttribute attributeInstanceList = null;
	private static MAttribute attributeMandatoryList = null;
	private static MAttribute attributeNotMandatoryList = null;
	private static MAttribute attributeInstanceBigDecimal = null;
	private static MAttributeValue mandatoryListValue1 = null;
	private static MAttributeValue mandatoryListValue2 = null;
	private static MAttributeValue notMandatoryListValue1 = null;
	private static MAttributeValue notMandatoryListValue2 = null;
	private static MAttributeSet attributeSet = null;
	private static MAttributeSetInstance attributeSetInstance = null;
	private static MAttributeValue stringValue = null;

	@BeforeAll
	static void localSetupBeforeClass()
	{

		attributeNotInstanceString = new MAttribute(ctx, 0, trxName);
		attributeNotInstanceString.setName("notInstanceString");
		attributeNotInstanceString.setIsInstanceAttribute(false);
		attributeNotInstanceString.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attributeNotInstanceString.saveEx();

		attributeInstanceString = new MAttribute(ctx, 0, trxName);
		attributeInstanceString.setName("InstanceString");
		attributeInstanceString.setIsInstanceAttribute(true);
		attributeInstanceString.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attributeInstanceString.saveEx();

		attributeNotInstanceList = new MAttribute(ctx, 0, trxName);
		attributeNotInstanceList.setName("notInstanceList");
		attributeNotInstanceList.setIsInstanceAttribute(false);
		attributeNotInstanceList.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_List);
		attributeNotInstanceList.saveEx();

		attributeInstanceList = new MAttribute(ctx, 0, trxName);
		attributeInstanceList.setName("InstanceList");
		attributeInstanceList.setIsInstanceAttribute(true);
		attributeInstanceList.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_List);
		attributeInstanceList.saveEx();

		attributeMandatoryList = new MAttribute(ctx, 0, trxName);
		attributeMandatoryList.setName("MandatoryList");
		attributeMandatoryList.setIsInstanceAttribute(false);
		attributeMandatoryList.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_List);
		attributeMandatoryList.setIsMandatory(true);
		attributeMandatoryList.saveEx();

		attributeInstanceBigDecimal = new MAttribute(ctx, 0, trxName);
		attributeInstanceBigDecimal.setName("InstanceBigDecimal");
		attributeInstanceBigDecimal.setIsInstanceAttribute(true);
		attributeInstanceBigDecimal.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_Number);
		attributeInstanceBigDecimal.saveEx();

		mandatoryListValue1 = new MAttributeValue(ctx, 0, trxName);
		mandatoryListValue1.setM_Attribute_ID(attributeMandatoryList.get_ID());
		mandatoryListValue1.setName("ListItem1");
		mandatoryListValue1.setValue("B1");
		mandatoryListValue1.saveEx();

		mandatoryListValue2 = new MAttributeValue(ctx, 0, trxName);
		mandatoryListValue2.setM_Attribute_ID(attributeMandatoryList.get_ID());
		mandatoryListValue2.setName("ListItem2");
		mandatoryListValue2.setValue("A1");
		mandatoryListValue2.saveEx();

		MAttributeValue inactive = new MAttributeValue(ctx, 0, trxName);
		inactive.setM_Attribute_ID(attributeMandatoryList.get_ID());
		inactive.setName("InactiveList");
		inactive.setValue("Inactive");
		inactive.setIsActive(false);
		inactive.saveEx();

		attributeNotMandatoryList = new MAttribute(ctx, 0, trxName);
		attributeNotMandatoryList.setName("MandatoryList");
		attributeNotMandatoryList.setIsInstanceAttribute(false);
		attributeNotMandatoryList.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_List);
		attributeNotMandatoryList.setIsMandatory(false);
		attributeNotMandatoryList.saveEx();

		notMandatoryListValue1 = new MAttributeValue(ctx, 0, trxName);
		notMandatoryListValue1.setM_Attribute_ID(attributeNotMandatoryList.get_ID());
		notMandatoryListValue1.setName("ListItem1");
		notMandatoryListValue1.setValue("BB");
		notMandatoryListValue1.saveEx();

		notMandatoryListValue2 = new MAttributeValue(ctx, 0, trxName);
		notMandatoryListValue2.setM_Attribute_ID(attributeNotMandatoryList.get_ID());
		notMandatoryListValue2.setName("ListItem2");
		notMandatoryListValue2.setValue("AA");
		notMandatoryListValue2.saveEx();

		inactive = new MAttributeValue(ctx, 0, trxName);
		inactive.setM_Attribute_ID(attributeNotMandatoryList.get_ID());
		inactive.setName("InactiveList");
		inactive.setValue("Inactive");
		inactive.setIsActive(false);
		inactive.saveEx();

		attributeSet = new MAttributeSet(ctx, 0, trxName);
		attributeSet.setName("Test AttributeSet with attributes");
		attributeSet.saveEx();

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeNotInstanceString.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeInstanceString.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		attributeSetInstance = new MAttributeSetInstance(ctx, 0, trxName);
		attributeSetInstance.setMAttributeSet(attributeSet);
		attributeSetInstance.saveEx();

		stringValue = new MAttributeValue(ctx, 0, trxName);
		stringValue.setM_Attribute_ID(attributeNotInstanceString.get_ID());
		stringValue.setName(attributeNotInstanceString.getName());
		stringValue.setValue("Not Instance String Value");
		stringValue.saveEx();

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, 0, trxName);
		attributeInstance.setM_AttributeSetInstance_ID(attributeSetInstance.get_ID());
		attributeInstance.setM_Attribute_ID(attributeNotInstanceString.get_ID());
		attributeInstance.setM_AttributeValue_ID(stringValue.get_ID());
		attributeInstance.saveEx();

	}

	@Test
	void testMAttribute_ToString()
	{

		StringBuffer sb = new StringBuffer("MAttribute[");
		sb.append(attributeNotInstanceString.get_ID()).append("-")
				.append(attributeNotInstanceString.getName())
				.append(",Type=").append(attributeNotInstanceString.getAttributeValueType())
				.append(",Instance=").append(attributeNotInstanceString.isInstanceAttribute())
				.append("]");

		assertEquals(sb.toString(), attributeNotInstanceString.toString(),
				"toString isn't returning the expected value");

	}

	@Test
	void testMAttribute_AfterSave_AfterDelete()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");
		as.saveEx();

		X_M_AttributeUse attributeUse = new X_M_AttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeNotInstanceString.get_ID());
		attributeUse.setM_AttributeSet_ID(as.get_ID());
		attributeUse.saveEx();

		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertFalse(as.isMandatory());

		attributeNotInstanceString.setIsMandatory(true);
		attributeNotInstanceString.saveEx();
		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertTrue(as.isMandatory());

		attributeNotInstanceString.setIsInstanceAttribute(true);
		attributeNotInstanceString.saveEx();
		as.load(trxName);
		assertTrue(as.isInstanceAttribute());
		assertTrue(as.isMandatory());

		attributeNotInstanceString.delete(true);
		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertFalse(as.isMandatory());

	}

	@Test
	void testMAttribute_GetOfClient()
	{

		boolean found = false;

		boolean onlyProductAttributes = false;
		boolean onlyListAttributes = false;

		MAttribute[] result = MAttribute.getOfClient(ctx, onlyProductAttributes, onlyListAttributes,
				trxName);

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the non-instance, string attribute");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the instance, string attribute");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the non-instance, list attribute");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the instance, list attribute");

		onlyProductAttributes = true;
		onlyListAttributes = false;
		result = MAttribute.getOfClient(ctx, onlyProductAttributes, onlyListAttributes, trxName);

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the non-instance, string attribute");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertFalse(found, "Found the instance, string attribute but shouldn't have");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the non-instance, list attribute");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertFalse(found, "Found the instance, list attribute but shouldn't have");

		onlyProductAttributes = false;
		onlyListAttributes = true;
		result = MAttribute.getOfClient(ctx, onlyProductAttributes, onlyListAttributes, trxName);

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertFalse(found, "Found the non-instance, string attribute but shouldn't have");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertFalse(found, "Found the instance, string attribute but shouldn't have");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the non-instance, list attribute");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the instance, list attribute");

		onlyProductAttributes = true;
		onlyListAttributes = true;
		result = MAttribute.getOfClient(ctx, onlyProductAttributes, onlyListAttributes, trxName);

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertFalse(found, "Found the non-instance, string attribute but shouldn't have");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceString.get_ID())
				.findFirst()
				.isPresent();

		assertFalse(found, "Found the instance, string attribute but shouldn't have");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeNotInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertTrue(found, "Did not find the non-instance, list attribute");

		found = Stream.of(result)
				.filter(attribute -> attribute.get_ID() == attributeInstanceList.get_ID())
				.findFirst()
				.isPresent();

		assertFalse(found, "Did not find the instance, list attribute");

	}

	@Test
	void testMAttribute_Constructor_PropertiesIntTrxName()
	{

		MAttribute attribute = new MAttribute(ctx, 0, trxName);
		assertEquals(0, attribute.get_ID(), "ID is not zero for unsaved instance");
		assertEquals(ATTRIBUTEVALUETYPE_StringMax40, attribute.getAttributeValueType(),
				"Default Type not set as expected");
		assertFalse(attribute.isInstanceAttribute(),
				"Default IsInstance not set as expected");
		assertFalse(attribute.isMandatory(),
				"Mandatory not set as expected");

		AdempiereException e = assertThrows(AdempiereException.class, attribute::saveEx);
		assertTrue(e.getMessage()
				.startsWith("ERROR: null value in column \"name\" violates not-null constraint"));

		attribute = new MAttribute(ctx, 0, trxName); // Required to reset new record
		attribute.setName("mandatoryName");
		attribute.saveEx();

		int id = attribute.get_ID();
		assertTrue(id > 0, "ID not set after save");

		attribute = new MAttribute(ctx, id, trxName);
		assertEquals(id, attribute.get_ID(), "ID isn't persisted");

	}

	@Test
	void testMAttribute_Constructor_PropertiesResultSetTrxName()
	{

		boolean tested = false;
		String sql = "SELECT * FROM M_Attribute "
				+ "WHERE AD_Client_ID=?";
		PreparedStatement pstmt = null;
		try
		{

			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, AD_CLIENT_ID);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{

				MAttribute attribute = new MAttribute(ctx, rs, trxName);
				assertNotNull(attribute, "Constructor returns null");
				assertEquals(rs.getInt(COLUMNNAME_M_Attribute_ID), attribute.get_ID(),
						"MAttribute id doesn't match record set.");
				tested = true;
				break;

			}
			rs.close();
			pstmt.close();
			pstmt = null;

		} catch (Exception e)
		{

			fail(e.getMessage());

		}
		try
		{

			if (pstmt != null)
				pstmt.close();
			pstmt = null;

		} catch (Exception e)
		{

			fail(e.getMessage());
			pstmt = null;

		}

		assertTrue(tested, "No valid result set found. Result set not tested.");

	}

	@Test
	void testMAttribute_GetMAttributeValues()
	{

		MAttributeValue[] listValues = attributeNotInstanceString.getMAttributeValues();
		assertNull(listValues, "getMAttributeValues should return null if there is no list");

		// Not mandatory lists include null as first entry
		listValues = attributeNotMandatoryList.getMAttributeValues();
		assertNotNull(listValues);
		assertEquals(3, listValues.length);
		assertNull(listValues[0]);
		assertEquals(notMandatoryListValue2.get_ID(), listValues[1].get_ID());
		assertEquals(notMandatoryListValue1.get_ID(), listValues[2].get_ID());

		listValues = attributeMandatoryList.getMAttributeValues();
		assertNotNull(listValues);
		assertEquals(2, listValues.length);
		assertNotNull(listValues[0]);
		assertEquals(mandatoryListValue2.get_ID(), listValues[0].get_ID());
		assertEquals(mandatoryListValue1.get_ID(), listValues[1].get_ID());

	}

	@Test
	void testMAttribute_GetMAttributeInstance()
	{

		MAttributeInstance instance = attributeNotInstanceString
				.getMAttributeInstance(attributeSetInstance.get_ID());
		assertNotNull(instance);
		assertEquals(stringValue.getValue(), instance.getM_AttributeValue().getValue());

		MAttributeSetInstance asiWithNoValues = new MAttributeSetInstance(ctx, 0,
				attributeSet.getM_AttributeSet_ID(), trxName);
		asiWithNoValues.saveEx();

		instance = attributeNotInstanceString.getMAttributeInstance(asiWithNoValues.get_ID());
		assertNull(instance);

	}

	@Test
	void testMAttribute_SetMAttributeInstanceIntMAttributeValue()
	{

		MAttributeValue newStringValue = new MAttributeValue(ctx, 0, trxName);
		newStringValue.setM_Attribute_ID(attributeInstanceString.get_ID());
		newStringValue.setName(attributeInstanceString.getName());
		newStringValue.setValue("Instance String Value");
		newStringValue.saveEx();

		MAttributeInstance instance = attributeInstanceString
				.getMAttributeInstance(attributeSetInstance.get_ID());
		assertNull(instance);

		attributeInstanceString.setMAttributeInstance(attributeSetInstance.get_ID(),
				newStringValue);

		instance = attributeInstanceString.getMAttributeInstance(attributeSetInstance.get_ID());
		assertNotNull(instance);
		assertEquals(newStringValue.getValue(), instance.getValue());

	}

	@Test
	void testMAttribute_SetMAttributeInstanceIntString()
	{

		MAttributeInstance instance = attributeInstanceString
				.getMAttributeInstance(attributeSetInstance.get_ID());
		assertNull(instance);

		String testValue = randomString(6);
		attributeInstanceString.setMAttributeInstance(attributeSetInstance.get_ID(), testValue);

		instance = attributeInstanceString.getMAttributeInstance(attributeSetInstance.get_ID());
		assertNotNull(instance);

		assertEquals(testValue, instance.getValue(),
				"Attribute Instance value not set as expected");

	}

	@Test
	void testMAttribute_SetMAttributeInstanceIntBigDecimal()
	{

		MAttributeInstance instance = attributeInstanceString
				.getMAttributeInstance(attributeSetInstance.get_ID());
		assertNull(instance);

		BigDecimal testValue = new BigDecimal(1234.56).setScale(2, BigDecimal.ROUND_HALF_UP);
		attributeInstanceBigDecimal.setMAttributeInstance(attributeSetInstance.get_ID(), testValue);

		instance = attributeInstanceBigDecimal.getMAttributeInstance(attributeSetInstance.get_ID());
		assertNotNull(instance);

		assertEquals(testValue.toString(), instance.getValue(),
				"Attribute Instance value not set as expected");

	}

}
