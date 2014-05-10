package org.deephacks.graphene;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import org.deephacks.vals.VirtualValue;

public class Entities {


  @Entity @VirtualValue
  public static interface Street {
    @Id
    String getFullName();

    String getStreetName();

    String getStreetNumber();

    String getPostalCode();

    City getCity();

    Location getLocation();
  }

  @Entity @VirtualValue
  public static interface City {
    @Id
    String getName();

    Location getLocation();
  }

  @Embedded @VirtualValue
  public static interface Location {
    Double getLongitude();
    Double getLatitude();

    default double distanceInKm(Location location) {
      LatLng point1 = new LatLng(getLatitude(), getLongitude());
      LatLng point2 = new LatLng(location.getLatitude(), location.getLongitude());
      return  LatLngTool.distance(point1, point2, LengthUnit.KILOMETER);
    }
  }
}
