/******************************************************************************
 * Product: ADempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2006-2019 ADempiere Foundation, All Rights Reserved.         *
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

package org.adempiere.plaf;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;

import org.compiere.swing.CButton;
import org.compiere.swing.CTextField;
import org.compiere.util.CLogger;
import org.compiere.util.Env;

/**
 *  ADempiere Number UI.
 *  Provides support for a Number editor.
 *
 *  @author Michael McKay, mckayERP@gmail.com
 *  
 *  @version 3.9.4
 */
public class AdempiereNumberUI extends AdempiereEditorAbstractUI
{
	
	CLogger log = CLogger.getCLogger(AdempiereNumberUI.class);
	
	/**	Number of Columns (12)		*/
	public final static int SIZE = 12;

	/**
	 *  Create UI
	 *  @param c
	 *  @return new instance of AdempiereDateUI
	 */
	public static ComponentUI createUI(JComponent c)
	{
		return new AdempiereNumberUI();
	}   //  CreateUI

    @Override
    protected ImageIcon getButtonIcon() {
    	return Env.getImageIcon("Calculator10.gif");
    }
    
    @Override
    protected String getPhantomString() {

    	return "10000.00";
    	
    }
    
    /**
     * Create the editor as a CTextField with trailing justification.
     */
    @Override
    protected JTextComponent createEditorComponent() {
    	
    	CTextField editor = new CTextField(SIZE);
		editor.setHorizontalAlignment(JTextField.TRAILING);
    	return editor;
    	
    }
    
	@Override
    protected CButton createButton() {
		
    	return new CButton();
    	
    }

}   //  AdempiereNumberUI
