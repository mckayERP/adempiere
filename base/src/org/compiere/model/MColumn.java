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
 *****************************************************************************/
package org.compiere.model;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.util.CCache;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.DisplayType;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Trx;
import org.compiere.util.Util;

/**
 *	Persistent Column Model
 *	
 *  @author Jorg Janke
 *  @author victor.perez@e-evolution.com, www.e-evolution.com
 *  	<li>FR [ 3426134 ] Add the Reference ,FieldLength, Reference Value
 * 		https://sourceforge.net/tracker/?func=detail&aid=3426134&group_id=176962&atid=879335
 * 		<li> Add method that valid if a column is encrypted
 *  @version $Id: MColumn.java,v 1.6 2006/08/09 05:23:49 jjanke Exp $
 *  @author Yamel Senih, ysenih@erpcya.com, ERPCyA http://www.erpcya.com
 *  	<li> BR [ 9223372036854775807 ] Lookup for search view not show button
 *  	<li> Add default length to Yes No Display Type
 *  	@see https://adempiere.atlassian.net/browse/ADEMPIERE-447
 *  	<li> BR [ 185 ] Fixed error with validation in beforeSave method for MColumn 
 *  	@see https://github.com/adempiere/adempiere/issues/185
 *  @author mckayERP www.mckayERP.com
 *  	<li> #213 Support for application dictionary changes 
 *  		 and configurable automatic syncing with the database
 */
public class MColumn extends X_AD_Column
{


	/**
	 * 
	 */
	private static final long serialVersionUID = 3455817869952578951L;

	public static final String SYSCONFIG_DATABASE_AUTO_SYNC="DATABASE_AUTO_SYNC";
	
	/**
     * Get if id column is Encrypted
     * @param columnId
     * @return true or false
     */
    public static boolean isEncrypted (int columnId)
    {
        final String sql = "SELECT IsEncrypted FROM AD_Column WHERE AD_Column_ID = ?";
        return "Y".equals(DB.getSQLValueString(null , sql , columnId));
    }

	/**
	 * Set default base on AD_Element FR [ 3426134 ]
	 * @param ctx context
	 * @param column AD Column
	 * @param trxName transaction Name
	 * @return I_AD_Column
	 */
	public static I_AD_Column setAD_Column(Properties ctx ,I_AD_Column column , String trxName)
	{
		MTable table = (MTable) column.getAD_Table();
		M_Element element =  new M_Element(ctx, column.getAD_Element_ID() , trxName);
		if(element.getAD_Reference_ID() == DisplayType.ID)
		{
			String columnName = table.getTableName()+"_ID";
            String tableDir = element.getColumnName().replace("_ID", "");
			if(!columnName.equals(element.getColumnName()) && MTable.getTable_ID(tableDir) > 0)
			{
				column.setAD_Reference_ID(DisplayType.TableDir);
			}
		}

		String entityType = column.getAD_Table().getEntityType();
		if (entityType == null)
			throw  new AdempiereException("@EntityType@ @@AD_Table_ID@ @NotFound@");

		if(!MTable.ENTITYTYPE_Dictionary.equals(entityType))
			column.setEntityType(entityType);
		
		if(column.getColumnName() == null || column.getColumnName().length() <= 0)
			column.setColumnName(element.getColumnName());	
		if(column.getFieldLength() <= 0 )
			column.setFieldLength(element.getFieldLength());
		if(column.getAD_Reference_ID() <= 0)	
			column.setAD_Reference_ID(element.getAD_Reference_ID());
		if(column.getAD_Reference_Value_ID() <= 0)
			column.setAD_Reference_Value_ID(element.getAD_Reference_Value_ID());
		if(column.getName() == null || column.getName().length() <= 0)
			column.setName(element.getName());
		if(column.getDescription() == null || column.getDescription().length() <= 0)
			column.setDescription(element.getDescription());
		if(column.getHelp() == null || column.getHelp().length() <= 0)
			column.setHelp(element.getHelp());
		/*if(column.getColumnName().equals("Name") || column.getColumnName().equals("Value"))
		{	
			column.setIsIdentifier(true);
			int seqNo = DB.getSQLValue(trxName,"SELECT MAX(SeqNo) FROM AD_Column "+
					"WHERE AD_Table_ID=?"+
					" AND IsIdentifier='Y'",column.getAD_Table_ID());
			column.setSeqNo(seqNo + 1);
		}*/
		return column;	
	}
	/**
	 * 	Get MColumn from Cache
	 *	@param ctx context
	 * 	@param AD_Column_ID id
	 *	@return MColumn
	 */
	public static MColumn get (Properties ctx, int AD_Column_ID)
	{
		Integer key = new Integer (AD_Column_ID);
		MColumn retValue = (MColumn) s_cache.get (key);
		if (retValue != null)
			return retValue;
		retValue = new MColumn (ctx, AD_Column_ID, null);
		if (retValue.get_ID () != 0)
			s_cache.put (key, retValue);
		return retValue;
	}	//	get

