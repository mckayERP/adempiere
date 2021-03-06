package org.adempiere.engine.storage;

import org.adempiere.engine.IDocumentLine;
import org.compiere.model.PO;

public interface IStorageEngine {

	//  TODO Translate
	String MSG_NO_MATCHING_RULE = "StorageEngine_NoMatchingRule";
	String MSG_UNABLE_TO_APPLY_RULE = "StorageEngine_UnableToApplyStorageRules";
	String MSG_UNABLE_TO_UPDATE_INVENTORY = "StorageEngine_UnableToUpdateInventory";

	String applyStorageRules(PO parent, IDocumentLine docLine);

}