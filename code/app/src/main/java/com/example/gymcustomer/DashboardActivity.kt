package com.example.gymcustomer

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.example.gymcustomer.databinding.ActivityDashboardBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.max

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var currentMember: Member
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Get member data from intent
        currentMember = intent.getSerializableExtra("member_data") as? Member ?: run {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            performLogout()
            return
        }

        setupUI()
        updateDashboard()
        generateQRCode()
        setupBackHandler()
    }

    private fun setupUI() {
        val fullName = "${currentMember.safePersonalInfo().safeFirstName()} ${currentMember.safePersonalInfo().safeLastName()}"
        binding.welcomeText.text = "Welcome, $fullName"

        // Setup logout button
        binding.logoutBtn.setOnClickListener {
            performLogout()
        }
    }

    private fun updateDashboard() {
        val remainingDays = calculateRemainingDays()
        binding.daysRemaining.text = "$remainingDays days remaining"

        binding.totalVisits.text = "Visits: ${currentMember.safeGymData().safeTotalVisits()}"
        binding.totalTime.text = "Time spent: ${currentMember.safeGymData().safeTotalTimeSpent()} mins"
    }

    private fun calculateRemainingDays(): Int {
        val endDateStr = currentMember.safeMembership().safeEndDate()
        if (endDateStr.isEmpty()) return 0

        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val endDate = sdf.parse(endDateStr)
            val today = Date()

            val diff = endDate.time - today.time
            max(0, ceil(diff / (1000.0 * 60 * 60 * 24)).toInt())
        } catch (e: Exception) {
            0
        }
    }

    private fun generateQRCode() {
        val qrContent = currentMember.safeGymData().safeUid()
        if (qrContent.isEmpty()) return

        try {
            val hints = mapOf<EncodeHintType, Any>(EncodeHintType.MARGIN to 1)
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 500, 500, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            binding.qrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun performLogout() {
        sessionManager.clearLoginSession()
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Move app to background instead of going back to login
                moveTaskToBack(true)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        try {
            System.gc()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}