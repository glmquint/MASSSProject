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
import java.util.Calendar
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

abstract class NoiseActivity: AppCompatActivity() {

    private lateinit var graph: Graph
    private lateinit var timer: Timer
    private lateinit var bleConfig: BLEConfig
    private lateinit var noise_map_io: NoiseMapIO
    var startDate:Long = 0
    var endDate:Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.noise_activity)
        bleConfig = BLEConfig(this)
        noise_map_io = NoiseMapIO(this.resources.getString(R.string.serverURL))
        graph = Graph(this, bleConfig)
        Log.i("NoiseActivity", "Noise activity started")
        // call the server function in order to obtain the config file
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
        var roomNoise  = noise_map_io.performGetRequest(System.currentTimeMillis()-this.resources.getInteger(R.integer.MILLISECONDS_IN_A_WEEK), System.currentTimeMillis())

        graph.makeplot(roomNoise) //

        val webView : WebView = findViewById(R.id.map_web_view)
        webView.settings.javaScriptEnabled = true;
        webView.settings.allowFileAccess = true;
        webView.settings.builtInZoomControls = true;
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
                    graph.makeplot(noise_map_io.performGetRequest(startDate, endDate))
                    // webView.loadUrl("file://" + filesDir.absolutePath + "/output.html") // TODO: check if we really need this line
                    Log.i("MainActivity", "Date range selected: $startDate - $endDate")
                    updateMap(startDate, endDate)
            }
            if (inSensingState()) {
                dateRangePicker.show(supportFragmentManager, "dateRangePicker")
            } else {
                updateMap()
            }
        }

        // register for the switch compat change event
        val switchCompat: SwitchCompat = findViewById(R.id.sensing_on_off)
        switchCompat.setOnCheckedChangeListener { _, isChecked ->
            Log.i("NoiseMapper","Switch event")
            if (isChecked){
                Log.i("NoiseMapper","Switch is on")
                enterSensingState()
            }else{
                Log.i("NoiseMapper","Switch is off")
                exitSensingState()
            }
        }
    }

    private fun inSensingState(): Boolean {
        return findViewById<SwitchCompat>(R.id.sensing_on_off).isChecked
    }

    fun enterSensingState(){
        Log.i("NoiseMapper","Entering sensing state")
        if (permissionCheck()){
            initializeSensors()
            startSensing()
            scheduleUpdate()
        }
    }

    private fun startSensing() {
        TODO("Not yet implemented")
    }

    private fun initializeSensors() {
        TODO("Not yet implemented")
    }

    private fun permissionCheck(): Boolean {
        TODO("Not yet implemented")
    }

    private fun scheduleUpdate() {
        // start a timer
        timer = fixedRateTimer(initialDelay = 2000, period = 10*1000) {
            // Get current time

            updateMap()
        }
    }

    private fun updateMap(startTS : Long? = null, endTS : Long? = null) {
        val currentTime = Calendar.getInstance()

        // Get time an hour before
        val timeAnHourBefore = Calendar.getInstance()
        timeAnHourBefore.add(Calendar.HOUR_OF_DAY, -1)

        // Convert to Unix timestamp
        val currentUnixTimestamp = endTS?:currentTime.timeInMillis
        val previousHourUnixTimestamp = startTS?:timeAnHourBefore.timeInMillis

        // Log the results
        Log.i("PollingRequest", "Current time: ${currentTime.time}")
        Log.i("PollingRequest", "Unix timestamp of current time: $currentUnixTimestamp")

        Log.i("PollingRequest", "Time an hour before: ${timeAnHourBefore.time}")
        Log.i(
            "PollingRequest",
            "Unix timestamp of time an hour before: $previousHourUnixTimestamp"
        )
        graph.makeplot(noise_map_io.performGetRequest(previousHourUnixTimestamp / 1000, currentUnixTimestamp / 1000))

    }

    fun exitSensingState(){
        Log.i("NoiseMapper","Exiting sensing state")
        stopSensing()
        stopUpdate()
    }

    private fun stopSensing() {
        TODO("Not yet implemented")
    }

    private fun stopUpdate() {
        timer.cancel()
    }
}

