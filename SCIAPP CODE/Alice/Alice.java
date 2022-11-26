import org.eclipse.paho.client.mqttv3.*;
import sun.net.www.protocol.http.HttpURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Alice {

    String Token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RhbWwuY29tL2xlZGdlci1hcGkiOnsibGVkZ2VySWQiOiJwYXJ0aWNpcGFudDIiLCJhcHBsaWNhdGlvbklkIjoiZm9vYmFyIiwiYWN0QXMiOlsiYWxpY2U6OjEyMjBjZTFkZmFhNjNiMDI3ZTFmNmNlZmI2NmM2OTkzMjM0ZmIzOGI5MDVmOGY0NmE0OGIyMzMwZmRmNWI5YjQxM2I3Il19fQ.PvepGOEUuDz0lVsh0t_Zqs-a0tNZdIwuyPji6h9UfOWC_BweTHgYtiHjNnaS0REEVnsfcqxvQVIqG-kcF_JTIqtAtAZwE3F-zOHmgrzaRW_CJiSe2xAe_kqE9UVWra1cWWOI3SL4e1gcEvxfELtLHe6VeaIYvRhR4aZV-n7-1q3E-MfAXuEsZKFdtaEfbuiJB03DIQlJTR4mOUSflC6HTQdJdB7Ki9ByzJULbmQIiI0AVRqCHAKI_cpRA1wTSBdhC_7y240TYQDKZgBud2FNZq33x_n9hWNlvcxw3FJQKrfl5nmTmm05v_LkiIVZbPnwgO19abX4Vjol3k8gTdrz1w";
    String partyAlice = "alice::1220c5e9e3f2b736b83853bb20bd567a6be777bb3c6a3930042888802042a45f11c6";
    //String partyBob = "bob::122033f10bca62fab35e0d8c3abee47abc23b78c8d91c90841ad7d4956c984eefecc";
    String Token_noAUT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJodHRwczovL2RhbWwuY29tL2xlZGdlci1hcGkiOnsibGVkZ2VySWQiOiJwYXJ0aWNpcGFudDIiLCJhcHBsaWNhdGlvbklkIjoiZm9vYmFyIiwiYWN0QXMiOlsiYWxpY2U6OjEyMjBjNWU5ZTNmMmI3MzZiODM4NTNiYjIwYmQ1NjdhNmJlNzc3YmIzYzZhMzkzMDA0Mjg4ODgwMjA0MmE0NWYxMWM2Il19fQ.basJbtk6WqomZV1iPzV1UEY8wmfwvP5mshgzoAR_ELg";
    static HttpURLConnection con;
    static HttpURLConnection con_exercise;
    static HttpURLConnection post;
    static HttpURLConnection post_updade;
    static String payload = "";
    static byte [] out;
    static byte [] out_exercise;
    static BufferedReader reader = null ;
    static String ContractId;
    String oldState;


    public Alice () throws InterruptedException {
        new DealWithAlice(this).start();
    }

    void setDamlConnection() throws IOException {
        URL url = new URL("http://localhost:7575/v1/query"); //URL
        try {
            con = (HttpURLConnection) url.openConnection();

        } catch (IOException e) {
            e.printStackTrace();
        }
        con.setRequestMethod("POST");
        con.setDoOutput(true);

        String input = "{\"templateIds\": [\"Lights:RequestChange\"], \"query\": {\"answerer\": " +"\"" + partyAlice + "\"" + "}}" ;
        out = input.getBytes(StandardCharsets.UTF_8);
        int length = out.length;
        con.setFixedLengthStreamingMode(length);
        con.setAuthenticationProperty("Authorization", "Bearer " + Token_noAUT);
        con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con.connect();

        try(OutputStream os = con.getOutputStream()) {
            os.write(out);
        }

        String line;
        try {
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        }
        catch(IOException e){
            if(reader == null)
                reader = new BufferedReader(new InputStreamReader(con.getErrorStream()));
        }
        while ((line = reader.readLine()) != null) {
            payload += line.toString();
        }
        manageQueryResult(payload);


    }

    private void manageQueryResult(String payload) throws IOException {
        System.out.println(payload);
        String [] line = payload.split("\"");
        if(line[7].equals("contractId")) {
            update("OPEN", "CONTROL_STATE");
            System.out.println("API UPDATE OpenHAB (Contact): " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
            try {
                ContractId = line[9];
                oldState = line [31]; // BUSCAR O CURRENT STATE DA LUZ DO BOB PARA FAZER POST NO STATE DA LUZ DA ALICE
                update(oldState, "CONTROL_LIGHT");
                waitForChange();
            } catch (IOException | MqttException e) {
                System.out.println("Contract does not exist");
            }
        }
        else
            System.out.println("Contract does not exist");
    }

    private void waitForChange() throws IOException, MqttException {
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
                String payload_state = new String(message.getPayload());
                System.out.println("*********************NEW STATE*****************************************" + payload_state);
                System.out.println("API MQTT SUBSCRIBE: " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
                exerciseChoice(ContractId, payload_state);
                update("CLOSED", "CONTROL_STATE");
                client.disconnect();
            }
        });

    }

    private void exerciseChoice(String contractId, String newState) throws IOException {
        URL url = new URL("http://localhost:7575/v1/exercise"); //URL
        try {
            con_exercise = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        con_exercise.setRequestMethod("POST");
        con_exercise.setDoOutput(true);

        String input = "{\"templateId\": \"Lights:RequestChange\", \"contractId\": " + "\"" + contractId + "\", \"choice\": \"Accept\", \"argument\": {\"feedback\": \"Accepted\", \"newState\": " + "\"" + newState + "\"" + " }}" ;

        out_exercise = input.getBytes(StandardCharsets.UTF_8);
        int length = out_exercise.length;
        con_exercise.setFixedLengthStreamingMode(length);
        con_exercise.setAuthenticationProperty("Authorization", "Bearer " + Token_noAUT);
        con_exercise.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        con_exercise.connect();

        try(OutputStream os = con_exercise.getOutputStream()) {
            System.out.println("ALICE API EXERCISE1: " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
            os.write(out_exercise);
            System.out.println("ALICE API EXERCISE2: " + System.currentTimeMillis() + " " + java.time.LocalDateTime.now());
        }
        System.out.println("Exercise Choice");

    }

    void update(String input, String item) throws IOException {
        URL url = new URL("http://localhost:8080/rest/items/" + item + "/state" ); //URL
        try {
            post_updade = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        post_updade.setRequestMethod("PUT");
        post_updade.setDoOutput(true);

        byte [] out = input.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        post_updade.setFixedLengthStreamingMode(length);
        post_updade.setAuthenticationProperty("Accept", "application/json" );
        post_updade.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        post_updade.connect();

        try(OutputStream os = post_updade.getOutputStream()) {
            os.write(out);
        }
        System.out.println("Update item: " + item + " State: " + input);
    }

    void post(String input, String item) throws IOException {
        //************** meter no URL o item escolhido******************///////
        URL url = new URL("http://localhost:8080/rest/items/" + item ); //URL

        try {
            post = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        post.setRequestMethod("POST");
        post.setDoOutput(true);

        byte [] out = input.getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        post.setFixedLengthStreamingMode(length);
        post.setAuthenticationProperty("Accept", "application/json" );
        post.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        post.connect();

        try(OutputStream os = post.getOutputStream()) {
            os.write(out);
        }
        System.out.println("Post to OpenHAB the Current State:" + input + " of Item: " + item );
    }

    public void setPayload() {
        payload = "";
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new Alice();
    }
}