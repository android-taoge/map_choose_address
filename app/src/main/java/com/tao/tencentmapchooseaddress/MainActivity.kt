package com.tao.tencentmapchooseaddress

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.tao.tencentmapchooseaddress.databinding.ActivityMainBinding
import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.tencentmap.mapsdk.maps.TencentMapInitializer

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mBinding.setVariable(BR.activity, this)
        /**
         * 设置用户是否同意隐私协议
         * 需要在初始化地图之前完成，传入true后才能正常使用地图功能
         * @param isAgree 是否同意隐私协议
         */
        TencentMapInitializer.setAgreePrivacy(true)
        TencentLocationManager.setUserAgreePrivacy(true)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            101
        )
    }

    companion object {
        const val CHOOSE_ADDRESS_CODE = 101
        const val CHOOSED_ADDRESS = "choosed_address"
    }

    fun chooseAddress() {
        Intent(this, MapActivity::class.java).also {
            startActivityForResult(it, CHOOSE_ADDRESS_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSE_ADDRESS_CODE && resultCode == RESULT_OK) {
            val choosePoi = data?.getParcelableExtra<PoiItem>(CHOOSED_ADDRESS)
            mBinding.tvAddress.text = choosePoi.toString()
        }
    }
}