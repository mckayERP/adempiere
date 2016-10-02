/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
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
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): victor.perez@e-evolution.com http://www.e-evolution.com    *
 *                 Teo Sarca, www.arhipac.ro                                  *
 *****************************************************************************/

package org.adempiere.engine;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttributeSetInstance;
import org.compiere.model.MClient;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MLocator;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MTable;
import org.compiere.model.MTransaction;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_M_InOutLine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * Storage Engine
 * @author victor.perez@e-evolution.com http://www.e-evolution.com
 * @author Teo Sarca
 */
public class StorageEngine 
{
	
	/**	Logger							*/
	protected static transient CLogger	log = CLogger.getCLogger (StorageEngine.class);

	public static void createTransaction (
			IDocumentLine docLine,
			String movementType , 
			Timestamp movementDate , 
			BigDecimal qty, 
			boolean isReversal , 
			int m_Warehouse_ID, 
			int reservationAttributeSetInstance_ID,
			int o_M_Warehouse_ID,
			boolean isSOTrx) 
	{
		
		createTransaction(docLine,
				movementType , 
				movementDate , 
				qty, 
				isReversal , 
				m_Warehouse_ID, 
				reservationAttributeSetInstance_ID,
				o_M_Warehouse_ID,
				isSOTrx,
				true);
	}

	public static void createTransaction (
			IDocumentLine docLine,
			String movementType , 
			Timestamp movementDate , 
			BigDecimal qty, 
			boolean isReversal , 
			int m_Warehouse_ID, 
			int reservationAttributeSetInstance_ID,
			int o_M_Warehouse_ID,
			boolean isSOTrx,
			boolean deleteExistingMAEntries
		)
	{	

		MProduct product = MProduct.get(docLine.getCtx(), docLine.getM_Product_ID());
		if (product == null || !product.isStocked())
			return;
			
		//Ignore the Material Policy when the document is a Reverse Correction.
		// The Material allocation is copied from the reversed/voided document.
		if(!isReversal)
		{
			checkMaterialPolicy(docLine, movementType, movementDate, m_Warehouse_ID, deleteExistingMAEntries);
		}
		
		IInventoryAllocation mas[] = StorageEngine.getMA(docLine);
		if (mas.length > 0) {  // There are material allocations
			for (int j = 0; j < mas.length; j++)
			{
				IInventoryAllocation ma = mas[j];

				updateStorageAndCreateTransaction(docLine, 
						movementType, 
						movementDate, 
						isSOTrx, 
						ma.getMovementQty(), 
						reservationAttributeSetInstance_ID, 
						ma.getM_MPolicyTicket_ID(), 
						m_Warehouse_ID, 
						o_M_Warehouse_ID);
			}
		}
		else { // No material allocations.  Use the docLine material policy ticket
			if (docLine.getM_MPolicyTicket_ID() == 0)
				throw new AdempiereException ("@Error@ @FillMandatory@ @M_MPolicyTicket_ID@");

			updateStorageAndCreateTransaction(docLine, 
					movementType, 
					movementDate, 
					isSOTrx, 
					docLine.getMovementQty(), 
					reservationAttributeSetInstance_ID, 
					docLine.getM_MPolicyTicket_ID(), 
					m_Warehouse_ID, 
					o_M_Warehouse_ID);
		}
	}


