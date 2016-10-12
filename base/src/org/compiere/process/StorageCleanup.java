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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.logging.Level;

import org.adempiere.engine.StorageEngine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MLocator;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.model.MStorage;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 	StorageCleanup
 *	
 *  @author Jorg Janke
 *  @version $Id: StorageCleanup.java,v 1.2 2006/07/30 00:51:02 jjanke Exp $
 */
public class StorageCleanup extends SvrProcess
{
	/** Movement Document Type	*/
	private int	p_C_DocType_ID = 0;
	private MMovement mMovement = null;
	
	/**
	 *  Prepare - e.g., get Parameters.
	 */
	protected void prepare()
	{
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("C_DocType_ID"))
				p_C_DocType_ID = para[i].getParameterAsInt();
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
		}
	}	//	prepare

	/**
	 * 	Process
	 *	@return message
	 *	@throws Exception
	 */
	protected String doIt () throws Exception
	{
		log.info("");
		removeMalformedASIValues();
		ensureQtyOnHandHasTicket();
		ensureQtyOrderedReservedHasTickets();
		coverNegativeQuantities();
		removeEmptyStorage();
		return "Storage clean up completed.";
	}	//	doIt

	private void ensureQtyOrderedReservedHasTickets() {

		// Remove the QtyOrdered/QtyReserved where there are no material policy tickets - there is a possible
		// corruption with matchPO that will need to be corrected.
		// All storage records with QtyOnHand > 0 should have tickets. See ensureQtyOnHandHasTicket();
		String sql = "DELETE FROM M_Storage "
			+ "WHERE QtyOnHand=0 AND (QtyReserved!=0 OR QtyOrdered!=0)"
			+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
			+ " AND COALESCE(M_MPolicyTicket_ID,0)=0";
		int no = DB.executeUpdate(sql, get_TrxName());
		log.info("Delete QtyReserved + QtyOrdered with no M_MPolicyTicket_ID: " + no);

		// Assume all SO lines have the correct qty Delivered
		// Update all PO orderlines to have the correct qty Delivered.  Fixes a historical issue where qty delivered was not used.
		sql = "UPDATE C_OrderLine ol"
				 + " SET QtyDelivered = (SELECT SUM(Qty) FROM M_MatchPO mpo WHERE mpo.C_OrderLine_ID = ol.C_OrderLine_ID),"
				 + "     DateDelivered = (SELECT MAX(DateTrx) FROM M_MatchPO mpo WHERE mpo.C_OrderLine_ID = ol.C_OrderLine_ID)"
				 + " WHERE ol.C_Order_ID = (SELECT C_Order_ID FROM C_Order o "
				 + "                           WHERE o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('CO')"
				 + "                           AND o.isSOTrx = 'N')"
				 + " AND ol.QtyDelivered < ol.QtyOrdered"
				 + " AND COALESCE(ol.M_MPolicyTicket_ID,0)=0"
				 + " AND ol.QtyDelivered < (SELECT SUM(Qty) FROM M_MatchPO mpo WHERE mpo.C_OrderLine_ID = ol.C_OrderLine_ID)"
				 + " AND ol.AD_Client_ID = " + Env.getAD_Client_ID(getCtx());
		no = DB.executeUpdate(sql, get_TrxName());
		log.info("Updated QtyDelivered on purchase orders: " + no);
		 
		
		// Get all the order lines that have zero MPolicyTicket and set the storage qtyReserved
		sql = "SELECT ol.* FROM C_OrderLine ol "
				+ " JOIN M_Product p ON (p.M_Product_ID = ol.M_Product_ID AND p.isStocked='Y')"
				+ " JOIN C_Order o ON (o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('IP','CO'))"
				+ " WHERE o.AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ "	AND ABS(ol.QtyDelivered) < ABS(ol.QtyOrdered)"
				+ " 	AND COALESCE(M_MPolicyTicket_ID,0)=0";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int lines = 0;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MOrderLine line = new MOrderLine(getCtx(), rs, get_TrxName());
				try {
					StorageEngine.createTransaction(line,
							"" , 							// No movement type
							line.getDateOrdered() , 
							Env.ZERO, 						// No movement qty
							false , 						// No reversals
							line.getM_Warehouse_ID(), 
							line.getM_AttributeSetInstance_ID(),
							line.getM_Warehouse_ID(), 
							line.isSOTrx());
				}
				catch (AdempiereException e) {
					log.severe("Unable to reserve/order stock: " + e.getLocalizedMessage());
				}
				lines++;
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

		log.info("Added tickets for ordere/reserved qty for order lines: " + lines);
		
		sql = "SELECT * FROM M_Storage "
				+ "WHERE AD_Client_ID = ?"
				+ " AND QtyOnHand != 0 AND (QtyReserved != 0 OR QtyOrdered != 0)";
		pstmt = null;
		rs = null;
		no = 0;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				eliminateReservation(new MStorage(getCtx(), rs, get_TrxName()));
				no++;
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
		
		log.info("Corrected Reservation lines: " + no);
		
		// Get all the incomplete order lines that have a MPolicyTicket that is not in MStorage and
		// Add the reservation/ordered amounts
		sql = "SELECT ol.* FROM C_OrderLine ol "
				+ " JOIN M_Product p ON (p.M_Product_ID = ol.M_Product_ID AND p.isStocked='Y')"
				+ " JOIN C_Order o ON (o.C_Order_ID=ol.C_Order_ID AND o.docstatus in ('IP','CO'))"
				+ " LEFT OUTER JOIN M_Storage s ON (ol.M_MPolicyTicket_ID = s.M_MPolicyTicket_ID)"
				+ " WHERE o.AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ "	AND ABS(ol.QtyDelivered) < ABS(ol.QtyOrdered)"
				+ " 	AND s.M_MPolicyTicket_ID is null";
		pstmt = null;
		rs = null;
		lines = 0;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				MOrderLine line = new MOrderLine(getCtx(), rs, get_TrxName());
				try {
					StorageEngine.createTransaction(line,
							"" , 							// No movement type
							line.getDateOrdered() , 
							Env.ZERO, 						// No movement qty
							false , 						// No reversals
							line.getM_Warehouse_ID(), 
							line.getM_AttributeSetInstance_ID(),
							line.getM_Warehouse_ID(), 
							line.isSOTrx());
				}
				catch (AdempiereException e) {
					log.severe("Unable to reserve/order stock: " + e.getLocalizedMessage());
				}
				lines++;
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

		log.info("Added tickets for ordere/reserved qty for order lines: " + lines);


	}

	private void coverNegativeQuantities() {
		// for each locator/product/asi combination.
		String sql = "SELECT s.M_Product_ID, s.M_Locator_ID, s.M_AttributeSetInstance_ID "
			+ "FROM M_Storage s "
			+ "WHERE AD_Client_ID = ?"
			+ " AND QtyOnHand < 0"
			//	Instance Attribute
		//	+ " AND EXISTS (SELECT * FROM M_Product p"
		//		+ " INNER JOIN M_AttributeSet mas ON (p.M_AttributeSet_ID=mas.M_AttributeSet_ID) "
		//		+ "WHERE s.M_Product_ID=p.M_Product_ID AND mas.IsInstanceAttribute='Y')"
			//	Stock in same location
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
				+ " AND sl.M_Warehouse_ID=swl.M_Warehouse_ID))"
			+ " GROUP BY s.M_Product_ID, s.M_Locator_ID, s.M_AttributeSetInstance_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		int lines = 0;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				lines += move (rs.getInt(1), rs.getInt(2), rs.getInt(3));
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
	}

	private void ensureQtyOnHandHasTicket() {
		log.info("");
		// Move/change qty on hand where M_MPolicyTicket_ID = 0 to a row with a policy ticket.
		// As these are likely old entries, use a zero Timestamp as the move date.
		String sql = "SELECT * FROM M_Storage "
				+ "WHERE AD_Client_ID = ?"
				+ " AND QtyOnHand != 0 AND COALESCE(M_MPolicyTicket_ID,0) = 0";
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
				no += addMissingPolicyTicket(new MStorage(getCtx(), rs, get_TrxName()));
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
		
		log.info("Added missing policy tickets where qtyOnHand != 0: " + no);
	}

	private void removeMalformedASIValues() {
		log.info("");
		//  Remove ASI values that have no attribute sets. ASI was replaced by the Material Policy Ticket as the
		//  Method of FIFO/LIFO tracking in storage
		String sql = "UPDATE M_Storage s "
				+ "SET M_AttributeSetInstance_ID = 0 "
				+ "WHERE s.M_AttributeSetInstance_ID != 0"
				+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND EXISTS (SELECT 1 FROM M_AttributeSetInstance asi WHERE"
				+ " asi.M_AttributeSet_ID=0"
				+ " AND asi.M_AttributeSetInstance_ID = s.M_AttributeSetInstance_ID)";
		int no = DB.executeUpdate(sql, get_TrxName());
		log.info("Set ASI values to zero where there was no Attribute Set #" + no);	
		
		sql = "UPDATE M_Storage s "
				+ "SET M_AttributeSetInstance_ID = 0 "
				+ "WHERE s.M_AttributeSetInstance_ID != 0"
				+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND NOT EXISTS (SELECT 1 FROM M_AttributeInstance ai WHERE"
				+ " ai.M_AttributeSetInstance_ID = s.M_AttributeSetInstance_ID)";
		no = DB.executeUpdate(sql, get_TrxName());
		log.info("Set ASI values to zero where there were no Attribute values #" + no);	

	}

	private void removeEmptyStorage() {
		log.info("");
		String sql = "DELETE FROM M_Storage "
				+ "WHERE QtyOnHand = 0 AND QtyReserved = 0 AND QtyOrdered = 0"
				+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND Created < SysDate-3";
			int no = DB.executeUpdate(sql, get_TrxName());
			log.info("Delete Empty #" + no);		
	}

	private int addMissingPolicyTicket(MStorage mStorage) {
		// Add a policy ticket to the storage location.  Need to create a new storage location to do this
		// as the ticket is part of the key. mStorage will be deleted.
		if (mStorage.getQtyOnHand().compareTo(Env.ZERO) == 0)
			return 0;
		
		// Create a new policy ticket (assume its old) to the storage record and set qtyOnHand from mStorage and
		// qtyOrdered and qtyReserved to zero.  These last two will be corrected elsewhere
		MMPolicyTicket ticket = MMPolicyTicket.create(getCtx(), null, Timestamp.from(Instant.EPOCH), get_TrxName());

		MStorage newStorage = MStorage.getCreate(getCtx(), 
				mStorage.getM_Locator_ID(), 
				mStorage.getM_Product_ID(), 
				mStorage.getM_AttributeSetInstance_ID(), 
				ticket.getM_MPolicyTicket_ID(), 
				get_TrxName());
		newStorage.setQtyOnHand(mStorage.getQtyOnHand());
		newStorage.setQtyOrdered(Env.ZERO);
		newStorage.setQtyReserved(Env.ZERO);
		newStorage.saveEx();
		
		mStorage.delete(true);
		
		return 1;
	}

	/**
	 * 	Move stock to location
	 *	@param target target storage
	 *	@return no of movements
	 */
	private int move (int m_product_id, int m_locator_id, int m_attributeSetInstance_id)
	{
		log.info("");
				
		MProduct product = new MProduct(getCtx(),m_product_id,get_TrxName());	
		MLocator locator = MLocator.get(getCtx(), m_locator_id);
		boolean positiveOnly = true;
		boolean fifo = MClient.MMPOLICY_FiFo.equals(product.getMMPolicy());
		int m_warehouse_id = locator.getM_Warehouse_ID();
		
		// Try locator first
		BigDecimal qty = MStorage.getQtyOnHand(getCtx(), m_product_id, m_attributeSetInstance_id, m_locator_id, get_TrxName());
		
		// Find all storage at this location
		MStorage[] storages = MStorage.getWarehouse(getCtx(), m_warehouse_id, m_product_id, m_attributeSetInstance_id, 
				0, null, fifo, false, m_locator_id, get_TrxName());

		// If the qty at this location is less than zero, use sources from the whole warehouse to fulfill the negative
		// quantities
		int tempLocator = m_locator_id;
		if (qty.signum() < 0)
			tempLocator = 0;
		
		MStorage[] sources = MStorage.getWarehouse(getCtx(), m_warehouse_id, m_product_id, m_attributeSetInstance_id, 
				0, null, fifo, positiveOnly, tempLocator, get_TrxName());
		
		BigDecimal qtyOnHand = Env.ZERO;
		for (MStorage source : sources) {
			qtyOnHand = qtyOnHand.add(source.getQtyOnHand());
		}
		
		BigDecimal qtyRequired = Env.ZERO;
		// Find the negative entries
		for (MStorage storage : storages) 
		{
			// Ignore positive entries
			if (storage.getQtyOnHand().signum() >= 0)
				continue;
			
			qtyRequired = qtyRequired.subtract(storage.getQtyOnHand());
		}
		
		BigDecimal qtyToMove = qtyOnHand.compareTo(qtyRequired) > 0 ? qtyRequired : qtyOnHand;
				
		BigDecimal qtyMoved = applySources(m_product_id, m_locator_id, m_attributeSetInstance_id, sources, qtyToMove);		

		//eliminateReservation(target);
		
		return 1;
	}	//	move

	/**
	 * 	Eliminate Reserved/Ordered
	 *	@param target target Storage
	 */
	private void eliminateReservation(MStorage target)
	{
		target.setQtyReserved(Env.ZERO);
		target.setQtyOrdered(Env.ZERO);
		target.saveEx();
	}	//	eliminateReservation
	
	/**
	 * 	Get Storage Sources
	 *	@param M_Product_ID product
	 *  @param M_Warehouse_ID warehouse
	 *	@param M_Locator_ID locator. Set to zero for all locators
	 *	@return sources
	 */
	private  BigDecimal applySources (int m_product_id, int m_locator_id, int m_attributeSetInstance_id, MStorage[] sources, BigDecimal qtyToMove)	{
		
		BigDecimal qtyMoved = Env.ZERO;
		BigDecimal qtyMovedTotal = Env.ZERO;

		if (sources.length == 0)
			return Env.ZERO;

		//	Create Movement
//		if (mMovement == null) {
			mMovement = new MMovement (getCtx(), 0, get_TrxName());
			mMovement.setAD_Org_ID(sources[0].getAD_Org_ID());
			mMovement.setC_DocType_ID(p_C_DocType_ID);
			mMovement.setDescription(getName());
			if (!mMovement.save())
				return Env.ZERO;
//		}
		
		int lines = 0;
		for (MStorage source : sources)
		{			
			//	Movement Line
			MMovementLine ml = new MMovementLine(mMovement);
			ml.setM_Product_ID(m_product_id);
			ml.setM_LocatorTo_ID(m_locator_id);
			ml.setM_AttributeSetInstanceTo_ID(m_attributeSetInstance_id);
			//	From
			ml.setM_Locator_ID(source.getM_Locator_ID());
			ml.setM_AttributeSetInstance_ID(source.getM_AttributeSetInstance_ID());
			
			if (qtyToMove.compareTo(source.getQtyOnHand()) > 0)
				qtyMoved = source.getQtyOnHand();
			else
				qtyMoved = qtyToMove;
			ml.setMovementQty(qtyMoved);
			//
			lines++;
			ml.setLine(lines*10);
			if (!ml.save())
				return Env.ZERO;
			
			qtyToMove = qtyToMove.subtract(qtyMoved);
			qtyMovedTotal = qtyMovedTotal.add(qtyMoved);
			if (qtyToMove.signum() <= 0)
				break;
		}	//	for all movements
		
		if (mMovement != null) {
			//	Process
			mMovement.processIt(MMovement.ACTION_Complete);
			mMovement.saveEx();
			
			addLog(0, null, new BigDecimal(lines), "@M_Movement_ID@ " + mMovement.getDocumentNo() + " (" 
				+ MRefList.get(getCtx(), MMovement.DOCSTATUS_AD_Reference_ID, 
					mMovement.getDocStatus(), get_TrxName()) + ")");
		}

		return qtyMovedTotal;  // Total moved
	}	//	getSources
	
}	//	StorageCleanup
