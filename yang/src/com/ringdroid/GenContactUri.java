package com.ringdroid;

import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Contacts.People;

public class GenContactUri {
	public static Uri getContextUri(String contactId) {
		if (Integer.parseInt(Build.VERSION.SDK) >= 5)
			return Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId);
		else 
			return Uri.withAppendedPath(People.CONTENT_URI, contactId);
	}
}
