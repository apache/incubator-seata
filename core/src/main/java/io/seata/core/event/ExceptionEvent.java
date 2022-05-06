package io.seata.core.event;

/**
 * Event data for exception.
 *
 * @author Bughue
 */
public class ExceptionEvent implements Event{

    private String name;

    public ExceptionEvent(String code){
        this.name = code;
    }

    public String getName(){
        return name;
    }
}
