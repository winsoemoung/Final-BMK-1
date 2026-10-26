package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cargo_shipments")
data class CargoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackingNumber: String,
    
    // 1. ကုန်မာ / ကုန်ပွ
    val cargoDensityType: String = "HEAVY", // "HEAVY" (ကုန်မာ) or "BULK" (ကုန်ပွ)
    
    // 2. ကုန်ပစ္စည်း အမည်
    val cargoName: String = "",
    
    // 3. ကုန်ပစ္စည်း တစ်ခု အလေးချိန် + ယူနစ်
    val unitWeight: Double = 0.0,
    val weightUnit: String = "ပိဿာ", // "ပိဿာ", "ခု", "အိတ်", "ဖာ", "ပုံး", "ကီလို"
    
    // 4. Customer လာအပ်သော အရေအတွက်
    val quantity: Int = 1,
    
    // 5. ကုန်ပစ္စည်း အသေးစိတ် ဖော်ပြချက်
    val itemDescription: String = "",
    
    // Total Calculated Weight
    val weightViss: Double = 0.0, // ပိဿာ
    val weightKg: Double = 0.0, // ကီလိုဂရမ်
    
    // 6. ပို့သူ အချက်အလက် (မန္တလေးဂိတ်)
    val senderName: String,
    val senderPhone: String,
    val senderAddress: String = "",
    
    // 7. ခေါက်တို သွားတိုက်ရန် အမှန်ခြစ်
    val isShuttlePickup: Boolean = false, // ခေါက်တို သွားတိုက်/သွားကြို
    val shuttleFee: Double = 0.0, // ခေါက်တို ကြေး
    val shuttleLaborFee: Double = 0.0, // ပစ္စည်း အတင် အချ အလုပ်သမားခ
    
    // 8. Kalaymyo မှ လက်ခံသူ
    val receiverName: String,
    val receiverPhone: String,
    val receiverShopAddress: String, // ကလေးမြို့ ပို့ရမည့် ဆိုင် / လိပ်စာ
    
    // Cargo Type Category
    val cargoType: String = "General Cargo",
    
    // Rate Calculations
    val freightRatePerViss: Double = 1200.0, // ၁ ပိဿာ တန်ဆာခ (default 1200)
    val freightFee: Double = 0.0, // weightViss * freightRatePerViss
    val mdyLoadingRatePerViss: Double = 25.0, // မန္တလေး ၁၂ ဘီး တင်ခ (default 25)
    val mdyLoadingLaborFee: Double = 0.0, // weightViss * mdyLoadingRatePerViss
    val klyUnloadingRatePerViss: Double = 25.0, // ကလေးမြို့ ၁၂ ဘီး ချခ (default 25)
    val klyUnloadingLaborFee: Double = 0.0, // weightViss * klyUnloadingRatePerViss
    
    val totalAmount: Double = 0.0,
    val paymentStatus: String = "PAID", // "PAID", "COLLECT", "PREPAID"
    val status: String = "MANDALAY_RECEIVED", // "MANDALAY_RECEIVED", "LOADING_12WHEELER", "IN_TRANSIT", "KALAY_ARRIVED", "DELIVERED_TO_SHOP"
    
    // 9. Car Tab မှ Car တစ်စီး ရွေးချယ်မှု
    val truckId: Long? = null,
    val truckNumber: String = "",
    val driverName: String = "",
    val driverPhone: String = "",
    val sequenceOrder: Int = 0, // Position on the truck (1..N)
    
    val createdTimestamp: Long = System.currentTimeMillis(),
    val remarks: String = ""
)

@Entity(tableName = "truck_drivers")
data class TruckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val truckNumber: String,
    val ownerName: String = "", // ကားပိုင်ရှင် အမည်
    val driverName: String, // ကားသမား / ယာဉ်မောင်း အမည်
    val driverPhone: String,
    val truckType: String = "12-Wheeler Box Truck",
    val mandalayToKalayFreightPrice: Double = 1500000.0, // ကားခ
    val advancePayment: Double = 0.0, // ကြိုထုတ်ငွေ
    val dispatchDate: String,
    val status: String = "EMPTY", // "EMPTY", "LOADING", "IN_TRANSIT", "KALAY_ARRIVED", "COMPLETED"
    val totalVissLoaded: Double = 0.0,
    val totalCargosCount: Int = 0,
    val notes: String = ""
)

@Entity(tableName = "compensation_claims")
data class CompensationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cargoId: Long = 0,
    val trackingNumber: String,
    val customerName: String,
    val customerPhone: String,
    val damageDescription: String,
    val claimAmount: Double,
    val paidAmount: Double = 0.0,
    val status: String = "PENDING", // "PENDING", "PAID"
    val date: String,
    val notes: String = ""
)

@Entity(tableName = "worker_labor_records")
data class WorkerLaborEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gateBranch: String, // "MANDALAY" or "KALAY"
    val workerName: String,
    val date: String,
    val vissHandled: Double = 0.0,
    val laborRatePerViss: Double = 25.0,
    val shuttleLaborAmount: Double = 0.0,
    val totalLaborPaid: Double = 0.0,
    val notes: String = ""
)
