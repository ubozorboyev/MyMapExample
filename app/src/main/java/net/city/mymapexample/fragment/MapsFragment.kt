package net.city.mymapexample.fragment

import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import net.city.mymapexample.R

class MapsFragment : Fragment() {

    private  val TAG = "MapsFragment"
    private lateinit var urel :String
    private lateinit var mMap: GoogleMap

    private val callback = OnMapReadyCallback { googleMap ->

        mMap = googleMap

        val bodomzor = LatLng(41.338616, 69.284353)
        val yourdirection = LatLng(41.323856,69.257316)

        urel = getURL(bodomzor,yourdirection)

        googleMap.addMarker(MarkerOptions().position(bodomzor).title("Marker in Uzbekistan"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bodomzor,16f))

        googleMap.addMarker(MarkerOptions().position(yourdirection).title("Your location"))

        polyLineDraw()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)


    }

    fun polyLineDraw(){
/*

        Executors.newSingleThreadExecutor().execute {
            val result = URL(urel).readText()
            Log.d(TAG, "polyLineDraw: $result ")
            val json: JSONObject = JSONObject(result)
            val layer = GeoJsonLayer(mMap,json)
            layer.addLayerToMap()

        }
*/

    }


    private fun getURL(from : LatLng, to : LatLng) : String {
        val origin = "origin=" + from.latitude + "," + from.longitude
        val dest = "destination=" + to.latitude + "," + to.longitude
        val sensor = "sensor=false"
        val key = "key${R.string.google_maps_key}"
        val params = "$origin&$dest&$sensor$key"
        Log.d(TAG, "getURL: $params")
        return "https://maps.googleapis.com/maps/api/directions/json?$params"
    }
}