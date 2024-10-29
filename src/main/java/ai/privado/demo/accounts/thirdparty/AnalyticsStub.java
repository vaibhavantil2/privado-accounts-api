package ai.privado.demo.analytics;

import com.segment.analytics.Analytics;
import com.segment.analytics.messages.TrackMessage;
import com.mixpanel.mixpanelapi.ClientDelivery;
import com.mixpanel.mixpanelapi.MessageBuilder;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import org.json.JSONObject;
import com.amplitude.api.Amplitude;

public class AnalyticsService {

    // Assuming these are your API keys, store them in environment variables or a config file
    private static final String MIXPANEL_API_KEY = "YOUR_MIXPANEL_API_KEY";
    private static final String SEGMENT_WRITE_KEY = "YOUR_SEGMENT_WRITE_KEY";
    private static final String AMPLITUDE_API_KEY = "YOUR_AMPLITUDE_API_KEY";

    // Mixpanel setup
    private final MixpanelAPI mixpanelAPI;

    // Segment setup
    private final Analytics segmentAnalytics;

    // Amplitude setup
    // Typically, Amplitude is used in mobile apps, but if you are using it server-side, adjust accordingly
    // private final Amplitude amplitude;

    public AnalyticsService() {
        this.mixpanelAPI = new MixpanelAPI();
        this.segmentAnalytics = Analytics.builder(SEGMENT_WRITE_KEY).build();
        // this.amplitude = Amplitude.getInstance().initialize(context, AMPLITUDE_API_KEY); // Context is needed in case of mobile apps
    }

    public void trackEvent(String eventName, JSONObject properties) {
        // Track event for Mixpanel
        MessageBuilder messageBuilder = new MessageBuilder(MIXPANEL_API_KEY);
        JSONObject event = messageBuilder.event("distinct_id", eventName, properties);
        ClientDelivery delivery = new ClientDelivery();
        delivery.addMessage(event);
        mixpanelAPI.deliver(delivery);

        // Track event for Segment
        segmentAnalytics.enqueue(TrackMessage.builder(eventName)
            .properties(properties.toMap())); // Conversion to Map might be needed

        // Track event for Amplitude
        // Amplitude.getInstance().logEvent(eventName, properties); // Uncomment and adjust if using Amplitude server-side

        // Consider doing these operations asynchronously or in a separate thread to not block main execution
    }

    // Add methods for other types of interactions (identify users, track pages, etc.) as needed

    // Remember to gracefully shutdown these services, especially for Segment
    public void shutdown() {
        segmentAnalytics.shutdown();
    }
}
