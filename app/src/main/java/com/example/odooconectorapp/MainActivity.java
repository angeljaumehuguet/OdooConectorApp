package com.example.odooconectorapp;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText editTextUrl, editTextDb, editTextUsername, editTextPassword;
    private Button buttonConnect;
    private TextView textViewStatus;

    private static final String TAG = "OdooConnector";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Enlazar los elementos de la UI con las variables
        editTextUrl = findViewById(R.id.editTextUrl);
        editTextDb = findViewById(R.id.editTextDb);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonConnect = findViewById(R.id.buttonConnect);
        textViewStatus = findViewById(R.id.textViewStatus);

        // Pre-llenar campos para pruebas (opcional)
        // editTextUrl.setText("http://tu_ip_o_dominio:8069");
        // editTextDb.setText("tu_base_de_datos");
        // editTextUsername.setText("tu_usuario");
        // editTextPassword.setText("tu_contraseña");


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
                // Iniciar la tarea asíncrona para la conexión
                new OdooConnectTask().execute(url, db, username, password);
            }
        });
    }

    // 3. AsyncTask para realizar la conexión en segundo plano
    private class OdooConnectTask extends AsyncTask<String, Void, Object> {

        private Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            textViewStatus.setText("Connectant...");
            buttonConnect.setEnabled(false); // Deshabilitar botón durante la conexión
        }

        @Override
        protected Object doInBackground(String... params) {
            String urlStr = params[0];
            String db = params[1];
            String username = params[2];
            String password = params[3];

            try {
                XmlRpcClientConfigImpl commonConfig = new XmlRpcClientConfigImpl();
                // El endpoint para autenticación es /xmlrpc/2/common
                commonConfig.setServerURL(new URL(urlStr + "/xmlrpc/2/common"));

                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(commonConfig);

                // El método 'authenticate' espera: db, username, password, environment (HashMap vacío)
                Object[] authParams = new Object[]{db, username, password, Collections.emptyMap()};
                Object result = client.execute("authenticate", authParams);

                // Si la autenticación es exitosa, 'result' será el UID (Integer).
                // Si falla, 'result' podría ser 'false' (Boolean) o lanzar una excepción.
                return result;

            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "Error en connexió Odoo: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            super.onPostExecute(result);
            buttonConnect.setEnabled(true); // Habilitar botón de nuevo

            if (exception != null) {
                textViewStatus.setText("Error de connexió: " + exception.getMessage());
            } else if (result != null) {
                // El método deserialize convierte la respuesta a una cadena legible
                String deserializedResult = deserialize(result);

                // Comprobar si la autenticación fue exitosa
                // Odoo devuelve el UID (un entero) si es exitoso, o 'false' (booleano) si falla.
                if (result instanceof Integer && (Integer) result > 0) {
                    textViewStatus.setText("Connexió exitosa! UID: " + deserializedResult);
                } else {
                    textViewStatus.setText("Error d'autenticació: " + deserializedResult);
                }
            } else {
                textViewStatus.setText("Error de connexió: Resposta desconeguda del servidor.");
            }
        }
    }

    /**
     * Mètode deserialize: converteix la dada rebuda (generalment de l'autenticació d'Odoo)
     * a un String per a la seva visualització o registre.
     *
     * @param data L'objecte rebut de la trucada XML-RPC (pot ser Integer, Boolean, etc.)
     * @return Una representació en String de la dada.
     */
    private String deserialize(Object data) {
        if (data == null) {
            return "null";
        }
        // Simplement convertim l'objecte a la seva representació String.
        // Per a l'autenticació d'Odoo:
        // - Si és èxit, 'data' és un Integer (el UID).
        // - Si falla, 'data' pot ser un Boolean (false).
        // Podries afegir lògica més complexa aquí si la resposta fos una estructura més elaborada.
        return String.valueOf(data);
    }
}