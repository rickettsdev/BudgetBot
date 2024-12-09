package com.parable.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TelegramMessage {
    private Integer message_id;
    private TelegramUser from;
    private TelegramUser chat;
    private Long date;
    private String text;
    private List<TelegramEntity> entities;
}
