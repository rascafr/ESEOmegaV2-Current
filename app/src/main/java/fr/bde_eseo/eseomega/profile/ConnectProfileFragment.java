/**
 * Copyright (C) 2016 - François LEPAROUX
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.bde_eseo.eseomega.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

import fr.bde_eseo.eseomega.Constants;
import fr.bde_eseo.eseomega.R;
import fr.bde_eseo.eseomega.gcmpush.RegistrationIntentService;
import fr.bde_eseo.eseomega.interfaces.OnUserProfileChange;
import fr.bde_eseo.eseomega.utils.ConnexionUtils;
import fr.bde_eseo.eseomega.utils.EncryptUtils;
import fr.bde_eseo.eseomega.utils.Utilities;

/**
 * Created by François on 13/04/2015.
 */
public class ConnectProfileFragment extends Fragment {

    public ConnectProfileFragment(){}

    private MaterialEditText etUserID, etUserPassword;
    private MaterialDialog mdProgress;
    private Button btValid;
    private String userID, userName, userPassword;
    private OnUserProfileChange mOnUserProfileChange;
    private String[] bullshitHint;
    private Random rand;
    private UserProfile profile;

    // Function to set the dialog visibity from activity
    public void setPushRegistration (boolean isSent) {
        if (isSent) {
            if (mdProgress != null) mdProgress.hide();
            //Toast.makeText(getActivity(), "Enregistrement notifs ok !", Toast.LENGTH_SHORT).show();

            MaterialDialog md = new MaterialDialog.Builder(getActivity())
                    .title("Bienvenue, " + profile.getFirstName() + " !")
                    .negativeText("Fermer")
                    .content("Votre profil a été synchronisé, et les notifications ont été activées, vous bénéficiez désormais de toutes les fonctionnalités.\nProfitez-en !")
                    .iconRes(R.drawable.ic_checked_user)
                    .show();

        } else {
            if (mdProgress != null) mdProgress.hide();

            MaterialDialog md = new MaterialDialog.Builder(getActivity())
                    .title("Oups ...")
                    .content("Impossible d'enregistrer votre appareil sur nos serveur\n" +
                            "Vous êtes connectés, mais vous ne pourrez pas recevoir les notifications news et cafétéria.\nTentez de vous reconnecter, ou contactez nous.")
                    .negativeText("Fermer")
                    .iconRes(R.drawable.ic_dizzy)
                    .show();
        }

        mOnUserProfileChange.OnUserProfileChange(profile);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.frame_container, new ViewProfileFragment(), "FRAG_VIEW_PROFILE")
                .commit();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mOnUserProfileChange = (OnUserProfileChange) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Find layout elements
        View rootView = inflater.inflate(R.layout.fragment_connect_profile, container, false);
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        //getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        btValid = (Button) rootView.findViewById(R.id.button_disconnect);
        etUserID = (MaterialEditText) rootView.findViewById(R.id.etUserID);
        etUserPassword = (MaterialEditText) rootView.findViewById(R.id.etUserPassword);
        //Utilities.hideSoftKeyboard(getActivity()); // UI's better with that

        rand = new Random();

        // Bullshit
        bullshitHint = getActivity().getResources().getStringArray(R.array.bullshitHintUser);
        etUserID.setHint(bullshitHint[rand.nextInt(bullshitHint.length)]);

