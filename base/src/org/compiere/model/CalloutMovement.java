/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 * Contributor(s): Armen Rizal (armen@goodwill.co.id) Bug Fix 1564496         *
 *****************************************************************************/
package org.compiere.model;

import java.math.BigDecimal;
import java.util.Properties;

import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 *	Inventory Movement Callouts
 *	
 *  @author Jorg Janke
 *  @version $Id: CalloutMovement.java,v 1.2 2006/07/30 00:51:03 jjanke Exp $
 * 
 * @author Teo Sarca, SC ARHIPAC SERVICE SRL
 * 			<li>BF [ 1879568 ] CalloutMouvement QtyAvailable issues
 * 
 *  @author mckayERP www.mckayERP.com
 *  		<li> #286 Provide methods to treat ASI fields in a consistent manner.
 */
public class CalloutMovement extends CalloutEngine
{
	/**
	 *  Product modified
	 * 		Set Attribute Set Instance
	 *
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param GridTab     Model Tab
	 *  @param GridField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String product (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		Integer M_Product_ID = (Integer)value;
		if (M_Product_ID == null)
			M_Product_ID = Integer.valueOf(0); 
		
		MProduct m_product = MProduct.get(Env.getCtx(), M_Product_ID);

		//	Set Attribute
		Boolean outgoing = true;
		// Set the ASI field with the default ASI for this product
		setAndTestASI(ctx, WindowNo, outgoing, mTab, 
						"M_AttributeSetInstance_ID", m_product, null);		

		if (((Integer) mTab.getValue("M_Locator_ID")).equals((Integer) mTab.getValue("M_LocatorTo_ID"))) {
			// Clear the ASI To field
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", m_product, 0);
		}
		else {
			// If the locators are different, use the same ASI. Just check that it is OK
			// for the incoming (To) locator
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", m_product, (Integer) mTab.getValue("M_AttributeSetInstanceTo_ID"));
		}
		 
		checkQtyAvailable(ctx, mTab, WindowNo, M_Product_ID, null);
		return "";
	}   //  product
	
	// Begin Armen 2006/10/01
	/**
	 *  Movement Line - MovementQty modified
	 *              called from MovementQty
	 *
	 *  @param ctx      Context
	 *  @param WindowNo current Window No
	 *  @param GridTab     Model Tab
	 *  @param GridField   Model Field
	 *  @param value    The new value
	 *  @return Error message or ""
	 */
	public String qty(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		if (isCalloutActive() || value == null)
			return "";

		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, "M_Product_ID");
		checkQtyAvailable(ctx, mTab, WindowNo, M_Product_ID, (BigDecimal)value);
		//
		return "";
	} //  qty
	
	/**
	 * Movement Line - Locator modified
	 * 
	 * @param ctx      Context
	 * @param WindowNo current Window No
	 * @param GridTab     Model Tab
	 * @param GridField   Model Field
	 * @param value    The new value
	 * @return Error message or ""
	 */
	public String locator(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		if (value == null)
			return "";
		
		Integer locatorID = null;
		Integer locatorToID = null;
		if (mField.getColumnName().equals("M_Locator_ID"))  // TODO hardcoded 
			locatorID = (Integer) value;
		else
			locatorID = (Integer) mTab.getValue("M_Locator_ID");
		if (mField.getColumnName().equals("M_LocatorTo_ID"))  // TODO hardcoded 
			locatorToID = (Integer) value;
		else
			locatorToID = (Integer) mTab.getValue("M_LocatorTo_ID");

		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, "M_Product_ID");
		MProduct product = MProduct.get(ctx, M_Product_ID);

		Boolean outgoing = true;
		if (locatorID == null)
			setAndTestASI(ctx, WindowNo, outgoing, mTab, 
					"M_AttributeSetInstance_ID", product, 0);		
		if (locatorToID == null)
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", product, 0);		
			
		if (locatorID != null && locatorToID != null && locatorID.equals(locatorToID)) {
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", product, 0);		
		}
		else if (locatorID != null && locatorToID != null 
				&& !locatorID.equals(locatorToID)
				&& ((mTab.getValue("M_AttributeSetInstanceTo_ID") != null 
						&& mTab.getValue("M_AttributeSetInstance_ID") != null
						&& ((Integer) mTab.getValue("M_AttributeSetInstanceTo_ID"))
						.equals((Integer) mTab.getValue("M_AttributeSetInstance_ID")))
					|| (mTab.getValue("M_AttributeSetInstanceTo_ID") == null 
							&& mTab.getValue("M_AttributeSetInstance_ID") != null))) {
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", product, (Integer) mTab.getValue("M_AttributeSetInstance_ID"));
		}

		checkQtyAvailable(ctx, mTab, WindowNo, M_Product_ID, null);
		return "";
	}

	/**
	 * Movement Line - attributeSetInstance modified
	 * 
	 * @param ctx      Context
	 * @param WindowNo current Window No
	 * @param GridTab     Model Tab
	 * @param GridField   Model Field
	 * @param value    The new value
	 * @return Error message or ""
	 */
	public String attributeSetInstance(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value) {
		
		if (isCalloutActive())
			return "";
		
		String retValue = "";
		Integer locatorID = null;
		Integer locatorToID = null;
		locatorID = (Integer) mTab.getValue("M_Locator_ID");
		locatorToID = (Integer) mTab.getValue("M_LocatorTo_ID");

		Integer attributeSetInstance_id = null;
		Integer attributeSetInstanceTo_id = null;

		if (mField.getColumnName().equals("M_AttributeSetInstance_ID"))  // TODO hardcoded 
			attributeSetInstance_id = (Integer) value;
		else
			attributeSetInstance_id = (Integer) mTab.getValue("M_AttributeSetInstance_ID");
		if (mField.getColumnName().equals("M_AttributeSetInstanceTo_ID"))  // TODO hardcoded 
			attributeSetInstanceTo_id = (Integer) value;
		else
			attributeSetInstanceTo_id = (Integer) mTab.getValue("M_AttributeSetInstanceTo_ID");

		if ((attributeSetInstance_id == null || attributeSetInstance_id == 0) 
			&& (attributeSetInstanceTo_id == null || attributeSetInstanceTo_id == 0))
			return "";
		
		int M_Product_ID = Env.getContextAsInt(ctx, WindowNo, "M_Product_ID");
		MProduct product = MProduct.get(ctx, M_Product_ID);
		
		Boolean outgoing = true;
		if (locatorID == null)
			setAndTestASI(ctx, WindowNo, outgoing, mTab, 
					"M_AttributeSetInstance_ID", product, 0);		
		if (locatorToID == null)
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", product, 0);		
			
		if (locatorID != null && locatorToID != null 
			&& !locatorID.equals(locatorToID)
			&& ((attributeSetInstanceTo_id != null && attributeSetInstance_id != null
					&& !(attributeSetInstanceTo_id.equals(attributeSetInstance_id)))
				|| (attributeSetInstanceTo_id == null && attributeSetInstance_id != null))) {
			// if the locators are the same, make sure the ASI's match
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", product, (Integer) mTab.getValue("M_AttributeSetInstance_ID"));		
			if (mField.getColumnName().equals("M_AttributeSetInstanceTo_ID")) // advise the user
				retValue = Msg.parseTranslation(ctx, "@M_Locator_ID@ != @M_LocatorTo_ID@ and @M_AttributeSetInstance_ID@ != @M_AttributeSetInstanceTo_ID@");
		} else if (locatorID != null && locatorToID != null 
				&& locatorID.equals(locatorToID)
				&& attributeSetInstanceTo_id != null 
				&& attributeSetInstance_id != null
				&& (attributeSetInstanceTo_id.equals(attributeSetInstance_id))) {
			setAndTestASI(ctx, WindowNo, !outgoing, mTab, 
					"M_AttributeSetInstanceTo_ID", product, null);		
			retValue = Msg.parseTranslation(ctx, "@M_Locator_ID@ == @M_LocatorTo_ID@ and @M_AttributeSetInstance_ID@ == @M_AttributeSetInstanceTo_ID@");
		}
		checkQtyAvailable(ctx, mTab, WindowNo, M_Product_ID, null);
		return retValue;
	}

	/**
	 * Check available qty
	 * 
	 * @param ctx context
	 * @param mTab Model Tab
	 * @param WindowNo current Window No
	 * @param M_Product_ID product ID
	 * @param MovementQty movement qty (if null will be get from context "MovementQty")
	 */
	private void checkQtyAvailable(Properties ctx, GridTab mTab, int WindowNo, int M_Product_ID, BigDecimal MovementQty) {
		// Begin Armen 2006/10/01
		if (M_Product_ID != 0) {
			MProduct product = MProduct.get(ctx, M_Product_ID);
			if (product.isStocked()) {
				if (MovementQty == null)
					MovementQty = (BigDecimal) mTab.getValue("MovementQty");
				int M_Locator_ID = Env.getContextAsInt(ctx, WindowNo, "M_Locator_ID");
				// If no locator, don't check anything and assume is ok
				if (M_Locator_ID <= 0)
					return;
				int M_AttributeSetInstance_ID = Env.getContextAsInt(ctx, WindowNo, "M_AttributeSetInstance_ID");
				BigDecimal available = MStorage.getQtyAvailable(0, M_Locator_ID, M_Product_ID, M_AttributeSetInstance_ID, null);
				if (available == null)
					available = Env.ZERO;
				if (available.signum() == 0)
					mTab.fireDataStatusEEvent("NoQtyAvailable", "0", false);
				else if (available.compareTo(MovementQty) < 0)
					mTab.fireDataStatusEEvent("InsufficientQtyAvailable", available.toString(), false);
			}
		}
		// End Armen
	}
}	//	CalloutMove