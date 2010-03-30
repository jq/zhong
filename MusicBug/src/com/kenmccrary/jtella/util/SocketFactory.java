package com.kenmccrary.jtella.util;

import java.net.Socket;
import java.io.IOException;
import java.io.InterruptedIOException;


public class SocketFactory
{
  private static final long POLL_TIME = 100;
  
  /**
   *  Creates a socket with a maxiumum wait value on the initial connection
   *
   *  @param host remote host
   *  @param port remote port
   *  @param maxWait maximum wait time for connection in milliseconds
   *  @return socket
   */
  public static Socket getSocket(String host, 
                                 int port, 
                                 int maxWait) throws IOException,
                                                     InterruptedIOException
  {
    Socket socket = null;
    
    ConnectThread connectThread = new ConnectThread(host, port);
    connectThread.start();
		long timer = 0;
    
    while ( true )
    {
      if ( connectThread.isConnected() )
      {
        socket = connectThread.getConnectedSocket();
        break;
      }  
      
      if ( connectThread.isError() )
      {
        throw connectThread.getException();
      }  
      
      try
			{
  			Thread.sleep ( POLL_TIME );
			}
			catch (InterruptedException ie) 
      {
        // ignore
      }

			// Increment timer
			timer += POLL_TIME;

			// Check to see if time limit exceeded
			if ( timer > maxWait )
			{
        connectThread.interrupt();
  			throw new InterruptedIOException();
			}
		}
  
    
    return socket;
  }
  
  
  /**
   *  Thread to run Socket connect
   */
  static class ConnectThread extends Thread
  {
    private String host;
    private int port;
    private Socket socket;
    private IOException exception;
    
    ConnectThread(String host, int port)
    {
      super("ConnectThread");
      this.host = host;
      this.port = port;
    }
    
    public void run()
    {
      try
      {
        socket = new Socket(host, port);
      }
      catch (IOException e)
      {
        // connection failed
        exception = e;
      }
    }
    
    /**
     *  Checks if an error occurred on the connection
     *
     */
    boolean isError()
    {
      return null != exception;
    }
    
    /**
     *  Get the exception
     */
    IOException getException()
    {
      return exception;
    }
    
    /**
     *  Checks if the connection is achieved
     */
    boolean isConnected()
    {
      return null != socket;
    }

    /**
     *  Get the completed connection
     *
     */
    Socket getConnectedSocket()
    {
      return socket;
    }
  }
}
