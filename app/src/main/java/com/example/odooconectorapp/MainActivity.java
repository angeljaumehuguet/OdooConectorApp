package com.example.odooconectorapp;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText editTextUrl, editTextDb, editTextUsername, editTextPassword;
    private Button buttonConnect;
    private TextView textViewStatus;

    private static final String TAG = "OdooConnector";
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar executor y handler
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        // 1. Enlazar los elementos de la UI con las variables
        editTextUrl = findViewById(R.id.editTextUrl);
        editTextDb = findViewById(R.id.editTextDb);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonConnect = findViewById(R.id.buttonConnect);
        textViewStatus = findViewById(R.id.textViewStatus);

        // Pre-llenar campos para pruebas con datos de Bitnami
        editTextUrl.setText("http://192.168.240.192:8069");
        editTextDb.setText("bitnami_odoo");
        editTextUsername.setText("user@example.com");
        editTextPassword.setText("uoRe7JI@URoZ");

        // 2. Configurar el OnClickListener para el botón
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = editTextUrl.getText().toString().trim();
                String db = editTextDb.getText().toString().trim();
                String username = editTextUsername.getText().toString().trim();
                String password = editTextPassword.getText().toString().trim();

                if (url.isEmpty() || db.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    textViewStatus.setText("Si us plau, omple tots els camps.");
                    return;
                }

                // Ejecutar conexión en hilo de fondo
                connectToOdoo(url, db, username, password);
            }
        });
    }

    private void connectToOdoo(String serverUrl, String database, String user, String pass) {
        // Actualizar UI en hilo principal - estado "conectando"
        textViewStatus.setText("Connectant...");
        buttonConnect.setEnabled(false);

        // Ejecutar conexión en hilo de fondo
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Object result = null;
                Exception exception = null;

                try {
                    XmlRpcClientConfigImpl commonConfig = new XmlRpcClientConfigImpl();
                    commonConfig.setServerURL(new URL(serverUrl + "/xmlrpc/2/common"));

                    XmlRpcClient client = new XmlRpcClient();
                    client.setConfig(commonConfig);

                    Object[] authParams = new Object[]{database, user, pass, Collections.emptyMap()};
                    result = client.execute("authenticate", authParams);

                } catch (Exception e) {
                    exception = e;
                    Log.e(TAG, "Error en connexió Odoo: " + e.getMessage(), e);
                }

                // Actualizar UI en hilo principal con el resultado
                final Object finalResult = result;
                final Exception finalException = exception;

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        buttonConnect.setEnabled(true);

                        if (finalException != null) {
                            textViewStatus.setText("Error de connexió: " + finalException.getMessage());
                            Log.e(TAG, "Exception details: ", finalException);
                        } else if (finalResult != null) {
                            String deserializedResult = deserialize(finalResult);

                            if (finalResult instanceof Integer && (Integer) finalResult > 0) {
                                textViewStatus.setText("✅ Connexió exitosa!\nUID: " + deserializedResult +
                                        "\nUsuari: " + user + "\nBase de dades: " + database);

                                Log.i(TAG, "Connexió exitosa - UID: " + finalResult);
                                Log.i(TAG, "Servidor: " + serverUrl);
                                Log.i(TAG, "Database: " + database);
                                Log.i(TAG, "User: " + user);

                            } else {
                                textViewStatus.setText("❌ Error d'autenticació: " + deserializedResult);
                                Log.w(TAG, "Autenticació fallida - Result: " + finalResult);
                            }
                        } else {
                            textViewStatus.setText("❌ Error de connexió: Resposta desconeguda del servidor.");
                            Log.e(TAG, "Resposta nula del servidor");
                        }
                    }
                });
            }
        });
    }

    /**
     * Mètode deserialize: converteix la dada rebuda (generalment de l'autenticació d'Odoo)
     * a un String per a la seva visualització o registre.
     *
     * Activitat 4 - Aclaració: Aquest mètode converteix qualsevol tipus de dada
     * (Integer, Boolean, etc.) rebuda de la crida XML-RPC a un String llegible.
     *
     * @param data L'objecte rebut de la trucada XML-RPC (pot ser Integer, Boolean, etc.)
     * @return Una representació en String de la dada.
     */
    private String deserialize(Object data) {
        if (data == null) {
            return "null";
        }

        Log.d(TAG, "Tipus de dada rebuda: " + data.getClass().getSimpleName());
        Log.d(TAG, "Valor de la dada: " + data.toString());

        return String.valueOf(data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}