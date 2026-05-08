package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val tvError = findViewById<TextView>(R.id.tvSignupError)
        val etFullName = findViewById<TextInputEditText>(R.id.etFullName)
        val etEmail = findViewById<TextInputEditText>(R.id.etSignupEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etSignupPassword)
        val btnCreate = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        btnCreate.setOnClickListener {
            val name = etFullName.text?.toString()?.trim() ?: ""
            val email = etEmail.text?.toString()?.trim() ?: ""
            val password = etPassword.text?.toString() ?: ""

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showError(tvError, "Please fill in all fields.")
                return@setOnClickListener
            }

            btnCreate.isEnabled = false
            btnCreate.text = "Creating account..."

            lifecycleScope.launch {
                val result = ApiClient.post("signup.php", mapOf(
                    "fname" to name,
                    "email" to email,
                    "password" to password
                ))

                runOnUiThread {
                    btnCreate.isEnabled = true
                    btnCreate.text = "Create Account"

                    if (result.optBoolean("ok", false)) {
                        val intent = Intent(this@SignupActivity, LoginActivity::class.java)
                        intent.putExtra("msg", "Account created! Please login.")
                        startActivity(intent)
                        finish()
                    } else {
                        showError(tvError, result.optString("error", "Signup failed."))
                    }
                }
            }
        }

        tvLoginLink.setOnClickListener { finish() }
    }

    private fun showError(tv: TextView, msg: String) {
        tv.text = msg
        tv.visibility = View.VISIBLE
    }
}
