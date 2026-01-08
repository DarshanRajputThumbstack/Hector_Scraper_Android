package com.hectorscraper.app.api

object ApiConstant {
    const val BOARD = "board"

    const val PG_LoginUrl = "v1/userLogin"
    const val PG_Verify_Otp = "v1/verifyOtp"
    const val PG_Register = "v1/registerUser"
    const val PG_Job_Category = "v1/listJobCategoryMaster"
    const val PG_Job_List_Category = "v1/listJobByCategory/{jobCategoryId}"
    const val PG_Apply_Job = "v1/applyJob"
    const val PG_Applied_Job = "v1/listUserAppliedJob"
    const val PG_Save_Job_List = "v1/listSavedUserJob"
    const val PG_Save_Job = "v1/saveUserJob"
}