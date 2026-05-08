package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class BookConfirmationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_confirmation)

        findViewById<View>(R.id.tvBack).setOnClickListener { finish() }

        val d = BookingData

        // Sender
        findViewById<TextView>(R.id.tvSenderName).text = "${d.firstName} ${d.lastName}"
        findViewById<TextView>(R.id.tvSenderPhone).text = d.phone
        findViewById<TextView>(R.id.tvSenderLocation).text = "${d.province}, ${d.city}"
        findViewById<TextView>(R.id.tvSenderBranch).text = "🏪 ${d.dropOffBranch}"
        findViewById<TextView>(R.id.tvDropoffDeadline).text = getDropoffDeadline()

        // Receiver
        findViewById<TextView>(R.id.tvReceiverName).text = "${d.rFname} ${d.rLname}"
        findViewById<TextView>(R.id.tvReceiverPhone).text = d.rPhone

        if (d.deliveryMethod == "Rider Delivery") {
            val addr = listOf(d.rStreet, d.rDelCity, d.rDelProvince)
                .filter { it.isNotEmpty() }.joinToString(", ")
            findViewById<TextView>(R.id.tvReceiverDestination).text = "🛵 $addr"
            findViewById<TextView>(R.id.tvDeliveryNote).text =
                "Estimated delivery: 1-3 days after processing."
        } else {
            findViewById<TextView>(R.id.tvReceiverDestination).text =
                "🏪 ${d.rBranch}\n${d.rCity}, ${d.rProvince}"
            findViewById<TextView>(R.id.tvDeliveryNote).text =
                "Estimated delivery is 3-6 days from actual pick up."
        }

        // Package + fees
        val itemVal = d.itemValue.toDoubleOrNull() ?: 0.0
        val shippingFee = 155.00
        val valuationFee = itemVal * 0.015
        val totalFee = shippingFee + valuationFee
        val fmt = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }

        findViewById<TextView>(R.id.tvItemName).text = d.itemName
        findViewById<TextView>(R.id.tvItemValue).text = "PHP ${fmt.format(itemVal)}"
        findViewById<TextView>(R.id.tvPackageType).text = d.packageType
        findViewById<TextView>(R.id.tvShippingFee).text = "₱ ${fmt.format(shippingFee)}"
        findViewById<TextView>(R.id.tvValuationFee).text = "₱ ${fmt.format(valuationFee)}"
        findViewById<TextView>(R.id.tvTotalFee).text = "₱ ${fmt.format(totalFee)}"

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnBookNow).setOnClickListener {
            val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please agree to the Terms of Service.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitBooking()
        }
    }

    private fun submitBooking() {
        val d = BookingData
        val params = mutableMapOf(
            "first_name" to d.firstName,
            "last_name" to d.lastName,
            "phone" to d.phone,
            "province" to d.province,
            "city" to d.city,
            "drop_off_branch" to d.dropOffBranch,
            "delivery_method" to d.deliveryMethod,
            "r_fname" to d.rFname,
            "r_lname" to d.rLname,
            "r_phone" to d.rPhone,
            "r_province" to d.rProvince,
            "r_city" to d.rCity,
            "r_branch" to d.rBranch,
            "r_street" to d.rStreet,
            "r_delivery_province" to d.rDelProvince,
            "r_delivery_city" to d.rDelCity,
            "item_name" to d.itemName,
            "item_value" to d.itemValue,
            "package_type" to d.packageType,
            "lead_time" to d.leadTime,
            "payment_collection" to d.paymentCollection
        )

        val btnBook = findViewById<MaterialButton>(R.id.btnBookNow)
        btnBook.isEnabled = false
        btnBook.text = "Booking..."

        lifecycleScope.launch {
            val result = ApiClient.post("booking_submit.php", params)
            runOnUiThread {
                btnBook.isEnabled = true
                btnBook.text = "Book Now"

                if (result.optBoolean("ok", false)) {
                    BookingData.clear()
                    val intent = Intent(this@BookConfirmationActivity, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    intent.putExtra("msg", "Shipment booked successfully!")
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@BookConfirmationActivity,
                        result.optString("error", "Booking failed. Please try again."),
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getDropoffDeadline(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, 3)
        val sdf = java.text.SimpleDateFormat("EEE dd MMM yyyy", Locale.US)
        return sdf.format(cal.time)
    }
}
