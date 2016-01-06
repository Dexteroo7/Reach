package reach.project.coreViews.push.friends;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;

import reach.backend.notifications.notificationApi.model.PushContainerJSON;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.push.ContactChooserInterface;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class MessageWriterFragment extends Fragment {

    public static final String USER_IDS = "USER_IDS";
    public static final String PUSH_CONTAINER = "PUSH_CONTAINER";
    public static final String FIRST_CONTENT_NAME = "FIRST_CONTENT_NAME";
    public static final String PUSH_SIZE = "PUSH_SIZE";

    @Nullable
    private static WeakReference<MessageWriterFragment> reference = null;
    private static long myUserId = 0;

    public static MessageWriterFragment getInstance(long[] userIds, String pushContainer, String firstSongName, int songCount) {

        final Bundle args;
        MessageWriterFragment fragment;
        if (reference == null || (fragment = reference.get()) == null || MiscUtils.isFragmentDead(fragment)) {
            reference = new WeakReference<>(fragment = new MessageWriterFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing MessageWriterFragment object :)");
            args = fragment.getArguments();
        }

        args.putLongArray(USER_IDS, userIds);
        args.putString(PUSH_CONTAINER, pushContainer);
        args.putString(FIRST_CONTENT_NAME, firstSongName);
        args.putInt(PUSH_SIZE, songCount);

        return fragment;
    }

    @Nullable
    private ContactChooserInterface chooserInterface;
    @Nullable
    private EditText editText;
    @Nullable
    private View sendPush;

    private final View.OnClickListener goBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (chooserInterface != null)
                chooserInterface.switchToContactChooser();
        }
    };

    private static final class SendPush extends AsyncTask<PushContainerJSON, Void, Boolean> {

        @NonNull
        @Override
        protected Boolean doInBackground(PushContainerJSON... params) {

            try {
                StaticData.NOTIFICATION_API.addPushMultiple(params[0]).execute();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(@NonNull Boolean aBoolean) {

            super.onPostExecute(aBoolean);

            MiscUtils.useContextAndFragment(reference, (context, fragment) -> {

                if (aBoolean) {
                    Toast.makeText(context, "Pushed successfully", Toast.LENGTH_SHORT).show();
                    fragment.getActivity().finish();
                } else {

                    //TODO track
                    Toast.makeText(context, "Push failed", Toast.LENGTH_SHORT).show();
                }

                if (fragment.sendPush != null) {

                    fragment.sendPush.setAlpha(1.0f);
                    fragment.sendPush.setClickable(true);
                }
            });
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final SharedPreferences sharedPreferences = getContext().getSharedPreferences("Reach", Context.MODE_PRIVATE);
        myUserId = SharedPrefUtils.getServerId(sharedPreferences);

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_message_writer, container, false);
        editText = (EditText) rootView.findViewById(R.id.message);

        final Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.writerToolbar);
        toolbar.setTitle("Send push");
        toolbar.inflateMenu(R.menu.menu_push);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.push_button:
                    final long[] serverIds = getArguments().getLongArray(USER_IDS);
                    final String pushContainer = getArguments().getString(PUSH_CONTAINER);
                    final String firstContentName = getArguments().getString(FIRST_CONTENT_NAME);
                    final int contentCount = getArguments().getInt(PUSH_SIZE, 0);

                    if (serverIds == null || serverIds.length == 0 || TextUtils.isEmpty(pushContainer)) {
                        getActivity().finish(); //should not happen
                        return true;
                    }
                    final PushContainerJSON pushJSON = new PushContainerJSON();
                    pushJSON.setContainer(pushContainer);
                    pushJSON.setCustomMessage(editText != null ? editText.getText().toString() : "");
                    pushJSON.setFirstContentName(firstContentName);
                    pushJSON.setReceiverId(MiscUtils.convertToList(serverIds));
                    pushJSON.setSize(contentCount);
                    pushJSON.setSenderId(myUserId);

                    if (sendPush != null) {

                        sendPush.setAlpha(0.1f);
                        sendPush.setClickable(false);
                    }
                    new SendPush().execute(pushJSON);
                    return true;
            }
            return false;
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ContactChooserInterface) {
            chooserInterface = (ContactChooserInterface) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        chooserInterface = null;
    }
}
