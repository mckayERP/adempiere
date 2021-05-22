package org.compiere.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.test.CommonGWSetup;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Test_MAttributeSet extends CommonGWSetup {

	private static MAttributeSet attributeSetUnderTest = null;
	private static MAttribute attribute1 = null;
	private static MAttribute attribute2 = null;

	@BeforeAll
	static void localSetUpBeforeClass()
	{

		attribute1 = new MAttribute(ctx, 0, trxName);
		attribute1.setName("TestAttribute1");
		attribute1.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attribute1.setIsMandatory(true);
		attribute1.setIsInstanceAttribute(false);
		attribute1.saveEx();

		attribute2 = new MAttribute(ctx, 0, trxName);
		attribute2.setName("TestAttribute2");
		attribute2.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attribute2.setIsMandatory(true);
		attribute2.setIsInstanceAttribute(true);
		attribute2.saveEx();

		attributeSetUnderTest = new MAttributeSet(ctx, 0, trxName);
		attributeSetUnderTest.setName("Test AttributeSet with attributes");
		attributeSetUnderTest.saveEx();

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute1.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSetUnderTest.get_ID());
		attributeUse.saveEx();

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute2.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSetUnderTest.get_ID());
		attributeUse.saveEx();

	}

	@Test
	void testMAttributeSet_translations()
	{

		assertNotEquals("LinesWithoutProductAttribute",
				Msg.getMsg(ctx, "LinesWithoutProductAttribute"), "Not translated");

	}

	@Test
	void testMAttributeSet_constructor_propertiesIDTrxName()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);

		assertTrue(as instanceof MAttributeSet);
		assertEquals(0, as.get_ID(), "ID of new record should be zero");
		assertFalse(as.isGuaranteeDate(), "IsGuaranteeDate should be false");
		assertFalse(as.isGuaranteeDateMandatory(), "IsGuaranteeDateMandatory should be false");
		assertFalse(as.isLot(), "IsLot should be false");
		assertFalse(as.isLotMandatory(), "IsLotMandatory should be false");
		assertFalse(as.isSerNo(), "IsSerNo should be false");
		assertFalse(as.isSerNoMandatory(), "IsSerNoMandatory should be false");
		assertFalse(as.isInstanceAttribute(), "IsInstanceAttribute should be false");
		assertEquals(MAttributeSet.MANDATORYTYPE_NotMandatory,
				as.getMandatoryType(), "Mandatory type");

		as.setName("temp");
		as.saveEx();

		int id = as.get_ID();

		assertTrue(id > 0, "ID of saved record should be > zero");

		as = new MAttributeSet(ctx, id, trxName);

		assertEquals(id, as.get_ID(), "ID of saved record should match");

	}

	@Test
	void testMAttributeSet_get()
	{

		int M_AttributeSet_ID = 101;

		MAttributeSet as = MAttributeSet.get(ctx, M_AttributeSet_ID);

		assertEquals(M_AttributeSet_ID,
				as.get_ID(), "Get() did not return the correct attribute set");

	}

	@Test
	void testMAttributeSet_constructor_propertiesResultSetTrxName()
	{

		boolean tested = false;
		String sql = "SELECT * FROM M_AttributeSet "
				+ "WHERE AD_Client_ID=?";
		PreparedStatement pstmt = null;
		try
		{

			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, AD_CLIENT_ID);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next())
			{

				MAttributeSet as = new MAttributeSet(ctx, rs, trxName);
				assertNotNull(as, "Constructor returns null");
				assertEquals(
						rs.getInt(MAttributeSet.COLUMNNAME_M_AttributeSet_ID), as.get_ID(),
						"MAttributeSet id doesn't match record set.");
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
	void testMAttributeSet_getMAttributesByType()
	{

		boolean instanceAttributes = true;
		attributeSetUnderTest.clearMAttributeCache();

		MAttribute[] attributes = attributeSetUnderTest.getMAttributes(instanceAttributes);

		assertNotNull(attributes, "getMAttributes(boolean) returned null");
		assertEquals(1, attributes.length, "getMAttributes(boolean) returned wrong size");
		assertEquals(attribute2.get_ID(),
				attributes[0].get_ID(), "getMAttributes(boolean) returned wrong attribute");

		attributes = attributeSetUnderTest.getMAttributes(!instanceAttributes);

		assertNotNull(attributes, "getMAttributes(boolean) returned null");
		assertEquals(1, attributes.length, "getMAttributes(boolean) returned wrong size");
		assertEquals(attribute1.get_ID(),
				attributes[0].get_ID(), "getMAttributes(boolean) returned wrong attribute");

	}

	@Test
	void testMAttributeSet_getMAttributes()
	{

		MAttribute[] attributes = attributeSetUnderTest.getMAttributes();

		assertNotNull(attributes, "getMAttributes(boolean) returned null");
		assertEquals(2, attributes.length, "getMAttributes(boolean) returned wrong size");
		assertEquals(
				attribute2.get_ID(), attributes[0].get_ID(),
				"getMAttributes(boolean) returned attributes in wrong order");
		assertEquals(attribute1.get_ID(),
				attributes[1].get_ID(), "getMAttributes(boolean) returned wrong attribute");

	}

	@Test
	void testMAttributeSet_isMandatory()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_AlwaysMandatory);
		assertTrue(as.isMandatory());

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_WhenShipping);
		assertTrue(as.isMandatory());

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_NotMandatory);
		assertFalse(as.isMandatory());

		as.setIsLotMandatory(true);
		assertTrue(as.isMandatory());

		as.setIsLotMandatory(false);
		assertFalse(as.isMandatory());

		as.setIsSerNoMandatory(true);
		assertTrue(as.isMandatory());

		as.setIsSerNoMandatory(false);
		assertFalse(as.isMandatory());

		as.setIsGuaranteeDateMandatory(true);
		assertTrue(as.isMandatory());

		as.setIsGuaranteeDateMandatory(false);
		assertFalse(as.isMandatory());

	}

	@Test
	void testMAttributeSet_isMandatoryAlways()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_AlwaysMandatory);
		assertTrue(as.isMandatoryAlways());

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_WhenShipping);
		assertFalse(as.isMandatoryAlways());

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_NotMandatory);
		assertFalse(as.isMandatoryAlways());

	}

	@Test
	void testMAttributeSet_isMandatoryShipping()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_AlwaysMandatory);
		assertTrue(as.isMandatoryShipping());

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_WhenShipping);
		assertTrue(as.isMandatoryShipping());

		as.setMandatoryType(MAttributeSet.MANDATORYTYPE_NotMandatory);
		assertFalse(as.isMandatoryShipping());

	}

	@Test
	void testMAttributeSet_excludeEntry()
	{

		X_M_AttributeSetExclude ase = new X_M_AttributeSetExclude(ctx, 0, trxName);
		ase.setM_AttributeSet_ID(attributeSetUnderTest.get_ID());
		ase.setAD_Table_ID(MInOutLine.Table_ID);
		ase.setIsSOTrx(true);
		ase.saveEx();

		assertTrue(attributeSetUnderTest.excludeEntry(MInOutLine.Table_ID, true));
		assertFalse(attributeSetUnderTest.excludeEntry(MInOutLine.Table_ID, false));
		assertFalse(attributeSetUnderTest.excludeEntry(MOrderLine.Table_ID, true));
		assertFalse(attributeSetUnderTest.excludeEntry(MOrderLine.Table_ID, false));

	}

	@Test
	void testMAttributeSet_isExcludeLot()
	{

		MLotCtl lotCtl = new MLotCtl(ctx, 0, trxName);
		lotCtl.setName("temp");
		lotCtl.saveEx();

		X_M_LotCtlExclude ase = new X_M_LotCtlExclude(ctx, 0, trxName);
		ase.setM_LotCtl_ID(lotCtl.getM_LotCtl_ID());
		ase.setAD_Table_ID(MInOutLine.Table_ID);
		ase.setIsSOTrx(true);
		ase.saveEx();

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");
		as.setIsLot(true);
		as.setM_LotCtl_ID(lotCtl.getM_LotCtl_ID());

		int columnID = MColumn.getColumn_ID(MInOutLine.Table_Name,
				MInOutLine.COLUMNNAME_M_AttributeSetInstance_ID);
		int columnID2 = MColumn.getColumn_ID(MOrderLine.Table_Name,
				MOrderLine.COLUMNNAME_M_AttributeSetInstance_ID);

		assertTrue(as.isExcludeLot(columnID, true));
		assertFalse(as.isExcludeLot(columnID, false));
		assertFalse(as.isExcludeLot(columnID2, true));
		assertFalse(as.isExcludeLot(columnID2, false));

	}

	@Test
	void testMAttributeSet_isExcludeSerNo()
	{

		MSerNoCtl serCtl = new MSerNoCtl(ctx, 0, trxName);
		serCtl.setName("temp");
		serCtl.saveEx();

		X_M_SerNoCtlExclude ase = new X_M_SerNoCtlExclude(ctx, 0, trxName);
		ase.setM_SerNoCtl_ID(serCtl.getM_SerNoCtl_ID());
		ase.setAD_Table_ID(MInOutLine.Table_ID);
		ase.setIsSOTrx(true);
		ase.saveEx();

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");
		as.setIsSerNo(true);
		as.setM_SerNoCtl_ID(serCtl.getM_SerNoCtl_ID());

		int columnID = MColumn.getColumn_ID(MInOutLine.Table_Name,
				MInOutLine.COLUMNNAME_M_AttributeSetInstance_ID);
		int columnID2 = MColumn.getColumn_ID(MOrderLine.Table_Name,
				MOrderLine.COLUMNNAME_M_AttributeSetInstance_ID);

		assertTrue(as.isExcludeSerNo(columnID, true));
		assertFalse(as.isExcludeSerNo(columnID, false));
		assertFalse(as.isExcludeSerNo(columnID2, true));
		assertFalse(as.isExcludeSerNo(columnID2, false));

	}

	@Test
	void testMAttributeSet_getLotCharStart()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");

		assertEquals("\u00ab",
				as.getLotCharStart(), "Lot characther start does not default as expected");

		as.setLotCharSOverwrite(null);
		assertEquals("\u00ab",
				as.getLotCharStart(), "Lot characther start does not default as expected");

		as.setLotCharSOverwrite("");
		assertEquals("\u00ab",
				as.getLotCharStart(), "Lot characther start does not default as expected");

		as.setLotCharSOverwrite(" ");
		assertEquals("\u00ab",
				as.getLotCharStart(), "Lot characther start does not default as expected");

		as.setLotCharSOverwrite("aa");
		assertEquals("a",
				as.getLotCharStart(), "Lot characther start does truncate and pass expected value");

		as.setLotCharSOverwrite("b");
		assertEquals("b",
				as.getLotCharStart(), "Lot characther start does not pass expected value");

	}

	@Test
	void testMAttributeSet_getLotCharEnd()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");

		assertEquals("\u00bb",
				as.getLotCharEnd(), "Lot characther end does not default as expected");

		as.setLotCharEOverwrite(null);
		assertEquals("\u00bb",
				as.getLotCharEnd(), "Lot characther end does not default as expected");

		as.setLotCharEOverwrite("");
		assertEquals("\u00bb",
				as.getLotCharEnd(), "Lot characther end does not default as expected");

		as.setLotCharEOverwrite(" ");
		assertEquals("\u00bb",
				as.getLotCharEnd(), "Lot characther end does not default as expected");

		as.setLotCharEOverwrite("zz");
		assertEquals("z",
				as.getLotCharEnd(), "Lot characther end does truncate and pass expected value");

		as.setLotCharEOverwrite("y");
		assertEquals("y", as.getLotCharEnd(), "Lot characther end does not pass expected value");

	}

	@Test
	void testMAttributeSet_getSerNoCharStart()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");

		assertEquals("#",
				as.getSerNoCharStart(), "SerNo characther start does not default as expected");

		as.setSerNoCharSOverwrite(null);
		assertEquals("#",
				as.getSerNoCharStart(), "SerNo characther start does not default as expected");

		as.setSerNoCharSOverwrite("");
		assertEquals("#",
				as.getSerNoCharStart(), "SerNo characther start does not default as expected");

		as.setSerNoCharSOverwrite(" ");
		assertEquals("#",
				as.getSerNoCharStart(), "SerNo characther start does not default as expected");

		as.setSerNoCharSOverwrite("aa");
		assertEquals("a",
				as.getSerNoCharStart(),
				"SerNo characther start does truncate and pass expected value");

		as.setSerNoCharSOverwrite("b");
		assertEquals("b",
				as.getSerNoCharStart(), "SerNo characther start does not pass expected value");

	}

	@Test
	void testMAttributeSet_getSerNoCharEnd()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");

		assertEquals("", as.getSerNoCharEnd(), "SerNo characther end does not default as expected");

		as.setSerNoCharEOverwrite(null);
		assertEquals("", as.getSerNoCharEnd(), "SerNo characther end does not default as expected");

		as.setSerNoCharEOverwrite("");
		assertEquals("", as.getSerNoCharEnd(), "SerNo characther end does not default as expected");

		as.setSerNoCharEOverwrite(" ");
		assertEquals("", as.getSerNoCharEnd(), "SerNo characther end does not default as expected");

		as.setSerNoCharEOverwrite("zz");
		assertEquals("z",
				as.getSerNoCharEnd(), "SerNo characther end does truncate and pass expected value");

		as.setSerNoCharEOverwrite("y");
		assertEquals("y",
				as.getSerNoCharEnd(), "SerNo characther end does not pass expected value");

	}

	@Test
	void testMAttributeSet_beforeSave()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");
		as.saveEx();

		assertFalse(as.isInstanceAttribute());
		assertEquals(MAttributeSet.MANDATORYTYPE_NotMandatory, as.getMandatoryType());

		as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName2");
		as.setIsSerNo(true);
		as.setIsSerNoMandatory(true);
		as.saveEx();

		assertTrue(as.isInstanceAttribute());
		assertEquals(MAttributeSet.MANDATORYTYPE_AlwaysMandatory, as.getMandatoryType());

		as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName3");
		as.setIsLot(true);
		as.setIsLotMandatory(true);
		as.saveEx();

		assertTrue(as.isInstanceAttribute());
		assertTrue(MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(as.getMandatoryType()));

		as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName4");
		as.setIsGuaranteeDate(true);
		as.setIsGuaranteeDateMandatory(true);
		as.saveEx();

		assertTrue(as.isInstanceAttribute());
		assertTrue(MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(as.getMandatoryType()));

		as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName5");
		as.saveEx();

		as.setIsSerNo(true);
		as.setIsSerNoMandatory(true);
		assertFalse(as.isInstanceAttribute());
		assertFalse(MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(as.getMandatoryType()));

		as.saveEx();
		assertTrue(as.isInstanceAttribute());
		assertTrue(MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(as.getMandatoryType()));

		as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName6");
		as.saveEx();

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute1.get_ID());
		attributeUse.setM_AttributeSet_ID(as.get_ID());
		attributeUse.saveEx();

		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertTrue(MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(as.getMandatoryType()));

		as.setIsSerNo(true);
		as.saveEx();
		assertTrue(as.isInstanceAttribute());

		as.setIsSerNo(false);
		as.saveEx();
		assertFalse(as.isInstanceAttribute());

		attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute2.get_ID());
		attributeUse.setM_AttributeSet_ID(as.get_ID());
		attributeUse.saveEx();

		as.load(trxName);
		assertTrue(as.isInstanceAttribute());
		assertTrue(MAttributeSet.MANDATORYTYPE_AlwaysMandatory.equals(as.getMandatoryType()));

		as.setIsSerNo(true);
		as.saveEx();
		assertTrue(as.isInstanceAttribute());

		as.setIsSerNo(false);
		as.saveEx();
		assertTrue(as.isInstanceAttribute());

	}

	@Test
	void testMAttributeSet_updateMAttributeSets()
	{

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");
		as.saveEx();

		X_M_AttributeUse attributeUse = new X_M_AttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute1.get_ID());
		attributeUse.setM_AttributeSet_ID(as.get_ID());
		attributeUse.saveEx();

		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertFalse(as.isMandatory());

		attributeUse = new X_M_AttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute2.get_ID());
		attributeUse.setM_AttributeSet_ID(as.get_ID());
		attributeUse.saveEx();

		as.load(trxName);
		assertFalse(as.isInstanceAttribute());
		assertFalse(as.isMandatory());

		MAttributeSet.updateMAttributeSets(trxName);
		as.load(trxName);
		assertTrue(as.isInstanceAttribute());
		assertTrue(as.isMandatory());
		assertTrue(as.isMandatoryAlways());

	}

	@Test
	void testMAttributeSet_clearMAttributeCache()
	{

		assertEquals(2, attributeSetUnderTest.getMAttributes().length);

		MAttribute attribute3 = new MAttribute(ctx, 0, trxName);
		attribute3.setName("TestAttribute3");
		attribute3.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attribute3.setIsMandatory(true);
		attribute3.setIsInstanceAttribute(true);
		attribute3.saveEx();

		MAttributeUse attributeUse = new MAttributeUse(ctx, 0, trxName);
		attributeUse.setM_Attribute_ID(attribute3.get_ID());
		attributeUse.setM_AttributeSet_ID(attributeSetUnderTest.get_ID());
		attributeUse.saveEx();

		assertEquals(2, attributeSetUnderTest.getMAttributes().length);

		attributeSetUnderTest.clearMAttributeCache();

		assertEquals(3, attributeSetUnderTest.getMAttributes().length);

		attributeUse.deleteEx(true);
		assertEquals(3, attributeSetUnderTest.getMAttributes().length);
		attributeSetUnderTest.clearMAttributeCache();
		assertEquals(2, attributeSetUnderTest.getMAttributes().length);

	}
}