	/**
	 * 	Get Column Name
	 *	@param ctx context
	 *	@param AD_Column_ID id
	 *	@return Column Name or null
	 */
	public static String getColumnName (Properties ctx, int AD_Column_ID)
	{
		MColumn col = MColumn.get(ctx, AD_Column_ID);
		if (col.get_ID() == 0)
			return null;
		return col.getColumnName();
	}	//	getColumnName
	
	/**	Cache						*/
	private static CCache<Integer,MColumn>	s_cache	= new CCache<Integer,MColumn>("AD_Column", 20);
	
	/**	Static Logger	*/
	private static CLogger	s_log	= CLogger.getCLogger (MColumn.class);

	private boolean isNewTable = false;
	
	/**************************************************************************
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param AD_Column_ID
	 *	@param trxName transaction
	 */
	public MColumn (Properties ctx, int AD_Column_ID, String trxName)
	{
		super (ctx, AD_Column_ID, trxName);
		if (AD_Column_ID == 0)
		{
		//	setAD_Element_ID (0);
		//	setAD_Reference_ID (0);
		//	setColumnName (null);
		//	setName (null);
		//	setEntityType (null);	// U
			setIsAlwaysUpdateable (false);	// N
			setIsEncrypted (false);
			setIsIdentifier (false);
			setIsKey (false);
			setIsMandatory (false);
			setIsParent (false);
			setIsSelectionColumn (false);
			setIsTranslated (false);
			setIsUpdateable (true);	// Y
			setVersion (Env.ZERO);
		}
	}	//	MColumn

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 *	@param trxName transaction
	 */
	public MColumn (Properties ctx, ResultSet rs, String trxName)
	{
		super(ctx, rs, trxName);
	}	//	MColumn
	
	/**
	 * 	Parent Constructor
	 *	@param parent table
	 */
	public MColumn (MTable parent)
	{
		this (parent.getCtx(), 0, parent.get_TrxName());
		setClientOrg(parent);
		setAD_Table_ID (parent.getAD_Table_ID());
		setEntityType(parent.getEntityType());
	}	//	MColumn
	

	/**
	 * create new column FR [ 3426134 ]
	 * @param parent
	 * @param columnName
	 * @param AD_Element_ID
	 * @param length
	 * @param AD_Reference
	 * @param defaultValue
	 */
	public MColumn (MTable parent, String columnName, int length , int AD_Reference , String defaultValue)
	{
		this (parent.getCtx(), 0, parent.get_TrxName());
		setClientOrg(parent);
		setAD_Table_ID (parent.getAD_Table_ID());
		setEntityType(parent.getEntityType());
		setColumnName(columnName);	
		M_Element AD_Element = M_Element.get(getCtx(),columnName);
		if(AD_Element != null )
		{	
			setAD_Element_ID(AD_Element.get_ID());
		}	
		setName(columnName);
		setIsActive(true);
		setVersion(Env.ONE);
		setIsMandatory(true);
		setIsAllowLogging(true);
		setFieldLength(length);
		setAD_Reference_ID(AD_Reference);
		setDefaultValue(defaultValue);
		setUpdateable(false);
	}	//	MColumn
	
	/**
	 * 	Is Standard Column
	 *	@return true for AD_Client_ID, etc.
	 */
	public boolean isStandardColumn()
	{
		String columnName = getColumnName();
		if (columnName.equals("AD_Client_ID") 
			|| columnName.equals("AD_Org_ID")
			|| columnName.equals("IsActive")
			|| columnName.startsWith("Created")
			|| columnName.startsWith("Updated") )
			return true;
		
		return false;
	}	//	isStandardColumn
	
	/**
	 * 	Is Virtual Column
	 *	@return true if virtual column
	 */
	public boolean isVirtualColumn()
	{
		String s = getColumnSQL();
		return s != null && s.length() > 0;
	}	//	isVirtualColumn
	
	/**
	 * 	Is the Column Encrypted?
	 *	@return true if encrypted
	 */
	public boolean isEncrypted()
	{
		String s = getIsEncrypted();
		return "Y".equals(s);
	}	//	isEncrypted
	