	/**
	 * 	Check Material Policy<br>
	 *  This function ensures that material transactions follow the material 
	 *  policy of fifo/lifo.  Each document line with an incoming product is 
	 *  given a material policy ticket and this ticket is included in the 
	 *  storage of the product.  For outgoing products, the material policy 
	 *  is used to select the tickets that will be used to fulfill the 
	 *  transaction. Tickets will be added to the document line Material 
	 *  Allocation (for example MInOutLineMA).<br>
	 *  <br>
	 *  If a transaction forces the quantity on hand to be negative, an exception
	 *  will be thrown.  The source document should not be completed until 
	 *  there is sufficient quantity on hand or a way to deal with cost corrections
	 *  is added to the code.<br>  
	 *  <br>
	 *  Negative quantity-on-hand can exists, created by reversals of material 
	 *  receipts for example.  The system will apply incoming material to the
	 *  negative storage tickets to bring them to zero quantity on hand.<br>
	 *  <br>
	 *  The function also sets the locator to the default for the warehouse if no
	 *  locator is defined on the line and there is insufficient stock of that product
	 *  and Attribute Set Instance.  If stock exists in the warehouse, the locator
	 *  with the highest priority and sufficient stock on hand is used.  For incoming
	 *  material, the default locator is used.  
	 *     
	 *  @param line - the document line that contains the product and ASI (if any)
	 *  @param movementType - a string that follows the movement type patterns (For example "C-" or "V+")
	 *  @param movementDate - a timestamp with the date the movement occurred
	 *  @param M_Warehouse_ID - the ID of the Warehouse to use
	 *  
	 *  @since 3.9.0 - prior to 3.9.0, the material attribute set instances were 
	 *  used as tickets. See <a href="https://github.com/adempiere/adempiere/issues/453">BR 453 
	 *  Attribute Set Instances are used to track FIFO/LIFO. Another method is 
	 *  required.</a>
	 *  
	 *  @see org.compiere.model.MMPolicyTicket
	 */
	private static void checkMaterialPolicy(
			IDocumentLine line, 
			String movementType, 
			Timestamp movementDate, 
			int M_Warehouse_ID,
			boolean deleteExistingMALines)
	{
		
		if (M_Warehouse_ID == 0)
			throw new AdempiereException("@InvalidValue@ @M_Warehouse_ID@==0");
		
		// In case the document process is being redone, delete work in progress
		// and start again.  There are cases where documents need to be processed twice
		// for to and from entries, in which case the previous work may be valid.
		if (deleteExistingMALines)
			deleteMA(line);

		//	Incoming Trx are positive receipts or negative shipments
		boolean incomingTrx = MTransaction.isIncomingTransaction(movementType) && line.getMovementQty().signum() >= 0
							|| !MTransaction.isIncomingTransaction(movementType) && line.getMovementQty().signum() < 0;	//	V+ Vendor Receipt
		MProduct product = MProduct.get(line.getCtx(), line.getM_Product_ID());
		if (product == null || line.getMovementQty().signum() == 0)  // Nothing to ticket
			return;

		//	Material Policy Tickets - used to track the FIFO/LIFO
		//  Create a Material Policy Ticket ID for any incoming transaction
		//  Where there is negative material on-hand, receive the new material using the ticket
		//  of the negative material.  This assumes the material receipt is a correction of the 
		//  cause of the negative quantity.  A single ticket is used as the costs are unique for 
		//  each receipt line.
		if (incomingTrx)
		{
			MMPolicyTicket ticket = null;
			boolean createdMA = false;
			
			BigDecimal qtyReceived = line.getMovementQty(); // Must be positive
			if (!MTransaction.isIncomingTransaction(movementType))
				qtyReceived = line.getMovementQty().negate();
			
			//  Find the storage locations to use.  Use the locator, or if that is zero, search the 
			//  whole warehouse. Prioritize any negative quantity-on-hand and apply incoming material
			//  to the associated ticket to correct the inventory balance.
			MStorage[] storages = MStorage.getWarehouse(line.getCtx(), M_Warehouse_ID, line.getM_Product_ID(), 0, 0,
					null, MClient.MMPOLICY_FiFo.equals(product.getMMPolicy()), false, line.getM_Locator_ID(), line.get_TrxName());
			for (MStorage storage : storages) 
			{
				// If there is negative storage ...
				if (storage.getQtyOnHand().signum() < 0) {
					// ... and the remaining qty received is not enough to make it zero or positive
					if (qtyReceived.compareTo(storage.getQtyOnHand().negate()) <= 0)	
					{
						// ... then assign all the quantity received to this ticket
						createMA (line, storage.getM_MPolicyTicket_ID(), qtyReceived);
						qtyReceived = Env.ZERO;
						log.fine("QtyReceived=" + qtyReceived);
						createdMA = true;
					}
					// ... and the remaining qty received is greater than this negative qty
					else
					{	
						// ... then apply enough material to this ticket to bring it to zero quantity
						createMA (line, storage.getM_MPolicyTicket_ID(), storage.getQtyOnHand().negate());
						qtyReceived = qtyReceived.subtract(storage.getQtyOnHand().negate());
 						log.fine("QtyReceived=" + qtyReceived);
						createdMA = true;
					}
				}
				
				if (qtyReceived.signum() == 0)
					break;
			}
			//  If there is qtyReceived remaining after fulfilling negative storage, create a new 
			//  material policy ticket so fifo/lifo work.
			if (qtyReceived.signum() > 0)
			{
				ticket = MMPolicyTicket.create(line.getCtx(), line, movementDate, line.get_TrxName());
				if (ticket == null) { // There is a problem
					log.severe("Can't create Material Policy Ticket for line " + line);
					throw new AdempiereException("Can't create Material Policy Ticket for line " + line);
				}
				if (createdMA) {  // Add the remainder to another material allocation line
					createMA (line, ticket.getM_MPolicyTicket_ID(), qtyReceived);					
				}
				else { //  For incoming transactions with no storage corrections, one ticket is created per MR line and is added to the line.			
					line.setM_MPolicyTicket_ID(ticket.getM_MPolicyTicket_ID());
				}
				qtyReceived = Env.ZERO;
				log.config("New Material Policy Ticket=" + line);
			}

			if (qtyReceived.signum() != 0) { // negative remaining is a problem.
				throw new AdempiereException("Can't receive all quantity on line " + line);
			}
		} // Incoming			
		else // Outgoing - use Material Allocations - there could be several per line.
		{
			String MMPolicy = product.getMMPolicy();
			Timestamp minGuaranteeDate = movementDate;
			MStorage[] storages = MStorage.getWarehouse(line.getCtx(), M_Warehouse_ID, 
					line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(), 0, 
					minGuaranteeDate, MClient.MMPOLICY_FiFo.equals(MMPolicy), true, line.getM_Locator_ID(), line.get_TrxName());
			BigDecimal qtyToDeliver = line.getMovementQty();
			// Check if this is a negative incoming movement and negate the quantity
			if (MTransaction.isIncomingTransaction(movementType))
				qtyToDeliver = qtyToDeliver.negate();
			
			for (MStorage storage : storages)
			{						
				if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
				{
					createMA (line, storage.getM_MPolicyTicket_ID(), qtyToDeliver);
					qtyToDeliver = Env.ZERO;
				}
				else
				{	
					createMA (line, storage.getM_MPolicyTicket_ID(), storage.getQtyOnHand());
					qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
					log.fine("QtyToDeliver=" + qtyToDeliver);						
				}

				if (qtyToDeliver.signum() == 0)
					break;
			}

			if (qtyToDeliver.signum() != 0)
			{
				//deliver using new asi
				//MAttributeSetInstance asi = MAttributeSetInstance.create(line.getCtx(), product, line.get_TrxName());
				//createMA(line, asi.getM_AttributeSetInstance_ID(), qtyToDeliver);
				
				// There is not enough stock to deliver this shipment. 
				// TODO - this should trigger a way to balance costs - outgoing shipments 
				// could have accounting with a generic cost guess (Steve's Shipment Plan for example).
				// The balancing incoming transaction could have accounting to reverse the generic 
				// cost and add the correct one.  This is left as a TODO.
				// 
				// For now, remove any Material Allocations already created and throw an error as
				// we shouldn't generate a zero cost transaction.
				log.warning(line + ", Insufficient quantity. Process later.");
				deleteMA(line);
				throw new AdempiereException("Insufficient quantity to deliver line " + line);
			}
		}	//	outgoing Trx
		save(line);
	}
	
