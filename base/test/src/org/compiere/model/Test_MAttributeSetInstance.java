package org.compiere.model;

import static org.adempiere.test.TestUtilities.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonGWSetup;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.KeyNamePair;
import org.compiere.util.TimeUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class Test_MAttributeSetInstance extends CommonGWSetup {

	private static MAttributeSetInstance attributeSetInstanceUnderTest = null;
	private static MAttributeSet attributeSet = null;
	private static MAttribute attributeString = null;
	private static MAttribute attributeNumber = null;
	private static MAttribute attributeList = null;
	private static MAttributeValue attributeValue1 = null;
	private static MAttributeValue attributeValue2 = null;
	private static MProduct product = null;
	private static int m_product_id = 0;
	
	@BeforeAll
static void localSetUpBeforeClass() {

		attributeString = new MAttribute(ctx, 0, trxName);
		attributeString.setName("TestAttribute1");
		attributeString.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_StringMax40);
		attributeString.setIsMandatory(false);
		attributeString.setIsInstanceAttribute(false);
		attributeString.saveEx();

		attributeNumber = new MAttribute(ctx, 0, trxName);
		attributeNumber.setName("TestAttribute2");
		attributeNumber.setAttributeValueType(MAttribute.ATTRIBUTEVALUETYPE_Number);
		attributeNumber.setIsMandatory(false);
		attributeNumber.setIsInstanceAttribute(false);
		attributeNumber.saveEx();

		attributeList = new MAttribute(ctx, 0, trxName);
		attributeList.setName("TestAttribute2");
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
		attributeValue2.setName("List attribute item 1");
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
		
		attributeSetInstanceUnderTest = new MAttributeSetInstance(ctx,0,trxName);
		attributeSetInstanceUnderTest.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		attributeSetInstanceUnderTest.saveEx();

		MAttributeInstance attributeInstance = new MAttributeInstance(ctx, 
				attributeString.getM_Attribute_ID(), 
				attributeSetInstanceUnderTest.getM_AttributeSetInstance_ID(),
				"stringAttributeValue",
				trxName);
		attributeInstance.saveEx();
		
		attributeInstance = new MAttributeInstance(ctx, 
				attributeNumber.getM_Attribute_ID(), 
				attributeSetInstanceUnderTest.getM_AttributeSetInstance_ID(),
				Env.ONE,
				trxName);
		attributeInstance.saveEx();

		attributeInstance = new MAttributeInstance(ctx, 
				attributeList.getM_Attribute_ID(), 
				attributeSetInstanceUnderTest.getM_AttributeSetInstance_ID(),
				attributeValue1.getM_AttributeValue_ID(),
				attributeValue1.getValue(),
				trxName);
		attributeInstance.saveEx();
		
		product = new MProduct(ctx, 0, trxName);
		product.setName("ProductUnderTest_" + randomString(4));
		product.setC_UOM_ID(MUOM.getDefault_UOM_ID(ctx));
		product.setM_Product_Category_ID(105);  // GW Standard
		product.setC_TaxCategory_ID(107);  // GW Standard
		product.setIsPurchased(true);
		product.setIsSold(true);
		product.setIsStocked(true);
		product.saveEx();
		m_product_id = product.getM_Product_ID();

	}
	
	@Test
void testMAttributeSetInstance_beforeSave() {
		
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx,0,trxName);
		assertThrows(AdempiereException.class, asi::saveEx);
		
		asi.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		asi.saveEx();
		
	}
	
	@Test
void testMAttributeSetInstance_ToString() {
		
		String expected = "ASI=" + attributeSetInstanceUnderTest.getM_AttributeSetInstance_ID() + " AS=" + attributeSetInstanceUnderTest.getM_AttributeSet_ID();
		assertEquals(expected, attributeSetInstanceUnderTest.toString(), "toString not returning expected info");
		
	}

	@Test
void testMAttributeSetInstance_getDescription() {

		MAttributeSetInstance asi = new MAttributeSetInstance(ctx,0,trxName);
		asi.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		asi.saveEx();
		
		String defaultDescription = Integer.valueOf(asi.get_ID()).toString();
		assertEquals(defaultDescription, asi.getDescription());

		asi = new MAttributeSetInstance(ctx,0,trxName);
		asi.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		asi.setDescription("testDescription");
		asi.saveEx();
		
		assertEquals("testDescription", asi.getDescription());

		asi = new MAttributeSetInstance(ctx,0,trxName);
		asi.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		asi.setDescription("");
		asi.saveEx();
		
		defaultDescription = Integer.valueOf(asi.get_ID()).toString();
		assertEquals(defaultDescription, asi.getDescription());

	}

	@Test
