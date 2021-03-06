package org.adempiere.engine.storage;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MClient;
import org.compiere.model.MLocator;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRefList;
import org.compiere.model.MTransaction;
import org.compiere.model.PO;
import org.compiere.util.Env;
import org.eevolution.model.MPPOrder;
import org.eevolution.model.MPPOrderBOMLine;

public class StorageTransactionInfoBuilder {
	
	private StorageTransactionInfo transactionInfo;

	public static final String NO_DOCUMENT_LINE = "Document Line can not be null.";
	public static final String UNKNOWN_MOVEMENT_TYPE = "Unknown Movement Type: ";
	public static final String NO_MOVEMENT_TYPE = "No Movement Type defined. A valid MovementType is required.";
	public static final String DOCUMENT_LINE_NOT_SAVED = "Document line has ID <= 0. The document line should be saved and have a valid ID.";
	public static final String NO_MOVEMENT_DATE = "The Movement Date cannot be null.";
	public static final String NO_PRODUCT_DEFINED = "The Product ID is not set.";
	public static final String NO_WAREHOUSE_DEFINED = "No transaction Warehouse ID is set.  A transaction warehouse is required.";
	public static final String NO_LOCATOR_DEFINED = "No Locator ID is set.  A transaction locator is required.";
	public static final String LOCATOR_NOT_IN_WAREHOUSE = "The Locator provided is in a different warehouse.";
	public static final String NO_MOVEMENT_QTY = "The Movement Quantity has not been set.";
	public static final String NO_ORDER_WAREHOUSE_DEFINED = "No order Warehouse ID is set.  An order warehouse is required.";
	
	
	public static StorageTransactionInfoBuilder aSetOfStorageTransactionInfo() {
		return new StorageTransactionInfoBuilder();
	}

	public StorageTransactionInfoBuilder() {
		transactionInfo = new StorageTransactionInfo();
	}

	public StorageTransactionInfoBuilder setTransactionInfo(StorageTransactionInfo info) {
		transactionInfo = info;
		return this;
	}

	public StorageTransactionInfoBuilder withDocumentLine(IDocumentLine line) {
		transactionInfo.setDocumentLine(line);
		return this;
	}
	
	public StorageTransactionInfoBuilder withMovementType(String movementType) {
		transactionInfo.setMovementType(movementType);
		return this;
	}
	
	public StorageTransactionInfoBuilder withMovementDate(Timestamp movementDate) {
		transactionInfo.setMovementDate(movementDate);
		return this;
	}

	public StorageTransactionInfoBuilder withMovementQty(BigDecimal movementQty) {
		transactionInfo.setMovementQty(movementQty);
		return this;
	}
	
	
	public StorageTransactionInfoBuilder withM_Product_ID(int m_product_id) {
		transactionInfo.setM_Product_ID(m_product_id);
		return this;
	}

	public StorageTransactionInfoBuilder withUseToFields(boolean useToFields) {
		transactionInfo.setUseToFields(useToFields);
		return this;
	}
	
	public StorageTransactionInfoBuilder withDeleteExistingMAEntries(boolean deleteExistingMAEntries) {
		transactionInfo.setDeleteExistingMAEntries(deleteExistingMAEntries);
		return this;
	}

	public StorageTransactionInfoBuilder withTransactionAttributeSetInstance_id(int  m_attributeSetInstance_id) {
		transactionInfo.setTransactionAttributeSetInstance_id(m_attributeSetInstance_id);
		return this;
	}
	
	public StorageTransactionInfoBuilder withTransactionMPolicyTicket_id(int  m_mPolicyTicket_id) {
		transactionInfo.setTransactionMPolicyTicket_id(m_mPolicyTicket_id);
		return this;
	}
	
	public StorageTransactionInfoBuilder withOrderMPolicyTicket_id(int  m_mPolicyTicket_id) {
		transactionInfo.setOrderMPolicyTicket_id(m_mPolicyTicket_id);
		return this;
	}
	
	public StorageTransactionInfoBuilder withTransactionWarehouse_id(int  m_warehouse_id) {
		transactionInfo.setTransactionWarehouse_id(m_warehouse_id);
		return this;
	}
	
	public StorageTransactionInfoBuilder withOrderWarehouse_id(int m_warehouse_id) {
		transactionInfo.setOrderWarehouse_id(m_warehouse_id);
		return this;
	}
	
	public StorageTransactionInfoBuilder withTransactionLocator_id(int m_locator_id) {
		transactionInfo.setTransactionLocator_id(m_locator_id);
		return this;
	}
	
	public StorageTransactionInfoBuilder withOrderAttributeSetInstance_id(int  m_attributeSetInstance_id) {
		transactionInfo.setOrderAttributeSetInstance_id(m_attributeSetInstance_id);
		return this;
	}
	
