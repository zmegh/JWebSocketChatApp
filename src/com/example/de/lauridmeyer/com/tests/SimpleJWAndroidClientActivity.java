package com.example.de.lauridmeyer.com.tests;


import org.codehaus.jackson.JsonParser;
import org.jwebsocket.api.WebSocketClientEvent;
import org.jwebsocket.api.WebSocketClientTokenListener;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.client.plugins.rpc.Rpc;
import org.jwebsocket.client.plugins.rpc.RpcListener;
import org.jwebsocket.client.plugins.rpc.Rrpc;
import org.jwebsocket.client.token.BaseTokenClient;
import org.jwebsocket.kit.WebSocketException;
import org.jwebsocket.token.Token;
import org.jwebsocket.token.TokenFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.example.de.lauridmeyer.com.tests.R.drawable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;


public class SimpleJWAndroidClientActivity extends Activity implements WebSocketClientTokenListener{

	// Audio Elements
	private MediaRecorder myAudioRecorder;
	private String outputFile = null;
	private boolean isRecording = false;
	
	// SMS Elements
	private final int   SENT     = 1;
	 private static final String SMS_DELIVERED = "SMS_DELIVERED";
     private static final String SMS_SENT = "SMS_SENT";
	//GUI Elements
	private static TextView log;
	private static EditText chatMessage;
	private static ImageButton btnSend;
	private static ImageButton btnAudio;
	private static RelativeLayout conversationLayout;
	private static ScrollView sv1;

	//stores if the SLider is changed by the user or the server
	private static Boolean isDragging=false;

	//stores the connection status
	private static Boolean connected=false;
	private int nMessageCount =2;
	//used for the connection
	private static BaseTokenClient btc = new BaseTokenClient(); //create a new instance of the base token client
	
	//used for messaging
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        	
        /************** AUDIO CODE *************************/
        outputFile = Environment.getExternalStorageDirectory().
        	      getAbsolutePath() + "/myrecording.3gp";;
	      InitializeRecorder();
        
        
        /*****************************************************/
        chatMessage=(EditText)findViewById(R.id.txtChat);
        btnSend= (ImageButton)findViewById(R.id.btnSend);
        conversationLayout = (RelativeLayout)findViewById(R.id.conversationLayout);
        sv1 = (ScrollView)findViewById(R.id.sv1);
        
        //add the listener for the websocket connection
		btc.addListener(this);//add this class as a listener
		btc.addListener(new RpcListener());//add an rpc listener
		Rpc.setDefaultBaseTokenClient(btc);//set it to the default btc
		Rrpc.setDefaultBaseTokenClient(btc);//same here
		
		Connect();
		addSendButtonListener();
		//addOnTouchAudioListener();
		addbtnAudioClickListener();
		
		
    }
    
   private void InitializeRecorder() {
	  myAudioRecorder = new MediaRecorder();
      myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
      myAudioRecorder.setOutputFile(outputFile);
	}

