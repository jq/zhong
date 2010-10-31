package com.ringtone.server;


import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jdo.annotations.Embedded;
import javax.jdo.annotations.EmbeddedOnly;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

@PersistenceCapable(identityType=IdentityType.APPLICATION)
public class QueryEntry {
	
    @PrimaryKey
	@Persistent 
	private String key;
	
	@Persistent
	private int query_count;
	
	@Persistent 
	private int result_count;

	public QueryEntry(String key) {
		super();
		this.key = key;
	}

	public int getQuery_count() {
		return query_count;
	}

	public void setQuery_count(int queryCount) {
		query_count = queryCount;
	}

	public String getKey() {
		return key;
	}

	public int getResult_count() {
		return result_count;
	}

	public void setResult_count(int resultCount) {
		result_count = resultCount;
	}
	
}
