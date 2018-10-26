package com.abnamro.beeldbankieren.innovation.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payload {

    private String type;
    private String meeting;
    private Object data;
    private String name;

}
