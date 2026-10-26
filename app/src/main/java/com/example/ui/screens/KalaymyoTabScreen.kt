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
import com.example.data.model.CargoEntity
import com.example.data.model.CompensationEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import java.text.NumberFormat
import java.util.Locale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AiAssistantDialog
import com.example.ui.viewmodel.AiLogisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KalaymyoTabScreen(
    viewModel: BmkViewModel,
    aiViewModel: AiLogisticsViewModel = viewModel()
) {
    val context = LocalContext.current
    val allCargo by viewModel.allCargo.collectAsState()
    val claims by viewModel.compensationClaims.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var showAiDialog by remember { mutableStateOf(false) }
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("ကလေးမြို့ ဆိုင်အရောက် ကုန်ပစ္စည်းများ", "ပျက်စီးမှု လျော်ကြေးပေးခြင်း")

    var selectedCargoDetails by remember { mutableStateOf<CargoEntity?>(null) }
    var selectedClaimDetails by remember { mutableStateOf<CompensationEntity?>(null) }
    var showClaimDialog by remember { mutableStateOf(false) }

    // Claim form
    var trackingNo by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var damageDescription by remember { mutableStateOf("") }
    var claimAmountStr by remember { mutableStateOf("25000") }
    var paidAmountStr by remember { mutableStateOf("25000") }
    var notes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (settingsState.language == "my") "ကလေးမြို့ဂိတ် (Kalaymyo Gate)" else "Kalaymyo Gate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (settingsState.language == "my") "၁၂ ဘီး ကုန်ချခြင်း နှင့် ဆိုင်အရောက် ပို့ဆောင်မှု" else "Unloading & Direct Shop Delivery",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAiDialog = true },
                        modifier = Modifier.testTag("kalay_action_open_ai")
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

            if (selectedSubTab == 0) {
                // Subtab 0: Kalaymyo Receivers & Unloading
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    item {
                        Text(
                            text = "ကလေးမြို့ လက်ခံသူ ကုန်ပစ္စည်းများ - [အမည်ကို နှိပ်၍ အသေးစိတ်ကြည့်ပါ]",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    items(allCargo) { cargo ->
                        CompactKalayCargoListItem(
                            cargo = cargo,
                            currencyFormat = currencyFormat,
                            onClick = { selectedCargoDetails = cargo }
                        )
                    }
                }
            } else {
                // Subtab 1: Compensation Claims
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ကုန်ပစ္စည်း ပျက်စီးမှု လျော်ကြေး စာရင်း",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Button(
                                    onClick = { showClaimDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("လျော်ကြေးသစ်", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        items(claims) { claim ->
                            CompactClaimListItem(
                                claim = claim,
                                currencyFormat = currencyFormat,
                                onClick = { selectedClaimDetails = claim }
                            )
                        }
                    }
                }
            }
        }
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
                    Text(text = cargo.trackingNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy)
                    Surface(
                        color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenBg else StatusAmberBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (cargo.status == "DELIVERED_TO_SHOP") "ဆိုင်အရောက် ပို့ပြီး" else "ကလေးဂိတ်ရောက်",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenText else StatusAmberText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ကလေးမြို့ လက်ခံသူ: ${cargo.receiverName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• ဖုန်းနံပါတ်: ${cargo.receiverPhone}", fontSize = 12.sp, color = BmkOrange, fontWeight = FontWeight.Bold)
                    Text("• ပို့ရမည့် ဆိုင်လိပ်စာ: ${cargo.receiverShopAddress}", fontSize = 12.sp, color = TextPrimary)
                    Text("• ကုန်ပစ္စည်း: ${cargo.cargoName.ifBlank { cargo.itemDescription }} (${cargo.quantity} ${cargo.weightUnit})", fontSize = 12.sp, color = TextPrimary)
                    Text("• ကလေး ၁၂ ဘီး ကုန်ချခ: ${currencyFormat.format(cargo.klyUnloadingLaborFee)} ကျပ် (၂၅ ကျပ်/ပိဿာ)", fontSize = 12.sp, color = TextSecondary)
                    Text("• စုစုပေါင်း ကျသင့်ငွေ: ${currencyFormat.format(cargo.totalAmount)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BmkOrange)
                    
                    Divider(color = CardBorderColor)
                    
                    Text("ကလေးမြို့ ဂိတ်အခြေအနေ ပြောင်းရန်:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "KALAY_ARRIVED" to "ကလေးဂိတ်ရောက်",
                            "DELIVERED_TO_SHOP" to "ဆိုင်အရောက်ပို့ပြီး"
                        ).forEach { (code, label) ->
                            FilterChip(
                                selected = cargo.status == code,
                                onClick = { viewModel.updateCargoStatus(cargo.id, code) },
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
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${cargo.receiverPhone}"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("လက်ခံသူ ဖုန်းခေါ်မည်")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCargoDetails = null }) {
                    Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
                }
            }
        )
    }

    // Claim Detail Dialog
    selectedClaimDetails?.let { claim ->
        AlertDialog(
            onDismissRequest = { selectedClaimDetails = null },
            containerColor = Color.White,
            title = { Text("ဘောက်ချာ: ${claim.trackingNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• ဖောက်သည်: ${claim.customerName} (${claim.customerPhone})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text("• ပျက်စီးမှု/ဆုံးရှုံးမှု: ${claim.damageDescription}", fontSize = 12.sp, color = TextPrimary)
                    Text("• တောင်းဆိုငွေ: ${currencyFormat.format(claim.claimAmount)} ကျပ်", fontSize = 12.sp, color = TextSecondary)
                    Text("• ပေးလျော်ပြီးငွေ: ${currencyFormat.format(claim.paidAmount)} ကျပ်", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BmkOrange)
                    Text("• အခြေအနေ: ${if (claim.status == "PAID") "လျော်ကြေး ပေးပြီး" else "စိစစ်နေဆဲ"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BmkNavy)
                }
            },
            confirmButton = {
                Button(onClick = { selectedClaimDetails = null }, colors = ButtonDefaults.buttonColors(containerColor = BmkNavy)) {
                    Text("ပိတ်မည်", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showClaimDialog) {
        AlertDialog(
            onDismissRequest = { showClaimDialog = false },
            containerColor = Color.White,
            title = { Text("ပျက်စီးမှု လျော်ကြေး စာရင်းသွင်းရန်", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BmkNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = trackingNo,
                        onValueChange = { trackingNo = it },
                        label = { Text("ဘောက်ချာ နံပါတ်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("ဖောက်သည် အမည်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("ဖုန်းနံပါတ်") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = damageDescription,
                        onValueChange = { damageDescription = it },
                        label = { Text("ပျက်စီးမှု အသေးစိတ်") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = claimAmountStr,
                            onValueChange = { claimAmountStr = it },
                            label = { Text("တောင်းဆိုငွေ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paidAmountStr,
                            onValueChange = { paidAmountStr = it },
                            label = { Text("ပေးလျော်ငွေ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val claim = claimAmountStr.toDoubleOrNull() ?: 0.0
                        val paid = paidAmountStr.toDoubleOrNull() ?: 0.0
                        viewModel.submitCompensationClaim(
                            cargoId = 0,
                            trackingNumber = trackingNo,
                            customerName = customerName,
                            customerPhone = customerPhone,
                            damageDescription = damageDescription,
                            claimAmount = claim,
                            paidAmount = paid,
                            date = "2026-10-25",
                            notes = notes,
                            onSuccess = {
                                showClaimDialog = false
                                Toast.makeText(context, "လျော်ကြေး စာရင်းသွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BmkOrange)
                ) {
                    Text("သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClaimDialog = false }) {
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
fun CompactKalayCargoListItem(
    cargo: CargoEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("kalay_cargo_${cargo.trackingNumber}"),
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
                    text = "ကလေးမြို့ လက်ခံသူ: ${cargo.receiverName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkNavy
                )
                Text(
                    text = "ဆိုင်: ${cargo.receiverShopAddress}",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    text = "ကုန်: ${cargo.cargoName.ifBlank { cargo.itemDescription }}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenBg else StatusAmberBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (cargo.status == "DELIVERED_TO_SHOP") "ဆိုင်ရောက်ပြီး" else "ကလေးဆိုက်",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (cargo.status == "DELIVERED_TO_SHOP") StatusGreenText else StatusAmberText,
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

@Composable
fun CompactClaimListItem(
    claim: CompensationEntity,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("claim_item_${claim.id}"),
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
                    text = "ဘောက်ချာ: ${claim.trackingNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkNavy
                )
                Text(
                    text = "ဖောက်သည်: ${claim.customerName} (${claim.customerPhone})",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${currencyFormat.format(claim.paidAmount)} ကျပ်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BmkOrange
                )
                Surface(
                    color = if (claim.status == "PAID") StatusGreenBg else StatusAmberBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (claim.status == "PAID") "ပေးပြီး" else "စိစစ်ဆဲ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (claim.status == "PAID") StatusGreenText else StatusAmberText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
