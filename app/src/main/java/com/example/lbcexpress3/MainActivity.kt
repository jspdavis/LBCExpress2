package com.example.lbcexpress3

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupTrackButton()
        setupServiceCards()
        setupQuickLinks()
        setupBottomNavigation()
    }

    private fun setupTrackButton() {
        val etTracking = findViewById<TextInputEditText>(R.id.etTrackingNumber)
        val btnTrack = findViewById<MaterialButton>(R.id.btnTrack)

        btnTrack.setOnClickListener {
            val trackingNumber = etTracking.text?.toString()?.trim() ?: ""
            if (trackingNumber.isEmpty()) {
                Toast.makeText(this, getString(R.string.tracking_error), Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, TrackingActivity::class.java)
                intent.putExtra(TrackingActivity.EXTRA_TRACKING_NUMBER, trackingNumber)
                startActivity(intent)
            }
        }
    }

    private fun setupServiceCards() {
        findViewById<MaterialCardView>(R.id.cardDomestic).setOnClickListener {
            openServices()
        }
        findViewById<MaterialCardView>(R.id.cardInternational).setOnClickListener {
            openServices()
        }
        findViewById<MaterialCardView>(R.id.cardMoney).setOnClickListener {
            openServices()
        }
        findViewById<MaterialCardView>(R.id.cardCargo).setOnClickListener {
            openServices()
        }
        findViewById<MaterialCardView>(R.id.cardBills).setOnClickListener {
            openServices()
        }
        findViewById<MaterialCardView>(R.id.cardEcommerce).setOnClickListener {
            openServices()
        }
    }

    private fun setupQuickLinks() {
        findViewById<LinearLayout>(R.id.quickLinkBranch).setOnClickListener {
            startActivity(Intent(this, BranchesActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.quickLinkRates).setOnClickListener {
            Toast.makeText(this, "Shipping Rates coming soon", Toast.LENGTH_SHORT).show()
        }
        findViewById<LinearLayout>(R.id.quickLinkContact).setOnClickListener {
            startActivity(Intent(this, ContactActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_track -> {
                    startActivity(Intent(this, TrackingActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_services -> {
                    openServices()
                    true
                }
                R.id.nav_branches -> {
                    startActivity(Intent(this, BranchesActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_more -> {
                    startActivity(Intent(this, ContactActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun openServices() {
        startActivity(Intent(this, ServicesActivity::class.java))
    }
}
