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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.R
import com.masaccounts.app.data.auth.FirebaseAuthManager
import com.masaccounts.app.data.local.entity.CompanyEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompanyScreen(
    viewModel: MasViewModel,
    onCompanyCreated: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var companyName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var ntn by remember { mutableStateOf("") }
    var strn by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("PKR") }
    var fiscalYear by remember { mutableStateOf("2026-2027") }

    var adminName by remember { mutableStateOf("Administrator") }
    var adminEmail by remember { mutableStateOf("admin@masaccounts.com") }
    var adminPassword by remember { mutableStateOf("admin123") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Slate50
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Executive Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(NavyDark, NavyPrimary)
                        )
                    )
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mas_logo),
                        contentDescription = "MAS Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "MAS Accounts",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "CREATE YOUR COMPANY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = GoldLight,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configure your official business identity. All records, invoices & ledgers will be customized for your company.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate300),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Company Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccountingRedLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccountingRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = AccountingRed,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                Text(
                    text = "Company Profile",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = {
                        companyName = it
                        errorMessage = null
                    },
                    label = { Text("Company Name * (Required)") },
                    placeholder = { Text("e.g. Al-Madina Trading Co.") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = NavyPrimary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("company_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Business Address") },
                    placeholder = { Text("Street / Market / Plaza") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = NavyPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        placeholder = { Text("e.g. Lahore") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone / Cell") },
                        placeholder = { Text("0300-1234567") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Company Email") },
                    placeholder = { Text("info@company.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = ntn,
                        onValueChange = { ntn = it },
                        label = { Text("NTN (National Tax No)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = strn,
                        onValueChange = { strn = it },
                        label = { Text("STRN (Sales Tax No)") },
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
                        onValueChange = { currency = it },
                        label = { Text("Currency") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fiscalYear,
                        onValueChange = { fiscalYear = it },
                        label = { Text("Fiscal Year") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Slate200)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Admin Account Setup",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                )

                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text("Admin Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = adminEmail,
                    onValueChange = { adminEmail = it },
                    label = { Text("Admin Email (for login)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NavyPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = adminPassword,
                    onValueChange = { adminPassword = it },
                    label = { Text("Admin Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NavyPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (companyName.isBlank()) {
                            errorMessage = "Company Name is mandatory. Please enter a company name."
                            return@Button
                        }
                        if (adminEmail.isBlank() || adminPassword.isBlank()) {
                            errorMessage = "Admin Email and Password are required."
                            return@Button
                        }

                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val company = CompanyEntity(
                                    name = companyName.trim(),
                                    address = address.trim(),
                                    city = city.trim(),
                                    phone = phone.trim(),
                                    email = email.trim(),
                                    ntn = ntn.trim(),
                                    strn = strn.trim(),
                                    currency = currency.trim().ifBlank { "PKR" },
                                    fiscalYear = fiscalYear.trim().ifBlank { "2026-2027" }
                                )
                                val companyId = viewModel.repository.createCompany(
                                    company = company,
                                    adminName = adminName.trim().ifBlank { "Administrator" },
                                    adminEmail = adminEmail.trim().lowercase(),
                                    adminPassword = adminPassword.trim()
                                )

                                // Sync Admin credentials to Firebase Auth
                                FirebaseAuthManager.createAccount(adminEmail.trim().lowercase(), adminPassword.trim())

                                val user = viewModel.repository.getUserByEmail(adminEmail.trim().lowercase(), companyId)
                                if (user != null) {
                                    viewModel.loginUser(user)
                                }
                                viewModel.showMessage("Company '${company.name}' created successfully!")
                                onCompanyCreated()
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Failed to create company."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_company_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = "CREATE & INITIALIZE COMPANY",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