	private static String getTableNameMA(IDocumentLine model)
	{
		return model.get_TableName()+"MA";
	}
	
	private static int deleteMA(IDocumentLine model)
	{
		String sql = "DELETE FROM "+getTableNameMA(model)+" WHERE "+model.get_TableName()+"_ID=?";
		int no = DB.executeUpdateEx(sql, new Object[]{model.get_ID()}, model.get_TrxName());
		if (no > 0)
			log.config("Delete old #" + no);
		return no;
	}
	
	private static void saveMA(IInventoryAllocation ma)
	{
		((PO)ma).saveEx();
	}
	
	private static void save(IDocumentLine line)
	{
		((PO)line).saveEx();
	}
	
	private static void create(IDocumentLine model, String MovementType, Timestamp MovementDate,
								int M_MPolicyTicket_ID, BigDecimal Qty)
	{
		MTransaction mtrx = new MTransaction (model.getCtx(), model.getAD_Org_ID(),
				MovementType, model.getM_Locator_ID(),
				model.getM_Product_ID(), model.getM_AttributeSetInstance_ID(), M_MPolicyTicket_ID,
				Qty, MovementDate, model.get_TrxName());
		setReferenceLine_ID(mtrx, model);
		mtrx.saveEx();
	}
	
	private static IInventoryAllocation createMA(IDocumentLine model, int M_MPolicyTicket_ID, BigDecimal MovementQty)
	{
		final Properties ctx = model.getCtx();
		final String tableName = getTableNameMA(model);
		final String trxName = model.get_TrxName();
		
		IInventoryAllocation ma = (IInventoryAllocation)MTable.get(ctx, tableName).getPO(0, trxName);
		ma.setAD_Org_ID(model.getAD_Org_ID());
		setReferenceLine_ID((PO)ma, model);
		ma.setM_MPolicyTicket_ID(M_MPolicyTicket_ID);
		ma.setMovementQty(MovementQty);
		
		saveMA(ma);
		log.fine("##: " + ma);
		
		return ma;
	}
	
