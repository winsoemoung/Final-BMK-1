package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiLogisticsViewModel"

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Success(val text: String) : AiUiState
    data class ParsedCargo(
        val cargoName: String,
        val cargoType: String, // "HEAVY" or "BULK"
        val quantity: Int,
        val packageUnit: String,
        val unitWeight: Double,
        val senderName: String,
        val senderPhone: String,
        val description: String
    ) : AiUiState
    data class Error(val message: String) : AiUiState
}

class AiLogisticsViewModel : ViewModel() {

    // Lazy initialization of Gemini model via Firebase AI Logic
    private val generalModel by lazy {
        Firebase.ai.generativeModel("gemini-3.8-flash")
    }

    // Structured JSON schema model for Cargo Entry parsing
    private val cargoSchema = Schema.obj(
        properties = mapOf(
            "cargoName" to Schema.string("Name of the cargo item in Burmese or English"),
            "cargoType" to Schema.enumeration(listOf("HEAVY", "BULK"), "HEAVY for ကုန်မာ, BULK for ကုန်ပွ"),
            "quantity" to Schema.integer("Number of packages or items"),
            "packageUnit" to Schema.string("Unit such as ဖာ, ခု, အိတ်, ပုံး, ထုပ်"),
            "unitWeight" to Schema.double("Weight per unit in Viss (ပိဿာ)"),
            "senderName" to Schema.string("Sender name if present"),
            "senderPhone" to Schema.string("Sender phone number if present"),
            "description" to Schema.string("Item details or fragile warnings")
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = cargoSchema
            }
        )
    }

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<Pair<Boolean, String>>>(
        listOf(
            false to "မင်္ဂလာပါ! BMK EXPRESS Car Gate AI အကူမှ ကြိုဆိုပါတယ်။ မန္တလေး - ကလေး ၁၂ ဘီး တန်ဆာခ၊ ခေါက်တိုကြေး၊ အလုပ်သမားခ နှင့် ကုန်ပစ္စည်း စာရင်းသွင်းခြင်းဆိုင်ရာများ မေးမြန်းနိုင်ပါတယ်။"
        )
    )
    val chatMessages: StateFlow<List<Pair<Boolean, String>>> = _chatMessages.asStateFlow()

    fun parseCargoText(inputPrompt: String) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val prompt = """
                    You are an intelligent BMK EXPRESS Car Gate logistics entry assistant for Mandalay-Kalaymyo 12-wheeler transport.
                    Extract cargo details from this input text: "$inputPrompt"
                    Ensure cargoType is "HEAVY" (for ကုန်မာ) or "BULK" (for ကုန်ပွ).
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    structuredModel.generateContent(prompt)
                }

                val jsonStr = response.text
                if (!jsonStr.isNullOrBlank()) {
                    val json = JSONObject(jsonStr)
                    val parsed = AiUiState.ParsedCargo(
                        cargoName = json.optString("cargoName", ""),
                        cargoType = json.optString("cargoType", "HEAVY"),
                        quantity = json.optInt("quantity", 1),
                        packageUnit = json.optString("packageUnit", "ဖာ"),
                        unitWeight = json.optDouble("unitWeight", 1.0),
                        senderName = json.optString("senderName", ""),
                        senderPhone = json.optString("senderPhone", ""),
                        description = json.optString("description", "")
                    )
                    _uiState.value = parsed
                } else {
                    _uiState.value = AiUiState.Error("AI တုံ့ပြန်မှု မရရှိပါ။ ကျေးဇူးပြု၍ ပြန်လည် ကြိုးစားပါ။")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Parsing timed out", e)
                _uiState.value = AiUiState.Error("ကြာချိန် စက္ကန့် ၆၀ ကျော်လွန်သွားပါသည်။ ပြန်လည် ကြိုးစားပါ။")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Cargo parsing failed", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "AI စာရင်းခွဲခြားမှု မအောင်မြင်ပါ။")
            }
        }
    }

    fun askAiAssistant(userMessage: String) {
        if (userMessage.isBlank()) return

        val currentList = _chatMessages.value.toMutableList()
        currentList.add(true to userMessage)
        _chatMessages.value = currentList
        _uiState.value = AiUiState.Loading

        viewModelScope.launch {
            try {
                val systemPrompt = """
                    You are BMK EXPRESS Car Gate AI Advisor for Mandalay-Kalaymyo 12-wheeler logistics.
                    Core Information:
                    - Freight Rate: 1,200 Kyats / Viss (ပိဿာ) between Mandalay and Kalaymyo.
                    - Mandalay Loading Labor Fee: 25 Kyats / Viss.
                    - Kalaymyo Unloading Labor Fee: 25 Kyats / Viss.
                    - Shuttle Pickup (ခေါက်တိုကြေး): Extra fee if customer cannot bring items directly to Mandalay Gate.
                    - Direct Shop Delivery in Kalaymyo is INCLUDED in the 12-wheeler freight price.
                    - Always reply politely in clear Burmese (Myanmar) language with professional formatting.

                    User Question: $userMessage
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    generalModel.generateContent(systemPrompt)
                }

                val aiReply = response.text ?: "တုံ့ပြန်ချက် ရေးသား၍ မရရှိပါ"
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(false to aiReply)
                _chatMessages.value = updatedList
                _uiState.value = AiUiState.Success(aiReply)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Assistant timed out", e)
                _uiState.value = AiUiState.Error("AI တုံ့ပြန်မှု ကြာချိန် လွန်သွားပါသည်။")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Assistant failed", e)
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "AI တုံ့ပြန်မှု မအောင်မြင်ပါ။")
            }
        }
    }

    fun generateViberReceipt(
        trackingNo: String,
        cargoName: String,
        senderName: String,
        senderPhone: String,
        receiverName: String,
        receiverPhone: String,
        totalViss: Double,
        freightFee: Double,
        laborFee: Double,
        shuttleFee: Double,
        totalAmount: Double
    ) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            try {
                val prompt = """
                    Generate a polite Burmese language Viber/SMS receipt message for BMK EXPRESS Car Gate customer.
                    Tracking No: $trackingNo
                    Cargo Name: $cargoName
                    Sender: $senderName ($senderPhone)
                    Receiver: $receiverName ($receiverPhone)
                    Total Weight: $totalViss Viss
                    Freight Fee (1200Ks/viss): $freightFee Ks
                    Mandalay Loading Labor (25Ks/viss): $laborFee Ks
                    Shuttle Pickup Fee: $shuttleFee Ks
                    TOTAL PAYABLE: $totalAmount Ks
                    
                    Format nicely with emojis and professional Myanmar text layout.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    generalModel.generateContent(prompt)
                }

                val receiptText = response.text ?: "ဘောက်ချာ စာသား ရေးသား၍ မရရှိပါ"
                _uiState.value = AiUiState.Success(receiptText)
            } catch (e: TimeoutCancellationException) {
                _uiState.value = AiUiState.Error("ဘောက်ချာ ရေးသားမှု ကြာချိန် လွန်သွားပါပြီ။")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = AiUiState.Error(e.localizedMessage ?: "ဘောက်ချာ ဖန်တီး၍ မရရှိပါ။")
            }
        }
    }

    fun resetState() {
        _uiState.value = AiUiState.Idle
    }
}
