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
package org.adempiere.controller.ed;

import org.adempiere.controller.form.AttributeDialogInfo;
import org.compiere.model.MQuery;
import org.compiere.swing.CButton;
import org.compiere.swing.CEditor;
import org.compiere.swing.CTextField;

public interface AttributeEditor extends CEditor {
    
    public static final String DEFAULT_COLUMN_NAME = "M_AttributeSetInstance_ID";
    
    void actionSearch();

    void enableButton(boolean isEnabled);

    void enableTextField(boolean isEnabled);

    int getAttributeSetInstanceFromDialog();

    String getAttributeSetInstanceNameFromDialog();

    boolean isAttributeDialogChanged();

    void openAttributeDialog(AttributeDialogInfo info);

    void requestFocus();

    void setAndBindValue(Object value);

    void setDefaultCursor();

    void setText(String textToSet);

    void setWaitCursor();

    void warn(String message);

    void zoomToWindow(MQuery zoomQuery, int windowId, int windowNo);

}
