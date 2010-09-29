/*
 * Copyright 2010 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connect.facebook;

import com.connect.facebook.SessionEvents.AuthListener;
import com.connect.facebook.SessionEvents.LogoutListener;
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Facebook.DialogListener;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;


public class Login {
    
    private Facebook mFb;
    private Handler mHandler;
    private SessionListener mSessionListener = new SessionListener();
    private String[] mPermissions;
    
    private Context context;
    
    public static final String APP_ID = "161482820533218";
    

    public Login (final Facebook fb, final String[] permissions, final Context ctx) {
        mFb = fb;
        mPermissions = permissions;
        mHandler = new Handler();
        context = ctx;
        
        SessionEvents.addAuthListener(mSessionListener);
        SessionEvents.addLogoutListener(mSessionListener);
    }
    
    public void LoginLogoutFacebook () {
      if (mFb.isSessionValid()) {
        SessionEvents.onLogoutBegin();
        AsyncFacebookRunner asyncRunner = new AsyncFacebookRunner(mFb);
        asyncRunner.logout(context, new LogoutRequestListener());
      } else {
        mFb.authorize(context, APP_ID, mPermissions,new LoginDialogListener());
      }
    }
    
    public void LoginFacebook () {
      if (!mFb.isSessionValid()) {
        mFb.authorize(context, APP_ID, mPermissions,new LoginDialogListener());
      }
    }
    

    private final class LoginDialogListener implements DialogListener {
        public void onComplete(Bundle values) {
            SessionEvents.onLoginSuccess();
        }

        public void onFacebookError(FacebookError error) {
            SessionEvents.onLoginError(error.getMessage());
        }
        
        public void onError(DialogError error) {
            SessionEvents.onLoginError(error.getMessage());
        }

        public void onCancel() {
            SessionEvents.onLoginError("Action Canceled");
        }
    }
    
    private class LogoutRequestListener extends BaseRequestListener {
        public void onComplete(String response) {
            // callback should be run in the original thread, 
            // not the background thread
            mHandler.post(new Runnable() {
                public void run() {
                    SessionEvents.onLogoutFinish();
                }
            });
        }
    }
    
    private class SessionListener implements AuthListener, LogoutListener {
        
        public void onAuthSucceed() {
            SessionStore.save(mFb, context);
        }

        public void onAuthFail(String error) {
        }
        
        public void onLogoutBegin() {           
        }
        
        public void onLogoutFinish() {
            SessionStore.clear(context);
        }
    }
    
}
