package org.eevolution.model;

import static java.util.Objects.requireNonNull;
import static org.adempiere.util.attributes.AttributeUtilities.validateAttributeSetInstanceMandatory;
import static org.compiere.model.ModelValidator.TIMING_AFTER_CLOSE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_COMPLETE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_PREPARE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_VOID;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_CLOSE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_COMPLETE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_PREPARE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_VOID;
import static org.compiere.model.X_M_Transaction.MOVEMENTTYPE_ProductionPlus;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.engine.storage.StorageEngine;
import org.adempiere.exceptions.PeriodClosedException;
import org.compiere.model.MClient;
import org.compiere.model.MConversionType;
import org.compiere.model.MDocType;
import org.compiere.model.MPeriod;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.DocumentReversalEnabled;
import org.compiere.util.Env;
import org.compiere.util.Msg;

public class MPPOrderReceipt extends X_PP_OrderReceipt
		implements DocAction, DocumentReversalEnabled, IDocumentLine {

	private static final long serialVersionUID = 7053275059258129064L;

	static final String PROCESS_MSG_PARENT_ORDER_NOT_IN_PROGRESS = "@MPPOrderReceipt_ParentOrderNotInProgress@";
	static final String PROCESS_MSG_MPPORDER_RECEIPTS_CANNOT_BE_REACTIVATED = "@MPPOrderReceipt_CannotBeReactivated@";

	public static MPPOrderReceipt copyFrom(MPPOrderReceipt receiptFrom, Timestamp movementDate,
			Timestamp dateDoc,
			boolean isReversal, String trxName) {

		MPPOrderReceipt receiptTo = new MPPOrderReceipt(receiptFrom.getCtx(), 0, trxName);
		copyValues(receiptFrom, receiptTo, receiptFrom.getAD_Client_ID(),
				receiptFrom.getAD_Org_ID());
		receiptTo.set_ValueNoCheck("PP_OrderReceipt_ID", I_ZERO);
		receiptTo.set_ValueNoCheck("DocumentNo", null);
		// For Reversal
		if (isReversal) {
			receiptTo.setReversal(true);
			receiptTo.setReversal_ID(receiptFrom.getPP_OrderReceipt_ID());
			MDocType docType = MDocType.get(receiptFrom.getCtx(), receiptFrom.getC_DocType_ID(),
					trxName);
			// Set Document No from flag
			if (docType.isCopyDocNoOnReversal()) {
				receiptTo.setDocumentNo(
						receiptFrom.getDocumentNo() + Msg.getMsg(receiptFrom.getCtx(), "^"));
			}
		}

		receiptTo.setDocStatus(DOCSTATUS_Drafted); // Draft
		receiptTo.setDocAction(DOCACTION_Complete);
		receiptTo.setIsApproved(false);
		receiptTo.setProcessed(false);
		receiptTo.setProcessing(false);

		receiptTo.setMovementDate(movementDate);
		receiptTo.setDateDoc(dateDoc);

		receiptTo.saveEx(trxName);

		return receiptTo;

	} // copyFrom

	private MPPOrder ppOrder;
	private boolean justPrepared;

	// Package-private on purpose
	String processMsg;
	transient ModelValidationEngine modelValidationEngine = null;
	transient DocumentEngine documentEngine = null;
	MClient client = getAD_Client();

	MClient getAD_Client() {

		return MClient.get(getCtx());

	}

	public MPPOrderReceipt(Properties ctx, int ppOrderReceiptId, String trxName) {

		super(ctx, ppOrderReceiptId, trxName);

	}

	public MPPOrderReceipt(Properties ctx, ResultSet rs, String trxName) {

		super(ctx, rs, trxName);

	}

	// Wrapper for static class
	protected int getDefaultDocType() {

		return MDocType.getDocType("MOR");

	}

	@Override
	protected boolean beforeSave(boolean newRecord) {

		if (getC_DocType_ID() == 0)
			setC_DocType_ID(getDefaultDocType());

		if (getDateDoc() == null)
			setDateDoc(Env.getContextAsDate(getCtx(), "#Date"));

		return true;

	} // beforeSave

	public void setParent(MPPOrder ppOrder) {

		this.ppOrder = requireNonNull(ppOrder);
		this.setPP_Order_ID(ppOrder.getPP_Order_ID());
		this.setM_Product_ID(ppOrder.getM_Product_ID());

	}

	public void addDescription(String description) {

		if (description == null || description.isEmpty())
			return;

		String desc = getDescription();
		if (desc == null || desc.isEmpty())
			setDescription(description);
		else
			setDescription(desc + " | " + description);

	}

	@Override
	public int getM_LocatorTo_ID() {

		// Not used
		return 0;

	}

	@Override
	public int getM_AttributeSetInstanceTo_ID() {

		// Not used
		return 0;

	}

	@Override
	public Timestamp getDateAcct() {

		return getMovementDate();

	}

	@Override
	public boolean isSOTrx() {

		return false;

	}

	@Override
	public int getC_Currency_ID() {

		return client.getC_Currency_ID();

	}

	@Override
	public int getC_ConversionType_ID() {

		return getDefaultConverstionType_ID();

	}

	protected int getDefaultConverstionType_ID() {

		return MConversionType.getDefault(getAD_Client_ID());

	}

	@Override
	public BigDecimal getPriceActual() {

		return BigDecimal.ZERO;

	}

	@Override
	public BigDecimal getPriceActualCurrency() {

		return BigDecimal.ZERO;

	}

	@Override
	public IDocumentLine getReversalDocumentLine() {

		return (IDocumentLine) getReversal();

	}

	@Override
	public boolean isReversalParent() {

		return getPP_OrderReceipt_ID() < getReversal_ID();

	}

	@Override
	public int getM_Warehouse_ID() {

		if (getM_Locator_ID() > 0) {
			return getM_Locator().getM_Warehouse_ID();
		}

		MPPOrder parent = (MPPOrder) getParent();

		if (parent == null)
			return 0;

		return parent.getM_Warehouse_ID();

	}

	@Override
	public String getMovementType() {

		return MOVEMENTTYPE_ProductionPlus;

	}

	@Override
	public PO getParent() {

		if (ppOrder == null && getPP_Order_ID() > 0)
			ppOrder = new MPPOrder(getCtx(), getPP_Order_ID(), get_TrxName());

		return ppOrder;

	}

	@Override
	public boolean processIt(String action) {

		processMsg = null;
		return getDocumentEngine().processIt(action, getDocAction());

	}

	@Override
	public boolean unlockIt() {

		setProcessing(false);
		return true;

	}

	@Override
	public boolean invalidateIt() {

		setDocAction(DOCACTION_Prepare);
		return true;

	}

	@Override
	public String prepareIt() {

		get_Logger().info(toString());

		MPPOrder order = (MPPOrder) getParent();
		if (!DOCSTATUS_InProgress.equals(order.getDocStatus())) {
			processMsg = PROCESS_MSG_PARENT_ORDER_NOT_IN_PROGRESS;
			return DocAction.STATUS_Invalid;
		}

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_BEFORE_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		processMsg = validateAttributeSetIntance();
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_AFTER_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		justPrepared = true;
		setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;

	}

	protected String validateAttributeSetIntance() {

		return validateAttributeSetInstanceMandatory(this);

	}

	@Override
	public boolean approveIt() {

		get_Logger().info("approveIt - " + toString());
		setIsApproved(true);
		return true;

	}

	@Override
	public boolean rejectIt() {

		get_Logger().info("rejectIt - " + toString());
		setIsApproved(false);
		return true;

	}

	@Override
	public String completeIt() {

		get_Logger().info("CompleteIt - " + toString());

		// Re-Check
		if (!isJustPrepared()) {
			String status = prepareIt();
			if (getProcessMsg() != null)
				return status;
		}

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_BEFORE_COMPLETE);
		if (processMsg != null)
			return DOCSTATUS_Invalid;

		// Implicit Approval
		if (!isApproved())
			approveIt();

		processMsg = applyStorageRules();
		if (processMsg != null) {
			return DOCSTATUS_Invalid;
		}

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_AFTER_COMPLETE);
		if (processMsg != null) {
			return DOCSTATUS_Invalid;
		}

		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DOCSTATUS_Completed;

	}

	public boolean isJustPrepared() {

		return justPrepared;

	}

	protected String applyStorageRules() {

		return StorageEngine.applyStorageRules(this);

	}

	@Override
	public boolean voidIt() {

		get_Logger().info("VoidIt - " + toString());

		processMsg = checkIfClosedReversedOrVoided();
		if (processMsg != null)
			return false;

		if (directVoidIsPossible()) {
			// Before Void
			processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_BEFORE_VOID);
			if (processMsg != null)
				return false;

			// Void actions
			setMovementQty(Env.ZERO);
			setDocStatus(DOCSTATUS_Voided);

			// After Void
			processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_AFTER_VOID);
			if (processMsg != null)
				return false;

			setProcessed(true);
			setDocAction(DOCACTION_None);
			return true;

		} else {

			if (isAccrualNecessary())
				return reverseAccrualIt();
			else
				return reverseCorrectIt();
		}

	}

	protected boolean directVoidIsPossible() {

		return DOCSTATUS_Drafted.equals(getDocStatus())
				|| DOCSTATUS_Invalid.equals(getDocStatus())
				|| DOCSTATUS_InProgress.equals(getDocStatus())
				|| DOCSTATUS_Approved.equals(getDocStatus())
				|| DOCSTATUS_NotApproved.equals(getDocStatus());

	}

	protected String checkIfClosedReversedOrVoided() {

		if (DOCSTATUS_Closed.equals(getDocStatus())
				|| DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus()))
			return "Document Closed: " + getDocStatus();
		else
			return null;

	}

	public boolean isAccrualNecessary() {

		try {

			MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocType_ID(), getAD_Org_ID(),
					get_TrxName());
		} catch (PeriodClosedException periodClosedException) {
			return true;
		}

		return false;

	}

	@Override
	public boolean closeIt() {

		get_Logger().info("CloseIt - " + toString());

		if (DOCSTATUS_Closed.equals(getDocStatus()))
			return true;

		if (!DOCSTATUS_Completed.equals(getDocStatus())) {

			setDocStatus(completeIt());
			if (getProcessMsg() != null) {
				setDocAction(DOCACTION_Complete);
				return false;
			}

		}

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_BEFORE_CLOSE);
		if (processMsg != null)
			return false;

		setDocStatus(DOCSTATUS_Closed);
		setDocAction(DOCACTION_None);

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_AFTER_CLOSE);

		return processMsg == null;

	}

	@Override
	public boolean reverseCorrectIt() {

		get_Logger().info("ReverseCorrectIt - " + toString());

		// Before reverseCorrection
		processMsg = getModelValidationEngine().fireDocValidate(this,
				ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (processMsg != null)
			return false;

		MPPOrderReceipt reversal = reverseIt(false);
		if (reversal == null)
			return false;

		// After reverseAccrual
		processMsg = getModelValidationEngine().fireDocValidate(this,
				ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (processMsg != null)
			return false;

		processMsg = reversal.getDocumentNo();
		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed); // may come from void
		setDocAction(DOCACTION_None);

		return true;

	}

	@Override
	public boolean reverseAccrualIt() {

		get_Logger().info("ReverseAccrualtIt - " + toString());

		// Before reverseAccrual
		processMsg = getModelValidationEngine().fireDocValidate(this,
				ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (processMsg != null)
			return false;

		MPPOrderReceipt reversal = reverseIt(true);
		if (reversal == null)
			return false;

		// After reverseAccrual
		processMsg = getModelValidationEngine().fireDocValidate(this,
				ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (processMsg != null)
			return false;

		processMsg = reversal.getDocumentNo();
		setProcessed(true);
		setDocStatus(DOCSTATUS_Reversed); // may come from void
		setDocAction(DOCACTION_None);

		return true;

	}

	@Override
	public MPPOrderReceipt reverseIt(boolean isAccrual) {

		Timestamp loginDate = Env.getContextAsDate(getCtx(), "#Date");
		Timestamp reversalDate = isAccrual ? loginDate : getDateAcct();
		Timestamp reversalMovementDate = isAccrual ? reversalDate : getMovementDate();

		testPeriodOpen(reversalDate);

		// Deep Copy
		MPPOrderReceipt reversal = copyFrom(reversalMovementDate, reversalDate,
				true);

		if (reversal == null) {
			processMsg = "Could not create Receipt Reversal";
			return null;
		}

		reversal.setMovementQty(getMovementQty().negate());
		reversal.addDescription("{->" + getDocumentNo() + ")");
		reversal.setReversal_ID(getPP_OrderReceipt_ID());
		reversal.setReversal(true);
		reversal.saveEx();

		if (!reversal.processIt(DocAction.ACTION_Complete)
				|| !reversal.getDocStatus().equals(DocAction.STATUS_Completed)) {
			processMsg = "Reversal ERROR: " + reversal.getProcessMsg();
			return null;
		}
		reversal.closeIt();
		reversal.setProcessing(false);
		reversal.setDocStatus(DOCSTATUS_Reversed);
		reversal.setDocAction(DOCACTION_None);
		reversal.saveEx(get_TrxName());
		//
		addDescription("(" + reversal.getDocumentNo() + "<-)");
		setReversal_ID(reversal.get_ID());
		setIsReversal(true);

		setDocStatus(DOCSTATUS_Reversed);

		return reversal;

	}

	@Override
	public boolean reActivateIt() {

		get_Logger().info("ReactivateIt - " + toString());

		processMsg = PROCESS_MSG_MPPORDER_RECEIPTS_CANNOT_BE_REACTIVATED;

		return false;

	}

	@Override
	public String getSummary() {

		return getM_Product().getValue() + "_" + getM_Product().getName() + "/Qty "
				+ getMovementQty() + "/" + getMovementDate();

	}

	@Override
	public String getDocumentInfo() {

		MDocType dt = getDocType();
		return dt.getName() + " " + getDocumentNo();

	}

	protected MDocType getDocType() {

		return MDocType.get(getCtx(), getC_DocType_ID(), null);

	}

	@Override
	public File createPDF() {

		get_Logger().info("createPDF not implemented");
		return null;

	}

	@Override
	public String getProcessMsg() {

		return processMsg;

	}

	@Override
	public int getDoc_User_ID() {

		if (getParent() != null)
			return ((MPPOrder) getParent()).getPlanner_ID();

		return 0;

	}

	@Override
	public BigDecimal getApprovalAmt() {

		return Env.ZERO;

	}

	protected ModelValidationEngine getModelValidationEngine() {

		if (modelValidationEngine == null)
			modelValidationEngine = ModelValidationEngine.get();

		return modelValidationEngine;

	}

	public DocumentEngine getDocumentEngine() {

		if (documentEngine == null)
			documentEngine = new DocumentEngine(this, getDocStatus());

		return documentEngine;

	}

	/**
	 * Sets the {@link org.compiere.process.DocumentEngine DocumentEngine} instance
	 * to use. Only use to inject classes for testing. Otherwise,
	 * {@code setDocumentEngine(null);} prior to calling
	 * {@link #getDocumentEngine()}.
	 * 
	 * @param documentEngine
	 */
	public void setDocumentEngine(DocumentEngine documentEngine) {

		this.documentEngine = documentEngine;

	}

	protected void testPeriodOpen(Timestamp date) {

		MDocType docType = getDocType();
		String docBaseType = docType.getDocBaseType();
		MPeriod.testPeriodOpen(getCtx(), date, docBaseType, getAD_Org_ID(), get_TrxName());

	}

	@Override
	public void setReversal(boolean isReversal) {

		setIsReversal(isReversal);

	}

	protected MPPOrderReceipt copyFrom(Timestamp reversalMovementDate, Timestamp reversalDate,
			boolean isReversal) {

		return copyFrom(this, reversalMovementDate, reversalDate, isReversal, get_TrxName());

	}

	@Override
	public int getReversalLine_ID() {

		return getReversal_ID();

	}

}
