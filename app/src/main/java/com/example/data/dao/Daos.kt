package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CargoEntity
import com.example.data.model.CompensationEntity
import com.example.data.model.TruckEntity
import com.example.data.model.WorkerLaborEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CargoDao {
    @Query("SELECT * FROM cargo_shipments ORDER BY createdTimestamp DESC")
    fun getAllCargo(): Flow<List<CargoEntity>>

    @Query("SELECT * FROM cargo_shipments WHERE status = :status ORDER BY createdTimestamp DESC")
    fun getCargoByStatus(status: String): Flow<List<CargoEntity>>

    @Query("SELECT * FROM cargo_shipments WHERE trackingNumber = :trackingNumber LIMIT 1")
    suspend fun getCargoByTracking(trackingNumber: String): CargoEntity?

    @Query("SELECT * FROM cargo_shipments WHERE trackingNumber LIKE '%' || :query || '%' OR senderPhone LIKE '%' || :query || '%' OR receiverPhone LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%' OR receiverName LIKE '%' || :query || '%' OR receiverShopAddress LIKE '%' || :query || '%' ORDER BY createdTimestamp DESC")
    fun searchCargo(query: String): Flow<List<CargoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargo(cargo: CargoEntity): Long

    @Query("UPDATE cargo_shipments SET status = :status WHERE id = :id")
    suspend fun updateCargoStatus(id: Long, status: String)

    @Query("UPDATE cargo_shipments SET truckId = :truckId, truckNumber = :truckNumber, driverName = :driverName, driverPhone = :driverPhone WHERE id = :id")
    suspend fun assignTruckToCargo(id: Long, truckId: Long, truckNumber: String, driverName: String, driverPhone: String)

    @Delete
    suspend fun deleteCargo(cargo: CargoEntity)

    @Query("SELECT COUNT(*) FROM cargo_shipments")
    fun getCargoCount(): Flow<Int>

    @Query("SELECT SUM(totalAmount) FROM cargo_shipments")
    fun getTotalRevenue(): Flow<Double?>
}

@Dao
interface TruckDao {
    @Query("SELECT * FROM truck_drivers ORDER BY id DESC")
    fun getAllTrucks(): Flow<List<TruckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTruck(truck: TruckEntity): Long

    @Query("UPDATE truck_drivers SET status = :status WHERE id = :id")
    suspend fun updateTruckStatus(id: Long, status: String)
}

@Dao
interface CompensationDao {
    @Query("SELECT * FROM compensation_claims ORDER BY id DESC")
    fun getAllClaims(): Flow<List<CompensationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: CompensationEntity): Long

    @Query("UPDATE compensation_claims SET status = :status, paidAmount = :paidAmount WHERE id = :id")
    suspend fun updateClaimStatus(id: Long, status: String, paidAmount: Double)
}

@Dao
interface WorkerLaborDao {
    @Query("SELECT * FROM worker_labor_records ORDER BY id DESC")
    fun getAllLaborRecords(): Flow<List<WorkerLaborEntity>>

    @Query("SELECT * FROM worker_labor_records WHERE gateBranch = :branch ORDER BY id DESC")
    fun getLaborByBranch(branch: String): Flow<List<WorkerLaborEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaborRecord(record: WorkerLaborEntity): Long
}
