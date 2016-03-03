package reach.project.utils;

import android.util.Log;

import com.google.api.client.json.GenericJson;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Created by dexter on 02/03/16.
 */
public enum ContentType {

    MUSIC,
    APP,
    LINK,
    PHOTO;

    public enum State {

        VISIBLE,
        PRESENT,
        LIKED;

        public static EnumMap<ContentType, Map<String, EnumSet<State>>> parseContentStateMap(@Nonnull GenericJson jsonMap) {

            final EnumMap<ContentType, Map<String, EnumSet<State>>> toReturn = new EnumMap<>(ContentType.class);
            toReturn.put(MUSIC, MiscUtils.getMap(100));
            toReturn.put(APP, MiscUtils.getMap(100));

            final Object musicMapObject = jsonMap.get(MUSIC.name());

            if (musicMapObject != null && musicMapObject instanceof Map) {

                Log.i("Ayush", "Found music map");
                final Map<String, EnumSet<State>> mapToAdd = toReturn.get(MUSIC);
                final Map musicMap = (Map) musicMapObject;
                final Set<Map.Entry> entries = musicMap.entrySet();
                for (Map.Entry entry : entries) {

                    final Object metaHashObject = entry.getKey();
                    final Object stateObject = entry.getValue();

                    if (metaHashObject != null && stateObject != null &&
                            metaHashObject instanceof String && stateObject instanceof Set) {

                        final EnumSet<State> setToInsert = EnumSet.noneOf(State.class);
                        final String metaHash = (String) metaHashObject;
                        final Set states = (Set) stateObject;

                        if (states.contains(State.VISIBLE.name()))
                            setToInsert.add(VISIBLE);
                        if (states.contains(State.PRESENT.name()))
                            setToInsert.add(PRESENT);
                        if (states.contains(State.LIKED.name()))
                            setToInsert.add(LIKED);

                        Log.i("Ayush", "Inserting " + metaHash + " " + setToInsert.toString());
                        mapToAdd.put(metaHash, setToInsert);
                    } else {
                        Log.i("Ayush", "Found junk data");
                    }
                }
            }

            final Object appMapObject = jsonMap.get(APP.name());

            if (appMapObject != null && appMapObject instanceof Map) {

                Log.i("Ayush", "Found app map");
                final Map<String, EnumSet<State>> mapToAdd = toReturn.get(APP);
                final Map appMap = (Map) appMapObject;
                final Set<Map.Entry> entries = appMap.entrySet();
                for (Map.Entry entry : entries) {

                    final Object metaHashObject = entry.getKey();
                    final Object stateObject = entry.getValue();

                    if (metaHashObject != null && stateObject != null &&
                            metaHashObject instanceof String && stateObject instanceof Set) {

                        final EnumSet<State> setToInsert = EnumSet.noneOf(State.class);
                        final String metaHash = (String) metaHashObject;
                        final Set states = (Set) stateObject;

                        if (states.contains(State.VISIBLE.name()))
                            setToInsert.add(VISIBLE);
                        if (states.contains(State.PRESENT.name()))
                            setToInsert.add(PRESENT);
                        if (states.contains(State.LIKED.name()))
                            setToInsert.add(LIKED);

                        Log.i("Ayush", "Inserting " + metaHash + " " + setToInsert.toString());
                        mapToAdd.put(metaHash, setToInsert);
                    } else {
                        Log.i("Ayush", "Found junk data");
                    }
                }
            }

            return toReturn;
        }
    }
}