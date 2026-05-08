package com.example.lbcexpress3

/**
 * Singleton that holds booking wizard data across the 4 steps,
 * mirroring the PHP session temp_booking approach.
 */
object BookingData {
    // Step 1 — Sender
    var firstName: String = ""
    var lastName: String = ""
    var phone: String = ""
    var province: String = ""
    var city: String = ""
    var dropOffBranch: String = ""

    // Step 2 — Receiver
    var deliveryMethod: String = "Branch Pick Up"
    var rFname: String = ""
    var rLname: String = ""
    var rPhone: String = ""
    // Branch pick up
    var rProvince: String = ""
    var rCity: String = ""
    var rBranch: String = ""
    // Rider delivery
    var rStreet: String = ""
    var rDelProvince: String = ""
    var rDelCity: String = ""

    // Step 3 — Package
    var itemName: String = ""
    var itemValue: String = ""
    var packageType: String = ""
    var leadTime: String = "Rush"
    var paymentCollection: String = "No"

    fun clear() {
        firstName = ""; lastName = ""; phone = ""
        province = ""; city = ""; dropOffBranch = ""
        deliveryMethod = "Branch Pick Up"
        rFname = ""; rLname = ""; rPhone = ""
        rProvince = ""; rCity = ""; rBranch = ""
        rStreet = ""; rDelProvince = ""; rDelCity = ""
        itemName = ""; itemValue = ""; packageType = ""
        leadTime = "Rush"; paymentCollection = "No"
    }
}
