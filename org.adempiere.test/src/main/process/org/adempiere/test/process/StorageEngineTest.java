package org.adempiere.test.process;

import java.math.BigDecimal;
import java.sql.Savepoint;
import java.util.List;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MStorage;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.model.X_C_Order;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.Util;

public class StorageEngineTest extends SvrProcess {
	

	private int m_product_id;
	private int m_attributeSetInstance_id;
	private Properties ctx;
	private String trxName;
	private StringBuffer processMsg = new StringBuffer();

	public StorageEngineTest() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void prepare() {
		m_product_id = getParameterAsInt("M_Product_ID");
		m_attributeSetInstance_id = getParameterAsInt("M_AttributeSetInstance_ID");
	}

	
	@Override
	protected String doIt() throws Exception {
		
		
		ctx = getCtx();

		// Create a new transaction
		trxName = Trx.createTrxName("StorageEngineTest");
		Trx trx = Trx.get(trxName, false);
		
		// Set a savepoint so the changes can be undone
		Savepoint savepoint = trx.setSavepoint(null);

		try {
			// Test storage data for consistency
			Util.assume(testStorageData(), processMsg.toString() + " -->Test of storage data failed!");		
			processMsg.append(" --> OK");
			
			// Test Sales Order Reservations
			Util.assume(testSOReservation(), processMsg.toString() + " -->Test of storage reservation failed!");
			processMsg.append(" --> OK");
					
			// Make a PO and test ordered
			Util.assume(testPOOrdered(), processMsg.toString() + " -->Test of storage ordered failed!");
			processMsg.append(" --> OK");

			// Make a MR and test reduction in ordered amount and increase in inventory
			// Make a SO and test reservation
			// Make a customer shipment and test reduction in reservation amount and decrease in inventory
			// Test physical inventory
			// Test inventory move
			// Test voids/reverse corrections
		
			// Test initial quantity in inventory
		}
		catch (AdempiereException e) {
			processMsg.append(" --X ").append(e.getLocalizedMessage());
			log.severe(e.getLocalizedMessage());
		}
		finally {
			// Undo all changes
			trx.rollback(savepoint);
			trx.close();
		}
		return processMsg.toString();
	}
	
	private boolean testStorageData() {
		
		boolean noError = true;
		
		processMsg.append("<br>*** Test of storage data ");
		// Get all the storage data
		List<MStorage> storages = new Query(ctx,MStorage.Table_Name,"",trxName)
				.setClient_ID()
				.list();
		
		if (storages.size() == 0) {
			processMsg.append("<br>   No storage data! Nothing to test. ");
			return false;
		}
		
		for (MStorage storage : storages) {
			// If M_MPolicyTickte_ID is null, quantity on hand, should be zero - record should only be used
			// for reservations and orders
			if (storage.getM_MPolicyTicket_ID() == 0) {
				if (storage.getQtyOnHand().compareTo(Env.ZERO) != 0) {
					processMsg.append("<br>Error: Quantity on hand is not zero when material policy ticket is zero: ")
						.append(storage.toString());
					noError = false;
				}
			}
			else {
				if (storage.getQtyReserved().compareTo(Env.ZERO) != 0) {
					processMsg.append("<br>Error: Quantity reserved is not zero when material policy ticket is not zero: ")
						.append(storage.toString());
					noError = false;
				}				
				if (storage.getQtyOrdered().compareTo(Env.ZERO) != 0) {
					processMsg.append("<br>Error: Quantity ordered is not zero when material policy ticket is not zero: ")
						.append(storage.toString());
					noError = false;
				}				
			}
		}
		
		return noError;
	}

	private boolean testSOReservation() {

		processMsg.append("<br>*** Test of Sales Reservations ");
		
		int m_warehouse_id = Env.getContextAsInt(ctx, "M_Warehouse_ID");
		int m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator().get_ID();
		MProduct product = MProduct.get(ctx, m_product_id);
		BigDecimal qtyToOrder = new BigDecimal(10);
		BigDecimal currentQtyReserved = Env.ZERO;
		
		//MStorage onhandStorage = MStorage.getM_Locator_ID(m_warehouse_id, m_product_id, product.getM_AttributeSetInstance_ID(), 0, qtyToOrder, trxName);
		BigDecimal currentQtyAvailable = MStorage.getQtyAvailable(m_warehouse_id, m_locator_id, m_product_id, m_attributeSetInstance_id, trxName);
		MStorage reservedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id, m_attributeSetInstance_id, trxName);
		if (reservedStorage != null) {
			currentQtyReserved = reservedStorage.getQtyReserved();
		}
		
