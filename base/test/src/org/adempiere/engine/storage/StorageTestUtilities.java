package org.adempiere.engine.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.adempiere.test.CommonGWData;
import org.compiere.model.MDocType;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutConfirm;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MStorage;
import org.compiere.process.DocAction;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

public class StorageTestUtilities {

	private static final int AD_ORG_ID = CommonGWData.AD_ORG_ID;
	private static final int SEEDFARM_ID = CommonGWData.SEEDFARM_ID;
	private static final int SEEDFARM_LOCATION_ID = CommonGWData.SEEDFARM_LOCATION_ID;


	private int product_id;
	private int attributeSetInstance_id = 0;
	private int locator_id;
	private int warehouse_id;
	private Properties ctx;
	private String trxName;
	private BigDecimal initialQtyOrdered = null;
	private BigDecimal initialQtyReserved = null;
	private BigDecimal initialQtyAvailable = null;
	private BigDecimal initialQtyOnHand = null;
	
	private BigDecimal currentQtyOrdered = null;
	private BigDecimal currentQtyReserved = null;
	private BigDecimal currentQtyAvailable = null;
	private BigDecimal currentQtyOnHand = null;
	private int initialNumberOfStorageLocations;
	private int currentNumberOfStorageLocations;
	private MStorage[] storages;
	private int bPartner_id = SEEDFARM_ID;
	private int bPartnerLocation_id = SEEDFARM_LOCATION_ID;

	private Timestamp now;
	
	
	public StorageTestUtilities(Properties ctx, int product_id, int attributeSetInstance_id, 
			int warehouse_id, int locator_id, String trxName)
	{
		this.ctx = ctx;
		this.product_id = product_id;
		this.attributeSetInstance_id = attributeSetInstance_id;
		this.warehouse_id = warehouse_id;
		this.locator_id = locator_id;
		this.trxName = trxName;
		now = TimeUtil.getDay(System.currentTimeMillis());
	}

	public void setM_Product_ID(int product_id) {
		this.product_id = product_id;
	}

	public void setbPartner_id(int bPartner_id) {
		this.bPartner_id = bPartner_id;
	}

	public void setbPartnerLocation_id(int bPartnerLocation_id) {
		this.bPartnerLocation_id = bPartnerLocation_id;
	}

	public void setM_AttributeSetInstance_ID(int attributeSetInstance_id) {
		this.attributeSetInstance_id = attributeSetInstance_id;
	}

	public void determineInitialQtyAmounts() {
	
		initialQtyOrdered = MStorage.getOrderedQty(ctx, product_id, 
				warehouse_id, attributeSetInstance_id, trxName);
		initialQtyReserved = MStorage.getReservedQty(ctx, product_id, 
				warehouse_id, attributeSetInstance_id, trxName);
		initialQtyAvailable = MStorage.getQtyAvailable(ctx, warehouse_id, 
				locator_id, product_id, attributeSetInstance_id, trxName);
		initialQtyOnHand = MStorage.getQtyOnHand(ctx, product_id, 
				attributeSetInstance_id, locator_id, trxName);
		initialNumberOfStorageLocations = MStorage.getOfProduct(ctx, product_id, trxName).length;
	
		
		
	}

	public void determineCurrentQtyAmounts() {
		
		currentQtyOrdered = MStorage.getOrderedQty(ctx, product_id, 
				warehouse_id, attributeSetInstance_id, trxName);
		currentQtyReserved = MStorage.getReservedQty(ctx, product_id, 
				warehouse_id, attributeSetInstance_id, trxName);
		currentQtyAvailable = MStorage.getQtyAvailable(ctx, warehouse_id, 
				locator_id, product_id, attributeSetInstance_id, trxName);
		currentQtyOnHand = MStorage.getQtyOnHand(ctx, product_id, 
				attributeSetInstance_id, locator_id, trxName);
		storages = MStorage.getOfProduct(ctx, product_id, trxName);
		currentNumberOfStorageLocations = storages.length;

	}

	public MInOutLine receiveProductAndCheckQty() {
		
		determineInitialQtyAmounts();
		MInOutLine mrLine = receiveProductIntoInventory();
		determineCurrentQtyAmounts();
		
		return mrLine;
		
	}
	
	public MInOutLine receiveProductIntoInventory() {

		return receiveProductIntoInventory(null, null, Env.ONE);
		
	}

	public MInOutLine receiveProductIntoInventory(BigDecimal qtyReceived) {

		return receiveProductIntoInventory(null, null, qtyReceived);
		
	}

