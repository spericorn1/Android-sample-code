package com.tempphoto.ui.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentTransaction;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.tempphoto.R;
import com.tempphoto.app.TempPhotoApplication;
import com.tempphoto.manager.SensorManager;
import com.tempphoto.manager.TPLocationManager;
import com.tempphoto.sensors.bluetooth.BluetoothDeviceList;
import com.tempphoto.sensors.bluetooth.BluetoothSensor;
import com.tempphoto.manager.PhotoManager;
import com.tempphoto.models.Photo;
import com.tempphoto.ui.adapter.ThumbnailListAdapter;
import com.tempphoto.ui.fragment.AboutFragment;
import com.tempphoto.ui.fragment.AboutFragment_;
import com.tempphoto.ui.fragment.AlertDialog;
import com.tempphoto.ui.fragment.AlertDialog_;
import com.tempphoto.ui.fragment.HelpFragment_;
import com.tempphoto.ui.fragment.ListDialogFragment;
import com.tempphoto.ui.fragment.ListFragment;
import com.tempphoto.ui.fragment.ListFragment_;
import com.tempphoto.ui.fragment.SettingsFragment;
import com.tempphoto.ui.fragment.SettingsFragment_;
import com.tempphoto.ui.fragment.TakePhotoFragment;
import com.tempphoto.ui.fragment.TakePhotoFragment_;
import com.tempphoto.voice.AlternativePhrases;
import com.tempphoto.voice.Command;
import com.tempphoto.voice.SpeechRecognizer;
import com.tempphoto.voice.VoiceConfirmation;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OnActivityResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Locale;

@EActivity
public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, BluetoothSensor.BluetoothReceiveListener, SpeechRecognizer.CommandListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Menu menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean eulaAccepted = mPreference.eulaAccepted().getOr(false);
        boolean loggedIn = mPreference.loggedIn().getOr(true);

        if (!eulaAccepted) {
            showEulaActivity();
        } else if (!loggedIn) {
            showLoginActivity();
        } else {
            TempPhotoApplication.setMainActivity(this);

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            // Push fragment after a delay to get the proper layout size.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onPostResume();
                            showTakePhotoFragment();
                        }
                    });
                }
            }, 1500);

            // Init Bluetooth sensors
            initializeBluetoothDevice();
        }

