package com.trans.music.search;


import com.admob.android.ads.*;
import com.trans.music.search.R;

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
public class SLMale extends Activity {
	
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
					//final String key = mType_Animals[position];
					/*
					final String key = (String)mTypesList.getItemAtPosition(position);
	                setResult(RESULT_OK, new Intent().putExtra("keyword", key));
	                finish();
	                */
	                final String key = (String)mTypesList.getItemAtPosition(position);
	                Intent intent = new Intent();
					Log.e("OnlineMusic ", "putExtra name : " + key);
					intent.putExtra("name", key);
	            	intent.setClass(SLMale.this, Artist.class);
					startActivity(intent);	

				}catch(Exception e) {
					e.printStackTrace();
				} 
            }
        });


    }

    private String[] mType_Animals = {
            "Aaron Carter","Aaron Neville","Acker Bilk","Afroman","Al Green","Alan Jackson","Alan Parsons","Alessandro Safina","Andre Rieu","Andrew Lloyd Webber","Andy Williams","Antonio Carlos Jobim","Antonio Vivaldi","Aphex Twin","Aqualung","Armand Van Helden","Art Garfunkel","Avant","Akon","Ashley Parker Angel","Atb",
			"Bubba Sparxxx","Bow Wow","Bobby Brown","B.B.King","Busta Rhymes","Burt Bacharach","Buddy Holly","Bryan Adams","Bruce Springsteen","Brian Wilson","Brian Mcknight","Brian Mcfadden","Brahms","Boney James","Bobby Mcferrin","Bobby Darin","Bob Marley","Bob Dylan","Bing Crosby","Billy Joel","Billy Idol","Billy Gilman","Billy Crawford","Benny Goodman","Ben E. King","Beenie Man","Barry White","Barry Manilow","Backstreet","Babyface","Baby Bash","B.B. King",
			"Cat Stevens","Charlie Parker","Cheb Mami","Chet Baker","Chingy","Chopin","Chris De Burgh","Christophe Goze","Chuck Berry","Clay Aiken","Cliff Richard","Coldplay","Coolio","Count Basie","Craig David","Chris Brown",
			"Damien Rice","Dan Gibson","Daniel Bedingfield","Dante Thomas","Darius","Darren Hayes","Darude","Dave Koz","David Bowie","David Byrne","David Grohl","Deftones","Dj Bobo","Dj Sammy","Don Carlos","Don Mclean","Don Ross","Don Williams","Duke Ellington",
			"Elton John","Elvis Costello","Elvis Presley","Eminem","Enrique Iglesias","Eric Carmen","Eric Clapton","Enigma",
			"Fabolous","Fats Domino","Ferry Corsten","Flow","Frank Sinatra","Frank Zappa",
			"Garth Brooks","George Benson","George Michael","Gigi D aGeorge Winston","Gigi D agostino","Gil Ofarim","Gipsy Kings","Glenn Lewis","Guns N Roses",
			"Hank Williams","Harry Connick Jr.","Hilary Stagg",
			"Ice Cube","Il divo",
			"Joe","Juvenile","Justin Timberlake","Juno Reactor","Julio Iglesias","Juelz Santana","Joy Division","Josh Groban","Johnny Cash","John Scofield","John Mellencamp","John Mayer","John Lennon","John Lee","John Denver","John Coltrane","Johann Sebastian Bach","Joe Satriani","Jackson","Joe Cocker","Jimi Hendrix","Jim Reeves","Jim Brickman","Jesse Mccartney","Jerry Lee Lewis","Jeff Buckley","Jeff Beck","Jean Michel","Jay Sean","James Taylor","James Last","James Horner","James Brown","Jamie Foxx","Ja Rule",
			"Kanye West","Karl Jenkins","Karunesh","Keith Urban","Kenny G","Kenny Rogers","Kern","Kid Rock",
			"Lamb Of God","Lambchop","Lenny Kravitz","Leonard Cohen","Lil Romeo","Lionel Richie","Lloyd Banks","Lou Reed","Louis Armstrong","Ludacris","Luther Vandross",
			"Maksim","Marc Anthony","Mario","Mario Winans","Marvin Gaye","Matthew Lien","Maxwell","Michael Bolton","Michael Buble","Michael Franks","Michael Jackson","Michael Learns To Rock","Miles Davis","Montell Jordan","Mozart","Muddy Waters","Muse","Michael W. Smith","Moby",
			"Nat king Cole","Neil Diamond","Neil Young","Nelly","Nick Carter",
			"Patrick Nuo","Paul Anka","Paul Mauriat","Paul Mccartney","Paul Simon","Paul Van Dyk","Paul Young","Phil Collins","Pink Floyd","Pink Martini","Placido Domingo","Prince","Puff Daddy","Quincy Jones",
			"Quincy Jones",
			"R.Kelly","Ray Charles","Richard Clayderman","Richard Marx","Richard Thompson","Rick Astley","Ricky Martin","Rob Zombie","Robbie Williams","Rod Stewart","Roger Waters","Rolling Stones","Ronan Keating",
			"Seal","Sean Paul","Shaggy","Shakins Stevens","Snoop Dogg","Stephen Gately","Steve Earle","Steve Vai","Steven Curtis Chapman","Stevie Wonder","Sting","Stratovarius","Santana","Shakin Stevens",
			"T.I.","The Beach Boys","The Darkness","Tim Mcgraw","Toby Keith","Tom Jones","Tom Petty","Tom Waits","Tony Bennett",
			"Usher",
			"Vangelis",
			"Will Smith","Will Young","Wyclef Jean",
			"Yanni"
		};
}

	


