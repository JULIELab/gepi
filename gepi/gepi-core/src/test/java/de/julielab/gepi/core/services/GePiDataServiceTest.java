package de.julielab.gepi.core.services;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.InputMode;
import org.apache.tapestry5.json.JSONArray;
import org.apache.tapestry5.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class GePiDataServiceTest {

    @Test
    public void writeExcelSummary() throws Exception {
        List<Event> events = new ArrayList<>();

        Event e = new Event();
        e.setDocId("135");
        e.setEventId("135_1");
        e.setAllEventTypes(List.of("Binding"));
        Argument a1 = new Argument("g1", "c1", "h1", "arg1");
        a1.setPreferredName("arg1");
        Argument a2 = new Argument("g2", "c2", "h2", "arg2");
        a2.setPreferredName("arg2");
        e.setArguments(Arrays.asList(a1, a2));
        events.add(e);
        // Let's have this event twice
        events.add(e);

        // This event shares the target of the first event
        Event e2 = new Event();
        e2.setDocId("PMC680");
        e2.setEventId("PMC680_2");
        e2.setAllEventTypes(List.of("Binding"));
        Argument a3 = new Argument("g3", "c3", "h3", "arg3");
        a3.setPreferredName("arg3");
        e2.setArguments(Arrays.asList(a3, a2));
        // Lets have this one three times.
        events.add(e2);
        e2 = new Event();
        e2.setDocId("PMC780");
        e2.setEventId("PMC780_1");
        e2.setAllEventTypes(List.of("Binding"));
        e2.setArguments(Arrays.asList(a3, a2));
        events.add(e2);
        e2 = new Event();
        e2.setDocId("PMC926");
        e2.setEventId("PMC926_13");
        e2.setAllEventTypes(List.of("Binding"));
        e2.setArguments(Arrays.asList(a3, a2));
        events.add(e2);

        // And one single event
        Event e4 = new Event();
        e4.setDocId("246");
        e4.setEventId("246_5");
        e4.setAllEventTypes(List.of("Binding"));
        Argument a6 = new Argument("g6", "c6", "h6", "arg6");
        a6.setPreferredName("arg6");
        e4.setArguments(Arrays.asList(a6, a3));
        events.add(e4);


        GePiDataService gePiDataService = new GePiDataService();
        File outputFile = gePiDataService.getOverviewExcel(events, 1234, EnumSet.of(InputMode.A), null, null, null);
        assertThat(outputFile).exists();
    }

    @Test
    public void testGetPairsWithCommonTarget() throws Exception {
        List<Event> events = new ArrayList<>();

        Event e = new Event();
        Argument a1 = new Argument("g1", "c1", "h1", "arg1");
        a1.setPreferredName("arg1");
        Argument a2 = new Argument("g2", "c2", "h2", "arg2");
        a2.setPreferredName("arg2");
        e.setArguments(Arrays.asList(a1, a2));
        events.add(e);
        // Let's have this event twice
        events.add(e);

        // This event shares the target of the first event
        Event e2 = new Event();
        Argument a3 = new Argument("g3", "c3", "h3", "arg3");
        a3.setPreferredName("arg3");
        e2.setArguments(Arrays.asList(a3, a2));
        // We'll have this one three times; to check for the equality relation between Event objects, we actually
        // create this event three times. The test will fail if two events with the same arguments are not equal.
        events.add(e2);
        e2 = new Event();
        e2.setArguments(Arrays.asList(a3, a2));
        events.add(e2);
        e2 = new Event();
        e2.setArguments(Arrays.asList(a3, a2));
        events.add(e2);


        // Now we will create another connected event pair with a more skewed distribution.
        Event e3 = new Event();
        Argument a4 = new Argument("g4", "c4", "h4", "arg4");
        a4.setPreferredName("arg4");
        Argument a5 = new Argument("g5", "c5", "h5", "arg5");
        a5.setPreferredName("arg5");
        e3.setArguments(Arrays.asList(a4, a5));
        // Add this event quite a few times, it is very frequent.
        for (int i = 0; i < 10; i++)
            events.add(e3);

        Event e4 = new Event();
        Argument a6 = new Argument("g6", "c6", "h6", "arg6");
        a6.setPreferredName("arg6");
        e4.setArguments(Arrays.asList(a6, a5));
        // This event is connected via a5 with e3. But it is only present once.
        events.add(e4);

        // And another connected event set that should be on top due to the strong connection
        Event e5 = new Event();
        Argument a7 = new Argument("g7", "c7", "h7", "arg7");
        a7.setPreferredName("arg7");
        Argument a8 = new Argument("g8", "c8", "h8", "arg8");
        a8.setPreferredName("arg8");
        e5.setArguments(Arrays.asList(a7, a8));
        // Add this event quite a few times, it is very frequent.
        for (int i = 0; i < 10; i++)
            events.add(e5);

        Event e6 = new Event();
        Argument a9 = new Argument("g9", "c9", "h9", "arg9");
        a9.setPreferredName("arg9");
        e6.setArguments(Arrays.asList(a9, a8));
        // Add this event quite a few times, it is very frequent.
        for (int i = 0; i < 10; i++)
            events.add(e6);

        final GePiDataService manager = new GePiDataService();
        final JSONObject nodesNLinks = manager.getPairsWithCommonTarget(events);
        final JSONArray pairs = nodesNLinks.getJSONArray("links");
        System.out.println(pairs);
        // These asserts base on first printing the pairs array above, checking that it is correct
        // and writing down what I saw then.
        assertThat(pairs.getJSONObject(0).getString("source")).isEqualTo("h7");
        assertThat(pairs.getJSONObject(0).getString("target")).isEqualTo("h8");
        assertThat(pairs.getJSONObject(0).getInt("frequency")).isEqualTo(10);

        assertThat(pairs.getJSONObject(1).getString("source")).isEqualTo("h9");
        assertThat(pairs.getJSONObject(1).getString("target")).isEqualTo("h8");
        assertThat(pairs.getJSONObject(1).getInt("frequency")).isEqualTo(10);

        assertThat(pairs.getJSONObject(2).getString("source")).isEqualTo("h1");
        assertThat(pairs.getJSONObject(2).getString("target")).isEqualTo("h2");
        assertThat(pairs.getJSONObject(2).getInt("frequency")).isEqualTo(2);

        assertThat(pairs.getJSONObject(3).getString("source")).isEqualTo("h3");
        assertThat(pairs.getJSONObject(3).getString("target")).isEqualTo("h2");
        assertThat(pairs.getJSONObject(3).getInt("frequency")).isEqualTo(3);

        assertThat(pairs.getJSONObject(4).getString("source")).isEqualTo("h4");
        assertThat(pairs.getJSONObject(4).getString("target")).isEqualTo("h5");
        assertThat(pairs.getJSONObject(4).getInt("frequency")).isEqualTo(10);

        assertThat(pairs.getJSONObject(5).getString("source")).isEqualTo("h6");
        assertThat(pairs.getJSONObject(5).getString("target")).isEqualTo("h5");
        assertThat(pairs.getJSONObject(5).getInt("frequency")).isEqualTo(1);
    }
}