        // Listener on validation button
        btValid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etUserID.getText().toString().length() < 1) {
                    etUserID.setError("Vous ne savez pas quoi mettre ?");
                } else if (etUserPassword.getText().toString().length() < 1) {
                    etUserPassword.setError("Un trou de mémoire ? Mmh ...");
                } else {
                    AsyncLogin asyncLogin = new AsyncLogin(getActivity());
                    asyncLogin.execute();
                }
                etUserID.setHint(bullshitHint[rand.nextInt(bullshitHint.length)]);

            }
        });




        return rootView;
    }

    /**
     * Async task to login client
     */
    class AsyncLogin extends AsyncTask<String, String, String> {

        private Context ctx;
        private String enPass;

        public AsyncLogin (Context ctx) {
            this.ctx = ctx;
        }

        @Override
        protected void onPreExecute() {
            userID = etUserID.getText().toString().toLowerCase(Locale.FRANCE).trim();
            etUserID.setText(userID);
            userPassword = etUserPassword.getText().toString();
            enPass = EncryptUtils.passBase64(userPassword);

            mdProgress = new MaterialDialog.Builder(ctx)
                    .title("Veuillez patienter ...")
                    .content("Connexion au serveur ESEO")
                    .negativeText("Annuler")
                    .cancelable(false)
                    .progress(true, 4, false)
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onNegative(MaterialDialog dialog) {
                            AsyncLogin.this.cancel(true);
                        }
                    })
                    .show();
            /*
                mdProgress = new MaterialDialog.Builder(ctx)
                        .title("Oups ...")
                        .content("Impossible d'accéder au réseau. Veuillez vérifier votre connexion, puis réessayer.")
                        .negativeText("Fermer")
                        .cancelable(false)
                        .iconRes(R.drawable.ic_facepalm)
                        .limitIconToDefaultSize()
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onNegative(MaterialDialog dialog) {
                                AsyncLogin.this.cancel(true);
                            }
                        })
                        .show();
            }*/
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            //Log.d("PROGRESS", "Called");
            //mdProgress.incrementProgress(1);
        }

        @Override
        protected String doInBackground(String... urls) {

            HashMap<String, String> pairs = new HashMap<>();
            pairs.put(getActivity().getResources().getString(R.string.username), userID);
            pairs.put(getActivity().getResources().getString(R.string.password), enPass);
            pairs.put(getActivity().getResources().getString(R.string.hash), EncryptUtils.sha256(userID + enPass + getActivity().getResources().getString(R.string.MEMORY_SYNC_USER)));

            if (Utilities.isOnline(getActivity())) {
                return ConnexionUtils.postServerData(Constants.URL_API_CLIENT_CONNECT, pairs, getActivity());
            } else {
                return null;
            }
        }

        // Once connexion is done
        @Override
        protected void onPostExecute(String result) {

            String res, info = "";
            int status = 0;
            boolean gantier = false;

            // Vérification des données reçues
            if (Utilities.isNetworkDataValid(result)) {

                try {
                    JSONObject obj = new JSONObject(result);
                    status = obj.getInt("status");

                    if (status == 1) {
                        JSONObject data = obj.getJSONObject("data");
                        userName = data.getString("username");
                        gantier = data.getBoolean("gantier");
                        info = data.getString("info");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Pas d'erreur ? Mdp ok ?
            if (status == 1) {

                // On crée le profil
                // Le nom / prénom de l'utilisateur est stocké dans le champ "username" du JSON retourné
                profile = new UserProfile(ctx, userName, userID, userPassword);
                if (gantier) profile.enableGuy();
                profile.guessEmailAddress();
                profile.registerProfileInPrefs(getActivity());

                // On check les Services Google, puis on démarre la classe Registration
                if (Utilities.checkPlayServices(getActivity())) {
                    mdProgress.setContent("Enregistrement de l'appareil");
                    // Start IntentService to register this application with GCM.
                    Intent intent = new Intent(getActivity(), RegistrationIntentService.class);
                    getActivity().startService(intent);
                }
            } else {

                // Erreur : -2 →
                mdProgress.dismiss();
                res = (status==-2) ?
                        "Mauvaise combinaison identifiant - mot de passe\nVeuillez vérifier vos informations, puis réessayer.":
                        "Erreur inconnue, impossible de valider votre connexion sur nos serveurs.\n";

                mdProgress = new MaterialDialog.Builder(ctx)
                        .title("Oups ...")
                        .content(res)
                        .negativeText("Fermer")
                        .iconRes(R.drawable.ic_dizzy)
                        .show();

            }
        }
    }
}
