package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupStats(
    val cargoCount: Int = 0,
    val truckCount: Int = 0,
    val templateCount: Int = 0,
    val voucherCount: Int = 0,
    val claimCount: Int = 0,
    val laborCount: Int = 0,
    val backupDate: String = ""
)

object BackupManager {

    suspend fun generateBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val cargos = db.cargoDao().getAllCargosList()
        val trucks = db.truckDao().getAllTrucksList()
        val templates = db.printTemplateDao().getAllTemplatesList()
        val vouchers = db.printedVoucherDao().getAllVouchersList()
        val claims = db.compensationDao().getAllClaimsList()
        val labor = db.workerLaborDao().getAllLaborRecordsList()

        val root = JSONObject()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        root.put("appName", "BMK Express Car Gate")
        root.put("version", 5)
        root.put("exportedAt", now)
        root.put("timestamp", System.currentTimeMillis())

        // 1. Cargos
        val cargosArray = JSONArray()
        for (c in cargos) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("trackingNumber", c.trackingNumber)
            obj.put("cargoDensityType", c.cargoDensityType)
            obj.put("cargoName", c.cargoName)
            obj.put("unitWeight", c.unitWeight)
            obj.put("weightUnit", c.weightUnit)
            obj.put("quantity", c.quantity)
            obj.put("itemDescription", c.itemDescription)
            obj.put("weightViss", c.weightViss)
            obj.put("weightKg", c.weightKg)
            obj.put("senderName", c.senderName)
            obj.put("senderPhone", c.senderPhone)
            obj.put("senderAddress", c.senderAddress)
            obj.put("isShuttlePickup", c.isShuttlePickup)
            obj.put("shuttleFee", c.shuttleFee)
            obj.put("shuttleLaborFee", c.shuttleLaborFee)
            obj.put("receiverName", c.receiverName)
            obj.put("receiverPhone", c.receiverPhone)
            obj.put("receiverShopAddress", c.receiverShopAddress)
            obj.put("cargoType", c.cargoType)
            obj.put("freightRatePerViss", c.freightRatePerViss)
            obj.put("freightFee", c.freightFee)
            obj.put("mdyLoadingRatePerViss", c.mdyLoadingRatePerViss)
            obj.put("mdyLoadingLaborFee", c.mdyLoadingLaborFee)
            obj.put("klyUnloadingRatePerViss", c.klyUnloadingRatePerViss)
            obj.put("klyUnloadingLaborFee", c.klyUnloadingLaborFee)
            obj.put("totalAmount", c.totalAmount)
            obj.put("paymentStatus", c.paymentStatus)
            obj.put("status", c.status)
            obj.put("truckId", c.truckId ?: JSONObject.NULL)
            obj.put("truckNumber", c.truckNumber)
            obj.put("driverName", c.driverName)
            obj.put("driverPhone", c.driverPhone)
            obj.put("sequenceOrder", c.sequenceOrder)
            obj.put("createdTimestamp", c.createdTimestamp)
            obj.put("remarks", c.remarks)
            cargosArray.put(obj)
        }
        root.put("cargos", cargosArray)

        // 2. Trucks
        val trucksArray = JSONArray()
        for (t in trucks) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("truckNumber", t.truckNumber)
            obj.put("ownerName", t.ownerName)
            obj.put("driverName", t.driverName)
            obj.put("driverPhone", t.driverPhone)
            obj.put("truckType", t.truckType)
            obj.put("mandalayToKalayFreightPrice", t.mandalayToKalayFreightPrice)
            obj.put("advancePayment", t.advancePayment)
            obj.put("dispatchDate", t.dispatchDate)
            obj.put("status", t.status)
            obj.put("totalVissLoaded", t.totalVissLoaded)
            obj.put("totalCargosCount", t.totalCargosCount)
            obj.put("notes", t.notes)
            trucksArray.put(obj)
        }
        root.put("trucks", trucksArray)

        // 3. Templates
        val templatesArray = JSONArray()
        for (tmpl in templates) {
            val obj = JSONObject()
            obj.put("id", tmpl.id)
            obj.put("templateName", tmpl.templateName)
            obj.put("paperWidthMm", tmpl.paperWidthMm)
            obj.put("headerSalutation", tmpl.headerSalutation)
            obj.put("gateName", tmpl.gateName)
            obj.put("routeTitle", tmpl.routeTitle)
            obj.put("mandalayAddress", tmpl.mandalayAddress)
            obj.put("mandalayPhones", tmpl.mandalayPhones)
            obj.put("kalayAddress", tmpl.kalayAddress)
            obj.put("kalayPhones", tmpl.kalayPhones)
            obj.put("hasTableGrid", tmpl.hasTableGrid)
            obj.put("tableColumns", tmpl.tableColumns)
            obj.put("tableColumnWidths", tmpl.tableColumnWidths)
            obj.put("tableRowCount", tmpl.tableRowCount)
            obj.put("footerNote", tmpl.footerNote)
            obj.put("showSignatureLine", tmpl.showSignatureLine)
            obj.put("signatureLabel", tmpl.signatureLabel)
            obj.put("voucherNumberPosition", tmpl.voucherNumberPosition)
            obj.put("showQrBarcode", tmpl.showQrBarcode)
            obj.put("isDefault", tmpl.isDefault)
            templatesArray.put(obj)
        }
        root.put("printTemplates", templatesArray)

        // 4. Vouchers
        val vouchersArray = JSONArray()
        for (v in vouchers) {
            val obj = JSONObject()
            obj.put("id", v.id)
            obj.put("voucherNumber", v.voucherNumber)
            obj.put("issueDate", v.issueDate)
            obj.put("createdTimestamp", v.createdTimestamp)
            obj.put("templateId", v.templateId)
            obj.put("templateName", v.templateName)
            obj.put("paperWidthMm", v.paperWidthMm)
            obj.put("cargoId", v.cargoId ?: JSONObject.NULL)
            obj.put("truckId", v.truckId ?: JSONObject.NULL)
            obj.put("customerName", v.customerName)
            obj.put("customerPhone", v.customerPhone)
            obj.put("customerAddress", v.customerAddress)
            obj.put("carNumber", v.carNumber)
            obj.put("driverName", v.driverName)
            obj.put("itemDescription", v.itemDescription)
            obj.put("quantity", v.quantity)
            obj.put("weightViss", v.weightViss)
            obj.put("freightFee", v.freightFee)
            obj.put("laborFee", v.laborFee)
            obj.put("totalAmount", v.totalAmount)
            obj.put("paymentStatus", v.paymentStatus)
            obj.put("tableRowsJson", v.tableRowsJson)
            obj.put("remarks", v.remarks)
            vouchersArray.put(obj)
        }
        root.put("printedVouchers", vouchersArray)

        // 5. Compensation Claims
        val claimsArray = JSONArray()
        for (cl in claims) {
            val obj = JSONObject()
            obj.put("id", cl.id)
            obj.put("cargoId", cl.cargoId)
            obj.put("trackingNumber", cl.trackingNumber)
            obj.put("customerName", cl.customerName)
            obj.put("customerPhone", cl.customerPhone)
            obj.put("damageDescription", cl.damageDescription)
            obj.put("claimAmount", cl.claimAmount)
            obj.put("paidAmount", cl.paidAmount)
            obj.put("status", cl.status)
            obj.put("date", cl.date)
            obj.put("notes", cl.notes)
            claimsArray.put(obj)
        }
        root.put("compensationClaims", claimsArray)

        // 6. Labor
        val laborArray = JSONArray()
        for (l in labor) {
            val obj = JSONObject()
            obj.put("id", l.id)
            obj.put("gateBranch", l.gateBranch)
            obj.put("workerName", l.workerName)
            obj.put("date", l.date)
            obj.put("vissHandled", l.vissHandled)
            obj.put("laborRatePerViss", l.laborRatePerViss)
            obj.put("shuttleLaborAmount", l.shuttleLaborAmount)
            obj.put("totalLaborPaid", l.totalLaborPaid)
            obj.put("notes", l.notes)
            laborArray.put(obj)
        }
        root.put("workerLaborRecords", laborArray)

        root.toString(2)
    }

    suspend fun exportBackupFile(context: Context): Uri = withContext(Dispatchers.IO) {
        val jsonStr = generateBackupJson(context)
        val dateCompact = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "bmk_express_backup_$dateCompact.json"
        val cacheFile = File(context.cacheDir, fileName)
        cacheFile.writeText(jsonStr)

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
    }

    fun shareOrSaveToDrive(context: Context, fileUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, "BMK Express Backup Data")
            putExtra(Intent.EXTRA_TEXT, "ဘုန်းမောင်ခန့် ကုန်စည်ပို့ဆောင်ရေး အချက်အလက် Backup ဖိုင်")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Google Drive သို့မဟုတ် အခြားနေရာသို့ Backup သိမ်းမည်"))
    }

    suspend fun restoreFromJson(context: Context, jsonString: String): BackupStats = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)
        val db = AppDatabase.getDatabase(context)

        // 1. Cargos
        val cargosList = mutableListOf<CargoEntity>()
        if (root.has("cargos")) {
            val arr = root.getJSONArray("cargos")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                cargosList.add(
                    CargoEntity(
                        id = o.optLong("id", 0),
                        trackingNumber = o.optString("trackingNumber", ""),
                        cargoDensityType = o.optString("cargoDensityType", "HEAVY"),
                        cargoName = o.optString("cargoName", ""),
                        unitWeight = o.optDouble("unitWeight", 0.0),
                        weightUnit = o.optString("weightUnit", "ပိဿာ"),
                        quantity = o.optInt("quantity", 1),
                        itemDescription = o.optString("itemDescription", ""),
                        weightViss = o.optDouble("weightViss", 0.0),
                        weightKg = o.optDouble("weightKg", 0.0),
                        senderName = o.optString("senderName", ""),
                        senderPhone = o.optString("senderPhone", ""),
                        senderAddress = o.optString("senderAddress", ""),
                        isShuttlePickup = o.optBoolean("isShuttlePickup", false),
                        shuttleFee = o.optDouble("shuttleFee", 0.0),
                        shuttleLaborFee = o.optDouble("shuttleLaborFee", 0.0),
                        receiverName = o.optString("receiverName", ""),
                        receiverPhone = o.optString("receiverPhone", ""),
                        receiverShopAddress = o.optString("receiverShopAddress", ""),
                        cargoType = o.optString("cargoType", "General Cargo"),
                        freightRatePerViss = o.optDouble("freightRatePerViss", 1200.0),
                        freightFee = o.optDouble("freightFee", 0.0),
                        mdyLoadingRatePerViss = o.optDouble("mdyLoadingRatePerViss", 25.0),
                        mdyLoadingLaborFee = o.optDouble("mdyLoadingLaborFee", 0.0),
                        klyUnloadingRatePerViss = o.optDouble("klyUnloadingRatePerViss", 25.0),
                        klyUnloadingLaborFee = o.optDouble("klyUnloadingLaborFee", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        paymentStatus = o.optString("paymentStatus", "PAID"),
                        status = o.optString("status", "MANDALAY_RECEIVED"),
                        truckId = if (o.isNull("truckId")) null else o.optLong("truckId"),
                        truckNumber = o.optString("truckNumber", ""),
                        driverName = o.optString("driverName", ""),
                        driverPhone = o.optString("driverPhone", ""),
                        sequenceOrder = o.optInt("sequenceOrder", 0),
                        createdTimestamp = o.optLong("createdTimestamp", System.currentTimeMillis()),
                        remarks = o.optString("remarks", "")
                    )
                )
            }
        }

        // 2. Trucks
        val trucksList = mutableListOf<TruckEntity>()
        if (root.has("trucks")) {
            val arr = root.getJSONArray("trucks")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                trucksList.add(
                    TruckEntity(
                        id = o.optLong("id", 0),
                        truckNumber = o.optString("truckNumber", ""),
                        ownerName = o.optString("ownerName", ""),
                        driverName = o.optString("driverName", ""),
                        driverPhone = o.optString("driverPhone", ""),
                        truckType = o.optString("truckType", "12-Wheeler Box Truck"),
                        mandalayToKalayFreightPrice = o.optDouble("mandalayToKalayFreightPrice", 1500000.0),
                        advancePayment = o.optDouble("advancePayment", 0.0),
                        dispatchDate = o.optString("dispatchDate", ""),
                        status = o.optString("status", "EMPTY"),
                        totalVissLoaded = o.optDouble("totalVissLoaded", 0.0),
                        totalCargosCount = o.optInt("totalCargosCount", 0),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        // 3. Templates
        val templatesList = mutableListOf<PrintTemplateEntity>()
        if (root.has("printTemplates")) {
            val arr = root.getJSONArray("printTemplates")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                templatesList.add(
                    PrintTemplateEntity(
                        id = o.optInt("id", i + 1),
                        templateName = o.optString("templateName", "ပုံစံ ${i + 1}"),
                        paperWidthMm = o.optInt("paperWidthMm", 80),
                        headerSalutation = o.optString("headerSalutation", "ဗုဒ္ဓံ ဓမ္မံ သံဃံ"),
                        gateName = o.optString("gateName", "ဘုန်းမောင်ခန့်"),
                        routeTitle = o.optString("routeTitle", "မန္တလေး <=> ကလေး ကုန်စည်ပို့ဆောင်ရေး"),
                        mandalayAddress = o.optString("mandalayAddress", "စွယ်တော်ကားကွင်း၊ မန္တလေးမြို့"),
                        mandalayPhones = o.optString("mandalayPhones", "09-987769641, 09-449362369"),
                        kalayAddress = o.optString("kalayAddress", "နယ်မြေ(၄)၊ တောင်ဖီလာရပ်ကွက်၊ ကလေးမြို့"),
                        kalayPhones = o.optString("kalayPhones", "09-987769642, 09-449362352"),
                        hasTableGrid = o.optBoolean("hasTableGrid", true),
                        tableColumns = o.optString("tableColumns", "စဉ်, အမည်, အကြောင်းအရာ, ဦးရေ, နှုန်း, သင့်ငွေကျပ်, လ/သ, မှတ်ချက်"),
                        tableColumnWidths = o.optString("tableColumnWidths", "8, 18, 24, 10, 10, 14, 8, 8"),
                        tableRowCount = o.optInt("tableRowCount", 8),
                        footerNote = o.optString("footerNote", "အားပေးမှုကို အထူးကျေးဇူးတင်ရှိပါသည်"),
                        showSignatureLine = o.optBoolean("showSignatureLine", true),
                        signatureLabel = o.optString("signatureLabel", "လက်မှတ်"),
                        voucherNumberPosition = o.optString("voucherNumberPosition", "TOP_RIGHT"),
                        showQrBarcode = o.optBoolean("showQrBarcode", true),
                        isDefault = o.optBoolean("isDefault", false)
                    )
                )
            }
        }

        // 4. Vouchers
        val vouchersList = mutableListOf<PrintedVoucherEntity>()
        if (root.has("printedVouchers")) {
            val arr = root.getJSONArray("printedVouchers")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                vouchersList.add(
                    PrintedVoucherEntity(
                        id = o.optLong("id", 0),
                        voucherNumber = o.optString("voucherNumber", ""),
                        issueDate = o.optString("issueDate", ""),
                        createdTimestamp = o.optLong("createdTimestamp", System.currentTimeMillis()),
                        templateId = o.optInt("templateId", 1),
                        templateName = o.optString("templateName", ""),
                        paperWidthMm = o.optInt("paperWidthMm", 80),
                        cargoId = if (o.isNull("cargoId")) null else o.optLong("cargoId"),
                        truckId = if (o.isNull("truckId")) null else o.optLong("truckId"),
                        customerName = o.optString("customerName", ""),
                        customerPhone = o.optString("customerPhone", ""),
                        customerAddress = o.optString("customerAddress", ""),
                        carNumber = o.optString("carNumber", ""),
                        driverName = o.optString("driverName", ""),
                        itemDescription = o.optString("itemDescription", ""),
                        quantity = o.optInt("quantity", 1),
                        weightViss = o.optDouble("weightViss", 0.0),
                        freightFee = o.optDouble("freightFee", 0.0),
                        laborFee = o.optDouble("laborFee", 0.0),
                        totalAmount = o.optDouble("totalAmount", 0.0),
                        paymentStatus = o.optString("paymentStatus", "PAID"),
                        tableRowsJson = o.optString("tableRowsJson", ""),
                        remarks = o.optString("remarks", "")
                    )
                )
            }
        }

        // 5. Claims
        val claimsList = mutableListOf<CompensationEntity>()
        if (root.has("compensationClaims")) {
            val arr = root.getJSONArray("compensationClaims")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                claimsList.add(
                    CompensationEntity(
                        id = o.optLong("id", 0),
                        cargoId = o.optLong("cargoId", 0),
                        trackingNumber = o.optString("trackingNumber", ""),
                        customerName = o.optString("customerName", ""),
                        customerPhone = o.optString("customerPhone", ""),
                        damageDescription = o.optString("damageDescription", ""),
                        claimAmount = o.optDouble("claimAmount", 0.0),
                        paidAmount = o.optDouble("paidAmount", 0.0),
                        status = o.optString("status", "PENDING"),
                        date = o.optString("date", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        // 6. Labor
        val laborList = mutableListOf<WorkerLaborEntity>()
        if (root.has("workerLaborRecords")) {
            val arr = root.getJSONArray("workerLaborRecords")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                laborList.add(
                    WorkerLaborEntity(
                        id = o.optLong("id", 0),
                        gateBranch = o.optString("gateBranch", "MANDALAY"),
                        workerName = o.optString("workerName", ""),
                        date = o.optString("date", ""),
                        vissHandled = o.optDouble("vissHandled", 0.0),
                        laborRatePerViss = o.optDouble("laborRatePerViss", 25.0),
                        shuttleLaborAmount = o.optDouble("shuttleLaborAmount", 0.0),
                        totalLaborPaid = o.optDouble("totalLaborPaid", 0.0),
                        notes = o.optString("notes", "")
                    )
                )
            }
        }

        // Perform Room insertions
        if (cargosList.isNotEmpty()) db.cargoDao().insertCargos(cargosList)
        if (trucksList.isNotEmpty()) db.truckDao().insertTrucks(trucksList)
        if (templatesList.isNotEmpty()) db.printTemplateDao().insertTemplates(templatesList)
        if (vouchersList.isNotEmpty()) db.printedVoucherDao().insertVouchers(vouchersList)
        if (claimsList.isNotEmpty()) db.compensationDao().insertClaims(claimsList)
        if (laborList.isNotEmpty()) db.workerLaborDao().insertLaborRecords(laborList)

        BackupStats(
            cargoCount = cargosList.size,
            truckCount = trucksList.size,
            templateCount = templatesList.size,
            voucherCount = vouchersList.size,
            claimCount = claimsList.size,
            laborCount = laborList.size,
            backupDate = root.optString("exportedAt", "Recent")
        )
    }

    // Google Cloud / Firebase Firestore Sync
    suspend fun syncBackupToCloud(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
            val uid = user?.uid ?: "anonymous_user"
            val jsonStr = generateBackupJson(context)
            val db = try {
                FirebaseFirestore.getInstance()
            } catch (e: Throwable) {
                return@withContext Result.failure(Exception("Cloud Sync Service မရရှိနိုင်ပါ: ${e.message}"))
            }

            val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val data = hashMapOf(
                "backupJson" to jsonStr,
                "userEmail" to (user?.email ?: "Unknown"),
                "userName" to (user?.displayName ?: "BMK Operator"),
                "updatedAt" to now,
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("user_backups").document(uid).set(data, SetOptions.merge()).await()
            Result.success("Google Cloud သို့ အောင်မြင်စွာ Backup ပြုလုပ်ပြီးပါပြီ ($now)")
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Cloud Sync အမှားဖြစ်ပေါ်ပါသည်"))
        }
    }

    suspend fun restoreBackupFromCloud(context: Context): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val user = try { FirebaseAuth.getInstance().currentUser } catch (e: Throwable) { null }
            val uid = user?.uid ?: "anonymous_user"
            val db = try {
                FirebaseFirestore.getInstance()
            } catch (e: Throwable) {
                return@withContext Result.failure(Exception("Cloud Service မရရှိနိုင်ပါ: ${e.message}"))
            }
            val doc = db.collection("user_backups").document(uid).get().await()

            if (!doc.exists()) {
                return@withContext Result.failure(Exception("Cloud ပေါ်တွင် ယခင် သိမ်းထားသော Backup မတွေ့ရှိပါ။"))
            }

            val jsonStr = doc.getString("backupJson") ?: return@withContext Result.failure(Exception("Backup ဒေတာ မမှန်ကန်ပါ။"))
            val stats = restoreFromJson(context, jsonStr)
            Result.success(stats)
        } catch (e: Throwable) {
            Result.failure(Exception(e.message ?: "Cloud Restore အမှားဖြစ်ပေါ်ပါသည်"))
        }
    }
}
