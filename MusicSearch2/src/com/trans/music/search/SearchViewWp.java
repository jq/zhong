package com.trans.music.search;

import com.jokes.search.R;

import android.view.View;
import android.widget.TextView;

public class SearchViewWp {
  TextView name;
  TextView artist;
  TextView size;
  TextView album;

  public SearchViewWp(View view) {
    view.setTag(this);
    name = (TextView)view.findViewById(R.id.row_title);
    artist = (TextView)view.findViewById(R.id.row_artist);
    size = (TextView)view.findViewById(R.id.song_size);
    album = (TextView) view.findViewById(R.id.row_album);
  }

}