void testMAttributeSetInstance_GetPropertiesIntIntString() {
		
		assertNull(MAttributeSetInstance.get(ctx, 0, 0, trxName));

		assertNull(MAttributeSetInstance.get(ctx, 0, -1, trxName));

		assertNull(MAttributeSetInstance.get(ctx, 0, 9999999, trxName));

		MAttributeSetInstance newInstance = MAttributeSetInstance.get(ctx, attributeSetInstanceUnderTest.get_ID(),
				0, trxName);
		assertNotNull(newInstance);
		assertEquals(attributeSetInstanceUnderTest.get_ID(), newInstance.get_ID());
		
		product.setM_AttributeSet_ID(0);
		product.saveEx();

		newInstance = MAttributeSetInstance.get(ctx, 0, m_product_id, trxName);
		assertNull(newInstance);
		
		product.setM_AttributeSet_ID(attributeSet.get_ID());
		product.saveEx();

		newInstance = MAttributeSetInstance.get(ctx, 0, m_product_id, trxName);
		assertNotNull(newInstance);
		assertEquals(0, newInstance.get_ID());
		assertEquals(product.getM_AttributeSet_ID(), newInstance.getM_AttributeSet_ID());

	}

	@Test
void testMAttributeSetInstance_constructor_PropertiesIntString() {
		
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		
		assertTrue(asi instanceof MAttributeSetInstance);
		assertEquals(0, asi.get_ID(), "ID of new record should be zero");
		assertEquals(0, asi.getM_AttributeSet_ID(), "M_AttributeSet_ID should be zero");
		// Missing M_AttributeSet_ID
		assertThrows("AttributeSet should be mandatory", AdempiereException.class, asi::saveEx);

		asi.setM_AttributeSet_ID(attributeSet.get_ID());
		asi.saveEx();
		int id = asi.get_ID();
		assertTrue(id > 0, "ID of saved record should be > zero");
		
		asi = new MAttributeSetInstance(ctx, id, trxName);

		assertEquals(id, asi.get_ID(), "ID of saved record should match");

	}

	@Test
void testMAttributeSetInstance_constructor_PropertiesResultSetString() {
		
		boolean tested = false;
		String sql = "SELECT * FROM M_AttributeSetInstance "
				+ "WHERE AD_Client_ID=?";
		PreparedStatement pstmt = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, AD_CLIENT_ID);
			ResultSet rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MAttributeSetInstance asi = new MAttributeSetInstance(ctx, rs, trxName);
				assertNotNull(asi, "Constructor returns null");		
				assertEquals(rs.getInt(MAttributeSetInstance.COLUMNNAME_M_AttributeSetInstance_ID), asi.get_ID(), "MAttributeSet id doesn't match record set.");
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

	@Test
void testMAttributeSetInstance_constructor_PropertiesIntIntString() {
		
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, 0, trxName);
		
		assertNotNull(asi);
		assertTrue(asi instanceof MAttributeSetInstance);
		assertEquals(0, asi.getM_AttributeSetInstance_ID());
		assertEquals(0, asi.getM_AttributeSet_ID());
		
		asi = new MAttributeSetInstance(ctx, 0, attributeSet.get_ID(), trxName);
		
		assertNotNull(asi);
		assertEquals(0, asi.getM_AttributeSetInstance_ID());
		assertEquals(attributeSet.get_ID(), asi.getM_AttributeSet_ID());
		
		asi = new MAttributeSetInstance(ctx, attributeSetInstanceUnderTest.get_ID(), attributeSet.get_ID(), trxName);
		
		assertNotNull(asi);
		assertEquals(attributeSetInstanceUnderTest.get_ID(), asi.getM_AttributeSetInstance_ID());
		assertEquals(attributeSetInstanceUnderTest.getM_AttributeSet_ID(), asi.getM_AttributeSet_ID());

		asi = new MAttributeSetInstance(ctx, attributeSetInstanceUnderTest.get_ID(), 0, trxName);
		
		assertNotNull(asi);
		assertEquals(attributeSetInstanceUnderTest.get_ID(), asi.getM_AttributeSetInstance_ID());
		assertEquals(attributeSetInstanceUnderTest.getM_AttributeSet_ID(), asi.getM_AttributeSet_ID());

		asi = new MAttributeSetInstance(ctx, attributeSetInstanceUnderTest.get_ID(), 999999, trxName);
		
		assertNotNull(asi);
		assertEquals(attributeSetInstanceUnderTest.get_ID(), asi.getM_AttributeSetInstance_ID());
		assertEquals(attributeSetInstanceUnderTest.getM_AttributeSet_ID(), asi.getM_AttributeSet_ID());

	}

	@Test
