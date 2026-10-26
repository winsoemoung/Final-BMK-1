package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiLogisticsViewModel
import com.example.ui.viewmodel.AiUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantDialog(
    aiViewModel: AiLogisticsViewModel,
    onDismiss: () -> Unit,
    onAutoFillCargo: ((AiUiState.ParsedCargo) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by aiViewModel.uiState.collectAsState()
    val chatMessages by aiViewModel.chatMessages.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("AI အမေး/အဖြေ", "AI စာရင်းခွဲခြားရန်", "Viber ဘောက်ချာ")

    var userQuery by remember { mutableStateOf("") }
    var smartTextPrompt by remember { mutableStateOf("ကြွေပြား ၅၀ ဖာ၊ အလေးချိန် ၁ ဖာ ၁၅ ပိဿာ၊ မှန်ကွဲနိုင်ပါတယ်၊ မန္တလေးမြို့မှ ဦးကျော် 091234567၊ ကလေးမြို့မှ ဒေါ်လှ 097654321") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI",
                        tint = BmkOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "BMK AI Smart Assistant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BmkNavy
                        )
                        Text(
                            text = "Powered by Gemini 3.8 Flash API",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SurfaceLight,
                    contentColor = BmkNavy
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) BmkNavy else TextSecondary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // AI Q&A Assistant Chat
                        Column(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(SurfaceLight, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chatMessages) { (isUser, message) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                    ) {
                                        Surface(
                                            color = if (isUser) BmkNavy else CardBackground,
                                            contentColor = if (isUser) Color.White else TextPrimary,
                                            shape = RoundedCornerShape(12.dp),
                                            border = if (isUser) null else BorderStroke(1.dp, CardBorderColor),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.widthIn(max = 240.dp)
                                        ) {
                                            Text(
                                                text = message,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(10.dp),
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }

                            if (uiState is AiUiState.Loading) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = BmkOrange
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = userQuery,
                                    onValueChange = { userQuery = it },
                                    placeholder = { Text("၁၂ ဘီး တန်ဆာခ၊ ခေါက်တို မေးရန်...", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("ai_query_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BmkNavy,
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        if (userQuery.isNotBlank()) {
                                            aiViewModel.askAiAssistant(userQuery)
                                            userQuery = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .background(BmkOrange, RoundedCornerShape(8.dp))
                                        .testTag("ai_send_btn")
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                                }
                            }
                        }
                    }

                    1 -> {
                        // AI Cargo Text Parser / Auto-Fill
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ဖောက်သည် ပေးပို့သော စာသား သို့မဟုတ် မှတ်စုကို ရိုက်ထည့်ပါ (Gemini AI မှ ကုန်အမည်၊ အလေးချိန်၊ ပို့သူ/လက်ခံသူ အလိုအလျောက် ခွဲခြားပေးမည်):",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )

                            OutlinedTextField(
                                value = smartTextPrompt,
                                onValueChange = { smartTextPrompt = it },
                                label = { Text("ကုန်ပစ္စည်း မှတ်စု စာသား") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("ai_smart_text_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BmkNavy,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            Button(
                                onClick = { aiViewModel.parseCargoText(smartTextPrompt) },
                                colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                                modifier = Modifier.fillMaxWidth().testTag("ai_parse_cargo_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI ဖြင့် စာရင်းခွဲခြားမည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            when (val state = uiState) {
                                is AiUiState.Loading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BmkOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Gemini AI မှ စိစစ်နေပါသည်...", fontSize = 12.sp, color = BmkNavy)
                                    }
                                }
                                is AiUiState.ParsedCargo -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                        border = BorderStroke(1.dp, CardBorderColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("ခွဲခြားရရှိသော အချက်အလက်များ:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BmkNavy)
                                            Text("• ကုန်အမည်: ${state.cargoName}", fontSize = 11.sp)
                                            Text("• အမျိုးအစား: ${if (state.cargoType == "HEAVY") "ကုန်မာ" else "ကုန်ပွ"}", fontSize = 11.sp)
                                            Text("• အရေအတွက်: ${state.quantity} ${state.packageUnit}", fontSize = 11.sp)
                                            Text("• တစ်ခု အလေးချိန်: ${state.unitWeight} ပိဿာ", fontSize = 11.sp)
                                            if (state.senderName.isNotBlank()) Text("• ပို့သူ: ${state.senderName} (${state.senderPhone})", fontSize = 11.sp)

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = {
                                                    onAutoFillCargo?.invoke(state)
                                                    onDismiss()
                                                    Toast.makeText(context, "ကုန်လက်ခံလွှာတွင် အလိုအလျောက် ဖြည့်သွင်းပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("ကုန်လက်ခံလွှာတွင် အလိုအလျောက် ဖြည့်သွင်းမည်", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                is AiUiState.Error -> {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                else -> {}
                            }
                        }
                    }

                    2 -> {
                        // Viber Receipt Generator
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "BMK ဖောက်သည်များထံ ပေးပို့ရန် Viber/SMS ဘောက်ချာ စာသားကို Gemini AI မှ ဖန်တီးပေးမည်:",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Button(
                                onClick = {
                                    aiViewModel.generateViberReceipt(
                                        trackingNo = "BMK-20261026-99",
                                        cargoName = "ကြွေပြား",
                                        senderName = "ဦးကျော်",
                                        senderPhone = "0945001122",
                                        receiverName = "ဒေါ်လှ",
                                        receiverPhone = "0979003344",
                                        totalViss = 160.0,
                                        freightFee = 192000.0,
                                        laborFee = 4000.0,
                                        shuttleFee = 5000.0,
                                        totalAmount = 201000.0
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F3CD7)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("generate_viber_receipt_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("နမူနာ Viber ဘောက်ချာ စာသား ဖန်တီးမည်", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            when (val state = uiState) {
                                is AiUiState.Loading -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BmkOrange)
                                    }
                                }
                                is AiUiState.Success -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                        border = BorderStroke(1.dp, CardBorderColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = state.text,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Button(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("BMK Receipt", state.text)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "ဘောက်ချာ စာသား ကူးယူပြီးပါပြီ (Copied)", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BmkOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("စာသား ကူးယူမည် (Copy Text)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                is AiUiState.Error -> {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ပိတ်မည်", fontWeight = FontWeight.Bold, color = TextSecondary)
            }
        }
    )
}
