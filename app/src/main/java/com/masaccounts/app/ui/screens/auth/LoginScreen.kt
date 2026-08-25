package com.masaccounts.app.ui.screens.auth

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.R
import com.masaccounts.app.data.auth.FirebaseAuthManager
import com.masaccounts.app.data.local.entity.CompanyEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.MasAppLogo
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MasViewModel,
    company: CompanyEntity? = null,
    onLoginSuccess: () -> Unit,
    onCreateNewCompanyClick: (() -> Unit)? = null
) {
    val activeComp by viewModel.activeCompany.collectAsState()
    val targetCompany = company ?: activeComp

    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("admin@masaccounts.com") }
    var password by remember { mutableStateOf("admin123") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
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
            // Header with MAS Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(NavyDark, NavyPrimary)
                        )
                    )
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MasAppLogo(size = 76.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "MAS Accounts",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = targetCompany?.name ?: "Accounting Portal",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = GoldLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Secure Business Sign-In",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate300)
                    )
                }
            }

            // Login Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Sign In to Your Account",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                )

                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccountingRedLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccountingRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = AccountingRed,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email / Username") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = NavyPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = NavyPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCreateNewCompanyClick != null) {
                        TextButton(onClick = onCreateNewCompanyClick) {
                            Text("+ Create Company", color = GoldDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    TextButton(onClick = { showForgotPasswordDialog = true }) {
                        Text("Forgot Password?", color = NavyPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both email and password."
                            return@Button
                        }
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val trimmedEmail = email.trim().lowercase()
                                val trimmedPassword = password.trim()
                                
                                // Attempt Firebase sign-in if connected
                                FirebaseAuthManager.signIn(trimmedEmail, trimmedPassword)

                                val compId = targetCompany?.id ?: 1L
                                val user = viewModel.repository.getUserByEmail(trimmedEmail, compId)
                                if (user == null) {
                                    val anyUser = viewModel.repository.getUserByEmailAnyCompany(trimmedEmail)
                                    if (anyUser != null) {
                                        if (anyUser.passwordHash == trimmedPassword) {
                                            viewModel.loginUser(anyUser)
                                            onLoginSuccess()
                                        } else {
                                            errorMessage = "Invalid password. Please try again."
                                        }
                                    } else {
                                        errorMessage = "User with this email was not found."
                                    }
                                } else {
                                    if (user.passwordHash == trimmedPassword) {
                                        viewModel.loginUser(user)
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = "Invalid password. Please try again."
                                    }
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Login failed."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sign_in_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = "LOGIN TO ${targetCompany?.name?.uppercase() ?: "MAS ACCOUNTS"}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Security & Roles Supported",
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• ADMIN: Full company management, accounting, reports & users\n• ACCOUNTANT: Daily sales, bills, vouchers & ledgers\n• VIEWER: Strict read-only audit & reporting view",
                            color = Slate600,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var newPassword by remember { mutableStateOf("") }
        var resetSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your email and a new password:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetSuccess) {
                        Text("Password updated! You can now log in.", color = AccountingGreen, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank() && newPassword.isNotBlank()) {
                            coroutineScope.launch {
                                val trimmedResetEmail = resetEmail.trim().lowercase()
                                val trimmedNewPassword = newPassword.trim()
                                FirebaseAuthManager.sendPasswordReset(trimmedResetEmail)
                                val compId = targetCompany?.id ?: 1L
                                val u = viewModel.repository.getUserByEmail(trimmedResetEmail, compId)
                                if (u != null) {
                                    viewModel.repository.updateUser(u.copy(passwordHash = trimmedNewPassword), u)
                                    resetSuccess = true
                                    password = trimmedNewPassword
                                } else {
                                    viewModel.showMessage("No local account matching that email. Firebase reset sent if registered.")
                                }
                            }
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
