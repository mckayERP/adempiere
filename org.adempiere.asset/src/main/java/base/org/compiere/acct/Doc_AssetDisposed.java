package org.compiere.acct;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAssetAcct;
import org.compiere.model.MAssetDisposed;
import org.compiere.model.MDocType;
import org.compiere.util.Env;


/**
 * @author Teo_Sarca, SC ARHIPAC SERVICE SRL
 */
public class Doc_AssetDisposed extends Doc
{
	/**
	 * @param ass
	 * @param clazz
	 * @param rs
	 * @param defaultDocumentType
	 * @param trxName
	 */
	public Doc_AssetDisposed (MAcctSchema[] as, ResultSet rs, String trxName)
	{
		super(as, MAssetDisposed.class, rs, MDocType.DOCBASETYPE_FixedAssetsDisposal, trxName);
	}

	
	protected String loadDocumentDetails()
	{
		return null;
	}
	
	
	public BigDecimal getBalance()
	{
		return Env.ZERO;
	}

	
	public ArrayList<Fact> createFacts(MAcctSchema as)
	{
		MAssetDisposed assetDisp = (MAssetDisposed)getPO();
		
		ArrayList<Fact> facts = new ArrayList<Fact>();
		Fact fact = new Fact(this, as, assetDisp.getPostingType());
		facts.add(fact);
		
		// Accounting
		// 
		// Revenue/Proceeds ............... Disposal Amt
		// Accumulated Depreciation........ Acc Dep Delta
		// Loss on sale (gain < 0) ........	Loss
		//     Gain on sale (gain > 0) ....              Gain
		//     Asset cost delta ...........              Cost Delta
		//
		// Proceed of sale (revenue) moved to cash/AR via an AR Invoice
		// 
		
		// Amounts
		BigDecimal cr_assetCostDelta = assetDisp.getA_Asset_Cost_Delta().negate();
		BigDecimal dr_assetAccDepDelta = assetDisp.getA_Accumulated_Depr_Delta().negate();
		BigDecimal dr_disposalAmt = assetDisp.getA_Disposal_Amt();
		BigDecimal cr_gainLossOnDisposal = assetDisp.getA_Disposal_GainLoss(); 
		//
		if (cr_assetCostDelta.signum() != 0)
		{
			fact.createLine(null, getAccount(MAssetAcct.COLUMNNAME_A_Asset_Acct)
					, as.getC_Currency_ID()
					, Env.ZERO, cr_assetCostDelta);
		}
		
		if (dr_assetAccDepDelta.signum() != 0)
		{
			fact.createLine(null, getAccount(MAssetAcct.COLUMNNAME_A_Accumdepreciation_Acct)
					, as.getC_Currency_ID()
					, dr_assetAccDepDelta, Env.ZERO);
		}

		if (dr_disposalAmt.signum() != 0)
		{
			fact.createLine(null, getAccount(MAssetAcct.COLUMNNAME_A_Disposal_Revenue_Acct)
					, as.getC_Currency_ID()
					, dr_disposalAmt, Env.ZERO);
		}

		if (cr_gainLossOnDisposal.signum() != 0)
		{
			if (cr_gainLossOnDisposal.signum() < 0)  // Loss, negate and debit
			{
				fact.createLine(null, getAccount(MAssetAcct.COLUMNNAME_A_Disposal_Loss_Acct)
						, as.getC_Currency_ID()
						, cr_gainLossOnDisposal.negate(), Env.ZERO);
			}
			else  // Its a gain, credit
			{
				fact.createLine(null, getAccount(MAssetAcct.COLUMNNAME_A_Disposal_Gain_Acct)
						, as.getC_Currency_ID()
						, Env.ZERO, cr_gainLossOnDisposal.negate());			
			}
		}
		//
		return facts;
	}
	
	private MAccount getAccount(String accountName)
	{
		MAssetDisposed assetDisp = (MAssetDisposed)getPO();
		MAssetAcct assetAcct = MAssetAcct.forA_Asset_ID(getCtx(), assetDisp.getA_Asset_ID(), assetDisp.getPostingType(), assetDisp.getDateAcct(),null);
		int account_id = (Integer)assetAcct.get_Value(accountName);
		return MAccount.get(getCtx(), account_id);
	}

}