	public MInOutLine receiveProductIntoInventory(MOrder order, MOrderLine orderLine, 
			BigDecimal qtyReceived) {
		String movementType = MInOut.MOVEMENTTYPE_VendorReceipts;
		boolean isSoTrx = false;
		MInOut mr = createMRHeader(order, movementType, isSoTrx);
		MInOutLine mrLine = createMRLine(mr, orderLine, qtyReceived);
		mr.processIt(MInOut.DOCACTION_Prepare);
		
		Arrays.asList(mr.getConfirmations(true)).stream()
			.forEach(confirm -> {
				Arrays.asList(confirm.getLines(true)).stream()
					.forEach(confirmLine -> {
						confirmLine.setConfirmedQty(confirmLine.getTargetQty());
						confirmLine.saveEx();
					});
				confirm.processIt(MInOutConfirm.DOCACTION_Complete);
				confirm.saveEx();
			});
		
		mr.processIt(MInOut.DOCACTION_Complete);
		mrLine.load(trxName);
		if (order != null) order.load(trxName);
		if (orderLine != null) orderLine.load(trxName);

		assertEquals(MInOut.DOCSTATUS_Completed, mr.getDocStatus(), 
				"Material Receipt was not completed. DocStatus:");
		
		return mrLine;
	}

	private MInOut createMRHeader(MOrder order, String movementType, boolean isSoTrx) {
		
		MInOut mr = new MInOut(ctx, 0, trxName);
		mr.setIsSOTrx(isSoTrx);
		mr.setC_DocType_ID();
		mr.setMovementType(movementType);
		mr.setC_BPartner_ID(bPartner_id); // GW Seed Farm
		mr.setC_BPartner_Location_ID(bPartnerLocation_id);
		mr.setM_Warehouse_ID(warehouse_id);
		mr.setMovementDate(now);
		mr.setDateAcct(now);
		if (order != null)
			mr.setC_Order_ID(order.getC_Order_ID());
		mr.saveEx();
		return mr;
	}

	private MInOutLine createMRLine(MInOut mr, MOrderLine orderLine, BigDecimal qty) {
		MInOutLine mrLine = new MInOutLine(mr);
		mrLine.setM_Product_ID(product_id);
		if (attributeSetInstance_id > 0)
			mrLine.setM_AttributeSetInstance_ID(attributeSetInstance_id);
		mrLine.setM_Locator_ID(locator_id);
		mrLine.setQty(qty);
		if (orderLine != null)
			mrLine.setC_OrderLine_ID(orderLine.getC_OrderLine_ID());
		else
			mrLine.setC_OrderLine_ID(0);
		mrLine.saveEx();
		return mrLine;
	}

	public MOrder createPOHeader() {
		
		int poDocType_id = 126;
		boolean vendorTrx = false;
		return createOrderHeader(poDocType_id, vendorTrx);
	}

	public MOrder createPOSHeader() {
		
		int poDocType_id = MDocType.getDocTypeBaseOnSubType(AD_ORG_ID, 
				MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_POSOrder);
		
		boolean salesTrx = true;
		return createOrderHeader(poDocType_id, salesTrx);
	}

	public MOrder createStandardSOHeader() {
		
		int stardard_docType_id = MDocType.getDocTypeBaseOnSubType(AD_ORG_ID, 
				MDocType.DOCBASETYPE_SalesOrder, MDocType.DOCSUBTYPESO_StandardOrder);
		
		boolean salesTrx = true;
		return createOrderHeader(stardard_docType_id, salesTrx);
	}

	private MOrder createOrderHeader(int docType_id, boolean isSOTrx) {
		MOrder order = new MOrder(ctx, 0, trxName);
		order.setIsSOTrx(isSOTrx);
		order.setC_DocTypeTarget_ID(docType_id);  // POS order
		order.setM_Warehouse_ID(warehouse_id);  // GW HQ Warehouse
		order.setC_BPartner_ID(120); // GW Seed Farm
		order.setM_PriceList_ID(102); // GW Purchase
		order.saveEx();
		return order;
	}
	public MOrderLine addProductAndCompleteOrder(MOrder order, BigDecimal qty) {
		
		MOrderLine orderLine = new MOrderLine(order);
		orderLine.setM_Product_ID(product_id);
		orderLine.setQty(qty);
		orderLine.saveEx();
		
		order.setDocAction(DocAction.ACTION_Complete);
		order.processIt(DocAction.ACTION_Complete);
		
		orderLine.load(trxName);
		return orderLine;
		
	}

	
	public boolean isPolicyTicketFoundInOnHandStorage(int mPolicyTicket_id) {
		
		return Stream.of(storages)
			.filter(storage -> storage.getQtyOnHand().signum() > 0)
			.anyMatch(storage -> mPolicyTicket_id==storage.getM_MPolicyTicket_ID());
		
	}

