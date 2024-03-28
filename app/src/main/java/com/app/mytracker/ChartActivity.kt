package com.app.mytracker

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class ChartActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        val anyChartView = findViewById<AnyChartView>(R.id.any_chart_view)
        anyChartView.setProgressBar(findViewById(R.id.progress_bar))


        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()



        fetchAndDisplayData()
    }


    private fun fetchAndDisplayData() {

        var dataType = intent.getStringExtra("data")

        if (dataType == null)
            dataType = ""



        val (itemName, valueOfItem) = when (dataType) {
            "daily_nutrition" -> Pair("food", "calories")
            "water_intake" -> Pair("intake", "water_intake")
            "daily_activities" -> Pair("activity", "time")
            "exercise_sessions" -> Pair("exercise", "duration")
            else -> Pair("", "")
        }


        val userId = auth.currentUser?.uid ?: return
        val userReference = database.reference
            .child(dataType)
            .child(userId)

        val dateEntries = mutableMapOf<String, MutableList<NutritionEntry>>()

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { dateSnapshot ->
                    val date = dateSnapshot.key ?: "Unknown Date"
                    val entriesForDate = mutableListOf<NutritionEntry>()
                    dateSnapshot.children.forEach { entrySnapshot ->

                        val item = if (dataType == "water_intake") {
                            "waterIntake"
                        } else
                            entrySnapshot.child(itemName).getValue(String::class.java)
                        val value = entrySnapshot.child(valueOfItem).getValue(String::class.java)
                        if (item != null && value != null) {
                            entriesForDate.add(NutritionEntry(item, value))
                        }
                    }
                    dateEntries[date] = entriesForDate
                }
                setupDateSpinner(ArrayList(dateEntries.keys), dateEntries)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@ChartActivity,
                    "Failed to fetch data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupDateSpinner(
        dates: ArrayList<String>,
        dateEntries: Map<String, MutableList<NutritionEntry>>
    ) {
        val spinner: Spinner = findViewById(R.id.date_spinner)
        val adapter: ArrayAdapter<String> =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, dates)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selectedDate = parent.getItemAtPosition(position) as String
                displayPieCharts(selectedDate, dateEntries[selectedDate] ?: emptyList())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    private fun displayPieCharts(date: String, nutritionEntries: List<NutritionEntry>) {
        val dataType = intent.getStringExtra("data") ?: ""
        val pie = AnyChart.pie()

        if (dataType == "water_intake") {
            val totalIntake = nutritionEntries.sumOf { it.calories.toIntOrNull() ?: 0 }
            val remaining = 3000 - totalIntake
            val data = mutableListOf<DataEntry>()
            Log.e("dsd", "displayPieCharts: $totalIntake")

            if (totalIntake > 0) data.add(ValueDataEntry("Intake", totalIntake))
            if (remaining > 0) data.add(ValueDataEntry("Remaining", remaining))

            pie.title("Water Intake on $date")
            pie.labels().format("{%Value} ml")
            pie.data(data)

            pie.legend().title().enabled(true)
            pie.legend().title().useHtml(true)
            pie.legend().title().text("Goal: 3000ml")

        }else {

            val data: MutableList<DataEntry> = mutableListOf()
            nutritionEntries.forEach { entry ->
                val calories = try {
                    entry.calories.toDouble()
                } catch (e: NumberFormatException) {
                    0.0
                }
                data.add(ValueDataEntry(entry.food, calories))
            }

            pie.data(data)
            pie.title("Daily ${intent.getStringExtra("data")} on $date")

        }
        val anyChartView = findViewById<AnyChartView>(R.id.any_chart_view)
        anyChartView.setChart(pie)
    }
}

data class NutritionEntry(val food: String, val calories: String)
