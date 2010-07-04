package com.ringdroid;

import android.net.Uri;

public class GenContactUri {
	public static Uri getContextUri(String contactId) {
			return Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId);
	}
}
