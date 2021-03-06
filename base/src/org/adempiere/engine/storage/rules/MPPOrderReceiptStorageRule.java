package org.adempiere.engine.storage.rules;

import static java.util.Objects.requireNonNull;

import java.math.BigDecimal;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderReceipt;

public class MPPOrderReceiptStorageRule extends StorageEngineRuleAbstract {

	protected MPPOrder order;

	@Override
	public boolean matches(IDocumentLine line) {
		
		return line instanceof MPPOrderReceipt && line.getParent() instanceof MPPOrder;

	}

	@Override
	protected StorageTransactionInfo createTransactionInfo(IDocumentLine line) {

		order = (MPPOrder) requireNonNull(line.getParent());
		MPPOrderReceipt receipt = (MPPOrderReceipt) requireNonNull(line);

		int orderWarehouseID = 0;
		int orderMPolicyTicketID = 0;
		int orderAttributeSetInstanceID = 0;

		orderWarehouseID = order.getM_Warehouse_ID();
		orderAttributeSetInstanceID = order.getM_AttributeSetInstance_ID();
		orderMPolicyTicketID = order.getM_MPolicyTicket_ID();

		return getASetOfStorageTransactionInfo()
				.withDocumentLine(line)
				.withMovementType(receipt.getMovementType())
				.withMovementDate(receipt.getMovementDate())
				.withMovementQty(receipt.getMovementQty())
				.withM_Product_ID(receipt.getM_Product_ID())
				.withTransactionAttributeSetInstance_id(receipt.getM_AttributeSetInstance_ID())
				.withTransactionWarehouse_id(receipt.getM_Warehouse_ID())
				.withTransactionLocator_id(receipt.getM_Locator_ID())
				.withOrderWarehouse_id(orderWarehouseID)
				.withOrderAttributeSetInstance_id(orderAttributeSetInstanceID)
				.withOrderMPolicyTicket_id(orderMPolicyTicketID)
				.withSOTrx(receipt.isSOTrx())
				.withCreateMaterialAllocation(!receipt.isReversal())
				.withProcessMA(true)
				.build();

	}

	@Override
	protected void setStorageRelatedFieldsInModel() {

		order.setQtyReserved(getStorageQtyOrdered());
		order.setQtyDelivered(order.getQtyDelivered().add(movementQty));
		order.saveEx();

	}

	@Override
	protected void clearStorageRelatedFieldsInModel() {

		order.setQtyDelivered(Env.ZERO);
		order.saveEx();

	}

	@Override
	protected BigDecimal getChangeInQtyOnHand() {
		return movementQty;
	}

	@Override
	protected BigDecimal getChangeInQtyOrdered() {
		return getChangeInQtyOnHand().negate();
	}

	@Override
	protected BigDecimal getChangeInQtyReserved() {
		return Env.ZERO;
	}

}
