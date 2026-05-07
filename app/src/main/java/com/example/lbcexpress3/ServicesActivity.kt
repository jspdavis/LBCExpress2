package com.example.lbcexpress3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ServicesActivity : AppCompatActivity() {

    private val services = listOf(
        ServiceItem("📦", "Domestic Delivery", "Fast and reliable delivery across all provinces and cities in the Philippines. Door-to-door service available."),
        ServiceItem("✈️", "International Delivery", "Send packages worldwide to over 200 countries. Partnered with global courier networks for seamless delivery."),
        ServiceItem("🚚", "Cargo Services", "Heavy and bulk shipment solutions for businesses. Ideal for large items and commercial freight."),
        ServiceItem("💰", "Money Remittance", "Send money to your loved ones anywhere in the Philippines. Fast, safe, and convenient."),
        ServiceItem("🧾", "Bills Payment", "Pay your utility bills, credit cards, and other obligations conveniently at any LBC branch."),
        ServiceItem("🛒", "E-Commerce Solutions", "Complete logistics support for online sellers. Integrated with major e-commerce platforms."),
        ServiceItem("📋", "Document Delivery", "Secure and timely delivery of important documents, contracts, and legal papers."),
        ServiceItem("❄️", "Cold Chain Logistics", "Temperature-controlled delivery for perishable goods, medicines, and food products."),
        ServiceItem("🏢", "Corporate Solutions", "Customized logistics solutions for businesses of all sizes. Dedicated account management."),
        ServiceItem("📱", "LBC App Booking", "Book pickups and deliveries through the LBC mobile app. Track in real-time.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val recyclerView = findViewById<RecyclerView>(R.id.rvServices)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ServicesAdapter(services) { service ->
            Toast.makeText(this, "${service.name} - Coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    data class ServiceItem(
        val icon: String,
        val name: String,
        val description: String
    )

    class ServicesAdapter(
        private val items: List<ServiceItem>,
        private val onClick: (ServiceItem) -> Unit
    ) : RecyclerView.Adapter<ServicesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvIcon: TextView = view.findViewById(R.id.tvServiceIcon)
            val tvName: TextView = view.findViewById(R.id.tvServiceName)
            val tvDesc: TextView = view.findViewById(R.id.tvServiceDesc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_service, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvIcon.text = item.icon
            holder.tvName.text = item.name
            holder.tvDesc.text = item.description
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
