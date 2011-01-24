package com.cinla.ringtone;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;

public class CategoriesListActivity extends ListActivity {

	private static ArrayList<CategoryItem> sCategoriesList;
	private static CategoryListAdapter sAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.categories_page);
		AdListener.createAds(this);
		init();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		SearchListActivity.startQueryByCategory(CategoriesListActivity.this, ((CategoryItem)(sCategoriesList.get(position))).getmCategoryKey());
	}

	private void init() {
		Utils.D("in init()");
		if (sCategoriesList != null) {
			sCategoriesList.clear();
		} else {
			sCategoriesList = new ArrayList<CategoryItem>();
		}
		for (int i=0; i<Constant.CATEGORIES_NAME.length; i++) {
			sCategoriesList.add(new CategoryItem(Constant.CATEGORIES_NAME[i], Constant.CATEGORIES_VALUE[i]));
		}
		Utils.D("categoryList size(): "+sCategoriesList.size());
		if (sAdapter == null) {
			sAdapter = new CategoryListAdapter(CategoriesListActivity.this, R.layout.category_item);
		}
		getListView().setAdapter(sAdapter);
		sAdapter.notifyDataSetChanged();
	}
	
	private class CategoryListAdapter extends BaseAdapter {
		
		private int mResource;
		private LayoutInflater mInflater;

		public CategoryListAdapter(Context context, int resource) {
			mResource = resource;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
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
			
			Utils.D(categoryItem.getmCategoryTitle());
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
