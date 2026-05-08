package com.example.lbcexpress3

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONArray

class SuperAdminActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    // branch list loaded from API: list of Pair(branchId, displayName)
    private val branchList = mutableListOf<Pair<Int, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_super_admin)

        session = SessionManager(this)
        if (session.getEmpRole() != "Super Admin") {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        findViewById<TextView>(R.id.tvWelcome).text = session.getEmpName()
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { logout() }

        loadBranchesForSpinner()

        findViewById<MaterialButton>(R.id.btnCreateAdmin).setOnClickListener {
            createBranchAdmin()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAdmins()
    }

    private fun loadBranchesForSpinner() {
        lifecycleScope.launch {
            val result = ApiClient.get("branches.php")
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    val branches = result.optJSONArray("branches") ?: JSONArray()
                    branchList.clear()
                    val labels = mutableListOf("Select Branch")
                    for (i in 0 until branches.length()) {
                        val b = branches.getJSONObject(i)
                        branchList.add(Pair(b.optInt("id"), "${b.optString("city")} — ${b.optString("name")}"))
                        labels.add("${b.optString("city")} — ${b.optString("name")}")
                    }
                    val spinner = findViewById<Spinner>(R.id.spinnerBranch)
                    val adapter = ArrayAdapter(this@SuperAdminActivity,
                        android.R.layout.simple_spinner_item, labels)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                }
            }
        }
    }

    private fun createBranchAdmin() {
        val name     = findViewById<TextInputEditText>(R.id.etName).text?.toString()?.trim() ?: ""
        val email    = findViewById<TextInputEditText>(R.id.etEmail).text?.toString()?.trim() ?: ""
        val password = findViewById<TextInputEditText>(R.id.etPassword).text?.toString() ?: ""
        val spinner  = findViewById<Spinner>(R.id.spinnerBranch)
        val pos      = spinner.selectedItemPosition

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields.", isError = true); return
        }
        if (pos == 0) {
            showMessage("Please select a branch.", isError = true); return
        }

        val branchId = branchList[pos - 1].first

        val btn = findViewById<MaterialButton>(R.id.btnCreateAdmin)
        btn.isEnabled = false; btn.text = "Creating..."

        lifecycleScope.launch {
            val result = ApiClient.post("super_admin_create.php", mapOf(
                "name"      to name,
                "email"     to email,
                "password"  to password,
                "branch_id" to branchId.toString()
            ))
            runOnUiThread {
                btn.isEnabled = true; btn.text = "CREATE BRANCH ADMIN"
                if (result.optBoolean("ok", false)) {
                    // Clear form
                    findViewById<TextInputEditText>(R.id.etName).text = null
                    findViewById<TextInputEditText>(R.id.etEmail).text = null
                    findViewById<TextInputEditText>(R.id.etPassword).text = null
                    spinner.setSelection(0)
                    showMessage("Branch Admin created successfully.", isError = false)
                    loadAdmins()
                } else {
                    showMessage(result.optString("error", "Failed to create admin."), isError = true)
                }
            }
        }
    }

    private fun loadAdmins() {
        lifecycleScope.launch {
            val result = ApiClient.get("super_admin.php", mapOf("tab" to "admins"))
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    renderAdmins(result.optJSONArray("admins") ?: JSONArray())
                }
            }
        }
    }

    private fun renderAdmins(admins: JSONArray) {
        val container = findViewById<LinearLayout>(R.id.adminListContainer)
        val empty     = findViewById<LinearLayout>(R.id.emptyState)
        val tvCount   = findViewById<TextView>(R.id.tvAdminCount)

        container.removeAllViews()
        tvCount.text = "${admins.length()} Admins"

        if (admins.length() == 0) { empty.visibility = View.VISIBLE; return }
        empty.visibility = View.GONE

        for (i in 0 until admins.length()) {
            val a   = admins.getJSONObject(i)
            val row = LayoutInflater.from(this).inflate(R.layout.item_staff_row, container, false)

            row.findViewById<TextView>(R.id.tvName).text = a.optString("Emp_FName")
            row.findViewById<TextView>(R.id.tvSub).text =
                "${a.optString("Brch_City")} — ${a.optString("Brch_Name")}"
            val tvRole = row.findViewById<TextView>(R.id.tvRole)
            tvRole.text = "Admin"
            tvRole.background.setTint(Color.parseColor("#7b1fa2"))

            val empId = a.optInt("Emp_Id")
            row.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Admin")
                    .setMessage("Delete ${a.optString("Emp_FName")}?")
                    .setPositiveButton("Delete") { _, _ -> deleteStaff(empId) }
                    .setNegativeButton("Cancel", null).show()
            }
            container.addView(row)
        }
    }

    private fun deleteStaff(id: Int) {
        lifecycleScope.launch {
            val result = ApiClient.post("delete_staff.php", mapOf("id" to id.toString()))
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    showMessage("Employee deleted.", isError = false)
                    loadAdmins()
                } else {
                    showMessage(result.optString("error", "Delete failed."), isError = true)
                }
            }
        }
    }

    private fun showMessage(msg: String, isError: Boolean) {
        val tv = findViewById<TextView>(R.id.tvMessage)
        tv.text = msg
        tv.setBackgroundResource(if (isError) R.drawable.bg_error else R.drawable.bg_success)
        tv.setTextColor(if (isError) Color.parseColor("#721c24") else Color.parseColor("#155724"))
        tv.visibility = View.VISIBLE
    }

    private fun logout() {
        session.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
