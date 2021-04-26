package org.adempiere.controller.form;

import java.io.Serializable;

public interface AttributeDialogInfo extends Serializable {
    
    String toString();

    String getTitle();
    void setTitle(String title);
    AttributeDialogInfo withTitle(String title);
    
    int getWindowNo();
    void setWindowNo(int windowNo);    
    AttributeDialogInfo withWindowNo(int windowNo);

    int getDialogWindowNo();
    void setDialogWindowNo(int windowNo);    
    AttributeDialogInfo withDialogWindowNo(int windowNo);

    int getProductId();
    void setProductId(int productId);
    AttributeDialogInfo withProductId(int productId);

    boolean isProductWindow();
    void setProductWindow(boolean isProductWindow);
    AttributeDialogInfo withProductWindow(boolean isProductWindow);

    int getAttributeSetInstanceId();
    void setAttributeSetInstanceId(int attributeSetInstanceId);
    AttributeDialogInfo withAttributeSetInstanceId(int attributeSetInstanceId);

    int getColumnId();
    void setColumnId(int columnId);
    AttributeDialogInfo withColumnId(int columnId);

    int getBpartnerId();
    void setBpartnerId(int bpartnerId);
    AttributeDialogInfo withBpartnerId(int bpartnerId);

    boolean isReadWrite();
    void setReadWrite(boolean isReadWrite);
    AttributeDialogInfo withReadWrite(boolean isReadWrite);
}
