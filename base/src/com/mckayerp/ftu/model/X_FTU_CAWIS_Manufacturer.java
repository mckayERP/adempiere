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
package com.mckayerp.ftu.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for FTU_CAWIS_Manufacturer
 *  @author Adempiere (generated) 
 *  @version Release 3.8.0 - $Id$ */
public class X_FTU_CAWIS_Manufacturer extends PO implements I_FTU_CAWIS_Manufacturer, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170407L;

    /** Standard Constructor */
    public X_FTU_CAWIS_Manufacturer (Properties ctx, int FTU_CAWIS_Manufacturer_ID, String trxName)
    {
      super (ctx, FTU_CAWIS_Manufacturer_ID, trxName);
      /** if (FTU_CAWIS_Manufacturer_ID == 0)
        {
			setFTU_CAWIS_Manufacturer (null);
			setFTU_CAWIS_Manufacturer_ID (0);
        } */
    }

    /** Load Constructor */
    public X_FTU_CAWIS_Manufacturer (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_FTU_CAWIS_Manufacturer[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	/** Set Manufacturer (CAWIS).
		@param FTU_CAWIS_Manufacturer 
		The manufacturer name as used by the CAWIS website.
	  */
	public void setFTU_CAWIS_Manufacturer (String FTU_CAWIS_Manufacturer)
	{
		set_ValueNoCheck (COLUMNNAME_FTU_CAWIS_Manufacturer, FTU_CAWIS_Manufacturer);
	}

	/** Get Manufacturer (CAWIS).
		@return The manufacturer name as used by the CAWIS website.
	  */
	public String getFTU_CAWIS_Manufacturer () 
	{
		return (String)get_Value(COLUMNNAME_FTU_CAWIS_Manufacturer);
	}

	/** Set CAWIS Manufacturer.
		@param FTU_CAWIS_Manufacturer_ID 
		The CAWIS manufacturer
	  */
	public void setFTU_CAWIS_Manufacturer_ID (int FTU_CAWIS_Manufacturer_ID)
	{
		if (FTU_CAWIS_Manufacturer_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_FTU_CAWIS_Manufacturer_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_FTU_CAWIS_Manufacturer_ID, Integer.valueOf(FTU_CAWIS_Manufacturer_ID));
	}

	/** Get CAWIS Manufacturer.
		@return The CAWIS manufacturer
	  */
	public int getFTU_CAWIS_Manufacturer_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_FTU_CAWIS_Manufacturer_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}