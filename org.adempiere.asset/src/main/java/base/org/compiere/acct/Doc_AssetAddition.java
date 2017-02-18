package org.compiere.acct;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.compiere.model.I_C_Project;
import org.compiere.model.I_C_Project_Acct;
import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAssetAcct;
import org.compiere.model.MAssetAddition;
import org.compiere.model.MCharge;
import org.compiere.model.MDocType;
import org.compiere.model.ProductCost;
import org.compiere.model.X_C_Project_Acct;
import org.compiere.util.DB;
import org.compiere.util.Env;


/**
 * @author Teo_Sarca, SC ARHIPAC SERVICE SRL
 * 
 * 
 */
public class Doc_AssetAddition extends Doc
{
	
	public Doc_AssetAddition (MAcctSchema[] as, ResultSet rs, String trxName)
	{
		super(as, MAssetAddition.class, rs, MDocType.DOCBASETYPE_FixedAssetsAddition, trxName);
	}

	
	protected String loadDocumentDetails()
	{
		return null;
	}

	
	public BigDecimal getBalance()
	{
		return Env.ZERO;
	}

	/**
	 * Produce inregistrarea:
	 * <pre>
	 *	20.., 21..[A_Asset_Acct]			=	23..[P_Asset_Acct/Project Acct]
	 * </pre>
	 */
	
	public ArrayList<Fact> createFacts(MAcctSchema as)
	{
		
		MAssetAddition assetAdd = getAssetAddition();
		ArrayList<Fact> facts = new ArrayList<Fact>();
		Fact fact = new Fact(this, as, assetAdd.getPostingType());
		facts.add(fact);
		//
		if (MAssetAddition.A_SOURCETYPE_Imported.equals(assetAdd.getA_SourceType())
//  @MckayERP - Not sure why an expense asset addition should have no facts.  Need to increase the asset value (original cost)
//  This is a key method of including items in the cost of the asset.
//				|| MAssetAddition.A_CAPVSEXP_Expense.equals(assetAdd.getA_CapvsExp()) //@win prevent create journal if expense addition
			)
		{
			// no accounting if is imported record
			return facts;
		}

		
		if (MAssetAddition.A_CAPVSEXP_Capital.equals(assetAdd.getA_CapvsExp()))
		{
			// For capital expense, need an asset product
			//    debit the asset "Asset" account
			//    credit accumulated depreciation
			//    credit product asset acct with the difference
	
			// Get the accounts involved
			MAccount dr_assetAcct = getA_Asset_Acct(as);
			MAccount cr_productAssetAcct = getP_Asset_Acct(as);
			MAccount cr_accDepAcct = getA_Accumdepreciation_Acct(as);
	
			//
			BigDecimal dr_assetValueAmt = assetAdd.getAssetValueAmt();
			BigDecimal cr_accDepAmt = assetAdd.getA_Accumulated_Depr();
			BigDecimal cr_productAssetAmt = dr_assetValueAmt.subtract(cr_accDepAmt);
			
			fact.createLine(null, dr_assetAcct, as.getC_Currency_ID(), dr_assetValueAmt, null);
			fact.createLine(null, cr_accDepAcct, as.getC_Currency_ID(), null, cr_accDepAmt);
			FactLine prodLine = fact.createLine(null, cr_productAssetAcct, as.getC_Currency_ID(), null, cr_productAssetAmt);
			
			// Set BPartner and C_Project dimension for "Imobilizari in curs / Property Being"
			final int invoiceBP_ID = getInvoicePartner_ID();
			final int invoiceProject_ID = getInvoiceProject_ID();
			if (invoiceBP_ID > 0)
			{
				prodLine.setC_BPartner_ID(invoiceBP_ID);
			}
			if (invoiceProject_ID >0)
			{
				 prodLine.setC_Project_ID(invoiceProject_ID);
			}
	
		}
		else if (MAssetAddition.A_CAPVSEXP_Expense.equals(assetAdd.getA_CapvsExp()))
		{
			// For expenses or items included in cost, there is no change to accumulated depreciation.
			// Instead the asset's "Asset" account is debited and the invoiced expense account is 
			// credited.
			//
			// The expense account could be the product expense account or a charge as determined by 
			// source document.  The Asset Addition product/charge fields should have the correct 
			// product or charge.
			
			// Get the accounts involved
			
			MAccount dr_assetAcct = getA_Asset_Acct(as);
			MAccount cr_sourceExpenseAcct = null;
			if (getM_Product_ID() == 0 && getC_Charge_ID() != 0)
			{
				//	Charge Account
				BigDecimal amt = new BigDecimal (+1);				//	Expense (+)
				cr_sourceExpenseAcct = getChargeAccount(as, amt);
			}
			else
			{
				//	Product Account
				cr_sourceExpenseAcct = new ProductCost (Env.getCtx(), getM_Product_ID(), assetAdd.getM_AttributeSetInstance_ID(), p_po.get_TrxName())
				    						.getAccount (ProductCost.ACCTTYPE_P_Expense, as, getAD_Org_ID());
				
			}
			
			//
			BigDecimal dr_assetValueAmt = assetAdd.getAssetValueAmt();
			BigDecimal cr_expenseAmt = dr_assetValueAmt;
			
			FactLine assetLine = fact.createLine(null, dr_assetAcct, as.getC_Currency_ID(), dr_assetValueAmt, null);			
			FactLine expenseLine = fact.createLine(null, cr_sourceExpenseAcct, as.getC_Currency_ID(), null, cr_expenseAmt);
			
			//Set the products properly
			assetLine.setM_Product_ID(assetAdd.getA_Asset().getM_Product_ID());
			expenseLine.setM_Product_ID(assetAdd.getM_Product_ID());  // Will help reconcile the product expense acct

			final int invoiceBP_ID = getInvoicePartner_ID();
			expenseLine.setC_BPartner_ID(invoiceBP_ID);
	
		}
		
		return facts;

	}
	

