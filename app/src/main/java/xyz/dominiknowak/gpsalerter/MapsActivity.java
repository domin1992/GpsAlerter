package xyz.dominiknowak.gpsalerter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    public double finishLat = 51.7958859;
    public double finishLon = 19.4897299;

    public int finishDistance = 500;

    public boolean destinationSelected = false;
    public boolean userAlerted = false;

    public Circle destinationCircle;

    public Marker mainMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.address:
                setAddress();
                return true;
            case R.id.distance:
                setDistance();
                return true;
            case R.id.removeMarkers:
                if(mainMarker != null)
                    mainMarker.remove();
                if(destinationCircle != null)
                    destinationCircle.remove();
                destinationSelected = false;
                userAlerted = false;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        double lat = 51.7706569;
        double lon = 19.4498972;

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                calcUserDistance(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
                Context context = getApplicationContext();
                CharSequence text = "Włącz odbiornik GPS";
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        };

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);

        LatLng current = new LatLng(lat, lon);
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if(mainMarker != null)
                    mainMarker.remove();
                if(destinationCircle != null)
                    destinationCircle.remove();
                destinationSelected = false;
                userAlerted = false;
                setMarker(latLng.latitude, latLng.longitude);
            }
        });
    }

    public void setAddress(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_address, null);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EditText etAddress = (EditText)dialogView.findViewById(R.id.address);
                if(etAddress != null && !etAddress.getText().toString().isEmpty()) {
                    try {
                        showResults(etAddress.getText().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void showResults(String userAddress) throws IOException {
        Geocoder geocoder = new Geocoder(getApplicationContext());
        final List<Address> addresses;
        addresses = geocoder.getFromLocationName(userAddress, 3);
        if(addresses.size() > 0){
            List<String> addressesArrayDisplay = new ArrayList<String>();
            for(int i = 0; i < addresses.size(); i++){
                String lines = "";
                for(int j = 0; j < addresses.get(i).getMaxAddressLineIndex(); j++){
                    if(j == 0)
                        lines += addresses.get(i).getAddressLine(j).toString();
                    else
                        lines += ", " + addresses.get(i).getAddressLine(j).toString();
                }
                addressesArrayDisplay.add(i, lines);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.dialog_address_pick, null);
            builder.setView(dialogView);
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.dismiss();
                }
            });

            ListView lvAddresses = (ListView) dialogView.findViewById(R.id.lvAddresses);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, addressesArrayDisplay);

            lvAddresses.setAdapter(arrayAdapter);

            final AlertDialog dialog = builder.create();
            dialog.show();

            lvAddresses.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(mainMarker != null)
                        mainMarker.remove();
                    if(destinationCircle != null)
                        destinationCircle.remove();
                    destinationSelected = false;
                    userAlerted = false;
                    setMarker(addresses.get(position).getLatitude(), addresses.get(position).getLongitude());
                    dialog.dismiss();
                }
            });

        }
        else{
            Context context = getApplicationContext();
            CharSequence text = "Nie znaleziono żadnych wyników";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
    }

    public void setDistance(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_distance, null);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                EditText etDistance = (EditText)dialogView.findViewById(R.id.etDistance);
                if(etDistance != null && !etDistance.getText().toString().isEmpty()) {
                    finishDistance = Integer.parseInt(etDistance.getText().toString());
                    distanceChanged();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void distanceChanged(){
        destinationCircle.setRadius(finishDistance);
    }

    public void setMarker(double lat, double lon){
        LatLng destinationCoords = new LatLng(lat, lon);
        mainMarker = mMap.addMarker(new MarkerOptions()
                .position(destinationCoords)
                .title("Miejsce docelowe")
        );
        mMap.moveCamera(CameraUpdateFactory.newLatLng(destinationCoords));

        CircleOptions circleOptions = new CircleOptions()
                .center(destinationCoords)
                .radius(finishDistance)
                .fillColor(Color.argb(128, 63, 81, 181))
                .strokeWidth(0);

        destinationCircle = mMap.addCircle(circleOptions);
        destinationSelected = true;
        userAlerted = false;
        finishLat = lat;
        finishLon = lon;
    }

    public void calcUserDistance(Location location){
        if(finishLat != 0 && finishLon != 0) {
            Location loc1 = new Location("");
            loc1.setLatitude(location.getLatitude());
            loc1.setLongitude(location.getLongitude());

            Location loc2 = new Location("");
            loc2.setLatitude(finishLat);
            loc2.setLongitude(finishLon);

            if(loc1.distanceTo(loc2) <= finishDistance && !userAlerted && destinationSelected){
                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Context context = getApplicationContext();
                CharSequence text = "Do celu pozostaĹ‚o " + finishDistance + " metrów";
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
                userAlerted = true;
            }
        }
    }
}
