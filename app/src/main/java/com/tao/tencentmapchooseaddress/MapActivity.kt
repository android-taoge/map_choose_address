package com.tao.tencentmapchooseaddress

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.databinding.ObservableArrayList
import com.tao.tencentmapchooseaddress.databinding.ActivityMapBinding
import com.tencent.lbssearch.TencentSearch
import com.tencent.lbssearch.httpresponse.HttpResponseListener
import com.tencent.lbssearch.`object`.param.Geo2AddressParam
import com.tencent.lbssearch.`object`.param.SuggestionParam
import com.tencent.lbssearch.`object`.result.Geo2AddressResultObject
import com.tencent.lbssearch.`object`.result.SuggestionResultObject
import com.tencent.map.geolocation.TencentLocation
import com.tencent.map.geolocation.TencentLocation.ERROR_OK
import com.tencent.map.geolocation.TencentLocationListener
import com.tencent.map.geolocation.TencentLocationManager
import com.tencent.map.geolocation.TencentLocationRequest
import com.tencent.tencentmap.mapsdk.maps.CameraUpdateFactory
import com.tencent.tencentmap.mapsdk.maps.TencentMap.OnCameraChangeListener
import com.tencent.tencentmap.mapsdk.maps.model.CameraPosition
import com.tencent.tencentmap.mapsdk.maps.model.LatLng
import me.tatarka.bindingcollectionadapter2.ItemBinding


