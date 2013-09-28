package org.deephacks.graphene.manual.eniro;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import org.deephacks.graphene.Criteria;
import org.deephacks.graphene.Embedded;
import org.deephacks.graphene.Entity;
import org.deephacks.graphene.EntityRepository;
import org.deephacks.graphene.Graphene;
import org.deephacks.graphene.Id;
import org.deephacks.graphene.ResultSet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Eniro {
    public static final String HOST = "api.eniro.com";
    public static final String URL = "/cs/search/basic?profile=%s&key=%s&version=1.1.3&country=%s&";

    public static void main(String[] args) {
        Eniro eniro = new Eniro("krisskross", "8280479948564619946", "se");
        List<CompanyInfo> infos = new ArrayList<>();
        EntityRepository repository = new EntityRepository();
        for (String city : getCities()) {
            /*
            infos.addAll(eniro.query(city, "nordea"));
            infos.addAll(eniro.query(city, "handelsbanken"));
            infos.addAll(eniro.query(city, "swedbank"));
            infos.addAll(eniro.query(city, "Nordnet AB"));
            infos.addAll(eniro.query(city, "Skandinaviska Enskilda Banken"));
            infos.addAll(eniro.query(city, "Avanza Bank AB"));
            infos.addAll(eniro.query(city, "Ica Banken AB"));
            */
        }
        infos.addAll(eniro.query("Stockholm", "nordea"));
        for (CompanyInfo info : infos) {
            repository.put(info);
        }
        repository.commit();

        try (ResultSet<CompanyInfo> resultSet = repository.select(CompanyInfo.class, Criteria.field("name").is(Criteria.contains("ordea"))).retrieve()) {
            for (CompanyInfo info : resultSet) {
                System.out.println(info);
            }
        }
        Graphene.get().get().close();
    }

    private final String url;

    public Eniro(String profile, String key, String country) {
        this.url = String.format(URL, new String[] { profile, key, country });
        System.out.println(url);
    }

    public static List<String> getCities() {
        List<String> list = new ArrayList<>();
        list.add("Malmö");
        list.add("Uppsala");
        list.add("Västerås");
        list.add("Örebro");
        list.add("Linköping");
        list.add("Helsingborg");
        list.add("Jönköping");
        list.add("Norrköping");
        list.add("Lund");
        list.add("Umeå");
        list.add("Gävle");
        list.add("Borås");
        list.add("Mölndal");
        list.add("Södertälje");
        list.add("Eskilstuna");
        list.add("Karlstad");
        list.add("Halmstad");
        list.add("Växjö");
        list.add("Sundsvall");
        list.add("Luleå");
        list.add("Trollhättan");
        list.add("Östersund");
        list.add("Borlänge");
        list.add("Falun");
        list.add("Kalmar");
        list.add("Skövde");
        list.add("Kristianstad");
        list.add("Karlskrona");
        list.add("Skellefteå");
        list.add("Uddevalla");
        list.add("Lidingö");
        list.add("Motala");
        list.add("Landskrona");
        list.add("Örnsköldsvik");
        list.add("Nyköping");
        list.add("Karlskoga");
        list.add("Varberg");
        list.add("Trelleborg");
        list.add("Lidköping");
        list.add("Alingsås");
        list.add("Piteå");
        list.add("Sandviken");
        list.add("Ängelholm");
        return list;
    }

    public List<CompanyInfo> query(String area, String search) {
        ArrayList<CompanyInfo> infos = new ArrayList<>();
        int from = 0;
        int to = 25;
        int totalHits = subQuery(search, area, from, to, infos);
        while (totalHits > to) {
            from += 25;
            to += 100;
            subQuery(search, area, from, to, infos);
        }
        return infos;
    }

    private int subQuery(String search, String area, int from, int to, ArrayList<CompanyInfo> infos) {
        StringBuilder query = new StringBuilder();

        StringBuilder sb = new StringBuilder();
        sb.append(url);
        sb.append("geo_area=");
        if (!Strings.isNullOrEmpty(area)) {
            try {
                area = URLEncoder.encode(area, Charsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException();
            }
            if (area != null) {
                sb.append(area);
            }
        }
        sb.append("&");
        if (!Strings.isNullOrEmpty(search)) {
            String searchUrl = null;
            try {
                searchUrl = URLEncoder.encode(search, Charsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException();
            }
            sb.append("search_word=").append(searchUrl).append("&");
        }
        sb.append("from_list=").append(from).append("to_list=").append(to);
        Http http = new Http(HOST, 80);
        String response = http.post(sb.toString());

        try {
            JSONObject json = new JSONObject(response);
            int totalHits = new Integer(json.getString("totalHits"));
            JSONArray array = json.getJSONArray("adverts");
            for (int i = 0; i < array.length(); i++) {
                JSONObject hit = array.getJSONObject(i);

                try {
                    CompanyInfo info = getCompanyInfo(hit);
                    setAddress(info, hit);
                    setPhone(info, hit);
                    setCoordinates(info, hit);
                    if (info.getName().toLowerCase().startsWith(search.toLowerCase())) {
                        infos.add(info);
                    }

                } catch (Exception e) {
                    continue;
                }

            }
            return totalHits;
        } catch (JSONException e) {
            try {
                JSONObject json = new JSONObject(response);
                String status = json.getString("status");
                if (status.equals("ZERO_RESULTS")) {
                    throw new RuntimeException(status);
                } else {
                    throw new RuntimeException(e);
                }
            } catch (JSONException e1) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setPhone(CompanyInfo info, JSONObject hit) throws JSONException {
        JSONArray array = hit.getJSONArray("phoneNumbers");
        if (array.length() > 0) {
            info.setPhone(array.getJSONObject(0).getString("phoneNumber"));
        }
    }

    private void setCoordinates(CompanyInfo info, JSONObject hit) throws JSONException {
        JSONArray array = hit.getJSONObject("location").getJSONArray("coordinates");
        if (array.length() > 0) {
            info.setLat(new Double(array.getJSONObject(0).getString("latitude")));
            info.setLng(new Double(array.getJSONObject(0).getString("longitude")));
        }
    }

    private static void setAddress(CompanyInfo info, JSONObject hit) throws JSONException {
        JSONObject o = hit.getJSONObject("address");
        String streetName = o.getString("streetName");
        String postCode = o.getString("postCode");
        String postArea = o.getString("postArea");
        info.setAddress(new Address(streetName, postCode, postArea));
    }

    private static CompanyInfo getCompanyInfo(JSONObject hit) throws JSONException {
        JSONObject o = hit.getJSONObject("companyInfo");
        return new CompanyInfo(o.getString("companyName"), o.getString("orgNumber"));
    }

    @Entity
    public static class CompanyInfo {
        @Id
        private final String id = UUID.randomUUID().toString();

        @Embedded
        private Address address;

        private final String name;
        private final String orgNum;

        private Double lat;
        private Double lng;
        private String phone;

        public CompanyInfo(String name, String orgNum) {
            this.name = name;
            this.orgNum = orgNum;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPhone() {
            return phone;
        }

        public String getName() {
            return name;
        }

        public String getOrgNum() {
            return orgNum;
        }

        public Double getLat() {
            return lat;
        }

        public void setLat(Double lat) {
            this.lat = lat;
        }

        public Double getLng() {
            return lng;
        }

        public void setLng(Double lng) {
            this.lng = lng;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        @Override
        public String toString() {
            return "CompanyInfo [name=" + name + ", orgNum=" + orgNum + ", lat=" + lat + ", lng="
                    + lng + ", address=" + address + ", phone=" + phone + "]";
        }

    }

    @Entity
    public static class Address {
        @Id
        private final String id = UUID.randomUUID().toString();
        private final String street;
        private final String postCode;
        private final String postArea;

        public Address(String street, String postCode, String postArea) {
            this.street = street;
            this.postCode = postCode;
            this.postArea = postArea;
        }

        public String getStreet() {
            return street;
        }

        public String getPostCode() {
            return postCode;
        }

        public String getPostArea() {
            return postArea;
        }

        @Override
        public String toString() {
            return "Address [street=" + street + ", postCode=" + postCode + ", postArea="
                    + postArea + "]";
        }

    }
}
