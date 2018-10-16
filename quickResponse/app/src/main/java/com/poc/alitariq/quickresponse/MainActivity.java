package com.poc.alitariq.quickresponse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;

import static java.lang.Thread.sleep;

//import android.telecom.*;

public class MainActivity extends Activity {
    //UI handlers
    TextView userNotification;
    ImageView checkAnswer;

    String phoneNo = "03174305965"; //"03474406284";

    //variables
    int closed = 0;
    int data_state[] = {1000, 10000, 19000, 28000};
    int waiting_for_call_connect;
    Method telephonyEndCall;
    Object telephonyObject;
    Button resetButton;

    private static MainActivity activity;

    //Encoding
    Map<Object, String> charToBinary = null;
    Map<String, String> binaryToString = null;
    Map<Integer, String> callLengthToBinary = null;
    String suggestions;
    int numberOfStates = 4;

    String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath()+"/testing/";
//    String sdCardRoot = Environment.getDataDirectory().getAbsolutePath().toString()+ "/testing/";


    //utility functions
    public void askPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.MODIFY_AUDIO_SETTINGS);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
                //showExplanation("Permission Needed", "Rationale", Manifest.permission.READ_PHONE_STATE, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                        1);
            }
        } else {
            Toast.makeText(MainActivity.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }

    }
    public int waitForCallUsingVisualizer() {
        waiting_for_call_connect = 0;
        Visualizer mVisualizer = new Visualizer(0);
        mVisualizer.setEnabled(false);
        int capRate = Visualizer.getMaxCaptureRate();
        int capSize = Visualizer.getCaptureSizeRange()[1];
        mVisualizer.setCaptureSize(capSize);
        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] != -128) {
                        //yes detected
                        waiting_for_call_connect = 1;
                        break;
                    }
                }
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }
        };

        int status2 = mVisualizer.setDataCaptureListener(captureListener, capRate, true/*wave*/, false/*no fft needed*/);
        mVisualizer.setEnabled(true);
        while (true) {
            if (waiting_for_call_connect == 1) {
                break;
            }
        }
        mVisualizer.setEnabled(false);
        mVisualizer.release();
        return 1;

    }
    public void clearFile(String fileName) {
        OutputStream fos;
        try {
            fos = new FileOutputStream(sdCardRoot + fileName);//"lastOffHook"
            String str = "";
            byte[] b = str.getBytes();
            fos.write(b);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void makeFile(String fileName, File myDir) {
        //make dir
        String temp = null;
        FileInputStream fin;
        try {
            fin = new FileInputStream(sdCardRoot + fileName);
            byte[] b = new byte[fin.available()];
            fin.read(b);
            String s = new String(b);
            temp = s;
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File file = new File(myDir, fileName );
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(temp.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public File makeDir() {
        long name = System.currentTimeMillis();
        File myDir = new File(sdCardRoot );
        myDir.mkdirs();

        return myDir;
    }
    public Method disconnectInitializer() {

        try {
            Class<?> telephonyClass = Class.forName("com.android.internal.telephony.ITelephony");
            Class<?> telephonyStubClass = telephonyClass.getClasses()[0];
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Class<?> serviceManagerNativeClass = Class.forName("android.os.ServiceManagerNative");

            Object serviceManagerObject;
            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);
            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod(
                    "asInterface", IBinder.class);
            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");
            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(
                    serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface",
                    IBinder.class);
            telephonyObject = serviceMethod.invoke(null, retbinder);
            telephonyEndCall = telephonyClass.getMethod("endCall");


        } catch (Exception e) {
            e.printStackTrace();

        }
        return telephonyEndCall;
    }
    public void disconnectCall() {
        try {
            telephonyEndCall.invoke(telephonyObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public String readFromFile(String fileName) {
        String temp = null;
        FileInputStream fin;
        try {
            fin = new FileInputStream(sdCardRoot + fileName);
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
    public void myToast(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }
    public void send_func(final String symbols) {
        writeToFile("qr_lastState", "r");
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                String number = readFromFile("qr_phoneNo");
                System.out.println("calling number: "+number);
                callIntent.setData(Uri.parse("tel:" + number));

                System.out.println("before: " + System.currentTimeMillis());
                long l = System.currentTimeMillis();
                try {
                    startActivity(callIntent);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                try {
                    sleep(6000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                waitForCallUsingVisualizer();
                System.out.println("qr_connect: " + System.currentTimeMillis());
                String str0 = readFromFile("qr_connect");
                writeToFile("qr_connect", str0 + System.currentTimeMillis() + "\n");
                try {
                    System.out.println("Option received: "+symbols);
                    if (symbols.compareTo("option0") == 0) {
//                        System.out.println("1");
                        sleep(data_state[0]);
                    } else if (symbols.compareTo("option1") == 0) {
//                        System.out.println("2");
                        sleep(data_state[1]);
                    } else if (symbols.compareTo("option2") == 0) {
//                        System.out.println("3");
                        sleep(data_state[2]);
                    } else {
//                        System.out.println("4");
                        sleep(data_state[3]);
                    }
                    //Thread.sleep(data_time[count]);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                String str = readFromFile("qr_disconnect");
                writeToFile("qr_disconnect", str + System.currentTimeMillis() + "\n");
                disconnectCall();
                System.out.println("disconnect: " + System.currentTimeMillis());
                long currentTime = System.currentTimeMillis();
                String strCurrentTime = Long.toString(currentTime);
                String prev_Str = readFromFile("qr_lastMsgOneTick");
                System.out.println("One ticks : "+ strCurrentTime);
                writeToFile("qr_lastMsgOneTick", prev_Str + '\n' + strCurrentTime);

            }
        });
        t.start();
    }
    public void resetState() {
        clearFile("qr_lastOffHook");
        clearFile("qr_lastIdeal");
        clearFile("qr_lastRinging");
        clearFile("qr_lastMsgOneTick");
        clearFile("qr_lastMsgTwoTick");
        clearFile("qr_lastMsgBlueTick");
        clearFile("qr_lastRinging");
        clearFile("qr_connect");
        clearFile("qr_disconnect");
        clearFile("qr_lastMsgRetransmission");
        clearFile("qr_notifications");
        clearFile("qr_popup");
        clearFile("qr_confirmationMsg");
        clearFile("qr_ringingTime");
        writeToFile("qr_output", "sent");
        writeToFile("qr_lastState","ideal");
        writeToFile("qr_lastStateChanged", System.currentTimeMillis()+"");
    }
    public void responseProcess(String choice) {
        System.out.println("responding option"+choice);
            if (choice.compareTo("0")==0) {
                writeToFile("qr_menuChoice", choice+"");
            } else if (choice.compareTo("1")==0) {
                writeToFile("qr_menuChoice", choice+"");
            } else if (choice.compareTo("2")==0) {
                writeToFile("qr_menuChoice", choice+"");
            } else if (choice.compareTo("3")==0) {
                writeToFile("qr_menuChoice", choice+"");
            } else {
//                send_func("option9");

                System.out.println("unknown option received!");
            }

        CheckBox cBox = (CheckBox) findViewById(R.id.checkBoxSMS);
        if (cBox.isChecked()) {
            send_func_Msg("option"+choice);
        } else {
            send_func("option"+choice);
        }
            checkAnswer.setImageResource(R.drawable.sent);

    }

    private void send_func_Msg(String s) {
        try {
            phoneNo = readFromFile("qr_phoneNo");//phoneNum.getText().toString().substring(4);
            String msg = "qrAppSMS:"+s;
            SmsManager sms = SmsManager.getDefault();

            long currentTime = System.currentTimeMillis();
            String strCurrentTime = Long.toString(currentTime);
            String prev_Str = readFromFile("qr_lastMsgOneTick");
            System.out.println("One ticks : "+ strCurrentTime);
            writeToFile("qr_lastMsgOneTick", prev_Str + '\n' + strCurrentTime);

            sms.sendTextMessage(phoneNo, null, msg, null, null);


            currentTime = System.currentTimeMillis();
            strCurrentTime = Long.toString(currentTime);
            prev_Str = readFromFile("qr_lastMsgTwoTick");
            System.out.println("Two ticks : "+ strCurrentTime);
            writeToFile("qr_lastMsgTwoTick", prev_Str + '\n' + strCurrentTime);


            System.out.println("Message Sent to "+phoneNo+" successfully!");
            System.out.println("Message Sent: "+msg);
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(MainActivity.this, "Sms not Send", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        activity = this;
    }

    BroadcastReceiver broadcastCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            dialogBox();
        }
    };

    BroadcastReceiver broadcastMiscallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showAnswer();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        userNotification = (TextView) findViewById(R.id.textView2);
        checkAnswer = (ImageView) findViewById(R.id.imageView);
        resetButton = (Button) findViewById(R.id.b_reset);
        disconnectInitializer();
        askPermissions();
        registerReceiver(broadcastCallReceiver, new IntentFilter("CALL_RECEIVED"));
        registerReceiver(broadcastMiscallReceiver, new IntentFilter("MISCALL_RECEIVED"));
        //for main activity

        resetButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                resetState();
            }
        });
    }

    public void showAnswer() {

        String prev_Str = readFromFile("qr_confirmationMsg");
        long currentTime = System.currentTimeMillis();
        String strCurrentTime = Long.toString(currentTime);
        writeToFile("qr_confirmationMsg", prev_Str + '\n' + strCurrentTime);

        String lastIdle_time = readFromFile("qr_lastIdeal");
        String[] tempo = lastIdle_time.split("\n");
        lastIdle_time = tempo[tempo.length-1];
        String lastRinging_time = readFromFile("qr_lastRinging");
        tempo = lastRinging_time.split("\n");
        lastRinging_time = tempo[tempo.length-1];
        Long call_length = Long.parseLong(lastIdle_time) - Long.parseLong(lastRinging_time);
        int closest_state = 0;
        int difference = 30000;
        for (int i = 0; i < numberOfStates; i++) {
            if ((int) Math.abs(call_length - data_state[i]) < difference) {
                difference = (int) Math.abs(call_length - data_state[i]);
                closest_state = i;
            }
        }
        closest_state = trimClosest(closest_state);

        if (closest_state == 0) {
            userNotification.setText("I’ll call you later.");
        } else if (closest_state == 1) {
            userNotification.setText("Can’t talk now. Call me later?");
        } else if (closest_state == 2) {
            userNotification.setText("Can’t talk now. What’s up?");
        } else if (closest_state == 3) {
            userNotification.setText("I’ll call you right back");
        } else {
            userNotification.setText("transmission error");
        }

        try {
            phoneNo = readFromFile("qr_phoneNo");//phoneNum.getText().toString().substring(4);
            String msg = "qrApp:"+closest_state;
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNo, null, msg, null, null);

            System.out.println("Message Sent to "+phoneNo+" successfully!");
            System.out.println("Message Sent: "+msg);
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(MainActivity.this, "Sms not Send", Toast.LENGTH_SHORT).show();
        }

    }

    private int trimClosest(int closest_state) {
        return closest_state;
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        unregisterReceiver(broadcastCallReceiver);
        unregisterReceiver(broadcastMiscallReceiver);
        super.onDestroy();
        closed = 1;
    }

    public void dialogBox() {

        String status = readFromFile("qr_output");
        System.out.println(status);
        if (status.compareTo("pending") == 0) {
            return;
        } else {
            System.out.println("dialogBox called");
            final CharSequence options[] = new CharSequence[]{"I’ll call you later", "Can’t talk now. Call me later?", "Can’t talk now. What’s up?", "I’ll call you right back.", "cancel"};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Pick a Response");
            builder.setCancelable(true);
            builder.setInverseBackgroundForced(true);
            System.out.println("show menu list");
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (options[which].toString().compareTo("cancel") == 0) {
                        dialog.dismiss();
                    } else {
//                  Toast.makeText(getApplicationContext(), Integer.toString(which), Toast.LENGTH_LONG).show();
                        long currentTime = System.currentTimeMillis();
                        String strCurrentTime = Long.toString(currentTime);
                        String str0 = readFromFile("qr_popup");
                        writeToFile("qr_popup", str0 + "\n" + strCurrentTime);
                        responseProcess(Integer.toString(which));
                    }
                }
            });
            builder.show();
            writeToFile("qr_output", "pending");
        }
    }

    public static MainActivity instance() {
        return activity;
    }

    public void updateChatIndicator(String s) {
        String prev_choice = readFromFile("qr_menuChoice");
        System.out.println("Stored choice: "+ prev_choice+ " & received choice: "+s);
        if (prev_choice.compareTo(s)==0) {
            checkAnswer = (ImageView) findViewById(R.id.imageView);
            checkAnswer.setImageResource(R.drawable.read);
            writeToFile("qr_output", "sent");

            long currentTime = System.currentTimeMillis();
            String strCurrentTime = Long.toString(currentTime);
            String prev_Str = readFromFile("qr_lastMsgBlueTick");
            System.out.println("Blue ticks : "+ strCurrentTime);
            writeToFile("qr_lastMsgBlueTick", prev_Str + '\n' + strCurrentTime);

        } else {
            System.out.println("Redailing with option"+prev_choice);
            long currentTime = System.currentTimeMillis();
            String strCurrentTime = Long.toString(currentTime);
            String prev_Str = readFromFile("qr_lastMsgRetransmission");
            System.out.println("Retransmission : "+ strCurrentTime);
            writeToFile("qr_lastMsgRetransmission", prev_Str + '\n' + strCurrentTime);
            responseProcess(prev_choice);
        }
    }

    public void updateAnswer(String s) {
        String s_temp = "";
        if (s.compareTo("option0") == 0) {
            userNotification.setText("I’ll call you later");
            s_temp = "0";
        } else if (s.compareTo("option1") == 0) {
            userNotification.setText("Can’t talk now. Call me later?");
            s_temp = "1";
        } else if (s.compareTo("option2") == 0) {
            userNotification.setText("Can’t talk now. What’s up?");
            s_temp = "2";
        } else if (s.compareTo("option3") == 0) {
            userNotification.setText("I’ll call you right back");
            s_temp = "3";
        } else {
            userNotification.setText("transmission error");
        }

        try {
            phoneNo = readFromFile("qr_phoneNo");//phoneNum.getText().toString().substring(4);
            String msg = "qrApp:"+s_temp;
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNo, null, msg, null, null);

            System.out.println("Message Sent to "+phoneNo+" successfully!");
            System.out.println("Message Sent: "+msg);
        } catch (Exception e) {
            System.out.println(e);
            Toast.makeText(MainActivity.this, "Sms not Send", Toast.LENGTH_SHORT).show();
        }
    }
}
