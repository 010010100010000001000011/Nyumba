package com.j01000011.nyumba

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.j01000011.nyumba.core.navigation.Screen
import com.j01000011.nyumba.domain.model.UserRole
import com.j01000011.nyumba.presentation.auth.LoginScreen
import com.j01000011.nyumba.presentation.auth.RegisterScreen
import com.j01000011.nyumba.presentation.landlord.*
import com.j01000011.nyumba.presentation.rooms.*
import com.j01000011.nyumba.presentation.splash.SplashScreen
import com.j01000011.nyumba.presentation.tenant.*
import com.j01000011.nyumba.presentation.notifications.*
import com.j01000011.nyumba.presentation.payments.*
import com.j01000011.nyumba.presentation.receipts.*
import com.j01000011.nyumba.presentation.complaints.*
import com.j01000011.nyumba.presentation.messages.*
import com.j01000011.nyumba.presentation.profile.*
import com.j01000011.nyumba.presentation.settings.*
import com.j01000011.nyumba.presentation.gallery.*
import com.j01000011.nyumba.ui.theme.NyumbaTheme
import com.j01000011.nyumba.core.util.ImageStorageService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var imageStorageService: ImageStorageService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NyumbaTheme {
                val context = LocalContext.current
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { _ -> }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    registerFcmToken()
                }

                NyumbaAppContent(imageStorageService)
            }
        }
    }

    private fun registerFcmToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("fcmToken", token)
            }
        }
    }
}

