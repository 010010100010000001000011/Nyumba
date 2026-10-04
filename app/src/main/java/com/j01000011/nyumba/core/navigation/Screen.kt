package com.j01000011.nyumba.core.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object TenantDashboard : Screen("tenant_dashboard")
    object LandlordDashboard : Screen("landlord_dashboard")
    
    // Landlord specific
    object AddBoardingHouse : Screen("add_boarding_house")
    object EditBoardingHouse : Screen("edit_boarding_house/{houseId}") {
        fun createRoute(houseId: String) = "edit_boarding_house/$houseId"
    }
    object RoomManagement : Screen("room_management/{houseId}") {
        fun createRoute(houseId: String) = "room_management/$houseId"
    }
    object AddRoom : Screen("add_room/{houseId}") {
        fun createRoute(houseId: String) = "add_room/$houseId"
    }
    object EditRoom : Screen("edit_room/{roomId}") {
        fun createRoute(roomId: String) = "edit_room/$roomId"
    }
    object RoomRequests : Screen("room_requests/{houseId}") {
        fun createRoute(houseId: String) = "room_requests/$houseId"
    }
    object RecordCashPayment : Screen("record_cash_payment/{houseId}") {
        fun createRoute(houseId: String) = "record_cash_payment/$houseId"
    }
    object LandlordPayments : Screen("landlord_payments/{houseId}") {
        fun createRoute(houseId: String) = "landlord_payments/$houseId"
    }
    object HouseReceipts : Screen("house_receipts/{houseId}") {
        fun createRoute(houseId: String) = "house_receipts/$houseId"
    }
    object TenantManagement : Screen("tenant_management/{houseId}") {
        fun createRoute(houseId: String) = "tenant_management/$houseId"
    }
    object TenantDetails : Screen("tenant_details/{tenantId}") {
        fun createRoute(tenantId: String) = "tenant_details/$tenantId"
    }
    object HouseComplaints : Screen("house_complaints/{houseId}") {
        fun createRoute(houseId: String) = "house_complaints/$houseId"
    }
    object FinancialReport : Screen("financial_report/{houseId}") {
        fun createRoute(houseId: String) = "financial_report/$houseId"
    }
    object AuditLogs : Screen("audit_logs/{houseId}") {
        fun createRoute(houseId: String) = "audit_logs/$houseId"
    }
    object BroadcastNotification : Screen("broadcast_notification/{houseId}") {
        fun createRoute(houseId: String) = "broadcast_notification/$houseId"
    }
    object DirectNotification : Screen("direct_notification/{tenantId}") {
        fun createRoute(tenantId: String) = "direct_notification/$tenantId"
    }

    // Tenant specific
    object RoomDiscovery : Screen("room_discovery")
    object RoomDetails : Screen("room_details/{roomId}") {
        fun createRoute(roomId: String) = "room_details/$roomId"
    }
    object MyRoom : Screen("my_room")
    object RentDetails : Screen("rent_details")
    object MakePayment : Screen("make_payment")
    object PaymentHistory : Screen("payment_history")
    object MyReceipts : Screen("my_receipts")
    object MyComplaints : Screen("my_complaints")
    object SubmitComplaint : Screen("submit_complaint")

    // Common
    object Notifications : Screen("notifications")
    object ReceiptDetails : Screen("receipt_details/{receiptId}") {
        fun createRoute(receiptId: String) = "receipt_details/$receiptId"
    }
    object ComplaintDetails : Screen("complaint_details/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_details/$complaintId"
    }
    object Messages : Screen("messages")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object Profile : Screen("profile")
    object Gallery : Screen("gallery")
    object Settings : Screen("settings")
}
