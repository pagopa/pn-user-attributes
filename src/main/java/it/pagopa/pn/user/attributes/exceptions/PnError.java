package it.pagopa.pn.user.attributes.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.io.Serializable;

@Data
public class PnError implements Serializable
{
    private static final long serialVersionUID = 2405172041950251807L;
    @JsonIgnore
    private HttpStatus httpStatus;
    private long status;
    private String error;
    private String message;

    public PnError(String message, HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.message = message;
    }

}
