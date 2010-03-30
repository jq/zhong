package com.macrohard.musicbug;

import java.io.IOException;
import java.util.HashMap;



import android.util.Log;

import com.kenmccrary.jtella.GNUTellaConnection;
import com.kenmccrary.jtella.LOG;
import com.kenmccrary.jtella.MessageReceiver;
import com.kenmccrary.jtella.PushMessage;
import com.kenmccrary.jtella.SearchMessage;
import com.kenmccrary.jtella.SearchReplyMessage;
import com.kenmccrary.jtella.GUID;
/**
 * Example JTella node, to be used as a controller with the JTellaAdapter and JTellaGUI.
 * 
 * @author alan
 *
 */
public class JTellaNode implements MessageReceiver {			

	// THIS is sample, first call App.jta.search and store the GUID
	// when MessageReceiver is no longer valid call	App.jta.removeGUID(guid);
	public void receiveSearchReply(SearchReplyMessage searchReplyMessage) {
		  Log.e("search", "p2p");
		String output = "";
		output = "Search Response from :"+searchReplyMessage.getIPAddress()+":\n" + output ;
		for (int i =0;i<searchReplyMessage.getFileCount();i++){
			output += searchReplyMessage.getFileRecord(i).getName() + "\n";
		}
		
//		if (key == "kiss")
		  Log.e("search",output);
		//gui.callBack(output);
	}

	public void receivePush(PushMessage pushMessage) {
		//do nothing
		
	}
}
