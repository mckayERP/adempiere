package org.compiere.model;

import static org.compiere.model.I_M_AttributeUse.COLUMNNAME_M_Attribute_ID;
import static org.compiere.model.I_M_AttributeUse.COLUMNNAME_M_AttributeSet_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.test.CommonGWSetup;
import org.compiere.util.DB;
import org.junit.jupiter.api.Test;

class Test_MAttributeUse extends CommonGWSetup {


	@Test
void testMAttributeUse_AfterSave_AfterDelete() {
		
		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");
		as.saveEx();

		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertFalse(as.isMandatory());

		MAttribute attribute = new MAttribute(ctx, 0, trxName);
		attribute.setName("InstanceString");
		attribute.setIsInstanceAttribute(true);
		attribute.setIsMandatory(true);
		attribute.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attribute.saveEx();

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute.get_ID());
		attributeUse.setM_AttributeSet_ID(as.get_ID());
		attributeUse.saveEx();

		as.load(trxName);
		assertTrue(as.isInstanceAttribute());
		assertTrue(as.isMandatory());
		
		attributeUse.delete(true);
		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertFalse(as.isMandatory());

		
	}

	@Test
void testMAttributeUse_PropertiesIdTrxName() {
		
		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		assertNotNull(attributeUse);
		
		Exception e = assertThrows(IllegalArgumentException.class, () -> {new MAttributeUse(ctx, 1, trxName);});
		assertEquals("Multi-Key", e.getMessage());
		
	}

	@Test
void testMAttributeUsePropertiesResultSetString() {
		
		boolean tested = false;
		String sql = "SELECT * FROM M_AttributeUse "
				+ "WHERE AD_Client_ID=?";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, AD_CLIENT_ID);
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MAttributeUse attributeUse = new MAttributeUse(ctx, rs, trxName);
				assertNotNull(attributeUse, "Constructor returns null");
				assertEquals(rs.getInt(COLUMNNAME_M_Attribute_ID), attributeUse.getM_Attribute_ID(), "MAttributeUse attribute id doesn't match record set.");
				assertEquals(rs.getInt(COLUMNNAME_M_AttributeSet_ID), attributeUse.getM_AttributeSet_ID(), "MAttributeUse attributeSet id doesn't match record set.");
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
