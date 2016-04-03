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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.Env;

/**
 *	Callout Engine.
 *	
 *  @author Jorg Janke
 *  @version $Id: CalloutEngine.java,v 1.3 2006/07/30 00:51:05 jjanke Exp $
 *  
 *  @author Teo Sarca, SC ARHIPAC SERVICE SRL
 *  		<li>BF [ 2104021 ] CalloutEngine returns null if the exception has null message
 *  
 *  @author mckayERP www.mckayERP.com
 *  		<li> #286 Provide methods to treat ASI fields in a consistent manner.
 */
public class CalloutEngine implements Callout
{
	/** No error return value. Use this when you are returning from a callout without error */
	public static final String NO_ERROR = "";
	
	/**
	 *	Constructor
	 */
	public CalloutEngine()
	{
		super();
	}

	/** Logger					*/
	protected CLogger		log = CLogger.getCLogger(getClass());
	private GridTab m_mTab;
	private GridField m_mField;

	/**
	 *	Start Callout.
	 *  <p>
	 *	Callout's are used for cross field validation and setting values in other fields
	 *	when returning a non empty (error message) string, an exception is raised
	 *  <p>
	 *	When invoked, the Tab model has the new value!
	 *
	 *  @param ctx      Context
	 *  @param methodName   Method name
	 *  @param WindowNo current Window No
	 *  @param mTab     Model Tab
	 *  @param mField   Model Field
	 *  @param value    The new value
	 *  @param oldValue The old value
	 *  @return Error message or ""
	 */
	public String start (Properties ctx, String methodName, int WindowNo,
		GridTab mTab, GridField mField, Object value, Object oldValue)
	{
		if (methodName == null || methodName.length() == 0)
			throw new IllegalArgumentException ("No Method Name");
		
		m_mTab = mTab;
		m_mField = mField;
		
		//
		String retValue = "";
		StringBuffer msg = new StringBuffer(methodName).append(" - ")
			.append(mField.getColumnName())
			.append("=").append(value)
			.append(" (old=").append(oldValue)
			.append(") {active=").append(isCalloutActive()).append("}");
		if (!isCalloutActive())
			log.info (msg.toString());
		
		//	Find Method
		Method method = getMethod(methodName);
		if (method == null)
			throw new IllegalArgumentException ("Method not found: " + methodName);
		int argLength = method.getParameterTypes().length;
		if (!(argLength == 5 || argLength == 6))
			throw new IllegalArgumentException ("Method " + methodName 
				+ " has invalid no of arguments: " + argLength);

		//	Call Method
		try
		{
			Object[] args = null;
			if (argLength == 6)
				args = new Object[] {ctx, new Integer(WindowNo), mTab, mField, value, oldValue};
			else
				args = new Object[] {ctx, new Integer(WindowNo), mTab, mField, value}; 
			retValue = (String)method.invoke(this, args);
		}
		catch (Exception e)
		{
			Throwable ex = e.getCause();	//	InvocationTargetException
			if (ex == null)
				ex = e;
			log.log(Level.SEVERE, "start: " + methodName, ex);
			retValue = ex.getLocalizedMessage();
			if (retValue == null)
			{
				retValue = ex.toString();
			}
		}
		finally
		{
			m_mTab = null;
			m_mField = null;
		}
		return retValue;
	}	//	start
	
