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
import org.adempiere.controller.form.AttributeDialogInfoImpl;

/**
 * A null class that does nothing.  Used as a replacement for the 
 * AttributeEditorController before the editors are initialized.  The 
 * controller gets called a lot but isn't setup properly.  This class
 * can be used to avoid a bunch of null checks.
 * 
 * See https://refactoring.guru/introduce-null-object
 * 
 * @author mckayERP
 *
 */
public class NullAttributeEditorControllerImpl extends AbstractEditorController
        implements AttributeEditorController {

    private static final long serialVersionUID = -2745394320596663373L;

    @Override
    public void actionText() {
        // Does nothing
    }

    @Override
    public void actionButton() {
        // Does nothing
    }

    @Override
    public void enableControls() {
        // Does nothing
    }

    @Override
    public String getText() {
        // Does nothing
        return null;
    }

    @Override
    public String getAttributeWhere() {
        // Does nothing
        return null;
    }

    @Override
    public String getToolTipText() {
        // Does nothing
        return null;
    }

    @Override
    public void actionZoom() {
        // Does nothing
    }

    @Override
    public boolean isProductWindow() {
        // Does nothing
        return false;
    }

    @Override
    public void setAttributeWhere(String whereClause) {
        // Does nothing
    }

    @Override
    public void setText(String displayText) {
        // Does nothing
    }

    @Override
    public void setSearchOnly(boolean searchOnly) {
        // Does nothing
    }

    @Override
    public void setError() {
        // Does nothing
    }

    @Override
    public AttributeDialogInfo getAttributeDialogInfo() {

        return new AttributeDialogInfoImpl();

    }

}
