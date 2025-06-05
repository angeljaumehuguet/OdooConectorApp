package com.example.odooconectorapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerActivity extends AppCompatActivity {

    private EditText editTextFilter, editTextName, editTextEmail, editTextPhone;
    private Button buttonSearch, buttonAdd, buttonUpdate, buttonDelete;
    private ListView listViewCustomers;
    private TextView textViewStatus;

    private String serverUrl, database, username, password;
    private int uid;
    private ArrayList<Map<String, Object>> customersList;
    private ArrayAdapter<String> customersAdapter;
    private ArrayList<String> customersDisplay;
    private Map<String, Object> selectedCustomer;

    private static final String TAG = "CustomerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);

        // Obtener datos de conexión del Intent
        Intent intent = getIntent();
        serverUrl = intent.getStringExtra("serverUrl");
        database = intent.getStringExtra("database");
        username = intent.getStringExtra("username");
        password = intent.getStringExtra("password");
        uid = intent.getIntExtra("uid", -1);

        if (uid == -1) {
            Toast.makeText(this, "Error: Dades de connexió no vàlides", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();

        // Cargar todos los clientes al inicio
        new SearchCustomersTask().execute("");
    }

    private void initializeViews() {
        editTextFilter = findViewById(R.id.editTextFilter);
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPhone = findViewById(R.id.editTextPhone);
        buttonSearch = findViewById(R.id.buttonSearch);
        buttonAdd = findViewById(R.id.buttonAdd);
        buttonUpdate = findViewById(R.id.buttonUpdate);
        buttonDelete = findViewById(R.id.buttonDelete);
        listViewCustomers = findViewById(R.id.listViewCustomers);
        textViewStatus = findViewById(R.id.textViewStatus);

        customersList = new ArrayList<>();
        customersDisplay = new ArrayList<>();
        customersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, customersDisplay);
        listViewCustomers.setAdapter(customersAdapter);

        // Inicialmente deshabilitamos los botones de Update y Delete
        buttonUpdate.setEnabled(false);
        buttonDelete.setEnabled(false);
    }

    private void setupListeners() {
        buttonSearch.setOnClickListener(v -> {
            String filter = editTextFilter.getText().toString().trim();
            new SearchCustomersTask().execute(filter);
        });

        buttonAdd.setOnClickListener(v -> {
            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String phone = editTextPhone.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "El nom és obligatori", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> customerData = new HashMap<>();
            customerData.put("name", name);
            customerData.put("email", email);
            customerData.put("phone", phone);
            customerData.put("is_company", false);
            customerData.put("customer_rank", 1);

            new CreateCustomerTask().execute(customerData);
        });

        buttonUpdate.setOnClickListener(v -> {
            if (selectedCustomer == null) {
                Toast.makeText(this, "Selecciona un client primer", Toast.LENGTH_SHORT).show();
                return;
            }

            String name = editTextName.getText().toString().trim();
            String email = editTextEmail.getText().toString().trim();
            String phone = editTextPhone.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "El nom és obligatori", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> customerData = new HashMap<>();
            customerData.put("name", name);
            customerData.put("email", email);
            customerData.put("phone", phone);

            int customerId = (Integer) selectedCustomer.get("id");
            new UpdateCustomerTask().execute(customerId, customerData);
        });

        buttonDelete.setOnClickListener(v -> {
            if (selectedCustomer == null) {
                Toast.makeText(this, "Selecciona un client primer", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Confirmar eliminació")
                    .setMessage("Estàs segur que vols eliminar aquest client?")
                    .setPositiveButton("Sí", (dialog, which) -> {
                        int customerId = (Integer) selectedCustomer.get("id");
                        new DeleteCustomerTask().execute(customerId);
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        listViewCustomers.setOnItemClickListener((parent, view, position, id) -> {
            selectedCustomer = customersList.get(position);

            // Cargar datos en los campos
            editTextName.setText((String) selectedCustomer.get("name"));
            editTextEmail.setText(selectedCustomer.get("email") != null ?
                    (String) selectedCustomer.get("email") : "");
            editTextPhone.setText(selectedCustomer.get("phone") != null ?
                    (String) selectedCustomer.get("phone") : "");

            // Habilitar botones de Update y Delete
            buttonUpdate.setEnabled(true);
            buttonDelete.setEnabled(true);

            textViewStatus.setText("Client seleccionat: " + selectedCustomer.get("name"));
        });
    }

    private void clearForm() {
        editTextName.setText("");
        editTextEmail.setText("");
        editTextPhone.setText("");
        selectedCustomer = null;
        buttonUpdate.setEnabled(false);
        buttonDelete.setEnabled(false);
    }

    // AsyncTask para buscar clientes
    private class SearchCustomersTask extends AsyncTask<String, Void, List<Map<String, Object>>> {
        private Exception exception = null;

        @Override
        protected void onPreExecute() {
            textViewStatus.setText("Cercant clients...");
        }

        @Override
        protected List<Map<String, Object>> doInBackground(String... params) {
            String keyword = params[0];

            try {
                XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                config.setServerURL(new URL(serverUrl + "/xmlrpc/2/object"));

                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(config);

                // Condiciones para buscar solo clientes
                List<Object> conditions;
                if (keyword.isEmpty()) {
                    // Solo clientes (customer_rank > 0)
                    conditions = Arrays.asList(
                            Arrays.asList("customer_rank", ">", 0)
                    );
                } else {
                    // Clientes que coincidan con el filtro de nombre
                    conditions = Arrays.asList(
                            Arrays.asList("customer_rank", ">", 0),
                            Arrays.asList("name", "ilike", keyword)
                    );
                }

                // Buscar IDs de clientes
                Object[] searchParams = new Object[]{
                        database, uid, password, "res.partner", "search",
                        new Object[]{conditions}
                };
                Object[] customerIds = (Object[]) client.execute("execute_kw", searchParams);

                if (customerIds.length == 0) {
                    return new ArrayList<>();
                }

                // Leer datos de los clientes
                Object[] readParams = new Object[]{
                        database, uid, password, "res.partner", "read",
                        new Object[]{customerIds, Arrays.asList("name", "email", "phone")}
                };
                Object[] customersData = (Object[]) client.execute("execute_kw", readParams);

                List<Map<String, Object>> result = new ArrayList<>();
                for (Object customerObj : customersData) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> customer = (Map<String, Object>) customerObj;
                    result.add(customer);
                }

                return result;

            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "Error buscant clients: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Map<String, Object>> result) {
            if (exception != null) {
                textViewStatus.setText("Error: " + exception.getMessage());
                return;
            }

            if (result == null) {
                textViewStatus.setText("Error en la cerca");
                return;
            }

            customersList.clear();
            customersDisplay.clear();

            for (Map<String, Object> customer : result) {
                customersList.add(customer);
                String displayText = customer.get("name") +
                        (customer.get("email") != null ? " (" + customer.get("email") + ")" : "");
                customersDisplay.add(displayText);
            }

            customersAdapter.notifyDataSetChanged();
            textViewStatus.setText("Trobats " + result.size() + " clients");
        }
    }

    // AsyncTask para crear cliente
    private class CreateCustomerTask extends AsyncTask<Map<String, Object>, Void, Integer> {
        private Exception exception = null;

        @Override
        protected void onPreExecute() {
            textViewStatus.setText("Creant client...");
        }

        @Override
        protected Integer doInBackground(Map<String, Object>... params) {
            Map<String, Object> customerData = params[0];

            try {
                XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                config.setServerURL(new URL(serverUrl + "/xmlrpc/2/object"));

                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(config);

                Object[] createParams = new Object[]{
                        database, uid, password, "res.partner", "create",
                        new Object[]{customerData}
                };

                Object result = client.execute("execute_kw", createParams);
                return (Integer) result;

            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "Error creant client: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (exception != null) {
                textViewStatus.setText("Error: " + exception.getMessage());
                return;
            }

            if (result != null) {
                textViewStatus.setText("Client creat amb ID: " + result);
                clearForm();
                // Recargar la lista
                new SearchCustomersTask().execute("");
            } else {
                textViewStatus.setText("Error creant el client");
            }
        }
    }

    // AsyncTask para actualizar cliente
    private class UpdateCustomerTask extends AsyncTask<Object, Void, Boolean> {
        private Exception exception = null;

        @Override
        protected void onPreExecute() {
            textViewStatus.setText("Actualitzant client...");
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Integer customerId = (Integer) params[0];
            @SuppressWarnings("unchecked")
            Map<String, Object> customerData = (Map<String, Object>) params[1];

            try {
                XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                config.setServerURL(new URL(serverUrl + "/xmlrpc/2/object"));

                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(config);

                Object[] updateParams = new Object[]{
                        database, uid, password, "res.partner", "write",
                        new Object[]{Arrays.asList(customerId), customerData}
                };

                Object result = client.execute("execute_kw", updateParams);
                return (Boolean) result;

            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "Error actualitzant client: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (exception != null) {
                textViewStatus.setText("Error: " + exception.getMessage());
                return;
            }

            if (result) {
                textViewStatus.setText("Client actualitzat correctament");
                clearForm();
                // Recargar la lista
                new SearchCustomersTask().execute("");
            } else {
                textViewStatus.setText("Error actualitzant el client");
            }
        }
    }

    // AsyncTask para eliminar cliente
    private class DeleteCustomerTask extends AsyncTask<Integer, Void, Boolean> {
        private Exception exception = null;

        @Override
        protected void onPreExecute() {
            textViewStatus.setText("Eliminant client...");
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            Integer customerId = params[0];

            try {
                XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
                config.setServerURL(new URL(serverUrl + "/xmlrpc/2/object"));

                XmlRpcClient client = new XmlRpcClient();
                client.setConfig(config);

                Object[] deleteParams = new Object[]{
                        database, uid, password, "res.partner", "unlink",
                        new Object[]{Arrays.asList(customerId)}
                };

                Object result = client.execute("execute_kw", deleteParams);
                return (Boolean) result;

            } catch (Exception e) {
                this.exception = e;
                Log.e(TAG, "Error eliminant client: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (exception != null) {
                textViewStatus.setText("Error: " + exception.getMessage());
                return;
            }

            if (result) {
                textViewStatus.setText("Client eliminat correctament");
                clearForm();
                // Recargar la lista
                new SearchCustomersTask().execute("");
            } else {
                textViewStatus.setText("Error eliminant el client");
            }
        }
    }
}