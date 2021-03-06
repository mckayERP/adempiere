package org.adempiere.engine.storage.rules;

import static java.util.Objects.requireNonNull;
import static org.adempiere.engine.storage.StorageTransactionInfoBuilder.aSetOfStorageTransactionInfo;
import static org.compiere.model.MTransaction.createTransaction;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.IInventoryAllocation;
import org.adempiere.engine.storage.IStorageEngine;
import org.adempiere.engine.storage.StorageEngine;
import org.adempiere.engine.storage.StorageTransactionInfo;
import org.adempiere.engine.storage.StorageTransactionInfoBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOutLineMA;
import org.compiere.model.MLocator;
import org.compiere.model.MMPolicyTicket;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MTable;
import org.compiere.model.MTransaction;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

public abstract class StorageEngineRuleAbstract implements StorageEngineRule<IDocumentLine, String> {

	protected StorageTransactionInfo info = null;
	protected MMPolicyTicket ticket = null;
	protected IDocumentLine docLine = null;
	protected String movementType = "";
	protected BigDecimal movementQty = null;
	protected Timestamp movementDate = null;
	protected int ad_org_id = 0;
	protected int m_product_id = 0;
	protected int transactionLocator_id = 0;
	protected int transactionWarehouse_id = 0;
	protected int transactionMPolicyTicket_id = 0;
	protected int transactionAttributeSetInstance_id = 0;
	protected int orderWarehouse_id = 0;
	protected int orderAttributeSetInstance_id = 0;
	protected int orderMPolicyTicket_id = 0;
	protected boolean isSOTrx =  true;
	protected boolean createMA =  false;
	protected boolean isProcessMA = true;
	protected boolean updateStorage = true;
	protected boolean productIsStocked = false;
	protected Properties ctx = null;
	protected String trxName = null;
	protected MLocator locator = null;
    protected MProduct product = null;
    protected boolean fifo;
    protected BigDecimal changeInQtyOnHand = null;
    protected BigDecimal changeInQtyReserved = null;
    protected BigDecimal changeInQtyOrdered = null;

	protected static CLogger log = CLogger.getCLogger(StorageEngineRuleAbstract.class); 
	
	protected String error_msg = "An error occured.";
	
	// For testing injection
	protected StorageTransactionInfoBuilder storageTransactionInfoBuilder;

	public String process(IDocumentLine line) {
		
		try {
			setStorageTransactionInfo(createTransactionInfo(line));
	
			if (productIsStocked) 
			{
			
				allocateMaterialAndAssignTickets();
				setChangeInQuantities();
				updateStorageRecords();
				setStorageRelatedFieldsInModel();
				
			}
			else
			{
				
				clearStorageRelatedFieldsInModel();
				
			}
			save(line);
			return "";
		}
		catch (AdempiereException e)
		{
			return e.getMessage();
		}
		
	}
	
	protected CLogger getLogger() {
		
		return log;
		
	}

	protected StorageTransactionInfo getStorageTransactionInfo() {
		return info;
	}
	
	protected void setStorageTransactionInfo(StorageTransactionInfo info) {

		requireNonNull(info, "StorageTransactionInfo must be non null");
		
		this.info = info;
		
		docLine = info.getDocumentLine();
		movementType = info.getMovementType();
		movementQty = info.getMovementQty();
		movementDate = info.getMovementDate();
		ad_org_id = docLine.getAD_Org_ID();
		m_product_id = info.getM_Product_ID();
		transactionLocator_id = info.getTransactionLocator_id();
		transactionWarehouse_id = info.getTransactionWarehouse_id();
		transactionAttributeSetInstance_id = info.getTransactionAttributeSetInstance_id();
		orderWarehouse_id = info.getOrderWarehouse_id();
		orderMPolicyTicket_id = info.getOrderMPolicyTicket_id();
		orderAttributeSetInstance_id = info.getOrderAttributeSetInstance_id();
		isSOTrx =  info.isSOTrx();
		createMA = info.isCreateMaterialAllocations();
		isProcessMA = info.isProcessMA();
		updateStorage = info.isUpdateStorage();
		fifo = info.isFifoPolicy();
		ctx = docLine.getCtx();
		trxName = docLine.get_TrxName();
		
		product = MProduct.get(ctx, m_product_id, trxName);
		productIsStocked = product != null && product.isStocked();
		
	}
	
