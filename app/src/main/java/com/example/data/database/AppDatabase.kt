package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CargoDao
import com.example.data.dao.CompensationDao
import com.example.data.dao.TruckDao
import com.example.data.dao.WorkerLaborDao
import com.example.data.model.CargoEntity
import com.example.data.model.CompensationEntity
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
        WorkerLaborEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cargoDao(): CargoDao
    abstract fun truckDao(): TruckDao
    abstract fun compensationDao(): CompensationDao
    abstract fun workerLaborDao(): WorkerLaborDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bmk_express_database_v3"
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
        }
    }
}
