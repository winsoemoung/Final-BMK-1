package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.CargoEntity
import com.example.data.model.CompensationEntity
import com.example.data.model.PrintTemplateEntity
import com.example.data.model.PrintedVoucherEntity
import com.example.data.model.TruckEntity
import com.example.data.model.WorkerLaborEntity
import com.example.data.repository.BmkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class AppSettingsState(
    val language: String = "my", // "my" for Burmese, "en" for English
    val freightRatePerViss: Double = 1200.0, // ၁ ပိဿာ တန်ဆာခ (default 1200 Ks)
    val mdyLoadingRatePerViss: Double = 25.0, // ၁၂ ဘီး တင်ခ (default 25 Ks)
    val klyUnloadingRatePerViss: Double = 25.0 // ၁၂ ဘီး ချခ (default 25 Ks)
)

class BmkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BmkRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BmkRepository(
            database.cargoDao(),
            database.truckDao(),
            database.compensationDao(),
            database.workerLaborDao(),
            database.printTemplateDao(),
            database.printedVoucherDao()
        )
    }

    // App Settings State (Language & Dynamic Rates)
    private val _settingsState = MutableStateFlow(AppSettingsState())
    val settingsState: StateFlow<AppSettingsState> = _settingsState.asStateFlow()

    fun updateSettings(
        language: String = _settingsState.value.language,
        freightRatePerViss: Double = _settingsState.value.freightRatePerViss,
        mdyLoadingRatePerViss: Double = _settingsState.value.mdyLoadingRatePerViss,
        klyUnloadingRatePerViss: Double = _settingsState.value.klyUnloadingRatePerViss
    ) {
        _settingsState.value = AppSettingsState(
            language = language,
            freightRatePerViss = freightRatePerViss,
            mdyLoadingRatePerViss = mdyLoadingRatePerViss,
            klyUnloadingRatePerViss = klyUnloadingRatePerViss
        )
    }

    val allCargo: StateFlow<List<CargoEntity>> = repository.allCargo.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cargoCount: StateFlow<Int> = repository.cargoCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val totalRevenue: StateFlow<Double?> = repository.totalRevenue.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<CargoEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.allCargo
        } else {
            repository.searchCargo(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _truckSearchQuery = MutableStateFlow("")
    val truckSearchQuery: StateFlow<String> = _truckSearchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val truckSearchResults: StateFlow<List<TruckEntity>> = _truckSearchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.allTrucks
        } else {
            repository.searchTrucks(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedCargo = MutableStateFlow<CargoEntity?>(null)
    val selectedCargo: StateFlow<CargoEntity?> = _selectedCargo.asStateFlow()

    val allTrucks: StateFlow<List<TruckEntity>> = repository.allTrucks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val compensationClaims: StateFlow<List<CompensationEntity>> = repository.allClaims.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val workerLaborRecords: StateFlow<List<WorkerLaborEntity>> = repository.allLaborRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTruckSearchQuery(query: String) {
        _truckSearchQuery.value = query
    }

    fun selectCargo(cargo: CargoEntity?) {
        _selectedCargo.value = cargo
    }

    fun findCargoByTracking(trackingNumber: String) {
        viewModelScope.launch {
            val cargo = repository.getCargoByTracking(trackingNumber.trim())
            _selectedCargo.value = cargo
        }
    }

    fun convertVissToKg(viss: Double): Double = viss * 1.63293

    fun convertKgToViss(kg: Double): Double = kg / 1.63293

    fun generateTrackingNumber(): String {
        val randomNum = Random.nextInt(10000, 99999)
        return "BMK-MDY-KLY-$randomNum"
    }

    // Mandalay Tab Action: Register Cargo Intake matching 10 requested items
    fun registerCargoIntake(
        cargoDensityType: String, // 1. "HEAVY" (ကုန်မာ) or "BULK" (ကုန်ပွ)
        cargoName: String, // 2. ကုန်ပစ္စည်း အမည်
        unitWeight: Double, // 3. ကုန်ပစ္စည်း တစ်ခု အလေးချိန်
        weightUnit: String, // 3. ယူနစ် (ခု, အိတ်, ဖာ, ပုံး, ပိဿာ, ကီလို)
        quantity: Int, // 4. Customer လာအပ်သော အရေအတွက်
        itemDescription: String, // 5. ကုန်ပစ္စည်း အသေးစိတ် ဖော်ပြချက်
        senderName: String, // 6. ပို့သူ အမည်
        senderPhone: String, // 6. ပို့သူ ဖုန်း
        senderAddress: String, // 6. ပို့သူ လိပ်စာ
        isShuttlePickup: Boolean, // 7. ခေါက်တို သွားတိုက်ရန် အမှန်ခြစ်
        shuttleFee: Double,
        shuttleLaborFee: Double,
        receiverName: String, // 8. Kalaymyo လက်ခံသူ အမည်
        receiverPhone: String, // 8. Kalaymyo လက်ခံသူ ဖုန်း
        receiverShopAddress: String, // 8. Kalaymyo လက်ခံသူ လိပ်စာ
        assignedTruck: TruckEntity?, // 9. Car Tab မှ ကား ရွေးချယ်မှု
        paymentStatus: String,
        remarks: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val trackingNo = generateTrackingNumber()

            val totalWeightViss = when (weightUnit) {
                "ကီလို" -> convertKgToViss(unitWeight * quantity)
                "ပိဿာ" -> unitWeight * quantity
                else -> unitWeight * quantity
            }
            val totalWeightKg = convertVissToKg(totalWeightViss)

            val currentRates = _settingsState.value
            val freightFee = totalWeightViss * currentRates.freightRatePerViss
            val mdyLoadingLaborFee = totalWeightViss * currentRates.mdyLoadingRatePerViss
            val klyUnloadingLaborFee = totalWeightViss * currentRates.klyUnloadingRatePerViss

            val totalAmount = freightFee + mdyLoadingLaborFee + klyUnloadingLaborFee + shuttleFee + shuttleLaborFee

            val cargo = CargoEntity(
                trackingNumber = trackingNo,
                cargoDensityType = cargoDensityType,
                cargoName = cargoName,
                unitWeight = unitWeight,
                weightUnit = weightUnit,
                quantity = quantity,
                itemDescription = itemDescription,
                weightViss = totalWeightViss,
                weightKg = totalWeightKg,
                senderName = senderName,
                senderPhone = senderPhone,
                senderAddress = senderAddress,
                isShuttlePickup = isShuttlePickup,
                shuttleFee = shuttleFee,
                shuttleLaborFee = shuttleLaborFee,
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                receiverShopAddress = receiverShopAddress,
                cargoType = if (cargoDensityType == "HEAVY") "ကုန်မာ (Solid Cargo)" else "ကုန်ပွ (Bulk Cargo)",
                freightRatePerViss = currentRates.freightRatePerViss,
                freightFee = freightFee,
                mdyLoadingRatePerViss = currentRates.mdyLoadingRatePerViss,
                mdyLoadingLaborFee = mdyLoadingLaborFee,
                klyUnloadingRatePerViss = currentRates.klyUnloadingRatePerViss,
                klyUnloadingLaborFee = klyUnloadingLaborFee,
                totalAmount = totalAmount,
                paymentStatus = paymentStatus,
                status = if (assignedTruck != null) "LOADING_12WHEELER" else "MANDALAY_RECEIVED",
                truckId = assignedTruck?.id,
                truckNumber = assignedTruck?.truckNumber ?: "",
                driverName = assignedTruck?.driverName ?: "",
                driverPhone = assignedTruck?.driverPhone ?: "",
                remarks = remarks
            )

            repository.registerCargo(cargo)
            onSuccess(trackingNo)
        }
    }

    // Bulk Assign Selected Cargos to a Truck
    fun assignSelectedCargosToTruck(
        cargoIds: List<Long>,
        truck: TruckEntity,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.assignTruckToCargoes(
                cargoIds = cargoIds,
                truckId = truck.id,
                truckNumber = truck.truckNumber,
                driverName = truck.driverName,
                driverPhone = truck.driverPhone
            )
            onSuccess()
        }
    }

    // Reorder Cargo Items on Truck
    fun swapCargoSequenceOnTruck(cargosList: List<CargoEntity>, fromIndex: Int, toIndex: Int) {
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= cargosList.size || toIndex >= cargosList.size) return
        viewModelScope.launch {
            val mutable = cargosList.toMutableList()
            val item1 = mutable[fromIndex]
            val item2 = mutable[toIndex]

            val updated1 = item1.copy(sequenceOrder = toIndex + 1)
            val updated2 = item2.copy(sequenceOrder = fromIndex + 1)

            mutable[fromIndex] = updated2
            mutable[toIndex] = updated1

            // Assign updated sequence numbers to all items
            val reorderedList = mutable.mapIndexed { index, cargo ->
                cargo.copy(sequenceOrder = index + 1)
            }
            repository.updateCargoList(reorderedList)
        }
    }

    // Update Cargo Status
    fun updateCargoStatus(cargoId: Long, newStatus: String) {
        viewModelScope.launch {
            repository.updateCargoStatus(cargoId, newStatus)
            _selectedCargo.value?.let { current ->
                if (current.id == cargoId) {
                    _selectedCargo.value = current.copy(status = newStatus)
                }
            }
        }
    }

    // Update Door-to-Door Delivery Status & Payment Promised Date at Kalaymyo
    fun updateCargoDeliveryAndPayment(
        cargoId: Long,
        deliveryStatus: String,
        paymentStatus: String,
        promisedDate: String
    ) {
        viewModelScope.launch {
            repository.updateDeliveryAndPaymentStatus(
                id = cargoId,
                deliveryStatus = deliveryStatus,
                paymentStatus = paymentStatus,
                promisedDate = promisedDate
            )
            _selectedCargo.value?.let { current ->
                if (current.id == cargoId) {
                    _selectedCargo.value = current.copy(
                        deliveryStatus = deliveryStatus,
                        paymentStatus = paymentStatus,
                        promisedPaymentDate = promisedDate
                    )
                }
            }
        }
    }

    // Auto-archive truck to COMPLETED if all cargos are delivered & settled
    fun autoArchiveTruckIfCompleted(truckId: Long, allTruckCargos: List<CargoEntity>) {
        if (allTruckCargos.isEmpty()) return
        val allDelivered = allTruckCargos.all {
            it.deliveryStatus == "UNLOADED_PAID" || it.deliveryStatus == "UNLOADED_CREDIT" || it.deliveryStatus == "DAMAGED"
        }
        if (allDelivered) {
            viewModelScope.launch {
                repository.updateTruckStatus(truckId, "COMPLETED")
            }
        }
    }

    // Car Tab Action: Register 12-Wheeler Truck
    fun registerTruck(
        truckNumber: String,
        ownerName: String,
        driverName: String,
        driverPhone: String,
        mandalayToKalayFreightPrice: Double,
        advancePayment: Double,
        dispatchDate: String,
        notes: String,
        onSuccess: (TruckEntity) -> Unit
    ) {
        viewModelScope.launch {
            val truck = TruckEntity(
                truckNumber = truckNumber,
                ownerName = ownerName,
                driverName = driverName,
                driverPhone = driverPhone,
                truckType = "12-Wheeler Box Truck",
                mandalayToKalayFreightPrice = mandalayToKalayFreightPrice,
                advancePayment = advancePayment,
                dispatchDate = dispatchDate,
                status = "EMPTY",
                notes = notes
            )
            val insertedId = repository.registerTruck(truck)
            onSuccess(truck.copy(id = insertedId))
        }
    }

    fun updateTruckStatus(truckId: Long, status: String) {
        viewModelScope.launch {
            repository.updateTruckStatus(truckId, status)
        }
    }

    // Kalay Tab Action: Submit Damage / Compensation Claim
    fun submitCompensationClaim(
        cargoId: Long,
        trackingNumber: String,
        customerName: String,
        customerPhone: String,
        damageDescription: String,
        claimAmount: Double,
        paidAmount: Double,
        date: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val claim = CompensationEntity(
                cargoId = cargoId,
                trackingNumber = trackingNumber,
                customerName = customerName,
                customerPhone = customerPhone,
                damageDescription = damageDescription,
                claimAmount = claimAmount,
                paidAmount = paidAmount,
                status = if (paidAmount >= claimAmount) "PAID" else "PENDING",
                date = date,
                notes = notes
            )
            repository.submitClaim(claim)
            onSuccess()
        }
    }

    fun updateClaimStatus(claimId: Long, status: String, paidAmount: Double) {
        viewModelScope.launch {
            repository.updateClaimStatus(claimId, status, paidAmount)
        }
    }

    // Worker Tab Action: Add Worker Labor Log
    fun addWorkerLabor(
        gateBranch: String,
        workerName: String,
        date: String,
        vissHandled: Double,
        laborRatePerViss: Double,
        shuttleLaborAmount: Double,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val totalLaborPaid = (vissHandled * laborRatePerViss) + shuttleLaborAmount
            val record = WorkerLaborEntity(
                gateBranch = gateBranch,
                workerName = workerName,
                date = date,
                vissHandled = vissHandled,
                laborRatePerViss = laborRatePerViss,
                shuttleLaborAmount = shuttleLaborAmount,
                totalLaborPaid = totalLaborPaid,
                notes = notes
            )
            repository.addLaborRecord(record)
            onSuccess()
        }
    }

    // ==========================================
    // Print Templates (7 Customizable Templates)
    // ==========================================
    val allTemplates: StateFlow<List<PrintTemplateEntity>> = repository.allTemplates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedTemplateId = MutableStateFlow(1)
    val selectedTemplateId: StateFlow<Int> = _selectedTemplateId.asStateFlow()

    fun selectTemplate(id: Int) {
        _selectedTemplateId.value = id
    }

    fun updatePrintTemplate(template: PrintTemplateEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateTemplate(template)
            onDone()
        }
    }

    // ==========================================
    // Printed Vouchers & Exact Search by Voucher No
    // ==========================================
    val allPrintedVouchers: StateFlow<List<PrintedVoucherEntity>> = repository.allPrintedVouchers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _voucherSearchQuery = MutableStateFlow("")
    val voucherSearchQuery: StateFlow<String> = _voucherSearchQuery.asStateFlow()

    fun setVoucherSearchQuery(query: String) {
        _voucherSearchQuery.value = query
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val voucherSearchResults: StateFlow<List<PrintedVoucherEntity>> = _voucherSearchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.allPrintedVouchers
        } else {
            repository.searchVouchers(query)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedVoucherForView = MutableStateFlow<PrintedVoucherEntity?>(null)
    val selectedVoucherForView: StateFlow<PrintedVoucherEntity?> = _selectedVoucherForView.asStateFlow()

    fun selectVoucherForView(voucher: PrintedVoucherEntity?) {
        _selectedVoucherForView.value = voucher
    }

    suspend fun findVoucherDirectly(voucherNumber: String): PrintedVoucherEntity? {
        return repository.findVoucherByNumber(voucherNumber.trim())
    }

    fun savePrintedVoucher(
        voucherNumber: String,
        templateId: Int,
        templateName: String,
        paperWidthMm: Int,
        cargoId: Long? = null,
        truckId: Long? = null,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        carNumber: String,
        driverName: String,
        itemDescription: String,
        quantity: Int,
        weightViss: Double,
        freightFee: Double,
        laborFee: Double,
        totalAmount: Double,
        paymentStatus: String,
        remarks: String = "",
        onSaved: (PrintedVoucherEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val entity = PrintedVoucherEntity(
                voucherNumber = voucherNumber,
                issueDate = dateStr,
                templateId = templateId,
                templateName = templateName,
                paperWidthMm = paperWidthMm,
                cargoId = cargoId,
                truckId = truckId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                carNumber = carNumber,
                driverName = driverName,
                itemDescription = itemDescription,
                quantity = quantity,
                weightViss = weightViss,
                freightFee = freightFee,
                laborFee = laborFee,
                totalAmount = totalAmount,
                paymentStatus = paymentStatus,
                remarks = remarks
            )
            repository.savePrintedVoucher(entity)
            _selectedVoucherForView.value = entity
            onSaved(entity)
        }
    }

    // Helper to generate a sequential or formatted voucher number
    fun generateNextVoucherNumber(): String {
        val rand = (1000..9999).random()
        return String.format(java.util.Locale.US, "%04d", rand)
    }
}

