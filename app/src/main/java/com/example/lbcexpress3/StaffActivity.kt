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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.json.JSONArray

class StaffActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var currentTab = "pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        session = SessionManager(this)
        if (session.getEmpRole() != "Staff") {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        findViewById<TextView>(R.id.tvWelcome).text = session.getEmpName()
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { logout() }

        val tabPending   = findViewById<TextView>(R.id.tabPending)
        val tabProcessed = findViewById<TextView>(R.id.tabProcessed)

        tabPending.setOnClickListener {
            currentTab = "pending"
            tabPending.setTypeface(null, Typeface.BOLD)
            tabPending.setTextColor(getColor(R.color.lbc_red))
            tabProcessed.setTypeface(null, Typeface.NORMAL)
            tabProcessed.setTextColor(getColor(R.color.lbc_text_secondary))
            loadData()
        }
        tabProcessed.setOnClickListener {
            currentTab = "processed"
            tabProcessed.setTypeface(null, Typeface.BOLD)
            tabProcessed.setTextColor(getColor(R.color.lbc_red))
            tabPending.setTypeface(null, Typeface.NORMAL)
            tabPending.setTextColor(getColor(R.color.lbc_text_secondary))
            loadData()
        }

        loadData()
    }

    override fun onResume() { super.onResume(); loadData() }

    private fun loadData() {
        lifecycleScope.launch {
            val result = ApiClient.get("staff.php", mapOf("tab" to currentTab))
            runOnUiThread {
                if (!result.optBoolean("ok", false)) {
                    Toast.makeText(this@StaffActivity,
                        result.optString("error", "Failed to load"), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val branchName = result.optString("branchName", "—")
                findViewById<TextView>(R.id.tvBranchName).text = branchName

                if (currentTab == "pending") {
                    renderPending(result.optJSONArray("bookings") ?: JSONArray())
                } else {
                    renderProcessed(result.optJSONArray("shipments") ?: JSONArray())
                }
            }
        }
    }

    private fun processBooking(bookingId: Int) {
        lifecycleScope.launch {
            val result = ApiClient.post("process_booking.php",
                mapOf("booking_id" to bookingId.toString()))
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    val tracking = result.optString("trackingNumber", "")
                    Toast.makeText(
                        this@StaffActivity,
                        "✅ Shipment created! Tracking: $tracking",
                        Toast.LENGTH_LONG
                    ).show()
                    loadData() // refresh — processed booking disappears from pending list
                } else {
                    Toast.makeText(
                        this@StaffActivity,
                        result.optString("error", "Failed to process booking."),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun renderPending(items: JSONArray) {
        val container = findViewById<LinearLayout>(R.id.listContainer)
        val empty     = findViewById<LinearLayout>(R.id.emptyState)
        val tvTitle   = findViewById<TextView>(R.id.tvListTitle)
        val tvSub     = findViewById<TextView>(R.id.tvListSubtitle)
        val tvCount   = findViewById<TextView>(R.id.tvCount)

        container.removeAllViews()
        tvTitle.text = "Incoming Customer Bookings"
        tvSub.text   = "Process a booking to create a shipment and tracking number"
        tvCount.text = "${items.length()} Pending"
        tvCount.background.setTint(Color.parseColor("#CC0000"))

        if (items.length() == 0) {
            empty.visibility = View.VISIBLE
            empty.findViewById<TextView>(R.id.tvEmptyLabel).text = "No pending bookings to process."
            return
        }
        empty.visibility = View.GONE

        for (i in 0 until items.length()) {
            val b    = items.getJSONObject(i)
            val card = LayoutInflater.from(this).inflate(R.layout.item_booking_staff, container, false)

            card.findViewById<TextView>(R.id.tvBookingId).text =
                "#${b.optInt("Bkng_Id")}"
            card.findViewById<TextView>(R.id.tvCustomer).text =
                "${b.optString("Cust_FName")} ${b.optString("Cust_LName")}"

            val method = b.optString("Bkng_Delivery_Method", "")
            val methodBadge = card.findViewById<TextView>(R.id.tvDeliveryMethod)
            if (method == "Rider Delivery") {
                methodBadge.text = "🛵 Rider"
                methodBadge.background.setTint(Color.parseColor("#e6a817"))
            } else {
                methodBadge.text = "🏪 Branch"
                methodBadge.background.setTint(Color.parseColor("#0dcaf0"))
            }

            card.findViewById<TextView>(R.id.tvSenderInfo).text =
                "${b.optString("Bkng_Sender_Name", b.optString("Cust_FName", "—"))}\n" +
                b.optString("Bkng_Sender_Branch", "—")

            val receiverDest = if (method == "Rider Delivery") {
                listOf(
                    b.optString("Bkng_Receiver_Street", "").let { if (it == "null") "" else it },
                    b.optString("Bkng_Receiver_Del_City", "").let { if (it == "null") "" else it },
                    b.optString("Bkng_Receiver_Del_Province", "").let { if (it == "null") "" else it }
                ).filter { it.isNotEmpty() }.joinToString(", ")
            } else {
                b.optString("Bkng_Receiver_Branch", "").let { if (it == "null" || it.isEmpty()) "—" else it }
            }
            card.findViewById<TextView>(R.id.tvReceiverInfo).text =
                "${b.optString("Bkng_Receiver_Name", "—")}\n$receiverDest"

            card.findViewById<TextView>(R.id.tvPackageInfo).text =
                "${b.optString("Bkng_Package_Type", "—")}\n${b.optString("Bkng_Item_Name", "—")}"

            val isCod = b.optString("Bkng_Payment_Collection", "No") == "Yes"
            val tvCod = card.findViewById<TextView>(R.id.tvCod)
            tvCod.visibility = if (isCod) View.VISIBLE else View.GONE

            card.findViewById<TextView>(R.id.tvDate).text =
                b.optString("Bkng_Date", b.optString("Bkng_Booking_Date", "—"))

            // Process button — calls the API to create shipment and tracking number
            card.findViewById<MaterialButton>(R.id.btnProcess).setOnClickListener {
                val bookingId = b.optInt("Bkng_Id")
                val custName  = "${b.optString("Cust_FName")} ${b.optString("Cust_LName")}"
                AlertDialog.Builder(this)
                    .setTitle("Process Booking #$bookingId")
                    .setMessage("Create shipment for $custName?\nA tracking number will be generated.")
                    .setPositiveButton("Process") { _, _ -> processBooking(bookingId) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            container.addView(card)
        }
    }

    private fun renderProcessed(items: JSONArray) {
        val container = findViewById<LinearLayout>(R.id.listContainer)
        val empty     = findViewById<LinearLayout>(R.id.emptyState)
        val tvTitle   = findViewById<TextView>(R.id.tvListTitle)
        val tvSub     = findViewById<TextView>(R.id.tvListSubtitle)
        val tvCount   = findViewById<TextView>(R.id.tvCount)

        container.removeAllViews()
        tvTitle.text = "Recently Processed by You"
        tvSub.text   = "Read-only — shipment management is handled by Branch Staff"
        tvCount.text = "${items.length()} shipments"
        tvCount.background.setTint(Color.parseColor("#198754"))

        if (items.length() == 0) {
            empty.visibility = View.VISIBLE
            empty.findViewById<TextView>(R.id.tvEmptyLabel).text = "No shipments processed yet."
            return
        }
        empty.visibility = View.GONE

        for (i in 0 until items.length()) {
            val s    = items.getJSONObject(i)
            val card = LayoutInflater.from(this).inflate(R.layout.item_shipment_row, container, false)

            card.findViewById<TextView>(R.id.tvTracking).text =
                s.optString("Ship_Tracking_Number")
            card.findViewById<TextView>(R.id.tvCustomer).text =
                "${s.optString("Cust_FName")} ${s.optString("Cust_LName")}"

            val status = s.optString("Ship_Status", "—")
            val tvStatus = card.findViewById<TextView>(R.id.tvStatus)
            tvStatus.text = status
            tvStatus.background.setTint(Color.parseColor(statusColor(status)))

            val method = s.optString("Bkng_Delivery_Method", "")
            val dest = if (method == "Rider Delivery") {
                val c = s.optString("Bkng_Receiver_Del_City","").let { if (it == "null") "" else it }
                val p = s.optString("Bkng_Receiver_Del_Province","").let { if (it == "null") "" else it }
                "🛵 " + listOf(c, p).filter { it.isNotEmpty() }.joinToString(", ")
            } else {
                val branch = s.optString("Bkng_Receiver_Branch", "").let { if (it == "null") "—" else it.ifEmpty { "—" } }
                "🏪 $branch"
            }
            card.findViewById<TextView>(R.id.tvDestination).text = dest
            card.findViewById<TextView>(R.id.tvExtra).text =
                "📍 ${s.optString("Origin_Branch", "—")} · ${s.optString("Ship_Shipment_Date", "")}"

            card.findViewById<MaterialButton>(R.id.btnAction).visibility = View.GONE

            container.addView(card)
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
