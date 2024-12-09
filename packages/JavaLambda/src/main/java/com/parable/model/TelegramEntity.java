package com.parable.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TelegramEntity {
    private Integer offset;
    private Integer length;
    private String type;
}