@Composable
fun NyumbaAppContent(imageStorageService: ImageStorageService) {
    val navController = rememberNavController()
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToTenantDashboard = {
                        navController.navigate(Screen.TenantDashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLandlordDashboard = {
                        navController.navigate(Screen.LandlordDashboard.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onLoginSuccess = { role ->
                        val route = when (role) {
                            UserRole.TENANT -> Screen.TenantDashboard.route
                            UserRole.LANDLORD -> Screen.LandlordDashboard.route
                        }
                        navController.navigate(route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onRegisterSuccess = { role ->
                        val route = when (role) {
                            UserRole.TENANT -> Screen.TenantDashboard.route
                            UserRole.LANDLORD -> Screen.LandlordDashboard.route
                        }
                        navController.navigate(route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.TenantDashboard.route) {
                TenantDashboardScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToRoomDiscovery = { navController.navigate(Screen.RoomDiscovery.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToRent = { navController.navigate(Screen.RentDetails.route) },
                    onNavigateToReceipts = { navController.navigate(Screen.MyReceipts.route) },
                    onNavigateToPaymentHistory = { navController.navigate(Screen.PaymentHistory.route) },
                    onNavigateToMyRoom = { navController.navigate(Screen.MyRoom.route) },
                    onNavigateToComplaints = { navController.navigate(Screen.MyComplaints.route) },
                    onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }
            composable(Screen.LandlordDashboard.route) {
                LandlordDashboardScreen(
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToRooms = { houseId: String -> navController.navigate(Screen.RoomManagement.createRoute(houseId)) },
                    onNavigateToAddBoardingHouse = { navController.navigate(Screen.AddBoardingHouse.route) },
                    onNavigateToEditHouse = { houseId: String -> navController.navigate(Screen.EditBoardingHouse.createRoute(houseId)) },
                    onNavigateToRoomRequests = { houseId: String -> navController.navigate(Screen.RoomRequests.createRoute(houseId)) },
                    onNavigateToNotifications = { navController.navigate(Screen.Notifications.route) },
                    onNavigateToPayments = { houseId: String -> navController.navigate(Screen.LandlordPayments.createRoute(houseId)) },
                    onNavigateToTenants = { houseId: String -> navController.navigate(Screen.TenantManagement.createRoute(houseId)) },
                    onNavigateToComplaints = { houseId: String -> navController.navigate(Screen.HouseComplaints.createRoute(houseId)) },
                    onNavigateToMessages = { navController.navigate(Screen.Messages.route) },
                    onNavigateToReports = { houseId: String -> navController.navigate(Screen.FinancialReport.createRoute(houseId)) },
                    onNavigateToAuditLogs = { houseId: String -> navController.navigate(Screen.AuditLogs.createRoute(houseId)) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onNavigateToBroadcast = { houseId: String -> navController.navigate(Screen.BroadcastNotification.createRoute(houseId)) },
                    onNavigateToHouseReceipts = { houseId: String -> navController.navigate(Screen.HouseReceipts.createRoute(houseId)) }
                )
            }
            
            composable(Screen.AddBoardingHouse.route) {
                AddBoardingHouseScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.EditBoardingHouse.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                EditBoardingHouseScreen(houseId = houseId, onNavigateBack = { navController.popBackStack() })
            }
            
            composable(
                route = Screen.RoomManagement.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                RoomManagementScreen(
                    houseId = houseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddRoom = { id: String -> navController.navigate(Screen.AddRoom.createRoute(id)) },
                    onNavigateToEditRoom = { roomId: String -> navController.navigate(Screen.EditRoom.createRoute(roomId)) },
                    onNavigateToRoomRequests = { id: String -> navController.navigate(Screen.RoomRequests.createRoute(id)) }
                )
            }

            composable(
                route = Screen.AddRoom.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                AddRoomScreen(houseId = houseId, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.EditRoom.route,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                EditRoomScreen(roomId = roomId, onNavigateBack = { navController.popBackStack() })
            }
            
            composable(
                route = Screen.RoomRequests.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                RoomRequestsScreen(houseId = houseId, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.RoomDiscovery.route) {
                RoomDiscoveryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRoomSelected = { roomId: String -> navController.navigate(Screen.RoomDetails.createRoute(roomId)) }
                )
            }

            composable(
                route = Screen.RoomDetails.route,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
                RoomDetailsScreen(
                    roomId = roomId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDashboard = {
                        navController.navigate(Screen.TenantDashboard.route) {
                            popUpTo(Screen.TenantDashboard.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationsScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.RentDetails.route) {
                RentDetailsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToMakePayment = { navController.navigate(Screen.MakePayment.route) }
                )
            }

            composable(Screen.MakePayment.route) {
                MakePaymentScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.LandlordPayments.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                LandlordPaymentsScreen(
                    houseId = houseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecordCash = { id: String -> navController.navigate(Screen.RecordCashPayment.createRoute(id)) }
                )
            }

            composable(
                route = Screen.RecordCashPayment.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                RecordCashPaymentScreen(houseId = houseId, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.TenantManagement.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                TenantManagementScreen(
                    boardingHouseId = houseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTenantDetails = { tenantId: String -> navController.navigate(Screen.TenantDetails.createRoute(tenantId)) }
                )
            }

            composable(
                route = Screen.TenantDetails.route,
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tenantId = backStackEntry.arguments?.getString("tenantId") ?: ""
                TenantDetailsScreen(
                    tenantId = tenantId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { convId: String -> navController.navigate(Screen.Chat.createRoute(convId)) },
                    onNavigateToDirectNotice = { id: String -> navController.navigate(Screen.DirectNotification.createRoute(id)) }
                )
            }

            composable(Screen.MyReceipts.route) {
                MyReceiptsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReceiptDetails = { receiptId: String -> navController.navigate(Screen.ReceiptDetails.createRoute(receiptId)) }
                )
            }

            composable(
                route = Screen.ReceiptDetails.route,
                arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
            ) { backStackEntry ->
                val receiptId = backStackEntry.arguments?.getString("receiptId") ?: ""
                ReceiptDetailsScreen(receiptId = receiptId, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.MyComplaints.route) {
                MyComplaintsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSubmit = { navController.navigate(Screen.SubmitComplaint.route) },
                    onNavigateToDetails = { id -> navController.navigate(Screen.ComplaintDetails.createRoute(id)) }
                )
            }

            composable(Screen.SubmitComplaint.route) {
                SubmitComplaintScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.HouseComplaints.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                HouseComplaintsScreen(
                    houseId = houseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetails = { id -> navController.navigate(Screen.ComplaintDetails.createRoute(id)) }
                )
            }

            composable(
                route = Screen.ComplaintDetails.route,
                arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
            ) { backStackEntry ->
                val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                ComplaintDetailsScreen(complaintId = complaintId, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Messages.route) {
                MessagesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToChat = { id -> navController.navigate(Screen.Chat.createRoute(id)) }
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
                ChatScreen(conversationId = conversationId, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.DirectNotification.route,
                arguments = listOf(navArgument("tenantId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tenantId = backStackEntry.arguments?.getString("tenantId") ?: ""
                DirectNotificationScreen(tenantId = tenantId, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.BroadcastNotification.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                BroadcastNotificationScreen(houseId = houseId, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.MyRoom.route) {
                MyRoomScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.FinancialReport.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                FinancialReportScreen(houseId = houseId, onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.AuditLogs.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                AuditLogScreen(houseId = houseId, onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToGallery = { navController.navigate(Screen.Gallery.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Gallery.route) {
                GalleryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    imageStorageService = imageStorageService
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.PaymentHistory.route) {
                PaymentHistoryScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(
                route = Screen.HouseReceipts.route,
                arguments = listOf(navArgument("houseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
                HouseReceiptsScreen(
                    houseId = houseId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReceiptDetails = { id -> navController.navigate(Screen.ReceiptDetails.createRoute(id)) }
                )
            }
        }
    }
}
