package org.compiere.grid;

/**
 * A simple callback interface that allows the SimpleLookup to
 * refresh the data from the source.  The columnName, set when 
 * creating the SimpleLookup, can be used to identify the required
 * data in cases where multiple lookups are generated within a 
 * single context. 
 * 
 * @author mckayERP
 *
 */
public interface SimpleLookupCallback {
	
	/**
	 * Returns a String array of entries, used to populate a 
	 * combo box type editor.  This function is called by the 
	 * control when the data needs to be refreshed.
	 * 
	 * @param columnName - a String name set when the SimpleLookup is created. It 
	 * can be used by the implementing class to distinguish which lookup is requesting
	 * the data.
	 *  
	 * @return the data as a String array.
	 */
	String [] getLookupData(String columnName);
}

