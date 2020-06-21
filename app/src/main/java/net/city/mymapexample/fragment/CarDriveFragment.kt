package net.city.mymapexample.fragment

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.NavigationUiOptions
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import net.city.mymapexample.R
import net.city.mymapexample.databinding.CardriveFragmetBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class CarDriveFragment :Fragment(), OnMapReadyCallback, MapboxMap.OnMapClickListener,
    PermissionsListener {

    private lateinit var binding :CardriveFragmetBinding
    private lateinit var mapBox:MapboxMap
    private lateinit var locationComponent:LocationComponent
    private lateinit var permissionsManager: PermissionsManager
    private  var navigationMapRoute:NavigationMapRoute? = null
    private lateinit var currentRoute:DirectionsRoute

    private  val TAG = "CarDriveFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = CardriveFragmetBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        binding.buttonStart.setOnClickListener {

//            if (!::currentRoute::isInitialized.get()) return@setOnClickListener
            val options: NavigationLauncherOptions = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(true)
                .build()

            NavigationLauncher.startNavigation(requireActivity(), options)
            //startNavigation()

        }

    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mapBox = mapboxMap

        mapboxMap.setStyle(getString(R.string.navigation_guidance_day),object :Style.OnStyleLoaded{

            override fun onStyleLoaded(style: Style) {

                enableLocationComponent(style)

                mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LatLng(locationComponent.lastKnownLocation!!.latitude, locationComponent.lastKnownLocation!!.longitude),12.toDouble())
                )
                addDestinationIconSymbolLayer(style)

            }
        })

        mapboxMap.addOnMapClickListener(this)

    }


    fun addDestinationIconSymbolLayer(loadedMapStyle:Style){

        loadedMapStyle.addImage(
            "destination-icon-id",
            BitmapFactory.decodeResource(this.resources, R.drawable.mapbox_marker_icon_default)
        )
        val geoJsonSource = GeoJsonSource("destination-source-id")
        loadedMapStyle.addSource(geoJsonSource)

        val destinationSymbolLayer =
            SymbolLayer("destination-symbol-layer-id", "destination-source-id")

        destinationSymbolLayer.withProperties(
            iconImage("destination-icon-id"),
            iconAllowOverlap(true),
            iconIgnorePlacement(true)
        )
        loadedMapStyle.addLayer(destinationSymbolLayer)
    }

    override fun onMapClick(point: LatLng): Boolean {


        val destinationPoint = Point.fromLngLat(point.longitude,point.latitude)
        val originPoint = Point.fromLngLat(locationComponent.lastKnownLocation!!.longitude,
        locationComponent.lastKnownLocation!!.latitude)

        val source = mapBox.getStyle()!!.getSourceAs<GeoJsonSource>("destination-source-id")

        if (source != null){
            source.setGeoJson(Feature.fromGeometry(destinationPoint))
        }
        getRoute(originPoint, destinationPoint);
        binding.buttonStart.setEnabled(true);
        binding.buttonStart.setBackgroundResource(R.color.colorPrimary);

        return true
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {

        if (PermissionsManager.areLocationPermissionsGranted(requireContext())) {
            locationComponent = mapBox.getLocationComponent()
            locationComponent.activateLocationComponent(requireContext(), loadedMapStyle)

            locationComponent.setLocationComponentEnabled(true)

            locationComponent.setCameraMode(CameraMode.TRACKING)
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(requireActivity())
        }
    }


    private fun getRoute( origin: Point, destination: Point ) {

        NavigationRoute.builder(requireContext())
            .accessToken(Mapbox.getAccessToken()!!)
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse?>{

                override fun onResponse(call: Call<DirectionsResponse?>?, response: Response<DirectionsResponse?>) {

                    Log.d(TAG, "Response code: ${response.body()}")

                    if (response.body() == null || response.body()!!.routes().size < 1) return

                     currentRoute = response.body()!!.routes().get(0)

                    if (navigationMapRoute != null) {
                        navigationMapRoute!!.removeRoute()
                    } else {
                        navigationMapRoute = NavigationMapRoute(null,binding.mapView,mapBox,R.style.NavigationMapRoute)
                    }
                    navigationMapRoute!!.addRoute(currentRoute)
                }

              override  fun onFailure(call: Call<DirectionsResponse?>?, throwable: Throwable) {
                    Log.e(TAG, "Error: " + throwable.message)
                }
            })
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {

    }

    override fun onPermissionResult(granted: Boolean) {

    }

/*
    fun startNavigation(){

        val navigation = MapboxNavigation(requireContext(),getString(R.string.mapbox_access_token))

        navigation.addFasterRouteListener {
            navigation.startNavigation(it)
        }
    }*/


}