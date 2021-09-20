package com.specknet.pdiotapp.bluetooth;

import static com.specknet.pdiotapp.utils.Utils.bytesToHex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.specknet.pdiotapp.MainActivity;
import com.specknet.pdiotapp.R;
import com.specknet.pdiotapp.utils.Constants;
import com.specknet.pdiotapp.utils.RESpeckPacketHandler;
import com.specknet.pdiotapp.utils.SpeckIntentFilters;
import com.specknet.pdiotapp.utils.ThingyPacketHandler;
import com.specknet.pdiotapp.utils.Utils;

import java.util.Arrays;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;

import rx.Observable;
import rx.Subscription;


public class BluetoothSpeckService extends Service {
    private static final String TAG = "BLT";

    public static RxBleClient rxBleClient;
    private BluetoothAdapter mBluetoothAdapter;

    private Subscription scanSubscription;
    private boolean mIsServiceRunning = false;

    // all Respeck variables
    private RxBleConnection.RxBleConnectionState mLastRESpeckConnectionState;
    private static String RESPECK_UUID;
    private static String RESPECK_BLE_ADDRESS;

    private Subscription respeckLiveSubscription;

    private boolean mIsRESpeckEnabled;

    private RESpeckPacketHandler respeckHandler;

    private RxBleDevice mRESpeckDevice;

    private boolean mIsRESpeckFound;

    private String mRESpeckName;

    private BroadcastReceiver respeckPausedReceiver;
    private BroadcastReceiver respeckIMUChangeReceiver;

    private boolean mIsRESpeckPaused;
    private boolean respeckUseIMUCharacteristic = true;

    private Observable<RxBleConnection> respeckConnection;

    // everything for the Thingy
    private RxBleConnection.RxBleConnectionState mLastThingyConnectionState;

    private static String THINGY_UUID;
    private static String THINGY_BLE_ADDRESS;

    private Subscription thingyLiveSubscription;

    private boolean mIsThingyEnabled;

    private ThingyPacketHandler thingyHandler;

    private RxBleDevice mThingyDevice;

    private boolean mIsThingyFound;

    private String mThingyName;

    private boolean mIsThingyPaused;

    private Observable<RxBleConnection> thingyConnection;

    public BluetoothSpeckService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: here");
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            startMyOwnForeground();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        Log.d(TAG, "startMyOwnForeground: here");
        final int SERVICE_NOTIFICATION_ID = 8598001;
        String NOTIFICATION_CHANNEL_ID = "com.specknet.pdiotapp";
        String channelName = "Respeck Bluetooth Service";
        NotificationChannel chan = null;
        chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.vec_wireless_active)
                .setContentTitle("Airrespeck Bluetooth Service")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        Log.d(TAG, "onStartCommand: here");
        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "Starting SpeckService...");
                startInForeground();
                initSpeckService();
                startServiceAndBluetoothScanning();
            }
        }.start();
        return START_STICKY;
    }

    private void startInForeground() {
        Log.d(TAG, "startInForeground: here");
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new Notification.Builder(this).setContentTitle(
                    getText(R.string.notification_speck_title)).setContentText(
                    getText(R.string.notification_speck_text)).setSmallIcon(
                    R.drawable.vec_wireless_active).setContentIntent(pendingIntent).build();

            // Just use a "random" service ID
            final int SERVICE_NOTIFICATION_ID = 8598001;
            startForeground(SERVICE_NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onDestroy() {
        stopSpeckService();
        Log.i(TAG, "SpeckService has been stopped");
//        int pid = android.os.Process.myPid();
//        android.os.Process.killProcess(pid);

        super.onDestroy();

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't allow threads to bind to this service. Once the service is started, it sends updates
        // via broadcasts and there is no need for calls from outside
        return null;
    }

    /**
     * Initiate Bluetooth adapter.
     */
    public void initSpeckService() {
        Log.d(TAG, "initSpeckService: here");
        loadConfigInstanceVariables();

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(
                Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Get singleton instances of packet handler classes
        respeckHandler = new RESpeckPacketHandler(this);
        thingyHandler = new ThingyPacketHandler(this);

        mIsRESpeckPaused = false;
        mIsThingyPaused = false;

        // Register broadcast receiver to receive respeck off signal
        final IntentFilter intentFilterRESpeckPaused = new IntentFilter();
        intentFilterRESpeckPaused.addAction(Constants.ACTION_RESPECK_RECORDING_PAUSE);
        intentFilterRESpeckPaused.addAction(Constants.ACTION_RESPECK_RECORDING_CONTINUE);
        respeckPausedReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.ACTION_RESPECK_RECORDING_PAUSE)) {
                    Log.i(TAG, "Received message to pause RESpeck recording");
                    mIsRESpeckPaused = true;
                } else if (intent.getAction().equals(Constants.ACTION_RESPECK_RECORDING_CONTINUE)) {
                    Log.i(TAG, "Received message to continue RESpeck recording");
                    mIsRESpeckPaused = false;
                }
            }
        };
        registerReceiver(respeckPausedReceiver, intentFilterRESpeckPaused);

        // register IMU mode change broadcast filter
