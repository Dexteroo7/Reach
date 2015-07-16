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

import reach.backend.entities.userApi.model.ReachFriend;
import reach.project.R;
import reach.project.adapter.ReachFriendRequestAdapter;
import reach.project.utils.SuperInterface;

public class FriendRequestFragment extends Fragment {

    private static WeakReference<FriendRequestFragment> reference = null;
    private SuperInterface mListener;

    public static FriendRequestFragment newInstance() {

        FriendRequestFragment fragment;
        if(reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new FriendRequestFragment());
        return fragment;
    }
    public FriendRequestFragment() {
        // Required empty public constructor
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            ReachFriend reachFriend = ((ReachFriendRequestAdapter) parent.getAdapter()).getItem(position);
            mListener.onOpenLibrary(reachFriend.getId());
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_list, container, false);
        final ListView listView = (ListView) rootView.findViewById(R.id.listView);

        ReachFriendRequestAdapter reachFriendRequestAdapter = new ReachFriendRequestAdapter(getActivity(),R.layout.notification_item,null);
        //listView.setAdapter(reachFriendRequestAdapter);
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
                    + " must implement SuperInterface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
