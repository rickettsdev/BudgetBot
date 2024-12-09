package com.parable.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TelegramUser {
    private Long id;
    private Boolean is_bot;
    private String first_name;
    private String last_name;
    private String language_code;
    private String type;
}
