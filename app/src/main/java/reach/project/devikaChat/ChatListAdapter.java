package reach.project.devikaChat;

import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.firebase.client.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import reach.project.R;

/**
 * @author greg
 * @since 6/21/13
 *
 * This class is an example of how to use FirebaseListAdapter. It uses the <code>Chat</code> class to encapsulate the
 * data for each individual chat message
 */
public class ChatListAdapter extends FirebaseListAdapter<Chat> {

//    // The mUsername for this client. We use this to indicate which messages originated from this user
//    private final long userId;
//    private final Context context;

    public ChatListAdapter(Query ref, Activity activity, int layout, long userId) {

        super(ref, Chat.class, layout, activity);
//        context = activity;
//        this.userId = userId;
    }

    /**
     * Bind an instance of the <code>Chat</code> class to our view. This method is called by <code>FirebaseListAdapter</code>
     * when there is a data change, and we are given an instance of a View that corresponds to the layout that we passed
     * to the constructor, as well as a single <code>Chat</code> instance that represents the current data to bind.
     *
     * @param view A view instance corresponding to the layout we passed to the constructor.
     * @param chat An instance representing the current state of a chat message
     */
    @Override
    protected void populateView(View view, Chat chat) {

        // Map a Chat object to an entry in our listView

        final TextView chatView = ((TextView) view.findViewById(R.id.message));
        final TextView timeView = ((TextView) view.findViewById(R.id.time));

        //Log.i("Chat", "Author:" + author);

        if (chat.getAdmin() == Chat.ADMIN) {

            chatView.setBackgroundResource(R.drawable.chat_bubble_gray);
            chatView.setTextColor(Color.DKGRAY);
            ((LinearLayout) view).setGravity(Gravity.LEFT);
        }
        else {

            chatView.setBackgroundResource(R.drawable.chat_bubble);
            chatView.setTextColor(Color.WHITE);
            ((LinearLayout) view).setGravity(Gravity.RIGHT);
        }

        switch (chat.getStatus()) {

            case Chat.PENDING : //no tick
            case Chat.SENT_TO_SERVER : //add single tick
            case Chat.UN_READ : //double tick
            case Chat.READ : //blue tick
        }

        chatView.setText(chat.getMessage());
        timeView.setText(new SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault()).format(
                new Date(chat.getTimestamp())));
    }
}
