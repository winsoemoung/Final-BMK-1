package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CargoEntity
import com.example.data.model.TruckEntity
import com.example.data.model.WorkerLaborEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTabScreen(
    viewModel: BmkViewModel
) {
    val allCargo by viewModel.allCargo.collectAsState()
    val allTrucks by viewModel.allTrucks.collectAsState()
    val workerLaborRecords by viewModel.workerLaborRecords.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Customer စာရင်း", "ကားများ History", "အလုပ်သမား History")

    var selectedCustomerName by remember { mutableStateOf<String?>(null) }
    var selectedTruckDetails by remember { mutableStateOf<TruckEntity?>(null) }
    var selectedWorkerName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "မှတ်တမ်းများ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
        ) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = CardBackground,
                contentColor = BmkNavy
            ) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { selectedSubTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (selectedSubTab == index) BmkNavy else TextSecondary
                            )
                        }
                    )
                }
            }

            when (selectedSubTab) {
                0 -> {
                    // Customer History
                    val groupedCustomers = remember(allCargo) {
                        allCargo.groupBy { it.senderName.ifBlank { "Unassigned Customer" } }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(groupedCustomers.keys.toList()) { custName ->
                            val cargosList = groupedCustomers[custName] ?: emptyList()
                            val firstCargo = cargosList.firstOrNull()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCustomerName = custName }
                                    .testTag("history_cust_$custName"),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Customer: $custName",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = BmkNavy
                                        )
                                        if (firstCargo != null) {
                                            Text(
                                                text = "ဖုန်း: ${firstCargo.senderPhone} | လိပ်စာ: ${firstCargo.senderAddress.ifBlank { "မန္တလေးဂိတ်" }}",
                                                fontSize = 12.sp,
                                                color = TextPrimary
                                            )
                                        }
                                        Text(
                                            text = "အပ်နှံခဲ့သော ပစ္စည်း စုစုပေါင်း: ${cargosList.size} မျိုး",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BmkNavy)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Truck Fleet History
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allTrucks) { truck ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTruckDetails = truck }
                                    .testTag("history_truck_${truck.id}"),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${truck.truckNumber} ${if (truck.ownerName.isNotBlank()) "(${truck.ownerName})" else ""}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = BmkNavy
                                        )
                                        Text(
                                            text = "ယာဉ်မောင်း: ${truck.driverName} (${truck.driverPhone})",
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "ထွက်ခွာရက်: ${truck.dispatchDate} | ကားခ: ${currencyFormat.format(truck.mandalayToKalayFreightPrice)} ကျပ်",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BmkNavy)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Worker Labor History
                    val groupedWorkers = remember(workerLaborRecords) {
                        workerLaborRecords.groupBy { it.workerName }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(groupedWorkers.keys.toList()) { worker ->
                            val records = groupedWorkers[worker] ?: emptyList()
                            val totalPaid = records.sumOf { it.totalLaborPaid }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWorkerName = worker }
                                    .testTag("history_worker_$worker"),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "အလုပ်သမား/အဖွဲ့: $worker",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = BmkNavy
                                        )
                                        Text(
                                            text = "လုပ်အားခ ရရှိခဲ့ဖူးသော အကြိမ်ပေါင်း: ${records.size} ကြိမ်",
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "စုစုပေါင်း ထုတ်ပေးခဲ့ငွေ: ${currencyFormat.format(totalPaid)} ကျပ်",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BmkOrange
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BmkNavy)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var cargoToPrint by remember { mutableStateOf<CargoEntity?>(null) }
    var truckToPrint by remember { mutableStateOf<TruckEntity?>(null) }
    var workerToPrint by remember { mutableStateOf<WorkerLaborEntity?>(null) }

    // Customer Detail Dialog
    selectedCustomerName?.let { custName ->
        val custCargos = allCargo.filter { it.senderName == custName }
        AlertDialog(
            onDismissRequest = { selectedCustomerName = null },
            containerColor = Color.White,
            title = {
                Text("Customer: $custName", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy)
            },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 350.dp)) {
                    items(custCargos) { cargo ->
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("• ဘောက်ချာ: ${cargo.trackingNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkNavy)
                                    Text("• ကုန်အမည်: ${cargo.cargoName.ifBlank { cargo.itemDescription }} (${cargo.quantity} ${cargo.weightUnit})", fontSize = 11.sp, color = TextPrimary)
                                    Text("• အလေးချိန်: ${cargo.weightViss} ပိဿာ", fontSize = 11.sp)
                                    Text("• ကလေးမြို့ လက်ခံသူ: ${cargo.receiverName} (${cargo.receiverPhone})", fontSize = 11.sp, color = TextSecondary)
                                    Text("• ကျသင့်ငွေ: ${currencyFormat.format(cargo.totalAmount)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkOrange)
                                }
                                IconButton(
                                    onClick = { cargoToPrint = cargo },
                                    modifier = Modifier
                                        .background(BmkOrange, RoundedCornerShape(8.dp))
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "Print Voucher", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCustomerName = null }) { Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary) }
            }
        )
    }

    // Truck Loaded Cargo History Dialog
    selectedTruckDetails?.let { truck ->
        val truckCargos = allCargo.filter { it.truckId == truck.id || it.truckNumber == truck.truckNumber }.sortedBy { it.sequenceOrder }
        AlertDialog(
            onDismissRequest = { selectedTruckDetails = null },
            containerColor = Color.White,
            title = {
                Text("${truck.truckNumber} စာရင်း", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ကားပိုင်ရှင်/ယာဉ်မောင်း: ${truck.driverName} (${truck.driverPhone})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("• ကားခ: ${currencyFormat.format(truck.mandalayToKalayFreightPrice)} ကျပ် | ကြိုထုတ်ငွေ: ${currencyFormat.format(truck.advancePayment)} ကျပ်", fontSize = 12.sp, color = BmkNavy)
                    Divider(color = CardBorderColor)
                    Text("ကားပေါ်ပါ ကုန်ပစ္စည်းများ စာရင်း (${truckCargos.size} မျိုး):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkOrange)

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 260.dp)) {
                        items(truckCargos.size) { idx ->
                            val item = truckCargos[idx]
                            Surface(color = SurfaceLight, shape = RoundedCornerShape(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = BmkNavy, shape = RoundedCornerShape(4.dp)) {
                                        Text("#${idx + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.cargoName.ifBlank { item.itemDescription }, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                        Text("ပို့သူ: ${item.senderName} -> လက်ခံသူ: ${item.receiverName}", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    Text("${item.weightViss} ပိဿာ", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BmkNavy)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { truckToPrint = truck },
                        colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ကားစာရင်း Print ထုတ်မည်", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    TextButton(onClick = { selectedTruckDetails = null }) { Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary) }
                }
            }
        )
    }

    // Worker Detail Dialog
    selectedWorkerName?.let { worker ->
        val workerRecords = workerLaborRecords.filter { it.workerName == worker }
        val firstRec = workerRecords.firstOrNull()
        AlertDialog(
            onDismissRequest = { selectedWorkerName = null },
            containerColor = Color.White,
            title = { Text("အလုပ်သမား: $worker", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                    items(workerRecords) { rec ->
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceLight)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("• နေ့ရက်: ${rec.date} | ဂိတ်: ${if (rec.gateBranch == "MANDALAY") "မန္တလေး" else "ကလေး"}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkNavy)
                                    Text("• မနိန်းကုန် ပိဿာ: ${rec.vissHandled} ပိဿာ", fontSize = 11.sp)
                                    Text("• ရှင်းပြီးငွေ: ${currencyFormat.format(rec.totalLaborPaid)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkOrange)
                                }
                                IconButton(
                                    onClick = { workerToPrint = rec },
                                    modifier = Modifier
                                        .background(BmkOrange, RoundedCornerShape(8.dp))
                                        .size(36.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "Print Worker Receipt", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWorkerName = null }) { Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary) }
            }
        )
    }

    // Print Dialog triggers
    cargoToPrint?.let { cargo ->
        com.example.ui.components.CargoPrintDialog(
            cargo = cargo,
            viewModel = viewModel,
            onDismiss = { cargoToPrint = null }
        )
    }

    truckToPrint?.let { truck ->
        val truckCargos = allCargo.filter { it.truckId == truck.id || it.truckNumber == truck.truckNumber }.sortedBy { it.sequenceOrder }
        com.example.ui.components.CargoPrintDialog(
            truck = truck,
            cargosOnTruck = truckCargos,
            viewModel = viewModel,
            onDismiss = { truckToPrint = null }
        )
    }

    workerToPrint?.let { record ->
        val pseudoCargo = com.example.data.model.CargoEntity(
            id = record.id,
            trackingNumber = "WRK-${record.id}",
            senderName = record.workerName,
            senderPhone = if (record.gateBranch == "MANDALAY") "မန္တလေးဂိတ် (M-wages)" else "ကလေးဂိတ် (K-wages)",
            senderAddress = "BMK Express Gate",
            receiverName = record.workerName,
            receiverPhone = record.date,
            receiverShopAddress = record.notes.ifBlank { "အလုပ်သမား ထုတ်ပေးငွေ ဘောက်ချာ" },
            cargoName = "အလုပ်သမားခ - ${record.vissHandled} ပိဿာ",
            itemDescription = "၁ ပိဿာ: ${record.laborRatePerViss} ကျပ်",
            weightViss = record.vissHandled,
            weightUnit = "ပိဿာ",
            quantity = 1,
            freightFee = 0.0,
            mdyLoadingLaborFee = record.totalLaborPaid,
            klyUnloadingLaborFee = 0.0,
            shuttleLaborFee = record.shuttleLaborAmount,
            totalAmount = record.totalLaborPaid,
            paymentStatus = "PAID",
            remarks = record.notes
        )
        com.example.ui.components.CargoPrintDialog(
            cargo = pseudoCargo,
            viewModel = viewModel,
            onDismiss = { workerToPrint = null }
        )
    }
}
