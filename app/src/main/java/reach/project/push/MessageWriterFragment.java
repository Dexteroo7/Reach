package reach.project.push;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.lang.ref.WeakReference;

import reach.project.R;

public class MessageWriterFragment extends Fragment {

    public static final String USER_IDS = "userIds";
    public static final String PUSH_CONTAINER = "push_container";
    public static final String FIRST_SONG_NAME = "first_song_name";
    public static final String PUSH_SIZE = "push_size";

    @Nullable
    private static WeakReference<MessageWriterFragment> reference = null;

    public static MessageWriterFragment getInstance(long [] userIds, String pushContainer, String firstSongName, int songCount) {

        final Bundle args;
        MessageWriterFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            reference = new WeakReference<>(fragment = new MessageWriterFragment());
            fragment.setArguments(args = new Bundle());
        } else {
            Log.i("Ayush", "Reusing MessageWriterFragment object :)");
            args = fragment.getArguments();
        }

        args.putLongArray(USER_IDS, userIds);
        args.putString(PUSH_CONTAINER, pushContainer);
        args.putString(FIRST_SONG_NAME, firstSongName);
        args.putInt(PUSH_SIZE, songCount);

        return fragment;
    }
    
    @Nullable
    private ContactChooserInterface chooserInterface;

    private final View.OnClickListener goBackListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (chooserInterface != null)
                chooserInterface.switchToContactChooser();
        }
    };

    private final View.OnClickListener sendPushListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            //TODO send the push
        }
    };

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_message_writer, container, false);

        final long [] serverIds = getArguments().getLongArray(USER_IDS);
        final String pushContainer = getArguments().getString(PUSH_CONTAINER);
        final String firstSongName = getArguments().getString(FIRST_SONG_NAME);
        final int songCount = getArguments().getInt(PUSH_SIZE, 0);

        if (serverIds == null || serverIds.length == 0 || TextUtils.isEmpty(pushContainer))
            return rootView; //should not happen

        final EditText editText = (EditText) rootView.findViewById(R.id.message);
        final View sendPush = rootView.findViewById(R.id.sendPush);
        final View back = rootView.findViewById(R.id.back);

        sendPush.setOnClickListener(sendPushListener);
        back.setOnClickListener(goBackListener);

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
