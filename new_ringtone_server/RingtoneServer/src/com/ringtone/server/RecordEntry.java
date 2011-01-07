package com.ringtone.server;

import java.util.Date;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Key;

@PersistenceCapable(identityType=IdentityType.APPLICATION)
public class RecordEntry {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Key key;

	@Persistent
	private String toEmail;
	
	@Persistent
	private String uuid;
	
	@Persistent
	private String filename;
	
	@Persistent
	private Date date;

	public RecordEntry(String toEmail, String uuid, String filename) {
		this.toEmail = toEmail;
		this.uuid = uuid;
		this.filename = filename;
		this.date = new Date();
	}
}
