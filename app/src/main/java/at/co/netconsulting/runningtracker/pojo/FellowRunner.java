package at.co.netconsulting.runningtracker.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class FellowRunner {
    private String person;
    private String sessionId;
    private double latitude;
    private double longitude;
    private float distance;
    private double currentSpeed;
    private String formattedTimestamp;
}