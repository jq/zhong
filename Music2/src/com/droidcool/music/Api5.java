package com.droidcool.music;

import android.app.Notification;

public class Api5 {
    public final static void startForeground(final MediaPlaybackService service, int id, Notification notification) {
        service.startForeground(id, notification);
    }
    public final static void stopForeground(final MediaPlaybackService service, boolean removeNotification) {
        service.stopForeground(removeNotification);
    }
}