	protected void save(IDocumentLine line)
	{
		if (line.getParent() != null) line.getParent().saveEx();
		((PO) line).saveEx();
	}

	protected void allocateMaterialAndAssignTickets() {
		
		if (!info.isCreateMaterialAllocations())
			return;
		
    	deleteExistingMaterialAllocations();
    	allocateMaterial();


	}

	private void createMaterialTransaction() {
		
		if (movementQty.signum() == 0)
			return;
		
		MTransaction mtrx = createTransaction(docLine, ad_org_id, transactionLocator_id, 
				m_product_id, transactionAttributeSetInstance_id, movementType, movementDate, transactionMPolicyTicket_id, movementQty);
		setReferenceLine_ID(mtrx, docLine);
		mtrx.saveEx();

	}
	
	protected boolean updateStorageRecords() {
		
		if (!isProcessMA) return false;
		
		if (updateStorage)
		{
						
			requireNonNull(changeInQtyOnHand);  // TODO - may not be required
			requireNonNull(changeInQtyReserved);
			requireNonNull(changeInQtyOrdered);

	
			getMaterialAllocations().stream()
			.forEach(ma -> {
				
				if (ma.getM_MPolicyTicket_ID() == 0)
					throw new AdempiereException ("@Error@ @FillMandatory@ @M_MPolicyTicket_ID@");

				// If the MA movement type is null, use the passed in value
				if (ma.getMovementType() != null && ma.getMovementType().length() > 0)
				{
					movementType = ma.getMovementType();
				}	
	
				boolean incomingTrx = true;
				
				if (!movementType.isEmpty())
					incomingTrx = MTransaction.isIncomingTransaction(movementType); 

				movementQty = Optional.ofNullable(ma.getMovementQty()).orElse(Env.ZERO);  // the movement qty - will be zero for orders.
				if (!incomingTrx)
					movementQty = movementQty.negate();		// Outgoing - reducing inventory

				transactionMPolicyTicket_id = ma.getM_MPolicyTicket_ID();
				
				if (ma.isUseToFields())
				{
					
					transactionLocator_id = docLine.getM_LocatorTo_ID();
					transactionAttributeSetInstance_id = docLine.getM_AttributeSetInstanceTo_ID();
					
				}
				else
				{
					
					transactionLocator_id = docLine.getM_Locator_ID();
					transactionAttributeSetInstance_id = docLine.getM_AttributeSetInstance_ID();

				}	
				
				if(!MStorage.add(ctx, 
						transactionWarehouse_id,
						orderWarehouse_id,
						transactionLocator_id,
						m_product_id, 
						transactionAttributeSetInstance_id, 
						orderAttributeSetInstance_id,
						transactionMPolicyTicket_id,
						orderMPolicyTicket_id,
						movementQty,
						changeInQtyReserved,
						changeInQtyOrdered,
						trxName))
				{
					throw new AdempiereException(IStorageEngine.MSG_UNABLE_TO_UPDATE_INVENTORY); // TODO Translate
				}

				createMaterialTransaction();
				
			});
		}
		
		return true;
			
	}
	
	private List<IInventoryAllocation> getMaterialAllocations() {
		
		List<IInventoryAllocation> list = getExistingMaterialAllocations();
		
		//  If the list is empty, add a dummy entry using the 
		//  data from the document line.
		if(list.size() == 0)
		{
			IInventoryAllocation dummy = new MInOutLineMA(docLine.getCtx(), 0, docLine.get_TrxName());
			dummy.setM_MPolicyTicket_ID(docLine.getM_MPolicyTicket_ID());
			dummy.setMovementType(docLine.getMovementType());
			dummy.setMovementQty(Optional.ofNullable(docLine.getMovementQty()).orElse(Env.ZERO));
			dummy.setUseToFields(false);
			
			list.add(dummy);	
		}
		
		return list;
	}

