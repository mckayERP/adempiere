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
package org.adempiere.controller.form;

public class AttributeDialogInfoImpl implements AttributeDialogInfo {

    private int attributeSetInstanceId = 0;
    private int productId = 0;
    private int bpartnerId = 0;
    private int columnId = 0;
    private int windowNo = 0;
    private int dialogWindowNo = 0;
    private boolean isReadWrite = false;
    private boolean isProductWindow = false;
    private String title = "Title not set";

    @Override
    public int getWindowNo() {

        return windowNo;

    }

    @Override
    public void setWindowNo(int windowNo) {

        this.windowNo = windowNo;

    }

    @Override
    public AttributeDialogInfo withWindowNo(int windowNo) {

        setWindowNo(windowNo);
        return this;

    }

    public String getTitle() {

        return title;

    }

    public void setTitle(String title) {

        this.title = title;

    }

    public int getAttributeSetInstanceId() {

        return attributeSetInstanceId;

    }

    public void setAttributeSetInstanceId(int attributeSetInstanceId) {

        this.attributeSetInstanceId = attributeSetInstanceId;

    }

    @Override
    public AttributeDialogInfo
            withAttributeSetInstanceId(int attributeSetInstanceId) {

        setAttributeSetInstanceId(attributeSetInstanceId);
        return this;

    }

    @Override
    public int getProductId() {

        return productId;

    }

    @Override
    public void setProductId(int productId) {

        this.productId = productId;

    }

    @Override
    public AttributeDialogInfo withProductId(int productId) {

        setProductId(productId);
        return this;

    }

    @Override
    public int getBpartnerId() {

        return bpartnerId;

    }

    @Override
    public void setBpartnerId(int bpartnerId) {

        this.bpartnerId = bpartnerId;

    }
    
    @Override
    public AttributeDialogInfo withBpartnerId(int bpartnerId) {

        this.bpartnerId = bpartnerId;
        return this;
    }
    
    @Override
    public int getColumnId() {

        return columnId;

    }

    @Override
    public void setColumnId(int columnId) {

        this.columnId = columnId;

    }

    @Override
    public AttributeDialogInfo withColumnId(int id) {
    
        columnId = id;
        return this;
    
    }

    @Override
    public String toString() {

        return "M_AttributeSetInstance_ID=" + attributeSetInstanceId
                + ", M_Product_ID=" + productId
                + ", C_BPartner_ID=" + bpartnerId
                + ", Column=" + columnId
                + ", windowNo=" + windowNo
                + ", title=" + title;

    }

    @Override
    public boolean isProductWindow() {

        return isProductWindow;

    }

    @Override
    public void setProductWindow(boolean isProductWindow) {

        this.isProductWindow = isProductWindow;

    }

    @Override
    public AttributeDialogInfo withProductWindow(boolean isProductWindow) {

        setProductWindow(isProductWindow);
        return this;

    }

    @Override
    public boolean isReadWrite() {
    
        return isReadWrite;
    
    }

    @Override
    public void setReadWrite(boolean isReadWrite) {
    
        this.isReadWrite = isReadWrite;
    
    }

    @Override
    public AttributeDialogInfo withReadWrite(boolean isReadWrite) {
    
        setReadWrite(isReadWrite);
        return this;
    
    }

    @Override
    public int getDialogWindowNo() {

        return dialogWindowNo;

    }

    @Override
    public void setDialogWindowNo(int windowNo) {

        dialogWindowNo = windowNo;

    }

    @Override
    public AttributeDialogInfo withDialogWindowNo(int windowNo) {

        dialogWindowNo = windowNo;
        return this;

    }

    @Override
    public AttributeDialogInfo withTitle(String title) {

        setTitle(title);
        return this;

    }


}