		MOrder so = new MOrder(ctx, 0, trxName);
		so.setBPartner(MBPartner.get(ctx, "JoeBlock"));
		so.setIsSOTrx(true); // Sales order
		so.setC_DocTypeTarget_ID();
		so.saveEx();
		
		MOrderLine sol = new MOrderLine(so);
		sol.setM_Product_ID(m_product_id);
		sol.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		sol.setQty(qtyToOrder);
		sol.saveEx();
		
		Util.assume(so.processIt(X_C_Order.DOCACTION_Complete), 
					"Could not complete sales order.");
		
		// Check the quantities
		// QtyAvailable
		BigDecimal changeInQtyAvailable = MStorage.getQtyAvailable(m_warehouse_id, m_locator_id, m_product_id, m_attributeSetInstance_id, trxName).subtract(currentQtyAvailable);
		Util.assume(changeInQtyAvailable.add(qtyToOrder).signum() == 0,
					"Quantity available changed on a standared sales order. It should remain the same.");
		
		reservedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id, m_attributeSetInstance_id, trxName);
		Util.assume(reservedStorage != null, "No reserved storage found!");
		Util.assume(reservedStorage.getM_MPolicyTicket_ID()==0, "Reserved Material Policy Ticket is not zero!");
		
		BigDecimal changeInReservedQty = reservedStorage.getQtyReserved().subtract(currentQtyReserved);
		
		Util.assume(changeInReservedQty.compareTo(qtyToOrder) == 0,
				"Quantity reserved (" + reservedStorage.getQtyReserved() + ") should be " 
		+ currentQtyReserved.add(qtyToOrder));
		
		return true;
	}

	private boolean testPOOrdered() {

		processMsg.append("<br>*** Test of Purchase Order ");
		
		int m_warehouse_id = Env.getContextAsInt(ctx, "M_Warehouse_ID");
		int m_locator_id = MWarehouse.get(ctx, m_warehouse_id).getDefaultLocator().get_ID();
		MProduct product = MProduct.get(ctx, m_product_id);
		BigDecimal qtyToOrder = new BigDecimal(10);
		BigDecimal currentQtyOrdered = Env.ZERO;
		
		BigDecimal currentQtyAvailable = MStorage.getQtyAvailable(m_warehouse_id, m_locator_id, m_product_id, m_attributeSetInstance_id, trxName);
		MStorage orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id, m_attributeSetInstance_id, trxName);
		if (orderedStorage != null) {
			currentQtyOrdered = orderedStorage.getQtyOrdered();
		}
		
		MOrder so = new MOrder(ctx, 0, trxName);
		so.setBPartner(MBPartner.get(ctx, "SeedFarm"));
		so.setIsSOTrx(false); // Purchase order
		so.setC_DocTypeTarget_ID();
		so.saveEx();
		
		MOrderLine sol = new MOrderLine(so);
		sol.setM_Product_ID(m_product_id);
		sol.setM_AttributeSetInstance_ID(m_attributeSetInstance_id);
		sol.setQty(qtyToOrder);
		sol.saveEx();
		
		Util.assume(so.processIt(X_C_Order.DOCACTION_Complete), 
					"Could not complete purchase order.");
		
		// Check the quantities
		// QtyAvailable
		Util.assume(currentQtyAvailable.compareTo(MStorage.getQtyAvailable(m_warehouse_id, m_locator_id, m_product_id, m_attributeSetInstance_id, trxName)) == 0,
					"Quantity available changed on a standared purchase order. It should remain the same.");
		
		orderedStorage = MStorage.getReservedOrdered(ctx, m_product_id, m_warehouse_id, m_attributeSetInstance_id, trxName);
		Util.assume(orderedStorage != null, "No ordered storage found!");
		Util.assume(orderedStorage.getM_MPolicyTicket_ID()==0, "Reserved Material Policy Ticket is not zero!");
		
		BigDecimal changeInOrderedQty = orderedStorage.getQtyOrdered().subtract(currentQtyOrdered);
		
		Util.assume(changeInOrderedQty.compareTo(qtyToOrder) == 0,
				"Quantity ordered (" + orderedStorage.getQtyOrdered() + ") should be " 
		+ currentQtyOrdered.add(qtyToOrder));
		
		return true;
	}

}