	private void setChangeInQuantities() {
		
		changeInQtyOnHand = Optional.ofNullable(getChangeInQtyOnHand()).orElse(Env.ZERO);
		changeInQtyOrdered = Optional.ofNullable(getChangeInQtyOrdered()).orElse(Env.ZERO);
		changeInQtyReserved = Optional.ofNullable(getChangeInQtyReserved()).orElse(Env.ZERO);
		
	}
	
	protected abstract StorageTransactionInfo createTransactionInfo(IDocumentLine line);
	protected abstract void setStorageRelatedFieldsInModel();
	protected abstract void clearStorageRelatedFieldsInModel();
	protected abstract BigDecimal getChangeInQtyOnHand();
	protected abstract BigDecimal getChangeInQtyOrdered();
	protected abstract BigDecimal getChangeInQtyReserved();

	protected void allocateToOutgoingTransactions()
		{
	
			if (info.isIncomingTransaction())
				return;
	
			BigDecimal qtyToDeliver = info.getMovementQty().abs(); // Must be positive
			
			IDocumentLine line = info.getDocumentLine();
			int m_product_id = info.getM_Product_ID();
			int m_attributeSetInstance_id = info.getTransactionAttributeSetInstance_id();
			int m_warehouse_id = info.getTransactionWarehouse_id();
			int m_locator_id = info.getTransactionLocator_id();
			boolean fifo = info.isFifoPolicy();
			String movementType = info.getMovementType();
			boolean useToFields = info.isUseToFields();
			Timestamp movementDate = info.getMovementDate();
	
			Timestamp minGuaranteeDate = movementDate;
			
			MStorage[] storages = MStorage.getWarehouse(line.getCtx(), m_warehouse_id, 
					line.getM_Product_ID(), m_attributeSetInstance_id, 0, 
					minGuaranteeDate, fifo, true, m_locator_id, line.get_TrxName());
			
			
			for (MStorage storage : storages)
			{	
				
				if (storage.getQtyOnHand().compareTo(qtyToDeliver) >= 0)
				{
					
					createMA (line, storage.getM_MPolicyTicket_ID(), movementType, qtyToDeliver, useToFields);
					qtyToDeliver = Env.ZERO;
					
				}
				else
				{	
					
					createMA (line, storage.getM_MPolicyTicket_ID(), movementType, storage.getQtyOnHand(), useToFields);
					qtyToDeliver = qtyToDeliver.subtract(storage.getQtyOnHand());
					getLogger().fine("QtyToDeliver=" + qtyToDeliver);
					
				}
	
				if (qtyToDeliver.signum() == 0)
					break;
			}
	
			if (qtyToDeliver.signum() != 0)
			{
				// There is not enough stock to deliver this shipment. 
				// TODO - this should trigger a way to balance costs or fire alarms - outgoing shipments 
				// could have accounting with a generic cost guess (Steve's Shipment Plan for example).
				// The balancing incoming transaction could have accounting to reverse the generic 
				// cost and add the correct one.  This is left as a TODO.
				
				MMPolicyTicket ticket = MMPolicyTicket.create(info);
				createMA (line, ticket.getM_MPolicyTicket_ID(), movementType, qtyToDeliver, useToFields);
				
	 
				// For now, remove any Material Allocations already created and throw an error as
				// we shouldn't generate a zero cost transaction.
	//				log.warning(line + ", Insufficient quantity. Process later.");
	//				deleteMA(line);
	//				throw new AdempiereException("Insufficient quantity to deliver line " + line);
			}
			
			((PO) line).saveEx();
		}

