package com.example.lbcexpress3

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lbc_session", Context.MODE_PRIVATE)

    // ── Customer ──────────────────────────────────────────────
    fun saveCustomer(id: Int, name: String, email: String) {
        prefs.edit()
            .putString("user_type", "customer")
            .putInt("cust_id", id)
            .putString("cust_name", name)
            .putString("cust_email", email)
            .apply()
        applyToApiClient()
    }

    fun getCustId(): Int = prefs.getInt("cust_id", -1)
    fun getCustName(): String = prefs.getString("cust_name", "") ?: ""

    // ── Employee ──────────────────────────────────────────────
    fun saveEmployee(id: Int, name: String, role: String, branchId: Int) {
        prefs.edit()
            .putString("user_type", "employee")
            .putInt("emp_id", id)
            .putString("emp_name", name)
            .putString("emp_role", role)
            .putInt("emp_branch_id", branchId)
            .apply()
        applyToApiClient()
    }

    fun getEmpId(): Int = prefs.getInt("emp_id", -1)
    fun getEmpName(): String = prefs.getString("emp_name", "") ?: ""
    fun getEmpRole(): String = prefs.getString("emp_role", "") ?: ""
    fun getEmpBranchId(): Int = prefs.getInt("emp_branch_id", -1)

    // ── Common ────────────────────────────────────────────────
    fun getUserType(): String = prefs.getString("user_type", "") ?: ""

    fun isLoggedIn(): Boolean =
        prefs.getInt("cust_id", -1) != -1 || prefs.getInt("emp_id", -1) != -1

    fun logout() {
        prefs.edit().clear().apply()
        ApiClient.authId   = -1
        ApiClient.authType = ""
    }

    /**
     * Called on app start (MainActivity) and after every save —
     * ensures ApiClient always has the right credentials loaded from prefs.
     */
    fun applyToApiClient() {
        val type = getUserType()
        when (type) {
            "customer" -> {
                ApiClient.authId   = getCustId()
                ApiClient.authType = "customer"
            }
            "employee" -> {
                ApiClient.authId   = getEmpId()
                ApiClient.authType = "employee"
            }
            else -> {
                ApiClient.authId   = -1
                ApiClient.authType = ""
            }
        }
    }
}
