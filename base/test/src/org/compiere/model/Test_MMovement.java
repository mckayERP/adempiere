package org.compiere.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.adempiere.test.CommonUnitTestSetup;
import org.compiere.process.DocAction;
import org.compiere.util.CLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class Test_MMovement extends CommonUnitTestSetup {

    @BeforeEach
    void setUp() throws Exception {

    }

    @Test
    final void testCompleteIt_callStorageRules() {

        CLogger logMock = mock(CLogger.class);
        ModelValidationEngine engineMock = mock(ModelValidationEngine.class);
        
        MMovementLine lineMock = mock(MMovementLine.class);
        
        MMovement moveMock = mock(MMovement.class);
        doCallRealMethod().when(moveMock).completeIt();
        moveMock.justPrepared = true;
        moveMock.log = logMock;
        doReturn(engineMock).when(moveMock).getModelValidationEngine();
        doReturn(new MMovementConfirm[] {}).when(moveMock).getConfirmations(anyBoolean());
        doReturn(true).when(moveMock).isApproved();
        doReturn(new MMovementLine[] {lineMock}).when(moveMock).getLines(anyBoolean());
        Mockito.doNothing().when(moveMock).setDefiniteDocumentNo();
        
        String status = moveMock.completeIt();
        
        assertEquals(DocAction.STATUS_Completed, status);
        

    }

}
