package com.trans.music.search;


import com.admob.android.ads.*;

import android.app.Activity;
import android.os.Bundle;

import java.util.ArrayList;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;
import android.view.View.OnClickListener;

import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A list view example where the 
 * data for the list comes from an array of strings.
 */
public class SLFemale extends Activity {
	
	ListView mTypesList;
	private String[] mCurTypes;
	private AdView mAd;

	JSONArray mFeedentries;
	
    ArrayAdapter<String> mAdapter; 
    ArrayList<String> mStrings = new ArrayList<String>();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.popular);
        findViewById(R.id.center_text).setVisibility(View.GONE);
		
        mTypesList = (ListView) findViewById(R.id.popular);

        
        mAd = (AdView) findViewById(R.id.ad);	
        mAd.setVisibility(View.VISIBLE);
        
        //new AdsView(this);
        
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mType_Animals);    
        mTypesList.setAdapter(mAdapter);

		try{
			mFeedentries = new JSONArray();
        }catch(Exception e) {
			e.printStackTrace();
		} 
		
        mTypesList.setTextFilterEnabled(true);


        mTypesList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {         
				try{		
	                final String key = (String)mTypesList.getItemAtPosition(position);
	                Intent intent = new Intent();
					Log.e("OnlineMusic ", "putExtra name : " + key);
					intent.putExtra("name", key);
	            	intent.setClass(SLFemale.this, Artist.class);
					startActivity(intent);	

				}catch(Exception e) {
					e.printStackTrace();
				} 
            }
        });


    }

    private String[] mType_Animals = {
            "Anastacia","Angie Stone","Anne Murray","Annie Lennox","Aselin Debison","Ashanti","Ashlee Simpson","Astrud Gilberto","Avril Lavigne","Aaliyah","Alanis Morissette","Alex Parks","Alicia Keys","Alison Krauss","Alizee","Amber","Amy Grant","Aretha Franklin",
			"Britney Spears","Beyonce","Barbra Streisand","Beth Orton","Bette Midler","Bic Runga","Billie Holiday","Billie Piper","Bjork","Blu Cantrell","Bonnie Tyler",
			"Crystal Gayle","Curve","Cyndi Lauper","Celine Dion","Christina Aguilera","Carly Simon","Carrie Underwood","Chantal Kreviazuk","Charlotte Church","Christina Milian",
			"Donna Lewis","Dido","Dannii Minogue","Deborah Cox","Delta Goodrem","Diana Krall","Diana Ross","Dolly Parton",
			"Enya","Elaine Paige","Emma Bunton","Eva Cassidy","Eve",
			"Faith Evans","Faith Hill",
			"Geri Halliwell","Gladys Knight","Gloria Estefan","Gretchen Wilson","Gwen Stefani",
			"Hayley Westenra","Hilary Duff","Holly Valance",
			"Ian Van Dahl","India Arie",
			"Jennifer Lopez","Janet Jackson","Jessica Andrews","Jessica Simpson","Jewel","Joan Osborne","Jojo","Joni Mitchell","Joss Stone","Judy Collins","Julie London","Jamelia","Jane Monheit","Jennifer Rush","Jennifer Warnes",
			"Kylie Minogue","K.D.Lang","Kate Bush","Kate Ryan","Kathryn Williams","Kelis","Kelly Clarkson","Kelly Rowland","Kimberley Locke",
			"Lene Marlin","Lisa Loeb","Lisa Marie Presley","Lisa Stansfield","Liz Phair","Loreena Mckennitt","Lulu","Lumidee","Lara Fabian","Laura Fygi","Laura Pausini","Lauryn Hill","Leann Rimes","Lee Ann Womack","Lil Kim","Linda Eder","Linda Ronstadt","Lindsay Lohan",
			"Mariah Carey","Madonna","Melanie C","Melissa Etheridge","Meredith Brooks","Michelle Branch","Minnie Riperton","Mirah","Mireille Mathieu","Missy Elliott","Monica","Mya","Mylene Farmer","Macy Gray","Mads House","Mandy Moore","Marianne Faithfull","Martina Mcbride","Mary Black","Mary J Blige","Meav",
			"Norah Jones","Nana Mouskouri","Natalie Imbruglia","Natalie Merchant","Natasha Thomas","Pat Benatar","Patricia Barber","Patti Austin","Paula Cole","Paulina Rubio","Pink",
			"Rachel Stevens","Reba Mcentire","Rihanna","Shana Morrison","Shania Twain","Sheryl Crow","Sinead O Connor","Sita","Sophie Ellis Bextor","Sophie Zelmani","Stacie Orrico","Suzanne Vega","Svala","Sarah Brightman","Sade","Samantha Mumba","Sandra","Sara Evans","Sarah Connor","Sarah Mclachlan","Shakira","Skye Sweetnam",
			"Tanya Donelly","This Mortal Coil","Tina Arena","Toni Braxton","Tori Amos","Tracy Chapman",
			"Vitamin C",
			"Whitney Houston"
		};
}
