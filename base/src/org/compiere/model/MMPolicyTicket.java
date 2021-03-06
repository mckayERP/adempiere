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

import static java.util.Objects.requireNonNull;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;

/**
 * This class controls the life-cycle of Material Policy Tickets which are used
 * to ensure that material received or shipped follows the material policy of
 * fifo/lifo. Each addition to inventory for a product is given a material
 * policy ticket and this ticket is included in the document line that added
 * that product. The material policy is used to select the tickets that will be
 * used to fulfill draw-downs on inventory. These are typically added to
 * Material Allocations o the particular document lines.<br>
 * <br>
 * Storage entries provide a means to link the product, Attribute Set Instance
 * (ASI) and material policy ticket. The tickets are also related to the cost
 * details for the transactions that create or consume the tickets.<br>
 * <br>
 * 
 * @since 3.9.0 - prior to 3.9.0, the material attribute set instances were used
 *        as tickets. See
 *        <a href="https://github.com/adempiere/adempiere/issues/453">BR 453
 *        Attribute Set Instances are used to track FIFO/LIFO. Another method is
 *        required.</a>
 * 
 * @see org.compiere.model.MInOut
 * 
 * @author Michael McKay, mckayERP (michael.mckay@mckayERP.com)
 *
 */
public class MMPolicyTicket extends X_M_MPolicyTicket {

	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5905583402118502877L;

	private CLogger logger = CLogger.getCLogger(this.getClass());

	private static void copyTransactionFieldValuesToTheTicket(MTransaction transaction,
			MMPolicyTicket ticket)
	{

		int count = transaction.get_ColumnCount();
		for (int i = 0; i < count; i++)
		{

			String columnName = transaction.get_ColumnName(i);
			if (isAColumnThatShouldBeCopied(columnName))
			{

				copyTheColumnFieldValues(transaction, ticket, i, columnName);

			}

		}

	}

	private static void copyTheColumnFieldValues(MTransaction transaction, MMPolicyTicket ticket,
			int i, String columnName)
	{

		int index = ticket.get_ColumnIndex(columnName);
		if (index >= 0)
		{

			ticket.set_Value(index, transaction.get_Value(i));

		} else
		{

			if (columnName.endsWith("_ID"))
			{
				ticket.logger.info("TILT: " + ticket.get_TableName()
						+ " does not contain reference column for " + columnName);

			}

		}

	}

	private static boolean isAColumnThatShouldBeCopied(String columnName)
	{

		return !(I_M_MPolicyTicket.COLUMNNAME_M_MPolicyTicket_ID.equalsIgnoreCase(columnName)
				|| "UUID".equalsIgnoreCase(columnName)
				|| "Created".equalsIgnoreCase(columnName)
				|| "Updated".equalsIgnoreCase(columnName)
				|| I_M_Transaction.COLUMNNAME_M_Product_ID.equalsIgnoreCase(columnName)
				|| I_M_Transaction.COLUMNNAME_M_AttributeSetInstance_ID
						.equalsIgnoreCase(columnName)
				|| I_M_Transaction.COLUMNNAME_M_Locator_ID.equalsIgnoreCase(columnName)
				|| I_M_Transaction.COLUMNNAME_M_Transaction_ID.equalsIgnoreCase(columnName));

	}

	private static boolean isNew(MMPolicyTicket ticket)
	{

		return ticket.getM_MPolicyTicket_ID() <= 0;

	}

	/**
	 * General constructor for an existing M_MPolicyTicket_ID. If no record is found
	 * with that ID, a new record will be returned with ID of 0.
	 * 
	 * @param ctx
	 * @param policyTicket_id
	 * @param trxName
	 */
	public MMPolicyTicket(Properties ctx, int policyTicket_id, String trxName) {

		super(ctx, policyTicket_id, trxName);

	}

	/**
	 * General constructor for a record set.
	 * 
	 * @param ctx
	 * @param rs
	 * @param trxName
	 */
	public MMPolicyTicket(Properties ctx, ResultSet rs, String trxName) {

		super(ctx, rs, trxName);

	}

	/**
	 * Create a MMPolicyTicket record based on the contents of a document line.
	 * 
	 * @param StorageTransactionInfo
	 * @return the newly created ticket.
	 */
	public static MMPolicyTicket create(StorageTransactionInfo info)
	{

		IDocumentLine line = requireNonNull(info.getDocumentLine());

		MMPolicyTicket ticket = new MMPolicyTicket(line.getCtx(), 0, line.get_TrxName());

		ticket.setMovementDate(info.getMovementDate());

		// Set the reference to the line
		String lineColumnName = line.get_TableName() + "_ID";
		if (ticket.get_ColumnIndex(lineColumnName) < 0)
		{
			throw new AdempiereException(
					ticket.get_TableName()
							+ " does not contain reference column for " + line);
		}
		ticket.set_ValueOfColumn(lineColumnName, line.get_ID());

		ticket.saveEx();
		return ticket;

	}

	/**
	 * Get or Create a MMPolicyTicket record based on the contents of a material
	 * transaction.
	 * 
	 * @param ctx
	 * @param line
	 * @param movementDate
	 * @param get_TrxName
	 * @return the newly created ticket.
	 */
	public static MMPolicyTicket getOrCreateFromTransaction(Properties ctx,
			MTransaction transaction, String trxName)
	{

		if (transaction == null)
			return null;

		int id = transaction.getM_MPolicyTicket_ID();
		MMPolicyTicket ticket = new MMPolicyTicket(ctx, id, trxName);

		if (isNew(ticket))
		{
			copyTransactionFieldValuesToTheTicket(transaction, ticket);
			ticket.saveEx();
		}
		return ticket;

	}

}
