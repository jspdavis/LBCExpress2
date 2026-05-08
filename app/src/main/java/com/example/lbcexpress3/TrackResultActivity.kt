package com.example.lbcexpress3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.json.JSONArray

class TrackResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_result)

        findViewById<View>(R.id.tvBack).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.btnBackToDashboard).setOnClickListener { finish() }

        val trackingNo = intent.getStringExtra("tracking") ?: ""
        if (trackingNo.isEmpty()) {
            finish()
            return
        }

        loadTracking(trackingNo)
    }

    private fun loadTracking(trackingNo: String) {
        lifecycleScope.launch {
            val result = ApiClient.get("track.php", mapOf("tracking" to trackingNo))
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    val shipment = result.optJSONObject("shipment")
                    val history = result.optJSONArray("history") ?: JSONArray()
                    renderTracking(shipment, history)
                } else {
                    // Show error
                    findViewById<TextView>(R.id.tvTrackingNo).text = trackingNo
                    findViewById<TextView>(R.id.tvCurrentStatus).text = "Not Found"
                    findViewById<LinearLayout>(R.id.emptyHistory).visibility = View.VISIBLE
                }
            }
        }
    }

    private fun renderTracking(shipment: org.json.JSONObject?, history: JSONArray) {
        if (shipment == null) return

        findViewById<TextView>(R.id.tvTrackingNo).text = shipment.optString("trackingNumber")
        findViewById<TextView>(R.id.tvShipDate).text =
            "Shipped: ${shipment.optString("shipmentDate")}"

        val status = shipment.optString("status")
        val tvStatus = findViewById<TextView>(R.id.tvCurrentStatus)
        tvStatus.text = status

        val badgeColor = when (status) {
            "Delivered"              -> "#198754"
            "In Transit",
            "Arrived at Destination" -> "#0d6efd"
            "Out for Delivery"       -> "#e6a817"
            "Received at Branch"     -> "#0dcaf0"
            else                     -> "#6c757d"
        }
        tvStatus.background.setTint(Color.parseColor(badgeColor))

        // Timeline
        val container = findViewById<LinearLayout>(R.id.timelineContainer)
        val emptyHistory = findViewById<LinearLayout>(R.id.emptyHistory)

        if (history.length() == 0) {
            emptyHistory.visibility = View.VISIBLE
            return
        }
        emptyHistory.visibility = View.GONE

        // Reverse so newest is on top
        val reversed = (0 until history.length()).map { history.getJSONObject(it) }.reversed()

        reversed.forEachIndexed { index, event ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_timeline, container, false)

            val eventStatus = event.optString("status")
            val tvEventStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            tvEventStatus.text = eventStatus

            val eventBadgeColor = when (eventStatus) {
                "Delivered"              -> "#198754"
                "In Transit",
                "Arrived at Destination" -> "#0d6efd"
                "Out for Delivery"       -> "#e6a817"
                "Received at Branch"     -> "#0dcaf0"
                else                     -> "#6c757d"
            }
            tvEventStatus.background.setTint(Color.parseColor(eventBadgeColor))

            itemView.findViewById<TextView>(R.id.tvDate).text = event.optString("timestamp")

            val location = event.optString("location", "")
            val tvLocation = itemView.findViewById<TextView>(R.id.tvLocation)
            if (location.isNotEmpty()) {
                tvLocation.text = "📍 $location"
                tvLocation.visibility = View.VISIBLE
            } else {
                tvLocation.visibility = View.GONE
            }

            val remarks = event.optString("remarks", "")
            val tvRemarks = itemView.findViewById<TextView>(R.id.tvRemarks)
            if (remarks.isNotEmpty()) {
                tvRemarks.text = remarks
                tvRemarks.visibility = View.VISIBLE
            } else {
                tvRemarks.visibility = View.GONE
            }

            // Hide line for last item
            if (index == reversed.size - 1) {
                itemView.findViewById<View>(R.id.line).visibility = View.INVISIBLE
            }

            // First item (newest) gets active dot
            val dot = itemView.findViewById<View>(R.id.dot)
            if (index == 0) {
                dot.background.setTint(Color.parseColor(
                    if (eventStatus == "Delivered") "#198754" else "#CC0000"
                ))
            }

            container.addView(itemView)
        }
    }
}