/***************************** AUDIO CODE *******************************************************/
    public void addbtnAudioClickListener() {

		btnAudio = (ImageButton) findViewById(R.id.btnAudio);
		btnAudio.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				
                try 
                {
              	  //CHECK IF RECORDING
                	if(!isRecording)
                	{
                		if(myAudioRecorder == null) InitializeRecorder();
                		
                		btnAudio.setBackground(null);
                		btnAudio.setBackgroundResource(drawable.record);
                		myAudioRecorder.prepare();
    					myAudioRecorder.start();
    					isRecording = true;
    					Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
                	}
                	else
                	{
            		 	myAudioRecorder.stop();
            		
           		     	myAudioRecorder.release();
           		     	myAudioRecorder  = null;
           		     
           		      btnAudio.setBackgroundResource(drawable.mic);
           		      Toast.makeText(getApplicationContext(), "Audio recorded successfully",
           		      Toast.LENGTH_LONG).show();
           		      isRecording = false;
           		      
           		      play(view);
           		      SendSMS(ConvertMediaToBytes());
                	}
                	
					
				} 
                catch (IllegalStateException e) {
				
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				}
                finally
                {
                	if(myAudioRecorder != null )
                	{
	                	 /*myAudioRecorder.stop();
	            		 isRecording = false;
	           		     myAudioRecorder.release();
	           		     myAudioRecorder  = null;*/
                	}
                }

			}
		});
	}
    
    public void play(View view) throws IllegalArgumentException,   
    SecurityException, IllegalStateException, IOException{
    
    MediaPlayer m = new MediaPlayer();
    m.setDataSource(outputFile);
    m.prepare();
    m.start();
    
    Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();
    }
    /****************************************** END AUDIO **************************************/
    
    private byte[] ConvertMediaToBytes() {
    	final Context context = getBaseContext();
        int bytesRead;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fileInputStream = null;
        try 
        {
        	File recordingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "myrecording.3gp");
        	//File file = this.getFileStreamPath(outputFile);
        	
        	fileInputStream = new FileInputStream(recordingFile);
            

            byte[] b = new byte[1024];

            while ((bytesRead = fileInputStream.read(b)) != -1) {

                bos.write(b, 0, bytesRead);
        }

            
        }catch(Exception e) {

            Toast.makeText(this, "Error starting draw. ",Toast.LENGTH_SHORT).show();

        }
        finally
        {
        	if(fileInputStream != null)
        	{
        		 try {
					fileInputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        }
        
        return bos.toByteArray();
	}

	/**************************** SEND SMS **************************************************/
    private void SendSMS(byte[] data)
    {
    	 short SMS_PORT = 8998;
    	 
    	 PendingIntent piSend = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
         PendingIntent piDelivered = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 0);
    	
    	 SmsManager smsManager = SmsManager.getDefault();
    	 
    	// smsManager.sendTextMessage("+18572308090", null, "test message", sendIntent, deliveryIntent);
    	 //sendData(9);
    	 String messageText = "testing data message";
    	 String phonenumber = "+16179537586";
    	 smsManager.sendDataMessage(phonenumber, null, (short) SMS_PORT, data,piSend, piDelivered);
    }
    
    private BroadcastReceiver sendreceiver = new BroadcastReceiver()
    {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                    String info = "Send information: ";
                    
                    switch(getResultCode())
                    {
                            case Activity.RESULT_OK: info += "send successful"; break;
                            case SmsManager.RESULT_ERROR_GENERIC_FAILURE: info += "send failed, generic failure"; break;
                            case SmsManager.RESULT_ERROR_NO_SERVICE: info += "send failed, no service"; break;
                            case SmsManager.RESULT_ERROR_NULL_PDU: info += "send failed, null pdu"; break;
                            case SmsManager.RESULT_ERROR_RADIO_OFF: info += "send failed, radio is off"; break;
                    }
                    
                    Toast.makeText(getBaseContext(), info, Toast.LENGTH_SHORT).show();

            }
    };
    
    private BroadcastReceiver deliveredreceiver = new BroadcastReceiver()
    {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                    String info = "Delivery information: ";
                    
                    switch(getResultCode())
                    {
                            case Activity.RESULT_OK: info += "delivered"; break;
                            case Activity.RESULT_CANCELED: info += "not delivered"; break;
                    }
                    
                    Toast.makeText(getBaseContext(), info, Toast.LENGTH_SHORT).show();
            }
    };
    
    private BroadcastReceiver smsreceiver = new BroadcastReceiver()
    {
            @Override
            public void onReceive(Context context, Intent intent)
            {
            Bundle bundle = intent.getExtras();        
            SmsMessage[] msgs = null;
            
            if(null != bundle)
            {
                    String info = "Text SMS from ";
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                
                for (int i=0; i<msgs.length; i++){
                    msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
                    info += msgs[i].getOriginatingAddress();                     
                    info += "\n*****TEXT MESSAGE*****\n";
                    info += msgs[i].getMessageBody().toString();
                }

                Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
            }                         
            }
    };
    /***************************************************************************************/
    
	public void addSendButtonListener() {

		btnSend = (ImageButton) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
			  
				try {
					
					chatMessage.clearComposingText();
					String newMsg = chatMessage.getText().toString();
					
					if(newMsg == null || newMsg =="") return;

					/* WRAP TEXT */
					RelativeLayout.LayoutParams childParams = new RelativeLayout.LayoutParams(
							 								LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
					childParams.setMargins(5,5, 5,5);
					
					childParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
					
					if(nMessageCount == 2) 
						childParams.addRule(RelativeLayout.BELOW,conversationLayout.getId());
					else
						childParams.addRule(RelativeLayout.BELOW,  nMessageCount -1);
					
		        	TextView tvNewMsg = new TextView(view.getContext());
		        	tvNewMsg.setId(nMessageCount);
		        	//childParams.addRule(RelativeLayout.BELOW, tvNewMsg.getId());
		        	tvNewMsg.setText(chatMessage.getText());
		        	tvNewMsg.setPadding(5, 7, 5, 5);
		        	tvNewMsg.setTextColor(Color.BLACK);
		        	tvNewMsg.setMaxWidth(400);
		        	tvNewMsg.setBackgroundResource(drawable.outgoingmsgbackground);
		        	
		        	//tvNewMsg.setLayoutParams(childParams);
		        	
		        	conversationLayout.addView(tvNewMsg, childParams);
		        	 nMessageCount++;
		        	AutoScrollChatView();
		        	chatMessage.setText("");
		        	
					btc.broadcastText(newMsg);
			
					
				} catch (WebSocketException e) {
					
					e.printStackTrace();
				}
    			
				
			}
		});
	}
   
    public void AppendNewMessage(String msg, int color)
    {
    	RelativeLayout.LayoutParams childParams = new RelativeLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		childParams.setMargins(5,5, 5,5);
		
		childParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		
		if(nMessageCount != 0) 
			childParams.addRule(RelativeLayout.BELOW, nMessageCount -1);
		
		TextView tvNewMsg = new TextView(getBaseContext());
		tvNewMsg.setId(nMessageCount);
		tvNewMsg.setText(msg);
		tvNewMsg.setPadding(5, 7, 5, 5);
		
		tvNewMsg.setTextColor(color);
		
		tvNewMsg.setBackgroundResource(drawable.outgoingmsgbackground);
		
		//tvNewMsg.setLayoutParams(childParams);
		
		conversationLayout.addView(tvNewMsg, childParams);
		nMessageCount++;
		AutoScrollChatView();
    };
    
   public void Connect(){
        	if(!connected)
        	{
        		//if not connected
        		try{
        			
        			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        			StrictMode.setThreadPolicy(policy); 
        			
        			
        			String ipAdress="ws://54.187.73.9:8787/jWebSocket/jWebSocket";
        			//outputText("connecting to "+ipAdress+" ...");//debug
        			//btc.open was throwing an exception
        			//solved using this http://stackoverflow.com/questions/2944074/eclipse-android-change-api-level THANKS SOF
        			btc.open(ipAdress);//try to open the connection to your server
        			changeConnectionStatus(true);
        			Date date = new Date();
        			//send test message
        			
        		}catch(Exception e){
        			outputText("Error while connecting...");//log errors
        		}
        	}else{//if connected
        		try{
        			btc.close();//try to close
        		}catch(Exception e){
        			outputText(e.toString());//log errors
        		}
        	}
        };
   
    /* Method is called when the connection status has changed
     * connected <-> disconnected */
    public static void changeConnectionStatus(Boolean isConnected) {
    	connected=isConnected;//change variable
  		
  		if(isConnected){//if connection established
  			outputText("successfully connected to server");//log
  			
  		}else{
  			outputText("disconnected from Server!");//log
  			
  		}
  	}

   
	public static void outputText(String msg) {
   		//log.append(msg+"\n");
   	}

	
	private Handler messageHandler = new Handler() {
		@Override
		public void handleMessage(Message aMessage) {
			if(!connected)
				return;
			
			if(aMessage == null) return;
					
			if(aMessage.what==0){//if it's a token
				Token aToken =(Token) aMessage.obj;
				//if the namespace matches my plugin
				if(aToken.getNS().equals("com.lauridmeyer.tests.LauridsPlugIn")){
					//and it's a command that the slider has changed
					if(aToken.getString("reqType").equals("sliderHasChanged")){
						int value=aToken.getInteger("value");//get the slider value
						isDragging=false;//make sure that the slider changes are not recognized as user inputs
						
					}
				}
				else
				{
					  try {
						  org.jwebsocket.token.MapToken obj =  ( org.jwebsocket.token.MapToken)aMessage.obj;
						  String sMsg = obj.getString("data");
						  
						if(sMsg == null || sMsg == "")return;
						
						
						  /* WRAP */
							RelativeLayout.LayoutParams childParams = new RelativeLayout.LayoutParams(
									 	LayoutParams.WRAP_CONTENT,
							            LayoutParams.WRAP_CONTENT);
							    childParams.setMargins(5, 5, 5, 5);
							 childParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
							 
						 if(nMessageCount > 0) 
								childParams.addRule(RelativeLayout.BELOW, nMessageCount -1);
							else
								childParams.addRule(RelativeLayout.BELOW, conversationLayout.getId());
							
						TextView tvNewMsg = new TextView(getBaseContext());
						tvNewMsg.setPadding(5, 7, 5, 5);
						tvNewMsg.setId(nMessageCount);
			        	tvNewMsg.setText(sMsg);
			        	tvNewMsg.setMaxWidth(400);
			        	tvNewMsg.setTextColor(Color.BLACK);
			        	tvNewMsg.setBackgroundResource(drawable.incomingmsgbackground);
			        	//tvNewMsg.setLayoutParams(childParams);
			        	nMessageCount++;		       
			      
			        	conversationLayout.addView(tvNewMsg, childParams);
			        	
			        	AutoScrollChatView();

					} catch (Exception e) {
						
						e.printStackTrace();
					}
					
					
				}
			}

		}
	};

	private void AutoScrollChatView()
	{
    	sv1.post(new Runnable() {
    	    @Override
    	    public void run() {
    	        sv1.fullScroll(ScrollView.FOCUS_DOWN);
    	    }
    	});
	};
	
    @Override
	public void processOpened(WebSocketClientEvent aEvent) {
    	changeConnectionStatus(true);//when connected change the status
	}

    @Override
	public void processClosed(WebSocketClientEvent aEvent) {
    	
    	AppendNewMessage("Connection closed!", Color.RED);
    	
    	changeConnectionStatus(false);//when disconnected change the status
    	Connect();
	}

	@Override
	public void processToken(WebSocketClientEvent aEvent, Token aToken) {
		//for some reason you can't process the token directly
		//you have to use the messagehandler
		Message lMsg = new Message();//create a new mess
		lMsg.what = 0;
		lMsg.obj = aToken;
		messageHandler.sendMessage(lMsg);
	}

	//following Methods are not used in this example, but have to be there :)
	@Override
	public void processPacket(WebSocketClientEvent aEvent, WebSocketPacket aPacket) {
	}

	@Override
	public void processOpening(WebSocketClientEvent aEvent) {
	}

	@Override
	public void processReconnecting(WebSocketClientEvent aEvent) {
	}
}