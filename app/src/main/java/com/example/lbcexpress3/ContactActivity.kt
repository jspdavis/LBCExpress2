package com.example.lbcexpress3

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class ContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Call hotline
        findViewById<MaterialCardView>(R.id.cardHotline).setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:8585999")
            }
            startActivity(intent)
        }

        // Send email
        findViewById<MaterialCardView>(R.id.cardEmail).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:customercare@lbcexpress.com")
                putExtra(Intent.EXTRA_SUBJECT, "Customer Inquiry")
            }
            startActivity(Intent.createChooser(intent, "Send Email"))
        }

        // Open website
        findViewById<MaterialCardView>(R.id.cardWebsite).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.lbcexpress.com")
            }
            startActivity(intent)
        }

        // Facebook
        findViewById<MaterialButton>(R.id.btnFacebook).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://www.facebook.com/LBCExpress")
            }
            startActivity(intent)
        }

        // Twitter/X
        findViewById<MaterialButton>(R.id.btnTwitter).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://twitter.com/LBCExpress")
            }
            startActivity(intent)
        }
    }
}
