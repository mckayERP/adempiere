/******************************************************************************
 * Product: Posterita Ajax UI 												  *
 * Copyright (C) 2007 Posterita Ltd.  All Rights Reserved.                    *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Posterita Ltd., 3, Draper Avenue, Quatre Bornes, Mauritius                 *
 * or via info@posterita.org or http://www.posterita.org/                     *
 *****************************************************************************/

package org.adempiere.webui.component;

import java.beans.PropertyChangeEvent;
import java.beans.VetoableChangeListener;
import java.util.List;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.ValueChangeListener;
import org.compiere.model.GridField;
import org.compiere.swing.IComboBox;
import org.compiere.swing.ILabel;
import org.zkoss.zul.Comboitem;

/**
 *
 * @author  <a href="mailto:agramdass@gmail.com">Ashley G Ramdass</a>
 * @date    Feb 25, 2007
 * @version $Revision: 0.10 $
 */
public class Combobox extends org.zkoss.zul.Combobox implements IComboBox
{
    /**
	 * 
	 */
	private static final long serialVersionUID = -6278632602577424842L;
    private boolean mandatory;
    private ILabel label;

	public void setEnabled(boolean enabled)
    {
        this.setDisabled(!enabled);
    }
    
    public Comboitem appendItem(String label) 
    {
        ComboItem item = new ComboItem(label);
        item.setParent(this);
        return item;
    }

	public boolean isEnabled() {
		return !isDisabled();
	}
	
	/**
	 * remove all items, to ease porting of swing form
	 */
	public void removeAllItems() {
		int cnt = getItemCount();
		for (int i = cnt - 1; i >=0; i--) {
			removeItemAt(i);
		}
	}

	public void appendItem(String name, Object value) {

		ComboItem item;
		
		if (name == "")
		{
			//  Null items are skipped in Comboitem.class setLable() on creation
			name = " ";
			item = new ComboItem(name, value);
		}
		else
			item = new ComboItem(name, value);
		
		this.appendChild(item);
	}
	
	 /** 
     * Set selected item for the list box based on the value of list item
     * set selected to none if no item found matching the value given or 
     * value is null
     * @param value Value of ListItem to set as selected
     */
    public void setValue(Object value)
    {
        setSelectedItem(null);
        
        if (value == null)
        {
            return ;
        }
        
        List<Comboitem> items = getItems();
        for (Comboitem item : items)
        {
        	if (value.getClass() != item.getValue().getClass()) {
        		// if the classes of value and item are different convert both to String
        		String stringValue = value.toString();
        		String stringItem = item.getValue().toString();
                if (stringValue.equals(stringItem))
                {
                    setSelectedItem(item);
                    break;
                }
        	} else {
                if (value.equals(item.getValue()))
                {
                    setSelectedItem(item);
                    break;
                }
        	}
        }
    }
    
    /**
     * 
     * @param value
     * @return boolean
     */
    public boolean isSelected(Object value) 
    {
    	if (value == null)
    		return false;
    	
    	Comboitem item = getSelectedItem();
    	if (item == null)
    		return false;
    	
    	return item.getValue().equals(value);
    }
    
    /** Returns RS_NO_WIDTH|RS_NO_HEIGHT.
	 */
	protected int getRealStyleFlags() {
		return super.getRealStyleFlags() & 0x0006;
	}

    @Override
    public void setReadWrite(boolean rw) {

        setReadonly(!rw);
    
    }

    @Override
    public boolean isReadWrite() {

        return !isReadonly();

    }

    @Override
    public void setMandatory(boolean mandatory) {

        this.mandatory = mandatory;
        
    }

    @Override
    public boolean isMandatory() {

        return mandatory;

    }

    @Override
    public void setBackground(boolean error) {

        // Do Nothing
        
    }

    @Override
    public String getDisplay() {

        return Optional.ofNullable((ComboItem) getSelectedItem())
                .map(ComboItem::getLabel)
                .orElse("");

    }

    @Override
    public void addVetoableChangeListener(VetoableChangeListener listener) {

        throw new AdempiereException("Method not Implemented");

    }

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {

        throw new AdempiereException("Method not Implemented");

    }

    @Override
    public GridField getField() {

        throw new AdempiereException("Method not Implemented");

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

        throw new AdempiereException("Method not Implemented");
        
    }

    @Override
    public boolean setVisible(boolean visible) {

        // TODO Auto-generated method stub
        return false;

    }

    @Override
    public void setEditable(boolean b) {

        // TODO Auto-generated method stub
        

    }

    @Override
    public Object getItemAt(int i) {

        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public void addItem(Object item) {

        // TODO Auto-generated method stub
        

    }

    @Override
    public void setSelectedItem(Object item) {

        // TODO Auto-generated method stub
        

    }

    @Override
    public void setVisibleState(boolean visible) {

        setVisible(visible);

    }

    @Override
    public void setLable(ILabel label) {

        this.label = label;
        
    }
}