	private static IInventoryAllocation[] getMA(IDocumentLine model)
	{
		final Properties ctx = model.getCtx();
		final String IDColumnName = model.get_TableName()+"_ID";
		final String tableName = getTableNameMA(model);
		final String trxName = model.get_TrxName();
		
		final String whereClause = IDColumnName+"=?";
		List<PO> list = new Query(ctx, tableName, whereClause, trxName)
											.setClient_ID()
											.setParameters(new Object[]{model.get_ID()})
											.setOrderBy(IDColumnName)
											.list();
		IInventoryAllocation[] arr = new IInventoryAllocation[list.size()];
		return list.toArray(arr);
	}
	
	private static void setReferenceLine_ID(PO model, IDocumentLine ref)
	{
		String refColumnName = ref.get_TableName()+"_ID";
		if (model.get_ColumnIndex(refColumnName) < 0)
		{
			throw new AdempiereException("Invalid inventory document line "+ref);
		}
		model.set_ValueOfColumn(refColumnName, ref.get_ID());
		
	}
		
	/**
	 * 	Set (default) Locator based on qty.
	 * 	@param Qty quantity
	 * 	Assumes Warehouse is set
	 */
	public static int getM_Locator_ID(
			Properties ctx,
			int M_Warehouse_ID, 
			int M_Product_ID, int M_AttributeSetInstance_ID,  
			BigDecimal Qty,
			String trxName)
	{	
		//	Get existing Location
		int M_Locator_ID = MStorage.getM_Locator_ID (M_Warehouse_ID, 
				M_Product_ID, M_AttributeSetInstance_ID, 0,
				Qty, trxName);
		//	Get default Location
		if (M_Locator_ID == 0)
		{
			MWarehouse wh = MWarehouse.get(ctx, M_Warehouse_ID);
			M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
		}
		return M_Locator_ID;
	}	//	setM_Locator_ID

