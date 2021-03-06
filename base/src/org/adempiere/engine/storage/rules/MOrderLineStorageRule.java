package org.adempiere.engine.storage.rules;

import static org.adempiere.engine.storage.StorageTransactionInfoBuilder.aSetOfStorageTransactionInfo;
import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MStorage;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

public class MOrderLineStorageRule extends StorageEngineRuleAbstract 
				implements StorageEngineRule<IDocumentLine, String> {

		
	boolean onlyIfOrderIsBinding = true;
	
	protected MOrderLine orderLine = null;

	public MOrderLineStorageRule() {
		super();
	}

	@Override
	public boolean matches(IDocumentLine line) {
		
		return (line instanceof MOrderLine && line.getParent() instanceof MOrder );
		
	}

	@Override
	public StorageTransactionInfo createTransactionInfo(IDocumentLine line) {
		
		orderLine = (MOrderLine) requireNonNull(line);
		MOrder order = (MOrder) requireNonNull(line.getParent());
		
		onlyIfOrderIsBinding = order.isOrderBinding();
		
		return aSetOfStorageTransactionInfo()
					.withDocumentLine(line)
					.withMovementDate(order.getDateOrdered())
					.withM_Product_ID(orderLine.getM_Product_ID())
					.withOrderWarehouse_id(orderLine.getM_Warehouse_ID())
					.withOrderMPolicyTicket_id(orderLine.getM_MPolicyTicket_ID())
					.withOrderAttributeSetInstance_id(orderLine.getM_AttributeSetInstance_ID())
					.withSOTrx(order.isSOTrx())
					.withCreateMaterialAllocation(onlyIfOrderIsBinding)
					.withUpdateStrorage(onlyIfOrderIsBinding)
					.withProcessMA(onlyIfOrderIsBinding)
				.build();

	}

	@Override
	protected void allocateMaterialAndAssignTickets() {

		//  Order document lines have one material policy ticket per line
		
		// Don't create Material Policy Tickets if the order is not binding
		if (!onlyIfOrderIsBinding)
			return;
		
		if(docLine.getM_MPolicyTicket_ID() <= 0) 
		{
			
			docLine.setM_MPolicyTicket_ID(getNewMaterialPolicyTicketId());
					
			orderMPolicyTicket_id = docLine.getM_MPolicyTicket_ID();
			
			getLogger().config("New Material Policy Ticket=" + docLine);	

		}

	}

	@Override
	protected BigDecimal getChangeInQtyReserved () {
		
		if (!isSOTrx)
			return Env.ZERO;;

		// Get the actual reserved qty and determine the difference based on the line
		BigDecimal qtyReserved = getStorageQtyReserved();

		return orderLine.getQtyOrdered().subtract(qtyReserved);

	}

	@Override
	protected BigDecimal getChangeInQtyOrdered () {
		
		if (isSOTrx)
			return Env.ZERO;
					
		// Get the actual ordered qty and determine the difference based on the line
		BigDecimal qtyOrdered = getStorageQtyOrdered();
		
		return orderLine.getQtyOrdered().subtract(qtyOrdered);
		
	}
	
	@Override
	protected BigDecimal getChangeInQtyOnHand () {
		return Env.ZERO;
	}

	@Override
	protected void clearStorageRelatedFieldsInModel() {
		
		orderLine.setQtyReserved(Env.ZERO);
		
	}

	@Override
	protected void setStorageRelatedFieldsInModel() {
		
		BigDecimal qtyReserved = getStorageQtyReserved();

		BigDecimal qtyOrdered = getStorageQtyOrdered();

		// At least one will be zero
		orderLine.setQtyReserved(qtyOrdered.add(qtyReserved));

	}

}
