package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LockPerson
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FlashDropCompactLogo
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
fun ShopkeeperInstructionsScreen(
    onNavigateToRegistration: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FlashDropCompactLogo(showText = false)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Shopkeeper Instructions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Verification, Operating Rules & Guidelines",
                                fontSize = 11.sp,
                                color = OrangeSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("shopkeeper_instructions_back_button")
                    ) {
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm)
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HERO OVERVIEW BANNER
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopkeeper_instructions_hero_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, OrangeSecondary.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(OrangeLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = OrangeSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = GreenLight
                                ) {
                                    Text(
                                        text = "OFFICIAL MERCHANT HANDBOOK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Welcome to FlashDrop Merchant Network",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Please read these instructions carefully before registering or operating your shop on FlashDrop. Following these guidelines ensures smooth verification, faster order settlements, and high customer trust across Mahoba.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )
                    }
                }

                // CRITICAL POLICY HIGHLIGHT: REAL-TIME PHOTO & SHOPKEEPER CHANGE RULE
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shopkeeper_photo_policy_notice_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.5.dp, Color(0xFFD97706))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Mandatory Real-Time Shopkeeper Photo Rule",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Step 2 Verification & Future Operator Change Policy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFFDE68A))
                        Spacer(modifier = Modifier.height(10.dp))

                        PolicyBulletPoint(
                            text = "During Step 2 of the 3-step verification process, you must capture a live, real-time photo of the shopkeeper — the actual person who will be operating the shop."
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        PolicyBulletPoint(
                            text = "Note: If the shopkeeper needs to be changed in the future, permission must first be obtained from the Admin; only then can the shopkeeper's photo be updated."
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        PolicyBulletPoint(
                            text = "Attempting to change the shop operator without prior Admin permission is not allowed for security and accountability."
                        )
                    }
                }

                // SECTION 1: 3-STEP VERIFICATION PROCESS
                InstructionSectionCard(
                    sectionNumber = "1",
                    title = "3-Step Shop Verification Process",
                    subtitle = "How to register and get your shop approved on FlashDrop",
                    icon = Icons.Default.VerifiedUser,
                    accentColor = OrangeSecondary,
                    accentBg = OrangeLight,
                    points = listOf(
                        "Step 1 — Store Details & Location: Enter your Shop Name, Owner/Operator Name, 10-digit Mobile Number, Category, Live GPS Address in Mahoba, and account password. (Note: GST Number is optional for Printing Press shops and mandatory for other categories.)",
                        "Step 2 — Real-Time Shopkeeper Photo & Documents: Capture a real-time camera photo of the shopkeeper who will operate the shop, and upload your Aadhaar Card (masked to last 4 digits), PAN Card, and GST Certificate (optional for Printing Press).",
                        "Step 3 — Unique Shop ID Generation & Admin Review: Generate your unique FlashDrop Shop ID (e.g., FD-SHOP-1001). Your application will be reviewed by the Main Admin before store activation."
                    )
                )

                // SECTION 2: CHANGING THE SHOPKEEPER IN THE FUTURE
                InstructionSectionCard(
                    sectionNumber = "2",
                    title = "Changing the Shopkeeper / Updating Photo",
                    subtitle = "Admin permission is compulsory before replacing the shop operator",
                    icon = Icons.Default.LockPerson,
                    accentColor = Color(0xFF6D28D9),
                    accentBg = Color(0xFFEDE9FE),
                    points = listOf(
                        "Locked Operator Identity: Once your real-time shopkeeper photo is verified in Step 2, it is locked to your shop profile.",
                        "How to Request a Change: If another family member, manager, or new shopkeeper will operate the shop in the future, go to 'Edit Shop Profile' and tap 'Request Admin Permission to Change Shopkeeper'.",
                        "Photo Update After Approval: Only after the Main Admin grants permission will the real-time camera option unlock to update the new shopkeeper's photo."
                    )
                )

                // SECTION 3: DAILY STORE TIMINGS & LIVE INVENTORY
                InstructionSectionCard(
                    sectionNumber = "3",
                    title = "Shop Timings & Live Stock Management",
                    subtitle = "Keep your catalog and store availability accurate",
                    icon = Icons.Default.Schedule,
                    accentColor = GreenPrimary,
                    accentBg = GreenLight,
                    points = listOf(
                        "Automatic Schedule or Manual Toggle: Set your daily Opening & Closing hours in the Shop Schedule tab so your shop opens and closes automatically on time.",
                        "Real-Time Product Stock: Immediately turn off the stock toggle for any item that is out of stock so customers only order available products.",
                        "Fair Pricing & Clear Photos: Upload clear photos of your products and maintain accurate MRP / selling prices."
                    )
                )

                // SECTION 4: ORDER PROCESSING & DELIVERY HANDOVER
                InstructionSectionCard(
                    sectionNumber = "4",
                    title = "Order Packing & Delivery Partner Handover",
                    subtitle = "Fast and accurate fulfillment for Mahoba customers",
                    icon = Icons.Default.LocalShipping,
                    accentColor = OrangeSecondary,
                    accentBg = OrangeLight,
                    points = listOf(
                        "Prompt Order Acceptance: Check incoming orders in your Shopkeeper Dashboard and start packing immediately.",
                        "Quality & Hygiene Check: Verify item expiry dates, packaging seal, and item count (or document print quality and page count for Printing Press orders).",
                        "Delivery Handover: Hand over the packed parcel to the assigned FlashDrop Delivery Partner and update the order status."
                    )
                )

                // SECTION 5: PAYOUTS, SETTLEMENTS & ADMIN ROLE ASSIGNMENT
                InstructionSectionCard(
                    sectionNumber = "5",
                    title = "Payouts, Settlements & Account Roles",
                    subtitle = "Transparent earnings and Admin-managed portal access",
                    icon = Icons.Default.AccountBalanceWallet,
                    accentColor = GreenPrimary,
                    accentBg = GreenLight,
                    points = listOf(
                        "Admin-Controlled Role Assignment: Shopkeeper and Delivery Partner roles are assigned and verified directly by the Main Admin.",
                        "Order Earnings & Settlements: Track every completed order, commission breakdown, and settled payout in the 'Earnings' tab of your Shopkeeper Dashboard.",
                        "Compliance: Any misuse, fake products, or unapproved operator changes may result in temporary shop suspension by the Admin."
                    )
                )

                // QUICK ACTION BUTTONS AT BOTTOM
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Ready to Get Started?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Text(
                            text = "You can start your 3-step shop registration now or sign in to an existing shopkeeper account.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onNavigateToRegistration,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("instructions_register_shop_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangeSecondary)
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Register Your Shop (3-Step Verification)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onNavigateToSignIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("instructions_signin_shopkeeper_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GreenPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Go to Shopkeeper Sign In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GreenPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PolicyBulletPoint(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFD97706),
            modifier = Modifier
                .padding(top = 2.dp)
                .size(15.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF78350F),
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun InstructionSectionCard(
    sectionNumber: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    accentBg: Color,
    points: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$sectionNumber. $title",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                points.forEach { point ->
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = point,
                            fontSize = 12.sp,
                            color = TextDark,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
