package com.example.lbcexpress3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class BranchesActivity : AppCompatActivity() {

    private val allBranches = listOf(
        Branch("LBC SM North EDSA", "SM City North EDSA, Quezon City, Metro Manila", "Mon–Sun: 10AM–8PM", "(02) 8-585-999"),
        Branch("LBC Robinsons Galleria", "Robinsons Galleria, Ortigas, Pasig City", "Mon–Sun: 10AM–9PM", "(02) 8-585-999"),
        Branch("LBC SM Mall of Asia", "SM Mall of Asia, Pasay City, Metro Manila", "Mon–Sun: 10AM–9PM", "(02) 8-585-999"),
        Branch("LBC Ayala Cebu", "Ayala Center Cebu, Cebu City", "Mon–Sun: 10AM–9PM", "(032) 888-1234"),
        Branch("LBC SM Davao", "SM City Davao, Davao City", "Mon–Sun: 10AM–9PM", "(082) 777-5678"),
        Branch("LBC Iloilo City", "Robinsons Place Iloilo, Iloilo City", "Mon–Sun: 10AM–8PM", "(033) 555-9012"),
        Branch("LBC Cagayan de Oro", "Limketkai Center, Cagayan de Oro City", "Mon–Sun: 10AM–8PM", "(088) 444-3456"),
        Branch("LBC Baguio City", "SM City Baguio, Baguio City", "Mon–Sun: 10AM–8PM", "(074) 333-7890"),
        Branch("LBC Makati Greenbelt", "Greenbelt 3, Makati City, Metro Manila", "Mon–Sun: 10AM–10PM", "(02) 8-585-999"),
        Branch("LBC Alabang Town Center", "Alabang Town Center, Muntinlupa City", "Mon–Sun: 10AM–9PM", "(02) 8-585-999"),
        Branch("LBC Pampanga", "SM City Pampanga, San Fernando, Pampanga", "Mon–Sun: 10AM–8PM", "(045) 222-1234"),
        Branch("LBC Batangas City", "SM City Batangas, Batangas City", "Mon–Sun: 10AM–8PM", "(043) 111-5678"),
        Branch("LBC Zamboanga City", "Zamboanga City Mall, Zamboanga City", "Mon–Sun: 9AM–7PM", "(062) 999-0123"),
        Branch("LBC General Santos", "SM City GenSan, General Santos City", "Mon–Sun: 10AM–8PM", "(083) 888-4567"),
        Branch("LBC Tacloban", "Robinsons Place Tacloban, Tacloban City", "Mon–Sun: 10AM–8PM", "(053) 777-8901")
    )

    private lateinit var adapter: BranchAdapter
    private var filteredBranches = allBranches.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_branches)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = BranchAdapter(filteredBranches) { branch ->
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${branch.phone}")
            }
            startActivity(intent)
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvBranches)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val etSearch = findViewById<TextInputEditText>(R.id.etSearchBranch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterBranches(s?.toString() ?: "")
            }
        })
    }

    private fun filterBranches(query: String) {
        filteredBranches.clear()
        if (query.isEmpty()) {
            filteredBranches.addAll(allBranches)
        } else {
            val lower = query.lowercase()
            filteredBranches.addAll(
                allBranches.filter {
                    it.name.lowercase().contains(lower) ||
                    it.address.lowercase().contains(lower)
                }
            )
        }
        adapter.notifyDataSetChanged()
    }

    data class Branch(
        val name: String,
        val address: String,
        val hours: String,
        val phone: String
    )

    class BranchAdapter(
        private val items: MutableList<Branch>,
        private val onCallClick: (Branch) -> Unit
    ) : RecyclerView.Adapter<BranchAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvBranchName)
            val tvAddress: TextView = view.findViewById(R.id.tvBranchAddress)
            val tvHours: TextView = view.findViewById(R.id.tvBranchHours)
            val tvPhone: TextView = view.findViewById(R.id.tvBranchPhone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_branch, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.tvAddress.text = item.address
            holder.tvHours.text = item.hours
            holder.tvPhone.text = item.phone
            holder.tvPhone.setOnClickListener { onCallClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
