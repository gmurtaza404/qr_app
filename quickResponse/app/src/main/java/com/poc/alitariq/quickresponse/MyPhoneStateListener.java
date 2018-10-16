package com.poc.alitariq.quickresponse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static java.lang.Thread.sleep;

public class MyPhoneStateListener extends PhoneStateListener {
	public static Boolean phoneRinging = false;
	public static Boolean offhook = false;
	public static Boolean ideal = false;
    private AudioManager myAudioManager;

	Context c;
	String sdCardRoot = Environment.getExternalStorageDirectory().toString()+ "/testing/";
    int currentVolume = 0;
    long lastStateChanged;
	public MyPhoneStateListener(Context con) {
		c = con;
	}

	public void writeToFile(String fileName,String str){
		OutputStream fos;
		try {
			fos = new FileOutputStream(sdCardRoot+fileName);
			byte[] b=str.getBytes();
			fos.write(b);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	};

	public String readFromFile(String fileName){
		String temp=null;
		FileInputStream fin;
		try {
			fin = new FileInputStream(sdCardRoot+fileName);
			byte[] b = new byte[fin.available()];
			fin.read(b);
			String s = new String(b);
			temp = s;
			fin.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return temp;
	}

	public void onCallStateChanged(int state, String incomingNumber) {

	    if (incomingNumber != null && incomingNumber.compareTo("")!=0) {
            writeToFile("qr_phoneNo", incomingNumber);
        }
//        System.out.println("State changed: "+ state);
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                ideal = true;
                phoneRinging = false;
                offhook = false;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                ideal = false;
                phoneRinging = false;
                offhook = true;
                break;
            case TelephonyManager.CALL_STATE_RINGING:
                ideal = false;
                phoneRinging = true;
                offhook = false;
                break;
        }

        if (offhook){
            long lastOffHook=System.currentTimeMillis();
            String strLastOffHook=Long.toString(lastOffHook);
            String str=readFromFile("qr_lastState");
            String str2=readFromFile("qr_lastStateChanged");
            if (str2.equalsIgnoreCase("")) {
                str2 = "0";
            }
            lastStateChanged = Long.parseLong(str2);
            if(str!=null && str.equals("offHook")) {}
            else{
                if (lastOffHook-lastStateChanged >1000) {
                    System.out.println("str: " + str + " lastStateChanged: " + lastStateChanged + " difference: " + (lastOffHook - lastStateChanged));
                    lastStateChanged = lastOffHook;
                    System.out.println(sdCardRoot + "qr_lastOffHook: " + lastOffHook);
                    String prev_Str = readFromFile("qr_lastOffHook");
                    writeToFile("qr_lastOffHook", prev_Str+ '\n' +strLastOffHook);
                    writeToFile("qr_lastState", "offHook");
                    writeToFile("qr_lastStateChanged", lastStateChanged + "");
                }
            }


        }

        if (ideal){
            long lastIdeal=System.currentTimeMillis();
            String strLastIdeal=Long.toString(lastIdeal);
            String str=readFromFile("qr_lastState");
            String str2=readFromFile("qr_lastStateChanged");
            if (str2.equalsIgnoreCase("")) {
                str2 = "0";
            }
            lastStateChanged = Long.parseLong(str2);
            if(str!=null && str.equals("ideal")){
            }else{
                if (lastIdeal-lastStateChanged >1000) {



                    System.out.println("str: " + str + " lastStateChanged: " + lastStateChanged + " difference: " + (lastIdeal - lastStateChanged));
                    lastStateChanged = lastIdeal;
                    System.out.println(sdCardRoot + "qr_lastIdeal: " + lastIdeal);
                    String prev_state = readFromFile("qr_lastState");
                    if (prev_state.compareTo("ringing")== 0) {
                        try {
                            sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (callLogs(c) == 5) {
                            System.out.println("rejected call fired!");
                            c.sendBroadcast(new Intent("CALL_RECEIVED"));
                        } else  {
                            System.out.println("missed call fired!");
                            c.sendBroadcast(new Intent("MISCALL_RECEIVED"));
                        }
                    }
                    String prev_Str = readFromFile("qr_lastIdeal");
                    writeToFile("qr_lastIdeal", prev_Str + '\n' + strLastIdeal);
                    writeToFile("qr_lastState", "ideal");
                    writeToFile("qr_lastStateChanged", lastStateChanged + "");
                }
            }
        }

        if(phoneRinging){
            long lastRinging=System.currentTimeMillis();
            String strLastRinging=Long.toString(lastRinging);
            String str=readFromFile("qr_lastState");
            String str2=readFromFile("qr_lastStateChanged");
            if (str2.equalsIgnoreCase("")) {
                str2 = "0";
            }
            lastStateChanged = Long.parseLong(str2);
            if(str!=null && str.equals("ringing")) {
            }
            else {
                if (lastRinging-lastStateChanged >1000) {
                    System.out.println("str: " + str + " lastStateChanged: " + lastStateChanged + " difference: " + (lastRinging - lastStateChanged));
                    lastStateChanged = lastRinging;
                    System.out.println(sdCardRoot + "lastRinging: " + lastRinging);
                    String prev_state = readFromFile("qr_lastState");
                    System.out.println("prev State: "+prev_state);
                    String prev_Str = readFromFile("qr_lastRinging");
                    writeToFile("qr_lastRinging", prev_Str + '\n' +strLastRinging);
                    writeToFile("qr_lastState", "ringing");
                    writeToFile("qr_lastStateChanged", lastStateChanged + "");
                }
            }
        }
	}

	public int callLogs(Context con) {
        Uri allCalls = Uri.parse("content://call_log/calls");
        Cursor cur = con.getContentResolver().query(allCalls, null, null, null, null);
        int type = 0;
        while (cur.moveToNext()) {
            cur.moveToFirst();
            String num = cur.getString(cur.getColumnIndex(CallLog.Calls.NUMBER));// for  number
            String name = cur.getString(cur.getColumnIndex(CallLog.Calls.CACHED_NAME));// for name
            String duration = cur.getString(cur.getColumnIndex(CallLog.Calls.DURATION));// for duration
            type = Integer.parseInt(cur.getString(cur.getColumnIndex(CallLog.Calls.TYPE)));// for call type, Incoming or out going
            System.out.println("Call Type received: " + Integer.toString(type));
            break;
        }
        return type;
    }
}