void testMAttributeSetInstance_SetMAttributeSet() {
		
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		
		asi.setMAttributeSet(attributeSet);
		assertEquals(attributeSet.getM_AttributeSet_ID(), asi.getM_AttributeSet_ID());
		assertEquals(attributeSet, asi.getM_AttributeSet());
		
		asi.setMAttributeSet(null);
		assertEquals(0, asi.getM_AttributeSet_ID());

		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		asi.setMAttributeSet(as);
		assertEquals(0, asi.getM_AttributeSet_ID());
		
	}

	@Test
void testMAttributeSetInstance_GetMAttributeSet() {
		
		MAttributeSetInstance asi = new MAttributeSetInstance(ctx, 0, trxName);
		
		assertNotNull(asi.getM_AttributeSet());
		assertEquals(0, asi.getM_AttributeSet().getM_AttributeSet_ID());
		
		MAttributeSet as = attributeSetInstanceUnderTest.getMAttributeSet();
		assertEquals(attributeSet.getM_AttributeSet_ID(), as.getM_AttributeSet_ID());
		
	}

	@Test
void testMAttributeSetInstance_SetDescription() {
		
		attributeSetInstanceUnderTest.setDescription("test");
		assertEquals("test", attributeSetInstanceUnderTest.getDescription());
		
		attributeSetInstanceUnderTest.setDescription();
		assertNotNull(attributeSetInstanceUnderTest.getDescription());
		assertNotEquals("test", attributeSetInstanceUnderTest.getDescription());
		
	}

	@Test
void testMAttributeSetInstance_GetGuaranteeDateBoolean() {

		assertNull(attributeSetInstanceUnderTest.getGuaranteeDate());
		assertNull(attributeSetInstanceUnderTest.getGuaranteeDate(false));

		Timestamp date = TimeUtil.getDay(System.currentTimeMillis());
		
		attributeSetInstanceUnderTest.setGuaranteeDate(date);
		assertNotNull(attributeSetInstanceUnderTest.getGuaranteeDate(false));
		assertEquals(date, attributeSetInstanceUnderTest.getGuaranteeDate(false));
		
		attributeSet.setGuaranteeDays(1);
		attributeSet.saveEx();
		attributeSetInstanceUnderTest.setMAttributeSet(attributeSet);
		
		Timestamp newDate = attributeSetInstanceUnderTest.getGuaranteeDate(true);
		assertNotNull(newDate);
		assertTrue(TimeUtil.getTimeBetween(date, newDate, TimeUtil.DURATIONUNIT_Day)==1);
		
	}

	@Test
void testMAttributeSetInstance_GetLotBooleanInt() {
		
		attributeSet.setM_LotCtl_ID(0);
		product.setM_AttributeSet_ID(0);
		product.saveEx();
		
		assertNull(attributeSetInstanceUnderTest.getLot(false, 0));
		assertNull(attributeSetInstanceUnderTest.getLot(false, m_product_id));

		MLotCtl ctl = new MLotCtl(ctx, 0, trxName);
		ctl.setName("testLotCtl");
		ctl.saveEx();
		
		attributeSet.setM_LotCtl_ID(ctl.getM_LotCtl_ID());
		
		assertThrows(AdempiereException.class, () -> {attributeSetInstanceUnderTest.getLot(true, 0);});

		product.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		product.saveEx();

		assertNotNull(attributeSetInstanceUnderTest.getLot(true, m_product_id));
		assertNotEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());
		assertNotNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(m_product_id, attributeSetInstanceUnderTest.getM_Lot().getM_Product_ID());
		
	}

	@Test
