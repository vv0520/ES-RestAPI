package com.itgeima.es.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Goods {
    private Long id;
    private List<String> name;
    private String title;
    private Long price;
}
