package com.feebe.musicsearch;


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
public class SLBand extends Activity {
	
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
	            	intent.setClass(SLBand.this, Artist.class);
					startActivity(intent);	

				}catch(Exception e) {
					e.printStackTrace();
				} 
            }
        });


    }

    private String[] mType_Animals = {
            "A1","ABBA","Acdc","Ace Of Base","Aerosmith","A-Ha","Air Supply","Alice Cooper","Alice In Chains","All Saints","All-4-One","Anyone","Aqua","Atb","Atomic Kitten","A*Teens","A3","Air","Al Di Meola","Alcazar","Alien Ant Farm","America","Another Level",
			"Beatles","B Rich","B2K","B3","Backstreet Boys","Bandari","Beck","Black Eyed Peas","Blur","Bon Jovi","Boyz Ii Men","Boyzone","Bread","Bush","Baha Men","Bananarama","Barenaked Ladies","Basement Jaxx","Beach Boys","Beastie Boys","Bee Gees","Big Mountain","Black Box Recorder","Blackmore s Night","Blink 182","Blondie","Bob Marley & The Wailers",
			"Cake","Cardigans","Carpenters","Coldplay","Cranberries","C21","Chicago","Chumbawamba","City High","Cocteau Twins","Creed","Crowded House","Culture Club","Cypress Hill",
			"Daft Punk","Deep Purple","Destiny s Child","Dr Dre","Dream","David Lee Roth","Dead Can Dance","Def Leppard","Depeche Mode","Disturbed","Dixie Chicks",
			"Elbow","Enigma","Evanescence","Eagles","Eiffel 65","Entwine",
			"Feeder","Foo Fighters","Fuel","Five",
			"Green Day","Goo Goo Dolls","Gorillaz","Guns N Roses","	Garbage","Girls Aloud","Groove Coverage",
			"Hanson","Him","Hoobastank","Hooverphonic","Human Nature",
			"Il Divo","Inxs",
			"K3","Kiss","Las Ketchup","Led Zeppelin","Linkin Park","Lighthouse Family","Limp Bizkit","Lmnt",
			"M2M","Machine Head","Massive Attack","Michael Learns To Rock","Mazzy Star","Mcfly","Men In Black","maroon 5",
			"Nirvana","No Angels","No Doubt","Nsync","Neil Young","Nightwish",
			"Oasis","O-Town",
			"Pearl Jam","Pink Floyd","Public Enemy","P.O.D","Pennywise","Pet Shop Boys","Plus One",
			"Queen",
			"Rolling Stones","R.E.M.","Radiohead","Roxette","Rage Against The Machine","Rammstein","Rascal Flatts","Red Hot Chili Peppers","Rooster",
			"Savage Garden","Scooter","Scorpions","Secret Garden","Simon and Garfunkel","Smash","Spice Girls","Steps","Strung Out","S Club 7","Sixpence None The Richer","Skid Row","Smile","Staind","Starsailor","Stratovarius","Suede","Sugar Ray","Sum 41","Supathugz",
			"The Chemical Brothers","t.A.T.u","Texas","The Beach Boys","The Beatles","The Calling","The Clash","The Corrs","The Doors","The Eagles","The Everly Brothers","The Ramones","The Rolling Stones","The Smashing Pumpkins","The Velvet Underground","Tlc","Take That","The Bravery","The Cardigans","The Cheeky Girls","The Cure","The Mamas & The Papas","The Moffatts","Toy-Box","Trademark","Train","Travis",
			"U2",
			"Vengaboys",
			"Westlife","Wham","Wet Wet Wet","Within Temptation",
			"X-Ecutioners",
			"Yes",
			"10,000 Maniacs","702","112","2 Unlimited","4 Non Blondes","50 Cent","911","98 Degrees","311","3 Doors Down","3Lw","3T"
		};
}

