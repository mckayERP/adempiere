/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2016 ADempiere Foundation, All Rights Reserved.         *
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
package org.compiere.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.exceptions.AdempiereException;

/**
 * 	This class controls the life-cycle of Material Policy Tickets which are
 *  used to ensure that material received or shipped follows the material 
 *  policy of fifo/lifo.  Each addition to inventory for a product is given 
 *  a material policy ticket and this ticket is included in the document 
 *  line that added that product. The material policy is used to select the 
 *  tickets that will be used to fulfill draw-downs on inventory. These are
 *  typically added to Material Allocations o the particular document lines.<br>
 *  <br>
 *  Storage entries provide a means to link the product, Attribute Set 
 *  Instance (ASI) and material policy ticket. The tickets are also related 
 *  to the cost details for the transactions that create or consume the 
 *  tickets.<br>  
 *  <br>
 *  
 *  @since 3.9.0 - prior to 3.9.0, the material attribute set instances were 
 *  used as tickets. See <a href="https://github.com/adempiere/adempiere/issues/453">BR 453 
 *  Attribute Set Instances are used to track FIFO/LIFO. Another method is 
 *  required.</a>
 *  
 *  @see org.compiere.model.MInOut
 * 
 *  @author Michael McKay, mckayERP (michael.mckay@mckayERP.com)
 *
 */
public class MMPolicyTicket extends X_M_MPolicyTicket {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5905583402118502877L;

	/**
	 * General constructor for an existing M_MPolicyTicket_ID.  If no record is 
	 * found with that ID, a new record will be returned with ID of 0. 
	 * @param ctx
	 * @param M_MPolicyTicket_ID
	 * @param trxName
	 */
	public MMPolicyTicket(Properties ctx, int M_MPolicyTicket_ID, String trxName) {
		super(ctx, M_MPolicyTicket_ID, trxName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * General constructor for a record set. 
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MMPolicyTicket(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Create a MMPolicyTicket record based on the contents of a MInOutLine.
	 * @param ctx
	 * @param line
	 * @param movementDate 
	 * @param get_TrxName
	 * @return If line is null or not a valid MInOutLine record, the function 
	 * will return null. Otherwise, the newly created ticket.
	 */
	public static MMPolicyTicket create(Properties ctx, IDocumentLine line,
			Timestamp movementDate, String trxName) {
		if (line == null)
			return null;
		
		MMPolicyTicket ticket = new MMPolicyTicket(ctx, 0, trxName);
		// TODO Client & Org ??
		ticket.setMovementDate(movementDate);
		
		// Set the reference to the line
		String lineColumnName = line.get_TableName()+"_ID";
		if (ticket.get_ColumnIndex(lineColumnName) < 0)
		{
			throw new AdempiereException(ticket.get_TableName() + " does not contain reference column for " + line);
		}
		ticket.set_ValueOfColumn(lineColumnName, line.get_ID());			
		ticket.saveEx();
		
		return ticket;
	}

}
