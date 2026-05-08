package com.example.lbcexpress3

import android.content.Intent
import android.graphics.Color
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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DashboardActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Welcome text
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        tvWelcome.text = "Welcome, ${session.getCustName()}"

        // Logout
        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            session.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Track button
        val etTracking = findViewById<TextInputEditText>(R.id.etTrackingNumber)
        findViewById<MaterialButton>(R.id.btnTrack).setOnClickListener {
            val num = etTracking.text?.toString()?.trim() ?: ""
            if (num.isEmpty()) {
                Toast.makeText(this, "Enter a tracking number", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, TrackResultActivity::class.java)
                intent.putExtra("tracking", num)
                startActivity(intent)
            }
        }

        // Book Now — clear any stale booking data before starting fresh
        findViewById<MaterialButton>(R.id.btnBookNow).setOnClickListener {
            BookingData.clear()
            startActivity(Intent(this, BookSenderActivity::class.java))
        }

        loadBookings()
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }

    private fun loadBookings() {
        lifecycleScope.launch {
            val result = ApiClient.get("bookings.php")
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    val bookings = result.optJSONArray("bookings") ?: JSONArray()
                    renderBookings(bookings)
                }
                // If API not available yet, show empty state gracefully
            }
        }
    }

    private fun renderBookings(bookings: JSONArray) {
        val container = findViewById<LinearLayout>(R.id.bookingListContainer)
        val emptyState = findViewById<LinearLayout>(R.id.emptyState)
        val tvCount = findViewById<TextView>(R.id.tvBookingCount)

        container.removeAllViews()
        tvCount.text = "${bookings.length()} booking(s)"

        if (bookings.length() == 0) {
            emptyState.visibility = View.VISIBLE
            return
        }
        emptyState.visibility = View.GONE

        for (i in 0 until bookings.length()) {
            val b = bookings.getJSONObject(i)
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_booking, container, false)
            bindBookingItem(itemView, b)
            container.addView(itemView)
        }
    }

    private fun bindBookingItem(view: View, b: JSONObject) {
        val shipStatus = b.optString("Ship_Status", "").let { if (it == "null") "" else it }
        val bkngStatus = b.optString("Bkng_Booking_Status", "Pending")
        val displayStatus = if (shipStatus.isNotEmpty()) shipStatus else bkngStatus

        // Status stripe color
        val stripeColor = when (displayStatus) {
            "Delivered"              -> "#198754"
            "In Transit",
            "Arrived at Destination" -> "#0d6efd"
            "Out for Delivery",
            "Pending"                -> "#ffc107"
            "Received at Branch"     -> "#0dcaf0"
            else                     -> "#adb5bd"
        }
        view.findViewById<View>(R.id.statusStripe).setBackgroundColor(Color.parseColor(stripeColor))

        // Badge color
        val badgeColor = when (displayStatus) {
            "Delivered"              -> "#198754"
            "In Transit",
            "Arrived at Destination" -> "#0d6efd"
            "Out for Delivery",
            "Pending"                -> "#e6a817"
            "Received at Branch"     -> "#0dcaf0"
            else                     -> "#6c757d"
        }
        val tvBadge = view.findViewById<TextView>(R.id.tvStatusBadge)
        tvBadge.text = displayStatus
        tvBadge.background.setTint(Color.parseColor(badgeColor))

        view.findViewById<TextView>(R.id.tvBookingId).text = "#${b.optInt("Bkng_Id")}"

        // COD badge
        val isCod = b.optString("Bkng_Payment_Collection") == "Yes"
        view.findViewById<TextView>(R.id.tvCodBadge).visibility =
            if (isCod) View.VISIBLE else View.GONE

        // Tracking number — SQL NULL comes back as the string "null" via JSONObject.optString()
        val trackingNo = b.optString("Ship_Tracking_Number", "")
            .let { if (it == "null") "" else it }
        val tvTracking = view.findViewById<TextView>(R.id.tvTrackingNumber)
        if (trackingNo.isNotEmpty()) {
            tvTracking.text = trackingNo
            tvTracking.setTextColor(Color.parseColor("#CC0000"))
        } else {
            tvTracking.text = "Pending"
            tvTracking.setTextColor(Color.parseColor("#e6a817"))
        }

        view.findViewById<TextView>(R.id.tvDate).text =
            b.optString("Bkng_Booking_Date", b.optString("Bkng_Date", ""))

        view.findViewById<TextView>(R.id.tvPackageType).text =
            b.optString("Bkng_Package_Type", "—")
        view.findViewById<TextView>(R.id.tvItemName).text =
            b.optString("Bkng_Item_Name", "—")

        // Route — FROM
        // optString() returns "null" (string) when the DB value is SQL NULL — filter that out
        val origin = b.safeStr("Origin_Branch")
            .ifEmpty { b.safeStr("Bkng_Sender_Branch") }
            .ifEmpty {
                listOf(b.safeStr("Bkng_Sender_City"), b.safeStr("Bkng_Sender_Province"))
                    .filter { it.isNotEmpty() }.joinToString(", ")
            }
            .ifEmpty { "—" }

        view.findViewById<TextView>(R.id.tvOrigin).text = "📍 $origin"
        view.findViewById<TextView>(R.id.tvSenderName).text =
            b.safeStr("Bkng_Sender_Name")

        val deliveryMethod = b.optString("Bkng_Delivery_Method", "Branch Pick Up")
        val dest = if (deliveryMethod == "Rider Delivery") {
            val street = b.optString("Bkng_Receiver_Street", "").let { if (it == "null") "" else it }
            val city   = b.optString("Bkng_Receiver_Del_City", "").let { if (it == "null") "" else it }
            val prov   = b.optString("Bkng_Receiver_Del_Province", "").let { if (it == "null") "" else it }
            "🛵 ${listOf(street, city, prov).filter { it.isNotEmpty() }.joinToString(", ")}"
        } else {
            "🏪 ${b.optString("Bkng_Receiver_Branch", "").let { if (it == "null" || it.isEmpty()) "—" else it }}"
        }
        view.findViewById<TextView>(R.id.tvDestination).text = dest
        view.findViewById<TextView>(R.id.tvReceiver).text =
            b.optString("Bkng_Receiver_Name", "")

        // Track button
        val btnTrack = view.findViewById<MaterialButton>(R.id.btnTrackBooking)
        if (trackingNo.isNotEmpty()) {
            btnTrack.visibility = View.VISIBLE
            btnTrack.setOnClickListener {
                val intent = Intent(this, TrackResultActivity::class.java)
                intent.putExtra("tracking", trackingNo)
                startActivity(intent)
            }
        } else {
            btnTrack.visibility = View.GONE
        }

        // Delete button
        view.findViewById<MaterialButton>(R.id.btnDeleteBooking).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Delete this booking?")
                .setPositiveButton("Delete") { _, _ ->
                    deleteBooking(b.optInt("Bkng_Id"))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteBooking(id: Int) {
        lifecycleScope.launch {
            val result = ApiClient.post("delete_booking.php", mapOf("id" to id.toString()))
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    Toast.makeText(this@DashboardActivity, "Booking deleted", Toast.LENGTH_SHORT).show()
                    loadBookings()
                } else {
                    Toast.makeText(this@DashboardActivity,
                        result.optString("error", "Delete failed"), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

// File-level extension — safe string that treats JSON null as empty
private fun org.json.JSONObject.safeStr(key: String): String {
    val v = optString(key, "")
    return if (v == "null" || v.isBlank()) "" else v
}
