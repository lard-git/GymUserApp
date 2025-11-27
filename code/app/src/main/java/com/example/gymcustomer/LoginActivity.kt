package com.example.gymcustomer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.gymcustomer.databinding.ActivityLoginBinding
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Enable Firebase offline persistence
        try {
            Firebase.database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Ignore if already enabled
        }

        // Check if already logged in - if yes, redirect to dashboard
        if (sessionManager.isLoggedIn()) {
            goToDashboard()
            return
        }

        showLoginScreen()
    }

    private fun showLoginScreen() {
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener {
            loginMember()
        }
    }

    private fun loginMember() {
        val uid = binding.uidInput.text.toString().trim()
        val fullNameInput = binding.fullNameInput.text.toString().trim()

        if (uid.isEmpty() || fullNameInput.isEmpty()) {
            Toast.makeText(this, "Please enter UID and full name", Toast.LENGTH_SHORT).show()
            return
        }

        val membersRef = Firebase.database.reference.child("Customers")

        // Enable persistence for this reference
        membersRef.keepSynced(true)

        membersRef.child(uid).get().addOnCompleteListener { task ->
            try {
                if (task.isSuccessful) {
                    val snapshot = task.result
                    if (snapshot.exists()) {
                        val member = snapshot.getValue(Member::class.java)
                        if (member != null) {
                            val dbFirstName = member.safePersonalInfo().safeFirstName()
                            val dbLastName = member.safePersonalInfo().safeLastName()

                            // Check both possible name formats
                            val isValidName = checkNameMatch(fullNameInput, dbFirstName, dbLastName)

                            if (isValidName) {
                                // Save login session
                                sessionManager.saveLoginSession(uid, "$dbFirstName $dbLastName")

                                // Go to dashboard
                                val intent = Intent(this, DashboardActivity::class.java)
                                intent.putExtra("member_data", member)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Name doesn't match our records", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Failed to read member data", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Member not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Login error: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(this, "Connection failed: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Checks if the input name matches the database name in either order
     * Supports: "Yuzuha Ukonami" OR "Ukonami Yuzuha"
     */
    private fun checkNameMatch(inputName: String, dbFirstName: String, dbLastName: String): Boolean {
        if (inputName.isBlank() || dbFirstName.isBlank() || dbLastName.isBlank()) {
            return false
        }

        // Normalize the names (trim and lowercase)
        val normalizedInput = inputName.toLowerCase(Locale.getDefault()).trim()
        val normalizedDbFirst = dbFirstName.toLowerCase(Locale.getDefault()).trim()
        val normalizedDbLast = dbLastName.toLowerCase(Locale.getDefault()).trim()

        // Create both possible combinations from database
        val dbNameNormalOrder = "$normalizedDbFirst $normalizedDbLast"
        val dbNameReverseOrder = "$normalizedDbLast $normalizedDbFirst"

        // Check if input matches either combination
        return normalizedInput == dbNameNormalOrder || normalizedInput == dbNameReverseOrder
    }

    private fun goToDashboard() {
        val uid = sessionManager.getUid()
        if (uid != null) {
            val membersRef = Firebase.database.reference.child("Customers")
            membersRef.keepSynced(true)

            membersRef.child(uid).get().addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.exists()) {
                    try {
                        val member = task.result.getValue(Member::class.java)
                        if (member != null) {
                            val intent = Intent(this, DashboardActivity::class.java)
                            intent.putExtra("member_data", member)
                            startActivity(intent)
                            finish()
                        } else {
                            // Couldn't load member data, go back to login
                            sessionManager.clearLoginSession()
                            showLoginScreen()
                        }
                    } catch (e: Exception) {
                        // Error loading data, but we can still try to show dashboard
                        sessionManager.clearLoginSession()
                        showLoginScreen()
                    }
                } else {
                    // No data available (offline and no cache)
                    Toast.makeText(this, "No data available offline", Toast.LENGTH_LONG).show()
                    sessionManager.clearLoginSession()
                    showLoginScreen()
                }
            }
        } else {
            sessionManager.clearLoginSession()
            showLoginScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any network resources
        try {
            System.gc()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}