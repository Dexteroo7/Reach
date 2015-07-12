package reach.project.adapter;

/**
* Created by dexter on 1/8/14.
*/

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.viewHelpers.CircleTransform;
import reach.project.viewHelpers.Contact;


public class ReachAllContactsAdapter extends ArrayAdapter<Contact> {

    private final  Context context;
    private final int layoutResourceId;
    private final Picasso picasso;
    private final List<Contact> originalData;
    private final List<Contact> filteredData;
    private OnEmptyContactsListener mListener = null;

    public interface OnEmptyContactsListener {
        void onEmptyContacts();
        void onNotEmptyContacts();
    }

    public void setOnEmptyContactsListener(OnEmptyContactsListener listener){
        this.mListener = listener;
    }

    public void cleanUp() {

        if(this.originalData != null)
            this.originalData.clear();
        if(this.filteredData != null)
            this.filteredData.clear();
    }

    public ReachAllContactsAdapter(Context context, int ResourceId,
                                   List<Contact> friends) {

        super(context, ResourceId, friends);
        this.context = context;
        this.picasso = Picasso.with(context);
        this.layoutResourceId = ResourceId;
        this.filteredData = friends;
        /**
         * Needed to avoid messing up original data
         */
        this.originalData = new ArrayList<>(friends);
    }

    public int getCount() {
        return filteredData.size();
    }

    public Contact getItem(int position) {
        return filteredData.get(position);
    }

    private Uri openPhoto(long contactId) {
        return Uri.withAppendedPath(ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
                                    ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
    }

    private final class ViewHolder{
        private final TextView userNameList, userInitials;
        private final ImageView profilePhotoList,listToggle;

        private ViewHolder(TextView userNameList, TextView userInitials, ImageView profilePhotoList, ImageView listToggle) {
            this.userNameList = userNameList;
            this.userInitials = userInitials;
            this.profilePhotoList = profilePhotoList;
            this.listToggle = listToggle;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Contact contact = getItem(position);
        final ViewHolder viewHolder;

        if(convertView==null) {

            convertView = ((Activity) context).getLayoutInflater().inflate(layoutResourceId, parent, false);

            viewHolder = new ViewHolder(
                    (TextView) convertView.findViewById(R.id.userNameList),
                    (TextView) convertView.findViewById(R.id.userInitials),
                    (ImageView) convertView.findViewById(R.id.profilePhotoList),
                    (ImageView) convertView.findViewById(R.id.listToggle));
            convertView.setTag(viewHolder);
        }
        else
            viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.userNameList.setText(contact.getUserName());
        viewHolder. userInitials.setText(MiscUtils.generateInitials(contact.getUserName()));
        picasso.load(openPhoto(contact.getUserID())).transform(new CircleTransform()).into(viewHolder.profilePhotoList);

        if(contact.isInviteSent())
            picasso.load(R.drawable.add_tick).into(viewHolder.listToggle);
        else
            picasso.load(R.drawable.add_myreach).into(viewHolder.listToggle);
        return convertView;
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {

                final FilterResults results = new FilterResults();
                if(charSequence == null || charSequence.length() == 0) {
                    results.values = originalData;
                    results.count = originalData.size();
                    Log.i("Ayush", "Filtering with original data");
                } else {
                    final List<Contact> filterResultsData = new ArrayList<>();
                    for(Contact data : originalData){
                        if(data.getUserName().toLowerCase().contains(charSequence.toString().toLowerCase()))
                            filterResultsData.add(data);
                    }
                    results.values = filterResultsData;
                    results.count = filterResultsData.size();
                    Log.i("Ayush", "Filtering with " + charSequence);
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

                if(filterResults.values instanceof List) {

                    if (((List) filterResults.values).size()==0)
                        mListener.onEmptyContacts();
                    else
                        mListener.onNotEmptyContacts();

                    filteredData.clear();
                    for(Object contact : (List) filterResults.values) {
                        if(contact instanceof Contact)
                            filteredData.add((Contact) contact);
                    }
                    notifyDataSetChanged();
                }
            }

        };
    }
}