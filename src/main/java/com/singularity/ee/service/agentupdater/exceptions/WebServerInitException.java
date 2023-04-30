package com.singularity.ee.service.agentupdater.exceptions;

import java.io.IOException;

public class WebServerInitException extends IOException {
    public WebServerInitException( String message ) {
        super(message);
    }
}
