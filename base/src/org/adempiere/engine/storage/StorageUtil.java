package org.adempiere.engine.storage;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_ProjectIssue;
import org.compiere.model.I_M_InOutLine;
import org.compiere.model.I_M_InventoryLine;
import org.compiere.model.I_M_MovementLine;
import org.compiere.model.I_M_ProductionLine;
import org.compiere.model.I_M_Transaction;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MOrderLine;
import org.compiere.model.MTransaction;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.I_PP_Cost_Collector;
import org.eevolution.model.I_PP_OrderReceipt;
import org.eevolution.model.I_PP_Order_BOMLineIssue;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderBOMLine;

public class StorageUtil {
	
	private static CLogger log = CLogger.getCLogger(StorageUtil.class);
	
	private Properties ctx;
	private String trxName;
	private StorageEngine storageEngine;
	private PreparedStatement pstmt = null;
	private ResultSet rs = null;
	
	protected CLogger getCLogger() {
		
		return log;
		
	}
	
	protected Properties getDefaultContext() {

		return Env.getCtx();

	}

	protected String getDefaultTrxName() {

		return null;

	}

	public StorageUtil () {

		ctx = getDefaultContext();
		trxName = getDefaultTrxName();
		storageEngine = StorageEngine.get();

	}
	
	public StorageUtil(Properties ctx, String trxName) {

		this.ctx = ctx;
		this.trxName = trxName;
		storageEngine = StorageEngine.get();

	}
	
	
	protected int dbExecuteUpdateEx(String sql) {
		return DB.executeUpdateEx(sql, trxName);
	}

	protected int dbExecuteUpdateEx(String sql, Object[] parameters) {
		return DB.executeUpdateEx(sql, parameters, trxName);
	}
	

	public void addMissingMaterialPolicyTickets() {
		
		String where = "COALESCE(M_MPolicyTicket_ID,0)=0";
		
		List<MTransaction> transactions = new Query(ctx, I_M_Transaction.Table_Name, where, trxName)
												.setClient_ID()
												.list();
		
		for (MTransaction transaction : transactions)
		{
			
			MMPolicyTicket ticket = MMPolicyTicket.getOrCreateFromTransaction(ctx, transaction, trxName);
			transaction.setM_MPolicyTicket_ID(ticket.getM_MPolicyTicket_ID());
			transaction.saveEx();
			
		}
		
	}
	
	
	public void addMissingTransactionsToDocumentLines() {
				
		List<String> tableNames = new ArrayList<>();
		tableNames.add(I_M_InOutLine.Table_Name);
		tableNames.add(I_M_InventoryLine.Table_Name);
		tableNames.add(I_M_MovementLine.Table_Name);
		tableNames.add(I_M_ProductionLine.Table_Name);
		tableNames.add(I_C_ProjectIssue.Table_Name);
		tableNames.add(I_PP_Cost_Collector.Table_Name);
		tableNames.add(I_PP_OrderReceipt.Table_Name);
		tableNames.add(I_PP_Order_BOMLineIssue.Table_Name);
		
		// Ensure every document line has a transaction associated with it.
		String whereTemplate  = "EXISTS (SELECT p.M_Product_ID FROM M_Product p WHERE tableName.M_Product_ID = p.M_Product_ID AND p.IsStocked='Y')"
				+ " AND NOT EXISTS (SELECT M_Transaction_ID FROM M_Transaction WHERE tableName.tableName_ID = M_Transaction.tableName_ID)"
				+ " AND tableName.processed = 'Y'";

		for (String tableName : tableNames)
		{
			String where = whereTemplate.replace("tableName", tableName);
			
			List<PO> lines = new Query(ctx, tableName, where, trxName)
											.setClient_ID()
											.list();
			
			
			for (PO line : lines)
			{
				IDocumentLine docLine = (IDocumentLine) line;
				storageEngine.applyStorageRules(docLine);
				
			}
		}
	}
	
	protected StorageEngine getStorageEngine() {
		
		return StorageEngine.get();
		
	}

	public void deleteAndRebuildStorageRecords() {
		
		deleteAllStorageRecords();
		rebuildStorageEntriesfromMTransaction();
		resetQtyOrderedAndQtyReserved();

	}

	protected void deleteAllStorageRecords() {
		String sql = "Delete FROM M_Storage "
				+ "WHERE AD_Client_ID = ?";
		int no = dbExecuteUpdateEx(sql, new Object[]{Env.getAD_Client_ID(ctx)});
		getCLogger().config("Deleted old #" + no);
	}

	protected void rebuildStorageEntriesfromMTransaction() {
		String sql = "INSERT INTO M_Storage "
			+ " (AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy, "
			+ " M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID, "
			+ " QtyOnHand, QtyReserved, QtyOrdered)"
			+ " SELECT AD_Client_ID, AD_Org_ID, 'Y', NOW(), " + Env.getAD_User_ID(ctx) + ", NOW(), "
			+ "       " + Env.getAD_User_ID(ctx) + ", "
					+ "M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID, "
			+ "       SUM(MovementQty), 0.0, 0.0 "
			+ "   FROM M_Transaction WHERE AD_Client_ID=" + Env.getAD_Client_ID(ctx)
			+ "   GROUP BY AD_Client_ID, AD_Org_ID, M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID"
			+ "   HAVING SUM(MovementQty) != 0";
		int no = dbExecuteUpdateEx(sql);
		getCLogger().config("Rebuilt from transactions #" + no);
	}