	protected void allocateToIncomingTransactions()
	{
		if (!info.isIncomingTransaction())
			return;
		
		//	Material Policy Tickets - used to track the FIFO/LIFO
		//  Create a Material Policy Ticket ID for any incoming transaction
		//  Where there is negative material on-hand, receive the new material using the ticket
		//  of the negative material.  This assumes the material receipt is a correction of the 
		//  cause of the negative quantity.  A single ticket is used as the costs are unique for 
		//  each receipt line.
		BigDecimal qtyReceived = info.getMovementQty().abs(); // Must be positive
		
		IDocumentLine line = info.getDocumentLine();
		int m_product_id = info.getM_Product_ID();
		int m_attributeSetInstance_id = info.getTransactionAttributeSetInstance_id();
		int m_warehouse_id = info.getTransactionWarehouse_id();
		int m_locator_id = info.getTransactionLocator_id();
		boolean fifo = info.isFifoPolicy();
		String movementType = info.getMovementType();
		boolean useToFields = info.isUseToFields();
		Timestamp movementDate = info.getMovementDate();
		
		//  Find the storage locations to use.  Use the locator, or if that is zero, search the 
		//  whole warehouse. Prioritize any negative quantity-on-hand and apply incoming material
		//  to the associated ticket to correct the inventory balance.
		MStorage[] storages = MStorage.getWarehouse(line.getCtx(), m_warehouse_id, m_product_id, m_attributeSetInstance_id, 0,
				null, fifo, false, m_locator_id, line.get_TrxName());
		
		
		for (MStorage storage : storages) 
		{
	
			// TODO Add a check of the inventory status to verify if filling negative inventory is
			// the correct behaviour.  What is here has implications on costing.
			
			// If there is negative storage ...
			if (storage.getQtyOnHand().signum() < 0) 
			{
				
				// ... and the remaining qty received is not enough to make it zero or positive
				if (qtyReceived.compareTo(storage.getQtyOnHand().negate()) <= 0)	
				{
					// ... then assign all the quantity received to this ticket
					createMA (line, storage.getM_MPolicyTicket_ID(), movementType, qtyReceived, useToFields);
					qtyReceived = Env.ZERO;
					getLogger().fine("QtyReceived=" + qtyReceived);
					
				}
				// ... and the remaining qty received is greater than this negative qty
				else
				{	
					// ... then apply enough material to this ticket to bring it to zero quantity
					createMA (line, storage.getM_MPolicyTicket_ID(), movementType, storage.getQtyOnHand().negate(), useToFields);
					qtyReceived = qtyReceived.subtract(storage.getQtyOnHand().negate());
					getLogger().fine("QtyReceived=" + qtyReceived);
					
				}
				
			}
			
			if (qtyReceived.signum() == 0)
			{
				
				break;
				
			}
		}
		
		//  If there is qtyReceived remaining after fulfilling negative storage, create a new 
		//  material policy ticket so fifo/lifo work.
		if (qtyReceived.signum() > 0)
		{
			
			MMPolicyTicket ticket = MMPolicyTicket.create(info);
			
			if (!getExistingMaterialAllocations().isEmpty()) 
			{  
				
				// Add the remainder to another material allocation line
				createMA (line, ticket.getM_MPolicyTicket_ID(), movementType, qtyReceived, useToFields);
				
			}
			else 
			{ 
				//  For incoming transactions with no storage corrections, one ticket is created per documentLine 
				//  and is added directly to the line.			
				line.setM_MPolicyTicket_ID(ticket.getM_MPolicyTicket_ID());
				
			}
			qtyReceived = Env.ZERO;
			getLogger().config("New Material Policy Ticket=" + line);
		}
	
		if (qtyReceived.signum() != 0) 
		{ // negative remaining is a problem.
			
			throw new AdempiereException("Can't receive all quantity on line " + line);
			
		}
	}

	protected void allocateMaterial() {
		
		allocateToIncomingTransactions();
		allocateToOutgoingTransactions();
		
	}

	protected String getWhereClause(IDocumentLine ref)
	{
		
		String refColumnName = ref.get_TableName()+"_ID";
		
		return refColumnName + "=? AND M_MPolicyTicket_ID=?";  
		
	}

