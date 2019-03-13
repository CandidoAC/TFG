package ai.snips.snipsdemo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import ai.snips.hermes.InjectionOperation;
import ai.snips.hermes.InjectionRequestMessage;
import ai.snips.hermes.IntentMessage;
import ai.snips.hermes.SessionEndedMessage;
import ai.snips.hermes.SessionQueuedMessage;
import ai.snips.hermes.SessionStartedMessage;
import ai.snips.hermes.Slot;
import ai.snips.nlu.ontology.IntentClassifierResult;
import ai.snips.platform.SnipsPlatformClient;
import ai.snips.platform.SnipsPlatformClient.SnipsPlatformError;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;

import static ai.snips.hermes.InjectionKind.AddFromVanilla;
import static android.media.MediaRecorder.AudioSource.MIC;

public class MainActivity extends AppCompatActivity {


    private static final int AUDIO_ECHO_REQUEST = 0;
    private static final String TAG = "MainActivity";

    private static final int FREQUENCY = 16_000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private File assistantLocation;

    private SnipsPlatformClient client;

    private Intent service;

    private AudioRecord recorder;

    private List<String> LMedicamentos;
    private int id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate ( savedInstanceState );
        setContentView ( R.layout.activity_main );
        LMedicamentos=Arrays.asList("adiro","aspirina") ;
        assistantLocation = new File ( getFilesDir () , "snips" );
        extractAssistantIfNeeded ( assistantLocation );
        ensurePermissions ();
        final MainActivity m=this;
        //startSnips ();
        findViewById ( R.id.start ).setOnClickListener ( new OnClickListener () {
            @Override
            public void onClick(View view) {
                if (ensurePermissions ()) {
                    final Button button = (Button) findViewById ( R.id.start );
                    button.setEnabled ( false );
                    button.setText ( R.string.loading );

                    final View scrollView = findViewById ( R.id.scrollView );
                    scrollView.setVisibility ( View.GONE );

                    final View loadingPanel = findViewById ( R.id.loadingPanel );
                    loadingPanel.setVisibility ( View.VISIBLE );

                    startSnips ();

                    service = new Intent ( getApplicationContext () , ServiceProactive.class );
                    startService ( service );
                    ServiceProactive.setUpdateListener ( m );

                }
            }
        } );
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            client.disconnect ();
        }
        super.onDestroy ();
        stopService ( service );

    }

    private boolean ensurePermissions() {
        int status = ActivityCompat.checkSelfPermission ( MainActivity.this ,
                Manifest.permission.RECORD_AUDIO );
        if (status != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions ( MainActivity.this , new String[]{
                    Manifest.permission.RECORD_AUDIO ,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            } , AUDIO_ECHO_REQUEST );
            return false;
        }
        return true;
    }

    private void startSnips() {

        if (client == null) {
            // a dir where the assistant models was unziped. It should contain the folders
            // custom_asr, custom_dialogue, custom_hotword and nlu_engine
            File assistantDir = new File ( assistantLocation , "assistant" );

            client = new SnipsPlatformClient.Builder ( assistantDir )
                    .enableDialogue ( true ) // defaults to true
                    .enableHotword ( true ) // defaults to true
                    .enableSnipsWatchHtml ( true ) // defaults to false
                    .enableLogs ( true ) // defaults to false
                    .withHotwordSensitivity ( 0.5f ) // defaults to 0.5
                    .enableStreaming ( true ) // defaults to false
                    .enableInjection ( true ) // defaults to false
                    .build ();

            client.setOnPlatformReady ( new Function0 <Unit> () {
                @Override
                public Unit invoke() {
                    runOnUiThread ( new Runnable () {
                        @Override
                        public void run() {
                            findViewById ( R.id.loadingPanel ).setVisibility ( View.GONE );
                            findViewById ( R.id.scrollView ).setVisibility ( View.VISIBLE );
                            client.startSession ( null , new ArrayList <String> () ,
                                    false , null );

                            File f=new File ( getFilesDir () , "_snips/g2p" );
                            try {
                                MainActivity.unzip ( getBaseContext ().getAssets ().open ( "g2p.zip" ) ,f );
                                HashMap <String, List <String>> values = new HashMap <> ();
                                values.put ( "medicamento" , LMedicamentos );

                                InjectionOperation op = new InjectionOperation ( AddFromVanilla , values );

                                client.requestInjection ( new InjectionRequestMessage (
                                        Collections.singletonList ( op ) ,
                                        Collections. <String, List <String>>emptyMap () ,
                                        null , null ) );

                                Log.i ( "injection" , "Añadiendo valores" );
                            } catch (IOException e) {
                                e.printStackTrace ();
                            }
                            final Button button = findViewById ( R.id.start );
                            button.setEnabled ( true );
                            button.setText ( R.string.start_dialog_session );
                            button.setOnClickListener ( new OnClickListener () {
                                @Override
                                public void onClick(View view) {
                                    // programmatically start a dialogue session
                                    client.startSession ( null , new ArrayList <String> () ,
                                            false , null );
                                }
                            } );
                        }
                    } );
                    return null;
                }
            } );
            client.setOnPlatformError ( new Function1 <SnipsPlatformError, Unit> () {

                @Override
                public Unit invoke(SnipsPlatformError snipsPlatformError) {
                    findViewById ( R.id.loadingPanel ).setVisibility ( View.VISIBLE );
                    findViewById ( R.id.scrollView ).setVisibility ( View.GONE );

                    final Button button = findViewById ( R.id.start );
                    button.setEnabled ( false );
                    return null;
                }
            } );


            client.setOnHotwordDetectedListener ( new Function0 <Unit> () {
                @Override
                public Unit invoke() {
                    Log.d ( TAG , "an hotword was detected !" );
                    // Do your magic here :D
                    return null;
                }
            } );

            client.setOnIntentDetectedListener ( new Function1 <IntentMessage, Unit> () {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public Unit invoke(IntentMessage intentMessage) {
                    Log.d ( TAG , "received an intent: " + intentMessage );
                    // Do your magic here :D

                    // For now, lets just use a random sentence to tell the user we understood but
                    // don't know what to do

                    String salida = "";
                    IntentClassifierResult SnipsMessage = intentMessage.getIntent ();
                    String IntentName = "caguilary:Anadir";
                    if (SnipsMessage.getIntentName ().equals ( IntentName )) {
                        List <Slot> LSlot = intentMessage.getSlots ();
                        if (!LSlot.isEmpty ()) {
                            int med = -1, fecha = -1;
                            int i;
                            for (i = 0; i < LSlot.size (); i++) {
                                if (LSlot.get ( i ).getSlotName ().equals ( "Fecha" )) {
                                    fecha = i;
                                } else {
                                    if (LSlot.get ( i ).getSlotName ().equals ( "Medicamento" )) {
                                        med = i;
                                    }
                                }
                            }
                            Date Fecha = new Date ();
                            try {
                                String s = LSlot.get ( fecha ).getValue ().toString ();
                                s = s.substring ( s.indexOf ( "=" ) + 1 , s.indexOf ( "," ) );
                                Fecha = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss" ).parse ( s );
                            } catch (Exception e) {
                                e.printStackTrace ();
                            }
                            Calendar cal = Calendar.getInstance ();
                            SimpleDateFormat mothdate = new SimpleDateFormat ( "MMMM" );
                            cal.setTime ( Fecha );
                            String minutos = "";
                            switch (Fecha.getMinutes ()) {
                                case 0:
                                    minutos = "";
                                    break;
                                case 15:
                                    minutos = " y cuarto";
                                    break;
                                case 30:
                                    minutos = " y media";
                                    break;
                                case 45:
                                    minutos = " menos cuarto";
                                    break;
                                default:
                                    minutos = " y " + Fecha.getMinutes ();
                                    break;
                            }
                            id++;
                            salida = "Añadiendo recordatorio para el día  " + cal.get ( Calendar.DAY_OF_MONTH ) + " de " + mothdate.format ( cal.getTime () ) + " del " + cal.get ( Calendar.YEAR ) + " a las " + Fecha.getHours () + minutos;
                            String s = LSlot.get ( med ).getValue ().toString ();
                            String medicamento=s.substring ( s.indexOf ( "=" ) + 1 , s.indexOf ( ")" ) );
                            salida=salida+" tomar " + medicamento;
                            if (med == -1) {
                                ServiceProactive.addEvent ( new Event ( Fecha , String.valueOf ( id ) ) );
                            } else {
                                ServiceProactive.addEvent ( new Event ( Fecha , medicamento ) );
                            }
                        }
                    } else {

                        List <String> answers = Arrays.asList (
                                "This is only a demo app. I understood you but I don't know how to do that" ,
                                "Can you teach me how to do that?" ,
                                "Oops! This action has not be coded yet!" ,
                                "Yes Master! ... hum, ..., er, ... imagine this as been done" ,
                                "Let's pretend I've done it! OK?" );


                        int index = Math.abs ( ThreadLocalRandom.current ().nextInt () ) % answers.size ();
                        salida = answers.get ( index );
                    }
                    client.endSession ( intentMessage.getSessionId () , salida );
                    return null;
                }
            } );

            client.setOnListeningStateChangedListener ( new Function1 <Boolean, Unit> () {
                @Override
                public Unit invoke(Boolean isListening) {
                    Log.d ( TAG , "asr listening state: " + isListening );
                    // Do you magic here :D
                    return null;
                }
            } );

            client.setOnSessionStartedListener ( new Function1 <SessionStartedMessage, Unit> () {
                @Override
                public Unit invoke(SessionStartedMessage sessionStartedMessage) {
                    Log.d ( TAG , "dialogue session started: " + sessionStartedMessage );
                    return null;
                }
            } );

            client.setOnSessionQueuedListener ( new Function1 <SessionQueuedMessage, Unit> () {
                @Override
                public Unit invoke(SessionQueuedMessage sessionQueuedMessage) {
                    Log.d ( TAG , "dialogue session queued: " + sessionQueuedMessage );
                    return null;
                }
            } );

            client.setOnSessionEndedListener ( new Function1 <SessionEndedMessage, Unit> () {
                @Override
                public Unit invoke(SessionEndedMessage sessionEndedMessage) {
                    Log.d ( TAG , "dialogue session ended: " + sessionEndedMessage );
                    return null;
                }
            } );

            // This api is really for debugging purposes and you should not have features depending
            // on its output. If you need us to expose more APIs please do ask !
            client.setOnSnipsWatchListener ( new Function1 <String, Unit> () {
                public Unit invoke(final String s) {
                    runOnUiThread ( new Runnable () {
                        public void run() {
                            // We enabled html logs in the builder, hence the fromHtml. If you only
                            // log to the console, or don't want colors to be displayed, do not
                            // enable the option
                            ((EditText) findViewById ( R.id.text )).append ( Html.fromHtml ( s + "<br />" ) );
                            findViewById ( R.id.scrollView ).post ( new Runnable () {
                                @Override
                                public void run() {
                                    ((ScrollView) findViewById ( R.id.scrollView )).fullScroll ( View.FOCUS_DOWN );
                                }
                            } );
                        }
                    } );
                    return null;
                }
            } );

            // We enabled steaming in the builder, so we need to provide the platform an audio
            // stream. If you don't want to manage the audio stream do no enable the option, and the
            // snips platform will grab the mic by itself
            startStreaming ();

            client.connect ( this.getApplicationContext () );
        }
    }

    private volatile boolean continueStreaming = true;

    private void startStreaming() {
        continueStreaming = true;
        new Thread () {
            public void run() {
                android.os.Process.setThreadPriority ( Process.THREAD_PRIORITY_URGENT_AUDIO );
                runStreaming ();
            }
        }.start ();
    }

    private void runStreaming() {
        Log.d ( TAG , "starting audio streaming" );
        final int minBufferSizeInBytes = AudioRecord.getMinBufferSize ( FREQUENCY , CHANNEL , ENCODING );
        Log.d ( TAG , "minBufferSizeInBytes: " + minBufferSizeInBytes );

        recorder = new AudioRecord ( MIC , FREQUENCY , CHANNEL , ENCODING , minBufferSizeInBytes );
        recorder.startRecording ();

        while (continueStreaming) {
            short[] buffer = new short[minBufferSizeInBytes / 2];
            recorder.read ( buffer , 0 , buffer.length );
            if (client != null) {
                client.sendAudioBuffer ( buffer );
            }
        }
        recorder.stop ();
        Log.d ( TAG , "audio streaming stopped" );
    }


    @Override
    protected void onResume() {
        super.onResume ();
        if (client != null) {
            startStreaming ();
            client.resume ();
        }
    }

    @Override
    protected void onPause() {
        continueStreaming = false;
        if (client != null) {
            client.pause ();
        }
        super.onPause ();
    }

    private void extractAssistantIfNeeded(File assistantLocation) {
        File versionFile = new File ( assistantLocation ,
                "android_version_" + BuildConfig.VERSION_NAME );

        if (versionFile.exists ()) {
            return;
        }

        try {
            assistantLocation.delete ();
            MainActivity.unzip ( getBaseContext ().getAssets ().open ( "assistant.zip" ) ,
                    assistantLocation );

            versionFile.createNewFile ();
        } catch (IOException e) {
            return;
        }
    }

    private static void unzip(InputStream zipFile , File targetDirectory)
            throws IOException {
        ZipInputStream zis = new ZipInputStream ( new BufferedInputStream ( zipFile ) );
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry ()) != null) {
                File file = new File ( targetDirectory , ze.getName () );
                File dir = ze.isDirectory () ? file : file.getParentFile ();
                if (!dir.isDirectory () && !dir.mkdirs ())
                    throw new FileNotFoundException ( "Failed to make directory: " +
                            dir.getAbsolutePath () );
                if (ze.isDirectory ())
                    continue;
                FileOutputStream fout = new FileOutputStream ( file );
                try {
                    while ((count = zis.read ( buffer )) != -1)
                        fout.write ( buffer , 0 , count );
                } finally {
                    fout.close ();
                }
            }
        } finally {
            zis.close ();
        }

    }

    public void sendMessage(String s) {
        client.startSession ( null , new ArrayList <String> () ,
                true , null );

        //TODO triple mensaje

        //TODO  Comprobación de acptación/ignoracion del evento
        client.endSession ( "0" , s );
    }
}
