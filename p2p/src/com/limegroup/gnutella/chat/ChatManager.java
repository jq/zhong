package com.limegroup.gnutella.chat;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.util.Comparators;

/**
 * This class establishes a connection for a chat, either
 * incoming or outgoing, and also maintains a list of all the 
 * chats currently in progress.
 *
 * @author rsoule
 */
public final class ChatManager {

	/**
	 * Constant for the <tt>ChatManager</tt> instance, following
	 * singleton.
	 */
	private static final ChatManager CHAT_MANAGER = new ChatManager();

	/** 
	 * <tt>List</tt> of InstantMessenger objects.
	 */
	private List _chatsInProgress 
		= Collections.synchronizedList(new LinkedList());

	/**
	 * Instance accessor for the <tt>ChatManager</tt>.
	 */
	public static ChatManager instance() {
		return CHAT_MANAGER;
	}

	/** 
	 * Accepts the given socket for a one-to-one
	 * chat connection, like an instant messanger.
	 */
	public void accept(Socket socket) {
        Thread.currentThread().setName("IncommingChatThread");
		// the Acceptor class recieved a message already, 
		// and asks the ChatManager to create an InstantMessager
		boolean allowChats = ChatSettings.CHAT_ENABLED;

		// see if chatting is turned off
		if (! allowChats) {
			try {
				socket.close();
			} catch (IOException e) {
			}
			return;
		}

		// do a check to see it the host has been blocked
		String host = socket.getInetAddress().getHostAddress();
        
		try {
			ActivityCallback callback = 
			    RouterService.getCallback();
			InstantMessenger im = 
			    new InstantMessenger(socket, this, callback);
			// insert the newly created InstantMessager into the list
			_chatsInProgress.add(im);
			callback.acceptChat(im);
			im.start();
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException ee) {
			}
		}
	}

	/** 
	 * Request a chat connection from the host specified 
	 * returns an uninitialized chat connection.  the callback
	 * will be called when the connection is established or
	 * the connection has died.
	 */
	public Chatter request(String host, int port) {
		InstantMessenger im = null;
		try {
			ActivityCallback callback = 
			    RouterService.getCallback();
			im = new InstantMessenger(host, port, this, callback);
			// insert the newly created InstantMessager into the list
			_chatsInProgress.add(im);
			im.start();
		} catch (IOException e) {
            // TODO: shouldn't we do some cleanup here?  Remove the session
            // from _chatsInProgress??
		} 
		return im;
	}

	/** 
	 * Remove the instance of chat from the list of chats
	 * in progress.
	 */
	public void removeChat(InstantMessenger chat) {
		_chatsInProgress.remove(chat);
	}

}
