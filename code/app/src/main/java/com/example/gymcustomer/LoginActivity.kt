package com.example.gymcustomer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.gymcustomer.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Enable Firebase offline persistence (do this once per app)
        try {
            Firebase.database.setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Ignore if already enabled
        }

        // Check if already logged in - this works offline due to SharedPreferences
        if (sessionManager.isLoggedIn()) {
            // User is logged in, go directly to dashboard
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
        val fullName = binding.fullNameInput.text.toString().trim()

        if (uid.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "Please enter UID and full name", Toast.LENGTH_SHORT).show()
            return
        }

        val membersRef = Firebase.database.reference.child("Customers")

        // Enable persistence for this specific reference
        membersRef.keepSynced(true)

        membersRef.child(uid).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot.exists()) {
                    try {
                        val member = try {
                            snapshot.getValue(Member::class.java)
                        } catch (e: Exception) {
                            createMemberFromSnapshot(snapshot)
                        }

                        if (member != null) {
                            val memberFullName = "${member.safePersonalInfo().safeFirstName()} ${member.safePersonalInfo().safeLastName()}".trim()

                            if (memberFullName.equals(fullName, ignoreCase = true)) {
                                // Save login session
                                sessionManager.saveLoginSession(uid, fullName)

                                // Go to dashboard
                                val intent = Intent(this, DashboardActivity::class.java)
                                intent.putExtra("member_data", member)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(this, "Invalid name", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Failed to read member data", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Member not found", Toast.LENGTH_SHORT).show()
                }
            } else {
                // This might be an offline error - try to use cached data
                Toast.makeText(this, "Network error - using cached data if available", Toast.LENGTH_SHORT).show()
                tryOfflineLogin(uid, fullName)
            }
        }
    }

    private fun tryOfflineLogin(uid: String, fullName: String) {
        // Try to get cached data from Firebase
        val membersRef = Firebase.database.reference.child("Customers")
        membersRef.child(uid).get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.exists()) {
                try {
                    val member = try {
                        task.result.getValue(Member::class.java)
                    } catch (e: Exception) {
                        createMemberFromSnapshot(task.result)
                    }

                    if (member != null) {
                        val memberFullName = "${member.safePersonalInfo().safeFirstName()} ${member.safePersonalInfo().safeLastName()}".trim()

                        if (memberFullName.equals(fullName, ignoreCase = true)) {
                            // Save login session
                            sessionManager.saveLoginSession(uid, fullName)

                            val intent = Intent(this, DashboardActivity::class.java)
                            intent.putExtra("member_data", member)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Invalid name (offline)", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Offline data error", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No cached data available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToDashboard() {
        val uid = sessionManager.getUid()
        if (uid != null) {
            val membersRef = Firebase.database.reference.child("Customers")
            membersRef.keepSynced(true) // Keep this data synced for offline

            membersRef.child(uid).get().addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.exists()) {
                    try {
                        val member = try {
                            task.result.getValue(Member::class.java)
                        } catch (e: Exception) {
                            createMemberFromSnapshot(task.result)
                        }

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

    @Suppress("UNCHECKED_CAST")
    private fun createMemberFromSnapshot(snapshot: com.google.firebase.database.DataSnapshot): Member {
        return Member(
            personal_info = convertToMap(snapshot.child("personal_info")),
            membership = convertToMap(snapshot.child("membership")),
            gym_data = convertToMap(snapshot.child("gym_data")),
            attendance_history = convertToMap(snapshot.child("attendance_history"))
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertToMap(snapshot: com.google.firebase.database.DataSnapshot): Map<String, Any>? {
        return when (val value = snapshot.value) {
            is Map<*, *> -> value as? Map<String, Any>
            else -> null
        }
    }
}