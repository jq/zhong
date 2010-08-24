package com.happy.life;

import com.droidcool.music.MediaPlaybackActivity;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.R;
import com.util.DownloadActivity;
import com.util.DownloadInfo;
import com.util.Utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class MusicDownloadActivity extends DownloadActivity {
  private static final int MENU_PLAY = Menu.FIRST + 6;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    getListView().setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View v,
              int position, long id) {
          if (mData == null || position >= mData.size())
              return;
          DownloadInfo d = mData.get(position);
          if (d == null) {
              Utils.D("No bound download info.");
              return;
          }
          // Default action.
          int state = d.getState();
          if (state == Downloader.COMPLETE) {
              playDownloadedMusic(d);
          }
          if (d.showToastForLongPress()) {
            Toast.makeText(MusicDownloadActivity.this,
                getString(R.string.music_option_prompt), Toast.LENGTH_LONG).show();
          }           
              
      }
    });
    
    getListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {

      @Override
      public void onCreateContextMenu(ContextMenu menu, View v,
              ContextMenuInfo menuInfo) {
          if (mData == null)
              return;
          
          if (!(menuInfo instanceof AdapterContextMenuInfo))
              return;
          
          int position = ((AdapterContextMenuInfo) menuInfo).position;
          if (position >= mData.size())
              return;
          
          DownloadInfo d = mData.get(position);
          if (d == null) {
              Utils.D("No bound download info.");
              return;
          }
          int state = d.getState();
          if (state == Downloader.COMPLETE) {
              menu.add(0, MENU_PLAY, 0, R.string.play);
              menu.add(0, MENU_CLEAR, 0, R.string.clear);
          } else if (state == Downloader.CORRUPT_FILE ||
                     state == Downloader.ABORTED ||
                     state == Downloader.GAVE_UP ||
                     state == Downloader.RECOVERY_FAILED) {
              menu.add(0, MENU_RETRY, 0, R.string.retry);
              menu.add(0, MENU_DELETE, 0, R.string.delete);
          } else if (state == Downloader.DOWNLOADING ||
                     state == Downloader.CONNECTING ||
                     state == Downloader.REMOTE_QUEUED ||
                     state == Downloader.SAVING ||
                     state == Downloader.IDENTIFY_CORRUPTION ||
                     state == Downloader.WAITING_FOR_RESULTS ||
                     state == Downloader.WAITING_FOR_RETRY ||
                     state == Downloader.WAITING_FOR_CONNECTIONS ||
                     state == Downloader.ITERATIVE_GUESSING) {
              if (!d.isScheduled()) {
                  menu.add(0, MENU_RETRY, 0, R.string.retry);
                  menu.add(0, MENU_DELETE, 0, R.string.delete);
              } else {
                  menu.add(0, MENU_PAUSE, 0, R.string.pause);
                  menu.add(0, MENU_STOP, 0, R.string.stop);
              }
          } else if (state == Downloader.QUEUED) {
              // menu.add(0, MENU_STOP, 0, R.string.stop);
          } else if (state == Downloader.PAUSED ||
                     state == Downloader.WAITING_FOR_USER) {
              menu.add(0, MENU_RESUME, 0, R.string.resume);
              menu.add(0, MENU_DELETE, 0, R.string.delete);
          } else if (state == Downloader.DISK_PROBLEM){
              menu.add(0, MENU_DELETE, 0, R.string.delete);
          } else if (d.pendingFailed()) {
              menu.add(0, MENU_RETRY, 0, R.string.retry);
              menu.add(0, MENU_DELETE, 0, R.string.delete);
          }
      }
      
    });
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    super.onContextItemSelected(item);
    AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item
            .getMenuInfo();
    DownloadInfo d = mData.get(menuInfo.position);
    switch (item.getItemId()) {
    case MENU_CLEAR: {
        if (mDownloadService != null) {
            mDownloadService.removeDownload(d);
            synchronized(d) {
              d.setScheduled(false);
              mDownloadService.notifyChanged();
            }
        }
        break;
    }
    case MENU_PLAY: {
        playDownloadedMusic(d);
        break;
    }
    case MENU_STOP: {
        synchronized(d) {
          d.stopDownload();
          if (mDownloadService != null)
            mDownloadService.notifyChanged();     
        }
        mAdapter.notifyDataSetChanged();
        break;
    }
    case MENU_RESUME: {
        if (mDownloadService != null) {
          mDownloadService.resumeDownload(d);
          mAdapter.notifyDataSetChanged();
        }
        break;
    }
    case MENU_DELETE: {
        if (mDownloadService != null) {
            mDownloadService.removeDownload(d);
            d.deleteDownload();
            synchronized (d) {
              // Force existing thread to stop.
              mDownloadService.notifyChanged();
            }
        }
        break;
    }
    case MENU_PAUSE: {
      synchronized(d) {
        d.pauseDownload();
      }
      mAdapter.notifyDataSetChanged();
      break;
    }
    case MENU_RETRY: {
        if (mDownloadService != null) {
            mAdapter.notifyDataSetChanged();
            mDownloadService.retryDownload(d);
        }
        break;
    }
    }

    return true;
  }
  
  private void playDownloadedMusic(final DownloadInfo info) {
    if (info == null)
        return;

    try {
        Intent intent = new Intent(this, MediaPlaybackActivity.class);
        intent.setDataAndType(Uri.parse("file://" + info.getTarget()), "audio");
          
        startActivity(intent);
    } catch (android.content.ActivityNotFoundException e) {
        e.printStackTrace();
        Toast.makeText(MusicDownloadActivity.this,
                getString(R.string.no_playing_activity), Toast.LENGTH_LONG).show();
    }
}

}
