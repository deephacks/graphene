package org.deephacks.graphene;

import org.deephacks.graphene.Entities.City;
import org.deephacks.graphene.Entities.Location;
import org.deephacks.graphene.Entities.Street;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class GoogleMaps {
  private static final String MAPS_ADDRESS = "maps.googleapis.com";
  private static final String GEOCODE_URL = "/maps/api/geocode/json";
  private static final String DISTANCE_URL = "/maps/api/distancematrix/json";
  private static EntityRepository repository = new EntityRepository();

  public static void main(String[] args) throws UnsupportedEncodingException, ClassNotFoundException {
    addStockholmLocation("S:t Eriksgatan 130A");
    addStockholmLocation("Atlasgatan 1");
    addStockholmLocation("Aktergatan 5");
    addStockholmLocation("Aluddsvägen 10");
    addStockholmLocation("Gävlegatan 35");
    addStockholmLocation("Sveav. 56");
    addStockholmLocation("Kungsholmsg. 16");
    addStockholmLocation("Kungsholmsg. 18");
    addStockholmLocation("Kungsholmsg. 20");
    addStockholmLocation("Vasag. 14");
    addStockholmLocation("Hamng. 14");
  }

  public static void addLocation(String street, String city) {
    Street loc = findLocation(street, city);
    repository.put(loc.getCity());
    repository.put(loc);
    System.out.println(loc);
  }

  public static void addStockholmLocation(String street) {
    Street loc = findLocation(street, "Stockholm");
    repository.put(loc.getCity());
    repository.put(loc);
    System.out.println(loc);
  }

  public static int computeLevenshteinDistance(String str1,String str2) {
    int[][] distance = new int[str1.length() + 1][str2.length() + 1];

    for (int i = 0; i <= str1.length(); i++)
      distance[i][0] = i;
    for (int j = 1; j <= str2.length(); j++)
      distance[0][j] = j;

    for (int i = 1; i <= str1.length(); i++)
      for (int j = 1; j <= str2.length(); j++)
        distance[i][j] = minimum(
                distance[i - 1][j] + 1,
                distance[i][j - 1] + 1,
                distance[i - 1][j - 1]+ ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));

    return distance[str1.length()][str2.length()];
  }
  private static int minimum(int a, int b, int c) {
    return Math.min(Math.min(a, b), c);
  }

  public static Long distance(String origin, String destination) {
    StringBuilder sb = new StringBuilder();
    sb.append(DISTANCE_URL).append("?");
    sb.append("origins=").append(origin).append("&");
    sb.append("destinations=").append(destination);
    sb.append("&sensor=false");
    Http http = new Http(MAPS_ADDRESS, 80);
    String response = http.post(sb.toString());

    try {
      JSONObject json = new JSONObject(response);
      json = json.getJSONArray("rows").getJSONObject(0).getJSONArray("elements")
              .getJSONObject(0);
      String result = json.getJSONObject("distance").getString("value");
      return Long.parseLong(result);

    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public static Street findLocation(String street, String city)  {
    StringBuilder sb = new StringBuilder();
    sb.append(GEOCODE_URL).append("?");

    sb.append("address=").append(urlEncode(street, city)).append("&");
    sb.append("&sensor=false");

    Http http = new Http(MAPS_ADDRESS, 80);
    String response = http.post(sb.toString());
    try {
      JSONObject json = new JSONObject(response);
      JSONObject results = json.getJSONArray("results").getJSONObject(0);

      JSONArray address_components = results.getJSONArray("address_components");
      String streetNumber = getAddressComponent(address_components, "street_number");
      String streetName = getAddressComponent(address_components, "route");
      String cityName = getAddressComponent(address_components, "locality");
      String postalCode = getAddressComponent(address_components, "postal_code");

      Location location = getLocation(results.getJSONObject("geometry").getJSONObject("location"));

      City aCity = new CityBuilder().withName(cityName).withLocation(location).build();
      return new StreetBuilder()
              .withCity(aCity)
              .withFullName(streetName + " " + streetNumber + " " + city)
              .withStreetName(streetName)
              .withPostalCode(postalCode)
              .withStreetNumber(streetNumber)
              .withLocation(location)
              .build();
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getAddressComponent(JSONArray json, String type) throws JSONException {
    for (int i = 0; i < json.length(); i++) {
      JSONObject component = json.getJSONObject(i);
      JSONArray types = component.getJSONArray("types");
      for (int j = 0; j < types.length(); j++) {
        if (types.getString(j).equals(type)) {
          return component.getString("long_name");
        }
      }
    }
    throw new IllegalArgumentException("Type " + type + " not found.");
  }

  private static City getCity(JSONObject json, Location location) throws JSONException {
    return new CityBuilder()
            .withLocation(location)
            .build();
  }


  private static Location getLocation(JSONObject json) throws JSONException {
    double lat = Double.parseDouble(json.getString("lat"));
    double lng = Double.parseDouble(json.getString("lng"));
    return new LocationBuilder()
            .withLatitude(lat)
            .withLongitude(lng)
            .build();
  }

  private static String urlEncode(String... values) {
    StringBuilder sb = new StringBuilder();
    for (String value : values) {
      try {
        sb.append(URLEncoder.encode(value, "UTF-8")).append(URLEncoder.encode(" ", "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    return sb.toString();

  }
}
