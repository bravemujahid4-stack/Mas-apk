package com.masaccounts.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.R
import com.masaccounts.app.data.local.entity.CompanyEntity
import com.masaccounts.app.data.local.entity.UserEntity
import com.masaccounts.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatCurrency(amount: Double, currency: String = "PKR"): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "$currency ${formatter.format(amount)}"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun MasAppLogo(
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_mas_logo),
        contentDescription = "MAS Accounts Logo",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasTopAppBar(
    title: String,
    company: CompanyEntity?,
    user: UserEntity?,
    onMenuClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = NavyDark,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MasAppLogo(size = 36.dp)
                        Column {
                            Text(
                                text = if (company != null && company.name.isNotBlank()) company.name else "MAS Accounts",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = GoldLight,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick, modifier = Modifier.testTag("top_bar_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    } else if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick, modifier = Modifier.testTag("top_bar_menu_button")) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    if (user != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when (user.role) {
                                "ADMIN" -> GoldPrimary.copy(alpha = 0.2f)
                                "ACCOUNTANT" -> AccountingBlueLight.copy(alpha = 0.2f)
                                else -> Slate300.copy(alpha = 0.2f)
                            },
                            border = BorderStroke(1.dp, GoldLight.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = user.role,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GoldLight
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    actions()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    }
}

@Composable
fun MasBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = NavyDark,
        contentColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        val items = listOf(
            Triple("dashboard", "Dashboard", Icons.Default.Dashboard),
            Triple("customers", "Customers", Icons.Default.People),
            Triple("sales", "Sales", Icons.Default.PointOfSale),
            Triple("purchases", "Purchases", Icons.Default.ShoppingBag),
            Triple("suppliers", "Suppliers", Icons.Default.LocalShipping)
        )

        items.forEach { (route, label, icon) ->
            val selected = currentRoute == route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) NavyDark else Slate300
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) GoldLight else Slate300,
                            fontSize = 10.sp
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = GoldLight,
                    selectedIconColor = NavyDark,
                    unselectedIconColor = Slate300
                ),
                modifier = Modifier.testTag("nav_item_$route")
            )
        }
    }
}

@Composable
fun MasBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    MasBottomNavigation(currentRoute, onNavigate)
}

@Composable
fun MetricCard(
    title: String,
    amount: Double,
    currency: String = "PKR",
    icon: ImageVector,
    accentColor: Color,
    bgColor: Color = Color.White,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate600,
                        fontWeight = FontWeight.Medium
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatCurrency(amount, currency),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        fontSize = 17.sp
                    )
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate600),
                textAlign = TextAlign.Center
            )
            if (actionButtonText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(actionButtonText, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