//        sendValues();
    }


    protected void showLoginActivity() {
        Intent intent = new Intent(this, LoginActivity_.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(intent);
    }

    protected void showEulaActivity() {
        Intent intent = new Intent(this, EulaActivity_.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            start();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.take_photo) {
            showTakePhotoFragment();
        } else if (id == R.id.list) {
            showListFragment();
        } else if (id == R.id.settings) {
            showSettingsFragment();
        } else if (id == R.id.help) {
            showHelpFragment();
        } else if (id == R.id.about) {
            showAboutFragment();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void editPhoto(final Photo photo) {
        if (photo != PhotoManager.getInstance().getCurrentPhoto()) {
            showTakePhotoFragment();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (takePhotoFragment().isAdded()) {
                        takePhotoFragment().editPhoto(photo);
                    }
                }
            }, 100);
        }
    }


    public void editPhotoCommand(Command command, Photo photo) {
        if (photo != PhotoManager.getInstance().getCurrentPhoto()) {
            showTakePhotoFragment();
            if (takePhotoFragment().isAdded()) {
                takePhotoFragment().editPhotoCommand(command, photo);
            }
        }
    }

    public void forceTakePhoto() {
        if (takePhotoFragment().isAdded()) {
            takePhotoFragment().forceTakePhoto();
        }
    }

    private void showTakePhotoFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.containerLayout, takePhotoFragment());
        transaction.commit();
    }

    private void showSettingsFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.containerLayout, settingsFragment());
        transaction.commitAllowingStateLoss();
    }

    private void showHelpFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.containerLayout, new HelpFragment_());
        transaction.commit();
    }

    private void showListFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.containerLayout, listFragment());
        transaction.commit();
    }

    public void showListDialogFragment() {
        listDialogFragment().show(getSupportFragmentManager(), "Tag");
        thumbnailsDialogShown = true;
    }

    private void showAboutFragment() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.containerLayout, new AboutFragment_());
        transaction.commit();
    }

    private ListDialogFragment listDialogFragment;

    private ListDialogFragment listDialogFragment() {
        if (listDialogFragment == null) {
            listDialogFragment = ListDialogFragment.newInstance(ThumbnailListAdapter.ListMode.ListModeThumbnailsAction, onDismissListener);
        }
        return listDialogFragment;
    }

    boolean thumbnailsDialogShown = false;
    DialogInterface.OnDismissListener onDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            thumbnailsDialogShown = false;
        }
    };

    private TakePhotoFragment takePhotoFragment;

    public TakePhotoFragment takePhotoFragment() {
        if (takePhotoFragment == null) {
            takePhotoFragment = new TakePhotoFragment_();
        }
        return takePhotoFragment;
    }

    private ListFragment listFragment;

    public ListFragment listFragment() {
        if (listFragment == null) {
            listFragment = new ListFragment_();
        }
        return listFragment;
    }

    private SettingsFragment settingsFragment;

    public SettingsFragment settingsFragment() {
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment_();
        }
        return settingsFragment;
    }

    /*******************************************************/
    /***********************Location***********************/
    /******************************************************/

    public static AlertDialog enableGPSAlert = null;

    public void showEnableGPSAlert() {
        if (enableGPSAlert == null) {
            AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                    .mTitle(getString(R.string.gps_not_enabled))
                    .mMessage(getString(R.string.enable_gps))
                    .mPositiveButtonText(getString(R.string.alert_yes))
                    .mNeutralButtonText(getString(R.string.alert_no));
            enableGPSAlert = alert.build();
            enableGPSAlert.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
                @Override
                public void onPositiveClick() {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }

                @Override
                public void onNeutralClick() {
                    //NOP
                }

                @Override
                public void onNegativeClick() {

                }
            });
        }
        if (!enableGPSAlert.isAdded()) {
            enableGPSAlert.show(getSupportFragmentManager(), AlertDialog.TAG);
        }
    }

    /*******************************************************/
    /***********************Bluetooth*********************/
    /******************************************************/

    public static final int REQUEST_PAIR_DEVICE = 4097;
    public static final int REQUEST_ENABLE_BLUETOOTH = 4096;
    public static AlertDialog enableBluetoothAlert = null;
    public static AlertDialog pairDeviceAlert = null;
    public static AlertDialog deviceSelectionAlert = null;

    public void showEnableBluetoothAlert() {
        if (enableBluetoothAlert == null) {
            AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                    .mTitle(getString(R.string.bluetooth_turned_off))
                    .mMessage(getString(R.string.bluetooth_turned_off_message))
                    .mPositiveButtonText(getString(R.string.alert_ok))
                    .mNeutralButtonText(getString(R.string.alert_cancel));
            enableBluetoothAlert = alert.build();
            enableBluetoothAlert.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
                @Override
                public void onPositiveClick() {
                    // Enable bluetooth
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH);
                }

                @Override
                public void onNeutralClick() {
                    //NOP
                }

                @Override
                public void onNegativeClick() {

                }
            });
        }
        if (!enableBluetoothAlert.isAdded()) {
            enableBluetoothAlert.show(getSupportFragmentManager(), AlertDialog.TAG);
        }
    }

    public void showPairDeviceAlert() {
        if (pairDeviceAlert == null) {
            AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                    .mTitle(getString(R.string.no_paired_device))
                    .mMessage(getString(R.string.no_paired_device_message))
                    .mPositiveButtonText(getString(R.string.alert_yes))
                    .mNeutralButtonText(getString(R.string.alert_no));
            pairDeviceAlert = alert.build();
            pairDeviceAlert.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
                @Override
                public void onPositiveClick() {
                    Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivityForResult(intent, REQUEST_PAIR_DEVICE);
                }

                @Override
                public void onNeutralClick() {
                    //NOP
                }

                @Override
                public void onNegativeClick() {

                }
            });
        }
        if (!pairDeviceAlert.isAdded()) {
            pairDeviceAlert.show(getSupportFragmentManager(), AlertDialog.TAG);
        }
    }

    public void showDeviceSelectionAlert() {
        if (deviceSelectionAlert == null) {
            AlertDialog_.FragmentBuilder_ alert = AlertDialog_.builder()
                    .mTitle(getString(R.string.select_device))
                    .mMessage(getString(R.string.select_device_message))
                    .mPositiveButtonText(getString(R.string.alert_ok))
                    .mNeutralButtonText(getString(R.string.alert_cancel));
            deviceSelectionAlert = alert.build();
            deviceSelectionAlert.setOptionClickListener(new AlertDialog.OnOptionClickListener() {
                @Override
                public void onPositiveClick() {
                    showSettingsFragment();
                }

                @Override
                public void onNeutralClick() {
                    //NOP
                }

                @Override
                public void onNegativeClick() {

                }
            });
        }
        if (!deviceSelectionAlert.isAdded()) {
            deviceSelectionAlert.show(getSupportFragmentManager(), AlertDialog.TAG);
        }
    }

    public void initializeBluetoothDevice() {
        if (!BluetoothDeviceList.isBluetoothEnabled()) {
            showEnableBluetoothAlert();
            takePhotoFragment().deviceNotConnected();
        } else {
            final ArrayList<String> bluetoothDeviceList = BluetoothDeviceList.getPairedDevices();
            if (bluetoothDeviceList.size() == 0) {
                showPairDeviceAlert();
                takePhotoFragment().deviceNotConnected();
            } else {
                String savedDeviceId = mPreference.bluetoothDeviceName().getOr("");
                if (savedDeviceId != null && savedDeviceId.length() > 0) {
                    if (bluetoothDeviceList.contains(savedDeviceId)) {
                        startBluetoothService();
                    } else {
                        takePhotoFragment().deviceNotConnected();
                        showDeviceSelectionAlert();
                    }
                } else {
                    takePhotoFragment().deviceNotConnected();
                    showDeviceSelectionAlert();
                }
            }
        }
    }

    public void startBluetoothService() {
        try {
            takePhotoFragment().deviceConnecting();
            String bluetoothDeviceAddress = mPreference.bluetoothDeviceAddress().get();
            SensorManager.getInstance().startBluetoothSensor(bluetoothDeviceAddress, getApplicationContext(), this);
        } catch (Exception e) {
            Log.e(TAG, "Bluetooth device error");
            e.printStackTrace();
            takePhotoFragment().sensorMessageReceived(getString(R.string.device_not_responding));
            takePhotoFragment().deviceNotConnected();
        }
    }

    @OnActivityResult(REQUEST_ENABLE_BLUETOOTH)
    protected void onEnableBluetoothResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            initializeBluetoothDevice();
        }
    }

    @OnActivityResult(REQUEST_PAIR_DEVICE)
    protected void onPairDeviceResult(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            initializeBluetoothDevice();
        }
    }


    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public void sendValues() {
        Double value = round(Math.random() * 100, 2);
//        Double centigrade = Math.ceil((value - 32) / 1.8);
//        mCurrentTemp = value.toString() + "F / " + centigrade + "C";
//        mTempView.setText(mCurrentTemp);

        onDataReceived(BluetoothSensor.STATUS_CODE_AWAITING_VALUES, value.toString());

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendValues();
            }
        }, 1000);
    }