	public boolean isPolicyTicketFoundInEmptyOnHandStorage(int mPolicyTicket_id) {
		
		return Stream.of(storages)
			.filter(storage -> storage.getQtyOnHand().signum() == 0)
			.anyMatch(storage -> mPolicyTicket_id==storage.getM_MPolicyTicket_ID());
		
	}

	public boolean isPolicyTicketFoundInOrderedStorage(int mPolicyTicket_id) {
		
		return Stream.of(storages)
			.filter(storage -> storage.getQtyOrdered().signum() > 0)
			.anyMatch(storage -> mPolicyTicket_id==storage.getM_MPolicyTicket_ID());
		
	}

	public boolean isPolicyTicketFoundInReservedStorage(int mPolicyTicket_id) {
		
		return Stream.of(storages)
			.filter(storage -> storage.getQtyReserved().signum() > 0)
			.anyMatch(storage -> mPolicyTicket_id==storage.getM_MPolicyTicket_ID());
		
	}

	public boolean isPolicyTicketFoundInEmptyReservedStorage(int mPolicyTicket_id) {
		
		return Stream.of(storages)
			.filter(storage -> storage.getQtyReserved().signum() == 0)
			.anyMatch(storage -> mPolicyTicket_id==storage.getM_MPolicyTicket_ID());
		
	}

	public BigDecimal getInitialQtyOrdered() {
		return initialQtyOrdered;
	}


	public BigDecimal getInitialQtyReserved() {
		return initialQtyReserved;
	}

	public BigDecimal getInitialQtyAvailable() {
		return initialQtyAvailable;
	}

	public BigDecimal getInitialQtyOnHand() {
		return initialQtyOnHand;
	}

	public int getInitialNumberOfStorageLocations() {
		return initialNumberOfStorageLocations;
	}

	public BigDecimal getCurrentQtyOrdered() {
		return currentQtyOrdered;
	}

	public BigDecimal getCurrentQtyReserved() {
		return currentQtyReserved;
	}

	public BigDecimal getCurrentQtyAvailable() {
		return currentQtyAvailable;
	}

	public BigDecimal getCurrentQtyOnHand() {
		return currentQtyOnHand;
	}

	public int getCurrentNumberOfStorageLocations() {
		return currentNumberOfStorageLocations;
	}

	public MStorage[] getStorages() {
		return storages;
	}

	public BigDecimal getChangeInQtyAvailable() {
		
		return currentQtyAvailable.subtract(initialQtyAvailable);
	}
	
	public BigDecimal getChangeInQtyOrdered() {
		
		return currentQtyOrdered.subtract(initialQtyOrdered);
	}

	public BigDecimal getChangeInQtyReserved() {
		
		return currentQtyReserved.subtract(initialQtyReserved);
	}

	public BigDecimal getChangeInQtyOnHand() {
		
		return currentQtyOnHand.subtract(initialQtyOnHand);
	}

	public int getChangeInNumberOfStorageLocations() {
		
		return currentNumberOfStorageLocations - initialNumberOfStorageLocations;
	}

	public void assertCorrectChangeInStorageQuantities(
			BigDecimal qtyAvailableDelta, 
			BigDecimal qtyOnHandDelta,
			BigDecimal qtyReservedDelta, 
			BigDecimal qtyOrderedDelta) {
		
		assertCorrectChangeInStorageQuantities(
				qtyAvailableDelta, null,
				qtyOnHandDelta, null,
				qtyReservedDelta, null,
				qtyOrderedDelta, null,
				0);
		
	}
	
	public void assertCorrectChangeInStorageQuantities(
			BigDecimal qtyAvailableDelta, String availMsg,
			BigDecimal qtyOnHandDelta, String onHandMsg,
			BigDecimal qtyReservedDelta, String reservedMsg,
			BigDecimal qtyOrderedDelta, String orderedMsg,
			int scale) {
		
		availMsg = Optional.ofNullable(availMsg).orElse("Qty Available");
		onHandMsg = Optional.ofNullable(onHandMsg).orElse("Qty On Hand");
		reservedMsg = Optional.ofNullable(reservedMsg).orElse("Quantity reserved");
		orderedMsg = Optional.ofNullable(orderedMsg).orElse("Quantity ordered");
		
		assertEquals(qtyAvailableDelta.setScale(scale), 
				getChangeInQtyAvailable().setScale(scale), 
				availMsg);
		assertEquals(qtyOnHandDelta.setScale(scale), 
				getChangeInQtyOnHand().setScale(scale), 
				onHandMsg);
		assertEquals(qtyReservedDelta.setScale(scale), 
				getChangeInQtyReserved().setScale(scale), 
				reservedMsg);
		assertEquals(qtyOrderedDelta.setScale(scale), 
				getChangeInQtyOrdered().setScale(scale), 
				orderedMsg);
		
	}

}
