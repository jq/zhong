package com.feebee.rings;

import com.feebe.lib.ImgLoader;
import com.feebee.rings.R;

import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class SearchViewWp extends ImgLoader{
  TextView name;
  TextView artist;
  RatingBar rating;

  public SearchViewWp(View view) {
    view.setTag(this);
    name = (TextView)view.findViewById(R.id.row_title);
    artist = (TextView)view.findViewById(R.id.row_artist);
    rating = (RatingBar)view.findViewById(R.id.row_small_ratingbar);
    image = (ImageView) view.findViewById(R.id.row_icon);
  }

}
