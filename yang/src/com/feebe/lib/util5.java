package com.feebe.lib;

import java.net.URISyntaxException;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;

// for sdk 5 and above
public class util5 {
  
  public static void loadBrowser(Activity act, String url) {
    Intent intent;
    try {
      intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
    } catch (URISyntaxException ex) {
      return;
    }
    // sanitize the Intent, ensuring web pages can not bypass browser
    // security (only access to BROWSABLE activities).
    intent.addCategory(Intent.CATEGORY_BROWSABLE);
    intent.setComponent(null);
    try {
        act.startActivityIfNeeded(intent, -1);
    } catch (ActivityNotFoundException ex) {
        // ignore the error. If no application can handle the URL,
        // eg about:blank, assume the browser can handle it.
    }
  }
  
}