	public StorageTransactionInfoBuilder withCreateMaterialAllocation(boolean createMA) {
		transactionInfo.setCreateMA(createMA);
		return this;
	}
	
	public StorageTransactionInfoBuilder withSOTrx(boolean isSOTrx) {
		transactionInfo.setSOTrx(isSOTrx);
		return this;
	}
	
	public StorageTransactionInfoBuilder withProcessMA(boolean processMA) {
		transactionInfo.setProcessMA(processMA);
		return this;
	}
	
	public StorageTransactionInfoBuilder withUpdateStrorage(boolean updateStorage) {
		transactionInfo.setUpdateStorage(updateStorage);
		return this;
	}

	public StorageTransactionInfoBuilder withIsIncomingTransaction(boolean isIncomingTransaction) {
		transactionInfo.setIsIncomingTransaction(isIncomingTransaction);
		return this;
	}
	
	public StorageTransactionInfoBuilder withIsFifoPolicy(boolean isFifoPolicy) {
		transactionInfo.setIsFifoPolicy(isFifoPolicy);
		return this;
	}
	
	public StorageTransactionInfo build() {
		
		if (transactionInfo.getDocumentLine() == null)
			throw new AdempiereException(NO_DOCUMENT_LINE);

		if (((PO) transactionInfo.getDocumentLine()).get_ID() <= 0)
			throw new AdempiereException(DOCUMENT_LINE_NOT_SAVED);

		if(transactionInfo.getM_Product_ID() <= 0)
			throw new AdempiereException(NO_PRODUCT_DEFINED);
		
		if (transactionInfo.getMovementDate() == null)
			throw new AdempiereException(NO_MOVEMENT_DATE);

		if (notAnOrderType()) 
		{ 
			if(transactionInfo.getTransactionWarehouse_id() <= 0)
				throw new AdempiereException(NO_WAREHOUSE_DEFINED);

			if(transactionInfo.getTransactionLocator_id() <= 0)
				throw new AdempiereException(NO_LOCATOR_DEFINED);
			
			if(transactionInfo.getTransactionWarehouse_id() 
					!= getMLocator().getM_Warehouse_ID())
				throw new AdempiereException(LOCATOR_NOT_IN_WAREHOUSE);
			
			if (transactionInfo.getMovementType().isEmpty())
				throw new AdempiereException(NO_MOVEMENT_TYPE);
			
			if (getRefList() == null)
				throw new AdempiereException(UNKNOWN_MOVEMENT_TYPE + ": " + transactionInfo.getMovementType());

			if (transactionInfo.getMovementQty() == null || transactionInfo.getMovementQty().signum() == 0)
				throw new AdempiereException(NO_MOVEMENT_QTY);

			setFifoPolicyType();
			setTransactionDirection();
		}
		else
		{
			if(transactionInfo.getOrderWarehouse_id() <= 0)
				throw new AdempiereException(NO_ORDER_WAREHOUSE_DEFINED);
			
			transactionInfo.setMovementQty(Optional.ofNullable(transactionInfo.getMovementQty()).orElse(Env.ZERO));
			
		}

		return transactionInfo;
	}

	private void setTransactionDirection() {
		
		//	Incoming Trx are positive receipts or negative shipments
		boolean incomingTrx = MTransaction.isIncomingTransaction(transactionInfo.getMovementType()) 
								&& transactionInfo.getMovementQty().signum() >= 0
							|| !MTransaction.isIncomingTransaction(transactionInfo.getMovementType()) 
								&& transactionInfo.getMovementQty().signum() < 0;	//	V+ Vendor Receipt

		transactionInfo.setIsIncomingTransaction(incomingTrx);
		
	}
	
	public void setFifoPolicyType() {
		
		if (transactionInfo.isFifoPolicyOptional().isPresent())
			return;
		
		MProduct product = getProduct();
		transactionInfo.setIsFifoPolicy(MClient.MMPOLICY_FiFo.equals(product.getMMPolicy()));
		
	}

	public MProduct getProduct() {
		
		return MProduct.get(transactionInfo.getDocumentLine().getCtx(), transactionInfo.getM_Product_ID(), 
				transactionInfo.getDocumentLine().get_TrxName());
	}

	public boolean notAnOrderType() {
		
		return ( ! (transactionInfo.getDocumentLine() instanceof MOrderLine
			|| transactionInfo.getDocumentLine() instanceof MPPOrder
			|| transactionInfo.getDocumentLine() instanceof MPPOrderBOMLine));
	}

	public MLocator getMLocator() {
		
		return MLocator.get(((PO) transactionInfo.getDocumentLine()).getCtx(), transactionInfo.getTransactionLocator_id());
		
	}
	
	public MRefList getRefList() {
		
		return MRefList.get(Env.getCtx(), MTransaction.MOVEMENTTYPE_AD_Reference_ID, transactionInfo.getMovementType(), null);
		
	}


}
