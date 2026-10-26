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

class BmkRepository(
    private val cargoDao: CargoDao,
    private val truckDao: TruckDao,
    private val compensationDao: CompensationDao,
    private val workerLaborDao: WorkerLaborDao
) {
    val allCargo: Flow<List<CargoEntity>> = cargoDao.getAllCargo()
    val cargoCount: Flow<Int> = cargoDao.getCargoCount()
    val totalRevenue: Flow<Double?> = cargoDao.getTotalRevenue()

    fun getCargoByStatus(status: String): Flow<List<CargoEntity>> = cargoDao.getCargoByStatus(status)

    fun searchCargo(query: String): Flow<List<CargoEntity>> = cargoDao.searchCargo(query)

    suspend fun getCargoByTracking(trackingNumber: String): CargoEntity? = cargoDao.getCargoByTracking(trackingNumber)

    suspend fun registerCargo(cargo: CargoEntity): Long = cargoDao.insertCargo(cargo)

    suspend fun updateCargoStatus(id: Long, status: String) = cargoDao.updateCargoStatus(id, status)

    suspend fun assignTruck(cargoId: Long, truckId: Long, truckNumber: String, driverName: String, driverPhone: String) {
        cargoDao.assignTruckToCargo(cargoId, truckId, truckNumber, driverName, driverPhone)
    }

    suspend fun deleteCargo(cargo: CargoEntity) = cargoDao.deleteCargo(cargo)

    // Trucks & Drivers
    val allTrucks: Flow<List<TruckEntity>> = truckDao.getAllTrucks()

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
}
