/**
 * 
 */
package org.compiere.FA.model;

import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.FillMandatoryException;
import org.compiere.acct.Fact;
import org.compiere.model.I_M_InOut;
import org.compiere.model.I_M_Product;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MAsset;
import org.compiere.model.MAssetAddition;
import org.compiere.model.MAssetDisposed;
import org.compiere.model.MAssetGroup;
import org.compiere.model.MClient;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MInvoice;
import org.compiere.model.MInvoiceLine;
import org.compiere.model.MMatchInv;
import org.compiere.model.MProduct;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.SetGetModel;
import org.compiere.model.SetGetUtil;
import org.compiere.model.X_C_InvoiceLine;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.compiere.FA.exceptions.AssetInvoiceWithMixedLines_LRO;
import org.compiere.FA.exceptions.AssetProductStockedException;



/**
 * Fixed Assets Model Validator
 * @author Teo_Sarca, SC ARHIPAC SERVICE SRL
 * @author Goodwill Consulting
 *
 */
public class ModelValidator
implements org.compiere.model.ModelValidator, org.compiere.model.FactsValidator
{
	/** Logger */
	private static CLogger log = CLogger.getCLogger(ModelValidator.class);
	/** Client */
	private int clientId = -1;

	
	public int getAD_Client_ID() {
		return clientId;
	}

	
	public void initialize(ModelValidationEngine engine, MClient client)
	{
		if (client != null)
		{
			clientId = client.getAD_Client_ID();
		}

		engine.addModelChange(MInvoiceLine.Table_Name, this);				
		engine.addModelChange(MMatchInv.Table_Name, this);
		engine.addModelChange(MAssetGroup.Table_Name, this);
		engine.addDocValidate(MInvoice.Table_Name, this);
		engine.addDocValidate(MInOut.Table_Name, this);
	}

	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID)
	{
		return null;
	}

	public String modelChange(PO po, int type) throws Exception
	{
		if (po instanceof MMatchInv // Performed in the model change since the MMatchInv does not have a "complete" process
				&& (TYPE_AFTER_NEW == type 
						|| (TYPE_AFTER_CHANGE == type && po.is_ValueChanged(MMatchInv.COLUMNNAME_Processed))))
		{
			MMatchInv mi = (MMatchInv)po;
			if (mi.isProcessed())
			{
				MInvoiceLine invoiceLine = new MInvoiceLine(mi.getCtx(), mi.getC_InvoiceLine_ID(), mi.get_TrxName());
				if (invoiceLine.isA_CreateUpdateAsset()
						&& !invoiceLine.isA_Processed() 
						/* commented by @win
						&& MAssetType.isFixedAssetGroup(mi.getCtx(), invoiceLine.getA_Asset_Group_ID())
						*/
					)
				{
					// Implies, for MatchInv, that the product is stocked
					
					int loopQty = 1; //Goodwill - Loop counter for collective asset validation
	
//  Moved to the invoice					
//					//Goodwill - If the Invoice Line is an expense type
//					if (MInvoiceLine.A_CAPVSEXP_Expense.equals(invoiceLine.getA_CapvsExp()))
//						invoiceLine.set_ValueOfColumn("IsCollectiveAsset", false);
//					
//					//Goodwill - If the Invoice Line is a capital type and with an Asset_ID
//					if (MInvoiceLine.A_CAPVSEXP_Capital.equals(invoiceLine.getA_CapvsExp())
//							&& invoiceLine.getA_Asset_ID() > 0)
//						invoiceLine.set_ValueOfColumn("IsCollectiveAsset", false);
//					
					//Goodwill - If the Invoice Line is a capital type and without Asset_ID
					if (MInvoiceLine.A_CAPVSEXP_Capital.equals(invoiceLine.getA_CapvsExp())
							&& invoiceLine.getA_Asset_ID() <= 0)
					{
						if (!invoiceLine.get_ValueAsBoolean("IsCollectiveAsset"))
							loopQty = mi.getQty().intValue();
					}
					
					//Goodwill - Loop for creating asset addition
					for (int i = 0; i < loopQty; i++)
					{
						MAssetAddition.createAsset(mi);
					}
				}
			}
		}

		// Invoice Line
		else if (po instanceof MInvoiceLine)
		{			
			MInvoiceLine il = (MInvoiceLine)po;
			beforeSave(il, type);
			afterChange(il,type);
		}
		
		// Asset Group
		else if (po.get_TableName().equals(MAssetGroup.Table_Name))
		{
			MAssetGroup assetGroup = (MAssetGroup)po;
			if (type == TYPE_NEW || type == TYPE_CHANGE) 
				beforeSave(assetGroup, type == TYPE_NEW);
			else if (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE) 
				afterSave(assetGroup, type == TYPE_AFTER_NEW);
			else if (type == TYPE_DELETE) 
				beforeDelete(assetGroup);
		}
		
		return null;
		
	}

	public String docValidate(PO po, int timing)
	{
			
		log.info(po.get_TableName() + " Timing: " + timing);
		String result = null;
		
		// TABLE C_Invoice
		String tableName = po.get_TableName();
		if(tableName.equals(MInvoice.Table_Name)){
			
//  MckayERP - Commented out to remove localization from general code 
//			// Invoice - Validate Fixed Assets Invoice (LRO)
//			if (timing==TIMING_AFTER_PREPARE)
//			{
//				MInvoice invoice = (MInvoice)po;
//				validateFixedAssetsInvoice_LRO(invoice);
//			}
			
			if(timing==TIMING_AFTER_COMPLETE){
				MInvoice mi = (MInvoice)po;
				if (mi.isSOTrx() && !mi.isReversal()) 
				{
					MInvoiceLine[] mils = mi.getLines();
					for (MInvoiceLine mil: mils) 
					{
						if (mil.isA_CreateUpdateAsset() && !mil.isA_Processed()) 
						{
							MAssetDisposed.createAssetDisposed(mil);
						}
					}
				}
				else if (!mi.isSOTrx() && !mi.isReversal())
				{
					// Vendor invoice
					MInvoiceLine[] mils = mi.getLines();
					for (MInvoiceLine mil: mils) 
					{
						if (mil.isA_CreateUpdateAsset() && !mil.isA_Processed()) 
						{
							if (mil.getM_Product_ID() > 0)
							{
								// Defer to Match Inv for stocked items
								if (mil.getM_Product().isStocked())
								{
									continue;
								}
							}
							
							// Collective assets - a collective asset is a single asset record
							// that includes a number of items, so it has a quantity.  A non-collective
							// asset has quantity 1 for each asset record.  The IsCollectiveAsset flag can be
							// be set to Yes only if an asset id is not specified and the addition is Capital.
							// In this case, a new asset will be created with the quantity on the invoice. 
							// Otherwise, with the flag set to N, a new asset will be created for each unit 
							// quantity on the invoice.  
							// If the asset ID is specified, the IsCollectiveAsset flag is ignored and the invoice
							// quantity is added to the asset quantity.
							
							int numberOfAssetsToCreate = 1;
							
							if (MInvoiceLine.A_CAPVSEXP_Capital.equals(mil.getA_CapvsExp())
									&& mil.getA_Asset_ID() <= 0)
							{
								if (!mil.get_ValueAsBoolean("IsCollectiveAsset"))  
									numberOfAssetsToCreate = mil.getQtyInvoiced().intValue();
							}
							
							//Goodwill - Loop for creating/updating asset addition
							for (int i = 0; i < numberOfAssetsToCreate; i++)
							{
								MAssetAddition assetAddition = MAssetAddition.createAssetAddition(mil);
								if (assetAddition != null)
								{
									// Complete the addition.
									if (!assetAddition.processIt(MAssetAddition.ACTION_Complete))
									{
										// Failure likely due to incomplete depreciation processing.  The depreciation needs
										// to be up to date.
										String errorMsg = assetAddition.getProcessMsg();
										log.warning("Asset Addition Process Failed: " + assetAddition + " - " + errorMsg);
										throw new IllegalStateException("Asset Addition Process Failed: " + assetAddition + " - " + errorMsg);
	
									}
								}
							}
						}
					}
				}
			} //end MInvoice TIMING_AFTER_COMPLETE
			
			if(timing==TIMING_AFTER_VOID)
			{
				MInvoice invoice = (MInvoice)po;
				String error = afterVoid(invoice);
				if (error != null)
					return error;
			}
			
			if(timing==TIMING_BEFORE_REVERSECORRECT)
			{
				MInvoice invoice = (MInvoice)po;
				String error = beforeReverseCorrect(invoice);
				if (error != null)
					return error;
			}
		}

		if (tableName.equals(MInOut.Table_Name))
		{
			if (timing == TIMING_AFTER_COMPLETE)
			{
				MInOut inOut = (MInOut) po;
				for (MInOutLine inOutLine :  inOut.getLines())
				{
					MProduct product = inOutLine.getProduct();
						//	Create Asset for SO
					if (product != null
							&& inOut.isSOTrx()
							&& product.isAssetProduct()
							&& !product.getM_Product_Category().getA_Asset_Group().isFixedAsset()
							&& inOutLine.getMovementQty().signum() > 0
							&& !inOut.isReversal()) {
						log.fine("Asset");
						//info.append("@A_Asset_ID@: ");
						int noAssets = inOutLine.getMovementQty().intValue();
						if (!product.isOneAssetPerUOM())
							noAssets = 1;
						for (int i = 0; i < noAssets; i++) {
							//if (i > 0)
							//	info.append(" - ");
							int deliveryCount = i + 1;
							if (!product.isOneAssetPerUOM())
								deliveryCount = 0;
							MAsset asset = new MAsset(inOut, inOutLine, deliveryCount);
							if (!asset.save(inOut.get_TrxName())) {
								//m_processMsg = "Could not create Asset";
								//return DocAction.STATUS_Invalid;
								throw new IllegalStateException("Could not create Asset");
							}
							//info.append(asset.getValue());
						}
					}
				}	//	Asset

			}
			if ( timing == TIMING_AFTER_REVERSECORRECT)
			{
				MInOut inOut = (MInOut) po;
				I_M_InOut inOutReversal = inOut.getReversal();
				for (MInOutLine inOutLine :  inOut.getLines()) {
					//	De-Activate Asset
					MAsset asset = MAsset.getFromShipment(inOut.getCtx(), inOutLine.getM_InOutLine_ID(), inOut.get_TrxName());
					if (asset != null) {
						asset.setIsActive(false);
						asset.setDescription(asset.getDescription() + " (" + inOutReversal.getDocumentNo() + " #" + inOutLine.getLine() + "<-)");
						asset.saveEx();
					}
				}
			}
		}
		return result;
	} // docValidate
	
	/**
	 * Model Change Invoice Line - updates the invoice header after changes in the invoice lines
	 * @param InvoiceLine 
	 * @param changeType set when called from model validator
	 */
	private boolean afterChange(MInvoiceLine invoiceLine, int changeType) 
	{
		
		// Update Invoice Header:
		if (TYPE_AFTER_NEW == changeType || TYPE_AFTER_CHANGE == changeType || TYPE_AFTER_DELETE == changeType) 
		{
			int invoiceId = invoiceLine.getC_InvoiceLine_ID();
			String sql =
				"UPDATE C_Invoice i SET IsFixedAssetInvoice"
						+"=(SELECT COALESCE(MAX(il.IsFixedAssetInvoice),'N')"
						+" FROM C_InvoiceLine il"
						+" WHERE il.C_Invoice_ID=i.C_Invoice_ID"
						+" AND il."+MInvoiceLine.COLUMNNAME_IsFixedAssetInvoice+"='N'"
						+")"
				+" WHERE C_Invoice_ID=?";
			DB.executeUpdateEx(sql, new Object[]{invoiceId},invoiceLine.get_TrxName());
			return true;
		}
		return false;
	}
	
	/**
	 * Check if is a valid fixed asset related invoice (LRO)
	 * @param invoice
	 */
	private void validateFixedAssetsInvoice_LRO(MInvoice invoice)
	{
		if (invoice.get_ValueAsBoolean("IsFixedAssetInvoice"))
		{
			boolean hasFixedAssetLines = false;
			boolean hasNormalLines = false;
			for (MInvoiceLine line : invoice.getLines())
			{
				if (line.get_ValueAsBoolean("IsFixedAssetInvoice"))
				{
					hasFixedAssetLines = true;
				}
				else if (line.getM_Product_ID() > 0)
				{
					MProduct product = MProduct.get(line.getCtx(), line.getM_Product_ID());
					if (product.isItem())
					{
						// Only items are forbiden for FA invoices because in Romania these should use
						// V_Liability vendor account and not V_Liability_FixedAssets vendor account
						hasNormalLines = true;
					}
				}
				//
				// No mixed lines are allowed
				if (hasFixedAssetLines && hasNormalLines)
				{
					throw new AssetInvoiceWithMixedLines_LRO();
				}
			}
		}
	}
	
	/**
	 *  Before Save Asset Group
	 *  @param assetGroup
	 *  @param newRecord
     *	@return error message or null
     *	@exception Exception if the recipient wishes the change to be not accept.
	 */
	private String beforeSave(MAssetGroup assetGroup, boolean newRecord) throws Exception
	{		
		if (assetGroup.is_ValueChanged("IsDefault"))
		{
			int no = DB.getSQLValue(assetGroup.get_TrxName(),
					"SELECT count(*) FROM A_Asset_Group WHERE IsActive='Y' AND IsDefault='Y' AND Ad_Client_ID=? AND Ad_Org_ID=?",
					assetGroup.getAD_Client_ID(),assetGroup.getAD_Org_ID());
			
			if (no == 1 && !assetGroup.isDefault() && !newRecord)
			{
				throw new IllegalStateException("One active Default is expected");		
			}
									
		}
		return null;
	} //beforeSave
	
	/**
	 *  After Save Asset Group
	 *  @param assetGroup
	 *  @param newRecord
     *	@return error message or null
     *	@exception Exception if the recipient wishes the change to be not accept.
	 */
	private String afterSave(MAssetGroup assetGroup, boolean newRecord) throws Exception
	{		
		if ( assetGroup.isDefault()) // now current group
		{
			DB.executeUpdateEx("UPDATE A_Asset_Group SET IsDefault='N' WHERE IsActive='Y' AND Ad_Client_ID=? AND Ad_Org_ID=? AND A_Asset_Group_ID !=?", 
				new	Object[]{assetGroup.getAD_Client_ID(),assetGroup.getAD_Org_ID(),assetGroup.getA_Asset_Group_ID()},assetGroup.get_TrxName());
		}
		return null;
	} //afterSave


	/**
	 * Before Delete
 	 * @param assetGroup
	 * @return
	 * @throws Exception
	 */
	private String beforeDelete(MAssetGroup assetGroup) throws Exception
	{
		int no = DB.getSQLValue(assetGroup.get_TrxName(),
				"SELECT count(*) FROM A_Asset_Group WHERE IsActive='Y' AND IsDefault='Y' AND Ad_Client_ID=? AND Ad_Org_ID=? AND A_Asset_Group_ID=? ",
				assetGroup.getAD_Client_ID(), assetGroup.getAD_Org_ID(), assetGroup.getA_Asset_Group_ID());
			
		if (no == 1)
		{
			throw new IllegalStateException("One active Default is expected");		
		}
		return null;
	} //beforeDelete
	
	/**
	 * Ensure the invoice lines that impact assets are setup correctly.
	 * This code is in the asset model validator due to dependency issues
	 * with the Fixed Asset module.
	 * @param invoiceLine
	 * @param type
	 * @return true if applied to the invoice line.
	 */
	private boolean beforeSave(MInvoiceLine invoiceLine, int type)
	{
		
		if (type != TYPE_CHANGE && type != TYPE_NEW)
			return false;
		
		MProduct product = null;
		MAsset asset = null;
		
		// If the product is asset related, force the Create Asset flag
		if (invoiceLine.getM_Product_ID() > 0) {
			product = (MProduct) invoiceLine.getM_Product();
			if (product != null && product.isAssetProduct()) 
			{
				invoiceLine.setA_Asset_Group_ID(product.getA_Asset_Group_ID());
				invoiceLine.setA_CreateUpdateAsset(true);
			}
		}
		
		// If the Create Asset flag is not set, ignore the rest.
		if (!invoiceLine.isA_CreateUpdateAsset())  
		{
			invoiceLine.setIsFixedAssetInvoice(false);
			invoiceLine.setA_Asset_Group_ID(0);
		}
		else
		{
			// We're dealing with a fixed asset, so set the fixed asset flag
			invoiceLine.setIsFixedAssetInvoice(true);
			
			// If an asset is identified, turn off the collective asset flag and check that 
			// the asset group matches the asset
			if (invoiceLine.getA_Asset_ID() > 0)
			{
				asset = (MAsset) invoiceLine.getA_Asset();
				if (asset != null)
				{
					// Match that asset group to asset ID if required
					invoiceLine.setA_Asset_Group_ID(asset.getA_Asset_Group_ID());	
				}
			}
			
			// Creating or updating and asset value.  There are two types: Capital and Expense
			// A Capital type is used when purchasing or disposing of an asset.  The expense type 
			// is used for items included in cost of an existing asset.  Capital types will generally
			// add quantity and cost to an asset or add new assets. Expenses types will only change the 
			// asset cost.  Neither type will affect the accumulated depreciation.
			
			if (X_C_InvoiceLine.A_CAPVSEXP_Capital.equals(invoiceLine.getA_CapvsExp()))
			{
				// If this is a Capital Expense with an asset product identified, ensure the product is linked to an
				// asset group.  
				if (product != null && product.isAssetProduct())
				{
					invoiceLine.setA_Asset_Group_ID(product.getA_Asset_Group_ID());
				}
				
				if (asset != null)
				{
					// Match that asset group to asset ID if required
					if (invoiceLine.getA_Asset_Group_ID() <= 0)
					{
						invoiceLine.setA_Asset_Group_ID(asset.getA_Asset_Group_ID());
					}
					else if (invoiceLine.getA_Asset_Group_ID() != asset.getA_Asset_Group_ID())
					{
						throw new AdempiereException(Msg.translate(invoiceLine.getCtx(), "Asset Group on Invoice Line is different from Asset Group on Asset"));
					}
					
					// Match the asset and product for capital types, if there is a product
					if (product != null && product.isAssetProduct() && invoiceLine.getM_Product_ID() != asset.getM_Product_ID())
					{
						throw new AdempiereException(Msg.translate(invoiceLine.getCtx(), "Product on Invoice Line is different from Asset Product"));
					}
					
					// If there is an asset identified, turn off the Collective Asset flag
					invoiceLine.set_ValueOfColumn("IsCollectiveAsset", false);

				}
			}
		
			//Expense Asset_ID check - Expense types are related to a specific asset so the Asset ID must be set
			if (X_C_InvoiceLine.A_CAPVSEXP_Expense.equals(invoiceLine.getA_CapvsExp()))
			{
				if (invoiceLine.getA_Asset_ID() <= 0)
				{
					throw new FillMandatoryException(MInvoiceLine.COLUMNNAME_A_Asset_ID);
				}
								
				// Expense types are not collective
				invoiceLine.set_ValueOfColumn("IsCollectiveAsset", false);
			}
			
		}
		return true;
	}
	
	/**
	 * 	Before Reverse Correct Invoice
	 * 	@param invoice
	 *	@return error message or null 
	 */
	private String beforeReverseCorrect(MInvoice invoice)
	{
//		// Goodwill - Check Asset Addition's status
//		if (invoice.get_ValueAsBoolean("IsFixedAssetInvoice"))
//		{
//			final String sql = "SELECT A_Asset_Addition_ID "
//					+"FROM A_Asset_Addition WHERE C_Invoice_ID=? ";
//			int A_Asset_Addition_ID = DB.getSQLValueEx(invoice.get_TrxName(), sql, invoice.get_ID());
//			MAssetAddition assetAdd = new MAssetAddition(invoice.getCtx(), A_Asset_Addition_ID, invoice.get_TrxName());
//			if (assetAdd.getDocStatus().equals(MAssetAddition.DOCSTATUS_Completed)
//				|| assetAdd.getDocStatus().equals(MAssetAddition.DOCSTATUS_Closed))
//			{
//				return "Can't Void or Reverse Invoice with Completed Asset Addition";
//			}
//		}
//		// End Check
		
		// mckayERP Void associated asset documents is possible
		String where = "C_Invoice_ID=? ";
		
		List<MAssetAddition> assetAdds = new Query(invoice.getCtx(), MAssetAddition.Table_Name, where, invoice.get_TrxName())
										.setParameters(invoice.get_ID())
										.list();
		
		for (MAssetAddition assetAdd : assetAdds)
		{
			// Delete asset addition if it's not completed
			if (MAssetAddition.DOCSTATUS_Drafted.equals(assetAdd.getDocStatus())
				|| MAssetAddition.DOCSTATUS_InProgress.equals(assetAdd.getDocStatus())
				|| MAssetAddition.DOCSTATUS_Invalid.equals(assetAdd.getDocStatus())
				|| MAssetAddition.DOCSTATUS_Approved.equals(assetAdd.getDocStatus())
				|| MAssetAddition.DOCSTATUS_NotApproved.equals(assetAdd.getDocStatus()))
			{
				assetAdd.deleteEx(true);
			}
			else if (MAssetAddition.DOCSTATUS_Completed.equals(assetAdd.getDocStatus()))
			{
				if (!assetAdd.processIt(MAssetAddition.DOCACTION_Void))
				{
					log.warning("Asset Addition Process Failed: " + assetAdd + " - " + assetAdd.getProcessMsg());
					throw new IllegalStateException("Asset Addition Process Failed: " + assetAdd + " - " + assetAdd.getProcessMsg());
				}
				assetAdd.saveEx();
			}
			else
			{
				// harmless - leave it as is.
			}
		}
		
		List<MAssetDisposed> assetDis = new Query(invoice.getCtx(), MAssetDisposed.Table_Name, where, invoice.get_TrxName())
										.setParameters(invoice.get_ID())
										.list();
		
		for (MAssetDisposed assetDi : assetDis)
		{
			// Delete asset disposal if possible
			if (MAssetDisposed.DOCSTATUS_Drafted.equals(assetDi.getDocStatus())
				|| MAssetDisposed.DOCSTATUS_InProgress.equals(assetDi.getDocStatus())
				|| MAssetDisposed.DOCSTATUS_Invalid.equals(assetDi.getDocStatus())
				|| MAssetDisposed.DOCSTATUS_Approved.equals(assetDi.getDocStatus())
				|| MAssetDisposed.DOCSTATUS_NotApproved.equals(assetDi.getDocStatus()))
			{
				
				assetDi.deleteEx(true);
				
			}
			else if (MAssetDisposed.DOCSTATUS_Completed.equals(assetDi.getDocStatus()))
			{					
				if (!assetDi.processIt(MAssetDisposed.DOCACTION_Void))
				{
					log.warning("Asset Disposal Process Failed: " + assetDi + " - " + assetDi.getProcessMsg());
					throw new IllegalStateException("Asset Addition Process Failed: " + assetDi + " - " + assetDi.getProcessMsg());
				}
				assetDi.saveEx();
			}
			else
			{
				// harmless - leave it as is.
			}
		}
		
		return null;
	}	//	beforeReverseCorrect	
	
	/**
	 *  After Void Invoice
	 *  @param invoice
     *	@return error message or null
	 */
	private String afterVoid(MInvoice invoice)
	{
		// Goodwill - check if invoice is for fixed asset
		if (invoice.get_ValueAsBoolean("IsFixedAssetInvoice"))
		{
			// Void associated documents
			String where = "C_Invoice_ID=? ";
			
			List<MAssetAddition> assetAdds = new Query(invoice.getCtx(), MAssetAddition.Table_Name, where, invoice.get_TrxName())
											.setParameters(invoice.get_ID())
											.list();
			
			for (MAssetAddition assetAdd : assetAdds)
			{
				// Delete asset addition if it's not completed
				if (MAssetAddition.DOCSTATUS_Drafted.equals(assetAdd.getDocStatus())
					|| MAssetAddition.DOCSTATUS_InProgress.equals(assetAdd.getDocStatus())
					|| MAssetAddition.DOCSTATUS_Invalid.equals(assetAdd.getDocStatus())
					|| MAssetAddition.DOCSTATUS_Approved.equals(assetAdd.getDocStatus())
					|| MAssetAddition.DOCSTATUS_NotApproved.equals(assetAdd.getDocStatus()))
				{
					assetAdd.deleteEx(true);
				}
				else if (MAssetAddition.DOCSTATUS_Completed.equals(assetAdd.getDocStatus()))
				{
					if (!assetAdd.processIt(MAssetAddition.DOCACTION_Void))
					{
						log.warning("Asset Addition Process Failed: " + assetAdd + " - " + assetAdd.getProcessMsg());
						throw new IllegalStateException("Asset Addition Process Failed: " + assetAdd + " - " + assetAdd.getProcessMsg());
					}
					assetAdd.saveEx();
				}
				else
				{
					// harmless - leave it as is.
				}
			}
			
			List<MAssetDisposed> assetDis = new Query(invoice.getCtx(), MAssetDisposed.Table_Name, where, invoice.get_TrxName())
											.setParameters(invoice.get_ID())
											.list();
			
			for (MAssetDisposed assetDi : assetDis)
			{
				// Delete asset disposal if possible
				if (MAssetDisposed.DOCSTATUS_Drafted.equals(assetDi.getDocStatus())
					|| MAssetDisposed.DOCSTATUS_InProgress.equals(assetDi.getDocStatus())
					|| MAssetDisposed.DOCSTATUS_Invalid.equals(assetDi.getDocStatus())
					|| MAssetDisposed.DOCSTATUS_Approved.equals(assetDi.getDocStatus())
					|| MAssetDisposed.DOCSTATUS_NotApproved.equals(assetDi.getDocStatus()))
				{
					
					assetDi.deleteEx(true);
					
				}
				else if (MAssetDisposed.DOCSTATUS_Completed.equals(assetDi.getDocStatus()))
				{					
					if (!assetDi.processIt(MAssetDisposed.DOCACTION_Void))
					{
						log.warning("Asset Disposal Process Failed: " + assetDi + " - " + assetDi.getProcessMsg());
						throw new IllegalStateException("Asset Addition Process Failed: " + assetDi + " - " + assetDi.getProcessMsg());
					}
					assetDi.saveEx();
				}
				else
				{
					// harmless - leave it as is.
				}
			}
			
		}		
		return null; 
	}	//	afterVoid

	
	public String factsValidate(MAcctSchema schema, List<Fact> facts, PO po) {
		// TODO: implement it
		return null;
	}
}
