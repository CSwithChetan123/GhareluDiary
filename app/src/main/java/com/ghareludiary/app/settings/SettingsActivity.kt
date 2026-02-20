package com.ghareludiary.app.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ghareludiary.app.GSignIn
import com.ghareludiary.app.R
import com.ghareludiary.app.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val auth = FirebaseAuth.getInstance()

    private val PRIVACY_POLICY_URL = "https://cswithchetan123.github.io/ghareludiary-privacyPolicy/"
    private val TERMS_URL = "https://cswithchetan123.github.io/ghareludiary-termsOfService/"
    private val FEEDBACK_FORM_URL = "https://forms.gle/Q8ssHN2wN8LKVvoNA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ADD THIS ONE LINE:
        binding.imageButton.setOnClickListener { finish() }

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.tvAppVersion.text = "v1.0.4"  // Just hardcode it
    }

    private fun setupClickListeners() {
        binding.cardPrivacyPolicy.setOnClickListener {
            openUrl(PRIVACY_POLICY_URL)
        }

        binding.cardTerms.setOnClickListener {
            openUrl(TERMS_URL)
        }

        binding.cardFeedback.setOnClickListener {
            openUrl(FEEDBACK_FORM_URL)
        }

        binding.btnSignOut.setOnClickListener {
            showSignOutDialog()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                signOut()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun signOut() {
        auth.signOut()
        val intent = Intent(this, GSignIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage(
                "⚠️ WARNING: This will permanently delete:\n\n" +
                        "• Your account\n" +
                        "• All your entries\n" +
                        "• All synced data\n\n" +
                        "This action CANNOT be undone."
            )
            .setPositiveButton("Delete Forever") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(this, "No user signed in", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress
        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Deleting account...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        // Delete account
        user.delete()
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()

                // Go to sign-in screen
                val intent = Intent(this, GSignIn::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()

                // Check if it's a re-authentication error
                if (e.message?.contains("requires-recent-login") == true ||
                    e.message?.contains("sensitive") == true) {

                    // Show re-authentication required message
                    AlertDialog.Builder(this)
                        .setTitle("Re-authentication Required")
                        .setMessage(
                            "For security reasons, you need to sign out and sign in again " +
                                    "before deleting your account.\n\n" +
                                    "Steps:\n" +
                                    "1. Click 'Sign Out' below\n" +
                                    "2. Sign in again\n" +
                                    "3. Try deleting account again"
                        )
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // Other error
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Handle back button click
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}