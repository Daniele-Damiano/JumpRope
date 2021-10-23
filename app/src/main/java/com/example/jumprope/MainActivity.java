/*
* Sviluppatore Damiano Daniele
* Quando ruota lo schermo non distrugge l'activity e i dati sono preservati ( vedi file manifest )
* startSchedule fa partire il timer e il metodo checkTerminationAndRestart fa partire un
* Thread che controlla la teriminazione dello schedule e del thread stesso.
* */

package com.example.jumprope;

import android.media.MediaPlayer;
import android.os.Bundle;

import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    int conteggio, totale_allenamento, tempo_set_input;
    long tempo_set, pausa_tra_set;
    boolean stop;
    ScheduledFuture<?> future;
    ScheduledExecutorService schedule;
    Thread t;
    TextView tempoDaVisualizzareTextView, setCompletiTextView, fineAllenamentoTextView;
    EditText totale_allenamento_edit_text, pausa_set_edit_text, set_edit_text;
    ToggleButton toggleButton;
    MediaPlayer fermo, riparti;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       initializeView();

       initializeMediaPlayer();

       initializeSchedule();

        conteggio =0;





        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){



                    stop = true;
                    getTextfromView();

                    /* Lancio il primo schedule(thread) per il conuntdown*/
                    startSchedule();

                    /*Lancia un Thread Indipendente che controlla la terminazione dell'app e la ripartenza*/
                    checkTerminationAndRestart();

                }else{


                    azzeraVariabiliTextViewAndStopThreadAndSchedule();

                }
            }
        });
    }


   //Esegue il contdouwn, countdown viene eseguita ogni secondo tramite il metodo scheduleAtFixedRate
    // post e come runOnuithread, esegue il codice nel main Thread cio nel metodo onCreate() per aggiornare la TextView
    //Altrimenti non si aggiorna vedi qua: https://developer.android.com/guide/components/processes-and-threads#java
    private void startSchedule() {
        Runnable conuntdown = new Runnable() {
            @Override
            public void run() { // Esegue lo schedule(Thread)
                if (tempo_set > 0) {
                    //Log.d("DEBUG", "Tempo set: " + tempo_set);
                    tempo_set--;
                    tempoDaVisualizzareTextView.post(new Runnable() {
                        @Override
                        public void run() {
                            tempoDaVisualizzareTextView.setText("" + tempo_set);
                        }
                    });
                }
            }

        };

        future = schedule.scheduleAtFixedRate(conuntdown, 0, 1, TimeUnit.SECONDS); // si ripete ogni secondo

    }


    //Thred controlla la terminazione del countdown
    //conteggio sono i minuti complessivi, conteggio = 1 è passato un minuto di allenamento
    //Quando conteggio e arrivato al limite ferma il ciclo while  e si ferma
    // runOnUiThread esegui il codice nel main Thread per aggiornare la TextView
    //Altrimenti non si aggiorna vedi qua: https://developer.android.com/guide/components/processes-and-threads#java
    private void checkTerminationAndRestart() {
        
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (stop) { // termina il thread quando e false
                    if (conteggio == totale_allenamento) {

                        toggleButton.post(new Runnable() {
                            @Override
                            public void run() {
                                toggleButton.setChecked(false);
                            }
                        });
                        fineAllenamentoTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                fineAllenamentoTextView.setText("FINE ALLENAMENTO");
                            }
                        });

                        break; // esce dal ciclo forzato termina il thread
                    }
                    if (tempo_set == 0 && conteggio != totale_allenamento) {
                        ++conteggio;

                        fermo.start();
                        // Si ferma - converte in millisecondi, pausa_tra_set * 1000 cioe
                        // se e 30 allora 30.000 millisecondi che equivale a 30 secondi
                        try {
                            Thread.sleep(pausa_tra_set * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        riparti.start();

                        tempo_set = TimeUnit.MILLISECONDS.toSeconds(tempo_set_input * 60 * 1000); //riparte il timer

                        setCompletiTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                setCompletiTextView.setText("" + conteggio + "/" + "" + totale_allenamento);
                            }
                        });
                    }
                }
                Log.d("DEBUG","EXIT FROM WHILE");
                future.cancel(true); // cancella lo schedule
                if(future.isCancelled()){
                    Log.d("DEBUG","Schedule Cancel");
                }else{
                    Log.d("DEBUG","Schedule Error");
                }
                if(future.isDone()){
                    Log.d("DEBUG","Is DOne");
                }else{
                    Log.d("DEBUG","Schedule Error");
                }

            }
        });
        t.start();
    }

    private  void initializeSchedule(){
        //Crea un thread con un solo thread nel thread pool
        schedule = Executors.newScheduledThreadPool(1);
    }

    private void initializeView(){
        tempoDaVisualizzareTextView = (TextView) findViewById(R.id.tempo_trascorso);
        setCompletiTextView = (TextView) findViewById(R.id.setCompletati);
        totale_allenamento_edit_text = (EditText) findViewById(R.id.durataAllenamento);
        pausa_set_edit_text = (EditText) findViewById(R.id.pausaTraSet);
        set_edit_text = (EditText) findViewById(R.id.Set);
        fineAllenamentoTextView = (TextView) findViewById(R.id.fine_allenamento);
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
    }

    private void initializeMediaPlayer() {
        fermo = MediaPlayer.create(this, R.raw.fermo);
        riparti = MediaPlayer.create(this, R.raw.riparti);
    }


    private void getTextfromView(){

        Toast.makeText(getApplicationContext(), "Start", Toast.LENGTH_SHORT).show();

        //Minuti Complessivi dell'allenamento
        String tot_Allenamento = totale_allenamento_edit_text.getText().toString();
        totale_allenamento = Integer.parseInt(tot_Allenamento);

        // Tempo di allenamento: ad esempio un minuti di allenamento con la corda
        // tempo_set_input * 60 * 1000 converte in minuti
        // se tempo_set_input = 1 allora 1 minuto convertito in secondi, cioe 1*60*1000 = 60000 secondi = 1 minuto
        String set_allenamento = set_edit_text.getText().toString();
        tempo_set_input = Integer.parseInt(set_allenamento);
        tempo_set = TimeUnit.MILLISECONDS.toSeconds(tempo_set_input * 60 * 1000); // converte minuti in millisecondi a secondi


        //Pausa tra gli allenamento
        // Ad eseempio pausa di 30 secondi dopo un minuto di allenamento
        String pausa = pausa_set_edit_text.getText().toString();
        pausa_tra_set = Integer.parseInt(pausa);

        //Azzera le TextView
        setCompletiTextView.setText("");
        fineAllenamentoTextView.setText("");
    }


    private void azzeraVariabiliTextViewAndStopThreadAndSchedule() {
        Toast.makeText(getApplicationContext(), "Stop", Toast.LENGTH_SHORT).show();
        stop = false; // Ferma il Thread in checkTerminationAndRestart
        tempo_set = 0;
        conteggio = 0;
        tempoDaVisualizzareTextView.setText("");
        setCompletiTextView.setText("");
        fineAllenamentoTextView.setText("");
        future.cancel(true);
        if(future.isCancelled()){
            Log.d("DEBUG","azzeraVariabiliTextViewAndStopThreadAndSchedule Schedule Cancel");
        }else{
            Log.d("DEBUG","azzeraVariabiliTextViewAndStopThreadAndSchedule Schedule Error");
        }
        if(future.isDone()){
            Log.d("DEBUG","azzeraVariabiliTextViewAndStopThreadAndSchedule Is DOne");
        }else{
            Log.d("DEBUG","azzeraVariabiliTextViewAndStopThreadAndSchedule Schedule Error");
        }
    }

    /*private void stopThreadStopSchedule(){
        stop = false;
        boolean test = future.cancel(true);
        if(test == false){
            Log.d("DEBUG","ERROR CANCEL SCHEDULE");
        }
    }*/



    @Override
    protected void onStop() {
        super.onStop();
        fermo.release();
        riparti.release();
        fermo = null;
        riparti = null;
        //schedule.shutdownNow();
        //t.interrupt();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }
}


