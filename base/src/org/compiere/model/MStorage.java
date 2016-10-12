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
package org.compiere.model;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogMgt;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 	Inventory Storage Model
 *
 *	@author Jorg Janke
 *	@version $Id: MStorage.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 *
 *  @author mckayERP www.mckayERP.com
 *  			<li> #282 MStorage.getQtyAvailable does not ignore the warehouse if M_Warehouse_ID = 0
 */
public class MStorage extends X_M_Storage
{
	/**
	 * generated serialVersionUID
	 */
	private static final long serialVersionUID = 9086223702645715061L;

	/**
	 * 	Get Storage Info - The entry for the locator, product and ASI where 
	 *  there is no Material Policy Ticket
	 *	@param ctx context
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID instance
	 *	@param trxName transaction
	 *	@return existing or null
	 */
	@Deprecated
	public static MStorage get (Properties ctx, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, String trxName)
	{
		return get(ctx, M_Locator_ID, M_Product_ID, M_AttributeSetInstance_ID, 0, trxName);
	}

	/**
	 * 	Get Storage Info - The entry for the locator, product and ASI and  
	 *  a Material Policy Ticket
	 *	@param ctx context
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID instance
	 *	@param trxName transaction
	 *	@return existing or null
	 */
	public static MStorage get (Properties ctx, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int M_MPolicyTicket_ID, String trxName)
	{
		MStorage retValue = null;
		String sql = "SELECT * FROM M_Storage "
			+ "WHERE M_Locator_ID=? AND M_Product_ID=?";
		if (M_AttributeSetInstance_ID == 0)
			sql += " AND (M_AttributeSetInstance_ID=? OR M_AttributeSetInstance_ID IS NULL)";
		else
			sql += " AND M_AttributeSetInstance_ID=?";
		if (M_MPolicyTicket_ID == 0)
			sql += " AND (M_MPolicyTicket_ID = ? OR M_MPolicyTicket_ID IS NULL)";
		else
			sql += " AND M_MPolicyTicket_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, M_Locator_ID);
			pstmt.setInt (2, M_Product_ID);
			pstmt.setInt (3, M_AttributeSetInstance_ID);
			pstmt.setInt (4, M_MPolicyTicket_ID);
			rs = pstmt.executeQuery ();
			if (rs.next ())
				retValue = new MStorage (ctx, rs, trxName);
		}
		catch (SQLException ex)
		{
			s_log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		if (retValue == null)
			s_log.fine("Not Found - M_Locator_ID=" + M_Locator_ID 
				+ ", M_Product_ID=" + M_Product_ID 
				+ ", M_AttributeSetInstance_ID=" + M_AttributeSetInstance_ID
				+ ", M_MPolicyTicket_ID=" + M_MPolicyTicket_ID);
		else
			s_log.fine("M_Locator_ID=" + M_Locator_ID 
				+ ", M_Product_ID=" + M_Product_ID 
				+ ", M_AttributeSetInstance_ID=" + M_AttributeSetInstance_ID
				+ ", M_MPolicyTicket_ID=" + M_MPolicyTicket_ID);
		return retValue;
	}	//	get


	/**
	 * Get the storage record used for reservations and orders of the product/ASI at a warehouse.  A 
	 * new storage location will be created if none exists. The locator used is always the default 
	 * locator for the warehouse.
	 * @param ctx
	 * @param productId
	 * @param warehouseId
	 * @param attributeSetInstanceId
	 * @param policyTicket_id the material policy ticket to use
	 * @param trxName
	 * @return The storage location to use for reservations and orders.
	 */
	public static MStorage getReservedOrdered (Properties ctx,
			int productId, int warehouseId, int attributeSetInstanceId, int policyTicket_id, String trxName)
		{
			MWarehouse wh = MWarehouse.get(ctx, warehouseId);
			// reservations and orders are made using the default locator of the source warehouse
			int xM_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();  
			return MStorage.getCreate(ctx, xM_Locator_ID, productId, attributeSetInstanceId, policyTicket_id, trxName);
		}	//	get

	/**
	 * get Qty Reserved
	 * @param ctx
	 * @param productId
	 * @param warehouseId
	 * @param attributeSetInstanceId
	 * @param trxName
	 * @deprecated as of 3.9.0 Use getReservedOrdered
	 * @return
	 */
	public static MStorage getQtyReserved (Properties ctx,
		int productId, int warehouseId, int attributeSetInstanceId, String trxName)
	{
		MWarehouse wh = MWarehouse.get(ctx, warehouseId);
		// reservations and orders are made using the default locator of the source warehouse
		int xM_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();   
		int M_MPolicyTicket_ID = 0; // Always for reservations and orders

		return MStorage.getCreate(ctx, xM_Locator_ID, productId, attributeSetInstanceId, M_MPolicyTicket_ID, trxName);
	}	//	get


	/**
	 * get Qty Ordered of a product/ASI from a particular warehouse. The returned value is the 
	 * highest quantity ordered. 
	 * @param ctx
	 * @param productId
	 * @param warehouseId
	 * @param attributeSetInstanceId
	 * @param trxName
	 * @deprecated as of 3.9.0  Use getReservedOrdered
	 * @return
	 */
	public static MStorage getQtyOrdered (Properties ctx,
		int productId, int warehouseId , int attributeSetInstanceId, String trxName)
	{
		// TODO - Does this need to include the M_MPolicyTicket_ID?
		final StringBuilder whereClause = new StringBuilder();
		whereClause.append("EXISTS (SELECT 1 FROM M_Locator l WHERE l.M_Locator_ID=M_Storage.M_Locator_ID AND l.M_Warehouse_ID=? ) AND ");
		whereClause.append(MStorage.COLUMNNAME_M_Product_ID).append("=?");
		if (attributeSetInstanceId > 0)
//			whereClause.append(MStorage.COLUMNNAME_M_AttributeSetInstance_ID).append("=? OR ").append(MStorage.COLUMNNAME_M_AttributeSetInstance_ID).append(" IS NULL ");
//		else
			whereClause.append(" AND ").append(MStorage.COLUMNNAME_M_AttributeSetInstance_ID).append("=?");

		whereClause.append(" AND QtyOrdered <> 0");

		Query query = new Query(ctx,MStorage.Table_Name, whereClause.toString(), trxName).setClient_ID()
		.setOrderBy(MStorage.COLUMNNAME_QtyOrdered);
		
		if (attributeSetInstanceId > 0)
			query.setParameters( warehouseId , productId , attributeSetInstanceId);
		else
			query.setParameters( warehouseId , productId);
		
		return query.first();
	}	//	get

