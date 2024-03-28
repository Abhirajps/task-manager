package com.app.mytracker

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class UserDataAggregator {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun aggregateUserDataForWeek(startDate: Date): List<String> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: error("User not logged in")

        val calendar = Calendar.getInstance().apply { time = startDate }
        val aggregatedData = AggregatedData()

        repeat(7) {
            val date = dateFormat.format(calendar.time)

            fetchAndSumDailyData(userId, date, aggregatedData)

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return@withContext presentAggregatedDataWithSuggestions(aggregatedData)
    }

    private suspend fun fetchAndSumDailyData(userId: String, date: String, aggregatedData: AggregatedData) {

        val dailyNutritionRef = database.reference.child("daily_nutrition").child(userId).child(date)
        val nutritionSnapshot = dailyNutritionRef.get().await()
        nutritionSnapshot.children.forEach {
            val calories = it.child("calories").getValue(String::class.java)?.toIntOrNull() ?: 0
            aggregatedData.totalCalories += calories
        }

        val dailyActivitiesRef = database.reference.child("daily_activities").child(userId).child(date)
        val activitiesSnapshot = dailyActivitiesRef.get().await()
        activitiesSnapshot.children.forEach {
            val time = it.child("time").getValue(String::class.java)?.toIntOrNull() ?: 0
            aggregatedData.totalActivityTime += time
        }

        val exerciseSessionsRef = database.reference.child("exercise_sessions").child(userId).child(date)
        val exerciseSnapshot = exerciseSessionsRef.get().await()
        exerciseSnapshot.children.forEach {
            val duration = it.child("duration").getValue(String::class.java)?.toIntOrNull() ?: 0
            aggregatedData.totalExerciseDuration += duration
        }

        val waterIntakeRef = database.reference.child("water_intake").child(userId).child(date)
        val waterIntakeSnapshot = waterIntakeRef.get().await()
        waterIntakeSnapshot.children.forEach {
            val waterIntake = it.child("water_intake").getValue(String::class.java)?.toIntOrNull() ?: 0
            aggregatedData.totalWaterIntake += waterIntake
        }
    }

    private fun presentAggregatedDataWithSuggestions(data: AggregatedData): List<String> {
        val resultsWithSuggestions = mutableListOf<String>()

        val calorieSuggestion = when {
            data.totalCalories > 2000 -> "Your calorie intake is high. Consider eating more fruits and vegetables."
            data.totalCalories < 1200 -> "Your calorie intake is low. Consider consulting with a dietitian."
            else -> "Your calorie intake is within a healthy range."
        }
        resultsWithSuggestions.add("Total Calories: ${data.totalCalories} \n$calorieSuggestion")

        val activitySuggestion = if (data.totalActivityTime < 150) {
            "Consider increasing your weekly activity time. Aiming for at least 150 minutes of moderate activity can improve your health."
        } else {
            "Great job on staying active!"
        }
        resultsWithSuggestions.add("Total Activity Time: ${data.totalActivityTime} minutes \n$activitySuggestion")

        val exerciseSuggestion = if (data.totalExerciseDuration < 75) {
            "Consider increasing your weekly exercise intensity for better cardiovascular health."
        } else {
            "Great job on maintaining a strong exercise routine!"
        }
        resultsWithSuggestions.add("Total Exercise Duration: ${data.totalExerciseDuration} minutes \n$exerciseSuggestion")

        val waterIntakeSuggestion = if (data.totalWaterIntake < 2000) {
            "Try to increase your water intake to at least 2 liters per day."
        } else {
            "Great job on staying hydrated!"
        }
        resultsWithSuggestions.add("Total Water Intake: ${data.totalWaterIntake} ml \n$waterIntakeSuggestion")

        return resultsWithSuggestions
    }

    private data class AggregatedData(
        var totalCalories: Int = 0,
        var totalActivityTime: Int = 0,
        var totalExerciseDuration: Int = 0,
        var totalWaterIntake: Int = 0
    )
}