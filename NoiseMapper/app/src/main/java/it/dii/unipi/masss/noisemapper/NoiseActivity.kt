package it.dii.unipi.masss.noisemapper

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import com.google.android.material.datepicker.MaterialDatePicker

import androidx.core.util.Pair
import java.util.Calendar
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

class NoiseActivity: AppCompatActivity() {

    private val RECORD_AUDIO_BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE = 101
    private lateinit var switchCompat: SwitchCompat
    private lateinit var webView: WebView
    private lateinit var graph: Graph
    private lateinit var timer: Timer
    private lateinit var bleConfig: BLEConfig
    private lateinit var noise_map_io: NoiseMapIO
    private lateinit var notGrantedPermissions : Array<String>
    private val requiredPermissions = arrayOf(Manifest.permission.RECORD_AUDIO) +
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                else arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) // Note that there is no need to ask about ACCESS_FINE_LOCATION anymore for BT scanning purposes for VERSION_CODES.S and higher if you add android:usesPermissionFlags="neverForLocation" under BLUETOOTH_SCAN in your manifest file.
    private var bluetoothAdapter : BluetoothAdapter? = null
    var startDate:Long = 0
    var endDate:Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.noise_activity)
        bleConfig = BLEConfig(this)
        if (!bleConfig.gotConfig()){
            Toast.makeText(
                this,
                "Please connect to Internet for the first app lunch",
                Toast.LENGTH_LONG
            ).show()
            finish() // go back to main activity
        }
        noise_map_io = NoiseMapIO(this.resources.getString(R.string.serverURL))
        graph = Graph(filesDir.absolutePath, bleConfig)
        webView = findViewById(R.id.map_web_view)
        switchCompat = findViewById(R.id.sensing_on_off)
        Log.i("NoiseActivity", "Noise activity started")
        // create the graph
        val timeAnHourBefore = Calendar.getInstance()
        timeAnHourBefore.add(Calendar.HOUR_OF_DAY, -1*24*7*4) // TODO: nocheckin
        updateMap(startTS = timeAnHourBefore.timeInMillis)

        webView.settings.javaScriptEnabled = true;
        webView.settings.allowFileAccess = true;
        webView.settings.builtInZoomControls = true;
        //webView.webViewClient = WebViewClient()
        webView.loadUrl("file://" + filesDir.absolutePath + "/output.html")
        bluetoothAdapter = android.bluetooth.BluetoothManager::class.java.cast(
            getSystemService(android.content.Context.BLUETOOTH_SERVICE)
        )?.adapter
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
        while (!permissionCheck()) {
            requestPermissions()
        }
        enableBT()
        initializeSensors()
        startSensing()
        scheduleUpdate()
    }

    @SuppressLint("MissingPermission")
    private fun enableBT() {
        // check that bluetooth is enabled, if not, ask the user to enable it
        if (!(bluetoothAdapter?.isEnabled)!!) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            // start the intent to enable bluetooth with a callback for when the user enables it

            startActivity(enableBtIntent) // TODO: look for a way to get the result of the intent
            // TODO: probably use startActivityForResult instead of startActivity
        }
    }

    private fun startSensing() {
        TODO("Not yet implemented")
    }

    private fun initializeSensors() {
        while (!(bluetoothAdapter?.isEnabled)!!) {
            continue // TODO: if the user doesn't enable the bluetooth, the app will be stuck here forever
        }
        TODO("Not yet implemented")
    }

    private fun permissionCheck(): Boolean {
        // check if the permissions are already granted
        notGrantedPermissions = requiredPermissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        return notGrantedPermissions.isEmpty()
    }

    private fun requestPermissions(){
        if (notGrantedPermissions.isEmpty()) {
            Log.w("NoiseMapper", "Permissions already granted")
            return
        }
        // request the permissions
        ActivityCompat.requestPermissions(
            this,
            notGrantedPermissions,
            RECORD_AUDIO_BLUETOOTH_SCAN_PERMISSION_REQUEST_CODE
        )
        return
    }

    private fun scheduleUpdate() {
        // start a timer
        timer = fixedRateTimer(initialDelay = 2000, period = 10*1000) {
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
        webView.loadUrl("file://" + filesDir.absolutePath + "/output.html")
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

    override fun onStop(){
        super.onStop()
        if (inSensingState()){
            switchCompat.isChecked = false
        }
    }
}

