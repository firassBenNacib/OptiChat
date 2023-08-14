package com.app.appfor.entities;
import lombok.*;
import java.io.Serializable;


@Getter
@Setter
@AllArgsConstructor
@ToString
public class user implements Serializable {

    private Long ID;
    private String username;
    private float time;
}
