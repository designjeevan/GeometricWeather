package wangdaye.com.geometricweather.main;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import wangdaye.com.geometricweather.basic.GeoActivity;
import wangdaye.com.geometricweather.basic.model.Location;
import wangdaye.com.geometricweather.main.dialog.BackgroundLocationDialog;
import wangdaye.com.geometricweather.main.model.Indicator;
import wangdaye.com.geometricweather.main.model.LocationResource;

public class MainActivityViewModel extends ViewModel
        implements MainActivityRepository.WeatherRequestCallback {

    private final MutableLiveData<LocationResource> currentLocation;
    private final MutableLiveData<Indicator> indicator;

    private MainActivityRepository repository;

    private @Nullable List<Location> totalList; // all locations.
    private @Nullable List<Location> validList; // location list optimized for resident city.
    private @Nullable Integer validIndex; // current index for validList.

    private boolean newInstance;

    public MainActivityViewModel() {
        currentLocation = new MutableLiveData<>();
        currentLocation.setValue(null);

        indicator = new MutableLiveData<>();
        indicator.setValue(new Indicator(1, 0));

        totalList = null;
        validList = null;
        validIndex = null;

        newInstance = true;
    }

    public void reset(GeoActivity activity) {
        LocationResource resource = currentLocation.getValue();
        if (resource != null) {
            init(activity, resource.data);
        }
    }

    public void init(GeoActivity activity, @NonNull Location location) {
        init(activity, location.getFormattedId());
    }

    public void init(GeoActivity activity, @Nullable String formattedId) {
        if (repository == null) {
            repository = new MainActivityRepository(activity);
        }

        final boolean[] firstInit = {true};

        List<Location> oldList = totalList == null
                ? new ArrayList<>()
                : Collections.unmodifiableList(totalList);
        repository.getLocationList(activity, oldList, locationList -> {
            if (locationList == null) {
                return;
            }

            List<Location> totalList = new ArrayList<>(locationList);
            List<Location> validList = Location.excludeInvalidResidentLocation(activity, totalList);

            int validIndex = firstInit[0]
                    ? indexLocation(validList, formattedId)
                    : indexLocation(this.validList, getCurrentFormattedId());
            firstInit[0] = false;

            setLocation(activity, totalList, validList, validIndex,
                    LocationResource.Source.REFRESH, null);
        });
    }

    public void setLocation(GeoActivity activity, int offset) {
        if (totalList == null || validList == null || validIndex == null) {
            return;
        }

        int index = validIndex;
        index = Math.max(index, 0);
        index = Math.min(index, validList.size() - 1);
        index += offset + validList.size();
        index %= validList.size();

        setLocation(
                activity,
                new ArrayList<>(totalList),
                new ArrayList<>(validList),
                index,
                LocationResource.Source.REFRESH,
                null
        );
    }

    private void setLocation(GeoActivity activity,
                             @NonNull List<Location> newTotalList,
                             @NonNull List<Location> newValidList,
                             int newValidIndex,
                             LocationResource.Source source,
                             @Nullable LocationResource resource) {
        totalList = newTotalList;
        validList = newValidList;

        validIndex = newValidIndex;
        validIndex = Math.max(validIndex, 0);
        validIndex = Math.min(validIndex, newValidList.size() - 1);

        Location current = validList.get(validIndex);
        Indicator i = new Indicator(validList.size(), validIndex);

        boolean defaultLocation = validIndex == 0;
        if (resource == null && MainModuleUtils.needUpdate(activity, current)) {
            currentLocation.setValue(LocationResource.loading(current, defaultLocation, source));
            updateWeather(activity, false);
        } else if (resource == null) {
            repository.cancel();
            currentLocation.setValue(LocationResource.success(current, defaultLocation, source));
        } else {
            currentLocation.setValue(resource);
        }
        indicator.setValue(i);
    }

    public void updateLocationFromBackground(GeoActivity activity, @Nullable String formattedId) {
        if (TextUtils.isEmpty(formattedId)
                || totalList == null || validList == null || validIndex == null) {
            return;
        }

        assert formattedId != null;
        repository.getLocationAndWeatherCache(activity, formattedId, location -> {

            List<Location> totalList = new ArrayList<>(this.totalList);
            for (int i = 0; i < totalList.size(); i ++) {
                if (totalList.get(i).equals(location)) {
                    totalList.set(i, location);
                    break;
                }
            }

            List<Location> validList = Location.excludeInvalidResidentLocation(activity, totalList);

            int validIndex = indexLocation(validList, getCurrentFormattedId());

            setLocation(activity, totalList, validList, validIndex,
                    LocationResource.Source.BACKGROUND, null);
        });
    }

    private static int indexLocation(List<Location> locationList, @Nullable String formattedId) {
        if (TextUtils.isEmpty(formattedId)) {
            return -1;
        }

        for (int i = 0; i < locationList.size(); i ++) {
            if (locationList.get(i).equals(formattedId)) {
                return i;
            }
        }

        return -1;
    }

    @Nullable
    public Location getLocationFromList(int offset) {
        if (totalList == null || validList == null || validIndex == null) {
            return null;
        }
        return validList.get((validIndex + offset + validList.size()) % validList.size());
    }

    public void updateWeather(GeoActivity activity, boolean triggeredByUser) {
        if (currentLocation.getValue() == null) {
            return;
        }

        repository.cancel();

        Location location = currentLocation.getValue().data;

        currentLocation.setValue(
                LocationResource.loading(
                        location,
                        currentLocation.getValue().defaultLocation,
                        LocationResource.Source.REFRESH
                )
        );

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || !location.isCurrentPosition()) {
            // don't need to request any permission -> request data directly.
            repository.getWeather(
                    activity, location, location.isCurrentPosition(), triggeredByUser, this);
            return;
        }

        // check basic location permissions.
        List<String> permissionList = getDeniedPermissionList(activity, false);
        if (permissionList.size() == 0) {
            // already got all permissions -> request data directly.
            repository.getWeather(
                    activity, location, true, triggeredByUser, this);
            return;
        }

        // need request permissions -> request basic location permissions.
        String[] permissions = permissionList.toArray(new String[0]);
        activity.requestPermissions(permissions, 0, (requestCode, permission, grantResult) -> {

            for (int i = 0; i < permission.length && i < grantResult.length; i++) {
                if (isPivotalPermission(permission[i])
                        && grantResult[i] != PackageManager.PERMISSION_GRANTED) {
                    // denied basic location permissions.
                    if (location.isUsable()) {
                        repository.getWeather(
                                activity, location, false, triggeredByUser, this);
                    } else {
                        currentLocation.setValue(
                                LocationResource.error(
                                        location,
                                        true,
                                        LocationResource.Source.REFRESH
                                )
                        );
                    }
                    return;
                }
            }

            // check background location permissions.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                List<String> backgroundPermissionList = getDeniedPermissionList(activity, true);
                if (backgroundPermissionList.size() != 0) {
                    BackgroundLocationDialog dialog = new BackgroundLocationDialog();
                    dialog.setOnSetButtonClickListener(() ->
                            activity.requestPermissions(
                                    backgroundPermissionList.toArray(new String[0]),
                                    0,
                                    null
                            )
                    );
                    dialog.show(activity.getSupportFragmentManager(), null);
                }
            }

            repository.getWeather(
                    activity, location, true, triggeredByUser, this);
        });
    }

    private List<String> getDeniedPermissionList(Context context, boolean background) {
        List<String> permissionList = repository.getLocatePermissionList(background);
        for (int i = permissionList.size() - 1; i >= 0; i --) {
            if (ActivityCompat.checkSelfPermission(context, permissionList.get(i))
                    == PackageManager.PERMISSION_GRANTED) {
                permissionList.remove(i);
            }
        }
        return permissionList;
    }

    private boolean isPivotalPermission(String permission) {
        return permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)
                || permission.equals(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public MutableLiveData<LocationResource> getCurrentLocation() {
        return currentLocation;
    }

    public MutableLiveData<Indicator> getIndicator() {
        return indicator;
    }

    @Nullable
    public Location getCurrentLocationValue() {
        LocationResource resource = currentLocation.getValue();
        if (resource != null) {
            return resource.data;
        } else {
            return null;
        }
    }

    @Nullable
    public String getCurrentFormattedId() {
        Location location = getCurrentLocationValue();
        if (location != null) {
            return location.getFormattedId();
        } else if (validList != null && validIndex != null) {
            return validList.get(validIndex).getFormattedId();
        } else {
            return null;
        }
    }

    public @NonNull List<Location> getLocationList() {
        if (validList != null) {
            return Collections.unmodifiableList(validList);
        } else {
            return new ArrayList<>();
        }
    }

    public boolean isNewInstance() {
        if (newInstance) {
            newInstance = false;
            return true;
        }
        return false;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (repository != null) {
            repository.cancel();
        }
    }

    // weather request callback.

    @Override
    public void onReadCacheCompleted(GeoActivity activity, Location location, boolean succeed, boolean done) {
        callback(activity, location, false, succeed, done);
    }

    @Override
    public void onLocationCompleted(GeoActivity activity, Location location, boolean succeed, boolean done) {
        callback(activity, location, !succeed, succeed, done);
    }

    @Override
    public void onGetWeatherCompleted(GeoActivity activity, Location location, boolean succeed, boolean done) {
        callback(activity, location, false, succeed, done);
    }

    private void callback(GeoActivity activity, Location location,
                          boolean locateFailed, boolean succeed, boolean done) {
        if (totalList == null || validList == null || validIndex == null) {
            return;
        }

        List<Location> totalList = new ArrayList<>(this.totalList);
        for (int i = 0; i < totalList.size(); i ++) {
            if (totalList.get(i).equals(location)) {
                totalList.set(i, location);
                break;
            }
        }

        List<Location> validList = Location.excludeInvalidResidentLocation(activity, totalList);

        int validIndex = indexLocation(validList, getCurrentFormattedId());

        boolean defaultLocation = location.equals(validList.get(0));
        LocationResource resource;
        if (!done) {
            resource = LocationResource.loading(
                    location, defaultLocation, locateFailed, LocationResource.Source.REFRESH);
        } else if (succeed) {
            resource = LocationResource.success(
                    location, defaultLocation, LocationResource.Source.REFRESH);
        } else {
            resource = LocationResource.error(
                    location, defaultLocation, locateFailed, LocationResource.Source.REFRESH);
        }

        setLocation(activity, totalList, validList, validIndex,
                LocationResource.Source.BACKGROUND, resource);
    }
}