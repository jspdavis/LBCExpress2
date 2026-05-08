package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tvError         = findViewById<TextView>(R.id.tvError)
        val toggleRole      = findViewById<MaterialButtonToggleGroup>(R.id.toggleRole)
        val tvEmailLabel    = findViewById<TextView>(R.id.tvEmailLabel)
        val etIdentifier    = findViewById<TextInputEditText>(R.id.etIdentifier)
        val signupContainer = findViewById<LinearLayout>(R.id.signupContainer)
        val btnSignIn       = findViewById<MaterialButton>(R.id.btnSignIn)
        val btnCreate       = findViewById<MaterialButton>(R.id.btnCreateAccount)

        // Show success message passed from signup
        intent.getStringExtra("msg")?.let {
            tvError.setBackgroundResource(R.drawable.bg_success)
            tvError.setTextColor(0xFF155724.toInt())
            tvError.text = it
            tvError.visibility = View.VISIBLE
        }

        // Default: Customer selected
        toggleRole.check(R.id.btnCustomer)

        toggleRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnEmployee -> {
                    tvEmailLabel.text = "Employee Email"
                    etIdentifier.hint = "example@lbc.com"
                    signupContainer.visibility = View.GONE
                }
                R.id.btnCustomer -> {
                    tvEmailLabel.text = "Customer Email"
                    etIdentifier.hint = "Enter your registered email"
                    signupContainer.visibility = View.VISIBLE
                }
            }
        }

        btnSignIn.setOnClickListener {
            val identifier = etIdentifier.text?.toString()?.trim() ?: ""
            val password   = findViewById<TextInputEditText>(R.id.etPassword).text?.toString() ?: ""

            // Read role directly from toggle at click time
            val role = if (toggleRole.checkedButtonId == R.id.btnEmployee) "employee" else "customer"

            if (identifier.isEmpty() || password.isEmpty()) {
                showError(tvError, "Please fill in all fields.")
                return@setOnClickListener
            }

            btnSignIn.isEnabled = false
            btnSignIn.text = "Signing in..."

            lifecycleScope.launch {
                val result = ApiClient.post("login.php", mapOf(
                    "identifier" to identifier,
                    "password"   to password,
                    "role"       to role
                ))

                runOnUiThread {
                    btnSignIn.isEnabled = true
                    btnSignIn.text = "Sign In"

                    // Log full response for debugging
                    Log.d("LOGIN_DEBUG", "sent role=$role | response=$result")

                    if (!result.optBoolean("ok", false)) {
                        showError(tvError, result.optString("error", "Login failed."))
                        return@runOnUiThread
                    }

                    val userType = result.optString("userType", "").trim()
                    Log.d("LOGIN_DEBUG", "userType='$userType'")

                    val session = SessionManager(this@LoginActivity)

                    when (userType) {
                        "customer" -> {
                            val c = result.optJSONObject("customer")
                            session.saveCustomer(
                                c?.optInt("id") ?: 0,
                                c?.optString("name") ?: "",
                                c?.optString("email") ?: ""
                            )
                            startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                            finish()
                        }
                        "employee" -> {
                            val e    = result.optJSONObject("emp")
                            val emp_role = e?.optString("role")?.trim() ?: ""
                            session.saveEmployee(
                                e?.optInt("id") ?: 0,
                                e?.optString("name") ?: "",
                                emp_role,
                                e?.optInt("branchId") ?: 0
                            )
                            val dest = when (emp_role) {
                                "Super Admin"  -> Intent(this@LoginActivity, SuperAdminActivity::class.java)
                                "Admin"        -> Intent(this@LoginActivity, AdminActivity::class.java)
                                "Branch Staff" -> Intent(this@LoginActivity, BranchStaffActivity::class.java)
                                "Rider"        -> Intent(this@LoginActivity, RiderActivity::class.java)
                                "Staff"        -> Intent(this@LoginActivity, StaffActivity::class.java)
                                else -> {
                                    showError(tvError, "Unknown employee role: '$emp_role'")
                                    return@runOnUiThread
                                }
                            }
                            startActivity(dest)
                            finish()
                        }
                        else -> {
                            showError(tvError, "Unexpected response. Please try again.")
                            Log.e("LOGIN_DEBUG", "Unhandled userType: '$userType' | full: $result")
                        }
                    }
                }
            }
        }

        btnCreate.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun showError(tv: TextView, msg: String) {
        tv.setBackgroundResource(R.drawable.bg_error)
        tv.setTextColor(0xFF721c24.toInt())
        tv.text = msg
        tv.visibility = View.VISIBLE
    }
}
