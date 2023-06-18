package com.tao.tencentmapchooseaddress

import com.tencent.tencentmap.mapsdk.maps.model.TencentMapGestureListener

/**
 * @Author tangtao
 * @Description:
 * @Date: 2023/6/18 11:43 AM
 */
class TencentMapGestureSimpleListener(val onMapScroll: (Boolean) -> Unit) :
    TencentMapGestureListener {
    override fun onDoubleTap(p0: Float, p1: Float): Boolean {
        return false
    }

    override fun onSingleTap(p0: Float, p1: Float): Boolean {
        return false
    }

    override fun onFling(p0: Float, p1: Float): Boolean {
        return false
    }

    override fun onScroll(p0: Float, p1: Float): Boolean {
        onMapScroll(true)
        return false
    }

    override fun onLongPress(p0: Float, p1: Float): Boolean {
        return false
    }

    override fun onDown(p0: Float, p1: Float): Boolean {
        return false
    }

    override fun onUp(p0: Float, p1: Float): Boolean {
        return false
    }

    override fun onMapStable() {

    }
}

