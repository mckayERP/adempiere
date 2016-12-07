package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.util.Env;
import org.compiere.FA.exceptions.AssetAlreadyDepreciatedException;
import org.compiere.FA.exceptions.AssetCheckDocumentException;
import org.compiere.FA.exceptions.AssetNotImplementedException;
import org.compiere.FA.exceptions.AssetNotSupportedException;
import org.compiere.FA.exceptions.AssetStatusChangedException;
import org.adempiere.exceptions.NoCurrencyConversionException;
import org.adempiere.util.POCacheLocal;


/**
 * Asset Disposal Model
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 
 * 
 */
public class MAssetDisposed extends X_A_Asset_Disposed
implements DocAction, DocOptions
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1763997880662445638L;

	public MAssetDisposed (Properties ctx, int A_Asset_Disposed_ID, String trxName)
	{
		super (ctx, A_Asset_Disposed_ID, trxName);
		if (A_Asset_Disposed_ID == 0)
		{
			setProcessed (false);
			setProcessing (false);
		}
		
	}
	
	//@win: autocreate asset disposal from ar invoice
	public static MAssetDisposed createAssetDisposed (MInvoiceLine invLine) {
		
		if (invLine == null)
			return null;
		
		MAssetDisposed assetDisposed = new MAssetDisposed(invLine);
		assetDisposed.dump();
		return assetDisposed;
	}
	
	private MAssetDisposed (MInvoiceLine invLine) {
		
		this(invLine.getCtx(),0,invLine.get_TrxName());
		if (log.isLoggable(Level.FINEST)) log.finest("Entering: Invoice line =" + invLine);
		setPostingType(POSTINGTYPE_Actual);
		//
		setM_InvoiceLine(invLine);  // also sets the associated amounts and dates.
		
		updateFromAsset(this);  // Get the asset balances
		
		// Calculate the disposal amounts - will be redone on complete
		
		if (getA_QTY_Current().signum() > 0 && getA_QTY_Delta().negate().compareTo(getA_QTY_Current()) <= 0)
		{
			BigDecimal ratio = getA_QTY_Delta().divide(getA_QTY_Current(), 12, RoundingMode.HALF_UP);  // normally negative
			setA_Asset_Cost_Delta(this.getA_Asset_Cost().multiply(ratio));
			setA_Accumulated_Depr_Delta(getA_Accumulated_Depr().multiply(ratio));
			setA_Disposal_GainLoss(getA_Disposal_Amt().subtract(getA_Asset_Cost_Delta().negate().subtract(getA_Accumulated_Depr_Delta().negate())));
		}
		else
		{
			throw new AssetCheckDocumentException("@A_QTY_Current@ < 0 || @A_QTY_Delta@ > @A_QTY_Current@");
		}

		saveEx(invLine.get_TrxName());
	}

	private final POCacheLocal<MInvoiceLine> m_cacheInvoiceLine = POCacheLocal.newInstance(this, MInvoiceLine.class);
	public MInvoiceLine getM_InvoiceLine(boolean requery)
	{
		return m_cacheInvoiceLine.get(requery);
	}
	
	
	private void setM_InvoiceLine(MInvoiceLine invLine)
	{
		if (invLine == null)
		{
			this.setC_InvoiceLine_ID(0);
			this.setA_Disposal_Amt(Env.ZERO);
			setA_Asset_ID(0);
			setA_Disposed_Date(null);
			setA_Disposed_Method(null);
			setA_QTY_Delta(Env.ZERO);
			return;
		}
		
		setA_Disposed_Method(A_DISPOSED_METHOD_Trade);

		setAD_Org_ID(invLine.getAD_Org_ID());

		MInvoice invoice = (MInvoice) invLine.getC_Invoice();		
		setDateDoc(invoice.getDateInvoiced());
		setDateAcct(invoice.getDateInvoiced());
		setA_Disposed_Date(invoice.getDateInvoiced());
		
		setC_Invoice_ID(invLine.getC_Invoice_ID());
		setC_InvoiceLine_ID(invLine.get_ID());
		m_cacheInvoiceLine.set(invLine);
		
		setA_Asset_ID(invLine.getA_Asset_ID());
		setA_QTY_Delta(invLine.getQtyInvoiced().negate());

		
		// Set the disposal amount - using the system currency
		int invCurrency_id = invoice.getC_Currency_ID();
		
		// Currency To
		int c_currencyTo_id = MClient.get(getCtx(), getAD_Client_ID()).getAcctSchema().getC_Currency_ID();

		BigDecimal disposalAmt = invLine.getLineNetAmt();
		
		if (c_currencyTo_id != invCurrency_id)
		{
			disposalAmt = MConversionRate.convert(getCtx(), invLine.getLineNetAmt(), 
					invCurrency_id, c_currencyTo_id, invoice.getDateInvoiced(), 
					0, getAD_Client_ID(), getAD_Org_ID());
			if (disposalAmt == null)
			{ // NoCurrencyConversion
				throw new NoCurrencyConversionException(invCurrency_id, c_currencyTo_id,
						invoice.getDateInvoiced(), 0,
						getAD_Client_ID(), getAD_Org_ID());
			}
		}

		setA_Disposal_Amt(disposalAmt);
				
	}
	//end @win: autocreate asset disposal from ar invoice
	
	public MAssetDisposed (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}
	
	public MAsset getAsset()
	{
		return MAsset.get(getCtx(), getA_Asset_ID(), null);
	}
	
	
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt
	
	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;

	
	public boolean unlockIt()
	{
		setProcessing(false);
		return true;
	}	//	unlockIt
	
	
	public boolean invalidateIt()
	{
		return false;
	}	//	invalidateIt
	
	
	public String prepareIt()
	{
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}
		
		if (MAsset.A_ASSET_STATUS_Disposed.equals(MAsset.get(getCtx(), getA_Asset_ID(), get_TrxName()).getA_Asset_Status()))
		{
			m_processMsg = "Asset alread disposed. A_Asset_ID " + getA_Asset_ID();
			return DocAction.STATUS_Invalid;
		}

		
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), "FAD", getAD_Org_ID());

		//saveEx() //commented by @win
		updateFromAsset(this);
		
		// Redo the calculations
		calculateAmounts();
		saveEx(get_TrxName()); //added by @win

		if (is_Changed())
		{
			throw new AssetStatusChangedException();
		}
		
		// Check that the FA is not just depreciated
		MDepreciationWorkfile assetwk = MDepreciationWorkfile.get(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		if (assetwk.isDepreciated(getDateAcct()))
		{
			throw new AssetAlreadyDepreciatedException();
		}
		MDepreciationExp.checkExistsNotProcessedEntries(getCtx(), getA_Asset_ID(), getDateAcct(), getPostingType(), get_TrxName());
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}
		//
		m_justPrepared = true;
		setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	
	private void calculateAmounts() {
		String method = this.getA_Disposed_Method();
		
		if (A_DISPOSED_METHOD_Simple_.equals(method))
		{
			// disposal amount = remaining undepreciated value with no (zero) gain/loss.
			setA_Asset_Cost_Delta(getA_Asset_Cost().negate());
			setA_Accumulated_Depr_Delta(getA_Accumulated_Depr().negate());
			setA_QTY_Delta(getA_QTY_Current().negate());
			setA_Disposal_Amt(getA_Asset_Cost().subtract(getA_Accumulated_Depr()));
			setA_Disposal_GainLoss(Env.ZERO);
		}
		else if (A_DISPOSED_METHOD_Trade.equals(method))  
		{
			if (getA_QTY_Current().signum() > 0 && getA_QTY_Delta().negate().compareTo(getA_QTY_Current()) <= 0)
			{
				BigDecimal ratio = getA_QTY_Delta().divide(getA_QTY_Current(), 12, RoundingMode.HALF_UP);  // normally negative
				setA_Asset_Cost_Delta(this.getA_Asset_Cost().multiply(ratio));
				setA_Accumulated_Depr_Delta(getA_Accumulated_Depr().multiply(ratio));
				setA_Disposal_GainLoss(getA_Disposal_Amt().subtract(getA_Asset_Cost_Delta().negate().subtract(getA_Accumulated_Depr_Delta().negate())));
			}
			else
			{
				throw new AssetCheckDocumentException("@A_QTY_Current@ < 0 || @A_QTY_Delta@ > @A_QTY_Current@");
			}
		}
	}

	public boolean approveIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("approveIt - " + toString());
		setIsApproved(true);
		return true;
	}	//	approveIt
	
	
	public boolean rejectIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("rejectIt - " + toString());
		setIsApproved(false);
		return true;
	}	//	rejectIt
	
	
	public String completeIt()
	{
		//	Re-Check
		if (!m_justPrepared)
		{
			String status = prepareIt();
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}
		
		String valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		if (log.isLoggable(Level.INFO)) log.info(toString());
		//
		
		//loading asset
		MAsset asset = getAsset();
		if (log.isLoggable(Level.FINE)) log.fine("asset=" + asset);

		// Activation
		if(!isDisposal())
		{
			String method = getA_Activation_Method();
			if(method.equals(A_ACTIVATION_METHOD_Activation))
			{ // reactivation
				asset.changeStatus(MAsset.A_ASSET_STATUS_Activated, getDateDoc());
			}
			else
			{
				throw new AssetNotSupportedException(COLUMNNAME_A_Activation_Method, method);
			}
		}
		// Preservation/Partial Retirement/etc
		else
		{
			String method = getA_Disposed_Method();
			if (A_DISPOSED_METHOD_Preservation.equals(method))
			{
				asset.changeStatus(MAsset.A_ASSET_STATUS_Preservation, getDateDoc());
			}
			// Goodwill BF: Disposed Method fix
			else if (A_DISPOSED_METHOD_Simple_.equals(method))
			{
				// Calculations updated during prepare
				asset.changeStatus(MAsset.A_ASSET_STATUS_Disposed, null);
				createDisposal();
			}
			else if (A_DISPOSED_METHOD_Trade.equals(method))  
			{
				// Calculations updated during prepare				
				if (getA_QTY_Current().signum() > 0 && getA_QTY_Delta().negate().compareTo(getA_QTY_Current()) == 0)
				{
					// Change the status to "Disposed" if the resulting quantity is zero
					asset.changeStatus(MAsset.A_ASSET_STATUS_Disposed, null);
				}
				createDisposal();
			}
			else if (A_DISPOSED_METHOD_PartialRetirement.equals(method))
			{
				createDisposal();
			}
			else
			{
				throw new AssetNotSupportedException(COLUMNNAME_A_Disposed_Method, method);
			}
		}
		
		// Goodwill - Disposal will update Product and Asset's Quantities
		MAssetProduct assetProduct = MAssetProduct.getCreate(getCtx(), 
				getA_Asset_ID(), asset.getM_Product_ID(), 
				asset.getM_AttributeSetInstance_ID(), get_TrxName());
		
		assetProduct.setA_QTY_Current(getA_QTY_Current().add(getA_QTY_Delta())); // The delta is negative
		assetProduct.setAD_Org_ID(asset.getAD_Org_ID());
		assetProduct.saveEx();
		assetProduct.updateAsset(asset);
		
		asset.setA_QTY_Current(getA_QTY_Current().add(getA_QTY_Delta()));
		asset.setQty(getA_QTY_Current().add(getA_QTY_Delta()));
		// End - Update
		
		asset.saveEx(get_TrxName());
		
		//	User Validation
		valid = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (valid != null)
		{
			m_processMsg = valid;
			return DocAction.STATUS_Invalid;
		}
		
		// Done
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	
	public boolean voidIt()
	{
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		
		this.setA_Asset_Cost_Delta(Env.ZERO);
		this.setA_Accumulated_Depr_Delta(Env.ZERO);
		this.setA_QTY_Delta(Env.ZERO);
		this.setA_Disposal_GainLoss(Env.ZERO);
		this.setA_Disposal_Amt(Env.ZERO);

		//	User Validation
		String errmsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
		if (errmsg != null)
		{
			m_processMsg = errmsg;
			return false;
		}
		
		// finish
		setProcessed(true);
		setDocAction(DOCACTION_None);
		return true;
	}	//	voidIt
	
	
	public boolean closeIt()
	{
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt
	
	
	public boolean reverseCorrectIt()
	{
		throw new AssetNotImplementedException("");
	}	//	reverseCorrectionIt
	
	
	public boolean reverseAccrualIt()
	{
		throw new AssetNotImplementedException("");
	}
	
	
	public boolean reActivateIt()
	{
		// TODO for testing only
		//throw new AssetNotImplementedException("");
		
		setProcessed(false);
		setDocStatus(DocAction.STATUS_InProgress);
		setDocAction(DOCACTION_Complete);
		return true;
	}
	
	
	
	public String getSummary()
	{
		return new StringBuffer()
				.append(getDocumentNo()).append("/").append(getDateDoc())
				.toString();
	}

	
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}

	
	public BigDecimal getApprovalAmt()
	{
		return Env.ZERO;
	} 
	
	
	public int getC_Currency_ID()
	{
		return MClient.get(getCtx(), getAD_Client_ID()).getAcctSchema().getC_Currency_ID();
	}
	
	
	protected boolean beforeSave (boolean newRecord)
	{
		if (MAsset.A_ASSET_STATUS_Disposed.equals(MAsset.get(getCtx(), getA_Asset_ID(), get_TrxName()).getA_Asset_Status()))
		{
			log.severe("Asset alread disposed. A_Asset_ID " + getA_Asset_ID());
			return false;
		}
		
		if (getDateAcct() == null)
		{
			setDateAcct(getDateDoc());
		}
		if (newRecord || is_ValueChanged(COLUMNNAME_DateAcct))
		{
			setC_Period_ID(MPeriod.get(getCtx(), getDateAcct(), getAD_Org_ID()).get_ID());
		}
		if (getA_Disposed_Date() == null)
		{
			setA_Disposed_Date(getDateAcct());
		}
		/* commented by @win - asset type
		if (!MAssetType.isFixedAsset(getA_Asset_ID()))
		{
			throw new AssetException("This is not a Fixed Asset!");
		}
		*/
		return true;
	}
	
	/**
	 * Copy fields from A_Asset
	 * @param model
	 * @param A_Asset_ID
	 */
	public static void updateFromAsset(I_A_Asset_Disposed bean)
	{
		int asset_id = bean.getA_Asset_ID();
		SetGetUtil.copyValues(
				SetGetUtil.wrap(bean),
				MAsset.Table_Name, asset_id,
				new String[] {
					MAsset.COLUMNNAME_IsDisposed,
					MAsset.COLUMNNAME_A_Asset_Status,
					"AD_Org_ID",
				}
		);
		
		MDepreciationWorkfile wk = MDepreciationWorkfile.get(Env.getCtx(), asset_id, bean.getPostingType(), null);
		if (wk != null)
		{
			bean.setA_Asset_Cost(wk.getA_Asset_Cost());
			bean.setA_Accumulated_Depr(wk.getA_Accumulated_Depr());
			bean.setA_QTY_Current(wk.getA_QTY_Current());
			if (bean.getA_QTY_Delta().signum() == 0)
				bean.setA_QTY_Delta(wk.getA_QTY_Current());
		}
		else
		{
			bean.setA_Asset_Cost(Env.ZERO);
			bean.setA_Accumulated_Depr(Env.ZERO);
			bean.setA_QTY_Current(Env.ZERO);
			bean.setA_QTY_Delta(Env.ZERO);
		}
	}
	
	
	public File createPDF ()
	{
		return null;
	}	//	createPDF
	
	
	public String getDocumentInfo()
	{
		return getDocumentNo();
	}	//	getDocumentInfo
	
	/**
	 * Check if this is a disposal (if the asset is not disposed)
	 * @return true if is disposal
	 */
	public boolean isDisposal()
	{
		return !isDisposed();
	}
	
	/**
	 * Sets the disposal amount to the remaining undepreciated value of the asset and the 
	 * gain/loss to zero.
	 * 
	 * @param disposal The initialized disposal record with the current balance values set according 
	 * to the depreciation worksheet for the asset. 
	 */
	public static void setA_Disposal_Amt(I_A_Asset_Disposed disposal)
	{
		
		int C_Currency_ID = MClient.get(Env.getCtx()).getC_Currency_ID();
		int precision = MCurrency.getStdPrecision(Env.getCtx(), C_Currency_ID);

		BigDecimal currentQty = disposal.getA_QTY_Current();
		BigDecimal deltaQty = disposal.getA_QTY_Delta();
		BigDecimal coef = Env.ZERO;
		if (currentQty.signum() != 0)
		{
			coef = deltaQty.divide(currentQty, 12, RoundingMode.HALF_UP);
		}
		//
		BigDecimal A_Accumulated_Depr = disposal.getA_Accumulated_Depr();
		BigDecimal A_Accumulated_Depr_Delta = A_Accumulated_Depr.multiply(coef).setScale(precision, RoundingMode.HALF_UP);
		BigDecimal A_Asset_Cost = disposal.getA_Asset_Cost();
		BigDecimal A_Asset_Cost_Delta = A_Asset_Cost.multiply(coef).setScale(precision, RoundingMode.HALF_UP);
		
		BigDecimal disposalAmt = A_Asset_Cost_Delta.subtract(A_Accumulated_Depr_Delta);
		//
		disposal.setA_Accumulated_Depr_Delta(A_Accumulated_Depr_Delta);
		disposal.setA_Asset_Cost_Delta(A_Asset_Cost_Delta);
		disposal.setA_Disposal_Amt(disposalAmt);
		disposal.setA_Disposal_GainLoss(Env.ZERO);
	}
	
	private void createDisposal()
	{
		MDepreciationWorkfile assetwk = MDepreciationWorkfile.get(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		//assetwk.adjustCost(getA_Disposal_Amt().negate(), Env.ZERO, false);
		
		//
		// Delete not processed expense entries. Do this before building workfile.
		List<MDepreciationExp> list = MDepreciationExp.getNotProcessedEntries(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		for (MDepreciationExp ex : list)
		{
			ex.deleteEx(false);
		}

		// Goodwill - Disposed Asset must not have quantity
		// mckayERP - Except when the asset is collective and only a portion is disposed.
		assetwk.adjustCost(getA_Asset_Cost_Delta().negate(), getA_QTY_Delta().negate(), false);
		
		assetwk.adjustAccumulatedDepr(getA_Accumulated_Depr_Delta().negate(), getA_Accumulated_Depr_Delta().negate(), false);
		assetwk.saveEx();
		assetwk.buildDepreciation();
		
		// Goodwill - Update Asset History
		MAssetChange.createDisposal(this, assetwk);
	}
	
	@Override
	public int customizeValidActions(String docStatus, Object processing,
			String orderType, String isSOTrx, int AD_Table_ID,
			String[] docAction, String[] options, int index) {
		// TODO Auto-generated method stub
		
		if (docStatus.equals(DocumentEngine.STATUS_Completed))
		{
			//options[index++] = DocumentEngine.ACTION_Reverse_Correct;
			options[index++] = DocumentEngine.ACTION_Void;
			options[index++] = DocumentEngine.ACTION_ReActivate;
		}
		
		if (docStatus.equals(DocumentEngine.STATUS_Voided))
		{
			//options[index++] = DocumentEngine.ACTION_Reverse_Correct;
			//options[index++] = DocumentEngine.ACTION_Void;
			options[index++] = DocumentEngine.ACTION_ReActivate;
		}
		
		return index;
	}
	
	/**************************************************************************
	 * 	Get Action Options based on current Status
	 *	@return array of actions
	 */

	public String[] getCustomizedActionOptions() {
		return null;
	}

	/**
	 * Process a custom action similar to the {@link org.compiere.process.DocumentEngine#processIt(String) processIt()} method in DocumentEngine.java.
	 * @param customAction - the two letter action code.
	 * @return true is successful.
	 */
	public boolean processCustomAction(String customAction) {
		return false;
	}

}
