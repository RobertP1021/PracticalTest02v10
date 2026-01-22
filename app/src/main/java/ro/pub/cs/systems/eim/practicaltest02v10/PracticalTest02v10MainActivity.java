package ro.pub.cs.systems.eim.practicaltest02v10;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PracticalTest02v10MainActivity extends AppCompatActivity {

    Button startServer, connect;
    EditText pokemonName, port;
    TextView abilitiesTextView, typeTextView;

    private static final String TAG = "POKEMON";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_practical_test02v10_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        startServer = findViewById(R.id.startServer);
        connect = findViewById(R.id.connectButton);
        pokemonName = findViewById(R.id.pokemonName);
        port = findViewById(R.id.serverPort);
        abilitiesTextView = findViewById(R.id.abilities);
        typeTextView = findViewById(R.id.type);

        startServer.setOnClickListener((v) -> {
            Server server = new Server(Integer.parseInt(port.getText().toString()));
            server.start();
        });

        connect.setOnClickListener((v) -> {
            String name = pokemonName.getText().toString();
            int serverPort = Integer.parseInt(port.getText().toString());
            Client client = new Client(serverPort, "localhost", name);
            client.start();
        });
    }

    class Server {
        Integer port;
        ServerSocket serverSocket;

        public Server(Integer port) {
            this.port = port;
        }

        public void start() {
            new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(port);
                    while (true) {
                        Socket socket = serverSocket.accept();
                        new Thread(() -> handleClient(socket)).start();
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }).start();
        }

        public void handleClient(Socket socket) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                String pokemonNameReceived = reader.readLine();

                String apiUrl = "https://pokeapi.co/api/v2/pokemon/" + pokemonNameReceived;

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(apiUrl)
                        .build();

                Response response = client.newCall(request).execute();
                String jsonData = response.body().string();

                JSONObject jsonObject = new JSONObject(jsonData);

                JSONArray typesArray = jsonObject.getJSONArray("types");
                String type = typesArray.getJSONObject(0)
                        .getJSONObject("type")
                        .getString("name");

                JSONArray abilitiesArray = jsonObject.getJSONArray("abilities");
                String ability = abilitiesArray.getJSONObject(0)
                        .getJSONObject("ability")
                        .getString("name");

                String result = type + ";" + ability;
                writer.println(result);

                socket.close();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    class Client {
        Integer port;
        String address;
        String pokemonName;

        public Client(Integer port, String address, String pokemonName) {
            this.port = port;
            this.address = address;
            this.pokemonName = pokemonName;
        }

        public void start() {
            new Thread(() -> {
                try {
                    Socket socket = new Socket(address, port);

                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    writer.println(pokemonName);

                    String response = reader.readLine();
                    Log.d(TAG, "Received from server: " + response);

                    if (response != null && response.contains(";")) {
                        String[] parts = response.split(";");
                        String type = parts[0];
                        String ability = parts[1];

                        runOnUiThread(() -> {
                            typeTextView.setText(type);
                            abilitiesTextView.setText(ability);
                        });
                    }

                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }).start();
        }
    }
}