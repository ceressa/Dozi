package com.bardino.dozi.core.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IlacItem(
    val ID: String,
    val barcode: String?,
    val ATC_code: String?,
    val Active_Ingredient: String?,
    val Product_Name: String,
    val Category_1: String?,
    val Category_2: String?,
    val Category_3: String?,
    val Category_4: String?,
    val Category_5: String?,
    val Description: String?
) : Parcelable