	/**
	 * 	Get all Storages for Product with ASI and QtyOnHand <> 0
	 *	@param ctx context
	 *	@param M_Product_ID product
	 *	@param M_Locator_ID locator
	 *	@param FiFo first in-first-out
	 *	@param trxName transaction
	 *	@return existing or null
	 */
	public static MStorage[] getAllWithASI (Properties ctx, int M_Product_ID, int M_Locator_ID, 
		boolean FiFo, String trxName)
	{
		ArrayList<MStorage> list = new ArrayList<MStorage>();
		String sql = "SELECT * FROM M_Storage s "
			+ "INNER JOIN M_PolicyTicket_ID p ON (s.M_MPolicyTicket_ID=p.M_MPolicyTicket_ID) "
			+ "WHERE s.M_Product_ID=? AND s.M_Locator_ID=?"
			+ " AND s.M_AttributeSetInstance_ID > 0 "
			+ " AND QtyOnHand <> 0 "			
			+ " ORDER BY COALESCE(p.MovementDate,to_timestamp(0.0))";
		if (!FiFo)
			sql += " DESC";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, M_Product_ID);
			pstmt.setInt (2, M_Locator_ID);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add(new MStorage (ctx, rs, trxName));
		}
		catch (SQLException ex)
		{
			s_log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		MStorage[] retValue = new MStorage[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getAllWithASI

	/**
	 * 	Get all Storages for Product where QtyOnHand <> 0
	 *	@param ctx context
	 *	@param M_Product_ID product
	 *	@param M_Locator_ID locator
	 *	@param trxName transaction
	 *	@return existing or null
	 */
	public static MStorage[] getAll (Properties ctx, 
		int M_Product_ID, int M_Locator_ID, String trxName)
	{
		ArrayList<MStorage> list = new ArrayList<MStorage>();
		String sql = "SELECT * FROM M_Storage s "
			+ "INNER JOIN M_MPolicyTicket p ON (s.M_MPolicyTicket_ID=p.M_MPolicyTicket_ID) "
			+ "WHERE s.M_Product_ID=? AND s.M_Locator_ID=?"
			+ " AND s.QtyOnHand <> 0 "
			+ "ORDER BY  COALESCE(p.MovementDate,to_timestamp(0.0))";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, M_Product_ID);
			pstmt.setInt (2, M_Locator_ID);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add(new MStorage (ctx, rs, trxName));
		}
		catch (SQLException ex)
		{
			s_log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		MStorage[] retValue = new MStorage[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getAll

	
	/**
	 * 	Get Storage Info for Product across warehouses
	 *	@param ctx context
	 *	@param M_Product_ID product
	 *	@param trxName transaction
	 *	@return existing or null
	 */
	public static MStorage[] getOfProduct (Properties ctx, int M_Product_ID, String trxName)
	{
		ArrayList<MStorage> list = new ArrayList<MStorage>();
		String sql = "SELECT * FROM M_Storage "
			+ "WHERE M_Product_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement (sql, trxName);
			pstmt.setInt (1, M_Product_ID);
			rs = pstmt.executeQuery ();
			while (rs.next ())
				list.add(new MStorage (ctx, rs, trxName));
		}
		catch (SQLException ex)
		{
			s_log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		MStorage[] retValue = new MStorage[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getOfProduct
	
	/**
	 * 	Get Storage Info for Warehouse
	 *	@param ctx context
	 *	@param M_Warehouse_ID 
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID instance
	 *	@param M_AttributeSet_ID attribute set
	 *	@param allAttributeInstances if true, all attribute set instances
	 *	@param minGuaranteeDate optional minimum guarantee date if all attribute instances
	 *	@param FiFo first in-first-out
	 *	@param trxName transaction
	 *	@return existing - ordered by location priority (desc) and/or guarantee date
	 *
	 *  @deprecated
	 */
	public static MStorage[] getWarehouse (Properties ctx, int M_Warehouse_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int M_AttributeSet_ID,
		boolean allAttributeInstances, Timestamp minGuaranteeDate,
		boolean FiFo, String trxName)
	{
		return getWarehouse(ctx, M_Warehouse_ID, M_Product_ID, M_AttributeSetInstance_ID, 
				minGuaranteeDate, FiFo, false, 0, trxName);
	}
	
	/**
	 * 	Get Storage Info for Warehouse or locator
	 *	@param ctx context
	 *	@param M_Warehouse_ID ignore if M_Locator_ID > 0
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID instance id, 0 to retrieve all instance
	 *	@param minGuaranteeDate optional minimum guarantee date if all attribute instances
	 *	@param FiFo first in-first-out
	 *  @param positiveOnly if true, only return storage records with qtyOnHand > 0
	 *  @param M_Locator_ID optional locator id
	 *	@param trxName transaction
	 *	@return existing - ordered by location priority (desc) and/or guarantee date
	 */
	@Deprecated
	public static MStorage[] getWarehouse (Properties ctx, int M_Warehouse_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, Timestamp minGuaranteeDate,
		boolean FiFo, boolean positiveOnly, int M_Locator_ID, String trxName)
	{
		return getWarehouse(ctx, M_Warehouse_ID, M_Product_ID, M_AttributeSetInstance_ID, 0, 
				minGuaranteeDate, FiFo, false, 0, trxName);
//		if ((M_Warehouse_ID == 0 && M_Locator_ID == 0) || M_Product_ID == 0)
//			return new MStorage[0];
//		
//		boolean allAttributeInstances = false;
//		if (M_AttributeSetInstance_ID == 0)
//			allAttributeInstances = true;		
//		
//		ArrayList<MStorage> list = new ArrayList<MStorage>();
//		//	Specific Attribute Set Instance
//		String sql = "SELECT s.M_Product_ID,s.M_Locator_ID,s.M_AttributeSetInstance_ID, s.M_MPolicyTicket_ID,"
//			+ "s.AD_Client_ID,s.AD_Org_ID,s.IsActive,s.Created,s.CreatedBy,s.Updated,s.UpdatedBy,"
//			+ "s.QtyOnHand,s.QtyReserved,s.QtyOrdered,s.DateLastInventory "
//			+ "FROM M_Storage s"
//			+ " INNER JOIN M_Locator l ON (l.M_Locator_ID=s.M_Locator_ID) ";
//		if (M_Locator_ID > 0)
//			sql += "WHERE l.M_Locator_ID = ?";
//		else
//			sql += "WHERE l.M_Warehouse_ID=?";
//		sql += " AND s.M_Product_ID=?"
//			 + " AND COALESCE(s.M_AttributeSetInstance_ID,0)=? ";
//		if (positiveOnly)
//		{
//			sql += " AND s.QtyOnHand > 0 ";
//		}
//		else
//		{
//			sql += " AND s.QtyOnHand <> 0 ";
//		}
//		sql += "ORDER BY l.PriorityNo DESC, M_AttributeSetInstance_ID";
//		if (!FiFo)
//			sql += " DESC";
//		//	All Attribute Set Instances
//		if (allAttributeInstances)
//		{
//			sql = "SELECT s.M_Product_ID,s.M_Locator_ID,s.M_AttributeSetInstance_ID, s.M_MPolicyTicket_ID, "
//				+ "s.AD_Client_ID,s.AD_Org_ID,s.IsActive,s.Created,s.CreatedBy,s.Updated,s.UpdatedBy,"
//				+ "s.QtyOnHand,s.QtyReserved,s.QtyOrdered,s.DateLastInventory "
//				+ "FROM M_Storage s"
//				+ " INNER JOIN M_Locator l ON (l.M_Locator_ID=s.M_Locator_ID)"
//				+ " LEFT OUTER JOIN M_AttributeSetInstance asi ON (s.M_AttributeSetInstance_ID=asi.M_AttributeSetInstance_ID) ";
//			if (M_Locator_ID > 0)
//				sql += "WHERE l.M_Locator_ID = ?";
//			else
//				sql += "WHERE l.M_Warehouse_ID=?";
//			sql += " AND s.M_Product_ID=? ";
//			if (positiveOnly)
//			{
//				sql += " AND s.QtyOnHand > 0 ";
//			}
//			else
//			{
//				sql += " AND s.QtyOnHand <> 0 ";
//			}
//			if (minGuaranteeDate != null)
//			{
//				sql += "AND (asi.GuaranteeDate IS NULL OR asi.GuaranteeDate>?) ";
//				sql += "ORDER BY l.PriorityNo DESC, " +
//					   "asi.GuaranteeDate, M_AttributeSetInstance_ID";
//				if (!FiFo)
//					sql += " DESC";
//				sql += ", s.QtyOnHand DESC";
//			}
//			else
//			{
//				sql += "ORDER BY l.PriorityNo DESC, l.M_Locator_ID, s.M_AttributeSetInstance_ID";
//				if (!FiFo)
//					sql += " DESC";
//				sql += ", s.QtyOnHand DESC";
//			}
//		} 
//		PreparedStatement pstmt = null;
//		ResultSet rs = null;
//		try
//		{
//			pstmt = DB.prepareStatement(sql, trxName);
//			pstmt.setInt(1, M_Locator_ID > 0 ? M_Locator_ID : M_Warehouse_ID);
//			pstmt.setInt(2, M_Product_ID);
//			if (!allAttributeInstances)
//			{
//				pstmt.setInt(3, M_AttributeSetInstance_ID);
//			}
//			else if (minGuaranteeDate != null)
//			{
//				pstmt.setTimestamp(3, minGuaranteeDate);
//			}
//			rs = pstmt.executeQuery();
//			while (rs.next())
//			{	
//				if(rs.getBigDecimal(12).signum() != 0)
//				list.add (new MStorage (ctx, rs, trxName));
//			}	
//		}
//		catch (Exception e)
//		{
//			s_log.log(Level.SEVERE, sql, e);
//		}
//		finally
//		{
//			DB.close(rs, pstmt);
//			rs = null; pstmt = null;
//		}
//		MStorage[] retValue = new MStorage[list.size()];
//		list.toArray(retValue);
//		return retValue;
	}	//	getWarehouse

	/**
	 * 	Get Storage Info for Warehouse or locator
	 *	@param ctx context
	 *	@param M_Warehouse_ID ignore if M_Locator_ID > 0
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID instance id, 0 to retrieve all instance
	 *  @param M_MPolicyTicket_ID FIFO/LIFO ticket ID, 0 to retrieve all available tickets
	 *	@param minGuaranteeDate optional minimum guarantee date if all attribute instances
	 *	@param FiFo first in-first-out
	 *  @param positiveOnly if true, only return storage records with qtyOnHand > 0
	 *  @param M_Locator_ID optional locator id
	 *	@param trxName transaction
	 *	@return existing - ordered by location priority (desc) and/or guarantee date
	 */
	public static MStorage[] getWarehouse (Properties ctx, int M_Warehouse_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int M_MPolicyTicket_ID, 
		Timestamp minGuaranteeDate, boolean FiFo, boolean positiveOnly, 
		int M_Locator_ID, String trxName)
	{
		if ((M_Warehouse_ID == 0 && M_Locator_ID == 0) || M_Product_ID == 0)
			return new MStorage[0];
		
		boolean allTickets = false;
		if (M_MPolicyTicket_ID == 0)
			allTickets = true;		

		ArrayList<MStorage> list = new ArrayList<MStorage>();

		String sql = "SELECT s.M_Product_ID,s.M_Locator_ID,s.M_AttributeSetInstance_ID,s.M_MPolicyTicket_ID,"
			+ "s.AD_Client_ID,s.AD_Org_ID,s.IsActive,s.Created,s.CreatedBy,s.Updated,s.UpdatedBy,"
			+ "s.QtyOnHand,s.QtyReserved,s.QtyOrdered,s.DateLastInventory "
			+ "FROM M_Storage s"
			+ " INNER JOIN M_Locator l ON (l.M_Locator_ID=s.M_Locator_ID)"
			+ " INNER JOIN M_MPolicyTicket p ON (s.M_MPolicyTicket_ID=p.M_MPolicyTicket_ID)"
			+ " LEFT OUTER JOIN M_AttributeSetInstance asi ON (s.M_AttributeSetInstance_ID=asi.M_AttributeSetInstance_ID)"
			+ " WHERE s.M_Product_ID=?"
			+ " AND s.M_AttributeSetInstance_ID=?";
		
		if (M_Locator_ID > 0)
			sql += " AND l.M_Locator_ID = ?";
		else
			sql += " AND l.M_Warehouse_ID=?";

		if (minGuaranteeDate != null) {
			sql += " AND (asi.GuaranteeDate IS NULL OR asi.GuaranteeDate>?)";
		}

		if (!allTickets)
			sql += " AND COALESCE(s.M_MPolicyTicket_ID,0)=?";

		if (positiveOnly) {
			sql += " AND s.QtyOnHand > 0";
		}
		else {
			sql += " AND s.QtyOnHand <> 0";
		}
		
		// Order By
		sql += " ORDER BY l.PriorityNo DESC, asi.GuaranteeDate";
		if (allTickets) {
			sql += ", COALESCE(p.MovementDate,to_timestamp(0.0))";			
			if (!FiFo)
				sql += " DESC";
		}
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, trxName);
			int i = 1;
			pstmt.setInt(i++, M_Product_ID);
			pstmt.setInt(i++, M_AttributeSetInstance_ID);
			pstmt.setInt(i++, M_Locator_ID > 0 ? M_Locator_ID : M_Warehouse_ID);
			if (minGuaranteeDate != null)
			{
				pstmt.setTimestamp(i++, minGuaranteeDate);
			}
			if (!allTickets)
			{
				pstmt.setInt(i++, M_AttributeSetInstance_ID);
			}
			rs = pstmt.executeQuery();
			while (rs.next())
			{	
				if(rs.getBigDecimal(12).signum() != 0)
				list.add (new MStorage (ctx, rs, trxName));
			}	
		}
		catch (Exception e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		MStorage[] retValue = new MStorage[list.size()];
		list.toArray(retValue);
		return retValue;
	}	//	getWarehouse

	
	/**
	 * 	Create or Get Storage Info
	 *	@param ctx context
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID instance
	 *	@param trxName transaction
	 *	@return existing/new or null
	 */
	@Deprecated
	public static MStorage getCreate (Properties ctx, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, String trxName)
	{
		return getCreate(ctx, M_Locator_ID, M_Product_ID, M_AttributeSetInstance_ID, 0, trxName);
	}
	
	/**
	 * 	Create or Get Storage Info.  Throws an IllegalArgumentException if the parameters are invalid.  
	 *  Throws an AdempiereException if the storage location isn't found and can't be created.
	 *	@param ctx context
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID instance
	 *	@param trxName transaction
	 *	@return existing/new. Guaranteed not null.
	 */
	public static MStorage getCreate (Properties ctx, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int M_MPolicyTicket_ID, String trxName)
	{
		if (M_Locator_ID <= 0)
			throw new IllegalArgumentException("M_Locator_ID<=0");
		if (M_Product_ID <= 0)
			throw new IllegalArgumentException("M_Product_ID<=0");
		if (M_AttributeSetInstance_ID < 0)
			throw new IllegalArgumentException("M_AttributeSetInstance_ID<0");

		if (M_MPolicyTicket_ID <= 0)
			throw new IllegalArgumentException("M_MPolicyTicket_ID<=0");

		MStorage storage = get(ctx, M_Locator_ID, M_Product_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID, trxName);
		if (storage != null) {
			s_log.fine("Existing " + storage);
		}
		else {
		
			// Try to create the storage
			
			//	Test for the existence of the parameters
			MLocator locator = new MLocator (ctx, M_Locator_ID, trxName);
			if (locator.get_ID() != M_Locator_ID)
				throw new IllegalArgumentException("Not found M_Locator_ID=" + M_Locator_ID);
	
			MProduct product = new MProduct (ctx, M_Product_ID, trxName);
			if (product.get_ID() != M_Product_ID)
				throw new IllegalArgumentException("Not found M_Product_ID=" + M_Product_ID);
	
			MAttributeSetInstance asi = new MAttributeSetInstance (ctx, M_AttributeSetInstance_ID, trxName);
			if (asi.get_ID() != M_AttributeSetInstance_ID || asi.getM_AttributeSet() == null)
				throw new IllegalArgumentException("Not found M_AttributeSetInstance_ID=" + M_AttributeSetInstance_ID);
	
			//
			storage = new MStorage (ctx, locator, M_Product_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID, trxName);
			if (storage != null) {
				storage.saveEx();
				s_log.fine("New " + storage);
			}
		}
		//	Verify
		if (storage==null || storage.getM_Locator_ID() != M_Locator_ID 
			|| storage.getM_Product_ID() != M_Product_ID
			|| storage.getM_AttributeSetInstance_ID() != M_AttributeSetInstance_ID
			|| storage.getM_MPolicyTicket_ID() != M_MPolicyTicket_ID)
		{
			String errorMsg = "No Storage found/created for - M_Locator_ID=" + M_Locator_ID 
					+ ",M_Product_ID=" + M_Product_ID + ",ASI=" + M_AttributeSetInstance_ID
					+ ", M_MPolicyTicket_ID=" + M_MPolicyTicket_ID;
			s_log.severe (errorMsg);
			throw new AdempiereException(errorMsg);
		}

		return storage;
	}	//	getCreate

	
	/**
	 * 	Update Storage Info add.
	 *	@param ctx context
	 *	@param M_Warehouse_ID warehouse
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID AS Instance
	 *	@param reservationAttributeSetInstance_ID reservation AS Instance
	 *	@param diffQtyOnHand add on hand
	 *	@param diffQtyReserved add reserved
	 *	@param diffQtyOrdered add order
	 *	@param trxName transaction
	 *	@return true if updated
	 */
	@Deprecated
	public static boolean add (Properties ctx, int M_Warehouse_ID, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int reservationAttributeSetInstance_ID,
		BigDecimal diffQtyOnHand, 
		BigDecimal diffQtyReserved, BigDecimal diffQtyOrdered, String trxName)
	{
		return add(ctx, M_Warehouse_ID, M_Locator_ID, 
				M_Product_ID, M_AttributeSetInstance_ID, reservationAttributeSetInstance_ID,
				0, // M_MPolicyTicket_ID
				diffQtyOnHand, diffQtyReserved, diffQtyOrdered, trxName);
	}

	/**
	 * 	Update Storage Info by adding the difference quantity to the storage location. 
	 *	@param ctx context
	 *	@param M_Warehouse_ID warehouse
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID AS Instance
	 *	@param reservationAttributeSetInstance_ID reservation AS Instance
	 *  @param M_MPolicyTicket_ID Material Policy Ticket
	 *	@param diffQtyOnHand add on hand
	 *	@param diffQtyReserved add reserved
	 *	@param diffQtyOrdered add order
	 *	@param trxName transaction
	 *	@return true if updated
	 */
	public static boolean add (Properties ctx, int M_Warehouse_ID, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int reservationAttributeSetInstance_ID,
		int M_MPolicyTicket_ID,
		BigDecimal diffQtyOnHand, 
		BigDecimal diffQtyReserved, BigDecimal diffQtyOrdered, String trxName)
	{
		return add( ctx,  M_Warehouse_ID,  M_Locator_ID, 
		 M_Product_ID,  M_AttributeSetInstance_ID,  reservationAttributeSetInstance_ID,
		 M_MPolicyTicket_ID,  M_MPolicyTicket_ID,
		 diffQtyOnHand, 
		 diffQtyReserved,  diffQtyOrdered,  trxName);
	}
	/**
	 * 	Update Storage Info by adding the difference quantity to the storage location. 
	 *	@param ctx context
	 *	@param M_Warehouse_ID warehouse
	 *	@param M_Locator_ID locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID AS Instance
	 *	@param reservationAttributeSetInstance_ID reservation AS Instance
	 *  @param M_MPolicyTicket_ID Material Policy Ticket
	 *  @param reservationMPolicyTicket_ID the ticket used to reserve/order stock
	 *	@param diffQtyOnHand add on hand
	 *	@param diffQtyReserved add reserved
	 *	@param diffQtyOrdered add order
	 *	@param trxName transaction
	 *	@return true if updated
	 */
	public static boolean add (Properties ctx, int M_Warehouse_ID, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int reservationAttributeSetInstance_ID,
		int M_MPolicyTicket_ID, int reservationMPolicyTicket_ID,
		BigDecimal diffQtyOnHand, 
		BigDecimal diffQtyReserved, BigDecimal diffQtyOrdered, String trxName)
	{
		if (diffQtyOnHand == null) 
			diffQtyOnHand = Env.ZERO; 
		if (diffQtyReserved == null) 
			diffQtyReserved = Env.ZERO; 
		if (diffQtyOrdered == null) 
			diffQtyOrdered = Env.ZERO;
		
		if ( diffQtyOnHand.signum() != 0 && M_MPolicyTicket_ID <= 0 ) {
			s_log.severe("Can't change Quantity On Hand witout a valid Matieral Policy Ticket");
			return false;			
		}

		if ( (diffQtyReserved.signum() != 0 || diffQtyOrdered.signum() !=0) && reservationMPolicyTicket_ID <= 0 ) {
			s_log.severe("Can't change Quantity reserved/ordered witout a valid reservation Matieral Policy Ticket");
			return false;			
		}

		StringBuffer diffText = new StringBuffer("");
		boolean changed = false;

		// CarlosRuiz - globalqss - Fix [ 1725383 ] QtyOrdered wrongly updated
		// If the product has no Attribute Set, it can't have any Attribute Set Instances
		MProduct product = new MProduct(ctx, M_Product_ID, trxName);
		if (product.getM_AttributeSet_ID() == 0) {
			M_AttributeSetInstance_ID = 0;
			reservationAttributeSetInstance_ID = 0;
		}

		//	Get Storage
		MStorage storage = null;
		if (diffQtyOnHand.signum() != 0) {
			try {
				storage = getCreate (ctx, M_Locator_ID, 
					M_Product_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID, trxName);
			}
			catch(AdempiereException | IllegalArgumentException e){
				s_log.severe(e.getLocalizedMessage());
				return false;
			}
			storage.setQtyOnHand (storage.getQtyOnHand().add (diffQtyOnHand));
			storage.saveEx();
			diffText.append(" (OnHand=").append(diffQtyOnHand);
			diffText.append(") -> ").append(storage.toString());
			changed = true;
		}
		
		// To correct the qty reserved and ordered, we need to know the product, ASI, warehouse and 
		// material policy ticket used to make the reservation/order.  The reservation material policy ticket 
		// will be different than the tickets used to fulfill the order so the reservations/orders 
		// will always be made on a different M_Storage record than the material receipt.
		// The locator will always be the default locator for the warehouse.
		MStorage storage0 = null;
		if(diffQtyReserved.signum() != 0 || diffQtyOrdered.signum() != 0) {
			try {
				storage0 = getReservedOrdered(ctx,
						M_Product_ID, M_Warehouse_ID , reservationAttributeSetInstance_ID, reservationMPolicyTicket_ID, trxName);
			}
			catch(AdempiereException | IllegalArgumentException e){
				s_log.severe(e.getLocalizedMessage());
				return false;
			}
			storage0.setQtyReserved(storage0.getQtyReserved().add(diffQtyReserved));
			storage0.setQtyOrdered (storage0.getQtyOrdered().add (diffQtyOrdered));
			storage0.saveEx();
			diffText.append(" (Reserved=").append(diffQtyReserved);
			diffText.append(" Ordered=").append(diffQtyOrdered);
			diffText.append(") -> ").append(storage0.toString());
			changed = true;
		}

		if (changed) {
			s_log.fine(diffText.toString());
		}
		
		return true;
	}	//	add

	
	/**************************************************************************
	 * 	Get Location with highest Locator Priority and a sufficient OnHand Qty.
	 *  The search tries to match Product and Attribute Set Instance.
	 * 	@param M_Warehouse_ID warehouse
	 * 	@param M_Product_ID product
	 * 	@param M_AttributeSetInstance_ID asi
	 * 	@param Qty qty
	 *	@param trxName transaction
	 * 	@return id
	 */
	@Deprecated
	public static int getM_Locator_ID (int M_Warehouse_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, BigDecimal Qty,
		String trxName)
	{
		// Get the locator with the "zero" material policy ticket.
		return getM_Locator_ID (M_Warehouse_ID, M_Product_ID, M_AttributeSetInstance_ID, 
				0, Qty, trxName);
	}

		/**************************************************************************
	 * 	Get Location with highest Locator Priority and a sufficient OnHand Qty.
	 *  The search tries to match Product, Attribute Set Instance (ASI) and Material Policy
	 *  Ticket.  If the ASI is zero, the product does not have an Attribute Set or the 
	 *  product's Attribute Set has no instances, the quantity for all ASI values is used.  
	 *  Similarly for the Ticket, if it is zero, the quantity of all tickets is used. 
	 * 	@param M_Warehouse_ID warehouse
	 * 	@param M_Product_ID product
	 * 	@param M_AttributeSetInstance_ID asi
	 *  @param M_MPolicyTicket_ID ticket
	 * 	@param Qty qty
	 *	@param trxName transaction
	 * 	@return id
	 */
	public static int getM_Locator_ID (int M_Warehouse_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, int M_MPolicyTicket_ID, BigDecimal Qty,
		String trxName)
	{
		if ( M_Product_ID == 0)
			return 0;
		
		int M_Locator_ID = 0;
		int firstM_Locator_ID = 0;
		String sql = "SELECT s.M_Locator_ID, SUM(s.QtyOnHand) as QtyOnHand "
			+ "FROM M_Storage s"
			+ " INNER JOIN M_Locator l ON (s.M_Locator_ID=l.M_Locator_ID)"
			+ " INNER JOIN M_Product p ON (s.M_Product_ID=p.M_Product_ID)"
			+ " LEFT OUTER JOIN M_AttributeSet mas ON (p.M_AttributeSet_ID=mas.M_AttributeSet_ID) "
			+ "WHERE l.M_Warehouse_ID=?"
			+ " AND s.M_Product_ID=?";
		if (M_AttributeSetInstance_ID != 0)
			sql += " AND (mas.IsInstanceAttribute IS NULL OR mas.IsInstanceAttribute='N' OR s.M_AttributeSetInstance_ID=?)";  // TODO does this work for product ASI?
		if (M_MPolicyTicket_ID != 0)
			sql += " AND s.M_MPolicyTicket_ID=?";
		sql	+= " AND l.IsActive='Y' "
			+ "GROUP BY l.PriorityNo, s.M_Locator_ID " // Groupby required to sum across all Material Policy Tickets
			+ "ORDER BY l.PriorityNo DESC, SUM(s.QtyOnHand) DESC";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			int i = 1;
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(i++, M_Warehouse_ID);
			pstmt.setInt(i++, M_Product_ID);
			if (M_AttributeSetInstance_ID != 0)  //TODO is this the right logic? !=0 or >= 0?  ASI can be valid zero, -1 is undefined
				pstmt.setInt(i++, M_AttributeSetInstance_ID);
			if (M_MPolicyTicket_ID != 0) 
				pstmt.setInt(i++, M_MPolicyTicket_ID);
			rs = pstmt.executeQuery();
			while (rs.next())
			{
				BigDecimal QtyOnHand = rs.getBigDecimal(2);
				if (QtyOnHand != null && Qty.compareTo(QtyOnHand) <= 0)
				{
					M_Locator_ID = rs.getInt(1);
					break;
				}
				if (firstM_Locator_ID == 0)
					firstM_Locator_ID = rs.getInt(1);
			}
		}
		catch (SQLException ex)
		{
			s_log.log(Level.SEVERE, sql, ex);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		if (M_Locator_ID != 0)
			return M_Locator_ID;
		return firstM_Locator_ID;
	}	//	getM_Locator_ID

	/**
	 * 	Get Available Qty.
	 * 	The call is accurate only if there is a storage record 
	 * 	and assumes that the product is stocked 
	 *	@param M_Warehouse_ID wh
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID masi
	 *	@param trxName transaction
	 *	@return qty available (QtyOnHand-QtyReserved) or null
	 * @deprecated Since 331b. Please use {@link #getQtyAvailable(int, int, int, int, String)}.
	 */
	public static BigDecimal getQtyAvailable (int M_Warehouse_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, String trxName)
	{
		return getQtyAvailable(M_Warehouse_ID, 0, M_Product_ID, M_AttributeSetInstance_ID, trxName);
	}
	
	/**
	 * Get Warehouse/Locator Available Qty.
	 * The call is accurate only if there is a storage record 
	 * and assumes that the product is stocked 
	 * @param M_Warehouse_ID wh (if the M_Locator_ID!=0 then M_Warehouse_ID is ignored)
	 * @param M_Locator_ID locator (if 0, the whole warehouse will be evaluated)
	 * @param M_Product_ID product
	 * @param M_AttributeSetInstance_ID masi
	 * @param trxName transaction
	 * @return qty available (QtyOnHand-QtyReserved) or null if error
	 */
	public static BigDecimal getQtyAvailable (int M_Warehouse_ID, int M_Locator_ID, 
		int M_Product_ID, int M_AttributeSetInstance_ID, String trxName)
	{
		ArrayList<Object> params = new ArrayList<Object>();
		StringBuffer sql = new StringBuffer("SELECT COALESCE(SUM(s.QtyOnHand-s.QtyReserved),0)")
								.append(" FROM M_Storage s")
								.append(" WHERE AD_Client_ID = ?")
								.append(" AND s.M_Product_ID=?");
		params.add(Env.getAD_Client_ID(Env.getCtx()));
		params.add(M_Product_ID);
		
		if (M_Warehouse_ID != 0) {
			// Warehouse level
			if (M_Locator_ID == 0) {
				sql.append(" AND EXISTS (SELECT 1 FROM M_Locator l WHERE s.M_Locator_ID=l.M_Locator_ID AND l.M_Warehouse_ID=?)");
				params.add(M_Warehouse_ID);
			}
			// Locator level
			else {
				sql.append(" AND s.M_Locator_ID=?");
				params.add(M_Locator_ID);
			}
		}
		// With ASI
		if (M_AttributeSetInstance_ID != 0) {
			sql.append(" AND s.M_AttributeSetInstance_ID=?");
			params.add(M_AttributeSetInstance_ID);
		}
		//
		BigDecimal retValue = DB.getSQLValueBD(trxName, sql.toString(), params);
		if (CLogMgt.isLevelFine())
			s_log.fine("M_Warehouse_ID=" + M_Warehouse_ID + ", M_Locator_ID=" + M_Locator_ID 
				+ ",M_Product_ID=" + M_Product_ID + " = " + retValue);
		return retValue;
	}	//	getQtyAvailable
	
	
	/**************************************************************************
	 * 	Persistency Constructor
	 *	@param ctx context
	 *	@param ignored ignored
	 *	@param trxName transaction
	 */
	public MStorage (Properties ctx, int ignored, String trxName)
	{
		super(ctx, 0, trxName);
		if (ignored != 0)
			throw new IllegalArgumentException("Multi-Key");
		//
		setQtyOnHand (Env.ZERO);
		setQtyOrdered (Env.ZERO);
		setQtyReserved (Env.ZERO);
	}	//	MStorage

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MStorage (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MStorage

	/**
	 * 	Full NEW Constructor. Creates an empty storage location for this product/ASI. No quantity
	 *  or Material Policy Ticket is assigned.
	 *	@param locator (parent) locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID attribute
	 */
	@Deprecated
	private MStorage (MLocator locator, int M_Product_ID, int M_AttributeSetInstance_ID)
	{
		this (locator.getCtx(), 0, locator.get_TrxName());
		setClientOrg(locator);
		setM_Locator_ID (locator.getM_Locator_ID());
		setM_Product_ID (M_Product_ID);
		setM_AttributeSetInstance_ID (M_AttributeSetInstance_ID);
		setM_MPolicyTicket_ID(0);
	}	//	MStorage

	/**
	 * 	Full NEW Constructor. Creates an empty storage location for this product/ASI. No quantity
	 *  is assigned.
	 *	@param locator (parent) locator
	 *	@param M_Product_ID product
	 *	@param M_AttributeSetInstance_ID attribute
	 *  @param M_MPolicyTicket_ID Material Policy Ticket
	 */
	private MStorage (Properties ctx, MLocator locator, int M_Product_ID, int M_AttributeSetInstance_ID, int M_MPolicyTicket_ID, String trxName)
	{
		this (ctx, 0, trxName);
		setClientOrg(locator);
		setM_Locator_ID (locator.getM_Locator_ID());
		setM_Product_ID (M_Product_ID);
		setM_AttributeSetInstance_ID (M_AttributeSetInstance_ID);
		setM_MPolicyTicket_ID(M_MPolicyTicket_ID);
	}	//	MStorage

	/** Log								*/
	private static CLogger		s_log = CLogger.getCLogger (MStorage.class);
	/** Warehouse						*/
	private int		m_M_Warehouse_ID = 0;
	
	/**
	 * 	Change Qty OnHand
	 *	@param qty quantity
	 *	@param add add if true 
	 */
	public void changeQtyOnHand (BigDecimal qty, boolean add)
	{
		if (qty == null || qty.signum() == 0)
			return;
		if (add)
			setQtyOnHand(getQtyOnHand().add(qty));
		else
			setQtyOnHand(getQtyOnHand().subtract(qty));
	}	//	changeQtyOnHand

	/**
	 * 	Get M_Warehouse_ID of Locator
	 *	@return warehouse
	 */
	public int getM_Warehouse_ID()
	{
		if (m_M_Warehouse_ID == 0)
		{
			MLocator loc = MLocator.get(getCtx(), getM_Locator_ID());
			m_M_Warehouse_ID = loc.getM_Warehouse_ID();
		}
		return m_M_Warehouse_ID;
	}	//	getM_Warehouse_ID
	
	/**
	 *	String Representation
	 * 	@return info
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer("MStorage[")
			.append("M_Locator_ID=").append(getM_Locator_ID())
				.append(",M_Product_ID=").append(getM_Product_ID())
				.append(",M_AttributeSetInstance_ID=").append(getM_AttributeSetInstance_ID())
				.append(",M_MPolicyTicket_ID=").append(getM_MPolicyTicket_ID())
			.append(": OnHand=").append(getQtyOnHand())
			.append(",Reserved=").append(getQtyReserved())
			.append(",Ordered=").append(getQtyOrdered())
			.append("]");
		return sb.toString();
	}	//	toString

	/**
	 * Returns the quantity ordered for a product/ASI across all locators in a warehouse
	 * @param ctx
	 * @param m_product_id
	 * @param m_warehouse_id
	 * @param m_attributeSetInstance_id
	 * @param trxName
	 * @return
	 */
	public static BigDecimal getOrderedQty(Properties ctx, int m_product_id,
			int m_warehouse_id, int m_attributeSetInstance_id, String trxName) {
	
		ArrayList<Object> params = new ArrayList<Object>();
		StringBuffer sql = new StringBuffer("SELECT COALESCE(SUM(s.QtyOrdered),0)")
								.append(" FROM M_Storage s")
								.append(" WHERE AD_Client_ID = ?")
								.append(" AND s.M_Product_ID=?")
								.append(" AND EXISTS (SELECT 1 FROM M_Locator l WHERE s.M_Locator_ID=l.M_Locator_ID AND l.M_Warehouse_ID=?)")
								.append(" AND s.M_AttributeSetInstance_ID=?");
		params.add(Env.getAD_Client_ID(Env.getCtx()));
		params.add(m_product_id);
		params.add(m_warehouse_id);
		params.add(m_attributeSetInstance_id);
		//
		BigDecimal retValue = DB.getSQLValueBD(trxName, sql.toString(), params);
		if (CLogMgt.isLevelFine())
			s_log.fine("M_Warehouse_ID=" + m_warehouse_id + 
				",M_Product_ID=" + m_product_id + ", M_AttributeSetInstance_ID=" + m_attributeSetInstance_id + " = " + retValue);
		return retValue;
	}	//	getOrderedQty

	/**
	 * Returns the quantity reserved for a product/ASI across all locators in a warehouse
	 * @param ctx
	 * @param m_product_id
	 * @param m_warehouse_id
	 * @param m_attributeSetInstance_id
	 * @param trxName
	 * @return BigDecimal quantity reserved
	 */
	public static BigDecimal getReservedQty(Properties ctx, int m_product_id,
			int m_warehouse_id, int m_attributeSetInstance_id, String trxName) {
	
		ArrayList<Object> params = new ArrayList<Object>();
		StringBuffer sql = new StringBuffer("SELECT COALESCE(SUM(s.QtyReserved),0)")
								.append(" FROM M_Storage s")
								.append(" WHERE AD_Client_ID = ?")
								.append(" AND s.M_Product_ID=?")
								.append(" AND EXISTS (SELECT 1 FROM M_Locator l WHERE s.M_Locator_ID=l.M_Locator_ID AND l.M_Warehouse_ID=?)")
								.append(" AND s.M_AttributeSetInstance_ID=?");
		params.add(Env.getAD_Client_ID(Env.getCtx()));
		params.add(m_product_id);
		params.add(m_warehouse_id);
		params.add(m_attributeSetInstance_id);
		//
		BigDecimal retValue = DB.getSQLValueBD(trxName, sql.toString(), params);
		if (CLogMgt.isLevelFine())
			s_log.fine("M_Warehouse_ID=" + m_warehouse_id + 
				",M_Product_ID=" + m_product_id + ", M_AttributeSetInstance_ID=" + m_attributeSetInstance_id + " = " + retValue);
		return retValue;
	}	//	getOrderedQty

	/**
	 * kviiksaar- taken from CalloutInventory
	 * 
	 * Returns the quantity on hand (current Book Qty) for given product/ASI at a particular locator.  
	 * This is different than the quantity available to promise which is qtyOnHand - qtyReserved.
	 * 
	 * @param ctx Context
	 * @param m_attributeSetInstance_id
	 * @param m_product_id
	 * @param m_locator_id
	 * @param trxName Transaction Name
	 * @return BigDecimal quantity on hand.
	 */
	public static BigDecimal getQtyOnHand(Properties ctx, int m_product_id,
			int m_attributeSetInstance_id, int m_locator_id,
			String trxName) {

		// Get the quantity on hand from storage location

		BigDecimal qtyOnHand = new BigDecimal(0);

		// Need to sum across all material policy tickets.
		String sql = "SELECT SUM(QtyOnHand) FROM M_Storage "
			+ "WHERE M_Product_ID=?"	//	1
			+ " AND M_Locator_ID=?"		//	2
			+ " AND M_AttributeSetInstance_ID=?"
			+ " AND AD_Client_ID = ?";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, m_product_id);
			pstmt.setInt(2, m_locator_id);
			pstmt.setInt(3, m_attributeSetInstance_id);
			pstmt.setInt(4, Env.getAD_Client_ID(ctx));
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				qtyOnHand = rs.getBigDecimal(1);
			} 
		}
		catch (SQLException e)
		{
			s_log.log(Level.SEVERE, sql, e);
		}
		finally {
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		
		if (qtyOnHand==null)
			qtyOnHand = Env.ZERO;

		return qtyOnHand;
	}

}	//	MStorage