	/**
	 * 	Set Encrypted
	 *	@param IsEncrypted encrypted
	 */
	public void setIsEncrypted (boolean IsEncrypted)
	{
		setIsEncrypted (IsEncrypted ? "Y" : "N");
	}	//	setIsEncrypted
	
	/**
	 * 	Before Save
	 *	@param newRecord new
	 *	@return true
	 */
	protected boolean beforeSave (boolean newRecord)
	{
		//set column default based in element when is a new column FR [ 3426134 ]
		if(newRecord)
			setAD_Column(getCtx(), this, get_TrxName());

		int displayType = getAD_Reference_ID();
		if (DisplayType.isLOB(displayType))	//	LOBs are 0
		{
			if (getFieldLength() != 0)
				setFieldLength(0);
		}
		else if (getFieldLength() == 0) 
		{
			if (DisplayType.isID(displayType))
				setFieldLength(10);
			else if (DisplayType.isNumeric (displayType))
				setFieldLength(14);
			else if (DisplayType.isDate (displayType))
				setFieldLength(7);
			else if(displayType == DisplayType.YesNo)
				setFieldLength(1);
			else {
				log.saveError("FillMandatory", Msg.getElement(getCtx(), "FieldLength"));
				return false;
			}
		}
		
		//	BR [ 9223372036854775807 ]
		//  Skip the validation if this is a Direct Load (from a migration) or the Element is changing.
		//  This is causing problems with packin - the table may not be visible - may need the context to 
		//  work
		if (!isDirectLoad() 
		    && (this.get_Value(MColumn.COLUMNNAME_AD_Element_ID).equals(get_ValueOld(MColumn.COLUMNNAME_AD_Element_ID))))
//			validLookup(getColumnName(), getAD_Reference_ID(), getAD_Reference_Value_ID());
		
		/** Views are not updateable
		UPDATE AD_Column c
		SET IsUpdateable='N', IsAlwaysUpdateable='N'
		WHERE AD_Table_ID IN (SELECT AD_Table_ID FROM AD_Table WHERE IsView='Y')
		**/
		
		/* Diego Ruiz - globalqss - BF [1651899] - AD_Column: Avoid dup. SeqNo for IsIdentifier='Y' */
		if (isIdentifier())
		{
			int cnt = DB.getSQLValue(get_TrxName(),"SELECT COUNT(*) FROM AD_Column "+
					"WHERE AD_Table_ID=?"+
					" AND AD_Column_ID!=?"+
					" AND IsIdentifier='Y'"+
					" AND SeqNo=?",
					new Object[] {getAD_Table_ID(), getAD_Column_ID(), getSeqNo()});
			if (cnt>0)
			{
				log.saveError("SaveErrorNotUnique", Msg.getElement(getCtx(), COLUMNNAME_SeqNo));
				return false;
			}
		}
		
		//	Virtual Column
		if (isVirtualColumn())
		{
			if (isMandatory())
				setIsMandatory(false);
			if (isUpdateable())
				setIsUpdateable(false);
		}
		//	Updateable
		if (isParent() || isKey())
			setIsUpdateable(false);
		if (isAlwaysUpdateable() && !isUpdateable())
			setIsAlwaysUpdateable(false);
		//	Encrypted
		if (isEncrypted()) 
		{
			int dt = getAD_Reference_ID();
			if (isKey() || isParent() || isStandardColumn()
				|| isVirtualColumn() || isIdentifier() || isTranslated()
				|| DisplayType.isLookup(dt) || DisplayType.isLOB(dt)
				|| "DocumentNo".equalsIgnoreCase(getColumnName())
				|| "Value".equalsIgnoreCase(getColumnName())
				|| "Name".equalsIgnoreCase(getColumnName()))
			{
				log.warning("Encryption not sensible - " + getColumnName());
				setIsEncrypted(false);
			}
		}	
		
		//	Sync Terminology
		if ((newRecord || is_ValueChanged ("AD_Element_ID")) 
			&& getAD_Element_ID() != 0)
		{
			M_Element element = new M_Element (getCtx(), getAD_Element_ID (), get_TrxName());
			setColumnName (element.getColumnName());
			setName (element.getName());
			setDescription (element.getDescription());
			setHelp (element.getHelp());
		}
		return true;
	}	//	beforeSave
	
