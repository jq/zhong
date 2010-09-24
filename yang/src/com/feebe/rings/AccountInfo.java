package com.feebe.rings;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.provider.Contacts.People;
import android.provider.ContactsContract.PhoneLookup;
import android.sax.StartElementListener;
import android.util.Log;

public class AccountInfo {
  
  public static final int GET_ACCOUNT_REQUEST_CODE = 123;
  public static String getAccountNameEclair(Context ctx) {
    AccountManager accountManager = AccountManager.get(ctx);
    Account[] accounts = accountManager.getAccountsByType("com.google");
    if(accounts.length > 0) {
      //// Log.e("Account:  ", accounts[0].name);
      return accounts[0].name;
    }
    return "noAccountInfo";
  }
  
  public static void getAccountName(Activity act) {
    com.google.android.googlelogin.GoogleLoginServiceHelper.getAccount(act, GET_ACCOUNT_REQUEST_CODE, true);
  }
  
  public static ArrayList<String> getFriendListEclair(Context ctx) {
    ArrayList<String> friendList = new ArrayList<String>();
    
    ContentResolver cr = ctx.getContentResolver();
    Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
    try {
      while (cursor.moveToNext()) {
        String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
  
        Cursor emailCursor = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
              null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=" + contactId, null, null);
        while (emailCursor.moveToNext()) {
          String email = emailCursor.getString(emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
          if (email!=null) {
            //// Log.e("add email elcair: ", email);
            friendList.add(email);
          }
        }
      }
    } catch (Exception e) {
      
    } finally {
      cursor.close();
    }
    return friendList;
  }
  
  public static ArrayList<String> getFriendList(Context ctx) {
    ArrayList<String> friendList = new ArrayList<String>();
    ContentResolver cr = ctx.getContentResolver();
    
    String columns[] = new String[]{People._ID, People.NAME};
    Cursor cursor = cr.query(People.CONTENT_URI, columns, null, null, People.NAME);
    if (cursor.moveToFirst()) {
      Cursor newCursor = null;
      do {
        //String name = cursor.getString(cursor.getColumnIndex(People.NAME));
        //String id = cursor.getString(cursor.getColumnIndex(People._ID));
        long peopleId = cursor.getLong(cursor.getColumnIndex(People._ID));
        
        String[] projection = new String[]{Contacts.ContactMethods._ID, Contacts.ContactMethods.KIND, Contacts.ContactMethods.DATA };
        newCursor = cr.query(Contacts.ContactMethods.CONTENT_URI, projection, Contacts.ContactMethods.PERSON_ID + "=\'" + peopleId + "\'", null, null);
        if(newCursor == null)
          continue;
        
        String email = "";
   
        if (newCursor.moveToFirst()) {
          email = newCursor.getString(newCursor.getColumnIndex(Contacts.ContactMethods.DATA));
        }
        if (email.length() > 0 && email.endsWith("gmail.com")) {
          //// Log.e("add email: ", email);
          friendList.add(email);
        }
        if (newCursor != null)
          newCursor.close();
        
      } while (cursor.moveToNext());
    }
    
    if (cursor != null)
      cursor.close();
    
    return friendList;
  }
  
  public static boolean isEclairOrLater() {
    return Build.VERSION.SDK.compareTo("5") >=0;
  }

}
