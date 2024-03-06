package com.app.mytracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var autoCompleteFood: AutoCompleteTextView
    private lateinit var editTextCalories: EditText
    private lateinit var editTextWaterIntake: EditText
    private lateinit var buttonAddToDatabase: Button
    private lateinit var buttonAddWaterDatabase: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var textViewMotivationalQuote: TextView
    private lateinit var firestore: FirebaseFirestore


    private val PREF_NAME = "ReminderPrefs"
    private val KEY_REMINDERS_SCHEDULED = "remindersScheduled"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        firestore = FirebaseFirestore.getInstance()

        textViewMotivationalQuote = findViewById(R.id.textViewMotivationalQuote)
        autoCompleteFood = findViewById(R.id.autoCompleteFood)
        editTextCalories = findViewById(R.id.editTextCalories)
        buttonAddToDatabase = findViewById(R.id.buttonAddToDatabase)
        buttonAddWaterDatabase = findViewById(R.id.buttonRecordIntake)
        editTextWaterIntake = findViewById(R.id.editTextWaterIntake)

        val foodSuggestions = arrayOf("Apple", "Banana", "Chicken", "Salad", "Pasta")
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, foodSuggestions)
        autoCompleteFood.setAdapter(adapter)


        buttonAddToDatabase.setOnClickListener {
            val food = autoCompleteFood.text.toString()
            val calories = editTextCalories.text.toString()

            if (food.isNotEmpty() && calories.isNotEmpty()) {
                addToDatabase(food, calories)
            } else {
                Toast.makeText(this, "Please enter both food and calories", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        buttonAddWaterDatabase.setOnClickListener {
            val water = editTextWaterIntake.text.toString()

            if (water.isNotEmpty()) {
                recordWaterIntake(water)
            } else {
                Toast.makeText(this, "Please enter water intake in ml", Toast.LENGTH_SHORT).show()
            }
        }


        checkAndRequestPermissions()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermissions()) {
                scheduleReminders()
            }
        } else {
            scheduleReminders()
        }


        fetchRandomQuote()


    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun checkPermissions(): Boolean {
        val setAlarmPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.SCHEDULE_EXACT_ALARM
        ) == PackageManager.PERMISSION_GRANTED

        val notificationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        return setAlarmPermissionGranted && notificationPermissionGranted
    }

    fun checkAndRequestPermissions() {
        // Check and request SET_ALARM permission
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.SCHEDULE_EXACT_ALARM
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM),
                100
            )
        } else {
            // SET_ALARM permission already granted, check and request notification permission
            checkAndRequestNotificationPermission()
        }
    }

    fun checkAndRequestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        } else {
            // Both permissions granted, proceed to schedule reminders
            scheduleReminders()
        }
    }

    private fun scheduleReminders() {
        val sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (!sharedPreferences.getBoolean(KEY_REMINDERS_SCHEDULED, false)) {
            val notificationService = NotificationService()
            notificationService.createNotificationChannel(this)
            notificationService.scheduleNotifications(this)

            sharedPreferences.edit {
                putBoolean(KEY_REMINDERS_SCHEDULED, true)
            }
            Log.d("ReminderScheduler", "Reminders scheduled")

        } else {
            Log.d("ReminderScheduler", "Reminders already scheduled")
        }

    }


    private fun addToDatabase(food: String, calories: String) {
        val userId = auth.currentUser?.uid
        val userReference =
            database.reference.child("daily_nutrition").child(userId ?: "").child("nutrition")

        val entryKey = userReference.push().key

        val nutritionData = mapOf(
            "food" to food,
            "calories" to calories
        )

        userReference.child(entryKey ?: "").setValue(nutritionData)
            .addOnSuccessListener {
                Toast.makeText(
                    this, "Food entry added to database successfully",
                    Toast.LENGTH_SHORT
                ).show()

                autoCompleteFood.text.clear()
                editTextCalories.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this, "Failed to add food entry to database. ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun recordWaterIntake(waterIntake: String) {
        val userId = auth.currentUser?.uid
        val userReference =
            database.reference.child("water_intake").child(userId ?: "").child("water_intake")

        val entryKey = userReference.push().key

        val waterIntakeData = mapOf(
            "water_intake" to waterIntake
        )

        userReference.child(entryKey ?: "").setValue(waterIntakeData)
            .addOnSuccessListener {
                Toast.makeText(
                    this, "Water intake recorded successfully",
                    Toast.LENGTH_SHORT
                ).show()

                editTextWaterIntake.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this, "Failed to record water intake. ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    private fun fetchRandomQuote() {
        firestore.collection("quotes").document("pmEXRDOgZeFPoaNFGTz0")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    val document = task.result
                    if (document!!.exists()) {
                        val quoteFields = document.data?.values?.toTypedArray()

                        val random = Random()
                        val randomIndex = random.nextInt(quoteFields?.size ?: 0)
                        val randomQuote = quoteFields?.get(randomIndex).toString()

                        textViewMotivationalQuote.text = randomQuote
                    }
                }
            }
    }

}