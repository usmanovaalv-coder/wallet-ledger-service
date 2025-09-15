package com.example.walletledgerservice.exception;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApiError {

    String message;
    String code;
    String details;
    String path;
    LocalDateTime timestamp;

}
