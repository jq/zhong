package com.feebe.rings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class CommentList extends ListActivity {

  public void onCreate(Bundle savedInstanceState) {  
    super.onCreate(savedInstanceState);
    setTitle("Ring Comments");
    try {
      key = getIntent().getStringExtra("Ring");
    } catch (Exception e) {
      
    }
    
    SimpleAdapter adapter = new SimpleAdapter(this, getData(), R.layout.comment_list_item, new String[]{"user","comment"}, new int[]{R.id.commentListItem1,R.id.commentListItem2});
    
    setListAdapter(adapter);
  }
  
  private List<Map<String, Object>> getData() {
    
    List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(); 
    Map<String, Object> map;
    
    List commentList = RingUtil.getJsonArrayFromUrl("http://ggapp.appspot.com/ringtone/getcm/" + key, 0);
    
    if(commentList != null && commentList.size() > 0) {
      for(Iterator<JSONObject> it = commentList.iterator(); it.hasNext(); ) {
        JSONObject commentObj = it.next();
        try {
          String user = commentObj.getString("user");
          String comment = commentObj.getString("comment");
          Log.e("user comment: ", user + "," + comment);
          
          if(user.length() > 0 && comment.length() > 0) {
            map = new HashMap<String, Object>();
            map.put("user", user);
            map.put("comment", comment);
            list.add(map);
          }
          
        } catch (JSONException e) {
          
        }
        
      }
    }
    
    if(list.size() == 0){
      noComment();
    }
    return list;
  }
  
  private void noComment() {
    Toast.makeText(getApplicationContext(), "No comments", Toast.LENGTH_SHORT).show();
    CommentList.this.finish();
  }
  
  private String key = "";

}
