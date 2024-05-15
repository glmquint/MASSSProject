package it.dii.unipi.masss.noisemapper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val editText = findViewById<EditText>(R.id.editText)
        //editText.text= editText.text.append("http://")
        findViewById<View>(R.id.button_noise_map).setOnClickListener {
            // get the editText text field and check if it is empty
            var text_server : String = editText.text.toString()
            if (text_server.isEmpty()) {
                Log.i("MainActivity", "Empty text field")
                return@setOnClickListener
            }
            else{
                // concat :5002 to the text_server do not change the UI
                val http = "http://"
                text_server = http.plus(text_server).plus(":5002")
                Log.i("MainActivity", "Text field not empty $text_server")
                Log.i("MainActivity", "Starting NoiseActivity")
                // start the NoiseActivity and addextra the text val
                val intent = Intent(this, NoiseActivity::class.java)
                intent.putExtra("serverURL", text_server.toString())
                startActivity(intent)
            }

        }

    }


}
