package com.poc.alitariq.quickresponse;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static java.lang.Thread.sleep;

/**
 * Created by ali tariq on 13/03/2018.
 */
public class MyBroadcastReceiver extends BroadcastReceiver
{
    TelephonyManager tm;
    String extra_foreground_call_state;
    String sdCardRoot = Environment.getExternalStorageDirectory().toString()+ "/testing/";
//    String sdCardRoot = Environment.getDataDirectory().getAbsolutePath().toString()+ "/testing/";
    String last_State = "none";
    long time_stamp = 0;
    final SmsManager sms = SmsManager.getDefault();

    @Override
    public void onReceive(Context context, final Intent intent)
    {

        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                String sender = "";
                for (int i = 0; i < pdusObj.length; i++) {
                    SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = sms.getDisplayOriginatingAddress();
                    sender = phoneNumber;
                    String message = sms.getDisplayMessageBody();

                    System.out.println("Msg received: ("+sender+"):"+ message);
                    String[] msg = message.split(":");
                    if(msg[0].compareTo("qrApp")==0) {
                        MainActivity inst = MainActivity.instance();
                        inst.updateChatIndicator(msg[1]);
                        writeToFile("mcq_phoneNo", sender);
                    } else if (msg[0].compareTo("qrAppSMS")==0) {
                        MainActivity inst = MainActivity.instance();
                        inst.updateAnswer(msg[1]);
                        writeToFile("mcq_phoneNo", sender);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void appendToFile(String fileName,String str){
        OutputStream fos;
        try {
//            System.out.println(str);
            String prev = readFromFile(fileName);
            prev = prev + "\n" + str;
            fos= new FileOutputStream(sdCardRoot+fileName);
            byte[] b=prev.getBytes();
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
    public void writeToFile(String fileName,String str){
        OutputStream fos;
        try {
//            System.out.println(str);
            fos= new FileOutputStream(sdCardRoot+fileName);
            byte[] b=str.getBytes();
            fos.write(b);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

}
