package ai.snips.snipsdemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceProactive extends Service {
    private Timer temporizador=new Timer (  );
    private static  final long Intervalo=1;
    private String sInitial,String="Evento detectado para ahora";
    private static MainActivity fragment;
    private Handler handle;
    private static List<Event> lEvent;
    private static String TAG="ServiceSnips";

    public static  void setUpdateListener(MainActivity fr){
        fragment=fr;
    }
    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate() {
        super.onCreate ();
        sInitial=String;
        lEvent=new ArrayList <Event> (  );
        Date date=new Date((2019-1900),3,5,20,14);//El mes es uno más(va de 0 a 11),año = año actual-1900
        lEvent.add ( new Event ( date,"prueba" ) );
        handle=new Handler (){
            @Override
            public void handleMessage(Message msg) {//TODO arreglar mandar mensaje proactivo
                fragment.sendMessage(String);
                String=sInitial;
                Log.i (TAG,"Mandando mensaje:"+String);
            }
        };
       getMessage ();
    }

    @Override
    public void onDestroy() {
        super.onDestroy ();
        Log.i ( "Service","Destroy service" );
    }

    private void getMessage() {
        Date FechaActual=new Date (  );
        FechaActual.setSeconds ( 0 );
        FechaActual.setMinutes ( FechaActual.getMinutes ()+1 );
        temporizador.schedule ( new TimerTask () {
            @Override
            public void run() {
                Date FechaActual=new Date (  );
                FechaActual.setSeconds ( 0 );
                for(int i=0;i<lEvent.size();i++){
                    if(lEvent.get ( i ).getFecha ().toString ().equals (FechaActual.toString ())){
                        String s=String;
                        String=String.concat ( "->"+lEvent.get ( i ).getNombre ()) ;
                        Log.i(TAG,"Hay evento ");
                        handle.sendEmptyMessageDelayed  ( 0,1);
                        lEvent.remove ( i );
                    }
                }
            }
        },FechaActual,Intervalo );
    }

    public static void addEvent(Event e){
        lEvent.add ( e );
        Log.i ( TAG,"añadiendo evento "+e.toString () );
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
