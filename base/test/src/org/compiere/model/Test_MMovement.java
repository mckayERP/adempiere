/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2021 ADempiere Foundation, All Rights Reserved.         *
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
package org.compiere.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.adempiere.engine.storage.StorageEngine;
import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class Test_MMovement extends CommonUnitTestSetup {

    @BeforeEach
    void setUp() throws Exception {

    }

    @Test
    final void testCompleteIt_callsStorageRules() {

        CLogger logMock = mock(CLogger.class);
        ModelValidationEngine engineMock = mock(ModelValidationEngine.class);
        
        MMovementLine lineMock = mock(MMovementLine.class);
        StorageEngine seMock = mock(StorageEngine.class);
        MMovement moveMock = mock(MMovement.class);
        doCallRealMethod().when(moveMock).completeIt();
        moveMock.justPrepared = true;
        moveMock.log = logMock;
        doReturn(engineMock).when(moveMock).getModelValidationEngine();
        doReturn(new MMovementConfirm[] {}).when(moveMock).getConfirmations(anyBoolean());
        doReturn(true).when(moveMock).isApproved();
        doReturn(new MMovementLine[] {lineMock}).when(moveMock).getLines(anyBoolean());
        doReturn(seMock).when(moveMock).getStorageEngine();
        doNothing().when(moveMock).setDefiniteDocumentNo();
        
        String status = moveMock.completeIt();
        
        assertEquals(DocAction.STATUS_Completed, status);
        verify(seMock).applyStorageRules(lineMock);

    }

}
