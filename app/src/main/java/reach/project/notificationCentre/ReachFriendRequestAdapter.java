package reach.project.notificationCentre;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Optional;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.List;

import reach.backend.entities.userApi.model.ReceivedRequest;
import reach.backend.notifications.notificationApi.model.Friend;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.coreViews.MyReachFragment;
import reach.project.friends.ReachFriendsHelper;
import reach.project.friends.ReachFriendsProvider;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CircleTransform;

/**
 * Created by ashish on 14/07/15.
 */
public class ReachFriendRequestAdapter extends ArrayAdapter<ReceivedRequest> {

    public static final LongSparseArray<Boolean> accepted = new LongSparseArray<>();
    private static final LongSparseArray<Boolean> opened = new LongSparseArray<>();

    private static final int a = MiscUtils.dpToPx(70);
    private static final int b = MiscUtils.dpToPx(110);
    private static long serverId;
    private final int rId;

    public ReachFriendRequestAdapter(Context context, int resourceId, List<ReceivedRequest> receivedRequestList, long id) {
        super(context, resourceId, receivedRequestList);

        this.rId = resourceId;
        serverId = id;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        MyReachFragment.checkNewNotifications();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(rId, parent, false);

        final View linearLayout = convertView.findViewById(R.id.linearLayout);

        final ImageView profilePhoto = (ImageView) convertView.findViewById(R.id.profilePhotoList);
        final TextView userName = (TextView) convertView.findViewById(R.id.userNameList);
        final TextView notificationType = (TextView) convertView.findViewById(R.id.notifType);
        final TextView userInitials = (TextView) convertView.findViewById(R.id.userInitials);

        final View libraryBtn = convertView.findViewById(R.id.libraryButton);
        final View actionBlock = convertView.findViewById(R.id.actionBlock);
        final View accept = convertView.findViewById(R.id.acceptBlock);
        final View reject = convertView.findViewById(R.id.rejectBlock);
        final ReceivedRequest receivedRequest = getItem(position);

        Picasso.with(convertView.getContext()).load(StaticData.cloudStorageImageBaseUrl + receivedRequest.getImageId()).fit().transform(new CircleTransform()).into(profilePhoto);
        userName.setText(receivedRequest.getUserName());
        userInitials.setText(MiscUtils.generateInitials(receivedRequest.getUserName()));

        if (accepted.get(receivedRequest.getId(), false)) {

            linearLayout.getLayoutParams().height = a;
            actionBlock.setVisibility(View.GONE);
            libraryBtn.setVisibility(View.VISIBLE);
            notificationType.setText("added to your friends");
        } else {

            notificationType.setText("has sent you an access request");
            libraryBtn.setVisibility(View.GONE);
            actionBlock.setVisibility(View.VISIBLE);

            if (!opened.get(receivedRequest.getId(), false))
                linearLayout.getLayoutParams().height = a;
            else
                linearLayout.getLayoutParams().height = b;
            linearLayout.setTag(receivedRequest.getId());
            linearLayout.setOnClickListener(expander);

            accept.setTag(new Object[]{receivedRequest.getId(), (Result) friend -> {

                if (friend != null) {
                    //success
                    getContext().getContentResolver().insert(
                            ReachFriendsProvider.CONTENT_URI,
                            ReachFriendsHelper.contentValuesCreator(friend));

                    accepted.put(receivedRequest.getId(), true);
                    expand(linearLayout, b, a);
                    linearLayout.setClickable(false);
                    actionBlock.setVisibility(View.GONE);
                    libraryBtn.setVisibility(View.VISIBLE);
                    notificationType.setText("added to your friends");
                }

            }});

            accept.setOnClickListener(acceptClick);
            reject.setOnClickListener(v -> {

                MiscUtils.autoRetryAsync(() -> {
                    StaticData.notificationApi.addBecameFriends(false, serverId, receivedRequest.getId()).execute();
                    return null;
                }, Optional.absent());

                //delete entry
                remove(receivedRequest);
                accepted.delete(receivedRequest.getId());
                opened.delete(receivedRequest.getId());
                notifyDataSetChanged();
            });
        }
        return convertView;
    }

    private static void expand(final View view, int a, int b) {

        final ValueAnimator va = ValueAnimator.ofInt(a, b);
        va.setDuration(300);
        va.addUpdateListener(animation -> {
            view.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            view.requestLayout();
        });
        //va.setInterpolator(new DecelerateInterpolator());
        va.start();
    }

    private static final View.OnClickListener expander = view -> {

        final long itemId = (long) view.getTag();
        final boolean toggle = !opened.get(itemId, false);

        if (toggle)
            expand(view, a, b);
        else
            expand(view, b, a);
        opened.put(itemId, toggle);
    };

    private static final View.OnClickListener acceptClick = accept -> {

        final Object temp = accept.getTag();
        if (temp == null) {
            Log.i("Ayush", "Fail of 1st order");
            return;
        }
        if (!(temp instanceof Object[])) {
            Log.i("Ayush", "Fail of 2nd order");
            return;
        }

        final Object[] data = (Object[]) temp;
        if (data.length != 2) {
            Log.i("Ayush", "Fail of 3rd order");
            return;
        }

        //disable and send request
        accept.setEnabled(false);
        //hostId, reference, view to enable (SERIAL_EXECUTOR to prevent problems of concurrency)
        new HandleAccept().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, data[0], new WeakReference<>(data[1]), accept);
    };

    private static final class HandleAccept extends AsyncTask<Object, Void, Object[]> {

        /**
         * @param params supplied parameters
         *               params[0] = hostId
         *               params[1] = un-cast weak reference of Result callback
         *               params[2] = view to enable back
         * @return [0] reference, [1] friend, [2] view
         */

        @Override
        protected Object[] doInBackground(Object... params) {

            return new Object[]{
                    params[1], //un-cast-reference
                    MiscUtils.autoRetry(() -> StaticData.notificationApi.addBecameFriends(true, serverId, (long) params[0]).execute(), Optional.absent()).orNull(),
                    params[2]}; //view to enable
        }

        @Override
        protected void onPostExecute(Object[] result) {

            super.onPostExecute(result);
            /**
             * result[0] = reference
             * result[1] = friend;
             * result[2] = view to enable back;
             */
            //enable view
            ((View) result[2]).setEnabled(true);

            final Object unCastReference = result[0];
            if (unCastReference == null) {
                Log.i("Ayush", "REFERENCE NULL");
                return;
            }

            if (!(unCastReference instanceof WeakReference)) {
                Log.i("Ayush", "Fail of 4th order");
                return;
            }

            final Object unCastResult = ((WeakReference) unCastReference).get();
            if (unCastResult == null) {
                Log.i("Ayush", "REFERENCE LOST");
                return;
            }

            //publish result
            ((Result) unCastResult).result((Friend) result[1]);
        }
    }

    private interface Result {
        void result(Friend friend);
    }
}