void testMAttributeSetInstance_CreateLot() {
		
		attributeSet.setM_LotCtl_ID(0);
		attributeSetInstanceUnderTest.setM_Lot_ID(0);
		attributeSetInstanceUnderTest.setLot(null);
		product.setM_AttributeSet_ID(0);
		product.saveEx();

		assertNull(attributeSetInstanceUnderTest.createLot(0));
		assertNull(attributeSetInstanceUnderTest.createLot(m_product_id));
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());
		assertNull(attributeSetInstanceUnderTest.getLot());
		
		MLotCtl ctl = new MLotCtl(ctx, 0, trxName);
		ctl.setName("testLotCtl");
		ctl.saveEx();
		
		attributeSet.setM_LotCtl_ID(ctl.getM_LotCtl_ID());
		
		assertThrows(AdempiereException.class, () -> {attributeSetInstanceUnderTest.createLot(0);});
		assertThrows(AdempiereException.class, () -> {attributeSetInstanceUnderTest.createLot(m_product_id);});
		
		product.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		product.saveEx();
		
		KeyNamePair newLot = attributeSetInstanceUnderTest.createLot(m_product_id);
		assertNotNull(newLot);
		assertTrue(attributeSetInstanceUnderTest.getM_Lot_ID() > 0);
		assertNotNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(attributeSetInstanceUnderTest.getM_Lot_ID(), newLot.getKey());
		assertEquals(attributeSetInstanceUnderTest.getLot(), newLot.getName());
		assertEquals("1", attributeSetInstanceUnderTest.getLot());
		assertNotNull(attributeSetInstanceUnderTest.getM_Lot());
		assertEquals(m_product_id, attributeSetInstanceUnderTest.getM_Lot().getM_Product_ID());
		
	}

	@Test
void testMAttributeSetInstance_SetLotStringInt() {
		
		attributeSet.setM_LotCtl_ID(0);
		attributeSetInstanceUnderTest.setM_Lot_ID(0);
		attributeSetInstanceUnderTest.setLot(null);
		
		attributeSetInstanceUnderTest.setLot(null, 0);
		assertNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());
		
		attributeSetInstanceUnderTest.setLot(null, -1);
		assertNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());

		attributeSetInstanceUnderTest.setLot("", 0);
		assertEquals("", attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());
		
		attributeSetInstanceUnderTest.setLot("", -1);
		assertEquals("", attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());

		String testLotName = "testLot";
		attributeSetInstanceUnderTest.setLot(testLotName, 0);
		assertNotNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(testLotName, attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());
		
		attributeSetInstanceUnderTest.setLot(testLotName, -1);
		assertNotNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(testLotName, attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());
		
		attributeSetInstanceUnderTest.setLot("", m_product_id);
		assertEquals("", attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());

		attributeSetInstanceUnderTest.setLot(testLotName, m_product_id);
		assertNotNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(testLotName, attributeSetInstanceUnderTest.getLot());
		assertEquals(0, attributeSetInstanceUnderTest.getM_Lot_ID());
		
		product.setM_AttributeSet_ID(attributeSet.getM_AttributeSet_ID());
		product.saveEx();
		
		MLotCtl ctl = new MLotCtl(ctx, 0, trxName);
		ctl.setName("testLotCtl");
		ctl.saveEx();
		
		attributeSet.setM_LotCtl_ID(ctl.getM_LotCtl_ID());
		
		MLot mLot = new MLot(ctx, 0, trxName);
		mLot.setM_Product_ID(m_product_id);
		mLot.setName(testLotName);
		mLot.saveEx();

		attributeSetInstanceUnderTest.setLot(testLotName, m_product_id);
		assertNotNull(attributeSetInstanceUnderTest.getLot());
		assertEquals(testLotName, attributeSetInstanceUnderTest.getLot());
		assertEquals(mLot.getM_Lot_ID(), attributeSetInstanceUnderTest.getM_Lot_ID());
		
	}

	@Test
