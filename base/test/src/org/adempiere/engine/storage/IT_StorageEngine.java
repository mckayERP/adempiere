/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2020 ADempiere Foundation, All Rights Reserved.         *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this license, you may reach us    *
 * or via info@adempiere.net or http://www.adempiere.net/license.html         *
 *****************************************************************************/
package org.adempiere.engine.storage;

import static org.adempiere.engine.storage.StorageEngine.applyStorageRules;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.adempiere.engine.IDocumentLine;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.test.CommonGWSetup;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("Storage")
@Tag("StorageEngine")
@ExtendWith(MockitoExtension.class)
class IT_StorageEngine extends CommonGWSetup{

	@Test
	void checkTranslations() {
		
		assertNotEquals(IStorageEngine.MSG_NO_MATCHING_RULE, 
				Msg.getMsg(ctx, IStorageEngine.MSG_NO_MATCHING_RULE),
				"Not translated");
		assertNotEquals(IStorageEngine.MSG_UNABLE_TO_APPLY_RULE, 
				Msg.getMsg(ctx, IStorageEngine.MSG_UNABLE_TO_APPLY_RULE),
				"Not translated");
		assertNotEquals(IStorageEngine.MSG_UNABLE_TO_UPDATE_INVENTORY, 
				Msg.getMsg(ctx, IStorageEngine.MSG_UNABLE_TO_UPDATE_INVENTORY),
				"Not translated");
		
	}
	
	@Test
	void getStorageEngine() {
		
		StorageEngine engine = StorageEngine.getStorageEngine();
		assertNotNull(engine, "getStorageEngine returned null");
		
	}

	@Test
	void getCLogger() {

		StorageEngine engine = StorageEngine.getStorageEngine();
		CLogger log = engine.getCLogger();
		assertNotNull(log, "getCLogger returned null");
		
	}

	@Test
	void applyStorageRules_ifPassedNullThrowsException() {
		
	    Exception exception = assertThrows(AdempiereException.class, () -> {
	        applyStorageRules(null);
	    });
	 
	    String expectedMessage = Msg.getMsg(Env.getCtx(), 
	    		IStorageEngine.MSG_NO_MATCHING_RULE) + " " + null;
	    String actualMessage = exception.getMessage();
	 
	    assertEquals(expectedMessage, actualMessage, "Message not as expected");
        
	}

	@Test
	void applyStorageRules_ifNoMatchThrowsException() {
		
		IDocumentLine lineMock = mock(IDocumentLine.class);
		
	    Exception exception = assertThrows(AdempiereException.class, () -> {
	        applyStorageRules((IDocumentLine) lineMock);
	    });
	 
	    String expectedMessage = Msg.getMsg(Env.getCtx(), 
	    		IStorageEngine.MSG_NO_MATCHING_RULE) + " " + lineMock;
	    String actualMessage = exception.getMessage();
	 
	    assertEquals(expectedMessage, actualMessage, "Message not as expected");
	    
	}

}
