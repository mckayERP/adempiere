/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2016 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
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
package org.compiere.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

/** Generated Model for M_MPolicyTicket
 *  @author Adempiere (generated) 
 *  @version Release 3.8.0 - $Id$ */
public class X_M_MPolicyTicket extends PO implements I_M_MPolicyTicket, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170206L;

    /** Standard Constructor */
    public X_M_MPolicyTicket (Properties ctx, int M_MPolicyTicket_ID, String trxName)
    {
      super (ctx, M_MPolicyTicket_ID, trxName);
      /** if (M_MPolicyTicket_ID == 0)
        {
			setM_MPolicyTicket_ID (0);
        } */
    }

    /** Load Constructor */
    public X_M_MPolicyTicket (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_M_MPolicyTicket[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_OrderLine getC_OrderLine() throws RuntimeException
    {
		return (org.compiere.model.I_C_OrderLine)MTable.get(getCtx(), org.compiere.model.I_C_OrderLine.Table_Name)
			.getPO(getC_OrderLine_ID(), get_TrxName());	}

	/** Set Sales Order Line.
		@param C_OrderLine_ID 
		Sales Order Line
	  */
	public void setC_OrderLine_ID (int C_OrderLine_ID)
	{
		if (C_OrderLine_ID < 1) 
			set_Value (COLUMNNAME_C_OrderLine_ID, null);
		else 
			set_Value (COLUMNNAME_C_OrderLine_ID, Integer.valueOf(C_OrderLine_ID));
	}

	/** Get Sales Order Line.
		@return Sales Order Line
	  */
	public int getC_OrderLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_OrderLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_ProjectIssue getC_ProjectIssue() throws RuntimeException
    {
		return (org.compiere.model.I_C_ProjectIssue)MTable.get(getCtx(), org.compiere.model.I_C_ProjectIssue.Table_Name)
			.getPO(getC_ProjectIssue_ID(), get_TrxName());	}

	/** Set Project Issue.
		@param C_ProjectIssue_ID 
		Project Issues (Material, Labor)
	  */
	public void setC_ProjectIssue_ID (int C_ProjectIssue_ID)
	{
		if (C_ProjectIssue_ID < 1) 
			set_Value (COLUMNNAME_C_ProjectIssue_ID, null);
		else 
			set_Value (COLUMNNAME_C_ProjectIssue_ID, Integer.valueOf(C_ProjectIssue_ID));
	}

	/** Get Project Issue.
		@return Project Issues (Material, Labor)
	  */
	public int getC_ProjectIssue_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_ProjectIssue_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_InOutLine getM_InOutLine() throws RuntimeException
    {
		return (org.compiere.model.I_M_InOutLine)MTable.get(getCtx(), org.compiere.model.I_M_InOutLine.Table_Name)
			.getPO(getM_InOutLine_ID(), get_TrxName());	}

	/** Set Shipment/Receipt Line.
		@param M_InOutLine_ID 
		Line on Shipment or Receipt document
	  */
	public void setM_InOutLine_ID (int M_InOutLine_ID)
	{
		if (M_InOutLine_ID < 1) 
			set_Value (COLUMNNAME_M_InOutLine_ID, null);
		else 
			set_Value (COLUMNNAME_M_InOutLine_ID, Integer.valueOf(M_InOutLine_ID));
	}

	/** Get Shipment/Receipt Line.
		@return Line on Shipment or Receipt document
	  */
	public int getM_InOutLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOutLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_InventoryLine getM_InventoryLine() throws RuntimeException
    {
		return (org.compiere.model.I_M_InventoryLine)MTable.get(getCtx(), org.compiere.model.I_M_InventoryLine.Table_Name)
			.getPO(getM_InventoryLine_ID(), get_TrxName());	}

	/** Set Phys.Inventory Line.
		@param M_InventoryLine_ID 
		Unique line in an Inventory document
	  */
	public void setM_InventoryLine_ID (int M_InventoryLine_ID)
	{
		if (M_InventoryLine_ID < 1) 
			set_Value (COLUMNNAME_M_InventoryLine_ID, null);
		else 
			set_Value (COLUMNNAME_M_InventoryLine_ID, Integer.valueOf(M_InventoryLine_ID));
	}

	/** Get Phys.Inventory Line.
		@return Unique line in an Inventory document
	  */
	public int getM_InventoryLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InventoryLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_MovementLine getM_MovementLine() throws RuntimeException
    {
		return (org.compiere.model.I_M_MovementLine)MTable.get(getCtx(), org.compiere.model.I_M_MovementLine.Table_Name)
			.getPO(getM_MovementLine_ID(), get_TrxName());	}

	/** Set Move Line.
		@param M_MovementLine_ID 
		Inventory Move document Line
	  */
	public void setM_MovementLine_ID (int M_MovementLine_ID)
	{
		if (M_MovementLine_ID < 1) 
			set_Value (COLUMNNAME_M_MovementLine_ID, null);
		else 
			set_Value (COLUMNNAME_M_MovementLine_ID, Integer.valueOf(M_MovementLine_ID));
	}

	/** Get Move Line.
		@return Inventory Move document Line
	  */
	public int getM_MovementLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_MovementLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Material Policy Ticket.
		@param M_MPolicyTicket_ID 
		A Material Policy Ticket is used to track the FIFO/LIFO lifecycle of products in storage according to the material policy 
	  */
	public void setM_MPolicyTicket_ID (int M_MPolicyTicket_ID)
	{
		if (M_MPolicyTicket_ID < 0) 
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

	public org.compiere.model.I_M_ProductionLine getM_ProductionLine() throws RuntimeException
    {
		return (org.compiere.model.I_M_ProductionLine)MTable.get(getCtx(), org.compiere.model.I_M_ProductionLine.Table_Name)
			.getPO(getM_ProductionLine_ID(), get_TrxName());	}

	/** Set Production Line.
		@param M_ProductionLine_ID 
		Document Line representing a production
	  */
	public void setM_ProductionLine_ID (int M_ProductionLine_ID)
	{
		if (M_ProductionLine_ID < 1) 
			set_Value (COLUMNNAME_M_ProductionLine_ID, null);
		else 
			set_Value (COLUMNNAME_M_ProductionLine_ID, Integer.valueOf(M_ProductionLine_ID));
	}

	/** Get Production Line.
		@return Document Line representing a production
	  */
	public int getM_ProductionLine_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_ProductionLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Movement Date.
		@param MovementDate 
		Date a product was moved in or out of inventory
	  */
	public void setMovementDate (Timestamp MovementDate)
	{
		set_Value (COLUMNNAME_MovementDate, MovementDate);
	}

	/** Get Movement Date.
		@return Date a product was moved in or out of inventory
	  */
	public Timestamp getMovementDate () 
	{
		return (Timestamp)get_Value(COLUMNNAME_MovementDate);
	}

	public org.eevolution.model.I_PP_Cost_Collector getPP_Cost_Collector() throws RuntimeException
    {
		return (org.eevolution.model.I_PP_Cost_Collector)MTable.get(getCtx(), org.eevolution.model.I_PP_Cost_Collector.Table_Name)
			.getPO(getPP_Cost_Collector_ID(), get_TrxName());	}

	/** Set Manufacturing Cost Collector.
		@param PP_Cost_Collector_ID Manufacturing Cost Collector	  */
	public void setPP_Cost_Collector_ID (int PP_Cost_Collector_ID)
	{
		if (PP_Cost_Collector_ID < 1) 
			set_Value (COLUMNNAME_PP_Cost_Collector_ID, null);
		else 
			set_Value (COLUMNNAME_PP_Cost_Collector_ID, Integer.valueOf(PP_Cost_Collector_ID));
	}

	/** Get Manufacturing Cost Collector.
		@return Manufacturing Cost Collector	  */
	public int getPP_Cost_Collector_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Cost_Collector_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}