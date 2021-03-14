/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
package org.compiere.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageEngine;
import org.adempiere.engine.storage.StorageUtil;
import org.adempiere.engine.storage.rules.StorageEngineRuleAbstract;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClient;
import org.compiere.model.MColumn;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MLocator;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductionLine;
import org.compiere.model.MProjectIssue;
import org.compiere.model.MRefList;
import org.compiere.model.MStorage;
import org.compiere.model.MTable;
import org.compiere.model.MTransaction;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MPPCostCollector;

/**
 * 	StorageCleanup
 *	
 *  @author Jorg Janke
 *  @version $Id: StorageCleanup.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class StorageCleanup extends StorageCleanupAbstract
{
	
	private boolean rebuildRequired = false;
	
	/**
	 * 	Process
	 *	@return message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		if(this.isOnlyReport())
		{
			addLog("Only reporting results.  No changes will be made to the data."); // TODO Translate
			if (reportProblems())
			{
				if (rebuildRequired)
					return "Issues found. Storage rebuild recommended. See log for details.";
				else
					return "Issues found. See log for details.";
			}
			return "Storage checked. No issues found.";
		}
		
		if (this.isCorrectBadASIValues())
			removeMalformedASIValues();
		
		if (this.isRebuildStorage())
			rebuildStorage();
		
		if (this.isCorrectMaterialAllocations())
			ensureDocumentLinesHaveMaterialAllocations();
		
		if (this.isCoverNegativeQty())
		{
			coverNegativeQuantities();
			removeEmptyStorage();
		}
		
		return "Storage Cleanup completed.";
	}	//	doIt

	private void ensureDocumentLinesHaveMaterialAllocations() {
				
		// TODO create a list of InventoryAllocation tables
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(MInOutLine.Table_Name);
		tableNames.add(MInventoryLine.Table_Name);
		tableNames.add(MMovementLine.Table_Name);
		tableNames.add(MProductionLine.Table_Name);
		tableNames.add(MProjectIssue.Table_Name);  
		tableNames.add(MPPCostCollector.Table_Name);
		
		// Ensure every document line has at least one material policy ticket associated with it.
		String whereTemplate  = "COALESCE(tableName.M_MPolicyTicket_ID,0) = 0"
				+ " AND NOT EXISTS (SELECT * FROM tableNameMA WHERE tableNameMA.tableName_ID=tableName.tableName_ID)";

		int no = 0;
		int total = 0;
		
		for (String tableName : tableNames)
		{
			String errMsg = tableName + ": Added Material Allocations to document lines: ";
			String noErrMsg = tableName + ": No changes required.";

			String where = whereTemplate.replaceAll("tableName", tableName);
			
			List<MInOutLine> lines = new Query(getCtx(), tableName, where, get_TrxName())
											.setClient_ID()
											.list();
			for (IDocumentLine line : lines)
			{
				
				// Find the associated transactions
				List<MTransaction> trxList = MTransaction.getByDocumentLine(line);
				if (trxList.size() == 0)
					continue;  // No transaction = DocStatus draft, product not stocked
				
				no += 1;
				
				if (trxList.size() == 1 && MTransaction.isIncomingTransaction(line.getMovementType()))
				{
					line.setM_MPolicyTicket_ID(trxList.get(0).getM_MPolicyTicket_ID());
					((PO) line).saveEx();
				}
//				else
//				{
//					for (MTransaction trx : trxList)
//					{
//						StorageEngineRuleAbstract.createMA(line, 
//								trx.getM_MPolicyTicket_ID(), 
//								trx.getMovementType(), 
//								trx.getMovementQty(),
//								false);
//					}
//				}
			}
//			logResult(errMsg, noErrMsg, no, false);
//			total += no;
			
		}
//		String errMsg = "Added Material Allocations to document lines: ";
//		String noErrMsg = "No changes to document lines required to correct Material Allocations.";
//		logResult(errMsg, noErrMsg, total, true);

	}

	/**
	 * Add the result to the process log
	 * @param errMsg the message to use if parameter no is > 0
	 * @param noErrMsg the message to use if parameter no <= 0
	 * @param no the number of errors to report
	 * @param addToProcessLog if true, the result will be added to the process log and logger. If 
	 * false, it will be added to the logger only.
	 */
	private void logResult(String errMsg, String noErrMsg, int no, boolean addToProcessLog) {

		if (no > 0)
		{
			if (addToProcessLog)
				addLog(errMsg + " " + no);
			log.info(errMsg + " " + no);
		}
		else
		{
			if (addToProcessLog)
				addLog(noErrMsg);
			log.info(noErrMsg);
		}

	}

	/**
	 * Report problems in the process log. Sets the field rebuildRequired.
	 * @return true if problems were found.
	 */
	private boolean reportProblems() {
		
		boolean foundProblems = false;
		
		//  Check for problems with ASI values
		//  First, ASI values that have no attribute sets. ASI values were replaced 
		//  by the Material Policy Ticket as the method of FIFO/LIFO tracking 
		//  in storage
		//  TODO - this only handles M_AttributeSetInstance_ID column names. 
		//  Other column names that reference an ASI value may get missed
		
		String where = "ColumnName=" + DB.TO_STRING("M_AttributeSetInstance_ID");

		List<MColumn> columns = new Query(getCtx(),MColumn.Table_Name, where, get_TrxName())
							.list();
		
		String errMsg = "Document lines that require a correction to the ASI value: ";
		String noErrMsg = "No document lines require correction to ASI values.";
		String sql = "";
		int no = 0;
		for (MColumn column : columns)
		{
			MTable table = (MTable) column.getAD_Table();
			if (table.isView() 
				|| table.getTableName().equals("M_AttributeSetInstance")
				|| table.getTableName().equals("M_Product")
				|| table.getTableName().equals("M_Storage")  // TODO why not include M_Storage?
				)
				continue;
			
			sql = "SELECT COUNT(*) FROM " + table.getTableName() + " t "
					+ "WHERE t.M_AttributeSetInstance_ID != 0"
					+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
					+ " AND EXISTS (SELECT 1 FROM M_AttributeSetInstance asi WHERE"
					+ " asi.M_AttributeSet_ID=0"
					+ " AND asi.M_AttributeSetInstance_ID = t.M_AttributeSetInstance_ID)";
			
			no += DB.getSQLValue(get_TrxName(), sql);
			
			// Potentially valid if there is an attribute set defined.
			sql = "SELECT COUNT(*) FROM "  + table.getTableName() + " t "
					+ "WHERE t.M_AttributeSetInstance_ID != 0"
					+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
					+ " AND NOT EXISTS (SELECT 1 FROM M_AttributeInstance ai WHERE"
					+ " ai.M_AttributeSetInstance_ID = t.M_AttributeSetInstance_ID)"
					+ " AND EXISTS (SELECT 1 FROM M_AttributeSetInstance asi WHERE"
					+ " asi.M_AttributeSetInstance_ID = t.M_AttributeSetInstance_ID"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_Lot + " is null"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_M_Lot_ID + " is null"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_GuaranteeDate + " is null"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_SerNo + " is null)";
			no += DB.getSQLValue(get_TrxName(), sql);		
		
			// If the table has a product
			// TODO only deals with the M_Product_ID column, not references
			where = "AD_Table_ID=" + table.getAD_Table_ID() + " AND "
					+ "ColumnName=" + DB.TO_STRING("M_Product_ID");

			 MColumn productColumn = new Query(getCtx(), MColumn.Table_Name, where, get_TrxName())
								.first();
	
			if (productColumn == null)
				continue;
			
			sql = "SELECT COUNT(*) FROM "  + table.getTableName() + " t "
					+ "WHERE COALESCE(M_AttributeSetInstance_ID,0) = 0"
					+ "	AND EXISTS (SELECT M_AttributeSetInstance_ID from M_Product p"
					+ "	WHERE t.M_Product_ID = p.M_Product_ID AND p.M_AttributeSetInstance_ID > 0)";
			no += DB.getSQLValue(get_TrxName(), sql);
		}
		if (no > 0)
			foundProblems = true;
		logResult(errMsg, noErrMsg, no, true);	
		
		//  Test for negative quantities
		errMsg = "Storage locators/layers with negative quantities: ";
		noErrMsg = "No negative quantities found.";
		
		sql = "SELECT COUNT(*) "
				+ "FROM M_Storage s "
				+ "WHERE AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND QtyOnHand < 0";
		no = DB.getSQLValue(get_TrxName(), sql);
		logResult(errMsg, noErrMsg, no, true);
		if (no > 0) 
		{
			foundProblems = true;
		
			//  Test for negative quantities that can be covered by local moves
			//  for each locator/product/asi combination.
			errMsg = "Storage locators/layers with negative quantities that can be partially or fully corrected: ";
			noErrMsg = "Negative quantities cannot be corrected with stock in the same locator or warehouse.";
			
			sql = "SELECT COUNT(*) "
					+ "FROM M_Storage s "
					+ "WHERE AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
					+ " AND QtyOnHand < 0"
					//  Stock in same locator 
					+ " AND (EXISTS (SELECT * FROM M_Storage sl "
						+ "WHERE sl.QtyOnHand > 0"
						+ " AND s.M_Product_ID=sl.M_Product_ID"
						+ " AND s.M_Locator_ID=sl.M_Locator_ID"
						+ " AND s.M_AttributeSetInstance_ID=sl.M_AttributeSetInstance_ID)"
					//	Stock in same Warehouse
					+ " OR EXISTS (SELECT * FROM M_Storage sw"
						+ " INNER JOIN M_Locator swl ON (sw.M_Locator_ID=swl.M_Locator_ID), M_Locator sl "
						+ "WHERE sw.QtyOnHand > 0"
						+ " AND s.M_Product_ID=sw.M_Product_ID"
						+ " AND s.M_AttributeSetInstance_ID=sw.M_AttributeSetInstance_ID"
						+ " AND s.M_Locator_ID=sl.M_Locator_ID"
						+ " AND s.M_AttributeSetInstance_ID=sw.M_AttributeSetInstance_ID"
						+ " AND sl.M_Warehouse_ID=swl.M_Warehouse_ID))";
			no = DB.getSQLValue(get_TrxName(), sql);
			if (no > 0)
				foundProblems = true;
			logResult(errMsg, noErrMsg, no, true);
		}
		
		errMsg = "Empty storage records that could be deleted:";
		noErrMsg = "No empty storage records found.";
		sql = "SELECT COUNT(*) FROM M_Storage "
				+ "WHERE QtyOnHand = 0 AND QtyReserved = 0 AND QtyOrdered = 0"
				+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx());
		no = DB.getSQLValue(get_TrxName(), sql);
		if (no > 0)
			foundProblems = true;
		logResult(errMsg, noErrMsg, no, true);

		errMsg = "Storage rows having non-zero QtyReserved/QtyOrdered with no M_MPolicyTicket_ID: ";
		noErrMsg = "All storage rows with QtyReserved/QtyOrdered have a M_PolicyTicket_ID.";
		sql = "SELECT COUNT(*) FROM M_Storage "
				+ "WHERE QtyOnHand=0 AND (QtyReserved!=0 OR QtyOrdered!=0)"
				+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND COALESCE(M_MPolicyTicket_ID,0)=0";
		
		no = DB.getSQLValue(get_TrxName(), sql);	
		if (no > 0)
		{
			foundProblems = true;
			rebuildRequired = true;
		}
		logResult(errMsg, noErrMsg, no, true);

		//  Assume all Sales Order lines have the correct QtyDelivered
		//  Check for a historical issue with Purchase Order lines where 
		//  the QtyDelivered was not updated.  Active purchase orders that 
		//  have this issue should be corrected. 
		errMsg = "Purchase order lines where QtyDelivered doesn't match the sum of qty on M_MatchPO: ";
		noErrMsg = "All purchase order lines have QtyDelivered matching the sum of qty on M_MatchPO.";
		sql = "SELECT COUNT(*) FROM C_OrderLine ol"
				+ " WHERE ol.C_Order_ID = (SELECT C_Order_ID FROM C_Order o "
				+ "                           WHERE o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('CO')"
				+ "                           AND o.isSOTrx = 'N')"
				+ " AND ol.QtyDelivered < ol.QtyOrdered"
				+ " AND COALESCE(ol.M_MPolicyTicket_ID,0)=0"
				+ " AND ol.QtyDelivered < (SELECT SUM(Qty) FROM M_MatchPO mpo WHERE mpo.C_OrderLine_ID = ol.C_OrderLine_ID)"
				+ " AND ol.AD_Client_ID = " + Env.getAD_Client_ID(getCtx());
		no = DB.getSQLValue(get_TrxName(), sql);
		if (no > 0)
		{
			foundProblems = true;
			rebuildRequired = true;
		}
		logResult(errMsg, noErrMsg, no, true);
		 		
		//  Active order lines that have no MPolicyTicket
		errMsg = "Order lines that require material policy tickets to manage qty ordered/reserved: ";
		noErrMsg = "All order lines have material policy tickets to manage qty ordered/reserved.";
		sql = "SELECT COUNT(ol.*) FROM C_OrderLine ol "
				+ " JOIN M_Product p ON (p.M_Product_ID = ol.M_Product_ID AND p.isStocked='Y')"
				+ " JOIN C_Order o ON (o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('IP','CO'))"
				+ " WHERE o.AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ "	AND ABS(ol.QtyDelivered) < ABS(ol.QtyOrdered)"
				+ " 	AND COALESCE(ol.M_MPolicyTicket_ID,0)=0";
		no = DB.getSQLValue(get_TrxName(), sql);
		if (no > 0)
		{
			foundProblems = true;
			rebuildRequired = true;
		}
		logResult(errMsg, noErrMsg, no, true);

		//  Test for any M_Storage entries that have QtyReserved/QtyOrdered and also QtyOnHand
		//  MStorage records should have non-zero QtyOnHand OR non-zero QtyReserved/QtyOrdered
		//  but not both.
		errMsg = "Storage lines that have QtyOnHand > 0 but also QtyReserved or QtyOrdered > 0: ";
		noErrMsg = "The QtyOnHand and QtyReserved/QtyOrdered are properly separated on storage lines.";
		sql = "SELECT COUNT(*) FROM M_Storage "
				+ "WHERE AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND QtyOnHand != 0 AND (QtyReserved != 0 OR QtyOrdered != 0)";
		no = DB.getSQLValue(get_TrxName(), sql);
		if (no > 0)
		{
			foundProblems = true;
			rebuildRequired = true;
		}
		logResult(errMsg, noErrMsg, no, true);
		
		//  Get all the incomplete order lines that have a MPolicyTicket that is not in MStorage
		//  These should be added to the Storage records
		errMsg = "Order lines that are not fulfilled and that do not have a material policy ticket: ";
		noErrMsg = "All unfulfilled order lines have a material policy ticket.";
		sql = "SELECT COUNT(ol.*) FROM C_OrderLine ol "
				+ " JOIN M_Product p ON (p.M_Product_ID = ol.M_Product_ID AND p.isStocked='Y')"
				+ " JOIN C_Order o ON (o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('IP','CO'))"
				+ " LEFT OUTER JOIN M_Storage s ON (ol.M_MPolicyTicket_ID = s.M_MPolicyTicket_ID)"
				+ " WHERE o.AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ "	AND ABS(ol.QtyDelivered) < ABS(ol.QtyOrdered)"
				+ " 	AND COALESCE(s.M_MPolicyTicket_ID,0) = 0";
		no = DB.getSQLValue(get_TrxName(), sql);
		if (no > 0)
		{
			foundProblems = true;
			rebuildRequired = true;
		}
		logResult(errMsg, noErrMsg, no, true);

		// Find all the ordered/reserved storage lines that refer to Material Policy tickets that 
		// do not exist in C_OrderLine - could be caused by a an interrupted process but generally
		// will not happen.  These should be deleted.
		errMsg = "Storage records with QtyReserved/QtyOrdered != 0 that are not connected to an order line: ";
		noErrMsg = "All storage records with QtyReserved/QtyOrdered are connected to an order line.";
		sql = "SELECT COUNT(*) FROM M_Storage s"
				+ " WHERE AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND QtyOnHand = 0 AND (QtyReserved != 0 OR QtyOrdered != 0)"
				+ " AND M_MPolicyTicket_ID > 0"
				+ " AND NOT EXISTS (SELECT C_OrderLine_ID FROM C_OrderLine ol WHERE"
				+ "   ol.M_MPolicyTicket_ID = s.M_MPolicyTicket_ID) ";
		no = DB.getSQLValue(get_TrxName(), sql);
		if (no > 0)
		{
			foundProblems = true;
			rebuildRequired = true;
		}
		logResult(errMsg, noErrMsg, no, true);

		//  Check if any storage records have  QtyOrdered or QtyReserved for voided orders.
		errMsg = "Storage records with QtyOrdered/QtyReserved != 0 where the order is voided or reversed: ";
		noErrMsg = "All voided/reversed orders have no QtyOrdered/QtyReversed in storage.";
		sql = "SELECT COUNT(*) FROM M_Storage s"
				+ " WHERE AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND QtyOnHand = 0 AND (QtyReserved != 0 OR QtyOrdered != 0)"
				+ " AND M_MPolicyTicket_ID > 0"
				+ " AND EXISTS (SELECT C_OrderLine_ID FROM C_Order o, C_OrderLine ol"
				+ " WHERE o.C_Order_ID = ol.C_Order_ID"
				+ " AND ol.M_MPolicyTicket_ID = s.M_MPolicyTicket_ID"
				+ " AND o.DocStatus in ('VO','RE'))";
		no = DB.getSQLValue(get_TrxName(), sql);
		if (no > 0)
		{
			foundProblems = true;
			rebuildRequired = true;
		}
		logResult(errMsg, noErrMsg, no, true);

		return foundProblems;
	}

	private void coverNegativeQuantities() {
		
		// for each locator/product/asi combination.
		String errMsg = "Storage entries that had negative quantities that were fully or partially covered by movements: ";
		String noErrMsg = "No movements were possible to cover negative quantities or no negative quantities existed.";
		String sql = "SELECT s.M_Product_ID, s.M_Locator_ID, s.M_AttributeSetInstance_ID, s.M_MPolicyTicket_ID "
						+ "FROM M_Storage s "
						+ "WHERE AD_Client_ID = ?"
						+ " AND QtyOnHand < 0"
						//	Stock in same location
						+ " AND (EXISTS (SELECT * FROM M_Storage sl "
							+ "WHERE sl.QtyOnHand > 0"
							+ " AND s.M_Product_ID=sl.M_Product_ID"
							+ " AND s.M_Locator_ID=sl.M_Locator_ID"
							+ " AND s.M_AttributeSetInstance_ID=sl.M_AttributeSetInstance_ID)";
		
		if (isAllowMoveBetweenLocators())
		{
						//	Stock in same Warehouse
			sql += " OR EXISTS (SELECT * FROM M_Storage sw"
							+ " INNER JOIN M_Locator swl ON (sw.M_Locator_ID=swl.M_Locator_ID), M_Locator sl "
							+ "WHERE sw.QtyOnHand > 0"
							+ " AND s.M_Product_ID=sw.M_Product_ID"
							+ " AND s.M_AttributeSetInstance_ID=sw.M_AttributeSetInstance_ID"
							+ " AND s.M_Locator_ID=sl.M_Locator_ID"
							+ " AND s.M_AttributeSetInstance_ID=sw.M_AttributeSetInstance_ID"
							+ " AND sl.M_Warehouse_ID=swl.M_Warehouse_ID)";
		}
		sql += ") GROUP BY s.M_Product_ID, s.M_AttributeSetInstance_ID, s.M_Locator_ID, s.M_MPolicyTicket_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int no = 0;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				no += move (rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4));
			}
 		}
		catch (Exception e)
		{
			log.log (Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		logResult(errMsg, noErrMsg, no, true);
		
	}

	private void rebuildStorage() {
		if(isOnlyReport() || !isRebuildStorage())
			return;
		
		getStorageUtil().deleteAndRebuildStorageRecords();
		
		addLog("Storage rebuilt.");
		
	}

	protected StorageUtil getStorageUtil() {
		return new StorageUtil(getCtx(), get_TrxName());
	}

	private void removeMalformedASIValues() {
				
		//  Remove ASI values that have no attribute sets. ASI was replaced by the Material 
		//  Policy Ticket as the method of FIFO/LIFO tracking in storage
		//  TODO - this only handles M_AttributeSetInstance_ID column names
		String where = "ColumnName=" + DB.TO_STRING("M_AttributeSetInstance_ID");

		List<MColumn> columns = new Query(getCtx(),MColumn.Table_Name, where, get_TrxName())
							.list();
		
		String errMsg = "Corrected ASI values on document lines: ";
		String noErrMsg = "All ASI values on document lines are valid.";
		int no = 0;
		
		for (MColumn column : columns)
		{
			MTable table = (MTable) column.getAD_Table();
			
			// There are cases where the M_AttributeSetInstance is part of the 
			// a multi-key constraint so the null value of zero is allowed.  For
			// the product table, the template ASI may fail these test as it is
			// deemed incomplete.  This also applies to the M_AttributeSetInstance
			// table itself.  For these cases, skip the table.
			if (table.isView() 
				|| table.getTableName().equals("M_AttributeSetInstance")
				|| table.getTableName().equals("M_Product")
				|| column.isKey()
				|| column.isParent()
				|| column.isMandatory()
//				|| table.getTableName().equals("M_Cost")
//				|| table.getTableName().equals("M_CostDetail")
//				|| table.get_TableName().equals("T_InventoryValue")
//				|| table.getTableName().equals("M_Storage") 
				)
				continue;
			
			// Check if the column is part of the primary key of the table
			boolean isDatabaseKey = false;

			
			String sql = "UPDATE " + table.getTableName() + " t "
					+ "SET M_AttributeSetInstance_ID = null "   // Invalid values can be null, like most model ID
					+ "WHERE t.M_AttributeSetInstance_ID >= 0"
					+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
					+ " AND EXISTS (SELECT 1 FROM M_AttributeSetInstance asi WHERE"
					+ " coalesce(asi.M_AttributeSet_ID,0)=0"
					+ " AND asi.M_AttributeSetInstance_ID = t.M_AttributeSetInstance_ID)";
			no += DB.executeUpdate(sql, get_TrxName());
			
			// Potentially valid if there is an attribute set defined.
			sql = "UPDATE " + table.getTableName() + " t "
					+ "SET M_AttributeSetInstance_ID = null "
					+ "WHERE t.M_AttributeSetInstance_ID >= 0"
					+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
					+ " AND NOT EXISTS (SELECT 1 FROM M_AttributeInstance ai WHERE"
					+ " ai.M_AttributeSetInstance_ID = t.M_AttributeSetInstance_ID)"
					+ " AND EXISTS (SELECT 1 FROM M_AttributeSetInstance asi WHERE"
					+ " asi.M_AttributeSetInstance_ID = t.M_AttributeSetInstance_ID"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_Lot + " is null"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_M_Lot_ID + " is null"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_GuaranteeDate + " is null"
					+ " AND asi." + MAttributeSetInstance.COLUMNNAME_SerNo + " is null)";
			no += DB.executeUpdate(sql, get_TrxName());
		
			// If the table has a product
			// TODO only deals with the M_Product_ID column, not references
			where = "AD_Table_ID=" + table.getAD_Table_ID() + " AND "
					+ "ColumnName=" + DB.TO_STRING("M_Product_ID");

			 MColumn productColumn = new Query(getCtx(), MColumn.Table_Name, where, get_TrxName())
								.first();
	
			if (productColumn == null)
				continue;
			
			sql = "UPDATE " + table.getTableName() + " t "
					+ " SET M_AttributeSetInstance_ID = (SELECT M_AttributeSetInstance_ID from M_Product p"
					+ " WHERE t.M_Product_ID = p.M_Product_ID) "
					+ "WHERE COALESCE(M_AttributeSetInstance_ID,0) = 0"
					+ "	AND EXISTS (SELECT M_AttributeSetInstance_ID from M_Product p"
					+ "	WHERE t.M_Product_ID = p.M_Product_ID AND p.M_AttributeSetInstance_ID > 0)";
			no += DB.executeUpdate(sql, get_TrxName());
		}		
		logResult(errMsg, noErrMsg, no, true);	
	}

	private void removeEmptyStorage() {
		
		boolean execute = !isOnlyReport() && isCorrectBadASIValues() && isRebuildStorage() && isCoverNegativeQty();
		if (!execute)
			return;
		
		String errMsg = "Empty storage records deleted:";
		String noErrMsg = "No empty storage records found.";
		String sql = "DELETE FROM M_Storage "
				+ "WHERE QtyOnHand = 0 AND QtyReserved = 0 AND QtyOrdered = 0"
				+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx());
		int no = DB.executeUpdate(sql, get_TrxName());
		logResult(errMsg, noErrMsg, no, true);
		
	}

	/**
	 * Move stock
	 * @param m_product_id
	 * @param m_locator_id
	 * @param m_attributeSetInstance_id
	 * @param m_mPolicyTicket_id
	 * @return
	 */
	private int move (int m_product_id, int m_locator_id, int m_attributeSetInstance_id, int m_mPolicyTicket_id)
	{
		MProduct product = new MProduct(getCtx(),m_product_id,get_TrxName());	
		MLocator locator = MLocator.get(getCtx(), m_locator_id);
		boolean positiveOnly = true;
		boolean fifo = MClient.MMPOLICY_FiFo.equals(product.getMMPolicy());
		int m_warehouse_id = locator.getM_Warehouse_ID();
		
		//  Move the positive to the negative in two stages. First within the 
		//  locator, which means moving between FIFO/LIFO layers. Second, 
		//  between locators within the same warehouse.

		//  ******** Within this locator ******** 
		//  Find all storage records for the product/ASI at this locator, whether positive or negative quantities
		MStorage[] storages = MStorage.getWarehouse(getCtx(), m_warehouse_id, m_product_id, m_attributeSetInstance_id, 
				0, null, fifo, false, m_locator_id, get_TrxName());

		MStorage[] sources = MStorage.getWarehouse(getCtx(), m_warehouse_id, m_product_id, m_attributeSetInstance_id, 
				0, null, fifo, positiveOnly, m_locator_id, get_TrxName());
		
		//  Get the quantities on hand and required
		BigDecimal qtyOnHand = Env.ZERO;
		BigDecimal qtyRequired = Env.ZERO;
		
		//  Find the negative entries
		for (MStorage storage : storages) 
		{
			//  Ignore positive entries
			if (storage.getQtyOnHand().signum() >= 0)
				continue;
			
			qtyRequired = qtyRequired.subtract(storage.getQtyOnHand());
		}

		//  From the positive sources, get the qty on hand
		for (MStorage source : sources) {
			qtyOnHand = qtyOnHand.add(source.getQtyOnHand());
		}
		
		//  How much to move
		BigDecimal qtyToMove = qtyOnHand.compareTo(qtyRequired) > 0 ? qtyRequired : qtyOnHand;
		
		//  Create the moves using the local sources
		BigDecimal qtyMoved = applySources(m_product_id, m_locator_id, m_attributeSetInstance_id, sources, qtyToMove);		
		
		//  ******** Between locators ******** 		
		if (this.isAllowMoveBetweenLocators())
		{
			//  Check if we still have negative storage after the local move
			storages = MStorage.getWarehouse(getCtx(), m_warehouse_id, m_product_id, m_attributeSetInstance_id, 
					0, null, fifo, false, m_locator_id, get_TrxName());
			
			//  Find the negative entries
			qtyRequired = Env.ZERO;
			for (MStorage storage : storages) 
			{
				//  Ignore positive entries
				if (storage.getQtyOnHand().signum() >= 0)
					continue;
				
				qtyRequired = qtyRequired.subtract(storage.getQtyOnHand());
			}
			
			if (qtyRequired.compareTo(Env.ZERO) == 0)
				return qtyMoved.intValue();  // No need to do more
			
			//  Moves between locators are required.  Find the source locators within the same warehouse
			sources = MStorage.getWarehouse(getCtx(), m_warehouse_id, m_product_id, m_attributeSetInstance_id, 
					0, null, fifo, positiveOnly, 0, get_TrxName());
			
			//  Get the quantities on hand
			qtyOnHand = Env.ZERO;
			for (MStorage source : sources) {
				qtyOnHand = qtyOnHand.add(source.getQtyOnHand());
			}
			
			//  How much to move
			qtyToMove = qtyOnHand.compareTo(qtyRequired) > 0 ? qtyRequired : qtyOnHand;
			
			//  Create the moves using the warehouse sources and add the quantity to the amount moved locally
			qtyMoved = qtyMoved.add(applySources(m_product_id, m_locator_id, m_attributeSetInstance_id, sources, qtyToMove));		

		}
		
		return qtyMoved.signum();
		
	}	//	move
	
	/**
	 * Apply the quantity to move from the source storage locations to the negative quantity 
	 * layers in the locator
	 * @param m_product_id
	 * @param m_attributeSetInstance_id
	 * @param m_locator_id 
	 * @param sources
	 * @param qtyToMove
	 * @return
	 */
	private  BigDecimal applySources (int m_product_id, int m_attributeSetInstance_id, int m_locator_id, MStorage[] sources, BigDecimal qtyToMove)	{
		
		BigDecimal qtyMoved = Env.ZERO;
		BigDecimal qtyMovedTotal = Env.ZERO;

		if (sources.length == 0  || qtyToMove.compareTo(Env.ZERO) == 0)
			return Env.ZERO;

		//	Create Movement
		MMovement movement = new MMovement (getCtx(), 0, get_TrxName());
		movement.setAD_Org_ID(sources[0].getAD_Org_ID());
		movement.setC_DocType_ID(getDocTypeId());
		movement.setDescription(getName());
		if (!movement.save())
			return Env.ZERO;
		
		int lines = 0;
		for (MStorage source : sources)
		{	
			//  Negative sources shouldn't happen but in case
			if (source.getQtyOnHand().signum() <= 0)
				continue;
			
			if (qtyToMove.compareTo(source.getQtyOnHand()) > 0)
				qtyMoved = source.getQtyOnHand();
			else
				qtyMoved = qtyToMove;
			
			if (qtyMoved.signum() <= 0)
				continue;

			//	Movement Line
			MMovementLine ml = new MMovementLine(movement);
			ml.setM_Product_ID(m_product_id);
			ml.setM_LocatorTo_ID(m_locator_id);
			ml.setM_AttributeSetInstanceTo_ID(m_attributeSetInstance_id);
			//	From
			ml.setM_Locator_ID(source.getM_Locator_ID());
			ml.setM_AttributeSetInstance_ID(source.getM_AttributeSetInstance_ID());
			
			ml.setMovementQty(qtyMoved);
			//
			lines++;
			ml.setLine(lines*10);
			if (!ml.save())
				return Env.ZERO;
			
			qtyToMove = qtyToMove.subtract(qtyMoved);
			qtyMovedTotal = qtyMovedTotal.add(qtyMoved);
			
			// If there is nothing left to move, break out of the loop
			if (qtyToMove.signum() <= 0)
				break;
			
		}	//	for all source locator/layers
		
		if (qtyMovedTotal.signum() > 0 && lines > 0) 
		{
			//	Process
			movement.processIt(MMovement.ACTION_Complete);
			movement.saveEx();
			
			addLog(0, null, new BigDecimal(lines), "@M_Movement_ID@ " + movement.getDocumentNo() + " (" 
				+ MRefList.get(getCtx(), MMovement.DOCSTATUS_AD_Reference_ID, 
					movement.getDocStatus(), get_TrxName()) + ")");
		}
		else
		{
			// If there is no quantity moved, delete the header
			movement.deleteEx(true);
		}

		return qtyMovedTotal;  // Total moved
	}	//	applySources
	
}	//	StorageCleanup
