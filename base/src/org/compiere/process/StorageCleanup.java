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

import org.compiere.model.MClient;
import org.compiere.model.MClientInfo;
import org.compiere.model.MLocator;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MMovement;
import org.compiere.model.MMovementLine;
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
		//	Clean up empty Storage
		String sql = "DELETE FROM M_Storage "
			+ "WHERE QtyOnHand = 0 AND QtyReserved = 0 AND QtyOrdered = 0"
			+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
			+ " AND Created < SysDate-3";
		int no = DB.executeUpdate(sql, get_TrxName());
		log.info("Delete Empty #" + no);
		
		//  Remove ASI values that have no attribute sets
		sql = "UPDATE M_Storage s "
				+ "SET M_AttributeSetInstance_ID = 0 "
				+ "WHERE s.M_AttributeSetInstance_ID != 0"
				+ " AND AD_Client_ID = " + Env.getAD_Client_ID(getCtx())
				+ " AND EXISTS (SELECT 1 FROM M_AttributeSetInstance asi WHERE"
				+ " asi.M_AttributeSet_ID=0"
				+ " AND asi.M_AttributeSetInstance_ID = s.M_AttributeSetInstance_ID)";
		no = DB.executeUpdate(sql, get_TrxName());
		log.info("Set ASI values to zero where there was no Attribute Set #" + no);
		
		// Move/change qty on hand where M_MPolicyTicket_ID = 0 to a row with a policy ticket.
		// As these are likely old entries, use a zero Timestamp as the move date.
		sql = "SELECT * FROM M_Storage "
				+ "WHERE AD_Client_ID = ?"
				+ " AND QtyOnHand != 0 AND M_MPolicyTicket_ID = 0";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		no = 0;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				no += addMissingPolicyTickets(new MStorage(getCtx(), rs, get_TrxName()));
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
		
		// Consolidate qtyReserved and qtyOrdered to a single storage line with zero policy ticket
		// for each locator/product/asi combination.
				
		//
		sql = "SELECT * "
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
				+ " AND sl.M_Warehouse_ID=swl.M_Warehouse_ID))";
		pstmt = null;
		rs = null;
		int lines = 0;
		try
		{
			pstmt = DB.prepareStatement (sql, get_TrxName());
			pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
			rs = pstmt.executeQuery ();
			while (rs.next ())
			{
				lines += move (new MStorage(getCtx(), rs, get_TrxName()));
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
		
		return "Moves " + lines;
	}	//	doIt

	private int addMissingPolicyTickets(MStorage mStorage) {
		// Add a policy ticket to the storage location.
		if (mStorage.getQtyOnHand().compareTo(Env.ZERO) == 0)
			return 0;
		
		MMPolicyTicket ticket = MMPolicyTicket.create(getCtx(), null, Timestamp.from(Instant.EPOCH), get_TrxName());
		
		MStorage newStorage = MStorage.getCreate(getCtx(), 
				mStorage.getM_Locator_ID(), 
				mStorage.getM_Product_ID(), 
				mStorage.getM_AttributeSetInstance_ID(), 
				ticket.getM_MPolicyTicket_ID(), get_TrxName());
		newStorage.setQtyOnHand(mStorage.getQtyOnHand());
		newStorage.saveEx();
		
		mStorage.setQtyOnHand(Env.ZERO);
		
		eliminateReservation(mStorage);

		if (mStorage.getQtyOnHand().signum() == 0 && mStorage.getQtyOrdered().signum() == 0 && mStorage.getQtyReserved().signum() == 0)
			mStorage.deleteEx(false);
		
		return 1;
	}

	/**
	 * 	Move stock to location
	 *	@param target target storage
	 *	@return no of movements
	 */
	private int move (MStorage target)
	{
		log.info(target.toString());
		BigDecimal qty = target.getQtyOnHand().negate();
		
		int M_Product_ID = target.getM_Product_ID();
		int M_Locator_ID = target.getM_Locator_ID();
		int M_AttributeSetInstance_ID = target.getM_AttributeSetInstance_ID();
		
		MProduct product = new MProduct(getCtx(),M_Product_ID,get_TrxName());	
		MLocator locator = MLocator.get(getCtx(), M_Locator_ID);
		boolean positiveOnly = true;
		boolean fifo = MClient.MMPOLICY_FiFo.equals(product.getMMPolicy());
		int M_Warehouse_ID = locator.getM_Warehouse_ID();
		
		// Try locator first
		
		MStorage[] sources = MStorage.getWarehouse(getCtx(), M_Warehouse_ID, M_Product_ID, M_AttributeSetInstance_ID, 
				0, null, fifo, positiveOnly, M_Locator_ID, get_TrxName());
		
		BigDecimal applied = applySources(sources, target);
		
		qty = qty.subtract(applied);
		
		if (qty.signum() > 0) {
			sources = MStorage.getWarehouse(getCtx(), M_Warehouse_ID, M_Product_ID, M_AttributeSetInstance_ID, 
					0, null, fifo, positiveOnly, 0, get_TrxName());		
			applied = applySources(sources, target);
		}

		eliminateReservation(target);
		
		return 1;
	}	//	move

	/**
	 * 	Eliminate Reserved/Ordered
	 *	@param target target Storage
	 */
	private void eliminateReservation(MStorage target)
	{
		// TODO - review for FIFO/LIFO compliance
		//	Negative Ordered / Reserved Qty
		if (target.getQtyReserved().signum() != 0 || target.getQtyOrdered().signum() != 0)
		{
			BigDecimal reserved = target.getQtyReserved();
			BigDecimal ordered = target.getQtyOrdered();
			
			//	Eliminate Reservation
			if (reserved.signum() != 0 || ordered.signum() != 0)
			{
				target.setQtyReserved(Env.ZERO);
				target.setQtyOrdered(Env.ZERO);
				target.saveEx();
				if (MStorage.add(getCtx(), target.getM_Warehouse_ID(), target.getM_Locator_ID(), 
					target.getM_Product_ID(), 
					target.getM_AttributeSetInstance_ID(), target.getM_AttributeSetInstance_ID(),
					0,
					Env.ZERO, reserved, ordered, get_TrxName())) {
						log.info("Reserved=" + reserved + ",Ordered=" + ordered);
				}
				else 
						log.warning("Failed Storage0 Update");
			}
			else
				log.warning("Failed Target Update");
		}
	}	//	eliminateReservation
	
	/**
	 * 	Get Storage Sources
	 *	@param M_Product_ID product
	 *  @param M_Warehouse_ID warehouse
	 *	@param M_Locator_ID locator. Set to zero for all locators
	 *	@return sources
	 */
	private  BigDecimal applySources (MStorage[] sources, MStorage target)	{
		
		BigDecimal qty = target.getQtyOnHand().negate();
		BigDecimal qtyMoved = Env.ZERO;
		BigDecimal qtyToMove = qty;

		if (sources.length == 0)
			return Env.ZERO;
		
		int M_Product_ID = target.getM_Product_ID();
		int M_Locator_ID = target.getM_Locator_ID();
				
		MMovement mh = null;

		//	Create Movement
		mh = new MMovement (getCtx(), 0, get_TrxName());
		mh.setAD_Org_ID(target.getAD_Org_ID());
		mh.setC_DocType_ID(p_C_DocType_ID);
		mh.setDescription(getName());
		if (!mh.save())
			return Env.ZERO;
		
		int lines = 0;
		for (int i = 0; i < sources.length; i++)
		{
			MStorage source = sources[i];
			
			//	Movement Line
			MMovementLine ml = new MMovementLine(mh);
			ml.setM_Product_ID(M_Product_ID);
			ml.setM_LocatorTo_ID(M_Locator_ID);
			ml.setM_AttributeSetInstanceTo_ID(target.getM_AttributeSetInstance_ID());
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
			if (qtyToMove.signum() <= 0)
				break;
		}	//	for all movements
		
		//	Process
		mh.processIt(MMovement.ACTION_Complete);
		mh.saveEx();
		
		addLog(0, null, new BigDecimal(lines), "@M_Movement_ID@ " + mh.getDocumentNo() + " (" 
			+ MRefList.get(getCtx(), MMovement.DOCSTATUS_AD_Reference_ID, 
				mh.getDocStatus(), get_TrxName()) + ")");

		return qty.subtract(qtyToMove);  // Total moved
	}	//	getSources
	
}	//	StorageCleanup
