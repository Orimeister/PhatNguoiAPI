package orimeister.phatnong;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TrafficViolation {
    private String dateTime;
    private String location;
    private String violationType;
    private String status;
}
