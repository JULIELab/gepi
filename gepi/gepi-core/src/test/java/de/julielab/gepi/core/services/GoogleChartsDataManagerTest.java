package de.julielab.gepi.core.services;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import org.apache.tapestry5.json.JSONArray;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
public class GoogleChartsDataManagerTest {

    @Test
    public void testGetPairesWithCommonTarget() {
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

        final GoogleChartsDataManager manager = new GoogleChartsDataManager();
        final JSONArray pairs = manager.getPairsWithCommonTarget(events);
        System.out.println(pairs);
        // These asserts base on first printing the pairs array above, checking that it is correct
        // and writing down what I saw then.
        assertThat(pairs.getJSONArray(0).getString(0)).isEqualTo("arg7");
        assertThat(pairs.getJSONArray(0).getString(1)).isEqualTo("arg8");
        assertThat(pairs.getJSONArray(0).getInt(2)).isEqualTo(10);

        assertThat(pairs.getJSONArray(1).getString(0)).isEqualTo("arg9");
        assertThat(pairs.getJSONArray(1).getString(1)).isEqualTo("arg8");
        assertThat(pairs.getJSONArray(1).getInt(2)).isEqualTo(10);

        assertThat(pairs.getJSONArray(2).getString(0)).isEqualTo("arg1");
        assertThat(pairs.getJSONArray(2).getString(1)).isEqualTo("arg2");
        assertThat(pairs.getJSONArray(2).getInt(2)).isEqualTo(2);

        assertThat(pairs.getJSONArray(3).getString(0)).isEqualTo("arg3");
        assertThat(pairs.getJSONArray(3).getString(1)).isEqualTo("arg2");
        assertThat(pairs.getJSONArray(3).getInt(2)).isEqualTo(3);

        assertThat(pairs.getJSONArray(4).getString(0)).isEqualTo("arg4");
        assertThat(pairs.getJSONArray(4).getString(1)).isEqualTo("arg5");
        assertThat(pairs.getJSONArray(4).getInt(2)).isEqualTo(10);

        assertThat(pairs.getJSONArray(5).getString(0)).isEqualTo("arg6");
        assertThat(pairs.getJSONArray(5).getString(1)).isEqualTo("arg5");
        assertThat(pairs.getJSONArray(5).getInt(2)).isEqualTo(1);
    }
}