	/**
	 * Verify if is a lookup valid
	 * @param p_ColumnName
	 * @param p_AD_Reference_ID
	 * @param p_AD_Reference_Value_ID
	 * @return
	 */
	public static void validLookup(String p_ColumnName, int p_AD_Reference_ID, int p_AD_Reference_Value_ID) {

		//	Valid 
		if(p_ColumnName == null
				||p_ColumnName.trim().length() == 0
				|| !DisplayType.isLookup(p_AD_Reference_ID)) {
			return;
		} else {
			String m_TableName = p_ColumnName.replace("_ID", "");
			//	BR [ 185 ]
			if(p_AD_Reference_ID == DisplayType.TableDir) {
				if(!p_ColumnName.endsWith("_ID"))
					throw new AdempiereException("@Reference@ @of@ @ColumnName@ @NotValid@");
				//	Valid Table
				MTable table = MTable.get(Env.getCtx(), m_TableName);
				//	Valid Exists table
				if(table == null)
					throw new AdempiereException("@AD_Table_ID@ @NotFound@");
			} else if(p_AD_Reference_ID == DisplayType.Table
					|| p_AD_Reference_ID == DisplayType.Search) {
				if(p_AD_Reference_Value_ID == 0
						&& !M_Element.isLookupColumnName(p_ColumnName, p_AD_Reference_ID))
					throw new AdempiereException("@AD_Reference_Value_ID@ @IsMandatory@");
			} else if(p_AD_Reference_ID == DisplayType.List) {
				if(p_AD_Reference_Value_ID == 0) {
					throw new AdempiereException("@AD_Reference_Value_ID@ @IsMandatory@");
				}
			}
		}
	}
	
	/**
	 * 	After Save
	 *	@param newRecord new
	 *	@param success success
	 *	@return success
	 */
	protected boolean afterSave (boolean newRecord, boolean success)
	{
		// TODO - auto sync
		// Check if we should auto-sync the column and table.
		if ("Y".equals(MSysConfig.getValue(SYSCONFIG_DATABASE_AUTO_SYNC,"Y",Env.getAD_Client_ID(Env.getCtx())))
				&& (newRecord
					|| is_ValueChanged(MColumn.COLUMNNAME_ColumnName)
					|| is_ValueChanged(MColumn.COLUMNNAME_AD_Reference_ID)
					|| is_ValueChanged(MColumn.COLUMNNAME_FieldLength)
					|| is_ValueChanged(MColumn.COLUMNNAME_IsMandatory)
					|| is_ValueChanged(MColumn.COLUMNNAME_IsKey)
					|| is_ValueChanged(MColumn.COLUMNNAME_DefaultValue)
					|| is_ValueChanged(MColumn.COLUMNNAME_ColumnSQL))) {
			syncDatabase((String) this.get_ValueOld(COLUMNNAME_ColumnName));
		}
		
		//	Update Fields
		if (!newRecord)
		{
			if (   is_ValueChanged(MColumn.COLUMNNAME_Name)
				|| is_ValueChanged(MColumn.COLUMNNAME_Description)
				|| is_ValueChanged(MColumn.COLUMNNAME_Help)
				) {
				StringBuffer whereClause = new StringBuffer("AD_Column_ID=? ")
								.append(" AND IsCentrallyMaintained=? ");
				List<Object> parameters = new ArrayList<>();
				parameters.add(this.getAD_Column_ID());
				parameters.add(true);
				List<MField> fields = new Query(getCtx(), MField.Table_Name, whereClause.toString(), get_TrxName())
						.setParameters(parameters)
						.list();
				int no = 0;
				for (MField field: fields)
				{
					field.setName(getName());
					field.setDescription(getDescription());
					field.setHelp(getHelp());
					field.saveEx();
					no++;
				}

//				StringBuffer sql = new StringBuffer("UPDATE AD_Field SET Name=")
//					.append(DB.TO_STRING(getName()))
//					.append(", Description=").append(DB.TO_STRING(getDescription()))
//					.append(", Help=").append(DB.TO_STRING(getHelp()))
//					.append(" WHERE AD_Column_ID=").append(get_ID())
//					.append(" AND IsCentrallyMaintained='Y'");
//				int no = DB.executeUpdate(sql.toString(), get_TrxName());
				log.fine("afterSave - Fields updated #" + no);
			}
		}
		return success;
	}	//	afterSave
	