class MapActivity : AppCompatActivity(), OnItemClickListener<PoiItem> {
    private lateinit var mBinding: ActivityMapBinding
    private lateinit var mLocationManager: TencentLocationManager
    private lateinit var mapCenterLng: LatLng
    private var cityName: String? = null
    private var needRegeo: Boolean = true
    private var clickSuggestItem: PoiItem? = null
    val itemSuggestBinding: ItemBinding<PoiItem> =
        ItemBinding.of<PoiItem?>(BR.poiItem, R.layout.item_map_poi).bindExtra(BR.itemClick, this)
    val suggestionResult = ObservableArrayList<PoiItem>()
    val poiResult = ObservableArrayList<PoiItem>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding =
            DataBindingUtil.setContentView<ActivityMapBinding?>(this, R.layout.activity_map).apply {
                setVariable(BR.activity, this@MapActivity)
            }
        mLocationManager = TencentLocationManager.getInstance(this)
        location()
        initListener()
    }

    private fun initListener() {
        with(mBinding) {
            etSearch.addTextChangedListener {
                if (it?.trim().isNullOrEmpty()) {
                    suggestionResult.clear()
                    return@addTextChangedListener
                }
                suggestion(it.toString())
            }

            mapview.map.apply {
                setOnCameraChangeListener(object : OnCameraChangeListener {
                    override fun onCameraChange(p0: CameraPosition?) {

                    }

                    override fun onCameraChangeFinished(p0: CameraPosition) {
                        if (needRegeo) reGeocoderPoi(p0.target)
                    }
                })

                setOnMapClickListener {

                    hideRvSuggest()
                }
                addTencentMapGestureListener(TencentMapGestureSimpleListener {
                    needRegeo = it
                })
                uiSettings.setGestureScaleByMapCenter(true)
            }
        }


    }


    /**
     * 发起一次定位
     */
    fun location() {
        val locationRequest = TencentLocationRequest.create()
            .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA)
        mLocationManager.requestSingleFreshLocation(
            locationRequest,
            object : TencentLocationListener {
                override fun onLocationChanged(p0: TencentLocation?, p1: Int, p2: String?) {
                    if (p1 == ERROR_OK) {
                        mapCenterLng = LatLng(p0?.latitude ?: 0.0, p0?.longitude ?: 0.0)
                        cityName = p0?.city
                        moveToMapCenter(mapCenterLng)
                    }
                }

                override fun onStatusUpdate(p0: String?, p1: Int, p2: String?) {

                }
            },
            mainLooper
        )


    }

    private fun moveToMapCenter(latLng: LatLng) {
        mapCenterLng = latLng
        val cameraPosition = CameraUpdateFactory.newCameraPosition(
            CameraPosition(
                latLng,
                15f,
                0f,
                0f
            )
        )
        mBinding.mapview.map.moveCamera(cameraPosition)
    }


    /**
     * 逆地理编码 检索poi
     */
    private fun reGeocoderPoi(latLng: LatLng) {
        if (poiResult.isNotEmpty()) poiResult.clear()

        val waitInsertItem = clickSuggestItem
        clickSuggestItem = null

        val tencentSearch = TencentSearch(this)
        //还可以传入其他坐标系的坐标，不过需要用coord_type()指明所用类型
        //这里设置返回周边poi列表，可以在一定程度上满足用户获取指定坐标周边poi的需求
        val geo2AddressParam = Geo2AddressParam(latLng).getPoi(true)
            .setPoiOptions(
                Geo2AddressParam.PoiOptions()
                    .setRadius(500)
                    .setPolicy(Geo2AddressParam.PoiOptions.POLICY_O2O)
            )
        tencentSearch.geo2address(
            geo2AddressParam,
            object : HttpResponseListener<Geo2AddressResultObject> {
                override fun onSuccess(arg0: Int, arg1: Geo2AddressResultObject?) {
                    if (arg1 == null) {
                        return
                    }
                    val poiResults = arg1.result.pois.mapIndexed { index, poi ->
                        PoiItem(
                            poi.title,
                            poi.address,
                            poi.latLng,
                            isPoi = true,
                        )
                    } as ArrayList

                    if (waitInsertItem != null) {
                        val sameItem = poiResults.find {
                            it.lng.latitude == waitInsertItem.lng.latitude
                                    && it.lng.longitude == waitInsertItem.lng.longitude
                                    && it.name == waitInsertItem.name

                        }
                        if (sameItem == null) {
                            poiResults.add(0, waitInsertItem.copy(check = true))
                        } else {
                            val checkIndex = poiResults.indexOf(sameItem)
                            poiResults[checkIndex] = sameItem.copy(check = true)
                        }
                    } else {
                        poiResults[0] = poiResults[0].copy(check = true)
                    }

                    poiResult.addAll(poiResults)
                }

                override fun onFailure(arg0: Int, arg1: String, arg2: Throwable) {
                    // Log.e("test", "error code:$arg0, msg:$arg1")
                }
            })
    }


    /**
     * 关键字提示
     * @param keyword
     */
    private fun suggestion(keyword: String) {
        if (keyword.trim().isEmpty()) {
            mBinding.fmSearch.visibility = View.GONE
            return
        }
        val tencentSearch = TencentSearch(this)
        val suggestionParam = SuggestionParam(keyword, cityName).regionFix(true)
        //suggestion也提供了filter()方法和region方法
        //具体说明见文档，或者官网的webservice对应接口
        tencentSearch.suggestion(
            suggestionParam,
            object : HttpResponseListener<SuggestionResultObject?> {
                override fun onSuccess(arg0: Int, arg1: SuggestionResultObject?) {
                    if (arg1 == null ||
                        mBinding.etSearch.text.toString().trim().length === 0
                    ) {
                        return
                    }
                    val results = arg1.data.map {
                        PoiItem(it.title, it.address, it.latLng)
                    }
                    suggestionResult.addAll(results)
                }

                override fun onFailure(arg0: Int, arg1: String, arg2: Throwable?) {
                    Log.e("test", "error code:$arg0, msg:$arg1")
                }
            })
    }

    fun hideRvSuggest() {
        hideInput()
        mBinding.fmSearch.isVisible = false
    }


    private fun hideInput() {
        mBinding.etSearch.setText("")
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (imm.isActive) {
            imm.hideSoftInputFromWindow(
                window.decorView.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    override fun onStart() {
        super.onStart()
        mBinding.mapview.onStart()
    }

    override fun onResume() {
        super.onResume()
        mBinding.mapview.onResume()
    }

    override fun onPause() {
        super.onPause()
        mBinding.mapview.onPause()
    }

    override fun onStop() {
        mBinding.mapview.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mBinding.mapview.onDestroy()
        super.onDestroy()
    }


    override fun onItemClick(item: PoiItem) {
        if (item.isPoi) {
            val oldCheckItem = poiResult.find { it.check }
            val oldCheckIndex = poiResult.indexOf(oldCheckItem)
            val newCheckIndex = poiResult.indexOf(item)
            val newCheckItem = item.copy(check = true)

            poiResult[oldCheckIndex] = oldCheckItem?.copy(check = false)
            poiResult[newCheckIndex] = newCheckItem
            hideRvSuggest()
            needRegeo = false


        } else {
            clickSuggestItem = item
            needRegeo = true
            hideRvSuggest()
        }
        if (item.lng.latitude != mapCenterLng.latitude || item.lng.longitude != mapCenterLng.longitude) {
            moveToMapCenter(item.lng)
        }

    }

    fun setAddressResult() {
        val choosePoi = poiResult.find { it.check }
        val intent = Intent().apply {
            putExtra(MainActivity.CHOOSED_ADDRESS, choosePoi)
        }
        setResult(RESULT_OK, intent)
        finish()

    }
}