void testMAttributeSetInstance_IsExcludeLot() {
		
		attributeSetInstanceUnderTest.setMAttributeSet(null);
		assertFalse(attributeSetInstanceUnderTest.isExcludeLot(0, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeLot(0, false));

		attributeSetInstanceUnderTest.setMAttributeSet(attributeSet);
		attributeSet.setM_LotCtl_ID(0);
		assertFalse(attributeSetInstanceUnderTest.isExcludeLot(0, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeLot(0, false));

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
		as.saveEx();

		attributeSetInstanceUnderTest.setMAttributeSet(as);

		int columnID = MColumn.getColumn_ID(MInOutLine.Table_Name, MInOutLine.COLUMNNAME_M_AttributeSetInstance_ID);
		int columnID2 = MColumn.getColumn_ID(MOrderLine.Table_Name, MOrderLine.COLUMNNAME_M_AttributeSetInstance_ID);

		assertTrue(attributeSetInstanceUnderTest.isExcludeLot(columnID, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeLot(columnID, false));
		assertFalse(attributeSetInstanceUnderTest.isExcludeLot(columnID2, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeLot(columnID2, false));

	}

	@Test
void testMAttributeSetInstance_GetSerNoBoolean() {
		
		attributeSetInstanceUnderTest.setSerNo(null);
		attributeSetInstanceUnderTest.setMAttributeSet(attributeSet);
		attributeSet.setM_SerNoCtl_ID(0);
		product.setM_AttributeSet_ID(0);
		product.saveEx();
		
		assertNull(attributeSetInstanceUnderTest.getSerNo(false));

		MSerNoCtl ctl = new MSerNoCtl(ctx, 0, trxName);
		ctl.setName("testSerNoCtl");
		ctl.saveEx();
		
		attributeSet.setM_SerNoCtl_ID(ctl.getM_SerNoCtl_ID());
		
		String serNo = attributeSetInstanceUnderTest.getSerNo(true);
		assertNotNull(serNo);
		assertNotNull(attributeSetInstanceUnderTest.getSerNo());		
		assertEquals(serNo, attributeSetInstanceUnderTest.getSerNo());
		assertEquals("1", serNo);
		
	}

	@Test
void testMAttributeSetInstance_IsExcludeSerNo() {
		
		attributeSetInstanceUnderTest.setMAttributeSet(null);
		assertFalse(attributeSetInstanceUnderTest.isExcludeSerNo(0, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeSerNo(0, false));

		attributeSetInstanceUnderTest.setMAttributeSet(attributeSet);
		attributeSet.setM_SerNoCtl_ID(0);
		assertFalse(attributeSetInstanceUnderTest.isExcludeSerNo(0, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeSerNo(0, false));

		MSerNoCtl serNoCtl = new MSerNoCtl(ctx, 0, trxName);
		serNoCtl.setName("temp");
		serNoCtl.saveEx();

		X_M_SerNoCtlExclude ase = new X_M_SerNoCtlExclude(ctx, 0, trxName);
		ase.setM_SerNoCtl_ID(serNoCtl.getM_SerNoCtl_ID());
		ase.setAD_Table_ID(MInOutLine.Table_ID);
		ase.setIsSOTrx(true);
		ase.saveEx();
		
		MAttributeSet as = new MAttributeSet(ctx, 0, trxName);
		as.setName("testName");
		as.setIsLot(true);
		as.setM_SerNoCtl_ID(serNoCtl.getM_SerNoCtl_ID());
		as.saveEx();

		attributeSetInstanceUnderTest.setMAttributeSet(as);

		int columnID = MColumn.getColumn_ID(MInOutLine.Table_Name, MInOutLine.COLUMNNAME_M_AttributeSetInstance_ID);
		int columnID2 = MColumn.getColumn_ID(MOrderLine.Table_Name, MOrderLine.COLUMNNAME_M_AttributeSetInstance_ID);

		assertTrue(attributeSetInstanceUnderTest.isExcludeSerNo(columnID, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeSerNo(columnID, false));
		assertFalse(attributeSetInstanceUnderTest.isExcludeSerNo(columnID2, true));
		assertFalse(attributeSetInstanceUnderTest.isExcludeSerNo(columnID2, false));
		
	}

	@Test
void testMAttributeSetInstance_Create() {
		
		assertThrows(NullPointerException.class, () -> {MAttributeSetInstance.create(ctx, null, trxName);});
		
		product.setM_AttributeSet_ID(0);
		assertThrows(IllegalArgumentException.class, () -> {MAttributeSetInstance.create(ctx, product, trxName);});

		MSerNoCtl serNoCtl = new MSerNoCtl(ctx, 0, trxName);
		serNoCtl.setName("temp");
		serNoCtl.saveEx();

		MLotCtl ctl = new MLotCtl(ctx, 0, trxName);
		ctl.setName("testLotCtl");
		ctl.saveEx();
		
		attributeSet.setM_SerNoCtl_ID(serNoCtl.getM_SerNoCtl_ID());
		attributeSet.setM_LotCtl_ID(ctl.getM_LotCtl_ID());
		attributeSet.setGuaranteeDays(1);
		attributeSet.saveEx();

		product.setM_AttributeSet_ID(attributeSet.get_ID());
		product.saveEx();
		
		MAttributeSetInstance asi = MAttributeSetInstance.create(ctx, product, trxName);
		assertNotNull(asi);
		assertTrue(asi.getM_AttributeSetInstance_ID() > 0);
		assertEquals(product.getM_AttributeSet_ID(), asi.getM_AttributeSet_ID());
		assertEquals("1", asi.getLot());
		assertEquals("1", asi.getSerNo());

		Timestamp date = TimeUtil.getDay(System.currentTimeMillis());
		Timestamp newDate = asi.getGuaranteeDate();
		assertTrue(TimeUtil.getTimeBetween(date, newDate, TimeUtil.DURATIONUNIT_Day)==1);
		
	}

}
