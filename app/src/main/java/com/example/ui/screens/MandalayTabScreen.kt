package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.CargoEntity
import com.example.data.model.TruckEntity
import com.example.ui.components.AiAssistantDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiLogisticsViewModel
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandalayTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val allCargo by viewModel.allCargo.collectAsState()
    val allTrucks by viewModel.allTrucks.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }
    var showIntakeForm by remember { mutableStateOf(false) }
    var selectedCargoDetails by remember { mutableStateOf<CargoEntity?>(null) }
    var selectedTruckForList by remember { mutableStateOf<TruckEntity?>(null) }

    // AA vs BB View Modes: 0 = (AA) Customer/Shop List, 1 = (BB) Detailed All Cargos List
    var viewMode by remember { mutableIntStateOf(0) }
    val viewModes = listOf("(AA) Customer/Shop အလိုက်", "(BB) ကုန်ပစ္စည်း စာရင်းပေါင်း")

    // Selection States for Truck Assignment
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedCargoIds = remember { mutableStateListOf<Long>() }
    var showAssignTruckDialog by remember { mutableStateOf(false) }
    var showAddTruckInAssignDialog by remember { mutableStateOf(false) }

    // New Truck Form Fields
    var newTruckNumber by remember { mutableStateOf("3N-776445") }
    var newOwnerName by remember { mutableStateOf("") }
    var newDriverName by remember { mutableStateOf("") }
    var newDriverPhone by remember { mutableStateOf("") }
    var newPriceStr by remember { mutableStateOf("1500000") }
    var newAdvanceStr by remember { mutableStateOf("200000") }
    var newDispatchDate by remember { mutableStateOf("2026-10-26") }

    // Intake Form Fields
    var cargoDensityType by remember { mutableStateOf("HEAVY") } // "HEAVY" (ကုန်မာ) or "BULK" (ကုန်ပွ)
    var cargoName by remember { mutableStateOf("") }
    var unitWeightStr by remember { mutableStateOf("3.2") }
    var weightTypeUnit by remember { mutableStateOf("ပိဿာ") }
    var quantityStr by remember { mutableStateOf("50") }
    var selectedPackageUnit by remember { mutableStateOf("ဖာ") }
    val packageUnitsList = listOf("ဖာ", "ခု", "အိတ်", "ပုံး", "ထုပ်", "စီး")
    var isPackageUnitDropdownExpanded by remember { mutableStateOf(false) }
    var itemDescription by remember { mutableStateOf("") }

    var senderName by remember { mutableStateOf("") }
    var senderPhone by remember { mutableStateOf("") }
    var senderAddress by remember { mutableStateOf("") }

    var isShuttlePickup by remember { mutableStateOf(false) }
    var shuttleFeeStr by remember { mutableStateOf("5000") }
    var shuttleLaborFeeStr by remember { mutableStateOf("2000") }

    var receiverName by remember { mutableStateOf("") }
    var receiverPhone by remember { mutableStateOf("") }
    var receiverShopAddress by remember { mutableStateOf("") }

    var selectedTruck by remember { mutableStateOf<TruckEntity?>(null) }
    var isTruckDropdownExpanded by remember { mutableStateOf(false) }

    var paymentStatus by remember { mutableStateOf("PAID") }
    var remarks by remember { mutableStateOf("") }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var issuedTrackingNo by remember { mutableStateOf("") }

    val unitWeight = unitWeightStr.toDoubleOrNull() ?: 0.0
    val quantity = quantityStr.toIntOrNull() ?: 1
    val totalWeightInEnteredUnit = unitWeight * quantity

    val totalCalculatedViss = if (weightTypeUnit == "Kg") {
        viewModel.convertKgToViss(totalWeightInEnteredUnit)
    } else {
        totalWeightInEnteredUnit
    }
    val totalCalculatedKg = if (weightTypeUnit == "Kg") {
        totalWeightInEnteredUnit
    } else {
        viewModel.convertVissToKg(totalWeightInEnteredUnit)
    }

    val freightFee = totalCalculatedViss * settingsState.freightRatePerViss
    val mdyLoadingFee = totalCalculatedViss * settingsState.mdyLoadingRatePerViss
    val klyUnloadingFee = totalCalculatedViss * settingsState.klyUnloadingRatePerViss
    val shuttleFee = if (isShuttlePickup) (shuttleFeeStr.toDoubleOrNull() ?: 0.0) else 0.0
    val shuttleLaborFee = if (isShuttlePickup) (shuttleLaborFeeStr.toDoubleOrNull() ?: 0.0) else 0.0
    val totalCalculatedAmount = freightFee + mdyLoadingFee + klyUnloadingFee + shuttleFee + shuttleLaborFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settingsState.language == "my") "မန္တလေးဂိတ် (Mandalay Gate)" else "Mandalay Gate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "ဂိုထောင် ကုန်လက်ခံခြင်း နှင့် ၁၂ ဘီး ကားပေါ်တင်ခြင်း",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.testTag("action_open_ai")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "BMK AI Smart Assistant",
                            tint = BmkOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BmkNavy)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showIntakeForm = true },
                containerColor = BmkOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("ကုန်လက်ခံမည်", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier.testTag("fab_receive_cargo")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
        ) {
            // View Mode Selector Bar [ (AA) By Customer/Shop | (BB) All Cargos List ]
            Surface(
                color = CardBackground,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewModes.forEachIndexed { index, modeTitle ->
                        FilterChip(
                            selected = viewMode == index,
                            onClick = { viewMode = index },
                            label = { Text(modeTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BmkNavy,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceLight,
                                labelColor = TextPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Selection Controls Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (viewMode == 0) "Customer / Shop အလိုက် စာရင်းများ" else "ကုန်ပစ္စည်း အပုဒ်ရေ (${allCargo.size}) စာရင်း",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isSelectionMode,
                        onClick = {
                            isSelectionMode = !isSelectionMode
                            if (!isSelectionMode) selectedCargoIds.clear()
                        },
                        label = { Text(if (isSelectionMode) "Select ပိတ်မည်" else "ကားပေါ်တင်ရန် Select မှတ်မည်", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BmkOrange,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceLight,
                            labelColor = TextPrimary
                        )
                    )

                    if (isSelectionMode && selectedCargoIds.isNotEmpty()) {
                        Button(
                            onClick = { showAssignTruckDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("ကားပေါ်တင်မည် (${selectedCargoIds.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Content List View
            if (viewMode == 0) {
                // (AA) Mode - Grouped by Customer / Shop Name
                val groupedCargos = remember(allCargo) {
                    allCargo.groupBy { it.senderName.ifBlank { "အမည်မသိ Customer / Shop" } }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groupedCargos.keys.toList()) { custName ->
                        val customerItems = groupedCargos[custName] ?: emptyList()
                        val allCustIds = customerItems.map { it.id }
                        val isAllCustSelected = selectedCargoIds.containsAll(allCustIds)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (isSelectionMode) {
                                            Checkbox(
                                                checked = isAllCustSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        allCustIds.forEach { if (!selectedCargoIds.contains(it)) selectedCargoIds.add(it) }
                                                    } else {
                                                        selectedCargoIds.removeAll(allCustIds)
                                                    }
                                                }
                                            )
                                        }
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = custName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = BmkNavy
                                            )
                                            val phone = customerItems.firstOrNull()?.senderPhone.orEmpty()
                                            if (phone.isNotBlank()) {
                                                Text("ဖုန်း: $phone", fontSize = 11.sp, color = TextSecondary)
                                            }
                                        }
                                    }

                                    Surface(color = BmkNavy.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "${customerItems.size} ထုပ်",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BmkNavy,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderColor)

                                // Inner list of items delivered by this customer
                                customerItems.forEach { cargo ->
                                    val isSelected = selectedCargoIds.contains(cargo.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                if (isSelectionMode) {
                                                    if (isSelected) selectedCargoIds.remove(cargo.id) else selectedCargoIds.add(cargo.id)
                                                } else {
                                                    selectedCargoDetails = cargo
                                                }
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelectionMode) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedCargoIds.add(cargo.id) else selectedCargoIds.remove(cargo.id)
                                                }
                                            )
                                        }

                                        // Bright Purple Badge for Bulk Cargo (ကုန်ပွ)
                                        Surface(
                                            color = if (cargo.cargoDensityType == "BULK") BulkPurpleBg else BmkNavy.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (cargo.cargoDensityType == "BULK") "ကုန်ပွ" else "ကုန်မာ",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (cargo.cargoDensityType == "BULK") BulkPurpleText else BmkNavy,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cargo.cargoName.ifBlank { cargo.itemDescription },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${cargo.quantity} ${cargo.weightUnit} (${cargo.weightViss} ပိဿာ)",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        if (cargo.truckNumber.isNotBlank()) {
                                            Surface(
                                                color = StatusOrangeBg,
                                                shape = RoundedCornerShape(4.dp),
                                                modifier = Modifier.clickable {
                                                    val foundTruck = allTrucks.find { it.id == cargo.truckId || it.truckNumber == cargo.truckNumber }
                                                    selectedTruckForList = foundTruck ?: TruckEntity(truckNumber = cargo.truckNumber, driverName = cargo.driverName, driverPhone = cargo.driverPhone, dispatchDate = "2026-10-26")
                                                }
                                            ) {
                                                Text(
                                                    text = cargo.truckNumber,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = StatusOrangeText,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // (BB) Mode - Detailed All Cargos List (Flat List of Thousands of Items)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allCargo) { cargo ->
                        val isSelected = selectedCargoIds.contains(cargo.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedCargoIds.remove(cargo.id) else selectedCargoIds.add(cargo.id)
                                    } else {
                                        selectedCargoDetails = cargo
                                    }
                                }
                                .testTag("cargo_bb_${cargo.id}"),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) BmkOrange else CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (isSelectionMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedCargoIds.add(cargo.id) else selectedCargoIds.remove(cargo.id)
                                            }
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = cargo.cargoName.ifBlank { cargo.itemDescription },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = BmkNavy
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Bright Purple Badge for Bulk Cargo (ကုန်ပွ)
                                            Surface(
                                                color = if (cargo.cargoDensityType == "BULK") BulkPurpleBg else BmkNavy.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (cargo.cargoDensityType == "BULK") "ကုန်ပွ" else "ကုန်မာ",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (cargo.cargoDensityType == "BULK") BulkPurpleText else BmkNavy,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "ပိုင်ရှင်: ${cargo.senderName} (${cargo.senderPhone})",
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "အရေအတွက်: ${cargo.quantity} ${cargo.weightUnit} (${cargo.weightViss} ပိဿာ)",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${currencyFormat.format(cargo.totalAmount)} ကျပ်",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BmkOrange
                                    )
                                    if (cargo.truckNumber.isNotBlank()) {
                                        Surface(
                                            color = StatusOrangeBg,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.clickable {
                                                val foundTruck = allTrucks.find { it.id == cargo.truckId || it.truckNumber == cargo.truckNumber }
                                                selectedTruckForList = foundTruck ?: TruckEntity(truckNumber = cargo.truckNumber, driverName = cargo.driverName, driverPhone = cargo.driverPhone, dispatchDate = "2026-10-26")
                                            }
                                        ) {
                                            Text(
                                                text = cargo.truckNumber,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusOrangeText,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Items Loaded on Selected Truck with Reordering (1..N)
    selectedTruckForList?.let { truck ->
        val truckCargos = remember(allCargo, truck) {
            allCargo.filter { it.truckId == truck.id || it.truckNumber == truck.truckNumber }.sortedBy { it.sequenceOrder }
        }

        AlertDialog(
            onDismissRequest = { selectedTruckForList = null },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ကားပေါ်ရောက် ကုန်ပစ္စည်းများ (${truck.truckNumber})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BmkNavy
                    )
                    Surface(color = StatusOrangeBg, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "${truckCargos.size} ထုပ်",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusOrangeText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ယာဉ်မောင်း/ကားသမား: ${truck.driverName} (${truck.driverPhone})", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("• အစဉ်လိုက် ရွှေ့ပြောင်းရန် ↑↓ မြှားများကို နှိပ်ပါ:", fontSize = 11.sp, color = TextSecondary)

                    Divider(color = CardBorderColor)

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(truckCargos) { index, item ->
                            Surface(
                                color = SurfaceLight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, CardBorderColor)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(color = BmkNavy, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "#${index + 1}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.cargoName.ifBlank { item.itemDescription },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "ပိုင်ရှင်: ${item.senderName} (${item.quantity} ${item.weightUnit})",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    // Up / Down Reorder Arrows
                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    viewModel.swapCargoSequenceOnTruck(truckCargos, index, index - 1)
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = if (index > 0) BmkNavy else Color.Gray, modifier = Modifier.size(16.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < truckCargos.size - 1) {
                                                    viewModel.swapCargoSequenceOnTruck(truckCargos, index, index + 1)
                                                }
                                            },
                                            enabled = index < truckCargos.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = if (index < truckCargos.size - 1) BmkNavy else Color.Gray, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedTruckForList = null }, colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }

    // Assign to Truck Dialog
    if (showAssignTruckDialog) {
        AlertDialog(
            onDismissRequest = { showAssignTruckDialog = false },
            containerColor = Color.White,
            title = { Text("ရွေးချယ်ထားသော ပစ္စည်း (${selectedCargoIds.size}) ထုပ်ကို ကားပေါ်တင်မည်", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ကားတစ်စီး ရွေးချယ်ပါ သို့မဟုတ် ကားအသစ် ပေါင်းထည့်ပါ:", fontSize = 12.sp, color = TextPrimary)

                    allTrucks.forEach { truck ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.assignSelectedCargosToTruck(
                                        cargoIds = selectedCargoIds.toList(),
                                        truck = truck,
                                        onSuccess = {
                                            showAssignTruckDialog = false
                                            isSelectionMode = false
                                            selectedCargoIds.clear()
                                            Toast.makeText(context, "${truck.truckNumber} ကားပေါ်သို့ ပစ္စည်းများ အောင်မြင်စွာ တင်လိုက်ပါပြီ", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(truck.truckNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                                    Text("ကားသမား: ${truck.driverName} (${truck.driverPhone})", fontSize = 11.sp, color = TextPrimary)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BmkOrange)
                            }
                        }
                    }

                    Button(
                        onClick = { showAddTruckInAssignDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ ကားအသစ် ပေါင်းထည့်၍ တင်မည် (Add Car)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAssignTruckDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Add New Truck inside Assign Dialog
    if (showAddTruckInAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAddTruckInAssignDialog = false },
            containerColor = Color.White,
            title = { Text("၁၂ ဘီး ကားအသစ် ပေါင်းထည့်ရန်", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newTruckNumber, onValueChange = { newTruckNumber = it }, label = { Text("ကားနံပါတ် (Car Number)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newOwnerName, onValueChange = { newOwnerName = it }, label = { Text("ကားပိုင်ရှင် အမည်") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newDriverName, onValueChange = { newDriverName = it }, label = { Text("မောင်းသူ / ကားသမား အမည်") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newDriverPhone, onValueChange = { newDriverPhone = it }, label = { Text("ဖုန်းနံပါတ်") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newPriceStr, onValueChange = { newPriceStr = it }, label = { Text("မန္တလေး - ကလေး ကားခ (ကျပ်)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = newAdvanceStr, onValueChange = { newAdvanceStr = it }, label = { Text("ကြိုထုတ်ငွေ (ကျပ်)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = newPriceStr.toDoubleOrNull() ?: 1500000.0
                        val advance = newAdvanceStr.toDoubleOrNull() ?: 0.0
                        viewModel.registerTruck(
                            truckNumber = newTruckNumber,
                            ownerName = newOwnerName,
                            driverName = newDriverName,
                            driverPhone = newDriverPhone,
                            mandalayToKalayFreightPrice = price,
                            advancePayment = advance,
                            dispatchDate = newDispatchDate,
                            notes = "",
                            onSuccess = { createdTruck ->
                                viewModel.assignSelectedCargosToTruck(
                                    cargoIds = selectedCargoIds.toList(),
                                    truck = createdTruck,
                                    onSuccess = {
                                        showAddTruckInAssignDialog = false
                                        showAssignTruckDialog = false
                                        isSelectionMode = false
                                        selectedCargoIds.clear()
                                        Toast.makeText(context, "${createdTruck.truckNumber} ပေါ်သို့ တင်လိုက်ပါပြီ", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange)
                ) {
                    Text("သိမ်းဆည်း၍ ကားပေါ်တင်မည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTruckInAssignDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Cargo Detail Dialog
    selectedCargoDetails?.let { cargo ->
        AlertDialog(
            onDismissRequest = { selectedCargoDetails = null },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cargo.trackingNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BmkNavy
                    )
                    Surface(
                        color = if (cargo.cargoDensityType == "BULK") BulkPurpleBg else BmkNavy.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (cargo.cargoDensityType == "BULK") "ကုန်ပွ" else "ကုန်မာ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cargo.cargoDensityType == "BULK") BulkPurpleText else BmkNavy,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ကုန်ပစ္စည်း အမည်: ${cargo.cargoName.ifBlank { cargo.itemDescription }}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• အရေအတွက် & ယူနစ်: ${cargo.quantity} ${cargo.weightUnit}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("• အလေးချိန်: ${cargo.weightViss} ပိဿာ / ${String.format("%.1f", cargo.weightKg)} Kg", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                    Divider(color = CardBorderColor)
                    Text("• ပို့သူ: ${cargo.senderName} (${cargo.senderPhone})", fontSize = 12.sp, color = TextPrimary)
                    if (cargo.senderAddress.isNotBlank()) Text("  မန္တလေး လိပ်စာ: ${cargo.senderAddress}", fontSize = 11.sp, color = TextSecondary)
                    Text("• ကလေးမြို့ လက်ခံသူ: ${cargo.receiverName} (${cargo.receiverPhone})", fontSize = 12.sp, color = TextPrimary)
                    Text("  ကလေးမြို့ ဆိုင်လိပ်စာ: ${cargo.receiverShopAddress}", fontSize = 11.sp, color = TextSecondary)
                    Divider(color = CardBorderColor)
                    if (cargo.truckNumber.isNotBlank()) Text("• သတ်မှတ် ၁၂ ဘီး ကား: ${cargo.truckNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BmkNavy)
                    Text("• စုစုပေါင်း ကျသင့်ငွေ: ${currencyFormat.format(cargo.totalAmount)} ကျပ် (${cargo.paymentStatus})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BmkOrange)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cargo.senderPhone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ဖုန်းခေါ်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCargoDetails = null }) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        )
    }

    // Refactored Intake Form Dialog
    if (showIntakeForm) {
        AlertDialog(
            onDismissRequest = { showIntakeForm = false },
            containerColor = Color.White,
            title = { Text("မန္တလေးဂိတ် ကုန်လက်ခံလွှာ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAiDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BmkNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_autofill_intake_btn")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BmkOrange, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✨ Gemini AI ဖြင့် စာကြောင်းမှ စာရင်းအလိုအလျောက်ဖြည့်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // 1. ကုန်မာ / ကုန်ပွ
                    item {
                        Text("၁။ ကုန်အမျိုးအစား:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { cargoDensityType = "HEAVY" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cargoDensityType == "HEAVY") BmkNavy else SurfaceLight,
                                    contentColor = if (cargoDensityType == "HEAVY") Color.White else TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (cargoDensityType == "HEAVY") BmkNavy else Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f)
                            ) { Text("ကုန်မာ (Heavy)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                            Button(
                                onClick = { cargoDensityType = "BULK" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (cargoDensityType == "BULK") BulkPurpleBright else SurfaceLight,
                                    contentColor = if (cargoDensityType == "BULK") Color.White else TextPrimary
                                ),
                                border = BorderStroke(1.dp, if (cargoDensityType == "BULK") BulkPurpleBright else Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f)
                            ) { Text("ကုန်ပွ (Bulk)", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }

                    // 2. ကုန်ပစ္စည်း အမည်
                    item {
                        OutlinedTextField(
                            value = cargoName,
                            onValueChange = { cargoName = it },
                            label = { Text("၂။ ကုန်ပစ္စည်း အမည် (e.g. ကြွေပြား)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_cargo_name"),
                            singleLine = true
                        )
                    }

                    // 3. တစ်ခု အလေးချိန် + (ပိဿာ or Kg)
                    item {
                        Column {
                            Text("၃။ တစ်ခု အလေးချိန်:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = unitWeightStr,
                                    onValueChange = { unitWeightStr = it },
                                    label = { Text("တစ်ခု အလေးချိန် (e.g. 3.2)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.3f).testTag("input_unit_weight"),
                                    singleLine = true
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .background(SurfaceLight, RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                ) {
                                    FilterChip(
                                        selected = weightTypeUnit == "ပိဿာ",
                                        onClick = { weightTypeUnit = "ပိဿာ" },
                                        label = { Text("ပိဿာ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BmkNavy, selectedLabelColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = weightTypeUnit == "Kg",
                                        onClick = { weightTypeUnit = "Kg" },
                                        label = { Text("Kg", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BmkNavy, selectedLabelColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Customer လာအပ်သော အရေအတွက် + ဘေးတွင် ယူနစ် (ဖာ, ခု, အိတ်, ပုံး, etc)
                    item {
                        Column {
                            Text("၄။ အရေအတွက် နှင့် ယူနစ်:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = quantityStr,
                                    onValueChange = { quantityStr = it },
                                    label = { Text("အရေအတွက် (e.g. 50)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1.2f).testTag("input_cargo_quantity"),
                                    singleLine = true
                                )

                                ExposedDropdownMenuBox(
                                    expanded = isPackageUnitDropdownExpanded,
                                    onExpandedChange = { isPackageUnitDropdownExpanded = !isPackageUnitDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedPackageUnit,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("ယူနစ် (e.g. ဖာ)") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPackageUnitDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isPackageUnitDropdownExpanded,
                                        onDismissRequest = { isPackageUnitDropdownExpanded = false }
                                    ) {
                                        packageUnitsList.forEach { unitItem ->
                                            DropdownMenuItem(
                                                text = { Text(unitItem, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    selectedPackageUnit = unitItem
                                                    isPackageUnitDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. အသေးစိတ် ဖော်ပြချက် ၏ အထက်တွင် စုစုပေါင်း အလေးချိန် ဖော်ပြချက်
                    item {
                        Surface(
                            color = BmkNavy.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BmkNavy.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "စုစုပေါင်း အလေးချိန် = [ အရေအတွက် $quantity x တစ်ခု အလေးချိန် $unitWeight $weightTypeUnit ]",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "စုစုပေါင်း = ${String.format("%.2f", totalCalculatedViss)} ပိဿာ (${String.format("%.2f", totalCalculatedKg)} Kg)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BmkNavy
                                )
                            }
                        }
                    }

                    // ၅။ အသေးစိတ် ဖော်ပြချက်
                    item {
                        OutlinedTextField(
                            value = itemDescription,
                            onValueChange = { itemDescription = it },
                            label = { Text("၅။ ကုန်ပစ္စည်း အသေးစိတ် ဖော်ပြချက်") },
                            modifier = Modifier.fillMaxWidth().testTag("input_item_description")
                        )
                    }

                    // ၆။ ပို့သူ (မန္တလေးဂိတ်)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("၆။ ပို့သူ (မန္တလေးဂိတ်):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            OutlinedTextField(
                                value = senderName,
                                onValueChange = { senderName = it },
                                label = { Text("အမည်") },
                                modifier = Modifier.fillMaxWidth().testTag("input_sender_name"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = senderPhone,
                                onValueChange = { senderPhone = it },
                                label = { Text("ဖုန်းနံပါတ်") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth().testTag("input_sender_phone"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = senderAddress,
                                onValueChange = { senderAddress = it },
                                label = { Text("မန္တလေး လိပ်စာ (Mandalay Address)") },
                                modifier = Modifier.fillMaxWidth().testTag("input_sender_address")
                            )
                        }
                    }

                    // ၇။ ခေါက်တို သွားတိုက်ရန် အမှန်ခြစ်
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusOrangeBg),
                            border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isShuttlePickup,
                                        onCheckedChange = { isShuttlePickup = it },
                                        colors = CheckboxDefaults.colors(checkedColor = BmkOrange)
                                    )
                                    Text("၇။ ဂိတ် မပို့နိုင်ရင် ခေါက်တို သွားတိုက်ရန်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusOrangeText)
                                }
                                AnimatedVisibility(visible = isShuttlePickup) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                                        OutlinedTextField(
                                            value = shuttleFeeStr,
                                            onValueChange = { shuttleFeeStr = it },
                                            label = { Text("ခေါက်တို ကြေး (ကျပ်)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = shuttleLaborFeeStr,
                                            onValueChange = { shuttleLaborFeeStr = it },
                                            label = { Text("အလုပ်သမားခ (ကျပ်)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ၈။ Kalaymyo မှ လက်ခံသူ
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("၈။ Kalaymyo မှ လက်ခံသူ:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            OutlinedTextField(
                                value = receiverName,
                                onValueChange = { receiverName = it },
                                label = { Text("အမည်") },
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_name"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = receiverPhone,
                                onValueChange = { receiverPhone = it },
                                label = { Text("ဖုန်းနံပါတ်") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_phone"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = receiverShopAddress,
                                onValueChange = { receiverShopAddress = it },
                                label = { Text("ကလေးမြို့ ပို့ရမည့် ဆိုင် / လိပ်စာ") },
                                modifier = Modifier.fillMaxWidth().testTag("input_receiver_shop_address")
                            )
                        }
                    }

                    // ၉။ Car Tab မှ Car တစ်စီး ရွေးချယ်မှု
                    item {
                        Column {
                            Text("၉။ Car Tab မှ Car တစ်စီး ရွေးချယ်ရန် (Optional):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            ExposedDropdownMenuBox(
                                expanded = isTruckDropdownExpanded,
                                onExpandedChange = { isTruckDropdownExpanded = !isTruckDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedTruck?.truckNumber ?: "၁၂ ဘီး ကား ရွေးချယ်ရန်",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("သတ်မှတ် ၁၂ ဘီး ကား") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTruckDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isTruckDropdownExpanded,
                                    onDismissRequest = { isTruckDropdownExpanded = false }
                                ) {
                                    allTrucks.forEach { truck ->
                                        DropdownMenuItem(
                                            text = { Text("${truck.truckNumber} (${truck.driverName})", fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                selectedTruck = truck
                                                isTruckDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Total Calculation Summary Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("ကျသင့်ငွေ တွက်ချက်မှု ချုပ်:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BmkNavy)
                                Text("• တန်ဆာခ (${String.format("%.1f", totalCalculatedViss)} ပိဿာ x ${settingsState.freightRatePerViss.toInt()} ကျပ်) = ${currencyFormat.format(freightFee)} ကျပ်", fontSize = 12.sp, color = TextPrimary)
                                Text("• မန္တလေး ၁၂ ဘီး တင်ခ = ${currencyFormat.format(mdyLoadingFee)} ကျပ်", fontSize = 12.sp, color = TextPrimary)
                                Text("• ကလေး ၁၂ ဘီး ချခ = ${currencyFormat.format(klyUnloadingFee)} ကျပ်", fontSize = 12.sp, color = TextPrimary)
                                if (isShuttlePickup) {
                                    Text("• ခေါက်တို ကြေး + အလုပ်သမားခ = ${currencyFormat.format(shuttleFee + shuttleLaborFee)} ကျပ်", fontSize = 12.sp, color = BmkOrange, fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFCBD5E1))
                                Text(
                                    text = "စုစုပေါင်း ကျသင့်ငွေ = ${currencyFormat.format(totalCalculatedAmount)} ကျပ်",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BmkOrange
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cargoName.isBlank() || senderName.isBlank() || receiverName.isBlank()) {
                            Toast.makeText(context, "ကျေးဇူးပြု၍ ကုန်အမည်၊ ပို့သူ နှင့် လက်ခံသူ ဖြည့်သွင်းပါ", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.registerCargoIntake(
                                cargoDensityType = cargoDensityType,
                                cargoName = cargoName,
                                unitWeight = unitWeight,
                                weightUnit = selectedPackageUnit,
                                quantity = quantity,
                                itemDescription = itemDescription.ifBlank { cargoName },
                                senderName = senderName,
                                senderPhone = senderPhone,
                                senderAddress = senderAddress,
                                isShuttlePickup = isShuttlePickup,
                                shuttleFee = shuttleFee,
                                shuttleLaborFee = shuttleLaborFee,
                                receiverName = receiverName,
                                receiverPhone = receiverPhone,
                                receiverShopAddress = receiverShopAddress.ifBlank { "ကလေးမြို့ ဂိတ်တွင် လာထုတ်မည်" },
                                assignedTruck = selectedTruck,
                                paymentStatus = paymentStatus,
                                remarks = remarks,
                                onSuccess = { trackingNo ->
                                    issuedTrackingNo = trackingNo
                                    showIntakeForm = false
                                    showSuccessDialog = true
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                    modifier = Modifier.testTag("confirm_register_btn")
                ) {
                    Text("စာရင်းသွင်းမည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showIntakeForm = false },
                    modifier = Modifier.testTag("cancel_register_btn")
                ) {
                    Text("Cancel (မလုပ်တော့ပါ)", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = Color.White,
            title = { Text("ကုန်ပစ္စည်း စာရင်းသွင်းပြီးပါပြီ!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = { Text("ဘောက်ချာ နံပါတ်: $issuedTrackingNo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkOrange) },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)) {
                    Text("အတည်ပြုပါသည်", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAiDialog) {
        AiAssistantDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showAiDialog = false },
            onAutoFillCargo = { parsed ->
                showIntakeForm = true
                cargoDensityType = parsed.cargoType
                cargoName = parsed.cargoName
                quantityStr = parsed.quantity.toString()
                selectedPackageUnit = parsed.packageUnit.ifBlank { "ဖာ" }
                unitWeightStr = parsed.unitWeight.toString()
                if (parsed.senderName.isNotBlank()) senderName = parsed.senderName
                if (parsed.senderPhone.isNotBlank()) senderPhone = parsed.senderPhone
                if (parsed.description.isNotBlank()) itemDescription = parsed.description
            }
        )
    }
}
