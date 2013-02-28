package com.montestello.runningman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity implements LocationListener,OnInitListener {
	
	private Button buttonReset;
	private TextView distanceField;
	private TextView timeField;
	private TextView speedField;
	private Button buttonOnOff;
	private LocationManager locationManager;
	private String provider;
	private Location prevLoc;
	private double total_distance;
	private TextToSpeech mTts;
	private boolean running;
	private long total_time;
	private Timer timer;
	private long last_km;
	private long last_km_time;
	private Switch switchTTS;
	private SeekBar seekBarVolume;
	
	public void tick(){
		if (running)
		{
			total_time++;
			this.runOnUiThread(Timer_Tick);
		}
	}
	
	private Runnable Timer_Tick = new Runnable() {
		public void run() {
		
		//This method runs in the same thread as the UI.    	       
		
		//Do something to the UI thread here
			
			updateTime();
		
		}
	};
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonOnOff = (Button) findViewById(R.id.buttonStartStop);
        distanceField = (TextView) findViewById(R.id.textDistance);
        speedField = (TextView) findViewById(R.id.textSpeed);
        timeField = (TextView) findViewById(R.id.textTime);
        switchTTS = (Switch) findViewById(R.id.switchTTS);
        seekBarVolume = (SeekBar) findViewById(R.id.seekBarVolume);
        
        if (savedInstanceState != null)
        {
        	prevLoc = savedInstanceState.getParcelable("prevLoc");
        	total_distance = savedInstanceState.getDouble("total_distance");
        	running = savedInstanceState.getBoolean("running");
        	total_time = savedInstanceState.getLong("total_time");
        	last_km = savedInstanceState.getLong("last_km");
        	last_km_time = savedInstanceState.getLong("last_km_time");
        	if (running)
        	{
        		timer = new Timer();
        		timer.schedule(new UpdateTime(), 1000, 1000);
        		buttonOnOff.setText(R.string.stop);
        		buttonReset.setClickable(false);
        	}
        	else
        	{
        		buttonOnOff.setText(R.string.start);
        		buttonReset.setClickable(true);        		
        	}
        }
        else
        {
        	buttonReset.setClickable(false);
        	switchTTS.setChecked(true);
        	seekBarVolume.setMax(100);
        	seekBarVolume.setProgress(30);
        	
        	prevLoc = null;
        	total_distance=0;
        	running = false;
        	total_time = 0;
        	last_km = 0;
        	last_km_time = 0;
        	timer = null;
        }
        updateTime();
    	updateDistance();
    	
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(false);
        provider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(provider, 5*1000, 0, this);
        System.out.println(provider);
        
        mTts = new TextToSpeech(this, this);
    }
    
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putDouble("total_distance", total_distance);
        savedInstanceState.putLong("total_time", total_time);
        savedInstanceState.putLong("last_km", last_km);
        savedInstanceState.putLong("last_km_time", last_km_time);
        savedInstanceState.putBoolean("running", running);
        savedInstanceState.putParcelable("prevLoc", prevLoc);
        
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onInit(int initStatus)
    {
    	if (initStatus == TextToSpeech.SUCCESS)
    	{
    		mTts.setLanguage(Locale.getDefault());
    	}
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public void onProviderEnabled(String str)
    {
    	System.out.println(str);
    }
    
    @Override
    public void onProviderDisabled(String str)
    {
    	System.out.println(str);
    }
    
    @Override
    public void onLocationChanged(Location loc)
    {
    	//System.out.println(loc.toString());
    	if (prevLoc != null)
    	{
    		double distance = loc.distanceTo(prevLoc);
    		//System.out.println("Distance: " + String.valueOf(distance) + " m");
    		double t = 1e-9*(loc.getElapsedRealtimeNanos() - prevLoc.getElapsedRealtimeNanos());
    		//System.out.println("time: " + String.valueOf(t) + " s");
    		double speed = distance/t;
    		//System.out.println(String.valueOf(speed) + " m/s -> " + String.valueOf(speed*3.6) + " km/h");
    		speedField.setText(String.format("%.2f", speed*3.6) + " km/h");
    		if (running)
    		{
    			total_distance = total_distance + distance;
    			updateDistance();
    		}
    		
    	}
    	prevLoc = loc;
    }
    
    private void updateDistance() {
    	distanceField.setText(String.format("%.3f", total_distance/1e3) + " km");
    	long nb_km = Math.round(Math.floor(total_distance/1e3));
    	if ( nb_km > last_km)
    	{
    		last_km = nb_km;
    		if (switchTTS.isChecked())
    			speakUp(true);
        	last_km_time = total_time;
    	}
    }

    private List<Integer> secondsToHMS(int s){
    	List<Integer> out = new ArrayList<Integer>();
    	int hours = s / 3600;
    	out.add(hours);
    	s = s - hours * 3600;
    	int minutes = s / 60;
    	out.add(minutes);
    	s = s - minutes * 60; 
    	out.add(s);
    	return out;
    }
    
    private String secondsToString(int in_s){
    	String st = "";
    	List<Integer> t = secondsToHMS(in_s);
    	int h = t.get(0);
    	int m = t.get(1);
    	int s = t.get(2);

    	if (h>0)
    	{
    		st = st.concat(String.format(getResources().getString(R.string.hours_), h));
    	}
    	if ((m>0) || (h>0))
    	{
    		st = st.concat(getResources().getString(R.string.minutes_and_, m));
    	}
    	st = st.concat(getResources().getString(R.string.seconds, s));
    	
    	return st;
    }
    
	private void speakUp(boolean rounded){
    	HashMap<String, String> myParams = new HashMap();
    	myParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
    	myParams.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, String.valueOf((float) seekBarVolume.getProgress() / 100.));
    	String strDistance;
    	String strTime;
    	String strSpeed;
    	String strLastKm;
    	
    	double speed = total_distance / total_time;
    	
    	if (rounded)
    		strDistance = String.format(Locale.getDefault(), getResources().getString(R.string.kilometres_int), last_km);
    	else
    		strDistance = String.format(Locale.getDefault(), getResources().getString(R.string.kilometres_float), total_distance/1e3);
    	
    	strTime = secondsToString((int) total_time);
    	
    	strSpeed = getResources().getString(R.string.kilometers_per_hours, speed*3.6);
    	System.out.println(strDistance);
    	System.out.println(strTime);
    	System.out.println(strSpeed);
    	
    	mTts.speak(strDistance, TextToSpeech.QUEUE_ADD, myParams);
    	mTts.speak(strTime, TextToSpeech.QUEUE_ADD, myParams);
    	mTts.speak(strSpeed, TextToSpeech.QUEUE_ADD, myParams);
    	if (rounded)
    	{
    		long seconds = (total_time - last_km_time);
    		strLastKm = getResources().getString(R.string.Last_kilometer_in_, secondsToString((int) seconds));
    		System.out.println(strLastKm);
    		mTts.speak(strLastKm, TextToSpeech.QUEUE_ADD, myParams);
    	}
    }
    
    
    private void updateTime() {
    	List<Integer> t = secondsToHMS((int) total_time);
    	int h = t.get(0);
    	int m = t.get(1);
    	int s = t.get(2);
    	String st = "";
    	if (h>0)
    	{
    		st = st.concat(String.format("%dh", h));
    	}
    	if ((m>0) || (h>0))
    	{
    		st = st.concat(String.format("%d'", m));
    	}
    	st = st.concat(String.format("%02d''", s));
    	//st = String.format("%d''", s);
    	//Log.i("Me", st);
    	timeField.setText(st);
    }
    
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    	System.out.println(provider + ": " + String.valueOf(status));
    }
    
    public void onClickReset(View view){
    	total_distance = 0;
    	total_time = 0;
    	last_km_time = 0;
    	last_km = 0;
    	updateDistance();
    	updateTime();
    }
    
    public void onClickStart(View view){
    	if (running)
    	{
    		running = false;
    		timer.cancel();
    		timer = null;
    		buttonOnOff.setText(R.string.start);
    		buttonReset.setClickable(true);
    		if (switchTTS.isChecked())
    			speakUp(false);
    	}
    	else
    	{
    		running = true;
    		timer = new Timer();
    		timer.schedule(new UpdateTime(), 1000, 1000);
    		buttonOnOff.setText(R.string.stop);
    		buttonReset.setClickable(false);
    	}
 
    }
    
    public void onClickQuit(MenuItem item){
    	AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    	alertDialog.setTitle(R.string.quitting);
    	alertDialog.setMessage(getResources().getString(R.string.quit_sure));
    	alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.yes),
    			new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int which) {
    			finish();
    	    } });
    	alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.no), 
    			(DialogInterface.OnClickListener) null);
    	alertDialog.show();
    	
    }
    
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	mTts.shutdown();
    	if (timer != null)
    		timer.cancel();
    }

    class UpdateTime  extends TimerTask { 
	
    	public void run(){
    		tick();
		
    	}
    }
}