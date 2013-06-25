package strawmon;

import java.util.ArrayList;

/**
 * Handle logic surrounding chat-triggered reboots.
 */
public class RobertsRules implements StrawmonEventRaiser {
    // Event listeners
    private ArrayList<StrawmonEventListener> listeners = new ArrayList<StrawmonEventListener>();

    // IRC name of the person asking to reboot
    private String originator;

    // Current server population
    private int population;

    /*
     * Constructor
     */
    public RobertsRules(String originator, int population) {
        this.originator = originator;
        this.population = population;
    }

    /*
     * Handle the Robert's Rules-derived voting process.
     *
     * http://www.robertsrules.com/
     * http://en.wikipedia.org/wiki/Robert%27s_Rules_of_Order
     */
    public void handleReboot() {
        if (this.population < 2) {
            // If the population is less than two, reboot in three minutes
            this.raiseStatusRequestEvent(new String[]{"Low population: restarting in three minutes. !nay to abort"});
            try {
                Thread.sleep(180000);
                // needs to be a config option
            } catch (Exception e) {
                System.out.println("Thread.sleep interrupted.");
                // read whitepapers re multithreading
            }
        } else {
            // If the population is greater than two, initiate the motion

        }
        //this.raise
    }

    /*
     * Implements addListener as required by interface StrawmonEventRaiser.
     */
    public void addListener(StrawmonEventListener listener) {
        this.listeners.add(listener);
    }

    /*
     * Implements removeListener as required by interface StrawmonEventRaiser.
     */
    public void removeListener(StrawmonEventListener listener) {
        this.listeners.remove(listener);
    }

    /*
     * Implements raiseEvent as required by interface StrawmonEventRaiser.
     */
    private void raiseStatusRequestEvent(String statusRequest) {
        StatusRequestEvent event = new StatusRequestEvent(this);
        event.setStatusRequest(statusRequest);
        for (StrawmonEventListener listener : this.listeners) {
            listener.StrawmonEventRaised(event);
        }
    }
}
