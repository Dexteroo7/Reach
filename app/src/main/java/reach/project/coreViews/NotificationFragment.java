package reach.project.coreViews;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.lang.ref.WeakReference;

import reach.project.R;
import reach.project.adapter.ReachNotificationAdapter;
import reach.project.database.notifications.BecameFriends;
import reach.project.database.notifications.Types;
import reach.project.utils.SuperInterface;

public class NotificationFragment extends Fragment {

    private static WeakReference<NotificationFragment> reference = null;
    private SuperInterface mListener;
    public static NotificationFragment newInstance() {

        NotificationFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new NotificationFragment());
        return fragment;
    }
    public NotificationFragment() {
        // Required empty public constructor
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Types type = Types.DEFAULT;
            switch (type) {
                case DEFAULT:
                    break;
                case LIKE:
                    mListener.anchorFooter(true);
                    break;
                case PUSH:
                    mListener.anchorFooter(true);
                    break;
                case BECAME_FRIENDS:
                    BecameFriends becameFriends = new BecameFriends();
                    mListener.onOpenLibrary(becameFriends.getHostId());
                    break;
                case PUSH_ACCEPTED:
                    mListener.anchorFooter(true);
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView listView = (ListView) rootView.findViewById(R.id.listView);
        final ReachNotificationAdapter reachNotificationAdapter = new ReachNotificationAdapter(getActivity(),R.layout.notification_item,null,0);
        listView.setAdapter(reachNotificationAdapter);
        listView.setOnItemClickListener(itemClickListener);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

}
