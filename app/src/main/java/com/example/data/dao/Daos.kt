package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    @Query("SELECT * FROM cargo_shipments WHERE truckId = :truckId ORDER BY sequenceOrder ASC, id ASC")
    fun getCargosByTruckId(truckId: Long): Flow<List<CargoEntity>>

    @Query("SELECT * FROM cargo_shipments WHERE trackingNumber = :trackingNumber LIMIT 1")
    suspend fun getCargoByTracking(trackingNumber: String): CargoEntity?

    @Query("SELECT * FROM cargo_shipments WHERE trackingNumber LIKE '%' || :query || '%' OR senderPhone LIKE '%' || :query || '%' OR receiverPhone LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%' OR receiverName LIKE '%' || :query || '%' OR receiverShopAddress LIKE '%' || :query || '%' ORDER BY createdTimestamp DESC")
    fun searchCargo(query: String): Flow<List<CargoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargo(cargo: CargoEntity): Long

    @Update
    suspend fun updateCargoList(cargos: List<CargoEntity>)

    @Query("UPDATE cargo_shipments SET status = :status WHERE id = :id")
    suspend fun updateCargoStatus(id: Long, status: String)

    @Query("UPDATE cargo_shipments SET truckId = :truckId, truckNumber = :truckNumber, driverName = :driverName, driverPhone = :driverPhone, sequenceOrder = :sequenceOrder, status = 'LOADING_12WHEELER' WHERE id = :id")
    suspend fun assignTruckToCargo(id: Long, truckId: Long, truckNumber: String, driverName: String, driverPhone: String, sequenceOrder: Int)

    @Query("UPDATE cargo_shipments SET sequenceOrder = :sequenceOrder WHERE id = :id")
    suspend fun updateCargoSequence(id: Long, sequenceOrder: Int)

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

    @Query("SELECT * FROM truck_drivers WHERE truckNumber LIKE '%' || :query || '%' OR driverName LIKE '%' || :query || '%' OR ownerName LIKE '%' || :query || '%' OR dispatchDate LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchTrucks(query: String): Flow<List<TruckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTruck(truck: TruckEntity): Long

    @Query("UPDATE truck_drivers SET status = :status WHERE id = :id")
    suspend fun updateTruckStatus(id: Long, status: String)

    @Query("UPDATE truck_drivers SET totalVissLoaded = :viss, totalCargosCount = :count WHERE id = :id")
    suspend fun updateTruckTotals(id: Long, viss: Double, count: Int)
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
