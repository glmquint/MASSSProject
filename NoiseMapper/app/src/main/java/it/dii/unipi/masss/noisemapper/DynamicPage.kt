package it.dii.unipi.masss.noisemapper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class DynamicPage: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dynamic_page)
        val button: Button = findViewById(R.id.return_button_dynamic_page)
        button.setOnClickListener {
            // Create an Intent to return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            // Close the current activity
            onStop()
        }
    }
    override fun onStop() {
        super.onStop()
    }

}
