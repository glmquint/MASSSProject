package it.dii.unipi.masss.noisemapper

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import android.os.SystemClock.sleep
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.datepicker.MaterialDatePicker

import androidx.core.util.Pair

class NoiseActivity: AppCompatActivity() {

    var startDate:Long = 0
    var endDate:Long = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.noise_activity)
        Log.i("NoiseActivity", "Noise activity started")
        // call the server function in order to obtain the config file
        val bleConfig =BLEConfig(this)
        // if no connection check that there is the config file
        // if no config file, show a toast message
        if (!bleConfig.gotConfig()){
            Toast.makeText(
                this,
                "Please connect to Internet for the first app lunch",
                Toast.LENGTH_LONG
            ).show()
            finish() // go back to main activity
        }
        // create the graph
        val noise_map_io = NoiseMapIO(this);
        var roomNoise  = noise_map_io.performGetRequest(System.currentTimeMillis()-this.resources.getInteger(R.integer.MILLISECONDS_IN_A_WEEK), System.currentTimeMillis())

        val graph = Graph(this, bleConfig)
        graph.makeplot(roomNoise) //

        val webView : WebView = findViewById(R.id.map_web_view)
        webView.settings.javaScriptEnabled = true;
        webView.settings.allowFileAccess = true;
        //webView.webViewClient = WebViewClient()
        // I'd like to to this:
        webView.loadUrl("file://" + filesDir.absolutePath + "/output.html")
        startDate = System.currentTimeMillis()-this.resources.getInteger(R.integer.MILLISECONDS_IN_A_WEEK)
        endDate = System.currentTimeMillis()
        val pickDateButton: Button = findViewById(R.id.pick_date_button)
        pickDateButton.setOnClickListener {
            val dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Select dates")
                    .setSelection(
                        Pair(
                            startDate,
                            endDate
                        )
                    )
                    .build()
            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                // selection is a Pair<Long, Long> representing the selected range

                startDate = selection.first
                endDate = selection.second
                //here i get the room noise
                graph.makeplot(noise_map_io.performGetRequest(startDate,endDate))
                //TODO call here the function that creates the map

                Log.i("MainActivity", "Date range selected: $startDate - $endDate")
            }
            dateRangePicker.show(supportFragmentManager, "dateRangePicker")
        }

        // register for the switch compat change event
        val switchCompat: SwitchCompat = findViewById(R.id.sensing_on_off)
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            Log.i("NoiseMapper","Switch event")
            if (isChecked){
                Log.i("NoiseMapper","Switch is on")
                //TODO start the sensing
            }else{
                Log.i("NoiseMapper","Switch is off")
                //TODO stop the sensing
            }
        }

        }
    }

