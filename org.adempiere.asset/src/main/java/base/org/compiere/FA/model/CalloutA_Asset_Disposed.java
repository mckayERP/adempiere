/**
 * 
 */
package org.compiere.FA.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Properties;

import org.adempiere.model.GridTabWrapper;
import org.compiere.model.CalloutEngine;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.I_A_Asset_Disposed;
import org.compiere.model.MAsset;
import org.compiere.model.MAssetDisposed;
import org.compiere.util.Env;



/**
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 */
public class CalloutA_Asset_Disposed extends CalloutEngine
{
	public String asset(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		I_A_Asset_Disposed bean = GridTabWrapper.create(mTab, I_A_Asset_Disposed.class);
		MAssetDisposed.updateFromAsset(bean);
		MAssetDisposed.setA_Disposal_Amt(bean);  // for zero gain/loss
		//
		return "";
	}

	public String date(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
		{
			return "";
		}
		String columnName = mField.getColumnName();
		//
		if (MAssetDisposed.COLUMNNAME_DateDoc.equals(columnName))
		{
			I_A_Asset_Disposed bean = GridTabWrapper.create(mTab, I_A_Asset_Disposed.class);
			Timestamp dateDoc = (Timestamp)value;
			bean.setDateAcct(dateDoc);
			bean.setA_Disposed_Date(dateDoc);
		}
		//
		return "";
	}

	public String amt(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		
		// gain/(loss) = disposal amount - (original cost delta - accumDep delta)
		// delta amounts = original amounts*(qtyDelta/qtyCurrent)
		// Gain/loss should be read only
		
		String columnName = mField.getColumnName();
		
		I_A_Asset_Disposed bean = GridTabWrapper.create(mTab, I_A_Asset_Disposed.class);
		//
		int asset_id = bean.getA_Asset_ID();
		if (asset_id <= 0)
		{
			bean.setA_Disposal_Amt(Env.ZERO);
			bean.setA_QTY_Delta(Env.ZERO);
			bean.setA_Asset_Cost_Delta(Env.ZERO);
			bean.setA_Accumulated_Depr_Delta(Env.ZERO);
			bean.setA_Disposal_GainLoss(Env.ZERO);
			return "";
		}
		
		if (MAssetDisposed.COLUMNNAME_A_Disposal_Amt.equals(columnName))
		{
			BigDecimal gainLoss = bean.getA_Disposal_Amt()
									.subtract(bean.getA_Asset_Cost_Delta())
									.subtract(bean.getA_Accumulated_Depr_Delta());
			bean.setA_Disposal_GainLoss(gainLoss);
		}
		else if (MAssetDisposed.COLUMNNAME_A_QTY_Delta.equals(columnName))
		{
			// Limited to Env.ONE by the table definition if the asset is single quantity per UOM
			// Don't need to test here
			if (bean.getA_QTY_Current().signum() <= 0)
				return "";
			
			BigDecimal ratio = ((BigDecimal) value).divide(bean.getA_QTY_Current(),12,RoundingMode.HALF_UP);
			bean.setA_Asset_Cost_Delta(bean.getA_Asset_Cost().multiply(ratio));
			bean.setA_Accumulated_Depr_Delta(bean.getA_Accumulated_Depr().multiply(ratio));
			BigDecimal gainLoss = bean.getA_Disposal_Amt()
					.subtract(bean.getA_Asset_Cost_Delta())
					.subtract(bean.getA_Accumulated_Depr_Delta());
			bean.setA_Disposal_GainLoss(gainLoss);
			
		}
		
		return "";
	}

}
