package org.adempiere.engine.storage.rules;

import static java.util.Objects.requireNonNull;
import static org.adempiere.engine.storage.StorageTransactionInfoBuilder.aSetOfStorageTransactionInfo;

import java.math.BigDecimal;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MStorage;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;

public class MPPOrderStorageRule extends StorageEngineRuleAbstract 
				implements StorageEngineRule<IDocumentLine, String> {


	protected MPPOrder order = null;

	@Override
	public boolean matches(IDocumentLine line) {
		
		return (line instanceof MPPOrder && line.getParent() == null );
		
	}

	@Override
	public StorageTransactionInfo createTransactionInfo(IDocumentLine line) {
		
		order = (MPPOrder) requireNonNull(line);
		
		return this.getASetOfStorageTransactionInfo()
					.withDocumentLine(line)
					.withMovementDate(order.getDateOrdered())
					.withM_Product_ID(order.getM_Product_ID())
					.withOrderWarehouse_id(order.getM_Warehouse_ID())
					.withOrderAttributeSetInstance_id(order.getM_AttributeSetInstance_ID())
					.withSOTrx(false)
				.build();

	}

	@Override
	protected void allocateMaterialAndAssignTickets() {

		//  Order document lines have one material policy ticket per line
		
		if(docLine.getM_MPolicyTicket_ID() <= 0) 
		{
			
			docLine.setM_MPolicyTicket_ID(getNewMaterialPolicyTicketId());
					
			getLogger().config("New Material Policy Ticket=" + docLine);	

		}
		orderMPolicyTicket_id = docLine.getM_MPolicyTicket_ID();

	}

	@Override
	protected void setStorageRelatedFieldsInModel() {

		order.setQtyReserved(getStorageQtyOrdered());
		order.saveEx();
		
	}
	
	@Override
	protected void clearStorageRelatedFieldsInModel() {
		
		order.setQtyReserved(Env.ZERO);
		order.saveEx();
	}

	@Override
	protected BigDecimal getChangeInQtyOnHand() {

		return Env.ZERO;
		
	}

	@Override
	protected BigDecimal getChangeInQtyOrdered() {
		
		return order.getQtyOrdered().subtract(getStorageQtyOrdered());
		
	}

	@Override
	protected BigDecimal getChangeInQtyReserved() {
		
		return Env.ZERO;
	}


}
