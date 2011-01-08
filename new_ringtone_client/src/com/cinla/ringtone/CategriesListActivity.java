package com.cinla.ringtone;

import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

public class CategriesListActivity extends ListActivity {

	private static ArrayList<CategoryItem> sCategoriesList;
	private static CategoryListAdapter sAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categories_page);
		init();
	}
	
	private void init() {
		if (sCategoriesList != null) {
			sCategoriesList.clear();
		} else {
			sCategoriesList = new ArrayList<CategoryItem>();
		}
		for (int i=0; i<sCategoriesList.size(); i++) {
			sCategoriesList.add(new CategoryItem(Constant.CATEGORIES_NAME[i], Constant.CATEGORIES_VALUE[i]));
		}
		if (sAdapter == null) {
			sAdapter = new CategoryListAdapter();
		}
		getListView().setAdapter(sAdapter);
	}
	
	private class CategoryListAdapter extends BaseAdapter {
		
		private int mResource;
		private LayoutInflater mInflater;
		
		@Override
		public int getCount() {
			if (sCategoriesList !=null) {
				return sCategoriesList.size();
			}
			return 0;
		}
		@Override
		public Object getItem(int position) {
			if (sCategoriesList!=null && position<sCategoriesList.size()) {
				return sCategoriesList.get(position);
			}
			return null;
		}
		@Override
		public long getItemId(int position) {
			if (sCategoriesList == null) {
				return -1;
			}
			return position;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			Object item = sCategoriesList.get(position);
			if (convertView == null) {
				v = mInflater.inflate(mResource, parent, false);
			} else {
				v = convertView;
			}
			CategoryItem categoryItem = (CategoryItem) item;
			
			((TextView) v.findViewById(R.id.category_title)).setText(categoryItem.getmCategoryTitle());
			return v;
		}
	}
	
	private class CategoryItem {
		private String mCategoryTitle;
		private String mCategoryKey;
		
		public CategoryItem(String mCategoryTitle, String mCategoryKey) {
			super();
			this.mCategoryTitle = mCategoryTitle;
			this.mCategoryKey = mCategoryKey;
		}

		public String getmCategoryTitle() {
			return mCategoryTitle;
		}

		public void setmCategoryTitle(String mCategoryTitle) {
			this.mCategoryTitle = mCategoryTitle;
		}

		public String getmCategoryKey() {
			return mCategoryKey;
		}

		public void setmCategoryKey(String mCategoryKey) {
			this.mCategoryKey = mCategoryKey;
		}
		
	}

}
