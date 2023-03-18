package Plane.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActuatorData {
    private String routingKey;

    private String msg;
}
