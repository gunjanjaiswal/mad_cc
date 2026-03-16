package com.example.mad_cc

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Auto-login check - only if email is verified
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val createAccountText = findViewById<TextView>(R.id.createAccountText)

        loginBtn.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginBtn.isEnabled = false
                loginBtn.text = "LOGGING IN..."

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null && user.isEmailVerified) {
                                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, DashboardActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                                loginBtn.isEnabled = true
                                loginBtn.text = "LOG IN"
                            }
                        } else {
                            Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            loginBtn.isEnabled = true
                            loginBtn.text = "LOG IN"
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter Email and Password", Toast.LENGTH_SHORT).show()
            }
        }

        createAccountText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
