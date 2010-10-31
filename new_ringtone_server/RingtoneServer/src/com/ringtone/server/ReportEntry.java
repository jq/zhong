package com.ringtone.server;

import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

public class ReportEntry {
    @PrimaryKey
	@Persistent 
	private String key;
	
	@Persistent
	private int query_count;
}
