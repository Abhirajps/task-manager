package com.app.mytracker

import android.app.DatePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import java.util.Calendar

class RegistrationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        val nameEditText: EditText = findViewById(R.id.nameEditText)
        val emailEditText: EditText = findViewById(R.id.emailEditTextRegistration)
        val passwordEditText: EditText = findViewById(R.id.passwordEditTextRegistration)
        val dobEditText: EditText = findViewById(R.id.dobEditText)
        val sexSpinner: Spinner = findViewById(R.id.sexSpinner)
        val registerButton: Button = findViewById(R.id.registerButton)



        val cal = Calendar.getInstance()
        val dobDatePicker = DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                dobEditText.setText(selectedDate)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        dobEditText.setOnClickListener {
            dobDatePicker.show()
        }


        val sexOptions = arrayOf("Male", "Female", "Other")
        val sexAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sexOptions)
        sexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sexSpinner.adapter = sexAdapter

        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val dob = dobEditText.text.toString()
            val sex = sexSpinner.selectedItem.toString()

            registerUser(name, email, password, dob, sex)
        }

    }

    private fun registerUser(name: String, email: String, password: String, dob: String, sex: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user: FirebaseUser? = auth.currentUser
                    saveUserDetailsToDatabase(user?.uid, name, dob, sex, email)
                } else {
                    Toast.makeText(
                        baseContext, "Registration failed. ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveUserDetailsToDatabase(userId: String?, name: String, dob: String, sex: String, email: String) {
        val userReference = database.reference.child("users").child(userId ?: "")
        val userDetails = UserDetails(name, dob, sex, email)

        userReference.setValue(userDetails)
            .addOnSuccessListener {
                Toast.makeText(
                    baseContext, "Registration success",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(Intent(this,MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    baseContext, "Failed to save user details to database. ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    data class UserDetails(val name: String, val dob: String, val sex: String, val email: String)



}