package org.compiere.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.process.DocAction;
import org.compiere.process.DocOptions;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProjectClose;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.FA.exceptions.AssetAlreadyDepreciatedException;
import org.compiere.FA.exceptions.AssetCheckDocumentException;
import org.compiere.FA.exceptions.AssetException;
import org.compiere.FA.exceptions.AssetNotImplementedException;
import org.compiere.FA.exceptions.AssetNotSupportedException;
import org.compiere.FA.feature.UseLifeImpl;
import org.adempiere.util.POCacheLocal;

/**
 *  Asset Addition Model
 *	@author Teo Sarca, SC ARHIPAC SERVICE SRL
 *
 * TODO: BUG: REG in depexp creates a zero if they have more sites Addition during 0?!
 */
public class MAssetAddition extends X_A_Asset_Addition 
	implements DocAction, DocOptions
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5977180589101094202L;
	
	/** Static Logger */
	private static CLogger s_log = CLogger.getCLogger(MAssetAddition.class);

	public MAssetAddition (Properties ctx, int A_Asset_Addition_ID, String trxName)
	{
		super (ctx, A_Asset_Addition_ID, trxName);
		if (A_Asset_Addition_ID == 0)
		{
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction(DOCACTION_Complete);
			setProcessed(false);
		}
	}	//	MAssetAddition
	public MAssetAddition (Properties ctx, ResultSet rs, String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MAAssetAddition

	
	protected boolean beforeSave (boolean newRecord)
	{
		setA_CreateAsset();
		/*
		if (getA_Asset().getM_Locator_ID() > 0) 
		{
			if (getM_Locator_ID() <= 0)
				setM_Locator_ID(getA_Asset().getM_Locator_ID());
			if (!getM_Locator().equals(getA_Asset().getM_Locator()))
				throw new AssetException("Asset and Addition's Locator are different");
		}*/
		
		//Goodwill - Set Useable Life according to Asset Group Accounting
		//TODO - multi-schema?
		MAssetGroupAcct assetGrpAcct;
		int assetGroupID = 0;
		if (this.getA_Asset_ID() > 0)
		{
			// Try the asset ID
			assetGroupID = getA_Asset().getA_Asset_Group_ID();
		}
		else
		{
			// Fall back to the default group
			assetGroupID = MAssetGroup.getDefault_ID(this.getCtx());
		}
		assetGrpAcct = MAssetGroupAcct.forA_Asset_Group_ID(getCtx(), assetGroupID, POSTINGTYPE_Actual);
		
		// MckayERP - Rather the Asset workbook than the group acct - allows for more control.  
		// The group is the template.  Assets within the group can vary.
		// TODO - multi-schema - need an asset addition for each schema!  Assume there is only one.
		int life = assetGrpAcct.getUseLifeYears();
		int life_f = assetGrpAcct.getUseLifeYears_F();
		
		MDepreciationWorkfile assetWk = null;
		try
		{
			Collection<MDepreciationWorkfile> assetWkCollection = MDepreciationWorkfile.forA_Asset_ID(getCtx(), getA_Asset_ID(), get_TrxName());
			assetWk = assetWkCollection.iterator().next();
			
			if (assetWk.getUseLifeYears() <= 0)
			{
				assetWk.setUseLifeYears(life);
			}
			else
			{
				life = assetWk.getUseLifeYears();
			}
			
			if (assetWk.getUseLifeYears_F() <= 0)
			{
				assetWk.setUseLifeYears_F(life_f);
			}
			else
			{
				life_f = assetWk.getUseLifeYears_F();
			}
		}
		catch (NullPointerException | NoSuchElementException e )
		{
			// Ignore
		}
		
		if (isA_CreateAsset())
		{
			setDeltaUseLifeYears(life);
			setDeltaUseLifeYears_F(life_f);
		}
		
		//Goodwill - Check for source invoice
		if (A_SOURCETYPE_Invoice.equals(getA_SourceType()))
		{
			MInvoiceLine iLine = new MInvoiceLine(getCtx(), getC_InvoiceLine_ID(), get_TrxName());
			if (A_CAPVSEXP_Capital.equals(iLine.getA_CapvsExp()))
			{
				if (!iLine.get_ValueAsBoolean("IsCollectiveAsset")) 
					setA_QTY_Current(Env.ONE);  // Force single unit quantity unless IsCollectiveAsset
				else
					setA_QTY_Current(iLine.getQtyEntered());
	
				if (iLine.getA_Asset_ID() > 0)
				{
					setA_CreateAsset(false);
				}
			}
			setA_CapvsExp(iLine.getA_CapvsExp());
		}//Goodwill - End check

		if (isA_CreateAsset() && getA_QTY_Current().signum() == 0 && !A_SOURCETYPE_Invoice.equals(getA_SourceType()))
		{
			setA_QTY_Current(Env.ONE);
		}
		if (getC_Currency_ID() <= 0)
		{
			setC_Currency_ID(MClient.get(getCtx()).getAcctSchema().getC_Currency_ID());
		}
		if (getC_ConversionType_ID() <= 0)
		{
			setC_ConversionType_ID(MConversionType.getDefault(getAD_Client_ID()));
		}
		getDateAcct();
		setAssetValueAmt();
		
		// Enforce asset product for capital creation of asset
		if (isA_CreateAsset() && MAssetAddition.A_CAPVSEXP_Capital.equals(getA_CapvsExp()))
		{
			this.setM_Product_ID(this.getA_Asset().getM_Product_ID());  // Should only create the asset product
		}
		
		//
		// Check suspect asset values (UseLife, Amount etc):
		/* arhipac: teo_sarca: TODO need to integrate
		if (hasZeroValues() && (!m_confirmed_AssetValues && !isProcessed() && is_UserEntry()))
		{
			String msg = "@AssetValueAmt@="+getAssetValueAmt()
						+ "\n@DeltaUseLifeYears@="+getDeltaUseLifeYears()
							+ "\n@DeltaUseLifeYears_F@="+getDeltaUseLifeYears_F()+"\n";
			m_confirmed_AssetValues = UIFactory.getUI().ask(get_WindowNo(), null, "Confirm", Msg.parseTranslation(getCtx(), msg), true);
			if (!m_confirmed_AssetValues)
			{
				throw new AssetCheckDocumentException(msg);
			}
		}
		*/
		
		// set approved
		setIsApproved();
		
		return true;
	}	//	beforeSave
	
//	private boolean m_confirmed_AssetValues = false;

	/**
	 * Create Asset and asset Addition from MMatchInv.
	 * MAssetAddition is saved.
	 * @param match match invoice
	 * @return asset addition
	 */
	public static MAssetAddition createAsset(MMatchInv match)
	{
		MAssetAddition assetAdd = new MAssetAddition(match);
		assetAdd.dump();
		assetAdd.saveEx();
		return assetAdd;
	}

	/**
	 * Create Asset Addition from an Invoice - expense types only.
	 * MAssetAddition is saved.
	 * @param match match invoice
	 * @return asset addition
	 */
	public static MAssetAddition createAssetAddition(MInvoiceLine invoiceLine)
	{
		// Want to be able to easily create assets from vendor invoices - implies both capital and expense transactions
		//if (! MInvoiceLine.A_CAPVSEXP_Expense.equals(invoiceLine.getA_CapvsExp()) )
		//		return null;
		
		MAssetAddition assetAddition = new MAssetAddition(invoiceLine);
		
		assetAddition.saveEx();
		
		return assetAddition;
	}

	/**
	 * Create Asset and asset Addition from MIFixedAsset. MAssetAddition is saved. 
	 * (@win note, not referenced from anywhere. incomplete feature)
	 * @param	match	match invoice
	 * @return asset addition
	 */
	public static MAssetAddition createAsset(MIFixedAsset ifa)
	{
		MAssetAddition assetAdd = new MAssetAddition(ifa);
		assetAdd.dump();
		//@win add condition to prevent asset creation when expense addition or second addition
		if (MAssetAddition.A_CAPVSEXP_Capital.equals(assetAdd.getA_CapvsExp())
				&& ifa.getA_Asset_ID() == 0) { 
		//end @win add condition to prevent asset creation when expense addition or second addition
			MAsset asset = assetAdd.createAsset();
			asset.dump();	
		}
		assetAdd.saveEx();
		return assetAdd;
	}
	
	//@win create asset from Project
	/**
	 * Create Asset and asset Addition from MProject. MAssetAddition is saved. 
	 * Addition from Project only allows initial addition (will definitely create new asset)
	 * @param	project
	 * @return asset addition
	 */
	public static MAssetAddition createAsset(MProject project, MProduct product)
	{
		MAssetAddition assetAdd = new MAssetAddition(project);
		assetAdd.dump();
		
		MAsset asset = assetAdd.createAsset();
		
		if (product != null) {
			asset.setM_Product_ID(product.getM_Product_ID());
			asset.setA_Asset_Group_ID(product.getA_Asset_Group_ID());
			MAttributeSetInstance asi = MAttributeSetInstance.create(Env.getCtx(), product, null);
			asset.setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
			assetAdd.setM_Product_ID(product.getM_Product_ID());
		}
		asset.setName(product.getName().concat(project.getName()));
		asset.setValue(product.getName().concat(project.getName()));	
		asset.saveEx();
		asset.dump();
		
		// Copy UseLife values from asset group to workfile
		MAssetGroupAcct assetgrpacct = MAssetGroupAcct.forA_Asset_Group_ID(asset.getCtx(), asset.getA_Asset_Group_ID(), assetAdd.getPostingType());
		assetAdd.setDeltaUseLifeYears(assetgrpacct.getUseLifeYears());
		assetAdd.setDeltaUseLifeYears_F(assetgrpacct.getUseLifeYears_F());
		assetAdd.setA_Asset(asset);
		assetAdd.saveEx();
		//@win add
		
		return assetAdd;
	}
	//end @win create asset from Project
	/**	
	 * Create Asset
	 */
	private MAsset createAsset()
	{
		MAsset asset = null;
		if (getA_Asset_ID() <= 0)
		{
			String sourceType = getA_SourceType();
			if (A_SOURCETYPE_Invoice.equals(sourceType))
			{
				if (getMatchInv(false) == null)
				{
					asset = new MAsset((MInvoiceLine) getC_InvoiceLine());
				}
				else
				{
					asset = new MAsset(getMatchInv(false));
				}
				asset.saveEx();
				
				//Goodwill - update asset name
				asset.setName(asset.getName() + "-" + asset.getInventoryNo());
				
				asset.saveEx();
				setA_Asset(asset);
			}
			else if (A_SOURCETYPE_Imported.equals(sourceType))
			{
				asset = new MAsset(getI_FixedAsset(false));
				asset.saveEx();
				setA_Asset(asset);
			}
			else if (A_SOURCETYPE_Project.equals(sourceType))
			{
				//@win add code for generate from Project
				asset = new MAsset(getC_Project(false));
				//end @win add code for generate from Project
			}
			else
			{
				throw new AssetNotSupportedException(COLUMNNAME_A_SourceType, sourceType);
			}
		}
		else
		{
			asset = getA_Asset(false);
		}
		//
		return asset;
	}
	
	/**
	 * Construct addition from match invoice 
	 * @param match	match invoice model
	 */
	private MAssetAddition (MMatchInv match)
	{
		this(match.getCtx(), 0, match.get_TrxName());
		setM_MatchInv(match);
	}
	
	//added by @win
	/**
	 * @author @win
	 * Construct addition from Project
	 * @param project 
	 */
	private MAssetAddition (MProject project)
	{
		this(project.getCtx(), 0, project.get_TrxName());
		if (log.isLoggable(Level.FINEST)) log.finest("Entering: Project=" + project);
		setAD_Org_ID(project.getAD_Org_ID());
		setPostingType(POSTINGTYPE_Actual);
		setA_SourceType(A_SOURCETYPE_Project);
		//
		setC_Currency_ID(project.getC_Currency_ID());
		if (project.get_ValueAsInt("C_ConversionType_ID")>0) {
			setC_ConversionType_ID(project.get_ValueAsInt("C_ConversionType_ID"));
		}
		setSourceAmt(project.getProjectBalanceAmt());
		setDateDoc(new Timestamp (System.currentTimeMillis()));
		setA_CreateAsset(true); //added by @win as create from project will certainly for createnew
		setA_CapvsExp(A_CAPVSEXP_Capital); // ensure the product is the asset product
		setDeltaUseLifeYears(I_ZERO);
		setDeltaUseLifeYears_F(I_ZERO);
		setC_DocType_ID();
		
		Timestamp dateAcct = new Timestamp (System.currentTimeMillis());
		if (dateAcct != null)
		{
			dateAcct = UseLifeImpl.getDateAcct(dateAcct, 1);
			if (log.isLoggable(Level.FINE)) log.fine("DateAcct=" + dateAcct);
			setDateAcct(dateAcct);
		}
		setC_Project(project);
	}
	
	private final POCacheLocal<MProject> m_cacheCProject = POCacheLocal.newInstance(this, MProject.class);
	public MProject getC_Project(boolean requery)
	{
		return m_cacheCProject.get(requery);
	}
	private void setC_Project(MProject project)
	{
		set_Value("C_Project_ID", project.get_ID());
		m_cacheCProject.set(project);
	}
	//end added by @win
	/**IDR
	 * Construct addition from import
	 * @param ifa	fixed asset import
	 */
	private MAssetAddition (MIFixedAsset ifa)
	{
		this(ifa.getCtx(), 0, ifa.get_TrxName());
		if (log.isLoggable(Level.FINEST)) log.finest("Entering: ifa=" + ifa);
		setAD_Org_ID(ifa.getAD_Org_ID());
		setPostingType(POSTINGTYPE_Actual);
		setA_SourceType(A_SOURCETYPE_Imported);
		//
		setM_Product_ID(ifa.getM_Product_ID());
		setSourceAmt(ifa.getA_Asset_Cost());
		setDateDoc(ifa.getAssetServiceDate());
		setM_Locator_ID(ifa.getM_Locator_ID());
		
		boolean isAccmDeprAdjust = (ifa.getA_Accumulated_Depr().compareTo(Env.ZERO) > 0) ? true : false;
		setA_Accumulated_Depr_Adjust(isAccmDeprAdjust);
		
		//Goodwill - if imported asset was already fully depreciated
		if (ifa.getA_Current_Period() == 0) 
		{
			setA_Period_Start(ifa.getUseLifeMonths() + 1);
			ifa.setA_Accumulated_Depr_F(ifa.getA_Accumulated_Depr());
			ifa.setUseLifeMonths_F(ifa.getUseLifeMonths());
		}
		//Goodwill - normal imported asset
		else 
			setA_Period_Start(ifa.getA_Current_Period());

		setA_Accumulated_Depr(ifa.getA_Accumulated_Depr());
		setA_Accumulated_Depr_F(ifa.getA_Accumulated_Depr_F());
		setDeltaUseLifeYears((int)(ifa.getUseLifeMonths() / 12));
		setDeltaUseLifeYears_F((int)(ifa.getUseLifeMonths_F() / 12));
		
		setA_CapvsExp(MAssetAddition.A_CAPVSEXP_Capital); //added by zuhri, import must be in Capital
		setA_CreateAsset(true); //added by zuhri, import must be create asset
		setA_Salvage_Value(ifa.getA_Salvage_Value());
		setC_DocType_ID();
		
		Timestamp dateAcct = ifa.getDateAcct();
		if (dateAcct != null)
		{
			//dateAcct = UseLifeImpl.getDateAcct(dateAcct, 1); //commented by @win -- i don't see why i should add 1 month
			if (log.isLoggable(Level.FINE)) log.fine("DateAcct=" + dateAcct);
			setDateAcct(dateAcct);
		}
		setI_FixedAsset(ifa);
	}
	
	private MAssetAddition(MInvoiceLine invoiceLine) {
		
		this(invoiceLine.getCtx(), 0, invoiceLine.get_TrxName());
		
		
		this.setPostingType(POSTINGTYPE_Actual);
		this.setA_SourceType(A_SOURCETYPE_Invoice);
		this.setC_InvoiceLine_ID(invoiceLine.getC_InvoiceLine_ID());
		this.setC_Invoice_ID(invoiceLine.getC_Invoice_ID());
		
		this.setDateDoc(invoiceLine.getC_Invoice().getDateInvoiced());
		this.setDateAcct(invoiceLine.getC_Invoice().getDateAcct());
		this.setDescription(invoiceLine.getDescription());

		this.setA_CapvsExp(invoiceLine.getA_CapvsExp());
		this.setM_Product_ID(invoiceLine.getM_Product_ID());  	// Important as it sets the expense account for the addition
		this.setC_Charge_ID(invoiceLine.getC_Charge_ID()); 		// Important as it sets the expense account for the addition
		this.setM_AttributeSetInstance_ID(invoiceLine.getM_AttributeSetInstance_ID());
		
		if (this.A_CAPVSEXP_Expense.equals(this.getA_CapvsExp()))
		{
			this.setA_QTY_Current(Env.ZERO);  // expenses don't change the quantities
		}
		else
		{
			this.setA_QTY_Current(invoiceLine.getQtyInvoiced());  // Product UoM
		}

		this.setC_Currency_ID(invoiceLine.getC_Invoice().getC_Currency_ID());
		
		BigDecimal sourceAmt = invoiceLine.getLineNetAmt();
		if (((MInvoice) invoiceLine.getC_Invoice()).isCreditMemo())
		{
			sourceAmt = sourceAmt.negate();
		}
		this.setAssetSourceAmt(sourceAmt);
		
		this.setA_Accumulated_Depr_Adjust(false);  // not from invoice

		this.setA_Asset(createAsset());

	}

	/** Match Invoice Cache */
	private final POCacheLocal<MMatchInv> m_cacheMatchInv = POCacheLocal.newInstance(this, MMatchInv.class);

	/**
	 * @param requery
	 * @return
	 */
	private MMatchInv getMatchInv(boolean requery)
	{
		return m_cacheMatchInv.get(requery);
	}
	
	private void setC_InvoiceLine(MInvoiceLine invoiceLine)
	{
		setAD_Org_ID(invoiceLine.getAD_Org_ID());
		setPostingType(POSTINGTYPE_Actual);
		setA_SourceType(A_SOURCETYPE_Invoice);
		setC_DocType_ID();

		if (MAssetAddition.A_CAPVSEXP_Capital.equals(invoiceLine.getA_CapvsExp()))
		{
			if (invoiceLine.getA_Asset_ID() == 0)
			{
				setA_CreateAsset(true);
				MAsset asset = createAsset();
				//@win add
				MAssetGroupAcct assetgrpacct = MAssetGroupAcct.forA_Asset_Group_ID(asset.getCtx(), asset.getA_Asset_Group_ID(), getPostingType());
				setDeltaUseLifeYears(assetgrpacct.getUseLifeYears());
				setDeltaUseLifeYears_F(assetgrpacct.getUseLifeYears_F());
			}
			else if (invoiceLine.getA_Asset_ID() > 0)
			{
				setA_CreateAsset(false);
				setA_Asset_ID(invoiceLine.getA_Asset_ID());
			}
		}
		else 
			setA_CreateAsset(false);
		 
		setC_Invoice_ID(invoiceLine.getC_Invoice_ID());
		setC_InvoiceLine_ID(invoiceLine.getC_InvoiceLine_ID());
		setLine(invoiceLine.getLine());

		setM_Product_ID(invoiceLine.getM_Product_ID());
		setM_AttributeSetInstance_ID(invoiceLine.getM_AttributeSetInstance_ID());

		setC_Charge_ID(invoiceLine.getC_Charge_ID());

		setA_CapvsExp(invoiceLine.getA_CapvsExp());
		setC_Currency_ID(invoiceLine.getC_Invoice().getC_Currency_ID());
		setC_ConversionType_ID(invoiceLine.getC_Invoice().getC_ConversionType_ID());
		setDateDoc(invoiceLine.getC_Invoice().getDateInvoiced());
		setDateAcct(invoiceLine.getC_Invoice().getDateInvoiced());

		//Goodwill - If the quantities were collective 
		if (MInvoiceLine.A_CAPVSEXP_Capital.equals(invoiceLine.getA_CapvsExp()) && invoiceLine.get_ValueAsBoolean("IsCollectiveAsset"))
		{
			setA_QTY_Current(invoiceLine.getQtyInvoiced());
			setAssetAmtEntered(invoiceLine.getLineNetAmt());
			setAssetSourceAmt(invoiceLine.getLineNetAmt());		
		}
		//Goodwill - If the quantities were not collective
		if (MInvoiceLine.A_CAPVSEXP_Capital.equals(invoiceLine.getA_CapvsExp()) 
				&& !invoiceLine.get_ValueAsBoolean("IsCollectiveAsset") 
				&& invoiceLine.getA_Asset_ID() <= 0
				&& invoiceLine.getQtyInvoiced().signum() != 0)
		{
			setA_QTY_Current(Env.ONE);
			setAssetAmtEntered(invoiceLine.getLineNetAmt().divide(invoiceLine.getQtyInvoiced()));
			setAssetSourceAmt(invoiceLine.getLineNetAmt().divide(invoiceLine.getQtyInvoiced()));
		}
		//Goodwill - If the invoice not create new asset
		// or if the invoice is an expense type
		if ((MInvoiceLine.A_CAPVSEXP_Capital.equals(invoiceLine.getA_CapvsExp()) && invoiceLine.getA_Asset_ID() > 0) || 
				MInvoiceLine.A_CAPVSEXP_Expense.equals(invoiceLine.getA_CapvsExp()))
		{
			setA_QTY_Current(Env.ZERO);
			setAssetAmtEntered(invoiceLine.getLineNetAmt());
			setAssetSourceAmt(invoiceLine.getLineNetAmt());
		}
		
		//@win add condition to prevent asset creation when expense addition or second addition
		if (MAssetAddition.A_CAPVSEXP_Capital.equals(getA_CapvsExp())
			&& invoiceLine.getA_Asset_ID() == 0 && isA_CreateAsset()) 
		{ 
		//end @win add condition to prevent asset creation when expense addition or second addition
			MAsset asset = createAsset();
			asset.dump();
			//@win add
			MAssetGroupAcct assetgrpacct = MAssetGroupAcct.forA_Asset_Group_ID(asset.getCtx(), asset.getA_Asset_Group_ID(), getPostingType());
			setDeltaUseLifeYears(assetgrpacct.getUseLifeYears());
			setDeltaUseLifeYears_F(assetgrpacct.getUseLifeYears_F());
		} 
		else {
			setA_Asset_ID(invoiceLine.getA_Asset_ID());
			setA_CreateAsset(false);
		}

	}
	
	private void setM_MatchInv(MMatchInv mi)
	{
		mi.load(get_TrxName());
		MInvoiceLine iLine = new MInvoiceLine(getCtx(), mi.getC_InvoiceLine_ID(), get_TrxName());
		setC_InvoiceLine(iLine);

		// Match invoice specifics
		setM_MatchInv_ID(mi.get_ID());
		
		setM_InOutLine_ID(mi.getM_InOutLine_ID());
		setM_Locator_ID(mi.getM_InOutLine().getM_Locator_ID());
		
		//Goodwill - If the quantities were collective 
		if (MInvoiceLine.A_CAPVSEXP_Capital.equals(iLine.getA_CapvsExp()) && iLine.get_ValueAsBoolean("IsCollectiveAsset"))
		{
			setA_QTY_Current(mi.getQty());
			setAssetAmtEntered(mi.getC_InvoiceLine().getLineNetAmt());
			setAssetSourceAmt(mi.getC_InvoiceLine().getLineNetAmt());		
		}
		//Goodwill - If the quantities were not collective
		if (MInvoiceLine.A_CAPVSEXP_Capital.equals(iLine.getA_CapvsExp()) && !iLine.get_ValueAsBoolean("IsCollectiveAsset") && iLine.getA_Asset_ID() <= 0)
		{
			setA_QTY_Current(Env.ONE);
			setAssetAmtEntered(mi.getC_InvoiceLine().getLineNetAmt().divide(mi.getQty()));
			setAssetSourceAmt(mi.getC_InvoiceLine().getLineNetAmt().divide(mi.getQty()));
		}

		m_cacheMatchInv.set(mi);
	}
	
	/**
	 * Copy fields from MatchInv+InvoiceLine+InOutLine
	 * @param model - to copy from
	 * @param M_MatchInv_ID - matching invoice id
	 * @param newRecord new object model is created
	 */
	public static boolean setM_MatchInv(SetGetModel model, int M_MatchInv_ID)
	{
		boolean newRecord = false;
		String trxName = null;
		if (model instanceof PO)
		{
			PO po = (PO)model;
			newRecord = po.is_new();
			trxName = po.get_TrxName();
			
		}
		
		if (s_log.isLoggable(Level.FINE)) s_log.fine("Entering: model=" + model + ", M_MatchInv_ID=" + M_MatchInv_ID + ", newRecord=" + newRecord + ", trxName=" + trxName);
		
		final String qMatchInv_select = "SELECT"
				+ "  C_Invoice_ID"
				+ ", C_InvoiceLine_ID"
				+ ", M_InOutLine_ID"
				+ ", M_Product_ID"
				+ ", M_AttributeSetInstance_ID"
				+ ", Qty AS "+COLUMNNAME_A_QTY_Current
				+ ", InvoiceLine AS "+COLUMNNAME_Line
				+ ", M_Locator_ID"
				+ ", A_CapVsExp"
				+ ", MatchNetAmt AS "+COLUMNNAME_AssetAmtEntered
				+ ", MatchNetAmt AS "+COLUMNNAME_AssetSourceAmt
				+ ", C_Currency_ID"
				+ ", C_ConversionType_ID"
				+ ", MovementDate AS "+COLUMNNAME_DateDoc
		;
		final String qMatchInv_from = " FROM mb_matchinv WHERE M_MatchInv_ID="; //@win change M_MatchInv_ARH to M_MatchInv
		
		String query = qMatchInv_select;
		if (newRecord) {
			query += ", A_Asset_ID, A_CreateAsset";
		}
		query += qMatchInv_from + M_MatchInv_ID;
		
		/*
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try
		{
			pstmt = DB.prepareStatement(query, trxName);
			DB.setParameters(pstmt, params);
			rs = pstmt.executeQuery();
			updateColumns(models, columnNames, rs);
		}
		catch (SQLException e)
		{
			throw new DBException(e, query);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
		*/
		SetGetUtil.updateColumns(model, null, query, trxName);
		
		s_log.fine("Leaving: RETURN TRUE");
		return true;
	}
	
	private final POCacheLocal<MIFixedAsset> m_cacheIFixedAsset = POCacheLocal.newInstance(this, MIFixedAsset.class);
	public MIFixedAsset getI_FixedAsset(boolean requery)
	{
		return m_cacheIFixedAsset.get(requery);
	}
	private void setI_FixedAsset(MIFixedAsset ifa)
	{
		setI_FixedAsset_ID(ifa.get_ID());
		m_cacheIFixedAsset.set(ifa);
	}
	
	/**
	 *	Sets the AssetValueAmt from AssetSourceAmt using C_Currency_ID and C_ConversionRate_ID
	 */
	private void setAssetValueAmt()
	{
		getDateAcct();
		MConversionRateUtil.convertBase(SetGetUtil.wrap(this),
				COLUMNNAME_DateAcct,
				COLUMNNAME_AssetSourceAmt,
				COLUMNNAME_AssetValueAmt,
				null);
	}
	
	public void setSourceAmt(BigDecimal amt)
	{
		setAssetAmtEntered(amt);
		setAssetSourceAmt(amt);
	}
	
	/**
	 *
	 */
	public void setIsApproved()
	{
		if(!isProcessed())
		{
			String str = Env.getContext(getCtx(), "#IsCanApproveOwnDoc");
			boolean isApproved = "Y".equals(str); //ARHIPAC.toBoolean(str, false);
			if (log.isLoggable(Level.FINE)) log.fine("#IsCanApproveOwnDoc=" + str + "=" + isApproved);
			setIsApproved(isApproved);
		}
	}
	
	
	public Timestamp getDateAcct()
	{
		Timestamp dateAcct = super.getDateAcct();
		if (dateAcct == null) {
			dateAcct = getDateDoc();
			setDateAcct(dateAcct);
		}
		return dateAcct;
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
		if (log.isLoggable(Level.INFO)) log.info("unlockIt - " + toString());
	//	setProcessing(false);
		return true;
	}	//	unlockIt
	
	
	public boolean invalidateIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("invalidateIt - " + toString());
		return false;
	}	//	invalidateIt
	
	
	public String prepareIt()
	{
		if (log.isLoggable(Level.INFO)) log.info(toString());
		
		// Call model validators
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}
		
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), "FAA", getAD_Org_ID()); //Goodwill - new asset doctype
		
		// Goodwill - setting Create Asset checkbox
		setA_CreateAsset();

