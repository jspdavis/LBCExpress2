package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val session = SessionManager(this)
        session.applyToApiClient()

        // Not logged in → always go to Login first
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Already logged in → route to the correct dashboard
        val dest = when (session.getUserType()) {
            "employee" -> when (session.getEmpRole()) {
                "Super Admin"  -> Intent(this, SuperAdminActivity::class.java)
                "Admin"        -> Intent(this, AdminActivity::class.java)
                "Branch Staff" -> Intent(this, BranchStaffActivity::class.java)
                "Rider"        -> Intent(this, RiderActivity::class.java)
                "Staff"        -> Intent(this, StaffActivity::class.java)
                else           -> Intent(this, LoginActivity::class.java)
            }
            "customer" -> Intent(this, DashboardActivity::class.java)
            else -> {
                // Unknown/corrupted session — clear it and go to login
                session.logout()
                Intent(this, LoginActivity::class.java)
            }
        }
        startActivity(dest)
        finish()
    }
}
