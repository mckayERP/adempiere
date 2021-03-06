/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2017 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * or (at your option) any later version.										*
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.eevolution.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for PP_OrderReceiptMA
 *  @author Adempiere (generated) 
 *  @version Release 3.9.3 - $Id$ */
public class X_PP_OrderReceiptMA extends PO implements I_PP_OrderReceiptMA, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20201101L;

    /** Standard Constructor */
    public X_PP_OrderReceiptMA (Properties ctx, int PP_OrderReceiptMA_ID, String trxName)
    {
      super (ctx, PP_OrderReceiptMA_ID, trxName);
      /** if (PP_OrderReceiptMA_ID == 0)
        {
			setM_MPolicyTicket_ID (0);
			setMovementQty (Env.ZERO);
			setPP_OrderReceiptMA_ID (0);
        } */
    }

    /** Load Constructor */
    public X_PP_OrderReceiptMA (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 3 - Client - Org 
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuffer sb = new StringBuffer ("X_PP_OrderReceiptMA[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public I_M_AttributeSetInstance getM_AttributeSetInstance() throws RuntimeException
    {
		return (I_M_AttributeSetInstance)MTable.get(getCtx(), I_M_AttributeSetInstance.Table_Name)
			.getPO(getM_AttributeSetInstance_ID(), get_TrxName());	}

	/** Set Attribute Set Instance.
		@param M_AttributeSetInstance_ID 
		Product Attribute Set Instance
	  */
	public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID)
	{
		if (M_AttributeSetInstance_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_AttributeSetInstance_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_AttributeSetInstance_ID, Integer.valueOf(M_AttributeSetInstance_ID));
	}

	/** Get Attribute Set Instance.
		@return Product Attribute Set Instance
	  */
	public int getM_AttributeSetInstance_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_AttributeSetInstance_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_MPolicyTicket getM_MPolicyTicket() throws RuntimeException
    {
		return (org.compiere.model.I_M_MPolicyTicket)MTable.get(getCtx(), org.compiere.model.I_M_MPolicyTicket.Table_Name)
			.getPO(getM_MPolicyTicket_ID(), get_TrxName());	}

	/** Set Material Policy Ticket.
		@param M_MPolicyTicket_ID 
		A Material Policy Ticket is used to track the FIFO/LIFO lifecycle of products in storage according to the material policy 
	  */
	public void setM_MPolicyTicket_ID (int M_MPolicyTicket_ID)
	{
		if (M_MPolicyTicket_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_MPolicyTicket_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_MPolicyTicket_ID, Integer.valueOf(M_MPolicyTicket_ID));
	}

	/** Get Material Policy Ticket.
		@return A Material Policy Ticket is used to track the FIFO/LIFO lifecycle of products in storage according to the material policy 
	  */
	public int getM_MPolicyTicket_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_MPolicyTicket_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Movement Quantity.
		@param MovementQty 
		Quantity of a product moved.
	  */
	public void setMovementQty (BigDecimal MovementQty)
	{
		set_Value (COLUMNNAME_MovementQty, MovementQty);
	}

	/** Get Movement Quantity.
		@return Quantity of a product moved.
	  */
	public BigDecimal getMovementQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_MovementQty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** MovementType AD_Reference_ID=189 */
	public static final int MOVEMENTTYPE_AD_Reference_ID=189;
	/** Customer Shipment = C- */
	public static final String MOVEMENTTYPE_CustomerShipment = "C-";
	/** Customer Returns = C+ */
	public static final String MOVEMENTTYPE_CustomerReturns = "C+";
	/** Vendor Receipts = V+ */
	public static final String MOVEMENTTYPE_VendorReceipts = "V+";
	/** Vendor Returns = V- */
	public static final String MOVEMENTTYPE_VendorReturns = "V-";
	/** Inventory Out = I- */
	public static final String MOVEMENTTYPE_InventoryOut = "I-";
	/** Inventory In = I+ */
	public static final String MOVEMENTTYPE_InventoryIn = "I+";
	/** Movement From = M- */
	public static final String MOVEMENTTYPE_MovementFrom = "M-";
	/** Movement To = M+ */
	public static final String MOVEMENTTYPE_MovementTo = "M+";
	/** Production + = P+ */
	public static final String MOVEMENTTYPE_ProductionPlus = "P+";
	/** Production - = P- */
	public static final String MOVEMENTTYPE_Production_ = "P-";
	/** Work Order + = W+ */
	public static final String MOVEMENTTYPE_WorkOrderPlus = "W+";
	/** Work Order - = W- */
	public static final String MOVEMENTTYPE_WorkOrder_ = "W-";
	/** Set Movement Type.
		@param MovementType 
		Method of moving the inventory
	  */
	public void setMovementType (String MovementType)
	{

		set_Value (COLUMNNAME_MovementType, MovementType);
	}

	/** Get Movement Type.
		@return Method of moving the inventory
	  */
	public String getMovementType () 
	{
		return (String)get_Value(COLUMNNAME_MovementType);
	}

	public org.eevolution.model.I_PP_OrderReceipt getPP_OrderReceipt() throws RuntimeException
    {
		return (org.eevolution.model.I_PP_OrderReceipt)MTable.get(getCtx(), org.eevolution.model.I_PP_OrderReceipt.Table_Name)
			.getPO(getPP_OrderReceipt_ID(), get_TrxName());	}

	/** Set Manufacturing Order Receipt ID.
		@param PP_OrderReceipt_ID Manufacturing Order Receipt ID	  */
	public void setPP_OrderReceipt_ID (int PP_OrderReceipt_ID)
	{
		if (PP_OrderReceipt_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_PP_OrderReceipt_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_PP_OrderReceipt_ID, Integer.valueOf(PP_OrderReceipt_ID));
	}

	/** Get Manufacturing Order Receipt ID.
		@return Manufacturing Order Receipt ID	  */
	public int getPP_OrderReceipt_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_OrderReceipt_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set PP_OrderReceiptMA ID.
		@param PP_OrderReceiptMA_ID PP_OrderReceiptMA ID	  */
	public void setPP_OrderReceiptMA_ID (int PP_OrderReceiptMA_ID)
	{
		if (PP_OrderReceiptMA_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_PP_OrderReceiptMA_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_PP_OrderReceiptMA_ID, Integer.valueOf(PP_OrderReceiptMA_ID));
	}

	/** Get PP_OrderReceiptMA ID.
		@return PP_OrderReceiptMA ID	  */
	public int getPP_OrderReceiptMA_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_OrderReceiptMA_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Use To Fields.
		@param UseToFields 
		Use the "To" fields. Relevant only in material allocation tables which have from-to ranges or pairs in the lines such as the MovementLine
	  */
	public void setUseToFields (boolean UseToFields)
	{
		set_Value (COLUMNNAME_UseToFields, Boolean.valueOf(UseToFields));
	}

	/** Get Use To Fields.
		@return Use the "To" fields. Relevant only in material allocation tables which have from-to ranges or pairs in the lines such as the MovementLine
	  */
	public boolean isUseToFields () 
	{
		Object oo = get_Value(COLUMNNAME_UseToFields);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Immutable Universally Unique Identifier.
		@param UUID 
		Immutable Universally Unique Identifier
	  */
	public void setUUID (String UUID)
	{
		set_Value (COLUMNNAME_UUID, UUID);
	}

	/** Get Immutable Universally Unique Identifier.
		@return Immutable Universally Unique Identifier
	  */
	public String getUUID () 
	{
		return (String)get_Value(COLUMNNAME_UUID);
	}
}