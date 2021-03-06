package org.adempiere.engine.storage.rules;

import static java.util.Objects.requireNonNull;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_CustomerReturns;
import static org.compiere.model.X_M_InOut.MOVEMENTTYPE_VendorReturns;

import java.math.BigDecimal;
import java.util.List;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MOrderLine;
import org.compiere.model.MRMALine;
import org.compiere.model.X_M_InOut;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

public class MInOutLineStorageRule extends StorageEngineRuleAbstract 
				implements StorageEngineRule<IDocumentLine, String> {

	protected static CLogger log = CLogger.getCLogger(MInOutLineStorageRule.class);
	
	protected StorageTransactionInfoBuilder infoBuilder = null;
	protected MInOutLine inOutLine = null;
	protected MOrderLine orderLine = null;
	
	public MInOutLineStorageRule() {
		super();
	}

	@Override
	public boolean matches(IDocumentLine line) {
		
		return (line instanceof MInOutLine);
		
	}

	@Override
	public StorageTransactionInfo createTransactionInfo(IDocumentLine line) {
		
		inOutLine = (MInOutLine) requireNonNull(line);
		MInOut inOut = (MInOut) requireNonNull(line.getParent());
				
		int orderWarehouseID = 0;
		int orderMPolicyTicketID = 0;
		int orderAttributeSetInstanceID = 0;
		orderLine = null;
		if (inOutLine.getC_OrderLine_ID() > 0)
		{
			
			orderLine = (MOrderLine) inOutLine.getC_OrderLine();

			orderWarehouseID = orderLine.getM_Warehouse_ID();
			orderAttributeSetInstanceID = orderLine.getM_AttributeSetInstance_ID();
			orderMPolicyTicketID = orderLine.getM_MPolicyTicket_ID();
			
		}
		
		return getASetOfStorageTransactionInfo()
					.withDocumentLine(line)
					.withMovementType(inOut.getMovementType())
					.withMovementDate(inOut.getMovementDate())
					.withMovementQty(inOutLine.getMovementQty())
					.withM_Product_ID(inOutLine.getM_Product_ID())
					.withTransactionAttributeSetInstance_id(inOutLine.getM_AttributeSetInstance_ID())
					.withTransactionWarehouse_id(inOutLine.getM_Warehouse_ID())
					.withTransactionLocator_id(inOutLine.getM_Locator_ID())
					.withOrderWarehouse_id(orderWarehouseID)
					.withOrderAttributeSetInstance_id(orderAttributeSetInstanceID)
					.withOrderMPolicyTicket_id(orderMPolicyTicketID)
					.withSOTrx(inOut.isSOTrx())
					.withCreateMaterialAllocation(!inOut.isReversal())
					.withProcessMA(true)
				.build();

	}

	@Override
	protected void allocateMaterialAndAssignTickets() {
		
		if (!getStorageTransactionInfo().isCreateMaterialAllocations())
			return;

    	deleteExistingMaterialAllocations();

    	if (applyRMAMaterialPolicyRules())
    		return;
    	
    	allocateMaterial();

	}

	protected boolean applyRMAMaterialPolicyRules() {

	    return matchAllocationsOnRMAtoOriginalInOutLine();
	    
	}

	private boolean matchAllocationsOnRMAtoOriginalInOutLine() {
		
	    if (inOutLine.getM_RMALine_ID() <= 0
	    	|| movementQty.signum() <= 0 )
	    {
	    	return false;
	    }
	    
	    BigDecimal quantity = movementQty;
	    
        MRMALine rmaLine = (MRMALine) inOutLine.getM_RMALine(); 
        MInOutLine originalIOLine = (MInOutLine) rmaLine.getM_InOutLine();
    	
    	if (isVendorReturn(movementType))
    	{
    		copyOriginalMaterialAllocationToRMA(quantity, originalIOLine);
    	}
    	else if (isCustomerReturn(movementType))
    	{
    		copyOriginalMaterialAllocationLinesToRMA(quantity, originalIOLine);
        }
    	else
    	{
    		throw new AdempiereException("TILT: RMALine_ID > 0 "
    				+ "but movementType is not a vendor or customer return: " + movementType);
    	}
    	return true;
	}

	private void copyOriginalMaterialAllocationLinesToRMA(BigDecimal quantity, 
			MInOutLine originalInOutLine) {
		
		List<MInOutLineMA> mas = getOriginalMaterialAllocations(originalInOutLine);
		
		BigDecimal qtyToReturn = quantity;  
		
		for (MInOutLineMA lineMA : mas)
		{
			
			if (lineMA.getMovementQty().compareTo(Env.ZERO) != 0)
			{
				BigDecimal qtyThisLineMA = lineMA.getMovementQty();
				if(qtyToReturn.compareTo(qtyThisLineMA) <= 0)
					qtyThisLineMA = qtyToReturn;
				
				createNewMaterialAllocationAndAttachToRMA(lineMA, qtyThisLineMA);			
				qtyToReturn = qtyToReturn.subtract(qtyThisLineMA);
				
			}
			
			if (qtyToReturn.compareTo(Env.ZERO) == 0)
				break;
		}
		
		if (qtyToReturn.compareTo(Env.ZERO) != 0)
		{
    		throw new AdempiereException("TILT: the quantities on the original "
    				+ "material allocations do not match the movementQty being returned. "
    				+ "The movementQty was " + movementQty + " and the total on the "
					+ "material allocations was " 
    				+ movementQty.subtract(qtyToReturn));			
		}
		
	}

	private void copyOriginalMaterialAllocationToRMA(BigDecimal quantity, 
			MInOutLine originalInOutLine) {
		
		int m_mPolicyTicket_id = originalInOutLine.getM_MPolicyTicket_ID();
		MInOutLineMA returnLineMA = getMInOutLineMA(m_mPolicyTicket_id, quantity);
		returnLineMA.setMovementType(movementType);
		returnLineMA.saveEx();
		
	}

	protected boolean isCustomerReturn(String movementType) {
		
		return MOVEMENTTYPE_CustomerReturns.equals(movementType);
		
	}

	protected boolean isVendorReturn(String movementType) {
		
		return MOVEMENTTYPE_VendorReturns.equals(movementType);
		
	}

	protected void createNewMaterialAllocationAndAttachToRMA(MInOutLineMA lineMA, BigDecimal qtyThisLineMA) {
		
		MInOutLineMA returnLineMA = new MInOutLineMA(inOutLine, lineMA.getM_MPolicyTicket_ID(), qtyThisLineMA);
		returnLineMA.saveEx();

	}

	protected List<MInOutLineMA> getOriginalMaterialAllocations(MInOutLine originalIOLine) {
		
		return MInOutLineMA.getInOrder(ctx, originalIOLine.getM_InOutLine_ID(), !fifo, trxName);
	}

	protected MInOutLineMA getMInOutLineMA(int m_mPolicyTicket_id, BigDecimal quantity) {
		
		return new MInOutLineMA(inOutLine, m_mPolicyTicket_id, quantity);
		
	}
	
	@Override
	protected void clearStorageRelatedFieldsInModel() {
		
		if (orderLine != null)
			orderLine.setQtyReserved(Env.ZERO);
		
	}

	@Override
	protected void setStorageRelatedFieldsInModel() {

		if (orderLine == null)
			return;
		
		if (isSOTrx) 
		{
			//	Purchase Orders are updated by Matching
			orderLine.setQtyDelivered(orderLine.getQtyDelivered().add(movementQty.negate()));
			orderLine.saveEx();
		}
		
	}

	@Override
	protected BigDecimal getChangeInQtyOnHand() {
		
		BigDecimal changeInQtyOnHand;
		if (MOVEMENTTYPE_VendorReturns.equals(info.getMovementType()))
		{
			changeInQtyOnHand = movementQty.negate();
		}
		else 
		{
			changeInQtyOnHand = movementQty;
		}
		return changeInQtyOnHand;
		
	}

	@Override
	protected BigDecimal getChangeInQtyOrdered() {

		BigDecimal changeInQtyOrdered;
		if (isSOTrx || orderLine == null)
		{
			
			changeInQtyOrdered = Env.ZERO;
			
		} 
		else if (MOVEMENTTYPE_VendorReturns.equals(info.getMovementType()))
		{
			changeInQtyOrdered = movementQty;
		}
		else 
		{
			changeInQtyOrdered = movementQty.negate();
		}
		return changeInQtyOrdered;
	
	}

	@Override
	protected BigDecimal getChangeInQtyReserved() {

		BigDecimal changeInQtyReserved;
		if (!isSOTrx || orderLine == null)
		{
			
			changeInQtyReserved = Env.ZERO;
			
		} 
		else if (MOVEMENTTYPE_CustomerReturns.equals(info.getMovementType()))
		{
			changeInQtyReserved = movementQty;
		}
		else 
		{
			changeInQtyReserved = movementQty.negate();
		}
		return changeInQtyReserved;
		
	}

}
