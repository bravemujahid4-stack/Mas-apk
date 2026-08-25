package com.masaccounts.app.ui.screens.company

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.R
import com.masaccounts.app.data.local.entity.CompanyEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.MasTopAppBar
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySetupScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit,
    onSaved: () -> Unit = {}
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isAdmin = currentUser?.role == "ADMIN"
    val isLocked = company?.isLocked == true
    val canEdit = isAdmin && (!isLocked || isAdmin) // Admin can unlock/edit

    var name by remember(company) { mutableStateOf(company?.name ?: "") }
    var address by remember(company) { mutableStateOf(company?.address ?: "") }
    var city by remember(company) { mutableStateOf(company?.city ?: "") }
    var phone by remember(company) { mutableStateOf(company?.phone ?: "") }
    var email by remember(company) { mutableStateOf(company?.email ?: "") }
    var ntn by remember(company) { mutableStateOf(company?.ntn ?: "") }
    var strn by remember(company) { mutableStateOf(company?.strn ?: "") }
    var currency by remember(company) { mutableStateOf(company?.currency ?: "PKR") }
    var fiscalYear by remember(company) { mutableStateOf(company?.fiscalYear ?: "2026-2027") }
    var lockedState by remember(company) { mutableStateOf(company?.isLocked ?: false) }

    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Company Setup",
                company = company,
                user = currentUser,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Company Header Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mas_logo),
                        contentDescription = "MAS Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = company?.name ?: "No Company",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Fiscal Year: ${company?.fiscalYear ?: "2026-2027"} | ${company?.currency ?: "PKR"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = GoldLight)
                        )
                        if (isLocked) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AccountingRedLight,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "SETTINGS LOCKED",
                                    color = AccountingRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isAdmin) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GoldContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = GoldDark)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Only ADMIN users can modify company configurations.",
                            color = Slate800,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            Text(
                text = "Business Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )
            )

            OutlinedTextField(
                value = name,
                onValueChange = { if (isAdmin) name = it },
                label = { Text("Company Name *") },
                enabled = isAdmin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { if (isAdmin) address = it },
                label = { Text("Address") },
                enabled = isAdmin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { if (isAdmin) city = it },
                    label = { Text("City") },
                    enabled = isAdmin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { if (isAdmin) phone = it },
                    label = { Text("Phone") },
                    enabled = isAdmin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { if (isAdmin) email = it },
                label = { Text("Email") },
                enabled = isAdmin,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = ntn,
                    onValueChange = { if (isAdmin) ntn = it },
                    label = { Text("NTN") },
                    enabled = isAdmin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = strn,
                    onValueChange = { if (isAdmin) strn = it },
                    label = { Text("STRN") },
                    enabled = isAdmin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = { if (isAdmin) currency = it },
                    label = { Text("Currency") },
                    enabled = isAdmin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = fiscalYear,
                    onValueChange = { if (isAdmin) fiscalYear = it },
                    label = { Text("Fiscal Year") },
                    enabled = isAdmin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (isAdmin) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lock Company Settings",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "When locked, viewers & accountants cannot alter core company info.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                            )
                        }
                        Switch(
                            checked = lockedState,
                            onCheckedChange = { lockedState = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = NavyPrimary)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            viewModel.showMessage("Company Name cannot be empty.")
                            return@Button
                        }
                        val comp = company ?: return@Button
                        val user = currentUser ?: return@Button

                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val updated = comp.copy(
                                    name = name.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    ntn = ntn.trim(),
                                    strn = strn.trim(),
                                    currency = currency.trim().ifBlank { "PKR" },
                                    fiscalYear = fiscalYear.trim().ifBlank { "2026-2027" },
                                    isLocked = lockedState
                                )
                                viewModel.repository.updateCompany(updated, user)
                                viewModel.showMessage("Company settings updated successfully.")
                                onBackClick()
                            } catch (e: Exception) {
                                viewModel.showMessage("Error: ${e.localizedMessage}")
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_company_settings_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("SAVE COMPANY SETTINGS", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
