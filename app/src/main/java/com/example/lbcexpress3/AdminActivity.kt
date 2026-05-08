package com.example.lbcexpress3

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
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

class AdminActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var currentTab = "staff"

    // Allowed roles for Admin to create
    private val roles = listOf("Staff", "Branch Staff", "Rider")
    private val vehicleTypes = listOf("Motorcycle", "Bicycle", "Van", "Truck")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        session = SessionManager(this)
        if (session.getEmpRole() != "Admin") {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        findViewById<TextView>(R.id.tvWelcome).text = session.getEmpName()
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { logout() }

        // Role spinner
        val spinnerRole = findViewById<Spinner>(R.id.spinnerRole)
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = roleAdapter

        // Vehicle spinner
        val spinnerVehicle = findViewById<Spinner>(R.id.spinnerVehicle)
        val vehicleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, vehicleTypes)
        vehicleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVehicle.adapter = vehicleAdapter

        // Show/hide rider fields based on role selection
        val riderFields = findViewById<LinearLayout>(R.id.riderFields)
        spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                riderFields.visibility = if (roles[pos] == "Rider") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        // Tab switching
        val tabStaff  = findViewById<TextView>(R.id.tabStaff)
        val tabRiders = findViewById<TextView>(R.id.tabRiders)

        tabStaff.setOnClickListener {
            currentTab = "staff"
            tabStaff.setTypeface(null, Typeface.BOLD)
            tabStaff.setTextColor(getColor(R.color.lbc_red))
            tabRiders.setTypeface(null, Typeface.NORMAL)
            tabRiders.setTextColor(getColor(R.color.lbc_text_secondary))
            loadDirectory()
        }
        tabRiders.setOnClickListener {
            currentTab = "riders"
            tabRiders.setTypeface(null, Typeface.BOLD)
            tabRiders.setTextColor(getColor(R.color.lbc_red))
            tabStaff.setTypeface(null, Typeface.NORMAL)
            tabStaff.setTextColor(getColor(R.color.lbc_text_secondary))
            loadDirectory()
        }

        // Create staff button
        findViewById<MaterialButton>(R.id.btnCreateStaff).setOnClickListener {
            createStaff()
        }

        loadBranchNote()
        loadDirectory()
    }

    override fun onResume() {
        super.onResume()
        loadDirectory()
    }

    private fun loadBranchNote() {
        lifecycleScope.launch {
            val result = ApiClient.get("admin.php", mapOf("tab" to "staff"))
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    val branch = result.optJSONObject("branch")
                    val name = "${branch?.optString("Brch_City") ?: ""} — ${branch?.optString("Brch_Name") ?: ""}"
                    findViewById<TextView>(R.id.tvBranchName).text = name
                    findViewById<TextView>(R.id.tvFormBranchNote).text =
                        "New staff will be assigned to: $name"
                }
            }
        }
    }

    private fun createStaff() {
        val name     = findViewById<TextInputEditText>(R.id.etName).text?.toString()?.trim() ?: ""
        val email    = findViewById<TextInputEditText>(R.id.etEmail).text?.toString()?.trim() ?: ""
        val password = findViewById<TextInputEditText>(R.id.etPassword).text?.toString() ?: ""
        val role     = roles[findViewById<Spinner>(R.id.spinnerRole).selectedItemPosition]

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showMessage("Please fill in all fields.", isError = true); return
        }

        val params = mutableMapOf(
            "name"     to name,
            "email"    to email,
            "password" to password,
            "role"     to role
        )

        // Add rider fields if applicable
        if (role == "Rider") {
            params["license_no"]   = findViewById<TextInputEditText>(R.id.etLicenseNo).text?.toString()?.trim() ?: ""
            params["vehicle_type"] = vehicleTypes[findViewById<Spinner>(R.id.spinnerVehicle).selectedItemPosition]
            params["plate_no"]     = findViewById<TextInputEditText>(R.id.etPlateNo).text?.toString()?.trim() ?: ""
        }

        val btn = findViewById<MaterialButton>(R.id.btnCreateStaff)
        btn.isEnabled = false; btn.text = "Creating..."

        lifecycleScope.launch {
            val result = ApiClient.post("admin_create_staff.php", params)
            runOnUiThread {
                btn.isEnabled = true; btn.text = "CREATE ACCOUNT"
                if (result.optBoolean("ok", false)) {
                    // Clear form
                    findViewById<TextInputEditText>(R.id.etName).text = null
                    findViewById<TextInputEditText>(R.id.etEmail).text = null
                    findViewById<TextInputEditText>(R.id.etPassword).text = null
                    findViewById<TextInputEditText>(R.id.etLicenseNo).text = null
                    findViewById<TextInputEditText>(R.id.etPlateNo).text = null
                    findViewById<Spinner>(R.id.spinnerRole).setSelection(0)
                    showMessage("Account created successfully.", isError = false)
                    loadDirectory()
                } else {
                    showMessage(result.optString("error", "Failed to create account."), isError = true)
                }
            }
        }
    }

    private fun loadDirectory() {
        lifecycleScope.launch {
            val result = ApiClient.get("admin.php", mapOf("tab" to currentTab))
            runOnUiThread {
                if (!result.optBoolean("ok", false)) return@runOnUiThread
                val branch = result.optJSONObject("branch")
                val branchDisplay = "${branch?.optString("Brch_City") ?: ""} — ${branch?.optString("Brch_Name") ?: ""}"
                findViewById<TextView>(R.id.tvBranchName).text = branchDisplay

                if (currentTab == "staff") {
                    renderList(result.optJSONArray("staff") ?: JSONArray(), "staff")
                } else {
                    renderList(result.optJSONArray("riders") ?: JSONArray(), "riders")
                }
            }
        }
    }

    private fun renderList(items: JSONArray, type: String) {
        val container = findViewById<LinearLayout>(R.id.listContainer)
        val empty     = findViewById<LinearLayout>(R.id.emptyState)
        val tvTitle   = findViewById<TextView>(R.id.tvListTitle)
        val tvCount   = findViewById<TextView>(R.id.tvCount)

        container.removeAllViews()
        tvTitle.text = if (type == "staff") "Staff Directory" else "Rider Directory"
        tvCount.text = "${items.length()} ${if (type == "staff") "Active" else "Riders"}"

        if (items.length() == 0) { empty.visibility = View.VISIBLE; return }
        empty.visibility = View.GONE

        val myId = session.getEmpId()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val row  = LayoutInflater.from(this).inflate(R.layout.item_staff_row, container, false)
            val empId = item.optInt("Emp_Id")

            row.findViewById<TextView>(R.id.tvName).text =
                item.optString("Emp_FName") + if (empId == myId) " (You)" else ""

            val tvRole = row.findViewById<TextView>(R.id.tvRole)
            if (type == "staff") {
                tvRole.text = item.optString("Emp_Role")
                row.findViewById<TextView>(R.id.tvSub).visibility = View.GONE
            } else {
                val status = item.optString("Rider_Status", "—")
                tvRole.text = status
                tvRole.background.setTint(Color.parseColor(
                    when (status) {
                        "Available"   -> "#198754"
                        "On Delivery" -> "#e6a817"
                        else          -> "#6c757d"
                    }
                ))
                row.findViewById<TextView>(R.id.tvSub).text =
                    "${item.optString("Rider_Vehicle_Type", "—")} · ${item.optString("Rider_Plate_No", "—")}"
            }

            val btnDel = row.findViewById<MaterialButton>(R.id.btnDelete)
            if (empId == myId) {
                btnDel.visibility = View.GONE
            } else {
                btnDel.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Delete")
                        .setMessage("Delete ${item.optString("Emp_FName")}?")
                        .setPositiveButton("Delete") { _, _ -> deleteStaff(empId) }
                        .setNegativeButton("Cancel", null).show()
                }
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
                    loadDirectory()
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
