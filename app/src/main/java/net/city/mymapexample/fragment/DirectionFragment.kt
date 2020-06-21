package net.city.mymapexample.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import net.city.mymapexample.R
import net.city.mymapexample.databinding.DirectionFragmentBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class DirectionFragment :Fragment(), OnMapReadyCallback {

    private val TAG = "DirectionFragment"

    private val ROUTE_LAYER_ID = "route-layer-id"
    private val ROUTE_SOURCE_ID = "route-source-id"
    private val ICON_LAYER_ID = "icon-layer-id"
    private val ICON_SOURCE_ID = "icon-source-id"
    private val RED_PIN_ICON_ID = "red-pin-icon-id"
    private var _binding: DirectionFragmentBinding? = null
    private val binding get() = _binding!!

    private var mMap: MapboxMap? = null

    private var firstLacation: Point?= Point.fromLngLat(41.338616, 69.284353)
    private var secondLocation:Point? = Point.fromLngLat(41.323856,69.257316)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        _binding = DirectionFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.mapquestMapView.onCreate(savedInstanceState)
        binding.mapquestMapView.getMapAsync(this)


    }

    override fun onMapReady(mapboxMap: MapboxMap) {


        mMap = mapboxMap

        val bodomzor = com.mapbox.mapboxsdk.geometry.LatLng(41.338616, 69.284353)

        mMap?.addMarker(MarkerOptions().position(bodomzor))?.setTitle("Marker in Uzbekistan")

        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(bodomzor, 12.toDouble()))
        
        mapboxMap.setStyle(Style.MAPBOX_STREETS,object :Style.OnStyleLoaded{
            override fun onStyleLoaded(style: Style) {
                initSource(style)
                initLayers(style)
                getRoute(firstLacation!!,secondLocation!!,mapboxMap)
            }
        })


        mapboxMap.addOnMapClickListener {

            if (firstLacation == null) firstLacation = getLocation(it)
            else if (secondLocation == null) {
                secondLocation = getLocation(it)

                 getRoute(firstLacation!!,secondLocation!!,mapboxMap)
            }

            if (firstLacation != null && secondLocation != null) {

                firstLacation = getLocation(it)
                secondLocation = null
            }
            true
        }
    }

    fun getLocation(latLng: com.mapbox.mapboxsdk.geometry.LatLng) = Point.fromLngLat(latLng.longitude,latLng.latitude)



    private fun initSource(@NonNull loadedMapStyle: Style) {

        loadedMapStyle.addSource(GeoJsonSource(ROUTE_SOURCE_ID))
        val iconGeoJsonSource = GeoJsonSource(
            ICON_SOURCE_ID, FeatureCollection.fromFeatures(
                arrayOf(
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            firstLacation!!.longitude(),
                            firstLacation!!.latitude()
                        )
                    ),
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            secondLocation!!.longitude(),
                            secondLocation!!.latitude()
                        )
                    )
                )
            )
        )
        loadedMapStyle.addSource(iconGeoJsonSource)
    }

    private fun initLayers(loadedMapStyle: Style) {
        val routeLayer = LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID)

        // Add the LineLayer to the map. This layer will display the directions route.
        routeLayer.setProperties(
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
            lineWidth(5f),
            lineColor(Color.parseColor("#009688"))
        )
        loadedMapStyle.addLayer(routeLayer)

        // Add the red marker icon image to the map
        BitmapUtils.getBitmapFromDrawable(
            resources.getDrawable(R.drawable.mapbox_marker_icon_default)
        )?.let {
            loadedMapStyle.addImage(
                RED_PIN_ICON_ID, it
            )
        }
        // Add the red marker icon SymbolLayer to the map
        loadedMapStyle.addLayer(
            SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
                iconImage(RED_PIN_ICON_ID),
                iconIgnorePlacement(true),
                iconAllowOverlap(true),
                iconOffset(arrayOf(0f, -9f))
            )
        )
    }


    fun getRoute(orign:Point,destination:Point,mapBox: MapboxMap){

        val clinet = MapboxDirections.builder()
            
            .origin(orign)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .accessToken(getString(R.string.mapbox_access_token))
            .build()


        clinet.enqueueCall(object : Callback<DirectionsResponse>{
            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                t.printStackTrace()
                Log.d(TAG, "onFailure: $t")
            }

            override fun onResponse(call: Call<DirectionsResponse>,response: Response<DirectionsResponse> ) {

                Log.d(TAG, "onResponse: routes ${response.body()?.routes()}")
                if (response.body() != null){

                    if (!(response.body()!!.routes().size < 1)){

                        val currentRoute = response.body()!!.routes()[0]


                       // val directionsRouteFeature = Feature.fromGeometry(LineString.fromPolyline(currentRoute.geometry()!!, PRECISION_6))

                        mapBox.getStyle(object :Style.OnStyleLoaded {

                            override fun onStyleLoaded(style: Style) {
                                Log.d(TAG, "onStyleLoaded: $currentRoute")
                                val source: GeoJsonSource? = style.getSourceAs(ROUTE_SOURCE_ID)

                                Log.d(TAG, "onStyleLoaded: source $source ")

                                source?.setGeoJson(LineString.fromPolyline(currentRoute.geometry()!!, PRECISION_6));
                            }

                        })
                    }
                }
            }
        })
    }
}