//        final IntentFilter intentFilterRESpeckIMUMode = SpeckIntentFilters.INSTANCE.getRESpeckIMUIntentFilter();
        respeckIMUChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // maybe the intended state could be included in the broadcast?
                respeckUseIMUCharacteristic = intent
                        .getBooleanExtra(Constants.RESPECK_USE_IMU_CHARACTERISTIC, false);
                Log.i(TAG, String.format("Received RESpeck IMU change intent! Changing characteristic to [%s]", respeckUseIMUCharacteristic));

                // re-establish connection
                setupRespeckSubscription(
                        respeckConnection,
                        respeckUseIMUCharacteristic
                );
            }
        };
        registerReceiver(respeckIMUChangeReceiver, SpeckIntentFilters.INSTANCE.getRESpeckIMUIntentFilter());

        // Provide a method for forcefully scanning for bluetooth devices.
        // Useful when Bluetooth or GPS are dropped.
        // TODO: test with other connected devices
        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "Received request to scan for Bluetooth devices...");
                startServiceAndBluetoothScanning();
            }
        }, SpeckIntentFilters.INSTANCE.getBluetoothServiceScanForDevicesIntentFilter());

    }

    private void loadConfigInstanceVariables() {
        Log.d(TAG, "loadConfigInstanceVariables: here");
        // Get references to Utils
        mIsRESpeckEnabled = true;
        mIsThingyEnabled = true;

        // Get Bluetooth address
        RESPECK_UUID = Utils.getRESpeckUUID(this);
        THINGY_UUID = Utils.getThingyUUID(this);

        Log.i(TAG, "Respeck uuid found = " + RESPECK_UUID);
        Log.i(TAG, "Thingy uuid found = " + THINGY_UUID);
    }

    /**
     * Check Bluetooth availability and initiate devices scanning.
     */
    public void startServiceAndBluetoothScanning() {
        Log.d(TAG, "startServiceAndBluetoothScanning: here");
        mIsServiceRunning = true;

        // Check if Bluetooth is supported on the device
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        mIsRESpeckFound = false;
        mIsThingyFound = false;

        rxBleClient = RxBleClient.create(this);

        Log.i(TAG, "Scanning..");

        scanForDevices();
    }

    private void scanForDevices() {
        Log.d(TAG, "scanForDevices: here");
        scanSubscription = rxBleClient.scanBleDevices().subscribe(rxBleScanResult -> {
//            Log.i(TAG,
//                    "FOUND :" + rxBleScanResult.getBleDevice().getName() + ", " + rxBleScanResult.getBleDevice().getMacAddress());

            if ((mIsRESpeckFound || !mIsRESpeckEnabled) && (mIsThingyFound || !mIsThingyEnabled)) {
                scanSubscription.unsubscribe();
            }

            if (mIsRESpeckEnabled && !mIsRESpeckFound) {
                if (RESPECK_UUID.contains(":")) {
                    // New BLE address
                    if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(RESPECK_UUID)) {
                        RESPECK_BLE_ADDRESS = RESPECK_UUID;
                        mIsRESpeckFound = true;
                        Log.i(TAG, "Connecting after scanning");
                        BluetoothSpeckService.this.connectToRESpeck();
                    }
                } else {
                    // Old UUID
                    byte[] ba = rxBleScanResult.getScanRecord();
                    if (ba != null && ba.length == 62) {
                        byte[] uuid = Arrays.copyOfRange(ba, 7, 15);
                        byte[] uuid4 = Arrays.copyOfRange(ba, 15, 23);
                        Log.i(TAG, "uuid from respeck: " + bytesToHex(uuid));
                        Log.i(TAG, "uuid from respeck4: " + bytesToHex(uuid4));
                        Log.i(TAG, "uuid from config: " + RESPECK_UUID.substring(5));
                        if (bytesToHex(uuid).equalsIgnoreCase(RESPECK_UUID.substring(5)) || bytesToHex(
                                uuid4).equalsIgnoreCase(RESPECK_UUID.substring(5))) {
                            mIsRESpeckFound = true;
                            RESPECK_BLE_ADDRESS = rxBleScanResult.getBleDevice().getMacAddress();
                            Log.i(TAG, "Connecting after scanning to: " + RESPECK_BLE_ADDRESS);
                            BluetoothSpeckService.this.connectToRESpeck();
                        }
                    }
                }
            }

            if (mIsThingyEnabled && !mIsThingyFound) {

                if (rxBleScanResult.getBleDevice().getMacAddress().equalsIgnoreCase(THINGY_UUID)) {
                    THINGY_BLE_ADDRESS = THINGY_UUID;
                    mIsThingyFound = true;
                    Log.i(TAG, "Connecting after scanning");
                    BluetoothSpeckService.this.connectToThingy();
                }

            }

        }, throwable -> {
            // Handle an error here.
            Log.e(TAG, "Error while scanning: " + throwable.toString());
            Log.e(TAG, "Scanning again in 10 seconds");

            // Try again after timeout
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    scanForDevices();
                }
            }, 10000);
        });
    }

    private void connectToRESpeck() {
        Log.d(TAG, "connectToRESpeck: here");
        mRESpeckDevice = rxBleClient.getBleDevice(RESPECK_BLE_ADDRESS);
        mRESpeckName = mRESpeckDevice.getName();

        Log.d(TAG, "connectToRESpeck: mRespeckDevice = " + mRESpeckDevice.toString());
        Log.d(TAG, "connectToRESpeck: mRespeckName = " + mRESpeckName);

        // re-usable connect callback
        Consumer<RxBleConnection.RxBleConnectionState> establishConnectionAndSubscribe = (connectionState) -> {
            respeckConnection = establishRESpeckConnection();
            String fwV = getRESpeckFwVersion();
            respeckHandler.setFwVersion(fwV);

            Log.d(TAG, "connectToRESpeck: set respeck handler");

            final String respeck_characteristic;
            if (fwV.contains("4") || fwV.contains("5") || fwV.contains("6")) {
                // characteristic to use depends on IMU setting
                respeck_characteristic = Constants.RESPECK_LIVE_V4_CHARACTERISTIC;
            } else {
                respeck_characteristic = Constants.RESPECK_LIVE_CHARACTERISTIC;
            }
            Log.i(TAG, String.format("Connecting to RESpeck with characteristic `%s`", respeck_characteristic));

            setupRespeckSubscription(
                    respeckConnection,
                    // specify which handler should process the characteristic values (for IMU / acc)
                    respeckUseIMUCharacteristic
            );

            // update the connection state coming from the connection observer
            mLastRESpeckConnectionState = connectionState;
        };

        mRESpeckDevice.observeConnectionStateChanges().subscribe(connectionState -> {
            if (connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED && mIsServiceRunning) {
                Log.d(TAG, "RESpeck disconnected");
                Intent respeckDisconnectedIntent = new Intent(Constants.ACTION_RESPECK_DISCONNECTED);
                sendBroadcast(respeckDisconnectedIntent);

                if (mLastRESpeckConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                    // If we were just disconnected, try to immediately connect again.
                    Log.i(TAG, "RESpeck connection lost, trying to reconnect");
                    new Timer().schedule(new TimerTask() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void run() {
                            // last connection state was OK - only re-establish the connection
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, 10000);
                } else if (mLastRESpeckConnectionState == RxBleConnection.RxBleConnectionState.CONNECTING) {
                    // This means we tried to reconnect, but there was a timeout. In this case we
                    // wait for x seconds before reconnecting
                    Log.i(TAG, String.format("RESpeck connection timeout, waiting %d seconds before reconnect",
                            Constants.RECONNECTION_TIMEOUT_MILLIS / 1000));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.i(TAG, "RESpeck reconnecting...");
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, Constants.RECONNECTION_TIMEOUT_MILLIS);
                }
            }
        }, throwable -> {
            Log.e(TAG, "Error occured while listening to RESpeck connection state changes: " + throwable.getMessage());
        });

        // first time establishing the connection, so the last state was "disconnected"
        establishConnectionAndSubscribe.accept(RxBleConnection.RxBleConnectionState.DISCONNECTED);
    }

    private void connectToThingy() {
        Log.d(TAG, "connectToThingy: here");
        mThingyDevice = rxBleClient.getBleDevice(THINGY_BLE_ADDRESS);
        mThingyName = mThingyDevice.getName();

        Log.d(TAG, "connectToThingy: mThingyDevice = " + mThingyDevice.toString());
        Log.d(TAG, "connectToThingy: mThingyName = " + mThingyName);

        // re-usable connect callback
        Consumer<RxBleConnection.RxBleConnectionState> establishConnectionAndSubscribe = (connectionState) -> {
            thingyConnection = establishThingyConnection();

            Log.d(TAG, "connectToThingy: set thingy handler");

            // TODO set thingy characteristic here

            final String thingy_characteristic;
            thingy_characteristic = Constants.THINGY_MOTION_CHARACTERISTIC;

            Log.i(TAG, String.format("Connecting to Thingy with characteristic `%s`", thingy_characteristic));

            setupThingySubscription(
                    thingyConnection
            );

            // update the connection state coming from the connection observer
            mLastThingyConnectionState = connectionState;
        };

        mThingyDevice.observeConnectionStateChanges().subscribe(connectionState -> {
            if (connectionState == RxBleConnection.RxBleConnectionState.DISCONNECTED && mIsServiceRunning) {
                Log.d(TAG, "Thingy disconnected");
                Intent thingyDisconnectedIntent = new Intent(Constants.ACTION_THINGY_DISCONNECTED);
                sendBroadcast(thingyDisconnectedIntent);

                if (mLastThingyConnectionState == RxBleConnection.RxBleConnectionState.CONNECTED) {
                    // If we were just disconnected, try to immediately connect again.
                    Log.i(TAG, "Thingy connection lost, trying to reconnect");
                    new Timer().schedule(new TimerTask() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void run() {
                            // last connection state was OK - only re-establish the connection
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, 10000);
                } else if (mLastThingyConnectionState == RxBleConnection.RxBleConnectionState.CONNECTING) {
                    // This means we tried to reconnect, but there was a timeout. In this case we
                    // wait for x seconds before reconnecting
                    Log.i(TAG, String.format("Thingy connection timeout, waiting %d seconds before reconnect",
                            Constants.RECONNECTION_TIMEOUT_MILLIS / 1000));
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            Log.i(TAG, "Thingy reconnecting...");
                            establishConnectionAndSubscribe.accept(connectionState);
                        }
                    }, Constants.RECONNECTION_TIMEOUT_MILLIS);
                }
            }
        }, throwable -> {
            Log.e(TAG, "Error occured while listening to RESpeck connection state changes: " + throwable.getMessage());
        });

        // first time establishing the connection, so the last state was "disconnected"
        establishConnectionAndSubscribe.accept(RxBleConnection.RxBleConnectionState.DISCONNECTED);
    }



    /**
     * Subscribes to a RESpeck bluetooth characteristic
     *
     * @param conn   an existing BLE connection
     * @param useIMU toggle to determine characteristic and processing
     * @apiNote is not set up to handle multiple RESpeck versions, characteristic ID is hardcoded!
     * TODO: update to accept a characteristic and consumer instead of a boolean toggle
     */
    private void setupRespeckSubscription(Observable<RxBleConnection> conn, boolean useIMU) {
        String characteristic = useIMU ?
                Constants.RESPECK_IMU_CHARACTERISTIC_UUID :
                Constants.RESPECK_LIVE_V4_CHARACTERISTIC;

        Log.i(TAG, String.format("Setting up subscription, characteristic: %s", characteristic));
        // specify the consumer function which will handle the RESpeck packets in different modes
//        Consumer<byte[]> cvHandler = useIMU ?
//                (byte[] cv) -> {
//                    RESpeckRawPacket r = RESpeckPacketDecoder.decodeV6PacketIMU(cv);
//                    for (RESpeckSensorData d : r.getBatchData()) {
//                        // From RESpeckPacketHandler
//                        Log.d("IMU Decoder", String.format("RESpeck IMU data: %s", d));
//                        Intent liveDataIntent = new Intent(Constants.ACTION_RESPECK_LIVE_BROADCAST);
//                        liveDataIntent.putExtra(Constants.RESPECK_LIVE_DATA, d.toRESpeckLiveData());
//                        this.sendBroadcast(liveDataIntent);
//                    }
//                } :
//                // Given characteristic has been changed, process the value
//                respeckHandler::processRESpeckLivePacket;

        Consumer<byte[]> cvHandler = useIMU ?
                (byte[] cv) -> respeckHandler.processRESpeckV6Packet(cv, true) : // if using the IMU, must be V6
                respeckHandler::processRESpeckLivePacket;

        // https://polidea.github.io/RxAndroidBle/#change-notifications
        Runnable subscribe = () -> respeckLiveSubscription = conn
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                    Log.i(TAG, "Subscribed to RESpeck");
                    Intent respeckFoundIntent = new Intent(Constants.ACTION_RESPECK_CONNECTED);
                    respeckFoundIntent.putExtra(Constants.Config.RESPECK_UUID, RESPECK_UUID);
                    sendBroadcast(respeckFoundIntent);

                })
                .flatMap(notificationObservable -> notificationObservable)
                .subscribe(
                        bytes -> {
                            if (mIsRESpeckPaused) {
                                Log.i(TAG, "RESpeck packet ignored as paused mode on");
                            } else {
                                // Call requires API level 24 (current min is 22): java.util.function.Consumer#accept
                                cvHandler.accept(bytes);
                            }
                        }, throwable -> {
                            // An error with autoConnect means that we are disconnected
                            String stackTrace = Utils.getStackTraceAsString(throwable);
                            Log.e(TAG, "Respeck disconnected: " + stackTrace);

                            Intent respeckDisconnectedIntent = new Intent(Constants.ACTION_RESPECK_DISCONNECTED);
                            sendBroadcast(respeckDisconnectedIntent);
                        });

        Log.i(TAG, String.format("Unsubscribing from subscription: %s", respeckLiveSubscription));
        long connectDelay;
        try {
            respeckLiveSubscription.unsubscribe();
            Log.i(TAG, "Unsubscribed! Pausing...");
            // if this is an unsubscribe, allow for enough time to change characteristic
            connectDelay = Constants.RESPECK_CHARACTERISTIC_CHANGE_TIMEOUT_MS;
        } catch (Exception e) {
            Log.w(TAG, String.format("Unsubscribe error: %s", e.getMessage()));
            // if the unsubscribe failed, there likely was no subscription,
            // so the connection can happen right away (short delay may be useful)
            connectDelay = 1000;
        }
        /*
         grace period for re-connecting
         TODO: Re-visit with new BLE library.
          - If no time delay is set, the characteristic will still successfully change,
            but there is a bug switching back from the IMU characteristic to the regular one.
          - It feels like the IMU characteristic is still subscribed to after switching,
            stopping the RESpeck from transmitting on the regular characteristic (which is
            intended behaviour in the RESpeck firmware).
          - With this delay, the RESpeck connection is re-established, which is confirmed by
            its LED flashing red (disconnect), then blue (connect).
        */
        (new Handler()).postDelayed(subscribe, connectDelay);
