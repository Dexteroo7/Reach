package reach.project.devikaChat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import reach.project.R;
import reach.project.core.GcmIntentService;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;

public class ChatActivityFragment extends Fragment {

    public static final AtomicBoolean connected = new AtomicBoolean(false);

    private static String chatUUID = "";
    private static long serverId = 0;

    private static WeakReference<ChatActivityFragment> reference = null;

    public static ChatActivityFragment newInstance() {

        ChatActivityFragment fragment;
        if (reference == null || (fragment = reference.get()) == null) {
            Log.i("Chat", "Creating new instance of my ChatActivityFragment");
            reference = new WeakReference<>(fragment = new ChatActivityFragment());
        } else
            Log.i("Chat", "Reusing ChatActivityFragment fragment object :)");

        return fragment;
    }

    public ChatActivityFragment() {
        reference = new WeakReference<>(this);
    }

    private ListView chatList = null;
    private ValueEventListener mConnectedListener = null;
    private ChatListAdapter mChatListAdapter = null;
    private SharedPreferences sharedPreferences = null;
    private Firebase firebaseReference = null;

    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        final Activity activity = getActivity();
        final View rootView = inflater.inflate(R.layout.fragment_chat, container, false);

        sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_PRIVATE);
        chatUUID = SharedPrefUtils.getChatUUID(sharedPreferences);
        serverId = SharedPrefUtils.getServerId(sharedPreferences);

        if (serverId == 0) {
            //TODO track
            Log.i("Chat", "ServerId not found !");
            activity.finish();
            return rootView;
        }

        // Setup our Firebase mFirebaseRef
        firebaseReference = new Firebase("https://flickering-fire-7874.firebaseio.com/");
        firebaseReference.keepSynced(true);

        final EditText messageInput = (EditText) rootView.findViewById(R.id.messageInput);
        final ImageView button = (ImageView) rootView.findViewById(R.id.sendButton);
        chatList = (ListView) rootView.findViewById(R.id.chatList);

        // Setup our input methods. Enter key on the keyboard or pushing the send button
        messageInput.setOnEditorActionListener(editorListener);
        button.setOnClickListener(sendListener);
        button.setTag(messageInput);

        //if not empty exit
        if (!TextUtils.isEmpty(SharedPrefUtils.getChatToken(sharedPreferences)) && !TextUtils.isEmpty(chatUUID)) {

            //TODO track
            //setup callback and wait/retry
            activity.finish();
        } else //everything cool, continue
            setUpUI();

        return rootView;
    }

    @Override
    public void onResume() {

        super.onResume();
        // Finally, a little indication of connection status
        mConnectedListener = firebaseReference.getRoot().child(".info/connected").addValueEventListener(connectionStatus);
        //remove chat notification
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
        notificationManager.cancel(GcmIntentService.NOTIFICATION_ID_CHAT);
    }

    @Override
    public void onPause() {

        super.onPause();
        connected.set(false);
        if (firebaseReference != null && mConnectedListener != null)
            firebaseReference.getRoot().child(".info/connected").removeEventListener(mConnectedListener);
    }

    @Override
    public void onDestroyView() {

        super.onDestroy();

        if (mChatListAdapter != null)
            mChatListAdapter.cleanup();

        connected.set(false);
        sharedPreferences = null;
        chatList = null;
        firebaseReference = null;
        mConnectedListener = null;
        mChatListAdapter = null;

        Log.i("Chat", "CHAT ON DESTROY !!");
    }

    private static void sendMessage(String message) {

        if (!TextUtils.isEmpty(message)) {

            // Create a new, auto-generated child of that chat location, and save our chat data there
            final Chat chat = new Chat();
            chat.setMessage(message);
            chat.setTimestamp(System.currentTimeMillis());

            //update user object to allow sort by time
            final Map<String, Object> userData = new HashMap<>();
            userData.put("uid", serverId);
            userData.put("newMessage", true);
            userData.put("lastActivated", System.currentTimeMillis());

            MiscUtils.useFragment(reference, fragment -> {

                final Firebase pushRef = fragment.firebaseReference.child("chat").child(chatUUID).push();
                pushRef.setValue(chat);
                final String uniqueKey = pushRef.getKey();
                chat.setChatId(uniqueKey);
                final Map<String, Object> temp = new HashMap<>(1);
                temp.put("chatId", uniqueKey);

                fragment.firebaseReference.child("chat").child(chatUUID).child(uniqueKey).updateChildren(temp);
                fragment.firebaseReference.child("user").child(chatUUID).updateChildren(userData);
            });
        }
    }

    private static final View.OnClickListener sendListener = v -> {

//        if (!connected)
//            return;

        if (TextUtils.isEmpty(chatUUID))
            return;

        final EditText editText = ((EditText) v.getTag());
        sendMessage(editText.getText().toString());
        editText.setText("");
    };

    private static final TextView.OnEditorActionListener editorListener = (v, actionId, event) -> {

//        if (!connected)
//            return false;

        if (TextUtils.isEmpty(chatUUID))
            return false;

        if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {

            final EditText editText = (EditText) v;
            sendMessage(editText.getText().toString());
            editText.setText("");
        }
        return true;
    };

    private static final ValueEventListener connectionStatus = new ValueEventListener() {

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {

            if (dataSnapshot.getValue() == null)
                return;

            connected.set((Boolean) dataSnapshot.getValue());

            /*MiscUtils.useContextFromFragment(reference, context -> {

                if (connected.get())
                    Toast.makeText(context, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context, "Disconnected from Firebase", Toast.LENGTH_SHORT).show();
            });*/
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {

            MiscUtils.useContextFromFragment(reference, context -> {
                Toast.makeText(context, "Firebase error", Toast.LENGTH_SHORT).show();
            });
        }
    };

    private void setUpUI() {

        /**
         * Setup our view and list adapter. Ensure it scrolls to the bottom as data changes
         * Tell our list adapter that we only want 50 messages at a time
         **/
        Log.i("Chat", "Setting up chat list adapter !");
        mChatListAdapter = new ChatListAdapter(
                firebaseReference.child("chat").child(chatUUID),
                getActivity(),
                R.layout.chat_message_me,
                firebaseReference,
                chatUUID);
        chatList.setAdapter(mChatListAdapter);

        mChatListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                chatList.setSelection(mChatListAdapter.getCount() - 1);
            }
        });
    }
}