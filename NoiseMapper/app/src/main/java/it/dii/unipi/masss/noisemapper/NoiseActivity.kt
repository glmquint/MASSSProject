package it.dii.unipi.masss.noisemapper

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.DatePicker
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker

import androidx.core.util.Pair

class NoiseActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pickDateButton: Button = findViewById(R.id.pick_date_button)
        pickDateButton.setOnClickListener {
            val dateRangePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                    .setTitleText("Select dates")
                    .setSelection(
                        Pair(
                            MaterialDatePicker.thisMonthInUtcMilliseconds(),
                            MaterialDatePicker.todayInUtcMilliseconds()
                        )
                    )
                    .build()
            dateRangePicker.addOnPositiveButtonClickListener { selection ->
                // selection is a Pair<Long, Long> representing the selected range
                val startDate = selection.first
                val endDate = selection.second
                Log.i("MainActivity", "Date range selected: $startDate - $endDate")
            }
            dateRangePicker.show(supportFragmentManager, "dateRangePicker")
        }
    }

}
