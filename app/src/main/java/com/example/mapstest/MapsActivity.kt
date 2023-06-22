package com.example.mapstest

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.mapstest.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.lang.Exception


class MapsActivity : AppCompatActivity(), OnMapReadyCallback,GoogleMap.OnCameraMoveListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // 현재 위치를 가져오기 위한 변수
    lateinit var mLastLocation: Location // 위치 값을 가지고 있는 객체
    internal lateinit var mLocationRequest: LocationRequest // 위치 정보 요청의 매개변수를 저장하는
    private val REQUEST_PERMISSION_LOCATION = 10

    var latTemp : Double? = null
    var lngTemp : Double? = null
    lateinit var marker1: Marker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mLocationRequest =  LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        mMap = googleMap

        if (checkPermissionForLocation(this)) {
            startLocationUpdates()
        }
        mMap.setOnCameraMoveListener(this)


        val btn = findViewById<Button>(R.id.button)
        btn.setOnClickListener{
            val emul = LatLng(latTemp as Double, lngTemp as Double)
            mMap.clear()
            marker1 = mMap.addMarker(MarkerOptions().position(emul).title("place").draggable(true))!!
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(emul, 20F))
            //업데이트가 안된 것입니다.
        }

        val findBtn = findViewById<Button>(R.id.findBtn)
        val addressOutput = findViewById<TextView>(R.id.address)
        val addressInput = findViewById<TextView>(R.id.inputAddress)

        val btn2 = findViewById<Button>(R.id.button2)
        btn2.setOnClickListener{
            val text1 = findViewById<TextView>(R.id.showLatLng)
            text1.text = "lat lng is :" +marker1.position.latitude + ", "+marker1.position.longitude
        }

        findBtn.setOnClickListener{
            try{
                val tempAddress = getGeocodeFromAddress(addressInput.text.toString())
                addressOutput.text = tempAddress.getAddressLine(0)
                val latLng = LatLng(tempAddress.latitude,tempAddress.longitude)
                marker1.position = latLng
                val bound = LatLngBounds(
                    marker1.position,  // SW bounds
                    marker1.position // NE bounds
                )
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bound, 0))


                val text1 = findViewById<TextView>(R.id.showLatLng)
                text1.text = "lat lng is :" +marker1.position.latitude + ", "+marker1.position.longitude
            }
            catch(e: Exception) {
             }
        }

    }

    fun getGeocodeFromAddress(address: String): Address {
        val coder = Geocoder(this)
        val geocodedAddress: List<Address> = coder.getFromLocationName(address, 50)
        Log.d("GmapViewFragment", "Geocode from Address ${geocodedAddress}${geocodedAddress.get(0).longitude}")
        return geocodedAddress[0]
    }


    private fun startLocationUpdates() {
        //FusedLocationProviderClient의 인스턴스를 생성.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        // 기기의 위치에 관한 정기 업데이트를 요청하는 메서드 실행
        // 지정한 루퍼 스레드(Looper.myLooper())에서 콜백(mLocationCallback)으로 위치 업데이트를 요청
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // 시스템에서 받은 location 정보를 onLocationChanged()에 전달
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location) {
        mLastLocation = location
        latTemp = mLastLocation.latitude.toDouble() // 갱신 된 위도
        lngTemp = mLastLocation.longitude.toDouble() // 갱신 된 경도
    }

    private fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // 권한이 없으므로 권한 요청 알림 보내기
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }

    // 사용자에게 권한 요청 후 결과에 대한 처리 로직
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()

            } else {
                Log.d("ttt", "onRequestPermissionsResult() _ 권한 허용 거부")
                Toast.makeText(this, "권한이 없어 해당 기능을 실행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCameraMove() {
        var addressOutput = findViewById<TextView>(R.id.address)
//        marker1.position = mMap.cameraPosition.target
        addressOutput.text = mMap.cameraPosition.target.toString()

    }

}