	/**
	 *	Conversion Rules.
	 *	Convert a String
	 *
	 *	@param methodName   method name
	 *  @param value    the value
	 *	@return converted String or Null if no method found
	 */
	public String convert (String methodName, String value)
	{
		if (methodName == null || methodName.length() == 0)
			throw new IllegalArgumentException ("No Method Name");
		//
		String retValue = null;
		StringBuffer msg = new StringBuffer(methodName).append(" - ").append(value);
		log.info (msg.toString());
		//
		//	Find Method
		Method method = getMethod(methodName);
		if (method == null)
			throw new IllegalArgumentException ("Method not found: " + methodName);
		int argLength = method.getParameterTypes().length;
		if (argLength != 1)
			throw new IllegalArgumentException ("Method " + methodName 
				+ " has invalid no of arguments: " + argLength);

		//	Call Method
		try
		{
			Object[] args = new Object[] {value};
			retValue = (String)method.invoke(this, args);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "convert: " + methodName, e);
			e.printStackTrace(System.err);
		}
		return retValue;
	}   //  convert
	
	/**
	 * 	Get Method
	 *	@param methodName method name
	 *	@return method or null
	 */
	private Method getMethod (String methodName)
	{
		Method[] allMethods = getClass().getMethods();
		for (int i = 0; i < allMethods.length; i++)
		{
			if (methodName.equals(allMethods[i].getName()))
				return allMethods[i];
		}
		return null;
	}	//	getMethod

	/*************************************************************************/
	
	//private static boolean s_calloutActive = false;
	
	/**
	 * 	Is the current callout being called in the middle of 
     *  another callout doing her works.
     *  Callout can use GridTab.getActiveCalloutInstance() method
     *  to find out callout for which field is running.
	 *	@return true if active
	 */
	protected boolean isCalloutActive()
	{
		//greater than 1 instead of 0 to discount this callout instance
		return m_mTab != null ? m_mTab.getActiveCallouts().length > 1 : false;
	}	//	isCalloutActive

	/**
	 * 	Set Callout (in)active.
     *  Depreciated as the implementation is not thread safe and
     *  fragile - break other callout if developer forget to call
     *  setCalloutActive(false) after calling setCalloutActive(true).
	 *  @deprecated
	 *	@param active active
	 */
	protected static void setCalloutActive (boolean active)
	{
		;
	}	//	setCalloutActive
	
	/**
	 *  Set Account Date Value.
	 * 	org.compiere.model.CalloutEngine.dateAcct
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String dateAcct (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())		//	assuming it is resetting value
			return NO_ERROR;
		if (value == null || !(value instanceof Timestamp))
			return NO_ERROR;
		mTab.setValue("DateAcct", value);
		return NO_ERROR;
	}	//	dateAcct

	/**
	 *	Rate - set Multiply Rate from Divide Rate and vice versa
	 *	org.compiere.model.CalloutEngine.rate
	 *	@param ctx context
	 *	@param WindowNo window no
	 *	@param mTab tab
	 *	@param mField field
	 *	@param value value
	 *	@return null or error message
	 */
	public String rate (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive() || value == null)		//	assuming it is Conversion_Rate
			return NO_ERROR;

		BigDecimal rate1 = (BigDecimal)value;
		BigDecimal rate2 = Env.ZERO;
		BigDecimal one = new BigDecimal(1.0);

		if (rate1.doubleValue() != 0.0)	//	no divide by zero
			rate2 = one.divide(rate1, 12, BigDecimal.ROUND_HALF_UP);
		//
		if (mField.getColumnName().equals("MultiplyRate"))
			mTab.setValue("DivideRate", rate2);
		else
			mTab.setValue("MultiplyRate", rate2);
		log.info(mField.getColumnName() + "=" + rate1 + " => " + rate2);
		return NO_ERROR;
	}	//	rate
	
	/**
	 * 
	 * @return gridTab
	 */
	public GridTab getGridTab() 
	{
		return m_mTab;
	}
	
	/**
	 * 
	 * @return gridField
	 */
	public GridField getGridField() 
	{
		return m_mField;
	}

	/**
	 * Called by callouts, this procedure will test the value of a field "fieldName" for the correct
	 * value of the Attribute Set Instance (ASI) for the given product and window.  If no existing 
	 * ASI ID is provided, an ASI will be proposed.  The field error (background color) will be set
	 * if the ASI is missing required information.
	 * @param ctx The Context
	 * @param WindowNo The Window Number in the Context
	 * @param isSOTrx True if the window represents a sales or outgoing transaction
	 * @param mTab The GridTab of the relevant tab
	 * @param fieldName The name of the field, typically "M_AttributeSetInstance_ID"
	 * @param product The product model.  If null, the ASI field will be set to zero.
	 * @param existingASI_ID If null, the default ASI will be used from the context, otherwise the
	 * provided ASI will be tested.
	 */
	public static void setAndTestASI(Properties ctx, int WindowNo, Boolean isSOTrx, GridTab mTab, String fieldName, 
			MProduct product, Integer existingASI_ID) {
		int AD_Column_ID = 0;
		Integer M_AttributeSetInstance_ID = Integer.valueOf(0);
		
		if (mTab == null)
			return;
				
		GridField column = mTab.getField(fieldName);
		if (column != null) {  // The column is found
			AD_Column_ID = column.getAD_Column_ID();
			if (product != null) {
				if (existingASI_ID == null) { // Set the ASI
					M_AttributeSetInstance_ID = product.getEnvAttributeSetInstance(ctx, WindowNo, AD_Column_ID);
					mTab.setValue(fieldName, M_AttributeSetInstance_ID);
				}
				else {  // Don't set, just test the existing
					M_AttributeSetInstance_ID = existingASI_ID;
				}
				// Set column error if the ASI is mandatory 
				column.setError(!product.isValidAttributeSetInstance(ctx, Env.isSOTrx(ctx, WindowNo), AD_Column_ID, M_AttributeSetInstance_ID));
//				MAttributeSet as = product.getAttributeSet();
//				// Hide the ASI if not attribute set applies.
//				if (as != null) {
//					column.setDisplayed(!as.excludeEntry(AD_Column_ID, Env.isSOTrx(ctx, WindowNo)));
//				}
			}
			else { // No product - so no ASI
				mTab.setValue(fieldName, 0);
				column.setError(false);
				//column.setDisplayed(false);
			}
		}
	}

	/**
	 * Standard callout for M_AttributeSetInstance_ID field.
	 * @param ctx
	 * @param WindowNo
	 * @param mTab
	 * @param mField
	 * @param value
	 * @return
	 */
	public String attributeSetInstance (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";
		
		Integer M_ASI_ID = 0;
		if (value != null)
			M_ASI_ID = (Integer) value;
		
		int M_Product_ID = 0;
		if (mTab.getValue("M_Product_ID") != null)
			M_Product_ID = ((Integer)mTab.getValue("M_Product_ID")).intValue();
		MProduct product = MProduct.get(ctx, M_Product_ID);
		setAndTestASI(ctx, WindowNo, Env.isSOTrx(ctx, WindowNo), mTab, 
				"M_AttributeSetInstance_ID", product, M_ASI_ID);

		//	Check the locator selection - it may have changed
		if (mTab.getField("M_Locator_ID") != null) {	
			int M_AttributeSetInstance_ID =	Env.getContextAsInt(Env.getCtx(), WindowNo, Env.TAB_INFO, "M_AttributeSetInstance_ID");
			if (M_ASI_ID.intValue() == M_AttributeSetInstance_ID)
			{
				int selectedM_Locator_ID = Env.getContextAsInt(Env.getCtx(), WindowNo, Env.TAB_INFO, "M_Locator_ID");
				if (selectedM_Locator_ID != 0)
				{
					log.fine("Selected M_Locator_ID=" + selectedM_Locator_ID);
					mTab.setValue("M_Locator_ID", new Integer (selectedM_Locator_ID));
				}
			}
		}

		return "";
	}	//	attributeSetInstance
	/**
	 * Standard callout for M_AttributeSetInstanceTo_ID field.
	 * @param ctx
	 * @param WindowNo
	 * @param mTab
	 * @param mField
	 * @param value
	 * @return
	 */
	public String attributeSetInstanceTo (Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value)
	{
		if (isCalloutActive())
			return "";
		
		Integer M_ASITo_ID = 0;
		if (value != null)
			M_ASITo_ID = (Integer) value;
		
		MProduct product = MProduct.get(ctx, (Integer)mTab.getValue("M_Product_ID"));
		setAndTestASI(ctx, WindowNo, Env.isSOTrx(ctx, WindowNo), mTab, 
				"M_AttributeSetInstanceTo_ID", product, M_ASITo_ID);

		//	Check the locator selection - it may have changed
		if (mTab.getField("M_Locator_ID") != null) {	
			int M_AttributeSetInstanceTo_ID =	Env.getContextAsInt(Env.getCtx(), WindowNo, Env.TAB_INFO, "M_AttributeSetInstanceTo_ID");
			if (M_ASITo_ID.intValue() == M_AttributeSetInstanceTo_ID)
			{
				int selectedM_Locator_ID = Env.getContextAsInt(Env.getCtx(), WindowNo, Env.TAB_INFO, "M_LocatorTo_ID");
				if (selectedM_Locator_ID != 0)
				{
					log.fine("Selected M_LocatorTo_ID=" + selectedM_Locator_ID);
					mTab.setValue("M_LocatorTo_ID", new Integer (selectedM_Locator_ID));
				}
			}
		}

		return "";
	}	//	attributeSetInstanceTo

}	//	CalloutEngine