	protected void setReferenceLine_ID(PO model, IDocumentLine ref)
	{
		
		String refColumnName = ref.get_TableName()+"_ID";
		if (model.get_ColumnIndex(refColumnName) < 0)
		{
			throw new AdempiereException("Invalid inventory document line "+ref);
		}
		model.set_ValueOfColumn(refColumnName, ref.get_ID());
		
	}

	protected String getTableNameMA(IDocumentLine model)
	{
		return model.get_TableName()+"MA";
	}

	private List<IInventoryAllocation> getExistingMaterialAllocations()
	{
		
		final String IDColumnName = docLine.get_TableName()+"_ID";
		final String tableName = getTableNameMA(docLine);		
		final String whereClause = IDColumnName+"=?";
		final int model_id = docLine.get_ID();
		
		List<IInventoryAllocation> list = new ArrayList<IInventoryAllocation>();
		
		try {
			list = new Query(ctx, tableName, whereClause, trxName)
							.setClient_ID()
							.setParameters(new Object[]{model_id})
							.setOrderBy(IDColumnName)
						.list(IInventoryAllocation.class);
	
		}
		catch (IllegalArgumentException e) { 
			// No MA table - leave the list empty
		}
		return list;
		
	}

	protected IInventoryAllocation createMA(
			IDocumentLine model, int M_MPolicyTicket_ID, String movementType, BigDecimal MovementQty, boolean useToFields)
	{
		
		final Properties ctx = model.getCtx();
		final String tableName = getTableNameMA(model);
		final String trxName = model.get_TrxName();
		
				
		// Check if the line_id and ticket are used in this MA.  In which case, add the movement qty.
		IInventoryAllocation ma = new Query(ctx, tableName, getWhereClause(model), trxName)
									.setClient_ID()
									.setParameters(model.get_ID(), M_MPolicyTicket_ID)
									.firstOnly();
		
		// If not found, create a new one.
		if (ma==null)
		{
			ma = (IInventoryAllocation)MTable.get(ctx, tableName).getPO(0, trxName);
			ma.setAD_Org_ID(model.getAD_Org_ID());
			setReferenceLine_ID((PO)ma, model);
			ma.setM_MPolicyTicket_ID(M_MPolicyTicket_ID);
			ma.setMovementType(movementType);
			ma.setUseToFields(useToFields);
		}
	
		ma.setMovementQty(ma.getMovementQty().add(MovementQty));
		
		((PO)ma).saveEx();
		
		getLogger().fine("##: " + ma);
		
		return ma;
	}

	protected void deleteExistingMaterialAllocations()
	{
		IDocumentLine line = info.getDocumentLine();
		
		String sql = "DELETE FROM "+ getTableNameMA(line)
				+ " WHERE "+line.get_TableName()+"_ID=?"
				+ " AND AD_Client_ID=?";
		int no = DB.executeUpdateEx(sql, new Object[]{line.get_ID(),line.getAD_Client_ID()}, line.get_TrxName());
		if (no > 0)
		{
			log.config("Delete old #" + no);
		}

		line.setM_MPolicyTicket_ID(0);
	}
	
	protected StorageTransactionInfoBuilder getASetOfStorageTransactionInfo() {
		
		if(storageTransactionInfoBuilder == null)
			return aSetOfStorageTransactionInfo();
		
		return storageTransactionInfoBuilder;
		
	}

	protected void setStorageTransactionInfoBuilder(StorageTransactionInfoBuilder builder) {
		
		storageTransactionInfoBuilder = builder;
		
	}

	protected int getNewMaterialPolicyTicketId() {
		
		return MMPolicyTicket.create(info).getM_MPolicyTicket_ID();

	}

	protected BigDecimal getStorageQtyOrdered() {
		
		return MStorage.getOrderedQty(ctx, m_product_id, orderWarehouse_id, 
				orderAttributeSetInstance_id, orderMPolicyTicket_id, trxName);
	
	}

	protected BigDecimal getStorageQtyReserved() {
		
		return MStorage.getReservedQty(ctx, m_product_id, orderWarehouse_id, 
				orderAttributeSetInstance_id, orderMPolicyTicket_id, trxName);

	}
	
}
