package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.SystemPrintHelper
import com.example.data.model.PrintedVoucherEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.BmkViewModel
import com.example.util.BluetoothPrinterManager
import com.example.util.ReceiptBitmapGenerator
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherSearchReprintDialog(
    viewModel: BmkViewModel,
    initialVoucherNumber: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    val templates by viewModel.allTemplates.collectAsState()
    val allVouchers by viewModel.allPrintedVouchers.collectAsState()

    var searchQuery by remember { mutableStateOf(initialVoucherNumber) }
    var selectedVoucher by remember { mutableStateOf<PrintedVoucherEntity?>(null) }

    // Bluetooth printer manager for reprinting
    val printerManager = remember { BluetoothPrinterManager(context) }
    val pairedDevices by printerManager.pairedDevices.collectAsState()
    var isReprinting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        printerManager.refreshPairedDevices()
        if (initialVoucherNumber.isNotBlank()) {
            val found = allVouchers.firstOrNull { it.voucherNumber.equals(initialVoucherNumber.trim(), ignoreCase = true) }
            if (found != null) {
                selectedVoucher = found
            }
        }
    }

    // Filtered vouchers based on search query
    val filteredVouchers = remember(searchQuery, allVouchers) {
        if (searchQuery.isBlank()) {
            allVouchers
        } else {
            allVouchers.filter {
                it.voucherNumber.contains(searchQuery.trim(), ignoreCase = true) ||
                it.customerName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.customerPhone.contains(searchQuery.trim(), ignoreCase = true) ||
                it.carNumber.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    // Selected voucher bitmap preview
    val voucherBitmap: Bitmap? = remember(selectedVoucher, templates) {
        val v = selectedVoucher ?: return@remember null
        val tmpl = templates.find { it.id == v.templateId } ?: templates.firstOrNull()
        if (tmpl != null) {
            ReceiptBitmapGenerator.fromPrintedVoucher(tmpl, v)
        } else {
            null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("ဘောင်ချာ နံပါတ်ဖြင့် ရှာဖွေခြင်း (Search Voucher)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BmkNavy)
                            Text("နံပါတ် အတိုင်း ရှာပါက ထုတ်ပေးခဲ့သော print page ပြန်လည် ထုတ်ပေးပါမည်", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorderColor)

                // Search Bar Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        // Auto-select if exact match
                        val exact = allVouchers.firstOrNull { v -> v.voucherNumber.equals(it.trim(), ignoreCase = true) }
                        if (exact != null) {
                            selectedVoucher = exact
                        }
                    },
                    placeholder = { Text("ဘောင်ချာနံပါတ် ရိုက်ထည့်ပါ (ဥပမာ: 0100, 0092, 1195...)") },
                    leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BmkNavy) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voucher_search_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BmkNavy,
                        unfocusedBorderColor = Color(0xFF94A3B8)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Content: List on left, Preview on right
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Left Column: Search Results List
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = BorderStroke(1.dp, CardBorderColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "ရှာတွေ့သော ဘောက်ချာများ (${filteredVouchers.size}) ခု:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (filteredVouchers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ဘောင်ချာ မတွေ့ရှိပါ", fontSize = 12.sp, color = TextMuted)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredVouchers) { voucher ->
                                        val isSelected = selectedVoucher?.id == voucher.id
                                        Surface(
                                            color = if (isSelected) BmkNavy else Color.White,
                                            border = BorderStroke(1.dp, if (isSelected) BmkNavy else CardBorderColor),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedVoucher = voucher }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            "No: ${voucher.voucherNumber}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = if (isSelected) Color.White else BmkOrange
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            color = if (isSelected) Color.White.copy(alpha = 0.2f) else SurfaceLight,
                                                            shape = RoundedCornerShape(4.dp)
                                                        ) {
                                                            Text(
                                                                voucher.issueDate,
                                                                fontSize = 9.sp,
                                                                color = if (isSelected) Color.White else TextMuted,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        voucher.customerName.ifBlank { "အမည် မသိ" },
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = if (isSelected) Color.White else TextPrimary
                                                    )
                                                    Text(
                                                        "${voucher.itemDescription} (${voucher.quantity} ခု, ${voucher.weightViss.toInt()} ပိဿာ)",
                                                        fontSize = 10.sp,
                                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary
                                                    )
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        "${currencyFormat.format(voucher.totalAmount)} Ks",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = if (isSelected) Color.White else BmkNavy
                                                    )
                                                    Text(
                                                        if (voucher.paymentStatus == "PAID") "ရှင်းပြီး" else "လ/ခ",
                                                        fontSize = 10.sp,
                                                        color = if (isSelected) Color.White else if (voucher.paymentStatus == "PAID") SuccessGreen else ErrorRed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right Column: Thermal Print Preview & Actions
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (selectedVoucher != null) "ဘောက်ချာ No. ${selectedVoucher?.voucherNumber} မူရင်းပုံစံ:" else "ဘောက်ချာအား ရွေးချယ်ပါ:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp))
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (voucherBitmap != null) {
                                Image(
                                    bitmap = voucherBitmap.asImageBitmap(),
                                    contentDescription = "Original Receipt Print",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("ဘောက်ချာ တစ်ခုအား ရွေးချယ်ပြီးပါက ယခုနေရာတွင် မူရင်း print စာရွက်ပုံစံ ပြသပါမည်", fontSize = 12.sp, color = TextMuted)
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (voucherBitmap != null && selectedVoucher != null) {
                                        try {
                                            SystemPrintHelper.printBitmap(context, "BMK-Reprint-${selectedVoucher!!.voucherNumber}", voucherBitmap)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = selectedVoucher != null,
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = BmkNavy, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PDF Export", fontSize = 11.sp, color = BmkNavy, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (voucherBitmap != null && selectedVoucher != null) {
                                        isReprinting = true
                                        coroutineScope.launch {
                                            // Connect to first paired device if not connected
                                            val firstDevice = pairedDevices.firstOrNull()
                                            if (firstDevice != null) {
                                                printerManager.connectToDevice(firstDevice)
                                            }
                                            val ok = printerManager.printBitmap(voucherBitmap)
                                            isReprinting = false
                                            if (ok) {
                                                Toast.makeText(context, "ဘောင်ချာ No. ${selectedVoucher!!.voucherNumber} အား ထပ်မံ Print ထုတ်ပြီးပါပြီ", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "Bluetooth Thermal Printer ချိတ်ဆက်မထားပါ", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                                enabled = selectedVoucher != null && !isReprinting,
                                modifier = Modifier.weight(1.3f).height(44.dp).testTag("reprint_voucher_btn")
                            ) {
                                if (isReprinting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ထပ်မံ Print ထုတ်မည်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