	private MAssetAddition getAssetAddition()
	{
		return (MAssetAddition)getPO();
	}
	
	private MAccount getP_Asset_Acct(MAcctSchema as)
	{
		MAssetAddition assetAdd = getAssetAddition();
		// Source Account
		MAccount pAssetAcct = null;
		if (MAssetAddition.A_SOURCETYPE_Project.equals(assetAdd.getA_SourceType()))
		{
			I_C_Project prj = assetAdd.getC_Project();
			return getProjectAcct(prj, as);
		}
		else if (MAssetAddition.A_SOURCETYPE_Manual.equals(assetAdd.getA_SourceType())
				&& getC_Charge_ID() > 0) // backward compatibility: only if charge defined; if not fallback to product account 
		{	
			pAssetAcct = MCharge.getAccount(getC_Charge_ID(), as, new BigDecimal(-1));
			return pAssetAcct;
		}	
		else if (MAssetAddition.A_SOURCETYPE_Invoice.equals(assetAdd.getA_SourceType())
				&& assetAdd.getC_InvoiceLine().getC_Project_ID() > 0)
		{
			I_C_Project prj = assetAdd.getC_InvoiceLine().getC_Project();
			return getProjectAcct(prj, as);
		}
		else if (MAssetAddition.A_SOURCETYPE_Invoice.equals(assetAdd.getA_SourceType())
				&& getC_Charge_ID() > 0)  
		{	
			pAssetAcct = MCharge.getAccount(getC_Charge_ID(), as, new BigDecimal(-1));
			return pAssetAcct;
		}			else
		{
			pAssetAcct = getP_Expense_Acct(assetAdd.getM_Product_ID(), as);
		}
		//
		return pAssetAcct;
	}
	
	public MAccount getP_Expense_Acct(int M_Product_ID, MAcctSchema as)
	{
		ProductCost pc = new ProductCost(getCtx(), M_Product_ID, 0, null);
		return pc.getAccount(ProductCost.ACCTTYPE_P_Expense, as);
	}
	
	
	private MAccount getProjectAcct(I_C_Project prj, MAcctSchema as)
	{
		String acctName = X_C_Project_Acct.COLUMNNAME_PJ_WIP_Acct;
		String sql = "SELECT "+acctName
					+ " FROM "+I_C_Project_Acct.Table_Name
					+ " WHERE "+I_C_Project_Acct.COLUMNNAME_C_Project_ID+"=?"
						+" AND "+I_C_Project_Acct.COLUMNNAME_C_AcctSchema_ID+"=?"
						;
		int acct_id = DB.getSQLValueEx(getTrxName(), sql, prj.getC_Project_ID(), as.get_ID());	
		return MAccount.get(getCtx(), acct_id);
	}

	private MAccount getA_Asset_Acct(MAcctSchema as)
	{
		MAssetAddition assetAdd = getAssetAddition();
		int acct_id = MAssetAcct
				.forA_Asset_ID(getCtx(), assetAdd.getA_Asset_ID(), assetAdd.getPostingType(), assetAdd.getDateAcct(), as.getC_AcctSchema_ID(), getTrxName())
				.getA_Asset_Acct();
		return MAccount.get(getCtx(), acct_id);
	}

	private MAccount getA_Accumdepreciation_Acct(MAcctSchema as)
	{
		MAssetAddition assetAdd = getAssetAddition();
		int acct_id = MAssetAcct
				.forA_Asset_ID(getCtx(), assetAdd.getA_Asset_ID(), assetAdd.getPostingType(), assetAdd.getDateAcct(), as.getC_AcctSchema_ID(), getTrxName())
				.getA_Accumdepreciation_Acct();
		return MAccount.get(getCtx(), acct_id);
	}

	public int getInvoicePartner_ID()
	{
		MAssetAddition assetAdd = getAssetAddition();
		if (MAssetAddition.A_SOURCETYPE_Invoice.equals(assetAdd.getA_SourceType())
				&& assetAdd.getC_Invoice_ID() > 0)
		{
			return assetAdd.getC_Invoice().getC_BPartner_ID();
		}
		else
		{
			return 0;
		}
	}
	public int getInvoiceProject_ID()
	{
		MAssetAddition assetAdd = getAssetAddition();
		if (MAssetAddition.A_SOURCETYPE_Invoice.equals(assetAdd.getA_SourceType())
				&& assetAdd.getC_Invoice_ID() > 0)			
		{
			return assetAdd.getC_InvoiceLine().getC_Project_ID();
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 *  Get Charge Account
	 *  @param as account schema
	 *  @param amount amount for expense(+)/revenue(-)
	 *  @return Charge Account or null
	 */
	public MAccount getChargeAccount (MAcctSchema as, BigDecimal amount)
	{
		int C_Charge_ID = getC_Charge_ID();
		if (C_Charge_ID == 0)
			return null;
		return MCharge.getAccount(C_Charge_ID, as, amount);
	}   //  getChargeAccount

}