//        subscribe.run();

    }

    private void setupThingySubscription(Observable<RxBleConnection> conn) {
        String characteristic =
                Constants.THINGY_MOTION_CHARACTERISTIC;

        Log.i(TAG, String.format("Setting up subscription, characteristic: %s", characteristic));

        Consumer<byte[]> cvHandler = (byte[] cv) -> thingyHandler.processThingyPacket(cv);

        // https://polidea.github.io/RxAndroidBle/#change-notifications
        Runnable subscribe = () -> thingyLiveSubscription = conn
                .flatMap(rxBleConnection -> rxBleConnection.setupNotification(UUID.fromString(characteristic)))
                .doOnNext(notificationObservable -> {
                    // Notification has been set up
                    Log.i(TAG, "Subscribed to Thingy");
                    Intent thingyFoundIntent = new Intent(Constants.ACTION_THINGY_CONNECTED);
                    thingyFoundIntent.putExtra(Constants.Config.THINGY_UUID, THINGY_UUID);
                    sendBroadcast(thingyFoundIntent);

                })
                .flatMap(notificationObservable -> notificationObservable)
                .subscribe(
                        bytes -> {
                            if (mIsThingyPaused) {
                                Log.i(TAG, "Thingy packet ignored as paused mode on");
                            } else {
                                // Call requires API level 24 (current min is 22): java.util.function.Consumer#accept
                                cvHandler.accept(bytes);
                            }
                        }, throwable -> {
                            // An error with autoConnect means that we are disconnected
                            String stackTrace = Utils.getStackTraceAsString(throwable);
                            Log.e(TAG, "Thingy disconnected: " + stackTrace);

                            Intent thingyDisconnectedIntent = new Intent(Constants.ACTION_THINGY_DISCONNECTED);
                            sendBroadcast(thingyDisconnectedIntent);
                        });

        Log.i(TAG, String.format("Unsubscribing from subscription: %s", thingyLiveSubscription));
        long connectDelay;
        try {
            thingyLiveSubscription.unsubscribe();
            Log.i(TAG, "Unsubscribed from thingy! Pausing...");
            // if this is an unsubscribe, allow for enough time to change characteristic
            connectDelay = Constants.RESPECK_CHARACTERISTIC_CHANGE_TIMEOUT_MS;
        } catch (Exception e) {
            Log.w(TAG, String.format("Unsubscribe error: %s", e.getMessage()));
            // if the unsubscribe failed, there likely was no subscription,
            // so the connection can happen right away (short delay may be useful)
            connectDelay = 1000;
        }
        /*
         grace period for re-connecting
         TODO: Re-visit with new BLE library.
          - If no time delay is set, the characteristic will still successfully change,
            but there is a bug switching back from the IMU characteristic to the regular one.
          - It feels like the IMU characteristic is still subscribed to after switching,
            stopping the RESpeck from transmitting on the regular characteristic (which is
            intended behaviour in the RESpeck firmware).
          - With this delay, the RESpeck connection is re-established, which is confirmed by
            its LED flashing red (disconnect), then blue (connect).
        */
        (new Handler()).postDelayed(subscribe, connectDelay);
//        subscribe.run();

    }

    private Observable<RxBleConnection> establishRESpeckConnection() {
        Log.d(TAG, "establishRESpeckConnection: here");
        Log.i(TAG, "Connecting to RESpeck...");

        return mRESpeckDevice.establishConnection(false);
    }

    private Observable<RxBleConnection> establishThingyConnection() {
        Log.d(TAG, "establishThingyConnection: here");
        Log.i(TAG, "Connecting to Thingy...");

        return mThingyDevice.establishConnection(false);
    }

    // TODO this could be an enum?
    public String getRESpeckFwVersion() {
        Log.d(TAG, "getRESpeckFwVersion: here");
        try {
            if (mRESpeckName.charAt(3) == '6') {
                Log.d(TAG, "getRESpeckFwVersion: returning" + mRESpeckName.substring(3));
                return mRESpeckName.substring(3);
            } else {
                Log.d(TAG, "getRESpeckFwVersion: returning" + mRESpeckName.substring(4));
                return mRESpeckName.substring(4);
            }
        } catch (Exception e) {
            // not connected yet
            Log.d(TAG, "getRESpeckFwVersion: returning -1");
            return "-1";
        }
    }

    public void stopSpeckService() {
        Log.i(TAG, "Stopping SpeckService");
        mIsServiceRunning = false;

        if (scanSubscription != null) {
            Log.i(TAG, "stopSpeckService: unsubscribed scansub");
            scanSubscription.unsubscribe();
        }

        if (respeckLiveSubscription != null) {
            Log.i(TAG, "stopSpeckService: unsubscribed respecksub");
            respeckLiveSubscription.unsubscribe();
        }

        if (thingyLiveSubscription != null) {
            Log.i(TAG, "stopSpeckService: unsubscribed thingysub");
            thingyLiveSubscription.unsubscribe();
        }

        // Close the handlers
        try {
            Log.i(TAG, "stopSpeckService: closing handler");
            respeckHandler.closeHandler();
        } catch (Exception e) {
            Log.e(TAG, "Error while closing handlers: " + e.getMessage());
        }

        if (respeckPausedReceiver != null ) {
            Log.i(TAG, "stopSpeckService: unregistered receiver");
            unregisterReceiver(respeckPausedReceiver);
        }

        if (respeckIMUChangeReceiver != null) {
            unregisterReceiver(respeckIMUChangeReceiver);
        }

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

}