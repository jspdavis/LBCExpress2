package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONArray

class BookReceiverActivity : AppCompatActivity() {

    private val branchData = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
    private var selectedDeliveryMethod = "Branch Pick Up"

    // Track selected values directly so we don't rely on spinner.selectedItem
    private var selectedRProvince = ""
    private var selectedRCity = ""
    private var selectedRBranch = ""
    private var selectedDelProvince = ""
    private var selectedDelCity = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_receiver)

        findViewById<View>(R.id.tvBack).setOnClickListener { finish() }

        val optBranch    = findViewById<LinearLayout>(R.id.optBranchPickup)
        val optRider     = findViewById<LinearLayout>(R.id.optRiderDelivery)
        val sectionBranch = findViewById<LinearLayout>(R.id.sectionBranchPickup)
        val sectionRider  = findViewById<LinearLayout>(R.id.sectionRiderDelivery)

        val spinnerRProvince  = findViewById<Spinner>(R.id.spinnerRProvince)
        val spinnerRCity      = findViewById<Spinner>(R.id.spinnerRCity)
        val spinnerRBranch    = findViewById<Spinner>(R.id.spinnerRBranch)
        val spinnerDelProvince = findViewById<Spinner>(R.id.spinnerDelProvince)
        val spinnerDelCity    = findViewById<Spinner>(R.id.spinnerDelCity)

        // Initialize all spinners with placeholder adapters immediately
        setPlaceholder(spinnerRProvince, "Loading provinces...")
        setPlaceholder(spinnerRCity, "Select Province first")
        setPlaceholder(spinnerRBranch, "Select City first")
        setPlaceholder(spinnerDelProvince, "Loading provinces...")
        setPlaceholder(spinnerDelCity, "Select Province first")

        fun selectBranchPickup() {
            selectedDeliveryMethod = "Branch Pick Up"
            optBranch.setBackgroundResource(R.drawable.delivery_option_selected)
            optRider.setBackgroundResource(R.drawable.delivery_option_normal)
            sectionBranch.visibility = View.VISIBLE
            sectionRider.visibility = View.GONE
        }

        fun selectRiderDelivery() {
            selectedDeliveryMethod = "Rider Delivery"
            optRider.setBackgroundResource(R.drawable.delivery_option_selected)
            optBranch.setBackgroundResource(R.drawable.delivery_option_normal)
            sectionBranch.visibility = View.GONE
            sectionRider.visibility = View.VISIBLE
        }

        optBranch.setOnClickListener { selectBranchPickup() }
        optRider.setOnClickListener { selectRiderDelivery() }
        selectBranchPickup()

        // Load branches then wire up spinners
        loadBranches { provinces ->
            // Branch pick-up spinners
            setSpinner(spinnerRProvince, listOf("Select Province") + provinces) { pos ->
                selectedRProvince = if (pos == 0) "" else provinces[pos - 1]
                selectedRCity = ""; selectedRBranch = ""
                if (pos == 0) {
                    setPlaceholder(spinnerRCity, "Select Province first")
                    setPlaceholder(spinnerRBranch, "Select City first")
                    return@setSpinner
                }
                val cities = branchData[selectedRProvince]?.keys?.sorted() ?: emptyList()
                setSpinner(spinnerRCity, listOf("Select City") + cities) { pos2 ->
                    selectedRCity = if (pos2 == 0) "" else cities[pos2 - 1]
                    selectedRBranch = ""
                    if (pos2 == 0) {
                        setPlaceholder(spinnerRBranch, "Select City first")
                        return@setSpinner
                    }
                    val branches = branchData[selectedRProvince]?.get(selectedRCity) ?: emptyList()
                    setSpinner(spinnerRBranch, listOf("Select Branch") + branches) { pos3 ->
                        selectedRBranch = if (pos3 == 0) "" else branches[pos3 - 1]
                    }
                }
            }

            // Rider delivery spinners (province + city only)
            setSpinner(spinnerDelProvince, listOf("Select Province") + provinces) { pos ->
                selectedDelProvince = if (pos == 0) "" else provinces[pos - 1]
                selectedDelCity = ""
                if (pos == 0) {
                    setPlaceholder(spinnerDelCity, "Select Province first")
                    return@setSpinner
                }
                val cities = branchData[selectedDelProvince]?.keys?.sorted() ?: emptyList()
                setSpinner(spinnerDelCity, listOf("Select City") + cities) { pos2 ->
                    selectedDelCity = if (pos2 == 0) "" else cities[pos2 - 1]
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            val rFname = findViewById<TextInputEditText>(R.id.etRFirstName).text?.toString()?.trim() ?: ""
            val rLname = findViewById<TextInputEditText>(R.id.etRLastName).text?.toString()?.trim() ?: ""
            val rPhone = findViewById<TextInputEditText>(R.id.etRPhone).text?.toString()?.trim() ?: ""

            if (rFname.isEmpty() || rLname.isEmpty() || rPhone.isEmpty()) {
                Toast.makeText(this, "Please fill in receiver details.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            BookingData.deliveryMethod = selectedDeliveryMethod
            BookingData.rFname = rFname
            BookingData.rLname = rLname
            BookingData.rPhone = rPhone

            if (selectedDeliveryMethod == "Branch Pick Up") {
                if (selectedRProvince.isEmpty() || selectedRCity.isEmpty() || selectedRBranch.isEmpty()) {
                    Toast.makeText(this, "Please select province, city, and branch.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                BookingData.rProvince = selectedRProvince
                BookingData.rCity     = selectedRCity
                BookingData.rBranch   = selectedRBranch
            } else {
                val street = findViewById<TextInputEditText>(R.id.etStreet).text?.toString()?.trim() ?: ""
                if (street.isEmpty() || selectedDelProvince.isEmpty() || selectedDelCity.isEmpty()) {
                    Toast.makeText(this, "Please fill in delivery address.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                BookingData.rStreet      = street
                BookingData.rDelProvince = selectedDelProvince
                BookingData.rDelCity     = selectedDelCity
            }

            startActivity(Intent(this, BookPackageActivity::class.java))
        }
    }

    /** Set a spinner with a single placeholder item */
    private fun setPlaceholder(spinner: Spinner, text: String) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf(text))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    /** Set a spinner with items and a callback when an item is selected */
    private fun setSpinner(spinner: Spinner, items: List<String>, onSelected: (Int) -> Unit) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                onSelected(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun loadBranches(onLoaded: (List<String>) -> Unit) {
        lifecycleScope.launch {
            val result = ApiClient.get("branches.php")
            runOnUiThread {
                if (result.optBoolean("ok", false)) {
                    val branches = result.optJSONArray("branches") ?: JSONArray()
                    for (i in 0 until branches.length()) {
                        val b = branches.getJSONObject(i)
                        branchData
                            .getOrPut(b.optString("province")) { mutableMapOf() }
                            .getOrPut(b.optString("city")) { mutableListOf() }
                            .add(b.optString("name"))
                    }
                    onLoaded(branchData.keys.sorted())
                } else {
                    Toast.makeText(this@BookReceiverActivity,
                        "Could not load branches. Check your connection.", Toast.LENGTH_LONG).show()
                    onLoaded(emptyList())
                }
            }
        }
    }
}