//
//    @Override
//    public void onDataReceived(Integer statusCode, String value) {
//        switch (statusCode) {
//
//        }
//    }


    @Override
    public void onDataReceived(int statusCode, String data) {
        switch (statusCode) {
            case BluetoothSensor.STATUS_CODE_DATA_RECEIVED:
                String value = null;

                if (data.toLowerCase().startsWith("l")) {
                    // Length values
                    value = data.substring(1).trim() + " mm";
                } else if (data.toLowerCase().startsWith("w")) {
                    // force or weight values
                    value = data.substring(1).trim() + " grams";
                } else if (data.toLowerCase().startsWith("r")) {
                    // rfid id
                    value = data.substring(1).trim() + " id";
                } else {
                    // Temp values
                    Double centigrade = Math.ceil((Double.parseDouble(data) - 32) / 1.8);

                    int tempFormat = Integer.parseInt(mPreference.tempFormat().getOr("100"));
                    String currentTemp = null;
                    if (tempFormat == 0) {
                        currentTemp = data + "F";
                    } else if (tempFormat == 50) {
                        currentTemp = centigrade + "C";
                    } else {
                        currentTemp = data + "F / " + centigrade + "C";
                    }

                    value = currentTemp;
                }

                takePhotoFragment().sensorValueReceived(value);
                break;

            case BluetoothSensor.STATUS_CODE_DEVICE_NOT_RESPONDING:
                takePhotoFragment().sensorMessageReceived(getString(R.string.device_not_responding));
                break;
            case BluetoothSensor.STATUS_CODE_AWAITING_VALUES:
                takePhotoFragment().sensorMessageReceived(getString(R.string.awaiting_for_results));
                break;
            case BluetoothSensor.STATUS_CODE_CONNECTED:
//                    takePhotoFragment().sensorMessageReceived(getString(R.string.awaiting_for_results));
                takePhotoFragment().deviceConnected();
                break;
            case BluetoothSensor.STATUS_CODE_CONNECT:
//                    takePhotoFragment().sensorMessageReceived(getString(R.string.awaiting_for_results));
                takePhotoFragment().deviceConnecting();
                break;
        }
    }


    /*****************************************************************************/
    /*********************************Speech************************************/
    /****************************************************************************/

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech mTts;

    private static final int MY_DATA_CHECK_CODE = 2048;

    protected void initTextToSpeech() {
        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    // Set preferred language to US english.
                    // Note that a language may not be available, and the result will indicate this.
                    int result = mTts.setLanguage(Locale.US);
                    // Try this someday for some interesting results.
                    // int result mTts.setLanguage(Locale.FRANCE);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Lanuage data is missing or the language is not supported.
                        Log.e("TAG", "Language is not available.");
                    } else {
                        // Check the documentation for other possible result codes.
                        // For example, the language may be available for the locale,
                        // but not for the specified country and variant.

                        // The TTS engine has been successfully initialized.
                        Log.e("TAG", "TextToSpeech initialized");
                    }
                } else {
                    // Initialization failed.
                    Log.e("TAG", "Could not initialize TextToSpeech.");
                }
            }
        });
    }

    public void updateStartButton() {
        if (speechRecognizer != null && speechRecognizer.isListening()) {
            if (menu != null) {
                menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.icon_mic_active));
            }
            Toast.makeText(this, getText(R.string.voice_commands_active), Toast.LENGTH_SHORT).show();
        } else {
            if (menu != null) {
                menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.icon_mic_default));
            }
            Toast.makeText(this, getText(R.string.voice_commands_inactive), Toast.LENGTH_SHORT).show();
        }
    }

    protected void start() {
        if (speechRecognizer.isListening()) {
            speechRecognizer.destroy();
        } else {
            if (mTts == null) {
                startTtsInitialization();
            } else {
                speechRecognizer.start();
            }
        }

        updateStartButton();
    }

    protected void startTtsInitialization() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
    }

    @Override
    public void commandReceived(Command c) {
        if (menu != null) {
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.icon_mic_success));
        }
        switch (c) {
            case CAMERA:
                takePhotoFragment().commandCamera(c);
                break;
            case REMOVE_ALL:
                if (listDialogFragment().isAdded() || listFragment().isAdded()) {
                    listDialogFragment().commandClearAll(c);
                    listFragment().commandClearAll(c);
                } else {
                    takePhotoFragment().commandClearAll(c);
                }
                break;
            case CLEAR_DOTS:
                takePhotoFragment().commandClearDots(c);
                break;
            case CLEAR_PHOTO:
                takePhotoFragment().commandClearPhoto(c);
                break;
            case CLEAR:
                takePhotoFragment().commandClear(c);
                break;
            case DELETE:
                listFragment().commandDeletePhoto(c);
                listDialogFragment().commandDeletePhoto(c);
                break;
            case DONE:
                takePhotoFragment().commandDone(c);
                break;
            case EXIT:
                takePhotoFragment().commandExit(c);
                break;
            case GO:
                takePhotoFragment().commandGo(c);
                break;
            case HELP:
                showHelpFragment();
                break;
            case LIST_PHOTOS:
                takePhotoFragment().commandListPhotos(c);
                break;
            case MODIFY:
                listFragment().commandModifyPhoto(c);
                listDialogFragment().commandModifyPhoto(c);
                break;
            case PLAY:
                takePhotoFragment().commandPlay(c);
                break;
            case REMOVE:
                takePhotoFragment().commandRemove(c);
                break;
            case SAVE:
                takePhotoFragment().commandSave(c);
                break;
            case SEND_ALL:
                listFragment().commandSendAll(c);
                listDialogFragment().commandSendAll(c);
                break;
            case SEND:
                listFragment().commandSendPhoto(c);
                listDialogFragment().commandSendPhoto(c);
                break;
            case SETTINGS:
                showSettingsFragment();
                break;
            case SETUP:
                takePhotoFragment().commandSetup(c);
                break;
            case TAKE_PHOTO:
                showTakePhotoFragment();
                break;
            case STOP:
                speechRecognizer.stop();
                updateStartButton();
                break;
            case YES:
                Toast.makeText(this, getText(R.string.alert_yes), Toast.LENGTH_SHORT).show();
                VoiceConfirmation.getInstance(this).commandYes(c);
                break;
            case NO:
                Toast.makeText(this, getText(R.string.alert_no), Toast.LENGTH_SHORT).show();
                VoiceConfirmation.getInstance(this).commandNo(c);
                break;
            case CANCEL:
                Toast.makeText(this, getText(R.string.alert_cancel), Toast.LENGTH_SHORT).show();
                VoiceConfirmation.getInstance(this).commandCancel(c);
                break;
        }
    }

    @Override
    public void speechRecognitionError(String message) {
        if (menu != null) {
            menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.icon_mic_error));
        }

        showLongToast(message);
        if (speechRecognizer != null) {
            speechRecognizer.stop();
        }

        updateStartButton();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.i("Speech", "onRmsChanged: " + rmsdB);
        if (rmsdB > 2) {
            if (menu != null) {
                menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.icon_mic_success));
            }
        } else {
            if (menu != null) {
                menu.getItem(0).setIcon(getResources().getDrawable(R.drawable.icon_mic_active));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Initialize speech recognizer
        if (speechRecognizer == null) {
            speechRecognizer = new SpeechRecognizer(this);
            speechRecognizer.commandListener = this;
        }

        // Initialize text to speech
        startTtsInitialization();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize alternative phrases file
        PhotoManager.getInstance();
        AlternativePhrases.getInstance();

        if (speechRecognizer != null) {
            speechRecognizer.reactivate();
        }
        updateStartButton();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        SensorManager.getInstance().stopBluetoothSensor();
    }

    @OnActivityResult(MY_DATA_CHECK_CODE)
    protected void onSpeechToTextResult(int resultCode) {
        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
            // success, create the TTS instance
            initTextToSpeech();
        } else {
            // missing data, install it
            Intent installIntent = new Intent();
            installIntent.setAction(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
            startActivity(installIntent);
        }
    }


    public void playText(String text) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, Math.random() + "");
        } else {
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


}
