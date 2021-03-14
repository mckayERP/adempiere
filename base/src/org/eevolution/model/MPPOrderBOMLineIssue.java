package org.eevolution.model;

import static java.util.Objects.requireNonNull;
import static org.compiere.model.ModelValidator.TIMING_AFTER_CLOSE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_COMPLETE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_PREPARE;
import static org.compiere.model.ModelValidator.TIMING_AFTER_VOID;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_CLOSE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_COMPLETE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_PREPARE;
import static org.compiere.model.ModelValidator.TIMING_BEFORE_VOID;
import static org.compiere.model.X_C_DocType.DOCBASETYPE_ManufacturingOrderIssue;
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

public class MPPOrderBOMLineIssue extends X_PP_Order_BOMLineIssue
		implements DocAction, DocumentReversalEnabled, IDocumentLine {

	static final String PROCESS_MSG_ISSUE_CANNOT_BE_REACTIVATED 
							= "@MPPOrderBOMLineIssue_IssueCannotBeReactivated@";
	static final String PROCESS_MSG_PARENT_NOT_IN_PROGRESS 
							= "@MPPOrderBOMLineIssue_ParentOrderNotInProgress@";

	public static MPPOrderBOMLineIssue copyFrom(MPPOrderBOMLineIssue issueFrom,
			Timestamp movementDate, Timestamp dateDoc,
			boolean isReversal, String trxName) {

		MPPOrderBOMLineIssue issueTo = new MPPOrderBOMLineIssue(issueFrom.getCtx(), 0, trxName);
		copyValues(issueFrom, issueTo, issueFrom.getAD_Client_ID(), issueFrom.getAD_Org_ID());
		issueTo.set_ValueNoCheck("PP_Order_BOMLineIssue_ID", I_ZERO);
		issueTo.set_ValueNoCheck("DocumentNo", null);
		// For Reversal
		if (isReversal) {
			issueTo.setReversal(true);
			issueTo.setReversal_ID(issueFrom.getPP_Order_BOMLineIssue_ID());
			MDocType docType = MDocType.get(issueFrom.getCtx(), issueFrom.getC_DocType_ID(),
					trxName);
			// Set Document No from flag
			if (docType.isCopyDocNoOnReversal()) {
				issueTo.setDocumentNo(
						issueFrom.getDocumentNo() + Msg.getMsg(issueFrom.getCtx(), "^"));
			}
		}

		issueTo.setDocStatus(DOCSTATUS_Drafted); // Draft
		issueTo.setDocAction(DOCACTION_Complete);
		issueTo.setIsApproved(false);
		issueTo.setProcessed(false);
		issueTo.setProcessing(false);

		issueTo.setMovementDate(movementDate);
		issueTo.setDateDoc(dateDoc);

		issueTo.saveEx(trxName);

		return issueTo;

	} // copyFrom

	private static final long serialVersionUID = -3244521811695893616L;
	private boolean justPrepared;
	private transient ModelValidationEngine modelValidationEngine;
	private String processMsg;
	private MPPOrderBOMLine bomLine = null;
	MClient client = getAD_Client();

	protected MClient getAD_Client() {

		return MClient.get(getCtx());

	}

	protected DocumentEngine getDocumentEngine() {

		return new DocumentEngine(this, getDocStatus());

	}

	protected ModelValidationEngine getModelValidationEngine() {

		if (modelValidationEngine == null)
			modelValidationEngine = ModelValidationEngine.get();

		return modelValidationEngine;

	}

	protected void setModelValidationEngine(ModelValidationEngine engine) {

		this.modelValidationEngine = engine;

	}

	public boolean isJustPrepared() {

		return justPrepared;

	}

	public MPPOrderBOMLineIssue(Properties ctx, int pp_order_bomLineIssue_id, String trxName) {

		super(ctx, pp_order_bomLineIssue_id, trxName);

	}

	public MPPOrderBOMLineIssue(Properties ctx, ResultSet rs, String trxName) {

		super(ctx, rs, trxName);

	}

	public MPPOrderBOMLineIssue(MPPOrderBOMLine bomLine) {

		this(bomLine.getCtx(), bomLine, bomLine.get_TrxName());

	}

	public MPPOrderBOMLineIssue(Properties ctx, MPPOrderBOMLine bomLine, String trxName) {

		super(ctx, 0, trxName);
		this.setParent(bomLine);
		this.setPP_Order_BOMLine_ID(bomLine.getPP_Order_BOMLine_ID());
		this.setM_Product_ID(bomLine.getM_Product_ID());
		this.setM_AttributeSetInstance_ID(bomLine.getM_AttributeSetInstance_ID());
		this.setM_Locator_ID(bomLine.getM_Locator_ID());

	}

	public void setParent(MPPOrderBOMLine bomLine) {

		this.bomLine = requireNonNull(bomLine);
		this.setPP_Order_BOMLine_ID(bomLine.getPP_Order_BOMLine_ID());
		this.setM_Product_ID(bomLine.getM_Product_ID());

	}

	protected String checkIfClosedReversedOrVoided() {

		if (DOCSTATUS_Closed.equals(getDocStatus())
				|| DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus()))
			return "Document Closed: " + getDocStatus();
		else
			return null;

	}

	@Override
	protected boolean beforeSave(boolean newRecord) {

		if (this.getC_DocType_ID() == 0)
			setC_DocType_ID(getDocType_ID());
		return true;

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

		return true;

	}

	@Override
	public IDocumentLine getReversalDocumentLine() {

		return (IDocumentLine) getReversal();

	}

	@Override
	public boolean isReversalParent() {

		return getPP_Order_BOMLineIssue_ID() < getReversal_ID();

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

		return null;

	}

	@Override
	public BigDecimal getPriceActualCurrency() {

		return null;

	}

	@Override
	public int getM_MPolicyTicket_ID() {

		return 0;

	}

	@Override
	public void setM_MPolicyTicket_ID(int m_mPolicyTicket_id) {

		// This field is required by the IDocumentLine interface but
		// is not used by the PP_Order_BOMLineIssue model.
		// See the Material Allocations instead.

	}

	@Override
	public int getM_Warehouse_ID() {

		if (getM_Locator_ID() > 0) {
			return getM_Locator().getM_Warehouse_ID();
		}

		MPPOrderBOMLine parent = (MPPOrderBOMLine) getParent();

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

		return bomLine;

	}

	@Override
	public MPPOrderBOMLineIssue reverseIt(boolean isAccrual) {

		Timestamp loginDate = Env.getContextAsDate(getCtx(), "#Date");
		Timestamp reversalDate = isAccrual ? loginDate : getDateAcct();
		Timestamp reversalMovementDate = isAccrual ? reversalDate : getMovementDate();

		testPeriodOpen(reversalDate);

		MPPOrderBOMLineIssue reversal = copyFrom(reversalMovementDate, reversalDate,
				true);

		if (reversal == null) {
			processMsg = "Could not create Receipt Reversal";
			return null;
		}

		reversal.setMovementQty(getMovementQty().negate());
		reversal.addDescription("{->" + getDocumentNo() + ")");
		reversal.setReversal_ID(getPP_Order_BOMLineIssue_ID());
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

	protected void testPeriodOpen(Timestamp date) {

		MDocType documentType = getDocType();
		MPeriod.testPeriodOpen(getCtx(), date, documentType.getDocBaseType(), getAD_Org_ID(),
				get_TrxName());

	}

	protected MPPOrderBOMLineIssue copyFrom(Timestamp reversalMovementDate, 
			Timestamp reversalDate, boolean isReversal) {

		return copyFrom(this, reversalMovementDate, reversalDate, isReversal, get_TrxName());

	}

	protected void addDescription(String description) {

		if (description == null || description.isEmpty())
			return;

		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);

	}

	@Override
	public void setReversal(boolean isReversal) {

		setIsReversal(isReversal);

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

		MPPOrder order = getParentOrder();
		if (!DOCSTATUS_InProgress.equals(order.getDocStatus())) {
			processMsg = PROCESS_MSG_PARENT_NOT_IN_PROGRESS;
			return DocAction.STATUS_Invalid;
		}

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_BEFORE_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		processMsg = getModelValidationEngine().fireDocValidate(this, TIMING_AFTER_PREPARE);
		if (processMsg != null)
			return DocAction.STATUS_Invalid;

		justPrepared = true;
		setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;

	}

	protected MPPOrder getParentOrder() {

		MPPOrderBOMLine parent = (MPPOrderBOMLine) getParent();
		return (MPPOrder) parent.getPP_Order();

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

	protected String applyStorageRules() {

        StorageEngine se = new StorageEngine();
		return se.applyStorageRules(this);

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

	protected boolean isAccrualNecessary() {

		try {
			testPeriodOpen(getDateAcct());
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
			String docStatus = completeIt();
			setDocStatus(docStatus);
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

		MPPOrderBOMLineIssue reversal = reverseIt(false);
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

		MPPOrderBOMLineIssue reversal = reverseIt(true);
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
	public boolean reActivateIt() {

		get_Logger().info("ReactivateIt - " + toString());

		processMsg = PROCESS_MSG_ISSUE_CANNOT_BE_REACTIVATED;

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

		return MDocType.get(getCtx(), getC_DocType_ID(), get_TrxName());

	}

	protected int getDocType_ID() {

		return MDocType.getDocType(DOCBASETYPE_ManufacturingOrderIssue);

	}

	@Override
	public File createPDF() {

		return null;

	}

	@Override
	public String getProcessMsg() {

		return processMsg;

	}

	protected void setProcessMsg(String processMsg) {

		this.processMsg = processMsg;

	}

	@Override
	public int getDoc_User_ID() {

		MPPOrder order = getParentOrder();
		if (order != null)
			return order.getPlanner_ID();

		return 0;

	}

	@Override
	public BigDecimal getApprovalAmt() {

		return Env.ZERO;

	}

	@Override
	public int getReversalLine_ID() {

		return getReversal_ID();

	}

	@Override
	public boolean equals(Object object) {

		return super.equals(object);

	}

	@Override
	public int hashCode() {

		return super.hashCode();

	}

}
