package reach.project.onBoarding;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.google.common.base.Optional;

import reach.backend.entities.userApi.model.OldUserContainerNew;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class AccountCreation extends Fragment {

    public static Fragment newInstance(Optional<OldUserContainerNew> container) {

        final AccountCreation fragment = new AccountCreation();

        if (container.isPresent()) {

            final Bundle bundle = new Bundle(2);
            final OldUserContainerNew userContainer = container.get();
            bundle.putStringArray("oldData", new String[]{
                    TextUtils.isEmpty(userContainer.getName()) ? "" : userContainer.getName(),
                    TextUtils.isEmpty(userContainer.getCoverPic()) ? "" : userContainer.getCoverPic(),
                    TextUtils.isEmpty(userContainer.getImageId()) ? "" : userContainer.getImageId()});
            fragment.setArguments(bundle);
        }

        return fragment;
    }

    private static final ResizeOptions PROFILE_PHOTO_RESIZE = new ResizeOptions(150, 150);
    static final int IMAGE_PICKER_SELECT = 999;

    @Nullable
    private Uri profilePicUri = null;
    @Nullable
    private SplashInterface mListener = null;
    @Nullable
    private SimpleDraweeView profilePhotoSelector = null;

    private final View.OnClickListener imagePicker = v -> {

        final Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_PICKER_SELECT);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Sorry! Your device does not support this feature", Toast.LENGTH_SHORT).show();
        }
    };

    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_account_creation, container, false);
        final EditText userName = (EditText) rootView.findViewById(R.id.userName);

        profilePhotoSelector = (SimpleDraweeView) rootView.findViewById(R.id.displayPic);
        profilePhotoSelector.setOnClickListener(imagePicker);
        userName.requestFocus();

        final String oldImageId, oldCoverPicId;
        final FragmentActivity activity = getActivity();
        final Bundle arguments;
        final String[] oldData;
        if ((arguments = getArguments()) != null && (oldData = arguments.getStringArray("oldData")) != null && oldData.length == 3) {

            /**
             * oldData[0] = name;
             * oldData[1] = oldCoverPic;
             * oldData[2] = oldImageId;
             */
            if (!TextUtils.isEmpty(oldData[0])) {

                userName.setText(oldData[0]);
                userName.setSelection(oldData[0].length());
            }

            oldCoverPicId = oldData[1];
            oldImageId = oldData[2];
            if (!TextUtils.isEmpty(oldImageId) && !oldImageId.equals("hello_world")) {

                final Uri uriToDisplay = Uri.parse(StaticData.CLOUD_STORAGE_IMAGE_BASE_URL + oldImageId);
                profilePhotoSelector.setController(MiscUtils.getControllerResize(profilePhotoSelector.getController(),
                        uriToDisplay, PROFILE_PHOTO_RESIZE));
            }
        } else {

            oldCoverPicId = "";
            oldImageId = "";
        }

        rootView.findViewById(R.id.verify).setOnClickListener(view -> {

            final String name;
            if (TextUtils.isEmpty(name = userName.getText().toString().trim())) {
                Toast.makeText(activity, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }

            //OK
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(userName.getWindowToken(), 0);
            view.setOnClickListener(null);
            view.setEnabled(false);
            profilePhotoSelector.setOnClickListener(null);

            final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
            final String phoneNumber = SharedPrefUtils.getPhoneNumber(sharedPreferences);
            //TODO temp cover solution
            mListener.onOpenScan(name, profilePicUri, oldImageId, oldCoverPicId, phoneNumber); //not possible to be null

            //TODO track
            /*final Map<PostParams, String> simpleParams = MiscUtils.getMap(2);
            simpleParams.put(PostParams.USER_NUMBER, phoneNumber);
            simpleParams.put(PostParams.USER_NAME, name);
            try {
                UsageTracker.trackLogEvent(simpleParams, UsageTracker.NAME_ENTERED);
            } catch (JSONException ignored) {}*/
        });
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {

        final Activity activity;
        if ((activity = getActivity()) == null || activity.isFinishing()) {

            Log.i("Ayush", "ACTIVITY NOT FOUND !");
            return;
        }

        if (requestCode != IMAGE_PICKER_SELECT || resultCode != Activity.RESULT_OK || (profilePicUri = data.getData()) == null) {

            Toast.makeText(activity, "Failed to set Profile Photo, try again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (profilePhotoSelector != null) {

            final DraweeController draweeController = MiscUtils.getControllerResize(
                    profilePhotoSelector.getController(), profilePicUri, PROFILE_PHOTO_RESIZE);
            profilePhotoSelector.setController(draweeController);
        }
    }

    @Override
    public void onAttach(Context context) {

        super.onAttach(context);
        try {
            mListener = (SplashInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SplashInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}