	/**
	 * 	Get SQL Add command
	 *	@param table table
	 *	@return sql
	 */
	public String getSQLAdd (MTable table)
	{
		if ( isVirtualColumn() )
			return null;
		
		StringBuffer sql = null;
		
		// If the table is new or the field can be null, it can be added with a single statement
		if (!isMandatory() || isNewTable) {
			sql = new StringBuffer ("ALTER TABLE ")
				.append(table.getTableName())
				.append(" ADD ").append(getSQLDDL());
		}
		else {
			// If the table is not new (it has existing fields) and the field is mandatory
			// then the new field values should be set to the defaults.  This takes three statements:
			//  1. Add the field
			//  2. Set the value
			//  3. Make it mandatory
			
			// SQL to add the field
			sql = new StringBuffer ("ALTER TABLE ")
				.append(table.getTableName())
				.append(" ADD ").append(getColumnName())
				.append(" ").append(getSQLDataType());
				//	Inline Constraint
				if (getAD_Reference_ID() == DisplayType.YesNo)
					sql.append(" CHECK (").append(getColumnName()).append(" IN ('Y','N'))");
			sql.append(DB.SQLSTATEMENT_SEPARATOR);
			
			// Set the value
			String defaultValue = getDefaultValueString();
			if (isMandatory() && defaultValue != null && defaultValue.length() > 0 && !defaultValue.equals("NULL"))
			{
				StringBuffer sqlSet = new StringBuffer("UPDATE ")
					.append(table.getTableName())
					.append(" SET ").append(getColumnName())
					.append("=").append(defaultValue)
					.append(" WHERE ").append(getColumnName()).append(" IS NULL");
				sql.append(sqlSet).append(DB.SQLSTATEMENT_SEPARATOR);

				//  Set the column to Not Null
				sql.append("ALTER TABLE ")
				.append(table.getTableName())
				.append(" ALTER ").append(getColumnName())
				.append(" SET NOT NULL")
				.append(DB.SQLSTATEMENT_SEPARATOR);
			}
			
			// Add the constraints
			sql.append("ALTER TABLE ")
			.append(table.getTableName())
			.append(" ALTER ").append(getColumnName())
			.append(" SET DEFAULT ").append(getDefaultValueString())
				.append(DB.SQLSTATEMENT_SEPARATOR);
				
			String constraint = getConstraint(table.getTableName());
			if (constraint != null && constraint.length() > 0) {
				sql.append("ALTER TABLE ")
				.append(table.getTableName())
				.append(" ADD ").append(constraint);
			}
		}
		return sql.toString();
	}	//	getSQLAdd

	public String getDefaultValueString()
	{
		//	Default
		String defaultValue = getDefaultValue();
		if (defaultValue != null && defaultValue.length() > 0)
		{
			
			if (defaultValue.indexOf('@') != -1		//	no variables
				|| !defaultValue.startsWith("#")		//	no context - eg. #AD_Client_ID
				|| (! (DisplayType.isID(getAD_Reference_ID()) && defaultValue.equals("-1") ) ) )  // not for ID's with default -1
			{
				defaultValue = "NULL";
			}
			else if (DisplayType.isText(getAD_Reference_ID()) 
					|| getAD_Reference_ID() == DisplayType.List
					|| getAD_Reference_ID() == DisplayType.YesNo
					// Two special columns: Defined as Table but DB Type is String 
					|| getColumnName().equals("EntityType") || getColumnName().equals("AD_Language")
					|| (getAD_Reference_ID() == DisplayType.Button &&
							!(getColumnName().endsWith("_ID"))))
			{
				if (!defaultValue.startsWith("'") && !defaultValue.endsWith("'"))
					defaultValue = DB.TO_STRING(defaultValue);
			}
		}
		else
		{
			// default not defined
			defaultValue = "NULL";
		}
		return defaultValue;
	}
	/**
	 * 	Get SQL DDL
	 *	@return columnName datataype ..
	 */
	public String getSQLDDL()
	{
		if (isVirtualColumn())
			return null;
		
		StringBuffer sql = new StringBuffer (getColumnName())
			.append(" ").append(getSQLDataType());

		//	Default
			sql.append(" DEFAULT ").append(getDefaultValueString());

		//	Inline Constraint
		if (getAD_Reference_ID() == DisplayType.YesNo)
			sql.append(" CHECK (").append(getColumnName()).append(" IN ('Y','N'))");

		//	Null
		if (isMandatory())
			sql.append(" NOT NULL");
		return sql.toString();
	}	//	getSQLDDL	
	
	/**
	 * 	Get SQL Modify command
	 *	@param table table
	 *	@param setNullOption generate null / not null statement
	 *	@return sql separated by ;
	 */
	public String getSQLModify (MTable table, boolean setNullOption)
	{
		return getSQLModify(table, null, setNullOption);
	}
	
