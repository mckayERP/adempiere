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
package org.eevolution.model;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.PeriodClosedException;
import org.adempiere.test.CommonGWData;
import org.adempiere.test.CommonGWSetup;
import org.adempiere.test.CommonIntegrationTestUtilities;
import org.compiere.model.MDocType;
import org.compiere.model.MPeriod;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.PO;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("Model")
@Tag("MPPOrderRelated")
@Tag("MPPOrderBOMLineIssue")
class IT_MPPOrderBOMLineIssue extends CommonGWSetup {

	private final static int m_product_id = CommonGWData.PATIOSET_PRODUCT_ID;
	private final static int m_attributeSetInstance_id = 0;
	private final static int S_Resource_ID = CommonGWData.FURNITURE_PLANT_RESOURCE_ID;
	private final static int AD_Workflow_ID = CommonGWData.PATIOSET_WORKFLOW_ID;
	private final static int PP_Product_BOM_ID = CommonGWData.PATIOSET_PP_PRODUCT_BOM_ID;

	private final static int m_warehouse_id = CommonGWData.FURNITURE_WAREHOUSE_ID;
	private static int m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator()
			.get_ID();

	private static MPPOrder ppOrder = null;
    private CommonIntegrationTestUtilities utils = new CommonIntegrationTestUtilities();

	private void assertValuesOfCopyCommonFields(MPPOrderBOMLineIssue issue,
			MPPOrderBOMLineIssue copyOfIssue) {
	
		assertFalse(copyOfIssue.is_new(), "The copy should be saved and not new");
		assertTrue(copyOfIssue.getPP_Order_BOMLineIssue_ID() > 0, "The ID should be set");
		assertNotEquals(issue.getPP_Order_BOMLineIssue_ID(),
				copyOfIssue.getPP_Order_BOMLineIssue_ID(),
				"The ID should be different than the original line");
		assertNotNull(copyOfIssue.getDocumentNo());
	
		assertEquals(X_PP_Order_BOMLineIssue.DOCSTATUS_Drafted, copyOfIssue.getDocStatus());
		assertEquals(X_PP_Order_BOMLineIssue.DOCACTION_Complete, copyOfIssue.getDocAction());
		assertFalse(copyOfIssue.isApproved());
		assertFalse(copyOfIssue.isProcessed());
		assertFalse(copyOfIssue.isProcessing());
		assertEquals(today, copyOfIssue.getMovementDate());
		assertEquals(today, copyOfIssue.getDateDoc());
	
	}

	private void assertValuesOfCopyWithNoReversal(MPPOrderBOMLineIssue issue,
			MPPOrderBOMLineIssue copyOfIssue) {
	
		assertValuesOfCopyCommonFields(issue, copyOfIssue);
		assertNotEquals(issue.getDocumentNo(), copyOfIssue.getDocumentNo(),
				"The document number should be different");
		assertFalse(copyOfIssue.isReversal());
		assertEquals(0, copyOfIssue.getReversal_ID());
	
	}

	private void assertValuesOfCopyWithReversal(MPPOrderBOMLineIssue issue,
			MPPOrderBOMLineIssue copyOfIssue, boolean copyDocNoOnReversal) {
	
		assertValuesOfCopyCommonFields(issue, copyOfIssue);
	
		assertNotEquals(issue.getDocumentNo(), copyOfIssue.getDocumentNo(),
				"The document number should be different");
		assertTrue(copyOfIssue.isReversal());
		assertEquals(issue.getPP_Order_BOMLineIssue_ID(), copyOfIssue.getReversal_ID());
		if (copyDocNoOnReversal)
			assertEquals(issue.getDocumentNo() + "^", copyOfIssue.getDocumentNo());
		else
			assertNotEquals(issue.getDocumentNo(), copyOfIssue.getDocumentNo());
	
	
	}

	@BeforeAll
	static void localSetUpBeforeClass() {

		Timestamp now = new Timestamp(System.currentTimeMillis());

		ppOrder = new MPPOrder(ctx, 0, trxName);
		ppOrder.setIsSOTrx(false);
		ppOrder.setC_DocTypeTarget_ID(MDocType.DOCBASETYPE_ManufacturingOrder);
		ppOrder.setM_Warehouse_ID(m_warehouse_id);
		ppOrder.setM_Locator_ID(m_locator_id);
		ppOrder.setM_Product_ID(m_product_id);
		ppOrder.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		ppOrder.setS_Resource_ID(S_Resource_ID);
		ppOrder.setAD_Workflow_ID(AD_Workflow_ID);
		ppOrder.setPP_Product_BOM_ID(PP_Product_BOM_ID);
		ppOrder.setPriorityRule(MPPOrder.PRIORITYRULE_High);
		ppOrder.setDateOrdered(now);
		ppOrder.setDatePromised(now);
		ppOrder.setDateStartSchedule(now);
		ppOrder.setQty(Env.ONE);
		ppOrder.saveEx();

	}

