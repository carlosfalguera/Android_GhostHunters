package edu.upc.ac.jorge.GhostHunters;


import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/*---------- Listener class to get coordinates ------------- */
public class MyLocationListener implements LocationListener {


    public double lastLongitude=0;
    public double lastLatitude=0;

    @Override
    public void onLocationChanged(Location loc) {

        lastLongitude=loc.getLongitude();
        lastLatitude=loc.getLatitude();
        /*
        makeUseOfNewLocation(loc);
        if(currentBestLocation == null){
            currentBestLocation = location;
            lastLongitude=currentBestLocation.getLongitude();
            lastLatitude=currentBestLocation.getLatitude();
        }
        */

        //....

        //editLocation.setText("");
        //pb.setVisibility(View.INVISIBLE);
        //Toast.makeText(getBaseContext(),"Location changed: Lat: " + loc.getLatitude() + " Lng: "+ loc.getLongitude(), Toast.LENGTH_SHORT).show();
        String longitude = "Longitude: " + lastLongitude;
        //Log.v(TAG, longitude);
        String latitude = "Latitude: " + lastLatitude;
        //Log.v(TAG, latitude);

        /*------- To get city name from coordinates -------- */
        //String cityName = null;
        //Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        //List<Address> addresses;
        //try {
        //addresses = gcd.getFromLocation(loc.getLatitude(),
        //        loc.getLongitude(), 1);
        //if (addresses.size() > 0) {
        //    System.out.println(addresses.get(0).getLocality());
        //    cityName = addresses.get(0).getLocality();
        //}
        //}catch (IOException e) { e.printStackTrace(); }
        //String s = longitude + "\n" + latitude + "\n\nMy Current City is: " + cityName;
        //editLocation.setText(s);
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}


    /**
     * This method modify the last know good location according to the arguments.
     *
     * @param location The possible new location.
     */
    Location currentBestLocation;
    void makeUseOfNewLocation(Location location) {
        if ( isBetterLocation(location, currentBestLocation) ) {
            currentBestLocation = location;
        }
    }

    /** Determines whether one location reading is better than the current location fix
     * @param location  The new location that you want to evaluate
     * @param currentBestLocation  The current location fix, to which you want to compare the new one.
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location,
        // because the user has likely moved.
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse.
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    // Checks whether two providers are the same
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * @return the last know best location
     */
    static final int TWO_MINUTES = 1000 * 60 * 2;


}
