package reach.project.utils;

import android.util.Log;

import com.google.api.client.json.GenericJson;

import java.util.Collection;
import java.util.Collections;
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

            final Map<String, EnumSet<State>> musicMapFinal;
            final Map<String, EnumSet<State>> appMapFinal;

            final Object musicMapObject = jsonMap.get(MUSIC.name());
            final Object appMapObject = jsonMap.get(APP.name());

            if (musicMapObject != null && musicMapObject instanceof Map) {

                Log.i("Ayush", "Found music map");
                final Map<String, Collection<String>> musicMap = (Map<String, Collection<String>>) musicMapObject;
                final Set<Map.Entry<String, Collection<String>>> entries = musicMap.entrySet();
                musicMapFinal = MiscUtils.getMap(musicMap.size());

                for (Map.Entry<String, Collection<String>> entry : entries) {

                    final String metaHash = entry.getKey();
                    final Collection<String> states = entry.getValue();
                    final EnumSet<State> setToInsert = EnumSet.noneOf(State.class);

                    for (Object object : states) {

                        if (object.toString().equals(State.VISIBLE.name()))
                            setToInsert.add(VISIBLE);
                        if (object.toString().equals(State.PRESENT.name()))
                            setToInsert.add(PRESENT);
                        if (object.toString().equals(State.LIKED.name()))
                            setToInsert.add(LIKED);
                    }

                    Log.i("Ayush", "Inserting " + metaHash + " " + setToInsert.toString());
                    musicMapFinal.put(metaHash, setToInsert);
                }
            } else
                musicMapFinal = Collections.emptyMap();

            if (appMapObject != null && appMapObject instanceof Map) {

                Log.i("Ayush", "Found app map");
                final Map<String, Collection<String>> appMap = (Map<String, Collection<String>>) appMapObject;
                final Set<Map.Entry<String, Collection<String>>> entries = appMap.entrySet();
                appMapFinal = MiscUtils.getMap(appMap.size());

                for (Map.Entry<String, Collection<String>> entry : entries) {

                    final String metaHash = entry.getKey();
                    final Collection<String> states = entry.getValue();
                    final EnumSet<State> setToInsert = EnumSet.noneOf(State.class);

                    for (Object object : states) {

                        if (object.toString().equals(State.VISIBLE.name()))
                            setToInsert.add(VISIBLE);
                        if (object.toString().equals(State.PRESENT.name()))
                            setToInsert.add(PRESENT);
                        if (object.toString().equals(State.LIKED.name()))
                            setToInsert.add(LIKED);
                    }
g
                    Log.i("Ayush", "Inserting " + metaHash + " " + setToInsert.toString());
                    appMapFinal.put(metaHash, setToInsert);
                }
            } else
                appMapFinal = Collections.emptyMap();

            final EnumMap<ContentType, Map<String, EnumSet<State>>> toReturn = new EnumMap<>(ContentType.class);
            toReturn.put(MUSIC, musicMapFinal);
            toReturn.put(APP, appMapFinal);

            return toReturn;
        }
    }
}