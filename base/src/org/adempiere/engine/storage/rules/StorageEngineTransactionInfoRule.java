package org.adempiere.engine.storage.rules;

public interface StorageEngineTransactionInfoRule<P, L, O> {
	
	public boolean matches(P parent, L line);
	
	public O process(P parent, L line);

}
