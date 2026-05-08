package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONArray

class BookSenderActivity : AppCompatActivity() {

    private val branchData = mutableMapOf<String, MutableMap<String, MutableList<String>>>()

    // Track selected values directly — don't rely on spinner.selectedItem
    private var selectedProvince = ""
    private var selectedCity = ""
    private var selectedBranch = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_sender)

        // BookingData.clear() is now called in DashboardActivity before launching this screen

        findViewById<View>(R.id.tvBack).setOnClickListener { finish() }

        val spinnerProvince = findViewById<Spinner>(R.id.spinnerProvince)
        val spinnerCity     = findViewById<Spinner>(R.id.spinnerCity)
        val spinnerBranch   = findViewById<Spinner>(R.id.spinnerBranch)

        // Initialize with placeholders immediately
        setPlaceholder(spinnerProvince, "Loading provinces...")
        setPlaceholder(spinnerCity, "Select Province first")
        setPlaceholder(spinnerBranch, "Select City first")

        loadBranches { provinces ->
            setSpinner(spinnerProvince, listOf("Select Province") + provinces) { pos ->
                selectedProvince = if (pos == 0) "" else provinces[pos - 1]
                selectedCity = ""; selectedBranch = ""
                if (pos == 0) {
                    setPlaceholder(spinnerCity, "Select Province first")
                    setPlaceholder(spinnerBranch, "Select City first")
                    return@setSpinner
                }
                val cities = branchData[selectedProvince]?.keys?.sorted() ?: emptyList()
                setSpinner(spinnerCity, listOf("Select City") + cities) { pos2 ->
                    selectedCity = if (pos2 == 0) "" else cities[pos2 - 1]
                    selectedBranch = ""
                    if (pos2 == 0) {
                        setPlaceholder(spinnerBranch, "Select City first")
                        return@setSpinner
                    }
                    val branches = branchData[selectedProvince]?.get(selectedCity) ?: emptyList()
                    setSpinner(spinnerBranch, listOf("Select Branch") + branches) { pos3 ->
                        selectedBranch = if (pos3 == 0) "" else branches[pos3 - 1]
                    }
                }
            }
        }

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            val firstName = findViewById<TextInputEditText>(R.id.etFirstName).text?.toString()?.trim() ?: ""
            val lastName  = findViewById<TextInputEditText>(R.id.etLastName).text?.toString()?.trim() ?: ""
            val phone     = findViewById<TextInputEditText>(R.id.etPhone).text?.toString()?.trim() ?: ""

            if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedProvince.isEmpty() || selectedCity.isEmpty() || selectedBranch.isEmpty()) {
                Toast.makeText(this, "Please select province, city, and branch.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            BookingData.firstName    = firstName
            BookingData.lastName     = lastName
            BookingData.phone        = phone
            BookingData.province     = selectedProvince
            BookingData.city         = selectedCity
            BookingData.dropOffBranch = selectedBranch

            startActivity(Intent(this, BookReceiverActivity::class.java))
        }
    }

    private fun setPlaceholder(spinner: Spinner, text: String) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf(text))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

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
                    Toast.makeText(this@BookSenderActivity,
                        "Could not load branches. Check your connection.", Toast.LENGTH_LONG).show()
                    onLoaded(emptyList())
                }
            }
        }
    }
}
