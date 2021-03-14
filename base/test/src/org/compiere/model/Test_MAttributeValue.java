package org.compiere.model;

import static org.adempiere.test.TestUtilities.randomString;
import static org.compiere.model.I_M_AttributeValue.COLUMNNAME_M_AttributeValue_ID;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonGWSetup;
import org.compiere.util.DB;
import org.junit.jupiter.api.Test;

class Test_MAttributeValue extends CommonGWSetup {

	@Test
void testMAttributeValue_ToString() {
		
		String testName = randomString(6);
		
		MAttributeValue value = new MAttributeValue(ctx, 0, trxName);
		value.setName(testName);
		
		assertEquals(testName, value.toString(), "toString not returning name");
		
	}

	@Test
void testMAttributeValue_Constructor_PropertiesIdTrxName() {
		
		MAttribute attribute = new MAttribute(ctx, 0, trxName);  // Required to reset new record
		attribute.setName("mandatoryName");
		attribute.saveEx();
		
		MAttributeValue attributeValue = new MAttributeValue(ctx, 0, trxName);
		assertEquals(0, attributeValue.get_ID(), "ID is not zero for unsaved instance");		
		AdempiereException e = assertThrows(AdempiereException.class, attributeValue::saveEx);
		assertTrue(e.getMessage().startsWith("ERROR: null value in column"));


		attributeValue = new MAttributeValue(ctx, 0, trxName);
		attributeValue.setM_Attribute_ID(attribute.getM_Attribute_ID());
		e = assertThrows(AdempiereException.class, attributeValue::saveEx);
		assertTrue(e.getMessage().startsWith("ERROR: null value in column"));
		
		attributeValue = new MAttributeValue(ctx, 0, trxName);
		attributeValue.setM_Attribute_ID(attribute.getM_Attribute_ID());
		attributeValue.setName("mandatoryName");
		attributeValue.saveEx();
		
		int id = attributeValue.get_ID();
		assertTrue(id > 0, "ID not set after save");
		
		attributeValue = new MAttributeValue(ctx, id, trxName);
		assertEquals(id, attributeValue.get_ID(), "ID isn't persisted");
		
	}

	@Test
void testMAttributeValue_Constructor_PropertiesResultSetString() {

		boolean tested = false;
		String sql = "SELECT * FROM M_AttributeValue "
				+ "WHERE AD_Client_ID=?";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, AD_CLIENT_ID);
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MAttributeValue attributeValue = new MAttributeValue(ctx, rs, trxName);
				assertNotNull(attributeValue, "Constructor returns null");		
				assertEquals(rs.getInt(COLUMNNAME_M_AttributeValue_ID), attributeValue.get_ID(), "MAttributeValue id doesn't match record set.");
				tested = true;
				break;
			}
			rs.close ();
			pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
		try
		{
			if (pstmt != null)
				pstmt.close ();
			pstmt = null;
		}
		catch (Exception e)
		{
			fail(e.getMessage());
			pstmt = null;
		}

		assertTrue(tested, "No valid result set found. Result set not tested.");

	}

}
