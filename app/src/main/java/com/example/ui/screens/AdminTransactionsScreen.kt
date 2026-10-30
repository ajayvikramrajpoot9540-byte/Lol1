package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.data.repository.ApnaBazRepository
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.BorderLight
import com.example.ui.theme.GreenLight
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.OrangeLight
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTransactionsScreen(
    repository: ApnaBazRepository,
    onBack: () -> Unit
) {
    val transactions by repository.transactions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterTabs = listOf("All", "Online (UPI)", "COD", "Refunds")

    var inspectingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val filteredList = remember(transactions, searchQuery, selectedFilterIndex) {
        transactions.filter { txn ->
            val matchesFilter = when (selectedFilterIndex) {
                1 -> txn.paymentMethod.equals("UPI", ignoreCase = true)
                2 -> txn.paymentMethod.equals("COD", ignoreCase = true)
                3 -> txn.status.equals("Refunded", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    txn.orderNumber.contains(searchQuery, ignoreCase = true) ||
                    txn.providerTxnId.contains(searchQuery, ignoreCase = true) ||
                    txn.customerName.contains(searchQuery, ignoreCase = true) ||
                    txn.shopName.contains(searchQuery, ignoreCase = true) ||
                    txn.referenceId.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Admin Transaction History",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Mahoba Payments & Audit Ledger",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("admin_txn_search_input"),
                placeholder = {
                    Text("Search by Order ID, Txn ID, or Customer...", color = TextMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = GreenPrimary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderLight
                ),
                singleLine = true
            )

            // Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedFilterIndex,
                containerColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedFilterIndex]),
                        color = GreenPrimary
                    )
                }
            ) {
                filterTabs.forEachIndexed { index, label ->
                    val count = when (index) {
                        0 -> transactions.size
                        1 -> transactions.count { it.paymentMethod.equals("UPI", ignoreCase = true) }
                        2 -> transactions.count { it.paymentMethod.equals("COD", ignoreCase = true) }
                        3 -> transactions.count { it.status.equals("Refunded", ignoreCase = true) }
                        else -> 0
                    }
                    Tab(
                        selected = selectedFilterIndex == index,
                        onClick = { selectedFilterIndex = index },
                        text = {
                            Text(
                                text = "$label ($count)",
                                fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedFilterIndex == index) GreenPrimary else TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction Items
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions found",
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Try clearing search or changing filter criteria.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { txn ->
                        TransactionRowItem(
                            transaction = txn,
                            onClick = { inspectingTransaction = txn }
                        )
                    }
                }
            }
        }
    }

    // Inspection Modal Dialog
    inspectingTransaction?.let { txn ->
        AlertDialog(
            onDismissRequest = { inspectingTransaction = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = txn.orderNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    TransactionStatusPill(status = txn.status)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider()
                    DetailLine("Amount Payable", "₹${txn.amount.toInt()}", isHighlight = true)
                    DetailLine("Payment Method", txn.paymentMethod)
                    DetailLine("Customer", txn.customerName)
                    DetailLine("Shop", txn.shopName)
                    DetailLine("Transaction ID", txn.providerTxnId.ifBlank { "N/A" })
                    DetailLine("Reference ID", txn.referenceId.ifBlank { "N/A" })
                    DetailLine("Timestamp", txn.timestamp)

                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Audit Proof: ${txn.verificationProof}",
                                fontSize = 11.sp,
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { inspectingTransaction = null },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun TransactionRowItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("txn_row_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (transaction.paymentMethod == "UPI") GreenLight else OrangeLight
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transaction.paymentMethod == "UPI") Icons.Default.QrCode else Icons.Default.Payments,
                            contentDescription = null,
                            tint = if (transaction.paymentMethod == "UPI") GreenPrimary else OrangeSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = transaction.orderNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                        Text(
                            text = "${transaction.customerName} • ${transaction.shopName}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${transaction.amount.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = GreenPrimary
                    )
                    TransactionStatusPill(status = transaction.status)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = transaction.timestamp,
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = "Txn: ${transaction.providerTxnId.take(16)}",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TransactionStatusPill(status: String) {
    val (bg, fg) = when (status.lowercase()) {
        "paid" -> GreenLight to GreenPrimary
        "collected" -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        "pending" -> OrangeLight to OrangeSecondary
        "failed" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        "refunded" -> Color(0xFFEDE9FE) to Color(0xFF6D28D9)
        else -> Color(0xFFF1F5F9) to TextMuted
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isHighlight) GreenPrimary else TextDark
        )
    }
}
