package com.app.mytracker

import android.content.Intent
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
    private lateinit var myTargetIL: TextInputLayout
    private lateinit var myTargetEt: TextInputEditText
    private var targetValue: Int = 0
    private lateinit var anyChartView: AnyChartView

    var defaultTargets = mutableMapOf(
        "water_intake" to 3000,       // ml for water intake
        "daily_nutrition" to 1600,    // kcal for daily nutrition
        "exercise_sessions" to 20,    // minutes for exercise sessions
        "daily_activities" to 60      // minutes for daily activities
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        anyChartView = findViewById<AnyChartView>(R.id.any_chart_view)
        anyChartView.setProgressBar(findViewById(R.id.progress_bar))


        myTargetIL = findViewById(R.id.MyTargetIL)
        myTargetEt = myTargetIL.editText as TextInputEditText


        auth = Firebase.auth
        database = FirebaseDatabase.getInstance()


        getTargetData()


        myTargetIL.setEndIconOnClickListener {
            if (myTargetEt.text.isNullOrEmpty()) {
                Toast.makeText(this, "Please input a value", Toast.LENGTH_SHORT).show()
            } else {
                recordActivity()
            }
        }
    }

    private fun getTargetData() {
        val userId = auth.currentUser?.uid ?: return

        val dateReference = database.reference
            .child(intent.getStringExtra("data").toString())
            .child(userId)
            .child("user_target")

        dateReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val dataType = intent.getStringExtra("data") ?: return
                if (!dataSnapshot.exists()) {
                    defaultTargets[dataType]?.let { target ->
                        myTargetEt.setText("$target")
                        fetchAndDisplayData()
                        return
                    }
                }
                val value = dataSnapshot.getValue(Long::class.java)
                if (value != null) {
                    if (value <= 0) {
                        defaultTargets[dataType]?.let { target ->
                            myTargetEt.setText("$target")
                        }

                        val target = defaultTargets[dataType] ?: 0
                        Toast.makeText(this@ChartActivity, "$target", Toast.LENGTH_SHORT).show()
                    } else {
                        defaultTargets[dataType] = value.toInt()
                        myTargetEt.setText(value.toString())
                    }

                }



                fetchAndDisplayData()

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("DatabaseError", databaseError.toException())
            }
        })
    }


    private fun recordActivity() {
        val value = myTargetEt.text.toString().toIntOrNull() ?: -1
        val userId = auth.currentUser?.uid ?: return

        val dateReference = database.reference
            .child(intent.getStringExtra("data").toString())
            .child(userId)




        dateReference.child("user_target").setValue(value)
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Target updated successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()

                val dataType = intent.getStringExtra("data") ?: ""

                val intent = Intent(this, ChartActivity::class.java)
                intent.putExtra("data",dataType)
                startActivity(intent)

            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this, "Failed to update. ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun retrieveTargetData() {
        /*val value = myTargetEt.text.toString().toIntOrNull() ?: -1
        if (value <= 0 ) {
            val dataType = intent.getStringExtra("data") ?: return
            defaultTargets[dataType]?.let { target ->
                myTargetEt.setText("$target")
            }
        }*/
        val userId = auth.currentUser?.uid ?: return
        val dateReference = database.reference
            .child(intent.getStringExtra("data").toString())
            .child(userId)
        val entryKey = dateReference.push().key ?: return
        dateReference.child(entryKey).get()
            .addOnSuccessListener {
                Toast.makeText(
                    this,
                    "Activity entry added to database successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }.addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to add activity entry to database. ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun fetchAndDisplayData() {
        val dataType = intent.getStringExtra("data") ?: ""

        val (itemName, valueOfItem) = when (dataType) {
            "daily_nutrition" -> Pair("food", "calories")
            "water_intake" -> Pair("intake", "water_intake")
            "daily_activities" -> Pair("activity", "time")
            "exercise_sessions" -> Pair("exercise", "duration")
            else -> Pair("", "")
        }

        val userId = auth.currentUser?.uid ?: return
        val userReference = database.reference.child(dataType).child(userId)

        userReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(
                        this@ChartActivity,
                        "No data.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val dateEntries = mutableMapOf<String, MutableList<NutritionEntry>>()

                dataSnapshot.children.forEach { dateSnapshot ->
                    val date = dateSnapshot.key ?: "Unknown Date"
                    val entriesForDate = mutableListOf<NutritionEntry>()
                    dateSnapshot.children.forEach { entrySnapshot ->
                        val item = if (dataType == "water_intake") {
                            "waterIntake"
                        } else entrySnapshot.child(itemName).getValue(String::class.java)

                        val value = entrySnapshot.child(valueOfItem).getValue(String::class.java)

                        if (item != null && value != null) {
                            entriesForDate.add(NutritionEntry(item, value))
                        }
                    }
                    if (entriesForDate.isNotEmpty()) {
                        dateEntries[date] = entriesForDate
                    }
                }
                if (dateEntries.isNotEmpty()) {
                    setupDateSpinner(ArrayList(dateEntries.keys), dateEntries)
                } else {

                    val defaultDate = "No Data"
                    val defaultEntries = mutableListOf(NutritionEntry("Intake", "0"))
                    val defaultDateEntries =
                        mutableMapOf<String, MutableList<NutritionEntry>>().apply {
                            put(defaultDate, defaultEntries)
                        }
                    setupDateSpinner(arrayListOf(defaultDate), defaultDateEntries)
                    Toast.makeText(
                        this@ChartActivity,
                        "No entries found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle the error more gracefully
                Log.e("DatabaseError", "Failed to fetch data: ${databaseError.message}")
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
        val dataType = intent.getStringExtra("data") ?: return
        val pie = AnyChart.pie()

        val target = defaultTargets[dataType] ?: 0
        val totalValue = nutritionEntries.sumOf { it.calories.toIntOrNull() ?: 0 }
        val remaining = target - totalValue
        val data = mutableListOf<DataEntry>()

        if (totalValue > 0) data.add(ValueDataEntry("Done", totalValue))
        if (remaining > 0) data.add(ValueDataEntry("Remaining", remaining))

        pie.data(data)
        pie.title("${dataType.replace("_"," ")} on $date")

        pie.legend().title().enabled(true)
        pie.legend().title().useHtml(true)
        val unit = when (dataType) {
            "water_intake" -> "ml"
            "daily_nutrition" -> "kcal"
            else -> "min"
        }
        pie.legend().title().text("Goal: $target$unit")

        val anyChartView = findViewById<AnyChartView>(R.id.any_chart_view)
        anyChartView.setChart(pie)
    }

}

data class NutritionEntry(val food: String, val calories: String)
