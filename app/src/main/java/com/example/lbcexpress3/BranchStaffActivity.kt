package com.example.lbcexpress3

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class BranchStaffActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private var currentTab = "origin"

    // Branch list for forward/update pickers: id → name, province, city
    data class BranchItem(val id: Int, val name: String, val province: String, val city: String)
    private val allBranches = mutableListOf<BranchItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_branch_staff)

        session = SessionManager(this)
        if (session.getEmpRole() != "Branch Staff") {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener { logout() }

        val tabOut = findViewById<TextView>(R.id.tabOutgoing)
        val tabIn  = findViewById<TextView>(R.id.tabIncoming)

        tabOut.setOnClickListener {
            currentTab = "origin"
            tabOut.setTypeface(null, Typeface.BOLD)
            tabOut.setTextColor(getColor(R.color.lbc_red))
            tabIn.setTypeface(null, Typeface.NORMAL)
            tabIn.setTextColor(getColor(R.color.lbc_text_secondary))
            loadData()
        }
        tabIn.setOnClickListener {
            currentTab = "dest"
            tabIn.setTypeface(null, Typeface.BOLD)
            tabIn.setTextColor(getColor(R.color.lbc_red))
            tabOut.setTypeface(null, Typeface.NORMAL)
            tabOut.setTextColor(getColor(R.color.lbc_text_secondary))
            loadData()
        }

        loadData()
    }

    override fun onResume() { super.onResume(); loadData() }

    private fun loadData() {
        lifecycleScope.launch {
            val result = ApiClient.get("branch_staff.php", mapOf("tab" to currentTab))
            runOnUiThread {
                if (!result.optBoolean("ok", false)) {
                    Toast.makeText(this@BranchStaffActivity,
                        result.optString("error", "Failed to load"), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val branchName = result.optString("branchName", "—")
                findViewById<TextView>(R.id.tvBranchName).text = branchName
                renderShipments(result.optJSONArray("shipments") ?: JSONArray())
            }
        }
    }

    private fun renderShipments(items: JSONArray) {
        val container = findViewById<LinearLayout>(R.id.listContainer)
        val empty     = findViewById<LinearLayout>(R.id.emptyState)
        val tvTitle   = findViewById<TextView>(R.id.tvListTitle)
        val tvCount   = findViewById<TextView>(R.id.tvCount)

        container.removeAllViews()
        tvTitle.text = if (currentTab == "origin") "Outgoing Parcels" else "Incoming Parcels"
        tvCount.text = "${items.length()} parcel(s)"

        if (items.length() == 0) {
            empty.visibility = View.VISIBLE
            empty.findViewById<TextView>(R.id.tvEmptyLabel).text =
                if (currentTab == "origin") "No outgoing parcels at this branch."
                else "No incoming parcels forwarded to this branch."
            return
        }
        empty.visibility = View.GONE

        for (i in 0 until items.length()) {
            val s    = items.getJSONObject(i)
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_shipment_row, container, false)

            val shipId   = s.optInt("Ship_Id")
            val tracking = s.optString("Ship_Tracking_Number")
            val status   = s.optString("Ship_Status", "—")
            val isLocked = status == "Arrived at Destination"

            card.findViewById<TextView>(R.id.tvTracking).text = tracking
            card.findViewById<TextView>(R.id.tvCustomer).text =
                "${s.optString("Cust_FName")} ${s.optString("Cust_LName")}"

            val tvStatus = card.findViewById<TextView>(R.id.tvStatus)
            tvStatus.text = status
            tvStatus.background.setTint(Color.parseColor(statusColor(status)))

            val method = s.optString("Bkng_Delivery_Method", "")
            val dest = if (method == "Rider Delivery") {
                val street = s.optString("Bkng_Receiver_Street", "").let { if (it == "null") "" else it }
                val city   = s.optString("Bkng_Receiver_Del_City", "").let { if (it == "null") "" else it }
                val prov   = s.optString("Bkng_Receiver_Del_Province", "").let { if (it == "null") "" else it }
                "🛵 " + listOf(street, city, prov).filter { it.isNotEmpty() }.joinToString(", ")
            } else {
                val branch = s.optString("Bkng_Receiver_Branch", "").let { if (it == "null") "—" else it.ifEmpty { "—" } }
                "🏪 $branch"
            }
            val receiverName = s.optString("Bkng_Receiver_Name", "").let { if (it == "null") "" else it }
            card.findViewById<TextView>(R.id.tvDestination).text =
                if (receiverName.isNotEmpty()) "$receiverName\n$dest" else dest

            val extra = if (currentTab == "origin") {
                val destBranch = s.optString("Dest_Branch_Name", "").let { if (it == "null") "" else it }
                if (destBranch.isNotEmpty()) "→ $destBranch" else ""
            } else {
                val origBranch = s.optString("Orig_Branch_Name", "").let { if (it == "null") "" else it }
                if (origBranch.isNotEmpty()) "From: $origBranch" else ""
            }
            card.findViewById<TextView>(R.id.tvExtra).text = extra

            // Replace single action button with a row of 3 buttons
            val btnAction = card.findViewById<MaterialButton>(R.id.btnAction)
            btnAction.visibility = View.GONE

            // Build action buttons dynamically
            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            if (isLocked) {
                // Locked — show grey label
                val tvLocked = TextView(this).apply {
                    text = "Handed off to destination branch"
                    setTextColor(Color.parseColor("#9E9E9E"))
                    textSize = 11f
                    setPadding(0, 8, 0, 0)
                }
                btnRow.addView(tvLocked)
            } else {
                // Forward button (outgoing tab only)
                if (currentTab == "origin") {
                    btnRow.addView(makeBtn("Forward", "#424242") {
                        showForwardDialog(shipId, tracking)
                    })
                }

                // Assign Rider button (incoming tab, Rider Delivery only)
                val deliveryMethod = s.optString("Bkng_Delivery_Method", "")
                if (currentTab == "dest" && deliveryMethod == "Rider Delivery") {
                    btnRow.addView(makeBtn("Assign Rider", "#e65100") {
                        showAssignRiderDialog(shipId, tracking)
                    })
                }

                // Update Status button
                btnRow.addView(makeBtn("Update", "#0d6efd") {
                    showUpdateStatusDialog(shipId, tracking, status)
                })

                // Delete button
                btnRow.addView(makeBtn("Delete", "#CC0000") {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Shipment")
                        .setMessage("Delete shipment $tracking? This cannot be undone.")
                        .setPositiveButton("Delete") { _, _ -> deleteShipment(shipId) }
                        .setNegativeButton("Cancel", null)
                        .show()
                })
            }

            // Add button row inside the card's content layout
            val cardInner = card.findViewById<LinearLayout>(R.id.cardInnerLayout)
            cardInner.addView(btnRow)

            container.addView(card)
        }
    }

    private fun makeBtn(label: String, colorHex: String, onClick: () -> Unit): MaterialButton {
        return MaterialButton(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(colorHex))
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.setMargins(0, 0, 6, 0)
            layoutParams = lp
            setPadding(4, 12, 4, 12)
            minWidth = 0
            minimumWidth = 0
            setOnClickListener { onClick() }
        }
    }

    // ── ASSIGN RIDER ─────────────────────────────────────────────────────────

    private fun showAssignRiderDialog(shipId: Int, tracking: String) {
        lifecycleScope.launch {
            val result = ApiClient.get("branch_riders.php")
            runOnUiThread {
                if (!result.optBoolean("ok", false)) {
                    Toast.makeText(this@BranchStaffActivity,
                        result.optString("error", "Could not load riders"), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                val ridersArr = result.optJSONArray("riders") ?: JSONArray()
                val view = LayoutInflater.from(this@BranchStaffActivity)
                    .inflate(R.layout.dialog_assign_rider, null)

                val tvTracking = view.findViewById<TextView>(R.id.tvAssignTracking)
                val spinner    = view.findViewById<Spinner>(R.id.spinnerRider)
                val tvNoRiders = view.findViewById<TextView>(R.id.tvNoRiders)

                tvTracking.text = "Tracking: $tracking"

                if (ridersArr.length() == 0) {
                    spinner.visibility = View.GONE
                    tvNoRiders.visibility = View.VISIBLE
                    AlertDialog.Builder(this@BranchStaffActivity)
                        .setTitle("Assign Rider")
                        .setView(view)
                        .setPositiveButton("OK", null)
                        .show()
                    return@runOnUiThread
                }

                val riderNames = mutableListOf("— Choose a Rider —")
                val riderIds   = mutableListOf(-1)
                for (i in 0 until ridersArr.length()) {
                    val r = ridersArr.getJSONObject(i)
                    riderNames.add(r.optString("Emp_FName"))
                    riderIds.add(r.optInt("Emp_Id"))
                }

                val adapter = ArrayAdapter(this@BranchStaffActivity,
                    android.R.layout.simple_spinner_item, riderNames)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter

                var selectedRiderId = -1
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        selectedRiderId = riderIds[pos]
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }

                AlertDialog.Builder(this@BranchStaffActivity)
                    .setTitle("Assign Rider — $tracking")
                    .setView(view)
                    .setPositiveButton("Assign") { _, _ ->
                        if (selectedRiderId == -1) {
                            Toast.makeText(this@BranchStaffActivity,
                                "Please select a rider", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        assignRider(shipId, selectedRiderId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun assignRider(shipId: Int, riderId: Int) {
        lifecycleScope.launch {
            val result = ApiClient.post("assign_rider.php", mapOf(
                "ship_id"  to shipId.toString(),
                "rider_id" to riderId.toString()
            ))
            runOnUiThread {
                Toast.makeText(this@BranchStaffActivity,
                    if (result.optBoolean("ok", false))
                        result.optString("message", "Rider assigned")
                    else
                        result.optString("error", "Assignment failed"),
                    Toast.LENGTH_LONG).show()
                if (result.optBoolean("ok", false)) loadData()
            }
        }
    }

    // ── FORWARD ──────────────────────────────────────────────────────────────

    private fun showForwardDialog(shipId: Int, tracking: String) {
        lifecycleScope.launch {
            val result = ApiClient.get("branches.php")
            runOnUiThread {
                if (!result.optBoolean("ok", false)) {
                    Toast.makeText(this@BranchStaffActivity, "Could not load branches", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }
                val branches = result.optJSONArray("branches") ?: JSONArray()
                allBranches.clear()
                for (i in 0 until branches.length()) {
                    val b = branches.getJSONObject(i)
                    allBranches.add(BranchItem(
                        b.optInt("id"), b.optString("name"),
                        b.optString("province"), b.optString("city")
                    ))
                }

                // Build province → city → branch map
                val branchMap = mutableMapOf<String, MutableMap<String, MutableList<BranchItem>>>()
                allBranches.forEach { b ->
                    branchMap.getOrPut(b.province) { mutableMapOf() }
                        .getOrPut(b.city) { mutableListOf() }
                        .add(b)
                }
                val provinces = branchMap.keys.sorted()

                val view = LayoutInflater.from(this@BranchStaffActivity)
                    .inflate(R.layout.dialog_forward, null)

                val spinnerProv   = view.findViewById<Spinner>(R.id.spinnerFwdProvince)
                val spinnerCity   = view.findViewById<Spinner>(R.id.spinnerFwdCity)
                val spinnerBranch = view.findViewById<Spinner>(R.id.spinnerFwdBranch)
                val etRemarks     = view.findViewById<TextInputEditText>(R.id.etFwdRemarks)

                var selectedBranchId = -1

                fun setAdapter(spinner: Spinner, items: List<String>) {
                    val a = ArrayAdapter(this@BranchStaffActivity,
                        android.R.layout.simple_spinner_item, items)
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = a
                }

                setAdapter(spinnerProv, listOf("Select Province") + provinces)
                setAdapter(spinnerCity, listOf("Select Province first"))
                setAdapter(spinnerBranch, listOf("Select City first"))

                spinnerProv.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        if (pos == 0) return
                        val prov = provinces[pos - 1]
                        val cities = branchMap[prov]?.keys?.sorted() ?: emptyList()
                        setAdapter(spinnerCity, listOf("Select City") + cities)
                        setAdapter(spinnerBranch, listOf("Select City first"))
                        selectedBranchId = -1

                        spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos2: Int, id2: Long) {
                                if (pos2 == 0) return
                                val city = cities[pos2 - 1]
                                val brs = branchMap[prov]?.get(city) ?: emptyList()
                                setAdapter(spinnerBranch, listOf("Select Branch") + brs.map { it.name })
                                selectedBranchId = -1

                                spinnerBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos3: Int, id3: Long) {
                                        selectedBranchId = if (pos3 == 0) -1 else brs[pos3 - 1].id
                                    }
                                    override fun onNothingSelected(p: AdapterView<*>?) {}
                                }
                            }
                            override fun onNothingSelected(p: AdapterView<*>?) {}
                        }
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }

                AlertDialog.Builder(this@BranchStaffActivity)
                    .setTitle("Forward Parcel — $tracking")
                    .setView(view)
                    .setPositiveButton("Forward") { _, _ ->
                        if (selectedBranchId == -1) {
                            Toast.makeText(this@BranchStaffActivity,
                                "Please select a destination branch", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        val remarks = etRemarks.text?.toString()?.trim() ?: ""
                        forwardShipment(shipId, selectedBranchId, remarks)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun forwardShipment(shipId: Int, branchId: Int, remarks: String) {
        lifecycleScope.launch {
            val result = ApiClient.post("forward_branch.php", mapOf(
                "ship_id"   to shipId.toString(),
                "branch_id" to branchId.toString(),
                "remarks"   to remarks
            ))
            runOnUiThread {
                Toast.makeText(this@BranchStaffActivity,
                    if (result.optBoolean("ok", false))
                        result.optString("message", "Parcel forwarded")
                    else
                        result.optString("error", "Forward failed"),
                    Toast.LENGTH_LONG).show()
                if (result.optBoolean("ok", false)) loadData()
            }
        }
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────

    private fun showUpdateStatusDialog(shipId: Int, tracking: String, currentStatus: String) {
        lifecycleScope.launch {
            val info = ApiClient.get("shipment_info.php", mapOf("ship_id" to shipId.toString()))
            runOnUiThread {
                if (!info.optBoolean("ok", false)) {
                    Toast.makeText(this@BranchStaffActivity,
                        info.optString("error", "Could not load shipment info"), Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                val allowedArr  = info.optJSONArray("allowedStatuses") ?: JSONArray()
                val branchesArr = info.optJSONArray("branches") ?: JSONArray()

                val allowedStatuses = (0 until allowedArr.length()).map { allowedArr.getString(it) }

                // Build branch map for location picker
                val branchMap = mutableMapOf<String, MutableMap<String, MutableList<BranchItem>>>()
                for (i in 0 until branchesArr.length()) {
                    val b = branchesArr.getJSONObject(i)
                    val item = BranchItem(b.optInt("Brch_Id"), b.optString("Brch_Name"),
                        b.optString("Brch_Province"), b.optString("Brch_City"))
                    branchMap.getOrPut(item.province) { mutableMapOf() }
                        .getOrPut(item.city) { mutableListOf() }
                        .add(item)
                }
                val provinces = branchMap.keys.sorted()

                val view = LayoutInflater.from(this@BranchStaffActivity)
                    .inflate(R.layout.dialog_update_status, null)

                val tvCurrent     = view.findViewById<TextView>(R.id.tvCurrentStatus)
                val spinnerStatus = view.findViewById<Spinner>(R.id.spinnerNewStatus)
                val spinnerProv   = view.findViewById<Spinner>(R.id.spinnerLocProvince)
                val spinnerCity   = view.findViewById<Spinner>(R.id.spinnerLocCity)
                val spinnerBranch = view.findViewById<Spinner>(R.id.spinnerLocBranch)
                val etRemarks     = view.findViewById<TextInputEditText>(R.id.etUpdateRemarks)

                tvCurrent.text = "Current: $currentStatus"

                fun setAdapter(spinner: Spinner, items: List<String>) {
                    val a = ArrayAdapter(this@BranchStaffActivity,
                        android.R.layout.simple_spinner_item, items)
                    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = a
                }

                setAdapter(spinnerStatus, allowedStatuses)
                // Pre-select current status
                val currentIdx = allowedStatuses.indexOf(currentStatus)
                if (currentIdx >= 0) spinnerStatus.setSelection(currentIdx)

                setAdapter(spinnerProv, listOf("Select Province") + provinces)
                setAdapter(spinnerCity, listOf("Select Province first"))
                setAdapter(spinnerBranch, listOf("— No specific branch —"))

                var selectedLocBranchId   = 0
                var selectedLocBranchName = ""
                var selectedLocCity       = ""
                var selectedLocProvince   = ""

                spinnerProv.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                        selectedLocProvince = if (pos == 0) "" else provinces[pos - 1]
                        selectedLocCity = ""; selectedLocBranchId = 0; selectedLocBranchName = ""
                        if (pos == 0) return
                        val cities = branchMap[selectedLocProvince]?.keys?.sorted() ?: emptyList()
                        setAdapter(spinnerCity, listOf("Select City") + cities)
                        setAdapter(spinnerBranch, listOf("— No specific branch —"))

                        spinnerCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos2: Int, id2: Long) {
                                selectedLocCity = if (pos2 == 0) "" else cities[pos2 - 1]
                                selectedLocBranchId = 0; selectedLocBranchName = ""
                                if (pos2 == 0) return
                                val brs = branchMap[selectedLocProvince]?.get(selectedLocCity) ?: emptyList()
                                setAdapter(spinnerBranch, listOf("— No specific branch —") + brs.map { it.name })

                                spinnerBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(p: AdapterView<*>?, v: View?, pos3: Int, id3: Long) {
                                        if (pos3 == 0) { selectedLocBranchId = 0; selectedLocBranchName = ""; return }
                                        val br = brs[pos3 - 1]
                                        selectedLocBranchId   = br.id
                                        selectedLocBranchName = br.name
                                    }
                                    override fun onNothingSelected(p: AdapterView<*>?) {}
                                }
                            }
                            override fun onNothingSelected(p: AdapterView<*>?) {}
                        }
                    }
                    override fun onNothingSelected(p: AdapterView<*>?) {}
                }

                AlertDialog.Builder(this@BranchStaffActivity)
                    .setTitle("Update Status — $tracking")
                    .setView(view)
                    .setPositiveButton("Save") { _, _ ->
                        val newStatus = spinnerStatus.selectedItem?.toString() ?: ""
                        if (newStatus.isEmpty() || selectedLocProvince.isEmpty()) {
                            Toast.makeText(this@BranchStaffActivity,
                                "Please select a status and location province", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                        updateStatus(
                            shipId, newStatus,
                            selectedLocProvince, selectedLocCity,
                            selectedLocBranchId, selectedLocBranchName,
                            etRemarks.text?.toString()?.trim() ?: ""
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun updateStatus(
        shipId: Int, newStatus: String,
        province: String, city: String,
        branchId: Int, branchName: String,
        remarks: String
    ) {
        lifecycleScope.launch {
            val result = ApiClient.post("update_status.php", mapOf(
                "ship_id"         to shipId.toString(),
                "status"          to newStatus,
                "loc_province"    to province,
                "loc_city"        to city,
                "loc_branch_id"   to branchId.toString(),
                "loc_branch_name" to branchName,
                "remarks"         to remarks
            ))
            runOnUiThread {
                Toast.makeText(this@BranchStaffActivity,
                    if (result.optBoolean("ok", false)) "Status updated"
                    else result.optString("error", "Update failed"),
                    Toast.LENGTH_SHORT).show()
                if (result.optBoolean("ok", false)) loadData()
            }
        }
    }

    // ── DELETE SHIPMENT ───────────────────────────────────────────────────────

    private fun deleteShipment(shipId: Int) {
        lifecycleScope.launch {
            val result = ApiClient.post("delete_shipment.php",
                mapOf("ship_id" to shipId.toString()))
            runOnUiThread {
                Toast.makeText(this@BranchStaffActivity,
                    if (result.optBoolean("ok", false)) "Shipment deleted"
                    else result.optString("error", "Delete failed"),
                    Toast.LENGTH_SHORT).show()
                if (result.optBoolean("ok", false)) loadData()
            }
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
