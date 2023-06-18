package com.tao.tencentmapchooseaddress

import android.os.Parcelable
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import kotlinx.parcelize.Parcelize

/**
 * @Author tangtao
 * @Description:
 * @Date: 2023/6/18 12:35 AM
 */

@Parcelize
data class PoiItem(
    val name: String,
    val address: String,
    val lng: LatLng,
    val isPoi: Boolean = false,
    val check: Boolean = false
) : Parcelable
