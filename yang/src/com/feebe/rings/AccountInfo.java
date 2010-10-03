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
  public static final int GET_ACCOUNT_FOR_FACEBOOK_REQUEST_CODE = 124;
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
  
  public static void getAccountNameFacebook(Activity act) {
    com.google.android.googlelogin.GoogleLoginServiceHelper.getAccount(act, GET_ACCOUNT_FOR_FACEBOOK_REQUEST_CODE, true);
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
  
}
