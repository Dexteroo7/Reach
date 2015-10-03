package reach.project.devikaChat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChatActivityFragment extends Fragment {

    private static final String FIREBASE_URL = "https://flickering-fire-7874.firebaseio.com/";
    private static final String TAG = "Reach Chat";

    private static String mUUID = "";
    private static long serverId = 0;
    private static boolean connected = false;

    private static WeakReference<ChatActivityFragment> reference = null;

    public static ChatActivityFragment newInstance() {

        ChatActivityFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            Log.i("Ayush", "Creating new instance of my ChatActivityFragment");
            reference = new WeakReference<>(fragment = new ChatActivityFragment());
        } else
            Log.i("Ayush", "Reusing ChatActivityFragment fragment object :)");

        return fragment;
    }

    private ListView chatList = null;
    private Firebase mFirebaseRef = null;
    private ValueEventListener mConnectedListener = null;
    private ChatListAdapter mChatListAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        final String chatToken = SharedPrefUtils.getChatToken(sharedPreferences);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);

        if (TextUtils.isEmpty(chatToken)) {
            Log.i("Ayush", "Chat token not found !");
            activity.finish();
            return;
        }
        if (serverId == 0) {
            Log.i("Ayush", "ServerId not found !");
            activity.finish();
            return;
        }

        Log.i("Ayush", "Found chat token " + chatToken + " " + serverId);

        //initialize firebase
        Firebase.setAndroidContext(activity);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        // Setup our Firebase mFirebaseRef
        mFirebaseRef = new Firebase(FIREBASE_URL);
        mFirebaseRef.keepSynced(true);
        mFirebaseRef.authWithCustomToken(chatToken, new Firebase.AuthResultHandler() {

            @Override
            public void onAuthenticationError(FirebaseError error) {
                Log.e(TAG, "Login Failed! " + error.getMessage());
            }

            @Override
            public void onAuthenticated(AuthData authData) {

                Log.i(TAG, "Login Succeeded!" + authData);
                mUUID = authData.getUid();
                // Finally, a little indication of connection status
                mConnectedListener = mFirebaseRef.getRoot().child(".info/connected").addValueEventListener(connectionStatus);

                /**
                 * Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
                 * Tell our list adapter that we only want 50 messages at a time
                 **/
                Log.i("Ayush", "Setting up chat list adapter !");
                mChatListAdapter = new ChatListAdapter(
                        mFirebaseRef.child("chat").child(mUUID).limitToLast(50),
                        getActivity(),
                        R.layout.chat_message_me,
                        serverId);
                chatList.setAdapter(mChatListAdapter);

                mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        chatList.setSelection(mChatListAdapter.getCount() - 1);
                    }
                });

            }
        });
    }

    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_chat, container, false);
        final EditText messageInput = (EditText) rootView.findViewById(R.id.messageInput);
        final ImageButton button = (ImageButton) rootView.findViewById(R.id.sendButton);
        chatList = (ListView) rootView.findViewById(R.id.chatList);

        // Setup our input methods. Enter key on the keyboard or pushing the send button
        messageInput.setOnEditorActionListener(editorListener);
        button.setOnClickListener(sendListener);

        messageInput.setTag(mFirebaseRef);
        button.setTag(messageInput);

        return rootView;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (mFirebaseRef != null && mConnectedListener != null)
            mFirebaseRef.getRoot().child(".info/connected").removeEventListener(mConnectedListener);

        if (mChatListAdapter != null)
            mChatListAdapter.cleanup();
    }

    private static void sendMessage(String message, Firebase mFirebaseRef) {

        if (!TextUtils.isEmpty(message)) {

            // Create our 'model', a Chat object
            final Chat chat = new Chat();
            chat.setIsDevika(false);
            chat.setMessage(message);
            chat.setRead(false);
            chat.setTimestamp(System.currentTimeMillis());
            chat.setUserId(serverId);

            // Create a new, auto-generated child of that chat location, and save our chat data there
            mFirebaseRef.child("chat").child(mUUID).push().setValue(chat);
        }
    }

    private static final View.OnClickListener sendListener = v -> {

        if (!connected)
            return;

        final EditText editText = ((EditText) v.getTag());
        final Object object = editText.getTag();
        if (object != null && object instanceof Firebase) {
            sendMessage(editText.getText().toString(), (Firebase) object);
            editText.setText("");
        } else
            Log.i("Ayush", "Fail of 1st order");
    };

    private static final TextView.OnEditorActionListener editorListener = (v, actionId, event) -> {

        if (!connected)
            return false;

        if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {

            final EditText editText = (EditText) v;
            final Object object = editText.getTag();
            if (object != null && object instanceof Firebase) {
                sendMessage(editText.getText().toString(), (Firebase) object);
                editText.setText("");
            } else
                Log.i("Ayush", "Fail of 1st order");
        }
        return true;
    };

    private static final ValueEventListener connectionStatus = new ValueEventListener() {

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            connected = (Boolean) dataSnapshot.getValue();
            MiscUtils.useContextFromFragment(reference, context -> {
                if (connected)
                    Toast.makeText(context, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

            MiscUtils.useContextFromFragment(reference, context -> {
                Toast.makeText(context, "Firebase error", Toast.LENGTH_SHORT).show();
            });
        }
    };
}
