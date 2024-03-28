package com.app.mytracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var autoCompleteFood: AutoCompleteTextView
    private lateinit var editTextCalories: EditText
    private lateinit var editTextWaterIntake: EditText
    private lateinit var timeEditText: EditText
    private lateinit var activitySpinner: Spinner
    private lateinit var buttonAddToDatabase: Button
    private lateinit var buttonAddWaterDatabase: Button
    private lateinit var buttonAddExerciseToDb: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var textViewMotivationalQuote: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var spinnerExercise: Spinner
    private lateinit var editTextSessionLength: EditText


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
                addNutritionToDatabase(food, calories)
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

        setDailyActivityCard()


        fetchRandomQuote()

        spinnerExercise = findViewById(R.id.spinnerExerciseType)
        editTextSessionLength = findViewById(R.id.editTextSessionLength)
        buttonAddExerciseToDb = findViewById(R.id.buttonRecordExercise)

        setupExerciseSpinner()
        


    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun setUpTextSwitcher(aggregateUserDataForWeek: List<String>) {

        var  currentIndex = 0
        val  textSwitcher:TextSwitcher = findViewById(R.id.textSwitcher)


        textSwitcher.setFactory {
            val textView = TextView(this)
            textView.gravity = Gravity.CENTER
            textView.textSize = 16f

            textView
        }

        val inAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val outAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        textSwitcher.inAnimation = inAnimation
        textSwitcher.outAnimation = outAnimation

        textSwitcher.setText(aggregateUserDataForWeek[currentIndex])

        GlobalScope.launch {
            while (true) {
                delay(4000)
                currentIndex = (currentIndex + 1) % aggregateUserDataForWeek.size
                withContext(Dispatchers.Main) {
                    textSwitcher.setText(aggregateUserDataForWeek[currentIndex])
                }
            }
        }

        textSwitcher.setOnClickListener{
            currentIndex = (currentIndex + 1) % aggregateUserDataForWeek.size
            textSwitcher.setText(aggregateUserDataForWeek[currentIndex])
        }
    }

    private fun setupExerciseSpinner() {
        val exercises = arrayOf("Chest", "Leg", "Biceps", "Shoulder", "Abs")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, exercises)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerExercise.adapter = adapter

        timeEditText = findViewById(R.id.editTextActivityTime)

        buttonAddExerciseToDb.setOnClickListener {
            val exercise = spinnerExercise.selectedItem.toString()
            val sessionLength = editTextSessionLength.text.toString()

            recordExerciseSession(exercise,sessionLength)
        }
    }

    private fun setDailyActivityCard() {

        val dailyActivities = arrayOf("Running", "Cycling", "Swimming", "Yoga", "Gym")

        activitySpinner = findViewById(R.id.spinnerDailyActivities)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dailyActivities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        activitySpinner.adapter = adapter

        timeEditText = findViewById(R.id.editTextActivityTime)

        val recordButton: Button = findViewById(R.id.buttonRecordActivity)
        recordButton.setOnClickListener {
            val selectedActivity = activitySpinner.selectedItem.toString()
            val enteredTime = timeEditText.text.toString()

            recordActivity(selectedActivity,enteredTime)
        }
    }

    private fun recordActivity(activity: String, time: String) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = dateFormat.format(java.util.Date())

        val userId = auth.currentUser?.uid ?: return

        val dateReference = database.reference
            .child("daily_activities")
            .child(userId)
            .child(date)

        val entryKey = dateReference.push().key ?: return

        val activityData = mapOf(
            "activity" to activity,
            "time" to time
        )

        dateReference.child(entryKey).setValue(activityData)
            .addOnSuccessListener {
                Toast.makeText(this, "Activity entry added to database successfully", Toast.LENGTH_SHORT).show()
                activitySpinner.setSelection(0)
                timeEditText.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add activity entry to database. ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
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


    private fun addNutritionToDatabase(food: String, calories: String) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = dateFormat.format(java.util.Date())

        val userId = auth.currentUser?.uid ?: return

        val dateReference = database.reference
            .child("daily_nutrition")
            .child(userId)
            .child(date)

        val entryKey = dateReference.push().key ?: return

        val nutritionData = mapOf(
            "food" to food,
            "calories" to calories
        )

        dateReference.child(entryKey).setValue(nutritionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Food entry added to database successfully", Toast.LENGTH_SHORT).show()
                autoCompleteFood.text.clear()
                editTextCalories.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add food entry to database. ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun recordWaterIntake(waterIntake: String) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = dateFormat.format(java.util.Date())

        val userId = auth.currentUser?.uid ?: return

        val dayReference = database.reference
            .child("water_intake")
            .child(userId)
            .child(date)

        val entryKey = dayReference.push().key ?: return
        val waterIntakeData = mapOf(
            "water_intake" to waterIntake
        )

        dayReference.child(entryKey).setValue(waterIntakeData)
            .addOnSuccessListener {
                Toast.makeText(this, "Water intake recorded successfully", Toast.LENGTH_SHORT).show()
                editTextWaterIntake.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to record water intake. ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun recordExerciseSession(exercise: String, duration: String) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = dateFormat.format(java.util.Date())

        val userId = auth.currentUser?.uid ?: return

        val exerciseSessionReference = database.reference
            .child("exercise_sessions")
            .child(userId)
            .child(date)

        val sessionKey = exerciseSessionReference.push().key ?: return

        val exerciseSessionData = mapOf(
            "exercise" to exercise,
            "duration" to duration
        )

        exerciseSessionReference.child(sessionKey).setValue(exerciseSessionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Exercise session added to database successfully.", Toast.LENGTH_SHORT).show()
                activitySpinner.setSelection(0)
                timeEditText.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add exercise session to database. ${e.message}", Toast.LENGTH_SHORT).show()
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