	public static void reserveOrOrderStock(Properties ctx, int M_Warehouse_ID, 
			int M_Product_ID, int M_AttributeSetInstance_ID, 
			BigDecimal qtyOrdered, BigDecimal qtyReserved, String trxName) {
		int M_Locator_ID = 0; 
		int M_MPolicyTicket_ID = 0; // Always for orders or reservations
		
		if (M_Product_ID <= 0)
			throw new AdempiereException("@Error@ @M_Product_ID@ @NotZero@");  //TODO check the translations

		if (M_Warehouse_ID <= 0)
			throw new AdempiereException("@Error@ @M_Warehouse_ID@ @NotZero@");  //TODO check the translations

		if (qtyOrdered.compareTo(Env.ZERO) == 0 && qtyReserved.compareTo(Env.ZERO) == 0)
			return; // Nothing to do
		
		if (qtyOrdered.compareTo(Env.ZERO) != 0 && qtyReserved.compareTo(Env.ZERO) != 0)
			throw new AdempiereException("@Error@ qtyOrdered and qtyReserved can't both be non-zero");  //TODO check the translations
		
		//	Get Locator to order/reserve
		// For orders, are there is a sufficient qty of this product/ASI (ASI could be zero) in inventory?
		// Get the locator with sufficient qty and with the highest locator priority.
		M_Locator_ID = MStorage.getM_Locator_ID (M_Warehouse_ID, 
				M_Product_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID, 
			qtyOrdered, trxName);
		//	Get default Location
		if (M_Locator_ID == 0)
		{
			// try to take default locator for product first
			// if it is from the selected warehouse
			MProduct product = new MProduct(ctx, M_Product_ID, null);
			if (product == null)
				throw new AdempiereException ("@Error@ @M_Product-ID@=" + M_Product_ID + " can't be found.");
			
			MWarehouse wh = MWarehouse.get(ctx, M_Warehouse_ID);
			M_Locator_ID = product.getM_Locator_ID();
			if (M_Locator_ID!=0) {
				MLocator locator = new MLocator(ctx, product.getM_Locator_ID(), trxName);
				//product has default locator defined but is not from the order warehouse
				if(locator.getM_Warehouse_ID()!=wh.get_ID()) {
					M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
				}
			} else {
				M_Locator_ID = wh.getDefaultLocator().getM_Locator_ID();
			}
		}
		//	Update Storage
		if (!MStorage.add(ctx, M_Warehouse_ID, M_Locator_ID, 
			M_Product_ID, 
			M_AttributeSetInstance_ID, M_AttributeSetInstance_ID, M_MPolicyTicket_ID,
			Env.ZERO, qtyReserved, qtyOrdered, trxName))
		{
			throw new AdempiereException(); //Cannot reserve or order stock
		}
			
	}

	private static BigDecimal getReservedDifference (IDocumentLine docLine, boolean isSOTrx, String movementType, BigDecimal movementQty) {
		
		BigDecimal reservedDiff = Env.ZERO;

		if (docLine != null && docLine instanceof MInOutLine && ((MInOutLine) docLine).getC_OrderLine_ID() != 0)
		{			
			if (isSOTrx) { // Reservations are only affected by sales transactions.
				// If the movement qty is positive - a customer shipment, the reservation is reduced
				if (MTransaction.isIncomingTransaction(movementType)) // Customer return: reservation increases with positive movements
					reservedDiff =  movementQty;
				else // Customer Shipment: reservation decreases with positive movements
					reservedDiff = movementQty.negate();
			}
		}
		
		return reservedDiff;

	}

