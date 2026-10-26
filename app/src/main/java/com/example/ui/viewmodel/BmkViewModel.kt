package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.CargoEntity
import com.example.data.model.CompensationEntity
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
            database.workerLaborDao()
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

            // Calculate total Viss depending on unit type
            val totalWeightViss = when (weightUnit) {
                "ကီလို" -> convertKgToViss(unitWeight * quantity)
                "ပိဿာ" -> unitWeight * quantity
                else -> unitWeight * quantity // If weight per item is given in Viss
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
                truckNumber = assignedTruck?.truckNumber ?: "MDY 7D-1212 (12-Wheeler)",
                driverName = assignedTruck?.driverName ?: "ဦးကိုကို (09-45001122)",
                driverPhone = assignedTruck?.driverPhone ?: "0945001122",
                remarks = remarks
            )

            repository.registerCargo(cargo)
            onSuccess(trackingNo)
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

    // Assign Truck to Cargo
    fun assignTruckToCargo(cargoId: Long, truck: TruckEntity) {
        viewModelScope.launch {
            repository.assignTruck(
                cargoId = cargoId,
                truckId = truck.id,
                truckNumber = truck.truckNumber,
                driverName = truck.driverName,
                driverPhone = truck.driverPhone
            )
        }
    }

    // Car Tab Action: Register 12-Wheeler Truck
    fun registerTruck(
        truckNumber: String,
        driverName: String,
        driverPhone: String,
        mandalayToKalayFreightPrice: Double,
        dispatchDate: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val truck = TruckEntity(
                truckNumber = truckNumber,
                driverName = driverName,
                driverPhone = driverPhone,
                truckType = "12-Wheeler Box Truck",
                mandalayToKalayFreightPrice = mandalayToKalayFreightPrice,
                dispatchDate = dispatchDate,
                status = "LOADING",
                notes = notes
            )
            repository.registerTruck(truck)
            onSuccess()
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
}
