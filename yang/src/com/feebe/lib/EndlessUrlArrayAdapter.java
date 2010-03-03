package com.feebe.lib;

import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class EndlessUrlArrayAdapter<T, W> extends UrlArrayAdapter<T, W> {
  public static int Throbber;
  public static int ThrobberImg;
  public static int ThrobberViewRes;
  private long expire_;
  public EndlessUrlArrayAdapter(Context context, int resource, long expire) {
    super(context, resource);
    expire_ = expire;
  }
  private View pendingView=null;
  //private int pendingPosition=-1;
  protected boolean keepOnAppending=true;

  /**
   * How many items are in the data set represented by this
   * Adapter.
   */
 @Override
 public int getCount() {
   if (keepOnAppending) {
     return(super.getCount()+1);   // one more for "pending"
   }
   
   return(super.getCount());
 }
 private RotateAnimation rotate=null;

 protected View getPendingView(ViewGroup parent) {
   View row= mInflater.inflate(ThrobberViewRes, parent, false);
   
   ImageView child = (ImageView)row.findViewById(Throbber);
   //child.setImageResource(ThrobberImg);
   if (rotate == null) {
     rotate=new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,
         0.5f, Animation.RELATIVE_TO_SELF,
         0.5f);
     rotate.setDuration(600);
     rotate.setRepeatMode(Animation.RESTART);
     rotate.setRepeatCount(Animation.INFINITE);
   }
   child.startAnimation(rotate);
   
   return(row);
 }

 /**
  * Get a View that displays the data at the specified
  * position in the data set. In this case, if we are at
  * the end of the list and we are still in append mode,
  * we ask for a pending view and return it, plus kick
  * off the background task to append more data to the
  * wrapped adapter.
  * @param position Position of the item whose data we want
  * @param convertView View to recycle, if not null
  * @param parent ViewGroup containing the returned View
  */
@Override
public View getView(int position, View convertView,
                    ViewGroup parent) {
  if (position==super.getCount() &&
      keepOnAppending) {
    if (pendingView==null) {
      pendingView=getPendingView(parent);
      //pendingPosition=position;
    }
    new AppendTask(expire_).execute(getUrl(position));
    return(pendingView);
  }
  return(super.getView(position, convertView, parent));
}

abstract protected String getUrl(int pos);
abstract protected void finishLoading();

protected void fetchMoreResult() {
  keepOnAppending =true;
}

@Override
protected void onNoResult(){
  keepOnAppending = false;
  super.onNoResult();
}
/**
 * A background task that will be run when there is a need
 * to append more data. Mostly, this code delegates to the
 * subclass, to append the data in the background thread and
 * rebind the pending view once that is done.
 */
class AppendTask extends UrlArrayAdapter<T,W>.AppendTask {
  AppendTask(long e) {
    super(e);
  }

  @Override
  protected void onPostExecute(JSONArray result) {
    // must set to false, to disable pending view.
    keepOnAppending = false;
    super.onPostExecute(result);
    finishLoading();
    pendingView=null;
  }
}

}