	@Test
	final void constructor_contextIdTrxName_parentBOMLineNotSet() {

		MPPOrderBOMLineIssue ppOrderBOMLineIssue = new MPPOrderBOMLineIssue(ctx, 0, trxName);

		assertTrue(ppOrderBOMLineIssue instanceof MPPOrderBOMLineIssue);
		assertEquals(0, ppOrderBOMLineIssue.get_ID(), "ID of new record should be zero");
		assertEquals(0, ppOrderBOMLineIssue.getPP_Order_BOMLine_ID(),
				"PP_Order_BOMLine_ID should be zero");

		assertThrows(AdempiereException.class, ppOrderBOMLineIssue::saveEx,
				"Expected an exception with the parent BOM Line is null");

	}

	@Test
	final void constructor_ContextIdTrxName_productIDIsNull() {

		MPPOrderBOMLineIssue ppOrderBOMLineIssue = new MPPOrderBOMLineIssue(ctx, 0, trxName);
		ppOrderBOMLineIssue.setPP_Order_BOMLine_ID(ppOrder.getLines()[0].get_ID());

		assertThrows(AdempiereException.class, ppOrderBOMLineIssue::saveEx,
				"M_Product_ID should be mandatory");

	}

	@Test
	final void constructor_ContextIdTrxName() {

		MPPOrderBOMLineIssue ppOrderBOMLineIssue = new MPPOrderBOMLineIssue(ctx, 0, trxName);
		ppOrderBOMLineIssue.setPP_Order_BOMLine_ID(ppOrder.getLines()[0].get_ID());
		ppOrderBOMLineIssue.setPP_Order_BOMLine_ID(ppOrder.getLines()[0].get_ID());
		ppOrderBOMLineIssue.setM_Product_ID(ppOrder.getLines()[0].getM_Product_ID());
		ppOrderBOMLineIssue.saveEx();

		int id = ppOrderBOMLineIssue.get_ID();
		assertTrue(id > 0, "ID of saved record should be > zero");

		ppOrderBOMLineIssue = new MPPOrderBOMLineIssue(ctx, id, trxName);
		assertEquals(id, ppOrderBOMLineIssue.get_ID(), "ID of saved record should match");

	}

