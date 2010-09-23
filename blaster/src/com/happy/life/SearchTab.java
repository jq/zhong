package com.happy.life;

import java.util.ArrayList;

import com.limegroup.gnutella.util.StringUtils;
import com.util.DbAdapter;
import com.util.SearchCursorAdapter;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

public class SearchTab extends Activity{
	
	private AutoCompleteTextView searchArtist;
	private AutoCompleteTextView searchTitle;
    private AutoCompleteTextView searchAlbum;
	private Button searchButton;
	//private AutoCompleteTextView autoCompleteTextViewArtist;
	//private AutoCompleteTextView autoCompleteTextViewTitle;
	//private ArrayList<String> searchHistory;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_tab);
		
        Utils.addMixedAds(this);
        
		searchArtist = setTextView(R.id.input_artist, DbAdapter.TYPE_ARTIST);
		searchArtist.setOnKeyListener(new OnKeyListener(){
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if((event.getAction()==KeyEvent.ACTION_DOWN)&&
						(keyCode==KeyEvent.KEYCODE_ENTER)){
				    searchTitle.requestFocus();
					return true;
				}
				return false;
			}
		});
		
		searchTitle = setTextView(R.id.input_title, DbAdapter.TYPE_TITLE); 
		searchTitle.setOnKeyListener(new OnKeyListener(){
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if((event.getAction()==KeyEvent.ACTION_DOWN)&&
						(keyCode==KeyEvent.KEYCODE_ENTER)){
				    searchAlbum.requestFocus();
					return true;
				}
				return false;
			}
		});
		
        searchAlbum = setTextView(R.id.input_album, DbAdapter.TYPE_ALBUM); 
        searchAlbum.setOnKeyListener(new OnKeyListener(){
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if((event.getAction()==KeyEvent.ACTION_DOWN)&&
                        (keyCode==KeyEvent.KEYCODE_ENTER)){
                    actionListener();
                    return true;
                }
                return false;
            }
        });
		
		searchButton = (Button) findViewById(R.id.search_button);
		
		searchButton.setOnClickListener(new OnClickListener() {		
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				actionListener();
			}
		});
	}
	
	private AutoCompleteTextView setTextView(int id, int type) {
	    Cursor c = com.util.Constants.dbAdapter.getHistoryByType(type);
	   
	    AutoCompleteTextView view = (AutoCompleteTextView) findViewById(id);
	    if (c != null) {
    	    view.setThreshold(1);
            SearchCursorAdapter myCursorAdapterTitle = new SearchCursorAdapter(
                this, c, type);
            view.setAdapter(myCursorAdapterTitle);
	    }
	    return view;
	}
	
	private void actionListener(){
        String artist = StringUtils.removeIllegalChars(searchArtist.getText().toString());
        String title = StringUtils.removeIllegalChars(searchTitle.getText().toString());
        String album = StringUtils.removeIllegalChars(searchAlbum.getText().toString());
        boolean noArtist = TextUtils.isEmpty(artist);
        boolean noTitle = TextUtils.isEmpty(title);
        boolean noAlbum = TextUtils.isEmpty(album);
        String key = "";
        if (noArtist && noTitle && noAlbum) return;
        // http://wiki.limewire.org/index.php?title=Metadata_Searches
        //          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + 
        // "<audio schema=\"http://www.limewire.com/schemas/audio.xsd\"" +
        // " title=\"lady gaga\" artist=\"lady gaga\"/>";
        StringBuilder b = new StringBuilder(256);
        b.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> <audio schema=\"http://www.limewire.com/schemas/audio.xsd\"");
        if (!noArtist) {
            com.util.Constants.dbAdapter.insertHistory(artist, DbAdapter.TYPE_ARTIST);
            b.append("artist=\"");
            b.append(artist);
            b.append("\" ");
            key += artist + " ";
        }
        if (!noTitle) {
            com.util.Constants.dbAdapter.insertHistory(title, DbAdapter.TYPE_TITLE);
            b.append("title=\"");
            b.append(title);
            b.append("\" ");
            key += title + " ";
        }
        if (!noAlbum) {
            com.util.Constants.dbAdapter.insertHistory(album, DbAdapter.TYPE_TITLE);
            b.append("album=\"");
            b.append(album);
            b.append("\" ");
            key += album;
        }
        b.append("/>");
        MusicSearchResultActivity.handleMp3ListXMLIntent(this, key, b.toString());
    }
}
