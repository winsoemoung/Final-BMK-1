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

    // 10. Kalaymyo Door-to-Door Delivery & Payment Tracking
    val deliveryStatus: String = "PENDING", // "UNLOADED_PAID" (ကုန်ချ ငွေရှင်းပြီး), "UNLOADED_CREDIT" (ကုန်ချ ငွေပေးမည့်ရက်), "UNLOADED_PENDING" (ကုန်ချပြီး/ငွေမရှင်းရသေး), "DAMAGED" (ကုန်ပစ္စည်း ပျက်စီး/လျော်ကြေး), "PENDING" (ကုန်ချဆဲ/မချရသေး)
    val promisedPaymentDate: String = "", // "YYYY-MM-DD" (e.g. "2026-10-30")
    val quarterName: String = "", // ရပ်ကွက် (e.g. "တောင်ဖီလာ", "တဟာန်", "ညောင်ပင်သာ")
    val streetName: String = "", // လမ်း (e.g. "၈၄ လမ်း", "လမ်း ၃၀")

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

@Entity(tableName = "print_templates")
data class PrintTemplateEntity(
    @PrimaryKey
    val id: Int, // 1 to 7
    val templateName: String, // e.g. "ပုံစံ ၁ (နမူနာ ၁ - ၈ ကော်လံ ယာဉ်တင်စာရင်း)"
    val paperWidthMm: Int = 80, // 58, 80, 100
    val headerSalutation: String = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
    val gateName: String = "ဘုန်းမောင်ခန့်",
    val routeTitle: String = "မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး",
    val mandalayAddress: String = "စွယ်တော်ကားကွင်း၊ မန္တလေးမြို့",
    val mandalayPhones: String = "09-987769641, 09-449362369",
    val kalayAddress: String = "နယ်မြေ(၄)၊ တောင်ဖီလာရပ်ကွက်၊ ကလေးမြို့",
    val kalayPhones: String = "09-987769642, 09-449362352",
    val hasTableGrid: Boolean = true,
    val tableColumns: String = "စဉ်, အမည်, အကြောင်းအရာ, ဦးရေ, နှုန်း, သင့်ငွေကျပ်, လ/သ, မှတ်ချက်",
    val tableColumnWidths: String = "8, 18, 24, 10, 10, 14, 8, 8",
    val tableRowCount: Int = 8,
    val footerNote: String = "အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်",
    val showSignatureLine: Boolean = true,
    val signatureLabel: String = "လက်မှတ်",
    val voucherNumberPosition: String = "TOP_RIGHT", // "TOP_RIGHT", "BELOW_HEADER", "BOTTOM_LEFT"
    val showQrBarcode: Boolean = true,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "printed_vouchers",
    indices = [androidx.room.Index(value = ["voucherNumber"], unique = true)]
)
data class PrintedVoucherEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voucherNumber: String, // e.g. "0100", "0092", "1195"
    val issueDate: String, // e.g. "2026-10-26"
    val createdTimestamp: Long = System.currentTimeMillis(),
    val templateId: Int = 1,
    val templateName: String = "ပုံစံ ၁",
    val paperWidthMm: Int = 80,
    val cargoId: Long? = null,
    val truckId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val carNumber: String = "",
    val driverName: String = "",
    val itemDescription: String = "",
    val quantity: Int = 1,
    val weightViss: Double = 0.0,
    val freightFee: Double = 0.0,
    val laborFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentStatus: String = "PAID",
    val tableRowsJson: String = "", // JSON list of rows for table view
    val remarks: String = ""
)