	@Test
	final void constructor_ContextResultSetTrxName() {

		boolean tested = false;

		MPPOrderBOMLineIssue ppOrderBOMLineIssue = new MPPOrderBOMLineIssue(ctx, 0, trxName);
		ppOrderBOMLineIssue.setPP_Order_BOMLine_ID(ppOrder.getLines()[0].get_ID());
		ppOrderBOMLineIssue.setM_Product_ID(ppOrder.getLines()[0].getM_Product_ID());
		ppOrderBOMLineIssue.saveEx();

		int id = ppOrderBOMLineIssue.get_ID();

		String sql = "SELECT * FROM " + MPPOrderBOMLineIssue.Table_Name
				+ " WHERE " + MPPOrderBOMLineIssue.COLUMNNAME_PP_Order_BOMLineIssue_ID + "=?";
		PreparedStatement pstmt = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, id);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ctx, rs, trxName);
				assertEquals(ppOrderBOMLineIssue, issue, "MPPOrderBOMLineIssue doesn't match");
				tested = true;
				break;
			}
			rs.close();
			pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			fail(e.getMessage());
		}
		try {
			if (pstmt != null)
				pstmt.close();
			pstmt = null;
		} catch (Exception e) {
			fail(e.getMessage());
			pstmt = null;
		}

		assertTrue(tested, "Did not find the saved id");

	}

	@Test
	final void constructor_ContextBOMLineTrxName() {

		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ctx, ppOrder.getLines()[0], trxName);

		assertEquals(ctx, issue.getCtx(), "Context should match");
		assertEquals(trxName, issue.get_TrxName(), "Transaction name should match");
		assertEquals((PO) ppOrder.getLines()[0], issue.getParent(), "Parent should match");
		assertEquals(ppOrder.getLines()[0].getM_Product_ID(), issue.getM_Product_ID(),
				"Product should match");
		assertEquals(ppOrder.getLines()[0].getM_AttributeSetInstance_ID(),
				issue.getM_AttributeSetInstance_ID(), "AttributeSetInstance should match");
		assertEquals(ppOrder.getLines()[0].getM_Locator_ID(), issue.getM_Locator_ID(),
				"Locator should match");

	}

	@Test
	final void constructor_BOMLine() {

		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ppOrder.getLines()[0]);

		assertEquals(ctx, issue.getCtx(), "Context should match");
		assertEquals(trxName, issue.get_TrxName(), "Transaction name should match");

	}

	@Test
	final void constructor_BOMLine_checkContextAndTrxName() {

		MPPOrderBOMLine line = ppOrder.getLines()[0];
		line.set_TrxName("someString");

		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(line);

		assertEquals(ctx, issue.getCtx(), "Context should match");
		assertEquals("someString", issue.get_TrxName(), "Transaction name should match");

	}

	@Test
	void getDocType() {

		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ppOrder.getLines()[0]);
		MDocType docType = issue.getDocType();
		assertNotNull(docType);

	}

	@Test
	void getParent() {

		MPPOrderBOM bom = ppOrder.getMPPOrderBOM();
		MPPOrderBOMLine expectedLine = bom.getLines()[0];
		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ppOrder.getLines()[0]);
		MPPOrderBOMLine line = (MPPOrderBOMLine) issue.getParent();

		assertEquals(expectedLine, line, "getParent did not return the BOM Line expected");

	}

	@Test
	void copyFromWithNoReversal() {

		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ctx, ppOrder.getLines()[0], trxName);
		issue.saveEx();

		boolean noReversal = false;
		MPPOrderBOMLineIssue copyOfIssue = MPPOrderBOMLineIssue.copyFrom(issue, today, today,
				noReversal,
				trxName);

		assertValuesOfCopyWithNoReversal(issue, copyOfIssue);

	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1})
	void copyFromWithReversal(int copyDocNo) {

		boolean copyDocNoOnReversal = copyDocNo == 1;
		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ctx, ppOrder.getLines()[0], trxName);
		issue.saveEx();

		MDocType docType = MDocType.get(ctx, issue.getC_DocType_ID(), trxName);
		docType.setIsCopyDocNoOnReversal(copyDocNoOnReversal);
		boolean reversal = true;
		MPPOrderBOMLineIssue copyOfIssue = issue.copyFrom(today, today, reversal);
		
		assertValuesOfCopyWithReversal(issue, copyOfIssue, copyDocNoOnReversal);

	}

	@Test
	void copyFromWithReversal_newDocNo() {

		boolean newDocNo = false;
		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ctx, ppOrder.getLines()[0], trxName);
		issue.saveEx();

		MDocType docType = MDocType.get(ctx, issue.getC_DocType_ID(), trxName);
		docType.setIsCopyDocNoOnReversal(newDocNo);
		boolean reversal = true;
		MPPOrderBOMLineIssue copyOfIssue = MPPOrderBOMLineIssue.copyFrom(issue, today, today,
				reversal,
				trxName);

		assertValuesOfCopyWithReversal(issue, copyOfIssue, newDocNo);

	}
	
	@Test
	void getDocumentEngine() {
		
		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ctx, ppOrder.getLines()[0], trxName);
		
		DocumentEngine engine = issue.getDocumentEngine();
		assertNotNull(engine, "DocumentEngine not found");

	}

	@Test
	void getModelValidationEngine() {
		
		MPPOrderBOMLineIssue issue = new MPPOrderBOMLineIssue(ctx, ppOrder.getLines()[0], trxName);
		
		ModelValidationEngine engine = issue.getModelValidationEngine();
		assertNotNull(engine, "ModelValidationEngine not found");

		ModelValidationEngine fromCacheThisTime = issue.getModelValidationEngine();
		assertNotNull(fromCacheThisTime, "ModelValidationEngine not found");

	}

	@Test
	@Disabled("TODO: Add the translations")
	void checkTranslationsExist() {
	
		String translation = Msg.parseTranslation(ctx, MPPOrderBOMLineIssue.PROCESS_MSG_ISSUE_CANNOT_BE_REACTIVATED);
		assertNotEquals("MPPOrderBOMLineIssue_IssueCannotBeReactivated", translation, "Translation not added. Check migrations");
		
		translation = Msg.parseTranslation(ctx, MPPOrderBOMLineIssue.PROCESS_MSG_PARENT_NOT_IN_PROGRESS);
		assertNotEquals("MPPOrderBOMLineIssue_ParentOrderNotInProgress", translation, "Translation not added. Check migrations");

	}

	@Nested
	@DisplayName("Given auto period control is off")
	class givenAutoPeriodControlIsOff {
	    
	    MPPOrderBOMLineIssue issue;
	    
	    @BeforeEach
	    void turnOffAutoPeriodControlAndCreateBOMLineIssue() {
	        
	           utils.turnOffAutoPeriodControl(ctx, AD_CLIENT_ID);
	            issue = new MPPOrderBOMLineIssue(ctx, ppOrder.getLines()[0], trxName);
	            issue.saveEx();

	    }
	    
    	@Test
    	void testPeriodClosed() {
    
    		int C_Period_ID=136;
    		
    		MPeriod period = new MPeriod(ctx, C_Period_ID, trxName);
    		Timestamp startDate = period.getStartDate();
            utils.closePeriod(ctx, AD_ORG_ID, startDate, "MOI", trxName);
    		    		
    		assertThrows(PeriodClosedException.class, () -> {
    			issue.testPeriodOpen(startDate);
    		}, "Should throw exception when period is closed");
    		
    	}
    
    	@Test
    	void testPeriodOpen() {
    
    		int C_Period_ID=136;
    		MPeriod period = new MPeriod(ctx, C_Period_ID, trxName);
    		Timestamp startDate = period.getStartDate();
    		utils.openPeriod(ctx, AD_ORG_ID, startDate, "MOI", trxName);
    		
    		issue.testPeriodOpen(startDate);
    		
    	}
	}
}
