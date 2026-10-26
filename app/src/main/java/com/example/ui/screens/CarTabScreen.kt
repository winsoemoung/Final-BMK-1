package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.model.TruckEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantDialog
import com.example.ui.viewmodel.AiLogisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val allTrucks by viewModel.allTrucks.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }
    var showAddTruckDialog by remember { mutableStateOf(false) }
    var selectedTruckDetails by remember { mutableStateOf<TruckEntity?>(null) }

    var truckNumber by remember { mutableStateOf("MDY 9B-5544 (12-Wheeler)") }
    var driverName by remember { mutableStateOf("ဦးစိုးမြင့် (Driver U Soe Myint)") }
    var driverPhone by remember { mutableStateOf("0945009988") }
    var priceStr by remember { mutableStateOf("1500000") }
    var dispatchDate by remember { mutableStateOf("2026-10-26") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settingsState.language == "my") "ကား / ကားသမား စာရင်း (Car & Drivers)" else "Truck & Driver Fleet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (settingsState.language == "my") "၁၂ ဘီး ကားများနှင့် ခရီးစဉ်များ" else "12-Wheeler Mandalay-Kalay Fleet",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.testTag("car_action_open_ai")
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
                onClick = { showAddTruckDialog = true },
                containerColor = BmkOrange,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(if (settingsState.language == "my") "၁၂ ဘီး ကားသစ် ထည့်မည်" else "Add 12-Wheeler", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                modifier = Modifier.testTag("fab_add_truck")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                Text(
                    text = "၁၂ ဘီး ကားများ စာရင်း (${allTrucks.size} စီး) - [ကားအမည်ကို နှိပ်၍ အသေးစိတ်ကြည့်ပါ]",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Compact Truck List Items
            items(allTrucks) { truck ->
                CompactTruckListItem(
                    truck = truck,
                    currencyFormat = currencyFormat,
                    onClick = { selectedTruckDetails = truck }
                )
            }
        }
    }

    // Truck Detail Modal Dialog when tapped
    selectedTruckDetails?.let { truck ->
        AlertDialog(
            onDismissRequest = { selectedTruckDetails = null },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = truck.truckNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BmkNavy
                    )
                    Surface(
                        color = when (truck.status) {
                            "LOADING" -> StatusAmberBg
                            "IN_TRANSIT" -> StatusBlueBg
                            "KALAY_ARRIVED" -> StatusOrangeBg
                            else -> StatusGreenBg
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = when (truck.status) {
                                "LOADING" -> "၁၂ ဘီး တင်နေဆဲ"
                                "IN_TRANSIT" -> "လမ်းခရီးတွင်"
                                "KALAY_ARRIVED" -> "ကလေးမြို့ဆိုက်"
                                else -> "ပြီးစီး"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (truck.status) {
                                "LOADING" -> StatusAmberText
                                "IN_TRANSIT" -> StatusBlueText
                                "KALAY_ARRIVED" -> StatusOrangeText
                                else -> StatusGreenText
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ကားသမား/ယာဉ်မောင်း: ${truck.driverName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• ကားသမား ဖုန်း: ${truck.driverPhone}", fontSize = 12.sp, color = BmkOrange, fontWeight = FontWeight.Bold)
                    Text("• မန္တလေး - ကလေး ၁၂ ဘီး ကားခ: ${currencyFormat.format(truck.mandalayToKalayFreightPrice)} ကျပ်", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BmkNavy)
                    Text("• ထွက်ခွာမည့် နေ့ရက်: ${truck.dispatchDate}", fontSize = 12.sp, color = TextSecondary)
                    if (truck.notes.isNotBlank()) Text("• မှတ်ချက်: ${truck.notes}", fontSize = 11.sp, color = TextMuted)
                    
                    Divider(color = CardBorderColor)
                    
                    Text("ခရီးစဉ် အခြေအနေ ပြောင်းရန်:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "LOADING" to "တင်ဆဲ",
                            "IN_TRANSIT" to "လမ်းခရီး",
                            "KALAY_ARRIVED" to "ကလေးဆိုက်",
                            "COMPLETED" to "ပြီးစီး"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = truck.status == code,
                                onClick = { viewModel.updateTruckStatus(truck.id, code) },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BmkNavy,
                                    selectedLabelColor = Color.White,
                                    containerColor = SurfaceLight,
                                    labelColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${truck.driverPhone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ကားသမား ဖုန်းခေါ်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTruckDetails = null }) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        )
    }

    if (showAddTruckDialog) {
        AlertDialog(
            onDismissRequest = { showAddTruckDialog = false },
            containerColor = Color.White,
            title = { Text("၁၂ ဘီး ကားသစ် စာရင်းသွင်းရန်", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = truckNumber,
                        onValueChange = { truckNumber = it },
                        label = { Text("ယာဉ်အမှတ် (Truck Number)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_truck_number")
                    )
                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        label = { Text("ကားသမား / ယာဉ်မောင်း အမည်") },
                        modifier = Modifier.fillMaxWidth().testTag("input_driver_name")
                    )
                    OutlinedTextField(
                        value = driverPhone,
                        onValueChange = { driverPhone = it },
                        label = { Text("ကားသမား ဖုန်းနံပါတ်") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("input_driver_phone")
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("မန္တလေး - ကလေး ၁၂ ဘီး ကားခ (ကျပ်)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dispatchDate,
                        onValueChange = { dispatchDate = it },
                        label = { Text("ထွက်ခွာမည့် နေ့ရက်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull() ?: 0.0
                        viewModel.registerTruck(
                            truckNumber = truckNumber,
                            driverName = driverName,
                            driverPhone = driverPhone,
                            mandalayToKalayFreightPrice = price,
                            dispatchDate = dispatchDate,
                            notes = notes,
                            onSuccess = {
                                showAddTruckDialog = false
                                Toast.makeText(context, "၁၂ ဘီး ကားသစ် စာရင်းသွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTruckDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAiDialog) {
        AiAssistantDialog(
            aiViewModel = aiViewModel,
            onDismiss = { showAiDialog = false }
        )
    }
}

@Composable
fun CompactTruckListItem(
    truck: TruckEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("truck_list_item_${truck.id}"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, CardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = truck.truckNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkNavy
                )
                Text(
                    text = "ကားသမား: ${truck.driverName} (${truck.driverPhone})",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = when (truck.status) {
                        "LOADING" -> StatusAmberBg
                        "IN_TRANSIT" -> StatusBlueBg
                        "KALAY_ARRIVED" -> StatusOrangeBg
                        else -> StatusGreenBg
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (truck.status) {
                            "LOADING" -> "၁၂ ဘီး တင်ဆဲ"
                            "IN_TRANSIT" -> "လမ်းခရီးတွင်"
                            "KALAY_ARRIVED" -> "ကလေးဆိုက်"
                            else -> "ပြီးစီး"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (truck.status) {
                            "LOADING" -> StatusAmberText
                            "IN_TRANSIT" -> StatusBlueText
                            "KALAY_ARRIVED" -> StatusOrangeText
                            else -> StatusGreenText
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("အသေးစိတ်", fontSize = 10.sp, color = BmkNavy, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
