import org.eclipse.paho.client.mqttv3.*;
import sun.net.www.protocol.http.HttpURLConnection;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bob {

    String partyBob = "bob::12206797e87af1f340589c1c19e3e3701b5439e0a3763d5f4390d3d00d13a9df474e";
    String partyAlice = "alice::1220ce1dfaa63b027e1f6cefb66c6993234fb38b905f8f46a48b2330fdf5b9b413b7";
    String Token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RhbWwuY29tL2xlZGdlci1hcGkiOnsibGVkZ2VySWQiOiJwYXJ0aWNpcGFudDEiLCJhcHBsaWNhdGlvbklkIjoiZm9vYmFyIiwiYWN0QXMiOlsiYm9iOjoxMjIwNjc5N2U4N2FmMWYzNDA1ODljMWMxOWUzZTM3MDFiNTQzOWUwYTM3NjNkNWY0MzkwZDNkMDBkMTNhOWRmNDc0ZSJdfX0.c9QKP33tbaTpmMBfAGOVJXSd0397a5g-umyVMLVOLB0krGWaUZzAONTQ5nd4B0CZkjJ_XpwblT816yll632wKqic3G1PggDsi3lgRCo__wgFIR0r-ZK_HvxnbWYGTMYiDXQZZnhtD2g1J1JXTFwN2Zr18KCJjRcTDRpkrCXNUVduP3EqBAWvmbpmo1gEas4HNs6FTOX_n0LcrzbinP2_-taCwPu2RDVARq3jMcJkbt-39Zo4IB4r_g-ek_5henxq2LYtdWxB_jxqNosdoC7cBxLafyHwhW9qS3kzgYP2c5Lfp15Xj7LYz3To5OzDdeH9bBmd6Hdk2WOKUCYBVn2mgw";
    HttpURLConnection get;
    HttpURLConnection post;
    HttpURLConnection con;
    HttpURLConnection con_query;
    HttpURLConnection con_first_query;
    HttpURLConnection post_update;
    byte [] out;
    byte [] out_query;
    byte [] first_query;
    String currentState;
    String place;
    String payload;
    BufferedReader reader = null;
    String newState;
    String item;
    String payload_query;
    String [] contractLine;
    int initialNumberOfContracts;

    public Bob () throws IOException {
        getFirstContracts();
    }

    //***************************************** DAML **********************************************//
    void setDamlConnection() throws IOException, InterruptedException {
        URL url = new URL("http://localhost:7575/v1/create"); //URL
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        String input = "{\"templateId\": \"Lights:RequestChange\", \"payload\": {\"asker\": " + "\"" + partyBob + "\"" + ", \"answerer\": " + "\"" + partyAlice + "\"" + ", \"place\": " + "\"" + place + "\"" + ", \"currentState\": " + "\"" + currentState + "\"" + " }}" ;
        out = input.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        con.setFixedLengthStreamingMode(length);
        con.setAuthenticationProperty("Authorization", "Bearer " + Token);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.connect();

        initiateDamlConnection();
    }

    void initiateDamlConnection() throws IOException, InterruptedException {
        try(OutputStream os = con.getOutputStream()) {
            System.out.println("BOB API CREATE1 " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
            os.write(out);
            System.out.println("BOB API CREATE2 " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
        }
        con.disconnect();
        Thread.sleep(2000);
        new DealWithBob(this).start();
    }

    void query() throws IOException {
        URL url = new URL("http://localhost:7575/v1/query"); //URL
        try {
            con_query = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        con_query.setRequestMethod("POST");
        con_query.setDoOutput(true);

        String input = "{\"templateIds\": [\"Lights:Change\"], \"query\": {\"answerer\": " + "\"" + partyAlice + "\"" + "}}" ;
        out_query = input.getBytes(StandardCharsets.UTF_8);
        int length = out_query.length;
        con_query.setFixedLengthStreamingMode(length);
        con_query.setAuthenticationProperty("Authorization", "Bearer " + Token);
        con_query.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con_query.connect();

        try(OutputStream os = con_query.getOutputStream()) {
            os.write(out_query);
        }

        String line;
        try {
            reader = new BufferedReader(new InputStreamReader(con_query.getInputStream()));
        }
        catch(IOException e){
            if(reader == null)
                reader = new BufferedReader(new InputStreamReader(con_query.getErrorStream()));
        }
        while ((line = reader.readLine()) != null) {
            payload_query = line.toString();
        }

        managePayload();
    }

    private void managePayload() throws IOException {
        //https://stackoverflow.com/questions/4403249/is-there-a-java-json-deserializer-that-decodes-a-string-into-dictionary-of-lists
        Map<String, Object> map = (Map<String, Object>) new flexjson.JSONDeserializer().deserialize(payload_query);

        String allContracts = map.get("result").toString();

        contractLine = allContracts.split("signatories");

        String last = contractLine[contractLine.length - 1];
        System.out.println("Last Contract: " + last);

        String regex = "(?<=newState=)(ON|OFF)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(last);

        if (matcher.find()) {
            newState = matcher.group(0);
            for (int i = 1; i <= matcher.groupCount(); i++) {
                newState = matcher.group(i);
            }
        }

        System.out.println(newState);
        manageQueryResult();
    }

    private void manageQueryResult() throws IOException {
        System.out.println(payload_query);

        if(initialNumberOfContracts < contractLine.length - 1) {
            update("OFF", "FF_CF_CONTROL");
            System.out.println("OpenHAB UPDATE: " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
            update(newState,"FF_CF_LIGHT");
        }
        else
            System.out.println("Contract was not created");
    }

    private void getFirstContracts() throws IOException {
        URL url = new URL("http://localhost:7575/v1/query"); //URL
        try {
            con_first_query = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        con_first_query.setRequestMethod("POST");
        con_first_query.setDoOutput(true);

        String input = "{\"templateIds\": [\"Lights:Change\"], \"query\": {\"answerer\": " + "\"" + partyAlice + "\"" + "}}" ;
        first_query = input.getBytes(StandardCharsets.UTF_8);
        int length = first_query.length;
        con_first_query.setFixedLengthStreamingMode(length);
        con_first_query.setAuthenticationProperty("Authorization", "Bearer " + Token);
        con_first_query.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con_first_query.connect();

        try(OutputStream os = con_first_query.getOutputStream()) {
            os.write(first_query);
        }

        String line;
        String payload = null;
        try {
            reader = new BufferedReader(new InputStreamReader(con_first_query.getInputStream()));
        }
        catch(IOException e){
            if(reader == null)
                reader = new BufferedReader(new InputStreamReader(con_first_query.getErrorStream()));
        }
        while ((line = reader.readLine()) != null) {
            payload = line.toString();
        }

        //DISCONNECT
        con_first_query.disconnect();

        //https://stackoverflow.com/questions/4403249/is-there-a-java-json-deserializer-that-decodes-a-string-into-dictionary-of-lists
        Map<String, Object> map = (Map<String, Object>) new flexjson.JSONDeserializer().deserialize(payload);

        String allContracts = map.get("result").toString();

        String [] contracts = allContracts.split("signatories");

        initialNumberOfContracts = contracts.length - 1;
    }

    public static void main(String[] args) throws IOException, MqttException, InterruptedException {
        new Bob().setOpenHABConnection();
    }

    //************************************* OPENHAB ***********************************************//

    void setOpenHABConnection() throws IOException, MqttException {
        final String serverUrl   = "tcp://localhost";
        final String clientId    = "my_mqtt_java_client";
        final String username    = "mqtt";
        final String password    = "mqtt";

        // MQTT connection options
        final MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        // connect the client to Cumulocity IoT
        final MqttClient client = new MqttClient(serverUrl, clientId, null);
        client.connect(options);

        client.subscribe("TopicTest", new IMqttMessageListener() {
            public void messageArrived (final String topic, final MqttMessage message) throws Exception {
                payload = new String(message.getPayload());
                String [] line = payload.split(":");
                if (line[0].equals("ON")) {  //If Bob wants to alice to Control the light (Switch Control The Lights ON)
                    System.out.println("MQTT SUBSCRIBE " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
                    currentState = get(line[1]);
                    place = line[2];
                    item = line[1];
                    System.out.println("Subscribe,  Item: " + line[1] + " Place: " + place + " Current State: " + currentState);
                    setDamlConnection();
                }
            }
        });

    }

    void update(String input, String item) throws IOException {
        URL url = new URL("http://localhost:8080/rest/items/" + item + "/state" ); //URL
        try {
            post_update = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        post_update.setRequestMethod("PUT");
        post_update.setDoOutput(true);

        byte [] out = input.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        post_update.setFixedLengthStreamingMode(length);
        post_update.setAuthenticationProperty("Accept", "application/json" );
        post_update.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        post_update.connect();

        try(OutputStream os = post_update.getOutputStream()) {
            os.write(out);
        }
        System.out.println("Update item: " + item + " State: " + input);
        post_update.disconnect();
    }

    String get(String item) throws IOException {
        //************** meter no URL o item escolhido******************///////
        URL url = new URL("http://localhost:8080/rest/items/" + item + "/state"); //URL

        try {
            get = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        get.setRequestMethod("GET");
        String a = new String();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(get.getInputStream()))) {
            a = reader.readLine();
        }
        return a;
    }
}
