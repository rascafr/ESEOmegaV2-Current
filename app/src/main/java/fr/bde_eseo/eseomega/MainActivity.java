package fr.bde_eseo.eseomega;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import fr.bde_eseo.eseomega.adapter.NavDrawerListAdapter;
import fr.bde_eseo.eseomega.community.CommunityFragment;
import fr.bde_eseo.eseomega.events.EventsFragment;
import fr.bde_eseo.eseomega.gcmpush.QuickstartPreferences;
import fr.bde_eseo.eseomega.gcmpush.RegistrationIntentService;
import fr.bde_eseo.eseomega.ingenews.IngeListActivity;
import fr.bde_eseo.eseomega.news.SimpleHTML;
import fr.bde_eseo.eseomega.profile.ConnectProfileFragment;
import fr.bde_eseo.eseomega.lacommande.DataManager;
import fr.bde_eseo.eseomega.lacommande.OrderTabsFragment;
import fr.bde_eseo.eseomega.profile.ViewProfileFragment;
import fr.bde_eseo.eseomega.hintsntips.TipsFragment;
import fr.bde_eseo.eseomega.interfaces.OnItemAddToCart;
import fr.bde_eseo.eseomega.interfaces.OnUserProfileChange;
import fr.bde_eseo.eseomega.model.NavDrawerItem;
import fr.bde_eseo.eseomega.profile.UserProfile;
import fr.bde_eseo.eseomega.lacommande.OrderListFragment;
import fr.bde_eseo.eseomega.news.NewsListFragment;
import fr.bde_eseo.eseomega.utils.ConnexionUtils;
import fr.bde_eseo.eseomega.utils.EncryptUtils;
import fr.bde_eseo.eseomega.utils.ImageUtils;
import fr.bde_eseo.eseomega.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements OnUserProfileChange, OnItemAddToCart {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DrawerLayout mDrawerLayout;
    private RecyclerView recList;
    private ActionBarDrawerToggle mDrawerToggle;

    // GCM
    //private BroadcastReceiver mRegistrationBroadcastReceiver;

    // Store app verification
    private final static String SIGNATURE = "mM6h5mqKyeuhXgBF8SLnMBKc7BE=";

    // Log TAG
    //private static final String TAG = "ESEOmain";

    // Help Text
    private static final String HELP_DIALOG_TEXT =  "Cette application est l'application officielle ESEOmega pour Android.\n\n" +
                                                    "Développeur :\nFrançois Leparoux\n\n" +
                                                    "Support technique :\ndevelopers.eseomega@gmail.com\n\n" +
                                                    "© 2015 ESEOmega";
    // Developer mail
    private static final String MAIL_DIALOG = "developers.eseomega@gmail.com";

    // Others constant values
    public static final int MAX_PROFILE_SIZE = 256; // seems good

    // Version app
    private String appVersion = "2.0";

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title
    private CharSequence mTitle;

    // Profile item
    UserProfile profile = new UserProfile();

    // Preferences
    private SharedPreferences prefs_Read;
    private SharedPreferences.Editor prefs_Write;

    // GCM
    private BroadcastReceiver mRegistrationBroadcastReceiver;

    // slide menu items
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;
    private ListView mDrawerList;
    private NavDrawerListAdapter navAdapter;
    private ArrayList<NavDrawerItem> navDrawerItems;

    // Material Toolbar
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Global UI View
        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        toolbar.setPadding(0, getStatusBarHeight(), 0, 0);
        setSupportActionBar(toolbar);
        mTitle = getTitle();

        // Check app auth
        boolean installPlayStore = verifyInstaller(this);
        boolean installSigned = checkAppSignature(this);

        if (!BuildConfig.DEBUG && (!installPlayStore || !installSigned)) {
            new MaterialDialog.Builder(this)
                .title(R.string.bad_signature_title)
                .content(R.string.bad_signature_content)
                .negativeText(R.string.bad_signature_button)
                .cancelable(false)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        MainActivity.this.finish();
                    }
                }).show();
        }

        // GCM Receiver
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d("NOTIF", "onReceive !");
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences.getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                ConnectProfileFragment mFragment = (ConnectProfileFragment) getSupportFragmentManager().findFragmentByTag("frag0");
                if (mFragment != null) {
                    mFragment.setPushRegistration(sentToken);
                    //Log.d("FRAG", "mFragment = " + (mFragment.isVisible() ? "visible" : "invisible"));
                } else {
                    //Log.d("FRAG", "mFragment = null");
                }


            }
        };

        // load slide menu items
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

        // nav drawer icons from resources
        navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        navDrawerItems = new ArrayList<>();

        // add profile view item
        profile.readProfilePromPrefs(this);
        navDrawerItems.add(profile.getDrawerProfile());

        // adding nav drawer items to array
        for (int it=1;it<navMenuTitles.length;it++)
            navDrawerItems.add(new NavDrawerItem(navMenuTitles[it], navMenuIcons.getResourceId(it, -1)));

        // Recycle the typed array
        navMenuIcons.recycle();

        // Listen events
        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

        // setting the nav drawer list adapter
        navAdapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
        // set picture
        // WARNING ! Bitmap could be null if picture is removed from storage !
        // EDIT : It's ok, getResizedBitmap has been modified to survive to that kind of mistake
        // TODO : correct photo orientation
        // @see http://stackoverflow.com/questions/7286714/android-get-orientation-of-a-camera-bitmap-and-rotate-back-90-degrees
        File fp = new File(profile.getPicturePath());
        if (fp.exists())
            navAdapter.setBitmap(ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(profile.getPicturePath()), MAX_PROFILE_SIZE));

        // set data adapter to our listview
        mDrawerList.setAdapter(navAdapter);

        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setIcon(new ColorDrawable(getResources().getColor(android.R.color.transparent)));

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                //super.onDrawerSlide(drawerView, slideOffset);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Receive Intent from notification
        Bundle extras = getIntent().getExtras();
        String message, title;
        int intendID = 1; // default if news
        boolean passInstance = false;

        if (extras != null) {
            title = extras.getString(Constants.KEY_MAIN_TITLE);
            message = extras.getString(Constants.KEY_MAIN_MESSAGE);
            intendID = extras.getInt(Constants.KEY_MAIN_INTENT);
            passInstance = true;
            if (intendID == Constants.NOTIF_CONNECT) {
                intendID = 0;
            } else if (intendID == Constants.NOTIF_GENERAL) {
                intendID++;
                if (title != null && title.length() > 0 && message != null && message.length() > 0) {
                    new MaterialDialog.Builder(this)
                            .title(title)
                            .content(message)
                            .negativeText("Fermer")
                            .show();
                }
            }
        }

        if (savedInstanceState == null || passInstance) {
            // on first time display view for first nav item
            displayView(intendID); // 0 is profile
        }

        // UNIVERSAL IMAGE LOADER SETUP
        DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
                .cacheOnDisc(true).cacheInMemory(true)
                .imageScaleType(ImageScaleType.IN_SAMPLE_POWER_OF_2)
                .displayer(new FadeInBitmapDisplayer(300)).build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
                getApplicationContext())
                .defaultDisplayImageOptions(defaultOptions)
                .memoryCache(new WeakMemoryCache())
                .discCacheSize(100 * 1024 * 1024).build();

        ImageLoader.getInstance().init(config);
        // END - UNIVERSAL IMAGE LOADER SETUP

        // If needed, show a welcome message
        prefs_Read = getSharedPreferences(Constants.PREFS_APP_WELCOME, 0);
        prefs_Write = prefs_Read.edit();

        // For V2.1.1, prevent profile suppression
        prefs_Write.putString(Constants.PREFS_APP_VERSION, BuildConfig.VERSION_NAME);
        prefs_Write.apply();

        if (prefs_Read.getBoolean(Constants.PREFS_APP_WELCOME_DATA, true)) {
            new MaterialDialog.Builder(this)
                    .title("Bienvenue !")
                    .content("Merci d'avoir téléchargé notre application !\n" +
                            "Prenez le temps de la découvrir, et n'oubliez pas de vous y connecter à l'aide de votre profil ESEO !\n\nL'équipe ESEOmega Ω")
                    .negativeText("Allons-y !")
                    .cancelable(false)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            super.onNegative(dialog);
                            mDrawerLayout.openDrawer(mDrawerList);
                            prefs_Write.putBoolean(Constants.PREFS_APP_WELCOME_DATA, false);
                            prefs_Write.putString(Constants.PREFS_APP_VERSION, BuildConfig.VERSION_NAME); // prevent next message
                            prefs_Write.apply();
                        }
                    })
                    .show();
        } else {

            // App already installed
            // Check if app is updated
            if ((!(prefs_Read.getString(Constants.PREFS_APP_VERSION, "").equals(BuildConfig.VERSION_NAME))) && profile.isCreated()) {
                new MaterialDialog.Builder(this)
                        .title("Re-bonjour !")
                        .content("Merci d'avoir pris le temps de faire cette mise à jour ! " +
                                "Elle apporte des correctifs de sécurité aux différentes fonctions de l'application, ainsi que la possibilité de recevoir les notifications liées à la caféteria et aux news.\n" +
                                "Pour accéder à l'ensemble de ces services, nous vous demandons de bien vouloir vous reconnecter avec votre profil ESEO.\n\nL'équipe ESEOmega Ω")
                        .negativeText("OK")
                        .cancelable(false)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                super.onNegative(dialog);
                                profile.removeProfileFromPrefs(MainActivity.this);
                                profile.readProfilePromPrefs(MainActivity.this);
                                OnUserProfileChange(profile);
                                prefs_Write.putBoolean(Constants.PREFS_APP_WELCOME_DATA, false); // prevents from previous message
                                prefs_Write.putString(Constants.PREFS_APP_VERSION, BuildConfig.VERSION_NAME);
                                prefs_Write.apply();
                            }
                        })
                        .show();
            }
        }

        // Test Token
        // If changed -> send it
        /*Intent intent = new Intent(this, RegistrationIntentService.class);
        this.startService(intent);
        Log.d("PUSH", "Current Token saved : " + profile.getPushToken());*/


        // Test tablet mode
        // TODO V2.1.1
        /*Utilities.isTabletDevice(this);

        boolean tabletSize = getResources().getBoolean(R.bool.isTablet);
        if (tabletSize) {
            // do something
            Toast.makeText(this, "Tablet mode enabled !", Toast.LENGTH_SHORT).show();
        } else {
            // do something else
            Toast.makeText(this, "Phone mode enabled !", Toast.LENGTH_SHORT).show();
        }*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    /**
     * Slide menu item click listener
     * */
    private class SlideMenuClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {
            // display view for selected nav drawer item
            displayView(position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.action_info:

                MaterialDialog md = new MaterialDialog.Builder(this)
                    .title("A propos")
                        .content(HELP_DIALOG_TEXT)
                    .positiveText("Contact")
                        .negativeText("Fermer")
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                                        "mailto", MAIL_DIALOG, null));
                                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[APP] Questions / Problèmes");
                                emailIntent.putExtra(Intent.EXTRA_TEXT, "Version de l'application : " + appVersion + "\n\n" + "...");
                                startActivity(Intent.createChooser(emailIntent, "Contacter les développeurs ..."));
                        }
                    })
                    .show();
                return true;

            case R.id.action_ingenews:

                Intent i = new Intent(MainActivity.this, IngeListActivity.class);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* *
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened, hide the action items
        // boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList); // not used
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Diplaying fragment view for selected nav drawer list item
     * */
    private void displayView(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;

        switch (position) {
            case 0: // Edit profile
                if (!profile.isCreated()) fragment = new ConnectProfileFragment();
                else fragment = new ViewProfileFragment();
                break;
            case 1: // News
                fragment = new NewsListFragment();
                break;
            case 2: // Events
                fragment = new EventsFragment();
                break;
            case 3: // Clubs & Vie Asso
                fragment = new CommunityFragment();
                break;
            case 4: // Commande Cafet
                fragment = new OrderListFragment();
                break;
            case 5: // Bons plans
                fragment = new TipsFragment();
                break;

            default:
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.frame_container, fragment, "frag" + position).commit();

            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            mDrawerList.setItemsCanFocus(true);
            setTitle(navMenuTitles[position]);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDrawerLayout.closeDrawer(mDrawerList);
                }
            }, 100);


        } else {
            // error in creating fragment
            //Log.e("ESEOmega", "Error in creating fragment");
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    public void OnUserProfileChange (UserProfile profile) {
        if (profile != null && navDrawerItems != null && navAdapter != null) {
            this.profile = profile;
            navDrawerItems.set(0, profile.getDrawerProfile());
            navAdapter.notifyDataSetChanged();
            mDrawerLayout.openDrawer(mDrawerList);
            navAdapter.setBitmap(profile.getPicturePath().length()==0?null:
                    ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(profile.getPicturePath()), MAX_PROFILE_SIZE));
        }
    }

    @Override
    public void OnItemAddToCart() {
        OrderTabsFragment mFragment = (OrderTabsFragment) getSupportFragmentManager().findFragmentByTag(Constants.TAG_FRAGMENT_ORDER_TABS);
        mFragment.refreshCart();
    }

    // A method to find height of the status bar
    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        // On back pressed : asks user if he really want to loose the cart's content (if viewing OrderTabsFragment)
        OrderTabsFragment fragment = (OrderTabsFragment) getSupportFragmentManager().findFragmentByTag(Constants.TAG_FRAGMENT_ORDER_TABS);
        if (fragment != null && fragment.isVisible() && DataManager.getInstance().getNbCartItems() > 0) {
            new MaterialDialog.Builder(MainActivity.this)
                    .title("Annuler la commande ?")
                    .content("Vous êtes sur le point de vider définitivement votre panier.\nEn êtes-vous sûr ?")
                    .positiveText("Oui")
                    .negativeText("Non")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            MainActivity.super.onBackPressed();
                        }
                    })
                    .show();
        } else { // Another fragment, we don't care
            MainActivity.super.onBackPressed();
        }
    }

    public static boolean checkAppSignature(Context context) {

        try {
            PackageInfo packageInfo = context.getPackageManager() .getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : packageInfo.signatures) {
                byte[] signatureBytes = signature.toByteArray();
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                final String currentSignature = Base64.encodeToString(md.digest(), Base64.NO_WRAP); // no '\n' character into string !
                //compare signatures
                //Log.d("SIGN", "Signature : app = " + currentSignature + ", certificate = " + SIGNATURE);
                if (currentSignature.equals(SIGNATURE)){
                    return true;
                }
            }

        } catch (Exception e) {
            //assumes an issue in checking signature., but we let the caller decide on what to do.
        }
        return false;
    }

    private static final String PLAY_STORE_APP_ID = "com.android.vending";

    public static boolean verifyInstaller(final Context context) {
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return installer != null&& installer.startsWith(PLAY_STORE_APP_ID);
    }
}
