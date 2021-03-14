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

package org.compiere.process;



/** Generated Process for (Storage Cleanup)
 *  @author ADempiere (generated) 
 *  @version Release 3.9.3
 */
public abstract class StorageCleanupAbstract extends SvrProcess {
	/** Process Value 	*/
	private static final String VALUE_FOR_PROCESS = "M_StorageCleanup";
	/** Process Name 	*/
	private static final String NAME_FOR_PROCESS = "Storage Cleanup";
	/** Process Id 	*/
	private static final int ID_FOR_PROCESS = 325;
	/**	Parameter Name for Only Report	*/
	public static final String ISONLYREPORT = "IsOnlyReport";
	/**	Parameter Name for Correct malformed Attribute Set Instance entries	*/
	public static final String ISCORRECTBADASIVALUES = "IsCorrectBadASIValues";
	/**	Parameter Name for Rebuild Storage	*/
	public static final String ISREBUILDSTORAGE = "IsRebuildStorage";
	/**	Parameter Name for Correct missing material allocations	*/
	public static final String ISCORRECTMATERIALALLOCATIONS = "IsCorrectMaterialAllocations";
	/**	Parameter Name for Correct qty ordered/reserved	*/
	public static final String ISCORRECTQTYORDEREDRESERVED = "IsCorrectQtyOrderedReserved";
	/**	Parameter Name for Cover negative quantities	*/
	public static final String ISCOVERNEGATIVEQTY = "IsCoverNegativeQty";
	/**	Parameter Name for Allow move between locators	*/
	public static final String ISALLOWMOVEBETWEENLOCATORS = "IsAllowMoveBetweenLocators";
	/**	Parameter Name for Document Type	*/
	public static final String C_DOCTYPE_ID = "C_DocType_ID";
	/**	Parameter Value for Only Report	*/
	private boolean isOnlyReport;
	/**	Parameter Value for Correct malformed Attribute Set Instance entries	*/
	private boolean isCorrectBadASIValues;
	/**	Parameter Value for Rebuild Storage	*/
	private boolean isRebuildStorage;
	/**	Parameter Value for Correct missing material allocations	*/
	private boolean isCorrectMaterialAllocations;
	/**	Parameter Value for Correct qty ordered/reserved	*/
	private boolean isCorrectQtyOrderedReserved;
	/**	Parameter Value for Cover negative quantities	*/
	private boolean isCoverNegativeQty;
	/** Parameter Value for Allow move between locators */
	private boolean isAllowMoveBetweenLocators;
	/**	Parameter Value for Document Type	*/
	private int docTypeId;

	@Override
	protected void prepare() {
		isOnlyReport = getParameterAsBoolean(ISONLYREPORT);
		isCorrectBadASIValues = getParameterAsBoolean(ISCORRECTBADASIVALUES);
		isRebuildStorage = getParameterAsBoolean(ISREBUILDSTORAGE);
		isCorrectMaterialAllocations = getParameterAsBoolean(ISCORRECTMATERIALALLOCATIONS);
		isCorrectQtyOrderedReserved = getParameterAsBoolean(ISCORRECTQTYORDEREDRESERVED);
		isCoverNegativeQty = getParameterAsBoolean(ISCOVERNEGATIVEQTY);
		isAllowMoveBetweenLocators = getParameterAsBoolean(ISALLOWMOVEBETWEENLOCATORS);
		docTypeId = getParameterAsInt(C_DOCTYPE_ID);
	}

	/**	 Getter Parameter Value for Only Report	*/
	protected boolean isOnlyReport() {
		return isOnlyReport;
	}

	/**	 Setter Parameter Value for Only Report	*/
	protected void setIsOnlyReport(boolean isOnlyReport) {
		this.isOnlyReport = isOnlyReport;
	}

	/**	 Getter Parameter Value for Correct malformed Attribute Set Instance entries	*/
	protected boolean isCorrectBadASIValues() {
		return isCorrectBadASIValues;
	}

	/**	 Setter Parameter Value for Correct malformed Attribute Set Instance entries	*/
	protected void setIsCorrectBadASIValues(boolean isCorrectBadASIValues) {
		this.isCorrectBadASIValues = isCorrectBadASIValues;
	}

	/**	 Getter Parameter Value for Rebuild Storage	*/
	protected boolean isRebuildStorage() {
		return isRebuildStorage;
	}

	/**	 Setter Parameter Value for Rebuild Storage	*/
	protected void setIsRebuildStorage(boolean isRebuildStorage) {
		this.isRebuildStorage = isRebuildStorage;
	}

	/**	 Getter Parameter Value for Correct missing material allocations	*/
	protected boolean isCorrectMaterialAllocations() {
		return isCorrectMaterialAllocations;
	}

	/**	 Setter Parameter Value for Correct missing material allocations	*/
	protected void setIsCorrectMaterialAllocations(boolean isCorrectMaterialAllocations) {
		this.isCorrectMaterialAllocations = isCorrectMaterialAllocations;
	}

	/**	 Getter Parameter Value for Correct qty ordered/reserved	*/
	protected boolean isCorrectQtyOrderedReserved() {
		return isCorrectQtyOrderedReserved;
	}

	/**	 Setter Parameter Value for Correct qty ordered/reserved	*/
	protected void setIsCorrectQtyOrderedReserved(boolean isCorrectQtyOrderedReserved) {
		this.isCorrectQtyOrderedReserved = isCorrectQtyOrderedReserved;
	}

	/**	 Getter Parameter Value for Allow move between locators	*/
	protected boolean isAllowMoveBetweenLocators() {
		return isAllowMoveBetweenLocators;
	}

	/**	 Setter Parameter Value for Allow move between locators	*/
	protected void setIsAllowMoveBetweenLocators(boolean isAllowMoveBetweenLocators) {
		this.isAllowMoveBetweenLocators = isAllowMoveBetweenLocators;
	}

	/**	 Getter Parameter Value for Cover negative quantities	*/
	protected boolean isCoverNegativeQty() {
		return isCoverNegativeQty;
	}

	/**	 Setter Parameter Value for Cover negative quantities	*/
	protected void setIsCoverNegativeQty(boolean isCoverNegativeQty) {
		this.isCoverNegativeQty = isCoverNegativeQty;
	}

	/**	 Getter Parameter Value for Document Type	*/
	protected int getDocTypeId() {
		return docTypeId;
	}

	/**	 Setter Parameter Value for Document Type	*/
	protected void setDocTypeId(int docTypeId) {
		this.docTypeId = docTypeId;
	}

	/**	 Getter Parameter Value for Process ID	*/
	public static final int getProcessId() {
		return ID_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Value	*/
	public static final String getProcessValue() {
		return VALUE_FOR_PROCESS;
	}

	/**	 Getter Parameter Value for Process Name	*/
	public static final String getProcessName() {
		return NAME_FOR_PROCESS;
	}
}