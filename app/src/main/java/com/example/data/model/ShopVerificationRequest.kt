package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/**
 * Shop Verification Request model representing business applications submitted
 * through the 3-step registration and verification workflow.
 *
 * Supported statuses:
 * - DRAFT: Saved draft in progress
 * - SUBMITTED: Initial submission
 * - PENDING_REVIEW: Documents submitted and waiting for Admin review
 * - DOCUMENTS_REQUIRED: Admin reviewed and requested corrected documents
 * - APPROVED: Verified and approved by Admin
 * - REJECTED: Application declined by Admin with reason
 * - SUSPENDED: Shopkeeper account suspended
 */
@IgnoreExtraProperties
data class ShopVerificationRequest(
    @DocumentId val id: String = "",
    val shopId: String = "", // Assigned FD-SHOP-XXXX
    val shopName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val category: String = "Grocery",
    val address: String = "",
    val description: String = "",
    val gstNumber: String = "",
    val passwordHash: String = "",

    // Business Verification Documents & Real-Time Shopkeeper Photo
    val shopkeeperPhotoUri: String = "",  // Real-time photo of the shopkeeper operating the shop
    val aadhaarNumberMasked: String = "", // Masked format only (e.g. XXXX-XXXX-1234)
    val aadhaarDocUri: String = "",       // Content or storage URI for Aadhaar card photo
    val panNumber: String = "",           // 10-character statutory format
    val panDocUri: String = "",           // Content or storage URI for PAN card photo
    val gstCertificateDocUri: String = "",// Content or storage URI for GST certificate
    val gstCertificateGstin: String = "", // Matches GSTIN from Step 1

    // Status & Admin Review Metadata
    val status: String = STATUS_PENDING_REVIEW,
    val rejectionReason: String = "",
    val documentsRequiredNotes: String = "",
    val reviewedByAdminId: String = "",
    val reviewedByAdminName: String = "",
    val reviewedAtFormatted: String = "",
    val submittedAtFormatted: String = "",
    val stepCompleted: Int = 1,
    val isThreeStepRegistration: Boolean = true,

    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_SUBMITTED = "SUBMITTED"
        const val STATUS_PENDING_REVIEW = "PENDING_REVIEW"
        const val STATUS_DOCUMENTS_REQUIRED = "DOCUMENTS_REQUIRED"
        const val STATUS_APPROVED = "APPROVED"
        const val STATUS_REJECTED = "REJECTED"
        const val STATUS_SUSPENDED = "SUSPENDED"
    }

    val isPending: Boolean
        get() = status in listOf(STATUS_PENDING_REVIEW, STATUS_SUBMITTED, STATUS_DOCUMENTS_REQUIRED)

    val isApproved: Boolean
        get() = status == STATUS_APPROVED
}
