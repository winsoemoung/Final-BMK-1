package com.example.data.repository

import com.example.data.dao.CargoDao
import com.example.data.dao.CompensationDao
import com.example.data.dao.TruckDao
import com.example.data.dao.WorkerLaborDao
import com.example.data.model.CargoEntity
import com.example.data.model.CompensationEntity
import com.example.data.model.TruckEntity
import com.example.data.model.WorkerLaborEntity
import kotlinx.coroutines.flow.Flow

import com.example.data.dao.PrintTemplateDao
import com.example.data.dao.PrintedVoucherDao
import com.example.data.model.PrintTemplateEntity
import com.example.data.model.PrintedVoucherEntity

class BmkRepository(
    private val cargoDao: CargoDao,
    private val truckDao: TruckDao,
    private val compensationDao: CompensationDao,
    private val workerLaborDao: WorkerLaborDao,
    private val printTemplateDao: PrintTemplateDao,
    private val printedVoucherDao: PrintedVoucherDao
) {
    val allCargo: Flow<List<CargoEntity>> = cargoDao.getAllCargo()
    val cargoCount: Flow<Int> = cargoDao.getCargoCount()
    val totalRevenue: Flow<Double?> = cargoDao.getTotalRevenue()

    fun getCargoByStatus(status: String): Flow<List<CargoEntity>> = cargoDao.getCargoByStatus(status)

    fun getCargosByTruckId(truckId: Long): Flow<List<CargoEntity>> = cargoDao.getCargosByTruckId(truckId)

    fun searchCargo(query: String): Flow<List<CargoEntity>> = cargoDao.searchCargo(query)

    suspend fun getCargoByTracking(trackingNumber: String): CargoEntity? = cargoDao.getCargoByTracking(trackingNumber)

    suspend fun registerCargo(cargo: CargoEntity): Long = cargoDao.insertCargo(cargo)

    suspend fun updateCargoStatus(id: Long, status: String) = cargoDao.updateCargoStatus(id, status)

    suspend fun updateDeliveryAndPaymentStatus(id: Long, deliveryStatus: String, paymentStatus: String, promisedDate: String) {
        cargoDao.updateDeliveryAndPaymentStatus(id, deliveryStatus, paymentStatus, promisedDate)
    }

    suspend fun updateCargo(cargo: CargoEntity) = cargoDao.updateCargo(cargo)

    suspend fun assignTruckToCargoes(
        cargoIds: List<Long>,
        truckId: Long,
        truckNumber: String,
        driverName: String,
        driverPhone: String
    ) {
        cargoIds.forEachIndexed { index, cargoId ->
            cargoDao.assignTruckToCargo(cargoId, truckId, truckNumber, driverName, driverPhone, index + 1)
        }
        // Update truck status to LOADING if EMPTY
        truckDao.updateTruckStatus(truckId, "LOADING")
    }

    suspend fun updateCargoSequence(id: Long, sequenceOrder: Int) {
        cargoDao.updateCargoSequence(id, sequenceOrder)
    }

    suspend fun updateCargoList(cargos: List<CargoEntity>) {
        cargoDao.updateCargoList(cargos)
    }

    suspend fun deleteCargo(cargo: CargoEntity) = cargoDao.deleteCargo(cargo)

    // Trucks & Drivers
    val allTrucks: Flow<List<TruckEntity>> = truckDao.getAllTrucks()

    fun searchTrucks(query: String): Flow<List<TruckEntity>> = truckDao.searchTrucks(query)

    suspend fun registerTruck(truck: TruckEntity): Long = truckDao.insertTruck(truck)

    suspend fun updateTruckStatus(id: Long, status: String) = truckDao.updateTruckStatus(id, status)

    // Compensation Claims
    val allClaims: Flow<List<CompensationEntity>> = compensationDao.getAllClaims()

    suspend fun submitClaim(claim: CompensationEntity): Long = compensationDao.insertClaim(claim)

    suspend fun updateClaimStatus(id: Long, status: String, paidAmount: Double) = compensationDao.updateClaimStatus(id, status, paidAmount)

    // Worker Labor Records
    val allLaborRecords: Flow<List<WorkerLaborEntity>> = workerLaborDao.getAllLaborRecords()

    fun getLaborByBranch(branch: String): Flow<List<WorkerLaborEntity>> = workerLaborDao.getLaborByBranch(branch)

    suspend fun addLaborRecord(record: WorkerLaborEntity): Long = workerLaborDao.insertLaborRecord(record)

    // Print Templates
    val allTemplates: Flow<List<PrintTemplateEntity>> = printTemplateDao.getAllTemplates()

    fun getTemplateById(id: Int): Flow<PrintTemplateEntity?> = printTemplateDao.getTemplateById(id)

    suspend fun getTemplateByIdOnce(id: Int): PrintTemplateEntity? = printTemplateDao.getTemplateByIdOnce(id)

    suspend fun updateTemplate(template: PrintTemplateEntity) = printTemplateDao.updateTemplate(template)

    suspend fun insertTemplate(template: PrintTemplateEntity) = printTemplateDao.insertTemplate(template)

    // Printed Vouchers & Historical Search
    val allPrintedVouchers: Flow<List<PrintedVoucherEntity>> = printedVoucherDao.getAllPrintedVouchers()

    fun searchVouchers(query: String): Flow<List<PrintedVoucherEntity>> = printedVoucherDao.searchVouchers(query)

    fun getVoucherByNumber(voucherNumber: String): Flow<PrintedVoucherEntity?> = printedVoucherDao.getVoucherByNumber(voucherNumber)

    suspend fun findVoucherByNumber(voucherNumber: String): PrintedVoucherEntity? = printedVoucherDao.findVoucherByNumber(voucherNumber)

    suspend fun savePrintedVoucher(voucher: PrintedVoucherEntity): Long = printedVoucherDao.insertVoucher(voucher)

    suspend fun deleteVoucher(voucher: PrintedVoucherEntity) = printedVoucherDao.deleteVoucher(voucher)
}