//		Asset additions should be possible with zero change to the amount, perhaps just qty or adjustment of accumulated depreciation
//		// Check AssetValueAmt != 0
//		if (getAssetValueAmt().signum() == 0) {
//			m_processMsg="@Invalid@ @AssetValueAmt@=0";
//			return DocAction.STATUS_Invalid;
//		}

		MAsset asset = null;
		if (isA_CreateAsset() && getA_Asset_ID() == 0)
		{
			asset = new MAsset(getCtx(), 0, get_TrxName());
		}
		else
		{	
			asset = getA_Asset(true);
		}
		
		MDepreciationWorkfile assetwk = MDepreciationWorkfile.get(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		
		// Goodwill - Check asset disposal status
		if (MAsset.A_ASSET_STATUS_Disposed.equals(asset.getA_Asset_Status()))
		{
			m_processMsg = "Asset aldready disposed";
			return DocAction.STATUS_Invalid;
		}// End - check asset disposal status
		
		// Goodwill - Check if asset already depreciated 
		if (!MAsset.A_ASSET_STATUS_New.equals(asset.getA_Asset_Status()) && assetwk != null 
				&& assetwk.getDateAcct() != null && assetwk.isDepreciated(getDateAcct()))
		{
			m_processMsg = "Asset already depreciated for this period";
			return DocAction.STATUS_Invalid;
		}// End - check asset depreciated status
		
		// Goodwill - Validation on Asset Addition Date
		if (asset.getA_Asset_CreateDate() != null && getDateDoc().before(asset.getA_Asset_CreateDate()))
		{
			throw new AssetCheckDocumentException("Document is date older than Asset Create Date");
		}
		else if (asset.getAssetServiceDate() != null && getDateDoc().before(asset.getAssetServiceDate()))
		{
			throw new AssetCheckDocumentException("Document is date older than Asset Service Date");
		}
				
		// If new assets (not renewals) must have nonzero values
		if (isA_CreateAsset() && hasZeroValues())
		{
			throw new AssetException("New document must have non-zero values");
		}
		
		// Goodwill - can add asset value without adding asset usable life
		if (!isA_CreateAsset() && getDeltaUseLifeYears() < 0)
		{
			throw new AssetException("Delta Use Life Years cannot be negative values");
		}
		
		// Goodwill - Validation on Depreciated Asset
		if (MAsset.A_ASSET_STATUS_Depreciated.equals(asset.getA_Asset_Status()))
		{
			throw new AssetException("Asset is fully depreciated");
		}
		
		// Only New assets can be activated
		if (isA_CreateAsset() && !MAsset.A_ASSET_STATUS_New.equals(asset.getA_Asset_Status()))
		{
			throw new AssetException("Only new assets can be activated");
		}
		//
		
		// Goodwill
		// Validate Source - Invoice
		if (A_SOURCETYPE_Invoice.equals(getA_SourceType()))
		{
			int C_Invoice_ID = getC_Invoice_ID();
			MInvoice invoice = new MInvoice(getCtx(), C_Invoice_ID, get_TrxName());
			if (MInvoice.DOCSTATUS_Voided.equals(invoice.getDocStatus()))
			{
				throw new AssetException("You cannot add asset from voided document(s)");
			}
		}
		// End Validate
		
		// Validate Source - Project
		if (A_SOURCETYPE_Project.equals(getA_SourceType()))
		{
			if (getC_Project_ID() <= 0)
			{
				throw new FillMandatoryException(COLUMNNAME_C_Project_ID);
			}
			final String whereClause = COLUMNNAME_C_Project_ID+"=?"
								+" AND DocStatus IN ('IP','CO','CL')"
								+" AND "+COLUMNNAME_A_Asset_Addition_ID+"<>?";
			List<MAssetAddition> list = new Query(getCtx(), Table_Name, whereClause, get_TrxName())
					.setParameters(new Object[]{getC_Project_ID(), get_ID()})
					.list();
			if (list.size() > 0)
			{
				StringBuilder sb = new StringBuilder("You can not create project for this asset,"
									+" Project already has assets. View: ");
				for (MAssetAddition aa : list)
				{
					sb.append(aa.getDocumentInfo()).append("; ");
				}
				throw new AssetException(sb.toString());
			}
		}
		
		// Call model validators
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}

		//	Done
		m_justPrepared = true;
		if (!DOCACTION_Complete.equals(getDocAction()))
			setDocAction(DOCACTION_Complete);
		return DocAction.STATUS_InProgress;
	}	//	prepareIt
	
	
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
		//	User Validation
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null) {
			return DocAction.STATUS_Invalid;
		}
		
		//	Implicit Approval
		if (!isApproved())
			approveIt();
		log.info(toString());
		//

		//loading asset
		MAsset asset = getA_Asset(!m_justPrepared); // requery if not just prepared
		log.fine("asset=" + asset);
		// Goodwill
		if (asset == null)
		{
			m_processMsg = "Asset not created/selected";
			return DocAction.STATUS_Invalid;
		}

		// Moved this above changes to the document in case of error
		// Do we have depreciation entries that are not processed and before this date:
		// Throw error if there are unprocessed records prior to the addition
		// This is important as changes to the balances should be made to the 
		// depreciated amounts.  Leave the document in the in progress state.
		// Get/Create Asset Workfile: creates a new file for this asset
		// TODO - Multi-schema? Or make the asset balances a sum to date
		MDepreciationWorkfile assetwk = MDepreciationWorkfile.get(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		if (assetwk == null)
		{
			assetwk = new MDepreciationWorkfile(asset, getPostingType(), null);
		}
		log.fine("workfile: " + assetwk);
		// Test of unprocessed depreciation entries
		try 
		{
			// Throws exception
			MDepreciationExp.checkExistsNotProcessedEntries(assetwk.getCtx(), assetwk.getA_Asset_ID(), getDateAcct(), assetwk.getPostingType(), assetwk.get_TrxName());
		}
		catch (AssetException e)
		{
			m_processMsg = e.getMessage();
			return DocAction.STATUS_InProgress;
		}

		// Creating/Updating asset product
		// TODO - this is about storage and should use the storage engine
		// The entire table AssetProduct can be replaced by MStorage with the link\
		// to the asset table through the product ID
		updateA_Asset_Product(false);

		if (this.isA_CreateAsset())
		{
			// Check/Create ASI:  the product will be the asset product in this case
			// set by the beforeSave()
			// TODO This doesn't make sense if this is a collective asset but the
			// product has an ASI - like a serial number.
			checkCreateASI();	
			asset.setM_AttributeSetInstance_ID(getM_AttributeSetInstance_ID());
		}
		// end Goodwill
		
//		if (this.getA_CapvsExp().equals(A_CAPVSEXP_Capital)) { 
//			//@win modification to asset value and use life should be restricted to Capital 
//			//@McKayERP no, there can be included costs added to the asset value and that can extend its
//			// life. These possibilities need to be allowed.
//		}

		if (this.getA_Salvage_Value().signum() > 0) {
			assetwk.setA_Salvage_Value(this.getA_Salvage_Value());
		}
		assetwk.adjustCost(getAssetValueAmt(), getA_QTY_Current(), isA_CreateAsset()); // reset if isA_CreateAsset
		assetwk.adjustUseLife(getDeltaUseLifeYears(), getDeltaUseLifeYears_F(), isA_CreateAsset()); // reset if isA_CreateAsset
		assetwk.setDateAcct(getDateAcct());
		assetwk.setProcessed(true);
		assetwk.saveEx();
			
		// Adding input to Asset History tab
		MAssetChange.createAddition(this, assetwk);
		
		// Setting locator if is CreateAsset
		if (isA_CreateAsset() && getM_Locator_ID() > 0)
		{
			// TODO - shouldn't the asset locator be a template value?
			// In the case of collective assets, there could be multiple
			// locators
			asset.setM_Locator_ID(getM_Locator_ID());
		}
		
		// Changing asset status to Activated or Depreciated
		if (isA_CreateAsset())
		{
			// Can't use document date if the asset is being added mid life.
			if (asset.getAssetServiceDate() == null)
			{
				asset.setAssetServiceDate(getDateDoc());				
			}
			asset.changeStatus(MAsset.A_ASSET_STATUS_Activated, getDateAcct());
			asset.setA_QTY_Original(getA_QTY_Current()); //Goodwill - first qty entered
		}
		asset.saveEx();
		
		//@win set initial depreciation period = 1 , m
		if (isA_CreateAsset() && !isA_Accumulated_Depr_Adjust())
		{
			assetwk.setA_Current_Period(1);
			assetwk.saveEx();
		}//
		
		if (isA_CreateAsset() && isA_Accumulated_Depr_Adjust())
		{
			assetwk.setA_Current_Period(getA_Period_Start());
			assetwk.setA_Accumulated_Depr(getA_Accumulated_Depr());
			assetwk.setA_Accumulated_Depr_F(getA_Accumulated_Depr_F());
			assetwk.saveEx();
		}
		
		// Accumulated depreciation (if any):
		/*
		if (isA_Accumulated_Depr_Adjust())
		{
			Collection<MDepreciationExp> expenses = MDepreciationExp.createDepreciation(assetwk,
														1, // PeriodNo
														getDateAcct(),
														getA_Accumulated_Depr(), getA_Accumulated_Depr_F(),
														null,	// Accum Amt
														null,	// Accum Amt (F)
														null,	// Help
														null);
			for (MDepreciationExp exp : expenses)
			{
				exp.setA_Asset_Addition_ID(getA_Asset_Addition_ID());
				exp.process();
			}
			
			if (isA_CreateAsset() && isA_Accumulated_Depr_Adjust())
			{
				assetwk.setA_Current_Period(getA_Period_Start());
				assetwk.saveEx();
			}
		}
		*/
		
		// Rebuild depreciation:
		assetwk.buildDepreciation();
		
		//
		updateSourceDocument(false);
		
		// finish
		setProcessed(true);
		setDocAction(DOCACTION_Close);
		//
		//	User Validation
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (m_processMsg != null) {
			return DocAction.STATUS_Invalid;
		}
		//
		return DocAction.STATUS_Completed;
	}	//	completeIt
	
	
	public boolean voidIt()
	{
		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;
		
		// Goodwill - check incomplete doc status
		if (DOCSTATUS_Drafted.equals(getDocStatus())
			|| DOCSTATUS_Invalid.equals(getDocStatus())
			|| DOCSTATUS_InProgress.equals(getDocStatus())
			|| DOCSTATUS_Approved.equals(getDocStatus())
			|| DOCSTATUS_NotApproved.equals(getDocStatus()))
		{
			setA_CreateAsset(false);
		}
		else
		{
			reverseIt(false);
		}

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
	
	private void reverseIt(boolean isReActivate)
	{
		if (DOCSTATUS_Closed.equals(getDocStatus())
				|| DOCSTATUS_Reversed.equals(getDocStatus())
				|| DOCSTATUS_Voided.equals(getDocStatus()))
		{
			setDocAction(DOCACTION_None);
			throw new AssetException("Document Closed: " + getDocStatus());
		}
		
		// Goodwill - Check asset disposal status
		MAsset asset = getA_Asset(true);
		if (MAsset.A_ASSET_STATUS_Disposed.equals(asset.getA_Asset_Status()))
		{
			setDocAction(DOCACTION_None);
			throw new AssetException("Asset already disposed");
		}// End - check asset disposal status
		

		// Handling Workfile
		MDepreciationWorkfile assetwk = MDepreciationWorkfile.get(getCtx(), getA_Asset_ID(), getPostingType(), get_TrxName());
		if (assetwk == null)
		{
			throw new AssetException("@NotFound@ @A_DepreciationWorkfile_ID");
		}
		
		// Goodwill: Check if there are Additions after this one
		final String additionClause = MAssetAddition.COLUMNNAME_A_Asset_Addition_ID+">?"
			+" AND "+MAssetAddition.COLUMNNAME_A_Asset_ID+"=?"
			+" AND "+MAssetAddition.COLUMNNAME_A_CapvsExp+"=?"
			+" AND "+MAssetAddition.COLUMNNAME_M_AttributeSetInstance_ID+">?"
			+" AND "+MAssetAddition.COLUMNNAME_AssetAmtEntered+">?"
			+" AND "+MAssetAddition.COLUMNNAME_DateAcct+">=?"
			//+" AND "+MAssetAddition.COLUMNNAME_DeltaUseLifeYears+">?"
			+" AND "+MAssetAddition.COLUMNNAME_Processed+"=?";
		
		List<MAssetAddition> listAddition = new Query(getCtx(), MAssetAddition.Table_Name, additionClause, get_TrxName())
				.setParameters(new Object[]{getA_Asset_Addition_ID(),getA_Asset_ID(),A_CAPVSEXP_Capital,getM_AttributeSetInstance_ID(),BigDecimal.ZERO,getDateAcct(),/*BigDecimal.ZERO,*/"Y"})
				.setOrderBy(MAssetAddition.COLUMNNAME_DateAcct+" ASC "
					+","+MAssetAddition.COLUMNNAME_A_Asset_Addition_ID+" ASC "
					+","+MAssetAddition.COLUMNNAME_M_AttributeSetInstance_ID+" ASC ")
				.list();
		// End check
		
		if (assetwk.isFullyDepreciated())
		{
			throw new AssetNotImplementedException("Unable to verify if it is fully depreciated");
		}
		
		// cannot update a previous period
		// cannot reverse already depreciated addition
		if (/*!isA_CreateAsset() &&*/ assetwk.isDepreciated(getDateAcct()))
		{
			throw new AssetAlreadyDepreciatedException();
		}
		
		// adjust the asset value
		assetwk.adjustCost(getAssetValueAmt().negate(), getA_QTY_Current().negate(), false);
		assetwk.adjustUseLife(0 - getDeltaUseLifeYears(), 0 - getDeltaUseLifeYears_F(), false);
		assetwk.saveEx();
		
		if (listAddition.size() == 0)
		{		
			/*
			// Delete Expense Entries that were created by this addition
			{
				final String whereClause = MDepreciationExp.COLUMNNAME_A_Asset_Addition_ID+"=?"
										+" AND "+MDepreciationExp.COLUMNNAME_PostingType+"=?";
				List<MDepreciationExp>
				list = new Query(getCtx(), MDepreciationExp.Table_Name, whereClause, get_TrxName())
							.setParameters(new Object[]{get_ID(), assetwk.getPostingType()})
							.setOrderBy(MDepreciationExp.COLUMNNAME_DateAcct+" DESC, "+MDepreciationExp.COLUMNNAME_A_Depreciation_Exp_ID+" DESC")
							.list();
				for (MDepreciationExp depexp: list)
				{
					depexp.deleteEx(true);
				}
			}*/
			// Update/Delete working file (after all entries were deleted)
			if (isA_CreateAsset())
			{
				assetwk.buildDepreciation();
				assetwk.deleteEx(true);
			}
			else
				assetwk.buildDepreciation();
		}
		else
		{
			assetwk.setA_Current_Period();
			assetwk.saveEx();
			// Goodwill
			assetwk.buildDepreciation();
		}
		
		// Creating/Updating asset product
		updateA_Asset_Product(true);
		
		// Change Asset Status
		if (isA_CreateAsset() && listAddition.size() == 0)
		{
			//MAsset asset = getA_Asset(true);
			asset.changeStatus(MAsset.A_ASSET_STATUS_New, getDateAcct());
			//asset.isDepreciated();
			//asset.setIsDepreciated(true);
			asset.saveEx();
						
			if (!isReActivate)
			{
				setA_CreateAsset(false); // reset flag
			}
		}
		
		MFactAcct.deleteEx(get_Table_ID(), get_ID(), get_TrxName());
    
		updateSourceDocument(true);
	} // reverseIt
	
	
	public boolean closeIt()
	{
		if (log.isLoggable(Level.INFO)) log.info("closeIt - " + toString());
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt
	
	
	public boolean reverseCorrectIt()
	{
		throw new AssetNotImplementedException("reverseCorrectIt");
	}	//	reverseCorrectionIt
	
	
	public boolean reverseAccrualIt()
	{
		throw new AssetNotImplementedException("reverseAccrualIt");
	}	//	reverseAccrualIt
	
	
	public boolean reActivateIt()
	{
		// Before
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;
		
		reverseIt(true);

		//	User Validation
		String errmsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
		if (errmsg != null) {
			m_processMsg = errmsg;
			return false;
		}
		
		// finish
		setProcessed(false);
		setDocAction(DOCACTION_Complete);
		return true;
	}	//	reActivateIt
	
	
	public String getSummary()
	{
		MAsset asset = getA_Asset(false);
		StringBuilder sb = new StringBuilder();
		sb.append("@DocumentNo@ #").append(getDocumentNo())
			.append(": @A_CreateAsset@=@").append(isA_CreateAsset() ? "Y" : "N").append("@")
		;
		if (asset != null)
		{
			sb.append(", @A_Asset_ID@=").append(asset.getName());
		}
		
		return Msg.parseTranslation(getCtx(), sb.toString());
	}	//	getSummary

	
	public String getProcessMsg()
	{
		return m_processMsg;
	}	//	getProcessMsg
	
	
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}	//	getDoc_User_ID

	
	public BigDecimal getApprovalAmt()
	{
		return getAssetValueAmt();
	}	//	getApprovalAmt
	
	
	/** Asset Cache */
	private final POCacheLocal<MAsset> m_cacheAsset = POCacheLocal.newInstance(this, MAsset.class);
	
	/**
	 * Get Asset 
	 * @param requery
	 * @return asset
	 */
	public MAsset getA_Asset(boolean requery)
	{
		return m_cacheAsset.get(requery);
	}
	
	/**
	 * Set Asset 
	 * @return asset
	 */
	private void setA_Asset(MAsset asset)
	{
		setA_Asset_ID(asset.getA_Asset_ID());
		m_cacheAsset.set(asset);
	} // setAsset
	
	
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		if(!success)
		{
			return false;
		}
		updateSourceDocument(false);
		return true;
	}	//	afterSave

	/**
	 * Update Source Document (Invoice, Project etc) Status
	 * @param isReversal is called from a reversal action (like Void, Reverse-Correct).
	 * 					We need this flag because that when we call the method from voidIt()
	 * 					the document is not marked as voided yet. Same thing applies for reverseCorrectIt too. 
	 */
	private void updateSourceDocument(final boolean isReversalParam)
	{
		boolean isReversal = isReversalParam;
		// Check if this document is reversed/voided
		String docStatus = getDocStatus();
		if (!isReversal && (DOCSTATUS_Reversed.equals(docStatus) || DOCSTATUS_Voided.equals(docStatus)))
		{
			isReversal = true;
		}
		final String sourceType = getA_SourceType();
		//
		// Invoice: mark C_InvoiceLine.A_Processed='Y' and set C_InvoiceLine.A_Asset_ID
		if (A_SOURCETYPE_Invoice.equals(sourceType) && isProcessed())
		{
			int C_InvoiceLine_ID = getC_InvoiceLine_ID();
			MInvoiceLine invoiceLine = new MInvoiceLine(getCtx(), C_InvoiceLine_ID, get_TrxName());
			invoiceLine.setA_Processed(!isReversal);
			//invoiceLine.setA_Asset_ID(isReversal ? 0 : getA_Asset_ID());
			invoiceLine.saveEx();
		}
		//
		// Project
		else if (A_SOURCETYPE_Project.equals(sourceType) && isProcessed())
		{
			if (isReversal)
			{
				// Project remains closed. We just void/reverse/reactivate the Addition
			}
			else
			{
				//TODO decide whether to close project first or later
				
				int project_id = getC_Project_ID();
				ProcessInfo pi = new ProcessInfo("", 0, MProject.Table_ID, project_id);
				pi.setAD_Client_ID(getAD_Client_ID());
				pi.setAD_User_ID(Env.getAD_User_ID(getCtx()));
				//
				ProjectClose proc = new ProjectClose();
				proc.startProcess(getCtx(), pi, Trx.get(get_TrxName(), false));
				if (pi.isError())
				{
					throw new AssetException(pi.getSummary());
				}
				
			}
		}
		//
		// Import
		else if (A_SOURCETYPE_Imported.equals(sourceType) && !isProcessed())
		{
			if (is_new() && getI_FixedAsset_ID() > 0)
			{
				MIFixedAsset ifa = getI_FixedAsset(false);
				if (ifa != null)
				{
					ifa.setI_IsImported(true);
					ifa.setA_Asset_ID(getA_Asset_ID());
					ifa.saveEx(get_TrxName());
				}
			}
		}
		//
		// Manual
		else if (A_SOURCETYPE_Manual.equals(sourceType) && isProcessed())
		{
		  // nothing to do
		 log.fine("Nothing to do");
		}
	}
	
	/**
	 * Check/Create ASI for Product (if any). If there is no product, no ASI will be created
	 */
	private void checkCreateASI() 
	{
		// TODO - this doesn't make any sense. Are there Lifo/Fifo rules?
		// On the addition, the product could be related to an expense, not the asset.
		MProduct product = MProduct.get(getCtx(), getM_Product_ID());
		// Check/Create ASI:
		MAttributeSetInstance asi = null;
		// TODO What if the product already has an ASI?
		if (product != null && getM_AttributeSetInstance_ID() == 0)
		{
			asi = new MAttributeSetInstance(getCtx(), 0, get_TrxName());
			asi.setAD_Org_ID(0);
			asi.setM_AttributeSet_ID(product.getM_AttributeSet_ID());
			asi.saveEx();
			setM_AttributeSetInstance_ID(asi.getM_AttributeSetInstance_ID());
		}
	}
	
	/**
	 * Creating/Updating asset product
	 * @param isReversal
	 */
	private void updateA_Asset_Product(boolean isReversal)
	{
		// Skip if no product
		if (getM_Product_ID() <= 0)
		{
			return;
		}
		//
		MAssetProduct assetProduct = MAssetProduct.getCreate(getCtx(),
										getA_Asset_ID(), getM_Product_ID(), getM_AttributeSetInstance_ID(),
										get_TrxName());
		//
		if (assetProduct.get_ID() <= 0 && isReversal)
		{
			log.warning("No Product found "+this+" [IGNORE]");
			return;
		}
		//
		BigDecimal adjQty = getA_QTY_Current();
		
		if (isReversal)
		{
			adjQty = adjQty.negate();
		}
		//
		assetProduct.addA_Qty_Current(adjQty); //Goodwill
		
		MAsset asset = getA_Asset(false);
		assetProduct.addA_Qty_Current(asset.getA_QTY_Current()); //Goodwill - Asset Product is the sum of addition product qty
		assetProduct.setM_Locator_ID(getM_Locator_ID());
		
		//Goodwill - Activated Asset must have quantity at least 1
		if (asset.getA_Asset_Status().equals(MAsset.A_ASSET_STATUS_Activated) 
				&& assetProduct.getA_QTY_Current().compareTo(BigDecimal.ZERO) <= 0
				&& !isReversal)
		{
			assetProduct.addA_Qty_Current(BigDecimal.ONE);
		}//
		
		assetProduct.setAD_Org_ID(getA_Asset().getAD_Org_ID()); 
		assetProduct.saveEx();
		if (isA_CreateAsset())
		{		
			//MAsset asset = getA_Asset(false);
			assetProduct.updateAsset(asset);
			
			// Goodwill - setting asset's quantities for the first time
			asset.setA_QTY_Current(getA_QTY_Current());
			asset.setA_QTY_Original(getA_QTY_Current());
			asset.setQty(getA_QTY_Current());
			// End - setting
			
			asset.saveEx();
		}
		else
		{
			// Goodwill - setting asset's quantities
			asset.setA_QTY_Current(assetProduct.getA_QTY_Current());
			asset.setQty(assetProduct.getA_QTY_Current());
			asset.saveEx();
			// End - setting
		}
	}
	
	public boolean hasZeroValues()
	{
		return
				getDeltaUseLifeYears() <= 0 
				//|| getDeltaUseLifeYears_F() <= 0								//commented by Goodwill
				//|| getDeltaUseLifeYears() != getDeltaUseLifeYears_F() 
				|| getAssetValueAmt().signum() <= 0
		;
	}

	
	public File createPDF ()
	{
		return null;
	}	//	createPDF
	
	
	public String getDocumentInfo()
	{
		return getDocumentNo() + " / " + getDateDoc();
	}	//	getDocumentInfo

	
	public String toString()
	{
		StringBuilder sb = new StringBuilder("@DocumentNo@: " + getDocumentNo());
		MAsset asset = getA_Asset(false);
		if(asset != null && asset.get_ID() > 0)
		{
			sb.append(", @A_Asset_ID@: ").append(asset.getName());
		}
		return sb.toString();
	}	// toString
	
	// Goodwill - check the completed addition, instead of the non-void one
	private void setA_CreateAsset()
	{
		if (DOCSTATUS_Voided.equals(getDocStatus()))
		{
			setA_CreateAsset(false);
		}
		else
		{
			
			// If the asset doesn't exist or is new (not activated), then create/activate the asset
			MAsset asset = new MAsset(getCtx(), getA_Asset_ID(), get_TrxName());
			if (asset == null || asset.getA_Asset_Status().equals(MAsset.A_ASSET_STATUS_New))
			{
				setA_CreateAsset(true);
			}
		}
			
//		Replaced the below as the logic is a bit messy.  Simply, if the asset exists, don't create it.
//			int cnt= 0;
//			if (getA_Asset_ID() > 0)
//			{
//				final String sql = "SELECT COUNT(*) FROM A_Asset_Addition WHERE A_Asset_ID=? AND A_CreateAsset='Y'"
//								//+" AND DocStatus<>'VO' AND IsActive='Y'"
//								+" AND DocStatus IN ('CO','CL') AND IsActive='Y'"      // Goodwill
//								+" AND A_Asset_Addition_ID<>?";
//				
//				cnt = DB.getSQLValueEx(null, sql, getA_Asset_ID(), getA_Asset_Addition_ID());
//			}
//			
//			MAsset asset = new MAsset(getCtx(), getA_Asset_ID(), get_TrxName());   // Goodwill
//			
//			//Goodwill - If Capital type and no asset is identified, create a new asset
//			if (A_CAPVSEXP_Capital.equals(getA_CapvsExp()) && getA_Asset_ID() == 0)
//			{
//				setA_CreateAsset(true);
//			}
//			
//			//Goodwill - If Capital type and Asset_ID exist, don't create asset
//			if (A_CAPVSEXP_Capital.equals(getA_CapvsExp()) && getA_Asset_ID() > 0 && cnt >= 1)
//				setA_CreateAsset(false);
//			
//			//Goodwill - If Expense type, don't create asset
//			if (A_CAPVSEXP_Expense.equals(getA_CapvsExp()))
//				setA_CreateAsset(false);
//			
//			if (isA_CreateAsset())
//			{
//				// A_CreateAsset='Y' must be unique
//				if (cnt >= 1)
//				{
//					setA_CreateAsset(false);
//				}
//				else if (cnt == 0)
//				{
//					// setA_CreateAsset(true);  // Already done
//					
//					// Goodwill - Check if Asset is Activated, don't Create Asset
//					if (asset.getA_Asset_Status().equals(MAsset.A_ASSET_STATUS_Activated))
////							&& !asset.getAssetActivationDate().equals(getDateDoc()))  // Why test the date doc?
//					{
//						setA_CreateAsset(false);
//					}
//				}
//			}
//			else  // A_CreateAsset == false
//			{
//				// Successful creation of Asset
//				if (cnt == 0)
//				{
//					// Goodwill - Check if Asset is Activated, don't Create Asset
//					if (!asset.getA_Asset_Status().equals(MAsset.A_ASSET_STATUS_Activated)
//							|| asset.getAssetActivationDate().equals(getDateDoc()))
//					{
//						setA_CreateAsset(true);
//					}
//				}
//			}
//		}
	}
	
	private void setC_DocType_ID() 
	{
		StringBuilder sql = new StringBuilder ("SELECT C_DocType_ID FROM C_DocType ")
			.append( "WHERE AD_Client_ID=? AND AD_Org_ID IN (0,").append( getAD_Org_ID())
			.append( ") AND DocBaseType='FAA' ")
			.append( "ORDER BY AD_Org_ID DESC, IsDefault DESC");
		int C_DocType_ID = DB.getSQLValue(null, sql.toString(), getAD_Client_ID());
		if (C_DocType_ID <= 0)
			log.severe ("No FAA found for AD_Client_ID=" + getAD_Client_ID ());
		else
		{
			if (log.isLoggable(Level.FINE)) log.fine("(PO) - " + C_DocType_ID);
			set_ValueOfColumn("C_DocType_ID", C_DocType_ID);
		}
	
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

}	//	MAssetAddition
