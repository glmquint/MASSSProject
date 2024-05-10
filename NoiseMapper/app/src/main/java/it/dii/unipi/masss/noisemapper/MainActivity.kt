package it.dii.unipi.masss.noisemapper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        View.OnClickListener { view ->
            when (view.id) {
                R.id.button_noise_map -> {
                    Log.i("MainActivity", "Noise map button clicked")
                    val intent = Intent(this, NoiseMap::class.java)
                    startActivity(intent)
                }
            }
        }
    }


}
