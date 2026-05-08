package com.example.lbcexpress3

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONArray

class RiderActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var currentTab = "active"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rider)

        session = SessionManager(this)
        if (session.getEmpRole() != "Rider") {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        findViewById<TextView>(R.id.tvWelcome).text = "🛵 ${session.getEmpName()}"
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { logout() }

        val tabActive = findViewById<TextView>(R.id.tabActive)
        val tabDone   = findViewById<TextView>(R.id.tabDone)

        tabActive.setOnClickListener {
            currentTab = "active"
            tabActive.setTypeface(null, Typeface.BOLD); tabActive.setTextColor(getColor(R.color.lbc_red))
            tabDone.setTypeface(null, Typeface.NORMAL); tabDone.setTextColor(getColor(R.color.lbc_text_secondary))
            loadData()
        }
        tabDone.setOnClickListener {
            currentTab = "done"
            tabDone.setTypeface(null, Typeface.BOLD); tabDone.setTextColor(getColor(R.color.lbc_red))
            tabActive.setTypeface(null, Typeface.NORMAL); tabActive.setTextColor(getColor(R.color.lbc_text_secondary))
            loadData()
        }

        loadData()
    }

    override fun onResume() { super.onResume(); loadData() }

    private fun loadData() {
        lifecycleScope.launch {
            val result = ApiClient.get("rider.php", mapOf("tab" to currentTab))
            runOnUiThread {
                if (!result.optBoolean("ok", false)) {
                    Toast.makeText(this@RiderActivity,
                        result.optString("error", "Failed to load"), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val branchName = result.optString("branchName", "—")
                findViewById<TextView>(R.id.tvBranchName).text = branchName

                val deliveries = result.optJSONArray("deliveries") ?: JSONArray()
                renderDeliveries(deliveries)
            }
        }
    }

    private fun renderDeliveries(items: JSONArray) {
        val container = findViewById<LinearLayout>(R.id.listContainer)
        val empty     = findViewById<LinearLayout>(R.id.emptyState)
        val tvTitle   = findViewById<TextView>(R.id.tvListTitle)
        val tvCount   = findViewById<TextView>(R.id.tvCount)

        container.removeAllViews()
        tvTitle.text = if (currentTab == "active") "Active Deliveries" else "Completed Deliveries"
        tvCount.text = "${items.length()} parcel(s)"

        if (items.length() == 0) { empty.visibility = View.VISIBLE; return }
        empty.visibility = View.GONE

        for (i in 0 until items.length()) {
            val s = items.getJSONObject(i)
            val card = LayoutInflater.from(this).inflate(R.layout.item_shipment_row, container, false)

            card.findViewById<TextView>(R.id.tvTracking).text =
                s.optString("Ship_Tracking_Number")
            card.findViewById<TextView>(R.id.tvCustomer).text =
                "${s.optString("Cust_FName")} ${s.optString("Cust_LName")}"

            val status = s.optString("Ship_Status", "—")
            val tvStatus = card.findViewById<TextView>(R.id.tvStatus)
            tvStatus.text = status
            tvStatus.background.setTint(Color.parseColor(statusColor(status)))

            // Delivery address
            val method = s.optString("Bkng_Delivery_Method", "")
            val dest = if (method == "Rider Delivery") {
                val parts = listOf(
                    s.optString("Bkng_Receiver_Street", "").let { if (it == "null") "" else it },
                    s.optString("Bkng_Receiver_Del_City", "").let { if (it == "null") "" else it },
                    s.optString("Bkng_Receiver_Del_Province", "").let { if (it == "null") "" else it }
                ).filter { it.isNotEmpty() }
                "🛵 ${parts.joinToString(", ")}"
            } else {
                "🏪 ${s.optString("Bkng_Receiver_Branch", "").let { if (it == "null" || it.isEmpty()) "—" else it }}"
            }
            card.findViewById<TextView>(R.id.tvDestination).text =
                "${s.optString("Bkng_Receiver_Name", "—")}\n$dest"

            val currentBranch = s.optString("Current_Branch", "")
            card.findViewById<TextView>(R.id.tvExtra).text =
                if (currentBranch.isNotEmpty()) "📍 $currentBranch" else ""

            // Action button — only for active deliveries
            val btnAction = card.findViewById<MaterialButton>(R.id.btnAction)
            if (currentTab == "active") {
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Mark Delivered"
                btnAction.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#198754"))
                val shipId = s.optInt("Ship_Id")
                val tracking = s.optString("Ship_Tracking_Number")
                btnAction.setOnClickListener {
                    showDeliverPanel(shipId, tracking)
                }
            } else {
                btnAction.visibility = View.GONE
            }

            container.addView(card)
        }
    }

    private fun showDeliverPanel(shipId: Int, tracking: String) {
        // Use a dialog instead of the bottom panel — always visible regardless of scroll
        val dialogView = layoutInflater.inflate(R.layout.dialog_mark_delivered, null)
        dialogView.findViewById<TextView>(R.id.tvDeliverTracking).text = "Tracking: $tracking"

        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.etDeliverLocation)
        val etRemarks  = dialogView.findViewById<TextInputEditText>(R.id.etDeliverRemarks)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Delivery")
            .setView(dialogView)
            .setPositiveButton("Confirm Delivered") { _, _ ->
                val location = etLocation.text?.toString()?.trim() ?: ""
                if (location.isEmpty()) {
                    Toast.makeText(this, "Please enter delivery location.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val remarks = etRemarks.text?.toString()?.trim() ?: ""
                doDeliver(shipId, location, remarks)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doDeliver(shipId: Int, location: String, remarks: String) {
        lifecycleScope.launch {
            val result = ApiClient.post("rider_deliver.php", mapOf(
                "ship_id"  to shipId.toString(),
                "location" to location,
                "remarks"  to remarks
            ))
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    Toast.makeText(this@RiderActivity, "✅ Marked as Delivered!", Toast.LENGTH_SHORT).show()
                    loadData()
                } else {
                    Toast.makeText(this@RiderActivity,
                        result.optString("error", "Failed"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun statusColor(status: String) = when (status) {
        "Delivered"              -> "#198754"
        "In Transit",
        "Arrived at Destination" -> "#0d6efd"
        "Out for Delivery"       -> "#e6a817"
        "Received at Branch"     -> "#0dcaf0"
        else                     -> "#6c757d"
    }

    private fun logout() {
        session.logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
