package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CargoDao
import com.example.data.dao.CompensationDao
import com.example.data.dao.PrintTemplateDao
import com.example.data.dao.PrintedVoucherDao
import com.example.data.dao.TruckDao
import com.example.data.dao.WorkerLaborDao
import com.example.data.model.CargoEntity
import com.example.data.model.CompensationEntity
import com.example.data.model.PrintTemplateEntity
import com.example.data.model.PrintedVoucherEntity
import com.example.data.model.TruckEntity
import com.example.data.model.WorkerLaborEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CargoEntity::class,
        TruckEntity::class,
        CompensationEntity::class,
        WorkerLaborEntity::class,
        PrintTemplateEntity::class,
        PrintedVoucherEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cargoDao(): CargoDao
    abstract fun truckDao(): TruckDao
    abstract fun compensationDao(): CompensationDao
    abstract fun workerLaborDao(): WorkerLaborDao
    abstract fun printTemplateDao(): PrintTemplateDao
    abstract fun printedVoucherDao(): PrintedVoucherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bmk_express_database_v5"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val cargoDao = database.cargoDao()
            val truckDao = database.truckDao()
            val compensationDao = database.compensationDao()
            val workerDao = database.workerLaborDao()

            // Pre-populate Trucks
            val truckId1 = truckDao.insertTruck(
                TruckEntity(
                    truckNumber = "MDY 7D-1212 (12-Wheeler Box)",
                    driverName = "ဦးကိုကို (Driver U Ko Ko)",
                    driverPhone = "0945001122",
                    mandalayToKalayFreightPrice = 1500000.0,
                    dispatchDate = "2026-10-25",
                    status = "IN_TRANSIT",
                    totalVissLoaded = 1250.0,
                    totalCargosCount = 18,
                    notes = "Mandalay -> Kalaymyo Direct Line"
                )
            )

            val truckId2 = truckDao.insertTruck(
                TruckEntity(
                    truckNumber = "MDY 9B-8833 (12-Wheeler)",
                    driverName = "ဦးကျော်စန်း (Driver U Kyaw San)",
                    driverPhone = "0979011223",
                    mandalayToKalayFreightPrice = 1600000.0,
                    dispatchDate = "2026-10-26",
                    status = "LOADING",
                    totalVissLoaded = 800.0,
                    totalCargosCount = 12,
                    notes = "Loading at Mandalay Gate"
                )
            )

            // Pre-populate Cargo
            cargoDao.insertCargo(
                CargoEntity(
                    trackingNumber = "BMK-MDY-KLY-90101",
                    cargoDensityType = "HEAVY", // ကုန်မာ
                    cargoName = "စက်ပစ္စည်း အပိုပစ္စည်း",
                    unitWeight = 16.66,
                    weightUnit = "ပုံး",
                    quantity = 3,
                    itemDescription = "Toyota Engine Cylinder & Brake Pads (၃ ပုံး)",
                    weightViss = 50.0,
                    weightKg = 81.65,
                    senderName = "ဒေါ်အေးအေး (Daw Aye Aye)",
                    senderPhone = "09450012345",
                    senderAddress = "၃၈ လမ်း၊ ၈၄ လမ်း၊ မန္တလေး",
                    isShuttlePickup = true,
                    shuttleFee = 5000.0,
                    shuttleLaborFee = 3000.0,
                    receiverName = "ဦးကျော်စွာ (U Kyaw Swar)",
                    receiverPhone = "09250098765",
                    receiverShopAddress = "ရွှေဆိုင်၊ တဟာန်ရပ်ကွက်၊ နာရီစင်အနီး၊ ကလေးမြို့",
                    freightRatePerViss = 1200.0,
                    freightFee = 60000.0,
                    mdyLoadingRatePerViss = 25.0,
                    mdyLoadingLaborFee = 1250.0,
                    klyUnloadingRatePerViss = 25.0,
                    klyUnloadingLaborFee = 1250.0,
                    totalAmount = 70500.0,
                    paymentStatus = "PAID",
                    status = "IN_TRANSIT",
                    truckId = truckId1,
                    truckNumber = "MDY 7D-1212 (12-Wheeler)",
                    driverName = "ဦးကိုကို (09-45001122)",
                    driverPhone = "0945001122",
                    remarks = "ကလေးမြို့ ဆိုင်အရောက် ပို့ရန်"
                )
            )

            cargoDao.insertCargo(
                CargoEntity(
                    trackingNumber = "BMK-MDY-KLY-90102",
                    cargoDensityType = "BULK", // ကုန်ပွ
                    cargoName = "အထည်အလိပ် ပုံးများ",
                    unitWeight = 16.0,
                    weightUnit = "ဖာ",
                    quantity = 5,
                    itemDescription = "အဝတ်အထည်နှင့် လူသုံးကုန် ပစ္စည်းများ (၅ ဖာ)",
                    weightViss = 80.0,
                    weightKg = 130.63,
                    senderName = "ဦးစိုးဝင်း (U Soe Win)",
                    senderPhone = "09970011223",
                    senderAddress = "စက်မှုဇုန် (၁)၊ မန္တလေး",
                    isShuttlePickup = false,
                    shuttleFee = 0.0,
                    shuttleLaborFee = 0.0,
                    receiverName = "ဒေါ်စန်းစန်း (Daw San San)",
                    receiverPhone = "09780099887",
                    receiverShopAddress = "စန်းစန်း ကုန်စုံဆိုင်၊ ညောင်ပင်သာ၊ ကလေးမြို့",
                    freightRatePerViss = 1200.0,
                    freightFee = 96000.0,
                    mdyLoadingRatePerViss = 25.0,
                    mdyLoadingLaborFee = 2000.0,
                    klyUnloadingRatePerViss = 25.0,
                    klyUnloadingLaborFee = 2000.0,
                    totalAmount = 100000.0,
                    paymentStatus = "COLLECT",
                    status = "MANDALAY_RECEIVED",
                    truckId = truckId2,
                    truckNumber = "MDY 9B-8833 (12-Wheeler)",
                    driverName = "ဦးကျော်စန်း (09-79011223)",
                    driverPhone = "0979011223",
                    remarks = "မန္တလေးဂိတ်တွင် ၁၂ ဘီး ကားပေါ် တင်နေဆဲ"
                )
            )

            // Compensation Records
            compensationDao.insertClaim(
                CompensationEntity(
                    cargoId = 1,
                    trackingNumber = "BMK-MDY-KLY-88102",
                    customerName = "ဦးဘသန်း (U Ba Than)",
                    customerPhone = "09790112233",
                    damageDescription = "မိုးရေစို၍ ကာတွန်းပုံး စုတ်ပြဲသွားခြင်း",
                    claimAmount = 25000.0,
                    paidAmount = 25000.0,
                    status = "PAID",
                    date = "2026-10-24",
                    notes = "ကလေးမြို့ဂိတ်တွင် လျော်ကြေး ရှင်းပေးပြီး"
                )
            )

            // Workers Labor Records
            workerDao.insertLaborRecord(
                WorkerLaborEntity(
                    gateBranch = "MANDALAY",
                    workerName = "ကိုအောင်မြင့် (Mandalay Loading Team)",
                    date = "2026-10-25",
                    vissHandled = 1250.0,
                    laborRatePerViss = 25.0,
                    shuttleLaborAmount = 15000.0,
                    totalLaborPaid = 46250.0,
                    notes = "၁၂ ဘီး ကားပေါ် ကုန်တင်ခ (၂၅ ကျပ်/ပိဿာ) + ခေါက်တို တင်ချခ"
                )
            )

            workerDao.insertLaborRecord(
                WorkerLaborEntity(
                    gateBranch = "KALAY",
                    workerName = "ကိုကျော်ဇင် (Kalay Unloading Team)",
                    date = "2026-10-25",
                    vissHandled = 1250.0,
                    laborRatePerViss = 25.0,
                    shuttleLaborAmount = 0.0,
                    totalLaborPaid = 31250.0,
                    notes = "ကလေးမြို့ ၁၂ ဘီး ကားပေါ်မှ ကုန်ချခ (၂၅ ကျပ်/ပိဿာ)"
                )
            )

            // Prepopulate 7 Customizable Print Templates
            val templateDao = database.printTemplateDao()
            val voucherDao = database.printedVoucherDao()

            val defaultTemplates = listOf(
                // Template 1: Matches User Uploaded Image 1 (Full 8-column waybill/manifest)
                PrintTemplateEntity(
                    id = 1,
                    templateName = "ပုံစံ ၁ (နမူနာ ၁ - ၈ ကော်လံ ယာဉ်တင်စာရင်း Waybill)",
                    paperWidthMm = 80,
                    headerSalutation = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
                    gateName = "ဘုန်းမောင်ခန့်",
                    routeTitle = "မန္တလေး <=> ကလေး ကုန်စည်အမြန် ပို့ဆောင်ရေး",
                    mandalayAddress = "စွယ်တော်ကားကွင်း၊ မန္တလေးမြို့",
                    mandalayPhones = "09-987769641, 09-449362369",
                    kalayAddress = "နယ်မြေ(၄)၊ တောင်ဖီလာရပ်ကွက်၊ ကလေးမြို့",
                    kalayPhones = "09-987769642, 09-449362352",
                    hasTableGrid = true,
                    tableColumns = "စဉ်, အမည်, အကြောင်းအရာ, ဦးရေ, နှုန်း, သင့်ငွေကျပ်, လ/သ, မှတ်ချက်",
                    tableColumnWidths = "8, 18, 24, 10, 10, 14, 8, 8",
                    tableRowCount = 8,
                    footerNote = "အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်",
                    showSignatureLine = true,
                    signatureLabel = "လက်မှတ် ...................",
                    voucherNumberPosition = "TOP_RIGHT",
                    showQrBarcode = true,
                    isDefault = true
                ),
                // Template 2: Matches User Uploaded Image 2 (Yellow customer receipt with totals box)
                PrintTemplateEntity(
                    id = 2,
                    templateName = "ပုံစံ ၂ (နမူနာ ၂ - တန်ဆာခ ဖြတ်ပိုင်း ပြေစာ Customer Receipt)",
                    paperWidthMm = 80,
                    headerSalutation = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
                    gateName = "ဘုန်းမောင်ခန့်",
                    routeTitle = "မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး",
                    mandalayAddress = "စွယ်တော်ကားကွင်း (I-1)၊ မန္တလေးမြို့",
                    mandalayPhones = "09-987769641, 09-449362369",
                    kalayAddress = "နယ်မြေ(၄)၊ တောင်ဖီလာရပ်ကွက်၊ ကလေးမြို့",
                    kalayPhones = "09-987769642, 09-449362352",
                    hasTableGrid = true,
                    tableColumns = "စဉ်, အမျိုးအမည်, ဦးရေ, နှုန်း, သင့်ငွေ",
                    tableColumnWidths = "10, 36, 14, 16, 24",
                    tableRowCount = 6,
                    footerNote = "အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်",
                    showSignatureLine = true,
                    signatureLabel = "လက်မှတ် ...................",
                    voucherNumberPosition = "TOP_RIGHT",
                    showQrBarcode = true,
                    isDefault = false
                ),
                // Template 3: Matches User Uploaded Image 3 (Slip format with 'ကုန်လက်ခံပြေစာ' badge)
                PrintTemplateEntity(
                    id = 3,
                    templateName = "ပုံစံ ၃ (နမူနာ ၃ - ကုန်လက်ခံပြေစာ Slip Receipt)",
                    paperWidthMm = 80,
                    headerSalutation = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
                    gateName = "ဘုန်းမောင်ခန့်",
                    routeTitle = "မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး",
                    mandalayAddress = "စွယ်တော်ကားကွင်း၊ မန္တလေးမြို့",
                    mandalayPhones = "09-987769641, 09-449362369",
                    kalayAddress = "နယ်မြေ(၄)၊ တောင်ဖီလာရပ်ကွက်၊ ကလေးမြို့",
                    kalayPhones = "09-987769642, 09-449362352",
                    hasTableGrid = false,
                    tableColumns = "စဉ်, အကြောင်းအရာ, သင့်ငွေ",
                    tableColumnWidths = "15, 55, 30",
                    tableRowCount = 4,
                    footerNote = "ကုန်လက်ခံပြေစာ ဖြစ်ပါသည်",
                    showSignatureLine = true,
                    signatureLabel = "လက်မှတ် ...................",
                    voucherNumberPosition = "FREE_SPACE",
                    showQrBarcode = true,
                    isDefault = false
                ),
                // Template 4: 58mm compact thermal POS
                PrintTemplateEntity(
                    id = 4,
                    templateName = "ပုံစံ ၄ (၅၈ မမ အသေးစား Compact 58mm POS)",
                    paperWidthMm = 58,
                    headerSalutation = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
                    gateName = "ဘုန်းမောင်ခန့် (BMK)",
                    routeTitle = "မန္တလေး <=> ကလေး",
                    mandalayAddress = "စွယ်တော်ကားကွင်း",
                    mandalayPhones = "09-987769641",
                    kalayAddress = "တောင်ဖီလာ၊ ကလေးမြို့",
                    kalayPhones = "09-987769642",
                    hasTableGrid = true,
                    tableColumns = "အမည်, ဦးရေ, သင့်ငွေ",
                    tableColumnWidths = "50, 20, 30",
                    tableRowCount = 4,
                    footerNote = "ကျေးဇူးတင်ပါသည်",
                    showSignatureLine = true,
                    signatureLabel = "လက်မှတ်",
                    voucherNumberPosition = "TOP_RIGHT",
                    showQrBarcode = true,
                    isDefault = false
                ),
                // Template 5: 80mm gate intake slip with shuttle & labor breakdown
                PrintTemplateEntity(
                    id = 5,
                    templateName = "ပုံစံ ၅ (၈၀ မမ ဂိတ်လက်ခံဖြတ်ပိုင်း Intake Slip with Shuttle)",
                    paperWidthMm = 80,
                    headerSalutation = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
                    gateName = "ဘုန်းမောင်ခန့် ကုန်စည်ပို့ဆောင်ရေး",
                    routeTitle = "မန္တလေး <=> ကလေး (ဂိတ်လက်ခံဖြတ်ပိုင်း)",
                    mandalayAddress = "စွယ်တော်ကားကွင်း၊ မန္တလေး",
                    mandalayPhones = "09-987769641, 09-449362369",
                    kalayAddress = "နယ်မြေ(၄)၊ တောင်ဖီလာ၊ ကလေး",
                    kalayPhones = "09-987769642, 09-449362352",
                    hasTableGrid = true,
                    tableColumns = "စဉ်, အမျိုးအမည်, ဦးရေ, ပိဿာ, တန်ဆာခ, ခေါက်တို, စုစုပေါင်း",
                    tableColumnWidths = "8, 30, 10, 12, 14, 12, 14",
                    tableRowCount = 6,
                    footerNote = "ပစ္စည်း ပျောက်ဆုံးပျက်စီးပါက ဂိတ်စည်းကမ်းအတိုင်း ဆောင်ရွက်ပေးမည်",
                    showSignatureLine = true,
                    signatureLabel = "လက်ခံသူ လက်မှတ်",
                    voucherNumberPosition = "TOP_RIGHT",
                    showQrBarcode = true,
                    isDefault = false
                ),
                // Template 6: 100mm / 4-inch wide full manifest for 12-wheeler departures
                PrintTemplateEntity(
                    id = 6,
                    templateName = "ပုံစံ ၆ (၁၀၀ မမ / ၄ လက်မ အပြည့်စုံ Manifest 100mm)",
                    paperWidthMm = 100,
                    headerSalutation = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
                    gateName = "ဘုန်းမောင်ခန့် မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး",
                    routeTitle = "၁၂ ဘီး တွဲကား ကုန်စည်ပို့ဆောင်မှု ယာဉ်တင်စာရင်းချုပ်",
                    mandalayAddress = "စွယ်တော်ကားကွင်း (၁-၁)၊ မန္တလေးမြို့",
                    mandalayPhones = "09-987769641, 09-449362369",
                    kalayAddress = "နယ်မြေ(၄)၊ တောင်ဖီလာရပ်ကွက်၊ ကလေးမြို့",
                    kalayPhones = "09-987769642, 09-449362352",
                    hasTableGrid = true,
                    tableColumns = "စဉ်, ပိုင်ရှင်အမည်, ဖုန်းနံပါတ်, ပစ္စည်းအမျိုးအမည်, ဦးရေ, ပိဿာ, နှုန်း, စုစုပေါင်း, လ/သ",
                    tableColumnWidths = "6, 16, 14, 18, 8, 8, 10, 12, 8",
                    tableRowCount = 10,
                    footerNote = "ကားသမားနှင့် ကုန်ချ အလုပ်သမားများ သေချာစွာ စစ်ဆေးလက်ခံပါသည်",
                    showSignatureLine = true,
                    signatureLabel = "ယာဉ်မောင်း လက်မှတ်",
                    voucherNumberPosition = "TOP_RIGHT",
                    showQrBarcode = true,
                    isDefault = false
                ),
                // Template 7: Custom free-text / simplified template
                PrintTemplateEntity(
                    id = 7,
                    templateName = "ပုံစံ ၇ (စိတ်ကြိုက် ပုံစံ - စာသားသီးသန့် ရိုးရှင်းပြေစာ Simple Slip)",
                    paperWidthMm = 80,
                    headerSalutation = "ဗုဒ္ဓံ ဓမ္မံ သံဃံ",
                    gateName = "ဘုန်းမောင်ခန့်",
                    routeTitle = "မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး",
                    mandalayAddress = "စွယ်တော်ကားကွင်း၊ မန္တလေး",
                    mandalayPhones = "09-987769641, 09-449362369",
                    kalayAddress = "တောင်ဖီလာ၊ ကလေးမြို့",
                    kalayPhones = "09-987769642, 09-449362352",
                    hasTableGrid = false,
                    tableColumns = "အကြောင်းအရာ, အချက်အလက်",
                    tableColumnWidths = "40, 60",
                    tableRowCount = 4,
                    footerNote = "အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်",
                    showSignatureLine = true,
                    signatureLabel = "လက်မှတ် ...................",
                    voucherNumberPosition = "FREE_SPACE",
                    showQrBarcode = true,
                    isDefault = false
                )
            )
            templateDao.insertTemplates(defaultTemplates)

            // Seed Sample Historical Vouchers matching user samples (0100, 0092, 1195)
            voucherDao.insertVoucher(
                PrintedVoucherEntity(
                    voucherNumber = "0100",
                    issueDate = "2026-10-25",
                    templateId = 1,
                    templateName = "ပုံစံ ၁ (နမူနာ ၁ - ၈ ကော်လံ ယာဉ်တင်စာရင်း Waybill)",
                    paperWidthMm = 80,
                    cargoId = 1,
                    truckId = truckId1,
                    customerName = "ဒေါ်အေးအေး",
                    customerPhone = "09450012345",
                    customerAddress = "၃၈ လမ်း၊ ၈၄ လမ်း၊ မန္တလေး",
                    carNumber = "MDY 7D-1212 (12-Wheeler Box)",
                    driverName = "ဦးကိုကို (Driver U Ko Ko)",
                    itemDescription = "Toyota Engine Cylinder & Brake Pads (၃ ပုံး)",
                    quantity = 3,
                    weightViss = 50.0,
                    freightFee = 60000.0,
                    laborFee = 2500.0,
                    totalAmount = 70500.0,
                    paymentStatus = "PAID",
                    remarks = "ကလေးမြို့ ဆိုင်အရောက် ပို့ရန်"
                )
            )

            voucherDao.insertVoucher(
                PrintedVoucherEntity(
                    voucherNumber = "0092",
                    issueDate = "2026-10-26",
                    templateId = 2,
                    templateName = "ပုံစံ ၂ (နမူနာ ၂ - တန်ဆာခ ဖြတ်ပိုင်း ပြေစာ Customer Receipt)",
                    paperWidthMm = 80,
                    cargoId = 2,
                    truckId = truckId2,
                    customerName = "ဒေါ်စန်းစန်း (စန်းစန်း ကုန်စုံဆိုင်)",
                    customerPhone = "09780099887",
                    customerAddress = "ညောင်ပင်သာ၊ ကလေးမြို့",
                    carNumber = "MDY 9B-8833 (12-Wheeler)",
                    driverName = "ဦးကျော်စန်း",
                    itemDescription = "အထည်အလိပ်နှင့် လူသုံးကုန် ပစ္စည်းများ (၅ ဖာ)",
                    quantity = 5,
                    weightViss = 80.0,
                    freightFee = 96000.0,
                    laborFee = 4000.0,
                    totalAmount = 100000.0,
                    paymentStatus = "COLLECT",
                    remarks = "တန်ဆာခ ဖြတ်ပိုင်း ပြေစာ"
                )
            )

            voucherDao.insertVoucher(
                PrintedVoucherEntity(
                    voucherNumber = "1195",
                    issueDate = "2026-10-26",
                    templateId = 3,
                    templateName = "ပုံစံ ၃ (နမူနာ ၃ - ကုန်လက်ခံပြေစာ Slip Receipt)",
                    paperWidthMm = 80,
                    cargoId = 1,
                    truckId = truckId1,
                    customerName = "ဦးကျော်စွာ",
                    customerPhone = "09250098765",
                    customerAddress = "ရွှေဆိုင်၊ တဟာန်ရပ်ကွက်၊ ကလေးမြို့",
                    carNumber = "MDY 7D-1212",
                    driverName = "ဦးကိုကို",
                    itemDescription = "စက်ပစ္စည်း အပိုပစ္စည်း",
                    quantity = 3,
                    weightViss = 50.0,
                    freightFee = 60000.0,
                    laborFee = 2500.0,
                    totalAmount = 62500.0,
                    paymentStatus = "PAID",
                    remarks = "ကုန်လက်ခံပြေစာ အတို"
                )
            )
        }
    }
}

