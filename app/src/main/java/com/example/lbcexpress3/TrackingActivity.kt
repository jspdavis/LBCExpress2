package com.example.lbcexpress3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class TrackingActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TRACKING_NUMBER = "extra_tracking_number"
    }

    // Sample tracking data for demo purposes
    private val sampleTrackingData = mapOf(
        "1234567890" to TrackingInfo(
            trackingNumber = "1234567890",
            status = "Out for Delivery",
            statusColor = "#E65100",
            location = "Quezon City Hub",
            origin = "Cebu City",
            destination = "Quezon City",
            lastUpdated = "May 7, 2026 9:15 AM",
            history = listOf(
                TimelineEvent("Out for Delivery", "Quezon City Hub", "May 7, 2026 9:15 AM"),
                TimelineEvent("Arrived at Destination Hub", "Quezon City Hub", "May 7, 2026 6:00 AM"),
                TimelineEvent("In Transit", "Manila Sorting Center", "May 6, 2026 11:30 PM"),
                TimelineEvent("Departed Origin Hub", "Cebu City Hub", "May 6, 2026 3:00 PM"),
                TimelineEvent("Package Accepted", "Cebu City Branch", "May 6, 2026 10:00 AM")
            )
        ),
        "9876543210" to TrackingInfo(
            trackingNumber = "9876543210",
            status = "Delivered",
            statusColor = "#2E7D32",
            location = "Makati City",
            origin = "Davao City",
            destination = "Makati City",
            lastUpdated = "May 5, 2026 2:30 PM",
            history = listOf(
                TimelineEvent("Delivered", "Makati City", "May 5, 2026 2:30 PM"),
                TimelineEvent("Out for Delivery", "Makati Hub", "May 5, 2026 8:00 AM"),
                TimelineEvent("Arrived at Destination Hub", "Makati Hub", "May 4, 2026 11:00 PM"),
                TimelineEvent("In Transit", "Manila Sorting Center", "May 4, 2026 6:00 PM"),
                TimelineEvent("Package Accepted", "Davao City Branch", "May 3, 2026 9:00 AM")
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val prefilledNumber = intent.getStringExtra(EXTRA_TRACKING_NUMBER)
        if (!prefilledNumber.isNullOrEmpty()) {
            val etTracking = findViewById<TextInputEditText>(R.id.etTrackingNumber)
            etTracking.setText(prefilledNumber)
            performTracking(prefilledNumber)
        }

        val btnTrack = findViewById<MaterialButton>(R.id.btnTrack)
        btnTrack.setOnClickListener {
            val etTracking = findViewById<TextInputEditText>(R.id.etTrackingNumber)
            val number = etTracking.text?.toString()?.trim() ?: ""
            if (number.isEmpty()) {
                Toast.makeText(this, getString(R.string.tracking_error), Toast.LENGTH_SHORT).show()
            } else {
                performTracking(number)
            }
        }
    }

    private fun performTracking(trackingNumber: String) {
        val cardResult = findViewById<MaterialCardView>(R.id.cardResult)
        val cardError = findViewById<MaterialCardView>(R.id.cardError)

        val info = sampleTrackingData[trackingNumber]

        if (info != null) {
            cardError.visibility = View.GONE
            cardResult.visibility = View.VISIBLE

            // Populate result fields
            findViewById<TextView>(R.id.tvTrackingNo).text = info.trackingNumber
            findViewById<TextView>(R.id.tvStatus).text = info.status
            findViewById<TextView>(R.id.tvStatusDate).text = info.lastUpdated
            findViewById<TextView>(R.id.tvLocation).text = info.location
            findViewById<TextView>(R.id.tvOrigin).text = info.origin
            findViewById<TextView>(R.id.tvDestination).text = info.destination

            // Build timeline
            val container = findViewById<LinearLayout>(R.id.timelineContainer)
            container.removeAllViews()

            info.history.forEachIndexed { index, event ->
                val itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_timeline, container, false)

                itemView.findViewById<TextView>(R.id.tvTimelineStatus).text = event.status
                itemView.findViewById<TextView>(R.id.tvTimelineLocation).text = event.location
                itemView.findViewById<TextView>(R.id.tvTimelineDate).text = event.date

                // Hide line for last item
                if (index == info.history.size - 1) {
                    itemView.findViewById<View>(R.id.line).visibility = View.INVISIBLE
                }

                container.addView(itemView)
            }
        } else {
            cardResult.visibility = View.GONE
            cardError.visibility = View.VISIBLE
            val tvError = findViewById<TextView>(R.id.tvErrorMessage)
            tvError.text = "No shipment found for tracking number: $trackingNumber\n\nPlease check the number and try again."
        }
    }

    data class TrackingInfo(
        val trackingNumber: String,
        val status: String,
        val statusColor: String,
        val location: String,
        val origin: String,
        val destination: String,
        val lastUpdated: String,
        val history: List<TimelineEvent>
    )

    data class TimelineEvent(
        val status: String,
        val location: String,
        val date: String
    )
}