	/**
	 * 	Get SQL Modify command
	 *	@param table table
	 *  @param oldColumnName the oldColumnName or null if there is no change.
	 *	@param setNullOption generate null / not null statement
	 *	@return sql separated by ;
	 */
	public String getSQLModify (MTable table, String oldColumnName, boolean setNullOption)
	{
		StringBuffer sql = new StringBuffer();
		if (oldColumnName != null) {
			// Rename the column in the database
			sql = new StringBuffer("ALTER TABLE ")
					.append(table.getTableName())
					.append(" RENAME COLUMN ").append(oldColumnName).append(" TO ")
					.append(getColumnName())
					.append(DB.SQLSTATEMENT_SEPARATOR);
		}

		// TODO handle the constraints.  Modifying the defaults requires the drop of the constraint
		// For now, just allow renames and ignore other changes.
		if (this.isKey() || (getColumnName().endsWith("_ID") && getColumnName().replace("_ID", "").equals(table.get_TableName()))) {
			if (sql.length() == 0)
				return null;
			return sql.toString();
		}
		
		StringBuffer sqlBase = new StringBuffer ("ALTER TABLE ")
			.append(table.getTableName())
			.append(" MODIFY ").append(getColumnName());
		
		//	Default
		sql.append(sqlBase).append(" ").append(getSQLDataType())
			.append(" DEFAULT ").append(getDefaultValueString());
		
		//	Constraint
		
		//	Null Values
		String defaultValue = getDefaultValueString();
		if (isMandatory() && defaultValue != null && defaultValue.length() > 0 && !defaultValue.equals("NULL"))
		{
			StringBuffer sqlSet = new StringBuffer("UPDATE ")
				.append(table.getTableName())
				.append(" SET ").append(getColumnName())
				.append("=").append(defaultValue)
				.append(" WHERE ").append(getColumnName()).append(" IS NULL");
			sql.append(DB.SQLSTATEMENT_SEPARATOR).append(sqlSet);
		}
		
		//	Null
		if (setNullOption)  // TODO Fails if there is a constraint on this column.
		{
			StringBuffer sqlNull = new StringBuffer(sqlBase);
			if (isMandatory() && defaultValue != null && defaultValue.length() > 0 && !defaultValue.equals("NULL"))
				sqlNull.append(" NOT NULL");
			else
				sqlNull.append(" NULL");
			sql.append(DB.SQLSTATEMENT_SEPARATOR).append(sqlNull);
		}
		//
		return sql.toString();
	}	//	getSQLModify

	/**
	 * 	Get SQL Data Type
	 *	@return e.g. NVARCHAR2(60)
	 */
	public String getSQLDataType()
	{
		String columnName = getColumnName();
		int dt = getAD_Reference_ID();
		return DisplayType.getSQLDataType (dt, columnName, getFieldLength());
	}	//	getSQLDataType
	
	/**
	 * 	Get SQL Data Type
	 *	@return e.g. NVARCHAR2(60)
	 */
	/*
	private String getSQLDataType()
	{
		int dt = getAD_Reference_ID();
		if (DisplayType.isID(dt) || dt == DisplayType.Integer)
			return "NUMBER(10)";
		if (DisplayType.isDate(dt))
			return "DATE";
		if (DisplayType.isNumeric(dt))
			return "NUMBER";
		if (dt == DisplayType.Binary)
			return "BLOB";
		if (dt == DisplayType.TextLong)
			return "CLOB";
		if (dt == DisplayType.YesNo)
			return "CHAR(1)";
		if (dt == DisplayType.List)
			return "NVARCHAR2(" + getFieldLength() + ")";
		if (dt == DisplayType.Button)
			return "CHAR(" + getFieldLength() + ")";
		else if (!DisplayType.isText(dt))
			log.severe("Unhandled Data Type = " + dt);
			
		return "NVARCHAR2(" + getFieldLength() + ")";
	}	//	getSQLDataType
	*/
	
	/**
	 * 	Get Table Constraint
	 *	@param tableName table name
	 *	@return table constraint
	 */
	public String getConstraint(String tableName)
	{
		if (isKey()) {
			String constraintName;
			if (tableName.length() > 26)
				// Oracle restricts object names to 30 characters
				constraintName = tableName.substring(0, 26) + "_Key";
			else
				constraintName = tableName + "_Key";
			return "CONSTRAINT " + constraintName + " PRIMARY KEY (" + getColumnName() + ")";
		}
		/**
		if (getAD_Reference_ID() == DisplayType.TableDir 
			|| getAD_Reference_ID() == DisplayType.Search)
			return "CONSTRAINT " ADTable_ADTableTrl
				+ " FOREIGN KEY (" + getColumnName() + ") REFERENCES "
				+ AD_Table(AD_Table_ID) ON DELETE CASCADE
		**/
		
		return "";
	}	//	getConstraint
	
