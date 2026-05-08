package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class BookPackageActivity : AppCompatActivity() {

    data class PackageOption(val label: String, val value: String, val desc: String)

    private val packageOptions = listOf(
        PackageOption("Select Type", "", ""),
        PackageOption("N-Pouch Regular (up to 0.5 kg)", "N-Pouch REG", "Up to 0.5 kg — for documents, letters, contracts"),
        PackageOption("N-Pouch XL (up to 1 kg)", "N-Pouch XL", "Up to 1 kg — for thicker documents, photos, small items"),
        PackageOption("N-Pouch SS / Slim (up to 0.3 kg)", "N-Pouch SS", "Up to 0.3 kg — slim pouch for single documents or IDs"),
        PackageOption("N-Pack Small (up to 1 kg)", "N-Pack Small", "Up to 1 kg — soft pack for clothes, accessories, small items"),
        PackageOption("N-Pack Large (up to 3 kg)", "N-Pack Large", "Up to 3 kg — soft pack for garments, shoes, or multiple items"),
        PackageOption("Kilobox Mini (up to 1 kg)", "Kilobox Mini", "Up to 1 kg — small rigid box, 12×12×12 cm"),
        PackageOption("Kilobox Small (up to 3 kg)", "Kilobox Small", "Up to 3 kg — for electronics, gadgets, small appliances"),
        PackageOption("Kilobox Slim (up to 3 kg)", "Kilobox Slim", "Up to 3 kg — flat slim box for books, tablets, flat items"),
        PackageOption("Kilobox Medium (up to 5 kg)", "Kilobox Medium", "Up to 5 kg — for household goods, multiple items"),
        PackageOption("Kilobox Large (up to 10 kg)", "Kilobox Large", "Up to 10 kg — for larger appliances, bulk items"),
        PackageOption("Kilobox XL (up to 19 kg)", "Kilobox XL", "Up to 19 kg — for heavy or bulky shipments"),
        PackageOption("Small Box (up to 10 kg)", "Small Box", "10×10×10\" interior — clothes, shoes, small electronics"),
        PackageOption("Medium Box (up to 15 kg)", "Medium Box", "12×12×12\" interior — household goods, books, small appliances"),
        PackageOption("Large Box (up to 20 kg)", "Large Box", "14×14×14\" interior — furniture parts, appliances, business stock"),
        PackageOption("Balikbayan Box (up to 50 kg)", "Balikbayan Box", "22×22×22\" interior — OFW box for clothes, gifts, personal items"),
        PackageOption("Own Box / Own Packaging (min. 5 kg)", "Own Box", "Bring your own box — minimum 5 kg, charged per kg")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_package)

        findViewById<View>(R.id.tvBack).setOnClickListener { finish() }

        val spinnerPkg = findViewById<Spinner>(R.id.spinnerPackageType)
        val tvHint = findViewById<TextView>(R.id.tvPackageHint)

        val labels = packageOptions.map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPkg.adapter = adapter

        spinnerPkg.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val desc = packageOptions[pos].desc
                if (desc.isNotEmpty()) {
                    tvHint.text = "📌 $desc"
                    tvHint.visibility = View.VISIBLE
                } else {
                    tvHint.visibility = View.GONE
                }
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            val itemName = findViewById<TextInputEditText>(R.id.etItemName).text?.toString()?.trim() ?: ""
            val itemValue = findViewById<TextInputEditText>(R.id.etItemValue).text?.toString()?.trim() ?: ""
            val pkgPos = spinnerPkg.selectedItemPosition

            if (itemName.isEmpty() || itemValue.isEmpty()) {
                Toast.makeText(this, "Please fill in item name and value.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pkgPos == 0) {
                Toast.makeText(this, "Please select a package type.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val leadTime = when (findViewById<RadioGroup>(R.id.rgLeadTime).checkedRadioButtonId) {
                R.id.rbRush -> "Rush"
                else -> "Regular"
            }
            val cod = when (findViewById<RadioGroup>(R.id.rgCod).checkedRadioButtonId) {
                R.id.rbCodYes -> "Yes"
                else -> "No"
            }

            BookingData.itemName = itemName
            BookingData.itemValue = itemValue
            BookingData.packageType = packageOptions[pkgPos].value
            BookingData.leadTime = leadTime
            BookingData.paymentCollection = cod

            startActivity(Intent(this, BookConfirmationActivity::class.java))
        }
    }
}
