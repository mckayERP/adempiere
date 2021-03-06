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
package org.adempiere.engine.storage.rules;

import static org.eevolution.model.X_PP_Order_BOMLine.COMPONENTTYPE_Co_Product;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderBOMLine;

public class MPPOrderBOMLineStorageRule extends StorageEngineRuleAbstract
		implements StorageEngineRule<IDocumentLine, String> {

	protected static CLogger log = CLogger.getCLogger(MPPOrderBOMLineStorageRule.class);

	protected MPPOrder order = null;
	protected MPPOrderBOMLine bomLine = null;

	public MPPOrderBOMLineStorageRule() {

		super();

	}

	@Override
	public boolean matches(IDocumentLine line)
	{

		return (line instanceof MPPOrderBOMLine);

	}

	@Override
	public StorageTransactionInfo createTransactionInfo(IDocumentLine line)
	{

		order = (MPPOrder) requireNonNull(line.getParent());
		bomLine = (MPPOrderBOMLine) requireNonNull(line);

		boolean isSOTrxIsTrueWhenConsummed = !COMPONENTTYPE_Co_Product
				.equals(bomLine.getComponentType());

		return getASetOfStorageTransactionInfo()
				.withDocumentLine(bomLine)
				.withMovementDate(bomLine.getMovementDate())
				.withM_Product_ID(bomLine.getM_Product_ID())
				.withOrderWarehouse_id(bomLine.getM_Warehouse_ID())
				.withOrderAttributeSetInstance_id(bomLine.getM_AttributeSetInstance_ID())
				.withSOTrx(isSOTrxIsTrueWhenConsummed)
				.build();

	}

	@Override
	protected void allocateMaterialAndAssignTickets()
	{

		if (docLine.getM_MPolicyTicket_ID() <= 0)
		{

			docLine.setM_MPolicyTicket_ID(getNewMaterialPolicyTicketId());
			orderMPolicyTicket_id = docLine.getM_MPolicyTicket_ID();
			getLogger().config("New Material Policy Ticket=" + docLine);

		}

	}

	@Override
	protected void setStorageRelatedFieldsInModel()
	{

		BigDecimal qtyReserved = getStorageQtyReserved();

		BigDecimal qtyOrdered = getStorageQtyOrdered();

		bomLine.setQtyReserved(qtyReserved.add(qtyOrdered));

	}

	@Override
	protected void clearStorageRelatedFieldsInModel()
	{

		bomLine.setQtyReserved(Env.ZERO);

	}

	@Override
	protected BigDecimal getChangeInQtyOnHand()
	{

		return Env.ZERO;

	}

	@Override
	protected BigDecimal getChangeInQtyOrdered()
	{

		BigDecimal orderedDiff = Env.ZERO;
		if (!isSOTrx)
		{
			BigDecimal qtyOrdered = getStorageQtyOrdered();
			orderedDiff = bomLine.getQtyRequired().subtract(qtyOrdered);
		}		
		return orderedDiff;

	}

	@Override
	protected BigDecimal getChangeInQtyReserved()
	{

		BigDecimal reservedDiff = Env.ZERO;
		if (isSOTrx)
		{
			BigDecimal qtyReserved = getStorageQtyReserved();
			reservedDiff = bomLine.getQtyRequired().subtract(qtyReserved);
		}		
		return reservedDiff;

	}

}
