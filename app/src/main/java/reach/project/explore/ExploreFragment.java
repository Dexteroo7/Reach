package reach.project.explore;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.utils.auxiliaryClasses.SuperInterface;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExploreFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExploreFragment extends Fragment implements ExploreAdapter.Explore,
        ExploreBuffer.Exploration<ExploreContainer> {

    private final ExploreBuffer<ExploreContainer> buffer = ExploreBuffer.getInstance(this);

    private ExploreAdapter exploreAdapter;

    private SuperInterface mListener;

    private static WeakReference<ExploreFragment> reference;

    public static ExploreFragment newInstance() {

        ExploreFragment fragment;
        if (reference == null || (fragment = reference.get()) == null)
            reference = new WeakReference<>(fragment = new ExploreFragment());

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (SuperInterface) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_explore, container, false);
        mListener.toggleSliding(false);

        final ViewPager explorePager = (ViewPager) rootView.findViewById(R.id.explorer);
        exploreAdapter = new ExploreAdapter(getActivity(), this);

        explorePager.setAdapter(exploreAdapter);
        explorePager.setOffscreenPageLimit(2);
        explorePager.setPageMargin(-1 * (MiscUtils.dpToPx(40)));
        explorePager.setPageTransformer(true, (view, position) -> {
            if (position <= 1) {
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                float vertMargin = view.getHeight() * (1 - scaleFactor) / 2;
                float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
                if (position < 0)
                    view.setTranslationX(horzMargin - vertMargin / 2);
                else
                    view.setTranslationX(-horzMargin + vertMargin / 2);

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        buffer.close();
        exploreAdapter = null;
    }

    @Override
    public synchronized Callable<Collection<ExploreContainer>> fetchNextBatch() {
//        Toast.makeText(getContext(), "Server Fetching next batch of 10", Toast.LENGTH_SHORT).show();
        return fetchNextBatch;
    }

    private static int counter = 0;

    private static final Callable<Collection<ExploreContainer>> fetchNextBatch = () -> {

        /*try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        final List<ExploreContainer> containers = new ArrayList<>(10);
        for (int i=0; i<20; i++)
            containers.add(new ExploreContainer("Page " + i, "Subtitle", "imageId", "userImageId",
                    "handle", 3.0f, ExploreTypes.MUSIC, new Random().nextLong()));
        counter += containers.size();
        if (counter > 50) {
            containers.clear();
            containers.add(new ExploreContainer(ExploreTypes.DONE_FOR_TODAY, new Random().nextLong()));
        }*/

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", "6571391281266688");
        JSONArray jsonArray = new JSONArray();
        for (int i=0; i<10; i++) {
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("friendId", "4503618472378368");
            jsonArray.put(jsonObject1);
        }
        /*JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("friendId", "4503618472378368");
        jsonArray.put(jsonObject1);
        JSONObject jsonObject2 = new JSONObject();
        jsonObject2.put("friendId", "4505211267710976");
        jsonArray.put(jsonObject2);
        JSONObject jsonObject3 = new JSONObject();
        jsonObject3.put("friendId", "4503788425576448");
        jsonArray.put(jsonObject3);
        JSONObject jsonObject4 = new JSONObject();
        jsonObject4.put("friendId", "4504653291061248");
        jsonArray.put(jsonObject4);
        JSONObject jsonObject5 = new JSONObject();
        jsonObject5.put("friendId", "4504219130265600");
        jsonArray.put(jsonObject5);*/

        jsonObject.put("friends", jsonArray);
        Log.d("Ashish", jsonObject.toString());

        /*HttpURLConnection con = (HttpURLConnection) new URL("http://52.74.175.56:8080/explore/getObjects").openConnection();
        con.setRequestMethod("POST");
        //con.setDoInput(true);
        con.setDoOutput(true);
        con.connect();

        OutputStream os = con.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os));
        writer.write(jsonObject.toString());
        writer.close();
        os.close();

        BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null)
            sb.append(line);
        br.close();*/

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody
                .create(MediaType.parse("application/json; charset=utf-8"), jsonObject.toString());
        Request request = new Request.Builder()
                .url("http://52.74.175.56:8080/explore/getObjects")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        Log.d("Ashish", response.body().string());
        JSONArray receivedData = new JSONArray(response.body().string());
        final List<ExploreContainer> containers = new ArrayList<>();
        for (int i=0; i<receivedData.length(); i++) {
            JSONObject object = receivedData.getJSONObject(i);
            String title = object.getString("displayName");
            String subTitle = object.getString("artistName");
            String imageId = object.getString("largeImageUrl");
            long contentId = object.getLong("contentId");
            int contentType = object.getInt("contentType");
            long userId = object.getLong("userId");

            containers.add(new ExploreContainer(title, subTitle, imageId, "userImageId",
                    "handle", 3.0f, ExploreTypes.MUSIC, contentId));
        }

        return containers;
    };

    @Override
    public ExploreContainer getContainerForIndex(int index) {

        //return data
        return buffer.getViewItem(index);
    }

    @Override
    public boolean isDoneForDay(ExploreContainer container) {
        return container.getTypes().equals(ExploreTypes.DONE_FOR_TODAY);
    }

    @Override
    public boolean isLoading(ExploreContainer container) {
        return container.getTypes().equals(ExploreTypes.LOADING);
    }

    @Override
    public ExploreContainer getLoadingResponse() {
        return new ExploreContainer(ExploreTypes.LOADING, new Random().nextLong());
    }

    @Override
    public int getCount() {
        return buffer.currentBufferSize();
    }

    @Override
    public synchronized void notifyDataAvailable() {

        //This is UI thread !
        exploreAdapter.notifyDataSetChanged();
    }

}
