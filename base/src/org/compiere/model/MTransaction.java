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
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.adempiere.engine.CostEngineFactory;
import org.adempiere.engine.IDocumentLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderBOMLine;

/**
 * 	Material Transaction Model
 *
 *	@author Jorg Janke
 *	@version $Id: MTransaction.java,v 1.3 2006/07/30 00:51:03 jjanke Exp $
 */
public class MTransaction extends X_M_Transaction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 3411351000865493212L;

	/**
	 * get the transaction based on Document Line and movement type
	 * @param model IDocumentLine
	 * @param type Movement Type
	 * @return first MTransaction
	 */
	public static MTransaction getByDocumentLine(IDocumentLine model, String type)
	{
		final String column_id = model.get_TableName() + "_ID";
		final String whereClause = column_id + "=? AND "
			   					 + MTransaction.COLUMNNAME_MovementType + "=? ";
		return new Query (model.getCtx(), I_M_Transaction.Table_Name, whereClause, model.get_TrxName())
		.setClient_ID()
		.setParameters(model.get_ID(), type)
		.first();
	}

	/**
	 * Gets all transactions associated with a Document Line
	 * @param model IDocumentLine
	 * @return the list of transactions
	 */
	public static List<MTransaction> getByDocumentLine(IDocumentLine model)
	{
		final String column_id = model.get_TableName() + "_ID";
		final String whereClause = column_id + "=?";
		return new Query (model.getCtx(), I_M_Transaction.Table_Name, whereClause, model.get_TrxName())
		.setClient_ID()
		.setParameters(model.get_ID())
		.list();
	}

	
	/**
	 * get the Material Transaction after Date Account
	 * @param ctx Context
	 * @param M_Product_ID Product ID
	 * @param dateAcct Date Account 
	 * @param trxName Transaction name
	 * @return List with the MTransaction after date account
	 */
	static public List<MTransaction> getAfterDateAcct(Properties ctx , int M_Product_ID,Timestamp dateAcct, String trxName)
	{
		ArrayList<MTransaction> list = new ArrayList();
		final String whereClause = I_M_Transaction.COLUMNNAME_M_Product_ID + "=?";
		List<MTransaction> trxs = new Query(ctx, Table_Name, whereClause, trxName)
			.setClient_ID()
			.setParameters(M_Product_ID)
			.list();
		
		for(MTransaction trx : trxs)
		{
			IDocumentLine model = trx.getDocumentLine();
			if(model.getDateAcct().compareTo(dateAcct) > 0)
			{
				list.add(trx);
			}
		}	
		return list;
	}
	
	/**
	 * get all material transaction for MInOutLine 
	 * @param line MInOutLine
	 * @return List the MTransaction
	 */
	static public List<MTransaction> getByInOutLine(MInOutLine line)
	{
		ArrayList<MTransaction> transactions = new ArrayList();
		
		List<MInOutLineMA> lines = MInOutLineMA.get(line.getCtx(), line.getM_InOutLine_ID(), line.get_TrxName());
		if(lines != null && lines.size() == 0)
		{
            MTransaction transaction = get(line, line.getM_MPolicyTicket_ID());
            if (transaction != null && transaction.get_ID() > 0)
			    transactions.add(transaction);

			return transactions;
		}
		for(MInOutLineMA ma : lines)
		{	
			MTransaction transaction = get(line, ma.getM_MPolicyTicket_ID());
			if (transaction != null && transaction.get_ID() > 0)
				transactions.add(transaction);
		}		
		return transactions;
	}
	
	static public MTransaction get(MInOutLine line , int m_mPolicyTicket_id)
	{
		final String whereClause = I_M_InOutLine.COLUMNNAME_M_Product_ID + "=? AND "
								 + I_M_InOutLine.COLUMNNAME_M_AttributeSetInstance_ID + "=? AND "
								 + I_M_InOutLine.COLUMNNAME_M_InOutLine_ID + "=? AND "
		 						 + I_M_InOutLine.COLUMNNAME_M_MPolicyTicket_ID + "=?";
		
		return new Query(line.getCtx(), Table_Name, whereClause, line.get_TrxName())
		.setClient_ID()
		.setParameters(line.getM_Product_ID(), line.getM_AttributeSetInstance_ID(), line.getM_InOutLine_ID(), m_mPolicyTicket_id)
		.firstOnly();
	}
	
	
	/**
	 * 
	 * get Material transaction for Reversal Document
	 * @param trx MTransaction
	 * @return
	 */
	static public MTransaction getByDocumentLine (MTransaction trx)
	{
		IDocumentLine reversal = trx.getDocumentLine().getReversalDocumentLine();
		List<Object> parameters = new ArrayList();
		String columnName =  reversal.get_TableName()+"_ID";			
		StringBuffer whereClause = new StringBuffer(I_M_Transaction.COLUMNNAME_M_Product_ID);
		whereClause.append( "=? AND ");
		parameters.add(reversal.getM_Product_ID());
		whereClause.append( columnName ).append("=? AND ");
		parameters.add(reversal.get_ID());
		whereClause.append(I_M_Transaction.COLUMNNAME_M_AttributeSetInstance_ID).append("=? AND ");
		parameters.add(trx.getM_AttributeSetInstance_ID());
		
//		
//		if (trx.getM_MPolicyTicket_ID() >  0)
//		{
//			whereClause.append(I_M_Transaction.COLUMNNAME_M_MPolicyTicket_ID).append("=? AND ");
//			parameters.add(trx.getM_MPolicyTicket_ID());
//		}
		
		whereClause.append(I_M_Transaction.COLUMNNAME_MovementType).append("=? AND ");
		if(MTransaction.MOVEMENTTYPE_InventoryIn.equals(trx.getMovementType()))
				parameters.add(MTransaction.MOVEMENTTYPE_InventoryOut);
		else if(MTransaction.MOVEMENTTYPE_InventoryOut.equals(trx.getMovementType()))
				parameters.add(MTransaction.MOVEMENTTYPE_InventoryIn);
		else
			parameters.add(trx.getMovementType());
			
		whereClause.append(I_M_Transaction.COLUMNNAME_M_MPolicyTicket_ID).append("=?");
		parameters.add(trx.getM_MPolicyTicket_ID());
		return new Query(trx.getCtx(), Table_Name, whereClause.toString(), trx.get_TrxName())
		.setClient_ID()
		.setParameters(parameters)
		.first();
	}
	
	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param M_Transaction_ID id
	 *	@param trxName transaction
	 */
	public MTransaction (Properties ctx, int M_Transaction_ID, String trxName)
	{
		super (ctx, M_Transaction_ID, trxName);
		if (M_Transaction_ID == 0)
		{
		//	setM_Transaction_ID (0);		//	PK
		//	setM_Locator_ID (0);
		//	setM_Product_ID (0);
			setMovementDate (new Timestamp(System.currentTimeMillis()));
			setMovementQty (Env.ZERO);
		//	setMovementType (MOVEMENTTYPE_CustomerShipment);
		}
	}	//	MTransaction

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MTransaction (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MTransaction

	/**
	 * 	Detail Constructor
	 *	@param ctx context
	 *	@param AD_Org_ID org
	 * 	@param MovementType movement type
	 * 	@param M_Locator_ID locator
	 * 	@param M_Product_ID product
	 * 	@param M_AttributeSetInstance_ID attribute
	 * 	@param MovementQty qty
	 * 	@param MovementDate optional date
	 *	@param trxName transaction
	 */
	@Deprecated
	public MTransaction (Properties ctx, int AD_Org_ID, 
		String MovementType, 
		int M_Locator_ID, int M_Product_ID, int M_AttributeSetInstance_ID, 
		BigDecimal MovementQty, Timestamp MovementDate, String trxName)
	{
		this(ctx, AD_Org_ID, MovementType, M_Locator_ID, 
				M_Product_ID, M_AttributeSetInstance_ID, 0,
				MovementQty, MovementDate, trxName);
	}
	/**
	 * 	Detail Constructor
	 *	@param ctx context
	 *	@param AD_Org_ID org
	 * 	@param MovementType movement type
	 * 	@param M_Locator_ID locator
	 * 	@param M_Product_ID product
	 * 	@param M_AttributeSetInstance_ID attribute
	 *  @param M_MPolicyTicket_ID Material Policy Ticket
	 * 	@param MovementQty qty
	 * 	@param MovementDate optional date
	 *	@param trxName transaction
	 */
	public MTransaction (Properties ctx, int AD_Org_ID, 
		String MovementType, 
		int M_Locator_ID, int M_Product_ID, int M_AttributeSetInstance_ID,
		int M_MPolicyTicket_ID,
		BigDecimal MovementQty, Timestamp MovementDate, String trxName)
	{
		super(ctx, 0, trxName);
		setAD_Org_ID(AD_Org_ID);
		setMovementType (MovementType);
		if (M_Locator_ID == 0)
			throw new IllegalArgumentException("No Locator");
		setM_Locator_ID (M_Locator_ID);
		if (M_Product_ID == 0)
			throw new IllegalArgumentException("No Product");
		setM_Product_ID (M_Product_ID);
		setM_AttributeSetInstance_ID (M_AttributeSetInstance_ID);
		setM_MPolicyTicket_ID(M_MPolicyTicket_ID);
		//
		if (MovementQty != null)		//	Can be 0
			setMovementQty (MovementQty);
		if (MovementDate == null)
			setMovementDate (new Timestamp(System.currentTimeMillis()));
		else
			setMovementDate(MovementDate);
	}	//	MTransaction

	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if (newRecord)
		{	
			CostEngineFactory.getCostEngine(getAD_Client_ID()).createCostDetail(this , getDocumentLine());
		}	
		return true;
	}	//	afterSave
	
	public IDocumentLine getDocumentLine()
	{
	    if(getM_InOutLine_ID() > 0)
	    	return (IDocumentLine) getM_InOutLine();
	    if(getM_InventoryLine_ID() > 0)
	    	return (IDocumentLine) getM_InventoryLine();
	    if(getM_MovementLine_ID() > 0)
	    	return (IDocumentLine) getM_MovementLine();
	    if(getM_ProductionLine_ID() > 0)
	    	return (IDocumentLine) getM_ProductionLine();
	    if(getPP_Cost_Collector_ID() > 0)
	    	return (IDocumentLine) getPP_Cost_Collector();
		if(getC_ProjectIssue_ID() > 0)
			return (IDocumentLine) getC_ProjectIssue();
		if(getPP_OrderReceipt_ID() > 0)
			return (IDocumentLine) getPP_OrderReceipt();
		if(getPP_Order_BOMLineIssue_ID() > 0)
			return (IDocumentLine) getPP_Order_BOMLineIssue();
	    
	    return null;	
	}
	
	/**
	 * get Warehouse ID
	 * @return Warehouse ID
	 */
	public int  getM_Warehouse_ID()
	{
		return getM_Locator().getM_Warehouse_ID();
	}
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString ()
	{
		StringBuffer sb = new StringBuffer ("MTransaction[");
		sb.append(get_ID()).append(",").append(getMovementType())
			.append(",MovementDate=").append(getMovementDate())
			.append(",Qty=").append(getMovementQty())
			.append(",M_Product_ID=").append(getM_Product_ID())
			.append(",ASI=").append(getM_AttributeSetInstance_ID())
			.append(",Mat Policy Ticket=").append(getM_MPolicyTicket_ID())
			.append ("]");
		return sb.toString ();
	}	//	toString
	
	public Timestamp getDateAcct()
	{
		if (getM_InOutLine_ID() != 0)
			return getM_InOutLine().getM_InOut().getDateAcct();
		if (getM_InventoryLine_ID() !=0)
			return getM_InventoryLine().getM_Inventory().getMovementDate();
		if (getM_MovementLine_ID() !=0)
			return getM_MovementLine().getM_Movement().getMovementDate();
		if (getM_ProductionLine_ID() !=0)
			return getM_ProductionLine().getM_Production().getMovementDate();
		if (getC_ProjectIssue_ID() !=0)
			return getC_ProjectIssue().getMovementDate();
		if (getPP_Cost_Collector_ID() !=0)
			return getPP_Cost_Collector().getDateAcct();
		return null;
	}
	

	/**
	 * Test the movement type to determine if the movement will increase inventory (incoming)
	 * or decrease inventory (outgoing).  A AdempiereException will be thrown if the 
	 * movementType is not recognized.
	 * @param movementType a string that must match one of the defined movement types in 
	 * the movement type reference list (AD_Reference_ID=189)
	 * @return true if the movement in incoming, false if outgoing.
	 */
	public static Boolean isIncomingTransaction(String movementType) {

		switch (movementType) {

			// Incoming
			case MOVEMENTTYPE_CustomerReturns:
			case MOVEMENTTYPE_VendorReceipts: 
			case MOVEMENTTYPE_InventoryIn:
			case MOVEMENTTYPE_MovementTo:
			case MOVEMENTTYPE_ProductionPlus:
			case MOVEMENTTYPE_WorkOrderPlus:
				return true;

			// Outgoing
			case MOVEMENTTYPE_CustomerShipment:
			case MOVEMENTTYPE_VendorReturns:
			case MOVEMENTTYPE_InventoryOut:
			case MOVEMENTTYPE_MovementFrom:
			case MOVEMENTTYPE_Production_:
			case MOVEMENTTYPE_WorkOrder_:
				return false;
				
			default:
				throw new AdempiereException("Unknown Movement Type: " + movementType);
		}
	}

	/**
	 * Test the movement type represented by the transaction.  This
	 * test is based only on the movement type and does not look at 
	 * the sign of the quantity being moved.
	 *   
	 * @return true if the movement type generally increases inventory or
	 * false if inventory is decreased
	 */
	public boolean isIncomingTransaction() {
		return isIncomingTransaction(getMovementType());
	}
	
	public static MTransaction createTransaction(IDocumentLine model, int ad_org_id, int m_locator_id, 
			int m_product_id, int m_attributeSetInstance_id,
			String MovementType, Timestamp MovementDate,
			int m_mPolicyTicket_id, BigDecimal Qty)
	{
		MTransaction mtrx = new MTransaction (model.getCtx(), ad_org_id,
		MovementType, m_locator_id,
		m_product_id, m_attributeSetInstance_id, m_mPolicyTicket_id,
		Qty, MovementDate, model.get_TrxName());
		return mtrx;
	}	
	
	
}	//	MTransaction
