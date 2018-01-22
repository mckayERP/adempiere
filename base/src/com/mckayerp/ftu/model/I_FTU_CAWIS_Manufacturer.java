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
package com.mckayerp.ftu.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import org.compiere.model.*;
import org.compiere.util.KeyNamePair;

/** Generated Interface for FTU_CAWIS_Manufacturer
 *  @author Adempiere (generated) 
 *  @version Release 3.8.0
 */
public interface I_FTU_CAWIS_Manufacturer 
{

    /** TableName=FTU_CAWIS_Manufacturer */
    public static final String Table_Name = "FTU_CAWIS_Manufacturer";

    /** AD_Table_ID=54215 */
    public static final int Table_ID = MTable.getTable_ID(Table_Name);

    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);

    /** AccessLevel = 3 - Client - Org 
     */
    BigDecimal accessLevel = BigDecimal.valueOf(3);

    /** Load Meta Data */

    /** Column name AD_Client_ID */
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";

	/** Get Client.
	  * Client/Tenant for this installation.
	  */
	public int getAD_Client_ID();

    /** Column name AD_Org_ID */
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";

	/** Set Organization.
	  * Organizational entity within client
	  */
	public void setAD_Org_ID (int AD_Org_ID);

	/** Get Organization.
	  * Organizational entity within client
	  */
	public int getAD_Org_ID();

    /** Column name Created */
    public static final String COLUMNNAME_Created = "Created";

	/** Get Created.
	  * Date this record was created
	  */
	public Timestamp getCreated();

    /** Column name CreatedBy */
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";

	/** Get Created By.
	  * User who created this records
	  */
	public int getCreatedBy();

    /** Column name FTU_CAWIS_Manufacturer */
    public static final String COLUMNNAME_FTU_CAWIS_Manufacturer = "FTU_CAWIS_Manufacturer";

	/** Set Manufacturer (CAWIS).
	  * The manufacturer name as used by the CAWIS website.
	  */
	public void setFTU_CAWIS_Manufacturer (String FTU_CAWIS_Manufacturer);

	/** Get Manufacturer (CAWIS).
	  * The manufacturer name as used by the CAWIS website.
	  */
	public String getFTU_CAWIS_Manufacturer();

    /** Column name FTU_CAWIS_Manufacturer_ID */
    public static final String COLUMNNAME_FTU_CAWIS_Manufacturer_ID = "FTU_CAWIS_Manufacturer_ID";

	/** Set CAWIS Manufacturer.
	  * The CAWIS manufacturer
	  */
	public void setFTU_CAWIS_Manufacturer_ID (int FTU_CAWIS_Manufacturer_ID);

	/** Get CAWIS Manufacturer.
	  * The CAWIS manufacturer
	  */
	public int getFTU_CAWIS_Manufacturer_ID();

    /** Column name IsActive */
    public static final String COLUMNNAME_IsActive = "IsActive";

	/** Set Active.
	  * The record is active in the system
	  */
	public void setIsActive (boolean IsActive);

	/** Get Active.
	  * The record is active in the system
	  */
	public boolean isActive();

    /** Column name Updated */
    public static final String COLUMNNAME_Updated = "Updated";

	/** Get Updated.
	  * Date this record was updated
	  */
	public Timestamp getUpdated();

    /** Column name UpdatedBy */
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";

	/** Get Updated By.
	  * User who updated this records
	  */
	public int getUpdatedBy();
}
