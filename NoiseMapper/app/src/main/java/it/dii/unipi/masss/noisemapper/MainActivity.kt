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
        editText.text= editText.text.append("http://")
        findViewById<View>(R.id.button_noise_map).setOnClickListener {
            // get the editText text field and check if it is empty
            var text = editText.text
            if (text.isEmpty()) {
                Log.i("MainActivity", "Empty text field")
                return@setOnClickListener
            }
            else{
                // concat :5002 to the text val
                text = text.append(":5002")
                Log.i("MainActivity", "Text field not empty $text")
                Log.i("MainActivity", "Starting NoiseActivity")
                // start the NoiseActivity and addextra the text val
                val intent = Intent(this, NoiseActivity::class.java)
                intent.putExtra("serverURL", text.toString())
                startActivity(intent)
            }

        }

    }


}
