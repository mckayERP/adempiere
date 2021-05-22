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
package org.compiere.model;

import static org.adempiere.test.TestUtilities.randomString;
import static org.compiere.model.I_M_AttributeInstance.COLUMNNAME_M_AttributeSetInstance_ID;
import static org.compiere.model.I_M_AttributeInstance.COLUMNNAME_M_Attribute_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.test.CommonGWSetup;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("Model")
@Tag("Attribute")
@Tag("MAttributeInstance")
class IT_MAttributeInstance extends CommonGWSetup {

	@Test
	void testMAttributeInstance_ToString()
	{

		String testValue = randomString(6);
		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, 0, trxName);

		assertNull(attributeInstance.toString());

		attributeInstance.setValue(testValue);

		assertEquals(testValue, attributeInstance.toString());

	}

	@Test
	void testMAttributeInstance_SetValueNumber()
	{

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, 0, trxName);

		assertEquals(Env.ZERO, attributeInstance.getValueNumber());
		assertNull(attributeInstance.getValue());

		BigDecimal testNumber = new BigDecimal(0.1230).setScale(4, BigDecimal.ROUND_HALF_UP);
		attributeInstance.setValueNumber(testNumber);
		assertEquals(testNumber, attributeInstance.getValueNumber(),
				"valueNumber not set as expected");
		assertEquals("0.123", attributeInstance.getValue(),
				"Value string not set as expected");

		testNumber = new BigDecimal(0.00010).setScale(6, BigDecimal.ROUND_HALF_UP);
		attributeInstance.setValueNumber(testNumber);
		assertEquals(testNumber, attributeInstance.getValueNumber(),
				"valueNumber not set as expected");
		assertEquals("0.0001", attributeInstance.getValue(), "Value string not set as expected");

		testNumber = new BigDecimal(56.7890).setScale(4, BigDecimal.ROUND_HALF_UP);
		attributeInstance.setValueNumber(testNumber);
		assertEquals(testNumber, attributeInstance.getValueNumber(),
				"valueNumber not set as expected");
		assertEquals("56.789", attributeInstance.getValue(),
				"Value string not set as expected");

		testNumber = new BigDecimal(1000).setScale(0, BigDecimal.ROUND_HALF_UP);
		attributeInstance.setValueNumber(testNumber);
		assertEquals(testNumber, attributeInstance.getValueNumber(),
				"valueNumber not set as expected");
		assertEquals("1000", attributeInstance.getValue(), "Value string not set as expected");

		testNumber = new BigDecimal(1000).setScale(-2, BigDecimal.ROUND_HALF_UP);
		attributeInstance.setValueNumber(testNumber);
		assertEquals(testNumber, attributeInstance.getValueNumber(),
				"valueNumber not set as expected");
		assertEquals("1.0E+3", attributeInstance.getValue(),
				"Value string not set as expected");

		testNumber = Env.ZERO.setScale(4);
		attributeInstance.setValueNumber(testNumber);
		assertEquals(testNumber, attributeInstance.getValueNumber(),
				"valueNumber not set as expected");
		assertEquals("0", attributeInstance.getValue(), "Value string not set as expected");

		testNumber = Env.ZERO.setScale(0);
		attributeInstance.setValueNumber(testNumber);
		assertEquals(testNumber, attributeInstance.getValueNumber(),
				"valueNumber not set as expected");
		assertEquals("0", attributeInstance.getValue(), "Value string not set as expected");

		attributeInstance.setValueNumber(null);
		assertEquals(Env.ZERO, attributeInstance.getValueNumber());
		assertNull(attributeInstance.getValue());

	}

	@Test
	void testMAttributeInstance_Constructor_PropertiesIdTrxName()
	{

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, 0, trxName);
		assertNotNull(attributeInstance);

		Exception e = assertThrows(IllegalArgumentException.class, () -> {

			new MAttributeUse(ctx, 1, trxName);

		});
		assertEquals("Multi-Key", e.getMessage());

	}

	@Test
	void testMAttributeInstance_Constructor_PropertiesResultSetTrxName()
	{

		boolean tested = false;
		String sql = "SELECT * FROM M_AttributeInstance "
				+ "WHERE AD_Client_ID=?";
		PreparedStatement pstmt = null;
		try
		{

			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, AD_CLIENT_ID);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{

				MAttributeInstance attributeInstance = new MAttributeInstance(ctx, rs, trxName);
				assertNotNull(attributeInstance, "Constructor returns null");
				assertEquals(rs.getInt(COLUMNNAME_M_Attribute_ID),
						attributeInstance.getM_Attribute_ID(),
						"MAttributeUse attribute id doesn't match record set.");
				assertEquals(rs.getInt(COLUMNNAME_M_AttributeSetInstance_ID),
						attributeInstance.getM_AttributeSetInstance_ID(),
						"MAttributeUse attributeSetInstance id doesn't match record set.");
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
	void testMAttributeInstance_Constructor_PropertiesIntIntStringTrxName()
	{

		int testAttributeId = 1;
		int testAttributeSetInstanceId = 2;
		String testValue = randomString(6);

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, testAttributeId,
				testAttributeSetInstanceId, testValue, trxName);
		assertNotNull(attributeInstance);
		assertEquals(testAttributeId, attributeInstance.getM_Attribute_ID());
		assertEquals(testAttributeSetInstanceId, attributeInstance.getM_AttributeSetInstance_ID());
		assertEquals(0, attributeInstance.getM_AttributeValue_ID());
		assertEquals(testValue, attributeInstance.getValue());

	}

	@Test
	void testMAttributeInstance_Constructor_PropertiesIntIntBigDecimalTrxName()
	{

		int testAttributeId = 1;
		int testAttributeSetInstanceId = 2;
		BigDecimal testValue = new BigDecimal(123.456).setScale(3, BigDecimal.ROUND_HALF_UP);

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, testAttributeId,
				testAttributeSetInstanceId, testValue, trxName);
		assertNotNull(attributeInstance);
		assertEquals(testAttributeId, attributeInstance.getM_Attribute_ID());
		assertEquals(testAttributeSetInstanceId, attributeInstance.getM_AttributeSetInstance_ID());
		assertEquals(0, attributeInstance.getM_AttributeValue_ID());
		assertEquals(testValue, attributeInstance.getValueNumber());

	}

	@Test
	void testMAttributeInstance_Constructor_PropertiesIntIntIntStringTrxName()
	{

		MAttribute attribute = new MAttribute(ctx, 0, trxName);
		attribute.setName("List Attribute");
		attribute.setIsInstanceAttribute(true);
		attribute.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_List);
		attribute.saveEx();

		MAttributeValue listValue1 = new MAttributeValue(ctx, 0, trxName);
		listValue1.setM_Attribute_ID(attribute.get_ID());
		listValue1.setName("List Item 1");
		listValue1.setValue("Item 1");
		listValue1.saveEx();

		MAttributeValue listValue2 = new MAttributeValue(ctx, 0, trxName);
		listValue2.setM_Attribute_ID(attribute.get_ID());
		listValue2.setName("List Item 2");
		listValue2.setValue("Item 2");
		listValue2.saveEx();

		int testAttributeId = attribute.get_ID();
		int testAttributeValueID = listValue1.getM_AttributeValue_ID();
		int testAttributeSetInstanceId = 2;

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, testAttributeId,
				testAttributeSetInstanceId,
				testAttributeValueID, listValue1.getValue(), trxName);

		assertNotNull(attributeInstance);
		assertEquals(testAttributeId, attributeInstance.getM_Attribute_ID());
		assertEquals(testAttributeSetInstanceId, attributeInstance.getM_AttributeSetInstance_ID());
		assertEquals(testAttributeValueID, attributeInstance.getM_AttributeValue_ID());
		assertEquals(listValue1.getValue(), attributeInstance.getValue());

	}

	@Test
	void testMAttributeInstance_Copy()
	{

		// Setup

		MAttribute attributeString = new MAttribute(ctx, 0, trxName);
		attributeString.setName("String Attribute");
		attributeString.setIsInstanceAttribute(true);
		attributeString.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attributeString.saveEx();

		MAttribute attributeList = new MAttribute(ctx, 0, trxName);
		attributeList.setName("List Attribute");
		attributeList.setIsInstanceAttribute(true);
		attributeList.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_List);
		attributeList.saveEx();

		MAttribute attributeNumber = new MAttribute(ctx, 0, trxName);
		attributeNumber.setName("Number Attribute");
		attributeNumber.setIsInstanceAttribute(true);
		attributeNumber.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_Number);
		attributeNumber.saveEx();

		MAttributeSet attributeSet = new MAttributeSet(ctx, 0, trxName);
		attributeSet.setName("Test AttributeSet with attributes");
		attributeSet.saveEx();

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeString.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeList.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attributeNumber.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSet.get_ID());
		attributeUse.saveEx();

		MAttributeSetInstance fromAttributeSetInstance = new MAttributeSetInstance(ctx, 0, trxName);
		fromAttributeSetInstance.setMAttributeSet(attributeSet);
		fromAttributeSetInstance.saveEx();

		MAttributeSetInstance toAttributeSetInstance = new MAttributeSetInstance(ctx, 0, trxName);
		toAttributeSetInstance.setMAttributeSet(attributeSet);
		toAttributeSetInstance.saveEx();

		MAttributeValue listValue = new MAttributeValue(ctx, 0, trxName);
		listValue.setM_Attribute_ID(attributeList.get_ID());
		listValue.setName(attributeList.getName());
		listValue.setValue("Item 1");
		listValue.saveEx();

		String testString = randomString(6);
		BigDecimal testNumber = new BigDecimal(123.456).setScale(3, BigDecimal.ROUND_HALF_UP);

		MAttributeInstance attributeStringInstance = new MAttributeInstance(ctx,
				attributeString.get_ID(), fromAttributeSetInstance.get_ID(),
				testString, trxName);
		attributeStringInstance.saveEx();

		MAttributeInstance attributeListInstance = new MAttributeInstance(ctx,
				attributeList.get_ID(), fromAttributeSetInstance.get_ID(),
				listValue.get_ID(), listValue.getValue(), trxName);
		attributeListInstance.saveEx();

		MAttributeInstance attributeNumberInstance = new MAttributeInstance(ctx,
				attributeNumber.get_ID(), fromAttributeSetInstance.get_ID(),
				testNumber, trxName);
		attributeNumberInstance.saveEx();

		// Tests
		MAttribute[] attributes = attributeSet.getMAttributes();
		for (MAttribute attribute : attributes)
		{

			MAttributeInstance instance = attribute
					.getMAttributeInstance(fromAttributeSetInstance.get_ID());
			assertNotNull(instance);

			instance = attribute.getMAttributeInstance(toAttributeSetInstance.get_ID());
			assertNull(instance);

		}

		MAttributeInstance.copy(ctx, toAttributeSetInstance.get_ID(),
				fromAttributeSetInstance.get_ID(), trxName);

		for (MAttribute attribute : attributes)
		{

			MAttributeInstance fromInstance = attribute
					.getMAttributeInstance(fromAttributeSetInstance.get_ID());
			assertNotNull(fromInstance);

			MAttributeInstance toInstance = attribute
					.getMAttributeInstance(toAttributeSetInstance.get_ID());
			assertNotNull(toInstance);

			assertEquals(toAttributeSetInstance.get_ID(),
					toInstance.getM_AttributeSetInstance_ID());
			assertEquals(fromInstance.getM_Attribute_ID(), toInstance.getM_Attribute_ID());
			assertEquals(fromInstance.getM_AttributeValue_ID(),
					toInstance.getM_AttributeValue_ID());
			assertEquals(fromInstance.getValueNumber(), toInstance.getValueNumber());
			assertEquals(fromInstance.getValue(), toInstance.getValue());

		}

	}

}