	protected void resetQtyOrderedAndQtyReserved() {
				
		setPurchaseOrderLineQtyDeliveredToSumOfMatchPO();
		
		findMOrderLinesAndUpdateStorage();
		findManufacturingOrderLinesAndUpdateStorage();
		findManufacturingOrderBOMLinesAndUpdateStorage();

	}

	protected void findManufacturingOrderBOMLinesAndUpdateStorage() {
		String sql;
		sql = "SELECT ppo.DatePromised, ppol.* FROM PP_Order_BOMLine ppol "
				+ " JOIN M_Product p ON (p.M_Product_ID = ppol.M_Product_ID AND p.isStocked='Y')"
				+ " JOIN PP_Order_BOM ppob ON (ppob.PP_Order_BOM_ID = ppol.PP_Order_BOM_ID)"
				+ " JOIN PP_Order ppo ON (ppo.PP_Order_ID = ppob.PP_Order_ID)"
				+ " LEFT OUTER JOIN M_Storage s ON (ppol.M_MPolicyTicket_ID = s.M_MPolicyTicket_ID)"
				+ " WHERE ppol.AD_Client_ID = " + Env.getAD_Client_ID(ctx)
				+ " AND ppo.docstatus in ('IP','CO')"
				+ "	AND ABS(ppol.QtyDelivered) < ABS(ppol.QtyEntered)"
				+ " 	AND COALESCE(s.M_MPolicyTicket_ID,0)=0";
		findDocumentLinesAndUpdateStorage(sql, MPPOrderBOMLine.class);
	}

	protected void findManufacturingOrderLinesAndUpdateStorage() {
		String sql;
		sql = "SELECT ppo.* FROM PP_Order ppo "
				+ " JOIN M_Product p ON (p.M_Product_ID = ppo.M_Product_ID AND p.isStocked='Y')"
				+ " LEFT OUTER JOIN M_Storage s ON (ppo.M_MPolicyTicket_ID = s.M_MPolicyTicket_ID)"
				+ " WHERE ppo.AD_Client_ID = " + Env.getAD_Client_ID(ctx)
				+ " AND ppo.docstatus in ('IP','CO')"
				+ "	AND ABS(ppo.QtyDelivered) < ABS(ppo.QtyOrdered)"
				+ " 	AND COALESCE(s.M_MPolicyTicket_ID,0) = 0";
		findDocumentLinesAndUpdateStorage(sql, MPPOrder.class);
	}

	protected void findMOrderLinesAndUpdateStorage() {
		String sql;
		sql = "SELECT ol.* FROM C_OrderLine ol "
				+ " JOIN M_Product p ON (p.M_Product_ID = ol.M_Product_ID AND p.isStocked='Y')"
				+ " JOIN C_Order o ON (o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('IP','CO'))"
				+ " LEFT OUTER JOIN M_Storage s ON (ol.M_MPolicyTicket_ID = s.M_MPolicyTicket_ID)"
				+ " WHERE o.AD_Client_ID = " + Env.getAD_Client_ID(ctx)
				+ "	AND ABS(ol.QtyDelivered) < ABS(ol.QtyOrdered)"
				+ " 	AND COALESCE(s.M_MPolicyTicket_ID,0) = 0";
		findDocumentLinesAndUpdateStorage(sql, MOrderLine.class);
	}

	protected void setPurchaseOrderLineQtyDeliveredToSumOfMatchPO() {

		//  There is a historical issue where the QtyDelivered in the Purchase Order was
		//  not used.  Correct this by ensuring the QtyDelivered is set to the sum
		//  of the M_MatchPO records for all Purchase Orders (isSOTrx = 'N') where 
		//  the qtyDelivered is less than the qtyOrdered.  Only correct Completed
		//  orders. Ignore closed or voided orders.

		String sql = "UPDATE C_OrderLine ol"
					 + " SET QtyDelivered = (SELECT SUM(Qty) FROM M_MatchPO mpo WHERE mpo.C_OrderLine_ID = ol.C_OrderLine_ID),"
					 + "     DateDelivered = (SELECT MAX(DateTrx) FROM M_MatchPO mpo WHERE mpo.C_OrderLine_ID = ol.C_OrderLine_ID)"
					 + " WHERE ol.C_Order_ID = (SELECT C_Order_ID FROM C_Order o "
					 + "                           WHERE o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('CO')"
					 + "                           AND o.isSOTrx = 'N')"
					 + " AND ol.QtyDelivered < ol.QtyOrdered"
					 + " AND ol.QtyDelivered < (SELECT SUM(Qty) FROM M_MatchPO mpo WHERE mpo.C_OrderLine_ID = ol.C_OrderLine_ID)"
					 + " AND ol.AD_Client_ID = " + Env.getAD_Client_ID(ctx);
		int no = DB.executeUpdate(sql, trxName);		
		log.config("Number of purchase order lines requiring corrected QtyDelivered: " + no);
	}

	protected void findDocumentLinesAndUpdateStorage(String querySQLToFindDocumentLines, Class<?> modelClass)
			 
	{
		try {
			Constructor<?> c = modelClass.getDeclaredConstructor(Properties.class, ResultSet.class, String.class);
			pstmt = DB.prepareStatement (querySQLToFindDocumentLines, trxName);
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				IDocumentLine line = (IDocumentLine) c.newInstance(ctx, rs, trxName);
				storageEngine.applyStorageRules(line);
			}
		} catch (SQLException | InstantiationException | IllegalAccessException 
				| IllegalArgumentException | InvocationTargetException 
				| NoSuchMethodException | SecurityException e) {
			getCLogger().severe(e.getMessage());
		} finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}
}
