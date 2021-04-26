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
package org.compiere.swing;

import java.awt.Color;

public interface ILabel {

    /**
     * 	Set Font to Bold
     *	@param bold true bold false normal
     */
    void setFontBold(boolean bold); //	setFontBold

    /**************************************************************************
     *  Set label text - if it includes &, the next character is the Mnemonic
     *  @param mnemonicLabel Label containing Mnemonic
     */
    void setText(String mnemonicLabel); //  setText

    /**
     *  Set ReadWrite
     *  @param rw enabled
     */
    void setReadWrite(boolean rw); //  setReadWrite

     /**
     * @return Returns the savedMnemonic.
     */
    char getSavedMnemonic(); //	getSavedMnemonic

    /**
     * @param savedMnemonic The savedMnemonic to set.
     */
    void setSavedMnemonic(char savedMnemonic); //	getSavedMnemonic

    void setToolTipText(String description);

    void setLabelFor(Object editor);

}