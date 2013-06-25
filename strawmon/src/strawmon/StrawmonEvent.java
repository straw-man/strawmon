package strawmon;

import java.util.EventObject;

/**
 * Base class for Strawmon event classes.
 */
abstract public class StrawmonEvent extends EventObject {
    /*
     * Class variables.
     */
    private static final long serialVersionUID = 1;
    private String[] messages;

    /*
     * Constructor.
     */
    public StrawmonEvent(StrawmonEvent source) {
        super(source);
    }

    /*
     * Getters.
     */
    public String[] getMessages()                   { return this.messages;     }
    public Object getSource()                       { return this.source;	    }

    /*
     * Setters.
     */
    public void setMessages(String[] messages)      { this.messages = messages; }
    public void setSource(StrawmonEvent source)     { this.source = source;     }
}