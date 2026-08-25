package com.masaccounts.app.ui.screens.settings

import androidx.compose.foundation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.data.auth.FirebaseAuthManager
import com.masaccounts.app.data.local.entity.UserEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit,
    onNavigateToCompanySetup: () -> Unit,
    onLogout: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("General", "Users & Roles", "Audit Trail", "About")

    var showAddUserDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Settings & Administration",
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NavyPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // General settings
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Company Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                                Text("Active Company: ${company?.name ?: "N/A"}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Currency: ${company?.currency ?: "PKR"}", fontSize = 12.sp, color = Slate600)
                                if (company?.address?.isNotBlank() == true) {
                                    Text("Address: ${company?.address}", fontSize = 12.sp, color = Slate600)
                                }
                                if (company?.city?.isNotBlank() == true) {
                                    Text("City: ${company?.city}", fontSize = 12.sp, color = Slate600)
                                }
                                if (company?.phone?.isNotBlank() == true) {
                                    Text("Phone: ${company?.phone}", fontSize = 12.sp, color = Slate600)
                                }

                                Button(
                                    onClick = onNavigateToCompanySetup,
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Company Profile")
                                }
                            }
                        }

                        // Security & session
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("User Session", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Logged in as: ${currentUser?.name ?: "User"}", fontWeight = FontWeight.SemiBold)
                                        Text("Role: ${currentUser?.role ?: "ADMIN"}", fontSize = 12.sp, color = GoldDark, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.logout()
                                            onLogout()
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccountingRed),
                                        border = BorderStroke(1.dp, AccountingRed)
                                    ) {
                                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Log Out")
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Users & Roles
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("System Users (${users.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                            if (currentUser?.role == "ADMIN") {
                                Button(
                                    onClick = { showAddUserDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add User", fontSize = 12.sp)
                                }
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(users) { u ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Slate200),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("Email / Username: ${u.email}", fontSize = 12.sp, color = Slate600)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = when (u.role) {
                                                "ADMIN" -> GoldContainer
                                                "ACCOUNTANT" -> AccountingBlue.copy(alpha = 0.12f)
                                                else -> Slate100
                                            }
                                        ) {
                                            Text(
                                                text = u.role,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = when (u.role) {
                                                    "ADMIN" -> GoldDark
                                                    "ACCOUNTANT" -> AccountingBlue
                                                    else -> Slate700
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Audit Trail
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text("Audit Trail & Action Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark, modifier = Modifier.padding(bottom = 10.dp))

                        if (auditLogs.isEmpty()) {
                            EmptyStateCard(
                                title = "No Audit Logs",
                                subtitle = "User activities such as creating invoices, posting vouchers, and master data edits will be logged here.",
                                icon = Icons.Default.History
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(auditLogs) { log ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                                                Text(formatDate(log.timestamp), fontSize = 10.sp, color = Slate400)
                                            }
                                            Text(log.details, fontSize = 12.sp, color = Slate700)
                                            Text("By: ${log.userName} (${log.userEmail})", fontSize = 11.sp, color = Slate500)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // About
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        MasAppLogo(size = 90.dp)
                        Text("MAS Accounts", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = NavyDark)
                        Text("Enterprise Mobile Accounting & Inventory", fontSize = 13.sp, color = Slate600)
                        Text("Version 2.0.0 (Native Android Build)", fontSize = 11.sp, color = Slate400)

                        Divider(color = Slate200)

                        Text(
                            text = "Engineered specifically for steel, iron, trading, and commission businesses. Features real-time FIFO stock valuation, double-entry general ledger posting, multi-currency support, multi-user role-based access control, and complete offline SQLite persistence.",
                            fontSize = 12.sp,
                            color = Slate600,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showAddUserDialog) {
        AddUserDialog(
            companyId = company?.id ?: 1L,
            onDismiss = { showAddUserDialog = false },
            onSave = { newUser ->
                val admin = currentUser ?: return@AddUserDialog
                coroutineScope.launch {
                    FirebaseAuthManager.createAccount(newUser.email, newUser.passwordHash)
                    viewModel.repository.createUser(newUser, admin)
                    viewModel.showMessage("User '${newUser.name}' added.")
                    showAddUserDialog = false
                }
            }
        )
    }
}

@Composable
fun AddUserDialog(
    companyId: Long,
    onDismiss: () -> Unit,
    onSave: (UserEntity) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("ACCOUNTANT") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add User", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error ?: "", color = AccountingRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email / Username *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Role:", fontSize = 12.sp, color = Slate600)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ADMIN", "ACCOUNTANT", "VIEWER").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isBlank() || name.isBlank() || password.isBlank()) {
                        error = "All fields are required."
                        return@Button
                    }
                    onSave(
                        UserEntity(
                            companyId = companyId,
                            name = name.trim(),
                            email = email.trim().lowercase(),
                            passwordHash = password.trim(),
                            role = role
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Save User")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