	/**
	 * 	String Representation
	 *	@return info
	 */
	public String toString()
	{
		StringBuffer sb = new StringBuffer ("MColumn[");
		sb.append (get_ID()).append ("-").append (getColumnName()).append ("]");
		return sb.toString ();
	}	//	toString

	/**
	 * 	get Column ID
	 * @param tableName
	 * @param columnName
     * @return
     */
	public static int getColumn_ID(String tableName,String columnName) {
		int m_table_id = MTable.getTable_ID(tableName);
		if (m_table_id == 0)
			return 0;
			
		int retValue = 0;
		String SQL = "SELECT AD_Column_ID FROM AD_Column WHERE AD_Table_ID = ?  AND columnname = ?";
		try
		{
			PreparedStatement pstmt = DB.prepareStatement(SQL, null);
			pstmt.setInt(1, m_table_id);
			pstmt.setString(2, columnName);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next())
				retValue = rs.getInt(1);
			rs.close();
			pstmt.close();
		}
		catch (SQLException e)
		{
			s_log.log(Level.SEVERE, SQL, e);
			retValue = -1;
		}
		return retValue;
	}
	//end vpj-cd e-evolution
	
	/**
	* Get Table Id for a column
	* @param ctx context
	* @param AD_Column_ID id
	* @param trxName transaction
	* @return MColumn
	*/
	public static int getTable_ID(Properties ctx, int AD_Column_ID, String trxName)
	{
		String sqlStmt = "SELECT AD_Table_ID FROM AD_Column WHERE AD_Column_ID=?";
		return DB.getSQLValue(trxName, sqlStmt, AD_Column_ID);
	}

	/**
	 * Sync this column with the database
	 * @return
	 */
	public String syncDatabase()
	{
		return syncDatabase(null);
	}
	
	/**
	 * Sync this column with the database. Provide the oldColumnName when the sync action is performed 
	 * after a save when get_ValueOld(ColumnName) will return null.
	 * @param oldColumnName : If the column name changed, the old Column Name or null if there was no name change.
	 * @return
	 */
	public String syncDatabase(String oldColumnName)
	{
		if (this.isVirtualColumn())
			return "Cannot sync a virtual column"; // TODO - Delete from database if it exists?
		
		// The table has to be in the cache or a new table will be created
		MTable table = MTable.get(getCtx(), getAD_Table_ID());
		
		if (table.isView())
			return "Cannot sync view";
		
		table.set_TrxName(get_TrxName());  // otherwise table.getSQLCreate may miss current column
		if (table.get_ID() == 0)
			throw new AdempiereException("@NotFound@ @AD_Table_ID@ " + getAD_Table_ID());
		
		if (oldColumnName != null && oldColumnName.equalsIgnoreCase(getColumnName())){ // There is no name change
			oldColumnName = null;
		}

		//	Find Column in Database
		Connection conn = null;
		ResultSet rs= null;
		String trxName = null;
		try {
			// This will not see tables or columns added in the current transaction.
			conn = DB.getConnectionRO();
			DatabaseMetaData md = conn.getMetaData();
			String catalog = DB.getDatabase().getCatalog();
			String schema = DB.getDatabase().getSchema();
			String tableName = table.getTableName();
			if (md.storesUpperCaseIdentifiers())
			{
				tableName = tableName.toUpperCase();
			}
			else if (md.storesLowerCaseIdentifiers())
			{
				tableName = tableName.toLowerCase();
			}
			int noColumns = 0;
			String sql = null;
			Boolean oldColumnExists = false;
			Boolean currentColumnExists = false;
			Boolean oldNotNull = false;
			Boolean currentNotNull = false;
			//
			// Find the table.  Tables can be new with no columns assigned.
			isNewTable = true;
			rs = md.getTables(catalog, schema, tableName, null);
			while (rs.next())
			{
				isNewTable = false;
			}
			rs.close();
			
			//	No Table
			if (isNewTable) {
				sql = table.getSQLCreate ();
			}
			else {
				rs = md.getColumns(catalog, schema, tableName, null);
				while (rs.next())
				{
					noColumns++;
					String columnName = rs.getString ("COLUMN_NAME");
					
					if (oldColumnName != null && columnName.equalsIgnoreCase(oldColumnName)) {
						oldColumnExists = true;
						oldNotNull = DatabaseMetaData.columnNoNulls == rs.getInt("NULLABLE");
					}
					
					if (columnName.equalsIgnoreCase(getColumnName())) {
						currentColumnExists = true;
						currentNotNull = DatabaseMetaData.columnNoNulls == rs.getInt("NULLABLE");
					}
				}
				rs.close();
				rs = null;
			
				//	No existing column
				if (!oldColumnExists && !currentColumnExists) {
					sql = getSQLAdd(table);
				}
				// Old column name exists
				else if (oldColumnExists && !currentColumnExists) {
					// Update the old column
					sql = getSQLModify(table, oldColumnName, isMandatory() != oldNotNull);
				}
				// New column name exists
				else if (!oldColumnExists && currentColumnExists) {
					// Update the current column - no name change
					sql = getSQLModify(table, null, isMandatory() != currentNotNull); // Can return a null string
				}
				// Both exist - which is a problem - so throw an error
				else if (oldColumnExists && currentColumnExists) {
					// TODO - Translate
					throw new AdempiereException("Can't synchronize the change of the column name. A column with that name alread exists in the table.");
				}
			}
			
			if ( sql == null )
				return "No sql. No changes made.";

			trxName = get_TrxName();
			if (!isDirectLoad())
				trxName = Trx.createTrxName("SyncColumn");
			
			if (sql.indexOf(DB.SQLSTATEMENT_SEPARATOR) == -1)
			{
				DB.executeUpdateEx(sql, trxName);
			}
			else
			{
				String statements[] = sql.split(DB.SQLSTATEMENT_SEPARATOR);
				for (int i = 0; i < statements.length; i++)
				{
//					try {
						DB.executeUpdateEx(statements[i], trxName);
//					}
//					catch (DBException e) {
//						if (e.getMessage()!= null && e.getMessage().contains("already exists")) {
//							// ignore the error
//							return "Column already exists. Probably just added.";
//						}
//						else {	
//							throw e;
//						}
//					}
				}
			}
			
			DB.commit(true, trxName);
			
			// Remove the old table definition from cache 
			POInfo.removeFromCache(getAD_Table_ID());
			return sql;

		} 
		catch (SQLException|DBException e ) {
			if (e.getMessage()!= null && e.getMessage().contains("already exists")) {
				// ignore the error
				return "Column already exists. Probably just added.";
			}
			else {	
				throw new AdempiereException(e);
			}
		}
		finally {
			DB.close(rs);
			if (conn != null) {
				try {
					conn.close();
				} catch (Exception e) {}
				conn = null;
			}
		}
	}

	public static boolean isSuggestSelectionColumn(String columnName, boolean caseSensitive)
	{
		if (Util.isEmpty(columnName, true))
			return false;
		//
        if (columnName.equals("Value") || (!caseSensitive && columnName.equalsIgnoreCase("Value")))
            return true;
        else if (columnName.equals("Name") || (!caseSensitive && columnName.equalsIgnoreCase("Name")))
            return true;
        else if (columnName.equals("DocumentNo") || (!caseSensitive && columnName.equalsIgnoreCase("DocumentNo")))
            return true;
        else if (columnName.equals("Description") || (!caseSensitive && columnName.equalsIgnoreCase("Description")))
            return true;
        else if (columnName.indexOf("Name") != -1
        		|| (!caseSensitive && columnName.toUpperCase().indexOf("Name".toUpperCase()) != -1) )
            return true;
        else
        	return false;
	}
	
	/**
	 * Determine the minimum allowable value for the KeyColumn.  Columns that end
	 * in "_ID" typically can have values of null or > 0.  In a few tables, the value
	 * "0" is valid.
	 * @param columnName
	 * @return
	 */
	public static Integer getKeyColumnFirstValue(String columnName) {

		Integer firstOK = null;
		
		if (columnName.endsWith("_ID"))
		{
			//	check special column  TODO hard-coded. Add to AD_Column to make this configurable
			if (columnName.equals("AD_Client_ID") || columnName.equals("AD_Org_ID")
				|| columnName.equals("Record_ID") || columnName.equals("C_DocType_ID")
				|| columnName.equals("Node_ID") || columnName.equals("AD_Role_ID")
				|| columnName.equals("M_AttributeSet_ID") || columnName.equals("M_AttributeSetInstance_ID")
				|| columnName.equals("M_MPolicyTicket_ID")
				|| columnName.equals("M_Warehouse_ID")) {
				firstOK = Integer.valueOf(0);
			}
			else
				firstOK = Integer.valueOf(1);
		}
		return firstOK;
	}
}	//	MColumn