	private static BigDecimal getOrderedDifference (IDocumentLine docLine, boolean isSOTrx, String movementType, BigDecimal movementQty) {
		
		BigDecimal orderedDiff = Env.ZERO;

		if (docLine != null && docLine instanceof MInOutLine && ((MInOutLine) docLine).getC_OrderLine_ID() != 0)
		{			
			if (!isSOTrx) {
				if (MTransaction.isIncomingTransaction(movementType)) // Vendor receipt: ordered quantity is reduced by positive movements.
					orderedDiff = movementQty.negate();
				else   // Vendor return: ordered quantity is increased by positive movements. 
					orderedDiff = movementQty;
			}
		}
		
		return orderedDiff;
	}
	
	private static void updateStorageAndCreateTransaction(IDocumentLine docLine, String movementType, Timestamp movementDate, Boolean isSOTrx, BigDecimal movementQty, 
								int reservationAttributeSetInstance_ID, int M_MPolicyTicket_ID, int M_Warehouse_ID, int o_M_Warehouse_ID) {
		
		boolean incomingTrx = MTransaction.isIncomingTransaction(movementType);	//	V+ Vendor Receipt
		
		//	Incoming Trx are positive receipts or negative shipments
		boolean sameWarehouse = M_Warehouse_ID == o_M_Warehouse_ID || o_M_Warehouse_ID == 0;

		BigDecimal qty = movementQty;
		if (!incomingTrx)
			qty = qty.negate();		// Outgoing - reducing inventory
		
		// For orders only, determine the changes to qty ordered or qty reserved. For other docs, 
		// these remain zero and have no effect.
		BigDecimal reservedDiff = getReservedDifference(docLine, isSOTrx, movementType, movementQty);
		BigDecimal orderedDiff = getOrderedDifference(docLine, isSOTrx, movementType, movementQty);
						
		//	Update Storage - see also VMatch.createMatchRecord
		if (!MStorage.add(docLine.getCtx(), 
			M_Warehouse_ID,
			docLine.getM_Locator_ID(),
			docLine.getM_Product_ID(), 
			docLine.getM_AttributeSetInstance_ID(), 
			reservationAttributeSetInstance_ID,
			M_MPolicyTicket_ID,
			qty,
			sameWarehouse ? reservedDiff : Env.ZERO,
			sameWarehouse ? orderedDiff : Env.ZERO,
			docLine.get_TrxName()))
		{
			throw new AdempiereException(); //Cannot correct Inventory (MA)
		}
		if (!sameWarehouse) {
			//correct qtyOrdered/qtyReserved in warehouse of order
			MWarehouse wh = MWarehouse.get(docLine.getCtx(), o_M_Warehouse_ID);
			if (!MStorage.add(docLine.getCtx(), 
					o_M_Warehouse_ID,
					wh.getDefaultLocator().getM_Locator_ID(),
					docLine.getM_Product_ID(), 
					docLine.getM_AttributeSetInstance_ID(), 
					reservationAttributeSetInstance_ID,
					M_MPolicyTicket_ID,
					Env.ZERO,
					reservedDiff,
					orderedDiff,
					docLine.get_TrxName()))
				{
					throw new AdempiereException(); //Cannot correct Inventory (MA)
				}
	
		}
		
		// Update Date Last Inventory if the docLine is a Physical Inventory and not internal use
		if(docLine instanceof MInventoryLine 
				&& ((MInventoryLine) docLine).getQtyInternalUse().compareTo(Env.ZERO) == 0)
		{	
			MStorage storage = MStorage.get(docLine.getCtx(), docLine.getM_Locator_ID(), 
					docLine.getM_Product_ID(), docLine.getM_AttributeSetInstance_ID(),
					M_MPolicyTicket_ID, docLine.get_TrxName());						
			storage.setDateLastInventory(movementDate);
			if (!storage.save(docLine.get_TrxName()))
			{
				throw new AdempiereException("Storage not updated(2)");
			}
		}

		create(docLine, movementType ,movementDate, M_MPolicyTicket_ID , qty);
	}
}
